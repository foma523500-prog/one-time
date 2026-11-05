package com.onetime.app.ui.chrome

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetime.app.data.AppData
import com.onetime.app.data.coinText
import com.onetime.app.data.formatDuration
import com.onetime.app.data.formatTopBar
import com.onetime.app.ui.LiveTimer
import com.onetime.app.ui.theme.CoinIcon
import com.onetime.app.ui.theme.KeyCap
import com.onetime.app.ui.theme.Paper
import com.onetime.app.ui.theme.SoftCard
import com.onetime.app.ui.theme.SquircleButton

/** 品牌图标：迷你表盘 + 当日数字 + 金点（对应基准图左上角） */
@Composable
fun BrandMark(size: androidx.compose.ui.unit.Dp = 20.dp, color: Color = Paper.current.ink) {
    val palette = Paper.current
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size * 1.55f)) {
            val r = this.size.minDimension / 2f
            drawCircle(color.copy(alpha = 0.16f), radius = r * 0.94f, style = Stroke(width = r * 0.10f))
            drawCircle(color.copy(alpha = 0.10f), radius = r * 0.74f, style = Stroke(width = r * 0.05f))
        }
        Text(
            text = "1",
            color = color,
            fontSize = (size.value * 0.62f).sp,
            fontWeight = FontWeight.SemiBold,
        )
        Canvas(Modifier.size(size * 1.55f)) {
            val r = this.size.minDimension / 2f
            drawCircle(
                palette.gold,
                radius = r * 0.13f,
                center = Offset(this.size.width * 0.80f, this.size.height * 0.78f),
            )
        }
    }
}

/** 顶部全局栏 */
@Composable
fun TopBar(
    data: AppData,
    now: Long,
    onBrandClick: () -> Unit,
    onTimeClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onCoinsClick: () -> Unit,
) {
    val palette = Paper.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquircleButton(onClick = onBrandClick, size = 40.dp, corner = 13.dp) {
            BrandMark(size = 22.dp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "ONE TIME",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = palette.ink,
                maxLines = 1,
            )
            Text(
                text = formatTopBar(now, data.settings.showSeconds),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = palette.muted,
                modifier = Modifier.clickable(onClick = onTimeClick),
            )
        }
        Box(Modifier.clickable(onClick = onCoinsClick)) { CoinPill(coins = data.coins) }
        Spacer(Modifier.width(8.dp))
        IconGhost(
            icon = if (data.settings.night) Icons.Filled.Star else Icons.Filled.Star,
            description = "切换主题",
            onClick = onThemeToggle,
        )

    }
}

@Composable
fun CoinPill(coins: Double, modifier: Modifier = Modifier) {
    val palette = Paper.current
    Row(
        modifier = modifier
            .height(30.dp)
            .background(palette.goldWash, CircleShape)
            .border(1.dp, palette.gold.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        CoinIcon(size = 14.dp)
        Text(
            text = coins.coinText(),
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
            color = palette.goldDeep,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun IconGhost(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = Paper.current.inkSoft,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(19.dp))
    }
}

/** 二级页头部：返回 + 页名 + 右侧数量/状态 */
@Composable
fun SecondaryHeader(
    title: String,
    trailing: String? = null,
    onBack: () -> Unit,
    timerChip: LiveTimer? = null,
    extra: (@Composable RowScope.() -> Unit)? = null,
) {
    val palette = Paper.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquircleButton(onClick = onBack, size = 38.dp, corner = 12.dp) {
            Icon(Icons.Filled.KeyboardArrowLeft, "返回", tint = palette.ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            fontSize = 19.sp,
            color = palette.ink,
        )
        Spacer(Modifier.weight(1f))
        if (timerChip != null && timerChip.active) {
            TimerChip(timerChip)
            Spacer(Modifier.width(8.dp))
        }
        if (extra != null) extra()
        if (trailing != null) {
            Text(
                trailing,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = palette.muted,
            )
        }
    }
}

/** 计时进行中状态胶囊 */
@Composable
fun TimerChip(timer: LiveTimer, onClick: (() -> Unit)? = null) {
    val palette = Paper.current
    val text = when {
        timer.paused -> "已暂停 · ${formatDuration(timer.graceRemainingMs)}"
        timer.isCountDown -> formatDuration(timer.remainingMs)
        else -> formatDuration(timer.elapsedMs)
    }
    Row(
        modifier = Modifier
            .height(26.dp)
            .background(palette.coralWash, CircleShape)
            .padding(horizontal = 9.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Canvas(Modifier.size(7.dp)) {
            drawCircle(if (timer.paused) palette.sage else palette.coral)
        }
        Text(
            text,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = if (timer.paused) palette.sageDeep else palette.coralDeep,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

enum class DockTab(val label: String, val icon: ImageVector) {
    POMODORO("番茄", Icons.Filled.PlayArrow),
    TODO("待办", Icons.Filled.CheckCircle),
    REVIEW("复盘", Icons.Filled.DateRange),
    NOTE("随记", Icons.Filled.Create),
}

/** 底部四键 Dock */
@Composable
fun Dock(
    selected: DockTab?,
    onSelect: (DockTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Paper.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DockTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val shape = RoundedCornerShape(16.dp)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(shape)
                    .background(if (isSelected) palette.goldWash else palette.card)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) palette.gold.copy(alpha = 0.55f) else palette.line,
                        shape = shape,
                    )
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    tab.icon,
                    contentDescription = tab.label,
                    tint = if (isSelected) palette.goldDeep else palette.inkSoft,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    tab.label,
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = if (isSelected) palette.goldDeep else palette.ink,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 收起后的迷你 Dock */
@Composable
fun MiniDock(
    timer: LiveTimer,
    coins: Double,
    onExpand: () -> Unit,
    onToggleTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Paper.current
    val progress by animateFloatAsState(
        targetValue = if (timer.isCountDown) {
            val total = (timer.plannedMinutes ?: 1) * 60_000f
            1f - (timer.remainingMs / total).coerceIn(0f, 1f)
        } else {
            (timer.elapsedMs % 3_600_000L) / 3_600_000f
        },
        animationSpec = tween(400),
        label = "mini-progress",
    )
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniRing(progress = progress, running = timer.running, onClick = onExpand)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).clickable(onClick = onExpand)) {
                Text(
                    text = timer.label.ifBlank { "正计时" },
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when {
                        !timer.active -> "未开始"
                        timer.paused -> "已暂停"
                        timer.isCountDown -> "剩余 ${formatDuration(timer.remainingMs)}"
                        else -> "已专注 ${formatDuration(timer.elapsedMs)}"
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
            }
            CoinPill(coins = coins)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(if (timer.paused) palette.sage else palette.coral, CircleShape)
                    .clickable(onClick = onToggleTimer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (timer.running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (timer.running) "暂停" else "开始",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun MiniRing(progress: Float, running: Boolean, onClick: () -> Unit) {
    val palette = Paper.current
    Box(Modifier.size(34.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(34.dp)) {
            val stroke = 3.dp.toPx()
            drawCircle(palette.line, radius = size.minDimension / 2f - stroke, style = Stroke(stroke))
            drawArc(
                color = if (running) palette.coral else palette.sage,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
    }
}

/** 通用小图标按钮（行内操作） */
@Composable
fun RowIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = Paper.current.inkSoft,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier.size(30.dp).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
fun CalendarGlyph(tint: Color = Paper.current.inkSoft, size: androidx.compose.ui.unit.Dp = 16.dp) {
    Icon(Icons.Filled.DateRange, contentDescription = null, tint = tint, modifier = Modifier.size(size))
}

/** 占位：让 Row 内的填充高度可用 */
@Composable
fun RowSpacer(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxHeight().width(0.dp))
}
