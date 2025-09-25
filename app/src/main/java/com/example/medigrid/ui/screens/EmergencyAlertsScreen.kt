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
import androidx.compose.runtime.Composable
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
                                onClick = { /* New alert */ },
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