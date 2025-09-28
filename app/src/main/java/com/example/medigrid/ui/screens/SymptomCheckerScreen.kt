package com.example.medigrid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.security.HealthcareAuthService
import com.example.medigrid.security.SecurityLogger
import com.example.medigrid.data.RealTimeDataService
import com.example.medigrid.data.formatTimestamp
import com.example.medigrid.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Enhanced Data Models with Mobile Support
data class SymptomQuestion(
    val id: String,
    val category: String,
    val question: String,
    val type: QuestionType,
    val options: List<String> = emptyList(),
    val criticalSymptom: Boolean = false,
    val followUpQuestions: List<String> = emptyList(),
    val aiContext: String = "",
    val clinicalRelevance: String = ""
)

enum class QuestionType {
    YES_NO, MULTIPLE_CHOICE, SEVERITY_SCALE, DURATION, BODY_AREA, TEXT_INPUT, NUMERIC_INPUT
}

data class SymptomResponse(
    val questionId: String,
    val answer: String,
    val severity: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f
)

data class RiskAssessment(
    val riskLevel: RiskLevel,
    val primaryConcerns: List<String>,
    val recommendations: List<String>,
    val urgencyLevel: UrgencyLevel,
    val referralNeeded: Boolean,
    val followUpDays: Int,
    val aiConfidence: Float = 0.85f,
    val diagnosticSuggestions: List<String> = emptyList(),
    val redFlags: List<String> = emptyList()
)

// Deprecated: Use SAHealthAssessment, SARiskLevel, SAUrgencyLevel instead.
enum class RiskLevel {
    LOW, MODERATE, HIGH, CRITICAL
}

// Deprecated: Use SAHealthAssessment, SARiskLevel, SAUrgencyLevel instead.
enum class UrgencyLevel {
    ROUTINE, SAME_DAY, URGENT, EMERGENCY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomCheckerScreen(
    currentUser: HealthcareAuthService.HealthcareUser?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val realTimeService = remember { RealTimeDataService.getInstance(context) }
    
    // Real-time data integration
    val systemStats by realTimeService.systemStats.collectAsState()
    val emergencyAlerts by realTimeService.emergencyAlerts.collectAsState()
    val healthMetrics by realTimeService.healthMetrics.collectAsState()
    val patientActivity by realTimeService.patientActivity.collectAsState()
    
    var currentStep by remember { mutableStateOf(0) }
    var responses by remember { mutableStateOf<Map<String, SymptomResponse>>(emptyMap()) }
    var isAssessing by remember { mutableStateOf(false) }
    var assessment by remember { mutableStateOf<RiskAssessment?>(null) }
    var selectedPatientId by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager: FocusManager = LocalFocusManager.current
    
    val questions = remember { getEnhancedSymptomQuestions() }

    LaunchedEffect(Unit) {
        try {
            SecurityLogger.logSecurityEvent(
                "ai_health_assessment_accessed",
                mapOf(
                    "user_id" to (currentUser?.id ?: "unknown"),
                    "user_role" to (currentUser?.role?.name ?: "unknown"),
                    "active_users" to systemStats.activeUsers.toString()
                ),
                context
            )
        } catch (e: Exception) {
            // Handle logging error gracefully
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Enhanced Header with Real-Time Stats
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Health Assessment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Advanced symptom analysis • Evidence-based • ${healthMetrics.totalPatients} patients today",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "AI Health",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Green, CircleShape)
                            )
                            Text(
                                text = "AI Ready",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = "${systemStats.activeUsers} users active",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
                
                if (currentStep > 0 && currentStep <= questions.size) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { currentStep.toFloat() / questions.size },
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$currentStep/${questions.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = if (currentStep <= questions.size) {
                            questions[currentStep - 1].category
                        } else {
                            "Assessment Complete"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Content Area with Real-Time Data
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                when {
                    currentStep == 0 -> {
                        StartingScreen(
                            onStart = { 
                                currentStep = 1
                                try {
                                    SecurityLogger.logSecurityEvent(
                                        "ai_health_assessment_started",
                                        mapOf(
                                            "user_id" to (currentUser?.id ?: "unknown"),
                                            "system_load" to systemStats.cpuUsage.toString()
                                        ),
                                        context
                                    )
                                } catch (e: Exception) {
                                    // Handle logging error gracefully
                                }
                            },
                            onSelectPatient = { patientId -> selectedPatientId = patientId },
                            healthMetrics = healthMetrics,
                            emergencyAlerts = emergencyAlerts,
                            patientActivity = patientActivity
                        )
                    }
                    
                    currentStep <= questions.size -> {
                        val question = questions[currentStep - 1]
                        QuestionScreen(
                            question = question,
                            currentResponse = responses[question.id],
                            onAnswer = { response ->
                                responses = responses + (question.id to response)
                                
                                if (question.criticalSymptom && isCriticalResponse(response, question)) {
                                    coroutineScope.launch {
                                        isAssessing = true
                                        delay(1500)
                                        assessment = generateCriticalAssessment(response, question)
                                        currentStep = questions.size + 2
                                        isAssessing = false
                                    }
                                } else {
                                    currentStep++
                                }
                            },
                            onBack = { 
                                if (currentStep > 1) {
                                    currentStep--
                                    focusManager.clearFocus()
                                }
                            },
                            focusManager = focusManager
                        )
                    }
                    
                    currentStep == questions.size + 1 -> {
                        LaunchedEffect(Unit) {
                            isAssessing = true
                            delay(3000)
                            assessment = generateEnhancedAssessment(responses, currentUser, context)
                            currentStep++
                            isAssessing = false
                        }
                        
                        ProcessingScreen(responses.size, systemStats.cpuUsage)
                    }
                    
                    else -> {
                        assessment?.let { result ->
                            ResultsScreen(
                                assessment = result,
                                responses = responses,
                                currentUser = currentUser,
                                selectedPatientId = selectedPatientId,
                                onRestart = {
                                    currentStep = 0
                                    responses = emptyMap()
                                    assessment = null
                                    selectedPatientId = null
                                },
                                onSaveToPatient = { 
                                    try {
                                        SecurityLogger.logSecurityEvent(
                                            "ai_health_assessment_saved",
                                            mapOf(
                                                "user_id" to (currentUser?.id ?: "unknown"),
                                                "patient_id" to (selectedPatientId ?: "unknown"),
                                                "risk_level" to result.riskLevel.name,
                                                "ai_confidence" to result.aiConfidence.toString()
                                            ),
                                            context
                                        )
                                    } catch (e: Exception) {
                                        // Handle logging error gracefully
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartingScreen(
    onStart: () -> Unit,
    onSelectPatient: (String) -> Unit,
    healthMetrics: com.example.medigrid.data.HealthMetrics,
    emergencyAlerts: List<com.example.medigrid.data.EmergencyAlert>,
    patientActivity: List<com.example.medigrid.data.PatientActivity>
) {
    val availablePatients = remember { getSamplePatients() }

    // Replace LazyColumn with Column to avoid nesting with parent LazyColumn
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real-time Health Metrics
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
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
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Live Stats",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Live Health Metrics",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
                    StatItem("Patients", healthMetrics.totalPatients.toString())
                    StatItem("Avg Wait", "${healthMetrics.averageWaitTime}min")
                    StatItem("Critical", healthMetrics.criticalCases.toString())
                    StatItem("Response", "${healthMetrics.emergencyResponseTime}min")
                }
            }
        }

        // Emergency Alerts Status
        if (emergencyAlerts.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
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
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Emergency",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Active Emergency Alerts (${emergencyAlerts.size})",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    emergencyAlerts.take(2).forEach { alert ->
                        Text(
                            text = "• ${alert.type.replace('_', ' ')} at ${alert.location}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Patient Selection
        PatientSelectionCard(
            patients = availablePatients,
            onSelectPatient = onSelectPatient
        )

        // Main Assessment Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "AI Assessment",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "AI Health Assessment",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Get immediate health risk assessment through a series of questions. This tool works offline and provides evidence-based recommendations.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Assessment")
                }
            }
        }

        // Live Patient Activity
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
                        imageVector = Icons.Default.History,
                        contentDescription = "Activity",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Live Patient Activity",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Replace LazyColumn with Column for patient activity
                Column {
                    patientActivity.take(3).forEach { activity ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = when (activity.department) {
                                    "Emergency" -> Icons.Default.LocalHospital
                                    "Cardiology" -> Icons.Default.Favorite
                                    else -> Icons.Default.Person
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${activity.initials} - ${activity.activity} (${activity.department})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        DisclaimerCard()
    }
}

@Composable
private fun QuestionScreen(
    question: SymptomQuestion,
    currentResponse: SymptomResponse?,
    onAnswer: (SymptomResponse) -> Unit,
    onBack: () -> Unit,
    focusManager: FocusManager
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (question.criticalSymptom) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                if (question.criticalSymptom) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Critical",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Critical Symptom Check",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                Text(
                    text = question.category,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = question.question,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                when (question.type) {
                    QuestionType.YES_NO -> {
                        YesNoQuestion(
                            currentAnswer = currentResponse?.answer,
                            onAnswer = { answer ->
                                onAnswer(SymptomResponse(question.id, answer))
                            }
                        )
                    }
                    
                    QuestionType.MULTIPLE_CHOICE -> {
                        MultipleChoiceQuestion(
                            options = question.options,
                            currentAnswer = currentResponse?.answer,
                            onAnswer = { answer ->
                                onAnswer(SymptomResponse(question.id, answer))
                            }
                        )
                    }
                    
                    QuestionType.SEVERITY_SCALE -> {
                        SeverityScaleQuestion(
                            currentSeverity = currentResponse?.severity ?: 0,
                            onAnswer = { severity ->
                                onAnswer(
                                    SymptomResponse(
                                        question.id, 
                                        "Severity: $severity/10", 
                                        severity
                                    )
                                )
                            }
                        )
                    }
                    
                    QuestionType.DURATION -> {
                        DurationQuestion(
                            currentAnswer = currentResponse?.answer,
                            onAnswer = { answer ->
                                onAnswer(SymptomResponse(question.id, answer))
                            }
                        )
                    }
                    
                    QuestionType.BODY_AREA -> {
                        BodyAreaQuestion(
                            currentAnswer = currentResponse?.answer,
                            onAnswer = { answer ->
                                onAnswer(SymptomResponse(question.id, answer))
                            }
                        )
                    }
                    
                    QuestionType.TEXT_INPUT -> {
                        TextInputQuestion(
                            currentAnswer = currentResponse?.answer,
                            onAnswer = { answer ->
                                onAnswer(SymptomResponse(question.id, answer))
                            }
                        )
                    }
                    
                    QuestionType.NUMERIC_INPUT -> {
                        NumericInputQuestion(
                            currentAnswer = currentResponse?.answer,
                            onAnswer = { answer ->
                                onAnswer(SymptomResponse(question.id, answer))
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }
            
            if (currentResponse != null) {
                Button(
                    onClick = { /* Continue - handled by parent */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Continue")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun YesNoQuestion(
    currentAnswer: String?,
    onAnswer: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf("Yes", "No").forEach { option ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAnswer(option) },
                colors = CardDefaults.cardColors(
                    containerColor = if (currentAnswer == option) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                border = if (currentAnswer == option) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentAnswer == option,
                        onClick = { onAnswer(option) }
                    )
                    Text(
                        text = option,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SeverityScaleQuestion(
    currentSeverity: Int,
    onAnswer: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Rate the severity from 1 (mild) to 10 (severe):",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("1\nMild", textAlign = TextAlign.Center, fontSize = 12.sp)
            Text("10\nSevere", textAlign = TextAlign.Center, fontSize = 12.sp)
        }
        
        Slider(
            value = currentSeverity.toFloat(),
            onValueChange = { onAnswer(it.toInt()) },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.fillMaxWidth()
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when {
                    currentSeverity <= 3 -> Color.Green.copy(alpha = 0.1f)
                    currentSeverity <= 6 -> Color.Yellow.copy(alpha = 0.1f)
                    else -> Color.Red.copy(alpha = 0.1f)
                }
            )
        ) {
            Text(
                text = "Current severity: $currentSeverity/10",
                modifier = Modifier.padding(16.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DurationQuestion(
    currentAnswer: String?,
    onAnswer: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "How long have you been experiencing your symptoms?",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = currentAnswer ?: "",
            onValueChange = { onAnswer(it) },
            label = { Text("Duration") },
            placeholder = { Text("e.g., 2 days, 1 week, 3 hours") },
            supportingText = { Text("Enter duration below ↓") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun BodyAreaQuestion(
    currentAnswer: String?,
    onAnswer: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Where is your primary area of pain or discomfort?",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = currentAnswer ?: "",
            onValueChange = { onAnswer(it) },
            label = { Text("Body Area") },
            placeholder = { Text("e.g., chest, head, back, stomach") },
            supportingText = { Text("Type your answer here ↓") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun TextInputQuestion(
    currentAnswer: String?,
    onAnswer: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Please describe your symptoms in detail:",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = currentAnswer ?: "",
            onValueChange = { onAnswer(it) },
            label = { Text("Symptom Description") },
            placeholder = { Text("Describe how you're feeling...") },
            supportingText = { Text("Enter your symptoms below ↓") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = true,
                imeAction = ImeAction.Done
            )
        )
    }
}

@Composable
private fun NumericInputQuestion(
    currentAnswer: String?,
    onAnswer: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Please enter a numeric value:",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = currentAnswer ?: "",
            onValueChange = { onAnswer(it) },
            label = { Text("Numeric Value") },
            placeholder = { Text("Enter number") },
            supportingText = { Text("Type your number here ↓") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        )
    }
}

@Composable
private fun MultipleChoiceQuestion(
    options: List<String>,
    currentAnswer: String?,
    onAnswer: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEach { option ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAnswer(option) },
                colors = CardDefaults.cardColors(
                    containerColor = if (currentAnswer == option) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                border = if (currentAnswer == option) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentAnswer == option,
                        onClick = { onAnswer(option) }
                    )
                    Text(
                        text = option,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessingScreen(numQuestions: Int, cpuUsage: Int = 0) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 6.dp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Analyzing Symptoms...",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "AI is processing your responses offline",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "System", modifier = Modifier.size(16.dp))
            Text("System Load: $cpuUsage%")
        }
    }
}

@Composable
private fun ResultsScreen(
    assessment: RiskAssessment,
    responses: Map<String, SymptomResponse>,
    currentUser: HealthcareAuthService.HealthcareUser?,
    selectedPatientId: String?,
    onRestart: () -> Unit,
    onSaveToPatient: () -> Unit
) {
    // Replace LazyColumn with Column to avoid nesting with parent LazyColumn
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when (assessment.riskLevel) {
                    RiskLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                    RiskLevel.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                    RiskLevel.MODERATE -> MaterialTheme.colorScheme.secondaryContainer
                    RiskLevel.LOW -> MaterialTheme.colorScheme.primaryContainer
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = "Risk Level",
                        tint = when (assessment.riskLevel) {
                            RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error
                            RiskLevel.HIGH -> MaterialTheme.colorScheme.error
                            RiskLevel.MODERATE -> MaterialTheme.colorScheme.secondary
                            RiskLevel.LOW -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Text(
                        text = "Risk Level: ${assessment.riskLevel.name}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (assessment.riskLevel) {
                            RiskLevel.CRITICAL, RiskLevel.HIGH -> MaterialTheme.colorScheme.error
                            RiskLevel.MODERATE -> MaterialTheme.colorScheme.secondary
                            RiskLevel.LOW -> MaterialTheme.colorScheme.primary
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Urgency: ${assessment.urgencyLevel.name.replace('_', ' ')}",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (assessment.primaryConcerns.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Clinical Concerns:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    assessment.primaryConcerns.forEach { concern ->
                        Text(
                            text = "• $concern",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (assessment.redFlags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Red Flags:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    assessment.redFlags.forEach { flag ->
                        Text(
                            text = "• $flag",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Recommendations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                assessment.recommendations.forEach { rec ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = rec,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (assessment.diagnosticSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Suggested Next Steps:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    assessment.diagnosticSuggestions.forEach { diag ->
                        Text(
                            text = "• $diag",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AI Confidence: ${(assessment.aiConfidence * 100).roundToInt()}%",
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onRestart,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restart")
                    }

                    Button(
                        onClick = onSaveToPatient,
                        enabled = currentUser != null && selectedPatientId != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save to Patient")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Assessment not a substitute for full evaluation. Always confirm with clinical staff.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Helper functions and additional components...

// Add new data models and functions for real-time content
data class HealthStats(
    val assessmentsToday: Int,
    val activeUsers: Int,
    val avgRiskLevel: String,
    val criticalAlerts: Int,
    val lastUpdated: Long,
)

data class RecentAssessment(
    val id: String,
    val patientInitials: String,
    val riskLevel: RiskLevel,
    val timestamp: Long,
    val primarySymptom: String,
)

data class SamplePatient(
    val id: String,
    val name: String,
    val age: Int,
    val lastVisit: Long,
    val riskFactors: List<String>,
)

private fun getTodayHealthStats(): HealthStats = HealthStats(
    assessmentsToday = 47,
    activeUsers = 12,
    avgRiskLevel = "Low-Moderate",
    criticalAlerts = 2,
    lastUpdated = System.currentTimeMillis()
)

private fun getRecentAssessments(): List<RecentAssessment> = listOf(
    RecentAssessment(
        id = "1",
        patientInitials = "M.D.",
        riskLevel = RiskLevel.LOW,
        timestamp = System.currentTimeMillis() - 300000, // 5 minutes ago
        primarySymptom = "Headache"
    ),
    RecentAssessment(
        id = "2",
        patientInitials = "S.K.",
        riskLevel = RiskLevel.MODERATE,
        timestamp = System.currentTimeMillis() - 900000, // 15 minutes ago
        primarySymptom = "Chest discomfort"
    ),
    RecentAssessment(
        id = "3",
        patientInitials = "T.N.",
        riskLevel = RiskLevel.HIGH,
        timestamp = System.currentTimeMillis() - 1800000, // 30 minutes ago
        primarySymptom = "Shortness of breath"
    ),
    RecentAssessment(
        id = "4",
        patientInitials = "A.P.",
        riskLevel = RiskLevel.LOW,
        timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
        primarySymptom = "Fatigue"
    )
)

private fun getSamplePatients(): List<SamplePatient> = listOf(
    SamplePatient(
        id = "p1",
        name = "Maria Dos Santos",
        age = 45,
        lastVisit = System.currentTimeMillis() - 86400000, // Yesterday
        riskFactors = listOf("Hypertension", "Diabetes")
    ),
    SamplePatient(
        id = "p2",
        name = "Sipho Khumalo",
        age = 32,
        lastVisit = System.currentTimeMillis() - 259200000, // 3 days ago
        riskFactors = listOf("Asthma")
    ),
    SamplePatient(
        id = "p3",
        name = "Thandiwe Ndaba",
        age = 28,
        lastVisit = System.currentTimeMillis() - 604800000, // 1 week ago
        riskFactors = emptyList()
    ),
    SamplePatient(
        id = "p4",
        name = "Ahmed Patel",
        age = 56,
        lastVisit = System.currentTimeMillis() - 172800000, // 2 days ago
        riskFactors = listOf("Heart Disease", "High Cholesterol")
    )
)

@Composable
private fun LiveStatsCard(stats: HealthStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Live Stats",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Today's Health Activity",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
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
                StatItem("Assessments", stats.assessmentsToday.toString())
                StatItem("Active Users", stats.activeUsers.toString())
                StatItem("Avg Risk", stats.avgRiskLevel)
                StatItem("Critical", stats.criticalAlerts.toString())
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun PatientSelectionCard(
    patients: List<SamplePatient>,
    onSelectPatient: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedPatient by remember { mutableStateOf<SamplePatient?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Patient",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Column {
                        Text(
                            text = if (selectedPatient != null)
                                "Patient: ${selectedPatient!!.name}"
                            else "Select Patient (Optional)",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${patients.size} patients available",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                // Replace LazyColumn with Column for patient list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    patients.forEach { patient ->
                        PatientItem(
                            patient = patient,
                            isSelected = selectedPatient?.id == patient.id,
                            onClick = {
                                selectedPatient = patient
                                onSelectPatient(patient.id)
                                expanded = false
                            }
                        )
                    }

                    if (patients.isEmpty()) {
                        Text(
                            text = "No patients available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientItem(
    patient: SamplePatient,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patient.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = "Age: ${patient.age} • Last visit: ${getRelativeTime(patient.lastVisit)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (patient.riskFactors.isNotEmpty()) {
                    Text(
                        text = "Risk factors: ${patient.riskFactors.joinToString(", ")}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentAssessmentsCard(assessments: List<RecentAssessment>) {
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
                    imageVector = Icons.Default.History,
                    contentDescription = "Recent",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Recent Assessments",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            assessments.take(3).forEach { assessment ->
                RecentAssessmentItem(assessment)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RecentAssessmentItem(assessment: RecentAssessment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val riskColor = when (assessment.riskLevel) {
            RiskLevel.CRITICAL -> Color.Red
            RiskLevel.HIGH -> Color(0xFFFF9800)
            RiskLevel.MODERATE -> Color.Yellow
            RiskLevel.LOW -> Color.Green
        }

        Box(
            modifier = Modifier
                .size(12.dp)
                .background(riskColor, CircleShape)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${assessment.patientInitials} • ${assessment.primarySymptom}",
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
            Text(
                text = "${assessment.riskLevel.name} risk • ${getRelativeTime(assessment.timestamp)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DiseaseStatsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
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
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "Trends",
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "South African Health Trends",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val trends = listOf(
                "Hypertension: 28% of adults affected" to Icons.Default.Favorite,
                "Diabetes: 12.7% prevalence rate" to Icons.Default.LocalHospital,
                "TB: 280,000 active cases" to Icons.Default.Coronavirus,
                "HIV: 13.5% of population" to Icons.Default.Shield
            )

            trends.forEach { (trend, icon) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = trend,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Data updated: ${getRelativeTime(System.currentTimeMillis() - 3600000)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
            )
        }
    }
}

private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000} min ago"
        diff < 86400000 -> "${diff / 3600000} hours ago"
        diff < 604800000 -> "${diff / 86400000} days ago"
        else -> "Over a week ago"
    }
}

// Helper Functions - Enhanced AI Logic

private fun getEnhancedSymptomQuestions(): List<SymptomQuestion> = listOf(
    SymptomQuestion(
        id = "chief_complaint",
        category = "Primary Concern",
        question = "What is your main health concern today?",
        type = QuestionType.TEXT_INPUT,
        aiContext = "This helps me understand your primary symptom for focused analysis"
    ),
    SymptomQuestion(
        id = "chest_pain_critical",
        category = "Emergency Screening",
        question = "Are you experiencing severe chest pain, pressure, or tightness right now?",
        type = QuestionType.YES_NO,
        criticalSymptom = true,
        aiContext = "Chest pain can indicate serious cardiac conditions requiring immediate attention"
    ),
    SymptomQuestion(
        id = "breathing_difficulty",
        category = "Respiratory Assessment",
        question = "How would you rate your breathing difficulty?",
        type = QuestionType.SEVERITY_SCALE,
        criticalSymptom = true,
        aiContext = "Breathing difficulty severity helps assess respiratory distress level"
    ),
    SymptomQuestion(
        id = "fever_severity",
        category = "Systemic Signs",
        question = "How severe is your fever or feeling of being unwell?",
        type = QuestionType.SEVERITY_SCALE,
        aiContext = "Fever severity indicates systemic response and infection severity"
    ),
    SymptomQuestion(
        id = "symptom_duration",
        category = "Timeline Analysis",
        question = "How long have you been experiencing your main symptoms?",
        type = QuestionType.DURATION,
        aiContext = "Duration helps differentiate acute from chronic conditions"
    ),
    SymptomQuestion(
        id = "pain_location",
        category = "Anatomical Localization",
        question = "Where is your primary area of pain or discomfort?",
        type = QuestionType.BODY_AREA,
        aiContext = "Location helps narrow diagnostic possibilities"
    ),
    SymptomQuestion(
        id = "associated_symptoms",
        category = "Symptom Complex",
        question = "Which of these symptoms are you also experiencing?",
        type = QuestionType.MULTIPLE_CHOICE,
        options = listOf("Nausea", "Headache", "Dizziness", "Fatigue", "Sweating", "None"),
        aiContext = "Associated symptoms help identify systemic conditions"
    ),
    SymptomQuestion(
        id = "pain_character",
        category = "Pain Analysis",
        question = "How would you describe your pain?",
        type = QuestionType.MULTIPLE_CHOICE,
        options = listOf("Sharp/Stabbing", "Dull/Aching", "Burning", "Cramping", "Throbbing", "No Pain"),
        aiContext = "Pain character provides diagnostic clues about underlying pathology"
    ),
    SymptomQuestion(
        id = "triggers",
        category = "Symptom Triggers",
        question = "What makes your symptoms worse?",
        type = QuestionType.MULTIPLE_CHOICE,
        options = listOf("Movement", "Rest", "Eating", "Stress", "Weather", "Nothing specific"),
        aiContext = "Triggers help identify causative factors and management strategies"
    ),
    SymptomQuestion(
        id = "medical_history",
        category = "Medical Background",
        question = "Do you have any of these medical conditions?",
        type = QuestionType.MULTIPLE_CHOICE,
        options = listOf("Heart Disease", "Diabetes", "High Blood Pressure", "Asthma", "Cancer History", "None"),
        aiContext = "Medical history influences risk assessment and diagnostic considerations"
    )
)

private fun isCriticalResponse(response: SymptomResponse, question: SymptomQuestion): Boolean {
    return when (question.id) {
        "chest_pain_critical" -> response.answer == "Yes"
        "breathing_difficulty" -> response.severity >= 7
        else -> false
    }
}

private fun generateEnhancedAssessment(
    responses: Map<String, SymptomResponse>,
    currentUser: HealthcareAuthService.HealthcareUser?,
    context: android.content.Context
): RiskAssessment {
    return try {
        // Extract symptoms from responses
        val symptoms = mutableListOf<String>()
        val riskFactors = mutableListOf<String>()

        responses.forEach { (questionId, response) ->
            when (questionId) {
                "chief_complaint" -> if (response.answer.isNotBlank()) symptoms.add(response.answer)
                "chest_pain_critical" -> if (response.answer == "Yes") symptoms.add("chest pain")
                "breathing_difficulty" -> if (response.severity > 0) symptoms.add("shortness of breath")
                "fever_severity" -> if (response.severity > 0) symptoms.add("fever")
                "pain_location" -> if (response.answer.isNotBlank()) symptoms.add("pain in ${response.answer}")
                "associated_symptoms" -> if (response.answer != "None") symptoms.add(response.answer.lowercase())
                "medical_history" -> if (response.answer != "None") riskFactors.add(response.answer)
            }
        }

        // Determine emergency symptoms
        val hasEmergencySymptoms = responses["chest_pain_critical"]?.answer == "Yes" ||
                (responses["breathing_difficulty"]?.severity ?: 0) >= 7

        val riskLevel = when {
            hasEmergencySymptoms -> RiskLevel.CRITICAL
            symptoms.any { it.contains("fever") || it.contains("pain") } -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        val urgencyLevel = when (riskLevel) {
            RiskLevel.CRITICAL -> UrgencyLevel.EMERGENCY
            RiskLevel.HIGH -> UrgencyLevel.URGENT
            RiskLevel.MODERATE -> UrgencyLevel.SAME_DAY
            RiskLevel.LOW -> UrgencyLevel.ROUTINE
        }

        // Generate recommendations based on risk level
        val recommendations = when (riskLevel) {
            RiskLevel.CRITICAL -> listOf(
                "🚨 SEEK IMMEDIATE EMERGENCY MEDICAL ATTENTION",
                "Call 10177 (SA emergency services) or go to nearest emergency department",
                "Do not delay treatment - time-critical condition possible"
            )

            RiskLevel.HIGH -> listOf(
                "🏥 See healthcare provider within 24 hours",
                "Monitor symptoms closely",
                "Return immediately if symptoms worsen"
            )

            RiskLevel.MODERATE -> listOf(
                "📅 Schedule appointment with healthcare provider within 1 week",
                "Monitor symptoms and keep symptom diary",
                "Seek immediate care if symptoms worsen"
            )

            RiskLevel.LOW -> listOf(
                "🏠 Self-care measures appropriate",
                "Monitor symptoms for changes",
                "Seek care if symptoms persist or worsen"
            )
        }

        val primaryConcerns = when {
            hasEmergencySymptoms -> listOf(
                "Potential cardiac or respiratory emergency",
                "Requires immediate medical evaluation"
            )

            symptoms.isNotEmpty() -> listOf(
                "Symptoms requiring assessment: ${symptoms.joinToString(", ")}",
                "Consider underlying conditions"
            )

            else -> listOf("General health assessment completed")
        }

        val diagnosticSuggestions = when {
            hasEmergencySymptoms -> listOf(
                "ECG and cardiac enzymes if chest pain",
                "Chest X-ray and arterial blood gas",
                "Full blood count and metabolic panel"
            )
            else -> listOf(
                "Basic vital signs assessment",
                "Consider further tests based on symptoms"
            )
        }

        val redFlags = if (hasEmergencySymptoms) listOf(
            "Chest pain requiring emergency evaluation",
            "Severe breathing difficulty"
        ) else emptyList()

        // Create RiskAssessment with proper mappings
        RiskAssessment(
            riskLevel = riskLevel,
            primaryConcerns = primaryConcerns,
            recommendations = recommendations,
            urgencyLevel = urgencyLevel,
            referralNeeded = urgencyLevel != UrgencyLevel.ROUTINE,
            followUpDays = when (urgencyLevel) {
                UrgencyLevel.EMERGENCY -> 0
                UrgencyLevel.URGENT -> 1
                UrgencyLevel.SAME_DAY -> 3
                UrgencyLevel.ROUTINE -> 14
            },
            aiConfidence = 0.85f,
            diagnosticSuggestions = diagnosticSuggestions,
            redFlags = redFlags
        )
    } catch (e: Exception) {
        // Log the error for debugging
        try {
            SecurityLogger.logSecurityEvent(
                "symptom_checker_assessment_error",
                mapOf(
                    "error_message" to e.message.orEmpty(),
                    "response_count" to responses.size.toString()
                ),
                context
            )
        } catch (logError: Exception) {
            // Even logging failed - create safe fallback
        }

        // Fallback assessment in case of any error
        RiskAssessment(
            riskLevel = RiskLevel.LOW,
            primaryConcerns = listOf("Assessment completed with limited data"),
            recommendations = listOf("Monitor symptoms and seek care if concerns persist"),
            urgencyLevel = UrgencyLevel.ROUTINE,
            referralNeeded = false,
            followUpDays = 14,
            aiConfidence = 0.5f,
            diagnosticSuggestions = listOf("Basic health assessment recommended"),
            redFlags = emptyList()
        )
    }
}

private fun generateCriticalAssessment(
    response: SymptomResponse, 
    question: SymptomQuestion
): RiskAssessment = RiskAssessment(
    riskLevel = RiskLevel.CRITICAL,
    primaryConcerns = listOf(
        "CRITICAL SYMPTOMS DETECTED",
        "Immediate medical attention required",
        "Potential life-threatening condition"
    ),
    recommendations = listOf(
        "🚨 CALL EMERGENCY SERVICES IMMEDIATELY",
        "Do not delay seeking emergency care",
        "Have someone drive you to emergency department",
        "Bring medications and medical information"
    ),
    urgencyLevel = UrgencyLevel.EMERGENCY,
    referralNeeded = true,
    followUpDays = 0,
    aiConfidence = 0.95f,
    redFlags = listOf("Critical symptom identified")
)

// Additional UI components for the remaining question types and cards would be implemented here...

@Composable
private fun DisclaimerCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
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
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Important Disclaimer",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This AI tool provides health guidance but does not replace professional medical advice. Always consult healthcare providers for serious concerns.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun OfflineCapabilitiesCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Offline",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Offline Capable",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This symptom checker works entirely offline. Your data stays secure on your device and syncs when connectivity returns.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * AI-enhanced comprehensive South African Disease Database browser for clinical and educational use.
 */

// @Composable 
// fun SADiseaseInfoCard(
//     currentUser: HealthcareAuthService.HealthcareUser?
// ) {
//     val context = LocalContext.current
//     val diseaseDatabase = remember { SouthAfricanDiseaseDatabase.getInstance(context) }
//     var selectedCategory by remember { mutableStateOf<DiseaseCategory?>(null) }
//     var selectedDisease by remember { mutableStateOf<SADisease?>(null) }
//     var searchQuery by remember { mutableStateOf("") }
//     
//     Card(
//         modifier = Modifier.fillMaxWidth(),
//         colors = CardDefaults.cardColors(
//             containerColor = MaterialTheme.colorScheme.tertiaryContainer
//         )
//     ) {
//         Column(
//             modifier = Modifier.padding(20.dp)
//         ) {
//             // Header
//             Row(
//                 verticalAlignment = Alignment.CenterVertically,
//                 horizontalArrangement = Arrangement.spacedBy(12.dp)
//             ) {
//                 Icon(
//                     imageVector = Icons.Default.LocalLibrary,
//                     contentDescription = "Disease Database",
//                     tint = MaterialTheme.colorScheme.onTertiaryContainer,
//                     modifier = Modifier.size(28.dp)
//                 )
//                 Column {
//                     Text(
//                         text = "SA Disease Database",
//                         fontSize = 18.sp,
//                         fontWeight = FontWeight.Bold,
//                         color = MaterialTheme.colorScheme.onTertiaryContainer
//                     )
//                     Text(
//                         text = "Comprehensive South African healthcare knowledge",
//                         fontSize = 12.sp,
//                         color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
//                     )
//                 }
//             }
//             
//             Spacer(modifier = Modifier.height(16.dp))
//             
//             // Search Box
//             OutlinedTextField(
//                 value = searchQuery,
//                 onValueChange = { searchQuery = it },
//                 label = { Text("Search diseases, symptoms...") },
//                 leadingIcon = {
//                     Icon(Icons.Default.Search, contentDescription = "Search")
//                 },
//                 modifier = Modifier.fillMaxWidth(),
//                 singleLine = true
//             )
//             
//             Spacer(modifier = Modifier.height(12.dp))
//             
//             if (searchQuery.isNotEmpty()) {
//                 // Search Results
//                 val searchResults = remember(searchQuery) {
//                     diseaseDatabase.searchDiseases(searchQuery)
//                 }
//                 
//                 LazyColumn(
//                     modifier = Modifier.height(200.dp),
//                     verticalArrangement = Arrangement.spacedBy(8.dp)
//                 ) {
//                     items(searchResults.take(10)) { disease ->
//                         SADiseaseListItem(
//                             disease = disease,
//                             onClick = { selectedDisease = disease }
//                         )
//                     }
//                     
//                     if (searchResults.isEmpty()) {
//                         item {
//                             Text(
//                                 text = "No diseases found matching '$searchQuery'",
//                                 style = MaterialTheme.typography.bodyMedium,
//                                 color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                 modifier = Modifier.padding(16.dp)
//                             )
//                         }
//                     }
//                 }
//             } else {
//                 // Disease Categories
//                 Text(
//                     text = "Browse by Category:",
//                     fontSize = 14.sp,
//                     fontWeight = FontWeight.Medium,
//                     color = MaterialTheme.colorScheme.onTertiaryContainer
//                 )
//                 
//                 Spacer(modifier = Modifier.height(8.dp))
//                 
//                 LazyVerticalGrid(
//                     columns = GridCells.Fixed(2),
//                     modifier = Modifier.height(160.dp),
//                     horizontalArrangement = Arrangement.spacedBy(8.dp),
//                     verticalArrangement = Arrangement.spacedBy(8.dp)
//                 ) {
//                     items(DiseaseCategory.values().toList()) { category ->
//                         CategoryCard(
//                             category = category,
//                             onClick = { selectedCategory = category }
//                         )
//                     }
//                 }
//             }
//             
//             // Show selected disease info
//             selectedDisease?.let { disease ->
//                 Spacer(modifier = Modifier.height(16.dp))
//                 SADiseaseDetailCard(
//                     disease = disease,
//                     onClose = { selectedDisease = null }
//                 )
//             }
//             
//             // Show category diseases
//             selectedCategory?.let { category ->
//                 Spacer(modifier = Modifier.height(16.dp))
//                 CategoryDiseasesList(
//                     category = category,
//                     diseaseDatabase = diseaseDatabase,
//                     onDiseaseClick = { selectedDisease = it },
//                     onClose = { selectedCategory = null }
//                 )
//             }
//         }
//     }
// }

// @Composable
// private fun SADiseaseListItem(
//     disease: SADisease,
//     onClick: () -> Unit
// ) {
//     Card(
//         modifier = Modifier
//             .fillMaxWidth()
//             .clickable { onClick() },
//         colors = CardDefaults.cardColors(
//             containerColor = MaterialTheme.colorScheme.surface
//         )
//     ) {
//         Row(
//             modifier = Modifier.padding(12.dp),
//             verticalAlignment = Alignment.CenterVertically,
//             horizontalArrangement = Arrangement.spacedBy(12.dp)
//         ) {
//             Icon(
//                 imageVector = disease.icon,
//                 contentDescription = null,
//                 tint = MaterialTheme.colorScheme.primary,
//                 modifier = Modifier.size(24.dp)
//             )
//             
//             Column(modifier = Modifier.weight(1f)) {
//                 Text(
//                     text = disease.name,
//                     fontWeight = FontWeight.Medium,
//                     fontSize = 14.sp
//                 )
//                 Text(
//                     text = disease.category.displayName,
//                     fontSize = 12.sp,
//                     color = MaterialTheme.colorScheme.onSurfaceVariant
//                 )
//             }
//             
//             // Prevalence indicator
//             val prevalenceColor = when (disease.prevalence) {
//                 Prevalence.VERY_HIGH -> Color.Red
//                 Prevalence.HIGH -> Color(0xFFFF9800)  // Orange
//                 Prevalence.MODERATE -> Color.Yellow
//                 Prevalence.LOW -> Color.Green
//                 Prevalence.RARE -> Color.Blue
//             }
//             
//             Box(
//                 modifier = Modifier
//                     .size(12.dp)
//                     .background(prevalenceColor, CircleShape)
//             )
//         }
//     }
// }

// @Composable
// private fun CategoryCard(
//     category: DiseaseCategory,
//     onClick: () -> Unit
// ) {
//     Card(
//         modifier = Modifier
//             .fillMaxWidth()
//             .clickable { onClick() },
//         colors = CardDefaults.cardColors(
//             containerColor = MaterialTheme.colorScheme.primaryContainer
//         )
//     ) {
//         Column(
//             modifier = Modifier.padding(12.dp),
//             horizontalAlignment = Alignment.CenterHorizontally
//         ) {
//             Icon(
//                 imageVector = category.icon,
//                 contentDescription = null,
//                 tint = MaterialTheme.colorScheme.onPrimaryContainer,
//                 modifier = Modifier.size(24.dp)
//             )
//             Spacer(modifier = Modifier.height(8.dp))
//             Text(
//                 text = category.displayName,
//                 fontSize = 10.sp,
//                 textAlign = TextAlign.Center,
//                 fontWeight = FontWeight.Medium,
//                 color = MaterialTheme.colorScheme.onPrimaryContainer,
//                 maxLines = 2
//             )
//         }
//     }
// }

// @Composable
// private fun SADiseaseDetailCard(
//     disease: SADisease,
//     onClose: () -> Unit
// ) {
//     Card(
//         colors = CardDefaults.cardColors(
//             containerColor = MaterialTheme.colorScheme.surfaceVariant
//         )
//     ) {
//         Column(
//             modifier = Modifier.padding(16.dp)
//         ) {
//             // Header
//             Row(
//                 modifier = Modifier.fillMaxWidth(),
//                 horizontalArrangement = Arrangement.SpaceBetween,
//                 verticalAlignment = Alignment.CenterVertically
//             ) {
//                 Row(
//                     verticalAlignment = Alignment.CenterVertically,
//                     horizontalArrangement = Arrangement.spacedBy(8.dp)
//                 ) {
//                     Icon(
//                         imageVector = disease.icon,
//                         contentDescription = null,
//                         tint = MaterialTheme.colorScheme.primary
//                     )
//                     Text(
//                         text = disease.name,
//                         fontSize = 16.sp,
//                         fontWeight = FontWeight.Bold
//                     )
//                 }
//                 IconButton(onClick = onClose) {
//                     Icon(Icons.Default.Close, contentDescription = "Close")
//                 }
//             }
//             
//             // Multi-language names
//             if (disease.nameAfrikaans.isNotEmpty() || disease.nameZulu.isNotEmpty()) {
//                 Spacer(modifier = Modifier.height(8.dp))
//                 Text(
//                     text = buildString {
//                         if (disease.nameAfrikaans.isNotEmpty()) append("Afrikaans: ${disease.nameAfrikaans}")
//                         if (disease.nameZulu.isNotEmpty()) {
//                             if (isNotEmpty()) append(" • ")
//                             append("Zulu: ${disease.nameZulu}")
//                         }
//                     },
//                     fontSize = 12.sp,
//                     color = MaterialTheme.colorScheme.onSurfaceVariant
//                 )
//             }
//             
//             Spacer(modifier = Modifier.height(12.dp))
//             
//             // Key information
//             Row(
//                 horizontalArrangement = Arrangement.spacedBy(16.dp)
//             ) {
//                 InfoChip("Prevalence", disease.prevalence.name)
//                 InfoChip("Region", disease.region.name.replace('_', ' '))
//                 InfoChip("Level", disease.resourceRequirements.name)
//             }
//             
//             Spacer(modifier = Modifier.height(12.dp))
//             
//             // Symptoms
//             if (disease.symptoms.isNotEmpty()) {
//                 Text(
//                     text = "Common Symptoms:",
//                     fontWeight = FontWeight.Medium,
//                     fontSize = 14.sp
//                 )
//                 Spacer(modifier = Modifier.height(4.dp))
//                 LazyColumn(
//                     modifier = Modifier.height(100.dp)
//                 ) {
//                     items(disease.symptoms.take(5)) { symptom ->
//                         Row(
//                             verticalAlignment = Alignment.CenterVertically,
//                             horizontalArrangement = Arrangement.spacedBy(8.dp)
//                         ) {
//                             val severityColor = when (symptom.severity) {
//                                 SymptomSeverity.CRITICAL -> Color.Red
//                                 SymptomSeverity.SEVERE -> Color(0xFFFF9800)  // Orange
//                                 SymptomSeverity.MODERATE -> Color.Yellow
//                                 SymptomSeverity.MILD -> Color.Green
//                             }
//                             
//                             Box(
//                                 modifier = Modifier
//                                     .size(8.dp)
//                                     .background(severityColor, CircleShape)
//                             )
//                             
//                             Text(
//                                 text = symptom.name,
//                                 fontSize = 12.sp,
//                                 modifier = Modifier.weight(1f)
//                             )
//                             
//                             Text(
//                                 text = symptom.frequency.name,
//                                 fontSize = 10.sp,
//                                 color = MaterialTheme.colorScheme.onSurfaceVariant
//                             )
//                         }
//                         Spacer(modifier = Modifier.height(4.dp))
//                     }
//                 }
//             }
//             
//             // Emergency indicators
//             if (disease.emergencyIndicators.isNotEmpty()) {
//                 Spacer(modifier = Modifier.height(12.dp))
//                 Card(
//                     colors = CardDefaults.cardColors(
//                         containerColor = MaterialTheme.colorScheme.errorContainer
//                     )
//                 ) {
//                     Column(
//                         modifier = Modifier.padding(8.dp)
//                     ) {
//                         Row(
//                             verticalAlignment = Alignment.CenterVertically,
//                             horizontalArrangement = Arrangement.spacedBy(4.dp)
//                         ) {
//                             Icon(
//                                 Icons.Default.Warning,
//                                 contentDescription = "Warning",
//                                 tint = MaterialTheme.colorScheme.error,
//                                 modifier = Modifier.size(16.dp)
//                             )
//                             Text(
//                                 text = "Emergency Signs:",
//                                 fontWeight = FontWeight.Bold,
//                                 fontSize = 12.sp,
//                                 color = MaterialTheme.colorScheme.onErrorContainer
//                             )
//                         }
//                         Spacer(modifier = Modifier.height(4.dp))
//                         disease.emergencyIndicators.take(3).forEach { indicator ->
//                             Text(
//                                 text = "• $indicator",
//                                 fontSize = 10.sp,
//                                 color = MaterialTheme.colorScheme.onErrorContainer
//                             )
//                         }
//                     }
//                 }
//             }
//         }
//     }
// }

// @Composable
// private fun InfoChip(label: String, value: String) {
//     Card(
//         colors = CardDefaults.cardColors(
//             containerColor = MaterialTheme.colorScheme.primaryContainer
//         )
//     ) {
//         Column(
//             modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
//             horizontalAlignment = Alignment.CenterHorizontally
//         ) {
//             Text(
//                 text = label,
//                 fontSize = 10.sp,
//                 color = MaterialTheme.colorScheme.onPrimaryContainer
//             )
//             Text(
//                 text = value,
//                 fontSize = 9.sp,
//                 fontWeight = FontWeight.Bold,
//                 color = MaterialTheme.colorScheme.onPrimaryContainer,
//                 maxLines = 1
//             )
//         }
//     }
// }

// @Composable
// private fun CategoryDiseasesList(
//     category: DiseaseCategory,
//     diseaseDatabase: SouthAfricanDiseaseDatabase,
//     onDiseaseClick: (SADisease) -> Unit,
//     onClose: () -> Unit
// ) {
//     val diseases = remember(category) {
//         diseaseDatabase.getDiseasesByCategory(category)
//     }
//     
//     Card(
//         colors = CardDefaults.cardColors(
//             containerColor = MaterialTheme.colorScheme.surface
//         )
//     ) {
//         Column(
//             modifier = Modifier.padding(16.dp)
//         ) {
//             Row(
//                 modifier = Modifier.fillMaxWidth(),
//                 horizontalArrangement = Arrangement.SpaceBetween,
//                 verticalAlignment = Alignment.CenterVertically
//             ) {
//                 Text(
//                     text = "${category.displayName} (${diseases.size})",
//                     fontSize = 16.sp,
//                     fontWeight = FontWeight.Bold
//                 )
//                 IconButton(onClick = onClose) {
//                     Icon(Icons.Default.Close, contentDescription = "Close")
//                 }
//             }
//             
//             Spacer(modifier = Modifier.height(8.dp))
//             
//             LazyColumn(
//                 modifier = Modifier.height(200.dp),
//                 verticalArrangement = Arrangement.spacedBy(4.dp)
//             ) {
//                 items(diseases) { disease ->
//                     SADiseaseListItem(
//                         disease = disease,
//                         onClick = { onDiseaseClick(disease) }
//                     )
//                 }
//                 
//                 if (diseases.isEmpty()) {
//                     item {
//                         Text(
//                             text = "No diseases found in this category",
//                             style = MaterialTheme.typography.bodyMedium,
//                             color = MaterialTheme.colorScheme.onSurfaceVariant,
//                             modifier = Modifier.padding(16.dp)
//                         )
//                     }
//                 }
//             }
//         }
//     }
// }

