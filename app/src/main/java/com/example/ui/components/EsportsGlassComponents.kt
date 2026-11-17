package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SpellViolet
import com.example.ui.theme.VictoryEmerald
import com.example.ui.theme.WarGold
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ObsidianGlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNight)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Top-right ambient cyan esports aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CyberCyan.copy(alpha = 0.16f),
                        CyberCyan.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.08f),
                    radius = size.maxDimension * 0.55f
                ),
                radius = size.maxDimension * 0.55f,
                center = Offset(size.width * 0.88f, size.height * 0.08f)
            )
            // Mid-left molten gold ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        WarGold.copy(alpha = 0.12f),
                        WarGold.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.1f, size.height * 0.45f),
                    radius = size.maxDimension * 0.48f
                ),
                radius = size.maxDimension * 0.48f,
                center = Offset(size.width * 0.1f, size.height * 0.45f)
            )
            // Bottom-right violet spell ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SpellViolet.copy(alpha = 0.11f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.8f, size.height * 0.88f),
                    radius = size.maxDimension * 0.45f
                ),
                radius = size.maxDimension * 0.45f,
                center = Offset(size.width * 0.8f, size.height * 0.88f)
            )
        }
        content()
    }
}

@Composable
fun EsportsGlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    accentColor: Color = CyberCyan,
    surfaceAlpha: Float = 0.68f,
    content: @Composable BoxScope.() -> Unit
) {
    val rimBrush = Brush.linearGradient(
        colors = listOf(
            accentColor.copy(alpha = 0.52f),
            Color.White.copy(alpha = 0.14f),
            accentColor.copy(alpha = 0.18f)
        )
    )
    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF151E32).copy(alpha = surfaceAlpha),
            Color(0xFF0B1120).copy(alpha = (surfaceAlpha + 0.12f).coerceAtMost(0.95f))
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(width = 1.dp, brush = rimBrush, shape = shape),
        content = content
    )
}

@Composable
fun EfficiencyArcGauge(
    score: Int,
    predictedStars: Int,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp
) {
    val animatedFraction by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "efficiency_arc"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 10.dp.toPx()
            val inset = strokePx / 2f + 4.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val startAngle = 145f
            val totalSweep = 250f

            // Track arc
            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active glowing gradient arc
            val sweep = totalSweep * animatedFraction
            if (sweep > 0.5f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(CyberCyan, VictoryEmerald, WarGold, CyberCyan)
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Glowing tip dot
                val tipRad = Math.toRadians((startAngle + sweep).toDouble())
                val rx = arcSize.width / 2f
                val ry = arcSize.height / 2f
                val cx = topLeft.x + rx
                val cy = topLeft.y + ry
                val tipOffset = Offset(
                    x = cx + rx * cos(tipRad).toFloat(),
                    y = cy + ry * sin(tipRad).toFloat()
                )
                drawCircle(
                    color = WarGold.copy(alpha = 0.35f),
                    radius = 10.dp.toPx(),
                    center = tipOffset
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = tipOffset
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score%",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "EFISIENSI",
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                repeat(3) { idx ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (idx < predictedStars) WarGold else Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MiniEfficiencyRing(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val fraction = (score.coerceIn(0, 100) / 100f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(CyberCyan, VictoryEmerald, WarGold, CyberCyan)),
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$score%",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GlassStatusPill(
    label: String,
    accentColor: Color,
    isPulsingDot: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(ObsidianNight.copy(alpha = 0.72f))
            .border(1.dp, accentColor.copy(alpha = 0.65f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        if (isPulsingDot) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accentColor
        )
    }
}
