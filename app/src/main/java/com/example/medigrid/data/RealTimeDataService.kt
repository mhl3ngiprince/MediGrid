package com.example.medigrid.data

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * Comprehensive real-time data service for the entire MediGrid application
 * Provides live updates for all screens and components
 */
class RealTimeDataService private constructor(private val context: Context) {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Real-time data flows
    private val _systemStats = MutableStateFlow(SystemStats())
    val systemStats: StateFlow<SystemStats> = _systemStats.asStateFlow()

    private val _networkActivity = MutableStateFlow<List<NetworkActivity>>(emptyList())
    val networkActivity: StateFlow<List<NetworkActivity>> = _networkActivity.asStateFlow()

    private val _patientActivity = MutableStateFlow<List<PatientActivity>>(emptyList())
    val patientActivity: StateFlow<List<PatientActivity>> = _patientActivity.asStateFlow()

    private val _emergencyAlerts = MutableStateFlow<List<EmergencyAlert>>(emptyList())
    val emergencyAlerts: StateFlow<List<EmergencyAlert>> = _emergencyAlerts.asStateFlow()

    private val _inventoryUpdates = MutableStateFlow<List<InventoryUpdate>>(emptyList())
    val inventoryUpdates: StateFlow<List<InventoryUpdate>> = _inventoryUpdates.asStateFlow()

    private val _clinicStatus = MutableStateFlow<Map<String, RealTimeClinicStatus>>(emptyMap())
    val clinicStatus: StateFlow<Map<String, RealTimeClinicStatus>> = _clinicStatus.asStateFlow()

    private val _powerStatus = MutableStateFlow(PowerStatus())
    val powerStatus: StateFlow<PowerStatus> = _powerStatus.asStateFlow()

    private val _healthMetrics = MutableStateFlow(HealthMetrics())
    val healthMetrics: StateFlow<HealthMetrics> = _healthMetrics.asStateFlow()

    private val _telemedicineActivity = MutableStateFlow<List<TelemedicineSession>>(emptyList())
    val telemedicineActivity: StateFlow<List<TelemedicineSession>> =
        _telemedicineActivity.asStateFlow()

    init {
        startRealTimeUpdates()
    }

    private fun startRealTimeUpdates() {
        // Update system stats every 5 seconds
        serviceScope.launch {
            while (true) {
                updateSystemStats()
                delay(5000)
            }
        }

        // Update network activity every 3 seconds
        serviceScope.launch {
            while (true) {
                updateNetworkActivity()
                delay(3000)
            }
        }

        // Update patient activity every 10 seconds
        serviceScope.launch {
            while (true) {
                updatePatientActivity()
                delay(10000)
            }
        }

        // Update emergency alerts every 15 seconds
        serviceScope.launch {
            while (true) {
                updateEmergencyAlerts()
                delay(15000)
            }
        }

        // Update inventory every 30 seconds
        serviceScope.launch {
            while (true) {
                updateInventoryStatus()
                delay(30000)
            }
        }

        // Update clinic status every 20 seconds
        serviceScope.launch {
            while (true) {
                updateClinicStatus()
                delay(20000)
            }
        }

        // Update power status every 8 seconds
        serviceScope.launch {
            while (true) {
                updatePowerStatus()
                delay(8000)
            }
        }

        // Update health metrics every 12 seconds
        serviceScope.launch {
            while (true) {
                updateHealthMetrics()
                delay(12000)
            }
        }

        // Update telemedicine activity every 7 seconds
        serviceScope.launch {
            while (true) {
                updateTelemedicineActivity()
                delay(7000)
            }
        }
    }

    private fun updateSystemStats() {
        val current = _systemStats.value
        _systemStats.value = current.copy(
            activeUsers = Random.nextInt(85, 156),
            totalSessions = current.totalSessions + Random.nextInt(0, 3),
            cpuUsage = Random.nextInt(15, 85),
            memoryUsage = Random.nextInt(25, 75),
            networkLatency = Random.nextInt(12, 95),
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun updateNetworkActivity() {
        val activities = listOf(
            NetworkActivity(
                "net_001",
                "Johannesburg General",
                "Data Sync",
                System.currentTimeMillis(),
                "ACTIVE"
            ),
            NetworkActivity(
                "net_002",
                "Cape Town Medical",
                "Patient Transfer",
                System.currentTimeMillis() - 30000,
                "COMPLETED"
            ),
            NetworkActivity(
                "net_003",
                "Durban Clinic Network",
                "Inventory Update",
                System.currentTimeMillis() - 120000,
                "ACTIVE"
            ),
            NetworkActivity(
                "net_004",
                "Pretoria Healthcare",
                "Emergency Alert",
                System.currentTimeMillis() - 45000,
                "URGENT"
            ),
            NetworkActivity(
                "net_005",
                "Port Elizabeth Regional",
                "Scheduled Backup",
                System.currentTimeMillis() - 300000,
                "COMPLETED"
            )
        ).shuffled().take(Random.nextInt(3, 6))

        _networkActivity.value = activities
    }

    private fun updatePatientActivity() {
        val activities = listOf(
            PatientActivity("PAT001", "J.M.", "Check-in", "Cardiology", System.currentTimeMillis()),
            PatientActivity(
                "PAT002",
                "S.N.",
                "Lab Results",
                "Pathology",
                System.currentTimeMillis() - 180000
            ),
            PatientActivity(
                "PAT003",
                "M.P.",
                "Prescription",
                "Pharmacy",
                System.currentTimeMillis() - 420000
            ),
            PatientActivity(
                "PAT004",
                "T.K.",
                "Appointment",
                "General Practice",
                System.currentTimeMillis() - 600000
            ),
            PatientActivity(
                "PAT005",
                "A.D.",
                "Discharge",
                "Emergency",
                System.currentTimeMillis() - 900000
            ),
            PatientActivity(
                "PAT006",
                "L.V.",
                "Registration",
                "Reception",
                System.currentTimeMillis() - 1200000
            )
        ).shuffled().take(Random.nextInt(4, 7))

        _patientActivity.value = activities
    }

    private fun updateEmergencyAlerts() {
        val shouldAddAlert = Random.nextFloat() < 0.3f // 30% chance of new alert

        if (shouldAddAlert) {
            val newAlert = EmergencyAlert(
                id = "EMG${System.currentTimeMillis()}",
                type = listOf(
                    "CARDIAC_ARREST",
                    "TRAUMA",
                    "STROKE",
                    "RESPIRATORY_FAILURE",
                    "OVERDOSE"
                ).random(),
                location = listOf(
                    "Johannesburg General",
                    "Cape Town Medical",
                    "Durban Central",
                    "Pretoria Regional"
                ).random(),
                severity = listOf("HIGH", "CRITICAL", "MODERATE").random(),
                timestamp = System.currentTimeMillis(),
                status = "ACTIVE"
            )

            val current = _emergencyAlerts.value.toMutableList()
            current.add(0, newAlert) // Add to top

            // Keep only last 10 alerts
            if (current.size > 10) {
                current.removeAt(current.size - 1)
            }

            _emergencyAlerts.value = current
        }
    }

    private fun updateInventoryStatus() {
        val updates = listOf(
            InventoryUpdate(
                "INV001",
                "Paracetamol 500mg",
                Random.nextInt(45, 200),
                "LOW",
                System.currentTimeMillis()
            ),
            InventoryUpdate(
                "INV002",
                "Amoxicillin 250mg",
                Random.nextInt(120, 500),
                "NORMAL",
                System.currentTimeMillis()
            ),
            InventoryUpdate(
                "INV003",
                "Insulin Rapid Acting",
                Random.nextInt(15, 80),
                "CRITICAL",
                System.currentTimeMillis()
            ),
            InventoryUpdate(
                "INV004",
                "Morphine 10mg",
                Random.nextInt(25, 100),
                "LOW",
                System.currentTimeMillis()
            ),
            InventoryUpdate(
                "INV005",
                "Bandages (Sterile)",
                Random.nextInt(200, 800),
                "NORMAL",
                System.currentTimeMillis()
            )
        ).shuffled().take(Random.nextInt(2, 4))

        _inventoryUpdates.value = updates
    }

    private fun updateClinicStatus() {
        val clinics = mapOf(
            "JHB001" to RealTimeClinicStatus(
                id = "JHB001",
                name = "Johannesburg General Hospital",
                status = listOf("OPERATIONAL", "BUSY", "MAINTENANCE").random(),
                patientCount = Random.nextInt(45, 120),
                staffCount = Random.nextInt(25, 60),
                bedAvailability = Random.nextInt(5, 45),
                powerStatus = listOf("STABLE", "BACKUP", "LOAD_SHEDDING").random(),
                lastUpdate = System.currentTimeMillis()
            ),
            "CPT001" to RealTimeClinicStatus(
                id = "CPT001",
                name = "Cape Town Medical Center",
                status = listOf("OPERATIONAL", "BUSY").random(),
                patientCount = Random.nextInt(30, 85),
                staffCount = Random.nextInt(20, 45),
                bedAvailability = Random.nextInt(8, 35),
                powerStatus = listOf("STABLE", "BACKUP").random(),
                lastUpdate = System.currentTimeMillis()
            ),
            "DBN001" to RealTimeClinicStatus(
                id = "DBN001",
                name = "Durban Regional Clinic",
                status = listOf("OPERATIONAL", "MAINTENANCE").random(),
                patientCount = Random.nextInt(25, 75),
                staffCount = Random.nextInt(15, 35),
                bedAvailability = Random.nextInt(10, 40),
                powerStatus = "STABLE",
                lastUpdate = System.currentTimeMillis()
            )
        )

        _clinicStatus.value = clinics
    }

    private fun updatePowerStatus() {
        _powerStatus.value = PowerStatus(
            nationalGridStatus = listOf("STABLE", "STRAINED", "CRITICAL").random(),
            loadSheddingStage = Random.nextInt(0, 7),
            generatorStatus = listOf("OPERATIONAL", "STANDBY", "MAINTENANCE").random(),
            batteryLevel = Random.nextInt(45, 100),
            estimatedUptime = Random.nextInt(120, 480), // minutes
            affectedClinics = Random.nextInt(0, 12),
            lastUpdate = System.currentTimeMillis()
        )
    }

    private fun updateHealthMetrics() {
        _healthMetrics.value = HealthMetrics(
            totalPatients = Random.nextInt(12450, 12600),
            dailyAdmissions = Random.nextInt(45, 120),
            averageWaitTime = Random.nextInt(15, 75), // minutes
            criticalCases = Random.nextInt(3, 18),
            bedOccupancyRate = Random.nextInt(65, 95), // percentage
            staffUtilization = Random.nextInt(70, 98), // percentage
            medicineStockLevel = Random.nextInt(55, 95), // percentage
            emergencyResponseTime = Random.nextInt(8, 25), // minutes
            lastUpdate = System.currentTimeMillis()
        )
    }

    private fun updateTelemedicineActivity() {
        val sessions = listOf(
            TelemedicineSession(
                "TM001",
                "Dr. Smith",
                "Patient Consultation",
                "ACTIVE",
                System.currentTimeMillis()
            ),
            TelemedicineSession(
                "TM002",
                "Dr. Patel",
                "Follow-up",
                "COMPLETED",
                System.currentTimeMillis() - 300000
            ),
            TelemedicineSession(
                "TM003",
                "Nurse Johnson",
                "Health Check",
                "SCHEDULED",
                System.currentTimeMillis() + 600000
            ),
            TelemedicineSession(
                "TM004",
                "Dr. Khumalo",
                "Emergency Consult",
                "URGENT",
                System.currentTimeMillis() - 120000
            )
        ).shuffled().take(Random.nextInt(2, 5))

        _telemedicineActivity.value = sessions
    }

    fun cleanup() {
        serviceScope.cancel()
    }

    companion object {
        @Volatile
        private var INSTANCE: RealTimeDataService? = null

        fun getInstance(context: Context): RealTimeDataService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RealTimeDataService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

// Data models for real-time information
data class SystemStats(
    val activeUsers: Int = 127,
    val totalSessions: Int = 1547,
    val cpuUsage: Int = 45,
    val memoryUsage: Int = 62,
    val networkLatency: Int = 28,
    val lastUpdated: Long = System.currentTimeMillis(),
)

data class NetworkActivity(
    val id: String,
    val location: String,
    val activity: String,
    val timestamp: Long,
    val status: String,
)

data class PatientActivity(
    val patientId: String,
    val initials: String,
    val activity: String,
    val department: String,
    val timestamp: Long,
)

data class EmergencyAlert(
    val id: String,
    val type: String,
    val location: String,
    val severity: String,
    val timestamp: Long,
    val status: String,
)

data class InventoryUpdate(
    val itemId: String,
    val itemName: String,
    val quantity: Int,
    val status: String,
    val lastUpdated: Long,
)

data class RealTimeClinicStatus(
    val id: String,
    val name: String,
    val status: String,
    val patientCount: Int,
    val staffCount: Int,
    val bedAvailability: Int,
    val powerStatus: String,
    val lastUpdate: Long,
)

data class PowerStatus(
    val nationalGridStatus: String = "STABLE",
    val loadSheddingStage: Int = 0,
    val generatorStatus: String = "STANDBY",
    val batteryLevel: Int = 85,
    val estimatedUptime: Int = 240, // minutes
    val affectedClinics: Int = 0,
    val lastUpdate: Long = System.currentTimeMillis(),
)

data class HealthMetrics(
    val totalPatients: Int = 12567,
    val dailyAdmissions: Int = 89,
    val averageWaitTime: Int = 42, // minutes
    val criticalCases: Int = 7,
    val bedOccupancyRate: Int = 78, // percentage
    val staffUtilization: Int = 85, // percentage
    val medicineStockLevel: Int = 72, // percentage
    val emergencyResponseTime: Int = 12, // minutes
    val lastUpdate: Long = System.currentTimeMillis(),
)

data class TelemedicineSession(
    val sessionId: String,
    val provider: String,
    val type: String,
    val status: String,
    val timestamp: Long,
)

// Utility functions for formatting
fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000} min ago"
        diff < 86400000 -> "${diff / 3600000} hours ago"
        else -> "${diff / 86400000} days ago"
    }
}

fun formatDateTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestamp))
}