package com.onetime.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onetime.app.data.ActiveTimer
import com.onetime.app.data.AppData
import com.onetime.app.data.BUILTIN_SHOP_ITEMS
import com.onetime.app.data.MAX_CUSTOM_DURATIONS
import com.onetime.app.data.MAX_CUSTOM_ITEMS
import com.onetime.app.data.MAX_DURATION
import com.onetime.app.data.MAX_NOTE_LENGTH
import com.onetime.app.data.MAX_SUBTASKS
import com.onetime.app.data.MIN_DURATION
import com.onetime.app.data.NoteItem
import com.onetime.app.data.OneTimeStore
import com.onetime.app.data.PAUSE_GRACE_MS
import com.onetime.app.data.PAUSE_VOUCHER_ID
import com.onetime.app.data.Redemption
import com.onetime.app.data.Session
import com.onetime.app.data.QuickSlot
import com.onetime.app.data.ShopItem
import com.onetime.app.data.STOP_VOUCHER_ID
import com.onetime.app.data.Subtask
import com.onetime.app.data.TimerMode
import com.onetime.app.data.TodoItem
import com.onetime.app.data.coinsForMinutes
import com.onetime.app.data.today
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

@Serializable
data class BackupEnvelope(
    val app: String = "ONE TIME",
    val schemaVersion: Int = 2,
    val exportedAt: String = "",
    val data: AppData = AppData(),
)

/** 结算弹窗数据 */
data class Settlement(
    val taskName: String,
    val minutes: Int,
    val coins: Double,
    val completed: Boolean,
    val reason: String,
)

/** 计时派生状态 */
data class LiveTimer(
    val active: Boolean = false,
    val running: Boolean = false,
    val paused: Boolean = false,
    val mode: String = TimerMode.COUNT_UP,
    val label: String = "",
    val plannedMinutes: Int? = null,
    val elapsedMs: Long = 0L,
    val remainingMs: Long = 0L,
    val pauseCount: Int = 0,
    val graceRemainingMs: Long = 0L,
) {
    val isCountDown: Boolean get() = mode == TimerMode.COUNT_DOWN && plannedMinutes != null
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = OneTimeStore(app)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    private val _data = MutableStateFlow(store.load().withSeed())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    /** 任务输入框内容 */
    private val _taskInput = MutableStateFlow("")
    val taskInput: StateFlow<String> = _taskInput.asStateFlow()

    /** 当前选中的倒计时分钟；null = 正计时 */
    private val _selectedMinutes = MutableStateFlow<Int?>(null)
    val selectedMinutes: StateFlow<Int?> = _selectedMinutes.asStateFlow()

    private val _settlement = MutableStateFlow<Settlement?>(null)
    val settlement: StateFlow<Settlement?> = _settlement.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private var tickJob: Job? = null
    private var clockJob: Job? = null
    private var saveJob: Job? = null

    init {
        startClock()
        ensureTick()
        persist()
    }

    // ---------------- 计时 ----------------

    fun liveTimer(): LiveTimer {
        val t = _data.value.timer
        val selected = _selectedMinutes.value
        if (t == null) {
            return LiveTimer(
                active = false,
                mode = if (selected != null) TimerMode.COUNT_DOWN else TimerMode.COUNT_UP,
                plannedMinutes = selected,
                label = _taskInput.value,
            )
        }
        val nowMs = _now.value
        val elapsed = if (t.paused) t.accumulatedMs else t.accumulatedMs + (nowMs - t.startedAt)
        val total = (t.plannedMinutes ?: 0) * 60_000L
        return LiveTimer(
            active = true,
            running = !t.paused,
            paused = t.paused,
            mode = t.mode,
            label = t.label,
            plannedMinutes = t.plannedMinutes,
            elapsedMs = elapsed,
            remainingMs = if (t.mode == TimerMode.COUNT_DOWN) (total - elapsed).coerceAtLeast(0) else 0,
            pauseCount = t.pauseCount,
            graceRemainingMs = if (t.paused) (PAUSE_GRACE_MS - (nowMs - t.pausedAt)).coerceAtLeast(0) else 0,
        )
    }

    fun setTaskInput(text: String) {
        _taskInput.value = text.take(80)
    }

    /** 选择今日任务：普通待办只填文本，子任务带分钟数并切倒计时 */
    fun applyTodoSelection(todo: TodoItem, subtask: Subtask? = null) {
        _taskInput.value = if (subtask != null) "${todo.title} · ${subtask.title}" else todo.title
        if (subtask != null) {
            setPlannedMinutes(subtask.minutes)
        }
    }

    /** 设定倒计时分钟数（5–180）；null 表示回到正计时 */
    fun setPlannedMinutes(minutes: Int?) {
        if (_data.value.timer != null) return
        _selectedMinutes.value = minutes?.coerceIn(MIN_DURATION, MAX_DURATION)
    }

    fun start() {
        val nowMs = System.currentTimeMillis()
        val d = _data.value
        val t = d.timer
        if (t == null) {
            val selected = _selectedMinutes.value
            _data.update {
                it.copy(
                    timer = ActiveTimer(
                        mode = if (selected != null) TimerMode.COUNT_DOWN else TimerMode.COUNT_UP,
                        label = _taskInput.value,
                        plannedMinutes = selected,
                        startedAt = nowMs,
                        accumulatedMs = 0L,
                    ),
                )
            }
        } else {
            _data.update {
                it.copy(timer = t.copy(paused = false, startedAt = nowMs, pausedAt = 0L))
            }
        }
        ensureTick()
        persist()
    }

    fun pauseVoucherCount(): Int = _data.value.shopItems.firstOrNull { it.id == PAUSE_VOUCHER_ID }?.stock ?: 0
    fun stopVoucherCount(): Int = _data.value.shopItems.firstOrNull { it.id == STOP_VOUCHER_ID }?.stock ?: 0

    /** 确认暂停：消耗 1 张暂停券 */
    fun pause(reason: String) {
        val d = _data.value
        val t = d.timer ?: return
        if (t.paused) return
        if (pauseVoucherCount() <= 0) {
            _toast.value = "暂停券不足，去商城补充吧"
            return
        }
        val nowMs = System.currentTimeMillis()
        val elapsed = t.accumulatedMs + (nowMs - t.startedAt)
        updateVoucher(PAUSE_VOUCHER_ID, -1)
        _data.update {
            it.copy(
                timer = t.copy(
                    paused = true,
                    pausedAt = nowMs,
                    accumulatedMs = elapsed,
                    pauseCount = t.pauseCount + 1,
                    pauseReason = reason,
                ),
            )
        }
        ensureTick()
        persist()
    }

    /** 正计时第三次暂停：确认后结算为完成 */
    fun pauseOrComplete(reason: String) {
        val t = _data.value.timer ?: return
        val isCountUp = t.mode == TimerMode.COUNT_UP
        if (isCountUp && t.pauseCount >= 2) {
            if (pauseVoucherCount() <= 0) {
                _toast.value = "暂停券不足，去商城补充吧"
                return
            }
            updateVoucher(PAUSE_VOUCHER_ID, -1)
            finish(completed = true, reason = reason, allowCoin = true)
            return
        }
        pause(reason)
    }

    /** 继续（5 分钟宽限内） */
    fun resume() {
        val t = _data.value.timer ?: return
        if (!t.paused) return
        val nowMs = System.currentTimeMillis()
        if (nowMs - t.pausedAt > PAUSE_GRACE_MS) {
            finish(completed = false, reason = "暂停超时", allowCoin = false)
            return
        }
        _data.update { it.copy(timer = t.copy(paused = false, startedAt = nowMs, pausedAt = 0L)) }
        ensureTick()
        persist()
    }

    /** 手动结束：消耗 1 张停止券 */
    fun manualFinish(reason: String) {
        val t = _data.value.timer ?: return
        if (stopVoucherCount() <= 0) {
            _toast.value = "停止券不足，去商城补充吧"
            return
        }
        updateVoucher(STOP_VOUCHER_ID, -1)
        val isCountUp = t.mode == TimerMode.COUNT_UP
        finish(completed = isCountUp, reason = reason, allowCoin = isCountUp)
    }

    private fun finish(completed: Boolean, reason: String, allowCoin: Boolean) {
        val d = _data.value
        val t = d.timer ?: return
        val nowMs = System.currentTimeMillis()
        val elapsed = if (t.paused) t.accumulatedMs else t.accumulatedMs + (nowMs - t.startedAt)
        val minutes = ((elapsed + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
        val coins = if (allowCoin && completed) coinsForMinutes(minutes) else 0.0
        val session = Session(
            id = newId(),
            taskName = t.label.ifBlank { "专注" },
            startAt = nowMs - elapsed,
            endAt = nowMs,
            minutes = minutes,
            coins = coins,
            plannedMinutes = t.plannedMinutes,
            status = if (completed) Session.STATUS_COMPLETED else Session.STATUS_INTERRUPTED,
            reason = reason,
            taskId = t.taskId,
        )
        _data.update {
            it.copy(
                coins = it.coins + coins,
                totalEarned = it.totalEarned + coins,
                sessions = (listOf(session) + it.sessions).take(1000),
                timer = null,
            )
        }
        _settlement.value = Settlement(
            taskName = session.taskName,
            minutes = minutes,
            coins = coins,
            completed = completed,
            reason = reason,
        )
        _taskInput.value = ""
        tickJob?.cancel()
        persist()
    }

    /** 倒计时自然结束 */
    private fun finishNaturally() {
        val d = _data.value
        val t = d.timer ?: return
        val planned = t.plannedMinutes ?: return
        val coins = coinsForMinutes(planned)
        val nowMs = System.currentTimeMillis()
        val session = Session(
            id = newId(),
            taskName = t.label.ifBlank { "专注" },
            startAt = nowMs - planned * 60_000L,
            endAt = nowMs,
            minutes = planned,
            coins = coins,
            plannedMinutes = planned,
            status = Session.STATUS_COMPLETED,
            taskId = t.taskId,
        )
        _data.update {
            it.copy(
                coins = it.coins + coins,
                totalEarned = it.totalEarned + coins,
                sessions = (listOf(session) + it.sessions).take(1000),
                timer = null,
            )
        }
        _settlement.value = Settlement(session.taskName, planned, coins, true, "倒计时完成")
        _taskInput.value = ""
        tickJob?.cancel()
        persist()
    }

    fun dismissSettlement(saveToNotes: Boolean, noteText: String) {
        val s = _settlement.value ?: return
        if (saveToNotes && noteText.isNotBlank()) {
            val body = noteText.trim().take(MAX_NOTE_LENGTH)
            val note = NoteItem(id = newId(), text = body, createdAt = System.currentTimeMillis(), taskName = s.taskName)
            _data.update { it.copy(notes = listOf(note) + it.notes) }
            _toast.value = "已保存到随记本"
            persist()
        }
        _settlement.value = null
    }

    private fun ensureTick() {
        if (tickJob?.isActive == true) return
        tickJob = viewModelScope.launch {
            while (isActive) {
                val t = _data.value.timer
                if (t == null) break
                val nowMs = System.currentTimeMillis()
                _now.value = nowMs
                if (t.paused) {
                    if (nowMs - t.pausedAt > PAUSE_GRACE_MS) {
                        finish(completed = false, reason = "暂停超时", allowCoin = false)
                        break
                    }
                } else if (t.mode == TimerMode.COUNT_DOWN && t.plannedMinutes != null) {
                    val elapsed = t.accumulatedMs + (nowMs - t.startedAt)
                    if (elapsed >= t.plannedMinutes * 60_000L) {
                        finishNaturally()
                        break
                    }
                }
                delay(200)
            }
        }
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (isActive) {
                _now.value = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    // ---------------- 设置 ----------------

    fun toggleTheme() {
        _data.update { it.copy(settings = it.settings.copy(night = !it.settings.night)) }
        persist()
    }

    fun toggleUnit() {
        _data.update { it.copy(settings = it.settings.copy(unitMinute = !it.settings.unitMinute)) }
        persist()
    }

    fun toggleSeconds() {
        _data.update { it.copy(settings = it.settings.copy(showSeconds = !it.settings.showSeconds)) }
        persist()
    }

    /** 拉环松手：把当前时长存成常用时段（已存在则不重复） */
    fun saveSlotFromMinutes(minutes: Int) {
        val m = minutes.coerceIn(MIN_DURATION, MAX_DURATION)
        val list = _data.value.settings.customSlots
        if (list.any { it.minutes == m }) return
        if (list.size >= MAX_CUSTOM_DURATIONS) {
            _toast.value = "最多 $MAX_CUSTOM_DURATIONS 个常用时段"
            return
        }
        val slot = QuickSlot(id = newId(), name = "$m 分钟", minutes = m)
        _data.update { it.copy(settings = it.settings.copy(customSlots = list + slot)) }
        _toast.value = "已固定「$m 分钟」"
        persist()
    }

    fun updateSlot(id: String, name: String, minutes: Int) {
        val m = minutes.coerceIn(MIN_DURATION, MAX_DURATION)
        _data.update { d ->
            d.copy(
                settings = d.settings.copy(
                    customSlots = d.settings.customSlots.map {
                        if (it.id == id) it.copy(name = name.ifBlank { "$m 分钟" }, minutes = m) else it
                    },
                ),
            )
        }
        persist()
    }

    fun removeSlot(id: String) {
        _data.update { d ->
            d.copy(settings = d.settings.copy(customSlots = d.settings.customSlots.filterNot { it.id == id }))
        }
        persist()
    }

    fun markNeedleIntroPlayed() {
        if (_data.value.settings.needleIntroPlayed) return
        _data.update { it.copy(settings = it.settings.copy(needleIntroPlayed = true)) }
        persist()
    }

    fun addQuickDay(days: Int) {
        val d = days.coerceIn(1, 60)
        val list = _data.value.settings.quickDays
        if (list.contains(d)) return
        _data.update { it.copy(settings = it.settings.copy(quickDays = (list + d).sorted().take(5))) }
        persist()
    }

    // ---------------- 待办 ----------------

    fun addTodo(title: String, dueAt: Long) {
        val text = title.trim()
        if (text.isEmpty()) return
        val item = TodoItem(
            id = newId(),
            title = text,
            dueAt = dueAt,
            createdAt = System.currentTimeMillis(),
        )
        _data.update { it.copy(todos = listOf(item) + it.todos) }
        persist()
    }

    fun addGoal(title: String, dueAt: Long, subtasks: List<Subtask>) {
        val text = title.trim()
        if (text.isEmpty() || subtasks.isEmpty()) return
        val item = TodoItem(
            id = newId(),
            title = text,
            dueAt = dueAt,
            createdAt = System.currentTimeMillis(),
            subtasks = subtasks.take(MAX_SUBTASKS),
        )
        _data.update { it.copy(todos = listOf(item) + it.todos) }
        persist()
    }

    fun toggleTodo(id: String) {
        _data.update { d ->
            val item = d.todos.find { it.id == id } ?: return@update d
            val nowDone = !item.done
            val nowMs = System.currentTimeMillis()
            val updated = item.copy(
                done = nowDone,
                doneAt = if (nowDone) nowMs else null,
                subtasks = item.subtasks.map {
                    it.copy(done = nowDone, doneAt = if (nowDone) nowMs else null)
                },
            )
            d.copy(todos = d.todos.map { if (it.id == id) updated else it })
        }
        persist()
    }

    fun toggleSubtask(todoId: String, subtaskId: String) {
        _data.update { d ->
            val item = d.todos.find { it.id == todoId } ?: return@update d
            val nowMs = System.currentTimeMillis()
            val subs = item.subtasks.map {
                if (it.id == subtaskId) {
                    it.copy(done = !it.done, doneAt = if (!it.done) nowMs else null)
                } else {
                    it
                }
            }
            val allDone = subs.isNotEmpty() && subs.all { it.done }
            val updated = item.copy(
                subtasks = subs,
                done = if (subs.isEmpty()) item.done else allDone,
                doneAt = when {
                    subs.isEmpty() -> item.doneAt
                    allDone -> item.doneAt ?: nowMs
                    else -> null
                },
            )
            d.copy(todos = d.todos.map { if (it.id == todoId) updated else it })
        }
        persist()
    }

    fun moveSubtask(todoId: String, index: Int, delta: Int) {
        _data.update { d ->
            val item = d.todos.find { it.id == todoId } ?: return@update d
            val list = item.subtasks.toMutableList()
            val target = index + delta
            if (index !in list.indices || target !in list.indices) return@update d
            val tmp = list[index]
            list[index] = list[target]
            list[target] = tmp
            d.copy(todos = d.todos.map { if (it.id == todoId) it.copy(subtasks = list) else it })
        }
        persist()
    }

    fun updateSubtaskMinutes(todoId: String, subtaskId: String, minutes: Int) {
        _data.update { d ->
            d.copy(
                todos = d.todos.map { todo ->
                    if (todo.id != todoId) todo else todo.copy(
                        subtasks = todo.subtasks.map {
                            if (it.id == subtaskId) it.copy(minutes = minutes.coerceIn(MIN_DURATION, MAX_DURATION)) else it
                        },
                    )
                },
            )
        }
        persist()
    }

    fun replaceSubtasks(todoId: String, subtasks: List<Subtask>) {
        _data.update { d ->
            d.copy(
                todos = d.todos.map {
                    if (it.id == todoId) it.copy(subtasks = subtasks.take(MAX_SUBTASKS)) else it
                },
            )
        }
        persist()
    }

    fun setTodoDueDate(id: String, dueAt: Long) {
        _data.update { d ->
            d.copy(todos = d.todos.map { if (it.id == id) it.copy(dueAt = dueAt) else it })
        }
        persist()
    }

    fun deleteTodo(id: String) {
        _data.update { d -> d.copy(todos = d.todos.filterNot { it.id == id }) }
        persist()
    }

    /** 从待办开始专注：填入任务，自动切回专注 */
    fun startFocusFromTodo(todo: TodoItem, subtask: Subtask? = null) {
        if (_data.value.timer != null) {
            _toast.value = "正在专注中，先结束当前计时"
            return
        }
        _taskInput.value = if (subtask != null) "${todo.title} · ${subtask.title}" else todo.title
        val minutes = subtask?.minutes
        _data.update {
            it.copy(
                timer = if (minutes != null) {
                    ActiveTimer(
                        mode = TimerMode.COUNT_DOWN,
                        label = _taskInput.value,
                        plannedMinutes = minutes,
                        startedAt = System.currentTimeMillis(),
                        accumulatedMs = 0L,
                        taskId = todo.id,
                        subtaskId = subtask.id,
                    )
                } else {
                    ActiveTimer(
                        mode = TimerMode.COUNT_UP,
                        label = _taskInput.value,
                        plannedMinutes = null,
                        startedAt = System.currentTimeMillis(),
                        accumulatedMs = 0L,
                        taskId = todo.id,
                    )
                },
            )
        }
        ensureTick()
        persist()
    }

    // ---------------- 随记 ----------------

    fun addNote(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        val note = NoteItem(id = newId(), text = body.take(MAX_NOTE_LENGTH), createdAt = System.currentTimeMillis())
        _data.update { it.copy(notes = listOf(note) + it.notes) }
        _toast.value = "已保存"
        persist()
    }

    fun updateNote(id: String, text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        _data.update { d ->
            d.copy(
                notes = d.notes.map {
                    if (it.id == id) it.copy(text = body.take(MAX_NOTE_LENGTH), editedAt = System.currentTimeMillis()) else it
                },
            )
        }
        persist()
    }

    // ---------------- 商城 ----------------

    fun redeem(item: ShopItem) {
        val d = _data.value
        if (d.coins < item.price) {
            _toast.value = "金币不足"
            return
        }
        val record = Redemption(
            id = newId(),
            name = item.name,
            emoji = item.emoji,
            price = item.price,
            at = System.currentTimeMillis(),
        )
        _data.update {
            it.copy(
                coins = it.coins - item.price,
                shopItems = it.shopItems.map { s ->
                    if (s.id == item.id && s.isVoucherLike()) s.copy(stock = s.stock + 1) else s
                },
                redemptions = (listOf(record) + it.redemptions).take(300),
            )
        }
        _toast.value = "已兑换「${item.name}」"
        persist()
    }

    private fun ShopItem.isVoucherLike(): Boolean = voucher != null

    fun addCustomItem(name: String, price: Double) {
        val n = name.trim()
        if (n.isEmpty()) return
        val customs = _data.value.shopItems.count { it.custom }
        if (customs >= MAX_CUSTOM_ITEMS) {
            _toast.value = "自定义商品最多 $MAX_CUSTOM_ITEMS 个"
            return
        }
        val item = ShopItem(
            id = newId(),
            name = n.take(40),
            emoji = "🎁",
            price = price.coerceIn(1.0, 9999.0),
            custom = true,
        )
        _data.update { it.copy(shopItems = it.shopItems + item) }
        persist()
    }

    fun deleteCustomItem(id: String) {
        _data.update { d -> d.copy(shopItems = d.shopItems.filterNot { it.id == id && it.custom }) }
        persist()
    }

    private fun updateVoucher(id: String, delta: Int) {
        _data.update { d ->
            d.copy(
                shopItems = d.shopItems.map {
                    if (it.id == id) it.copy(stock = (it.stock + delta).coerceAtLeast(0)) else it
                },
            )
        }
    }

    // ---------------- 备份 ----------------

    fun exportJson(): String {
        val envelope = BackupEnvelope(
            exportedAt = Instant.now().toString(),
            data = _data.value,
        )
        return json.encodeToString(envelope)
    }

    fun exportFileName(): String {
        val t = Instant.now().atZone(java.time.ZoneId.systemDefault())
        return "one-time-backup-%04d%02d%02d-%02d%02d.json".format(
            t.year, t.monthValue, t.dayOfMonth, t.hour, t.minute,
        )
    }

    fun importJson(raw: String): Boolean {
        return runCatching {
            val envelope = json.decodeFromString<BackupEnvelope>(raw)
            require(envelope.schemaVersion == 2)
            val imported = envelope.data.copy(seeded = true)
            tickJob?.cancel()
            _data.value = imported
            _settlement.value = null
            _taskInput.value = ""
            persist()
        }.isSuccess
    }

    // ---------------- 通用 ----------------

    fun consumeToast() {
        _toast.value = null
    }

    private fun persist() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(200)
            val snapshot = _data.value
            withContext(Dispatchers.IO) { store.save(snapshot) }
        }
    }

    override fun onCleared() {
        runCatching { store.save(_data.value) }
        super.onCleared()
    }

    private fun newId(): String = UUID.randomUUID().toString()
}

private fun AppData.withSeed(): AppData =
    if (seeded) {
        this
    } else {
        copy(
            seeded = true,
            shopItems = if (shopItems.isEmpty()) BUILTIN_SHOP_ITEMS else shopItems,
            settings = settings.copy(night = true),
        )
    }
