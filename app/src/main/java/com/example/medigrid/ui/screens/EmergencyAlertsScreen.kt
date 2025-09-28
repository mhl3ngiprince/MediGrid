package com.example.medigrid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.medigrid.data.Alert
import com.example.medigrid.data.AlertLevel
import com.example.medigrid.data.RealTimeDataService
import com.example.medigrid.data.formatTimestamp
import com.example.medigrid.security.FirebaseDataService
import com.example.medigrid.security.SecurityLogger
import com.example.medigrid.data.DataManager
import com.example.medigrid.data.StatCard
import com.example.medigrid.ui.components.StatCardComponent
import com.example.medigrid.ui.theme.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyAlertsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataManager = remember { DataManager.getInstance(context) }
    val realTimeService = remember { RealTimeDataService.getInstance(context) }
    val firebaseDataService = remember { FirebaseDataService.getInstance(context) }
    
    // Real-time data integration
    val systemStats by realTimeService.systemStats.collectAsState()
    val emergencyAlerts by realTimeService.emergencyAlerts.collectAsState()
    val healthMetrics by realTimeService.healthMetrics.collectAsState()
    val patientActivity by realTimeService.patientActivity.collectAsState()
    val clinicStatus by realTimeService.clinicStatus.collectAsState()
    val networkActivity by realTimeService.networkActivity.collectAsState()
    
    // Firebase connection status
    val connectionStatus by firebaseDataService.connectionStatus.collectAsState()
    val firebaseEmergencyAlerts by firebaseDataService.emergencyAlerts.collectAsState()
    
    // Local state
    var alerts by remember { mutableStateOf(dataManager.getAlerts()) }
    var selectedFilter by remember { mutableStateOf(AlertLevel.URGENT) }
    var showNewAlertDialog by remember { mutableStateOf(false) }
    var showFilterOptions by remember { mutableStateOf(false) }
    var lastUpdateTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Auto-refresh alerts every 10 seconds
    LaunchedEffect(Unit) {
        while (true) {
            alerts = dataManager.getAlerts()
            lastUpdateTime = System.currentTimeMillis()
            delay(10000) // 10 seconds
        }
    }
    
    // Log Firebase connection status
    LaunchedEffect(connectionStatus) {
        if (connectionStatus) {
            SecurityLogger.logSecurityEvent(
                "emergency_screen_firebase_connected",
                mapOf(
                    "screen" to "emergency_alerts",
                    "live_alerts_count" to emergencyAlerts.size.toString(),
                    "system_load" to systemStats.cpuUsage.toString()
                ),
                context
            )
        }
    }

    // Convert Firebase data to Alert objects
    val firebaseAlertsConverted = firebaseEmergencyAlerts.mapNotNull { alertMap ->
        try {
            Alert(
                id = alertMap["id"] as? String ?: "",
                title = alertMap["title"] as? String ?: "",
                location = alertMap["location"] as? String ?: "",
                description = alertMap["description"] as? String ?: "",
                time = "Real-time",
                level = AlertLevel.valueOf(alertMap["level"] as? String ?: "INFO"),
                isActive = alertMap["isActive"] as? Boolean ?: true
            )
        } catch (e: Exception) { null }
    }
    
    // Combine real-time emergency alerts with Firebase data
    val combinedAlerts = (emergencyAlerts.map { alert ->
        Alert(
            id = alert.id,
            title = "${alert.type.replace('_', ' ')} Emergency",
            description = "${alert.severity} priority emergency response required",
            location = alert.location,
            time = formatTimestamp(alert.timestamp),
            level = when (alert.severity) {
                "CRITICAL" -> AlertLevel.URGENT
                "HIGH" -> AlertLevel.URGENT
                "MODERATE" -> AlertLevel.WARNING
                else -> AlertLevel.INFO
            },
            isActive = alert.status == "ACTIVE"
        )
    } + firebaseAlertsConverted + alerts).distinctBy { it.id }

    // Manual refresh function
    fun refreshData() {
        isRefreshing = true
        alerts = dataManager.getAlerts()
        lastUpdateTime = System.currentTimeMillis()
        
        try {
            SecurityLogger.logSecurityEvent(
                "emergency_alerts_refreshed",
                mapOf(
                    "total_alerts" to combinedAlerts.size.toString(),
                    "urgent_alerts" to combinedAlerts.count { it.level == AlertLevel.URGENT }.toString(),
                    "firebase_connected" to connectionStatus.toString()
                ),
                context
            )
        } catch (e: Exception) {
            // Handle logging error gracefully
        }
        
        isRefreshing = false
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Enhanced Header with Real-Time Data
        item {
            EmergencyHeader(
                alertCount = combinedAlerts.count { it.level == AlertLevel.URGENT },
                totalAlerts = combinedAlerts.size,
                systemStats = systemStats,
                healthMetrics = healthMetrics,
                lastUpdate = lastUpdateTime,
                onAddAlert = { showNewAlertDialog = true },
                onFilter = { showFilterOptions = true },
                onRefresh = { refreshData() },
                isRefreshing = isRefreshing,
                connectionStatus = connectionStatus
            )
        }

        // Real-Time System Status for Emergency Response
        item {
            EmergencySystemStatusCard(
                systemStats = systemStats,
                healthMetrics = healthMetrics,
                clinicStatusMap = clinicStatus
            )
        }

        // Live Emergency Response Metrics
        item {
            EmergencyResponseMetricsCard(
                emergencyAlerts = emergencyAlerts,
                healthMetrics = healthMetrics,
                networkActivity = networkActivity.filter { it.status == "URGENT" }
            )
        }

        // Enhanced statistics with real-time data
        item {
            Text(
                text = "Live Emergency Statistics",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val stats = getEnhancedEmergencyStats(
                    combinedAlerts,
                    emergencyAlerts,
                    healthMetrics,
                    systemStats
                )
                items(stats.size) { index ->
                    StatCardComponent(statCard = stats[index])
                }
            }
        }

        // Active Real-Time Alerts
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Live Emergency Alerts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Red, CircleShape)
                )
                Text(
                    text = "LIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (combinedAlerts.isNotEmpty()) {
            items(combinedAlerts.sortedByDescending { 
                when (it.level) {
                    AlertLevel.URGENT -> 3
                    AlertLevel.WARNING -> 2
                    AlertLevel.INFO -> 1
                }
            }) { alert ->
                EnhancedEmergencyAlertCard(
                    alert = alert,
                    isLiveAlert = emergencyAlerts.any { it.id == alert.id }
                )
            }
        } else {
            item {
                EmptyAlertsCard()
            }
        }

        // Emergency Contact Information
        item {
            EmergencyContactsCard()
        }
    }

    // Add Alert Dialog
    if (showNewAlertDialog) {
        AddEmergencyAlertDialog(
            onDismiss = { showNewAlertDialog = false },
            onAddAlert = { newAlert ->
                dataManager.addAlert(newAlert)
                alerts = dataManager.getAlerts()
                showNewAlertDialog = false
            }
        )
    }
}

@Composable
private fun EmergencyHeader(
    alertCount: Int,
    totalAlerts: Int,
    systemStats: com.example.medigrid.data.SystemStats,
    healthMetrics: com.example.medigrid.data.HealthMetrics,
    lastUpdate: Long,
    onAddAlert: () -> Unit,
    onFilter: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    connectionStatus: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Emergency Alerts",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$alertCount active emergencies out of $totalAlerts",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onFilter) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
            }
            
            Button(onClick = onAddAlert) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Alert")
            }
            
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }
    }
}

@Composable
private fun EnhancedEmergencyAlertCard(
    alert: Alert,
    isLiveAlert: Boolean
) {
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
                    AlertLevel.INFO -> Icons.Default.Info
                },
                contentDescription = null,
                tint = when (alert.level) {
                    AlertLevel.URGENT -> MaterialTheme.colorScheme.error
                    AlertLevel.WARNING -> MaterialTheme.colorScheme.secondary
                    AlertLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
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
            
            if (isLiveAlert) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Red, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun EmptyAlertsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Active Alerts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "All systems are operating normally",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddEmergencyAlertDialog(
    onDismiss: () -> Unit,
    onAddAlert: (Alert) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf(AlertLevel.INFO) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Emergency Alert") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Alert Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AlertLevel.values().forEach { level ->
                        FilterChip(
                            onClick = { selectedLevel = level },
                            label = { Text(level.name) },
                            selected = selectedLevel == level,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank() && location.isNotBlank()) {
                        val newAlert = Alert(
                            id = "alert_${System.currentTimeMillis()}",
                            title = title,
                            description = description,
                            location = location,
                            time = "Just now",
                            level = selectedLevel
                        )
                        onAddAlert(newAlert)
                    }
                }
            ) {
                Text("Add Alert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getEnhancedEmergencyStats(
    alerts: List<Alert>,
    emergencyAlerts: List<com.example.medigrid.data.EmergencyAlert>,
    healthMetrics: com.example.medigrid.data.HealthMetrics,
    systemStats: com.example.medigrid.data.SystemStats,
) = listOf(
    StatCard(
        title = "Active Emergencies",
        value = alerts.count { it.level == AlertLevel.URGENT }.toString(),
        change = "Requires Action",
        isPositive = false,
        icon = Icons.Filled.Warning
    ),
    StatCard(
        title = "Total Alerts",
        value = alerts.size.toString(),
        change = "All levels",
        isPositive = true,
        icon = Icons.Filled.Notifications
    ),
    StatCard(
        title = "Warning Level",
        value = alerts.count { it.level == AlertLevel.WARNING }.toString(),
        change = "Monitor closely",
        isPositive = true,
        icon = Icons.Filled.Info
    ),
    StatCard(
        title = "Info Level",
        value = alerts.count { it.level == AlertLevel.INFO }.toString(),
        change = "Routine updates",
        isPositive = true,
        icon = Icons.Filled.CheckCircle
    ),
    StatCard(
        title = "Live Emergency Alerts",
        value = emergencyAlerts.size.toString(),
        change = "Real-time",
        isPositive = true,
        icon = Icons.Filled.Notifications
    ),
    StatCard(
        title = "System Load",
        value = "${systemStats.cpuUsage}%",
        change = "Current load",
        isPositive = systemStats.cpuUsage < 80,
        icon = Icons.Filled.Info
    )
)

@Composable
private fun EmergencySystemStatusCard(
    systemStats: com.example.medigrid.data.SystemStats,
    healthMetrics: com.example.medigrid.data.HealthMetrics,
    clinicStatusMap: Map<String, com.example.medigrid.data.RealTimeClinicStatus>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "System Status",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "CPU Usage: ${systemStats.cpuUsage}%",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Memory Usage: ${systemStats.memoryUsage}%",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Active Clinics: ${clinicStatusMap.values.count { it.status == "OPERATIONAL" }}/${clinicStatusMap.size}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmergencyResponseMetricsCard(
    emergencyAlerts: List<com.example.medigrid.data.EmergencyAlert>,
    healthMetrics: com.example.medigrid.data.HealthMetrics,
    networkActivity: List<com.example.medigrid.data.NetworkActivity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Emergency Response Metrics",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Response Time: ${healthMetrics.emergencyResponseTime}ms",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Network Activity: ${networkActivity.size} requests",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Emergency Alerts: ${emergencyAlerts.size}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmergencyContactsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Emergency Contacts",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Call 911 for immediate assistance",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Contact clinic administration for non-emergency issues",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
