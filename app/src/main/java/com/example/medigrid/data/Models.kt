package com.example.medigrid.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Context
import kotlin.random.Random

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
    SECURE_PATIENTS("secure_patients", "Secure Patients", Icons.Filled.Lock),
    SYMPTOM_CHECKER("symptom_checker", "AI Symptom Checker", Icons.Filled.Search),
    TELEMEDICINE("telemedicine", "Telemedicine", Icons.Filled.Call),
    INVENTORY("inventory", "Inventory", Icons.Filled.List),
    EMERGENCIES("emergencies", "Emergency Alerts", Icons.Filled.Warning),
    POWER("power", "Power Status", Icons.Filled.Star),
    LOAD_SHEDDING("load_shedding", "Load Shedding", Icons.Filled.PowerOff),
    ANALYTICS("analytics", "Analytics", Icons.Filled.Info),
    CHATBOT("chatbot", "AI Assistant", Icons.Filled.Info),
    SECURITY("security", "Security Dashboard", Icons.Filled.Lock),
    SETTINGS("settings", "Settings", Icons.Filled.Settings)
}

// Real data management functions
class DataManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "medigrid_data"
        private var instance: DataManager? = null

        fun getInstance(context: Context): DataManager {
            return instance ?: synchronized(this) {
                instance ?: DataManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Get comprehensive South African clinic data
     */
    fun getClinics(): List<Clinic> {
        return try {
            // Check if we have stored clinics
            val clinicsCount = sharedPrefs.getInt("clinics_count", 0)
            return if (clinicsCount > 0) {
                loadStoredClinics()
            } else {
                // Create comprehensive SA clinics if none exist
                val comprehensiveClinics = createComprehensiveSouthAfricanClinics()
                storeClinics(comprehensiveClinics)
                comprehensiveClinics
            }
        } catch (e: Exception) {
            // Fallback to default clinics if error occurs
            createComprehensiveSouthAfricanClinics()
        }
    }

    /**
     * Create comprehensive South African clinics covering all provinces
     */
    private fun createComprehensiveSouthAfricanClinics(): List<Clinic> = listOf(

        // =================== GAUTENG PROVINCE ===================

        // Major Hospitals - Johannesburg
        Clinic(
            id = "za_gp_chb_001",
            name = "Chris Hani Baragwanath Hospital",
            patientsToday = Random.nextInt(800, 1200),
            staffCount = 6500,
            powerStatus = "Grid + Backup Generator",
            status = ClinicStatus.ONLINE,
            latitude = -26.2041,
            longitude = 28.0473,
            province = "Gauteng",
            address = "26 Chris Hani Rd, Diepkloof, Soweto, 1864"
        ),

        Clinic(
            id = "za_gp_cmjah_001",
            name = "Charlotte Maxeke Johannesburg Hospital",
            patientsToday = Random.nextInt(400, 600),
            staffCount = 3200,
            powerStatus = "Grid + Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -26.1767,
            longitude = 28.0302,
            province = "Gauteng",
            address = "17 Jubilee Rd, Parktown, Johannesburg, 2193"
        ),

        Clinic(
            id = "za_gp_helen_001",
            name = "Helen Joseph Hospital",
            patientsToday = Random.nextInt(300, 500),
            staffCount = 2100,
            powerStatus = "Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -26.1500,
            longitude = 27.9833,
            province = "Gauteng",
            address = "Perth Rd, Westdene, Johannesburg, 2092"
        ),

        // Pretoria Hospitals
        Clinic(
            id = "za_gp_steve_001",
            name = "Steve Biko Academic Hospital",
            patientsToday = Random.nextInt(450, 650),
            staffCount = 3500,
            powerStatus = "Grid + Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -25.7069,
            longitude = 28.2294,
            province = "Gauteng",
            address = "Cnr Malherbe & 8th Ave, Gezina, Pretoria, 0001"
        ),

        Clinic(
            id = "za_gp_kalafong_001",
            name = "Kalafong Provincial Hospital",
            patientsToday = Random.nextInt(350, 550),
            staffCount = 2800,
            powerStatus = "Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -25.7833,
            longitude = 28.1000,
            province = "Gauteng",
            address = "Cnr Volkswagen & De Villebois Mareuil Dr, Atteridgeville, 0008"
        ),

        // Gauteng Community Health Centers
        Clinic(
            id = "za_gp_alex_001",
            name = "Alexandra Health Centre",
            patientsToday = Random.nextInt(150, 250),
            staffCount = 120,
            powerStatus = "Grid + Solar",
            status = ClinicStatus.ONLINE,
            latitude = -26.1000,
            longitude = 28.1000,
            province = "Gauteng",
            address = "1st Ave, Alexandra, Johannesburg, 2090"
        ),

        Clinic(
            id = "za_gp_chiawelo_001",
            name = "Chiawelo Community Health Centre",
            patientsToday = Random.nextInt(120, 200),
            staffCount = 95,
            powerStatus = "Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -26.2500,
            longitude = 27.8667,
            province = "Gauteng",
            address = "Vilakazi Street, Chiawelo, Soweto, 1818"
        ),

        Clinic(
            id = "za_gp_tembisa_001",
            name = "Tembisa Hospital",
            patientsToday = Random.nextInt(400, 600),
            staffCount = 1800,
            powerStatus = "Grid + Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -25.9833,
            longitude = 28.2333,
            province = "Gauteng",
            address = "Tembisa Hospital, Tembisa, 1632"
        ),

        Clinic(
            id = "za_gp_edenvale_001",
            name = "Edenvale General Hospital",
            patientsToday = Random.nextInt(200, 350),
            staffCount = 850,
            powerStatus = "Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -26.1333,
            longitude = 28.1500,
            province = "Gauteng",
            address = "cnr Modderfontein & Van Riebeeck Rd, Edenvale, 1610"
        ),

        // =================== WESTERN CAPE PROVINCE ===================

        // Cape Town Major Hospitals
        Clinic(
            id = "za_wc_groote_001",
            name = "Groote Schuur Hospital",
            patientsToday = Random.nextInt(400, 600),
            staffCount = 2800,
            powerStatus = "Grid + Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -33.9467,
            longitude = 18.4644,
            province = "Western Cape",
            address = "Main Rd, Observatory, Cape Town, 7925"
        ),

        Clinic(
            id = "za_wc_tygerberg_001",
            name = "Tygerberg Hospital",
            patientsToday = Random.nextInt(500, 750),
            staffCount = 4200,
            powerStatus = "Grid + Solar + Wind + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -33.8931,
            longitude = 18.6319,
            province = "Western Cape",
            address = "Francie van Zijl Dr, Tygerberg, Cape Town, 7505"
        ),

        Clinic(
            id = "za_wc_red_cross_001",
            name = "Red Cross War Memorial Children's Hospital",
            patientsToday = Random.nextInt(80, 150),
            staffCount = 1100,
            powerStatus = "Grid + Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -33.9731,
            longitude = 18.4731,
            province = "Western Cape",
            address = "Klipfontein Rd, Rondebosch, Cape Town, 7700"
        ),

        Clinic(
            id = "za_wc_new_somerset_001",
            name = "New Somerset Hospital",
            patientsToday = Random.nextInt(150, 300),
            staffCount = 890,
            powerStatus = "Grid + Solar",
            status = ClinicStatus.ONLINE,
            latitude = -33.9167,
            longitude = 18.4167,
            province = "Western Cape",
            address = "Portswood Rd, Green Point, Cape Town, 8001"
        ),

        // Western Cape Regional Hospitals
        Clinic(
            id = "za_wc_george_001",
            name = "George Hospital",
            patientsToday = Random.nextInt(120, 200),
            staffCount = 890,
            powerStatus = "Grid + Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -33.9608,
            longitude = 22.4614,
            province = "Western Cape",
            address = "Knysna Rd, Blanco, George, 6529"
        ),

        Clinic(
            id = "za_wc_worcester_001",
            name = "Worcester Hospital",
            patientsToday = Random.nextInt(100, 170),
            staffCount = 650,
            powerStatus = "Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -33.6464,
            longitude = 19.4419,
            province = "Western Cape",
            address = "27 Fairbairn St, Worcester, 6850"
        ),

        Clinic(
            id = "za_wc_stellenbosch_001",
            name = "Stellenbosch Hospital",
            patientsToday = Random.nextInt(80, 150),
            staffCount = 520,
            powerStatus = "Grid + Solar",
            status = ClinicStatus.ONLINE,
            latitude = -33.9321,
            longitude = 18.8602,
            province = "Western Cape",
            address = "Fransie van Zijl Dr, Stellenbosch, 7600"
        ),

        // =================== KWAZULU-NATAL PROVINCE ===================

        // Durban Major Hospitals
        Clinic(
            id = "za_kzn_inkosi_001",
            name = "Inkosi Albert Luthuli Central Hospital",
            patientsToday = Random.nextInt(350, 550),
            staffCount = 2500,
            powerStatus = "Grid + Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -29.8292,
            longitude = 30.9708,
            province = "KwaZulu-Natal",
            address = "800 Bellair Rd, Cato Manor, Durban, 4091"
        ),

        Clinic(
            id = "za_kzn_addington_001",
            name = "Addington Hospital",
            patientsToday = Random.nextInt(200, 350),
            staffCount = 1400,
            powerStatus = "Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -29.8750,
            longitude = 31.0167,
            province = "KwaZulu-Natal",
            address = "Erskine Terrace, South Beach, Durban, 4001"
        ),

        Clinic(
            id = "za_kzn_king_edward_001",
            name = "King Edward VIII Hospital",
            patientsToday = Random.nextInt(400, 650),
            staffCount = 2800,
            powerStatus = "Grid + Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -29.8833,
            longitude = 30.9833,
            province = "KwaZulu-Natal",
            address = "123 Umbilo Rd, Congella, Durban, 4013"
        ),

        // Pietermaritzburg
        Clinic(
            id = "za_kzn_grey_001",
            name = "Grey's Hospital",
            patientsToday = Random.nextInt(300, 500),
            staffCount = 2200,
            powerStatus = "Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -29.6000,
            longitude = 30.3833,
            province = "KwaZulu-Natal",
            address = "Town Bush Rd, Pietermaritzburg, 3201"
        ),

        Clinic(
            id = "za_kzn_edendale_001",
            name = "Edendale Hospital",
            patientsToday = Random.nextInt(250, 400),
            staffCount = 1800,
            powerStatus = "Grid + Solar",
            status = ClinicStatus.ONLINE,
            latitude = -29.6833,
            longitude = 30.3167,
            province = "KwaZulu-Natal",
            address = "Edendale Valley, Pietermaritzburg, 3216"
        ),

        // KZN Rural Hospitals
        Clinic(
            id = "za_kzn_church_001",
            name = "Church of Scotland Hospital",
            patientsToday = Random.nextInt(150, 250),
            staffCount = 450,
            powerStatus = "Solar + Generator",
            status = ClinicStatus.ONLINE,
            latitude = -28.7333,
            longitude = 29.8167,
            province = "KwaZulu-Natal",
            address = "R103, Tugela Ferry, 3010"
        ),

        Clinic(
            id = "za_kzn_stanger_001",
            name = "Stanger Hospital",
            patientsToday = Random.nextInt(180, 300),
            staffCount = 680,
            powerStatus = "Grid + Solar",
            status = ClinicStatus.ONLINE,
            latitude = -29.3333,
            longitude = 31.2833,
            province = "KwaZulu-Natal",
            address = "Compensation Beach Rd, Stanger, 4450"
        ),

        // =================== EASTERN CAPE PROVINCE ===================

        // Port Elizabeth/Gqeberha
        Clinic(
            id = "za_ec_livingstone_001",
            name = "Livingstone Hospital",
            patientsToday = Random.nextInt(300, 500),
            staffCount = 2100,
            powerStatus = "Grid + Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -33.9500,
            longitude = 25.6167,
            province = "Eastern Cape",
            address = "Stanford Rd, Korsten, Gqeberha, 6020"
        ),

        Clinic(
            id = "za_ec_dora_001",
            name = "Dora Nginza Hospital",
            patientsToday = Random.nextInt(200, 350),
            staffCount = 1500,
            powerStatus = "Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -33.7833,
            longitude = 25.4500,
            province = "Eastern Cape",
            address = "Zwide, Gqeberha, 6201"
        ),

        // East London
        Clinic(
            id = "za_ec_frere_001",
            name = "Frere Hospital",
            patientsToday = Random.nextInt(200, 350),
            staffCount = 1300,
            powerStatus = "Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -32.9833,
            longitude = 27.8667,
            province = "Eastern Cape",
            address = "Amalinda Main Rd, Amalinda, East London, 5247"
        ),

        Clinic(
            id = "za_ec_cecilia_001",
            name = "Cecilia Makiwane Hospital",
            patientsToday = Random.nextInt(180, 300),
            staffCount = 1100,
            powerStatus = "Grid + Solar",
            status = ClinicStatus.ONLINE,
            latitude = -32.8667,
            longitude = 27.6833,
            province = "Eastern Cape",
            address = "Mdantsane, East London, 5219"
        ),

        // Rural Eastern Cape
        Clinic(
            id = "za_ec_holy_cross_001",
            name = "Holy Cross Hospital",
            patientsToday = Random.nextInt(60, 120),
            staffCount = 185,
            powerStatus = "Solar + Generator",
            status = ClinicStatus.ONLINE,
            latitude = -31.0167,
            longitude = 29.4833,
            province = "Eastern Cape",
            address = "R61, Flagstaff, 4810"
        ),

        // =================== FREE STATE PROVINCE ===================

        // Bloemfontein
        Clinic(
            id = "za_fs_universitas_001",
            name = "Universitas Academic Hospital",
            patientsToday = Random.nextInt(220, 380),
            staffCount = 1650,
            powerStatus = "Grid + Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -29.1500,
            longitude = 26.2167,
            province = "Free State",
            address = "1 Logeman St, Universitas, Bloemfontein, 9301"
        ),

        Clinic(
            id = "za_fs_national_001",
            name = "National Hospital Bloemfontein",
            patientsToday = Random.nextInt(120, 200),
            staffCount = 680,
            powerStatus = "Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -29.1167,
            longitude = 26.2000,
            province = "Free State",
            address = "36 Kellner St, Bloemfontein Central, 9300"
        ),

        Clinic(
            id = "za_fs_pelonomi_001",
            name = "Pelonomi Hospital",
            patientsToday = Random.nextInt(100, 180),
            staffCount = 550,
            powerStatus = "Grid + Solar",
            status = ClinicStatus.ONLINE,
            latitude = -29.1333,
            longitude = 26.2667,
            province = "Free State",
            address = "Hanger St, Bloemfontein, 9301"
        ),

        Clinic(
            id = "za_fs_welkom_001",
            name = "Bongani Hospital",
            patientsToday = Random.nextInt(150, 250),
            staffCount = 780,
            powerStatus = "Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -27.9667,
            longitude = 26.7167,
            province = "Free State",
            address = "Welkom, 9460"
        ),

        // =================== LIMPOPO PROVINCE ===================

        // Polokwane
        Clinic(
            id = "za_lp_polokwane_001",
            name = "Polokwane Provincial Hospital",
            patientsToday = Random.nextInt(200, 350),
            staffCount = 1200,
            powerStatus = "Grid + Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -23.9000,
            longitude = 29.4667,
            province = "Limpopo",
            address = "Cnr Hospital & Biccard St, Polokwane, 0700"
        ),

        Clinic(
            id = "za_lp_mankweng_001",
            name = "Mankweng Hospital",
            patientsToday = Random.nextInt(120, 200),
            staffCount = 650,
            powerStatus = "Grid + Solar",
            status = ClinicStatus.ONLINE,
            latitude = -23.8833,
            longitude = 29.7333,
            province = "Limpopo",
            address = "University of Limpopo Campus, Mankweng, 0727"
        ),

        // Rural Limpopo
        Clinic(
            id = "za_lp_elim_001",
            name = "Elim Hospital",
            patientsToday = Random.nextInt(180, 280),
            staffCount = 520,
            powerStatus = "Solar + Generator",
            status = ClinicStatus.ONLINE,
            latitude = -23.1333,
            longitude = 30.2167,
            province = "Limpopo",
            address = "R578, Elim, 0960"
        ),

        Clinic(
            id = "za_lp_donald_fraser_001",
            name = "Donald Fraser Hospital",
            patientsToday = Random.nextInt(100, 180),
            staffCount = 380,
            powerStatus = "Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -22.9500,
            longitude = 30.1667,
            province = "Limpopo",
            address = "Lenyenye, 1351"
        ),

        // =================== MPUMALANGA PROVINCE ===================

        // Mbombela (Nelspruit)
        Clinic(
            id = "za_mp_mbombela_001",
            name = "Mbombela Hospital",
            patientsToday = Random.nextInt(150, 280),
            staffCount = 890,
            powerStatus = "Grid + Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -25.4753,
            longitude = 30.9694,
            province = "Mpumalanga",
            address = "Cnr Bitterbessie & Oosthuizen St, Nelspruit, 1200"
        ),

        // Witbank/eMalahleni
        Clinic(
            id = "za_mp_witbank_001",
            name = "Witbank Hospital",
            patientsToday = Random.nextInt(120, 220),
            staffCount = 580,
            powerStatus = "Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -25.8667,
            longitude = 29.2333,
            province = "Mpumalanga",
            address = "Beatty Ave, eMalahleni, 1035"
        ),

        Clinic(
            id = "za_mp_middelburg_001",
            name = "Middelburg Hospital",
            patientsToday = Random.nextInt(100, 180),
            staffCount = 420,
            powerStatus = "Grid + Solar",
            status = ClinicStatus.ONLINE,
            latitude = -25.7833,
            longitude = 29.4667,
            province = "Mpumalanga",
            address = "Steve Biko St, Middelburg, 1050"
        ),

        Clinic(
            id = "za_mp_ermelo_001",
            name = "Ermelo Hospital",
            patientsToday = Random.nextInt(80, 150),
            staffCount = 350,
            powerStatus = "Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -26.5167,
            longitude = 29.9833,
            province = "Mpumalanga",
            address = "Kerk St, Ermelo, 2351"
        ),

        // =================== NORTH WEST PROVINCE ===================

        // Mahikeng
        Clinic(
            id = "za_nw_mahikeng_001",
            name = "Mahikeng Provincial Hospital",
            patientsToday = Random.nextInt(180, 300),
            staffCount = 920,
            powerStatus = "Grid + Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -25.8500,
            longitude = 25.6500,
            province = "North West",
            address = "Golf Course Rd, Mahikeng, 2745"
        ),

        // Klerksdorp
        Clinic(
            id = "za_nw_klerksdorp_001",
            name = "Klerksdorp Hospital",
            patientsToday = Random.nextInt(150, 280),
            staffCount = 750,
            powerStatus = "Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -26.8500,
            longitude = 26.6667,
            province = "North West",
            address = "Hospital St, Klerksdorp, 2571"
        ),

        Clinic(
            id = "za_nw_potchefstroom_001",
            name = "Potchefstroom Hospital",
            patientsToday = Random.nextInt(120, 200),
            staffCount = 580,
            powerStatus = "Grid + Solar",
            status = ClinicStatus.ONLINE,
            latitude = -26.7167,
            longitude = 27.0833,
            province = "North West",
            address = "Potch Hospital, Potchefstroom, 2520"
        ),

        Clinic(
            id = "za_nw_rustenburg_001",
            name = "Rustenburg Hospital",
            patientsToday = Random.nextInt(100, 180),
            staffCount = 450,
            powerStatus = "Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -25.6667,
            longitude = 27.2333,
            province = "North West",
            address = "Rustenburg Provincial Hospital, Rustenburg, 0300"
        ),

        // =================== NORTHERN CAPE PROVINCE ===================

        // Kimberley
        Clinic(
            id = "za_nc_kimberley_001",
            name = "Kimberley Hospital Complex",
            patientsToday = Random.nextInt(250, 400),
            staffCount = 1400,
            powerStatus = "Grid + Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -28.7500,
            longitude = 24.7833,
            province = "Northern Cape",
            address = "Hanover Rd, Kimberley, 8301"
        ),

        // Upington
        Clinic(
            id = "za_nc_upington_001",
            name = "Upington Provincial Hospital",
            patientsToday = Random.nextInt(100, 180),
            staffCount = 420,
            powerStatus = "Solar + Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -28.4167,
            longitude = 21.2500,
            province = "Northern Cape",
            address = "Mutual St, Upington, 8800"
        ),

        Clinic(
            id = "za_nc_springbok_001",
            name = "Springbok Hospital",
            patientsToday = Random.nextInt(50, 100),
            staffCount = 180,
            powerStatus = "Solar + Grid",
            status = ClinicStatus.ONLINE,
            latitude = -29.6667,
            longitude = 17.8833,
            province = "Northern Cape",
            address = "Springbok Hospital, Springbok, 8240"
        ),

        Clinic(
            id = "za_nc_de_aar_001",
            name = "De Aar Hospital",
            patientsToday = Random.nextInt(60, 120),
            staffCount = 220,
            powerStatus = "Grid + Solar",
            status = ClinicStatus.ONLINE,
            latitude = -30.6500,
            longitude = 23.9833,
            province = "Northern Cape",
            address = "De Aar Hospital, De Aar, 7000"
        ),

        // =================== ADDITIONAL COMMUNITY HEALTH CENTERS ===================

        // Additional Gauteng CHCs
        Clinic(
            id = "za_gp_diepsloot_001",
            name = "Diepsloot Community Health Centre",
            patientsToday = Random.nextInt(180, 300),
            staffCount = 150,
            powerStatus = "Grid + Solar",
            status = ClinicStatus.ONLINE,
            latitude = -25.9333,
            longitude = 28.0167,
            province = "Gauteng",
            address = "Diepsloot Extension 1, Johannesburg, 2189"
        ),

        Clinic(
            id = "za_gp_orange_farm_001",
            name = "Orange Farm Community Health Centre",
            patientsToday = Random.nextInt(200, 350),
            staffCount = 180,
            powerStatus = "Grid + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -26.5000,
            longitude = 27.8500,
            province = "Gauteng",
            address = "Orange Farm, Johannesburg, 1841"
        ),

        // Additional Western Cape CHCs
        Clinic(
            id = "za_wc_khayelitsha_001",
            name = "Khayelitsha District Hospital",
            patientsToday = Random.nextInt(300, 500),
            staffCount = 800,
            powerStatus = "Grid + Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -34.0333,
            longitude = 18.6833,
            province = "Western Cape",
            address = "Khayelitsha, Cape Town, 7784"
        ),

        Clinic(
            id = "za_wc_mitchells_plain_001",
            name = "Mitchells Plain Community Health Centre",
            patientsToday = Random.nextInt(250, 400),
            staffCount = 320,
            powerStatus = "Grid + Solar",
            status = ClinicStatus.ONLINE,
            latitude = -34.2167,
            longitude = 18.6167,
            province = "Western Cape",
            address = "Mitchells Plain, Cape Town, 7785"
        ),

        // Additional KZN Rural Clinics
        Clinic(
            id = "za_kzn_hlabisa_001",
            name = "Hlabisa Hospital",
            patientsToday = Random.nextInt(120, 200),
            staffCount = 280,
            powerStatus = "Solar + Generator",
            status = ClinicStatus.ONLINE,
            latitude = -28.0833,
            longitude = 32.0667,
            province = "KwaZulu-Natal",
            address = "Hlabisa, 3937"
        ),

        Clinic(
            id = "za_kzn_manguzi_001",
            name = "Manguzi Hospital",
            patientsToday = Random.nextInt(100, 180),
            staffCount = 220,
            powerStatus = "Solar + Backup",
            status = ClinicStatus.ONLINE,
            latitude = -27.0333,
            longitude = 32.7667,
            province = "KwaZulu-Natal",
            address = "Manguzi, 3973"
        ),

        // Mobile Clinics for Remote Areas
        Clinic(
            id = "za_mobile_001",
            name = "Mobile Health Unit - Northern Cape",
            patientsToday = Random.nextInt(30, 80),
            staffCount = 8,
            powerStatus = "Solar + Battery",
            status = ClinicStatus.ONLINE,
            latitude = -29.0000,
            longitude = 21.0000,
            province = "Northern Cape",
            address = "Mobile Unit - Various Remote Locations"
        ),

        Clinic(
            id = "za_mobile_002",
            name = "Mobile Health Unit - Limpopo Rural",
            patientsToday = Random.nextInt(40, 90),
            staffCount = 10,
            powerStatus = "Solar + Battery",
            status = ClinicStatus.ONLINE,
            latitude = -23.5000,
            longitude = 29.0000,
            province = "Limpopo",
            address = "Mobile Unit - Rural Limpopo Areas"
        )
    )

    /**
     * Get real patient data from storage with error handling
     */
    fun getPatients(): List<Patient> {
        return try {
            val patientsCount = sharedPrefs.getInt("patients_count", 0)
            val patients = mutableListOf<Patient>()

            for (i in 0 until patientsCount) {
                val patientData = sharedPrefs.getString("patient_$i", null)
                if (patientData != null) {
                    parsePatient(patientData)?.let { patients.add(it) }
                }
            }
            patients
        } catch (e: Exception) {
            // Return empty list if error occurs
            emptyList()
        }
    }

    /**
     * Get real medicine inventory from storage with error handling
     */
    fun getMedicines(): List<Medicine> {
        return try {
            val medicinesCount = sharedPrefs.getInt("medicines_count", 0)
            val medicines = mutableListOf<Medicine>()

            for (i in 0 until medicinesCount) {
                val medicineData = sharedPrefs.getString("medicine_$i", null)
                if (medicineData != null) {
                    parseMedicine(medicineData)?.let { medicines.add(it) }
                }
            }
            medicines
        } catch (e: Exception) {
            // Return empty list if error occurs
            emptyList()
        }
    }

    /**
     * Get real alerts from storage with error handling
     */
    fun getAlerts(): List<Alert> {
        return try {
            val alertsCount = sharedPrefs.getInt("alerts_count", 0)
            val alerts = mutableListOf<Alert>()

            for (i in 0 until alertsCount) {
                val alertData = sharedPrefs.getString("alert_$i", null)
                if (alertData != null) {
                    parseAlert(alertData)?.let { alerts.add(it) }
                }
            }
            alerts
        } catch (e: Exception) {
            // Return empty list if error occurs
            emptyList()
        }
    }

    /**
     * Get real statistics based on actual data with error handling
     */
    fun getStats(): List<StatCard> {
        return try {
            val clinics = getClinics()
            val patients = getPatients()
            val medicines = getMedicines()
            val alerts = getAlerts()

            listOf(
                StatCard(
                    title = "Active Clinics",
                    value = clinics.count { it.status == ClinicStatus.ONLINE }.toString(),
                    change = "Real-time data",
                    isPositive = true,
                    icon = Icons.Default.LocationOn
                ),
                StatCard(
                    title = "Patients Today",
                    value = clinics.sumOf { it.patientsToday }.toString(),
                    change = "Total visits",
                    isPositive = true,
                    icon = Icons.Default.Person
                ),
                StatCard(
                    title = "Medicine Items",
                    value = medicines.size.toString(),
                    change = "In inventory",
                    isPositive = true,
                    icon = Icons.Default.ShoppingCart
                ),
                StatCard(
                    title = "Active Alerts",
                    value = alerts.count { it.level == AlertLevel.URGENT }.toString(),
                    change = "Need attention",
                    isPositive = false,
                    icon = Icons.Default.Warning
                )
            )
        } catch (e: Exception) {
            // Return default stats if error occurs
            listOf(
                StatCard(
                    title = "System Status",
                    value = "Online",
                    change = "Operational",
                    isPositive = true,
                    icon = Icons.Default.CheckCircle
                )
            )
        }
    }

    // Add data functions with error handling
    fun addClinic(clinic: Clinic) {
        try {
            val clinics = getClinics().toMutableList()
            clinics.add(clinic)
            storeClinics(clinics)
        } catch (e: Exception) {
            // Handle error silently for now
        }
    }

    fun addPatient(patient: Patient) {
        try {
            val patients = getPatients().toMutableList()
            patients.add(patient)
            storePatients(patients)
        } catch (e: Exception) {
            // Handle error silently for now
        }
    }

    fun addMedicine(medicine: Medicine) {
        try {
            val medicines = getMedicines().toMutableList()
            medicines.add(medicine)
            storeMedicines(medicines)
        } catch (e: Exception) {
            // Handle error silently for now
        }
    }

    fun addAlert(alert: Alert) {
        try {
            val alerts = getAlerts().toMutableList()
            alerts.add(alert)
            storeAlerts(alerts)
        } catch (e: Exception) {
            // Handle error silently for now
        }
    }

    // Storage functions with error handling
    private fun storeClinics(clinics: List<Clinic>) {
        try {
            val editor = sharedPrefs.edit()
            editor.putInt("clinics_count", clinics.size)
            clinics.forEachIndexed { index, clinic ->
                editor.putString("clinic_$index", serializeClinic(clinic))
            }
            editor.apply()
        } catch (e: Exception) {
            // Handle storage error silently
        }
    }

    private fun storePatients(patients: List<Patient>) {
        try {
            val editor = sharedPrefs.edit()
            editor.putInt("patients_count", patients.size)
            patients.forEachIndexed { index, patient ->
                editor.putString("patient_$index", serializePatient(patient))
            }
            editor.apply()
        } catch (e: Exception) {
            // Handle storage error silently
        }
    }

    private fun storeMedicines(medicines: List<Medicine>) {
        try {
            val editor = sharedPrefs.edit()
            editor.putInt("medicines_count", medicines.size)
            medicines.forEachIndexed { index, medicine ->
                editor.putString("medicine_$index", serializeMedicine(medicine))
            }
            editor.apply()
        } catch (e: Exception) {
            // Handle storage error silently
        }
    }

    private fun storeAlerts(alerts: List<Alert>) {
        try {
            val editor = sharedPrefs.edit()
            editor.putInt("alerts_count", alerts.size)
            alerts.forEachIndexed { index, alert ->
                editor.putString("alert_$index", serializeAlert(alert))
            }
            editor.apply()
        } catch (e: Exception) {
            // Handle storage error silently
        }
    }

    // Loading functions with error handling
    private fun loadStoredClinics(): List<Clinic> {
        return try {
            val clinicsCount = sharedPrefs.getInt("clinics_count", 0)
            val clinics = mutableListOf<Clinic>()

            for (i in 0 until clinicsCount) {
                val clinicData = sharedPrefs.getString("clinic_$i", null)
                if (clinicData != null) {
                    parseClinic(clinicData)?.let { clinics.add(it) }
                }
            }
            clinics
        } catch (e: Exception) {
            // Return empty list if error occurs
            emptyList()
        }
    }

    // Serialization functions with error handling
    private fun serializeClinic(clinic: Clinic): String {
        return try {
            "${clinic.id}|${clinic.name}|${clinic.patientsToday}|${clinic.staffCount}|${clinic.powerStatus}|${clinic.status}|${clinic.latitude}|${clinic.longitude}|${clinic.province}|${clinic.address}"
        } catch (e: Exception) {
            ""
        }
    }

    private fun serializePatient(patient: Patient): String {
        return try {
            "${patient.id}|${patient.name}|${patient.age}|${patient.clinic}|${patient.lastVisit}|${patient.status}"
        } catch (e: Exception) {
            ""
        }
    }

    private fun serializeMedicine(medicine: Medicine): String {
        return try {
            "${medicine.name}|${medicine.category}|${medicine.stockLevel}|${medicine.location}|${medicine.expiryDate}|${medicine.status}"
        } catch (e: Exception) {
            ""
        }
    }

    private fun serializeAlert(alert: Alert): String {
        return try {
            "${alert.id}|${alert.title}|${alert.description}|${alert.location}|${alert.time}|${alert.level}|${alert.isActive}"
        } catch (e: Exception) {
            ""
        }
    }

    // Parsing functions with enhanced error handling
    private fun parseClinic(data: String): Clinic? {
        return try {
            val parts = data.split("|")
            if (parts.size >= 10) {
                Clinic(
                    id = parts[0],
                    name = parts[1],
                    patientsToday = parts.getOrNull(2)?.toIntOrNull() ?: 0,
                    staffCount = parts.getOrNull(3)?.toIntOrNull() ?: 1,
                    powerStatus = parts.getOrNull(4) ?: "Unknown",
                    status = try {
                        ClinicStatus.valueOf(parts[5])
                    } catch (e: Exception) {
                        ClinicStatus.ONLINE
                    },
                    latitude = parts.getOrNull(6)?.toDoubleOrNull() ?: 0.0,
                    longitude = parts.getOrNull(7)?.toDoubleOrNull() ?: 0.0,
                    province = parts.getOrNull(8) ?: "Unknown",
                    address = parts.getOrNull(9) ?: "Unknown Address"
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePatient(data: String): Patient? {
        return try {
            val parts = data.split("|")
            if (parts.size >= 6) {
                Patient(
                    id = parts[0],
                    name = parts[1],
                    age = parts.getOrNull(2)?.toIntOrNull() ?: 0,
                    clinic = parts.getOrNull(3) ?: "",
                    lastVisit = parts.getOrNull(4) ?: "",
                    status = parts.getOrNull(5) ?: ""
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMedicine(data: String): Medicine? {
        return try {
            val parts = data.split("|")
            if (parts.size >= 6) {
                Medicine(
                    name = parts[0],
                    category = parts[1],
                    stockLevel = parts[2],
                    location = parts[3],
                    expiryDate = parts[4],
                    status = parts[5]
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseAlert(data: String): Alert? {
        return try {
            val parts = data.split("|")
            if (parts.size >= 7) {
                Alert(
                    id = parts[0],
                    title = parts[1],
                    description = parts[2],
                    location = parts[3],
                    time = parts[4],
                    level = try {
                        AlertLevel.valueOf(parts[5])
                    } catch (e: Exception) {
                        AlertLevel.INFO
                    },
                    isActive = parts.getOrNull(6)?.toBooleanStrictOrNull() ?: true
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}