package com.example.medigrid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.data.Clinic
import com.example.medigrid.data.ClinicStatus
import com.example.medigrid.data.DataManager
import com.example.medigrid.data.SouthAfricanHealthcareFacilities
import com.example.medigrid.data.HealthcareFacility
import com.example.medigrid.data.FacilityType
import com.example.medigrid.data.LoadSheddingService
import com.example.medigrid.data.PowerRiskAssessment
import com.example.medigrid.data.LoadSheddingStage
import com.example.medigrid.data.PowerRiskLevel
import com.example.medigrid.security.SecurityLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkMapScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataManager = remember { DataManager.getInstance(context) }
    val configuration = LocalConfiguration.current

    // Enhanced data with comprehensive SA facilities
    var facilities by remember { mutableStateOf(emptyList<HealthcareFacility>()) }
    var selectedProvince by remember { mutableStateOf("All") }
    var selectedFacilityType by remember { mutableStateOf<FacilityType?>(null) }
    var selectedFacility by remember { mutableStateOf<HealthcareFacility?>(null) }
    var showEmergencyOnly by remember { mutableStateOf(false) }

    // Load comprehensive SA healthcare facilities
    LaunchedEffect(Unit) {
        facilities = SouthAfricanHealthcareFacilities.getAllFacilities()
        SecurityLogger.logSecurityEvent(
            "healthcare_network_map_accessed",
            mapOf(
                "total_facilities" to facilities.size,
                "provinces" to facilities.map { it.province }.distinct().size
            ),
            context
        )
    }

    // LoadSheddingService - get power risk assessments for facilities
    val loadSheddingService = remember { LoadSheddingService.getInstance(context) }
    val powerAssessments = remember(facilities) {
        facilities.associate { facility ->
            facility.id to loadSheddingService.getPowerRiskAssessment(facility.id)
        }
    }

    // Get provinces and facility types from comprehensive data
    val provinces = remember(facilities) {
        listOf("All") + facilities.map { it.province }.distinct().sorted()
    }
    
    val facilityTypes = remember(facilities) {
        facilities.map { it.facilityType }.distinct().sortedBy { it.name }
    }

    // Filter facilities based on selections
    val filteredFacilities = remember(facilities, selectedProvince, selectedFacilityType, showEmergencyOnly) {
        facilities.filter { facility ->
            val provinceMatch = selectedProvince == "All" || facility.province == selectedProvince
            val typeMatch = selectedFacilityType == null || facility.facilityType == selectedFacilityType
            val emergencyMatch = !showEmergencyOnly || facility.emergencyServices
            
            provinceMatch && typeMatch && emergencyMatch
        }.sortedWith(
            compareBy<HealthcareFacility> { it.province }
                .thenBy { it.facilityType }
                .thenBy { it.name }
        )
    }

    // Responsive layout
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val mapHeight = if (isTablet) 400.dp else 300.dp

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Enhanced Header
        item {
            EnhancedMapHeaderWithPower(
                title = "South African Healthcare Network Map",
                totalFacilities = facilities.size,
                filteredCount = filteredFacilities.size,
                selectedProvince = selectedProvince,
                powerAssessments = powerAssessments,
                isTablet = isTablet
            )
        }

        // Filter Controls
        item {
            FilterControlsCard(
                provinces = provinces,
                selectedProvince = selectedProvince,
                onProvinceSelected = { selectedProvince = it },
                facilityTypes = facilityTypes,
                selectedFacilityType = selectedFacilityType,
                onFacilityTypeSelected = { selectedFacilityType = it },
                showEmergencyOnly = showEmergencyOnly,
                onEmergencyFilterToggle = { showEmergencyOnly = !showEmergencyOnly },
                isTablet = isTablet
            )
        }

        // Enhanced Map View with Statistics
        item {
            EnhancedMapView(
                facilities = filteredFacilities,
                selectedFacility = selectedFacility,
                onFacilitySelected = { selectedFacility = it },
                height = mapHeight,
                isTablet = isTablet
            )
        }
        
        // Statistics Overview
        item {
            FacilitiesStatsCard(
                facilities = filteredFacilities,
                selectedProvince = selectedProvince,
                isTablet = isTablet
            )
        }

        // Facilities List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Healthcare Facilities ${if (selectedProvince != "All") "in $selectedProvince" else ""}",
                    fontSize = if (isTablet) 20.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (selectedFacilityType != null || showEmergencyOnly) {
                    OutlinedButton(
                        onClick = {
                            selectedFacilityType = null
                            showEmergencyOnly = false
                        }
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear filters",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Filters")
                    }
                }
            }
        }

        // Facilities List
        if (filteredFacilities.isNotEmpty()) {
            items(filteredFacilities) { facility ->
                EnhancedFacilityCard(
                    facility = facility,
                    powerAssessment = powerAssessments[facility.id],
                    isSelected = selectedFacility == facility,
                    onClick = { 
                        selectedFacility = if (selectedFacility == facility) null else facility 
                    },
                    isTablet = isTablet
                )
            }
        } else {
            item {
                EmptyFacilitiesCard(
                    selectedProvince = selectedProvince,
                    hasFilters = selectedFacilityType != null || showEmergencyOnly,
                    isTablet = isTablet
                )
            }
        }

        // Selected Facility Details
        selectedFacility?.let { facility ->
            item {
                DetailedFacilityCard(
                    facility = facility,
                    onClose = { selectedFacility = null },
                    isTablet = isTablet
                )
            }
        }
    }
}

@Composable
private fun EnhancedMapHeaderWithPower(
    title: String,
    totalFacilities: Int,
    filteredCount: Int,
    selectedProvince: String,
    powerAssessments: Map<String, PowerRiskAssessment>,
    isTablet: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 24.dp else 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = "Map",
                    modifier = Modifier.size(if (isTablet) 32.dp else 28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = title,
                        fontSize = if (isTablet) 24.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$filteredCount of $totalFacilities facilities",
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.height(8.dp))

            // Power Risk Summary
            val criticalFacilities =
                powerAssessments.values.count { it.currentRisk == PowerRiskLevel.CRITICAL }
            val highRiskFacilities =
                powerAssessments.values.count { it.currentRisk == PowerRiskLevel.HIGH }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Power,
                    contentDescription = "Power Risk",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Power Risk: $criticalFacilities Critical, $highRiskFacilities High Risk",
                    fontSize = if (isTablet) 13.sp else 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (selectedProvince != "All") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Province",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Viewing: $selectedProvince Province",
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterControlsCard(
    provinces: List<String>,
    selectedProvince: String,
    onProvinceSelected: (String) -> Unit,
    facilityTypes: List<FacilityType>,
    selectedFacilityType: FacilityType?,
    onFacilityTypeSelected: (FacilityType?) -> Unit,
    showEmergencyOnly: Boolean,
    onEmergencyFilterToggle: () -> Unit,
    isTablet: Boolean
) {
    Card {
        Column(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp)
        ) {
            Text(
                text = "Filter Facilities",
                fontSize = if (isTablet) 16.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Province Filter
            Text(
                text = "Province:",
                fontSize = if (isTablet) 14.sp else 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(provinces) { province ->
                    FilterChip(
                        onClick = { onProvinceSelected(province) },
                        label = { 
                            Text(
                                text = province,
                                fontSize = if (isTablet) 12.sp else 10.sp
                            )
                        },
                        selected = selectedProvince == province
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Facility Type Filter
            Text(
                text = "Facility Type:",
                fontSize = if (isTablet) 14.sp else 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        onClick = { onFacilityTypeSelected(null) },
                        label = { 
                            Text(
                                text = "All Types",
                                fontSize = if (isTablet) 12.sp else 10.sp
                            )
                        },
                        selected = selectedFacilityType == null
                    )
                }
                items(facilityTypes) { type ->
                    FilterChip(
                        onClick = { onFacilityTypeSelected(type) },
                        label = { 
                            Text(
                                text = type.name.replace('_', ' '),
                                fontSize = if (isTablet) 12.sp else 10.sp
                            )
                        },
                        selected = selectedFacilityType == type
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Emergency Services Filter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = showEmergencyOnly,
                    onCheckedChange = { onEmergencyFilterToggle() }
                )
                Text(
                    text = "Emergency Services Only",
                    fontSize = if (isTablet) 14.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.Default.Emergency,
                    contentDescription = "Emergency",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EnhancedMapView(
    facilities: List<HealthcareFacility>,
    selectedFacility: HealthcareFacility?,
    onFacilitySelected: (HealthcareFacility) -> Unit,
    height: androidx.compose.ui.unit.Dp,
    isTablet: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = "Healthcare Network Map",
                    modifier = Modifier.size(if (isTablet) 80.dp else 64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Interactive Healthcare Network Map",
                    fontSize = if (isTablet) 20.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${facilities.size} facilities across ${
                        facilities.map { it.province }.distinct().size
                    } provinces",
                    fontSize = if (isTablet) 14.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                if (selectedFacility != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📍 ${selectedFacility.name}",
                                fontSize = if (isTablet) 14.sp else 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${selectedFacility.city}, ${selectedFacility.province}",
                                fontSize = if (isTablet) 12.sp else 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FacilitiesStatsCard(
    facilities: List<HealthcareFacility>,
    selectedProvince: String,
    isTablet: Boolean
) {
    val totalBeds = facilities.sumOf { it.bedCapacity }
    val totalStaff = facilities.sumOf { it.staffCount }
    val emergencyFacilities = facilities.count { it.emergencyServices }
    val helicopterFacilities = facilities.count { it.hasHelicopter }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                    Icons.Default.Analytics,
                    contentDescription = "Statistics",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Network Statistics ${if (selectedProvince != "All") "- $selectedProvince" else ""}",
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    title = "Total Beds",
                    value = totalBeds.toString(),
                    icon = Icons.Default.Hotel,
                    isTablet = isTablet
                )
                StatItem(
                    title = "Staff",
                    value = totalStaff.toString(),
                    icon = Icons.Default.People,
                    isTablet = isTablet
                )
                StatItem(
                    title = "Emergency",
                    value = emergencyFacilities.toString(),
                    icon = Icons.Default.Emergency,
                    isTablet = isTablet
                )
                StatItem(
                    title = "Helicopter",
                    value = helicopterFacilities.toString(),
                    icon = Icons.Default.Flight,
                    isTablet = isTablet
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isTablet: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = title,
            modifier = Modifier.size(if (isTablet) 24.dp else 20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = if (isTablet) 18.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = title,
            fontSize = if (isTablet) 12.sp else 10.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun EnhancedFacilityCard(
    facility: HealthcareFacility,
    powerAssessment: PowerRiskAssessment?,
    isSelected: Boolean,
    onClick: () -> Unit,
    isTablet: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status and Type Indicators
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = when (facility.status) {
                                ClinicStatus.ONLINE -> Color.Green
                                ClinicStatus.BACKUP -> MaterialTheme.colorScheme.secondary
                                ClinicStatus.OFFLINE -> MaterialTheme.colorScheme.error
                            },
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Facility type icon
                Icon(
                    imageVector = when (facility.facilityType) {
                        FacilityType.NATIONAL_HOSPITAL -> Icons.Default.LocalHospital
                        FacilityType.PROVINCIAL_HOSPITAL -> Icons.Default.LocalHospital
                        FacilityType.REGIONAL_HOSPITAL -> Icons.Default.MedicalServices
                        FacilityType.DISTRICT_HOSPITAL -> Icons.Default.MedicalServices
                        FacilityType.PRIVATE_HOSPITAL -> Icons.Default.Business
                        FacilityType.COMMUNITY_HEALTH_CENTER -> Icons.Default.HealthAndSafety
                        FacilityType.PRIMARY_CLINIC -> Icons.Default.LocalPharmacy
                        FacilityType.SPECIALIZED_HOSPITAL -> Icons.Default.Psychology
                        else -> Icons.Default.LocationOn
                    },
                    contentDescription = facility.facilityType.name,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                // Facility load shedding stage
                powerAssessment?.let { assessment ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Power Risk: ${assessment.currentRisk.name}",
                            fontSize = if (isTablet) 10.sp else 8.sp,
                            color = when (assessment.currentRisk) {
                                PowerRiskLevel.CRITICAL -> MaterialTheme.colorScheme.error
                                PowerRiskLevel.HIGH -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                PowerRiskLevel.MODERATE -> MaterialTheme.colorScheme.secondary
                                PowerRiskLevel.LOW -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }

            // Facility Information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = facility.name,
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "${facility.city}, ${facility.province}",
                    fontSize = if (isTablet) 12.sp else 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = facility.facilityType.name.replace('_', ' '),
                    fontSize = if (isTablet) 11.sp else 9.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${facility.bedCapacity} beds",
                        fontSize = if (isTablet) 11.sp else 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${facility.staffCount} staff",
                        fontSize = if (isTablet) 11.sp else 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${facility.patientsToday} patients",
                        fontSize = if (isTablet) 11.sp else 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Service Indicators
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (facility.emergencyServices) {
                    Icon(
                        Icons.Default.Emergency,
                        contentDescription = "Emergency Services",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                if (facility.hasAmbulance) {
                    Icon(
                        Icons.Default.AirportShuttle,
                        contentDescription = "Ambulance",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (facility.hasHelicopter) {
                    Icon(
                        Icons.Default.Flight,
                        contentDescription = "Helicopter",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailedFacilityCard(
    facility: HealthcareFacility,
    onClose: () -> Unit,
    isTablet: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 24.dp else 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocalHospital,
                        contentDescription = "Hospital",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Facility Details",
                        fontSize = if (isTablet) 18.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Facility Name and Type
            Text(
                text = facility.name,
                fontSize = if (isTablet) 20.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Text(
                text = facility.facilityType.name.replace('_', ' '),
                fontSize = if (isTablet) 14.sp else 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            // Power status and load shedding for facility
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Power,
                    contentDescription = "Power Status",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Power Status: ${facility.powerStatus}",
                    fontSize = if (isTablet) 12.sp else 10.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Location Information
            DetailRow(
                label = "Address:",
                value = facility.address,
                icon = Icons.Default.LocationOn,
                isTablet = isTablet
            )
            
            DetailRow(
                label = "City:",
                value = facility.city,
                icon = Icons.Default.Place,
                isTablet = isTablet
            )
            
            DetailRow(
                label = "Province:",
                value = facility.province,
                icon = Icons.Default.Public,
                isTablet = isTablet
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Capacity Information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CapacityItem(
                    label = "Bed Capacity",
                    value = facility.bedCapacity.toString(),
                    icon = Icons.Default.Hotel,
                    isTablet = isTablet
                )
                CapacityItem(
                    label = "Staff Count",
                    value = facility.staffCount.toString(),
                    icon = Icons.Default.People,
                    isTablet = isTablet
                )
                CapacityItem(
                    label = "Patients Today",
                    value = facility.patientsToday.toString(),
                    icon = Icons.Default.Person,
                    isTablet = isTablet
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Contact Information
            if (facility.phoneNumber.isNotEmpty()) {
                DetailRow(
                    label = "Phone:",
                    value = facility.phoneNumber,
                    icon = Icons.Default.Phone,
                    isTablet = isTablet
                )
            }
            
            DetailRow(
                label = "Operating Hours:",
                value = facility.operatingHours,
                icon = Icons.Default.Schedule,
                isTablet = isTablet
            )
            
            // Specialties
            if (facility.specialties.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Specialties:",
                    fontSize = if (isTablet) 14.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(facility.specialties) { specialty ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = specialty,
                                    fontSize = if (isTablet) 12.sp else 10.sp
                                )
                            }
                        )
                    }
                }
            }
            
            // Languages
            if (facility.languages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Languages: ${facility.languages.joinToString(", ")}",
                    fontSize = if (isTablet) 12.sp else 10.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
            
            // Services
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (facility.emergencyServices) {
                    ServiceChip(
                        text = "Emergency",
                        icon = Icons.Default.Emergency,
                        color = MaterialTheme.colorScheme.error,
                        isTablet = isTablet
                    )
                }
                if (facility.hasAmbulance) {
                    ServiceChip(
                        text = "Ambulance",
                        icon = Icons.Default.AirportShuttle,
                        color = MaterialTheme.colorScheme.primary,
                        isTablet = isTablet
                    )
                }
                if (facility.hasHelicopter) {
                    ServiceChip(
                        text = "Helicopter",
                        icon = Icons.Default.Flight,
                        color = MaterialTheme.colorScheme.secondary,
                        isTablet = isTablet
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isTablet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$label ",
            fontSize = if (isTablet) 12.sp else 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
            text = value,
            fontSize = if (isTablet) 12.sp else 10.sp,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun CapacityItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isTablet: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(if (isTablet) 24.dp else 20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = if (isTablet) 16.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
            text = label,
            fontSize = if (isTablet) 10.sp else 8.sp,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ServiceChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isTablet: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = text,
                modifier = Modifier.size(if (isTablet) 16.dp else 14.dp),
                tint = color
            )
            Text(
                text = text,
                fontSize = if (isTablet) 12.sp else 10.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
private fun EmptyFacilitiesCard(
    selectedProvince: String,
    hasFilters: Boolean,
    isTablet: Boolean
) {
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
                    Icons.Default.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(if (isTablet) 64.dp else 48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (hasFilters) "No facilities match current filters" else "No facilities found",
                    fontSize = if (isTablet) 18.sp else 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (hasFilters) "Try adjusting your filters" else "Try selecting a different province",
                    fontSize = if (isTablet) 14.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// Legacy functions for backward compatibility
@Composable
private fun ResponsiveMapHeader(
    title: String,
    subtitle: String,
    isTablet: Boolean,
) {
    Column {
        Text(
            text = title,
            fontSize = if (isTablet) 28.sp else 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = subtitle,
            fontSize = if (isTablet) 16.sp else 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResponsiveMapView(
    clinics: List<Clinic>,
    selectedClinic: Clinic?,
    onClinicSelected: (Clinic) -> Unit,
    height: androidx.compose.ui.unit.Dp,
    isTablet: Boolean,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Map",
                    modifier = Modifier.size(if (isTablet) 64.dp else 48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Interactive Healthcare Network Map",
                    fontSize = if (isTablet) 18.sp else 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${clinics.size} facilities across ${
                        clinics.map { it.province }.distinct().size
                    } provinces",
                    fontSize = if (isTablet) 14.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                if (selectedClinic != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            text = "Selected: ${selectedClinic.name}",
                            fontSize = if (isTablet) 12.sp else 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkClinicCard(
    clinic: Clinic,
    isSelected: Boolean,
    onClick: () -> Unit,
    isTablet: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        )
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
                        color = when (clinic.status) {
                            ClinicStatus.ONLINE -> MaterialTheme.colorScheme.primary
                            ClinicStatus.BACKUP -> MaterialTheme.colorScheme.secondary
                            ClinicStatus.OFFLINE -> MaterialTheme.colorScheme.error
                        },
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clinic.name,
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${clinic.address}, ${clinic.province}",
                    fontSize = if (isTablet) 12.sp else 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${clinic.patientsToday} patients • ${clinic.staffCount} staff",
                    fontSize = if (isTablet) 12.sp else 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptyMapCard(isTablet: Boolean) {
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
                    text = "No Clinics in Selected Area",
                    fontSize = if (isTablet) 20.sp else 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Try selecting a different province",
                    fontSize = if (isTablet) 14.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}