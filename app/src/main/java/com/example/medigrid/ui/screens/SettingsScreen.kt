package com.example.medigrid.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // System Configuration
        item {
            SystemConfigurationCard()
        }

        // Notification Settings
        item {
            NotificationSettingsCard()
        }

        // API Configuration
        item {
            ApiConfigurationCard()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SystemConfigurationCard() {
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "System Configuration",
                    tint = MediBlue
                )
                Text(
                    text = "System Configuration",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            var orgName by remember { mutableStateOf("MediGrid Healthcare Network") }
            var emergencyContact by remember { mutableStateOf("+27 11 123 4567") }
            var alertThreshold by remember { mutableStateOf("15") }

            // Organization Name
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Organization Name",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = orgName,
                    onValueChange = { orgName = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MediBlue,
                        unfocusedBorderColor = BorderColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Emergency Contact
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Emergency Contact",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MediBlue,
                        unfocusedBorderColor = BorderColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Alert Threshold
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Alert Threshold (Minutes)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = alertThreshold,
                    onValueChange = { alertThreshold = it },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MediBlue,
                        unfocusedBorderColor = BorderColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { /* Save settings */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediBlue
                ),
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text("Save Settings")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsCard() {
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
                text = "Notification Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            var emergencyAlerts by remember { mutableStateOf(true) }
            var powerOutageNotifications by remember { mutableStateOf(true) }
            var medicineStockAlerts by remember { mutableStateOf(true) }
            var dailyReports by remember { mutableStateOf(false) }

            // Emergency Alerts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Emergency Alerts",
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Switch(
                    checked = emergencyAlerts,
                    onCheckedChange = { emergencyAlerts = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MediBlue,
                        checkedTrackColor = MediBlue.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Power Outage Notifications
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Power Outage Notifications",
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Switch(
                    checked = powerOutageNotifications,
                    onCheckedChange = { powerOutageNotifications = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MediBlue,
                        checkedTrackColor = MediBlue.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Medicine Stock Alerts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Medicine Stock Alerts",
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Switch(
                    checked = medicineStockAlerts,
                    onCheckedChange = { medicineStockAlerts = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MediBlue,
                        checkedTrackColor = MediBlue.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Daily Reports
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Reports",
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Switch(
                    checked = dailyReports,
                    onCheckedChange = { dailyReports = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MediBlue,
                        checkedTrackColor = MediBlue.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiConfigurationCard() {
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
                text = "API Configuration",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            var apiEndpoint by remember { mutableStateOf("https://api.medigrid.co.za") }
            var syncInterval by remember { mutableStateOf("15") }

            // API Endpoint
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "API Endpoint",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiEndpoint,
                    onValueChange = { apiEndpoint = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MediBlue,
                        unfocusedBorderColor = BorderColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sync Interval
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Sync Interval (minutes)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                var expanded by remember { mutableStateOf(false) }
                val intervalOptions = listOf("5", "10", "15", "30")

                // Simplified dropdown using Box and DropdownMenu to avoid Experimental API issues
                Box {
                    OutlinedTextField(
                        value = "$syncInterval minutes",
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true },
                        trailingIcon = { Icon(Icons.Filled.Settings, "Select interval") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MediBlue,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        intervalOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("$option minutes") },
                                onClick = {
                                    syncInterval = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Test connection */ },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MediBlue
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Test Connection"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Connection")
                }

                Button(
                    onClick = { /* Save API settings */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MediBlue
                    )
                ) {
                    Text("Save API Settings")
                }
            }
        }
    }
}