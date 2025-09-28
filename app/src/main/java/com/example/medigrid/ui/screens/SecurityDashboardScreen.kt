package com.example.medigrid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.security.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDashboardScreen(
    currentUser: HealthcareAuthService.HealthcareUser?,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var securityReport by remember { mutableStateOf<SecurityTestFramework.SecurityReport?>(null) }
    var isRunningTests by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var emergencyReason by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            
            Text(
                text = "Security Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        isRunningTests = true
                        try {
                            securityReport = SecurityTestFramework.runSecurityAssessment(context)
                        } catch (e: Exception) {
                            SecurityLogger.logSecurityIncident(
                                "security_test_error",
                                e.message ?: "Unknown error",
                                context
                            )
                        } finally {
                            isRunningTests = false
                        }
                    }
                }
            ) {
                if (isRunningTests) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Run Security Tests")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // User Role Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = currentUser?.username ?: "Unknown User",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Role: ${currentUser?.role?.name ?: "Unknown"}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${currentUser?.role?.permissions?.size ?: 0} permissions",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Access Control") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("PHI Protection") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Compliance") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Security Tests") }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tab Content
        when (selectedTab) {
            0 -> AccessControlTab(currentUser)
            1 -> PhiProtectionTab(currentUser, onEmergencyAccess = { showEmergencyDialog = true })
            2 -> ComplianceTab(currentUser)
            3 -> SecurityTestsTab(securityReport, isRunningTests)
        }
    }
    
    // Emergency Access Dialog
    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = { Text("Emergency PHI Access") },
            text = {
                Column {
                    Text("This action will be logged and audited. Please provide a reason:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emergencyReason,
                        onValueChange = { emergencyReason = it },
                        label = { Text("Emergency Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emergencyReason.isNotBlank()) {
                            SecurityLogger.logSecurityEvent(
                                "emergency_phi_access",
                                mapOf(
                                    "user_id" to (currentUser?.id ?: "unknown"),
                                    "reason" to emergencyReason
                                ),
                                context
                            )
                            showEmergencyDialog = false
                            emergencyReason = ""
                        }
                    },
                    enabled = emergencyReason.isNotBlank()
                ) {
                    Text("Grant Access")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showEmergencyDialog = false
                    emergencyReason = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AccessControlTab(currentUser: HealthcareAuthService.HealthcareUser?) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Role-Based Access Control",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    currentUser?.role?.permissions?.forEach { permission ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Granted",
                                tint = Color.Green,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = permission,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Session Security",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    listOf(
                        "Session Timeout: ${SecurityConfig.SESSION_TIMEOUT_MINUTES} minutes",
                        "Max Login Attempts: ${SecurityConfig.MAX_LOGIN_ATTEMPTS}",
                        "Account Lockout: ${SecurityConfig.ACCOUNT_LOCKOUT_MINUTES} minutes",
                        "Multi-Factor Authentication: Enabled"
                    ).forEach { item ->
                        Row(
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
                                text = item,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PhiProtectionTab(
    currentUser: HealthcareAuthService.HealthcareUser?,
    onEmergencyAccess: () -> Unit
) {
    val context = LocalContext.current
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "PHI Protection Status",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    listOf(
                        "AES-256 Encryption: Active",
                        "Data Classification: Implemented",
                        "Access Logging: Enabled",
                        "Audit Trail: Complete"
                    ).forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Protected",
                                tint = Color.Green,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Data Sanitization",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Medical data is automatically sanitized to remove:",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    listOf(
                        "SA ID numbers → [ID_REDACTED]",
                        "Phone numbers → [PHONE_REDACTED]",
                        "Malicious scripts → Removed",
                        "SQL injection attempts → Blocked"
                    ).forEach { item ->
                        Text(
                            text = "• $item",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Emergency Access",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Break-glass access for critical situations. All emergency access is logged and audited.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            SecurityLogger.logSecurityEvent(
                                "emergency_access_attempt",
                                mapOf("user_id" to (currentUser?.id ?: "unknown")),
                                context
                            )
                            onEmergencyAccess()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Emergency"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Request Emergency Access")
                    }
                }
            }
        }
    }
}

@Composable
fun ComplianceTab(currentUser: HealthcareAuthService.HealthcareUser?) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "POPIA Compliance Status",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val complianceItems = listOf(
                        "Legal Basis: Healthcare Provision" to true,
                        "Data Minimization: Implemented" to true,
                        "Purpose Limitation: Enforced" to true,
                        "Consent Management: Active" to true,
                        "Data Subject Rights: Supported" to true,
                        "Breach Notification: Ready" to true
                    )
                    
                    complianceItems.forEach { (item, compliant) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (compliant) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = if (compliant) "Compliant" else "Non-compliant",
                                tint = if (compliant) Color.Green else Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Audit Logging",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "All PHI access and security events are logged with:",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    listOf(
                        "User identification (hashed)",
                        "Timestamp and session info",
                        "Action performed",
                        "Patient ID (hashed)",
                        "Purpose of access",
                        "Device and app version"
                    ).forEach { item ->
                        Text(
                            text = "• $item",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityTestsTab(
    securityReport: SecurityTestFramework.SecurityReport?,
    isRunning: Boolean
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (securityReport != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (securityReport.securityPosture) {
                            "GOOD" -> Color.Green.copy(alpha = 0.1f)
                            "LOW RISK" -> Color.Yellow.copy(alpha = 0.1f)
                            "MODERATE RISK" -> Color(0xFFFFA500).copy(alpha = 0.1f)
                            "HIGH RISK", "CRITICAL" -> Color.Red.copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Security Posture: ${securityReport.securityPosture}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${securityReport.passedTests}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Green)
                                Text("Passed", fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${securityReport.failedTests}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Red)
                                Text("Failed", fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${securityReport.criticalIssues}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Red)
                                Text("Critical", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            
            items(securityReport.results) { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            result.passed -> Color.Green.copy(alpha = 0.1f)
                            result.severity == SecurityTestFramework.TestSeverity.CRITICAL -> Color.Red.copy(alpha = 0.1f)
                            result.severity == SecurityTestFramework.TestSeverity.HIGH -> Color(0xFFFFA500).copy(alpha = 0.1f)
                            else -> Color.Yellow.copy(alpha = 0.1f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = result.testName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Icon(
                                imageVector = if (result.passed) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = if (result.passed) "Passed" else "Failed",
                                tint = if (result.passed) Color.Green else Color.Red
                            )
                        }
                        
                        Text(
                            text = "Category: ${result.category}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = result.description,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        
                        if (!result.passed && result.recommendation.isNotEmpty()) {
                            Text(
                                text = "Recommendation: ${result.recommendation}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        } else if (isRunning) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Running security assessment...")
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run Tests",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Tap the refresh button to run security tests")
                    }
                }
            }
        }
    }
}