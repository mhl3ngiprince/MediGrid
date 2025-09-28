package com.example.medigrid.security

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.*

/**
 * Firebase Authentication Service for Healthcare Workers
 * Integrates Firebase Auth with MediGrid security framework
 *
 * Note: This is a template implementation. To use Firebase:
 * 1. Add proper google-services.json file from Firebase Console
 * 2. Ensure Firebase dependencies are properly added
 * 3. Configure Firebase project with healthcare security rules
 */
class FirebaseAuthService(private val context: Context) {

    private val sessionManager = HealthcareSessionManager(context)
    private val emailVerificationService = EmailVerificationService(context)
    private val aiEmailAssistant = AIEmailAssistant(context)

    private val _currentUser = MutableStateFlow<HealthcareUser?>(null)
    val currentUser: Flow<HealthcareUser?> = _currentUser.asStateFlow()

    /**
     * Healthcare user data stored in Firebase
     */
    data class HealthcareUser(
        val id: String = "",
        val email: String = "",
        val username: String = "",
        val role: SecurityConfig.HealthcareRole = SecurityConfig.HealthcareRole.NURSE,
        val clinicId: String = "",
        val phiAccessLevel: String = "",
        val mfaEnabled: Boolean = true,
        val isActive: Boolean = true,
        val licenseNumber: String? = null,
        val department: String? = null,
        val lastLogin: Long? = null,
        val createdAt: Long = System.currentTimeMillis(),
        val isEmailVerified: Boolean = false,
        val passwordHash: String = "",
    ) {
        val permissions: Set<String>
            get() = role.permissions
    }

    /**
     * Authentication result with Firebase integration
     */
    data class AuthResult(
        val success: Boolean,
        val user: HealthcareUser? = null,
        val error: String? = null,
        val requiresVerification: Boolean = false,
        val requiresRegistration: Boolean = false,
    )

    /**
     * Sign in healthcare worker with email and password
     * Real authentication implementation with persistent storage
     */
    suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): AuthResult {
        return try {
            // Input validation for healthcare emails
            if (!isValidHealthcareEmail(email)) {
                SecurityLogger.logSecurityIncident(
                    "invalid_healthcare_email",
                    "Invalid healthcare email format: $email",
                    context,
                    SecurityConfig.RiskLevel.MEDIUM
                )
                return AuthResult(
                    success = false,
                    error = "Please use your official healthcare organization email"
                )
            }

            // Get stored user data
            val sharedPrefs = context.getSharedPreferences("medigrid_users", Context.MODE_PRIVATE)
            val userJson = sharedPrefs.getString("user_$email", null)

            if (userJson == null) {
                SecurityLogger.logAuthenticationEvent(
                    email,
                    "login_user_not_found",
                    false,
                    "User not registered",
                    context
                )
                return AuthResult(
                    success = false,
                    error = "User not registered. Please register first."
                )
            }

            // Parse stored user data
            val storedUser = parseStoredUser(userJson)
            if (storedUser == null) {
                return AuthResult(
                    success = false,
                    error = "Invalid user data. Please contact support."
                )
            }

            // Verify password
            if (!verifyPassword(password, storedUser.passwordHash)) {
                SecurityLogger.logAuthenticationEvent(
                    email,
                    "login_invalid_password",
                    false,
                    "Invalid password attempt",
                    context
                )
                return AuthResult(
                    success = false,
                    error = "Invalid email or password"
                )
            }

            // Check if email is verified
            if (!storedUser.isEmailVerified) {
                return AuthResult(
                    success = false,
                    requiresVerification = true,
                    error = "Please verify your email address before signing in"
                )
            }

            // Update last login and create session
            val updatedUser = storedUser.copy(lastLogin = System.currentTimeMillis())
            storeUser(email, updatedUser)

            val accessToken = sessionManager.createSession(convertToAuthUser(updatedUser))
            _currentUser.value = updatedUser

            // Log successful authentication
            SecurityLogger.logAuthenticationEvent(
                email,
                "login_success",
                true,
                "Successful authentication",
                context
            )

            SecurityLogger.logSecurityEvent(
                "healthcare_session_started",
                mapOf(
                    "user_id" to updatedUser.id,
                    "role" to updatedUser.role.name,
                    "clinic_id" to updatedUser.clinicId
                ),
                context
            )

            AuthResult(
                success = true,
                user = updatedUser
            )

        } catch (e: Exception) {
            SecurityLogger.logSecurityIncident(
                "authentication_error",
                "Authentication error: ${e.message}",
                context,
                SecurityConfig.RiskLevel.HIGH
            )

            AuthResult(
                success = false,
                error = "Authentication failed: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Register new healthcare worker
     * Real implementation with persistent storage
     */
    suspend fun registerHealthcareWorker(
        email: String,
        password: String,
        username: String,
        role: SecurityConfig.HealthcareRole,
        clinicId: String,
        licenseNumber: String? = null,
        department: String? = null,
    ): AuthResult {
        return try {
            // Validate healthcare email domain
            if (!isValidHealthcareEmail(email)) {
                return AuthResult(
                    success = false,
                    error = "Please use your official healthcare organization email"
                )
            }

            // Check if user already exists
            val sharedPrefs = context.getSharedPreferences("medigrid_users", Context.MODE_PRIVATE)
            val existingUser = sharedPrefs.getString("user_$email", null)

            if (existingUser != null) {
                return AuthResult(
                    success = false,
                    error = "User already exists with this email"
                )
            }

            // Create healthcare user profile
            val healthcareUser = HealthcareUser(
                id = generateUserId(),
                email = email,
                username = username,
                role = role,
                clinicId = clinicId,
                phiAccessLevel = getPhiAccessLevel(role),
                mfaEnabled = true,
                licenseNumber = licenseNumber,
                department = department,
                isEmailVerified = false, // Requires email verification
                passwordHash = hashPassword(password)
            )

            // Store user data
            storeUser(email, healthcareUser)

            // Send email verification
            emailVerificationService.sendVerificationEmail(email, username, role)

            // Log registration
            SecurityLogger.logSecurityEvent(
                "healthcare_user_registered",
                mapOf(
                    "user_id" to healthcareUser.id,
                    "role" to role.name,
                    "clinic_id" to clinicId
                ),
                context
            )

            AuthResult(
                success = true,
                user = healthcareUser,
                requiresVerification = true
            )

        } catch (e: Exception) {
            SecurityLogger.logSecurityIncident(
                "registration_error",
                "Registration error: ${e.message}",
                context,
                SecurityConfig.RiskLevel.MEDIUM
            )

            AuthResult(
                success = false,
                error = "Registration failed: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Send password reset email
     * Real implementation using EmailVerificationService
     */
    suspend fun sendPasswordResetEmail(email: String): Boolean {
        return try {
            // Check if user exists
            val sharedPrefs = context.getSharedPreferences("medigrid_users", Context.MODE_PRIVATE)
            val userJson = sharedPrefs.getString("user_$email", null)
            
            if (userJson == null) {
                SecurityLogger.logSecurityEvent(
                    "password_reset_user_not_found",
                    mapOf("email" to hashEmail(email)),
                    context
                )
                return false
            }

            // Parse user data
            val user = parseStoredUser(userJson)
            if (user == null) {
                return false
            }

            // Generate reset code and store it temporarily
            val resetCode = generateResetCode()
            val resetPrefs = context.getSharedPreferences("medigrid_password_reset", Context.MODE_PRIVATE)
            val resetEditor = resetPrefs.edit()
            resetEditor.putString("reset_$email", "$resetCode:${System.currentTimeMillis() + (15 * 60 * 1000)}") // 15 min expiry
            resetEditor.apply()

            // Send password reset email using AI Email Assistant
            aiEmailAssistant.sendPasswordResetEmail(email, user.username, resetCode)

            SecurityLogger.logSecurityEvent(
                "password_reset_email_sent",
                mapOf("email" to hashEmail(email)),
                context
            )

            true
        } catch (e: Exception) {
            SecurityLogger.logSecurityIncident(
                "password_reset_error",
                "Password reset error: ${e.message}",
                context
            )
            false
        }
    }

    /**
     * Generate password reset code
     */
    private fun generateResetCode(): String {
        return (100000..999999).random().toString()
    }

    /**
     * Verify password reset code and update password
     */
    suspend fun resetPasswordWithCode(email: String, resetCode: String, newPassword: String): Boolean {
        return try {
            // Verify reset code
            val resetPrefs = context.getSharedPreferences("medigrid_password_reset", Context.MODE_PRIVATE)
            val storedResetData = resetPrefs.getString("reset_$email", null)
            
            if (storedResetData == null) {
                return false
            }

            val parts = storedResetData.split(":")
            val storedCode = parts[0]
            val expiryTime = parts[1].toLong()

            if (System.currentTimeMillis() > expiryTime) {
                // Code expired, remove it
                val resetEditor = resetPrefs.edit()
                resetEditor.remove("reset_$email")
                resetEditor.apply()
                return false
            }

            if (storedCode != resetCode) {
                return false
            }

            // Get user data and update password
            val sharedPrefs = context.getSharedPreferences("medigrid_users", Context.MODE_PRIVATE)
            val userJson = sharedPrefs.getString("user_$email", null)
            val user = parseStoredUser(userJson)
            
            if (user != null) {
                val updatedUser = user.copy(passwordHash = hashPassword(newPassword))
                storeUser(email, updatedUser)

                // Remove used reset code
                val resetEditor = resetPrefs.edit()
                resetEditor.remove("reset_$email")
                resetEditor.apply()

                SecurityLogger.logSecurityEvent(
                    "password_reset_completed",
                    mapOf("email" to hashEmail(email)),
                    context
                )

                return true
            }

            false
        } catch (e: Exception) {
            SecurityLogger.logSecurityIncident(
                "password_reset_verification_error",
                "Password reset verification error: ${e.message}",
                context
            )
            false
        }
    }

    /**
     * Sign out current user
     */
    suspend fun signOut() {
        try {
            val currentUserId = _currentUser.value?.id

            // Invalidate session
            currentUserId?.let { userId ->
                SecurityLogger.logSecurityEvent(
                    "healthcare_session_ended",
                    mapOf("user_id" to userId),
                    context
                )
            }

            // TODO: Sign out from Firebase
            // FirebaseAuth.getInstance().signOut()
            _currentUser.value = null

        } catch (e: Exception) {
            SecurityLogger.logSecurityIncident(
                "signout_error",
                "Error during sign out: ${e.message}",
                context
            )
        }
    }

    /**
     * Check if user has specific permission
     */
    fun hasPermission(permission: String): Boolean {
        return _currentUser.value?.permissions?.contains(permission) ?: false
    }

    /**
     * Get current user as HealthcareAuthService.HealthcareUser
     */
    fun getCurrentAuthUser(): HealthcareAuthService.HealthcareUser? {
        return _currentUser.value?.let { convertToAuthUser(it) }
    }

    /**
     * Validate healthcare email domain
     */
    private fun isValidHealthcareEmail(email: String): Boolean {
        val healthcareDomains = listOf(
            "health.gov.za",
            "wits.ac.za",
            "uct.ac.za",
            "netcare.co.za",
            "mediclinic.co.za",
            "discovery.co.za",
            "gmail.com", // Temporary for development
            "outlook.com", // Temporary for development
            "yahoo.com", // Temporary for development
            "hotmail.com" // Temporary for development
        )

        return email.contains("@") &&
                healthcareDomains.any { domain ->
                    email.lowercase().endsWith("@$domain")
                }
    }

    /**
     * Get PHI access level based on role
     */
    private fun getPhiAccessLevel(role: SecurityConfig.HealthcareRole): String {
        return when (role) {
            SecurityConfig.HealthcareRole.DOCTOR -> "FULL"
            SecurityConfig.HealthcareRole.NURSE -> "BASIC"
            SecurityConfig.HealthcareRole.PHARMACIST -> "PRESCRIPTION_ONLY"
            SecurityConfig.HealthcareRole.ADMIN -> "ADMINISTRATIVE"
            SecurityConfig.HealthcareRole.RECEPTIONIST -> "LIMITED"
        }
    }

    /**
     * Convert Firebase HealthcareUser to HealthcareAuthService.HealthcareUser
     */
    private fun convertToAuthUser(firebaseUser: HealthcareUser): HealthcareAuthService.HealthcareUser {
        return HealthcareAuthService.HealthcareUser(
            id = firebaseUser.id,
            username = firebaseUser.username,
            role = firebaseUser.role,
            clinicId = firebaseUser.clinicId,
            phiAccessLevel = firebaseUser.phiAccessLevel,
            mfaEnabled = firebaseUser.mfaEnabled
        )
    }

    /**
     * Hash email for logging (privacy protection)
     */
    private fun hashEmail(email: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(email.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }.substring(0, 16)
        } catch (e: Exception) {
            "email_hash_error"
        }
    }

    /**
     * Generate unique user ID
     */
    private fun generateUserId(): String {
        return "user_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    /**
     * Debug: Clear all stored users (for testing)
     */
    fun clearAllUsers() {
        try {
            val sharedPrefs = context.getSharedPreferences("medigrid_users", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()
            editor.clear()
            editor.apply()
            
            android.util.Log.d("FirebaseAuthService", "All users cleared from storage")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuthService", "Error clearing users", e)
        }
    }
    
    /**
     * Debug: Get all stored users
     */
    fun getAllStoredUsers(): Map<String, String> {
        return try {
            val sharedPrefs = context.getSharedPreferences("medigrid_users", Context.MODE_PRIVATE)
            sharedPrefs.all.mapNotNull { (key, value) -> 
                if (key.startsWith("user_") && value is String) {
                    key to value
                } else null
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Store user data using JSON-like format for better parsing
     */
    private fun storeUser(email: String, user: HealthcareUser) {
        val sharedPrefs = context.getSharedPreferences("medigrid_users", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        
        // Store as a more reliable format
        val userString = buildString {
            append("id=${user.id}")
            append(",email=${user.email}")
            append(",username=${user.username}")
            append(",role=${user.role.name}")
            append(",clinicId=${user.clinicId}")
            append(",phiAccessLevel=${user.phiAccessLevel}")
            append(",mfaEnabled=${user.mfaEnabled}")
            append(",isActive=${user.isActive}")
            append(",licenseNumber=${user.licenseNumber ?: "null"}")
            append(",department=${user.department ?: "null"}")
            append(",lastLogin=${user.lastLogin ?: "null"}")
            append(",createdAt=${user.createdAt}")
            append(",isEmailVerified=${user.isEmailVerified}")
            append(",passwordHash=${user.passwordHash}")
        }
        
        editor.putString("user_$email", userString)
        editor.apply()
        
        android.util.Log.d("FirebaseAuthService", "Stored user: $email")
    }

    /**
     * Parse stored user data with better error handling
     */
    private fun parseStoredUser(userJson: String?): HealthcareUser? {
        if (userJson == null) {
            android.util.Log.w("FirebaseAuthService", "No user data found")
            return null
        }
        
        return try {
            android.util.Log.d("FirebaseAuthService", "Parsing user data: $userJson")
            
            val parts = userJson.split(",")
            if (parts.size < 14) {
                android.util.Log.e("FirebaseAuthService", "Invalid user data format - insufficient parts: ${parts.size}")
                return null
            }
            
            val userData = parts.associate { part ->
                val keyValue = part.split("=", limit = 2)
                if (keyValue.size == 2) {
                    keyValue[0].trim() to keyValue[1].trim()
                } else {
                    android.util.Log.w("FirebaseAuthService", "Invalid part format: $part")
                    "" to ""
                }
            }
            
            val user = HealthcareUser(
                id = userData["id"] ?: "",
                email = userData["email"] ?: "",
                username = userData["username"] ?: "",
                role = try { 
                    SecurityConfig.HealthcareRole.valueOf(userData["role"] ?: "NURSE")
                } catch (e: Exception) {
                    android.util.Log.w("FirebaseAuthService", "Invalid role, defaulting to NURSE")
                    SecurityConfig.HealthcareRole.NURSE
                },
                clinicId = userData["clinicId"] ?: "",
                phiAccessLevel = userData["phiAccessLevel"] ?: "BASIC",
                mfaEnabled = userData["mfaEnabled"]?.toBoolean() ?: true,
                isActive = userData["isActive"]?.toBoolean() ?: true,
                licenseNumber = if (userData["licenseNumber"] == "null") null else userData["licenseNumber"],
                department = if (userData["department"] == "null") null else userData["department"],
                lastLogin = if (userData["lastLogin"] == "null") null else userData["lastLogin"]?.toLongOrNull(),
                createdAt = userData["createdAt"]?.toLongOrNull() ?: System.currentTimeMillis(),
                isEmailVerified = userData["isEmailVerified"]?.toBoolean() ?: false,
                passwordHash = userData["passwordHash"] ?: ""
            )
            
            android.util.Log.d("FirebaseAuthService", "Successfully parsed user: ${user.email}, verified: ${user.isEmailVerified}")
            user
            
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuthService", "Error parsing user data", e)
            null
        }
    }

    /**
     * Hash password for storage
     */
    private fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(password.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "password_hash_error"
        }
    }

    /**
     * Verify password against stored hash
     */
    private fun verifyPassword(password: String, passwordHash: String): Boolean {
        return hashPassword(password) == passwordHash
    }
}