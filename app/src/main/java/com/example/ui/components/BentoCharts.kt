package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TextGray
import com.example.ui.viewmodel.ChartDataPoint
import java.util.Locale

@Composable
fun InteractiveBarChart(
    data: List<ChartDataPoint>,
    accentColor: Color, // Still kept for backward compatibility but we use monochrome theme colors primarily
    valueSuffix: String = " TL",
    modifier: Modifier = Modifier
) {
    if (data.isEmpty() || data.all { it.value == 0.0 }) {
        Box(
            modifier = modifier.background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(6.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Henüz harcama verisi bulunmuyor",
                color = TextGray,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val maxValue = data.maxOf { it.value }.coerceAtLeast(10.0)
    var selectedIndex by remember(data) { mutableStateOf(-1) }

    val labelStyle = TextStyle(
        color = TextGray,
        fontSize = 10.sp,
        fontFamily = MaterialTheme.typography.labelSmall.fontFamily
    )

    val tooltipStyle = TextStyle(
        color = MaterialTheme.colorScheme.onPrimary,
        fontSize = 11.sp,
        fontFamily = MaterialTheme.typography.labelMedium.fontFamily
    )

    // Make charts use the primary theme color (monochrome Black/White)
    val chartAccentColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val width = size.width
                        val paddingLeft = 40f
                        val paddingRight = 10f
                        val chartWidth = width - paddingLeft - paddingRight
                        val barSpacingRatio = 0.3f
                        val barCount = data.size
                        val barWidthWithSpacing = chartWidth / barCount
                        val barWidth = barWidthWithSpacing * (1f - barSpacingRatio)

                        val touchedX = offset.x - paddingLeft
                        val clickedIndex = (touchedX / barWidthWithSpacing).toInt()

                        if (clickedIndex in 0 until barCount) {
                            val barLeft = clickedIndex * barWidthWithSpacing + (barWidthWithSpacing * barSpacingRatio / 2f)
                            if (offset.x >= (barLeft + paddingLeft) && offset.x <= (barLeft + barWidth + paddingLeft)) {
                                selectedIndex = if (selectedIndex == clickedIndex) -1 else clickedIndex
                            } else {
                                selectedIndex = -1
                            }
                        } else {
                            selectedIndex = -1
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            val paddingLeft = 40f
            val paddingRight = 10f
            val paddingTop = 30f
            val paddingBottom = 40f

            val chartWidth = width - paddingLeft - paddingRight
            val chartHeight = height - paddingTop - paddingBottom

            // Draw Y-Axis gridlines (3 reference levels)
            val gridLines = 3
            for (i in 0..gridLines) {
                val ratio = i.toFloat() / gridLines
                val y = paddingTop + chartHeight * (1f - ratio)
                val gridValue = maxValue * ratio

                // Draw simple clean gray dashed line
                drawLine(
                    color = chartAccentColor.copy(alpha = 0.08f),
                    start = Offset(paddingLeft, y),
                    end = Offset(width - paddingRight, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )

                // Draw Y-Axis labels
                val formattedVal = if (gridValue >= 1000) {
                    String.format(Locale.getDefault(), "%.1fB", gridValue / 1000.0)
                } else {
                    gridValue.toInt().toString()
                }
                drawText(
                    textMeasurer = textMeasurer,
                    text = formattedVal,
                    topLeft = Offset(5f, y - 15f),
                    style = labelStyle
                )
            }

            // Draw Bars
            val barCount = data.size
            val barSpacingRatio = 0.3f
            val barWidthWithSpacing = chartWidth / barCount
            val barWidth = barWidthWithSpacing * (1f - barSpacingRatio)
            val barSpacing = barWidthWithSpacing * barSpacingRatio

            data.forEachIndexed { idx, point ->
                val barLeft = idx * barWidthWithSpacing + (barSpacing / 2f) + paddingLeft
                val barRatio = point.value / maxValue
                val barHeight = chartHeight * barRatio
                val barTop = paddingTop + chartHeight - barHeight

                val isSelected = idx == selectedIndex

                // Flat monochrome color
                val barColor = if (isSelected) {
                    chartAccentColor
                } else {
                    chartAccentColor.copy(alpha = 0.25f)
                }

                // Rounded top bar
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(barLeft, barTop.toFloat()),
                    size = Size(barWidth, barHeight.toFloat()),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Selected indicator
                if (isSelected) {
                    // Draw floating tooltip details
                    val tooltipText = String.format(Locale.getDefault(), "%.1f%s", point.value, valueSuffix)
                    val textLayoutResult = textMeasurer.measure(tooltipText, tooltipStyle)
                    val tooltipWidth = textLayoutResult.size.width
                    val tooltipHeight = textLayoutResult.size.height

                    val tooltipLeft = (barLeft + (barWidth / 2) - (tooltipWidth / 2)).coerceIn(10f, width - tooltipWidth - 10f)
                    val tooltipTop = (barTop - tooltipHeight - 12f).toFloat().coerceAtLeast(5f)

                    // Flat Tooltip Background (Matches the contrast color of the theme)
                    drawRoundRect(
                        color = chartAccentColor,
                        topLeft = Offset(tooltipLeft - 8f, tooltipTop - 4f),
                        size = Size(tooltipWidth + 16f, tooltipHeight + 8f),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Tooltip text
                    drawText(
                        textMeasurer = textMeasurer,
                        text = tooltipText,
                        topLeft = Offset(tooltipLeft, tooltipTop),
                        style = tooltipStyle
                    )
                }

                // X-Axis label
                val textLayoutResult = textMeasurer.measure(point.label, labelStyle)
                val labelWidth = textLayoutResult.size.width
                val labelLeft = barLeft + (barWidth / 2) - (labelWidth / 2)
                drawText(
                    textMeasurer = textMeasurer,
                    text = point.label,
                    topLeft = Offset(labelLeft, height - paddingBottom + 8f),
                    style = labelStyle
                )
            }
        }

        if (selectedIndex != -1) {
            Text(
                text = "Seçilen: ${data[selectedIndex].label} ➜ " +
                        String.format(Locale.getDefault(), "%.2f%s", data[selectedIndex].value, valueSuffix),
                color = chartAccentColor,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
        }
    }
}

@Composable
fun InteractiveLineChart(
    data: List<ChartDataPoint>,
    accentColor: Color, // Backward compatibility
    valueSuffix: String = " L/100km",
    modifier: Modifier = Modifier
) {
    if (data.isEmpty() || data.all { it.value == 0.0 }) {
        Box(
            modifier = modifier.background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(6.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Henüz verimlilik verisi bulunmuyor",
                color = TextGray,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val maxValue = data.maxOf { it.value }.coerceAtLeast(5.0) * 1.1
    val minValue = (data.minOf { it.value } * 0.9).coerceAtLeast(0.0)
    val valueDiff = (maxValue - minValue).coerceAtLeast(1.0)

    var selectedIndex by remember(data) { mutableStateOf(-1) }

    val labelStyle = TextStyle(
        color = TextGray,
        fontSize = 10.sp,
        fontFamily = MaterialTheme.typography.labelSmall.fontFamily
    )

    val tooltipStyle = TextStyle(
        color = MaterialTheme.colorScheme.onPrimary,
        fontSize = 11.sp,
        fontFamily = MaterialTheme.typography.labelMedium.fontFamily
    )

    // Make charts use the primary theme color (monochrome Black/White)
    val chartAccentColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val width = size.width
                        val paddingLeft = 40f
                        val paddingRight = 20f
                        val chartWidth = width - paddingLeft - paddingRight
                        val pointsCount = data.size
                        val xInterval = if (pointsCount > 1) chartWidth / (pointsCount - 1) else chartWidth

                        // Find closest point in X direction
                        val touchedX = offset.x - paddingLeft
                        val closestIdx = (touchedX / xInterval + 0.5f).toInt().coerceIn(0, pointsCount - 1)
                        val pointX = paddingLeft + closestIdx * xInterval

                        if (Math.abs(offset.x - pointX) < 30f) {
                            selectedIndex = if (selectedIndex == closestIdx) -1 else closestIdx
                        } else {
                            selectedIndex = -1
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            val paddingLeft = 40f
            val paddingRight = 20f
            val paddingTop = 30f
            val paddingBottom = 40f

            val chartWidth = width - paddingLeft - paddingRight
            val chartHeight = height - paddingTop - paddingBottom

            // Draw Y-Axis gridlines (3 reference levels)
            val gridLines = 3
            for (i in 0..gridLines) {
                val ratio = i.toFloat() / gridLines
                val y = paddingTop + chartHeight * (1f - ratio)
                val gridValue = minValue + valueDiff * ratio

                // Draw dashed line
                drawLine(
                    color = chartAccentColor.copy(alpha = 0.08f),
                    start = Offset(paddingLeft, y),
                    end = Offset(width - paddingRight, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )

                // Draw Y-Axis labels
                drawText(
                    textMeasurer = textMeasurer,
                    text = String.format(Locale.getDefault(), "%.1f", gridValue),
                    topLeft = Offset(5f, y - 15f),
                    style = labelStyle
                )
            }

            // Draw line connecting points
            val pointsCount = data.size
            val xInterval = if (pointsCount > 1) chartWidth / (pointsCount - 1) else chartWidth

            val path = Path()
            val fillPath = Path()

            data.forEachIndexed { idx, point ->
                val x = paddingLeft + idx * xInterval
                val ratio = (point.value - minValue) / valueDiff
                val y = paddingTop + chartHeight * (1f - ratio)

                if (idx == 0) {
                    path.moveTo(x, y.toFloat())
                    fillPath.moveTo(x, (paddingTop + chartHeight).toFloat())
                    fillPath.lineTo(x, y.toFloat())
                } else {
                    path.lineTo(x, y.toFloat())
                    fillPath.lineTo(x, y.toFloat())
                }

                if (idx == pointsCount - 1) {
                    fillPath.lineTo(x, (paddingTop + chartHeight).toFloat())
                    fillPath.close()
                }
            }

            // Draw subtle monochrome area fill
            drawPath(
                path = fillPath,
                color = chartAccentColor.copy(alpha = 0.05f)
            )

            // Draw actual trend line
            drawPath(
                path = path,
                color = chartAccentColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Point Circles & Tooltips
            data.forEachIndexed { idx, point ->
                val x = paddingLeft + idx * xInterval
                val ratio = (point.value - minValue) / valueDiff
                val y = paddingTop + chartHeight * (1f - ratio)

                val isSelected = idx == selectedIndex

                // Outer circle
                drawCircle(
                    color = if (isSelected) backgroundColor else chartAccentColor,
                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                    center = Offset(x, y.toFloat())
                )

                // Inner circle
                drawCircle(
                    color = if (isSelected) chartAccentColor else backgroundColor,
                    radius = if (isSelected) 3.dp.toPx() else 2.dp.toPx(),
                    center = Offset(x, y.toFloat())
                )

                if (isSelected) {
                    // Draw Floating Tooltip
                    val tooltipText = String.format(Locale.getDefault(), "%.1f%s", point.value, valueSuffix)
                    val textLayoutResult = textMeasurer.measure(tooltipText, tooltipStyle)
                    val tooltipWidth = textLayoutResult.size.width
                    val tooltipHeight = textLayoutResult.size.height

                    val tooltipLeft = (x - (tooltipWidth / 2)).coerceIn(10f, width - tooltipWidth - 10f)
                    val tooltipTop = (y - tooltipHeight - 12f).toFloat().coerceAtLeast(5f)

                    // Flat Tooltip Background
                    drawRoundRect(
                        color = chartAccentColor,
                        topLeft = Offset(tooltipLeft - 8f, tooltipTop - 4f),
                        size = Size(tooltipWidth + 16f, tooltipHeight + 8f),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Tooltip text
                    drawText(
                        textMeasurer = textMeasurer,
                        text = tooltipText,
                        topLeft = Offset(tooltipLeft, tooltipTop),
                        style = tooltipStyle
                    )
                }

                // X-Axis label
                val textLayoutResult = textMeasurer.measure(point.label, labelStyle)
                val labelWidth = textLayoutResult.size.width
                val labelLeft = x - (labelWidth / 2)
                drawText(
                    textMeasurer = textMeasurer,
                    text = point.label,
                    topLeft = Offset(labelLeft, height - paddingBottom + 8f),
                    style = labelStyle
                )
            }
        }

        if (selectedIndex != -1) {
            Text(
                text = "Tarih: ${data[selectedIndex].label} ➜ " +
                        String.format(Locale.getDefault(), "%.2f%s", data[selectedIndex].value, valueSuffix),
                color = chartAccentColor,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
        }
    }
}
