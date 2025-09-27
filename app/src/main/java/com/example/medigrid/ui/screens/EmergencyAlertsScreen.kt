package com.example.medigrid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
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
import com.example.medigrid.security.FirebaseDataService
import com.example.medigrid.security.SecurityLogger
import com.example.medigrid.data.DataManager
import com.example.medigrid.data.StatCard
import com.example.medigrid.ui.components.StatCardComponent
import com.example.medigrid.ui.theme.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyAlertsScreen(
    modifier: Modifier = Modifier
) {
    var showNewAlertDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dataManager = remember { DataManager.getInstance(context) }
    
    // Firebase integration for real-time alerts (optional)
    val firebaseDataService = remember { FirebaseDataService.getInstance(context) }
    val connectionStatus by firebaseDataService.connectionStatus.collectAsState()
    
    // Log Firebase connection status
    LaunchedEffect(connectionStatus) {
        if (connectionStatus) {
            SecurityLogger.logSecurityEvent(
                "emergency_screen_firebase_connected",
                mapOf("screen" to "emergency_alerts"),
                context
            )
        }
    }

    // Real-time data
    var alerts by remember { mutableStateOf(dataManager.getAlerts()) }
    var selectedFilter by remember { mutableStateOf(AlertLevel.URGENT) }
    var showAddAlert by remember { mutableStateOf(false) }
    var showFilterOptions by remember { mutableStateOf(false) }
    
    // Real-time Firebase synchronization
    val emergencyAlerts by firebaseDataService.emergencyAlerts.collectAsState()
    
    // Convert Firebase data to Alert objects
    val firebaseAlertsConverted = emergencyAlerts.mapNotNull { alertMap ->
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
    
    // Use Firebase data if available, otherwise fall back to local data
    val displayAlerts = if (firebaseAlertsConverted.isNotEmpty()) firebaseAlertsConverted else alerts

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            EmergencyHeader(
                alertCount = displayAlerts.count { it.level == AlertLevel.URGENT },
                onAddAlert = { showNewAlertDialog = true },
                onFilter = { showFilterOptions = true }
            )
        }

        // Statistics
        item {
            Text(
                text = "Emergency Statistics",
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
                val stats = getEmergencyStats(displayAlerts)
                items(stats.size) { index ->
                    StatCardComponent(statCard = stats[index])
                }
            }
        }

        // Active Alerts
        item {
            Text(
                text = "Active Alerts",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (displayAlerts.isNotEmpty()) {
            items(displayAlerts.size) { index ->
                EmergencyAlertCard(alert = displayAlerts[index])
            }
        } else {
            item {
                EmptyAlertsCard()
            }
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
    onAddAlert: () -> Unit,
    onFilter: () -> Unit
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
                text = "$alertCount active emergencies",
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
        }
    }
}

@Composable
private fun EmergencyAlertCard(alert: Alert) {
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

private fun getEmergencyStats(alerts: List<Alert>) = listOf(
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
    )
)
