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
import androidx.compose.runtime.Composable
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
                                onClick = { /* Add medicine */ },
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