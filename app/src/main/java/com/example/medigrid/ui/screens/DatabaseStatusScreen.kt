package com.example.medigrid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.security.FirebaseConfig
import com.example.medigrid.security.FirebaseDataService

@Composable
fun DatabaseStatusScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val firebaseDataService = remember { FirebaseDataService.getInstance(context) }
    val connectionStatus by firebaseDataService.connectionStatus.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Database",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Firebase Database Status",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item {
            DatabaseInfoCard(
                title = "Database URL",
                value = FirebaseConfig.DATABASE_URL,
                icon = Icons.Default.Link
            )
        }

        item {
            DatabaseInfoCard(
                title = "Project ID",
                value = FirebaseConfig.PROJECT_ID,
                icon = Icons.Default.Badge
            )
        }

        item {
            DatabaseInfoCard(
                title = "Storage Bucket",
                value = FirebaseConfig.STORAGE_BUCKET,
                icon = Icons.Default.CloudQueue
            )
        }

        item {
            DatabaseInfoCard(
                title = "Console URL",
                value = "console.firebase.google.com/project/${FirebaseConfig.PROJECT_ID}/database",
                icon = Icons.Default.Web
            )
        }

        item {
            ConnectionStatusCard(
                isConnected = connectionStatus,
                onTestConnection = {
                    firebaseDataService.simulateConnectionChange(!connectionStatus)
                }
            )
        }

        item {
            DatabasePathsCard()
        }
    }
}

@Composable
private fun DatabaseInfoCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    isConnected: Boolean,
    onTestConnection: () -> Unit,
) {
    val context = LocalContext.current
    val firebaseDataService = remember { FirebaseDataService.getInstance(context) }
    var isTesting by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = "Connection Status",
                        modifier = Modifier.size(32.dp),
                        tint = if (isConnected) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Connection Status",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isConnected) "Connected" else "Disconnected",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Button(
                        onClick = onTestConnection,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Test")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            isTesting = true
                            val result = firebaseDataService.testDatabaseConnection()
                            android.widget.Toast.makeText(
                                context,
                                if (result) "✅ Database test successful!" else "❌ Database test failed",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            isTesting = false
                        },
                        enabled = !isTesting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text("Test DB", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DatabasePathsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Database Paths",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))

            val paths = listOf(
                "telemedicine_sessions" to "Video consultations and remote care",
                "emergency_alerts" to "Critical health emergencies and alerts",
                "clinics_data" to "Healthcare facility information",
                "inventory_data" to "Medical supplies and equipment",
                "analytics_data" to "Healthcare metrics and insights",
                "healthcare_users" to "Medical staff and user accounts",
                "security_logs" to "Audit trails and security events",
                "patient_records" to "Protected health information (PHI)"
            )

            paths.forEach { (path, description) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "/$path",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}