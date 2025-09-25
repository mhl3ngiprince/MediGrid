package com.example.medigrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.data.StatCard
import com.example.medigrid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatCardComponent(
    statCard: StatCard,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isCompact = screenWidth < 600

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 16.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top gradient line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(MediBlue, MediGreen)
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 16.dp))

            // Main statistic number - responsive sizing
            Text(
                text = statCard.value,
                fontSize = if (isCompact) 24.sp else 32.sp,
                fontWeight = FontWeight.Bold,
                color = MediBlue
            )

            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 8.dp))

            // Label - responsive sizing
            Text(
                text = statCard.title.uppercase(),
                fontSize = if (isCompact) 10.sp else 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))

            // Change indicator - responsive sizing
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (statCard.isPositive) Icons.Filled.Check else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (statCard.isPositive) SuccessGreen else DangerRed,
                    modifier = Modifier.size(if (isCompact) 12.dp else 14.dp)
                )
                Text(
                    text = statCard.change,
                    fontSize = if (isCompact) 10.sp else 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (statCard.isPositive) SuccessGreen else DangerRed
                )
            }
        }
    }
}