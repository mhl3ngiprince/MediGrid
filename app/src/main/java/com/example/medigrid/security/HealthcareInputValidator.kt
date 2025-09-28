package com.example.medigrid.security

import java.util.regex.Pattern
import java.text.SimpleDateFormat
import java.util.*

/**
 * Healthcare Input Validator
 * POPIA-compliant input validation for South African healthcare data
 */
object HealthcareInputValidator {

    // South African specific patterns
    private val SA_ID_PATTERN = Pattern.compile("^\\d{13}$")
    private val SA_PHONE_PATTERN = Pattern.compile("^(\\+27|0)[1-9]\\d{8}$")
    private val NAME_PATTERN = Pattern.compile("^[A-Za-z\\s\\-']{2,100}$")
    private val EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")

    // Medical data patterns
    private val MEDICAL_ID_PATTERN = Pattern.compile("^[A-Z]{2}\\d{6,10}$")
    private val MEDICINE_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9\\s\\-().]{2,200}$")
    private val DIAGNOSIS_PATTERN = Pattern.compile("^[A-Za-z0-9\\s\\-.,():/]{5,500}$")

    // Data classification for PHI
    enum class DataClassification {
        PUBLIC,
        INTERNAL,
        CONFIDENTIAL,
        PHI_RESTRICTED
    }

    /**
     * Validation result with details
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: Map<String, String> = emptyMap(),
        val sanitizedData: Map<String, Any> = emptyMap(),
        val dataClassification: DataClassification = DataClassification.PUBLIC,
    )

    /**
     * Validate South African ID number using Luhn algorithm
     */
    fun validateSaIdNumber(idNumber: String): ValidationResult {
        val errors = mutableMapOf<String, String>()

        if (idNumber.isBlank()) {
            errors["id_number"] = "ID number is required"
            return ValidationResult(false, errors)
        }

        if (!SA_ID_PATTERN.matcher(idNumber).matches()) {
            errors["id_number"] = "Invalid SA ID number format (must be 13 digits)"
            return ValidationResult(false, errors)
        }

        // Luhn algorithm validation
        if (!isValidLuhnChecksum(idNumber)) {
            errors["id_number"] = "Invalid SA ID number checksum"
            return ValidationResult(false, errors)
        }

        // Extract date of birth and validate
        val dobString = idNumber.substring(0, 6)
        val year = if (dobString.substring(0, 2).toInt() > 50) {
            "19${dobString.substring(0, 2)}"
        } else {
            "20${dobString.substring(0, 2)}"
        }

        try {
            val dob = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                .parse("$year${dobString.substring(2)}")
            val currentDate = Date()

            if (dob?.after(currentDate) == true) {
                errors["id_number"] = "Date of birth cannot be in the future"
                return ValidationResult(false, errors)
            }

            val age = calculateAge(dob!!)
            if (age > 150) {
                errors["id_number"] = "Invalid age calculated from ID number"
                return ValidationResult(false, errors)
            }

        } catch (e: Exception) {
            errors["id_number"] = "Invalid date in ID number"
            return ValidationResult(false, errors)
        }

        return ValidationResult(
            isValid = true,
            sanitizedData = mapOf("id_number" to idNumber),
            dataClassification = DataClassification.PHI_RESTRICTED
        )
    }

    /**
     * Validate patient medical data
     */
    fun validatePatientData(patientData: Map<String, Any>): ValidationResult {
        val errors = mutableMapOf<String, String>()
        val sanitizedData = mutableMapOf<String, Any>()
        var maxClassification = DataClassification.INTERNAL

        // Validate name
        patientData["name"]?.let { name ->
            val nameStr = name.toString().trim()
            if (nameStr.isBlank()) {
                errors["name"] = "Patient name is required"
            } else if (!NAME_PATTERN.matcher(nameStr).matches()) {
                errors["name"] = "Invalid name format (only letters, spaces, hyphens, apostrophes)"
            } else if (nameStr.length < 2 || nameStr.length > 100) {
                errors["name"] = "Name must be between 2 and 100 characters"
            } else {
                sanitizedData["name"] = sanitizeName(nameStr)
                maxClassification = DataClassification.PHI_RESTRICTED
            }
        }

        // Validate age
        patientData["age"]?.let { age ->
            try {
                val ageInt = when (age) {
                    is String -> age.toInt()
                    is Number -> age.toInt()
                    else -> throw NumberFormatException("Invalid age type")
                }

                if (ageInt < 0 || ageInt > 150) {
                    errors["age"] = "Age must be between 0 and 150"
                } else {
                    sanitizedData["age"] = ageInt
                }
            } catch (e: NumberFormatException) {
                errors["age"] = "Age must be a valid number"
            }
        }

        // Validate phone number
        patientData["phone"]?.let { phone ->
            val phoneStr = phone.toString().trim()
            if (phoneStr.isNotBlank()) {
                if (!SA_PHONE_PATTERN.matcher(phoneStr).matches()) {
                    errors["phone"] = "Invalid SA phone number format"
                } else {
                    sanitizedData["phone"] = sanitizePhoneNumber(phoneStr)
                    maxClassification = DataClassification.PHI_RESTRICTED
                }
            }
        }

        // Validate email
        patientData["email"]?.let { email ->
            val emailStr = email.toString().trim()
            if (emailStr.isNotBlank()) {
                if (!EMAIL_PATTERN.matcher(emailStr).matches()) {
                    errors["email"] = "Invalid email format"
                } else {
                    sanitizedData["email"] = emailStr.lowercase()
                    maxClassification = DataClassification.PHI_RESTRICTED
                }
            }
        }

        // Validate gender
        patientData["gender"]?.let { gender ->
            val genderStr = gender.toString().uppercase().trim()
            if (genderStr !in listOf("M", "F", "MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY")) {
                errors["gender"] = "Invalid gender value"
            } else {
                sanitizedData["gender"] = when (genderStr) {
                    "MALE", "M" -> "MALE"
                    "FEMALE", "F" -> "FEMALE"
                    else -> genderStr
                }
            }
        }

        // Validate symptoms
        patientData["symptoms"]?.let { symptoms ->
            when (symptoms) {
                is String -> {
                    val sanitized = sanitizeSymptomDescription(symptoms)
                    if (sanitized.isNotBlank()) {
                        sanitizedData["symptoms"] = sanitized
                        maxClassification = DataClassification.PHI_RESTRICTED
                    }
                }

                is List<*> -> {
                    val sanitizedList = symptoms.filterIsInstance<String>()
                        .map { sanitizeSymptomDescription(it) }
                        .filter { it.isNotBlank() }
                    if (sanitizedList.isNotEmpty()) {
                        sanitizedData["symptoms"] = sanitizedList
                        maxClassification = DataClassification.PHI_RESTRICTED
                    }
                }
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            sanitizedData = sanitizedData,
            dataClassification = maxClassification
        )
    }

    /**
     * Validate medicine data
     */
    fun validateMedicineData(medicineData: Map<String, Any>): ValidationResult {
        val errors = mutableMapOf<String, String>()
        val sanitizedData = mutableMapOf<String, Any>()

        // Validate medicine name
        medicineData["name"]?.let { name ->
            val nameStr = name.toString().trim()
            if (nameStr.isBlank()) {
                errors["name"] = "Medicine name is required"
            } else if (!MEDICINE_NAME_PATTERN.matcher(nameStr).matches()) {
                errors["name"] = "Invalid medicine name format"
            } else {
                sanitizedData["name"] = nameStr
            }
        } ?: run {
            errors["name"] = "Medicine name is required"
        }

        // Validate quantity
        medicineData["quantity"]?.let { quantity ->
            try {
                val quantityInt = when (quantity) {
                    is String -> quantity.toInt()
                    is Number -> quantity.toInt()
                    else -> throw NumberFormatException("Invalid quantity type")
                }

                if (quantityInt < 0) {
                    errors["quantity"] = "Quantity cannot be negative"
                } else {
                    sanitizedData["quantity"] = quantityInt
                }
            } catch (e: NumberFormatException) {
                errors["quantity"] = "Quantity must be a valid number"
            }
        }

        // Validate expiry date
        medicineData["expiry_date"]?.let { expiryDate ->
            try {
                val dateStr = expiryDate.toString()
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                val currentDate = Date()

                if (date?.before(currentDate) == true) {
                    errors["expiry_date"] = "Medicine has already expired"
                } else {
                    sanitizedData["expiry_date"] = dateStr
                }
            } catch (e: Exception) {
                errors["expiry_date"] = "Invalid date format (use YYYY-MM-DD)"
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            sanitizedData = sanitizedData,
            dataClassification = DataClassification.CONFIDENTIAL
        )
    }

    /**
     * Luhn algorithm implementation for SA ID validation
     */
    private fun isValidLuhnChecksum(idNumber: String): Boolean {
        var sum = 0
        var alternate = false

        for (i in idNumber.length - 1 downTo 0) {
            var n = idNumber[i].toString().toInt()

            if (alternate) {
                n *= 2
                if (n > 9) {
                    n = n % 10 + 1
                }
            }

            sum += n
            alternate = !alternate
        }

        return sum % 10 == 0
    }

    /**
     * Calculate age from date of birth
     */
    private fun calculateAge(dob: Date): Int {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        calendar.time = dob
        val birthYear = calendar.get(Calendar.YEAR)
        return currentYear - birthYear
    }

    /**
     * Sanitize patient name (remove potential injection attempts)
     */
    private fun sanitizeName(name: String): String {
        return name.trim()
            .replace(Regex("[<>\"']"), "")
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Sanitize phone number to standard format
     */
    private fun sanitizePhoneNumber(phone: String): String {
        val cleaned = phone.replace(Regex("[^+0-9]"), "")
        return if (cleaned.startsWith("0")) {
            "+27${cleaned.substring(1)}"
        } else {
            cleaned
        }
    }

    /**
     * Sanitize symptom descriptions (remove PII and potential injection attempts)
     */
    private fun sanitizeSymptomDescription(symptoms: String): String {
        return symptoms.trim()
            .replace(Regex("\\b\\d{13}\\b"), "[ID_REDACTED]")      // SA ID numbers
            .replace(Regex("\\b\\d{10}\\b"), "[PHONE_REDACTED]")   // Phone numbers
            .replace(Regex("[<>\"';&]"), "")                        // Potential injection chars
            .replace(Regex("\\s+"), " ")                           // Multiple spaces
            .take(500)                                             // Limit length
    }
}