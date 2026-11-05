package com.onetime.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetime.app.data.AppData
import com.onetime.app.data.MAX_NOTE_LENGTH
import com.onetime.app.data.NoteItem
import com.onetime.app.data.formatStamp
import com.onetime.app.data.today
import com.onetime.app.data.startOfDayMillis
import com.onetime.app.ui.LiveTimer
import com.onetime.app.ui.chrome.SecondaryHeader
import com.onetime.app.ui.theme.InsetBox
import com.onetime.app.ui.theme.KeyCap
import com.onetime.app.ui.theme.Paper
import com.onetime.app.ui.theme.SoftCard

private enum class NoteMode { CARD, BULLET }

@Composable
fun NoteScreen(
    data: AppData,
    timer: LiveTimer,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onUpdate: (String, String) -> Unit,
) {
    val palette = Paper.current
    var draft by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(true) }
    var rangeIndex by rememberSaveable { mutableIntStateOf(1) }
    var mode by rememberSaveable { mutableStateOf(NoteMode.CARD) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var editingText by remember { mutableStateOf("") }

    val filtered = remember(data.notes, rangeIndex) { filterNotes(data, rangeIndex) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
    ) {
        item {
            SecondaryHeader(
                title = "随记",
                trailing = "${data.notes.size} 条",
                onBack = onBack,
                timerChip = timer,
            )
        }

        item {
            SoftCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(14.dp)) {
                    InsetBox(
                        Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Box(Modifier.padding(12.dp)) {
                            if (draft.isEmpty()) {
                                Text("记下此刻想法……", fontSize = 14.sp, color = palette.muted)
                            }
                            BasicTextField(
                                value = draft,
                                onValueChange = { if (it.length <= MAX_NOTE_LENGTH) draft = it },
                                textStyle = TextStyle(fontSize = 14.sp, color = palette.ink, lineHeight = 21.sp),
                                cursorBrush = SolidColor(palette.coral),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${draft.length} / $MAX_NOTE_LENGTH",
                            fontSize = 11.sp,
                            color = palette.muted,
                        )
                        Spacer(Modifier.weight(1f))
                        KeyCap(
                            onClick = {
                                if (draft.isNotBlank()) {
                                    onAdd(draft)
                                    draft = ""
                                }
                            },
                            enabled = draft.isNotBlank(),
                            color = palette.goldWash,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 11.dp),
                        ) {
                            Text("保存随记", fontSize = 13.sp, color = palette.goldDeep, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item {
            SoftCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(palette.goldWash),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Create,
                            contentDescription = null,
                            tint = palette.goldDeep,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text("随记本", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.ink)
                        Text(if (expanded) "点击收起" else "点击展开", fontSize = 10.5.sp, color = palette.muted)
                    }
                    Text("${filtered.size} 条", fontSize = 11.sp, color = palette.muted)
                    Icon(
                        if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = palette.muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (expanded) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("本周", "本月", "今年").forEachIndexed { index, label ->
                        FilterPill(label, rangeIndex == index) { rangeIndex = index }
                    }
                    Spacer(Modifier.weight(1f))
                    FilterPill(if (mode == NoteMode.CARD) "卡片模式" else "弹幕模式", true) {
                        mode = if (mode == NoteMode.CARD) NoteMode.BULLET else NoteMode.CARD
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Text("这个范围还没有随记", fontSize = 12.sp, color = palette.muted)
                    }
                }
            } else if (mode == NoteMode.CARD) {
                items(filtered, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        editing = editingId == note.id,
                        editingText = editingText,
                        onEditingText = { editingText = it },
                        onStartEdit = {
                            editingId = note.id
                            editingText = note.text
                        },
                        onCancel = { editingId = null },
                        onSave = {
                            onUpdate(note.id, editingText)
                            editingId = null
                        },
                    )
                }
            } else {
                item { BulletMode(filtered) }
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = Paper.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) palette.goldWash else palette.card)
            .border(1.dp, if (selected) palette.gold.copy(alpha = 0.55f) else palette.line, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            label,
            fontSize = 11.sp,
            color = if (selected) palette.goldDeep else palette.inkSoft,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun NoteCard(
    note: NoteItem,
    editing: Boolean,
    editingText: String,
    onEditingText: (String) -> Unit,
    onStartEdit: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val palette = Paper.current
    SoftCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = 3.dp) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("“", fontSize = 20.sp, color = palette.coral.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (!editing) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "编辑",
                        tint = palette.muted,
                        modifier = Modifier.size(15.dp).clickable(onClick = onStartEdit),
                    )
                }
            }
            if (editing) {
                InsetBox(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Box(Modifier.padding(10.dp)) {
                        BasicTextField(
                            value = editingText,
                            onValueChange = { if (it.length <= MAX_NOTE_LENGTH) onEditingText(it) },
                            textStyle = TextStyle(fontSize = 13.5.sp, color = palette.ink, lineHeight = 20.sp),
                            cursorBrush = SolidColor(palette.coral),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onCancel) { Text("取消", color = palette.muted, fontSize = 12.sp) }
                    TextButton(onClick = onSave) { Text("保存修改", color = palette.coralDeep, fontSize = 12.sp) }
                }
            } else {
                Text(
                    note.text,
                    fontSize = 14.sp,
                    color = palette.ink,
                    lineHeight = 21.sp,
                )
                if (note.editedAt != null) {
                    Text("已编辑", fontSize = 9.5.sp, color = palette.muted)
                }
                if (!note.taskName.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        note.taskName,
                        fontSize = 12.sp,
                        color = palette.coralDeep,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(formatStamp(note.createdAt), fontSize = 10.sp, color = palette.muted)
            }
        }
    }
}

@Composable
private fun BulletMode(notes: List<NoteItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Paper.current.paperDeep.copy(alpha = 0.4f))
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        notes.take(7).forEachIndexed { index, note ->
            BulletLane(note, index)
        }
    }
}

@Composable
private fun BulletLane(note: NoteItem, index: Int) {
    val palette = Paper.current
    val transition = rememberInfiniteTransition(label = "bullet-$index")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000 + index * 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bullet-x-$index",
    )
    BoxWithConstraints(Modifier.fillMaxWidth().height(30.dp)) {
        val widthPx = constraints.maxWidth.toFloat()
        Box(
            modifier = Modifier.offset { IntOffset((widthPx - progress * (widthPx + 900f)).toInt(), 0) },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(palette.coral.copy(alpha = 0.6f)),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    note.text.replace("\n", " "),
                    fontSize = 12.sp,
                    color = palette.inkSoft,
                    maxLines = 1,
                    modifier = Modifier.widthIn(max = 420.dp),
                )
            }
        }
    }
}

private fun filterNotes(data: AppData, rangeIndex: Int): List<NoteItem> {
    val t = today()
    val from = when (rangeIndex) {
        0 -> t.minusDays((t.dayOfWeek.value - 1).toLong())
        1 -> t.withDayOfMonth(1)
        else -> t.withDayOfYear(1)
    }.startOfDayMillis()
    return data.notes.filter { it.createdAt >= from }.sortedByDescending { it.createdAt }
}
