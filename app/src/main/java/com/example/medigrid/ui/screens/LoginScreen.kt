package com.example.medigrid.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.medigrid.R
import com.example.medigrid.security.*
import kotlinx.coroutines.launch

// --- Password validation helpers ---
private fun getPasswordStrength(password: String): String {
    val length = password.length
    return when {
        length >= 8 && password.any { it.isDigit() } && password.any { it.isLetter() } -> "Strong"
        length >= 6 -> "Medium"
        length > 0 -> "Weak"
        else -> "Empty"
    }
}

@Composable
private fun getPasswordStrengthColor(password: String): androidx.compose.ui.graphics.Color {
    val strength = getPasswordStrength(password)
    return when (strength) {
        "Strong" -> MaterialTheme.colorScheme.primary
        "Medium" -> MaterialTheme.colorScheme.secondary
        "Weak" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun EmailVerificationCard(
    email: String,
    userName: String,
    userRole: SecurityConfig.HealthcareRole,
    onVerificationSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    var verificationCode by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var showCodeDirectly by remember { mutableStateOf(false) }
    var displayedCode by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val emailService = remember { EmailVerificationService(context) }
    val coroutineScope = rememberCoroutineScope()

    // Show code directly after 3 seconds as fallback
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        showCodeDirectly = true

        // First, try to get any existing stored verification code
        val sharedPrefs = context.getSharedPreferences("medigrid_verification", Context.MODE_PRIVATE)
        val storedCode = sharedPrefs.getString("code_$email", null)

        if (storedCode != null) {
            val parts = storedCode.split(":")
            if (parts.size >= 2) {
                val code = parts[0]
                val expiryTime = parts[1].toLongOrNull() ?: 0

                if (System.currentTimeMillis() <= expiryTime) {
                    displayedCode = code
                    android.util.Log.i("EmailVerification", "Using existing code for $email: $code")
                }
            }
        }

        // If no valid code found, generate a new one
        if (displayedCode.isEmpty()) {
            try {
                val newCode = emailService.generateAndStoreCode(email)
                displayedCode = newCode
                android.util.Log.i(
                    "EmailVerification",
                    "Generated new fallback code for $email: $newCode"
                )
            } catch (e: Exception) {
                // Final fallback - generate a simple code
                val simpleCode = (100000..999999).random().toString()
                displayedCode = simpleCode

                // Store this simple code manually
                val editor = sharedPrefs.edit()
                val expiryTime = System.currentTimeMillis() + (15 * 60 * 1000) // 15 minutes
                editor.putString("code_$email", "$simpleCode:$expiryTime:false")
                editor.apply()

                android.util.Log.i(
                    "EmailVerification",
                    "Generated emergency fallback code for $email: $simpleCode"
                )
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Email Verification",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Email Verification Required",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "A verification email has been sent to $email. Please check your email app and enter the 6-digit code below.",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // AI Assistant info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Assistant",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Email sent via MediGrid AI Assistant",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            if (showCodeDirectly && displayedCode.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Can't access email? Use this code:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = displayedCode,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(16.dp),
                                letterSpacing = 4.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Copy this code and paste it below",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (successMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = successMessage,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Verification Code Input
            OutlinedTextField(
                value = verificationCode,
                onValueChange = { 
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        verificationCode = it
                        errorMessage = ""
                        successMessage = ""
                    }
                },
                label = { Text("6-Digit Verification Code") },
                placeholder = { Text("123456") },
                supportingText = { 
                    Text(
                        text = "Enter the 6-digit code from your email ↓",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Pin,
                        contentDescription = "Code",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { /* Trigger verification */ }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (verificationCode.length == 6) 
                        MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.outline
                ),
                isError = errorMessage.isNotEmpty(),
                trailingIcon = {
                    Row {
                        if (verificationCode.length == 6) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Valid",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (showCodeDirectly && displayedCode.isNotEmpty()) {
                            IconButton(
                                onClick = { verificationCode = displayedCode }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste Code",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            )
            
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Verify Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        isVerifying = true
                        errorMessage = ""
                        successMessage = ""
                        
                        try {
                            val isValid = emailService.verifyCode(email, verificationCode)
                            if (isValid) {
                                // Mark email as verified in SharedPreferences
                                val sharedPrefs = context.getSharedPreferences("medigrid_users", Context.MODE_PRIVATE)
                                val userJson = sharedPrefs.getString("user_$email", null)
                                
                                if (userJson != null) {
                                    // Update the user's email verification status properly
                                    val updatedJson = userJson.replace("isEmailVerified=false", "isEmailVerified=true")
                                    val editor = sharedPrefs.edit()
                                    editor.putString("user_$email", updatedJson)
                                    editor.apply()
                                    
                                    // Also clear the verification code
                                    val verificationPrefs = context.getSharedPreferences("medigrid_verification", Context.MODE_PRIVATE)
                                    val verificationEditor = verificationPrefs.edit()
                                    verificationEditor.remove("code_$email")
                                    verificationEditor.apply()
                                }
                                
                                onVerificationSuccess()
                            } else {
                                errorMessage = "Invalid or expired verification code"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Verification failed. Please try again."
                        } finally {
                            isVerifying = false
                        }
                    }
                },
                enabled = !isVerifying && !isResending && verificationCode.length == 6,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Verify Code")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBackToLogin,
                    enabled = !isVerifying && !isResending
                ) {
                    Text("Back to Login")
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isResending = true
                            errorMessage = ""
                            successMessage = ""
                            
                            try {
                                val newCode = emailService.resendVerificationEmail(email, userName, userRole)
                                verificationCode = ""
                                displayedCode = newCode
                                successMessage = "New verification code generated"
                                
                                SecurityLogger.logSecurityEvent(
                                    "email_verification_resent",
                                    mapOf("email" to email),
                                    context
                                )
                            } catch (e: Exception) {
                                errorMessage = "Failed to generate new code. Please try again."
                            } finally {
                                isResending = false
                            }
                        }
                    },
                    enabled = !isVerifying && !isResending
                ) {
                    if (isResending) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.Email, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resend Email")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationDialog(
    firebaseAuthService: FirebaseAuthService,
    onDismiss: () -> Unit,
    onRegistrationSuccess: (FirebaseAuthService.HealthcareUser) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(SecurityConfig.HealthcareRole.NURSE) }
    var isRoleDropdownExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Available healthcare roles
    val availableRoles = SecurityConfig.HealthcareRole.values().toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Register",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Register Healthcare Worker")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it.trim().lowercase()
                        if (errorMessage.contains("email", ignoreCase = true)) {
                            errorMessage = ""
                        }
                    },
                    label = { Text("Healthcare Email Address") },
                    placeholder = { Text("doctor@hospital.co.za") },
                    supportingText = { 
                        Text(
                            text = "Use your professional healthcare email ↓",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (email.contains("@") && email.contains(".")) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Valid email format",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        autoCorrect = false
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { /* Move to next field */ }
                    ),
                    isError = errorMessage.contains("email", ignoreCase = true)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { 
                        username = it
                        if (errorMessage.contains("name", ignoreCase = true)) {
                            errorMessage = ""
                        }
                    },
                    label = { Text("Full Professional Name") },
                    placeholder = { Text("Dr. Sarah Johnson") },
                    supportingText = { 
                        Text(
                            text = "Enter your name as it appears on your license ↓",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Name",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (username.trim().length >= 3) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Valid name",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { /* Move to next field */ }
                    )
                )

                OutlinedTextField(
                    value = licenseNumber,
                    onValueChange = { licenseNumber = it.uppercase().trim() },
                    label = { Text("Professional License Number") },
                    placeholder = { Text("SA12345 or MP67890") },
                    supportingText = { 
                        Text(
                            text = "Optional - Your medical/nursing license number ↓",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = "License",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (licenseNumber.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "License provided",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Characters
                    )
                )

                // Healthcare Role Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = isRoleDropdownExpanded,
                    onExpandedChange = { isRoleDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedRole.name.replace('_', ' '),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Healthcare Role") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRoleDropdownExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = isRoleDropdownExpanded,
                        onDismissRequest = { isRoleDropdownExpanded = false }
                    ) {
                        availableRoles.forEach { role ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = role.name.replace('_', ' '),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = getRoleDescription(role),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedRole = role
                                    isRoleDropdownExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getRoleIcon(role),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (errorMessage.contains("password", ignoreCase = true)) {
                            errorMessage = ""
                        }
                    },
                    label = { Text("Create Password") },
                    placeholder = { Text("Minimum 6 characters") },
                    supportingText = {
                        Text(
                            text = "Password strength: ${getPasswordStrength(password)} ↓",
                            color = getPasswordStrengthColor(password)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Row {
                            if (password.length >= 6) {
                                Icon(
                                    imageVector = when {
                                        password.length >= 8 && password.any { it.isDigit() } &&
                                                password.any { it.isLetter() } -> Icons.Default.Security

                                        password.length >= 6 -> Icons.Default.Check
                                        else -> Icons.Default.Warning
                                    },
                                    contentDescription = "Password strength",
                                    tint = getPasswordStrengthColor(password),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { /* Move to next field */ }
                    ),
                    isError = password.isNotBlank() && password.length < 6
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        if (errorMessage.contains("match", ignoreCase = true)) {
                            errorMessage = ""
                        }
                    },
                    label = { Text("Confirm Password") },
                    placeholder = { Text("Re-enter your password") },
                    supportingText = {
                        Text(
                            text = if (confirmPassword.isNotBlank()) {
                                if (confirmPassword == password) "✓ Passwords match" else "⚠ Passwords don't match"
                            } else "Confirm your password above ↓",
                            color = if (confirmPassword.isNotBlank() && confirmPassword == password)
                                MaterialTheme.colorScheme.primary
                            else if (confirmPassword.isNotBlank())
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LockReset,
                            contentDescription = "Confirm Password",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (confirmPassword.isNotBlank()) {
                            Icon(
                                imageVector = if (confirmPassword == password)
                                    Icons.Default.CheckCircle
                                else Icons.Default.Error,
                                contentDescription = if (confirmPassword == password)
                                    "Passwords match" else "Passwords don't match",
                                tint = if (confirmPassword == password)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { /* Submit form if valid */ }
                    ),
                    isError = confirmPassword.isNotBlank() && confirmPassword != password
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "⚠️ Registration creates a Firebase account. Email verification required.",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = ""

                        if (password != confirmPassword) {
                            errorMessage = "Passwords do not match"
                            isLoading = false
                            return@launch
                        }

                        if (password.length < 6) {
                            errorMessage = "Password must be at least 6 characters"
                            isLoading = false
                            return@launch
                        }

                        try {
                            val result = firebaseAuthService.registerHealthcareWorker(
                                email = email,
                                password = password,
                                username = username,
                                role = selectedRole,
                                clinicId = "clinic_001",
                                licenseNumber = licenseNumber.takeIf { it.isNotBlank() }
                            )

                            if (result.success && result.user != null) {
                                // Generate and store verification code immediately during registration
                                val emailService = EmailVerificationService(context)
                                val verificationCode = emailService.sendVerificationEmail(
                                    email,
                                    username,
                                    selectedRole
                                )

                                // Log the generated code for development
                                android.util.Log.i(
                                    "Registration",
                                    "Verification code generated for $email: $verificationCode"
                                )

                                onRegistrationSuccess(result.user)
                            } else {
                                errorMessage = result.error ?: "Registration failed"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Registration error: ${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && username.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Register")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Get description for healthcare role
 */
private fun getRoleDescription(role: SecurityConfig.HealthcareRole): String {
    return when (role) {
        SecurityConfig.HealthcareRole.DOCTOR -> "Full patient access, prescriptions"
        SecurityConfig.HealthcareRole.NURSE -> "Patient care, basic records access"
        SecurityConfig.HealthcareRole.PHARMACIST -> "Prescription management, inventory"
        SecurityConfig.HealthcareRole.ADMIN -> "System administration, user management"
        SecurityConfig.HealthcareRole.RECEPTIONIST -> "Appointments, basic patient info"
    }
}

private fun getRoleIcon(role: SecurityConfig.HealthcareRole): ImageVector {
    return when (role) {
        SecurityConfig.HealthcareRole.DOCTOR -> Icons.Default.LocalHospital
        SecurityConfig.HealthcareRole.NURSE -> Icons.Default.Healing
        SecurityConfig.HealthcareRole.PHARMACIST -> Icons.Default.Medication
        SecurityConfig.HealthcareRole.ADMIN -> Icons.Default.AdminPanelSettings
        SecurityConfig.HealthcareRole.RECEPTIONIST -> Icons.Default.Person
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordResetDialog(
    firebaseAuthService: FirebaseAuthService,
    onDismiss: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var resetCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf(1) } // 1: Email, 2: Reset Code + New Password
    var showResetCodeDirectly by remember { mutableStateOf(false) }
    var displayedResetCode by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Show reset code directly when on step 2 as fallback
    LaunchedEffect(step) {
        if (step == 2) {
            kotlinx.coroutines.delay(2000)
            showResetCodeDirectly = true
            // Get the reset code and expiry from SharedPreferences
            val resetPrefs =
                context.getSharedPreferences("medigrid_password_reset", Context.MODE_PRIVATE)
            val storedResetData = resetPrefs.getString("reset_$email", null)
            if (storedResetData != null) {
                val parts = storedResetData.split(":")
                if (parts.size >= 2) {
                    val code = parts[0]
                    val expiryTime = parts[1].toLongOrNull() ?: 0

                    // Check if code is still valid
                    if (System.currentTimeMillis() <= expiryTime) {
                        displayedResetCode = code
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (step == 1) "Reset Password" else "Enter Reset Code")
        },
        text = {
            Column {
                if (message.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                when (step) {
                    1 -> {
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it.trim().lowercase()
                                message = ""
                            },
                            label = { Text("Healthcare Email Address") },
                            placeholder = { Text("your.email@hospital.co.za") },
                            supportingText = {
                                Text(
                                    text = "Enter the email address for your healthcare account ↓",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (email.contains("@") && email.contains(".") && email.length > 5) {
                                    Icon(
                                        imageVector = Icons.Default.MarkEmailRead,
                                        contentDescription = "Valid email",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done,
                                autoCorrect = false
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { /* Submit email for reset */ }
                            )
                        )
                    }
                    2 -> {
                        Text(
                            text = "Check your email for the reset code and enter it below:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // AI Assistant info card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "AI Assistant",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Password reset sent via AI Assistant",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        // Show reset code directly as fallback
                        if (showResetCodeDirectly && displayedResetCode.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = "Reset Code",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Can't access email? Use this reset code:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Display the reset code prominently
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Text(
                                            text = displayedResetCode,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondary,
                                            modifier = Modifier.padding(12.dp),
                                            letterSpacing = 2.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        OutlinedTextField(
                            value = resetCode,
                            onValueChange = { 
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    resetCode = it
                                    message = ""
                                }
                            },
                            label = { Text("6-Digit Reset Code") },
                            placeholder = { Text("123456") },
                            supportingText = { 
                                Text(
                                    text = "Enter the reset code from your email ↓",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "Reset Code",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                Row {
                                    if (resetCode.length == 6) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Valid code length",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    if (showResetCodeDirectly && displayedResetCode.isNotEmpty()) {
                                        IconButton(
                                            onClick = { resetCode = displayedResetCode }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "Paste Code",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { /* Move to password field */ }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (resetCode.length == 6) 
                                    MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.outline
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { 
                                newPassword = it
                                message = ""
                            },
                            label = { Text("New Password") },
                            placeholder = { Text("Create a strong password") },
                            supportingText = { 
                                Text(
                                    text = "Password strength: ${getPasswordStrength(newPassword)} ↓",
                                    color = getPasswordStrengthColor(newPassword)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = "New Password",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (newPassword.length >= 6) {
                                    Icon(
                                        imageVector = when {
                                            newPassword.length >= 8 && newPassword.any { it.isDigit() } && 
                                            newPassword.any { it.isLetter() } -> Icons.Default.Security
                                            newPassword.length >= 6 -> Icons.Default.Check
                                            else -> Icons.Default.Warning
                                        },
                                        contentDescription = "Password strength",
                                        tint = getPasswordStrengthColor(newPassword),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { /* Move to confirm password */ }
                            ),
                            isError = newPassword.isNotBlank() && newPassword.length < 6
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { 
                                confirmPassword = it
                                message = ""
                            },
                            label = { Text("Confirm New Password") },
                            placeholder = { Text("Re-enter your new password") },
                            supportingText = { 
                                Text(
                                    text = if (confirmPassword.isNotBlank()) {
                                        if (confirmPassword == newPassword) "✓ Passwords match" else "⚠ Passwords don't match"
                                    } else "Confirm your new password ↓",
                                    color = if (confirmPassword.isNotBlank() && confirmPassword == newPassword) 
                                        MaterialTheme.colorScheme.primary 
                                    else if (confirmPassword.isNotBlank()) 
                                        MaterialTheme.colorScheme.error 
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Confirm Password",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (confirmPassword.isNotBlank()) {
                                    Icon(
                                        imageVector = if (confirmPassword == newPassword) 
                                            Icons.Default.CheckCircle 
                                        else Icons.Default.Error,
                                        contentDescription = if (confirmPassword == newPassword) 
                                            "Passwords match" else "Passwords don't match",
                                        tint = if (confirmPassword == newPassword) 
                                            MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { /* Submit form */ }
                            ),
                            isError = confirmPassword.isNotBlank() && confirmPassword != newPassword
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        message = ""
                        isSuccess = false
                        
                        when (step) {
                            1 -> {
                                // Send reset email
                                val success = firebaseAuthService.sendPasswordResetEmail(email)
                                if (success) {
                                    message = "Password reset code sent to your email"
                                    isSuccess = true
                                    step = 2
                                } else {
                                    message = "Failed to send reset email. Please check your email address."
                                    isSuccess = false
                                }
                            }
                            2 -> {
                                // Verify code and reset password
                                if (newPassword != confirmPassword) {
                                    message = "Passwords do not match"
                                    isSuccess = false
                                } else if (newPassword.length < 6) {
                                    message = "Password must be at least 6 characters"
                                    isSuccess = false
                                } else {
                                    val success = firebaseAuthService.resetPasswordWithCode(email, resetCode, newPassword)
                                    if (success) {
                                        message = "Password reset successfully! You can now log in with your new password."
                                        isSuccess = true
                                    } else {
                                        message = "Invalid or expired reset code. Please try again."
                                        isSuccess = false
                                    }
                                }
                            }
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading && when (step) {
                    1 -> email.isNotBlank()
                    2 -> resetCode.length == 6 && newPassword.isNotBlank() && confirmPassword.isNotBlank()
                    else -> false
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text(when (step) {
                        1 -> "Send Reset Code"
                        2 -> "Reset Password"
                        else -> "Continue"
                    })
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (HealthcareAuthService.HealthcareUser) -> Unit,
    onNavigateToSecurity: () -> Unit
) {
    var email by remember { mutableStateOf("") } // Remove pre-filled test email
    var password by remember { mutableStateOf("") } // Remove pre-filled test password
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showRegistration by remember { mutableStateOf(false) }
    var showPasswordReset by remember { mutableStateOf(false) }
    var requiresVerification by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf(SecurityConfig.HealthcareRole.NURSE) }
    
    val context = LocalContext.current
    val firebaseAuthService = remember { FirebaseAuthService(context) }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // Helper to get username/role from SharedPreferences for verification step.
    fun getUserDataForVerification(email: String, context: Context): Pair<String, SecurityConfig.HealthcareRole> {
        val sharedPrefs = context.getSharedPreferences("medigrid_users", Context.MODE_PRIVATE)
        val userJson = sharedPrefs.getString("user_$email", null)
        if (userJson != null) {
            return try {
                val parts = userJson.split(",")
                val name = parts[2].split("=")[1].trim()
                val role = SecurityConfig.HealthcareRole.valueOf(parts[3].split("=")[1].trim())
                Pair(name, role)
            } catch (e: Exception) {
                Pair("User", SecurityConfig.HealthcareRole.NURSE)
            }
        }
        return Pair("User", SecurityConfig.HealthcareRole.NURSE)
    }

    // Improved login logic
    suspend fun performLogin(
        email: String,
        password: String,
        firebaseAuthService: FirebaseAuthService,
        context: Context,
        onLoading: (Boolean) -> Unit,
        onError: (String) -> Unit,
        onSuccess: (HealthcareAuthService.HealthcareUser) -> Unit,
        onRequiresVerification: () -> Unit
    ) {
        onLoading(true)
        onError("")
        try {
            val result = firebaseAuthService.signInWithEmailAndPassword(email, password)
            if (result.success && result.user != null) {
                val authUser = firebaseAuthService.getCurrentAuthUser()
                if (authUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "firebase_login_success",
                        mapOf("user_role" to authUser.role.name),
                        context
                    )
                    onSuccess(authUser)
                }
            } else if (result.requiresVerification) {
                onRequiresVerification()
                onError("")
            } else {
                onError(result.error ?: "Authentication failed")
            }
        } catch (e: Exception) {
            onError("System error: Please try again")
            SecurityLogger.logSecurityIncident(
                "login_system_error",
                e.message ?: "Unknown error",
                context
            )
        } finally {
            onLoading(false)
        }
    }

    // Make the screen scrollable for mobile keyboards
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .imePadding(), // Important for keyboard handling
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Logo and Title
        item {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "MediGrid Logo",
                modifier = Modifier.size(100.dp) // Smaller for mobile
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "MediGrid",
                fontSize = 28.sp, // Slightly smaller for mobile
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Secure Healthcare Management",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Security Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Firebase Auth • POPIA Compliant",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
        
        if (requiresVerification) {
            // Email Verification Required
            item {
                EmailVerificationCard(
                    email = email,
                    userName = username,
                    userRole = userRole,
                    onVerificationSuccess = {
                        onLoginSuccess(firebaseAuthService.getCurrentAuthUser()!!)
                    },
                    onBackToLogin = {
                        requiresVerification = false
                        errorMessage = ""
                    }
                )
            }
        } else {
            // Login Form
            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it.trim().lowercase()
                        errorMessage = "" // Clear error when user types
                    },
                    label = { Text("Healthcare Email Address") },
                    placeholder = { Text("your.name@hospital.co.za") },
                    supportingText = {
                        Text(
                            text = "Enter your registered healthcare email ↓",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (email.contains("@") && email.contains(".") && email.length > 5) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Valid email format",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        autoCorrect = false
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (email.contains("@") && email.contains("."))
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    ),
                    isError = errorMessage.contains("email", ignoreCase = true) ||
                            errorMessage.contains("invalid", ignoreCase = true)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                var passwordVisible by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        errorMessage = "" // Clear error when user types
                    },
                    label = { Text("Password") },
                    placeholder = { Text("Enter your secure password") },
                    supportingText = {
                        Text(
                            text = "Your healthcare account password ↓",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Row {
                            if (password.length >= 6) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Secure password",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            focusManager.clearFocus()
                            // Trigger login
                            if (email.isNotBlank() && password.isNotBlank()) {
                                coroutineScope.launch {
                                    performLogin(
                                        email = email,
                                        password = password,
                                        firebaseAuthService = firebaseAuthService,
                                        context = context,
                                        onLoading = { isLoading = it },
                                        onError = { errorMessage = it },
                                        onSuccess = { user -> onLoginSuccess(user) },
                                        onRequiresVerification = { 
                                            requiresVerification = true
                                            // Get user data for verification
                                            val userData = getUserDataForVerification(email, context)
                                            username = userData.first
                                            userRole = userData.second
                                        }
                                    )
                                }
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (password.length >= 6)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    ),
                    isError = errorMessage.contains("password", ignoreCase = true) ||
                            errorMessage.contains("incorrect", ignoreCase = true)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            
            // Error Message
            if (errorMessage.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            // Login Button
            item {
                Button(
                    onClick = {
                        focusManager.clearFocus() // Hide keyboard
                        coroutineScope.launch {
                            performLogin(
                                email = email,
                                password = password,
                                firebaseAuthService = firebaseAuthService,
                                context = context,
                                onLoading = { isLoading = it },
                                onError = { errorMessage = it },
                                onSuccess = { user -> onLoginSuccess(user) },
                                onRequiresVerification = { 
                                    requiresVerification = true
                                    // Get user data for verification
                                    val userData = getUserDataForVerification(email, context)
                                    username = userData.first
                                    userRole = userData.second
                                }
                            )
                        }
                    },
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Login",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign In",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = { showPasswordReset = true }
                    ) {
                        Text("Reset Password", fontSize = 13.sp)
                    }
                    
                    TextButton(
                        onClick = { showRegistration = true }
                    ) {
                        Text("Register", fontSize = 13.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Security Settings Button
            item {
                OutlinedButton(
                    onClick = onNavigateToSecurity,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Security Settings",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Security Settings", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Debug Panel (for development)
            item {
                DebugUserPanel(firebaseAuthService = firebaseAuthService)
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Footer information
        item {
            Text(
                text = "Secure healthcare management system\nCompliant with POPIA regulations",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Registration Dialog
    if (showRegistration) {
        RegistrationDialog(
            firebaseAuthService = firebaseAuthService,
            onDismiss = { showRegistration = false },
            onRegistrationSuccess = { user ->
                showRegistration = false
                requiresVerification = true
                username = user.username
                userRole = user.role
            }
        )
    }

    // Password Reset Dialog
    if (showPasswordReset) {
        PasswordResetDialog(
            firebaseAuthService = firebaseAuthService,
            onDismiss = { showPasswordReset = false }
        )
    }
}

@Composable
private fun DebugUserPanel(
    firebaseAuthService: FirebaseAuthService,
) {
    var showDebug by remember { mutableStateOf(false) }
    var storedUsers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔧 Debug Panel",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = {
                        showDebug = !showDebug
                        if (showDebug) {
                            storedUsers = firebaseAuthService.getAllStoredUsers()
                        }
                    }
                ) {
                    Text(
                        text = if (showDebug) "Hide" else "Show",
                        fontSize = 10.sp
                    )
                }
            }

            if (showDebug) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            firebaseAuthService.clearAllUsers()
                            storedUsers = emptyMap()
                            android.widget.Toast.makeText(
                                context,
                                "All users cleared",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All", fontSize = 10.sp)
                    }

                    Button(
                        onClick = {
                            storedUsers = firebaseAuthService.getAllStoredUsers()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Refresh", fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Stored Users: ${storedUsers.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                if (storedUsers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    storedUsers.forEach { (key, value) ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = key.removePrefix("user_"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = value.take(100) + if (value.length > 100) "..." else "",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}