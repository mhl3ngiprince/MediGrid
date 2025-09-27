package com.example.medigrid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.data.DataManager
import com.example.medigrid.data.Patient
import com.example.medigrid.security.*
import com.example.medigrid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientsScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dataManager = remember { DataManager.getInstance(context) }
    val configuration = LocalConfiguration.current

    // Create a demo user for authentication context
    val currentUser = remember {
        HealthcareAuthService.HealthcareUser(
            id = "user_${System.currentTimeMillis()}",
            username = "healthcare.user",
            role = SecurityConfig.HealthcareRole.NURSE,
            clinicId = "clinic_001",
            phiAccessLevel = "BASIC",
            mfaEnabled = true
        )
    }

    // Real-time data
    var patients by remember { mutableStateOf(emptyList<Patient>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Load real data
    LaunchedEffect(Unit) {
        patients = dataManager.getPatients()
    }

    // Refresh function
    fun refreshData() {
        isRefreshing = true
        patients = dataManager.getPatients()
        isRefreshing = false
    }

    // Filter patients based on search query
    val filteredPatients = if (searchQuery.isBlank()) {
        patients
    } else {
        patients.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.clinic.contains(searchQuery, ignoreCase = true) ||
                    it.status.contains(searchQuery, ignoreCase = true)
        }
    }

    // Responsive layout
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            ResponsivePatientsHeader(
                title = "Patient Management",
                subtitle = "${filteredPatients.size} patients",
                onRefresh = { refreshData() },
                onAdd = { showAddDialog = true },
                isRefreshing = isRefreshing,
                isTablet = isTablet
            )
        }

        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search patients...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Quick stats
        item {
            ResponsivePatientsStats(
                patients = patients,
                isTablet = isTablet
            )
        }

        // Patients list
        if (filteredPatients.isNotEmpty()) {
            items(filteredPatients) { patient ->
                ResponsivePatientCard(
                    patient = patient,
                    isTablet = isTablet,
                    onClick = { /* Navigate to patient details */ }
                )
            }
        } else {
            item {
                EmptyPatientsCard(isTablet = isTablet)
            }
        }
    }

    // Add patient dialog
    if (showAddDialog) {
        AddPatientDialog(
            onDismiss = { showAddDialog = false },
            onAddPatient = { newPatient ->
                dataManager.addPatient(newPatient)
                patients = dataManager.getPatients()
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ResponsivePatientsHeader(
    title: String,
    subtitle: String,
    onRefresh: () -> Unit,
    onAdd: () -> Unit,
    isRefreshing: Boolean,
    isTablet: Boolean
) {
    if (isTablet) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = subtitle,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh")
                }
                Button(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Patient")
                }
            }
        }
    } else {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Patient")
            }
        }
    }
}

@Composable
private fun ResponsivePatientsStats(
    patients: List<Patient>,
    isTablet: Boolean
) {
    val activePatients = patients.count { it.status.contains("Active", ignoreCase = true) }
    val criticalPatients = patients.count { it.status.contains("Critical", ignoreCase = true) }
    val followUpPatients = patients.count { it.status.contains("Follow", ignoreCase = true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        if (isTablet) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Total Patients", patients.size.toString(), Icons.Default.Person)
                StatItem("Active Cases", activePatients.toString(), Icons.Default.CheckCircle)
                StatItem("Critical", criticalPatients.toString(), Icons.Default.Warning)
                StatItem("Follow-up", followUpPatients.toString(), Icons.Default.Schedule)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(120.dp).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { StatItem("Total", patients.size.toString(), Icons.Default.Person) }
                item { StatItem("Active", activePatients.toString(), Icons.Default.CheckCircle) }
                item { StatItem("Critical", criticalPatients.toString(), Icons.Default.Warning) }
                item { StatItem("Follow-up", followUpPatients.toString(), Icons.Default.Schedule) }
            }
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResponsivePatientCard(
    patient: Patient,
    isTablet: Boolean,
    onClick: () -> Unit
) {
    val statusColor = when {
        patient.status.contains("Critical", ignoreCase = true) -> MaterialTheme.colorScheme.error
        patient.status.contains("Active", ignoreCase = true) -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = statusColor,
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patient.name,
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Age: ${patient.age} • ${patient.clinic}",
                    fontSize = if (isTablet) 12.sp else 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Last visit: ${patient.lastVisit}",
                    fontSize = if (isTablet) 12.sp else 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusColor.copy(alpha = 0.1f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = patient.status,
                    fontSize = if (isTablet) 10.sp else 8.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyPatientsCard(isTablet: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 48.dp else 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(if (isTablet) 64.dp else 48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Patients Found",
                    fontSize = if (isTablet) 20.sp else 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Add patients to manage their records",
                    fontSize = if (isTablet) 14.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddPatientDialog(
    onDismiss: () -> Unit,
    onAddPatient: (Patient) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var clinic by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Active") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Patient") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Patient Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = clinic,
                    onValueChange = { clinic = it },
                    label = { Text("Clinic") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Active", "Follow-up", "Critical").forEach { statusOption ->
                        FilterChip(
                            onClick = { status = statusOption },
                            label = { Text(statusOption) },
                            selected = status == statusOption,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && age.isNotBlank() && clinic.isNotBlank()) {
                        val newPatient = Patient(
                            id = "patient_${System.currentTimeMillis()}",
                            name = name,
                            age = age.toIntOrNull() ?: 0,
                            clinic = clinic,
                            lastVisit = "Today",
                            status = status
                        )
                        onAddPatient(newPatient)
                    }
                }
            ) {
                Text("Add Patient")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}