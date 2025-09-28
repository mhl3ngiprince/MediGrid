package com.example.medigrid.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.*
import java.util.*

/**
 * Email Verification Service for MediGrid Healthcare App
 * Provides real email verification functionality
 */
class EmailVerificationService(private val context: Context) {

    companion object {
        private const val TAG = "EmailVerificationService"
        private const val VERIFICATION_CODE_LENGTH = 6
        private const val CODE_VALIDITY_MINUTES = 15
        private const val VERIFICATION_PREFS = "medigrid_verification"
    }

    // AI Email Assistant for direct email sending
    private val aiEmailAssistant = AIEmailAssistant(context)

    data class VerificationCode(
        val code: String,
        val email: String,
        val expiryTime: Long,
        val isUsed: Boolean = false,
    )

    /**
     * Send verification email using AI Email Assistant
     */
    suspend fun sendVerificationEmail(
        email: String,
        userName: String,
        role: SecurityConfig.HealthcareRole,
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                // Generate verification code
                val verificationCode = generateVerificationCode()
                val expiryTime = System.currentTimeMillis() + (CODE_VALIDITY_MINUTES * 60 * 1000)

                // Store verification code in SharedPreferences for consistency
                storeVerificationCode(email, verificationCode, expiryTime)

                // Send email using AI Assistant
                val emailSent = aiEmailAssistant.sendVerificationEmail(
                    email = email,
                    userName = userName,
                    verificationCode = verificationCode,
                    role = role
                )

                if (!emailSent) {
                    // Fallback to device email app if AI assistant fails
                    Log.w(TAG, "AI Email Assistant failed, falling back to device email app")
                    sendEmailViaDeviceApp(email, userName, verificationCode, role)
                }

                // Log the verification attempt
                SecurityLogger.logSecurityEvent(
                    "email_verification_sent",
                    mapOf(
                        "email" to hashEmail(email),
                        "role" to role.name,
                        "method" to if (emailSent) "ai_assistant" else "device_fallback"
                    ),
                    context
                )

                Log.d(TAG, "Verification email processed for: ${email.take(5)}***")
                verificationCode

            } catch (e: Exception) {
                SecurityLogger.logSecurityIncident(
                    "email_verification_failed",
                    "Failed to send verification email: ${e.message}",
                    context,
                    SecurityConfig.RiskLevel.MEDIUM
                )

                Log.e(TAG, "Failed to send verification email", e)
                throw e
            }
        }
    }

    /**
     * Store verification code in SharedPreferences
     */
    private fun storeVerificationCode(email: String, code: String, expiryTime: Long) {
        val sharedPrefs = context.getSharedPreferences(VERIFICATION_PREFS, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putString("code_$email", "$code:$expiryTime:false") // code:expiry:isUsed
        editor.apply()
        Log.d(TAG, "Stored verification code for $email: $code (expires at $expiryTime)")
    }

    /**
     * Get stored verification code
     */
    private fun getStoredVerificationCode(email: String): VerificationCode? {
        val sharedPrefs = context.getSharedPreferences(VERIFICATION_PREFS, Context.MODE_PRIVATE)
        val storedData = sharedPrefs.getString("code_$email", null) ?: return null

        return try {
            val parts = storedData.split(":")
            if (parts.size >= 3) {
                VerificationCode(
                    code = parts[0],
                    email = email,
                    expiryTime = parts[1].toLong(),
                    isUsed = parts[2].toBoolean()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing stored verification code", e)
            null
        }
    }

    /**
     * Mark verification code as used
     */
    private fun markCodeAsUsed(email: String) {
        val sharedPrefs = context.getSharedPreferences(VERIFICATION_PREFS, Context.MODE_PRIVATE)
        val storedData = sharedPrefs.getString("code_$email", null)
        if (storedData != null) {
            val parts = storedData.split(":")
            if (parts.size >= 3) {
                val editor = sharedPrefs.edit()
                editor.putString("code_$email", "${parts[0]}:${parts[1]}:true")
                editor.apply()
            }
        }
    }

    /**
     * Fallback: Send email via device app when AI assistant fails
     */
    private fun sendEmailViaDeviceApp(
        email: String,
        userName: String,
        verificationCode: String,
        role: SecurityConfig.HealthcareRole,
    ) {
        try {
            val subject = "MediGrid Healthcare - Email Verification Required"
            val emailBody = createVerificationEmailBody(userName, verificationCode, role)
            sendEmailViaIntent(email, subject, emailBody)
        } catch (e: Exception) {
            Log.e(TAG, "Device email app fallback also failed", e)
            // Final fallback: Log the code
            showVerificationCodeDirectly(email)
        }
    }

    /**
     * Send email using Android's email intent - now public for password reset functionality
     */
    fun sendEmailViaIntent(email: String, subject: String, body: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to open email app", e)
            // Fallback: Try to send via Gmail specifically
            sendViaGmail(email, subject, body)
        }
    }

    /**
     * Fallback: Try to send via Gmail app
     */
    private fun sendViaGmail(email: String, subject: String, body: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                setPackage("com.google.android.gm")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Gmail app not available", e)
            // Final fallback: Show verification code directly
            showVerificationCodeDirectly(email)
        }
    }

    /**
     * Fallback: Show verification code in a notification or log
     */
    private fun showVerificationCodeDirectly(email: String) {
        val code = getStoredVerificationCode(email)?.code
        if (code != null) {
            // For development: Log the code so user can see it
            Log.i(TAG, "VERIFICATION CODE for $email: $code")

            // You could also show a notification here
            SecurityLogger.logSecurityEvent(
                "verification_code_shown_directly",
                mapOf(
                    "email" to hashEmail(email),
                    "reason" to "email_delivery_failed"
                ),
                context
            )
        }
    }

    /**
     * Verify the entered code
     */
    fun verifyCode(email: String, enteredCode: String): Boolean {
        return try {
            val storedCode = getStoredVerificationCode(email)

            when {
                storedCode == null -> {
                    Log.w(TAG, "No verification code found for email: $email")
                    false
                }

                storedCode.isUsed -> {
                    Log.w(TAG, "Verification code already used for email: $email")
                    false
                }

                System.currentTimeMillis() > storedCode.expiryTime -> {
                    Log.w(TAG, "Verification code expired for email: $email")
                    // Remove expired code
                    val sharedPrefs =
                        context.getSharedPreferences(VERIFICATION_PREFS, Context.MODE_PRIVATE)
                    val editor = sharedPrefs.edit()
                    editor.remove("code_$email")
                    editor.apply()
                    false
                }

                storedCode.code.equals(enteredCode, ignoreCase = false) -> {
                    // Mark code as used
                    markCodeAsUsed(email)

                    SecurityLogger.logSecurityEvent(
                        "email_verification_successful",
                        mapOf("email" to hashEmail(email)),
                        context
                    )

                    Log.i(TAG, "Email verification successful for: $email")
                    true
                }

                else -> {
                    SecurityLogger.logSecurityEvent(
                        "email_verification_failed",
                        mapOf(
                            "email" to hashEmail(email),
                            "reason" to "invalid_code",
                            "expected" to storedCode.code,
                            "received" to enteredCode
                        ),
                        context
                    )

                    Log.w(
                        TAG,
                        "Invalid verification code entered for: $email (expected: ${storedCode.code}, received: $enteredCode)"
                    )
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying code for email: $email", e)
            false
        }
    }

    /**
     * Resend verification email
     */
    suspend fun resendVerificationEmail(
        email: String,
        userName: String,
        role: SecurityConfig.HealthcareRole,
    ): String {
        // Remove old code
        val sharedPrefs = context.getSharedPreferences(VERIFICATION_PREFS, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.remove("code_$email")
        editor.apply()

        // Send new verification email
        return sendVerificationEmail(email, userName, role)
    }

    /**
     * Generate and store verification code (for fallback display)
     */
    fun generateAndStoreCode(email: String): String {
        val verificationCode = generateVerificationCode()
        val expiryTime = System.currentTimeMillis() + (CODE_VALIDITY_MINUTES * 60 * 1000)

        // Store verification code
        storeVerificationCode(email, verificationCode, expiryTime)

        SecurityLogger.logSecurityEvent(
            "verification_code_generated_fallback",
            mapOf("email" to hashEmail(email)),
            context
        )

        Log.d(TAG, "Generated fallback verification code for $email: $verificationCode")
        return verificationCode
    }

    /**
     * Generate a random verification code
     */
    private fun generateVerificationCode(): String {
        return (100000..999999).random().toString()
    }

    /**
     * Create email body content
     */
    private fun createVerificationEmailBody(
        userName: String,
        verificationCode: String,
        role: SecurityConfig.HealthcareRole,
    ): String {
        return """
            Dear $userName,
            
            Welcome to MediGrid Healthcare Management System!
            
            Your registration as a ${role.name} has been received. To complete your account setup, please verify your email address using the code below:
            
            VERIFICATION CODE: $verificationCode
            
            This code will expire in $CODE_VALIDITY_MINUTES minutes.
            
            Please enter this code in the MediGrid app to activate your account.
            
            If you did not request this registration, please ignore this email.
            
            Security Information:
            • This account has ${role.name} level access
            • All activities are logged for POPIA compliance
            • Your data is protected with AES-256 encryption
            
            For support, contact your system administrator.
            
            Best regards,
            MediGrid Security Team
            
            ---
            This is an automated message from MediGrid Healthcare Management System.
            Please do not reply to this email.
        """.trimIndent()
    }

    /**
     * Hash email for logging (privacy protection)
     */
    private fun hashEmail(email: String): String {
        return try {
            email.hashCode().toString(16).takeLast(8)
        } catch (e: Exception) {
            "email_hash_error"
        }
    }

    /**
     * Clean up expired verification codes
     */
    fun cleanupExpiredCodes() {
        val currentTime = System.currentTimeMillis()
        val sharedPrefs = context.getSharedPreferences(VERIFICATION_PREFS, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        for (entry in sharedPrefs.all) {
            val parts = entry.value.toString().split(":")
            if (parts.size >= 3) {
                val expiryTime = parts[1].toLong()
                if (currentTime > expiryTime) {
                    editor.remove(entry.key)
                }
            }
        }
        editor.apply()
    }

    /**
     * Check if email has pending verification
     */
    fun hasPendingVerification(email: String): Boolean {
        val storedCode = getStoredVerificationCode(email)
        return storedCode != null && !storedCode.isUsed && System.currentTimeMillis() <= storedCode.expiryTime
    }

    /**
     * Get remaining time for verification code
     */
    fun getRemainingTimeMinutes(email: String): Int {
        val code = getStoredVerificationCode(email) ?: return 0
        val remainingMs = code.expiryTime - System.currentTimeMillis()
        return if (remainingMs > 0) (remainingMs / (60 * 1000)).toInt() else 0
    }
}