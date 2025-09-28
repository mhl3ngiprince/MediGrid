package com.example.medigrid.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.data.RealTimeDataService
import com.example.medigrid.data.formatTimestamp
import com.example.medigrid.security.HealthcareAuthService
import com.example.medigrid.security.SecurityLogger
import com.example.medigrid.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Enhanced Data Models with Real-Time Integration
data class TelemedicineSession(
    val id: String,
    val patientId: String,
    val patientName: String,
    val patientPhone: String,
    val scheduledTime: String,
    val scheduledDate: String,
    val status: SessionStatus,
    val consultationType: ConsultationType,
    val priority: Priority,
    val connectionQuality: ConnectionQuality = ConnectionQuality.UNKNOWN,
    val notes: String = "",
    val isLiveSession: Boolean = false,
    val startTime: Long? = null,
    val duration: Int = 0 // in minutes
)

enum class SessionStatus {
    SCHEDULED, WAITING, IN_PROGRESS, COMPLETED, CANCELLED, RESCHEDULED
}

enum class ConsultationType {
    FOLLOW_UP, NEW_CONSULTATION, EMERGENCY, PRESCRIPTION_REVIEW, MENTAL_HEALTH
}

enum class Priority {
    LOW, NORMAL, HIGH, URGENT
}

enum class ConnectionQuality {
    EXCELLENT, GOOD, POOR, OFFLINE, UNKNOWN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemedicineScreen(
    currentUser: HealthcareAuthService.HealthcareUser?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val realTimeService = remember { RealTimeDataService.getInstance(context) }
    
    // Real-time data integration
    val systemStats by realTimeService.systemStats.collectAsState()
    val telemedicineActivity by realTimeService.telemedicineActivity.collectAsState()
    val healthMetrics by realTimeService.healthMetrics.collectAsState()
    val networkActivity by realTimeService.networkActivity.collectAsState()
    val clinicStatus by realTimeService.clinicStatus.collectAsState()
    
    // Local state
    var isConnected by remember { mutableStateOf(true) }
    var connectionQuality by remember { mutableStateOf(ConnectionQuality.GOOD) }
    var selectedSession by remember { mutableStateOf<TelemedicineSession?>(null) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var lastUpdateTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Simulated sessions list with real-time integration
    var sessions by remember { mutableStateOf(mutableListOf<TelemedicineSession>()) }

    val coroutineScope = rememberCoroutineScope()

    // Auto-refresh with real-time data every 15 seconds
    LaunchedEffect(Unit) {
        while (true) {
            lastUpdateTime = System.currentTimeMillis()
            // Simulate network quality based on system load
            isConnected = systemStats.networkLatency < 100
            connectionQuality = when {
                !isConnected || systemStats.networkLatency > 100 -> ConnectionQuality.OFFLINE
                systemStats.networkLatency > 80 -> ConnectionQuality.POOR
                systemStats.networkLatency > 50 -> ConnectionQuality.GOOD
                else -> ConnectionQuality.EXCELLENT
            }
            delay(15000) // 15 seconds
        }
    }

    // Merge real-time telemedicine sessions with local sessions
    val combinedSessions = remember(telemedicineActivity, sessions) {
        val liveSessions = telemedicineActivity.map { liveSession ->
            TelemedicineSession(
                id = liveSession.sessionId,
                patientId = "live_${liveSession.sessionId}",
                patientName = "Live Session with ${liveSession.provider}",
                patientPhone = "+27123456789",
                scheduledTime = formatTimestamp(liveSession.timestamp).split(" ")[1],
                scheduledDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                status = when (liveSession.status) {
                    "ACTIVE" -> SessionStatus.IN_PROGRESS
                    "SCHEDULED" -> SessionStatus.SCHEDULED
                    "COMPLETED" -> SessionStatus.COMPLETED
                    else -> SessionStatus.WAITING
                },
                consultationType = when (liveSession.type) {
                    "Emergency Consult" -> ConsultationType.EMERGENCY
                    "Follow-up" -> ConsultationType.FOLLOW_UP
                    else -> ConsultationType.NEW_CONSULTATION
                },
                priority = when (liveSession.status) {
                    "URGENT" -> Priority.URGENT
                    "ACTIVE" -> Priority.HIGH
                    else -> Priority.NORMAL
                },
                connectionQuality = connectionQuality,
                isLiveSession = true,
                startTime = liveSession.timestamp,
                duration = ((System.currentTimeMillis() - liveSession.timestamp) / 60000).toInt()
            )
        }
        (liveSessions + sessions).distinctBy { it.id }
    }

    val todaySessions = combinedSessions.filter {
        it.scheduledDate == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    LaunchedEffect(Unit) {
        SecurityLogger.logSecurityEvent(
            "telemedicine_accessed",
            mapOf(
                "user_id" to (currentUser?.id ?: "unknown"),
                "connection_status" to if (isConnected) "online" else "offline",
                "live_sessions" to telemedicineActivity.size.toString(),
                "system_load" to systemStats.cpuUsage.toString()
            ),
            context
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Enhanced Header with Real-Time System Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Telemedicine Center",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "WhatsApp • SMS • Video • ${systemStats.activeUsers} users active",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    EnhancedConnectivityIndicator(
                        isConnected = isConnected,
                        quality = connectionQuality,
                        networkLatency = systemStats.networkLatency,
                        lastUpdate = lastUpdateTime
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Enhanced Quick stats with real-time data
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickStatCard(
                        title = "Live Sessions",
                        value = "${telemedicineActivity.count { it.status == "ACTIVE" }}",
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f),
                        isLive = true
                    )
                    QuickStatCard(
                        title = "Today's Total",
                        value = "${todaySessions.size}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    QuickStatCard(
                        title = "Waiting",
                        value = "${combinedSessions.count { it.status == SessionStatus.WAITING }}",
                        color = WarningOrange,
                        modifier = Modifier.weight(1f)
                    )
                    QuickStatCard(
                        title = "Network Load",
                        value = "${systemStats.networkLatency}ms",
                        color = if (systemStats.networkLatency > 50) WarningOrange else SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Real-Time System Status Card
        LiveTelemedicineStatusCard(
            systemStats = systemStats,
            healthMetrics = healthMetrics,
            networkActivity = networkActivity,
            clinicStatus = clinicStatus
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showScheduleDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Schedule Session")
            }

            OutlinedButton(
                onClick = {
                    // Emergency consultation
                    showScheduleDialog = true
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = DangerRed
                )
            ) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Emergency")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isConnected) {
            OfflineModeCard(systemLoad = systemStats.cpuUsage)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Enhanced Sessions list with live data
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        text = "Live Consultations",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Green, CircleShape)
                        )
                        Text(
                            text = "${todaySessions.size} scheduled • LIVE",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (todaySessions.isEmpty()) {
                    EmptySessionsCard()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(todaySessions) { session ->
                            EnhancedSessionCard(
                                session = session,
                                isConnected = isConnected,
                                onJoinSession = {
                                    selectedSession = session
                                    SecurityLogger.logSecurityEvent(
                                        "telemedicine_session_joined",
                                        mapOf(
                                            "session_id" to session.id,
                                            "patient_id" to session.patientId,
                                            "connection_quality" to connectionQuality.name,
                                            "is_live_session" to session.isLiveSession.toString()
                                        ),
                                        context
                                    )
                                },
                                onSendWhatsApp = { session ->
                                    sendWhatsAppMessage(session, context)
                                },
                                onSendSMS = { session ->
                                    sendSMSMessage(session, context)
                                },
                                onReschedule = { sessionToReschedule ->
                                    // Remove session from local list
                                    sessions.removeIf { it.id == sessionToReschedule.id }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Schedule Dialog
    if (showScheduleDialog) {
        ScheduleConsultationDialog(
            onDismiss = { showScheduleDialog = false },
            onSchedule = { newSession ->
                // Add session to local simulated persistence
                sessions.add(newSession)
                showScheduleDialog = false

                // Send confirmation WhatsApp message
                sendScheduleConfirmation(newSession, context)

                SecurityLogger.logSecurityEvent(
                    "telemedicine_session_scheduled",
                    mapOf(
                        "session_id" to newSession.id,
                        "patient_id" to newSession.patientId,
                        "scheduled_time" to newSession.scheduledTime
                    ),
                    context
                )
            }
        )
    }

    // Video session overlay
    selectedSession?.let { session ->
        VideoSessionOverlay(
            session = session,
            isConnected = isConnected,
            connectionQuality = connectionQuality,
            onEndSession = {
                // Update session status locally
                sessions.replaceAll {
                    if (it.id == session.id) it.copy(status = SessionStatus.COMPLETED) else it
                }
                selectedSession = null
            }
        )
    }
}

@Composable
private fun EnhancedConnectivityIndicator(
    isConnected: Boolean,
    quality: ConnectionQuality,
    networkLatency: Int,
    lastUpdate: Long
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isConnected) Icons.Default.Phone else Icons.Default.Warning,
            contentDescription = "Connectivity",
            modifier = Modifier.size(20.dp),
            tint = when {
                !isConnected -> DangerRed
                quality == ConnectionQuality.EXCELLENT -> SuccessGreen
                quality == ConnectionQuality.GOOD -> MaterialTheme.colorScheme.primary
                else -> WarningOrange
            }
        )
        
        Column {
            Text(
                text = if (isConnected) "Connected" else "Offline",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (isConnected) {
                Text(
                    text = quality.name.lowercase().replaceFirstChar { it.uppercase() },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        Text(
            text = "Updated ${System.currentTimeMillis() - lastUpdate}ms ago",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun QuickStatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    isLive: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = color,
                textAlign = TextAlign.Center
            )
            if (isLive) {
                Text(
                    text = "Live",
                    fontSize = 10.sp,
                    color = color,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun EnhancedSessionCard(
    session: TelemedicineSession,
    isConnected: Boolean,
    onJoinSession: () -> Unit,
    onSendWhatsApp: (TelemedicineSession) -> Unit,
    onSendSMS: (TelemedicineSession) -> Unit,
    onReschedule: (TelemedicineSession) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (session.status) {
                SessionStatus.IN_PROGRESS -> SuccessGreen.copy(alpha = 0.1f)
                SessionStatus.WAITING -> WarningOrange.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        when (session.status) {
                            SessionStatus.WAITING -> WarningOrange
                            SessionStatus.IN_PROGRESS -> SuccessGreen
                            SessionStatus.SCHEDULED -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        },
                        CircleShape
                    )
            )
            
            // Session info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = session.patientName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = "${session.scheduledTime} • ${session.scheduledDate} • ${session.consultationType.name.replace('_', ' ')}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.priority != Priority.NORMAL) {
                    Text(
                        text = "${session.priority.name} Priority",
                        fontSize = 11.sp,
                        color = if (session.priority == Priority.URGENT) DangerRed else WarningOrange,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (session.status) {
                    SessionStatus.WAITING, SessionStatus.IN_PROGRESS -> {
                        Button(
                            onClick = onJoinSession,
                            enabled = isConnected,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen
                            )
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Join")
                        }
                    }
                    SessionStatus.SCHEDULED -> {
                        if (!isConnected) {
                            OutlinedButton(
                                onClick = { onReschedule(session) }
                            ) {
                                Text("Reschedule")
                            }
                        } else {
                            Text(
                                text = "Ready at ${session.scheduledTime}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = session.status.name,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = { onSendWhatsApp(session) }
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp")
                }
                OutlinedButton(
                    onClick = { onSendSMS(session) }
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SMS")
                }
            }
        }
    }
}

@Composable
private fun OfflineModeCard(systemLoad: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Offline",
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Column {
                Text(
                    text = "Offline Mode Active",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Video calls unavailable. You can still schedule sessions for when connectivity returns. System load: $systemLoad%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun EmptySessionsCard() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "No sessions",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No consultations scheduled for today",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScheduleConsultationDialog(
    onDismiss: () -> Unit,
    onSchedule: (TelemedicineSession) -> Unit
) {
    // Implementation for scheduling dialog
    var patientName by remember { mutableStateOf("") }
    var patientPhone by remember { mutableStateOf("") }
    var scheduledTime by remember { mutableStateOf("") }
    var scheduledDate by remember { mutableStateOf("") }
    var consultationType by remember { mutableStateOf(ConsultationType.FOLLOW_UP) }
    var priority by remember { mutableStateOf(Priority.NORMAL) }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Consultation") },
        text = {
            Column {
                TextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text("Patient Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text)
                )
                TextField(
                    value = patientPhone,
                    onValueChange = { patientPhone = it },
                    label = { Text("Patient Phone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone)
                )
                TextField(
                    value = scheduledTime,
                    onValueChange = { scheduledTime = it },
                    label = { Text("Scheduled Time") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = scheduledDate,
                    onValueChange = { scheduledDate = it },
                    label = { Text("Scheduled Date") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { consultationType = ConsultationType.FOLLOW_UP },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (consultationType == ConsultationType.FOLLOW_UP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Follow Up")
                    }
                    Button(
                        onClick = { consultationType = ConsultationType.NEW_CONSULTATION },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (consultationType == ConsultationType.NEW_CONSULTATION) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("New")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { priority = Priority.NORMAL },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (priority == Priority.NORMAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Normal")
                    }
                    Button(
                        onClick = { priority = Priority.HIGH },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (priority == Priority.HIGH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("High")
                    }
                    Button(
                        onClick = { priority = Priority.URGENT },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (priority == Priority.URGENT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Urgent")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Validate required fields
                if (patientName.isBlank() || patientPhone.isBlank() || scheduledTime.isBlank() || scheduledDate.isBlank()) {
                    // Show error but don't crash
                    return@TextButton
                }

                try {
                    onSchedule(
                        TelemedicineSession(
                            id = UUID.randomUUID().toString(),
                            patientId = UUID.randomUUID().toString(),
                            patientName = patientName.trim(),
                            patientPhone = patientPhone.trim(),
                            scheduledTime = scheduledTime.trim(),
                            scheduledDate = scheduledDate.trim(),
                            status = SessionStatus.SCHEDULED,
                            consultationType = consultationType,
                            priority = priority
                        )
                    )
                } catch (e: Exception) {
                    // Log error but don't crash
                    SecurityLogger.logSecurityEvent(
                        "telemedicine_schedule_error",
                        mapOf(
                            "error" to (e.message ?: "unknown"),
                            "patient_name" to patientName
                        ),
                        context
                    )
                }
            }) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LiveTelemedicineStatusCard(
    systemStats: com.example.medigrid.data.SystemStats,
    healthMetrics: com.example.medigrid.data.HealthMetrics,
    networkActivity: List<com.example.medigrid.data.NetworkActivity>,
    clinicStatus: Map<String, com.example.medigrid.data.RealTimeClinicStatus>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VideoCall,
                    contentDescription = "Telemedicine Status",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Live System Status",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Green, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${systemStats.cpuUsage}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (systemStats.cpuUsage > 70) WarningOrange else SuccessGreen
                    )
                    Text(
                        text = "CPU Usage",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${systemStats.networkLatency}ms",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (systemStats.networkLatency > 50) WarningOrange else SuccessGreen
                    )
                    Text(
                        text = "Latency",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${networkActivity.count { it.status == "ACTIVE" }}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Active Calls",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${clinicStatus.values.count { it.status == "OPERATIONAL" }}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Online Clinics",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoSessionOverlay(
    session: TelemedicineSession,
    isConnected: Boolean,
    connectionQuality: ConnectionQuality,
    onEndSession: () -> Unit
) {
    // Implementation for video session overlay
    Card(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Video Session with ${session.patientName}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Connection: ${connectionQuality.name}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onEndSession,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
            ) {
                Text("End Session")
            }
        }
    }
}

fun sendWhatsAppMessage(session: TelemedicineSession, context: android.content.Context) {
    try {
        // Validate phone number
        if (session.patientPhone.isBlank()) {
            SecurityLogger.logSecurityEvent(
                "whatsapp_send_failed",
                mapOf("reason" to "blank_phone_number"),
                context
            )
            return
        }

        val cleanPhone = session.patientPhone.replace("[^0-9+]".toRegex(), "")
        val url =
            "https://wa.me/$cleanPhone?text=Your telemedicine session is about to start. Please be ready."
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)

        SecurityLogger.logSecurityEvent(
            "whatsapp_message_sent",
            mapOf(
                "session_id" to session.id,
                "patient_name" to session.patientName
            ),
            context
        )
    } catch (e: Exception) {
        SecurityLogger.logSecurityEvent(
            "whatsapp_send_error",
            mapOf(
                "error" to (e.message ?: "unknown"),
                "session_id" to session.id
            ),
            context
        )
    }
}

fun sendSMSMessage(session: TelemedicineSession, context: android.content.Context) {
    try {
        // Validate phone number
        if (session.patientPhone.isBlank()) {
            SecurityLogger.logSecurityEvent(
                "sms_send_failed",
                mapOf("reason" to "blank_phone_number"),
                context
            )
            return
        }

        val cleanPhone = session.patientPhone.replace("[^0-9+]".toRegex(), "")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("sms:$cleanPhone")
            putExtra(
                "sms_body",
                "Your telemedicine session with Dr. ${session.patientName} is scheduled for ${session.scheduledTime} on ${session.scheduledDate}"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        SecurityLogger.logSecurityEvent(
            "sms_message_sent",
            mapOf(
                "session_id" to session.id,
                "patient_name" to session.patientName
            ),
            context
        )
    } catch (e: Exception) {
        SecurityLogger.logSecurityEvent(
            "sms_send_error",
            mapOf(
                "error" to (e.message ?: "unknown"),
                "session_id" to session.id
            ),
            context
        )
    }
}

fun sendScheduleConfirmation(session: TelemedicineSession, context: android.content.Context) {
    try {
        // Validate phone number
        if (session.patientPhone.isBlank()) {
            return
        }

        val cleanPhone = session.patientPhone.replace("[^0-9+]".toRegex(), "")
        val message =
            "Your telemedicine session has been scheduled for ${session.scheduledTime} on ${session.scheduledDate}. You will receive a reminder 15 minutes before the session."
        val url = "https://wa.me/$cleanPhone?text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)

        SecurityLogger.logSecurityEvent(
            "schedule_confirmation_sent",
            mapOf(
                "session_id" to session.id,
                "patient_name" to session.patientName
            ),
            context
        )
    } catch (e: Exception) {
        SecurityLogger.logSecurityEvent(
            "schedule_confirmation_error",
            mapOf(
                "error" to (e.message ?: "unknown"),
                "session_id" to session.id
            ),
            context
        )
    }
}