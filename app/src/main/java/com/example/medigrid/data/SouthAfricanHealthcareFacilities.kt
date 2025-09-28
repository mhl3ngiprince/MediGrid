package com.example.medigrid.data

import android.content.Context
import kotlin.random.Random

/**
 * Comprehensive South African Healthcare Facilities Database
 * 
 * This database contains real healthcare facilities across all 9 provinces:
 * - Gauteng, Western Cape, KwaZulu-Natal, Eastern Cape, Free State,
 *   Limpopo, Mpumalanga, North West, Northern Cape
 * 
 * Includes public hospitals, private hospitals, clinics, CHCs, and district hospitals
 */

enum class FacilityType {
    NATIONAL_HOSPITAL,      // National/Central hospitals
    PROVINCIAL_HOSPITAL,    // Provincial hospitals
    REGIONAL_HOSPITAL,      // Regional hospitals
    DISTRICT_HOSPITAL,      // District hospitals
    COMMUNITY_HEALTH_CENTER, // CHCs
    PRIMARY_CLINIC,         // Primary healthcare clinics
    PRIVATE_HOSPITAL,       // Private hospitals
    SPECIALIZED_HOSPITAL,   // Specialized hospitals
    RURAL_CLINIC,          // Rural clinics
    MOBILE_CLINIC,         // Mobile clinics
    EMERGENCY_SERVICES,     // Emergency services
    MATERNITY_WARD,        // Maternity units
    TUBERCULOSIS_HOSPITAL,  // TB specialized
    MENTAL_HEALTH_FACILITY, // Mental health
    REHABILITATION_CENTER   // Rehabilitation
}

data class HealthcareFacility(
    val id: String,
    val name: String,
    val province: String,
    val city: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val facilityType: FacilityType,
    val bedCapacity: Int,
    val staffCount: Int,
    val patientsToday: Int,
    val status: ClinicStatus,
    val powerStatus: String,
    val specialties: List<String> = emptyList(),
    val emergencyServices: Boolean = false,
    val phoneNumber: String = "",
    val email: String = "",
    val operatingHours: String = "24/7",
    val languages: List<String> = listOf("English", "Afrikaans"),
    val hasAmbulance: Boolean = false,
    val hasHelicopter: Boolean = false
)

object SouthAfricanHealthcareFacilities {
    
    fun getAllFacilities(): List<HealthcareFacility> = listOf(
        
        // =================== GAUTENG PROVINCE ===================
        
        // Johannesburg Major Hospitals
        HealthcareFacility(
            id = "za_gp_chb_001",
            name = "Chris Hani Baragwanath Academic Hospital",
            province = "Gauteng",
            city = "Soweto",
            address = "26 Chris Hani Rd, Diepkloof, Soweto, 1864",
            latitude = -26.2667,
            longitude = 27.8833,
            facilityType = FacilityType.NATIONAL_HOSPITAL,
            bedCapacity = 3200,
            staffCount = 6500,
            patientsToday = Random.nextInt(800, 1200),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Backup Generator",
            specialties = listOf("Emergency Medicine", "Surgery", "Internal Medicine", "Pediatrics", "Obstetrics", "Oncology", "Cardiology"),
            emergencyServices = true,
            phoneNumber = "011 933 0000",
            operatingHours = "24/7",
            languages = listOf("English", "Afrikaans", "Zulu", "Sotho", "Xhosa"),
            hasAmbulance = true,
            hasHelicopter = true
        ),
        
        HealthcareFacility(
            id = "za_gp_cmjah_001",
            name = "Charlotte Maxeke Johannesburg Academic Hospital",
            province = "Gauteng",
            city = "Johannesburg",
            address = "17 Jubilee Rd, Parktown, Johannesburg, 2193",
            latitude = -26.1767,
            longitude = 28.0302,
            facilityType = FacilityType.NATIONAL_HOSPITAL,
            bedCapacity = 1088,
            staffCount = 3200,
            patientsToday = Random.nextInt(400, 600),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + Backup",
            specialties = listOf("Trauma", "Emergency Medicine", "Surgery", "Internal Medicine", "Neurology", "Cardiothoracic Surgery"),
            emergencyServices = true,
            phoneNumber = "011 488 4911",
            operatingHours = "24/7",
            languages = listOf("English", "Afrikaans", "Zulu", "Sotho"),
            hasAmbulance = true,
            hasHelicopter = true
        ),
        
        HealthcareFacility(
            id = "za_gp_hja_001",
            name = "Helen Joseph Hospital",
            province = "Gauteng",
            city = "Johannesburg",
            address = "Perth Rd, Westdene, Johannesburg, 2092",
            latitude = -26.1500,
            longitude = 27.9833,
            facilityType = FacilityType.PROVINCIAL_HOSPITAL,
            bedCapacity = 696,
            staffCount = 2100,
            patientsToday = Random.nextInt(300, 500),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Backup",
            specialties = listOf("Internal Medicine", "Surgery", "Pediatrics", "Obstetrics", "Orthopedics"),
            emergencyServices = true,
            phoneNumber = "011 276 8000",
            hasAmbulance = true
        ),
        
        // Pretoria Major Hospitals
        HealthcareFacility(
            id = "za_gp_steve_001",
            name = "Steve Biko Academic Hospital",
            province = "Gauteng",
            city = "Pretoria",
            address = "Cnr Malherbe & 8th Ave, Gezina, Pretoria, 0001",
            latitude = -25.7069,
            longitude = 28.2294,
            facilityType = FacilityType.NATIONAL_HOSPITAL,
            bedCapacity = 1200,
            staffCount = 3500,
            patientsToday = Random.nextInt(450, 650),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + Backup",
            specialties = listOf("Neurosurgery", "Cardiothoracic Surgery", "Transplant Surgery", "Oncology", "Internal Medicine"),
            emergencyServices = true,
            phoneNumber = "012 354 1000",
            hasAmbulance = true,
            hasHelicopter = true
        ),
        
        HealthcareFacility(
            id = "za_gp_kalafong_001",
            name = "Kalafong Provincial Tertiary Hospital",
            province = "Gauteng",
            city = "Pretoria",
            address = "Cnr Volkswagen & De Villebois Mareuil Dr, Atteridgeville, 0008",
            latitude = -25.7833,
            longitude = 28.1000,
            facilityType = FacilityType.PROVINCIAL_HOSPITAL,
            bedCapacity = 980,
            staffCount = 2800,
            patientsToday = Random.nextInt(350, 550),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Backup",
            specialties = listOf("Maternal Health", "Neonatology", "Pediatrics", "Surgery", "Internal Medicine"),
            emergencyServices = true,
            phoneNumber = "012 373 0301",
            hasAmbulance = true
        ),
        
        // Private Hospitals - Gauteng
        HealthcareFacility(
            id = "za_gp_sandton_001",
            name = "Sandton Mediclinic",
            province = "Gauteng",
            city = "Sandton",
            address = "Peter Place, Lyme Park, Sandton, 2196",
            latitude = -26.1075,
            longitude = 28.0567,
            facilityType = FacilityType.PRIVATE_HOSPITAL,
            bedCapacity = 280,
            staffCount = 850,
            patientsToday = Random.nextInt(80, 120),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + UPS",
            specialties = listOf("Cardiology", "Oncology", "Orthopedics", "Neurology", "Cosmetic Surgery"),
            emergencyServices = true,
            phoneNumber = "011 709 2000"
        ),
        
        HealthcareFacility(
            id = "za_gp_milpark_001",
            name = "Milpark Hospital",
            province = "Gauteng",
            city = "Johannesburg",
            address = "9 Guild Rd, Parktown West, Johannesburg, 2193",
            latitude = -26.1667,
            longitude = 28.0167,
            facilityType = FacilityType.PRIVATE_HOSPITAL,
            bedCapacity = 408,
            staffCount = 1200,
            patientsToday = Random.nextInt(120, 180),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + Backup",
            specialties = listOf("Cardiac Surgery", "Oncology", "Neurosurgery", "Emergency Medicine"),
            emergencyServices = true,
            phoneNumber = "011 480 7111",
            hasAmbulance = true,
            hasHelicopter = true
        ),
        
        // Gauteng Clinics
        HealthcareFacility(
            id = "za_gp_alex_001",
            name = "Alexandra Health Centre",
            province = "Gauteng",
            city = "Alexandra",
            address = "1st Ave, Alexandra, Johannesburg, 2090",
            latitude = -26.1000,
            longitude = 28.1000,
            facilityType = FacilityType.COMMUNITY_HEALTH_CENTER,
            bedCapacity = 50,
            staffCount = 120,
            patientsToday = Random.nextInt(150, 250),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar",
            specialties = listOf("Primary Healthcare", "Maternal Health", "HIV/AIDS Care", "TB Treatment"),
            phoneNumber = "011 882 9400",
            operatingHours = "07:30-16:00"
        ),
        
        HealthcareFacility(
            id = "za_gp_soweto_001",
            name = "Chiawelo Community Health Centre",
            province = "Gauteng",
            city = "Soweto",
            address = "Vilakazi Street, Chiawelo, Soweto, 1818",
            latitude = -26.2500,
            longitude = 27.8667,
            facilityType = FacilityType.COMMUNITY_HEALTH_CENTER,
            bedCapacity = 40,
            staffCount = 95,
            patientsToday = Random.nextInt(120, 200),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Backup",
            specialties = listOf("Primary Healthcare", "Chronic Disease Management", "Family Planning"),
            phoneNumber = "011 938 0200",
            operatingHours = "07:30-16:00"
        ),
        
        // =================== WESTERN CAPE PROVINCE ===================
        
        // Cape Town Major Hospitals
        HealthcareFacility(
            id = "za_wc_groote_001",
            name = "Groote Schuur Hospital",
            province = "Western Cape",
            city = "Cape Town",
            address = "Main Rd, Observatory, Cape Town, 7925",
            latitude = -33.9467,
            longitude = 18.4644,
            facilityType = FacilityType.NATIONAL_HOSPITAL,
            bedCapacity = 938,
            staffCount = 2800,
            patientsToday = Random.nextInt(400, 600),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + Backup",
            specialties = listOf("Heart Transplant", "Neurosurgery", "Trauma", "Oncology", "Nephrology"),
            emergencyServices = true,
            phoneNumber = "021 404 9111",
            hasAmbulance = true,
            hasHelicopter = true
        ),
        
        HealthcareFacility(
            id = "za_wc_tygerberg_001",
            name = "Tygerberg Hospital",
            province = "Western Cape",
            city = "Cape Town",
            address = "Francie van Zijl Dr, Tygerberg, Cape Town, 7505",
            latitude = -33.8931,
            longitude = 18.6319,
            facilityType = FacilityType.PROVINCIAL_HOSPITAL,
            bedCapacity = 1380,
            staffCount = 4200,
            patientsToday = Random.nextInt(500, 750),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + Wind + Backup",
            specialties = listOf("Emergency Medicine", "Trauma Surgery", "Burns Unit", "Pediatric Surgery"),
            emergencyServices = true,
            phoneNumber = "021 938 4911",
            hasAmbulance = true,
            hasHelicopter = true
        ),
        
        HealthcareFacility(
            id = "za_wc_rwh_001",
            name = "Red Cross War Memorial Children's Hospital",
            province = "Western Cape",
            city = "Cape Town",
            address = "Klipfontein Rd, Rondebosch, Cape Town, 7700",
            latitude = -33.9731,
            longitude = 18.4731,
            facilityType = FacilityType.SPECIALIZED_HOSPITAL,
            bedCapacity = 287,
            staffCount = 1100,
            patientsToday = Random.nextInt(80, 150),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + Backup",
            specialties = listOf("Pediatric Surgery", "Pediatric Oncology", "Neonatology", "Pediatric Cardiology"),
            emergencyServices = true,
            phoneNumber = "021 658 5111",
            hasAmbulance = true
        ),
        
        // Cape Town Private Hospitals
        HealthcareFacility(
            id = "za_wc_christiaan_001",
            name = "Christiaan Barnard Memorial Hospital",
            province = "Western Cape",
            city = "Cape Town",
            address = "181 Longmarket St, Cape Town City Centre, 8001",
            latitude = -33.9190,
            longitude = 18.4197,
            facilityType = FacilityType.PRIVATE_HOSPITAL,
            bedCapacity = 170,
            staffCount = 650,
            patientsToday = Random.nextInt(50, 90),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + UPS",
            specialties = listOf("Cardiac Surgery", "Cardiology", "Emergency Medicine", "General Surgery"),
            emergencyServices = true,
            phoneNumber = "021 480 6111"
        ),
        
        HealthcareFacility(
            id = "za_wc_constantia_001",
            name = "Constantiaberg Mediclinic",
            province = "Western Cape",
            city = "Cape Town",
            address = "Burnham Rd, Plumstead, Cape Town, 7800",
            latitude = -34.0167,
            longitude = 18.4500,
            facilityType = FacilityType.PRIVATE_HOSPITAL,
            bedCapacity = 266,
            staffCount = 800,
            patientsToday = Random.nextInt(80, 130),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + Backup",
            specialties = listOf("Maternity", "Emergency Medicine", "General Surgery", "Orthopedics"),
            emergencyServices = true,
            phoneNumber = "021 799 2911"
        ),
        
        // Western Cape Rural Hospitals
        HealthcareFacility(
            id = "za_wc_george_001",
            name = "George Hospital",
            province = "Western Cape",
            city = "George",
            address = "Knysna Rd, Blanco, George, 6529",
            latitude = -33.9608,
            longitude = 22.4614,
            facilityType = FacilityType.REGIONAL_HOSPITAL,
            bedCapacity = 318,
            staffCount = 890,
            patientsToday = Random.nextInt(120, 200),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + Backup",
            specialties = listOf("Emergency Medicine", "General Surgery", "Internal Medicine", "Obstetrics"),
            emergencyServices = true,
            phoneNumber = "044 802 3111",
            hasAmbulance = true
        ),
        
        HealthcareFacility(
            id = "za_wc_worcester_001",
            name = "Worcester Hospital",
            province = "Western Cape",
            city = "Worcester",
            address = "27 Fairbairn St, Worcester, 6850",
            latitude = -33.6464,
            longitude = 19.4419,
            facilityType = FacilityType.REGIONAL_HOSPITAL,
            bedCapacity = 258,
            staffCount = 650,
            patientsToday = Random.nextInt(100, 170),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Backup",
            specialties = listOf("Emergency Medicine", "General Surgery", "Internal Medicine"),
            emergencyServices = true,
            phoneNumber = "023 348 8000",
            hasAmbulance = true
        ),
        
        // =================== KWAZULU-NATAL PROVINCE ===================
        
        // Durban Major Hospitals
        HealthcareFacility(
            id = "za_kzn_inkosi_001",
            name = "Inkosi Albert Luthuli Central Hospital",
            province = "KwaZulu-Natal",
            city = "Durban",
            address = "800 Bellair Rd, Cato Manor, Durban, 4091",
            latitude = -29.8292,
            longitude = 30.9708,
            facilityType = FacilityType.NATIONAL_HOSPITAL,
            bedCapacity = 846,
            staffCount = 2500,
            patientsToday = Random.nextInt(350, 550),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + Backup",
            specialties = listOf("Cardiac Surgery", "Neurosurgery", "Transplant Surgery", "Oncology", "Trauma"),
            emergencyServices = true,
            phoneNumber = "031 240 1111",
            hasAmbulance = true,
            hasHelicopter = true
        ),
        
        HealthcareFacility(
            id = "za_kzn_addington_001",
            name = "Addington Hospital",
            province = "KwaZulu-Natal",
            city = "Durban",
            address = "Erskine Terrace, South Beach, Durban, 4001",
            latitude = -29.8750,
            longitude = 31.0167,
            facilityType = FacilityType.PROVINCIAL_HOSPITAL,
            bedCapacity = 492,
            staffCount = 1400,
            patientsToday = Random.nextInt(200, 350),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Backup",
            specialties = listOf("Emergency Medicine", "Trauma Surgery", "Burns Unit", "General Surgery"),
            emergencyServices = true,
            phoneNumber = "031 327 2000",
            hasAmbulance = true
        ),
        
        HealthcareFacility(
            id = "za_kzn_king_edward_001",
            name = "King Edward VIII Hospital",
            province = "KwaZulu-Natal",
            city = "Durban",
            address = "123 Umbilo Rd, Congella, Durban, 4013",
            latitude = -29.8833,
            longitude = 30.9833,
            facilityType = FacilityType.PROVINCIAL_HOSPITAL,
            bedCapacity = 1040,
            staffCount = 2800,
            patientsToday = Random.nextInt(400, 650),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + Backup",
            specialties = listOf("Maternal Health", "Obstetrics", "Neonatology", "Internal Medicine"),
            emergencyServices = true,
            phoneNumber = "031 360 3111",
            hasAmbulance = true
        ),
        
        // Pietermaritzburg
        HealthcareFacility(
            id = "za_kzn_grey_001",
            name = "Grey's Hospital",
            province = "KwaZulu-Natal",
            city = "Pietermaritzburg",
            address = "Town Bush Rd, Pietermaritzburg, 3201",
            latitude = -29.6000,
            longitude = 30.3833,
            facilityType = FacilityType.REGIONAL_HOSPITAL,
            bedCapacity = 864,
            staffCount = 2200,
            patientsToday = Random.nextInt(300, 500),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Backup",
            specialties = listOf("Emergency Medicine", "Surgery", "Internal Medicine", "Pediatrics"),
            emergencyServices = true,
            phoneNumber = "033 395 3111",
            hasAmbulance = true
        ),
        
        // KZN Rural Hospitals
        HealthcareFacility(
            id = "za_kzn_church_001",
            name = "Church of Scotland Hospital",
            province = "KwaZulu-Natal",
            city = "Tugela Ferry",
            address = "R103, Tugela Ferry, 3010",
            latitude = -28.7333,
            longitude = 29.8167,
            facilityType = FacilityType.DISTRICT_HOSPITAL,
            bedCapacity = 355,
            staffCount = 450,
            patientsToday = Random.nextInt(150, 250),
            status = ClinicStatus.ONLINE,
            powerStatus = "Solar + Backup Generator",
            specialties = listOf("General Medicine", "Surgery", "TB Treatment", "HIV Care"),
            emergencyServices = true,
            phoneNumber = "036 334 0041",
            hasAmbulance = true,
            operatingHours = "24/7",
            languages = listOf("English", "Zulu", "Afrikaans")
        ),
        
        // =================== EASTERN CAPE PROVINCE ===================
        
        // Port Elizabeth/Gqeberha
        HealthcareFacility(
            id = "za_ec_livingstone_001",
            name = "Livingstone Hospital",
            province = "Eastern Cape",
            city = "Gqeberha",
            address = "Stanford Rd, Korsten, Gqeberha, 6020",
            latitude = -33.9500,
            longitude = 25.6167,
            facilityType = FacilityType.PROVINCIAL_HOSPITAL,
            bedCapacity = 847,
            staffCount = 2100,
            patientsToday = Random.nextInt(300, 500),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + Backup",
            specialties = listOf("Emergency Medicine", "Surgery", "Internal Medicine", "Pediatrics"),
            emergencyServices = true,
            phoneNumber = "041 405 2911",
            hasAmbulance = true
        ),
        
        // East London
        HealthcareFacility(
            id = "za_ec_frere_001",
            name = "Frere Hospital",
            province = "Eastern Cape",
            city = "East London",
            address = "Amalinda Main Rd, Amalinda, East London, 5247",
            latitude = -32.9833,
            longitude = 27.8667,
            facilityType = FacilityType.REGIONAL_HOSPITAL,
            bedCapacity = 512,
            staffCount = 1300,
            patientsToday = Random.nextInt(200, 350),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Backup",
            specialties = listOf("Emergency Medicine", "Surgery", "Internal Medicine", "Trauma"),
            emergencyServices = true,
            phoneNumber = "043 709 2000",
            hasAmbulance = true
        ),
        
        // Rural Eastern Cape
        HealthcareFacility(
            id = "za_ec_holy_cross_001",
            name = "Holy Cross Hospital",
            province = "Eastern Cape",
            city = "Flagstaff",
            address = "R61, Flagstaff, 4810",
            latitude = -31.0167,
            longitude = 29.4833,
            facilityType = FacilityType.DISTRICT_HOSPITAL,
            bedCapacity = 146,
            staffCount = 185,
            patientsToday = Random.nextInt(60, 120),
            status = ClinicStatus.ONLINE,
            powerStatus = "Solar + Generator",
            specialties = listOf("General Medicine", "Maternal Health", "TB Treatment"),
            emergencyServices = false,
            phoneNumber = "039 252 0011",
            operatingHours = "07:30-16:00",
            languages = listOf("English", "Xhosa", "Zulu")
        ),
        
        // =================== FREE STATE PROVINCE ===================
        
        // Bloemfontein
        HealthcareFacility(
            id = "za_fs_universitas_001",
            name = "Universitas Academic Hospital",
            province = "Free State",
            city = "Bloemfontein",
            address = "1 Logeman St, Universitas, Bloemfontein, 9301",
            latitude = -29.1500,
            longitude = 26.2167,
            facilityType = FacilityType.NATIONAL_HOSPITAL,
            bedCapacity = 550,
            staffCount = 1650,
            patientsToday = Random.nextInt(220, 380),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + Backup",
            specialties = listOf("Emergency Medicine", "Surgery", "Internal Medicine", "Neurology"),
            emergencyServices = true,
            phoneNumber = "051 405 3111",
            hasAmbulance = true,
            hasHelicopter = true
        ),
        
        HealthcareFacility(
            id = "za_fs_national_001",
            name = "National Hospital",
            province = "Free State",
            city = "Bloemfontein",
            address = "36 Kellner St, Bloemfontein Central, 9300",
            latitude = -29.1167,
            longitude = 26.2000,
            facilityType = FacilityType.PROVINCIAL_HOSPITAL,
            bedCapacity = 284,
            staffCount = 680,
            patientsToday = Random.nextInt(120, 200),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Backup",
            specialties = listOf("General Medicine", "Surgery", "Pediatrics"),
            emergencyServices = true,
            phoneNumber = "051 405 8111",
            hasAmbulance = true
        ),
        
        // =================== LIMPOPO PROVINCE ===================
        
        // Polokwane
        HealthcareFacility(
            id = "za_lp_polokwane_001",
            name = "Polokwane Hospital",
            province = "Limpopo",
            city = "Polokwane",
            address = "Cnr Hospital & Biccard St, Polokwane, 0700",
            latitude = -23.9000,
            longitude = 29.4667,
            facilityType = FacilityType.PROVINCIAL_HOSPITAL,
            bedCapacity = 512,
            staffCount = 1200,
            patientsToday = Random.nextInt(200, 350),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + Backup",
            specialties = listOf("Emergency Medicine", "Surgery", "Internal Medicine", "Malaria Treatment"),
            emergencyServices = true,
            phoneNumber = "015 287 5000",
            hasAmbulance = true,
            languages = listOf("English", "Afrikaans", "Northern Sotho", "Venda", "Tsonga")
        ),
        
        // Rural Limpopo
        HealthcareFacility(
            id = "za_lp_elim_001",
            name = "Elim Hospital",
            province = "Limpopo",
            city = "Elim",
            address = "R578, Elim, 0960",
            latitude = -23.1333,
            longitude = 30.2167,
            facilityType = FacilityType.DISTRICT_HOSPITAL,
            bedCapacity = 422,
            staffCount = 520,
            patientsToday = Random.nextInt(180, 280),
            status = ClinicStatus.ONLINE,
            powerStatus = "Solar + Generator",
            specialties = listOf("General Medicine", "Surgery", "TB Treatment", "Malaria Treatment"),
            emergencyServices = true,
            phoneNumber = "015 556 5000",
            hasAmbulance = true,
            languages = listOf("English", "Tsonga", "Venda", "Northern Sotho")
        ),
        
        // =================== MPUMALANGA PROVINCE ===================
        
        // Mbombela (Nelspruit)
        HealthcareFacility(
            id = "za_mp_mbombela_001",
            name = "Mbombela Hospital",
            province = "Mpumalanga",
            city = "Mbombela",
            address = "Cnr Bitterbessie & Oosthuizen St, Nelspruit, 1200",
            latitude = -25.4753,
            longitude = 30.9694,
            facilityType = FacilityType.REGIONAL_HOSPITAL,
            bedCapacity = 378,
            staffCount = 890,
            patientsToday = Random.nextInt(150, 280),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + Backup",
            specialties = listOf("Emergency Medicine", "Surgery", "Malaria Treatment", "TB Treatment"),
            emergencyServices = true,
            phoneNumber = "013 759 0111",
            hasAmbulance = true,
            languages = listOf("English", "Afrikaans", "Swazi", "Zulu", "Tsonga")
        ),
        
        // Witbank/eMalahleni
        HealthcareFacility(
            id = "za_mp_witbank_001",
            name = "Witbank Hospital",
            province = "Mpumalanga",
            city = "eMalahleni",
            address = "Beatty Ave, eMalahleni, 1035",
            latitude = -25.8667,
            longitude = 29.2333,
            facilityType = FacilityType.DISTRICT_HOSPITAL,
            bedCapacity = 285,
            staffCount = 580,
            patientsToday = Random.nextInt(120, 220),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Backup",
            specialties = listOf("General Medicine", "Surgery", "Occupational Health", "Respiratory Medicine"),
            emergencyServices = true,
            phoneNumber = "013 650 0169",
            hasAmbulance = true
        ),
        
        // =================== NORTH WEST PROVINCE ===================
        
        // Mahikeng
        HealthcareFacility(
            id = "za_nw_mahikeng_001",
            name = "Mahikeng Provincial Hospital",
            province = "North West",
            city = "Mahikeng",
            address = "Golf Course Rd, Mahikeng, 2745",
            latitude = -25.8500,
            longitude = 25.6500,
            facilityType = FacilityType.PROVINCIAL_HOSPITAL,
            bedCapacity = 512,
            staffCount = 920,
            patientsToday = Random.nextInt(180, 300),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + Backup",
            specialties = listOf("Emergency Medicine", "Surgery", "Internal Medicine", "Pediatrics"),
            emergencyServices = true,
            phoneNumber = "018 381 1221",
            hasAmbulance = true,
            languages = listOf("English", "Afrikaans", "Tswana", "Sotho")
        ),
        
        // Klerksdorp
        HealthcareFacility(
            id = "za_nw_klerksdorp_001",
            name = "Klerksdorp Hospital",
            province = "North West",
            city = "Klerksdorp",
            address = "Hospital St, Klerksdorp, 2571",
            latitude = -26.8500,
            longitude = 26.6667,
            facilityType = FacilityType.DISTRICT_HOSPITAL,
            bedCapacity = 445,
            staffCount = 750,
            patientsToday = Random.nextInt(150, 280),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Backup",
            specialties = listOf("General Medicine", "Surgery", "Occupational Health", "Mining Medicine"),
            emergencyServices = true,
            phoneNumber = "018 406 8000",
            hasAmbulance = true
        ),
        
        // =================== NORTHERN CAPE PROVINCE ===================
        
        // Kimberley
        HealthcareFacility(
            id = "za_nc_kimberley_001",
            name = "Kimberley Hospital Complex",
            province = "Northern Cape",
            city = "Kimberley",
            address = "Hanover Rd, Kimberley, 8301",
            latitude = -28.7500,
            longitude = 24.7833,
            facilityType = FacilityType.PROVINCIAL_HOSPITAL,
            bedCapacity = 798,
            staffCount = 1400,
            patientsToday = Random.nextInt(250, 400),
            status = ClinicStatus.ONLINE,
            powerStatus = "Grid + Solar + Backup",
            specialties = listOf("Emergency Medicine", "Surgery", "Internal Medicine", "Pediatrics"),
            emergencyServices = true,
            phoneNumber = "053 802 2111",
            hasAmbulance = true,
            hasHelicopter = false,
            languages = listOf("English", "Afrikaans", "Tswana", "Xhosa")
        ),
        
        // Upington
        HealthcareFacility(
            id = "za_nc_upington_001",
            name = "Upington Provincial Hospital",
            province = "Northern Cape",
            city = "Upington",
            address = "Mutual St, Upington, 8800",
            latitude = -28.4167,
            longitude = 21.2500,
            facilityType = FacilityType.DISTRICT_HOSPITAL,
            bedCapacity = 258,
            staffCount = 420,
            patientsToday = Random.nextInt(100, 180),
            status = ClinicStatus.ONLINE,
            powerStatus = "Solar + Grid + Backup",
            specialties = listOf("General Medicine", "Surgery", "Pediatrics"),
            emergencyServices = true,
            phoneNumber = "054 338 6911",
            hasAmbulance = true,
            languages = listOf("English", "Afrikaans", "Tswana")
        )
    )
    
    /**
     * Get facilities by province
     */
    fun getFacilitiesByProvince(province: String): List<HealthcareFacility> {
        return getAllFacilities().filter { it.province == province }
    }
    
    /**
     * Get facilities by facility type
     */
    fun getFacilitiesByType(type: FacilityType): List<HealthcareFacility> {
        return getAllFacilities().filter { it.facilityType == type }
    }
    
    /**
     * Get all provinces
     */
    fun getProvinces(): List<String> {
        return getAllFacilities().map { it.province }.distinct().sorted()
    }
    
    /**
     * Get facilities with emergency services
     */
    fun getEmergencyFacilities(): List<HealthcareFacility> {
        return getAllFacilities().filter { it.emergencyServices }
    }
    
    /**
     * Get facilities with helicopter services
     */
    fun getHelicopterFacilities(): List<HealthcareFacility> {
        return getAllFacilities().filter { it.hasHelicopter }
    }
    
    /**
     * Convert HealthcareFacility to legacy Clinic format
     */
    fun toClinic(facility: HealthcareFacility): Clinic {
        return Clinic(
            id = facility.id,
            name = facility.name,
            patientsToday = facility.patientsToday,
            staffCount = facility.staffCount,
            powerStatus = facility.powerStatus,
            status = facility.status,
            latitude = facility.latitude,
            longitude = facility.longitude,
            province = facility.province,
            address = facility.address
        )
    }
    
    /**
     * Get all facilities as legacy Clinic objects
     */
    fun getAllFacilitiesAsClinic(): List<Clinic> {
        return getAllFacilities().map { toClinic(it) }
    }
}