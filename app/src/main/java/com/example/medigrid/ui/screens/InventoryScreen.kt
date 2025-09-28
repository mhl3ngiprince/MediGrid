package com.example.medigrid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.medigrid.data.RealTimeDataService
import com.example.medigrid.data.formatTimestamp
import com.example.medigrid.security.SecurityLogger
import com.example.medigrid.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataManager = remember { DataManager.getInstance(context) }
    val realTimeService = remember { RealTimeDataService.getInstance(context) }
    val configuration = LocalConfiguration.current
    
    // Real-time data integration
    val systemStats by realTimeService.systemStats.collectAsState()
    val inventoryUpdates by realTimeService.inventoryUpdates.collectAsState()
    val healthMetrics by realTimeService.healthMetrics.collectAsState()
    val emergencyAlerts by realTimeService.emergencyAlerts.collectAsState()
    val clinicStatus by realTimeService.clinicStatus.collectAsState()
    
    // Local inventory data
    var medicines by remember { mutableStateOf(emptyList<Medicine>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var lastUpdateTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Auto-refresh inventory data every 20 seconds
    LaunchedEffect(Unit) {
        while (true) {
            medicines = dataManager.getMedicines()
            lastUpdateTime = System.currentTimeMillis()
            delay(20000) // 20 seconds
        }
    }
    
    // Initial data load
    LaunchedEffect(Unit) {
        medicines = dataManager.getMedicines()
    }
    
    // Refresh function with real-time logging
    fun refreshData() {
        isRefreshing = true
        medicines = dataManager.getMedicines()
        lastUpdateTime = System.currentTimeMillis()
        
        try {
            SecurityLogger.logSecurityEvent(
                "inventory_data_refreshed",
                mapOf(
                    "medicines_count" to medicines.size.toString(),
                    "critical_items" to inventoryUpdates.count { it.status == "CRITICAL" }.toString(),
                    "system_load" to systemStats.cpuUsage.toString()
                ),
                context
            )
        } catch (e: Exception) {
            // Handle logging error gracefully
        }
        
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
        // Enhanced Header with Real-Time Data
        item {
            ResponsiveInventoryHeader(
                title = "Medicine Inventory",
                subtitle = "${filteredMedicines.size} items • ${systemStats.activeUsers} users active",
                onRefresh = { refreshData() },
                onAdd = { showAddDialog = true },
                isRefreshing = isRefreshing,
                isTablet = isTablet,
                systemLoad = systemStats.cpuUsage,
                lastUpdate = lastUpdateTime,
                medicineStockLevel = healthMetrics.medicineStockLevel
            )
        }

        // Real-Time Inventory Alerts
        if (inventoryUpdates.any { it.status == "CRITICAL" || it.status == "LOW" }) {
            item {
                InventoryAlertsCard(
                    inventoryUpdates = inventoryUpdates,
                    isTablet = isTablet
                )
            }
        }

        // Live System Status for Inventory
        item {
            InventorySystemStatusCard(
                systemStats = systemStats,
                healthMetrics = healthMetrics,
                clinicStatus = clinicStatus,
                isTablet = isTablet
            )
        }

        // Enhanced search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Medicine Inventory") },
                placeholder = { Text("Medicine name, category, or location...") },
                supportingText = { 
                    Text(
                        text = if (searchQuery.isNotBlank()) 
                            "Found ${filteredMedicines.size} medicine${if (filteredMedicines.size == 1) "" else "s"} matching \"$searchQuery\""
                        else "Search by medicine name, category, or storage location ↓",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Green, CircleShape)
                        )
                        Text(
                            text = "LIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { /* Handle search action */ }
                )
            )
        }

        // Enhanced stats with real-time data
        item {
            ResponsiveInventoryStats(
                medicines = medicines,
                inventoryUpdates = inventoryUpdates,
                healthMetrics = healthMetrics,
                isTablet = isTablet
            )
        }

        // Real-Time Inventory Updates Feed
        item {
            LiveInventoryUpdatesCard(
                inventoryUpdates = inventoryUpdates,
                isTablet = isTablet
            )
        }

        // Medicine grid with live indicators
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
                            isTablet = isTablet,
                            hasLiveUpdate = inventoryUpdates.any { 
                                it.itemName.contains(filteredMedicines[index].name, ignoreCase = true)
                            },
                            liveQuantity = inventoryUpdates.find { 
                                it.itemName.contains(filteredMedicines[index].name, ignoreCase = true)
                            }?.quantity
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
private fun InventoryAlertsCard(
    inventoryUpdates: List<com.example.medigrid.data.InventoryUpdate>,
    isTablet: Boolean
) {
    val criticalItems = inventoryUpdates.filter { it.status == "CRITICAL" }
    val lowItems = inventoryUpdates.filter { it.status == "LOW" }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Inventory Alerts",
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Inventory Alerts (${criticalItems.size + lowItems.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Red, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            (criticalItems + lowItems).take(if (isTablet) 4 else 3).forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (item.status == "CRITICAL") Icons.Default.Emergency else Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (item.status == "CRITICAL") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${item.itemName} - ${item.status}",
                            fontSize = if (isTablet) 13.sp else 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Quantity: ${item.quantity} • Updated ${formatTimestamp(item.lastUpdated)}",
                            fontSize = if (isTablet) 11.sp else 9.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventorySystemStatusCard(
    systemStats: com.example.medigrid.data.SystemStats,
    healthMetrics: com.example.medigrid.data.HealthMetrics,
    clinicStatus: Map<String, com.example.medigrid.data.RealTimeClinicStatus>,
    isTablet: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = "System Status",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Live Inventory System",
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Green, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InventoryStatItem("Stock Level", "${healthMetrics.medicineStockLevel}%")
                    InventoryStatItem("Active Users", systemStats.activeUsers.toString())
                    InventoryStatItem("Network Load", "${systemStats.cpuUsage}%")
                    InventoryStatItem("Facilities", clinicStatus.size.toString())
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { InventoryStatItem("Stock", "${healthMetrics.medicineStockLevel}%") }
                    item { InventoryStatItem("Users", systemStats.activeUsers.toString()) }
                    item { InventoryStatItem("Load", "${systemStats.cpuUsage}%") }
                    item { InventoryStatItem("Sites", clinicStatus.size.toString()) }
                }
            }
        }
    }
}

@Composable
private fun InventoryStatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun LiveInventoryUpdatesCard(
    inventoryUpdates: List<com.example.medigrid.data.InventoryUpdate>,
    isTablet: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Update,
                    contentDescription = "Live Updates",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Live Inventory Updates",
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Green, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            inventoryUpdates.take(if (isTablet) 5 else 3).forEach { update ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = when (update.status) {
                            "CRITICAL" -> Icons.Default.Error
                            "LOW" -> Icons.Default.Warning
                            "NORMAL" -> Icons.Default.CheckCircle
                            else -> Icons.Default.Inventory
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = when (update.status) {
                            "CRITICAL" -> MaterialTheme.colorScheme.error
                            "LOW" -> MaterialTheme.colorScheme.secondary
                            "NORMAL" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${update.itemName} - Qty: ${update.quantity}",
                            fontSize = if (isTablet) 13.sp else 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${update.status} • ${formatTimestamp(update.lastUpdated)}",
                            fontSize = if (isTablet) 11.sp else 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResponsiveInventoryHeader(
    title: String,
    subtitle: String,
    onRefresh: () -> Unit,
    onAdd: () -> Unit,
    isRefreshing: Boolean,
    isTablet: Boolean,
    systemLoad: Int = 0,
    lastUpdate: Long,
    medicineStockLevel: Int
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
                Text(
                    text = "Stock Level: $medicineStockLevel% • Load: $systemLoad% • Updated ${formatTimestamp(lastUpdate)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
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
                
                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }
            
            Text(
                text = "Stock: $medicineStockLevel% • Load: $systemLoad% • Updated ${formatTimestamp(lastUpdate)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
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
    inventoryUpdates: List<com.example.medigrid.data.InventoryUpdate>,
    healthMetrics: com.example.medigrid.data.HealthMetrics,
    isTablet: Boolean
) {
    val lowStock = medicines.count { it.status.contains("Low", ignoreCase = true) }
    val expiringSoon = medicines.count { it.status.contains("Expiring", ignoreCase = true) }
    val goodStock = medicines.count { it.status.contains("Good", ignoreCase = true) }
    val criticalItems = inventoryUpdates.count { it.status == "CRITICAL" }
    
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
                StatItem("Critical", criticalItems.toString(), Icons.Default.Error)
                StatItem("Stock Level", "${healthMetrics.medicineStockLevel}%", Icons.Default.Inventory)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(140.dp).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { StatItem("Total", medicines.size.toString(), Icons.Default.ShoppingCart) }
                item { StatItem("Good", goodStock.toString(), Icons.Default.CheckCircle) }
                item { StatItem("Low", lowStock.toString(), Icons.Default.Warning) }
                item { StatItem("Critical", criticalItems.toString(), Icons.Default.Error) }
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
    isTablet: Boolean,
    hasLiveUpdate: Boolean,
    liveQuantity: Int?
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
                        text = if (hasLiveUpdate && liveQuantity != null) liveQuantity.toString() else medicine.stockLevel,
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

                // Live update indicator
                if (hasLiveUpdate) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Green, CircleShape)
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

                if (hasLiveUpdate) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
    var errorMessage by remember { mutableStateOf("") }

    // Validation helpers
    val isNameValid = name.trim().length >= 2
    val isCategoryValid = category.trim().isNotBlank()
    val isStockValid = stockLevel.toIntOrNull()?.let { it >= 0 } == true
    val isLocationValid = location.trim().isNotBlank()
    val isExpiryValid = expiryDate.trim().isNotBlank()
    val isFormValid =
        isNameValid && isCategoryValid && isStockValid && isLocationValid && isExpiryValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Medicine",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Add New Medicine")
            }
        },
        text = {
            Column(
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
                    onValueChange = {
                        name = it
                        errorMessage = ""
                    },
                    label = { Text("Medicine Name") },
                    placeholder = { Text("e.g., Paracetamol 500mg") },
                    supportingText = {
                        Text(
                            text = if (name.isNotBlank()) {
                                if (isNameValid) "✓ Valid medicine name" else "⚠ Name too short (min 2 characters)"
                            } else "Enter the medicine name and strength ↓",
                            color = if (name.isNotBlank()) {
                                if (isNameValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            } else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = "Medicine",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (isNameValid) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid name",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words
                    ),
                    isError = name.isNotBlank() && !isNameValid
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                        errorMessage = ""
                    },
                    label = { Text("Medicine Category") },
                    placeholder = { Text("e.g., Analgesic, Antibiotic") },
                    supportingText = {
                        Text(
                            text = if (category.isNotBlank()) {
                                if (isCategoryValid) "✓ Valid category" else "⚠ Category required"
                            } else "Select or enter medicine category ↓",
                            color = if (category.isNotBlank()) {
                                if (isCategoryValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            } else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Category",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (isCategoryValid) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid category",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words
                    ),
                    isError = category.isNotBlank() && !isCategoryValid
                )

                OutlinedTextField(
                    value = stockLevel,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } && it.length <= 6) {
                            stockLevel = it
                            errorMessage = ""
                        }
                    },
                    label = { Text("Stock Quantity") },
                    placeholder = { Text("100") },
                    supportingText = {
                        Text(
                            text = if (stockLevel.isNotBlank()) {
                                if (isStockValid) "✓ Valid quantity" else "⚠ Invalid quantity"
                            } else "Enter current stock quantity ↓",
                            color = if (stockLevel.isNotBlank()) {
                                if (isStockValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            } else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = "Stock",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (isStockValid) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid stock",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                    ),
                    isError = stockLevel.isNotBlank() && !isStockValid
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = {
                        location = it
                        errorMessage = ""
                    },
                    label = { Text("Storage Location") },
                    placeholder = { Text("e.g., Shelf A1, Cold Storage") },
                    supportingText = {
                        Text(
                            text = if (location.isNotBlank()) {
                                if (isLocationValid) "✓ Valid location" else "⚠ Location required"
                            } else "Enter storage location/shelf ↓",
                            color = if (location.isNotBlank()) {
                                if (isLocationValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            } else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (isLocationValid) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid location",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words
                    ),
                    isError = location.isNotBlank() && !isLocationValid
                )

                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = {
                        expiryDate = it
                        errorMessage = ""
                    },
                    label = { Text("Expiry Date") },
                    placeholder = { Text("Dec 2025 or 12/2025") },
                    supportingText = {
                        Text(
                            text = if (expiryDate.isNotBlank()) {
                                if (isExpiryValid) "✓ Valid expiry date" else "⚠ Date required"
                            } else "Enter expiry month/year ↓",
                            color = if (expiryDate.isNotBlank()) {
                                if (isExpiryValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            } else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Expiry Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (isExpiryValid) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid expiry",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    isError = expiryDate.isNotBlank() && !isExpiryValid
                )

                // Summary card
                if (isFormValid) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "✓ Ready to add medicine",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "$name - Qty: $stockLevel - Location: $location",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid) {
                        try {
                            val newMedicine = Medicine(
                                name = name.trim(),
                                category = category.trim(),
                                stockLevel = stockLevel,
                                location = location.trim(),
                                expiryDate = expiryDate.trim(),
                                status = "Good Stock"
                            )
                            onAddMedicine(newMedicine)
                        } catch (e: Exception) {
                            errorMessage = "Failed to add medicine. Please check all fields."
                        }
                    } else {
                        errorMessage = "Please fill in all required fields correctly."
                    }
                },
                enabled = isFormValid
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
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