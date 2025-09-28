package com.example.medigrid.ai

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Comprehensive South African Disease Database and AI Knowledge Base
 * 
 * This database contains:
 * - Endemic diseases specific to South Africa
 * - Common conditions in SA healthcare settings
 * - Regional disease patterns and risk factors
 * - Traditional medicine interactions
 * - Resource-limited healthcare considerations
 * - Multi-language symptom descriptions
 */

data class SADisease(
    val id: String,
    val name: String,
    val nameAfrikaans: String = "",
    val nameZulu: String = "",
    val nameXhosa: String = "",
    val category: DiseaseCategory,
    val prevalence: Prevalence,
    val region: SARegion,
    val symptoms: List<Symptom>,
    val riskFactors: List<String>,
    val complications: List<String>,
    val diagnosticApproach: List<String>,
    val treatmentApproach: List<String>,
    val preventionMeasures: List<String>,
    val emergencyIndicators: List<String> = emptyList(),
    val traditionalRemedies: List<TraditionalRemedy> = emptyList(),
    val resourceRequirements: ResourceLevel,
    val referralCriteria: List<String> = emptyList(),
    val epidemiologyNotes: String = "",
    val culturalConsiderations: List<String> = emptyList(),
    val icon: ImageVector = Icons.Default.LocalHospital
)

data class Symptom(
    val name: String,
    val severity: SymptomSeverity,
    val frequency: SymptomFrequency,
    val description: String,
    val associatedFindings: List<String> = emptyList(),
    val differentialImportance: Float = 0.5f // 0.0-1.0 for AI weighting
)

data class TraditionalRemedy(
    val name: String,
    val description: String,
    val interactions: List<String> = emptyList(),
    val safetyNotes: String = ""
)

enum class DiseaseCategory(val displayName: String, val icon: ImageVector) {
    INFECTIOUS_ENDEMIC("Endemic Infections", Icons.Default.Coronavirus),
    INFECTIOUS_COMMON("Common Infections", Icons.Default.Healing),
    NON_COMMUNICABLE("Non-Communicable", Icons.Default.MonitorHeart),
    TROPICAL_PARASITIC("Tropical & Parasitic", Icons.Default.BugReport),
    NUTRITIONAL("Nutritional Disorders", Icons.Default.Restaurant),
    ENVIRONMENTAL("Environmental Health", Icons.Default.Air),
    MATERNAL_CHILD("Maternal & Child Health", Icons.Default.Person),
    MENTAL_HEALTH("Mental Health", Icons.Default.Psychology),
    OCCUPATIONAL("Occupational Health", Icons.Default.Work),
    EMERGENCY_TRAUMA("Emergency & Trauma", Icons.Default.Emergency)
}

enum class Prevalence {
    VERY_HIGH,    // >50% population exposure risk
    HIGH,         // 20-50% population exposure risk
    MODERATE,     // 5-20% population exposure risk
    LOW,          // 1-5% population exposure risk
    RARE          // <1% population exposure risk
}

enum class SARegion {
    NATIONAL,           // Nationwide distribution (default value)
    COASTAL,            // Coastal provinces (KZN, Western Cape, Eastern Cape)
    INLAND,             // Inland provinces (Gauteng, Free State, North West)
    NORTHERN,           // Limpopo, Mpumalanga
    RURAL,              // Rural areas nationwide
    URBAN,              // Urban areas nationwide
    MINING,             // Mining regions
    TROPICAL_NORTHEAST  // KZN, Mpumalanga lowveld
}

enum class SymptomSeverity { MILD, MODERATE, SEVERE, CRITICAL }
enum class SymptomFrequency { RARE, OCCASIONAL, COMMON, VERY_COMMON, UNIVERSAL }
enum class ResourceLevel { PRIMARY, SECONDARY, TERTIARY, QUATERNARY }

class SouthAfricanDiseaseDatabase private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: SouthAfricanDiseaseDatabase? = null
        
        fun getInstance(context: Context): SouthAfricanDiseaseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SouthAfricanDiseaseDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val diseases: List<SADisease> by lazy { initializeDiseaseDatabase() }
    
    /**
     * AI-powered symptom matching for South African diseases
     */
    suspend fun analyzeSymptoms(
        symptoms: List<String>,
        patientAge: Int? = null,
        patientSex: String? = null,
        region: SARegion = SARegion.NATIONAL,
        riskFactors: List<String> = emptyList()
    ): SADiseaseAnalysis = withContext(Dispatchers.Default) {
        
        val matchingDiseases = diseases.map { disease ->
            val score = calculateDiseaseScore(
                disease = disease,
                inputSymptoms = symptoms,
                age = patientAge,
                sex = patientSex,
                region = region,
                riskFactors = riskFactors
            )
            DiseaseMatch(disease, score)
        }.filter { it.matchScore > 0.1f }
         .sortedByDescending { it.matchScore }
         .take(10)
        
        val highRiskMatches = matchingDiseases.filter { 
            it.disease.emergencyIndicators.any { indicator ->
                symptoms.any { symptom -> 
                    symptom.contains(indicator, ignoreCase = true) 
                }
            }
        }
        
        SADiseaseAnalysis(
            primaryMatches = matchingDiseases.take(3),
            allMatches = matchingDiseases,
            emergencyRisk = highRiskMatches.isNotEmpty(),
            recommendations = generateRecommendations(matchingDiseases, region),
            confidenceScore = calculateOverallConfidence(matchingDiseases),
            regionalConsiderations = getRegionalConsiderations(region),
            culturalConsiderations = getCulturalConsiderations(matchingDiseases)
        )
    }
    
    private fun calculateDiseaseScore(
        disease: SADisease,
        inputSymptoms: List<String>,
        age: Int?,
        sex: String?,
        region: SARegion,
        riskFactors: List<String>
    ): Float {
        var score = 0.0f
        var maxPossibleScore = 0.0f
        
        // Symptom matching (60% of score)
        disease.symptoms.forEach { diseaseSymptom ->
            maxPossibleScore += diseaseSymptom.differentialImportance * 0.6f
            
            inputSymptoms.forEach { inputSymptom ->
                if (symptomMatches(inputSymptom, diseaseSymptom.name)) {
                    score += diseaseSymptom.differentialImportance * 0.6f * getSymptomWeight(diseaseSymptom)
                }
            }
        }
        
        // Regional relevance (15% of score)
        maxPossibleScore += 0.15f
        if (disease.region == region || disease.region == SARegion.NATIONAL) {
            score += 0.15f * getRegionalRelevance(disease.region, region)
        }
        
        // Risk factors (15% of score)
        maxPossibleScore += 0.15f
        val matchingRiskFactors = disease.riskFactors.count { diseaseRisk ->
            riskFactors.any { patientRisk -> 
                patientRisk.contains(diseaseRisk, ignoreCase = true) || 
                diseaseRisk.contains(patientRisk, ignoreCase = true)
            }
        }
        if (disease.riskFactors.isNotEmpty()) {
            score += 0.15f * (matchingRiskFactors.toFloat() / disease.riskFactors.size)
        }
        
        // Prevalence weight (10% of score)
        maxPossibleScore += 0.1f
        score += 0.1f * when (disease.prevalence) {
            Prevalence.VERY_HIGH -> 1.0f
            Prevalence.HIGH -> 0.8f
            Prevalence.MODERATE -> 0.6f
            Prevalence.LOW -> 0.4f
            Prevalence.RARE -> 0.2f
        }
        
        return if (maxPossibleScore > 0) score / maxPossibleScore else 0f
    }
    
    private fun symptomMatches(input: String, diseaseSymptom: String): Boolean {
        val inputWords = input.lowercase().split(" ")
        val symptomWords = diseaseSymptom.lowercase().split(" ")
        
        return inputWords.any { inputWord ->
            symptomWords.any { symptomWord ->
                inputWord.contains(symptomWord) || 
                symptomWord.contains(inputWord) ||
                calculateLevenshteinSimilarity(inputWord, symptomWord) > 0.8f
            }
        }
    }
    
    private fun calculateLevenshteinSimilarity(s1: String, s2: String): Float {
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0f
        return 1.0f - (levenshteinDistance(s1, s2).toFloat() / maxLen)
    }
    
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = min(
                    dp[i - 1][j] + 1,
                    min(
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + if (s1[i - 1] == s2[j - 1]) 0 else 1
                    )
                )
            }
        }
        return dp[s1.length][s2.length]
    }
    
    private fun getSymptomWeight(symptom: Symptom): Float {
        return when (symptom.severity) {
            SymptomSeverity.CRITICAL -> 1.0f
            SymptomSeverity.SEVERE -> 0.8f
            SymptomSeverity.MODERATE -> 0.6f
            SymptomSeverity.MILD -> 0.4f
        } * when (symptom.frequency) {
            SymptomFrequency.UNIVERSAL -> 1.0f
            SymptomFrequency.VERY_COMMON -> 0.9f
            SymptomFrequency.COMMON -> 0.7f
            SymptomFrequency.OCCASIONAL -> 0.5f
            SymptomFrequency.RARE -> 0.3f
        }
    }
    
    private fun getRegionalRelevance(diseaseRegion: SARegion, patientRegion: SARegion): Float {
        return when {
            diseaseRegion == patientRegion -> 1.0f
            diseaseRegion == SARegion.NATIONAL -> 0.9f
            patientRegion == SARegion.NATIONAL -> 0.8f
            else -> 0.5f
        }
    }
    
    /**
     * Get diseases by category
     */
    fun getDiseasesByCategory(category: DiseaseCategory): List<SADisease> {
        return diseases.filter { it.category == category }
    }
    
    /**
     * Get diseases by region
     */
    fun getDiseasesByRegion(region: SARegion): List<SADisease> {
        return diseases.filter { it.region == region || it.region == SARegion.NATIONAL }
    }
    
    /**
     * Search diseases by name (supports multiple languages)
     */
    fun searchDiseases(query: String): List<SADisease> {
        val lowercaseQuery = query.lowercase()
        return diseases.filter { disease ->
            disease.name.lowercase().contains(lowercaseQuery) ||
            disease.nameAfrikaans.lowercase().contains(lowercaseQuery) ||
            disease.nameZulu.lowercase().contains(lowercaseQuery) ||
            disease.nameXhosa.lowercase().contains(lowercaseQuery) ||
            disease.symptoms.any { it.name.lowercase().contains(lowercaseQuery) }
        }
    }
    
    private fun generateRecommendations(
        matches: List<DiseaseMatch>,
        region: SARegion
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (matches.isEmpty()) {
            recommendations.add("No specific disease patterns identified")
            recommendations.add("Consider general symptom management")
            recommendations.add("Follow up if symptoms persist or worsen")
            return recommendations
        }
        
        val topMatch = matches.first()
        
        // Emergency recommendations
        if (topMatch.disease.emergencyIndicators.isNotEmpty()) {
            recommendations.add("🚨 Consider emergency evaluation for: ${topMatch.disease.name}")
            recommendations.addAll(topMatch.disease.emergencyIndicators.map { "Watch for: $it" })
        }
        
        // Diagnostic recommendations
        recommendations.add("Consider diagnostic workup for ${topMatch.disease.name}")
        recommendations.addAll(topMatch.disease.diagnosticApproach.take(3))
        
        // Treatment recommendations
        if (topMatch.disease.treatmentApproach.isNotEmpty()) {
            recommendations.add("Initial management approach:")
            recommendations.addAll(topMatch.disease.treatmentApproach.take(2))
        }
        
        // Prevention
        if (topMatch.disease.preventionMeasures.isNotEmpty()) {
            recommendations.add("Prevention measures:")
            recommendations.addAll(topMatch.disease.preventionMeasures.take(2))
        }
        
        // Regional considerations
        recommendations.addAll(getRegionalConsiderations(region))
        
        return recommendations
    }
    
    private fun getRegionalConsiderations(region: SARegion): List<String> {
        return when (region) {
            SARegion.COASTAL -> listOf(
                "Consider coastal endemic diseases",
                "Higher malaria risk in KZN coastal areas",
                "Shark bite protocol if relevant"
            )
            SARegion.NORTHERN -> listOf(
                "Higher malaria transmission risk",
                "Consider tick-borne diseases",
                "Traditional medicine use more common"
            )
            SARegion.MINING -> listOf(
                "Consider occupational lung diseases",
                "Higher TB and silicosis prevalence",
                "Noise-induced hearing loss screening"
            )
            SARegion.RURAL -> listOf(
                "Limited diagnostic resources available",
                "Consider transport challenges for referral",
                "Traditional healers often first contact"
            )
            SARegion.URBAN -> listOf(
                "Higher HIV/TB prevalence",
                "Air pollution related conditions",
                "Lifestyle disease prevalence higher"
            )
            else -> listOf("Standard South African disease patterns apply")
        }
    }
    
    private fun getCulturalConsiderations(matches: List<DiseaseMatch>): List<String> {
        val considerations = mutableSetOf<String>()
        
        matches.forEach { match ->
            considerations.addAll(match.disease.culturalConsiderations)
        }
        
        if (considerations.isEmpty()) {
            considerations.addAll(listOf(
                "Consider patient's cultural background and beliefs",
                "Assess traditional medicine use",
                "Ensure culturally sensitive communication"
            ))
        }
        
        return considerations.toList()
    }
    
    private fun calculateOverallConfidence(matches: List<DiseaseMatch>): Float {
        if (matches.isEmpty()) return 0.0f
        
        val topScore = matches.first().matchScore
        val scoreSpread = if (matches.size > 1) {
            topScore - matches[1].matchScore
        } else {
            topScore
        }
        
        return min(topScore + scoreSpread * 0.3f, 1.0f)
    }
    
    /**
     * Initialize the comprehensive South African disease database
     */
    private fun initializeDiseaseDatabase(): List<SADisease> = listOf(
        
        // ENDEMIC INFECTIOUS DISEASES
        
        SADisease(
            id = "tuberculosis_pulmonary",
            name = "Pulmonary Tuberculosis",
            nameAfrikaans = "Longtuberkulose",
            nameZulu = "Isifo sephapha",
            nameXhosa = "Isifo sephapha",
            category = DiseaseCategory.INFECTIOUS_ENDEMIC,
            prevalence = Prevalence.VERY_HIGH,
            region = SARegion.NATIONAL,
            symptoms = listOf(
                Symptom("Persistent cough", SymptomSeverity.MODERATE, SymptomFrequency.UNIVERSAL, "Cough lasting >2 weeks", differentialImportance = 0.9f),
                Symptom("Fever", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "Usually evening fever with sweats", differentialImportance = 0.7f),
                Symptom("Weight loss", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "Unintentional weight loss", differentialImportance = 0.8f),
                Symptom("Night sweats", SymptomSeverity.MILD, SymptomFrequency.VERY_COMMON, "Drenching night sweats", differentialImportance = 0.6f),
                Symptom("Chest pain", SymptomSeverity.MILD, SymptomFrequency.COMMON, "Chest pain with breathing", differentialImportance = 0.5f),
                Symptom("Hemoptysis", SymptomSeverity.SEVERE, SymptomFrequency.OCCASIONAL, "Coughing up blood", differentialImportance = 0.95f)
            ),
            riskFactors = listOf("HIV positive", "Malnutrition", "Diabetes", "Smoking", "Alcohol abuse", "Overcrowding", "Previous TB", "Immunosuppression"),
            complications = listOf("Pleural effusion", "Miliary TB", "TB meningitis", "Drug resistance", "Respiratory failure"),
            diagnosticApproach = listOf(
                "Sputum microscopy for AFB (2-3 specimens)",
                "Chest X-ray",
                "GeneXpert MTB/RIF test",
                "HIV testing (mandatory)",
                "Sputum culture if available",
                "Contact tracing"
            ),
            treatmentApproach = listOf(
                "DOTS (Directly Observed Treatment Short-course)",
                "Standard regimen: HRZE for 2 months, then HR for 4 months",
                "Daily treatment preferred",
                "Monitor for drug resistance",
                "Treat HIV co-infection",
                "Nutritional support"
            ),
            preventionMeasures = listOf(
                "BCG vaccination",
                "Contact screening",
                "Infection control measures",
                "Improve ventilation",
                "Address HIV epidemic",
                "Nutritional support",
                "Reduce overcrowding"
            ),
            emergencyIndicators = listOf(
                "Massive hemoptysis",
                "Respiratory distress",
                "Altered mental state",
                "High fever with rigors"
            ),
            traditionalRemedies = listOf(
                TraditionalRemedy(
                    "African potato (Hypoxis hemerocallidea)",
                    "Used as immune booster",
                    interactions = listOf("May interact with antiretrovirals"),
                    safetyNotes = "Monitor for hepatotoxicity"
                )
            ),
            resourceRequirements = ResourceLevel.PRIMARY,
            referralCriteria = listOf(
                "Drug-resistant TB suspected",
                "Treatment failure",
                "Severe complications",
                "Extrapulmonary TB"
            ),
            epidemiologyNotes = "SA has one of highest TB incidence rates globally. 60% co-infected with HIV.",
            culturalConsiderations = listOf(
                "TB stigma affects treatment compliance",
                "Traditional healers often first contact",
                "Family support crucial for DOTS success",
                "Language barriers in health education"
            ),
            icon = Icons.Default.Coronavirus
        ),
        
        SADisease(
            id = "hiv_aids",
            name = "HIV/AIDS",
            nameAfrikaans = "MIV/VIGS",
            nameZulu = "Igciwane",
            nameXhosa = "Igciwane",
            category = DiseaseCategory.INFECTIOUS_ENDEMIC,
            prevalence = Prevalence.VERY_HIGH,
            region = SARegion.NATIONAL,
            symptoms = listOf(
                Symptom("Recurrent infections", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "Frequent bacterial, viral, fungal infections", differentialImportance = 0.8f),
                Symptom("Weight loss", SymptomSeverity.SEVERE, SymptomFrequency.VERY_COMMON, "Significant unintentional weight loss", differentialImportance = 0.7f),
                Symptom("Persistent diarrhea", SymptomSeverity.MODERATE, SymptomFrequency.COMMON, "Chronic diarrhea >1 month", differentialImportance = 0.6f),
                Symptom("Oral thrush", SymptomSeverity.MILD, SymptomFrequency.VERY_COMMON, "Candidal infections in mouth", differentialImportance = 0.7f),
                Symptom("Skin rash", SymptomSeverity.MILD, SymptomFrequency.COMMON, "Various skin manifestations", differentialImportance = 0.4f),
                Symptom("Swollen lymph nodes", SymptomSeverity.MILD, SymptomFrequency.COMMON, "Generalized lymphadenopathy", differentialImportance = 0.5f)
            ),
            riskFactors = listOf("Unprotected sex", "Multiple partners", "STI history", "IV drug use", "Mother-to-child transmission", "Blood transfusion", "Occupational exposure"),
            complications = listOf("Opportunistic infections", "Kaposi's sarcoma", "Lymphoma", "HIV encephalopathy", "Wasting syndrome", "Death"),
            diagnosticApproach = listOf(
                "HIV rapid test",
                "ELISA confirmation",
                "CD4 count",
                "Viral load",
                "STI screening",
                "TB screening",
                "Hepatitis B co-infection testing"
            ),
            treatmentApproach = listOf(
                "Antiretroviral therapy (ART) - treat all HIV+",
                "Standard regimen: TDF+3TC+EFV or DTG-based",
                "Cotrimoxazole prophylaxis if CD4<200",
                "TB treatment if co-infected",
                "Adherence counseling",
                "Regular monitoring"
            ),
            preventionMeasures = listOf(
                "Safe sex practices",
                "Male circumcision",
                "PrEP for high-risk individuals",
                "PMTCT programs",
                "Blood safety",
                "Universal precautions",
                "Needle exchange programs"
            ),
            emergencyIndicators = listOf(
                "Severe opportunistic infection",
                "CNS involvement",
                "Severe wasting",
                "Respiratory distress"
            ),
            traditionalRemedies = listOf(
                TraditionalRemedy(
                    "Sutherlandia (Lessertia frutescens)",
                    "Used as immune booster",
                    interactions = listOf("May affect ART levels"),
                    safetyNotes = "Use with caution, inform healthcare provider"
                )
            ),
            resourceRequirements = ResourceLevel.PRIMARY,
            referralCriteria = listOf(
                "AIDS-defining illness",
                "Treatment failure",
                "Drug resistance",
                "Pregnancy"
            ),
            epidemiologyNotes = "SA has largest HIV program globally. Prevalence ~19% in adults, higher in women.",
            culturalConsiderations = listOf(
                "Significant stigma and discrimination",
                "Gender-based violence increases risk",
                "Traditional beliefs about causation",
                "Disclosure challenges"
            ),
            icon = Icons.Default.Shield
        ),
        
        SADisease(
            id = "malaria_falciparum",
            name = "Falciparum Malaria",
            nameAfrikaans = "Malaria",
            nameZulu = "Umkhuhlane",
            nameXhosa = "Umkhuhlane",
            category = DiseaseCategory.TROPICAL_PARASITIC,
            prevalence = Prevalence.HIGH,
            region = SARegion.NORTHERN,
            symptoms = listOf(
                Symptom("High fever", SymptomSeverity.SEVERE, SymptomFrequency.UNIVERSAL, "Fever >38.5°C, may be cyclical", differentialImportance = 0.95f),
                Symptom("Headache", SymptomSeverity.SEVERE, SymptomFrequency.VERY_COMMON, "Severe frontal headache", differentialImportance = 0.7f),
                Symptom("Chills and rigors", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "Violent shaking chills", differentialImportance = 0.8f),
                Symptom("Nausea and vomiting", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "May lead to dehydration", differentialImportance = 0.6f),
                Symptom("Muscle aches", SymptomSeverity.MODERATE, SymptomFrequency.COMMON, "Generalized myalgia", differentialImportance = 0.5f),
                Symptom("Altered consciousness", SymptomSeverity.CRITICAL, SymptomFrequency.OCCASIONAL, "Cerebral malaria", differentialImportance = 1.0f)
            ),
            riskFactors = listOf("Travel to endemic areas", "Poor housing", "Pregnancy", "Young age", "Immunocompromised", "Rainy season"),
            complications = listOf("Cerebral malaria", "Severe anemia", "Respiratory distress", "Renal failure", "Hypoglycemia", "Death"),
            diagnosticApproach = listOf(
                "Malaria rapid diagnostic test (RDT)",
                "Thick and thin blood smears",
                "Full blood count",
                "Blood glucose",
                "Travel history essential",
                "Repeat tests if negative but high suspicion"
            ),
            treatmentApproach = listOf(
                "Artemether-lumefantrine (Coartem) for uncomplicated cases",
                "Artesunate IV for severe malaria",
                "Supportive care for complications",
                "Monitor blood glucose",
                "Treat anemia if severe"
            ),
            preventionMeasures = listOf(
                "Indoor residual spraying",
                "Long-lasting insecticidal nets",
                "Chemoprophylaxis for travelers",
                "Early diagnosis and treatment",
                "Environmental management"
            ),
            emergencyIndicators = listOf(
                "Altered consciousness",
                "Severe anemia (Hb<5g/dL)",
                "Respiratory distress",
                "Hypoglycemia",
                "Convulsions"
            ),
            resourceRequirements = ResourceLevel.PRIMARY,
            referralCriteria = listOf(
                "Signs of severe malaria",
                "Treatment failure",
                "Complications",
                "Pregnancy with malaria"
            ),
            epidemiologyNotes = "Endemic in low altitude areas of Limpopo, Mpumalanga, and KZN. Peak transmission Oct-May.",
            culturalConsiderations = listOf(
                "Traditional beliefs about fever causation",
                "Use of traditional fever remedies",
                "Seasonal migration patterns affect exposure"
            ),
            icon = Icons.Default.BugReport
        ),

        // NON-COMMUNICABLE DISEASES
        
        SADisease(
            id = "hypertension",
            name = "Hypertension",
            nameAfrikaans = "Hoë bloeddruk",
            nameZulu = "Umfutho wegazi ophezulu",
            nameXhosa = "Umfutho wegazi ophezulu",
            category = DiseaseCategory.NON_COMMUNICABLE,
            prevalence = Prevalence.VERY_HIGH,
            region = SARegion.NATIONAL,
            symptoms = listOf(
                Symptom("Headache", SymptomSeverity.MILD, SymptomFrequency.COMMON, "Usually occipital, morning headaches", differentialImportance = 0.4f),
                Symptom("Dizziness", SymptomSeverity.MILD, SymptomFrequency.COMMON, "Especially on standing", differentialImportance = 0.3f),
                Symptom("Blurred vision", SymptomSeverity.MODERATE, SymptomFrequency.OCCASIONAL, "May indicate retinal changes", differentialImportance = 0.6f),
                Symptom("Chest pain", SymptomSeverity.MODERATE, SymptomFrequency.OCCASIONAL, "May indicate cardiac involvement", differentialImportance = 0.7f),
                Symptom("Shortness of breath", SymptomSeverity.MODERATE, SymptomFrequency.OCCASIONAL, "Exercise intolerance", differentialImportance = 0.6f)
            ),
            riskFactors = listOf("Obesity", "High salt diet", "Physical inactivity", "Smoking", "Alcohol excess", "Stress", "Family history", "Age", "Diabetes"),
            complications = listOf("Stroke", "Heart attack", "Heart failure", "Kidney disease", "Retinal damage", "Peripheral arterial disease"),
            diagnosticApproach = listOf(
                "Multiple BP readings on different occasions",
                "24-hour ambulatory BP monitoring if available",
                "Assess target organ damage",
                "ECG and echocardiogram",
                "Urine protein and kidney function",
                "Lipid profile and blood glucose"
            ),
            treatmentApproach = listOf(
                "Lifestyle modifications first",
                "Start medication if BP >140/90 mmHg",
                "ACE inhibitor or ARB as first line",
                "Add thiazide diuretic or CCB",
                "Aim for BP <130/80 in most patients",
                "Regular monitoring and titration"
            ),
            preventionMeasures = listOf(
                "Reduce salt intake <6g/day",
                "Regular physical activity",
                "Maintain healthy weight",
                "Limit alcohol consumption",
                "Stop smoking",
                "Manage stress",
                "Regular BP screening"
            ),
            emergencyIndicators = listOf(
                "BP >180/120 mmHg with symptoms",
                "Chest pain with high BP",
                "Neurological symptoms",
                "Severe headache"
            ),
            traditionalRemedies = listOf(
                TraditionalRemedy(
                    "Wild garlic (Tulbaghia violacea)",
                    "Used to lower blood pressure",
                    interactions = listOf("May enhance antihypertensive effects"),
                    safetyNotes = "Monitor BP closely if used with medications"
                )
            ),
            resourceRequirements = ResourceLevel.PRIMARY,
            referralCriteria = listOf(
                "Resistant hypertension",
                "Secondary hypertension suspected",
                "Target organ damage",
                "Hypertensive emergency"
            ),
            epidemiologyNotes = "Affects ~30% of SA adults. Higher prevalence in urban areas and certain ethnic groups.",
            culturalConsiderations = listOf(
                "Often asymptomatic leading to poor compliance",
                "Traditional diet high in salt",
                "Cost of medications affects adherence",
                "Family involvement important for lifestyle changes"
            ),
            icon = Icons.Default.MonitorHeart
        ),
        
        SADisease(
            id = "diabetes_type2",
            name = "Type 2 Diabetes Mellitus",
            nameAfrikaans = "Tipe 2 diabetes",
            nameZulu = "Isifo sikashukela",
            nameXhosa = "Isifo sikashukela",
            category = DiseaseCategory.NON_COMMUNICABLE,
            prevalence = Prevalence.HIGH,
            region = SARegion.NATIONAL,
            symptoms = listOf(
                Symptom("Increased urination", SymptomSeverity.MILD, SymptomFrequency.VERY_COMMON, "Polyuria, especially at night", differentialImportance = 0.7f),
                Symptom("Excessive thirst", SymptomSeverity.MILD, SymptomFrequency.VERY_COMMON, "Polydipsia", differentialImportance = 0.7f),
                Symptom("Increased appetite", SymptomSeverity.MILD, SymptomFrequency.COMMON, "Polyphagia with weight loss", differentialImportance = 0.5f),
                Symptom("Fatigue", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "Persistent tiredness", differentialImportance = 0.4f),
                Symptom("Blurred vision", SymptomSeverity.MILD, SymptomFrequency.COMMON, "Due to osmotic lens changes", differentialImportance = 0.6f),
                Symptom("Slow healing wounds", SymptomSeverity.MODERATE, SymptomFrequency.COMMON, "Poor wound healing", differentialImportance = 0.8f)
            ),
            riskFactors = listOf("Obesity", "Physical inactivity", "Family history", "Age >45 years", "Previous gestational diabetes", "Hypertension", "Ethnicity"),
            complications = listOf("Diabetic retinopathy", "Diabetic nephropathy", "Diabetic neuropathy", "Cardiovascular disease", "Diabetic foot", "Ketoacidosis"),
            diagnosticApproach = listOf(
                "Random glucose >11.1 mmol/L with symptoms",
                "Fasting glucose >7.0 mmol/L",
                "HbA1c >6.5% (48 mmol/mol)",
                "Oral glucose tolerance test",
                "Assess for complications",
                "Cardiovascular risk assessment"
            ),
            treatmentApproach = listOf(
                "Lifestyle modification as foundation",
                "Metformin as first-line medication",
                "Glycemic control targets: HbA1c <7%",
                "BP and lipid management",
                "Annual screening for complications",
                "Patient education and self-monitoring"
            ),
            preventionMeasures = listOf(
                "Maintain healthy weight",
                "Regular physical activity",
                "Healthy diet low in refined sugars",
                "Regular screening if high risk",
                "Manage other cardiovascular risk factors"
            ),
            emergencyIndicators = listOf(
                "Severe hyperglycemia >25 mmol/L",
                "Diabetic ketoacidosis",
                "Severe hypoglycemia",
                "Diabetic foot infection"
            ),
            traditionalRemedies = listOf(
                TraditionalRemedy(
                    "Bitter melon (Momordica charantia)",
                    "Traditional glucose-lowering plant",
                    interactions = listOf("May enhance hypoglycemic effects"),
                    safetyNotes = "Monitor blood glucose closely"
                )
            ),
            resourceRequirements = ResourceLevel.PRIMARY,
            referralCriteria = listOf(
                "Difficult glycemic control",
                "Diabetic complications",
                "Type 1 diabetes suspected",
                "Pregnancy with diabetes"
            ),
            epidemiologyNotes = "Prevalence ~7% nationally, higher in urban areas. Often undiagnosed for years.",
            culturalConsiderations = listOf(
                "Traditional diet modification challenging",
                "Family meals important in management",
                "Cost of medications and test strips",
                "Traditional beliefs about sugar consumption"
            ),
            icon = Icons.Default.Bloodtype
        ),

        // RESPIRATORY CONDITIONS
        
        SADisease(
            id = "asthma",
            name = "Bronchial Asthma",
            nameAfrikaans = "Asma",
            nameZulu = "Isifuba",
            nameXhosa = "Isifuba",
            category = DiseaseCategory.NON_COMMUNICABLE,
            prevalence = Prevalence.HIGH,
            region = SARegion.NATIONAL,
            symptoms = listOf(
                Symptom("Wheezing", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "High-pitched whistling sound", differentialImportance = 0.9f),
                Symptom("Shortness of breath", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "Especially with exertion", differentialImportance = 0.8f),
                Symptom("Cough", SymptomSeverity.MILD, SymptomFrequency.VERY_COMMON, "Often dry, worse at night", differentialImportance = 0.6f),
                Symptom("Chest tightness", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "Feeling of pressure in chest", differentialImportance = 0.7f)
            ),
            riskFactors = listOf("Allergies", "Family history", "Smoking exposure", "Air pollution", "Respiratory infections", "Occupational exposures"),
            complications = listOf("Status asthmaticus", "Respiratory failure", "Pneumothorax", "COPD overlap", "Growth retardation in children"),
            diagnosticApproach = listOf(
                "Clinical history and examination",
                "Peak flow monitoring",
                "Spirometry with bronchodilator response",
                "Allergy testing if indicated",
                "Chest X-ray to exclude other causes"
            ),
            treatmentApproach = listOf(
                "Inhaled corticosteroids as controller therapy",
                "Short-acting bronchodilators for relief",
                "Asthma action plan",
                "Trigger avoidance",
                "Regular monitoring and adjustment"
            ),
            preventionMeasures = listOf(
                "Avoid known triggers",
                "Vaccination (influenza, pneumococcal)",
                "Good indoor air quality",
                "Smoking cessation",
                "Allergen control measures"
            ),
            emergencyIndicators = listOf(
                "Severe respiratory distress",
                "Unable to speak in sentences",
                "Cyanosis",
                "Silent chest"
            ),
            resourceRequirements = ResourceLevel.PRIMARY,
            referralCriteria = listOf(
                "Poor asthma control despite treatment",
                "Frequent severe exacerbations",
                "Occupational asthma suspected",
                "Need for specialist assessment"
            ),
            epidemiologyNotes = "High prevalence in SA, especially in urban areas with air pollution. Often underdiagnosed.",
            culturalConsiderations = listOf(
                "Inhaler technique education crucial",
                "Cost of medications affects adherence",
                "Traditional smoke exposure in rural areas",
                "Stigma about chronic disease in children"
            ),
            icon = Icons.Default.Air
        ),

        // INFECTIOUS DISEASES - COMMON
        
        SADisease(
            id = "gastroenteritis_infectious",
            name = "Infectious Gastroenteritis",
            nameAfrikaans = "Maag-en dermontsteking",
            nameZulu = "Ukuhambisa",
            nameXhosa = "Ukuhambisa",
            category = DiseaseCategory.INFECTIOUS_COMMON,
            prevalence = Prevalence.VERY_HIGH,
            region = SARegion.NATIONAL,
            symptoms = listOf(
                Symptom("Diarrhea", SymptomSeverity.MODERATE, SymptomFrequency.UNIVERSAL, "Loose, watery stools", differentialImportance = 0.9f),
                Symptom("Vomiting", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "May lead to dehydration", differentialImportance = 0.7f),
                Symptom("Abdominal pain", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "Cramping pain", differentialImportance = 0.6f),
                Symptom("Fever", SymptomSeverity.MILD, SymptomFrequency.COMMON, "Usually low-grade", differentialImportance = 0.5f),
                Symptom("Dehydration", SymptomSeverity.SEVERE, SymptomFrequency.COMMON, "Signs of fluid loss", differentialImportance = 0.8f)
            ),
            riskFactors = listOf("Poor sanitation", "Contaminated water", "Food handling", "Overcrowding", "Immunocompromised", "Travel"),
            complications = listOf("Severe dehydration", "Electrolyte imbalance", "Shock", "Renal failure", "Death in severe cases"),
            diagnosticApproach = listOf(
                "Clinical assessment of dehydration",
                "Stool microscopy and culture if severe",
                "Electrolytes and kidney function",
                "Blood glucose",
                "Consider parasites in chronic cases"
            ),
            treatmentApproach = listOf(
                "Oral rehydration therapy (ORT)",
                "Continue breastfeeding in infants",
                "Gradual return to normal diet",
                "Zinc supplementation in children",
                "Antibiotics only if indicated",
                "IV fluids if severe dehydration"
            ),
            preventionMeasures = listOf(
                "Safe water and sanitation",
                "Hand hygiene",
                "Food safety practices",
                "Vaccination (rotavirus in children)",
                "Breastfeeding promotion"
            ),
            emergencyIndicators = listOf(
                "Severe dehydration",
                "Altered consciousness",
                "Bloody diarrhea with high fever",
                "Signs of shock"
            ),
            traditionalRemedies = listOf(
                TraditionalRemedy(
                    "Rooibos tea",
                    "Used for stomach upset",
                    interactions = emptyList(),
                    safetyNotes = "Generally safe, but doesn't replace ORT"
                )
            ),
            resourceRequirements = ResourceLevel.PRIMARY,
            referralCriteria = listOf(
                "Severe dehydration requiring IV therapy",
                "Bloody diarrhea with high fever",
                "Immunocompromised patient",
                "Failure to improve with ORT"
            ),
            epidemiologyNotes = "Leading cause of childhood morbidity and mortality. Seasonal peaks in summer.",
            culturalConsiderations = listOf(
                "Traditional remedies often tried first",
                "ORS preparation and usage education needed",
                "Breastfeeding practices vary by culture",
                "Food taboos during illness"
            ),
            icon = Icons.Default.Sick
        ),

        // NUTRITIONAL DISORDERS
        
        SADisease(
            id = "kwashiorkor",
            name = "Kwashiorkor",
            nameAfrikaans = "Kwashiorkor",
            nameZulu = "Kwashiorkor",
            nameXhosa = "Kwashiorkor",
            category = DiseaseCategory.NUTRITIONAL,
            prevalence = Prevalence.MODERATE,
            region = SARegion.RURAL,
            symptoms = listOf(
                Symptom("Edema", SymptomSeverity.SEVERE, SymptomFrequency.UNIVERSAL, "Bilateral pitting edema, especially feet", differentialImportance = 0.95f),
                Symptom("Growth retardation", SymptomSeverity.SEVERE, SymptomFrequency.UNIVERSAL, "Stunted growth", differentialImportance = 0.9f),
                Symptom("Hair changes", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "Sparse, thin, discolored hair", differentialImportance = 0.7f),
                Symptom("Skin changes", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "Hypopigmentation, dermatosis", differentialImportance = 0.6f),
                Symptom("Irritability", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "Behavioral changes", differentialImportance = 0.5f),
                Symptom("Hepatomegaly", SymptomSeverity.MODERATE, SymptomFrequency.COMMON, "Enlarged liver", differentialImportance = 0.8f)
            ),
            riskFactors = listOf("Protein-energy malnutrition", "Poverty", "Food insecurity", "Weaning practices", "Infections", "Poor maternal nutrition"),
            complications = listOf("Severe malnutrition", "Increased infection risk", "Growth failure", "Cognitive impairment", "Death"),
            diagnosticApproach = listOf(
                "Clinical assessment and growth charts",
                "Weight-for-height Z-scores",
                "Mid-upper arm circumference",
                "Serum albumin and total protein",
                "Assess for complications",
                "Micronutrient deficiencies"
            ),
            treatmentApproach = listOf(
                "Therapeutic feeding with F-75 then F-100",
                "Treat medical complications",
                "Antibiotics for infections",
                "Micronutrient supplementation",
                "Gradual nutritional rehabilitation",
                "Family counseling and support"
            ),
            preventionMeasures = listOf(
                "Promote exclusive breastfeeding 0-6 months",
                "Appropriate complementary feeding",
                "Food security programs",
                "Growth monitoring",
                "Treatment of infections",
                "Maternal nutrition programs"
            ),
            emergencyIndicators = listOf(
                "Severe acute malnutrition with complications",
                "Hypoglycemia",
                "Hypothermia",
                "Severe dehydration"
            ),
            resourceRequirements = ResourceLevel.SECONDARY,
            referralCriteria = listOf(
                "Severe acute malnutrition",
                "Medical complications",
                "Failure to respond to treatment",
                "Need for inpatient management"
            ),
            epidemiologyNotes = "More common in rural areas with food insecurity. Often follows infections or poor weaning.",
            culturalConsiderations = listOf(
                "Traditional weaning foods may be inadequate",
                "Stigma around malnutrition",
                "Extended breastfeeding practices",
                "Food taboos affecting protein intake"
            ),
            icon = Icons.Default.Restaurant
        ),

        // ENVIRONMENTAL HEALTH
        
        SADisease(
            id = "pneumoconiosis_silicosis",
            name = "Silicosis",
            nameAfrikaans = "Silicose",
            nameZulu = "Isifo samalotha",
            nameXhosa = "Isifo samalotha",
            category = DiseaseCategory.OCCUPATIONAL,
            prevalence = Prevalence.HIGH,
            region = SARegion.MINING,
            symptoms = listOf(
                Symptom("Progressive shortness of breath", SymptomSeverity.SEVERE, SymptomFrequency.UNIVERSAL, "Exertional dyspnea worsening over time", differentialImportance = 0.9f),
                Symptom("Dry cough", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "Persistent dry cough", differentialImportance = 0.7f),
                Symptom("Chest pain", SymptomSeverity.MILD, SymptomFrequency.COMMON, "Chest tightness or pain", differentialImportance = 0.5f),
                Symptom("Fatigue", SymptomSeverity.MODERATE, SymptomFrequency.COMMON, "Exercise intolerance", differentialImportance = 0.4f),
                Symptom("Weight loss", SymptomSeverity.MODERATE, SymptomFrequency.COMMON, "Unintentional weight loss", differentialImportance = 0.6f)
            ),
            riskFactors = listOf("Mining exposure", "Sandblasting", "Quarry work", "Construction work", "Years of exposure", "Inadequate protection"),
            complications = listOf("Progressive massive fibrosis", "Respiratory failure", "Cor pulmonale", "Increased TB risk", "Lung cancer risk"),
            diagnosticApproach = listOf(
                "Occupational history essential",
                "Chest X-ray (ILO classification)",
                "High-resolution CT scan",
                "Pulmonary function tests",
                "TB screening",
                "Assessment of disability"
            ),
            treatmentApproach = listOf(
                "Remove from further exposure",
                "Supportive care",
                "Bronchodilators if airway obstruction",
                "Oxygen therapy if hypoxic",
                "Treatment of complications",
                "Compensation claims assistance"
            ),
            preventionMeasures = listOf(
                "Dust control measures",
                "Personal protective equipment",
                "Regular health surveillance",
                "Pre-employment screening",
                "Training on dust hazards",
                "Regulatory enforcement"
            ),
            emergencyIndicators = listOf(
                "Acute respiratory failure",
                "Massive hemoptysis",
                "Pneumothorax",
                "Cor pulmonale"
            ),
            resourceRequirements = ResourceLevel.SECONDARY,
            referralCriteria = listOf(
                "Progressive disease",
                "Complications",
                "Compensation assessment needed",
                "Lung transplant evaluation"
            ),
            epidemiologyNotes = "Very high prevalence in SA mining industry. Major occupational health challenge.",
            culturalConsiderations = listOf(
                "Migrant labor patterns affect follow-up",
                "Language barriers in health education",
                "Economic pressures to continue working",
                "Traditional beliefs about lung disease"
            ),
            icon = Icons.Default.Work
        ),

        // MATERNAL AND CHILD HEALTH
        
        SADisease(
            id = "preeclampsia",
            name = "Pre-eclampsia",
            nameAfrikaans = "Pre-eklampsie",
            nameZulu = "Ukuphakama kwegazi ekhulelweni",
            nameXhosa = "Ukuphakama kwegazi ekhulelweni",
            category = DiseaseCategory.MATERNAL_CHILD,
            prevalence = Prevalence.HIGH,
            region = SARegion.NATIONAL,
            symptoms = listOf(
                Symptom("High blood pressure", SymptomSeverity.SEVERE, SymptomFrequency.UNIVERSAL, "BP >140/90 mmHg after 20 weeks", differentialImportance = 0.95f),
                Symptom("Protein in urine", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "Proteinuria >300mg/24hr", differentialImportance = 0.8f),
                Symptom("Headache", SymptomSeverity.SEVERE, SymptomFrequency.COMMON, "Severe persistent headache", differentialImportance = 0.7f),
                Symptom("Visual disturbances", SymptomSeverity.SEVERE, SymptomFrequency.COMMON, "Blurred vision, scotomata", differentialImportance = 0.8f),
                Symptom("Upper abdominal pain", SymptomSeverity.SEVERE, SymptomFrequency.COMMON, "RUQ or epigastric pain", differentialImportance = 0.7f),
                Symptom("Swelling", SymptomSeverity.MILD, SymptomFrequency.COMMON, "Edema, especially face and hands", differentialImportance = 0.4f)
            ),
            riskFactors = listOf("First pregnancy", "Age >35 or <20", "Multiple pregnancy", "Previous pre-eclampsia", "Diabetes", "Hypertension", "Obesity", "Family history"),
            complications = listOf("Eclampsia", "HELLP syndrome", "Placental abruption", "Maternal death", "Fetal growth restriction", "Preterm delivery"),
            diagnosticApproach = listOf(
                "Regular BP monitoring in pregnancy",
                "Urine protein assessment",
                "Full blood count and platelets",
                "Liver function tests",
                "Kidney function tests",
                "Fetal monitoring"
            ),
            treatmentApproach = listOf(
                "Delivery is definitive treatment",
                "Antihypertensive therapy if severe",
                "Magnesium sulfate for seizure prevention",
                "Corticosteroids for fetal lung maturity",
                "Close maternal and fetal monitoring"
            ),
            preventionMeasures = listOf(
                "Low-dose aspirin in high-risk women",
                "Calcium supplementation in low-calcium diets",
                "Regular antenatal care",
                "Early detection and management",
                "Lifestyle modifications"
            ),
            emergencyIndicators = listOf(
                "BP >160/110 mmHg",
                "Severe headache or visual symptoms",
                "Epigastric pain",
                "Decreased urine output",
                "Seizures (eclampsia)"
            ),
            resourceRequirements = ResourceLevel.SECONDARY,
            referralCriteria = listOf(
                "Severe pre-eclampsia",
                "Signs of impending eclampsia",
                "HELLP syndrome",
                "Need for delivery"
            ),
            epidemiologyNotes = "Leading cause of maternal mortality in SA. Higher risk in adolescent and older mothers.",
            culturalConsiderations = listOf(
                "Late antenatal care booking",
                "Traditional beliefs about pregnancy symptoms",
                "Access to healthcare in rural areas",
                "Family decision-making in pregnancy"
            ),
            icon = Icons.Default.Person
        ),

        // MENTAL HEALTH
        
        SADisease(
            id = "depression_major",
            name = "Major Depressive Disorder",
            nameAfrikaans = "Ernstige depressie",
            nameZulu = "Ukudangala okujulile",
            nameXhosa = "Ukudangala okujulile",
            category = DiseaseCategory.MENTAL_HEALTH,
            prevalence = Prevalence.HIGH,
            region = SARegion.NATIONAL,
            symptoms = listOf(
                Symptom("Persistent sadness", SymptomSeverity.MODERATE, SymptomFrequency.UNIVERSAL, "Low mood most days for >2 weeks", differentialImportance = 0.9f),
                Symptom("Loss of interest", SymptomSeverity.MODERATE, SymptomFrequency.UNIVERSAL, "Anhedonia - loss of pleasure", differentialImportance = 0.9f),
                Symptom("Sleep disturbances", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "Insomnia or hypersomnia", differentialImportance = 0.6f),
                Symptom("Fatigue", SymptomSeverity.MODERATE, SymptomFrequency.VERY_COMMON, "Low energy, tiredness", differentialImportance = 0.5f),
                Symptom("Appetite changes", SymptomSeverity.MILD, SymptomFrequency.COMMON, "Weight loss or gain", differentialImportance = 0.4f),
                Symptom("Concentration problems", SymptomSeverity.MODERATE, SymptomFrequency.COMMON, "Difficulty making decisions", differentialImportance = 0.6f),
                Symptom("Suicidal thoughts", SymptomSeverity.CRITICAL, SymptomFrequency.OCCASIONAL, "Thoughts of death or suicide", differentialImportance = 1.0f)
            ),
            riskFactors = listOf("Previous depression", "Family history", "Trauma", "Chronic illness", "Substance abuse", "Social isolation", "Unemployment", "Poverty"),
            complications = listOf("Suicide", "Functional impairment", "Relationship problems", "Work/school problems", "Substance abuse", "Physical health problems"),
            diagnosticApproach = listOf(
                "Clinical interview and mental status exam",
                "Depression screening tools (PHQ-9, BDI)",
                "Rule out medical causes",
                "Assess suicide risk",
                "Substance use assessment",
                "Social support evaluation"
            ),
            treatmentApproach = listOf(
                "Psychotherapy (CBT, IPT)",
                "Antidepressant medication if indicated",
                "Combination therapy for moderate-severe cases",
                "Social support and lifestyle changes",
                "Regular follow-up and monitoring"
            ),
            preventionMeasures = listOf(
                "Early identification and treatment",
                "Stress management techniques",
                "Social support systems",
                "Treatment of substance abuse",
                "Chronic disease management",
                "Community mental health programs"
            ),
            emergencyIndicators = listOf(
                "Active suicidal ideation with plan",
                "Psychotic features",
                "Severe functional impairment",
                "Self-harm behavior"
            ),
            traditionalRemedies = listOf(
                TraditionalRemedy(
                    "Kanna (Sceletium tortuosum)",
                    "Traditional mood enhancer",
                    interactions = listOf("May interact with antidepressants"),
                    safetyNotes = "Consult healthcare provider before use"
                )
            ),
            resourceRequirements = ResourceLevel.PRIMARY,
            referralCriteria = listOf(
                "High suicide risk",
                "Psychotic depression",
                "Treatment-resistant cases",
                "Need for specialized therapy"
            ),
            epidemiologyNotes = "High prevalence in SA linked to socioeconomic factors, trauma, and HIV epidemic.",
            culturalConsiderations = listOf(
                "Stigma around mental health",
                "Traditional explanations for mental illness",
                "Religious and spiritual coping",
                "Extended family support systems",
                "Language barriers in mental health services"
            ),
            icon = Icons.Default.Psychology
        )
    )
}

/**
 * Data classes for AI analysis results
 */
data class SADiseaseAnalysis(
    val primaryMatches: List<DiseaseMatch>,
    val allMatches: List<DiseaseMatch>,
    val emergencyRisk: Boolean,
    val recommendations: List<String>,
    val confidenceScore: Float,
    val regionalConsiderations: List<String>,
    val culturalConsiderations: List<String>
)

data class DiseaseMatch(
    val disease: SADisease,
    val matchScore: Float,
    val matchingSymptoms: List<String> = emptyList(),
    val riskFactorMatches: List<String> = emptyList()
)

/**
 * Extension functions for enhanced functionality
 */
fun SADisease.getLocalizedName(language: String): String {
    return when (language.lowercase()) {
        "af", "afr", "afrikaans" -> nameAfrikaans.takeIf { it.isNotEmpty() } ?: name
        "zu", "zul", "zulu" -> nameZulu.takeIf { it.isNotEmpty() } ?: name
        "xh", "xho", "xhosa" -> nameXhosa.takeIf { it.isNotEmpty() } ?: name
        else -> name
    }
}

fun List<SADisease>.filterByPrevalence(minPrevalence: Prevalence): List<SADisease> {
    val prevalenceOrder = listOf(Prevalence.RARE, Prevalence.LOW, Prevalence.MODERATE, Prevalence.HIGH, Prevalence.VERY_HIGH)
    val minIndex = prevalenceOrder.indexOf(minPrevalence)
    return filter { prevalenceOrder.indexOf(it.prevalence) >= minIndex }
}

fun List<SADisease>.filterByResourceLevel(maxLevel: ResourceLevel): List<SADisease> {
    val levelOrder = listOf(ResourceLevel.PRIMARY, ResourceLevel.SECONDARY, ResourceLevel.TERTIARY, ResourceLevel.QUATERNARY)
    val maxIndex = levelOrder.indexOf(maxLevel)
    return filter { levelOrder.indexOf(it.resourceRequirements) <= maxIndex }
}

/**
 * AI Analysis Service for South African healthcare context
 */
class SAHealthAIAnalyzer private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: SAHealthAIAnalyzer? = null
        
        fun getInstance(context: Context): SAHealthAIAnalyzer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SAHealthAIAnalyzer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val diseaseDatabase = SouthAfricanDiseaseDatabase.getInstance(context)
    
    /**
     * Analyze symptoms and provide comprehensive health assessment
     */
    suspend fun analyzeSymptoms(
        symptoms: List<String>,
        patientAge: Int? = null,
        patientSex: String? = null,
        region: SARegion = SARegion.NATIONAL,
        riskFactors: List<String> = emptyList(),
        responses: Map<String, Any> = emptyMap()
    ): SAHealthAssessment {
        
        // Get disease matches from database
        val diseaseAnalysis = diseaseDatabase.analyzeSymptoms(
            symptoms = symptoms,
            patientAge = patientAge,
            patientSex = patientSex,
            region = region,
            riskFactors = riskFactors
        )
        
        // Convert to enhanced health assessment
        return convertToHealthAssessment(diseaseAnalysis, responses, symptoms)
    }
    
    private fun convertToHealthAssessment(
        diseaseAnalysis: SADiseaseAnalysis,
        responses: Map<String, Any>,
        symptoms: List<String>
    ): SAHealthAssessment {
        
        // Determine urgency level based on emergency indicators
        val urgencyLevel = when {
            diseaseAnalysis.emergencyRisk -> SAUrgencyLevel.EMERGENCY
            diseaseAnalysis.primaryMatches.any { it.matchScore > 0.8f } -> SAUrgencyLevel.URGENT
            diseaseAnalysis.primaryMatches.any { it.matchScore > 0.6f } -> SAUrgencyLevel.SAME_DAY
            else -> SAUrgencyLevel.ROUTINE
        }
        
        // Determine risk level
        val riskLevel = when {
            diseaseAnalysis.emergencyRisk -> SARiskLevel.CRITICAL
            diseaseAnalysis.primaryMatches.any { 
                it.disease.complications.any { comp -> 
                    comp.contains("death", ignoreCase = true) || 
                    comp.contains("failure", ignoreCase = true) 
                }
            } -> SARiskLevel.HIGH
            diseaseAnalysis.primaryMatches.any { it.matchScore > 0.5f } -> SARiskLevel.MODERATE
            else -> SARiskLevel.LOW
        }
        
        // Generate recommendations
        val recommendations = mutableListOf<String>().apply {
            addAll(diseaseAnalysis.recommendations)
            
            // Add SA-specific recommendations
            if (urgencyLevel == SAUrgencyLevel.EMERGENCY) {
                add(0, "🚨 SEEK IMMEDIATE EMERGENCY MEDICAL ATTENTION")
                add(1, "Call 10177 (emergency services) or go to nearest emergency department")
            }
            
            // Add cultural considerations
            addAll(diseaseAnalysis.culturalConsiderations.map { "Cultural note: $it" })
            
            // Add regional considerations  
            addAll(diseaseAnalysis.regionalConsiderations.map { "Regional note: $it" })
        }
        
        // Extract primary concerns from top disease matches
        val primaryConcerns = diseaseAnalysis.primaryMatches.take(3).map { match ->
            "${match.disease.name}: ${match.disease.complications.take(2).joinToString(", ")}"
        }
        
        // Generate diagnostic suggestions from top matches
        val diagnosticSuggestions = diseaseAnalysis.primaryMatches.take(2).flatMap { match ->
            match.disease.diagnosticApproach.take(3)
        }.distinct()
        
        // Identify red flags
        val redFlags = diseaseAnalysis.primaryMatches.flatMap { match ->
            match.disease.emergencyIndicators
        }.distinct()
        
        return SAHealthAssessment(
            riskLevel = riskLevel,
            urgencyLevel = urgencyLevel,
            primaryConcerns = primaryConcerns,
            recommendations = recommendations,
            diagnosticSuggestions = diagnosticSuggestions,
            redFlags = redFlags,
            aiConfidence = diseaseAnalysis.confidenceScore,
            diseaseMatches = diseaseAnalysis.primaryMatches,
            followUpDays = when (urgencyLevel) {
                SAUrgencyLevel.EMERGENCY -> 0
                SAUrgencyLevel.URGENT -> 1
                SAUrgencyLevel.SAME_DAY -> 3
                SAUrgencyLevel.ROUTINE -> 14
            },
            referralNeeded = urgencyLevel != SAUrgencyLevel.ROUTINE,
            culturalConsiderations = diseaseAnalysis.culturalConsiderations,
            regionalConsiderations = diseaseAnalysis.regionalConsiderations
        )
    }
}

/**
 * Enhanced health assessment result for South African context
 */
data class SAHealthAssessment(
    val riskLevel: SARiskLevel,
    val urgencyLevel: SAUrgencyLevel,
    val primaryConcerns: List<String>,
    val recommendations: List<String>,
    val diagnosticSuggestions: List<String>,
    val redFlags: List<String>,
    val aiConfidence: Float,
    val diseaseMatches: List<DiseaseMatch>,
    val followUpDays: Int,
    val referralNeeded: Boolean,
    val culturalConsiderations: List<String>,
    val regionalConsiderations: List<String>
)

enum class SARiskLevel {
    LOW, MODERATE, HIGH, CRITICAL
}

enum class SAUrgencyLevel {
    ROUTINE, SAME_DAY, URGENT, EMERGENCY
}