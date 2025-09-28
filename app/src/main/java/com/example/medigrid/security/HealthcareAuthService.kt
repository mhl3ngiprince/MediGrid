package com.example.medigrid.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * Healthcare Authentication Service
 * POPIA-compliant authentication with MFA for healthcare workers
 */
class HealthcareAuthService(private val context: Context) {

    private val preferences: SharedPreferences = context.getSharedPreferences(
        "medigurid_auth_prefs",
        Context.MODE_PRIVATE
    )
    private val sessionManager = HealthcareSessionManager(context)

    /**
     * Authentication result
     */
    data class AuthResult(
        val success: Boolean,
        val user: HealthcareUser? = null,
        val accessToken: String? = null,
        val refreshToken: String? = null,
        val error: String? = null,
        val requiresMfa: Boolean = false,
        val mfaSecret: String? = null,
    )

    /**
     * Healthcare user model
     */
    data class HealthcareUser(
        val id: String,
        val username: String,
        val role: SecurityConfig.HealthcareRole,
        val clinicId: String,
        val phiAccessLevel: String,
        val mfaEnabled: Boolean,
        val lastLogin: Date? = null,
        val failedAttempts: Int = 0,
        val accountLocked: Boolean = false,
        val lockoutUntil: Date? = null,
    )

    /**
     * Authenticate healthcare worker with username and password
     */
    fun authenticateUser(username: String, password: String): AuthResult {
        try {
            // Input validation
            if (username.isBlank() || password.isBlank()) {
                SecurityLogger.logAuthenticationEvent(
                    username,
                    "login_attempt",
                    false,
                    "Empty credentials",
                    context
                )
                return AuthResult(false, error = "Invalid credentials")
            }

            // Get user from secure storage (mock implementation)
            val user = getUserByUsername(username)
            if (user == null) {
                SecurityLogger.logAuthenticationEvent(
                    username,
                    "login_attempt",
                    false,
                    "User not found",
                    context
                )
                return AuthResult(false, error = "Invalid credentials")
            }

            // Check account lockout
            if (isAccountLocked(user)) {
                SecurityLogger.logSecurityIncident(
                    "locked_account_access_attempt",
                    "Attempt to access locked account: $username",
                    context,
                    SecurityConfig.RiskLevel.HIGH
                )
                return AuthResult(false, error = "Account is locked")
            }

            // Verify password
            if (!verifyPassword(password, user.id)) {
                incrementFailedAttempts(user.id)
                SecurityLogger.logAuthenticationEvent(
                    username,
                    "login_attempt",
                    false,
                    "Invalid password",
                    context
                )
                return AuthResult(false, error = "Invalid credentials")
            }

            // Check if MFA is required
            if (user.mfaEnabled) {
                val mfaSecret = generateMfaSetupSecret(user.id)
                return AuthResult(
                    success = false,
                    requiresMfa = true,
                    mfaSecret = mfaSecret,
                    user = user
                )
            }

            // Generate session tokens
            val accessToken = sessionManager.createSession(user)
            val refreshToken = generateRefreshToken(user.id)

            // Reset failed attempts and update last login
            resetFailedAttempts(user.id)
            updateLastLogin(user.id)

            SecurityLogger.logAuthenticationEvent(
                username,
                "login_success",
                true,
                "Successful login",
                context
            )

            return AuthResult(
                success = true,
                user = user,
                accessToken = accessToken,
                refreshToken = refreshToken
            )

        } catch (e: Exception) {
            SecurityLogger.logSecurityIncident(
                "authentication_error",
                "Authentication error for user $username: ${e.message}",
                context,
                SecurityConfig.RiskLevel.HIGH
            )
            return AuthResult(false, error = "Authentication failed")
        }
    }

    /**
     * Complete MFA authentication
     */
    fun completeMfaAuthentication(username: String, mfaToken: String): AuthResult {
        try {
            val user = getUserByUsername(username)
                ?: return AuthResult(false, error = "Invalid session")

            if (!verifyMfaToken(user.id, mfaToken)) {
                incrementFailedAttempts(user.id)
                SecurityLogger.logAuthenticationEvent(
                    username,
                    "mfa_verification",
                    false,
                    "Invalid MFA token",
                    context
                )
                return AuthResult(false, error = "Invalid MFA token")
            }

            // Generate session tokens
            val accessToken = sessionManager.createSession(user)
            val refreshToken = generateRefreshToken(user.id)

            // Reset failed attempts and update last login
            resetFailedAttempts(user.id)
            updateLastLogin(user.id)

            SecurityLogger.logAuthenticationEvent(
                username,
                "mfa_success",
                true,
                "Successful MFA authentication",
                context
            )

            return AuthResult(
                success = true,
                user = user,
                accessToken = accessToken,
                refreshToken = refreshToken
            )

        } catch (e: Exception) {
            SecurityLogger.logSecurityIncident(
                "mfa_authentication_error",
                "MFA authentication error for user $username: ${e.message}",
                context,
                SecurityConfig.RiskLevel.HIGH
            )
            return AuthResult(false, error = "Authentication failed")
        }
    }

    /**
     * Verify user permissions for specific healthcare actions
     */
    fun hasPermission(user: HealthcareUser, permission: String): Boolean {
        return user.role.permissions.contains(permission)
    }

    /**
     * Check if user can access specific patient PHI
     */
    fun canAccessPatientPhi(user: HealthcareUser, patientId: String, purpose: String): Boolean {
        // Check role-based permissions
        if (!hasPermission(user, "READ_PHI")) {
            return false
        }

        // Log PHI access attempt
        SecurityLogger.logPhiAccess(
            user.id,
            patientId,
            "access_check",
            purpose,
            context
        )

        // In a real implementation, this would check:
        // - Patient consent
        // - Treatment relationship
        // - Emergency access rules
        // - Break-glass procedures

        return true
    }

    /**
     * Logout and invalidate session
     */
    fun logout(accessToken: String): Boolean {
        return try {
            sessionManager.invalidateSession(accessToken)
            SecurityLogger.logSecurityEvent(
                "user_logout",
                mapOf("token" to "present"),
                context
            )
            true
        } catch (e: Exception) {
            SecurityLogger.logSecurityIncident(
                "logout_error",
                "Error during logout: ${e.message}",
                context
            )
            false
        }
    }

    /**
     * Mock implementation - get user by username
     */
    private fun getUserByUsername(username: String): HealthcareUser? {
        // In real implementation, this would query encrypted database
        return when (username) {
            "dr.smith" -> HealthcareUser(
                id = "usr_001",
                username = "dr.smith",
                role = SecurityConfig.HealthcareRole.DOCTOR,
                clinicId = "clinic_001",
                phiAccessLevel = "FULL",
                mfaEnabled = true
            )

            "nurse.jane" -> HealthcareUser(
                id = "usr_002",
                username = "nurse.jane",
                role = SecurityConfig.HealthcareRole.NURSE,
                clinicId = "clinic_001",
                phiAccessLevel = "BASIC",
                mfaEnabled = true
            )

            else -> null
        }
    }

    /**
     * Verify password against stored hash
     */
    private fun verifyPassword(password: String, userId: String): Boolean {
        // In real implementation, use proper password hashing (bcrypt, Argon2, etc.)
        val storedHash = preferences.getString("password_hash_$userId", null)
        return storedHash != null && verifyPasswordHash(password, storedHash)
    }

    /**
     * Verify password hash (mock implementation)
     */
    private fun verifyPasswordHash(password: String, hash: String): Boolean {
        // This is a simplified implementation
        // Real implementation would use bcrypt or Argon2
        return hashPassword(password) == hash
    }

    /**
     * Hash password (simplified for demo)
     */
    private fun hashPassword(password: String): String {
        // This is a simplified implementation
        // Real implementation would use bcrypt or Argon2
        return password.hashCode().toString()
    }

    /**
     * Check if account is locked
     */
    private fun isAccountLocked(user: HealthcareUser): Boolean {
        val lockoutUntil = preferences.getLong("lockout_until_${user.id}", 0L)
        return if (lockoutUntil > System.currentTimeMillis()) {
            true
        } else {
            // Clear expired lockout
            preferences.edit().remove("lockout_until_${user.id}").apply()
            false
        }
    }

    /**
     * Increment failed login attempts
     */
    private fun incrementFailedAttempts(userId: String) {
        val attempts = preferences.getInt("failed_attempts_$userId", 0) + 1
        preferences.edit().putInt("failed_attempts_$userId", attempts).apply()

        if (attempts >= SecurityConfig.MAX_LOGIN_ATTEMPTS) {
            val lockoutUntil =
                System.currentTimeMillis() + (SecurityConfig.ACCOUNT_LOCKOUT_MINUTES * 60 * 1000)
            preferences.edit().putLong("lockout_until_$userId", lockoutUntil).apply()

            SecurityLogger.logSecurityIncident(
                "account_locked",
                "Account locked due to excessive failed attempts: $userId",
                context,
                SecurityConfig.RiskLevel.HIGH
            )
        }
    }

    /**
     * Reset failed login attempts
     */
    private fun resetFailedAttempts(userId: String) {
        preferences.edit().remove("failed_attempts_$userId").apply()
    }

    /**
     * Update last login timestamp
     */
    private fun updateLastLogin(userId: String) {
        preferences.edit().putLong("last_login_$userId", System.currentTimeMillis()).apply()
    }

    /**
     * Generate MFA setup secret
     */
    private fun generateMfaSetupSecret(userId: String): String {
        // Simplified TOTP secret generation
        val secret = ByteArray(20)
        SecureRandom().nextBytes(secret)
        return Base64.encodeToString(secret, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    /**
     * Verify MFA token (simplified TOTP implementation)
     */
    private fun verifyMfaToken(userId: String, token: String): Boolean {
        // In real implementation, use proper TOTP library
        // This is a simplified mock
        return token.length == 6 && token.all { it.isDigit() }
    }

    /**
     * Generate refresh token
     */
    private fun generateRefreshToken(userId: String): String {
        val random = ByteArray(32)
        SecureRandom().nextBytes(random)
        return Base64.encodeToString(random, Base64.URL_SAFE or Base64.NO_WRAP)
    }
}