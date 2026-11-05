package com.onetime.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetime.app.data.AppData
import com.onetime.app.data.RangeStats
import com.onetime.app.data.ReviewRange
import com.onetime.app.data.Session
import com.onetime.app.data.TodoItem
import com.onetime.app.data.coinText
import com.onetime.app.data.completedTodosIn
import com.onetime.app.data.dailyFocus
import com.onetime.app.data.formatDuration
import com.onetime.app.data.formatStamp
import com.onetime.app.data.recordsIn
import com.onetime.app.data.statsIn
import com.onetime.app.ui.LiveTimer
import com.onetime.app.ui.chrome.SecondaryHeader
import com.onetime.app.ui.theme.CoinIcon
import com.onetime.app.ui.theme.Paper
import com.onetime.app.ui.theme.SoftCard
import java.time.LocalDate

@Composable
fun ReviewScreen(
    data: AppData,
    timer: LiveTimer,
    onBack: () -> Unit,
) {
    val palette = Paper.current
    var rangeIndex by rememberSaveable { mutableStateOf(0) }
    val range = ReviewRange.entries[rangeIndex]
    val stats = remember(data, rangeIndex) { data.statsIn(range) }
    val daily = remember(data, rangeIndex) { data.dailyFocus(range) }
    val doneTodos = remember(data, rangeIndex) { data.completedTodosIn(range) }
    val records = remember(data, rangeIndex) { data.recordsIn(range) }
    val avg = if (daily.isEmpty()) 0 else daily.sumOf { it.second } / daily.size

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
    ) {
        item {
            SecondaryHeader(
                title = "复盘",
                trailing = "${stats.completedCount} 完成",
                onBack = onBack,
                timerChip = timer,
            )
        }

        item { RangeTabs(rangeIndex) { rangeIndex = it } }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("专注时长", "${stats.focusMinutes} 分钟", Modifier.weight(1f))
                MetricCard("完成", "${stats.completedCount}", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("中断", "${stats.interruptedCount}", Modifier.weight(1f))
                CoinMetricCard(stats, Modifier.weight(1f))
            }
        }

        item {
            SoftCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("每日专注", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.ink)
                        Spacer(Modifier.weight(1f))
                        Text("日均专注 $avg 分钟", fontSize = 11.sp, color = palette.muted)
                    }
                    Spacer(Modifier.height(12.dp))
                    FocusChart(daily)
                }
            }
        }

        item {
            SoftCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("完成待办", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.ink)
                        Spacer(Modifier.weight(1f))
                        Text("${doneTodos.size} 项待办", fontSize = 11.sp, color = palette.muted)
                    }
                    Spacer(Modifier.height(10.dp))
                    if (doneTodos.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                            Text("这个区间还没有完成的待办", fontSize = 12.sp, color = palette.muted)
                        }
                    } else {
                        doneTodos.take(8).forEach { CompletedTodoRow(it) }
                    }
                }
            }
        }

        item {
            SoftCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("完成记录", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.ink)
                    Spacer(Modifier.height(8.dp))
                    if (records.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
                            Text("这个区间还没有专注记录", fontSize = 12.sp, color = palette.muted)
                        }
                    } else {
                        records.take(20).forEach { RecordRow(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeTabs(selected: Int, onSelect: (Int) -> Unit) {
    val palette = Paper.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.card)
            .border(1.dp, palette.line, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ReviewRange.entries.forEachIndexed { index, r ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) palette.cardHigh else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    r.label,
                    fontSize = 12.5.sp,
                    color = if (isSelected) palette.ink else palette.muted,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    val palette = Paper.current
    SoftCard(modifier, shape = RoundedCornerShape(16.dp), elevation = 3.dp) {
        Column(Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.5.sp, color = palette.muted)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = palette.ink)
        }
    }
}

@Composable
private fun CoinMetricCard(stats: RangeStats, modifier: Modifier) {
    val palette = Paper.current
    SoftCard(modifier, shape = RoundedCornerShape(16.dp), elevation = 3.dp) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(size = 11.dp)
                Spacer(Modifier.width(4.dp))
                Text("金币", fontSize = 11.5.sp, color = palette.muted)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("+${stats.coinsEarned.coinText()}", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = palette.goldDeep)
                Spacer(Modifier.width(6.dp))
                Text("-${stats.coinsSpent.coinText()}", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = palette.coralDeep)
            }
        }
    }
}

@Composable
private fun FocusChart(points: List<Pair<LocalDate, Int>>) {
    val palette = Paper.current
    val maxValue = (points.maxOfOrNull { it.second } ?: 0).coerceAtLeast(10)
    val ticks = listOf(maxValue, (maxValue * 2) / 3, maxValue / 3, 0)

    Column {
        Row {
            Column(
                modifier = Modifier.height(120.dp).width(32.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                ticks.forEach {
                    Text("${it}m", fontSize = 9.5.sp, color = palette.muted)
                }
            }
            Spacer(Modifier.width(6.dp))
            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                val w = size.width
                val h = size.height
                // 虚线网格
                ticks.forEachIndexed { i, _ ->
                    val y = h * i / (ticks.size - 1)
                    var x = 0f
                    while (x < w) {
                        drawLine(
                            palette.line.copy(alpha = 0.9f),
                            Offset(x, y),
                            Offset((x + 4.dp.toPx()).coerceAtMost(w), y),
                            strokeWidth = 1f,
                        )
                        x += 9.dp.toPx()
                    }
                }
                if (points.isNotEmpty()) {
                    val stepX = if (points.size <= 1) 0f else w / (points.size - 1)
                    val path = Path()
                    points.forEachIndexed { i, (_, v) ->
                        val x = if (points.size <= 1) w / 2 else stepX * i
                        val y = h - (v.toFloat() / maxValue) * h
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path,
                        palette.coral.copy(alpha = 0.55f),
                        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
                    )
                    points.forEachIndexed { i, (_, v) ->
                        val x = if (points.size <= 1) w / 2 else stepX * i
                        val y = h - (v.toFloat() / maxValue) * h
                        drawCircle(palette.coral, radius = 2.4.dp.toPx(), center = Offset(x, y))
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().padding(start = 38.dp), horizontalArrangement = Arrangement.Center) {
            Text(
                points.lastOrNull()?.first?.let { "%02d/%02d".format(it.monthValue, it.dayOfMonth) } ?: "",
                fontSize = 9.5.sp,
                color = palette.muted,
            )
        }
    }
}

@Composable
private fun CompletedTodoRow(todo: TodoItem) {
    val palette = Paper.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✓", fontSize = 11.sp, color = palette.sageDeep)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(todo.title, fontSize = 12.5.sp, color = palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (todo.hasSubtasks) {
                Text(
                    "子任务 ${todo.doneSubtasks}/${todo.subtasks.size}",
                    fontSize = 10.sp,
                    color = palette.muted,
                )
            }
        }
        todo.doneAt?.let {
            Text(formatStamp(it), fontSize = 9.5.sp, color = palette.muted)
        }
    }
}

@Composable
private fun RecordRow(session: Session) {
    val palette = Paper.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (session.completed) palette.sage else palette.coral),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                session.taskName,
                fontSize = 12.5.sp,
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row {
                Text(formatStamp(session.endAt), fontSize = 9.5.sp, color = palette.muted)
                Text(" · ", fontSize = 9.5.sp, color = palette.muted)
                Text(formatDuration(session.minutes * 60_000L), fontSize = 9.5.sp, color = palette.muted)
                if (session.reason.isNotBlank()) {
                    Text(" · ${session.reason}", fontSize = 9.5.sp, color = palette.muted, maxLines = 1)
                }
            }
        }
        Text(
            if (session.coins > 0) "+${session.coins.coinText()}" else "无金币",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (session.coins > 0) palette.goldDeep else palette.muted,
        )
    }
}
