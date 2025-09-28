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
import com.example.medigrid.data.RealTimeDataService
import com.example.medigrid.data.formatTimestamp
import com.example.medigrid.security.*
import com.example.medigrid.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientsScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dataManager = remember { DataManager.getInstance(context) }
    val realTimeService = remember { RealTimeDataService.getInstance(context) }
    val configuration = LocalConfiguration.current

    // Real-time data integration
    val systemStats by realTimeService.systemStats.collectAsState()
    val patientActivity by realTimeService.patientActivity.collectAsState()
    val emergencyAlerts by realTimeService.emergencyAlerts.collectAsState()
    val healthMetrics by realTimeService.healthMetrics.collectAsState()
    val clinicStatus by realTimeService.clinicStatus.collectAsState()

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

    // Real-time patient data
    var patients by remember { mutableStateOf(emptyList<Patient>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var lastUpdateTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Auto-refresh patient data every 30 seconds
    LaunchedEffect(Unit) {
        while (true) {
            patients = dataManager.getPatients()
            lastUpdateTime = System.currentTimeMillis()
            delay(30000) // 30 seconds
        }
    }

    // Initial data load
    LaunchedEffect(Unit) {
        patients = dataManager.getPatients()
    }

    // Refresh function
    fun refreshData() {
        isRefreshing = true
        patients = dataManager.getPatients()
        lastUpdateTime = System.currentTimeMillis()
        
        try {
            SecurityLogger.logSecurityEvent(
                "patients_data_refreshed",
                mapOf(
                    "user_id" to currentUser.id,
                    "patients_count" to patients.size.toString(),
                    "active_users" to systemStats.activeUsers.toString()
                ),
                context
            )
        } catch (e: Exception) {
            // Handle logging error gracefully
        }
        
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
        // Enhanced Header with Real-Time Data
        item {
            ResponsivePatientsHeader(
                title = "Patient Management",
                subtitle = "${filteredPatients.size} patients • ${systemStats.activeUsers} users active",
                onRefresh = { refreshData() },
                onAdd = { showAddDialog = true },
                isRefreshing = isRefreshing,
                isTablet = isTablet,
                systemLoad = systemStats.cpuUsage,
                lastUpdate = lastUpdateTime
            )
        }

        // Real-Time System Status
        item {
            RealTimeSystemStatusCard(
                systemStats = systemStats,
                healthMetrics = healthMetrics,
                isTablet = isTablet
            )
        }

        // Emergency Alerts for Patients
        if (emergencyAlerts.isNotEmpty()) {
            item {
                EmergencyAlertsCard(
                    alerts = emergencyAlerts,
                    isTablet = isTablet
                )
            }
        }

        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Patients") },
                placeholder = { Text("Name, clinic, or status...") },
                supportingText = { 
                    Text(
                        text = if (searchQuery.isNotBlank()) 
                            "Found ${filteredPatients.size} patient${if (filteredPatients.size == 1) "" else "s"}"
                        else "Search by patient name, clinic, or status ↓",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Green, CircleShape)
                        )
                        Text(
                            text = "LIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { /* Handle search action */ }
                )
            )
        }

        // Enhanced stats with real-time data
        item {
            ResponsivePatientsStats(
                patients = patients,
                healthMetrics = healthMetrics,
                clinicStatus = clinicStatus,
                isTablet = isTablet
            )
        }

        // Real-Time Patient Activity
        item {
            LivePatientActivityCard(
                patientActivity = patientActivity,
                isTablet = isTablet
            )
        }

        // Patients list with live updates
        if (filteredPatients.isNotEmpty()) {
            items(filteredPatients) { patient ->
                ResponsivePatientCard(
                    patient = patient,
                    isTablet = isTablet,
                    onClick = { /* Navigate to patient details */ },
                    isLiveUpdate = patientActivity.any { it.patientId == patient.id }
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
private fun RealTimeSystemStatusCard(
    systemStats: com.example.medigrid.data.SystemStats,
    healthMetrics: com.example.medigrid.data.HealthMetrics,
    isTablet: Boolean
) {
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
        Column(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MonitorHeart,
                    contentDescription = "System Status",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Live System Status",
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Green, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SystemStatItem("CPU", "${systemStats.cpuUsage}%")
                    SystemStatItem("Memory", "${systemStats.memoryUsage}%")
                    SystemStatItem("Users", systemStats.activeUsers.toString())
                    SystemStatItem("Wait Time", "${healthMetrics.averageWaitTime}min")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { SystemStatItem("CPU", "${systemStats.cpuUsage}%") }
                    item { SystemStatItem("Memory", "${systemStats.memoryUsage}%") }
                    item { SystemStatItem("Users", systemStats.activeUsers.toString()) }
                    item { SystemStatItem("Wait", "${healthMetrics.averageWaitTime}min") }
                }
            }
        }
    }
}

@Composable
private fun SystemStatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun EmergencyAlertsCard(
    alerts: List<com.example.medigrid.data.EmergencyAlert>,
    isTablet: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Emergency",
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Active Emergency Alerts (${alerts.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            alerts.take(if (isTablet) 4 else 2).forEach { alert ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = when (alert.type) {
                            "CARDIAC_ARREST" -> Icons.Default.Favorite
                            "TRAUMA" -> Icons.Default.Warning
                            else -> Icons.Default.Emergency
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${alert.type.replace('_', ' ')} - ${alert.severity}",
                            fontSize = if (isTablet) 13.sp else 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "${alert.location} • ${formatTimestamp(alert.timestamp)}",
                            fontSize = if (isTablet) 11.sp else 9.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    if (alert.status == "ACTIVE") {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Red, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LivePatientActivityCard(
    patientActivity: List<com.example.medigrid.data.PatientActivity>,
    isTablet: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Activity",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Live Patient Activity",
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Green, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            patientActivity.take(if (isTablet) 5 else 3).forEach { activity ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = when (activity.department) {
                            "Emergency" -> Icons.Default.LocalHospital
                            "Cardiology" -> Icons.Default.Favorite
                            "Pharmacy" -> Icons.Default.LocalPharmacy
                            "Laboratory" -> Icons.Default.Science
                            else -> Icons.Default.Person
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${activity.initials} - ${activity.activity}",
                            fontSize = if (isTablet) 13.sp else 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${activity.department} • ${formatTimestamp(activity.timestamp)}",
                            fontSize = if (isTablet) 11.sp else 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResponsivePatientsHeader(
    title: String,
    subtitle: String,
    onRefresh: () -> Unit,
    onAdd: () -> Unit,
    isRefreshing: Boolean,
    isTablet: Boolean,
    systemLoad: Int = 0,
    lastUpdate: Long
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
                Text(
                    text = "System Load: $systemLoad% • Updated ${formatTimestamp(lastUpdate)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
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

                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }

            Text(
                text = "System Load: $systemLoad% • Last update: ${formatTimestamp(lastUpdate)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
    healthMetrics: com.example.medigrid.data.HealthMetrics,
    clinicStatus: Map<String, com.example.medigrid.data.RealTimeClinicStatus>,
    isTablet: Boolean
) {
    val activePatients = patients.count { it.status.contains("Active", ignoreCase = true) }
    val criticalPatients = patients.count { it.status.contains("Critical", ignoreCase = true) }
    val followUpPatients = patients.count { it.status.contains("Follow", ignoreCase = true) }
    val totalClinicPatients = clinicStatus.values.sumOf { it.patientCount }

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
                StatItem("Critical", "${healthMetrics.criticalCases}", Icons.Default.Warning)
                StatItem("Network Total", totalClinicPatients.toString(), Icons.Default.LocationOn)
                StatItem("Follow-up", followUpPatients.toString(), Icons.Default.Schedule)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(140.dp).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { StatItem("Total", patients.size.toString(), Icons.Default.Person) }
                item { StatItem("Active", activePatients.toString(), Icons.Default.CheckCircle) }
                item { StatItem("Critical", "${healthMetrics.criticalCases}", Icons.Default.Warning) }
                item { StatItem("Network", totalClinicPatients.toString(), Icons.Default.LocationOn) }
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
    onClick: () -> Unit,
    isLiveUpdate: Boolean
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
            if (isLiveUpdate) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Green, CircleShape)
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
    var errorMessage by remember { mutableStateOf("") }

    // Validation helpers
    val isNameValid = name.trim().length >= 2
    val isAgeValid = age.toIntOrNull()?.let { it in 1..120 } == true
    val isClinicValid = clinic.trim().isNotBlank()
    val isFormValid = isNameValid && isAgeValid && isClinicValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add Patient",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Add New Patient")
            }
        },
        text = {
            Column(
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
                            modifier = androidx.compose.ui.Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        errorMessage = ""
                    },
                    label = { Text("Patient Full Name") },
                    placeholder = { Text("John Doe") },
                    supportingText = { 
                        Text(
                            text = if (name.isNotBlank()) {
                                if (isNameValid) "✓ Valid name" else "⚠ Name too short (min 2 characters)"
                            } else "Enter the patient's full name ↓",
                            color = if (name.isNotBlank()) {
                                if (isNameValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            } else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Patient Name",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (isNameValid) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid name",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = androidx.compose.ui.Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words
                    ),
                    isError = name.isNotBlank() && !isNameValid
                )

                OutlinedTextField(
                    value = age,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() } && it.length <= 3) {
                            age = it
                            errorMessage = ""
                        }
                    },
                    label = { Text("Age") },
                    placeholder = { Text("25") },
                    supportingText = { 
                        Text(
                            text = if (age.isNotBlank()) {
                                if (isAgeValid) "✓ Valid age" else "⚠ Age must be between 1-120"
                            } else "Enter patient's age (1-120) ↓",
                            color = if (age.isNotBlank()) {
                                if (isAgeValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            } else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = "Age",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (isAgeValid) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid age",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = androidx.compose.ui.Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                    ),
                    isError = age.isNotBlank() && !isAgeValid
                )

                OutlinedTextField(
                    value = clinic,
                    onValueChange = { 
                        clinic = it
                        errorMessage = ""
                    },
                    label = { Text("Clinic/Hospital") },
                    placeholder = { Text("General Hospital") },
                    supportingText = { 
                        Text(
                            text = if (clinic.isNotBlank()) {
                                if (isClinicValid) "✓ Valid clinic name" else "⚠ Clinic name required"
                            } else "Enter the clinic or hospital name ↓",
                            color = if (clinic.isNotBlank()) {
                                if (isClinicValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            } else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = "Clinic",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (isClinicValid) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid clinic",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = androidx.compose.ui.Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words
                    ),
                    isError = clinic.isNotBlank() && !isClinicValid
                )

                // Status Selection
                Text(
                    text = "Patient Status",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Active" to Icons.Default.CheckCircle,
                        "Follow-up" to Icons.Default.Schedule,
                        "Critical" to Icons.Default.Warning
                    ).forEach { (statusOption, icon) ->
                        FilterChip(
                            onClick = { status = statusOption },
                            label = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = androidx.compose.ui.Modifier.size(14.dp)
                                    )
                                    Text(statusOption, fontSize = 12.sp)
                                }
                            },
                            selected = status == statusOption,
                            modifier = androidx.compose.ui.Modifier.weight(1f)
                        )
                    }
                }

                // Summary card
                if (isFormValid) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = androidx.compose.ui.Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "✓ Ready to add patient",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "$name, age $age, at $clinic",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid) {
                        try {
                            val newPatient = Patient(
                                id = "patient_${System.currentTimeMillis()}",
                                name = name.trim(),
                                age = age.toInt(),
                                clinic = clinic.trim(),
                                lastVisit = "Today",
                                status = status
                            )
                            onAddPatient(newPatient)
                        } catch (e: Exception) {
                            errorMessage = "Failed to create patient record. Please try again."
                        }
                    } else {
                        errorMessage = "Please fill in all required fields correctly."
                    }
                },
                enabled = isFormValid
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = androidx.compose.ui.Modifier.size(16.dp)
                )
                Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
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