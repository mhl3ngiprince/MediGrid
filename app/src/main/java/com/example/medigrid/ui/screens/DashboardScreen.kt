package com.example.medigrid.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.data.DataManager
import com.example.medigrid.data.Alert
import com.example.medigrid.data.AlertLevel
import com.example.medigrid.data.StatCard
import com.example.medigrid.ui.components.StatCardComponent
import com.example.medigrid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dataManager = remember { DataManager.getInstance(context) }
    
    // Real-time data
    var stats by remember { mutableStateOf(emptyList<StatCard>()) }
    var alerts by remember { mutableStateOf(emptyList<Alert>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Load real data
    LaunchedEffect(Unit) {
        stats = dataManager.getStats()
        alerts = dataManager.getAlerts().take(3) // Show recent alerts
    }
    
    // Refresh function
    fun refreshData() {
        isRefreshing = true
        stats = dataManager.getStats()
        alerts = dataManager.getAlerts().take(3)
        isRefreshing = false
    }

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
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Healthcare Dashboard",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Real-time network overview",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = { refreshData() }
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh")
                }
            }
        }

        // Statistics Grid
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier.height(240.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stats) { stat ->
                    StatCardComponent(statCard = stat)
                }
            }
        }

        // Recent Alerts Section
        item {
            Text(
                text = "Recent Alerts",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Alert items
        if (alerts.isNotEmpty()) {
            items(alerts) { alert ->
                AlertCard(alert = alert)
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Active Alerts",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "All systems operating normally",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: Alert) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (alert.level) {
                AlertLevel.URGENT -> MaterialTheme.colorScheme.errorContainer
                AlertLevel.WARNING -> MaterialTheme.colorScheme.secondaryContainer
                AlertLevel.INFO -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (alert.level) {
                    AlertLevel.URGENT -> Icons.Default.Warning
                    AlertLevel.WARNING -> Icons.Default.Info
                    AlertLevel.INFO -> Icons.Default.Notifications
                },
                contentDescription = null,
                tint = when (alert.level) {
                    AlertLevel.URGENT -> MaterialTheme.colorScheme.error
                    AlertLevel.WARNING -> MaterialTheme.colorScheme.secondary
                    AlertLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = alert.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${alert.location} • ${alert.time}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}