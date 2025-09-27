package com.example.medigrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.data.NavigationItem
import com.example.medigrid.ui.screens.LoadSheddingScreen
import com.example.medigrid.ui.theme.*
import com.example.medigrid.security.HealthcareAuthService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawer(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    currentUser: HealthcareAuthService.HealthcareUser? = null,
    onLogout: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(CardBackground)
    ) {
        // Header with logo and user info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = MediBlue,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "MediGrid",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MediBlue
                    )
                }

                // User information
                if (currentUser != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MediBlue.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = currentUser.username,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MediBlue
                            )
                            Text(
                                text = currentUser.role.name,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "${currentUser.role.permissions.size} permissions",
                                fontSize = 10.sp,
                                color = TextSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            color = BorderColor,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Navigation items
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(NavigationItem.values()) { item ->
                NavigationItemComponent(
                    item = item,
                    isSelected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) },
                    currentUser = currentUser
                )
            }
        }

        // Logout button
        if (currentUser != null && onLogout != null) {
            HorizontalDivider(
                color = BorderColor,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Logout",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }

        // App version info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MediGrid v1.0",
                    fontSize = 12.sp,
                    color = TextSecondary.copy(alpha = 0.6f)
                )
                if (currentUser != null) {
                    Text(
                        text = "POPIA Compliant",
                        fontSize = 10.sp,
                        color = SuccessGreen.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationItemComponent(
    item: NavigationItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    currentUser: HealthcareAuthService.HealthcareUser?,
    modifier: Modifier = Modifier,
) {
    // Check if user has permission for security-related items
    val hasAccess = when (item.route) {
        NavigationItem.PATIENTS.route -> {
            currentUser?.role?.permissions?.contains("READ_PHI") ?: false
        }

        NavigationItem.EMERGENCIES.route -> {
            currentUser?.role?.permissions?.contains("EMERGENCY_ACCESS") ?: false
        }

        else -> true
    }

    val backgroundColor = if (isSelected) {
        Brush.horizontalGradient(listOf(MediBlue, MediBlueLight))
    } else {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    val contentColor = when {
        !hasAccess -> TextSecondary.copy(alpha = 0.5f)
        isSelected -> Color.White
        else -> TextSecondary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = hasAccess) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = item.title,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            fontSize = 14.sp
        )

        // Lock icon for restricted access
        if (!hasAccess && currentUser != null) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Restricted",
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}