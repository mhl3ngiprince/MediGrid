package com.example.medigrid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.data.Clinic
import com.example.medigrid.data.ClinicStatus
import com.example.medigrid.data.SampleData
import com.example.medigrid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicsScreen(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
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
                    text = "Healthcare Network Overview",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* Add clinic */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MediBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Clinic"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Clinic")
                    }
                    OutlinedButton(
                        onClick = { /* Refresh */ },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MediBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Refresh")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Clinics List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(SampleData.clinics) { clinic ->
                    ClinicItem(clinic = clinic)
                }
            }
        }
    }
}

@Composable
private fun ClinicItem(
    clinic: Clinic,
    modifier: Modifier = Modifier,
) {
    val statusColor = when (clinic.status) {
        ClinicStatus.ONLINE -> SuccessGreen
        ClinicStatus.BACKUP -> WarningOrange
        ClinicStatus.OFFLINE -> DangerRed
    }

    val statusText = when (clinic.status) {
        ClinicStatus.ONLINE -> "Online"
        ClinicStatus.BACKUP -> "Backup Power"
        ClinicStatus.OFFLINE -> "Offline"
    }

    val statusBackground = when (clinic.status) {
        ClinicStatus.ONLINE -> SuccessGreen.copy(alpha = 0.1f)
        ClinicStatus.BACKUP -> WarningOrange.copy(alpha = 0.1f)
        ClinicStatus.OFFLINE -> DangerRed.copy(alpha = 0.1f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MediBlue.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status indicator circle
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = statusColor,
                        shape = CircleShape
                    )
            )

            // Clinic information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = clinic.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${clinic.patientsToday} patients today • Staff: ${clinic.staffCount} • Power: ${clinic.powerStatus}",
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
                    text = statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}