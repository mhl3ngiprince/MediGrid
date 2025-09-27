package com.example.medigrid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.data.Clinic
import com.example.medigrid.data.ClinicStatus
import com.example.medigrid.data.SampleData
import com.example.medigrid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicsScreen(
    modifier: Modifier = Modifier,
) {
    var showAddClinicDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var clinicsList by remember { mutableStateOf(SampleData.clinics) }

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
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
                    text = "Healthcare Network Overview",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAddClinicDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MediBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Clinic"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Clinic")
                    }
                    OutlinedButton(
                        onClick = { 
                            isRefreshing = true
                            // Simulate refresh delay
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MediBlue
                        ),
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isRefreshing) "Refreshing..." else "Refresh")
                    }
                }
            }

            // Refresh effect
            LaunchedEffect(isRefreshing) {
                if (isRefreshing) {
                    kotlinx.coroutines.delay(2000) // Simulate API call
                    isRefreshing = false
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Clinics List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(clinicsList) { clinic ->
                    ClinicItem(clinic = clinic)
                }
            }
        }
    }

    // Add Clinic Dialog
    if (showAddClinicDialog) {
        AddClinicDialog(
            onDismiss = { showAddClinicDialog = false },
            onClinicAdded = { clinic: Clinic ->
                showAddClinicDialog = false
                clinicsList = clinicsList + clinic
            }
        )
    }
}

@Composable
private fun ClinicItem(
    clinic: Clinic,
    modifier: Modifier = Modifier,
) {
    val statusColor = when (clinic.status) {
        ClinicStatus.ONLINE -> SuccessGreen
        ClinicStatus.BACKUP -> WarningOrange
        ClinicStatus.OFFLINE -> DangerRed
    }

    val statusText = when (clinic.status) {
        ClinicStatus.ONLINE -> "Online"
        ClinicStatus.BACKUP -> "Backup Power"
        ClinicStatus.OFFLINE -> "Offline"
    }

    val statusBackground = when (clinic.status) {
        ClinicStatus.ONLINE -> SuccessGreen.copy(alpha = 0.1f)
        ClinicStatus.BACKUP -> WarningOrange.copy(alpha = 0.1f)
        ClinicStatus.OFFLINE -> DangerRed.copy(alpha = 0.1f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MediBlue.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status indicator circle
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = statusColor,
                        shape = CircleShape
                    )
            )

            // Clinic information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = clinic.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${clinic.patientsToday} patients today • Staff: ${clinic.staffCount} • Power: ${clinic.powerStatus}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Status badge
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = statusBackground)
            ) {
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClinicDialog(
    onDismiss: () -> Unit,
    onClinicAdded: (Clinic) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var staffCount by remember { mutableStateOf("") }
    var facilityType by remember { mutableStateOf("") }
    var powerSource by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(ClinicStatus.ONLINE) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val facilityTypes = listOf(
        "Community Health Centre",
        "Primary Healthcare Clinic", 
        "District Hospital",
        "Regional Hospital",
        "Specialized Clinic",
        "Mobile Clinic"
    )

    val powerSources = listOf(
        "Grid Connected",
        "Solar + Grid Hybrid",
        "Generator Backup",
        "Battery Backup",
        "Off-Grid Solar"
    )

    val statusOptions = mapOf(
        ClinicStatus.ONLINE to "Online - Fully Operational",
        ClinicStatus.BACKUP to "Backup Power - Limited Operations",
        ClinicStatus.OFFLINE to "Offline - Maintenance Required"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Clinic",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Healthcare Facility")
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

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Facility Name") },
                    placeholder = { Text("Soweto Community Health Centre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location/Address") },
                    placeholder = { Text("123 Main Road, Soweto, Johannesburg") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                // Facility Type Selection
                var typeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = facilityType,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Facility Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        facilityTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    facilityType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = staffCount,
                        onValueChange = { staffCount = it },
                        label = { Text("Staff Count") },
                        placeholder = { Text("25") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    // Power Source Selection
                    var powerExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = powerExpanded,
                        onExpandedChange = { powerExpanded = !powerExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = powerSource,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Power Source") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = powerExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = powerExpanded,
                            onDismissRequest = { powerExpanded = false }
                        ) {
                            powerSources.forEach { source ->
                                DropdownMenuItem(
                                    text = { Text(source) },
                                    onClick = {
                                        powerSource = source
                                        powerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Status Selection
                var statusExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded }
                ) {
                    OutlinedTextField(
                        value = statusOptions[selectedStatus] ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Operational Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        statusOptions.forEach { (status, displayName) ->
                            DropdownMenuItem(
                                text = { Text(displayName) },
                                onClick = {
                                    selectedStatus = status
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "🏥 This facility will be added to the healthcare network and monitored for power status and patient capacity.",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || location.isBlank() || facilityType.isBlank()) {
                        errorMessage = "Please fill in all required fields"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = ""

                    // Create new clinic
                    val newClinic = Clinic(
                        id = "C${System.currentTimeMillis()}",
                        name = name,
                        patientsToday = (5..50).random(),
                        staffCount = staffCount.toIntOrNull() ?: (5..30).random(),
                        powerStatus = powerSource.ifBlank { "Grid Connected" },
                        status = selectedStatus
                    )

                    onClinicAdded(newClinic)
                    isLoading = false
                },
                enabled = !isLoading && name.isNotBlank() && location.isNotBlank() && facilityType.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Add Facility")
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