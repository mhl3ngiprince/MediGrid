package com.example.medigrid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.data.Patient
import com.example.medigrid.data.SampleData
import com.example.medigrid.ui.theme.*
import com.example.medigrid.security.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientsScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showAddPatientDialog by remember { mutableStateOf(false) }

    // This would normally get the current user from a proper state management system
    // For demo purposes, we'll create a sample user
    val currentUser = remember {
        HealthcareAuthService.HealthcareUser(
            id = "demo_user",
            username = "demo.user",
            role = SecurityConfig.HealthcareRole.DOCTOR,
            clinicId = "clinic_001",
            phiAccessLevel = "FULL",
            mfaEnabled = true
        )
    }

    // Log PHI access
    LaunchedEffect(Unit) {
        SecurityLogger.logPhiAccess(
            currentUser.id,
            "patient_list",
            "view_list",
            "Accessed patient management screen",
            context
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Security header card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Patient Management",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Secure",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PHI Protected • POPIA Compliant",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Role-based access control for add button
                        if (currentUser.role.permissions.contains("WRITE_PHI")) {
                            Button(
                                onClick = {
                                    SecurityLogger.logSecurityEvent(
                                        "add_patient_initiated",
                                        mapOf("user_id" to currentUser.id),
                                        context
                                    )
                                    showAddPatientDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add Patient"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Patient")
                            }
                        } else {
                            // Show disabled button with tooltip for insufficient permissions
                            OutlinedButton(
                                onClick = { },
                                enabled = false
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Restricted"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Patient")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // User role and permissions display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Role",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Role: ${currentUser.role.name}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "${currentUser.role.permissions.size} permissions",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // PHI Access Warning Card (for non-doctors)
        if (currentUser.role != SecurityConfig.HealthcareRole.DOCTOR) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Limited PHI access based on your role. All access is logged and audited.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Patient list with PHI protection
        items(SampleData.patients) { patient ->
            SecurePatientItem(
                patient = patient,
                currentUser = currentUser,
                onPatientClick = { patientId ->
                    SecurityLogger.logPhiAccess(
                        currentUser.id,
                        patientId,
                        "view_details",
                        "Clicked on patient details",
                        context
                    )
                }
            )
        }

        // Footer with compliance info
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
                    Text(
                        text = "Data Protection Notice",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All patient health information is protected under POPIA. " +
                                "Access is logged, encrypted, and monitored for compliance. " +
                                "Unauthorized access may result in legal action.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    // Add Patient Dialog
    if (showAddPatientDialog) {
        AddPatientFormDialog(
            currentUser = currentUser,
            onDismiss = { showAddPatientDialog = false },
            onPatientAdded = { patient ->
                showAddPatientDialog = false
                // In a real app, this would add to database/API
                SecurityLogger.logSecurityEvent(
                    "patient_added",
                    mapOf(
                        "user_id" to currentUser.id,
                        "patient_id" to patient.id,
                        "clinic_id" to currentUser.clinicId
                    ),
                    context
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientFormDialog(
    currentUser: HealthcareAuthService.HealthcareUser,
    onDismiss: () -> Unit,
    onPatientAdded: (Patient) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var medicalHistory by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("Active") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val statusOptions = listOf("Active", "Follow-up", "Critical", "Discharged")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Patient",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Patient")
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
                    label = { Text("Full Name") },
                    placeholder = { Text("John Doe") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
                    placeholder = { Text("25") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+27 81 234 5678") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    placeholder = { Text("123 Main St, City") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                OutlinedTextField(
                    value = medicalHistory,
                    onValueChange = { medicalHistory = it },
                    label = { Text("Medical History (Optional)") },
                    placeholder = { Text("Previous conditions, allergies...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Status Selection
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedStatus,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        statusOptions.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    selectedStatus = status
                                    expanded = false
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
                        text = "🔒 Patient data is encrypted and POPIA compliant. Access logged for ${currentUser.username}.",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || age.isBlank() || phone.isBlank()) {
                        errorMessage = "Please fill in all required fields"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = ""

                    // Create new patient
                    val newPatient = Patient(
                        id = "P${System.currentTimeMillis()}",
                        name = name,
                        age = age.toIntOrNull() ?: 0,
                        status = selectedStatus,
                        lastVisit = "Today",
                        clinic = currentUser.clinicId
                    )

                    onPatientAdded(newPatient)
                    isLoading = false
                },
                enabled = !isLoading && name.isNotBlank() && age.isNotBlank() && phone.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Add Patient")
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
private fun SecurePatientItem(
    patient: Patient,
    currentUser: HealthcareAuthService.HealthcareUser,
    onPatientClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor = when (patient.status) {
        "Active" -> SuccessGreen
        "Follow-up" -> WarningOrange
        "Critical" -> DangerRed
        else -> TextSecondary
    }

    val statusBackground = when (patient.status) {
        "Active" -> SuccessGreen.copy(alpha = 0.1f)
        "Follow-up" -> WarningOrange.copy(alpha = 0.1f)
        "Critical" -> DangerRed.copy(alpha = 0.1f)
        else -> TextSecondary.copy(alpha = 0.1f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        onClick = { onPatientClick(patient.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Patient icon with security indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MediBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Patient",
                    tint = MediBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Patient information with PHI masking for certain roles
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        // Show full name only for authorized roles
                        val displayName = if (currentUser.role.permissions.contains("READ_PHI")) {
                            "${patient.name} (${patient.id})"
                        } else {
                            "Patient ${patient.id.takeLast(4)}" // Show only last 4 digits for privacy
                        }

                        Text(
                            text = displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Age: ${patient.age} • ${patient.clinic}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Last visit: ${patient.lastVisit}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // PHI protection indicator
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "PHI Protected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )

                        // Status badge
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = statusBackground)
                        ) {
                            Text(
                                text = patient.status,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}