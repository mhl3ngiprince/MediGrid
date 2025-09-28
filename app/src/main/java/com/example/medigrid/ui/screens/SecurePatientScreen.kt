package com.example.medigrid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.security.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurePatientScreen(
    currentUser: HealthcareAuthService.HealthcareUser,
    modifier: Modifier = Modifier
) {
    var showAddPatientDialog by remember { mutableStateOf(false) }
    var patients by remember { mutableStateOf(listOf<SecurePatient>()) }
    var searchQuery by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with security indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Patient Management",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "PHI Protected • POPIA Compliant",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (currentUser.role.permissions.contains("WRITE_PHI")) {
                    Button(
                        onClick = { showAddPatientDialog = true }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Patient")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Patient")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search with audit logging
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                SecurityLogger.logPhiAccess(
                    currentUser.id,
                    "search_query",
                    "patient_search",
                    "Patient search query",
                    context
                )
            },
            label = { Text("Search Patients") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Patient list with PHI protection
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(patients.filter { 
                it.displayName.contains(searchQuery, ignoreCase = true) 
            }) { patient ->
                SecurePatientCard(
                    patient = patient,
                    currentUser = currentUser,
                    onViewDetails = { patientId ->
                        SecurityLogger.logPhiAccess(
                            currentUser.id,
                            patientId,
                            "view_details",
                            "Patient details accessed",
                            context
                        )
                    }
                )
            }
            
            if (patients.isEmpty()) {
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
                                imageVector = Icons.Default.Person,
                                contentDescription = "No Patients",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No patients found",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Add Patient Dialog
    if (showAddPatientDialog) {
        AddPatientDialog(
            currentUser = currentUser,
            onDismiss = { showAddPatientDialog = false },
            onPatientAdded = { newPatient ->
                patients = patients + newPatient
                showAddPatientDialog = false
            }
        )
    }
}

@Composable
fun SecurePatientCard(
    patient: SecurePatient,
    currentUser: HealthcareAuthService.HealthcareUser,
    onViewDetails: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = patient.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Age: ${patient.age} • ${patient.gender}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Last Visit: ${patient.lastVisit}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    // Emergency indicator
                    if (patient.isEmergency) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Emergency",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    // PHI protection indicator
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "PHI Protected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sanitized symptoms (if any)
            if (patient.sanitizedSymptoms.isNotEmpty()) {
                Text(
                    text = "Symptoms: ${patient.sanitizedSymptoms}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onViewDetails(patient.id) }
                ) {
                    Text("View Details")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientDialog(
    currentUser: HealthcareAuthService.HealthcareUser,
    onDismiss: () -> Unit,
    onPatientAdded: (SecurePatient) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("MALE") }
    var symptoms by remember { mutableStateOf("") }
    var validationErrors by remember { mutableStateOf(mapOf<String, String>()) }
    var isProcessing by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Patient",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Patient")
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        isError = validationErrors.containsKey("name"),
                        supportingText = {
                            validationErrors["name"]?.let { 
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = idNumber,
                        onValueChange = { idNumber = it },
                        label = { Text("SA ID Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = validationErrors.containsKey("id_number"),
                        supportingText = {
                            validationErrors["id_number"]?.let { 
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Age *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = validationErrors.containsKey("age"),
                        supportingText = {
                            validationErrors["age"]?.let { 
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = validationErrors.containsKey("phone"),
                        supportingText = {
                            validationErrors["phone"]?.let { 
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    ExposedDropdownMenuBox(
                        expanded = false,
                        onExpandedChange = { }
                    ) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Gender *") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = symptoms,
                        onValueChange = { symptoms = it },
                        label = { Text("Symptoms") },
                        minLines = 3,
                        maxLines = 5,
                        placeholder = { Text("Describe patient symptoms...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "⚠️ All patient data is encrypted and POPIA compliant. Personal information will be automatically sanitized.",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isProcessing = true
                        
                        // Validate input data
                        val patientData = mapOf(
                            "name" to name,
                            "age" to age,
                            "phone" to phone,
                            "gender" to gender,
                            "symptoms" to symptoms
                        )
                        
                        val patientValidation = HealthcareInputValidator.validatePatientData(patientData)
                        val idValidation = HealthcareInputValidator.validateSaIdNumber(idNumber)
                        
                        validationErrors = patientValidation.errors + idValidation.errors
                        
                        if (validationErrors.isEmpty()) {
                            try {
                                // Create secure patient record with null safety
                                val securePatient = SecurePatient(
                                    id = generatePatientId(),
                                    displayName = patientValidation.sanitizedData["name"] as? String
                                        ?: name.trim(),
                                    age = (patientValidation.sanitizedData["age"] as? Int)
                                        ?: age.toIntOrNull()
                                        ?: 0,
                                    gender = patientValidation.sanitizedData["gender"] as? String
                                        ?: gender,
                                    sanitizedSymptoms = patientValidation.sanitizedData["symptoms"] as? String
                                        ?: symptoms.trim(),
                                    lastVisit = "Today",
                                    isEmergency = symptoms.lowercase()
                                        .contains("emergency") || symptoms.lowercase()
                                        .contains("urgent"),
                                    dataClassification = patientValidation.dataClassification
                                )

                                // Log PHI creation
                                SecurityLogger.logPhiAccess(
                                    currentUser.id,
                                    securePatient.id,
                                    "create_patient",
                                    "New patient record created successfully",
                                    context
                                )

                                onPatientAdded(securePatient)
                            } catch (e: Exception) {
                                // Handle patient creation error
                                SecurityLogger.logSecurityEvent(
                                    "patient_creation_error",
                                    mapOf(
                                        "error" to (e.message ?: "unknown"),
                                        "user_id" to currentUser.id
                                    ),
                                    context
                                )

                                validationErrors =
                                    mapOf("system" to "Failed to create patient record. Please try again.")
                            }
                        }
                        
                        isProcessing = false
                    }
                },
                enabled = !isProcessing && name.isNotBlank() && idNumber.isNotBlank() && age.isNotBlank()
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Add Patient")
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

// Secure Patient Data Model
data class SecurePatient(
    val id: String,
    val displayName: String, // Sanitized display name
    val age: Int,
    val gender: String,
    val sanitizedSymptoms: String,
    val lastVisit: String,
    val isEmergency: Boolean,
    val dataClassification: HealthcareInputValidator.DataClassification
)

private fun generatePatientId(): String {
    return "PAT_${System.currentTimeMillis()}"
}