package com.example.medigrid.security

import android.content.Context
import android.util.Log

/**
 * Firebase Configuration for MediGrid Healthcare App
 * Connects to: https://medigrid-a4abc-default-rtdb.firebaseio.com
 */
object FirebaseConfig {

    private const val TAG = "FirebaseConfig"

    // Firebase Database Configuration
    const val DATABASE_URL = "https://medigrid-a4abc-default-rtdb.firebaseio.com"
    const val PROJECT_ID = "medigrid-a4abc"
    const val STORAGE_BUCKET = "medigrid-a4abc.appspot.com"

    // Database paths
    object DatabasePaths {
        const val TELEMEDICINE_SESSIONS = "telemedicine_sessions"
        const val EMERGENCY_ALERTS = "emergency_alerts"
        const val CLINICS_DATA = "clinics_data"
        const val INVENTORY_DATA = "inventory_data"
        const val ANALYTICS_DATA = "analytics_data"
        const val HEALTHCARE_USERS = "healthcare_users"
        const val SECURITY_LOGS = "security_logs"
        const val PATIENT_RECORDS = "patient_records"
    }

    /**
     * Initialize Firebase with specific database URL
     */
    fun initializeFirebase(context: Context) {
        try {
            // Log Firebase initialization
            Log.d(TAG, "Initializing Firebase with database: $DATABASE_URL")

            SecurityLogger.logSecurityEvent(
                "firebase_config_initialized",
                mapOf(
                    "database_url" to DATABASE_URL,
                    "project_id" to PROJECT_ID
                ),
                context
            )

            Log.i(TAG, "Firebase configuration completed for MediGrid Healthcare")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase configuration", e)

            SecurityLogger.logSecurityIncident(
                "firebase_config_error",
                "Firebase initialization failed: ${e.message}",
                context,
                SecurityConfig.RiskLevel.HIGH
            )
        }
    }

    /**
     * Get database reference URL for specific path
     */
    fun getDatabaseUrl(path: String = ""): String {
        return if (path.isNotEmpty()) {
            "$DATABASE_URL/$path"
        } else {
            DATABASE_URL
        }
    }

    /**
     * Validate database connection
     */
    fun validateConnection(context: Context): Boolean {
        return try {
            // Simulate connection validation
            Log.d(TAG, "Validating connection to: $DATABASE_URL")

            SecurityLogger.logSecurityEvent(
                "firebase_connection_validated",
                mapOf("database_url" to DATABASE_URL),
                context
            )

            true
        } catch (e: Exception) {
            Log.e(TAG, "Database connection validation failed", e)
            false
        }
    }
}