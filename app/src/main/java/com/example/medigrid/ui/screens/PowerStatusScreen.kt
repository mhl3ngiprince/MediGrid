package com.example.medigrid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.data.StatCard
import com.example.medigrid.ui.components.StatCardComponent
import com.example.medigrid.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerStatusScreen(
    modifier: Modifier = Modifier,
) {
    var isRefreshing by remember { mutableStateOf(false) }
    var lastRefreshed by remember { mutableStateOf("Never") }
    var scheduleData by remember { mutableStateOf(getLoadSheddingSchedule()) }
    var batteryData by remember { mutableStateOf(getBatteryStatus()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Power Statistics
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(280.dp)
            ) {
                items(getPowerStats()) { stat ->
                    StatCardComponent(statCard = stat)
                }
            }
        }

        // Load-shedding Schedule Section
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
                        Column {
                            Text(
                                text = "Load-shedding Schedule",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Last updated: $lastRefreshed",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = {
                                isRefreshing = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MediBlue
                            ),
                            enabled = !isRefreshing
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Refresh Schedule"
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRefreshing) "Updating..." else "Refresh Schedule")
                        }
                    }

                    // Refresh effect
                    LaunchedEffect(isRefreshing) {
                        if (isRefreshing) {
                            delay(3000) // Simulate API call
                            // Update schedule data
                            scheduleData = getUpdatedLoadSheddingSchedule()
                            batteryData = getUpdatedBatteryStatus()
                            lastRefreshed =
                                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                    .format(java.util.Date())
                            isRefreshing = false
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Schedule Items
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        scheduleData.forEach { scheduleItem ->
                            LoadSheddingItem(item = scheduleItem)
                        }
                    }
                }
            }
        }

        // Battery Status Section
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Battery Status",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        if (isRefreshing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Refreshing...",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Battery levels for different clinics
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        batteryData.forEach { batteryItem ->
                            BatteryStatusItem(item = batteryItem)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadSheddingItem(
    item: LoadSheddingScheduleItem,
    modifier: Modifier = Modifier,
) {
    val statusColor = when (item.status) {
        "Active" -> DangerRed
        "Upcoming" -> WarningOrange
        "Scheduled" -> MediBlue
        else -> TextSecondary
    }

    val statusBackground = when (item.status) {
        "Active" -> DangerRed.copy(alpha = 0.1f)
        "Upcoming" -> WarningOrange.copy(alpha = 0.1f)
        "Scheduled" -> MediBlue.copy(alpha = 0.1f)
        else -> TextSecondary.copy(alpha = 0.1f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = statusBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.timeSlot,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Stage ${item.stage}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Column {
                Text(
                    text = "Affected Areas",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.affectedAreas,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.2f))
            ) {
                Text(
                    text = item.status,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun BatteryStatusItem(
    item: BatteryStatusItem,
    modifier: Modifier = Modifier,
) {
    val batteryColor = when {
        item.percentage > 80 -> SuccessGreen
        item.percentage > 40 -> WarningOrange
        else -> DangerRed
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MediBlue.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = batteryColor,
                        shape = CircleShape
                    )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.clinicName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Battery Level: ${item.percentage}%",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                if (item.timeRemaining.isNotEmpty()) {
                    Text(
                        text = "Time remaining: ${item.timeRemaining}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Battery percentage
            Text(
                text = "${item.percentage}%",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = batteryColor
            )
        }
    }
}

data class LoadSheddingScheduleItem(
    val timeSlot: String,
    val stage: Int,
    val affectedAreas: String,
    val status: String,
)

data class BatteryStatusItem(
    val clinicName: String,
    val percentage: Int,
    val timeRemaining: String,
)

private fun getPowerStats() = listOf(
    StatCard("Grid Connected", "25", "Stable Power", true, Icons.Filled.Check),
    StatCard("Backup Power", "3", "Load-shedding Active", false, Icons.Filled.Warning),
    StatCard("Power Outage", "1", "Critical", false, Icons.Filled.Warning),
    StatCard("Network Uptime", "92%", "Above Target", true, Icons.Filled.Check)
)

private fun getLoadSheddingSchedule() = listOf(
    LoadSheddingScheduleItem("14:00 - 16:00", 4, "Alexandra, Orange Farm", "Active"),
    LoadSheddingScheduleItem("18:00 - 20:00", 6, "Soweto, Midrand", "Upcoming"),
    LoadSheddingScheduleItem("22:00 - 00:00", 2, "Johannesburg CBD", "Scheduled")
)

private fun getUpdatedLoadSheddingSchedule() = listOf(
    LoadSheddingScheduleItem("14:00 - 16:00", 4, "Alexandra, Orange Farm", "Active"),
    LoadSheddingScheduleItem("18:00 - 20:00", 6, "Soweto, Midrand", "Upcoming"),
    LoadSheddingScheduleItem("22:00 - 00:00", 2, "Johannesburg CBD", "Scheduled")
)

private fun getBatteryStatus() = listOf(
    BatteryStatusItem("Soweto Community", 95, "12 hours"),
    BatteryStatusItem("Alexandra Primary", 87, "8 hours"),
    BatteryStatusItem("Orange Farm", 23, "2 hours"),
    BatteryStatusItem("Midrand Medical", 91, "10 hours"),
    BatteryStatusItem("Sandton Clinic", 88, "9 hours")
)

private fun getUpdatedBatteryStatus() = listOf(
    BatteryStatusItem("Soweto Community", 95, "12 hours"),
    BatteryStatusItem("Alexandra Primary", 87, "8 hours"),
    BatteryStatusItem("Orange Farm", 23, "2 hours"),
    BatteryStatusItem("Midrand Medical", 91, "10 hours"),
    BatteryStatusItem("Sandton Clinic", 88, "9 hours")
)