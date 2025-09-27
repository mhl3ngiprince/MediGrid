package com.example.medigrid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
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
import com.example.medigrid.data.Alert
import com.example.medigrid.data.AlertLevel
import com.example.medigrid.data.SampleData
import com.example.medigrid.data.StatCard
import com.example.medigrid.ui.components.StatCardComponent
import com.example.medigrid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyAlertsScreen(
    modifier: Modifier = Modifier,
) {
    var showNewAlertDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Emergency Statistics
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(280.dp)
            ) {
                items(getEmergencyStats()) { stat ->
                    StatCardComponent(statCard = stat)
                }
            }
        }

        // Active Emergencies Section
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
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Emergency Alert System",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showNewAlertDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MediBlue
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "New Alert"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("New Alert")
                            }
                            OutlinedButton(
                                onClick = { /* View history */ },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MediBlue
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.List,
                                    contentDescription = "View History"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View History")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Emergency Alerts
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Add more detailed emergency alerts
                        getDetailedEmergencyAlerts().forEach { alert ->
                            DetailedEmergencyAlertItem(alert = alert)
                        }
                    }
                }
            }
        }
    }

    // New Alert Dialog
    if (showNewAlertDialog) {
        NewAlertDialog(
            onDismiss = { showNewAlertDialog = false },
            onAlertCreated = { alert ->
                showNewAlertDialog = false
                // In a real app, this would send to emergency system
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAlertDialog(
    onDismiss: () -> Unit,
    onAlertCreated: (DetailedEmergencyAlert) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var patientInfo by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf(AlertLevel.WARNING) }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val levelOptions = mapOf(
        AlertLevel.URGENT to "URGENT - Life Threatening",
        AlertLevel.WARNING to "HIGH - Requires Attention", 
        AlertLevel.INFO to "INFO - General Alert"
    )

    val locationOptions = listOf(
        "Soweto Community Clinic",
        "Alexandra Primary Healthcare", 
        "Orange Farm Community Health",
        "Midrand Medical Centre",
        "Johannesburg General Hospital",
        "Other Location"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "New Alert",
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Emergency Alert")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp
                        )
                    }
                }

                // Alert Level Selection
                var levelExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = levelExpanded,
                    onExpandedChange = { levelExpanded = !levelExpanded }
                ) {
                    OutlinedTextField(
                        value = levelOptions[selectedLevel] ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Alert Level") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = when(selectedLevel) {
                                AlertLevel.URGENT -> DangerRed
                                AlertLevel.WARNING -> WarningOrange
                                AlertLevel.INFO -> MediBlue
                            }
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = levelExpanded,
                        onDismissRequest = { levelExpanded = false }
                    ) {
                        levelOptions.forEach { (level, displayName) ->
                            DropdownMenuItem(
                                text = { Text(displayName) },
                                onClick = {
                                    selectedLevel = level
                                    levelExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Alert Title") },
                    placeholder = { Text("Emergency: Cardiac Event") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Location Selection
                var locationExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = locationExpanded,
                    onExpandedChange = { locationExpanded = !locationExpanded }
                ) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = locationExpanded,
                        onDismissRequest = { locationExpanded = false }
                    ) {
                        locationOptions.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc) },
                                onClick = {
                                    location = loc
                                    locationExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = patientInfo,
                    onValueChange = { patientInfo = it },
                    label = { Text("Patient Information") },
                    placeholder = { Text("Male, 58 years") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = symptoms,
                    onValueChange = { symptoms = it },
                    label = { Text("Symptoms/Situation") },
                    placeholder = { Text("Chest pain, shortness of breath") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Additional Details") },
                    placeholder = { Text("Any additional emergency information...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when(selectedLevel) {
                            AlertLevel.URGENT -> DangerRed.copy(alpha = 0.1f)
                            AlertLevel.WARNING -> WarningOrange.copy(alpha = 0.1f)
                            AlertLevel.INFO -> MediBlue.copy(alpha = 0.1f)
                        }
                    )
                ) {
                    Text(
                        text = "🚨 This alert will be immediately dispatched to emergency responders and medical staff.",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(8.dp),
                        color = when(selectedLevel) {
                            AlertLevel.URGENT -> DangerRed
                            AlertLevel.WARNING -> WarningOrange
                            AlertLevel.INFO -> MediBlue
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || location.isBlank()) {
                        errorMessage = "Please fill in title and location"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = ""

                    // Create new alert
                    val newAlert = DetailedEmergencyAlert(
                        id = "E${System.currentTimeMillis()}",
                        title = title,
                        location = location,
                        patientInfo = patientInfo,
                        symptoms = symptoms,
                        status = "Active",
                        time = "Just now",
                        level = selectedLevel
                    )

                    onAlertCreated(newAlert)
                    isLoading = false
                },
                enabled = !isLoading && title.isNotBlank() && location.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when(selectedLevel) {
                        AlertLevel.URGENT -> DangerRed
                        AlertLevel.WARNING -> WarningOrange
                        AlertLevel.INFO -> MediBlue
                    }
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Create Alert")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DetailedEmergencyAlertItem(
    alert: DetailedEmergencyAlert,
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
        colors = CardDefaults.cardColors(containerColor = alertBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Alert level indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(80.dp)
                    .background(
                        color = alertColor,
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alert.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Location: ${alert.location}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        if (alert.patientInfo.isNotEmpty()) {
                            Text(
                                text = "Patient: ${alert.patientInfo}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                        if (alert.symptoms.isNotEmpty()) {
                            Text(
                                text = "Symptoms: ${alert.symptoms}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${alert.status} • ${alert.time}",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    // Action buttons
                    if (alert.level == AlertLevel.URGENT) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { /* Dispatch */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = alertColor
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "Dispatch",
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                            OutlinedButton(
                                onClick = { /* Details */ },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = alertColor
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "Details",
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class DetailedEmergencyAlert(
    val id: String,
    val title: String,
    val location: String,
    val patientInfo: String,
    val symptoms: String,
    val status: String,
    val time: String,
    val level: AlertLevel,
)

private fun getEmergencyStats() = listOf(
    StatCard("Active Emergencies", "5", "Requires Action", false, Icons.Filled.Warning),
    StatCard("Resolved Today", "23", "Good Response Time", true, Icons.Filled.Check),
    StatCard("Avg Response Time", "12 min", "Under Target", true, Icons.Filled.Person),
    StatCard("Success Rate", "96%", "Excellent Performance", true, Icons.Filled.Check)
)

private fun getDetailedEmergencyAlerts() = listOf(
    DetailedEmergencyAlert(
        "E001",
        "CRITICAL: Cardiac Emergency",
        "Soweto Community Clinic",
        "Male, 58 years",
        "Chest pain, shortness of breath",
        "Active",
        "2 minutes ago",
        AlertLevel.URGENT
    ),
    DetailedEmergencyAlert(
        "E002",
        "URGENT: Labor Emergency",
        "Orange Farm Community Health",
        "Female, 24 years",
        "Complications during delivery",
        "Active",
        "8 minutes ago",
        AlertLevel.URGENT
    ),
    DetailedEmergencyAlert(
        "E003",
        "HIGH: Fever & Seizure",
        "Alexandra Primary Healthcare",
        "Child, 3 years",
        "High fever (39.5°C), seizure activity",
        "Active",
        "15 minutes ago",
        AlertLevel.WARNING
    ),
    DetailedEmergencyAlert(
        "E004",
        "SYSTEM: Power Outage Alert",
        "Midrand Medical Centre",
        "",
        "Battery Level: 87% (6 hours remaining)",
        "Active",
        "22 minutes ago",
        AlertLevel.INFO
    )
)