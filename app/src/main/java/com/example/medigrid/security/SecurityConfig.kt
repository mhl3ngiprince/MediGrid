package com.example.medigrid.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import java.security.SecureRandom

/**
 * MediGrid Security Configuration
 * Implements POPIA-compliant encryption and security controls for healthcare data
 */
object SecurityConfig {

    // Security Constants
    const val KEYSTORE_ALIAS = "MediGridSecurityKey"
    const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    const val GCM_IV_LENGTH = 12
    const val GCM_TAG_LENGTH = 16

    // Session Management
    const val SESSION_TIMEOUT_MINUTES = 15
    const val MAX_LOGIN_ATTEMPTS = 3
    const val ACCOUNT_LOCKOUT_MINUTES = 30

    // Security Headers
    val SECURITY_HEADERS = mapOf(
        "X-Content-Type-Options" to "nosniff",
        "X-Frame-Options" to "DENY",
        "X-XSS-Protection" to "1; mode=block",
        "Strict-Transport-Security" to "max-age=31536000; includeSubDomains"
    )

    // Risk Assessment Levels
    enum class RiskLevel(val score: Int) {
        CRITICAL(10),
        HIGH(8),
        MEDIUM(5),
        LOW(2)
    }

    // Healthcare Roles with PHI Access Levels
    enum class HealthcareRole(val permissions: Set<String>) {
        DOCTOR(setOf("READ_PHI", "WRITE_PHI", "EMERGENCY_ACCESS", "PRESCRIBE")),
        NURSE(setOf("READ_PHI", "WRITE_BASIC", "EMERGENCY_ACCESS")),
        PHARMACIST(setOf("READ_PRESCRIPTION", "MANAGE_INVENTORY")),
        ADMIN(setOf("READ_PHI", "SYSTEM_CONFIG", "USER_MANAGEMENT")),
        RECEPTIONIST(setOf("READ_BASIC", "SCHEDULE_APPOINTMENTS"))
    }

    /**
     * Initialize Android Keystore for PHI encryption
     */
    fun initializeKeystore(context: Context): Boolean {
        return try {
            val keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                // Remove user authentication requirement to prevent crashes
                // .setUserAuthenticationRequired(true)
                // .setUserAuthenticationValidityDurationSeconds(SESSION_TIMEOUT_MINUTES * 60)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()

            SecurityLogger.logSecurityEvent(
                "keystore_initialized",
                mapOf("status" to "success"),
                context
            )
            true
        } catch (e: Exception) {
            SecurityLogger.logSecurityIncident(
                "keystore_initialization_failed",
                e.message ?: "Unknown error",
                context
            )
            false
        }
    }

    /**
     * Get security key from Android Keystore
     */
    private fun getSecurityKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
        } catch (e: Exception) {
            null
        }
    }
}