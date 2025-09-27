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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.data.Clinic
import com.example.medigrid.data.ClinicStatus
import com.example.medigrid.data.DataManager
import com.example.medigrid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataManager = remember { DataManager.getInstance(context) }
    val configuration = LocalConfiguration.current
    
    // Real-time data
    var clinics by remember { mutableStateOf(emptyList<Clinic>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    // Load real data
    LaunchedEffect(Unit) {
        clinics = dataManager.getClinics()
    }
    
    // Refresh function
    fun refreshData() {
        isRefreshing = true
        clinics = dataManager.getClinics()
        isRefreshing = false
    }
    
    // Responsive layout calculations
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val gridColumns = if (isTablet) 2 else 1
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            ResponsiveHeader(
                title = "Clinic Network",
                subtitle = "${clinics.size} healthcare facilities",
                onRefresh = { refreshData() },
                onAdd = { showAddDialog = true },
                isRefreshing = isRefreshing,
                isTablet = isTablet
            )
        }

        // Quick stats
        item {
            ResponsiveClinicStats(
                clinics = clinics,
                isTablet = isTablet
            )
        }

        // Clinics grid
        item {
            if (clinics.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier.height(400.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(clinics) { clinic ->
                        ResponsiveClinicCard(
                            clinic = clinic,
                            isTablet = isTablet,
                            onClick = { /* Navigate to clinic details */ }
                        )
                    }
                }
            } else {
                EmptyStateCard(isTablet = isTablet)
            }
        }
    }

    // Add clinic dialog
    if (showAddDialog) {
        AddClinicDialog(
            onDismiss = { showAddDialog = false },
            onAddClinic = { newClinic ->
                dataManager.addClinic(newClinic)
                clinics = dataManager.getClinics()
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ResponsiveHeader(
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
                    Text("Add Clinic")
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
                Text("Add New Clinic")
            }
        }
    }
}

@Composable
private fun ResponsiveClinicStats(
    clinics: List<Clinic>,
    isTablet: Boolean
) {
    val onlineClinics = clinics.count { it.status == ClinicStatus.ONLINE }
    val totalPatients = clinics.sumOf { it.patientsToday }
    val totalStaff = clinics.sumOf { it.staffCount }
    
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
                StatItem("Online", onlineClinics.toString(), Icons.Default.CheckCircle)
                StatItem("Patients Today", totalPatients.toString(), Icons.Default.Person)
                StatItem("Total Staff", totalStaff.toString(), Icons.Default.Group)
                StatItem("Total Clinics", clinics.size.toString(), Icons.Default.LocationOn)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(120.dp).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { StatItem("Online", onlineClinics.toString(), Icons.Default.CheckCircle) }
                item { StatItem("Patients", totalPatients.toString(), Icons.Default.Person) }
                item { StatItem("Staff", totalStaff.toString(), Icons.Default.Group) }
                item { StatItem("Total", clinics.size.toString(), Icons.Default.LocationOn) }
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
private fun ResponsiveClinicCard(
    clinic: Clinic,
    isTablet: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isTablet) Modifier.height(180.dp) else Modifier.height(160.dp)),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = clinic.name,
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                
                StatusBadge(
                    status = clinic.status,
                    isCompact = !isTablet
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = clinic.address,
                fontSize = if (isTablet) 14.sp else 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(
                    icon = Icons.Default.Person,
                    text = "${clinic.patientsToday}",
                    label = "Patients",
                    isCompact = !isTablet
                )
                InfoChip(
                    icon = Icons.Default.Group,
                    text = "${clinic.staffCount}",
                    label = "Staff",
                    isCompact = !isTablet
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = clinic.powerStatus,
                    fontSize = if (isTablet) 12.sp else 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: ClinicStatus,
    isCompact: Boolean
) {
    val (color, text) = when (status) {
        ClinicStatus.ONLINE -> MaterialTheme.colorScheme.primary to "Online"
        ClinicStatus.BACKUP -> MaterialTheme.colorScheme.secondary to "Backup"
        ClinicStatus.OFFLINE -> MaterialTheme.colorScheme.error to "Offline"
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = if (isCompact) 10.sp else 12.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    label: String,
    isCompact: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(if (isCompact) 14.dp else 16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = text,
                fontSize = if (isCompact) 12.sp else 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = label,
                fontSize = if (isCompact) 8.sp else 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyStateCard(isTablet: Boolean) {
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
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(if (isTablet) 64.dp else 48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Clinics Found",
                    fontSize = if (isTablet) 20.sp else 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Add your first clinic to get started",
                    fontSize = if (isTablet) 14.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddClinicDialog(
    onDismiss: () -> Unit,
    onAddClinic: (Clinic) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Clinic") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Clinic Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = province,
                    onValueChange = { province = it },
                    label = { Text("Province") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && address.isNotBlank() && province.isNotBlank()) {
                        val newClinic = Clinic(
                            id = "clinic_${System.currentTimeMillis()}",
                            name = name,
                            patientsToday = 0,
                            staffCount = 1,
                            powerStatus = "Grid Connected",
                            status = ClinicStatus.ONLINE,
                            latitude = -26.2041,
                            longitude = 28.0473,
                            province = province,
                            address = address
                        )
                        onAddClinic(newClinic)
                    }
                }
            ) {
                Text("Add Clinic")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}