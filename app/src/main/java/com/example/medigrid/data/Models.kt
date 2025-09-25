package com.example.medigrid.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class ClinicStatus {
    ONLINE, BACKUP, OFFLINE
}

enum class AlertLevel {
    URGENT, WARNING, INFO
}

data class Clinic(
    val id: String,
    val name: String,
    val patientsToday: Int,
    val staffCount: Int,
    val powerStatus: String,
    val status: ClinicStatus
)

data class Patient(
    val id: String,
    val name: String,
    val age: Int,
    val clinic: String,
    val lastVisit: String,
    val status: String
)

data class Alert(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val time: String,
    val level: AlertLevel,
    val isActive: Boolean = true
)

data class StatCard(
    val title: String,
    val value: String,
    val change: String,
    val isPositive: Boolean,
    val icon: ImageVector
)

data class Medicine(
    val name: String,
    val category: String,
    val stockLevel: String,
    val location: String,
    val expiryDate: String,
    val status: String
)

enum class NavigationItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Filled.Home),
    CLINICS("clinics", "Clinic Network", Icons.Filled.LocationOn),
    PATIENTS("patients", "Patient Management", Icons.Filled.Person),
    INVENTORY("inventory", "Medicine Inventory", Icons.Filled.List),
    EMERGENCIES("emergencies", "Emergency Alerts", Icons.Filled.Warning),
    POWER("power", "Power Status", Icons.Filled.Star),
    ANALYTICS("analytics", "Analytics", Icons.Filled.Info),
    SETTINGS("settings", "Settings", Icons.Filled.Settings)
}

// Sample data
object SampleData {
    val stats = listOf(
        StatCard("Active Clinics", "28", "+2 since yesterday", true, Icons.Filled.LocationOn),
        StatCard("Patients Today", "247", "+18% from last week", true, Icons.Filled.Person),
        StatCard("Active Emergencies", "5", "Urgent attention needed", false, Icons.Filled.Warning),
        StatCard("Network Uptime", "94%", "Excellent performance", true, Icons.Filled.CheckCircle)
    )

    val clinics = listOf(
        Clinic("C001", "Soweto Community Clinic", 156, 12, "Grid Connected", ClinicStatus.ONLINE),
        Clinic("C002", "Alexandra Primary Healthcare", 89, 8, "Battery Backup", ClinicStatus.BACKUP),
        Clinic("C003", "Johannesburg General Hospital", 342, 45, "Grid Connected", ClinicStatus.ONLINE),
        Clinic("C004", "Orange Farm Community Health", 23, 5, "Outage", ClinicStatus.OFFLINE),
        Clinic("C005", "Midrand Medical Centre", 78, 15, "Grid Connected", ClinicStatus.ONLINE)
    )

    val patients = listOf(
        Patient("P001", "Sarah Mthembu", 34, "Soweto Community", "Today, 14:30", "Active"),
        Patient("P002", "John Ndlovu", 67, "Alexandra Primary", "Yesterday, 09:15", "Follow-up"),
        Patient("P003", "Maria Santos", 28, "Midrand Medical", "2 days ago", "Active"),
        Patient("P004", "David Zulu", 45, "Orange Farm", "3 days ago", "Critical")
    )

    val alerts = listOf(
        Alert("A001", "Emergency: Cardiac Event", "Soweto Community Clinic - Patient requires immediate transport", "Soweto Community Clinic", "2 minutes ago", AlertLevel.URGENT),
        Alert("A002", "Power Outage Detected", "Alexandra Clinic switched to backup power", "Alexandra Clinic", "15 minutes ago", AlertLevel.WARNING),
        Alert("A003", "Medicine Stock Low", "Orange Farm Clinic - Diabetes medication below threshold", "Orange Farm Clinic", "1 hour ago", AlertLevel.INFO)
    )

    val medicines = listOf(
        Medicine("Paracetamol 500mg", "Analgesic", "850 tablets", "Soweto Community", "Dec 2025", "Good Stock"),
        Medicine("Insulin Glargine", "Diabetes", "12 vials", "Orange Farm", "Jan 2026", "Low Stock"),
        Medicine("Amoxicillin 250mg", "Antibiotic", "340 capsules", "Alexandra Primary", "Sep 2025", "Expiring Soon"),
        Medicine("Metformin 500mg", "Diabetes", "567 tablets", "Midrand Medical", "Nov 2025", "Good Stock")
    )
}