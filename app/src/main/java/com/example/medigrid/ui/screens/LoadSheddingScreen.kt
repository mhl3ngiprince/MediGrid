package com.example.medigrid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.data.*
import com.example.medigrid.security.HealthcareAuthService
import com.example.medigrid.security.SecurityLogger
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadSheddingScreen(
    currentUser: HealthcareAuthService.HealthcareUser?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var loadSheddingService by remember { mutableStateOf<LoadSheddingService?>(null) }
    var selectedFacility by remember { mutableStateOf<HealthcareFacility?>(null) }
    var powerRiskAssessment by remember { mutableStateOf<PowerRiskAssessment?>(null) }
    var activeOutages by remember { mutableStateOf<List<PowerOutageAlert>>(emptyList()) }
    var isDownloading by remember { mutableStateOf(false) }
    var lastUpdateTime by remember { mutableStateOf<Long?>(null) }
    
    // Initialize service
    LaunchedEffect(Unit) {
        try {
            loadSheddingService = LoadSheddingService.getInstance(context)
            activeOutages = loadSheddingService?.getActiveOutageAlerts() ?: emptyList()
            lastUpdateTime = System.currentTimeMillis()
            
            SecurityLogger.logSecurityEvent(
                "load_shedding_dashboard_accessed",
                mapOf(
                    "user_id" to (currentUser?.id ?: "unknown"),
                    "active_outages" to activeOutages.size
                ),
                context
            )
        } catch (e: Exception) {
            // Handle initialization error
        }
    }
    
    // Update selected facility assessment
    LaunchedEffect(selectedFacility) {
        selectedFacility?.let { facility ->
            powerRiskAssessment = loadSheddingService?.getPowerRiskAssessment(facility.id)
        }
    }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            LoadSheddingHeader(
                activeOutages = activeOutages.size,
                lastUpdateTime = lastUpdateTime,
                onRefresh = {
                    coroutineScope.launch {
                        isDownloading = true
                        loadSheddingService?.downloadSchedules()
                        activeOutages = loadSheddingService?.getActiveOutageAlerts() ?: emptyList()
                        lastUpdateTime = System.currentTimeMillis()
                        isDownloading = false
                    }
                },
                isDownloading = isDownloading
            )
        }
        
        // Current Load Shedding Status
        item {
            CurrentLoadSheddingStatus(
                service = loadSheddingService
            )
        }
        
        // Active Power Outages
        if (activeOutages.isNotEmpty()) {
            item {
                ActiveOutagesCard(
                    outages = activeOutages,
                    onFacilitySelected = { facilityId ->
                        // Find and select facility
                        val facilities = SouthAfricanHealthcareFacilities.getAllFacilities()
                        selectedFacility = facilities.find { it.id == facilityId }
                    }
                )
            }
        }
        
        // Facility Selection
        item {
            FacilitySelectionCard(
                selectedFacility = selectedFacility,
                onFacilitySelected = { selectedFacility = it }
            )
        }
        
        // Power Risk Assessment
        powerRiskAssessment?.let { assessment ->
            item {
                PowerRiskAssessmentCard(assessment = assessment)
            }
        }
        
        // Load Shedding Schedule
        selectedFacility?.let { facility ->
            item {
                LoadSheddingScheduleCard(
                    facility = facility,
                    service = loadSheddingService
                )
            }
        }
        
        // Emergency Protocols
        selectedFacility?.let { facility ->
            item {
                EmergencyProtocolsCard(
                    facility = facility,
                    service = loadSheddingService
                )
            }
        }
        
        // Critical Equipment Status
        selectedFacility?.let { facility ->
            item {
                CriticalEquipmentCard(
                    facility = facility,
                    service = loadSheddingService
                )
            }
        }
    }
}

@Composable
private fun LoadSheddingHeader(
    activeOutages: Int,
    lastUpdateTime: Long?,
    onRefresh: () -> Unit,
    isDownloading: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Load Shedding Dashboard",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Real-time power status for healthcare facilities",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                
                IconButton(
                    onClick = onRefresh,
                    enabled = !isDownloading
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Active outages indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                if (activeOutages > 0) Color.Red else Color.Green,
                                CircleShape
                            )
                    )
                    Text(
                        text = "$activeOutages Active Outages",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                // Last update time
                lastUpdateTime?.let { time ->
                    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                    Text(
                        text = "Updated: ${format.format(Date(time))}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentLoadSheddingStatus(
    service: LoadSheddingService?
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Current Load Shedding Status",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Show current stages for major cities
            val majorCities = listOf(
                Triple("City of Johannesburg", "Johannesburg Central", "JHB"),
                Triple("City of Cape Town", "Cape Town CBD", "CPT"),
                Triple("eThekwini Municipality", "Durban Central", "DBN"),
                Triple("City of Tshwane", "Pretoria Central", "PTA")
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(majorCities) { (municipality, area, code) ->
                    val stage = service?.getCurrentStage(municipality, area) ?: LoadSheddingStage.STAGE_0
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = getStageColor(stage).copy(alpha = 0.1f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            getStageColor(stage)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = code,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = getStageColor(stage)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Icon(
                                imageVector = stage.icon,
                                contentDescription = stage.displayName,
                                tint = getStageColor(stage),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Stage ${stage.stage}",
                                fontSize = 10.sp,
                                color = getStageColor(stage)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveOutagesCard(
    outages: List<PowerOutageAlert>,
    onFacilitySelected: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Active Outages",
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Active Power Outages (${outages.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            outages.take(5).forEach { outage ->
                OutageListItem(
                    outage = outage,
                    onClick = { onFacilitySelected(outage.facilityId) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (outages.size > 5) {
                Text(
                    text = "... and ${outages.size - 5} more facilities",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun OutageListItem(
    outage: PowerOutageAlert,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = outage.stage.icon,
                contentDescription = outage.stage.displayName,
                tint = getStageColor(outage.stage),
                modifier = Modifier.size(20.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = outage.facilityName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${outage.stage.displayName} • Until ${formatTime(outage.outageEnd)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Backup power status indicator
            val backupColor = when (outage.backupPowerStatus) {
                BackupPowerStatus.FULLY_OPERATIONAL -> Color.Green
                BackupPowerStatus.PARTIAL -> Color.Yellow
                BackupPowerStatus.MINIMAL -> MaterialTheme.colorScheme.secondary
                BackupPowerStatus.NONE -> Color.Red
                BackupPowerStatus.MAINTENANCE_REQUIRED -> Color.Gray
            }
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(backupColor, CircleShape)
            )
        }
    }
}

@Composable
private fun FacilitySelectionCard(
    selectedFacility: HealthcareFacility?,
    onFacilitySelected: (HealthcareFacility) -> Unit
) {
    var showFacilityList by remember { mutableStateOf(false) }
    
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Select Healthcare Facility",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = { showFacilityList = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedFacility?.name ?: "Choose facility...",
                    maxLines = 1
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            
            AnimatedVisibility(visible = showFacilityList) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val facilities = remember { SouthAfricanHealthcareFacilities.getAllFacilities() }
                    
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(facilities.take(20)) { facility ->
                            TextButton(
                                onClick = {
                                    onFacilitySelected(facility)
                                    showFacilityList = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = facility.name,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Start,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = facility.province,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PowerRiskAssessmentCard(
    assessment: PowerRiskAssessment
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (assessment.currentRisk) {
                PowerRiskLevel.LOW -> MaterialTheme.colorScheme.primaryContainer
                PowerRiskLevel.MODERATE -> MaterialTheme.colorScheme.secondaryContainer
                PowerRiskLevel.HIGH -> MaterialTheme.colorScheme.tertiaryContainer
                PowerRiskLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = assessment.currentStage.icon,
                    contentDescription = "Risk Level",
                    tint = getStageColor(assessment.currentStage)
                )
                Text(
                    text = "Power Risk Assessment",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Risk level
            Text(
                text = "Risk Level: ${assessment.currentRisk.name}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = getStageColor(assessment.currentStage)
            )
            
            Text(
                text = "Current Stage: ${assessment.currentStage.displayName}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Backup power status
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (assessment.backupPowerReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = "Backup Power",
                    tint = if (assessment.backupPowerReady) Color.Green else Color.Red,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Backup Power: ${if (assessment.backupPowerReady) "Ready" else "Not Ready"}",
                    fontSize = 12.sp
                )
            }
            
            // Next outage
            assessment.nextOutage?.let { outage ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Next Outage: ${formatTime(outage.outageStart)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Recommendations
            if (assessment.recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Recommendations:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                assessment.recommendations.take(3).forEach { recommendation ->
                    Text(
                        text = "• $recommendation",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadSheddingScheduleCard(
    facility: HealthcareFacility,
    service: LoadSheddingService?
) {
    val schedule = service?.getScheduleForFacility(facility.id)
    
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Load Shedding Schedule",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            schedule?.let { sched ->
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "${sched.municipality} - ${sched.area}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Block: ${sched.block} • Eskom ID: ${sched.eskomId}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Today's Schedule:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                
                val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                val todaySlots = sched.timeSlots.filter { it.dayOfWeek == today }
                
                if (todaySlots.isNotEmpty()) {
                    todaySlots.forEach { slot ->
                        Text(
                            text = "• ${slot.startTime} - ${slot.endTime} (${slot.duration} min)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "No scheduled outages today",
                        fontSize = 11.sp,
                        color = Color.Green
                    )
                }
                
                // Emergency contacts
                if (sched.emergencyContacts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Emergency Contacts:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    sched.emergencyContacts.forEach { contact ->
                        Text(
                            text = "📞 $contact",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } ?: run {
                Text(
                    text = "Schedule not available for this facility",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmergencyProtocolsCard(
    facility: HealthcareFacility,
    service: LoadSheddingService?
) {
    val upcomingOutages = service?.getUpcomingOutages(facility.id) ?: emptyList()
    
    if (upcomingOutages.isNotEmpty()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Emergency,
                        contentDescription = "Emergency Protocol",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Emergency Protocols",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val nextOutage = upcomingOutages.first()
                
                Text(
                    text = "Protocol for ${nextOutage.stage.displayName}:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                
                nextOutage.emergencyProtocol.actions.take(5).forEach { action ->
                    Text(
                        text = "• $action",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Emergency Contacts:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                
                nextOutage.emergencyProtocol.contactNumbers.take(3).forEach { contact ->
                    Text(
                        text = "📞 $contact",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CriticalEquipmentCard(
    facility: HealthcareFacility,
    service: LoadSheddingService?
) {
    val upcomingOutages = service?.getUpcomingOutages(facility.id) ?: emptyList()
    
    if (upcomingOutages.isNotEmpty()) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.MedicalServices,
                        contentDescription = "Critical Equipment"
                    )
                    Text(
                        text = "Critical Equipment Status",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val equipment = upcomingOutages.first().criticalEquipment
                
                equipment.sortedBy { it.priority }.take(6).forEach { eq ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Priority indicator
                        val priorityColor = when (eq.priority) {
                            EquipmentPriority.LIFE_SUPPORT -> Color.Red
                            EquipmentPriority.CRITICAL_CARE -> MaterialTheme.colorScheme.secondary
                            EquipmentPriority.DIAGNOSTIC -> Color.Blue
                            EquipmentPriority.SUPPORT -> Color.Gray
                            EquipmentPriority.NON_ESSENTIAL -> Color.LightGray
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(priorityColor, CircleShape)
                        )
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = eq.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${eq.powerRequirement}W • ${eq.batteryLife}min UPS",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (eq.alternativePower) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Backup Available",
                                tint = Color.Green,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// Helper functions
private fun getStageColor(stage: LoadSheddingStage): Color {
    return when (stage.riskLevel) {
        PowerRiskLevel.LOW -> Color.Green
        PowerRiskLevel.MODERATE -> Color(0xFFFF9800) // Orange
        PowerRiskLevel.HIGH -> Color(0xFFFF5722) // Deep Orange
        PowerRiskLevel.CRITICAL -> Color.Red
    }
}

private fun formatTime(timestamp: Long): String {
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    return format.format(Date(timestamp))
}