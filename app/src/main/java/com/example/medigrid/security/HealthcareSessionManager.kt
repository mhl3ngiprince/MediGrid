package com.example.medigrid.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom
import java.util.*
import org.json.JSONObject

/**
 * Healthcare Session Manager
 * Secure session management for healthcare applications
 */
class HealthcareSessionManager(private val context: Context) {

    private val preferences: SharedPreferences = context.getSharedPreferences(
        "medigurid_sessions",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val SESSION_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes
        private const val CLEANUP_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    }

    /**
     * Session data
     */
    data class Session(
        val sessionId: String,
        val userId: String,
        val username: String,
        val role: SecurityConfig.HealthcareRole,
        val clinicId: String,
        val createdAt: Long,
        val lastAccessed: Long,
        val expiresAt: Long,
        val permissions: Set<String>,
    )

    /**
     * Create new session for authenticated user
     */
    fun createSession(user: HealthcareAuthService.HealthcareUser): String {
        val sessionId = generateSessionId()
        val currentTime = System.currentTimeMillis()
        val expiresAt = currentTime + SESSION_TIMEOUT_MS

        val session = Session(
            sessionId = sessionId,
            userId = user.id,
            username = user.username,
            role = user.role,
            clinicId = user.clinicId,
            createdAt = currentTime,
            lastAccessed = currentTime,
            expiresAt = expiresAt,
            permissions = user.role.permissions
        )

        // Store session
        storeSession(session)

        // Log session creation
        SecurityLogger.logSecurityEvent(
            "session_created",
            mapOf(
                "user_id" to user.id,
                "session_id" to sessionId,
                "expires_at" to expiresAt
            ),
            context
        )

        // Schedule cleanup
        scheduleSessionCleanup()

        return sessionId
    }

    /**
     * Validate session and return session data
     */
    fun validateSession(sessionId: String): Session? {
        val sessionData = getSession(sessionId) ?: return null
        val currentTime = System.currentTimeMillis()

        // Check if session has expired
        if (sessionData.expiresAt < currentTime) {
            invalidateSession(sessionId)
            SecurityLogger.logSecurityEvent(
                "session_expired",
                mapOf("session_id" to sessionId),
                context
            )
            return null
        }

        // Update last accessed time and extend session
        val updatedSession = sessionData.copy(
            lastAccessed = currentTime,
            expiresAt = currentTime + SESSION_TIMEOUT_MS
        )

        storeSession(updatedSession)

        return updatedSession
    }

    /**
     * Invalidate session
     */
    fun invalidateSession(sessionId: String): Boolean {
        return try {
            preferences.edit().remove("session_$sessionId").apply()
            SecurityLogger.logSecurityEvent(
                "session_invalidated",
                mapOf("session_id" to sessionId),
                context
            )
            true
        } catch (e: Exception) {
            SecurityLogger.logSecurityIncident(
                "session_invalidation_error",
                "Failed to invalidate session $sessionId: ${e.message}",
                context
            )
            false
        }
    }

    /**
     * Invalidate all sessions for a user
     */
    fun invalidateAllUserSessions(userId: String): Boolean {
        return try {
            val allSessions = preferences.all
            val userSessions = allSessions.filter { entry ->
                entry.key.startsWith("session_") &&
                        entry.value.toString().contains("\"userId\":\"$userId\"")
            }

            val editor = preferences.edit()
            userSessions.forEach { entry ->
                editor.remove(entry.key)
            }
            editor.apply()

            SecurityLogger.logSecurityEvent(
                "all_user_sessions_invalidated",
                mapOf(
                    "user_id" to userId,
                    "sessions_count" to userSessions.size
                ),
                context
            )

            true
        } catch (e: Exception) {
            SecurityLogger.logSecurityIncident(
                "user_sessions_invalidation_error",
                "Failed to invalidate user sessions for $userId: ${e.message}",
                context
            )
            false
        }
    }

    /**
     * Get active sessions count for monitoring
     */
    fun getActiveSessionsCount(): Int {
        val currentTime = System.currentTimeMillis()
        var activeCount = 0

        preferences.all.forEach { entry ->
            if (entry.key.startsWith("session_")) {
                try {
                    val sessionJson = JSONObject(entry.value.toString())
                    val expiresAt = sessionJson.getLong("expiresAt")
                    if (expiresAt > currentTime) {
                        activeCount++
                    }
                } catch (e: Exception) {
                    // Ignore malformed sessions
                }
            }
        }

        return activeCount
    }

    /**
     * Generate secure session ID
     */
    private fun generateSessionId(): String {
        val random = ByteArray(32)
        SecureRandom().nextBytes(random)
        return Base64.encodeToString(random, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    /**
     * Store session in secure preferences
     */
    private fun storeSession(session: Session) {
        val sessionJson = JSONObject().apply {
            put("sessionId", session.sessionId)
            put("userId", session.userId)
            put("username", session.username)
            put("role", session.role.name)
            put("clinicId", session.clinicId)
            put("createdAt", session.createdAt)
            put("lastAccessed", session.lastAccessed)
            put("expiresAt", session.expiresAt)
            put("permissions", session.permissions.joinToString(","))
        }

        preferences.edit()
            .putString("session_${session.sessionId}", sessionJson.toString())
            .apply()
    }

    /**
     * Retrieve session from secure preferences
     */
    private fun getSession(sessionId: String): Session? {
        return try {
            val sessionData = preferences.getString("session_$sessionId", null) ?: return null
            val sessionJson = JSONObject(sessionData)

            Session(
                sessionId = sessionJson.getString("sessionId"),
                userId = sessionJson.getString("userId"),
                username = sessionJson.getString("username"),
                role = SecurityConfig.HealthcareRole.valueOf(sessionJson.getString("role")),
                clinicId = sessionJson.getString("clinicId"),
                createdAt = sessionJson.getLong("createdAt"),
                lastAccessed = sessionJson.getLong("lastAccessed"),
                expiresAt = sessionJson.getLong("expiresAt"),
                permissions = sessionJson.getString("permissions").split(",").toSet()
            )
        } catch (e: Exception) {
            SecurityLogger.logSecurityIncident(
                "session_retrieval_error",
                "Failed to retrieve session $sessionId: ${e.message}",
                context
            )
            null
        }
    }

    /**
     * Schedule periodic cleanup of expired sessions
     */
    private fun scheduleSessionCleanup() {
        // In a real implementation, this would use a proper scheduler
        // For now, we'll do cleanup on session operations
        cleanupExpiredSessions()
    }

    /**
     * Clean up expired sessions
     */
    private fun cleanupExpiredSessions() {
        val currentTime = System.currentTimeMillis()
        val expiredSessions = mutableListOf<String>()

        preferences.all.forEach { entry ->
            if (entry.key.startsWith("session_")) {
                try {
                    val sessionJson = JSONObject(entry.value.toString())
                    val expiresAt = sessionJson.getLong("expiresAt")
                    if (expiresAt < currentTime) {
                        expiredSessions.add(entry.key)
                    }
                } catch (e: Exception) {
                    // Remove malformed sessions
                    expiredSessions.add(entry.key)
                }
            }
        }

        if (expiredSessions.isNotEmpty()) {
            val editor = preferences.edit()
            expiredSessions.forEach { sessionKey ->
                editor.remove(sessionKey)
            }
            editor.apply()

            SecurityLogger.logSecurityEvent(
                "expired_sessions_cleaned",
                mapOf("count" to expiredSessions.size),
                context
            )
        }
    }
}