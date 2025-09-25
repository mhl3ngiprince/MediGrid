package com.example.medigrid

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medigrid.data.NavigationItem
import com.example.medigrid.ui.components.NavigationDrawer
import com.example.medigrid.ui.screens.*
import com.example.medigrid.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediGridTheme {
                MediGridApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediGridApp() {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp.dp

    // Use drawer for smaller screens or portrait mode
    val useDrawer = screenWidth < 840.dp || !isLandscape

    var currentRoute by remember { mutableStateOf(NavigationItem.DASHBOARD.route) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (useDrawer) {
        // Mobile/Portrait Layout with Navigation Drawer
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(280.dp),
                    drawerContainerColor = CardBackground
                ) {
                    NavigationDrawer(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            currentRoute = route
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        ) {
            MainContent(
                currentRoute = currentRoute,
                onMenuClick = {
                    scope.launch {
                        drawerState.open()
                    }
                },
                showMenuButton = true
            )
        }
    } else {
        // Desktop/Landscape Layout with Permanent Sidebar
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
        ) {
            // Permanent Navigation Sidebar
            NavigationDrawer(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    currentRoute = route
                },
                modifier = Modifier.fillMaxHeight()
            )

            MainContent(
                currentRoute = currentRoute,
                onMenuClick = { },
                showMenuButton = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    currentRoute: String,
    onMenuClick: () -> Unit,
    showMenuButton: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = getPageTitle(currentRoute),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            },
            navigationIcon = {
                if (showMenuButton) {
                    IconButton(
                        onClick = onMenuClick
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Open Menu",
                            tint = MediBlue
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = CardBackground
            ),
            actions = {
                // Status indicators - responsive sizing
                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                if (isLandscape || configuration.screenWidthDp > 600) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        StatusIndicator(
                            text = "Network Online",
                            color = SuccessGreen
                        )
                        StatusIndicator(
                            text = "3 Clinics on Backup",
                            color = WarningOrange
                        )
                    }
                } else {
                    // Compact status for small screens
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        StatusIndicator(
                            text = "Online",
                            color = SuccessGreen
                        )
                    }
                }
            }
        )

        // Content based on current route
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentRoute) {
                NavigationItem.DASHBOARD.route -> {
                    DashboardScreen()
                }
                NavigationItem.CLINICS.route -> {
                    ClinicsScreen()
                }

                NavigationItem.PATIENTS.route -> {
                    PatientsScreen()
                }

                NavigationItem.INVENTORY.route -> {
                    InventoryScreen()
                }

                NavigationItem.EMERGENCIES.route -> {
                    EmergencyAlertsScreen()
                }

                NavigationItem.POWER.route -> {
                    PowerStatusScreen()
                }

                NavigationItem.ANALYTICS.route -> {
                    AnalyticsScreen()
                }

                NavigationItem.SETTINGS.route -> {
                    SettingsScreen()
                }

                else -> {
                    PlaceholderScreen(getPageTitle(currentRoute))
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = color,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MediBlue
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Coming Soon",
                    fontSize = 16.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

private fun getPageTitle(route: String): String {
    return when (route) {
        NavigationItem.DASHBOARD.route -> "Healthcare Network Dashboard"
        NavigationItem.CLINICS.route -> "Clinic Network Management"
        NavigationItem.PATIENTS.route -> "Patient Management System"
        NavigationItem.INVENTORY.route -> "Medicine Inventory Control"
        NavigationItem.EMERGENCIES.route -> "Emergency Alert Center"
        NavigationItem.POWER.route -> "Power Status Monitor"
        NavigationItem.ANALYTICS.route -> "Healthcare Analytics"
        NavigationItem.SETTINGS.route -> "System Settings"
        else -> "MediGrid Dashboard"
    }
}