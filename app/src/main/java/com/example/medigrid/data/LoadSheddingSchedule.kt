package com.example.medigrid.data

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * South African Load Shedding Schedule System
 * 
 * Integrates with Eskom load shedding stages and provides:
 * - Real-time load shedding status for healthcare facilities
 * - Power backup planning for critical medical equipment
 * - Emergency response protocols during outages
 * - Province-specific schedules and municipality data
 */

enum class LoadSheddingStage(
    val stage: Int, 
    val displayName: String, 
    val description: String,
    val dailyHours: Int,
    val icon: ImageVector,
    val riskLevel: PowerRiskLevel
) {
    STAGE_0(0, "No Load Shedding", "Normal power supply", 0, Icons.Default.Power, PowerRiskLevel.LOW),
    STAGE_1(1, "Stage 1", "Low impact - 2-3 hours daily", 2, Icons.Default.BatteryAlert, PowerRiskLevel.LOW),
    STAGE_2(2, "Stage 2", "Moderate impact - 4-6 hours daily", 5, Icons.Default.Warning, PowerRiskLevel.MODERATE),
    STAGE_3(3, "Stage 3", "High impact - 6-8 hours daily", 7, Icons.Default.Report, PowerRiskLevel.HIGH),
    STAGE_4(4, "Stage 4", "Severe impact - 8-12 hours daily", 10, Icons.Default.Error, PowerRiskLevel.CRITICAL),
    STAGE_5(5, "Stage 5", "Critical impact - 10-14 hours daily", 12, Icons.Default.Emergency, PowerRiskLevel.CRITICAL),
    STAGE_6(6, "Stage 6", "Extreme impact - 12+ hours daily", 14, Icons.Default.PowerOff, PowerRiskLevel.CRITICAL)
}

enum class PowerRiskLevel {
    LOW, MODERATE, HIGH, CRITICAL
}

data class LoadSheddingSchedule(
    val municipality: String,
    val province: String,
    val area: String,
    val block: String,
    val stage: LoadSheddingStage,
    val timeSlots: List<TimeSlot>,
    val lastUpdated: Long = System.currentTimeMillis(),
    val eskomId: String = "",
    val emergencyContacts: List<String> = emptyList()
)

data class TimeSlot(
    val startTime: String,
    val endTime: String,
    val dayOfWeek: Int, // 1-7 (Monday-Sunday)
    val duration: Int // minutes
)

data class PowerOutageAlert(
    val id: String,
    val facilityId: String,
    val facilityName: String,
    val outageStart: Long,
    val outageEnd: Long,
    val stage: LoadSheddingStage,
    val backupPowerStatus: BackupPowerStatus,
    val criticalEquipment: List<CriticalEquipment>,
    val emergencyProtocol: EmergencyProtocol,
    val isActive: Boolean = true
)

data class CriticalEquipment(
    val name: String,
    val powerRequirement: Int, // Watts
    val batteryLife: Int, // minutes on UPS
    val priority: EquipmentPriority,
    val alternativePower: Boolean = false
)

enum class EquipmentPriority {
    LIFE_SUPPORT, CRITICAL_CARE, DIAGNOSTIC, SUPPORT, NON_ESSENTIAL
}

enum class BackupPowerStatus {
    FULLY_OPERATIONAL, PARTIAL, MINIMAL, NONE, MAINTENANCE_REQUIRED
}

data class EmergencyProtocol(
    val actions: List<String>,
    val contactNumbers: List<String>,
    val evacuationPlan: String? = null,
    val medicalEmergencyProcedure: String
)

/**
 * Load Shedding Service - Real-time schedule management
 */
class LoadSheddingService private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: LoadSheddingService? = null
        
        fun getInstance(context: Context): LoadSheddingService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LoadSheddingService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val schedules: MutableList<LoadSheddingSchedule> = mutableListOf()
    private val powerOutageAlerts: MutableList<PowerOutageAlert> = mutableListOf()
    
    init {
        initializeLoadSheddingSchedules()
        generateCurrentOutageAlerts()
    }
    
    /**
     * Download and initialize comprehensive SA load shedding schedules
     */
    suspend fun downloadSchedules(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Simulate downloading from Eskom API / City Power / Municipal sources
            initializeLoadSheddingSchedules()
            generateCurrentOutageAlerts()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get current load shedding stage for a specific area
     */
    fun getCurrentStage(municipality: String, area: String): LoadSheddingStage {
        val schedule = schedules.find { 
            it.municipality.equals(municipality, ignoreCase = true) && 
            it.area.equals(area, ignoreCase = true) 
        }
        return schedule?.stage ?: LoadSheddingStage.STAGE_0
    }
    
    /**
     * Get load shedding schedule for healthcare facility
     */
    fun getScheduleForFacility(facilityId: String): LoadSheddingSchedule? {
        // Map facility to municipality/area
        val facilityArea = getFacilityArea(facilityId)
        return schedules.find { 
            it.municipality == facilityArea.municipality && 
            it.area == facilityArea.area 
        }
    }
    
    /**
     * Get upcoming power outages for next 24 hours
     */
    fun getUpcomingOutages(facilityId: String): List<PowerOutageAlert> {
        val now = System.currentTimeMillis()
        val next24Hours = now + (24 * 60 * 60 * 1000)
        
        return powerOutageAlerts.filter { alert ->
            alert.facilityId == facilityId &&
            alert.outageStart <= next24Hours &&
            alert.outageEnd >= now
        }.sortedBy { it.outageStart }
    }
    
    /**
     * Get all active power outage alerts
     */
    fun getActiveOutageAlerts(): List<PowerOutageAlert> {
        val now = System.currentTimeMillis()
        return powerOutageAlerts.filter { alert ->
            alert.isActive &&
            alert.outageStart <= now &&
            alert.outageEnd >= now
        }
    }
    
    /**
     * Get power risk assessment for facility
     */
    fun getPowerRiskAssessment(facilityId: String): PowerRiskAssessment {
        val schedule = getScheduleForFacility(facilityId)
        val upcomingOutages = getUpcomingOutages(facilityId)
        val activeOutage = getActiveOutageAlerts().find { it.facilityId == facilityId }
        
        val riskLevel = when {
            activeOutage != null -> activeOutage.stage.riskLevel
            upcomingOutages.isNotEmpty() -> upcomingOutages.first().stage.riskLevel
            schedule != null -> schedule.stage.riskLevel
            else -> PowerRiskLevel.LOW
        }
        
        return PowerRiskAssessment(
            facilityId = facilityId,
            currentRisk = riskLevel,
            currentStage = schedule?.stage ?: LoadSheddingStage.STAGE_0,
            nextOutage = upcomingOutages.firstOrNull(),
            backupPowerReady = checkBackupPowerStatus(facilityId),
            recommendations = generatePowerRecommendations(riskLevel, facilityId)
        )
    }
    
    private fun initializeLoadSheddingSchedules() {
        schedules.clear()
        schedules.addAll(createSouthAfricanLoadSheddingSchedules())
    }
    
    private fun generateCurrentOutageAlerts() {
        powerOutageAlerts.clear()
        
        // Generate realistic outage alerts based on current SA load shedding patterns
        val facilities = SouthAfricanHealthcareFacilities.getAllFacilities()
        
        facilities.forEach { facility ->
            val currentStage = getCurrentLoadSheddingStage()
            if (currentStage != LoadSheddingStage.STAGE_0) {
                val outageAlert = generateOutageAlert(facility, currentStage)
                powerOutageAlerts.add(outageAlert)
            }
        }
    }
    
    private fun createSouthAfricanLoadSheddingSchedules(): List<LoadSheddingSchedule> = listOf(
        
        // =================== GAUTENG PROVINCE ===================
        
        // Johannesburg (City Power)
        LoadSheddingSchedule(
            municipality = "City of Johannesburg",
            province = "Gauteng",
            area = "Johannesburg Central",
            block = "Block 1",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "JHBCITYPOWER001",
            emergencyContacts = listOf("011-490-7870", "011-375-5555")
        ),
        
        LoadSheddingSchedule(
            municipality = "City of Johannesburg", 
            province = "Gauteng",
            area = "Soweto",
            block = "Block 2",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "JHBSOWETO002"
        ),
        
        LoadSheddingSchedule(
            municipality = "City of Johannesburg",
            province = "Gauteng", 
            area = "Sandton",
            block = "Block 3",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "JHBSANDTON003"
        ),
        
        // Pretoria (Tshwane)
        LoadSheddingSchedule(
            municipality = "City of Tshwane",
            province = "Gauteng",
            area = "Pretoria Central", 
            block = "Block 4",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "TSHWANECENTRAL004",
            emergencyContacts = listOf("012-358-9999", "012-427-6000")
        ),
        
        LoadSheddingSchedule(
            municipality = "City of Tshwane",
            province = "Gauteng",
            area = "Centurion",
            block = "Block 5", 
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "TSHWANECENTURION005"
        ),
        
        // Ekurhuleni
        LoadSheddingSchedule(
            municipality = "City of Ekurhuleni",
            province = "Gauteng",
            area = "Benoni",
            block = "Block 6",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "EKURHULENIBENONI006"
        ),
        
        LoadSheddingSchedule(
            municipality = "City of Ekurhuleni", 
            province = "Gauteng",
            area = "Kempton Park",
            block = "Block 7",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "EKURHULENITEMPTON007"
        ),
        
        // =================== WESTERN CAPE PROVINCE ===================
        
        // Cape Town
        LoadSheddingSchedule(
            municipality = "City of Cape Town",
            province = "Western Cape",
            area = "Cape Town CBD",
            block = "Block 1",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "CPTCBD001",
            emergencyContacts = listOf("021-480-7700", "021-400-4911")
        ),
        
        LoadSheddingSchedule(
            municipality = "City of Cape Town",
            province = "Western Cape", 
            area = "Tygerberg",
            block = "Block 2",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "CPTTYGERBERG002"
        ),
        
        LoadSheddingSchedule(
            municipality = "City of Cape Town",
            province = "Western Cape",
            area = "Khayelitsha",
            block = "Block 3", 
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "CPTKHAYELITSHA003"
        ),
        
        LoadSheddingSchedule(
            municipality = "City of Cape Town",
            province = "Western Cape",
            area = "Mitchells Plain",
            block = "Block 4",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(), 
            eskomId = "CPTMITCHELLS004"
        ),
        
        // George Municipality
        LoadSheddingSchedule(
            municipality = "George Municipality",
            province = "Western Cape",
            area = "George Central",
            block = "Block 1",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "GEORGECENTRAL001"
        ),
        
        // =================== KWAZULU-NATAL PROVINCE ===================
        
        // Durban (eThekwini)
        LoadSheddingSchedule(
            municipality = "eThekwini Municipality",
            province = "KwaZulu-Natal",
            area = "Durban Central",
            block = "Block 1", 
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "ETHEKWINICENTRAL001",
            emergencyContacts = listOf("031-311-1111", "031-240-1111")
        ),
        
        LoadSheddingSchedule(
            municipality = "eThekwini Municipality",
            province = "KwaZulu-Natal",
            area = "Chatsworth",
            block = "Block 2",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "ETHEKWINICHATSWORTH002"
        ),
        
        LoadSheddingSchedule(
            municipality = "eThekwini Municipality", 
            province = "KwaZulu-Natal",
            area = "Umlazi",
            block = "Block 3",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "ETHEKWINIUMLAZI003"
        ),
        
        // Pietermaritzburg
        LoadSheddingSchedule(
            municipality = "Msunduzi Municipality",
            province = "KwaZulu-Natal",
            area = "Pietermaritzburg",
            block = "Block 1",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "MSUNDUZIPMBG001"
        ),
        
        // =================== EASTERN CAPE PROVINCE ===================
        
        // Nelson Mandela Bay (Port Elizabeth/Gqeberha) 
        LoadSheddingSchedule(
            municipality = "Nelson Mandela Bay Municipality",
            province = "Eastern Cape",
            area = "Port Elizabeth Central",
            block = "Block 1",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "NELSONMANDELABAY001",
            emergencyContacts = listOf("041-506-5911")
        ),
        
        LoadSheddingSchedule(
            municipality = "Nelson Mandela Bay Municipality",
            province = "Eastern Cape", 
            area = "Uitenhage",
            block = "Block 2",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "NELSONMANDELABAY002"
        ),
        
        // Buffalo City (East London)
        LoadSheddingSchedule(
            municipality = "Buffalo City Municipality", 
            province = "Eastern Cape",
            area = "East London",
            block = "Block 1",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "BUFFALOCITYEL001"
        ),
        
        // =================== FREE STATE PROVINCE ===================
        
        // Bloemfontein
        LoadSheddingSchedule(
            municipality = "Mangaung Metropolitan Municipality",
            province = "Free State",
            area = "Bloemfontein",
            block = "Block 1", 
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "MANGAUNGBLMFTN001",
            emergencyContacts = listOf("051-405-8911")
        ),
        
        LoadSheddingSchedule(
            municipality = "Mangaung Metropolitan Municipality",
            province = "Free State",
            area = "Botshabelo",
            block = "Block 2",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "MANGAUNGBOTSHABELO002"  
        ),
        
        // =================== LIMPOPO PROVINCE ===================
        
        // Polokwane
        LoadSheddingSchedule(
            municipality = "Polokwane Municipality",
            province = "Limpopo",
            area = "Polokwane Central",
            block = "Block 1",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "POLOKWANECENTRAL001",
            emergencyContacts = listOf("015-290-2000")
        ),
        
        LoadSheddingSchedule(
            municipality = "Polokwane Municipality",
            province = "Limpopo", 
            area = "Seshego",
            block = "Block 2",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "POLOKWANESESHEGO002"
        ),
        
        // =================== MPUMALANGA PROVINCE ===================
        
        // Mbombela (Nelspruit)
        LoadSheddingSchedule(
            municipality = "City of Mbombela",
            province = "Mpumalanga",
            area = "Nelspruit",
            block = "Block 1",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "MBOMBELANELSPRUIT001",
            emergencyContacts = listOf("013-759-1333")
        ),
        
        // eMalahleni (Witbank) 
        LoadSheddingSchedule(
            municipality = "Emalahleni Municipality",
            province = "Mpumalanga",
            area = "Witbank",
            block = "Block 1",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "EMALAHLENIWB001"
        ),
        
        // =================== NORTH WEST PROVINCE ===================
        
        // Mahikeng
        LoadSheddingSchedule(
            municipality = "Mahikeng Municipality",
            province = "North West",
            area = "Mahikeng Central", 
            block = "Block 1",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "MAHIKENGCENTRAL001",
            emergencyContacts = listOf("018-381-0088")
        ),
        
        // Klerksdorp
        LoadSheddingSchedule(
            municipality = "City of Matlosana", 
            province = "North West",
            area = "Klerksdorp",
            block = "Block 1",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "MATLOSANAKLERKSDORP001"
        ),
        
        // =================== NORTHERN CAPE PROVINCE ===================
        
        // Kimberley
        LoadSheddingSchedule(
            municipality = "Sol Plaatje Municipality",
            province = "Northern Cape",
            area = "Kimberley",
            block = "Block 1",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "SOLPLAATJEKIMBERLEY001",
            emergencyContacts = listOf("053-830-6911")
        ),
        
        // Upington
        LoadSheddingSchedule(
            municipality = "Dawid Kruiper Municipality",
            province = "Northern Cape", 
            area = "Upington",
            block = "Block 1",
            stage = getCurrentLoadSheddingStage(),
            timeSlots = generateTimeSlots(),
            eskomId = "DAWIDKRUIPERUPINGTON001"
        )
    )
    
    private fun getCurrentLoadSheddingStage(): LoadSheddingStage {
        // Simulate realistic SA load shedding patterns
        val stages = listOf(
            LoadSheddingStage.STAGE_0 to 20, // 20% chance no load shedding
            LoadSheddingStage.STAGE_1 to 15, // 15% chance Stage 1
            LoadSheddingStage.STAGE_2 to 25, // 25% chance Stage 2
            LoadSheddingStage.STAGE_3 to 20, // 20% chance Stage 3
            LoadSheddingStage.STAGE_4 to 15, // 15% chance Stage 4
            LoadSheddingStage.STAGE_5 to 4,  // 4% chance Stage 5
            LoadSheddingStage.STAGE_6 to 1   // 1% chance Stage 6
        )
        
        val random = Random.nextInt(100)
        var cumulative = 0
        
        for ((stage, percentage) in stages) {
            cumulative += percentage
            if (random < cumulative) {
                return stage
            }
        }
        
        return LoadSheddingStage.STAGE_2 // Default fallback
    }
    
    private fun generateTimeSlots(): List<TimeSlot> {
        val timeSlots = mutableListOf<TimeSlot>()
        
        // Generate realistic load shedding time slots
        val commonTimes = listOf(
            "06:00" to "08:00", // Morning peak
            "09:00" to "11:00", // Late morning
            "12:00" to "14:00", // Midday
            "16:00" to "18:00", // Afternoon peak
            "18:00" to "20:00", // Evening peak
            "20:00" to "22:00"  // Night
        )
        
        // Generate slots for each day of the week
        for (dayOfWeek in 1..7) {
            val selectedTimes = commonTimes.shuffled().take(Random.nextInt(1, 4))
            
            selectedTimes.forEach { (start, end) ->
                timeSlots.add(TimeSlot(
                    startTime = start,
                    endTime = end,
                    dayOfWeek = dayOfWeek,
                    duration = calculateDuration(start, end)
                ))
            }
        }
        
        return timeSlots
    }
    
    private fun calculateDuration(startTime: String, endTime: String): Int {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return try {
            val start = format.parse(startTime)!!
            val end = format.parse(endTime)!!
            ((end.time - start.time) / (1000 * 60)).toInt()
        } catch (e: Exception) {
            120 // Default 2 hours
        }
    }
    
    private fun getFacilityArea(facilityId: String): FacilityArea {
        // Map facility ID to municipality and area
        return when {
            facilityId.contains("za_gp_") -> FacilityArea("City of Johannesburg", "Johannesburg Central")
            facilityId.contains("za_wc_") -> FacilityArea("City of Cape Town", "Cape Town CBD") 
            facilityId.contains("za_kzn_") -> FacilityArea("eThekwini Municipality", "Durban Central")
            facilityId.contains("za_ec_") -> FacilityArea("Nelson Mandela Bay Municipality", "Port Elizabeth Central")
            facilityId.contains("za_fs_") -> FacilityArea("Mangaung Metropolitan Municipality", "Bloemfontein")
            facilityId.contains("za_lp_") -> FacilityArea("Polokwane Municipality", "Polokwane Central")
            facilityId.contains("za_mp_") -> FacilityArea("City of Mbombela", "Nelspruit") 
            facilityId.contains("za_nw_") -> FacilityArea("Mahikeng Municipality", "Mahikeng Central")
            facilityId.contains("za_nc_") -> FacilityArea("Sol Plaatje Municipality", "Kimberley")
            else -> FacilityArea("Unknown Municipality", "Unknown Area")
        }
    }
    
    private fun generateOutageAlert(
        facility: HealthcareFacility,
        stage: LoadSheddingStage
    ): PowerOutageAlert {
        val now = System.currentTimeMillis()
        val outageStart = now + Random.nextLong(0, 6 * 60 * 60 * 1000) // Next 6 hours
        val outageDuration = stage.dailyHours * 60 * 60 * 1000L / 3 // Divide daily hours by typical outage cycles
        
        return PowerOutageAlert(
            id = "outage_${facility.id}_${System.currentTimeMillis()}",
            facilityId = facility.id,
            facilityName = facility.name,
            outageStart = outageStart,
            outageEnd = outageStart + outageDuration,
            stage = stage,
            backupPowerStatus = generateBackupPowerStatus(facility),
            criticalEquipment = generateCriticalEquipment(facility),
            emergencyProtocol = generateEmergencyProtocol(facility, stage)
        )
    }
    
    private fun generateBackupPowerStatus(facility: HealthcareFacility): BackupPowerStatus {
        return when (facility.facilityType) {
            FacilityType.NATIONAL_HOSPITAL, FacilityType.PROVINCIAL_HOSPITAL -> 
                if (Random.nextBoolean()) BackupPowerStatus.FULLY_OPERATIONAL else BackupPowerStatus.PARTIAL
            FacilityType.REGIONAL_HOSPITAL, FacilityType.DISTRICT_HOSPITAL ->
                if (Random.nextBoolean()) BackupPowerStatus.PARTIAL else BackupPowerStatus.MINIMAL
            FacilityType.PRIVATE_HOSPITAL ->
                BackupPowerStatus.FULLY_OPERATIONAL
            else -> BackupPowerStatus.MINIMAL
        }
    }
    
    private fun generateCriticalEquipment(facility: HealthcareFacility): List<CriticalEquipment> {
        val equipment = mutableListOf<CriticalEquipment>()
        
        // Life support equipment (always present in hospitals)
        if (facility.facilityType in listOf(
                FacilityType.NATIONAL_HOSPITAL, 
                FacilityType.PROVINCIAL_HOSPITAL,
                FacilityType.REGIONAL_HOSPITAL,
                FacilityType.PRIVATE_HOSPITAL
            )) {
            equipment.addAll(listOf(
                CriticalEquipment("Ventilators", 500, 30, EquipmentPriority.LIFE_SUPPORT, true),
                CriticalEquipment("ICU Monitors", 200, 60, EquipmentPriority.CRITICAL_CARE, true),
                CriticalEquipment("Defibrillators", 300, 45, EquipmentPriority.CRITICAL_CARE, true),
                CriticalEquipment("Dialysis Machines", 1000, 15, EquipmentPriority.CRITICAL_CARE, false),
                CriticalEquipment("Blood Bank Refrigeration", 800, 120, EquipmentPriority.CRITICAL_CARE, false),
                CriticalEquipment("Operating Theatre Lights", 400, 90, EquipmentPriority.CRITICAL_CARE, true),
                CriticalEquipment("X-Ray Machines", 2000, 10, EquipmentPriority.DIAGNOSTIC, false),
                CriticalEquipment("CT Scanner", 5000, 5, EquipmentPriority.DIAGNOSTIC, false)
            ))
        }
        
        // Basic equipment for smaller facilities
        if (facility.facilityType in listOf(
                FacilityType.COMMUNITY_HEALTH_CENTER,
                FacilityType.PRIMARY_CLINIC
            )) {
            equipment.addAll(listOf(
                CriticalEquipment("Vaccine Refrigeration", 300, 240, EquipmentPriority.CRITICAL_CARE, false),
                CriticalEquipment("Emergency Lights", 100, 480, EquipmentPriority.SUPPORT, true),
                CriticalEquipment("Communication Systems", 50, 360, EquipmentPriority.SUPPORT, true)
            ))
        }
        
        return equipment
    }
    
    private fun generateEmergencyProtocol(
        facility: HealthcareFacility, 
        stage: LoadSheddingStage
    ): EmergencyProtocol {
        val actions = mutableListOf<String>()
        
        when (stage.riskLevel) {
            PowerRiskLevel.LOW -> {
                actions.addAll(listOf(
                    "Check backup generator fuel levels",
                    "Test UPS systems", 
                    "Prepare emergency lighting",
                    "Inform staff of upcoming outage"
                ))
            }
            PowerRiskLevel.MODERATE -> {
                actions.addAll(listOf(
                    "Activate backup generator 15 minutes before outage",
                    "Switch critical equipment to UPS", 
                    "Prepare manual procedures for power-dependent processes",
                    "Restrict non-essential electricity usage",
                    "Brief medical staff on emergency procedures"
                ))
            }
            PowerRiskLevel.HIGH, PowerRiskLevel.CRITICAL -> {
                actions.addAll(listOf(
                    "Activate full emergency power protocol",
                    "Prioritize life-support equipment",
                    "Consider transferring non-critical patients",
                    "Implement manual documentation systems",
                    "Activate emergency communication protocols",
                    "Consider postponing elective procedures",
                    "Prepare for extended outage duration"
                ))
            }
        }
        
        return EmergencyProtocol(
            actions = actions,
            contactNumbers = listOf(
                "Emergency Services: 10177",
                "Eskom Fault Reporting: 086-003-7566",
                "Municipal Emergency: ${facility.phoneNumber}",
                "Hospital Management: 24/7 On-call"
            ),
            evacuationPlan = if (stage.riskLevel == PowerRiskLevel.CRITICAL) {
                "Prepare vertical evacuation plan for ventilator-dependent patients to facilities with reliable backup power"
            } else null,
            medicalEmergencyProcedure = "Maintain life-support systems on backup power. Manual ventilation protocols ready. Emergency medications prepared."
        )
    }
    
    private fun checkBackupPowerStatus(facilityId: String): Boolean {
        // Simulate backup power readiness check
        return Random.nextFloat() > 0.2f // 80% chance backup power is ready
    }
    
    private fun generatePowerRecommendations(
        riskLevel: PowerRiskLevel,
        facilityId: String
    ): List<String> {
        return when (riskLevel) {
            PowerRiskLevel.LOW -> listOf(
                "Monitor power status regularly",
                "Ensure backup systems are tested", 
                "Maintain emergency supplies"
            )
            PowerRiskLevel.MODERATE -> listOf(
                "Prepare for scheduled outages",
                "Check fuel levels for generators",
                "Brief staff on emergency procedures",
                "Charge all portable medical devices"
            )
            PowerRiskLevel.HIGH -> listOf(
                "Activate emergency power protocols",
                "Consider rescheduling non-urgent procedures", 
                "Ensure critical patients are stable",
                "Prepare manual backup systems",
                "Contact suppliers for emergency fuel delivery"
            )
            PowerRiskLevel.CRITICAL -> listOf(
                "Implement full emergency response",
                "Consider patient transfers to stable facilities",
                "Activate disaster management protocols",
                "Ensure 72-hour fuel supply available", 
                "Coordinate with emergency services",
                "Implement water storage protocols"
            )
        }
    }
}

data class FacilityArea(
    val municipality: String,
    val area: String
)

data class PowerRiskAssessment(
    val facilityId: String,
    val currentRisk: PowerRiskLevel,
    val currentStage: LoadSheddingStage,
    val nextOutage: PowerOutageAlert?,
    val backupPowerReady: Boolean,
    val recommendations: List<String>
)