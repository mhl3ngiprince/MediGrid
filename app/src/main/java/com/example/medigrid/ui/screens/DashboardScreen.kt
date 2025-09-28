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
import com.example.medigrid.data.SouthAfricanHealthcareFacilities
import com.example.medigrid.data.ClinicStatus
import com.example.medigrid.data.RealTimeDataService
import com.example.medigrid.data.formatTimestamp
import com.example.medigrid.data.formatDateTime
import com.example.medigrid.ui.components.StatCardComponent
import com.example.medigrid.ui.theme.*
import com.example.medigrid.security.SecurityLogger
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dataManager = remember { DataManager.getInstance(context) }
    val realTimeService = remember { RealTimeDataService.getInstance(context) }

    // Real-time data states
    val systemStats by realTimeService.systemStats.collectAsState()
    val networkActivity by realTimeService.networkActivity.collectAsState()
    val patientActivity by realTimeService.patientActivity.collectAsState()
    val emergencyAlerts by realTimeService.emergencyAlerts.collectAsState()
    val inventoryUpdates by realTimeService.inventoryUpdates.collectAsState()
    val clinicStatus by realTimeService.clinicStatus.collectAsState()
    val powerStatus by realTimeService.powerStatus.collectAsState()
    val healthMetrics by realTimeService.healthMetrics.collectAsState()

    // Legacy states for compatibility
    var stats by remember { mutableStateOf(emptyList<StatCard>()) }
    var alerts by remember { mutableStateOf(emptyList<Alert>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var facilities by remember { mutableStateOf(emptyList<com.example.medigrid.data.HealthcareFacility>()) }
    var networkStatus by remember { mutableStateOf("All Systems Operational") }
    var lastUpdateTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Real-time network status based on live data
    LaunchedEffect(systemStats, clinicStatus) {
        val activeUsers = systemStats.activeUsers
        val onlineClinics = clinicStatus.values.count { it.status == "OPERATIONAL" }
        val totalClinics = clinicStatus.size.coerceAtLeast(1)
        
        networkStatus = when {
            onlineClinics == totalClinics && systemStats.cpuUsage < 70 -> "All Systems Operational"
            onlineClinics >= (totalClinics * 0.9) -> "Network Stable - ${activeUsers} users active"
            onlineClinics >= (totalClinics * 0.7) -> "Minor Issues Detected - Monitoring"
            else -> "Network Issues - Multiple Facilities Offline"
        }
    }

    // Generate enhanced real-time statistics
    LaunchedEffect(healthMetrics, systemStats, clinicStatus) {
        stats = listOf(
            StatCard(
                title = "Active Users",
                value = systemStats.activeUsers.toString(),
                change = "Live count",
                isPositive = true,
                icon = Icons.Default.Person
            ),
            StatCard(
                title = "Total Patients",
                value = healthMetrics.totalPatients.toString(),
                change = "+${healthMetrics.dailyAdmissions} today",
                isPositive = true,
                icon = Icons.Default.People
            ),
            StatCard(
                title = "Clinic Network",
                value = clinicStatus.values.count { it.status == "OPERATIONAL" }.toString(),
                change = "/${clinicStatus.size} online",
                isPositive = true,
                icon = Icons.Default.LocationOn
            ),
            StatCard(
                title = "Critical Cases",
                value = healthMetrics.criticalCases.toString(),
                change = "Monitoring",
                isPositive = false,
                icon = Icons.Default.Warning
            ),
            StatCard(
                title = "Bed Occupancy",
                value = "${healthMetrics.bedOccupancyRate}%",
                change = "Current rate",
                isPositive = healthMetrics.bedOccupancyRate < 85,
                icon = Icons.Default.LocalHospital
            ),
            StatCard(
                title = "Response Time",
                value = "${healthMetrics.emergencyResponseTime}min",
                change = "Average",
                isPositive = healthMetrics.emergencyResponseTime < 15,
                icon = Icons.Default.Timer
            )
        )
    }

    // Auto-refresh compatibility data
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000) // 30 seconds
            if (!isRefreshing) {
                lastUpdateTime = System.currentTimeMillis()
                alerts = dataManager.getAlerts().take(3)
                facilities = SouthAfricanHealthcareFacilities.getAllFacilities()
                
                try {
                    SecurityLogger.logSecurityEvent(
                        "dashboard_realtime_update",
                        mapOf(
                            "active_users" to systemStats.activeUsers.toString(),
                            "network_activities" to networkActivity.size.toString(),
                            "emergency_alerts" to emergencyAlerts.size.toString(),
                            "system_cpu" to systemStats.cpuUsage.toString(),
                            "power_status" to powerStatus.nationalGridStatus
                        ),
                        context
                    )
                } catch (e: Exception) {
                    // Handle logging error gracefully
                }
            }
        }
    }

    // Manual refresh
    fun refreshData() {
        isRefreshing = true
        lastUpdateTime = System.currentTimeMillis()
        // The real-time service automatically updates, so we just need to refresh legacy data
        alerts = dataManager.getAlerts().take(3)
        facilities = SouthAfricanHealthcareFacilities.getAllFacilities()
        isRefreshing = false
    }

    // Layout & responsive
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp
    val gridColumns = when {
        screenWidth > 1200 -> 4 // Large screens
        screenWidth > 800 -> 3
        screenWidth > 600 -> 2
        else -> if (isLandscape) 2 else 1
    }
    val contentPadding = if (screenWidth < 600) 8.dp else 16.dp
    val itemSpacing = if (screenWidth < 600) 12.dp else 24.dp

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        // ENHANCED HEADER WITH REAL-TIME INFO
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
                        text = "Live network overview • ${systemStats.activeUsers} users active • Updated ${formatDateTime(systemStats.lastUpdated)}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    FilledTonalButton(
                        onClick = { refreshData() },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isRefreshing) "Refreshing..." else "Refresh")
                    }
                    
                    Text(
                        text = "CPU: ${systemStats.cpuUsage}% | Latency: ${systemStats.networkLatency}ms",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // ENHANCED SYSTEM STATUS WITH REAL-TIME DATA
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        systemStats.cpuUsage > 80 -> MaterialTheme.colorScheme.errorContainer
                        systemStats.cpuUsage > 60 -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MonitorHeart, 
                            contentDescription = null, 
                            tint = when {
                                systemStats.cpuUsage > 80 -> MaterialTheme.colorScheme.error
                                systemStats.cpuUsage > 60 -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = networkStatus,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sessions: ${systemStats.totalSessions} | Memory: ${systemStats.memoryUsage}%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Live indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Green, androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // ENHANCED STATISTICS GRID WITH REAL-TIME DATA
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier.height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stats) { stat ->
                    StatCardComponent(statCard = stat)
                }
            }
        }

        // REAL-TIME POWER STATUS
        item {
            PowerStatusCard(powerStatus = powerStatus)
        }

        // REAL-TIME NETWORK ACTIVITY
        item {
            Text(
                text = "Live Network Activity",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    networkActivity.take(5).forEach { activity ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = when (activity.status) {
                                    "ACTIVE" -> Icons.Default.Circle
                                    "COMPLETED" -> Icons.Default.CheckCircle
                                    "URGENT" -> Icons.Default.Warning
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = when (activity.status) {
                                    "ACTIVE" -> Color.Green
                                    "COMPLETED" -> MaterialTheme.colorScheme.primary
                                    "URGENT" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${activity.location} - ${activity.activity}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${activity.status} • ${formatTimestamp(activity.timestamp)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // REAL-TIME CLINIC STATUS OVERVIEW
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Online Facilities
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Online",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${clinicStatus.values.count { it.status == "OPERATIONAL" }}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Online Clinics",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Total Patients
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = "Patients",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${clinicStatus.values.sumOf { it.patientCount }}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Current Patients",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Available Beds
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Hotel,
                            contentDescription = "Beds",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${clinicStatus.values.sumOf { it.bedAvailability }}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Available Beds",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // REAL-TIME EMERGENCY ALERTS
        item {
            Text(
                text = "Live Emergency Alerts",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (emergencyAlerts.isNotEmpty()) {
            items(emergencyAlerts.take(3)) { alert ->
                RealTimeAlertCard(alert = alert)
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
                                text = "No Active Emergency Alerts",
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

        // REAL-TIME PATIENT ACTIVITY
        item {
            Text(
                text = "Recent Patient Activity",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    patientActivity.take(4).forEach { activity ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = when (activity.department) {
                                    "Emergency" -> Icons.Default.LocalHospital
                                    "Cardiology" -> Icons.Default.Favorite
                                    "Pharmacy" -> Icons.Default.LocalPharmacy
                                    else -> Icons.Default.Person
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${activity.initials} - ${activity.activity}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${activity.department} • ${formatTimestamp(activity.timestamp)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // QUICK ACTIONS (Enhanced with live data)
        item {
            Text(
                text = "Quick Actions",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (screenWidth > 600) 3 else 2),
                modifier = Modifier.height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    QuickActionCard(
                        title = "Emergency Alerts",
                        subtitle = "${emergencyAlerts.size} active",
                        icon = Icons.Default.Warning,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                item {
                    QuickActionCard(
                        title = "Network Map",
                        subtitle = "${clinicStatus.size} facilities",
                        icon = Icons.Default.Map,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    QuickActionCard(
                        title = "Power Status",
                        subtitle = "Stage ${powerStatus.loadSheddingStage}",
                        icon = Icons.Default.PowerOff,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                item {
                    QuickActionCard(
                        title = "Symptom Checker",
                        subtitle = "AI Assessment",
                        icon = Icons.Default.Psychology,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                item {
                    QuickActionCard(
                        title = "Inventory",
                        subtitle = "${inventoryUpdates.count { it.status == "CRITICAL" }} critical",
                        icon = Icons.Default.Inventory,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                item {
                    QuickActionCard(
                        title = "Analytics",
                        subtitle = "Live trends",
                        icon = Icons.Default.Analytics,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// New real-time components

@Composable
private fun PowerStatusCard(powerStatus: com.example.medigrid.data.PowerStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                powerStatus.loadSheddingStage >= 4 -> MaterialTheme.colorScheme.errorContainer
                powerStatus.loadSheddingStage >= 2 -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PowerOff,
                contentDescription = null,
                tint = when {
                    powerStatus.loadSheddingStage >= 4 -> MaterialTheme.colorScheme.error
                    powerStatus.loadSheddingStage >= 2 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Power Status: ${powerStatus.nationalGridStatus}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Load Shedding Stage ${powerStatus.loadSheddingStage} • ${powerStatus.affectedClinics} facilities affected",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${powerStatus.batteryLevel}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RealTimeAlertCard(alert: com.example.medigrid.data.EmergencyAlert) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (alert.severity) {
                "CRITICAL" -> MaterialTheme.colorScheme.errorContainer
                "HIGH" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                "MODERATE" -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (alert.type) {
                    "CARDIAC_ARREST" -> Icons.Default.Favorite
                    "TRAUMA" -> Icons.Default.Warning
                    "STROKE" -> Icons.Default.Psychology
                    "RESPIRATORY_FAILURE" -> Icons.Default.Air
                    else -> Icons.Default.Emergency
                },
                contentDescription = null,
                tint = when (alert.severity) {
                    "CRITICAL" -> MaterialTheme.colorScheme.error
                    "HIGH" -> MaterialTheme.colorScheme.error
                    "MODERATE" -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${alert.type.replace('_', ' ')} - ${alert.severity}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = alert.location,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${alert.status} • ${formatTimestamp(alert.timestamp)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Live indicator for active alerts
            if (alert.status == "ACTIVE") {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Red, androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    }
}

// Legacy components for compatibility

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

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = color
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}