package com.example.medigrid.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.data.Alert
import com.example.medigrid.data.AlertLevel
import com.example.medigrid.data.SampleData
import com.example.medigrid.ui.components.StatCardComponent
import com.example.medigrid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp

    // Responsive grid columns based on screen size
    val gridColumns = when {
        screenWidth > 1200 -> 4 // Large screens
        screenWidth > 800 -> 3  // Medium screens
        screenWidth > 600 -> 2  // Small tablets
        else -> if (isLandscape) 2 else 1 // Phones
    }

    // Responsive spacing
    val contentPadding = if (screenWidth < 600) 8.dp else 16.dp
    val itemSpacing = if (screenWidth < 600) 12.dp else 24.dp

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        // Statistics Grid
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (gridColumns > 2) 2 else gridColumns),
                horizontalArrangement = Arrangement.spacedBy(if (screenWidth < 600) 8.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (screenWidth < 600) 8.dp else 16.dp),
                modifier = Modifier.height(if (gridColumns == 1) 600.dp else 280.dp)
            ) {
                items(SampleData.stats) { stat ->
                    StatCardComponent(statCard = stat)
                }
            }
        }

        // Recent Alerts Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(if (screenWidth < 600) 16.dp else 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Alerts",
                            fontSize = if (screenWidth < 600) 18.sp else 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        OutlinedButton(
                            onClick = { /* Navigate to alerts */ },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MediBlue
                            ),
                            modifier = Modifier.then(
                                if (screenWidth < 600) Modifier.height(32.dp)
                                else Modifier
                            )
                        ) {
                            Text(
                                "View All",
                                fontSize = if (screenWidth < 600) 12.sp else 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SampleData.alerts.forEach { alert ->
                            AlertItem(
                                alert = alert,
                                isCompact = screenWidth < 600
                            )
                        }
                    }
                }
            }
        }

        // Network Map Placeholder - Adaptive height
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(if (screenWidth < 600) 16.dp else 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Network Map",
                            fontSize = if (screenWidth < 600) 18.sp else 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Button(
                            onClick = { /* Expand map */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MediBlue
                            ),
                            modifier = if (screenWidth < 600) Modifier.height(32.dp) else Modifier
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Expand"
                            )
                            if (screenWidth >= 600) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Full View")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Map placeholder - responsive height
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (screenWidth < 600) 150.dp else 200.dp)
                            .background(
                                color = MediBlue,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Interactive Map Loading...",
                                color = Color.White,
                                fontSize = if (screenWidth < 600) 14.sp else 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "28 clinics • 5 provinces • Real-time status",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = if (screenWidth < 600) 12.sp else 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertItem(
    alert: Alert,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val alertColor = when (alert.level) {
        AlertLevel.URGENT -> DangerRed
        AlertLevel.WARNING -> WarningOrange
        AlertLevel.INFO -> MediBlue
    }

    val alertBackgroundColor = when (alert.level) {
        AlertLevel.URGENT -> DangerRed.copy(alpha = 0.1f)
        AlertLevel.WARNING -> WarningOrange.copy(alpha = 0.1f)
        AlertLevel.INFO -> MediBlue.copy(alpha = 0.1f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = alertBackgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 12.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Alert indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (isCompact) 30.dp else 40.dp)
                    .background(
                        color = alertColor,
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = alert.title,
                    fontSize = if (isCompact) 12.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.description,
                    fontSize = if (isCompact) 10.sp else 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.time,
                    fontSize = if (isCompact) 8.sp else 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}