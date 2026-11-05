package com.onetime.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetime.app.data.AppData
import com.onetime.app.data.MAX_DURATION
import com.onetime.app.data.MAX_SUBTASKS
import com.onetime.app.data.MIN_DURATION
import com.onetime.app.data.Subtask
import com.onetime.app.data.TodoItem
import com.onetime.app.data.formatClockTime
import com.onetime.app.data.formatDueDate
import com.onetime.app.data.formatStamp
import com.onetime.app.data.today
import com.onetime.app.data.startOfDayMillis
import com.onetime.app.ui.LiveTimer
import com.onetime.app.ui.chrome.IconGhost
import com.onetime.app.ui.chrome.RowIcon
import com.onetime.app.ui.chrome.SecondaryHeader
import com.onetime.app.ui.theme.InsetBox
import com.onetime.app.ui.theme.KeyCap
import com.onetime.app.ui.theme.Paper
import com.onetime.app.ui.theme.SoftCard
import java.time.LocalDate
import java.util.UUID

private data class TodoGroup(val title: String, val items: List<TodoItem>)

@Composable
fun TodoScreen(
    data: AppData,
    timer: LiveTimer,
    onBack: () -> Unit,
    onAdd: (String, Long) -> Unit,
    onAddGoal: (String, Long, List<Subtask>) -> Unit,
    onToggle: (String) -> Unit,
    onToggleSub: (String, String) -> Unit,
    onPlayTodo: (TodoItem) -> Unit,
    onPlaySub: (TodoItem, Subtask) -> Unit,
    onDelete: (String) -> Unit,
    onSetDue: (String, Long) -> Unit,
    onEditSplit: (TodoItem, List<Subtask>) -> Unit,
    onMoveSub: (String, Int, Int) -> Unit,
    onAddQuickDay: (Int) -> Unit,
) {
    val palette = Paper.current
    var input by rememberSaveable { mutableStateOf("") }
    var due by rememberSaveable { mutableLongStateOf(today().startOfDayMillis()) }
    var splitTarget by remember { mutableStateOf<TodoItem?>(null) }
    var showSplit by remember { mutableStateOf(false) }
    var editingDays by remember { mutableStateOf<Int?>(null) }

    val open = data.todos.count { !it.done }
    val groups = remember(data.todos) { groupTodos(data.todos) }

    fun submit() {
        val text = input.trim()
        if (text.isEmpty()) return
        onAdd(text, due)
        input = ""
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
    ) {
        item {
            SecondaryHeader(
                title = "待办",
                trailing = "$open 项待办",
                onBack = onBack,
                timerChip = timer,
            )
        }

        item {
            SoftCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InsetBox(
                            Modifier.weight(1f).heightIn(min = 42.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Box(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                                if (input.isEmpty()) {
                                    Text("添加一条待办…", fontSize = 14.sp, color = palette.muted)
                                }
                                BasicTextField(
                                    value = input,
                                    onValueChange = { if (it.length <= 120) input = it },
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 14.sp, color = palette.ink),
                                    cursorBrush = SolidColor(palette.coral),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { submit() }),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        KeyCap(
                            onClick = { submit() },
                            color = palette.goldWash,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 13.dp, vertical = 12.dp),
                        ) {
                            Text("发送", fontSize = 13.sp, color = palette.goldDeep, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.width(6.dp))
                        KeyCap(
                            onClick = {
                                if (input.isBlank()) {
                                    input = ""
                                } else {
                                    showSplit = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 13.dp, vertical = 12.dp),
                        ) {
                            Text("拆分", fontSize = 13.sp, color = palette.inkSoft, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("计划日期", fontSize = 11.5.sp, color = palette.muted)
                        Spacer(Modifier.width(6.dp))
                        QuickDateChip(
                            label = "今天",
                            selected = due == today().startOfDayMillis(),
                            onClick = { due = today().startOfDayMillis() },
                        )
                        Spacer(Modifier.width(4.dp))
                        data.settings.quickDays.filter { it > 0 }.forEach { days ->
                            QuickDateChip(
                                label = when (days) {
                                    0 -> "今天"
                                    1 -> "明天"
                                    else -> "$days 天后"
                                },
                                selected = due == today().plusDays(days.toLong()).startOfDayMillis(),
                                onClick = { due = today().plusDays(days.toLong()).startOfDayMillis() },
                                onLongClick = { if (days >= 3) editingDays = days },
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Text(formatDueDate(due), fontSize = 10.5.sp, color = palette.inkSoft)
                    }
                }
            }
        }

        if (groups.isEmpty()) {
            item { EmptyTodoBox() }
        } else {
            groups.forEach { group ->
                item {
                    Text(
                        group.title,
                        fontSize = 12.sp,
                        color = palette.muted,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                }
                items(group.items, key = { it.id }) { todo ->
                    TodoRow(
                        todo = todo,
                        onToggle = { onToggle(todo.id) },
                        onToggleSub = { subId -> onToggleSub(todo.id, subId) },
                        onPlay = { onPlayTodo(todo) },
                        onPlaySub = { sub -> onPlaySub(todo, sub) },
                        onDelete = { onDelete(todo.id) },
                        onEditSplit = { splitTarget = todo },
                        onMoveSub = { index, delta -> onMoveSub(todo.id, index, delta) },
                        timerActive = timer.active,
                    )
                }
            }
        }
    }

    editingDays?.let { days ->
        var text by remember(days) { mutableStateOf(days.toString()) }
        AlertDialog(
            onDismissRequest = { editingDays = null },
            containerColor = palette.card,
            title = { Text("自定义快捷天数", fontSize = 16.sp, color = palette.ink) },
            text = {
                Column {
                    OutlinedField(text, { v -> text = v.filter { c -> c.isDigit() }.take(2) }, "几天后（1–60）")
                    Spacer(Modifier.height(6.dp))
                    Text("会记住这个天数，下次直接出现在快捷项里", fontSize = 10.5.sp, color = palette.muted)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val d = (text.toIntOrNull() ?: days).coerceIn(1, 60)
                    onAddQuickDay(d)
                    due = today().plusDays(d.toLong()).startOfDayMillis()
                    editingDays = null
                }) { Text("保存", color = palette.coralDeep, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { editingDays = null }) { Text("取消", color = palette.muted) } },
        )
    }

    if (showSplit) {
        SplitDialog(
            initialTitle = input,
            initialSubtasks = emptyList(),
            onDismiss = { showSplit = false },
            onConfirm = { title, subs ->
                onAddGoal(title, due, subs)
                input = ""
                showSplit = false
            },
        )
    }

    splitTarget?.let { todo ->
        SplitDialog(
            initialTitle = todo.title,
            initialSubtasks = todo.subtasks,
            onDismiss = { splitTarget = null },
            onConfirm = { _, subs ->
                onEditSplit(todo, subs)
                splitTarget = null
            },
        )
    }
}

@Composable
private fun QuickDateChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val palette = Paper.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (selected) palette.goldWash else palette.inset)
            .border(1.dp, if (selected) palette.gold.copy(alpha = 0.6f) else palette.line, RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            label,
            fontSize = 10.5.sp,
            color = if (selected) palette.goldDeep else palette.inkSoft,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun EmptyTodoBox() {
    val palette = Paper.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.paperDeep.copy(alpha = 0.35f))
            .dashedBorder(if (palette.isNight) palette.muted.copy(alpha = 0.55f) else palette.line, 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.card)
                    .border(1.dp, palette.line, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = palette.gold,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("还没有待办，写下第一件要做的事吧", fontSize = 12.5.sp, color = palette.muted)
        }
    }
}

private fun Modifier.dashedBorder(color: androidx.compose.ui.graphics.Color, radius: androidx.compose.ui.unit.Dp): Modifier =
    this.drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx(), radius.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1.2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(9.dp.toPx(), 7.dp.toPx()),
                    0f,
                ),
            ),
        )
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodoRow(
    todo: TodoItem,
    onToggle: () -> Unit,
    onToggleSub: (String) -> Unit,
    onPlay: () -> Unit,
    onPlaySub: (Subtask) -> Unit,
    onDelete: () -> Unit,
    onEditSplit: () -> Unit,
    onMoveSub: (Int, Int) -> Unit,
    timerActive: Boolean,
) {
    val palette = Paper.current
    var expanded by remember { mutableStateOf(false) }

    SoftCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = 4.dp) {
        Column(Modifier.padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = todo.done,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = palette.gold,
                        uncheckedColor = palette.line,
                        checkmarkColor = palette.cardHigh,
                    ),
                    modifier = Modifier.size(26.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        todo.title,
                        fontSize = 14.sp,
                        color = if (todo.done) palette.muted else palette.ink,
                        textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatDueDate(todo.dueAt),
                            fontSize = 10.sp,
                            color = palette.muted,
                        )
                        Text(" · ", fontSize = 10.sp, color = palette.muted)
                        Text(
                            if (todo.hasSubtasks) {
                                "${todo.doneSubtasks}/${todo.subtasks.size} 子任务 · ${todo.totalMinutes} 分"
                            } else {
                                "创建于 ${formatClockTime(todo.createdAt)}"
                            },
                            fontSize = 10.sp,
                            color = palette.muted,
                        )

                    }
                }
                RowIcon(Icons.Filled.PlayArrow, "开始专注", onPlay, tint = palette.goldDeep, enabled = !timerActive)
                if (todo.hasSubtasks) {
                    RowIcon(Icons.Filled.ArrowForward, "编辑拆分", onEditSplit)
                }
                RowIcon(Icons.Filled.Delete, "删除", onDelete)
                if (todo.hasSubtasks) {
                    Box(
                        Modifier.size(24.dp).clickable { expanded = !expanded },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = "展开子任务",
                            tint = palette.muted,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }

            if (todo.done && todo.doneAt != null) {
                Row(Modifier.fillMaxWidth().padding(end = 4.dp, top = 2.dp)) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        "完成于 ${formatStamp(todo.doneAt)}",
                        fontSize = 9.5.sp,
                        color = palette.sageDeep,
                    )
                }
            }

            AnimatedVisibility(visible = expanded && todo.hasSubtasks) {
                Column(Modifier.padding(start = 30.dp, top = 2.dp)) {
                    todo.subtasks.forEachIndexed { index, sub ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${index + 1}", fontSize = 10.sp, color = palette.muted, modifier = Modifier.width(14.dp))
                            Checkbox(
                                checked = sub.done,
                                onCheckedChange = { onToggleSub(sub.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = palette.sage,
                                    uncheckedColor = palette.line,
                                    checkmarkColor = palette.cardHigh,
                                ),
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                sub.title,
                                fontSize = 12.5.sp,
                                color = if (sub.done) palette.muted else palette.inkSoft,
                                textDecoration = if (sub.done) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text("${sub.minutes} 分", fontSize = 10.sp, color = palette.goldDeep)
                            RowIcon(
                                Icons.Filled.PlayArrow,
                                "开始子任务",
                                { onPlaySub(sub) },
                                tint = palette.sageDeep,
                                enabled = !timerActive && !sub.done,
                            )
                            RowIcon(Icons.Filled.KeyboardArrowUp, "上移", { onMoveSub(index, -1) }, enabled = index > 0)
                            RowIcon(
                                Icons.Filled.KeyboardArrowDown,
                                "下移",
                                { onMoveSub(index, 1) },
                                enabled = index < todo.subtasks.size - 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitDialog(
    initialTitle: String,
    initialSubtasks: List<Subtask>,
    onDismiss: () -> Unit,
    onConfirm: (String, List<Subtask>) -> Unit,
) {
    val palette = Paper.current
    var title by remember { mutableStateOf(initialTitle) }
    var subInput by remember { mutableStateOf("") }
    var subMinutes by remember { mutableStateOf("25") }
    var subs by remember { mutableStateOf(initialSubtasks) }
    var editingIndex by remember { mutableStateOf(-1) }

    fun addSub() {
        val text = subInput.trim()
        if (text.isEmpty() || subs.size >= MAX_SUBTASKS) return
        val minutes = (subMinutes.toIntOrNull() ?: 25).coerceIn(MIN_DURATION, MAX_DURATION)
        subs = subs + Subtask(UUID.randomUUID().toString(), text, minutes)
        subInput = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        title = { Text("拆分目标", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = palette.ink) },
        text = {
            Column {
                OutlinedField(title, { title = it.take(120) }, "总目标")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedField(subInput, { subInput = it.take(120) }, "子目标", Modifier.weight(1f))
                    Spacer(Modifier.width(6.dp))
                    OutlinedField(subMinutes, { subMinutes = it.filter { c -> c.isDigit() }.take(3) }, "分钟", Modifier.width(64.dp))
                    Spacer(Modifier.width(6.dp))
                    KeyCap(
                        onClick = { addSub() },
                        color = palette.goldWash,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    ) { Text("添加", fontSize = 12.sp, color = palette.goldDeep) }
                }
                Spacer(Modifier.height(8.dp))
                if (subs.isNotEmpty()) {
                    Column(Modifier.heightIn(max = 200.dp)) {
                        subs.forEachIndexed { index, sub ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${index + 1}.", fontSize = 11.sp, color = palette.muted, modifier = Modifier.width(18.dp))
                                Text(
                                    sub.title,
                                    fontSize = 12.5.sp,
                                    color = palette.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${sub.minutes} 分",
                                    fontSize = 11.sp,
                                    color = palette.goldDeep,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { editingIndex = index }
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                )
                                RowIcon(Icons.Filled.KeyboardArrowUp, "上移", {
                                    if (index > 0) {
                                        val list = subs.toMutableList()
                                        val tmp = list[index]; list[index] = list[index - 1]; list[index - 1] = tmp
                                        subs = list
                                    }
                                }, enabled = index > 0)
                                RowIcon(Icons.Filled.KeyboardArrowDown, "下移", {
                                    if (index < subs.size - 1) {
                                        val list = subs.toMutableList()
                                        val tmp = list[index]; list[index] = list[index + 1]; list[index + 1] = tmp
                                        subs = list
                                    }
                                }, enabled = index < subs.size - 1)
                                RowIcon(Icons.Filled.Delete, "删除", { subs = subs.filterIndexed { i, _ -> i != index } })
                            }
                        }
                    }
                    Text(
                        "共 ${subs.size} 个子任务 · 预计 ${subs.sumOf { it.minutes }} 分钟",
                        fontSize = 11.sp,
                        color = palette.muted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title.trim().ifBlank { "拆分目标" }, subs) },
                enabled = subs.isNotEmpty(),
            ) { Text("保存", color = palette.coralDeep, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = palette.muted) } },
    )

    if (editingIndex >= 0 && editingIndex < subs.size) {
        var value by remember(editingIndex) { mutableStateOf(subs[editingIndex].minutes.toString()) }
        AlertDialog(
            onDismissRequest = { editingIndex = -1 },
            containerColor = palette.card,
            title = { Text("修改计划分钟", fontSize = 16.sp, color = palette.ink) },
            text = {
                OutlinedField(value, { value = it.filter { c -> c.isDigit() }.take(3) }, "分钟")
            },
            confirmButton = {
                TextButton(onClick = {
                    val m = (value.toIntOrNull() ?: 25).coerceIn(MIN_DURATION, MAX_DURATION)
                    subs = subs.mapIndexed { i, s -> if (i == editingIndex) s.copy(minutes = m) else s }
                    editingIndex = -1
                }) { Text("确定", color = palette.coralDeep) }
            },
            dismissButton = { TextButton(onClick = { editingIndex = -1 }) { Text("取消", color = palette.muted) } },
        )
    }
}

@Composable
private fun OutlinedField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val palette = Paper.current
    InsetBox(modifier.heightIn(min = 40.dp), shape = RoundedCornerShape(10.dp)) {
        Box(Modifier.padding(horizontal = 10.dp, vertical = 11.dp)) {
            if (value.isEmpty()) Text(label, fontSize = 12.5.sp, color = palette.muted)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = palette.ink),
                cursorBrush = SolidColor(palette.coral),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun groupTodos(todos: List<TodoItem>): List<TodoGroup> {
    val todayStart = today().startOfDayMillis()
    val tomorrowStart = today().plusDays(1).startOfDayMillis()
    val past = todos.filter { it.dueAt < todayStart }
        .sortedWith(compareBy<TodoItem> { it.done }.thenByDescending { it.dueAt })
    val todayItems = todos.filter { it.dueAt in todayStart until tomorrowStart }
        .sortedWith(compareBy<TodoItem> { it.done }.thenBy { it.createdAt })
    val future = todos.filter { it.dueAt >= tomorrowStart }
        .sortedWith(compareBy<TodoItem> { it.done }.thenBy { it.dueAt })
    return buildList {
        if (past.isNotEmpty()) add(TodoGroup("昨天", past))
        if (todayItems.isNotEmpty()) add(TodoGroup("今天", todayItems))
        if (future.isNotEmpty()) add(TodoGroup("未来", future))
    }
}
