package com.myday.dailyfocus.ui.components.foko

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class FokoPalette(
    val highlight: Color,
    val mid: Color,
    val base: Color,
    val shadow: Color,
    val earLight: Color,
    val earDark: Color,
    val wingLight: Color,
    val wingDark: Color,
    val shadowColor: Color,
    val cheek: Color,
    val beakLight: Color,
    val beakDark: Color,
    val accent: Color
)

private val IdlePalette = FokoPalette(
    highlight = Color(0xFFC4BDFF), mid = Color(0xFF8B7CF7), base = Color(0xFF6C5CE7), shadow = Color(0xFF5641D4),
    earLight = Color(0xFFB8B0FF), earDark = Color(0xFF7B6BF0),
    wingLight = Color(0xFF7B6BF0), wingDark = Color(0xFF5641D4),
    shadowColor = Color(0xFF4834B5),
    cheek = Color(0xFFFF9FF3),
    beakLight = Color(0xFFFFCC80), beakDark = Color(0xFFFF9F43),
    accent = Color(0xFFE84393)
)

private val FocusingPalette = FokoPalette(
    highlight = Color(0xFFFFBE7B), mid = Color(0xFFF0872A), base = Color(0xFFE55A24), shadow = Color(0xFFCC3B14),
    earLight = Color(0xFFFFB86C), earDark = Color(0xFFEE6B2F),
    wingLight = Color(0xFFE67E22), wingDark = Color(0xFFCC3B14),
    shadowColor = Color(0xFFB33A12),
    cheek = Color(0xFFFFCCCC),
    beakLight = Color(0xFFFFCC80), beakDark = Color(0xFFFF9F43),
    accent = Color(0xFFFDCB6E)
)

private val TaskCompletePalette = FokoPalette(
    highlight = Color(0xFF7BFFCF), mid = Color(0xFF1DD1A1), base = Color(0xFF00B894), shadow = Color(0xFF009975),
    earLight = Color(0xFF6EEFC0), earDark = Color(0xFF10AC84),
    wingLight = Color(0xFF1ABC9C), wingDark = Color(0xFF009975),
    shadowColor = Color(0xFF00795E),
    cheek = Color(0xFFFF9FF3),
    beakLight = Color(0xFFFFCC80), beakDark = Color(0xFFFF9F43),
    accent = Color(0xFFFDCB6E)
)

private val BreakTimePalette = FokoPalette(
    highlight = Color(0xFF93CBFF), mid = Color(0xFF3DA2F0), base = Color(0xFF0984E3), shadow = Color(0xFF0767B5),
    earLight = Color(0xFF85C1FF), earDark = Color(0xFF2196F3),
    wingLight = Color(0xFF2196F3), wingDark = Color(0xFF0767B5),
    shadowColor = Color(0xFF064D8A),
    cheek = Color(0xFFBFE0FF),
    beakLight = Color(0xFFFFCC80), beakDark = Color(0xFFFF9F43),
    accent = Color(0xFF74B9FF)
)

private val CelebratingPalette = FokoPalette(
    highlight = Color(0xFFFFE082), mid = Color(0xFFFFC107), base = Color(0xFFF9A825), shadow = Color(0xFFE67E22),
    earLight = Color(0xFFFFD54F), earDark = Color(0xFFF0A30A),
    wingLight = Color(0xFFF0A30A), wingDark = Color(0xFFE67E22),
    shadowColor = Color(0xFFB8700F),
    cheek = Color(0xFFFF9FF3),
    beakLight = Color(0xFFFFCC80), beakDark = Color(0xFFFF9F43),
    accent = Color(0xFFE84393)
)

private val StreakAlertPalette = FokoPalette(
    highlight = Color(0xFFFFB8E0), mid = Color(0xFFF368E0), base = Color(0xFFE84393), shadow = Color(0xFFC0307B),
    earLight = Color(0xFFFFA4D4), earDark = Color(0xFFE84393),
    wingLight = Color(0xFFE84393), wingDark = Color(0xFFC0307B),
    shadowColor = Color(0xFF8A1A55),
    cheek = Color(0xFFFF6B9D),
    beakLight = Color.White, beakDark = Color(0xFFF3D9E6),
    accent = Color(0xFFFF6B9D)
)

private fun paletteFor(state: FokoState): FokoPalette = when (state) {
    is FokoState.Idle -> IdlePalette
    is FokoState.Focusing -> FocusingPalette
    is FokoState.TaskComplete -> TaskCompletePalette
    is FokoState.BreakTime -> BreakTimePalette
    is FokoState.Celebrating -> CelebratingPalette
    is FokoState.StreakAlert -> StreakAlertPalette
}

/**
 * Foko, drawn entirely with Canvas draw calls (no bitmaps) so the character scales cleanly
 * from the 36dp Summary placement up to the 72dp Day Complete placement.
 */
@Composable
fun FokoCharacter(
    state: FokoState,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val transform = rememberFokoTransform(state)
    val accentTime = rememberFokoAccentTime()
    val palette = paletteFor(state)

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = transform.scale
                scaleY = transform.scale
                rotationZ = transform.rotationZ
                translationY = transform.translationYDp.dp.toPx()
            }
            .clearAndSetSemantics { }
    ) {
        drawFokoBody(palette)
        drawFokoFace(palette, state, accentTime)
        drawFokoAccents(palette, state, accentTime)
    }
}

private fun DrawScope.drawFokoBody(palette: FokoPalette) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val bodyTopLeft = Offset(cx - w * 0.35f, cy - h * 0.33f)
    val bodySize = Size(w * 0.70f, h * 0.66f)

    // Drop shadow grounds the character.
    drawOval(
        color = palette.shadowColor.copy(alpha = 0.12f),
        topLeft = Offset(cx - w * 0.28f, cy + h * 0.30f),
        size = Size(w * 0.56f, h * 0.12f)
    )

    // Wings, slightly behind the body.
    val wingBrush = Brush.radialGradient(listOf(palette.wingLight, palette.wingDark))
    drawOval(brush = wingBrush, topLeft = Offset(cx - w * 0.47f, cy - h * 0.04f), size = Size(w * 0.20f, h * 0.40f))
    drawOval(brush = wingBrush, topLeft = Offset(cx + w * 0.27f, cy - h * 0.04f), size = Size(w * 0.20f, h * 0.40f))

    // Ear tufts.
    val earBrush = Brush.radialGradient(listOf(palette.earLight, palette.earDark))
    val leftEar = Path().apply {
        moveTo(cx - w * 0.26f, cy - h * 0.28f)
        quadraticTo(cx - w * 0.34f, cy - h * 0.50f, cx - w * 0.18f, cy - h * 0.40f)
        close()
    }
    val rightEar = Path().apply {
        moveTo(cx + w * 0.26f, cy - h * 0.28f)
        quadraticTo(cx + w * 0.34f, cy - h * 0.50f, cx + w * 0.18f, cy - h * 0.40f)
        close()
    }
    drawPath(leftEar, brush = earBrush)
    drawPath(rightEar, brush = earBrush)

    // Body: radial gradient lit top-left, shadowed bottom-right for a spherical illusion.
    val bodyBrush = Brush.radialGradient(
        colorStops = arrayOf(0f to palette.highlight, 0.4f to palette.mid, 0.75f to palette.base, 1f to palette.shadow),
        center = Offset(bodyTopLeft.x + bodySize.width * 0.4f, bodyTopLeft.y + bodySize.height * 0.35f),
        radius = bodySize.width * 0.65f
    )
    drawOval(brush = bodyBrush, topLeft = bodyTopLeft, size = bodySize)

    // Rim light: shiny top-left edge highlight.
    val rimBrush = Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
        center = Offset(bodyTopLeft.x + bodySize.width * 0.28f, bodyTopLeft.y + bodySize.height * 0.24f),
        radius = bodySize.width * 0.32f
    )
    drawOval(brush = rimBrush, topLeft = bodyTopLeft, size = bodySize)

    // Belly highlight: light bounce lower-center.
    val bellyBrush = Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
        center = Offset(cx, cy + h * 0.16f),
        radius = w * 0.18f
    )
    drawOval(brush = bellyBrush, topLeft = Offset(cx - w * 0.16f, cy + h * 0.03f), size = Size(w * 0.32f, h * 0.26f))
}

private fun DrawScope.drawFokoFace(palette: FokoPalette, state: FokoState, accentTime: Float) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val eyeY = cy - h * 0.03f
    val eyeDx = w * 0.13f
    val eyeRadius = w * 0.10f

    // Cheeks
    val cheekAlpha = if (state is FokoState.StreakAlert) 0.30f else 0.55f
    drawCircle(brush = Brush.radialGradient(listOf(palette.cheek.copy(alpha = cheekAlpha), Color.Transparent)), radius = w * 0.10f, center = Offset(cx - eyeDx * 1.6f, eyeY + h * 0.10f))
    drawCircle(brush = Brush.radialGradient(listOf(palette.cheek.copy(alpha = cheekAlpha), Color.Transparent)), radius = w * 0.10f, center = Offset(cx + eyeDx * 1.6f, eyeY + h * 0.10f))

    when (state) {
        is FokoState.BreakTime -> {
            // Sleepy horizontal-line eyes.
            drawLine(Color.White, Offset(cx - eyeDx - eyeRadius, eyeY), Offset(cx - eyeDx + eyeRadius, eyeY), strokeWidth = w * 0.03f)
            drawLine(Color.White, Offset(cx + eyeDx - eyeRadius, eyeY), Offset(cx + eyeDx + eyeRadius, eyeY), strokeWidth = w * 0.03f)
            drawArc(palette.shadowColor, startAngle = 20f, sweepAngle = 40f, useCenter = false, topLeft = Offset(cx - w * 0.09f, cy + h * 0.10f), size = Size(w * 0.18f, h * 0.10f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.025f))
        }
        is FokoState.TaskComplete -> {
            // Happy closed-eye arcs.
            val eyeArcStroke = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.035f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawArc(Color(0xFF006D5B), startAngle = 200f, sweepAngle = 140f, useCenter = false, topLeft = Offset(cx - eyeDx - eyeRadius, eyeY - eyeRadius * 0.6f), size = Size(eyeRadius * 2, eyeRadius * 1.4f), style = eyeArcStroke)
            drawArc(Color(0xFF006D5B), startAngle = 200f, sweepAngle = 140f, useCenter = false, topLeft = Offset(cx + eyeDx - eyeRadius, eyeY - eyeRadius * 0.6f), size = Size(eyeRadius * 2, eyeRadius * 1.4f), style = eyeArcStroke)
            drawArc(Color.White, startAngle = 20f, sweepAngle = 140f, useCenter = false, topLeft = Offset(cx - w * 0.12f, cy + h * 0.06f), size = Size(w * 0.24f, h * 0.14f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.035f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        is FokoState.Celebrating -> {
            drawGlossyEye(palette, Offset(cx - eyeDx, eyeY), eyeRadius, pupilShift = 0f)
            drawGlossyEye(palette, Offset(cx + eyeDx, eyeY), eyeRadius, pupilShift = 0f)
            drawArc(Color.White, startAngle = 15f, sweepAngle = 150f, useCenter = false, topLeft = Offset(cx - w * 0.15f, cy + h * 0.05f), size = Size(w * 0.30f, h * 0.16f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        is FokoState.StreakAlert -> {
            drawGlossyEye(palette, Offset(cx - eyeDx, eyeY), eyeRadius * 1.15f, pupilShift = 0f)
            drawGlossyEye(palette, Offset(cx + eyeDx, eyeY), eyeRadius * 1.15f, pupilShift = 0f)
            val browStroke = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.025f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawArc(palette.shadow, startAngle = 200f, sweepAngle = 60f, useCenter = false, topLeft = Offset(cx - eyeDx - eyeRadius, eyeY - eyeRadius * 1.8f), size = Size(eyeRadius * 2, eyeRadius), style = browStroke)
            drawArc(palette.shadow, startAngle = 320f, sweepAngle = 60f, useCenter = false, topLeft = Offset(cx + eyeDx - eyeRadius, eyeY - eyeRadius * 1.8f), size = Size(eyeRadius * 2, eyeRadius), style = browStroke)
        }
        is FokoState.Focusing -> {
            drawGlossyEye(palette, Offset(cx - eyeDx, eyeY), eyeRadius, pupilShift = -eyeRadius * 0.25f)
            drawGlossyEye(palette, Offset(cx + eyeDx, eyeY), eyeRadius, pupilShift = -eyeRadius * 0.25f)
            drawLine(Color.White, Offset(cx - w * 0.05f, cy + h * 0.14f), Offset(cx + w * 0.05f, cy + h * 0.14f), strokeWidth = w * 0.025f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        else -> {
            drawGlossyEye(palette, Offset(cx - eyeDx, eyeY), eyeRadius, pupilShift = 0f)
            drawGlossyEye(palette, Offset(cx + eyeDx, eyeY), eyeRadius, pupilShift = 0f)
            drawArc(palette.accent, startAngle = 20f, sweepAngle = 140f, useCenter = false, topLeft = Offset(cx - w * 0.09f, cy + h * 0.08f), size = Size(w * 0.18f, h * 0.10f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.025f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
    }

    // Beak: small dimensional triangle, lighter on the left half.
    val beakTop = cy + h * 0.10f
    val beakWidth = w * 0.10f
    val beakHeight = h * 0.08f
    val leftBeak = Path().apply {
        moveTo(cx, beakTop); lineTo(cx - beakWidth / 2, beakTop); lineTo(cx, beakTop + beakHeight); close()
    }
    val rightBeak = Path().apply {
        moveTo(cx, beakTop); lineTo(cx + beakWidth / 2, beakTop); lineTo(cx, beakTop + beakHeight); close()
    }
    drawPath(leftBeak, color = palette.beakLight)
    drawPath(rightBeak, color = palette.beakDark)
}

private fun DrawScope.drawGlossyEye(palette: FokoPalette, center: Offset, radius: Float, pupilShift: Float) {
    drawCircle(brush = Brush.radialGradient(listOf(Color.White, Color(0xFFE8E6F0)), center = center, radius = radius), radius = radius, center = center)
    val pupilCenter = Offset(center.x + pupilShift - radius * 0.1f, center.y - radius * 0.1f)
    val pupilRadius = radius * 0.55f
    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF3D2B7A), Color(0xFF1A0E45)), center = pupilCenter, radius = pupilRadius), radius = pupilRadius, center = pupilCenter)
    drawCircle(Color.White, radius = pupilRadius * 0.32f, center = Offset(pupilCenter.x + pupilRadius * 0.35f, pupilCenter.y - pupilRadius * 0.35f))
    drawCircle(Color.White.copy(alpha = 0.4f), radius = pupilRadius * 0.18f, center = Offset(pupilCenter.x - pupilRadius * 0.3f, pupilCenter.y + pupilRadius * 0.35f))
}

private fun DrawScope.drawFokoAccents(palette: FokoPalette, state: FokoState, accentTime: Float) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f

    when (state) {
        is FokoState.Focusing -> {
            repeat(3) { i ->
                val yOffset = cy - h * 0.10f + i * h * 0.10f
                val phase = (accentTime + i * 0.15f) % 1f
                val lineLength = w * 0.16f * (0.6f + 0.4f * sin(phase * 2f * PI.toFloat()).let { (it + 1f) / 2f })
                drawLine(
                    color = palette.accent,
                    start = Offset(cx + w * 0.38f, yOffset),
                    end = Offset(cx + w * 0.38f + lineLength, yOffset),
                    strokeWidth = w * 0.025f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
        is FokoState.TaskComplete -> {
            listOf(-0.28f, 0f, 0.28f).forEachIndexed { i, dx ->
                drawStar(Offset(cx + dx * w, cy - h * 0.48f - (i % 2) * h * 0.05f), w * 0.06f, if (i == 1) Color(0xFFF9A825) else Color(0xFFFDCB6E))
            }
        }
        is FokoState.BreakTime -> {
            listOf(0.6f, 0.85f, 1f).forEachIndexed { i, sizeFrac ->
                val phase = (accentTime + i * 0.3f) % 1f
                val alpha = 1f - phase
                val yPos = cy - h * 0.35f - phase * h * 0.35f
                val zColor = when (i) { 0 -> Color(0xFF74B9FF); 1 -> Color(0xFFA8D8FF); else -> Color(0xFFD4ECFF) }
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = zColor.copy(alpha = alpha.coerceIn(0f, 1f)).toArgb()
                        textSize = w * 0.16f * sizeFrac
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                    drawText("z", cx + w * 0.30f + i * w * 0.06f, yPos, paint)
                }
            }
        }
        is FokoState.Celebrating -> {
            val confettiColors = listOf(Color(0xFFFF6B9D), Color(0xFFA29BFE), Color(0xFF55EFC4), Color(0xFFFDCB6E), Color(0xFF74B9FF))
            confettiColors.forEachIndexed { i, color ->
                val phase = (accentTime + i * 0.18f) % 1f
                val angle = i * (2f * PI.toFloat() / confettiColors.size)
                val radius = w * 0.55f
                val x = cx + cos(angle) * radius
                val y = cy - h * 0.10f - phase * h * 0.25f + sin(angle) * h * 0.05f
                drawCircle(color.copy(alpha = 0.5f * (1f - phase)), radius = w * 0.035f, center = Offset(x, y))
            }
            listOf(-0.30f to Color(0xFFFF6B9D), 0f to Color(0xFFA29BFE), 0.30f to Color(0xFF55EFC4)).forEach { (dx, color) ->
                drawStar(Offset(cx + dx * w, cy - h * 0.50f), w * 0.06f, color)
            }
        }
        is FokoState.StreakAlert -> {
            repeat(4) { i ->
                val phase = (accentTime + i * 0.25f) % 1f
                val angle = (i * 90f + phase * 40f) * (PI.toFloat() / 180f)
                val radius = w * 0.50f + phase * w * 0.10f
                val x = cx + cos(angle) * radius
                val y = cy + sin(angle) * radius * 0.7f
                drawHeart(Offset(x, y), w * 0.05f, palette.accent.copy(alpha = 0.4f * (1f - phase * 0.5f)))
            }
        }
        else -> Unit
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    for (i in 0 until 3) {
        val angle = -PI.toFloat() / 2f + i * (2f * PI.toFloat() / 3f)
        val point = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(path, color = color)
}

private fun DrawScope.drawHeart(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y + size * 0.6f)
        cubicTo(
            center.x - size * 1.2f, center.y - size * 0.4f,
            center.x - size * 0.4f, center.y - size * 1.2f,
            center.x, center.y - size * 0.3f
        )
        cubicTo(
            center.x + size * 0.4f, center.y - size * 1.2f,
            center.x + size * 1.2f, center.y - size * 0.4f,
            center.x, center.y + size * 0.6f
        )
        close()
    }
    drawPath(path, color = color)
}
