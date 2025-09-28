package com.example.medigrid.security

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Healthcare Security Logger
 * POPIA-compliant logging system for PHI access and security incidents
 */
object SecurityLogger {

    private const val TAG = "MediGridSecurity"
    private const val PHI_ACCESS_LOG = "phi_access.log"
    private const val SECURITY_INCIDENT_LOG = "security_incidents.log"
    private const val COMPLIANCE_AUDIT_LOG = "compliance_audit.log"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    /**
     * Log PHI access for compliance monitoring
     */
    fun logPhiAccess(
        userId: String,
        patientId: String,
        accessType: String,
        purpose: String,
        context: Context,
    ) {
        val logEntry = JSONObject().apply {
            put("timestamp", dateFormat.format(Date()))
            put("event_type", "PHI_ACCESS")
            put("user_id", hashSensitiveData(userId))
            put("patient_id", hashSensitiveData(patientId))
            put("access_type", accessType)
            put("purpose", purpose)
            put("session_id", getCurrentSessionId())
            put("device_id", getDeviceId(context))
            put("app_version", getAppVersion(context))
        }

        writeToSecureLog(PHI_ACCESS_LOG, logEntry.toString(), context)

        // Also log to Android system logs (without sensitive data)
        Log.i(TAG, "PHI Access: User accessed patient data - Type: $accessType, Purpose: $purpose")
    }

    /**
     * Log security incidents for immediate response
     */
    fun logSecurityIncident(
        incidentType: String,
        details: String,
        context: Context,
        severity: SecurityConfig.RiskLevel = SecurityConfig.RiskLevel.HIGH,
    ) {
        val logEntry = JSONObject().apply {
            put("timestamp", dateFormat.format(Date()))
            put("event_type", "SECURITY_INCIDENT")
            put("incident_type", incidentType)
            put("severity", severity.name)
            put("risk_score", severity.score)
            put("details", details)
            put("session_id", getCurrentSessionId())
            put("device_id", getDeviceId(context))
            put("app_version", getAppVersion(context))
        }

        writeToSecureLog(SECURITY_INCIDENT_LOG, logEntry.toString(), context)

        // Log to Android system with appropriate severity
        when (severity) {
            SecurityConfig.RiskLevel.CRITICAL -> Log.e(
                TAG,
                "CRITICAL SECURITY INCIDENT: $incidentType - $details"
            )

            SecurityConfig.RiskLevel.HIGH -> Log.w(
                TAG,
                "HIGH SECURITY INCIDENT: $incidentType - $details"
            )

            else -> Log.i(TAG, "Security Incident: $incidentType - $details")
        }
    }

    /**
     * Log general security events for monitoring
     */
    fun logSecurityEvent(
        eventType: String,
        parameters: Map<String, Any>,
        context: Context,
    ) {
        val logEntry = JSONObject().apply {
            put("timestamp", dateFormat.format(Date()))
            put("event_type", "SECURITY_EVENT")
            put("event_name", eventType)
            put("session_id", getCurrentSessionId())
            put("device_id", getDeviceId(context))

            for ((key, value) in parameters) {
                put(key, value)
            }
        }

        writeToSecureLog(COMPLIANCE_AUDIT_LOG, logEntry.toString(), context)
        Log.d(TAG, "Security Event: $eventType")
    }

    /**
     * Log authentication events
     */
    fun logAuthenticationEvent(
        userId: String,
        eventType: String,
        success: Boolean,
        details: String? = null,
        context: Context,
    ) {
        val logEntry = JSONObject().apply {
            put("timestamp", dateFormat.format(Date()))
            put("event_type", "AUTHENTICATION")
            put("user_id", hashSensitiveData(userId))
            put("auth_event", eventType)
            put("success", success)
            put("device_id", getDeviceId(context))
            details?.let { put("details", it) }
        }

        writeToSecureLog(SECURITY_INCIDENT_LOG, logEntry.toString(), context)

        val logLevel = if (success) "INFO" else "WARN"
        val message = "Authentication $eventType: ${if (success) "SUCCESS" else "FAILED"}"

        if (success) {
            Log.i(TAG, message)
        } else {
            Log.w(TAG, message + (details?.let { " - $it" } ?: ""))
        }
    }

    /**
     * Write log entry to secure internal storage
     */
    private fun writeToSecureLog(filename: String, logEntry: String, context: Context) {
        try {
            val logsDir = File(context.filesDir, "secure_logs")
            if (!logsDir.exists()) {
                logsDir.mkdirs()
            }

            val logFile = File(logsDir, filename)
            FileWriter(logFile, true).use { writer ->
                writer.appendLine(logEntry)
                writer.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write security log: ${e.message}")
        }
    }

    /**
     * Hash sensitive data for logging (one-way hash for audit trails)
     */
    private fun hashSensitiveData(data: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(data.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }.substring(0, 16) // First 16 chars
        } catch (e: Exception) {
            "hash_error"
        }
    }

    /**
     * Get current session ID (mock implementation)
     */
    private fun getCurrentSessionId(): String {
        // In real implementation, this would come from session manager
        return "session_${System.currentTimeMillis()}"
    }

    /**
     * Get device ID for audit trails
     */
    private fun getDeviceId(context: Context): String {
        // In real implementation, use Android ID or other secure identifier
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }

    /**
     * Get app version for logging
     */
    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "unknown_version"
        }
    }
}