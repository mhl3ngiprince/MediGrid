package com.example.medigrid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.data.Medicine
import com.example.medigrid.data.SampleData
import com.example.medigrid.data.StatCard
import com.example.medigrid.ui.components.StatCardComponent
import com.example.medigrid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    modifier: Modifier = Modifier
) {
    var showAddMedicineDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Inventory Statistics
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(280.dp)
            ) {
                items(getInventoryStats()) { stat ->
                    StatCardComponent(statCard = stat)
                }
            }
        }

        // Medicine List Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Medicine Inventory Management",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showAddMedicineDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MediBlue
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add Medicine"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Medicine")
                            }
                            OutlinedButton(
                                onClick = { /* Request delivery */ },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MediBlue
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Send,
                                    contentDescription = "Request Delivery"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Request Delivery")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Medicine Items
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SampleData.medicines.forEach { medicine ->
                            MedicineItem(medicine = medicine)
                        }
                    }
                }
            }
        }
    }

    // Add Medicine Dialog
    if (showAddMedicineDialog) {
        AddMedicineDialog(
            onDismiss = { showAddMedicineDialog = false },
            onMedicineAdded = { medicine ->
                showAddMedicineDialog = false
                // In a real app, this would add to database/API
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineDialog(
    onDismiss: () -> Unit,
    onMedicineAdded: (Medicine) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var stockLevel by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("Good Stock") }
    var batchNumber by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val statusOptions = listOf("Good Stock", "Low Stock", "Expiring Soon", "Out of Stock")
    val categoryOptions = listOf("Antibiotics", "Pain Relief", "Vitamins", "Chronic Medication", "Emergency", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Medicine",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Medicine")
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medicine Name") },
                    placeholder = { Text("Paracetamol 500mg") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Category Selection
                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categoryOptions.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockLevel,
                        onValueChange = { stockLevel = it },
                        label = { Text("Stock Level") },
                        placeholder = { Text("250 units") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        placeholder = { Text("A-12") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = { Text("Expiry Date") },
                        placeholder = { Text("2025-12-31") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = batchNumber,
                        onValueChange = { batchNumber = it },
                        label = { Text("Batch #") },
                        placeholder = { Text("B001234") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text("Supplier") },
                    placeholder = { Text("PharmaCorp Ltd") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Status Selection
                var statusExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedStatus,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Stock Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        statusOptions.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    selectedStatus = status
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "Medicine inventory is tracked and monitored for expiry dates and stock levels.",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || category.isBlank() || stockLevel.isBlank() || location.isBlank()) {
                        errorMessage = "Please fill in all required fields"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = ""

                    // Create new medicine
                    val newMedicine = Medicine(
                        name = name,
                        category = category,
                        stockLevel = stockLevel,
                        location = location,
                        expiryDate = expiryDate.ifBlank { "2025-12-31" },
                        status = selectedStatus
                    )

                    onMedicineAdded(newMedicine)
                    isLoading = false
                },
                enabled = !isLoading && name.isNotBlank() && category.isNotBlank() && stockLevel.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Add Medicine")
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

@Composable
private fun MedicineItem(
    medicine: Medicine,
    modifier: Modifier = Modifier
) {
    val statusColor = when (medicine.status) {
        "Good Stock" -> SuccessGreen
        "Low Stock" -> DangerRed
        "Expiring Soon" -> WarningOrange
        else -> TextSecondary
    }

    val statusBackground = when (medicine.status) {
        "Good Stock" -> SuccessGreen.copy(alpha = 0.1f)
        "Low Stock" -> DangerRed.copy(alpha = 0.1f)
        "Expiring Soon" -> WarningOrange.copy(alpha = 0.1f)
        else -> TextSecondary.copy(alpha = 0.1f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MediBlue.copy(alpha = 0.03f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = medicine.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Category: ${medicine.category}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // Status badge
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = statusBackground)
                ) {
                    Text(
                        text = medicine.status,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Stock Level",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = medicine.stockLevel,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text(
                        text = "Location",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = medicine.location,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text(
                        text = "Expiry Date",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = medicine.expiryDate,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun getInventoryStats() = listOf(
    StatCard("Total Items", "1,247", "Well Stocked", true, Icons.Filled.Add),
    StatCard("Low Stock Items", "23", "Needs Attention", false, Icons.Filled.Warning),
    StatCard("Expiring Soon", "7", "Within 30 days", false, Icons.Filled.Warning),
    StatCard("Total Value", "R 2.4M", "Optimized Distribution", true, Icons.Filled.Check)
)