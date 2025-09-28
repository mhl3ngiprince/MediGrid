package com.example.medigrid.security

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * AI-Powered Email Assistant for MediGrid Healthcare App
 * Simulates email sending with prominent logging for development
 */
class AIEmailAssistant(private val context: Context) {

    companion object {
        private const val TAG = "AIEmailAssistant"
    }

    /**
     * Send verification email using AI assistant (simulated with prominent logging)
     */
    suspend fun sendVerificationEmail(
        email: String,
        userName: String,
        verificationCode: String,
        role: SecurityConfig.HealthcareRole
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val subject = "MediGrid Healthcare - Email Verification Required"
                val body = createVerificationEmailBody(userName, verificationCode, role)

                // Always log the verification code prominently for development
                Log.i(
                    TAG, """
                ╔════════════════════════════════════════════════════════════════╗
                ║                    📧 VERIFICATION EMAIL SENT 📧                ║
                ║                                                                ║
                ║  TO: $email                                     
                ║  USER: $userName                                              
                ║  ROLE: ${role.name}                                              
                ║                                                                ║
                ║  🔐 VERIFICATION CODE: $verificationCode                     
                ║                                                                ║
                ║  ⏰ Code expires in 15 minutes                                 ║
                ║  💡 Use "Can't access email?" button to see this code         ║
                ╚════════════════════════════════════════════════════════════════╝
                """.trimIndent()
                )

                val success = sendEmailViaLogging(email, subject, body)

                if (success) {
                    SecurityLogger.logSecurityEvent(
                        "ai_email_verification_sent",
                        mapOf(
                            "email" to hashEmail(email),
                            "role" to role.name,
                            "method" to "ai_email_assistant"
                        ),
                        context
                    )
                    Log.d(TAG, "Verification email sent successfully via AI assistant")
                } else {
                    // Fallback: Log the verification code for development
                    Log.w(
                        TAG,
                        "Email sending failed, verification code for $email: $verificationCode"
                    )
                }

                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send verification email via AI assistant", e)
                
                // Fallback: Log the verification code for development
                Log.i(TAG, "⚠️ EMAIL FAILED - VERIFICATION CODE for $email: $verificationCode")
                
                SecurityLogger.logSecurityIncident(
                    "ai_email_verification_failed",
                    "Failed to send verification email: ${e.message}",
                    context,
                    SecurityConfig.RiskLevel.MEDIUM
                )
                
                false
            }
        }
    }

    /**
     * Send password reset email using AI assistant (simulated)
     */
    suspend fun sendPasswordResetEmail(
        email: String,
        userName: String,
        resetCode: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val subject = "MediGrid Healthcare - Password Reset Request"
                val body = createPasswordResetEmailBody(userName, resetCode)

                // Log the reset code prominently for development
                Log.i(
                    TAG, """
                ╔════════════════════════════════════════════════════════════════╗
                ║                  🔑 PASSWORD RESET EMAIL SENT 🔑               ║
                ║                                                                ║
                ║  TO: $email                                     
                ║  USER: $userName                                              
                ║                                                                ║
                ║  🔐 RESET CODE: $resetCode                                   
                ║                                                                ║
                ║  ⏰ Code expires in 15 minutes                                 ║
                ║  💡 Use "Can't access email?" section to see this code        ║
                ╚════════════════════════════════════════════════════════════════╝
                """.trimIndent()
                )

                val success = sendEmailViaLogging(email, subject, body)
                
                if (success) {
                    SecurityLogger.logSecurityEvent(
                        "ai_password_reset_sent",
                        mapOf("email" to hashEmail(email)),
                        context
                    )
                    Log.d(TAG, "Password reset email sent successfully via AI assistant")
                } else {
                    // Fallback: Log the reset code for development
                    Log.w(TAG, "Email sending failed, reset code for $email: $resetCode")
                }

                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send password reset email via AI assistant", e)
                
                // Fallback: Log the reset code for development
                Log.i(TAG, "⚠️ EMAIL FAILED - PASSWORD RESET CODE for $email: $resetCode")
                
                SecurityLogger.logSecurityIncident(
                    "ai_password_reset_failed",
                    "Failed to send password reset email: ${e.message}",
                    context,
                    SecurityConfig.RiskLevel.MEDIUM
                )
                
                false
            }
        }
    }

    /**
     * Send email via logging
     */
    private suspend fun sendEmailViaLogging(
        recipientEmail: String,
        subject: String,
        body: String,
    ): Boolean {
        return try {
            Log.i(
                TAG, """
                ═══════════════════════════════════════════════════════════════
                📧 MEDIGRID EMAIL SERVICE - EMAIL SENT SUCCESSFULLY 📧
                ═══════════════════════════════════════════════════════════════
                TO: $recipientEmail
                SUBJECT: $subject
                
                MESSAGE CONTENT:
                ────────────────────────────────────────────────────────────────
                $body
                ────────────────────────────────────────────────────────────────
                
                📱 NOTE: Check the verification code shown above in this log
                📱 You can copy the code from the app's "Can't access email?" section
                ═══════════════════════════════════════════════════════════════
            """.trimIndent()
            )

            // Simulate network delay
            delay(1500)

            // For now, return true to simulate successful email sending
            true
        } catch (e: Exception) {
            Log.e(TAG, "Email logging failed", e)
            false
        }
    }

    /**
     * Create verification email body content
     */
    private fun createVerificationEmailBody(
        userName: String,
        verificationCode: String,
        role: SecurityConfig.HealthcareRole
    ): String {
        return """
            Dear $userName,
            
            Welcome to MediGrid Healthcare Management System!
            
            Your registration as a ${role.name} has been received. To complete your account setup, please verify your email address using the code below:
            
            🔐 VERIFICATION CODE: $verificationCode
            
            This code will expire in 15 minutes.
            
            Please enter this code in the MediGrid app to activate your account.
            
            If you did not request this registration, please ignore this email.
            
            Security Information:
            • This account has ${role.name} level access
            • All activities are logged for POPIA compliance
            • Your data is protected with AES-256 encryption
            
            For support, contact your system administrator.
            
            Best regards,
            MediGrid AI Assistant
            
            ---
            This email was sent automatically by MediGrid AI Email Assistant.
            Please do not reply to this email.
        """.trimIndent()
    }

    /**
     * Create password reset email body content
     */
    private fun createPasswordResetEmailBody(
        userName: String,
        resetCode: String
    ): String {
        return """
            Dear $userName,
            
            You have requested a password reset for your MediGrid Healthcare account.
            
            🔑 PASSWORD RESET CODE: $resetCode
            
            This code will expire in 15 minutes.
            
            To reset your password:
            1. Return to the MediGrid app
            2. Enter this reset code when prompted
            3. Create a new secure password
            
            If you did not request this password reset, please ignore this email and contact your system administrator immediately.
            
            Security Information:
            • This reset code can only be used once
            • All password changes are logged for security compliance
            • Your account access is protected by POPIA regulations
            
            For support, contact your system administrator.
            
            Best regards,
            MediGrid AI Assistant
            
            ---
            This email was sent automatically by MediGrid AI Email Assistant.
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
     * Send notification email for clinic alerts
     */
    suspend fun sendClinicAlert(
        email: String,
        userName: String,
        clinicName: String,
        alertMessage: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val subject = "🚨 MediGrid Alert: $clinicName"
                val body = """
                    Dear $userName,
                    
                    This is an automated alert from MediGrid Healthcare Management System.
                    
                    Clinic: $clinicName
                    Alert: $alertMessage
                    Time: ${java.util.Date()}
                    
                    Please take appropriate action as needed.
                    
                    Best regards,
                    MediGrid AI Assistant
                """.trimIndent()

                sendEmailViaLogging(email, subject, body)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send clinic alert email", e)
                false
            }
        }
    }

    /**
     * Test email connectivity
     */
    suspend fun testEmailService(): Boolean {
        return try {
            // Test with a simple ping or test email
            Log.d(TAG, "Testing AI Email Assistant connectivity...")
            
            // Simulate test
            delay(500)
            
            Log.d(TAG, "AI Email Assistant is ready!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Email service test failed", e)
            false
        }
    }
}