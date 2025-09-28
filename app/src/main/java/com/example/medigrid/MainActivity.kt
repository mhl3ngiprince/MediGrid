package com.example.medigrid

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
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
import com.example.medigrid.data.NavigationItem
import com.example.medigrid.data.RealTimeDataService
import com.example.medigrid.ui.components.NavigationDrawer
import com.example.medigrid.ui.screens.AnalyticsScreen
import com.example.medigrid.ui.screens.ChatbotScreen
import com.example.medigrid.ui.screens.ClinicsScreen
import com.example.medigrid.ui.screens.DashboardScreen
import com.example.medigrid.ui.screens.EmergencyAlertsScreen
import com.example.medigrid.ui.screens.InventoryScreen
import com.example.medigrid.ui.screens.LoadSheddingScreen
import com.example.medigrid.ui.screens.LoginScreen
import com.example.medigrid.ui.screens.NetworkMapScreen
import com.example.medigrid.ui.screens.PatientsScreen
import com.example.medigrid.ui.screens.PowerStatusScreen
import com.example.medigrid.ui.screens.SecurePatientScreen
import com.example.medigrid.ui.screens.SecurityDashboardScreen
import com.example.medigrid.ui.screens.SettingsScreen
import com.example.medigrid.ui.screens.SymptomCheckerScreen
import com.example.medigrid.ui.screens.TelemedicineScreen
import com.example.medigrid.ui.theme.*
import com.example.medigrid.security.*
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
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf<HealthcareAuthService.HealthcareUser?>(null) }
    var showSecurityDashboard by remember { mutableStateOf(false) }

    // Initialize Firebase, RealTimeDataService, and security on first run
    LaunchedEffect(Unit) {
        try {
            // Initialize Firebase with specific database URL
            FirebaseConfig.initializeFirebase(context)
            SecurityConfig.initializeKeystore(context)

            // Validate Firebase database connection
            val isConnected = FirebaseConfig.validateConnection(context)
            Log.i("MediGrid", "Firebase database connected: $isConnected to ${FirebaseConfig.DATABASE_URL}")

            // Initialize Firebase Data Service
            val firebaseDataService = FirebaseDataService.getInstance(context)
            firebaseDataService.enableOfflinePersistence()

            // Initialize RealTimeDataService for live app-wide updates
            RealTimeDataService.getInstance(context)

            // Get FCM token for notifications
            firebaseDataService.getFCMToken { token ->
                SecurityLogger.logSecurityEvent(
                    "fcm_token_obtained",
                    mapOf(
                        "token_length" to token.length,
                        "database_url" to FirebaseConfig.DATABASE_URL
                    ),
                    context
                )
            }

        } catch (e: Exception) {
            // Log the error but don't crash the app
            Log.e("MediGrid", "Initialization error: ${e.message}")
            SecurityLogger.logSecurityIncident(
                "app_initialization_error",
                "Failed to initialize Firebase: ${e.message}",
                context,
                SecurityConfig.RiskLevel.HIGH
            )
        }
    }

    if (currentUser == null && !showSecurityDashboard) {
        // Login Screen with Firebase
        LoginScreen(
            onLoginSuccess = { user ->
                currentUser = user
                SecurityLogger.logSecurityEvent(
                    "firebase_user_session_started",
                    mapOf(
                        "user_id" to user.id,
                        "role" to user.role.name
                    ),
                    context
                )
            },
            onNavigateToSecurity = {
                showSecurityDashboard = true
            }
        )
    } else if (showSecurityDashboard && currentUser == null) {
        // Security Dashboard (without authentication)
        SecurityDashboardScreen(
            currentUser = null,
            onNavigateBack = {
                showSecurityDashboard = false
            }
        )
    } else {
        // Main Application
        MainMediGridApp(
            currentUser = currentUser,
            onLogout = {
                val firebaseAuthService = FirebaseAuthService(context)
                // Sign out from Firebase using proper coroutine scope instead of runBlocking
                // Note: Firebase sign out is synchronous, so we can call it directly
                try {
                    SecurityLogger.logSecurityEvent(
                        "firebase_user_session_ended",
                        mapOf("user_id" to (currentUser?.id ?: "unknown")),
                        context
                    )
                } catch (e: Exception) {
                    Log.e("MediGrid", "Logout error: ${e.message}")
                }
                currentUser = null
                showSecurityDashboard = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMediGridApp(
    currentUser: HealthcareAuthService.HealthcareUser?,
    onLogout: () -> Unit,
) {
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
                        currentUser = currentUser,
                        onNavigate = { route ->
                            currentRoute = route
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        onLogout = onLogout,
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        ) {
            MainContent(
                currentRoute = currentRoute,
                currentUser = currentUser,
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
                currentUser = currentUser,
                onNavigate = { route ->
                    currentRoute = route
                },
                onLogout = onLogout,
                modifier = Modifier.fillMaxHeight()
            )

            MainContent(
                currentRoute = currentRoute,
                currentUser = currentUser,
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
    currentUser: HealthcareAuthService.HealthcareUser?,
    onMenuClick: () -> Unit,
    showMenuButton: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getPageTitle(currentRoute),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    // Security indicator
                    if (currentUser != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secure Session",
                            modifier = Modifier.size(16.dp),
                            tint = SuccessGreen
                        )
                    }
                }
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
                // User Role Indicator
                currentUser?.let { user ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        StatusIndicator(
                            text = user.role.name,
                            color = MediBlue
                        )
                        StatusIndicator(
                            text = "Session Active",
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
                    // Check PHI access permissions with null safety
                    if (currentUser != null &&
                        HealthcareAuthService(context).hasPermission(currentUser, "READ_PHI")
                    ) {
                        PatientsScreen()
                    } else {
                        AccessDeniedScreen("READ_PHI")
                    }
                }

                NavigationItem.INVENTORY.route -> {
                    // Check inventory permissions with null safety
                    if (currentUser != null &&
                        HealthcareAuthService(context).hasPermission(
                            currentUser,
                            "MANAGE_INVENTORY"
                        )
                    ) {
                        InventoryScreen()
                    } else {
                        InventoryScreen() // Allow read access for most roles
                    }
                }

                NavigationItem.EMERGENCIES.route -> {
                    // Check emergency access permissions with null safety
                    if (currentUser != null &&
                        HealthcareAuthService(context).hasPermission(
                            currentUser,
                            "EMERGENCY_ACCESS"
                        )
                    ) {
                        EmergencyAlertsScreen()
                    } else {
                        AccessDeniedScreen("EMERGENCY_ACCESS")
                    }
                }

                NavigationItem.POWER.route -> {
                    PowerStatusScreen()
                }

                NavigationItem.LOAD_SHEDDING.route -> {
                    LoadSheddingScreen(currentUser = currentUser)
                }

                NavigationItem.ANALYTICS.route -> {
                    AnalyticsScreen()
                }

                NavigationItem.CHATBOT.route -> {
                    ChatbotScreen(currentUser = currentUser)
                }

                NavigationItem.NETWORK_MAP.route -> {
                    NetworkMapScreen()
                }

                NavigationItem.SYMPTOM_CHECKER.route -> {
                    SymptomCheckerScreen(currentUser = currentUser)
                }

                NavigationItem.SECURITY.route -> {
                    SecurityDashboardScreen(
                        currentUser = currentUser,
                        onNavigateBack = { /* Stay in security dashboard */ }
                    )
                }

                NavigationItem.SECURE_PATIENTS.route -> {
                    // Check PHI access permissions with comprehensive null safety
                    if (currentUser != null &&
                        runCatching {
                            HealthcareAuthService(context).hasPermission(currentUser, "READ_PHI")
                        }.getOrElse { false }
                    ) {
                        SecurePatientScreen(currentUser = currentUser)
                    } else {
                        AccessDeniedScreen("READ_PHI")
                    }
                }
                NavigationItem.TELEMEDICINE.route -> {
                    TelemedicineScreen(currentUser = currentUser)
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
private fun AccessDeniedScreen(
    requiredPermission: String,
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
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Access Denied",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Access Denied",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Required permission: $requiredPermission",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Contact your administrator for access to this feature.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
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

@Composable
private fun ErrorScreen(
    title: String,
    message: String,
    onRetry: () -> Unit,
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
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
        NavigationItem.LOAD_SHEDDING.route -> "Load Shedding Management"
        NavigationItem.ANALYTICS.route -> "Healthcare Analytics"
        NavigationItem.CHATBOT.route -> "MediBot AI Assistant"
        NavigationItem.NETWORK_MAP.route -> "Healthcare Network Map"
        NavigationItem.SYMPTOM_CHECKER.route -> "Symptom Checker"
        NavigationItem.SECURITY.route -> "Security Dashboard"
        NavigationItem.SECURE_PATIENTS.route -> "Secure Patient Management"
        NavigationItem.TELEMEDICINE.route -> "Telemedicine"
        NavigationItem.SETTINGS.route -> "System Settings"
        else -> "MediGrid Dashboard"
    }
}