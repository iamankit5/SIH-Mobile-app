package com.example.sailfreight.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sailfreight.data.model.ForecastPoint
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun ForecastChart(
    points: List<ForecastPoint>,
    selectedPoint: ForecastPoint?,
    onPointScrubbed: (ForecastPoint?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    var touchX by remember { mutableFloatStateOf(-1f) }

    // Min and max values for Y scaling
    val allValues = mutableListOf<Double>()
    points.forEach { p ->
        p.benchmarkPmt?.let { allValues.add(it) }
        p.forecastPmt?.let { allValues.add(it) }
        p.lowerBoundPmt?.let { allValues.add(it) }
        p.upperBoundPmt?.let { allValues.add(it) }
    }
    val minY = max(2.0, (allValues.minOrNull() ?: 3.0) - 0.3)
    val maxY = (allValues.maxOrNull() ?: 5.0) + 0.4

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "📈 AI Multi-Horizon Freight Rate Forecast",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9)
                    )
                    Text(
                        text = "Prophet 95% Confidence Band • Touch to inspect rates",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                if (selectedPoint != null) {
                    val rate = selectedPoint.forecastPmt ?: selectedPoint.benchmarkPmt ?: 0.0
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(
                                text = selectedPoint.dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "$${"%.2f".format(Locale.US, rate)}/MT",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPoint.isHistorical) Color(0xFF60A5FA) else Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .pointerInput(points) {
                            detectTapGestures(
                                onPress = { offset ->
                                    touchX = offset.x
                                    val idx = ((offset.x / size.width) * (points.size - 1))
                                        .toInt()
                                        .coerceIn(0, points.size - 1)
                                    onPointScrubbed(points[idx])
                                }
                            )
                        }
                        .pointerInput(points) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    touchX = offset.x
                                    val idx = ((offset.x / size.width) * (points.size - 1))
                                        .toInt()
                                        .coerceIn(0, points.size - 1)
                                    onPointScrubbed(points[idx])
                                },
                                onDrag = { change, _ ->
                                    touchX = change.position.x
                                    val idx = ((change.position.x / size.width) * (points.size - 1))
                                        .toInt()
                                        .coerceIn(0, points.size - 1)
                                    onPointScrubbed(points[idx])
                                },
                                onDragEnd = {
                                    touchX = -1f
                                    onPointScrubbed(null)
                                },
                                onDragCancel = {
                                    touchX = -1f
                                    onPointScrubbed(null)
                                }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val paddingLeft = 40.dp.toPx()
                    val paddingBottom = 24.dp.toPx()
                    val chartW = w - paddingLeft
                    val chartH = h - paddingBottom

                    fun getX(index: Int): Float {
                        return paddingLeft + (index.toFloat() / (points.size - 1).toFloat()) * chartW
                    }

                    fun getY(value: Double): Float {
                        val normalized = (value - minY) / (maxY - minY)
                        return (chartH - (normalized * chartH)).toFloat()
                    }

                    // Draw Horizontal Grid Lines & Y-axis labels
                    val yStep = (maxY - minY) / 4.0
                    for (i in 0..4) {
                        val gridVal = minY + (i * yStep)
                        val yPos = getY(gridVal)
                        drawLine(
                            color = Color(0xFF334155).copy(alpha = 0.5f),
                            start = Offset(paddingLeft, yPos),
                            end = Offset(w, yPos),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "$${"%.1f".format(Locale.US, gridVal)}",
                            topLeft = Offset(4.dp.toPx(), yPos - 8.dp.toPx()),
                            style = TextStyle(
                                color = Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }

                    // Draw Confidence Interval Band (for future points)
                    val futurePoints = points.filterIndexed { idx, _ -> idx >= 45 }
                    if (futurePoints.isNotEmpty()) {
                        val confidencePath = Path()
                        var first = true

                        // Upper curve
                        for (p in futurePoints) {
                            val globalIdx = points.indexOf(p)
                            val x = getX(globalIdx)
                            val y = getY(p.upperBoundPmt ?: p.forecastPmt ?: CURRENT_SPOT_PMT_FALLBACK)
                            if (first) {
                                confidencePath.moveTo(x, y)
                                first = false
                            } else {
                                confidencePath.lineTo(x, y)
                            }
                        }

                        // Lower curve back
                        for (i in futurePoints.indices.reversed()) {
                            val p = futurePoints[i]
                            val globalIdx = points.indexOf(p)
                            val x = getX(globalIdx)
                            val y = getY(p.lowerBoundPmt ?: p.forecastPmt ?: CURRENT_SPOT_PMT_FALLBACK)
                            confidencePath.lineTo(x, y)
                        }
                        confidencePath.close()

                        drawPath(
                            path = confidencePath,
                            color = Color(0xFFF59E0B).copy(alpha = 0.18f)
                        )
                    }

                    // Draw Historical Line (Solid Sky Blue)
                    val histPath = Path()
                    var histStarted = false
                    for (i in 0..45) {
                        if (i < points.size) {
                            val p = points[i]
                            val x = getX(i)
                            val yVal = p.benchmarkPmt ?: continue
                            val y = getY(yVal)
                            if (!histStarted) {
                                histPath.moveTo(x, y)
                                histStarted = true
                            } else {
                                histPath.lineTo(x, y)
                            }
                        }
                    }
                    drawPath(
                        path = histPath,
                        color = Color(0xFF60A5FA),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw AI Forecast Line (Dashed Amber)
                    val forecastPath = Path()
                    var forecastStarted = false
                    for (i in 45 until points.size) {
                        val p = points[i]
                        val x = getX(i)
                        val yVal = p.forecastPmt ?: continue
                        val y = getY(yVal)
                        if (!forecastStarted) {
                            forecastPath.moveTo(x, y)
                            forecastStarted = true
                        } else {
                            forecastPath.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = forecastPath,
                        color = Color(0xFFF59E0B),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                            cap = StrokeCap.Round
                        )
                    )

                    // Draw Date Milestones along X-axis
                    val milestones = listOf(0, 22, 45, 59, 74)
                    milestones.forEach { idx ->
                        if (idx < points.size) {
                            val p = points[idx]
                            val x = getX(idx)
                            drawText(
                                textMeasurer = textMeasurer,
                                text = p.dateStr,
                                topLeft = Offset(max(paddingLeft, x - 18.dp.toPx()), h - 18.dp.toPx()),
                                style = TextStyle(
                                    color = if (idx == 45) Color(0xFF38BDF8) else Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    fontWeight = if (idx == 45) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }

                    // Scrubber Indicator
                    if (touchX >= paddingLeft && touchX <= w) {
                        val scrubIdx = (((touchX - paddingLeft) / chartW) * (points.size - 1))
                            .toInt()
                            .coerceIn(0, points.size - 1)
                        val p = points[scrubIdx]
                        val xPos = getX(scrubIdx)
                        val valScrub = p.forecastPmt ?: p.benchmarkPmt ?: 3.56
                        val yPos = getY(valScrub)

                        drawLine(
                            color = Color.White.copy(alpha = 0.7f),
                            start = Offset(xPos, 0f),
                            end = Offset(xPos, chartH),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                        drawCircle(
                            color = if (p.isHistorical) Color(0xFF60A5FA) else Color(0xFFF59E0B),
                            radius = 5.dp.toPx(),
                            center = Offset(xPos, yPos)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = Offset(xPos, yPos)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chart Legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = Color(0xFF60A5FA), label = "Historical Benchmark")
                LegendItem(color = Color(0xFFF59E0B), label = "AI Forecast", isDashed = true)
                LegendItem(color = Color(0xFFF59E0B).copy(alpha = 0.4f), label = "95% Confidence Band")
            }
        }
    }
}

private const val CURRENT_SPOT_PMT_FALLBACK = 3.56

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isDashed: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isDashed) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(3.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFCBD5E1)
        )
    }
}
