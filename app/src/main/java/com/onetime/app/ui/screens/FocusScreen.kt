package com.onetime.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetime.app.data.AppData
import com.onetime.app.data.QuickSlot
import com.onetime.app.data.MAX_DURATION
import com.onetime.app.data.MIN_DURATION
import com.onetime.app.data.Subtask
import com.onetime.app.data.TodoItem
import com.onetime.app.data.TodoItem as Todo
import com.onetime.app.data.coinText
import com.onetime.app.data.formatDueDate
import com.onetime.app.data.todayCompletedCount
import com.onetime.app.data.todayCoins
import com.onetime.app.data.todayOpenTodos
import com.onetime.app.ui.LiveTimer
import com.onetime.app.ui.components.StopwatchDial
import com.onetime.app.ui.theme.CoinIcon
import com.onetime.app.ui.chrome.IconGhost
import com.onetime.app.ui.theme.InsetBox
import com.onetime.app.ui.theme.SoftCard
import com.onetime.app.ui.theme.KeyCap
import com.onetime.app.ui.theme.Paper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    data: AppData,
    timer: LiveTimer,
    selectedMinutes: Int?,
    taskInput: String,
    onTaskInput: (String) -> Unit,
    onSelectMinutes: (Int?) -> Unit,
    onToggleUnit: () -> Unit,
    onToggleTimer: () -> Unit,
    onPause: (String) -> Unit,
    onPauseOrComplete: (String) -> Unit,
    onStop: (String) -> Unit,
    onResume: () -> Unit,
    onSaveSlot: () -> Unit,
    onUpdateSlot: (String, String, Int) -> Unit,
    onRemoveSlot: (String) -> Unit,
    onPickTodo: (Todo, Subtask?) -> Unit,
    playIntro: Boolean = false,
    onIntroFinished: () -> Unit = {},
) {
    val palette = Paper.current
    var showPause by remember { mutableStateOf(false) }
    var showStop by remember { mutableStateOf(false) }
    var showSelector by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<QuickSlot?>(null) }

    val pauseTickets = data.shopItems.firstOrNull { it.voucher == "pause" }?.stock ?: 0
    val stopTickets = data.shopItems.firstOrNull { it.voucher == "stop" }?.stock ?: 0
    val isThirdPause = data.timer != null &&
        data.timer.mode == com.onetime.app.data.TimerMode.COUNT_UP &&
        data.timer.pauseCount >= 2

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 任务输入
        SoftCard(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (taskInput.isEmpty()) {
                        Text(
                            "这次要专注做什么？",
                            fontSize = 14.5.sp,
                            color = palette.muted,
                        )
                    }
                    BasicTextField(
                        value = taskInput,
                        onValueChange = { if (it.length <= 80) onTaskInput(it) },
                        enabled = !timer.active,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.5.sp, color = palette.ink),
                        cursorBrush = SolidColor(palette.coral),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (!timer.active) onToggleTimer() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                IconGhost(
                    icon = Icons.Filled.KeyboardArrowDown,
                    description = "今日任务选择器",
                    onClick = { if (!timer.active) showSelector = true },
                    tint = palette.inkSoft,
                )
            }
        }

        Spacer(Modifier.weight(0.45f))

        StopwatchDial(
            timer = timer,
            selectedMinutes = selectedMinutes,
            unitMinute = data.settings.unitMinute,
            slots = data.settings.customSlots,
            pauseTickets = pauseTickets,
            stopTickets = stopTickets,
            onSelectMinutes = onSelectMinutes,
            onToggleUnit = onToggleUnit,
            onToggleTimer = onToggleTimer,
            onPause = {
                when {
                    timer.paused -> onResume()
                    !timer.active -> Unit
                    isThirdPause -> showPause = true
                    else -> showPause = true
                }
            },
            onStop = { if (timer.active) showStop = true },
            onHangerRelease = onSaveSlot,
            playIntro = playIntro,
            onIntroFinished = onIntroFinished,
        )

        // 自定义时长（金色标签）
        Spacer(Modifier.height(16.dp))

        // 今日统计
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatCell("今日金币", data.todayCoins().coinText(), Modifier.weight(1f), withCoin = true)
            StatDivider()
            StatCell("今日完成", data.todayCompletedCount().toString(), Modifier.weight(1f))
            StatDivider()
            StatCell("累计完成", data.sessions.size.toString(), Modifier.weight(1f))
        }

        Spacer(Modifier.weight(1f))
    }

    if (showPause) {
        ReasonDialog(
            title = "暂停专注",
            task = timer.label.ifBlank { "未命名专注" },
            timeText = if (timer.isCountDown) "剩余 ${formatRemaining(timer)}" else "已专注 ${formatElapsed(timer)}",
            warning = if (isThirdPause) {
                "这是第 3 次暂停：确认后将直接结束本次专注并结算金币。"
            } else {
                "暂停 5 分钟内继续可保留金币，超过 5 分钟本次奖励取消。"
            },
            voucherText = "本次将消耗 1 张暂停券（剩余 $pauseTickets 张）",
            confirmText = if (isThirdPause) "确认结束" else "确认暂停",
            onDismiss = { showPause = false },
            onConfirm = { reason ->
                showPause = false
                if (isThirdPause) onPauseOrComplete(reason) else onPause(reason)
            },
        )
    }

    if (showStop) {
        ReasonDialog(
            title = "结束专注",
            task = timer.label.ifBlank { "未命名专注" },
            timeText = if (timer.isCountDown) "剩余 ${formatRemaining(timer)}" else "已专注 ${formatElapsed(timer)}",
            warning = if (timer.isCountDown) {
                "倒计时手动结束按中断处理，本次不获得金币。"
            } else {
                "正计时手动结束按完成处理，按实际时长结算金币。"
            },
            voucherText = "本次将消耗 1 张停止券（剩余 $stopTickets 张）",
            confirmText = "结束",
            onDismiss = { showStop = false },
            onConfirm = { reason ->
                showStop = false
                onStop(reason)
            },
        )
    }

    editingSlot?.let { slot ->
        SlotMenu(
            slot = slot,
            onDismiss = { editingSlot = null },
            onSave = { name, minutes ->
                onUpdateSlot(slot.id, name, minutes)
                editingSlot = null
            },
            onDelete = {
                onRemoveSlot(slot.id)
                editingSlot = null
            },
        )
    }

    if (showSelector) {
        TodoSelectorSheet(
            todos = data.todayOpenTodos(),
            onDismiss = { showSelector = false },
            onPick = { todo, sub ->
                onPickTodo(todo, sub)
                showSelector = false
            },
        )
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier, withCoin: Boolean = false) {
    val palette = Paper.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.5.sp, color = palette.muted)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (withCoin) CoinIcon(size = 12.dp)
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = palette.goldDeep)
        }
    }
}

@Composable
private fun StatDivider() {
    Box(
        Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(26.dp)
            .background(Paper.current.line),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomDurationChip(
    slot: QuickSlot,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val palette = Paper.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(palette.gold.copy(alpha = if (selected) 0.28f else 0.14f))
            .border(1.dp, palette.gold.copy(alpha = if (selected) 0.8f else 0.4f), RoundedCornerShape(9.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            slot.name,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.goldDeep,
        )
    }
}

@Composable
private fun ReasonDialog(
    title: String,
    task: String,
    timeText: String,
    warning: String,
    voucherText: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val palette = Paper.current
    var reason by remember { mutableStateOf("") }
    val quick = listOf("接电话", "临时会议", "需要休息", "身体不适", "其他")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        title = { Text(title, color = palette.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(task, fontSize = 14.sp, color = palette.ink, fontWeight = FontWeight.Medium)
                Text(timeText, fontSize = 12.sp, color = palette.muted)
                Spacer(Modifier.height(10.dp))
                Text(warning, fontSize = 12.sp, color = palette.coralDeep, lineHeight = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text(voucherText, fontSize = 12.sp, color = palette.goldDeep)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 160) reason = it },
                    label = { Text("原因（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    quick.take(3).forEach { q ->
                        QuickChip(q) { reason = q }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    quick.drop(3).forEach { q ->
                        QuickChip(q) { reason = q }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason) }) {
                Text(confirmText, color = palette.coralDeep, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = palette.muted) }
        },
    )
}

@Composable
private fun QuickChip(text: String, onClick: () -> Unit) {
    val palette = Paper.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(palette.inset)
            .border(1.dp, palette.line, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, fontSize = 11.sp, color = palette.inkSoft)
    }
}

@Composable
private fun SlotMenu(
    slot: QuickSlot,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
    onDelete: () -> Unit,
) {
    val palette = Paper.current
    var name by remember { mutableStateOf(slot.name) }
    var value by remember { mutableStateOf(slot.minutes.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        title = { Text("常用时段", color = palette.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 12) name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { input -> value = input.filter { it.isDigit() }.take(3) },
                    label = { Text("分钟（$MIN_DURATION–$MAX_DURATION）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDelete) { Text("删除", color = palette.danger) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), value.toIntOrNull() ?: slot.minutes) }) {
                Text("保存", color = palette.coralDeep, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = palette.muted) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoSelectorSheet(
    todos: List<Todo>,
    onDismiss: () -> Unit,
    onPick: (Todo, Subtask?) -> Unit,
) {
    val palette = Paper.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.card,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("今日任务", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = palette.ink)
            Spacer(Modifier.height(3.dp))
            Text("只显示今天到期且未完成的任务", fontSize = 12.sp, color = palette.muted)
            Spacer(Modifier.height(12.dp))
            if (todos.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("今天没有可用的待办", fontSize = 13.sp, color = palette.muted)
                }
            } else {
                todos.forEach { todo ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPick(todo, null) }
                            .padding(vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = palette.inkSoft,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                todo.title,
                                fontSize = 14.sp,
                                color = palette.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text("正计时", fontSize = 10.5.sp, color = palette.muted)
                        }
                        todo.subtasks.filterNot { it.done }.forEach { sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 23.dp, top = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onPick(todo, sub) }
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("·", color = palette.muted)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    sub.title,
                                    fontSize = 13.sp,
                                    color = palette.inkSoft,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text("${sub.minutes} 分", fontSize = 10.5.sp, color = palette.goldDeep)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatRemaining(timer: LiveTimer): String = com.onetime.app.data.formatDuration(timer.remainingMs)
private fun formatElapsed(timer: LiveTimer): String = com.onetime.app.data.formatDuration(timer.elapsedMs)
