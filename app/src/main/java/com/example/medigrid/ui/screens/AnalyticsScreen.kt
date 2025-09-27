package com.example.medigrid.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Patient Trends Line Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Patient Trends (Last 30 Days)",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var selectedPeriod by remember { mutableStateOf("Last 30 Days") }
                            ExposedDropdownMenuBox(
                                expanded = false,
                                onExpandedChange = { }
                            ) {
                                OutlinedButton(
                                    onClick = { },
                                    modifier = Modifier.menuAnchor()
                                ) {
                                    Text(selectedPeriod)
                                }
                            }
                            OutlinedButton(
                                onClick = { /* Export */ },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MediBlue
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Export"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    LineChart(
                        data = listOf(
                            245,
                            267,
                            189,
                            298,
                            234,
                            278,
                            312,
                            289,
                            345,
                            298,
                            267,
                            234,
                            298,
                            345,
                            267
                        ),
                        labels = listOf(
                            "1",
                            "3",
                            "5",
                            "7",
                            "9",
                            "11",
                            "13",
                            "15",
                            "17",
                            "19",
                            "21",
                            "23",
                            "25",
                            "27",
                            "30"
                        ),
                        modifier = Modifier.height(200.dp),
                        lineColor = MediBlue
                    )
                }
            }
        }

        // Health Conditions Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Top Health Conditions",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    val conditions = listOf(
                        ChartData("Diabetes", 35, MediBlue),
                        ChartData("Hypertension", 28, MediGreen),
                        ChartData("Respiratory", 18, WarningOrange),
                        ChartData("Cardiac", 12, DangerRed),
                        ChartData("Other", 7, TextSecondary)
                    )

                    BarChart(
                        data = conditions,
                        modifier = Modifier.height(200.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Legend
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        conditions.forEach { condition ->
                            ConditionItem(condition.label, condition.value, condition.color)
                        }
                    }
                }
            }
        }

        // Medicine Usage Horizontal Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Medicine Usage Analytics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    val medicines = listOf(
                        ChartData("Paracetamol", 850, MediBlue),
                        ChartData("Amoxicillin", 340, MediGreen),
                        ChartData("Metformin", 567, WarningOrange),
                        ChartData("Insulin", 120, DangerRed),
                        ChartData("Aspirin", 289, Color(0xFF9C27B0))
                    )

                    HorizontalBarChart(
                        data = medicines,
                        modifier = Modifier.height(250.dp)
                    )
                }
            }
        }

        // Response Time Pie Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Emergency Response Times",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val responseData = listOf(
                            ChartData("0-5 min", 15, SuccessGreen),
                            ChartData("5-15 min", 65, MediBlue),
                            ChartData("15-30 min", 18, WarningOrange),
                            ChartData("30+ min", 2, DangerRed)
                        )

                        PieChart(
                            data = responseData,
                            modifier = Modifier.size(200.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            responseData.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(item.color, RoundedCornerShape(2.dp))
                                    )
                                    Text(
                                        text = "${item.label}: ${item.value}%",
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Average Response: 12 min",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SuccessGreen
                            )
                        }
                    }
                }
            }
        }

        // Network Performance Metrics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Network Performance Metrics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AnimatedMetricCard("Uptime", "94.2%", SuccessGreen, "▲ 2.1%")
                        AnimatedMetricCard("Avg Load", "67%", MediBlue, "▼ 5.2%")
                        AnimatedMetricCard("Response", "1.2s", WarningOrange, "▲ 0.3s")
                        AnimatedMetricCard("Errors", "0.02%", DangerRed, "▼ 0.01%")
                    }
                }
            }
        }
    }
}

data class ChartData(
    val label: String,
    val value: Int,
    val color: Color,
)

@Composable
private fun LineChart(
    data: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    lineColor: Color = MediBlue,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(2000, easing = EaseOutCubic)
    )

    Canvas(modifier = modifier.fillMaxWidth()) {
        val width = size.width
        val height = size.height
        val padding = 40.dp.toPx()

        // Draw background grid
        drawGrid(width, height, padding)

        if (data.isNotEmpty()) {
            val maxValue = data.maxOrNull() ?: 0
            val stepX = (width - 2 * padding) / (data.size - 1)
            val chartHeight = height - 2 * padding

            // Draw line
            val path = Path()
            data.forEachIndexed { index, value ->
                val x = padding + index * stepX
                val y =
                    padding + chartHeight - (value.toFloat() / maxValue * chartHeight) * animatedProgress

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
            )

            // Draw points
            data.forEachIndexed { index, value ->
                val x = padding + index * stepX
                val y =
                    padding + chartHeight - (value.toFloat() / maxValue * chartHeight) * animatedProgress

                drawCircle(
                    color = lineColor,
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
private fun BarChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1500, easing = EaseOutCubic)
    )

    Canvas(modifier = modifier.fillMaxWidth()) {
        val width = size.width
        val height = size.height
        val padding = 40.dp.toPx()

        if (data.isNotEmpty()) {
            val maxValue = data.maxByOrNull { it.value }?.value ?: 0
            val barWidth = (width - 2 * padding) / data.size * 0.8f
            val barSpacing = (width - 2 * padding) / data.size * 0.2f
            val chartHeight = height - 2 * padding

            data.forEachIndexed { index, item ->
                val barHeight = (item.value.toFloat() / maxValue * chartHeight) * animatedProgress
                val x = padding + index * (barWidth + barSpacing)
                val y = height - padding - barHeight

                drawRect(
                    color = item.color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )
            }
        }
    }
}

@Composable
private fun HorizontalBarChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1500, easing = EaseOutCubic)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val maxValue = data.maxByOrNull { it.value }?.value ?: 1

        data.forEach { item ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.label,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${item.value}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = item.color
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.1f),
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (item.value.toFloat() / maxValue) * animatedProgress)
                            .fillMaxHeight()
                            .background(
                                item.color,
                                RoundedCornerShape(10.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PieChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(2000, easing = EaseOutCubic)
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = minOf(size.width, size.height) / 2 * 0.8f

        val total = data.sumOf { it.value }
        var startAngle = -90f

        data.forEach { item ->
            val sweepAngle = (item.value.toFloat() / total * 360f) * animatedProgress

            drawArc(
                color = item.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            startAngle += sweepAngle
        }

        // Draw center circle for donut effect
        drawCircle(
            color = Color.White,
            radius = radius * 0.4f,
            center = center
        )
    }
}

@Composable
private fun AnimatedMetricCard(
    title: String,
    value: String,
    color: Color,
    change: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondary
            )
            Text(
                text = change,
                fontSize = 9.sp,
                color = if (change.startsWith("▲")) SuccessGreen else DangerRed
            )
        }
    }
}

private fun DrawScope.drawGrid(width: Float, height: Float, padding: Float) {
    val gridColor = Color.Gray.copy(alpha = 0.3f)

    // Vertical grid lines
    for (i in 0..4) {
        val x = padding + i * (width - 2 * padding) / 4
        drawLine(
            color = gridColor,
            start = Offset(x, padding),
            end = Offset(x, height - padding),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Horizontal grid lines
    for (i in 0..4) {
        val y = padding + i * (height - 2 * padding) / 4
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(width - padding, y),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
private fun ConditionItem(
    name: String,
    percentage: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )

        Text(
            text = name,
            fontSize = 14.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "$percentage%",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}