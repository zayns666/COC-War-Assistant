package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.model.AttackStep
import com.example.model.BaseWeakness
import com.example.model.ManualMarker
import com.example.model.MarkerType
import com.example.model.NormalizedPoint
import com.example.model.TacticalAnalysisResult
import com.example.model.WeaknessSeverity
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SpellViolet
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.VictoryEmerald
import com.example.ui.theme.WarGold
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TacticalMapCanvas(
    analysisResult: TacticalAnalysisResult?,
    activeStepIndex: Int,
    isSimulating: Boolean,
    showAiPaths: Boolean,
    showWeaknessHeatmap: Boolean,
    manualMarkers: List<ManualMarker>,
    interactiveMarkingEnabled: Boolean,
    selectedTool: MarkerType,
    onTapPlaceMarker: (Float, Float) -> Unit,
    onDrawPathComplete: (List<NormalizedPoint>) -> Unit,
    modifier: Modifier = Modifier,
    drawGridOverlay: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tactical_sim")
    val simProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "path_progress"
    )
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "weakness_pulse"
    )

    val textMeasurer = rememberTextMeasurer()
    val liveDragPoints = remember { mutableStateListOf<NormalizedPoint>() }

    val gestureModifier = if (interactiveMarkingEnabled) {
        if (selectedTool == MarkerType.PATH_ARROW) {
            Modifier.pointerInput(selectedTool) {
                detectDragGestures(
                    onDragStart = { offset ->
                        liveDragPoints.clear()
                        liveDragPoints.add(
                            NormalizedPoint(
                                (offset.x / size.width.coerceAtLeast(1)).coerceIn(0.02f, 0.98f),
                                (offset.y / size.height.coerceAtLeast(1)).coerceIn(0.02f, 0.98f)
                            )
                        )
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val pt = NormalizedPoint(
                            (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0.02f, 0.98f),
                            (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0.02f, 0.98f)
                        )
                        val last = liveDragPoints.lastOrNull()
                        if (last == null || kotlin.math.hypot(pt.x - last.x, pt.y - last.y) > 0.015f) {
                            liveDragPoints.add(pt)
                        }
                    },
                    onDragEnd = {
                        if (liveDragPoints.size >= 2) {
                            onDrawPathComplete(liveDragPoints.toList())
                        }
                        liveDragPoints.clear()
                    },
                    onDragCancel = {
                        liveDragPoints.clear()
                    }
                )
            }
        } else {
            Modifier.pointerInput(selectedTool) {
                detectTapGestures { offset ->
                    val xNorm = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0.02f, 0.98f)
                    val yNorm = (offset.y / size.height.coerceAtLeast(1)).coerceIn(0.02f, 0.98f)
                    onTapPlaceMarker(xNorm, yNorm)
                }
            }
        }
    } else {
        Modifier
    }

    Box(modifier = modifier.then(gestureModifier)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (drawGridOverlay) {
                drawTacticalReticleGrid()
            }

            if (analysisResult != null) {
                if (showWeaknessHeatmap) {
                    analysisResult.weaknesses.forEach { weakness ->
                        drawWeaknessZone(weakness, pulseFraction, textMeasurer)
                    }
                }

                if (showAiPaths && analysisResult.steps.isNotEmpty()) {
                    // Draw inactive steps faintly for context
                    analysisResult.steps.forEachIndexed { idx, step ->
                        if (idx != activeStepIndex) {
                            drawAttackStep(
                                step = step,
                                isActive = false,
                                progress = 0f,
                                textMeasurer = textMeasurer
                            )
                        }
                    }
                    // Draw active step prominently with animated troop simulation
                    val activeStep = analysisResult.steps.getOrNull(activeStepIndex)
                    if (activeStep != null) {
                        drawAttackStep(
                            step = activeStep,
                            isActive = true,
                            progress = if (isSimulating) simProgress else 0.5f,
                            textMeasurer = textMeasurer
                        )
                    }
                }
            }

            // Draw user real-time tactical markers
            manualMarkers.forEachIndexed { idx, marker ->
                drawManualMarker(marker, idx + 1, pulseFraction, textMeasurer)
            }

            // Draw live drag stroke preview
            if (liveDragPoints.size >= 2) {
                drawNormalizedArrowPath(
                    points = liveDragPoints,
                    color = WarGold,
                    strokeWidth = 6f,
                    dashed = false,
                    progress = null
                )
            }
        }
    }
}

private fun DrawScope.drawTacticalReticleGrid() {
    val gridColor = CyberCyan.copy(alpha = 0.08f)
    val cols = 6
    val rows = 4
    for (c in 1 until cols) {
        val x = size.width * c / cols
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
    }
    for (r in 1 until rows) {
        val y = size.height * r / rows
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
    }
}

private fun DrawScope.drawWeaknessZone(
    weakness: BaseWeakness,
    pulse: Float,
    textMeasurer: TextMeasurer
) {
    val cx = weakness.xNorm * size.width
    val cy = weakness.yNorm * size.height
    val radiusPx = weakness.radiusNorm * minOf(size.width, size.height)

    val zoneColor = when (weakness.severity) {
        WeaknessSeverity.HIGH -> ThreatCrimson
        WeaknessSeverity.MEDIUM -> WarGold
        WeaknessSeverity.LOW -> VictoryEmerald
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                zoneColor.copy(alpha = 0.36f * pulse),
                zoneColor.copy(alpha = 0.14f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = radiusPx * 1.25f
        ),
        radius = radiusPx * 1.25f,
        center = Offset(cx, cy)
    )

    drawCircle(
        color = zoneColor.copy(alpha = 0.85f),
        radius = radiusPx * (0.78f + 0.12f * pulse),
        center = Offset(cx, cy),
        style = Stroke(
            width = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )
    )

    drawBadgeLabel(
        text = weakness.label.take(18),
        center = Offset(cx, (cy - radiusPx * 0.85f).coerceAtLeast(24f)),
        bgColor = ObsidianNight.copy(alpha = 0.88f),
        borderColor = zoneColor,
        textColor = Color.White,
        textMeasurer = textMeasurer
    )
}

private fun DrawScope.drawAttackStep(
    step: AttackStep,
    isActive: Boolean,
    progress: Float,
    textMeasurer: TextMeasurer
) {
    val baseAlpha = if (isActive) 1f else 0.24f
    val pathColor = CyberCyan.copy(alpha = baseAlpha)

    // Spell circles
    step.spellZones.forEach { spell ->
        val sx = spell.xNorm * size.width
        val sy = spell.yNorm * size.height
        val r = spell.radiusNorm * minOf(size.width, size.height)
        val spellColor = when {
            spell.spellName.contains("Freeze", ignoreCase = true) -> Color(0xFF38BDF8)
            spell.spellName.contains("Jump", ignoreCase = true) ||
                spell.spellName.contains("Heal", ignoreCase = true) -> VictoryEmerald
            else -> SpellViolet
        }
        drawCircle(
            color = spellColor.copy(alpha = 0.22f * baseAlpha),
            radius = r,
            center = Offset(sx, sy)
        )
        drawCircle(
            color = spellColor.copy(alpha = 0.9f * baseAlpha),
            radius = r,
            center = Offset(sx, sy),
            style = Stroke(
                width = if (isActive) 3f else 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
            )
        )
        if (isActive) {
            drawBadgeLabel(
                text = spell.spellName.take(12),
                center = Offset(sx, sy),
                bgColor = ObsidianNight.copy(alpha = 0.88f),
                borderColor = spellColor,
                textColor = Color.White,
                textMeasurer = textMeasurer
            )
        }
    }

    // Troop paths
    if (step.pathPoints.size >= 2) {
        drawNormalizedArrowPath(
            points = step.pathPoints,
            color = pathColor,
            strokeWidth = if (isActive) 6.5f else 3f,
            dashed = isActive,
            progress = if (isActive) progress else null
        )
    }

    // Drop zones
    step.dropZones.forEachIndexed { idx, drop ->
        val dx = drop.xNorm * size.width
        val dy = drop.yNorm * size.height
        drawCircle(
            color = WarGold.copy(alpha = 0.25f * baseAlpha),
            radius = if (isActive) 22f else 12f,
            center = Offset(dx, dy)
        )
        drawCircle(
            color = WarGold.copy(alpha = baseAlpha),
            radius = if (isActive) 13f else 7f,
            center = Offset(dx, dy)
        )
        if (isActive) {
            drawBadgeLabel(
                text = "${step.stepNumber}.${idx + 1} ${drop.label.take(14)}",
                center = Offset(dx, (dy - 28f).coerceAtLeast(20f)),
                bgColor = ObsidianNight.copy(alpha = 0.92f),
                borderColor = WarGold,
                textColor = WarGold,
                textMeasurer = textMeasurer
            )
        }
    }
}

private fun DrawScope.drawNormalizedArrowPath(
    points: List<NormalizedPoint>,
    color: Color,
    strokeWidth: Float,
    dashed: Boolean,
    progress: Float?
) {
    if (points.size < 2) return
    val path = Path().apply {
        val first = points.first()
        moveTo(first.x * size.width, first.y * size.height)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = ((prev.x + curr.x) / 2f) * size.width
            val midY = ((prev.y + curr.y) / 2f) * size.height
            quadraticTo(prev.x * size.width, prev.y * size.height, midX, midY)
        }
        val last = points.last()
        lineTo(last.x * size.width, last.y * size.height)
    }

    // Outer glow
    drawPath(
        path = path,
        color = color.copy(alpha = color.alpha * 0.35f),
        style = Stroke(width = strokeWidth * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Core path
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = if (dashed) {
                PathEffect.dashPathEffect(floatArrayOf(22f, 14f), -(progress ?: 0f) * 72f)
            } else null
        )
    )

    // Animated troop squad pulse moving along the path
    if (progress != null) {
        val measure = PathMeasure()
        measure.setPath(path, false)
        val totalLen = measure.length
        if (totalLen > 1f) {
            listOf(progress, (progress + 0.33f) % 1f, (progress + 0.66f) % 1f).forEachIndexed { idx, frac ->
                val pos = measure.getPosition(totalLen * frac)
                val tan = measure.getTangent(totalLen * frac)
                if (pos != Offset.Unspecified) {
                    val alpha = if (idx == 0) 1f else 0.7f
                    drawCircle(
                        color = WarGold.copy(alpha = 0.35f * alpha),
                        radius = if (idx == 0) 18f else 12f,
                        center = pos
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = if (idx == 0) 8f else 5f,
                        center = pos
                    )
                    if (tan != Offset.Unspecified) {
                        val angle = atan2(tan.y, tan.x)
                        drawArrowHead(pos, angle, WarGold.copy(alpha = alpha), if (idx == 0) 16f else 11f)
                    }
                }
            }
        }
    }

    // End arrowhead
    val pPrev = points[points.size - 2]
    val pLast = points.last()
    val endPos = Offset(pLast.x * size.width, pLast.y * size.height)
    val startPos = Offset(pPrev.x * size.width, pPrev.y * size.height)
    val angle = atan2(endPos.y - startPos.y, endPos.x - startPos.x)
    drawArrowHead(endPos, angle, color, 20f)
}

private fun DrawScope.drawArrowHead(tip: Offset, angleRad: Float, color: Color, sizePx: Float) {
    val spread = Math.toRadians(28.0).toFloat()
    val p1 = Offset(
        tip.x - sizePx * cos(angleRad - spread),
        tip.y - sizePx * sin(angleRad - spread)
    )
    val p2 = Offset(
        tip.x - sizePx * cos(angleRad + spread),
        tip.y - sizePx * sin(angleRad + spread)
    )
    val head = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(head, color = color)
}

private fun DrawScope.drawManualMarker(
    marker: ManualMarker,
    index: Int,
    pulse: Float,
    textMeasurer: TextMeasurer
) {
    val cx = marker.xNorm * size.width
    val cy = marker.yNorm * size.height

    when (marker.type) {
        MarkerType.TROOP_DROP -> {
            drawCircle(
                color = CyberCyan.copy(alpha = 0.28f * pulse),
                radius = 26f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = CyberCyan,
                radius = 12f,
                center = Offset(cx, cy)
            )
            drawBadgeLabel(
                text = "P$index",
                center = Offset(cx, (cy - 26f).coerceAtLeast(18f)),
                bgColor = ObsidianNight.copy(alpha = 0.9f),
                borderColor = CyberCyan,
                textColor = CyberCyan,
                textMeasurer = textMeasurer
            )
        }
        MarkerType.SPELL_ZONE -> {
            val r = minOf(size.width, size.height) * 0.085f
            drawCircle(
                color = SpellViolet.copy(alpha = 0.24f),
                radius = r,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = SpellViolet,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))
            )
            drawBadgeLabel(
                text = "S$index",
                center = Offset(cx, cy),
                bgColor = ObsidianNight.copy(alpha = 0.9f),
                borderColor = SpellViolet,
                textColor = Color.White,
                textMeasurer = textMeasurer
            )
        }
        MarkerType.PRIORITY_TARGET -> {
            val r = 24f
            drawCircle(
                color = ThreatCrimson,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 3.5f)
            )
            drawLine(ThreatCrimson, Offset(cx - r * 1.3f, cy), Offset(cx + r * 1.3f, cy), 3f)
            drawLine(ThreatCrimson, Offset(cx, cy - r * 1.3f), Offset(cx, cy + r * 1.3f), 3f)
            drawBadgeLabel(
                text = "T$index",
                center = Offset(cx, (cy - 34f).coerceAtLeast(18f)),
                bgColor = ObsidianNight.copy(alpha = 0.9f),
                borderColor = ThreatCrimson,
                textColor = ThreatCrimson,
                textMeasurer = textMeasurer
            )
        }
        MarkerType.PATH_ARROW -> {
            if (marker.points.size >= 2) {
                drawNormalizedArrowPath(
                    points = marker.points,
                    color = WarGold,
                    strokeWidth = 6f,
                    dashed = false,
                    progress = null
                )
            }
        }
    }
}

private fun DrawScope.drawBadgeLabel(
    text: String,
    center: Offset,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    textMeasurer: TextMeasurer
) {
    val result = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    )
    val padX = 10f
    val padY = 5f
    val boxW = result.size.width + padX * 2
    val boxH = result.size.height + padY * 2
    val left = (center.x - boxW / 2f).coerceIn(4f, (size.width - boxW - 4f).coerceAtLeast(4f))
    val top = (center.y - boxH / 2f).coerceIn(4f, (size.height - boxH - 4f).coerceAtLeast(4f))

    drawRoundRect(
        color = bgColor,
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(boxW, boxH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = borderColor,
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(boxW, boxH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
        style = Stroke(width = 1.6f)
    )
    drawText(
        textLayoutResult = result,
        topLeft = Offset(left + padX, top + padY)
    )
}
