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
    val status: ClinicStatus,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val province: String = "",
    val address: String = "",
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
    CLINICS("clinics", "Clinics", Icons.Filled.LocationOn),
    NETWORK_MAP("network_map", "Network Map", Icons.Filled.Place),
    PATIENTS("patients", "Patients", Icons.Filled.Person),
    INVENTORY("inventory", "Inventory", Icons.Filled.List),
    EMERGENCIES("emergencies", "Emergency Alerts", Icons.Filled.Warning),
    POWER("power", "Power Status", Icons.Filled.Star),
    ANALYTICS("analytics", "Analytics", Icons.Filled.Info),
    CHATBOT("chatbot", "AI Assistant", Icons.Filled.Info),
    SECURITY("security", "Security Dashboard", Icons.Filled.Lock),
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
        Clinic(
            "C001",
            "Soweto Community Clinic",
            156,
            12,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -26.2619,
            27.8648,
            "Gauteng",
            "1234 Soweto"
        ),
        Clinic(
            "C002",
            "Alexandra Primary Healthcare",
            89,
            8,
            "Battery Backup",
            ClinicStatus.BACKUP,
            -26.1037,
            28.0722,
            "Gauteng",
            "5678 Alexandra"
        ),
        Clinic(
            "C003",
            "Johannesburg General Hospital",
            342,
            45,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -26.1945,
            28.0373,
            "Gauteng",
            "9012 Johannesburg"
        ),
        Clinic(
            "C004",
            "Orange Farm Community Health",
            23,
            5,
            "Outage",
            ClinicStatus.OFFLINE,
            -26.4584,
            27.8539,
            "Gauteng",
            "1111 Orange Farm"
        ),
        Clinic(
            "C005",
            "Midrand Medical Centre",
            78,
            15,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -25.9934,
            28.1363,
            "Gauteng",
            "2222 Midrand"
        ),
        Clinic(
            "C006",
            "Cape Town Community Clinic",
            145,
            10,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -33.9249,
            18.4241,
            "Western Cape",
            "3333 Cape Town"
        ),
        Clinic(
            "C007",
            "Khayelitsha Primary Healthcare",
            67,
            8,
            "Battery Backup",
            ClinicStatus.BACKUP,
            -34.0384,
            18.6755,
            "Western Cape",
            "4444 Khayelitsha"
        ),
        Clinic(
            "C008",
            "Cape Town General Hospital",
            278,
            50,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -33.9293,
            18.4161,
            "Western Cape",
            "5555 Cape Town"
        ),
        Clinic(
            "C009",
            "Langa Community Health",
            56,
            6,
            "Outage",
            ClinicStatus.OFFLINE,
            -33.9614,
            18.5159,
            "Western Cape",
            "6666 Langa"
        ),
        Clinic(
            "C010",
            "Stellenbosch Medical Centre",
            90,
            12,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -33.9343,
            18.8632,
            "Western Cape",
            "7777 Stellenbosch"
        ),
        Clinic(
            "C011",
            "Durban Community Clinic",
            120,
            10,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -29.8587,
            31.0292,
            "KwaZulu-Natal",
            "8888 Durban"
        ),
        Clinic(
            "C012",
            "Umlazi Primary Healthcare",
            45,
            6,
            "Battery Backup",
            ClinicStatus.BACKUP,
            -29.9715,
            30.8833,
            "KwaZulu-Natal",
            "9999 Umlazi"
        ),
        Clinic(
            "C013",
            "Durban General Hospital",
            245,
            40,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -29.8573,
            31.0284,
            "KwaZulu-Natal",
            "1010 Durban"
        ),
        Clinic(
            "C014",
            "Pietermaritzburg Community Health",
            34,
            5,
            "Outage",
            ClinicStatus.OFFLINE,
            -29.6032,
            30.3793,
            "KwaZulu-Natal",
            "1111 Pietermaritzburg"
        ),
        Clinic(
            "C015",
            "Richards Bay Medical Centre",
            60,
            8,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -28.7839,
            32.0608,
            "KwaZulu-Natal",
            "1222 Richards Bay"
        ),
        Clinic(
            "C016",
            "Port Elizabeth Community Clinic",
            100,
            9,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -33.9244,
            25.6123,
            "Eastern Cape",
            "1333 Port Elizabeth"
        ),
        Clinic(
            "C017",
            "New Brighton Primary Healthcare",
            50,
            6,
            "Battery Backup",
            ClinicStatus.BACKUP,
            -33.9033,
            25.6232,
            "Eastern Cape",
            "1444 New Brighton"
        ),
        Clinic(
            "C018",
            "Port Elizabeth General Hospital",
            200,
            35,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -33.9582,
            25.6005,
            "Eastern Cape",
            "1555 Port Elizabeth"
        ),
        Clinic(
            "C019",
            "Uitenhage Community Health",
            28,
            4,
            "Outage",
            ClinicStatus.OFFLINE,
            -33.7625,
            25.4016,
            "Eastern Cape",
            "1666 Uitenhage"
        ),
        Clinic(
            "C020",
            "East London Medical Centre",
            70,
            10,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -33.0156,
            27.8933,
            "Eastern Cape",
            "1777 East London"
        ),
        Clinic(
            "C021",
            "Bloemfontein Community Clinic",
            110,
            8,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -29.1211,
            26.2256,
            "Free State",
            "1888 Bloemfontein"
        ),
        Clinic(
            "C022",
            "Thaba Nchu Primary Healthcare",
            30,
            5,
            "Battery Backup",
            ClinicStatus.BACKUP,
            -29.2021,
            26.8375,
            "Free State",
            "1999 Thaba Nchu"
        ),
        Clinic(
            "C023",
            "Bloemfontein General Hospital",
            180,
            30,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -29.1144,
            26.2169,
            "Free State",
            "2000 Bloemfontein"
        ),
        Clinic(
            "C024",
            "Welkom Community Health",
            40,
            6,
            "Outage",
            ClinicStatus.OFFLINE,
            -27.9757,
            26.7359,
            "Free State",
            "2111 Welkom"
        ),
        Clinic(
            "C025",
            "Kroonstad Medical Centre",
            80,
            10,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -27.6511,
            27.2364,
            "Free State",
            "2222 Kroonstad"
        ),
        Clinic(
            "C026",
            "Kimberley Community Clinic",
            130,
            9,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -28.7453,
            24.7649,
            "Northern Cape",
            "2333 Kimberley"
        ),
        Clinic(
            "C027",
            "Galeshewe Primary Healthcare",
            55,
            7,
            "Battery Backup",
            ClinicStatus.BACKUP,
            -28.7439,
            24.7874,
            "Northern Cape",
            "2444 Galeshewe"
        ),
        Clinic(
            "C028",
            "Kimberley General Hospital",
            220,
            40,
            "Grid Connected",
            ClinicStatus.ONLINE,
            -28.7484,
            24.7741,
            "Northern Cape",
            "2555 Kimberley"
        )
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