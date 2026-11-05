package com.onetime.app.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onetime.app.R

/** 纸面背景：暖白 + 极细纸纤维纹理（ImageShader 平铺） */
@Composable
fun PaperBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val palette = Paper.current
    val context = LocalContext.current
    val bitmap = remember {
        android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.paper_tile).asImageBitmap()
    }
    val shader = remember(bitmap) { ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated) }
    Box(
        modifier = modifier
            .background(palette.paper)
            .drawBehind {
                drawRect(
                    brush = object : ShaderBrush() {
                        override fun createShader(size: Size) = shader
                    },
                    alpha = if (palette.isNight) 0.22f else 0.92f,
                )
                // 四周极轻晕影，纸张边缘受光更弱
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            palette.shadow.copy(alpha = if (palette.isNight) 0.10f else 0.05f),
                        ),
                        center = Offset(size.width / 2f, size.height * 0.42f),
                        radius = size.maxDimension * 0.78f,
                    ),
                )
            },
        content = content,
    )
}

/**
 * 纸面卡片：低层次白卡 + 极浅投影 + 发丝边。
 * elevation 越小越"平"，非交互卡片层次弱于按钮。
 */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    color: Color = Paper.current.card,
    elevation: Dp = 6.dp,
    borderColor: Color = Paper.current.line.copy(alpha = 0.75f),
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = Paper.current
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = palette.shadow.copy(alpha = 0.10f),
                spotColor = palette.shadow.copy(alpha = 0.16f),
            )
            .clip(shape)
            .background(color)
            .border(1.dp, borderColor, shape),
        content = content,
    )
}

/** 内凹容器：输入区、编辑区 */
@Composable
fun InsetBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    color: Color = Paper.current.inset,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = Paper.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(color)
            .border(1.dp, palette.line.copy(alpha = 0.85f), shape)
            .drawInnerEdge(palette)
    ) {
        content()
    }
}

/** 上缘内阴影 + 下缘内高光，做出"按下去"的凹陷感 */
private fun Modifier.drawInnerEdge(palette: PaperPalette): Modifier = this.then(
    Modifier.drawBehind {
        val r = 14.dp.toPx()
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(palette.shadow.copy(alpha = 0.10f), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.5f,
            ),
            cornerRadius = CornerRadius(r, r),
            size = Size(size.width, size.height),
        )
        drawLine(
            color = Color.White.copy(alpha = if (palette.isNight) 0.04f else 0.65f),
            start = Offset(r, size.height - 1f),
            end = Offset(size.width - r, size.height - 1f),
            strokeWidth = 1f,
        )
    },
)

/**
 * 凸起键帽：上缘细高光、下缘短柔影，按下时降低凸起。
 */
@Composable
fun KeyCap(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Paper.current.card,
    contentColor: Color = Paper.current.ink,
    shape: Shape = RoundedCornerShape(14.dp),
    elevation: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val palette = Paper.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift by animateDpAsState(
        targetValue = if (enabled && !pressed) elevation else 0.6.dp,
        label = "keycap-lift",
    )
    val face = if (enabled) color else color.copy(alpha = 0.55f)

    Row(
        modifier = modifier
            .shadow(
                elevation = lift,
                shape = shape,
                ambientColor = palette.shadow.copy(alpha = 0.12f),
                spotColor = palette.shadow.copy(alpha = 0.22f),
            )
            .clip(shape)
            .background(face)
            .border(
                width = 1.dp,
                color = if (enabled) Color.White.copy(alpha = if (palette.isNight) 0.06f else 0.9f)
                else palette.line.copy(alpha = 0.5f),
                shape = shape,
            )
            .topHighlight(palette)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

private fun Modifier.topHighlight(palette: PaperPalette): Modifier = this.then(
    Modifier.drawBehind {
        drawLine(
            color = Color.White.copy(alpha = if (palette.isNight) 0.05f else 0.75f),
            start = Offset(6.dp.toPx(), 1.2f),
            end = Offset(size.width - 6.dp.toPx(), 1.2f),
            strokeWidth = 1.4f,
        )
        drawLine(
            color = palette.shadow.copy(alpha = 0.08f),
            start = Offset(6.dp.toPx(), size.height - 1f),
            end = Offset(size.width - 6.dp.toPx(), size.height - 1f),
            strokeWidth = 1f,
        )
    },
)

/** 金币图标：金圆 + 内环 + 四角星 */
@Composable
fun CoinIcon(size: Dp = 16.dp, modifier: Modifier = Modifier) {
    val palette = Paper.current
    androidx.compose.foundation.Canvas(modifier = modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(color = palette.gold.copy(alpha = 0.25f), radius = r, center = c)
        drawCircle(color = palette.gold, radius = r * 0.82f, center = c)
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = r * 0.6f,
            center = c,
            style = Stroke(width = r * 0.12f),
        )
        val star = Path().apply {
            val outer = r * 0.42f
            val inner = r * 0.12f
            moveTo(c.x, c.y - outer)
            lineTo(c.x + inner, c.y - inner)
            lineTo(c.x + outer, c.y)
            lineTo(c.x + inner, c.y + inner)
            lineTo(c.x, c.y + outer)
            lineTo(c.x - inner, c.y + inner)
            lineTo(c.x - outer, c.y)
            lineTo(c.x - inner, c.y - inner)
            close()
        }
        drawPath(star, Color.White.copy(alpha = 0.95f))
    }
}

/** 白色圆角方块图标按钮（品牌入口、返回） */
@Composable
fun SquircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    corner: Dp = 15.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = Paper.current
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(corner),
                ambientColor = palette.shadow.copy(alpha = 0.12f),
                spotColor = palette.shadow.copy(alpha = 0.2f),
            )
            .clip(RoundedCornerShape(corner))
            .background(palette.cardHigh)
            .border(1.dp, Color.White.copy(alpha = if (palette.isNight) 0.05f else 0.9f), RoundedCornerShape(corner))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun HairlineDivider(modifier: Modifier = Modifier, color: Color = Paper.current.line.copy(alpha = 0.9f)) {
    Box(
        modifier = modifier
            .size(width = 0.dp, height = 1.dp)
            .background(color),
    )
}

/** 小标签：金色 / 灰 */
@Composable
fun SoftLabel(
    text: String,
    color: Color = Paper.current.inkSoft,
    background: Color = Color.Transparent,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelMedium,
    bold: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

