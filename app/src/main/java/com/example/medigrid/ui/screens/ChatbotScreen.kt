package com.example.medigrid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.security.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
    currentUser: HealthcareAuthService.HealthcareUser?,
    modifier: Modifier = Modifier
) {
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputMessage by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var showQuickActions by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Initialize with welcome message
    LaunchedEffect(Unit) {
        messages = listOf(
            ChatMessage(
                id = "welcome",
                content = "Hello ${currentUser?.username ?: "Healthcare Professional"}! 👋\n\nI'm **MediBot**, your AI-powered healthcare assistant trained on:\n• Clinical guidelines & protocols\n• Drug interactions & contraindications\n• Diagnostic decision support\n• POPIA healthcare compliance\n• Emergency procedures\n\nHow can I assist you with your clinical practice today?",
                isUser = false,
                timestamp = System.currentTimeMillis(),
                messageType = MessageType.GREETING
            )
        )
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding() // Essential for keyboard handling
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "MediBot",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MediBot Healthcare AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Clinical Decision Support • POPIA Compliant",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Color.Green,
                                shape = RoundedCornerShape(50)
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Online",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quick Actions - Mobile optimized
        AnimatedVisibility(visible = showQuickActions) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(getQuickActions(currentUser)) { action ->
                    QuickActionChip(
                        action = action,
                        onClick = { 
                            inputMessage = action.prompt
                            showQuickActions = false
                            focusRequester.requestFocus()
                        }
                    )
                }
            }
        }

        // Messages - Mobile responsive
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                ChatMessageItem(
                    message = message,
                    currentUser = currentUser
                )
            }
            
            if (isTyping) {
                item {
                    TypingIndicator()
                }
            }

            // Extra space for keyboard
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Input area - Mobile optimized with keyboard handling
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        placeholder = { 
                            Text(
                                text = "Ask about symptoms, drugs, protocols, POPIA...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        label = { 
                            Text("Ask MediBot Healthcare AI") 
                        },
                        supportingText = {
                            Text(
                                text = if (inputMessage.isNotEmpty()) {
                                    "${inputMessage.length}/500 characters • AI will respond in ${(inputMessage.length * 50L).coerceAtMost(3000L) / 1000}s"
                                } else {
                                    "Type your healthcare question below ↓ • Examples: drug interactions, symptoms, protocols"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "AI Chat",
                                tint = if (inputMessage.isNotEmpty()) 
                                    MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Send,
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputMessage.isNotBlank()) {
                                    coroutineScope.launch {
                                        sendMessage(
                                            inputMessage,
                                            currentUser,
                                            context,
                                            onTypingStart = { isTyping = true },
                                            onTypingEnd = { isTyping = false }
                                        ) { userMsg, botResponse ->
                                            messages = messages + userMsg + botResponse
                                            inputMessage = ""
                                            showQuickActions = false
                                        }
                                    }
                                }
                                focusManager.clearFocus()
                            }
                        ),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (inputMessage.isNotEmpty()) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.outline,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (inputMessage.length > 100) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = "AI Ready",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                IconButton(
                                    onClick = {
                                        // Voice input functionality
                                        SecurityLogger.logSecurityEvent(
                                            "chatbot_voice_input_requested",
                                            mapOf("user_id" to (currentUser?.id ?: "unknown")),
                                            context
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )
                    
                    FloatingActionButton(
                        onClick = {
                            if (inputMessage.isNotBlank()) {
                                coroutineScope.launch {
                                    sendMessage(
                                        inputMessage,
                                        currentUser,
                                        context,
                                        onTypingStart = { isTyping = true },
                                        onTypingEnd = { isTyping = false }
                                    ) { userMsg, botResponse ->
                                        messages = messages + userMsg + botResponse
                                        inputMessage = ""
                                        showQuickActions = false
                                    }
                                }
                            }
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = if (inputMessage.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message",
                            tint = if (inputMessage.isNotBlank()) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // Helpful tips for mobile users
                if (inputMessage.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 Try: \"Drug interactions with aspirin\" or \"Chest pain protocol\"",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    currentUser: HealthcareAuthService.HealthcareUser?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = "MediBot",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            modifier = Modifier.widthIn(max = 320.dp), // Increased for mobile
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        fontSize = 10.sp,
                        color = if (message.isUser) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        }
                    )
                    
                    if (!message.isUser) {
                        Icon(
                            imageVector = when (message.messageType) {
                                MessageType.EMERGENCY -> Icons.Default.Warning
                                MessageType.MEDICATION -> Icons.Default.Medication
                                MessageType.MEDICAL_INFO -> Icons.Default.LocalHospital
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        if (message.isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "User",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun QuickActionChip(
    action: QuickAction,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = { 
            Text(
                action.title, 
                fontSize = 12.sp,
                maxLines = 1
            )
        },
        selected = false,
        leadingIcon = {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        },
        modifier = Modifier.height(36.dp) // Optimized for mobile touch
    )
}

@Composable
private fun TypingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = "MediBot",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    var alpha by remember { mutableStateOf(0.3f) }

                    LaunchedEffect(index) {
                        while (true) {
                            delay(300L + (300L * index.toLong()))
                            alpha = 1f
                            delay(600L)
                            alpha = 0.3f
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                                RoundedCornerShape(50)
                            )
                    )
                    
                    if (index < 2) Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

// Data classes
data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long,
    val messageType: MessageType = MessageType.TEXT
)

enum class MessageType {
    TEXT, GREETING, MEDICAL_INFO, EMERGENCY, MEDICATION
}

data class QuickAction(
    val title: String,
    val prompt: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// Helper functions - Enhanced AI responses
private fun getQuickActions(currentUser: HealthcareAuthService.HealthcareUser?): List<QuickAction> {
    return listOf(
        QuickAction("Symptom Analysis", "Help me analyze these patient symptoms:", Icons.Default.Search),
        QuickAction("Drug Check", "Check drug interactions for:", Icons.Default.Medication),
        QuickAction("Emergency Protocol", "What's the emergency protocol for:", Icons.Default.Warning),
        QuickAction("Clinical Guidelines", "Show me guidelines for:", Icons.Default.MenuBook),
        QuickAction("Differential Diagnosis", "Help with differential diagnosis for:", Icons.Default.Psychology),
        QuickAction("POPIA Compliance", "POPIA requirements for:", Icons.Default.Security)
    )
}

private suspend fun sendMessage(
    message: String,
    currentUser: HealthcareAuthService.HealthcareUser?,
    context: android.content.Context,
    onTypingStart: () -> Unit,
    onTypingEnd: () -> Unit,
    onComplete: (ChatMessage, ChatMessage) -> Unit
) {
    try {
        // Input validation
        if (message.isBlank()) {
            return
        }

        // Log the interaction
        SecurityLogger.logSecurityEvent(
            "chatbot_interaction",
            mapOf(
                "user_id" to (currentUser?.id ?: "unknown"),
                "message_length" to message.length,
                "user_role" to (currentUser?.role?.name ?: "unknown")
            ),
            context
        )

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = message.trim(),
            isUser = true,
            timestamp = System.currentTimeMillis()
        )

        // Start typing indicator
        onTypingStart()

        // Simulate AI processing with realistic delay
        try {
            delay(800L + (message.length * 50L).coerceAtMost(3000L))
        } catch (e: Exception) {
            delay(1500L) // Fallback delay
        }

        // Generate enhanced AI response
        val botResponse = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = generateEnhancedHealthcareResponse(message, currentUser),
            isUser = false,
            timestamp = System.currentTimeMillis(),
            messageType = determineMessageType(message)
        )

        onTypingEnd()
        onComplete(userMessage, botResponse)

    } catch (e: Exception) {
        onTypingEnd()
        SecurityLogger.logSecurityEvent(
            "chatbot_message_error",
            mapOf(
                "error" to (e.message ?: "unknown"),
                "user_id" to (currentUser?.id ?: "unknown")
            ),
            context
        )

        // Create error message for user
        val errorMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = "I apologize, but I'm experiencing a temporary issue. Please try your question again, or rephrase it for better results.",
            isUser = false,
            timestamp = System.currentTimeMillis(),
            messageType = MessageType.TEXT
        )

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = message.take(100), // Truncate to prevent issues
            isUser = true,
            timestamp = System.currentTimeMillis()
        )

        onComplete(userMessage, errorMessage)
    }
}

private fun generateEnhancedHealthcareResponse(
    message: String,
    currentUser: HealthcareAuthService.HealthcareUser?
): String {
    val lowerMessage = message.lowercase()
    
    return when {
        // Enhanced symptom analysis
        lowerMessage.contains("symptom") || lowerMessage.contains("pain") || lowerMessage.contains("ache") -> {
            """
            **🩺 Symptom Analysis Support**
            
            I can help analyze symptoms using evidence-based approaches. For comprehensive assessment, please provide:
            
            **📋 Essential Information:**
            • **Onset**: When did symptoms start?
            • **Duration**: How long present?
            • **Severity**: 1-10 scale
            • **Character**: Sharp, dull, cramping, etc.
            • **Location**: Specific area affected
            • **Radiation**: Does pain move/spread?
            • **Timing**: Constant vs intermittent
            • **Triggers**: What makes it worse/better?
            • **Associated symptoms**: Nausea, fever, etc.
            
            **⚠️ Red Flags to Consider:**
            • Severe sudden onset
            • Progressive worsening
            • Associated systemic symptoms
            • Patient appears unwell
            
            As a **${currentUser?.role?.name ?: "healthcare professional"}**, you have access to our clinical decision support tools and differential diagnosis algorithms.
            
            What specific symptoms would you like me to help analyze?
            """.trimIndent()
        }
        
        // Enhanced drug interactions
        lowerMessage.contains("drug") || lowerMessage.contains("medication") || lowerMessage.contains("interaction") -> {
            """
            **💊 Medication & Drug Interaction Analysis**
            
            I can provide comprehensive drug information including interactions, contraindications, and dosing guidelines.
            
            **🔍 Available Information:**
            • **Drug-Drug Interactions**: Major, moderate, minor
            • **Contraindications**: Absolute and relative
            • **Special Populations**: Pediatric, geriatric, pregnancy
            • **Renal/Hepatic Adjustments**: Dose modifications
            • **Adverse Effects**: Common and serious
            • **Monitoring Parameters**: Labs, vitals, symptoms
            
            **⚠️ Always Verify:**
            • Patient allergy history
            • Current medication list
            • Renal/hepatic function
            • Age and weight considerations
            • Pregnancy/breastfeeding status
            
            **🔬 Evidence Sources:**
            • WHO Essential Medicines
            • SAMF Guidelines
            • International pharmacovigilance data
            
            Which medications would you like me to analyze? Please specify:
            1. Primary medication of interest
            2. Any concurrent medications
            3. Patient demographics (age group, special conditions)
            """.trimIndent()
        }
        
        // Enhanced emergency protocols
        lowerMessage.contains("emergency") || lowerMessage.contains("urgent") || lowerMessage.contains("critical") -> {
            """
            **🚨 Emergency Clinical Protocols**
            
            **IMMEDIATE PRIORITY: ABC Assessment**
            • **A**irway - Patent, secure, protected
            • **B**reathing - Rate, effort, oxygen saturation
            • **C**irculation - Pulse, BP, perfusion, bleeding
            
            **📊 Rapid Assessment Tools:**
            • **NEWS Score** - National Early Warning System
            • **qSOFA** - Quick Sequential Organ Failure Assessment
            • **PEWS** - Pediatric Early Warning Score
            • **GCS** - Glasgow Coma Scale
            
            **🏥 Common Emergency Protocols:**
            
            **Cardiac Arrest:** CPR → Defibrillation → ACLS
            **Sepsis:** 3-hour bundle → Antibiotics → Fluid resuscitation
            **Stroke:** FAST assessment → CT brain → Thrombolysis window
            **MI:** ECG → Troponins → Dual antiplatelet → PCI
            **Anaphylaxis:** Adrenaline → Fluid → Steroids → H1/H2 blockers
            
            **⏰ Time-Critical Conditions:**
            • Stroke: 4.5-hour thrombolysis window
            • STEMI: 90-minute door-to-balloon
            • Sepsis: 1-hour antibiotic administration
            • Trauma: Golden hour principle
            
            What specific emergency scenario requires protocol guidance?
            """.trimIndent()
        }
        
        // Enhanced POPIA compliance
        lowerMessage.contains("popia") || lowerMessage.contains("privacy") || lowerMessage.contains("compliance") -> {
            """
            **🔒 POPIA Healthcare Compliance Guide**
            
            **Core POPIA Principles for Healthcare:**
            
            **1. 📋 Lawful Processing:**
            • Patient consent for data processing
            • Legitimate medical interest
            • Legal obligation (public health)
            • Vital interests (emergency care)
            
            **2. 🎯 Purpose Limitation:**
            • Collect only necessary health information
            • Use data only for stated medical purposes
            • Clear communication of data use
            
            **3. 🔐 Data Security:**
            • Access controls based on role (${currentUser?.role?.name})
            • Audit trails for all PHI access
            • Encryption for data transmission
            • Secure storage systems
            
            **4. 👤 Patient Rights:**
            • Right to access their health records
            • Right to correction of inaccurate data
            • Right to data portability
            • Right to restriction of processing
            
            **📊 Your Current Access Level:**
            • **Role**: ${currentUser?.role?.name ?: "Healthcare Professional"}
            • **PHI Access**: ${when(currentUser?.role?.name) {
                "DOCTOR" -> "Full patient records, prescribing"
                "NURSE" -> "Care-related information, basic records"
                "PHARMACIST" -> "Medication history, prescriptions"
                "ADMIN" -> "Administrative data, user management"
                else -> "Role-appropriate access"
            }}
            
            **🚨 POPIA Violations to Avoid:**
            • Accessing records outside your care role
            • Sharing patient info without consent
            • Using personal devices for PHI
            • Leaving systems unlocked/unattended
            
            All MediGrid interactions are logged for compliance monitoring.
            
            What specific POPIA compliance question do you have?
            """.trimIndent()
        }
        
        // Enhanced diagnostics
        lowerMessage.contains("diagnos") || lowerMessage.contains("differential") -> {
            """
            **🔬 Diagnostic Decision Support**
            
            **Clinical Reasoning Framework:**
            
            **1. 📝 History Taking:**
            • Chief complaint analysis
            • History of presenting illness (OPQRST)
            • Past medical/surgical history
            • Medications and allergies
            • Social and family history
            • Systems review
            
            **2. 🔍 Physical Examination:**
            • General appearance and vitals
            • System-specific examination
            • Focused vs comprehensive approach
            • Documentation of findings
            
            **3. 🧠 Differential Diagnosis:**
            • **Life-threatening first** (rule out worst)
            • **Common diseases** (horses, not zebras)
            • **Pattern recognition** vs analytical reasoning
            • **Anatomical approach** (organ systems)
            
            **4. 📊 Investigations:**
            • **Pre-test probability** assessment
            • **Sensitivity/Specificity** considerations
            • **Cost-effectiveness** analysis
            • **Risk vs benefit** evaluation
            
            **🎯 Diagnostic Tools Available:**
            • Clinical prediction rules
            • Laboratory interpretation
            • Imaging guidelines
            • Referral criteria
            
            **⚠️ Cognitive Biases to Avoid:**
            • Anchoring bias
            • Confirmation bias
            • Availability heuristic
            • Premature closure
            
            What clinical presentation would you like diagnostic support for? Please provide:
            • Patient demographics
            • Primary symptoms
            • Duration and progression
            • Key examination findings
            """.trimIndent()
        }
        
        // Enhanced treatment planning
        lowerMessage.contains("treatment") || lowerMessage.contains("management") || lowerMessage.contains("therapy") -> {
            """
            **💊 Evidence-Based Treatment Planning**
            
            **Treatment Decision Framework:**
            
            **1. 🎯 Goal Setting:**
            • Cure vs palliation
            • Symptom control priorities
            • Quality of life considerations
            • Patient preferences and values
            
            **2. 📚 Evidence Review:**
            • **Level I**: Systematic reviews, meta-analyses
            • **Level II**: Randomized controlled trials
            • **Level III**: Cohort studies
            • **Level IV**: Case series, expert opinion
            
            **3. 🔄 Treatment Options:**
            • **First-line therapy** (evidence-based)
            • **Alternative options** (contraindications)
            • **Combination therapy** (synergistic effects)
            • **Step-up/Step-down** approaches
            
            **4. 📊 Monitoring Plan:**
            • **Efficacy markers** (symptom improvement)
            • **Safety monitoring** (adverse effects)
            • **Laboratory monitoring** (drug levels, toxicity)
            • **Follow-up scheduling** (response assessment)
            
            **🏥 Treatment Considerations:**
            • Patient comorbidities
            • Drug interactions
            • Cost-effectiveness
            • Local availability
            • Resistance patterns (infections)
            
            **📋 South African Guidelines:**
            • SAMF formulary compliance
            • Essential medicines list
            • STG/EML recommendations
            • Provincial protocols
            
            **⚖️ Risk-Benefit Analysis:**
            • Number needed to treat (NNT)
            • Number needed to harm (NNH)
            • Absolute vs relative risk
            • Patient-specific factors
            
            What condition requires treatment planning support? Please specify:
            • Diagnosis/condition
            • Patient characteristics
            • Previous treatments tried
            • Current medications
            """.trimIndent()
        }
        
        // Clinical guidelines
        lowerMessage.contains("guideline") || lowerMessage.contains("protocol") || lowerMessage.contains("standard") -> {
            """
            **📖 Clinical Guidelines & Protocols**
            
            **Available Guideline Sources:**
            
            **🇿🇦 South African Guidelines:**
            • **SAMF** - South African Medicines Formulary
            • **NDOH** - National Department of Health protocols
            • **Provincial guidelines** - Local adaptations
            • **SAMA** - South African Medical Association
            
            **🌍 International Guidelines:**
            • **WHO** - World Health Organization
            • **AHA/ACC** - Cardiology guidelines
            • **ATS/ERS** - Respiratory guidelines
            • **EASD/ADA** - Diabetes guidelines
            • **NICE** - UK clinical guidelines
            
            **📚 Specialty Guidelines:**
            • **Infectious diseases** - Antibiotic stewardship
            • **Cardiovascular** - Hypertension, dyslipidemia
            • **Respiratory** - Asthma, COPD protocols
            • **Endocrine** - Diabetes, thyroid disorders
            • **Mental health** - Depression, anxiety treatment
            
            **🔄 Implementation Tools:**
            • Clinical pathways
            • Order sets
            • Decision algorithms
            • Quality indicators
            
            **📊 Evidence Grading:**
            • **Grade A** - Strong recommendation
            • **Grade B** - Moderate recommendation
            • **Grade C** - Weak recommendation
            • **Grade D** - Against intervention
            
            Which clinical area or condition requires guideline information?
            """.trimIndent()
        }
        
        // Default comprehensive response
        else -> {
            """
            **🤖 MediBot Healthcare AI Assistant**
            
            I'm here to support your clinical decision-making with evidence-based information. I can help with:
            
            **🩺 Clinical Support:**
            • **Symptom analysis** - OPQRST framework, red flags
            • **Differential diagnosis** - Systematic approach, pattern recognition
            • **Treatment planning** - Evidence-based, guideline recommendations
            • **Drug information** - Interactions, dosing, monitoring
            
            **🚨 Emergency Medicine:**
            • **Protocols** - ACLS, ATLS, sepsis bundles
            • **Risk stratification** - NEWS, qSOFA, PEWS
            • **Time-critical interventions** - Stroke, MI, trauma
            
            **📚 Guidelines & Evidence:**
            • **Clinical guidelines** - SAMF, WHO, international
            • **Quality measures** - Evidence grading, outcomes
            • **Best practices** - South African healthcare standards
            
            **🔐 Compliance & Safety:**
            • **POPIA requirements** - Healthcare data protection
            • **Patient safety** - Error prevention, reporting
            • **Professional standards** - HPCSA guidelines
            
            **💡 Example Queries:**
            • "Drug interactions with warfarin and antibiotics"
            • "Emergency protocol for chest pain in 60-year-old"
            • "POPIA compliance for sharing patient results"
            • "Differential diagnosis for acute abdominal pain"
            
            **Role-Specific Access:** ${currentUser?.role?.name ?: "Healthcare Professional"}
            
            How can I assist you with your clinical practice today?
            """.trimIndent()
        }
    }
}

private fun determineMessageType(message: String): MessageType {
    val lowerMessage = message.lowercase()
    return when {
        lowerMessage.contains("emergency") || lowerMessage.contains("urgent") || lowerMessage.contains("critical") -> MessageType.EMERGENCY
        lowerMessage.contains("drug") || lowerMessage.contains("medication") -> MessageType.MEDICATION
        lowerMessage.contains("symptom") || lowerMessage.contains("diagnos") || lowerMessage.contains("treatment") -> MessageType.MEDICAL_INFO
        else -> MessageType.TEXT
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}