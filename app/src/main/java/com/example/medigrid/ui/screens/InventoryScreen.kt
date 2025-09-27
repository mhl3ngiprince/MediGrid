package com.example.medigrid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.data.DataManager
import com.example.medigrid.data.Medicine
import com.example.medigrid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataManager = remember { DataManager.getInstance(context) }
    val configuration = LocalConfiguration.current
    
    // Real-time data
    var medicines by remember { mutableStateOf(emptyList<Medicine>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Load real data
    LaunchedEffect(Unit) {
        medicines = dataManager.getMedicines()
    }
    
    // Refresh function
    fun refreshData() {
        isRefreshing = true
        medicines = dataManager.getMedicines()
        isRefreshing = false
    }
    
    // Filter medicines based on search query
    val filteredMedicines = if (searchQuery.isBlank()) {
        medicines
    } else {
        medicines.filter { 
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true) ||
            it.location.contains(searchQuery, ignoreCase = true)
        }
    }
    
    // Responsive layout
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val gridColumns = if (isTablet) 2 else 1

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            ResponsiveInventoryHeader(
                title = "Medicine Inventory",
                subtitle = "${filteredMedicines.size} items available",
                onRefresh = { refreshData() },
                onAdd = { showAddDialog = true },
                isRefreshing = isRefreshing,
                isTablet = isTablet
            )
        }

        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search medicines...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Quick stats
        item {
            ResponsiveInventoryStats(
                medicines = medicines,
                isTablet = isTablet
            )
        }

        // Medicine grid
        if (filteredMedicines.isNotEmpty()) {
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier.height(400.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMedicines.size) { index ->
                        ResponsiveMedicineCard(
                            medicine = filteredMedicines[index],
                            isTablet = isTablet
                        )
                    }
                }
            }
        } else {
            item {
                EmptyInventoryCard(isTablet = isTablet)
            }
        }
    }

    // Add medicine dialog
    if (showAddDialog) {
        AddMedicineDialog(
            onDismiss = { showAddDialog = false },
            onAddMedicine = { newMedicine ->
                dataManager.addMedicine(newMedicine)
                medicines = dataManager.getMedicines()
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ResponsiveInventoryHeader(
    title: String,
    subtitle: String,
    onRefresh: () -> Unit,
    onAdd: () -> Unit,
    isRefreshing: Boolean,
    isTablet: Boolean
) {
    if (isTablet) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = subtitle,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh")
                }
                Button(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Medicine")
                }
            }
        }
    } else {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Medicine")
            }
        }
    }
}

@Composable
private fun ResponsiveInventoryStats(
    medicines: List<Medicine>,
    isTablet: Boolean
) {
    val lowStock = medicines.count { it.status.contains("Low", ignoreCase = true) }
    val expiringSoon = medicines.count { it.status.contains("Expiring", ignoreCase = true) }
    val goodStock = medicines.count { it.status.contains("Good", ignoreCase = true) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        if (isTablet) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Total Items", medicines.size.toString(), Icons.Default.ShoppingCart)
                StatItem("Good Stock", goodStock.toString(), Icons.Default.CheckCircle)
                StatItem("Low Stock", lowStock.toString(), Icons.Default.Warning)
                StatItem("Expiring Soon", expiringSoon.toString(), Icons.Default.Schedule)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(120.dp).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { StatItem("Total", medicines.size.toString(), Icons.Default.ShoppingCart) }
                item { StatItem("Good", goodStock.toString(), Icons.Default.CheckCircle) }
                item { StatItem("Low", lowStock.toString(), Icons.Default.Warning) }
                item { StatItem("Expiring", expiringSoon.toString(), Icons.Default.Schedule) }
            }
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResponsiveMedicineCard(
    medicine: Medicine,
    isTablet: Boolean
) {
    val statusColor = when {
        medicine.status.contains("Low", ignoreCase = true) -> MaterialTheme.colorScheme.error
        medicine.status.contains("Expiring", ignoreCase = true) -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isTablet) Modifier.height(180.dp) else Modifier.height(160.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = medicine.name,
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = medicine.status,
                        fontSize = if (isTablet) 10.sp else 8.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = medicine.category,
                fontSize = if (isTablet) 14.sp else 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Stock Level",
                        fontSize = if (isTablet) 10.sp else 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = medicine.stockLevel,
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text(
                        text = "Location",
                        fontSize = if (isTablet) 10.sp else 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = medicine.location,
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Expires: ${medicine.expiryDate}",
                    fontSize = if (isTablet) 12.sp else 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyInventoryCard(isTablet: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 48.dp else 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(if (isTablet) 64.dp else 48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Medicines Found",
                    fontSize = if (isTablet) 20.sp else 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Add medicines to track inventory",
                    fontSize = if (isTablet) 14.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddMedicineDialog(
    onDismiss: () -> Unit,
    onAddMedicine: (Medicine) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var stockLevel by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Medicine") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medicine Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = stockLevel,
                    onValueChange = { stockLevel = it },
                    label = { Text("Stock Level") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Expiry Date") },
                    placeholder = { Text("Dec 2025") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && category.isNotBlank() && stockLevel.isNotBlank() && 
                        location.isNotBlank() && expiryDate.isNotBlank()) {
                        val newMedicine = Medicine(
                            name = name,
                            category = category,
                            stockLevel = stockLevel,
                            location = location,
                            expiryDate = expiryDate,
                            status = "Good Stock"
                        )
                        onAddMedicine(newMedicine)
                    }
                }
            ) {
                Text("Add Medicine")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}