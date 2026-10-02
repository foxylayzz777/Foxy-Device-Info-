package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SparklineGraph(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 80.dp,
    maxValue: Float? = null,
    showGrid: Boolean = true
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val gradientColors = listOf(
        lineColor.copy(alpha = 0.35f),
        lineColor.copy(alpha = 0.0f)
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
    ) {
        val width = size.width
        val canvasHeight = size.height

        if (showGrid) {
            // Horizontal reference guide lines
            drawLine(
                color = gridColor,
                start = Offset(0f, canvasHeight * 0.25f),
                end = Offset(width, canvasHeight * 0.25f),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = gridColor,
                start = Offset(0f, canvasHeight * 0.75f),
                end = Offset(width, canvasHeight * 0.75f),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (dataPoints.size < 2) {
            // Idle placeholder line
            drawLine(
                color = lineColor.copy(alpha = 0.4f),
                start = Offset(0f, canvasHeight * 0.8f),
                end = Offset(width, canvasHeight * 0.8f),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            return@Canvas
        }

        val maxVal = maxValue ?: (dataPoints.maxOrNull()?.coerceAtLeast(10f) ?: 100f)
        val minVal = 0f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val padX = 6.dp.toPx()
        val usableWidth = (width - padX * 2).coerceAtLeast(1f)
        val stepX = usableWidth / (dataPoints.size - 1).coerceAtLeast(1)

        val linePath = Path()
        val fillPath = Path()

        val points = dataPoints.mapIndexed { index, value ->
            val normY = (1f - ((value - minVal) / range)).coerceIn(0f, 1f)
            Offset(padX + index * stepX, normY * (canvasHeight - 16.dp.toPx()) + 8.dp.toPx())
        }

        linePath.moveTo(points.first().x, points.first().y)
        fillPath.moveTo(points.first().x, canvasHeight)
        fillPath.lineTo(points.first().x, points.first().y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val cx = (p0.x + p1.x) / 2f
            linePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
        }

        fillPath.lineTo(points.last().x, canvasHeight)
        fillPath.close()

        // Draw gradient area under the curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = gradientColors,
                startY = 0f,
                endY = canvasHeight
            )
        )

        // Draw line curve
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // Draw last point indicator pulse dot
        val lastPoint = points.last()
        drawCircle(
            color = lineColor,
            radius = 4.5.dp.toPx(),
            center = lastPoint
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = lastPoint
        )
    }
}
