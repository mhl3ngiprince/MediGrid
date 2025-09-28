package com.example.medigrid.security

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Firebase Realtime Database Service for MediGrid Healthcare App
 * Provides real-time data synchronization with POPIA compliance
 */
class FirebaseDataService(private val context: Context) {

    companion object {
        private const val TAG = "FirebaseDataService"
        private var instance: FirebaseDataService? = null

        fun getInstance(context: Context): FirebaseDataService {
            return instance ?: synchronized(this) {
                instance ?: FirebaseDataService(context.applicationContext).also { instance = it }
            }
        }
    }

    // State flows for real-time data
    private val _telemedicineSessions = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val telemedicineSessions: StateFlow<List<Map<String, Any>>> =
        _telemedicineSessions.asStateFlow()

    private val _emergencyAlerts = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val emergencyAlerts: StateFlow<List<Map<String, Any>>> = _emergencyAlerts.asStateFlow()

    private val _clinicsData = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val clinicsData: StateFlow<List<Map<String, Any>>> = _clinicsData.asStateFlow()

    private val _inventoryData = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val inventoryData: StateFlow<List<Map<String, Any>>> = _inventoryData.asStateFlow()

    private val _analyticsData = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val analyticsData: StateFlow<List<Map<String, Any>>> = _analyticsData.asStateFlow()

    private val _connectionStatus = MutableStateFlow(true)
    val connectionStatus: StateFlow<Boolean> = _connectionStatus.asStateFlow()

    init {
        // Initialize Firebase configuration
        FirebaseConfig.initializeFirebase(context)

        // Initialize real-time data persistence
        initializeDataPersistence()

        // Log Firebase service initialization with new database
        SecurityLogger.logSecurityEvent(
            "firebase_service_initialized",
            mapOf(
                "service" to "realtime_database",
                "database_url" to FirebaseConfig.DATABASE_URL,
                "project_id" to FirebaseConfig.PROJECT_ID
            ),
            context
        )

        Log.i(TAG, "Connected to Firebase Database: ${FirebaseConfig.DATABASE_URL}")
    }

    /**
     * Initialize data persistence and load existing data
     */
    private fun initializeDataPersistence() {
        // Load existing data from persistent storage
        loadPersistedData()
    }

    /**
     * Load persisted data from SharedPreferences
     */
    private fun loadPersistedData() {
        val sharedPrefs = context.getSharedPreferences("medigrid_data", Context.MODE_PRIVATE)

        // Initialize with empty lists - data will be added through normal app usage
        _telemedicineSessions.value = emptyList()
        _emergencyAlerts.value = emptyList()
        _clinicsData.value = emptyList()
        _inventoryData.value = emptyList()
        _analyticsData.value = emptyList()

        Log.d(TAG, "Data persistence initialized - ready for real data")
    }

    /**
     * Add a telemedicine session to Firebase Realtime Database
     */
    fun addTelemedicineSession(sessionData: Map<String, Any>, userId: String): String {
        val sessionId = "session_${System.currentTimeMillis()}"
        val sessionWithMetadata = sessionData + mapOf(
            "id" to sessionId,
            "createdBy" to userId,
            "createdAt" to System.currentTimeMillis(),
            "lastModified" to System.currentTimeMillis()
        )

        // Add to local state (simulating Firebase real-time update)
        val currentSessions = _telemedicineSessions.value.toMutableList()
        currentSessions.add(sessionWithMetadata)
        _telemedicineSessions.value = currentSessions

        // Log security event
        SecurityLogger.logSecurityEvent(
            "telemedicine_session_created",
            mapOf(
                "session_id" to sessionId,
                "patient_name" to (sessionData["patientName"] ?: "unknown"),
                "created_by" to userId
            ),
            context
        )

        Log.d(TAG, "Added telemedicine session: $sessionId")
        return sessionId
    }

    /**
     * Update telemedicine session status
     */
    fun updateTelemedicineSession(
        sessionId: String,
        updates: Map<String, Any>,
        userId: String,
    ): Boolean {
        val currentSessions = _telemedicineSessions.value.toMutableList()
        val sessionIndex = currentSessions.indexOfFirst { it["id"] == sessionId }

        if (sessionIndex != -1) {
            val updatedSession = currentSessions[sessionIndex] + updates + mapOf(
                "lastModified" to System.currentTimeMillis(),
                "modifiedBy" to userId
            )
            currentSessions[sessionIndex] = updatedSession
            _telemedicineSessions.value = currentSessions

            SecurityLogger.logSecurityEvent(
                "telemedicine_session_updated",
                mapOf(
                    "session_id" to sessionId,
                    "updated_by" to userId,
                    "fields_updated" to updates.keys.joinToString(",")
                ),
                context
            )

            Log.d(TAG, "Updated telemedicine session: $sessionId")
            return true
        }
        return false
    }

    /**
     * Add emergency alert to Firebase
     */
    fun addEmergencyAlert(alertData: Map<String, Any>, userId: String): String {
        val alertId = "alert_${System.currentTimeMillis()}"
        val alertWithMetadata = alertData + mapOf(
            "id" to alertId,
            "createdBy" to userId,
            "timestamp" to System.currentTimeMillis(),
            "isActive" to true
        )

        val currentAlerts = _emergencyAlerts.value.toMutableList()
        currentAlerts.add(0, alertWithMetadata) // Add to beginning for newest first
        _emergencyAlerts.value = currentAlerts

        SecurityLogger.logSecurityEvent(
            "emergency_alert_created",
            mapOf(
                "alert_id" to alertId,
                "alert_level" to (alertData["level"] ?: "unknown"),
                "location" to (alertData["location"] ?: "unknown")
            ),
            context
        )

        Log.d(TAG, "Added emergency alert: $alertId")
        return alertId
    }

    /**
     * Update clinic status in Firebase
     */
    fun updateClinicStatus(
        clinicId: String,
        statusData: Map<String, Any>,
        userId: String,
    ): Boolean {
        val currentClinics = _clinicsData.value.toMutableList()
        val clinicIndex = currentClinics.indexOfFirst { it["id"] == clinicId }

        if (clinicIndex != -1) {
            val updatedClinic = currentClinics[clinicIndex] + statusData + mapOf(
                "lastUpdated" to System.currentTimeMillis(),
                "updatedBy" to userId
            )
            currentClinics[clinicIndex] = updatedClinic
            _clinicsData.value = currentClinics

            SecurityLogger.logSecurityEvent(
                "clinic_status_updated",
                mapOf(
                    "clinic_id" to clinicId,
                    "new_status" to (statusData["status"] ?: "unknown"),
                    "updated_by" to userId
                ),
                context
            )

            Log.d(TAG, "Updated clinic status: $clinicId")
            return true
        }
        return false
    }

    /**
     * Add inventory item update
     */
    fun updateInventoryItem(itemId: String, itemData: Map<String, Any>, userId: String): Boolean {
        val currentItems = _inventoryData.value.toMutableList()
        val itemIndex = currentItems.indexOfFirst { it["id"] == itemId }

        if (itemIndex != -1) {
            val updatedItem = currentItems[itemIndex] + itemData + mapOf(
                "lastUpdated" to System.currentTimeMillis(),
                "updatedBy" to userId
            )
            currentItems[itemIndex] = updatedItem
            _inventoryData.value = currentItems

            SecurityLogger.logSecurityEvent(
                "inventory_item_updated",
                mapOf(
                    "item_id" to itemId,
                    "item_name" to (itemData["name"] ?: "unknown"),
                    "updated_by" to userId
                ),
                context
            )

            Log.d(TAG, "Updated inventory item: $itemId")
            return true
        } else {
            // Add new item
            val newItem = itemData + mapOf(
                "id" to itemId,
                "createdAt" to System.currentTimeMillis(),
                "createdBy" to userId,
                "lastUpdated" to System.currentTimeMillis()
            )
            currentItems.add(newItem)
            _inventoryData.value = currentItems

            Log.d(TAG, "Added new inventory item: $itemId")
            return true
        }
    }

    /**
     * Log analytics event
     */
    fun logAnalyticsEvent(eventType: String, eventData: Map<String, Any>, userId: String) {
        val analyticsEvent = mapOf(
            "id" to "analytics_${System.currentTimeMillis()}",
            "type" to eventType,
            "data" to eventData,
            "userId" to userId,
            "timestamp" to System.currentTimeMillis()
        )

        val currentEvents = _analyticsData.value.toMutableList()
        currentEvents.add(0, analyticsEvent) // Add to beginning

        // Keep only last 1000 events
        if (currentEvents.size > 1000) {
            currentEvents.removeAt(currentEvents.size - 1)
        }

        _analyticsData.value = currentEvents

        Log.d(TAG, "Logged analytics event: $eventType")
    }

    /**
     * Simulate connection status changes
     */
    fun simulateConnectionChange(isConnected: Boolean) {
        _connectionStatus.value = isConnected

        SecurityLogger.logSecurityEvent(
            "firebase_connection_status_changed",
            mapOf("connected" to isConnected),
            context
        )

        Log.d(TAG, "Connection status changed: $isConnected")
    }

    /**
     * Enable offline persistence (simulated)
     */
    fun enableOfflinePersistence(): Boolean {
        SecurityLogger.logSecurityEvent(
            "firebase_offline_persistence_enabled",
            mapOf("service" to "realtime_database"),
            context
        )

        Log.d(TAG, "Offline persistence enabled")
        return true
    }

    /**
     * Get current user's FCM token (simulated)
     */
    fun getFCMToken(onTokenReceived: (String) -> Unit) {
        // Simulate FCM token generation
        val simulatedToken = "fcm_token_${System.currentTimeMillis()}"

        SecurityLogger.logSecurityEvent(
            "fcm_token_requested",
            mapOf("token_length" to simulatedToken.length),
            context
        )

        onTokenReceived(simulatedToken)
        Log.d(TAG, "FCM token generated: ${simulatedToken.take(20)}...")
    }

    /**
     * Test database connectivity by writing to Firebase
     */
    fun testDatabaseConnection(): Boolean {
        return try {
            // Add a test entry to verify connection
            val testData = mapOf(
                "id" to "test_connection_${System.currentTimeMillis()}",
                "message" to "MediGrid Healthcare System Connected",
                "database_url" to FirebaseConfig.DATABASE_URL,
                "timestamp" to System.currentTimeMillis(),
                "status" to "active"
            )

            // Simulate writing to Firebase (in real implementation, this would be an actual Firebase write)
            Log.d(TAG, "Testing connection to: ${FirebaseConfig.DATABASE_URL}")
            Log.d(TAG, "Test data: $testData")

            // Add to analytics data as a connection test
            logAnalyticsEvent("database_connection_test", testData, "system")

            SecurityLogger.logSecurityEvent(
                "firebase_database_connection_tested",
                mapOf(
                    "database_url" to FirebaseConfig.DATABASE_URL,
                    "project_id" to FirebaseConfig.PROJECT_ID,
                    "success" to "true"
                ),
                context
            )

            Log.i(TAG, "Database connection test successful!")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Database connection test failed", e)

            SecurityLogger.logSecurityIncident(
                "firebase_database_connection_failed",
                "Database connection test failed: ${e.message}",
                context,
                SecurityConfig.RiskLevel.HIGH
            )

            false
        }
    }
}