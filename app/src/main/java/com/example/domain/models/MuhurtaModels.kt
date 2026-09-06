package com.example.domain.models

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Activity categories supported by the Vedic Muhurta Engine (Phase 10).
 */
enum class MuhurtaActivityType(
    val englishName: String,
    val sanskritName: String,
    val hindiName: String,
    val description: String
) {
    GENERAL_AUSPICIOUS(
        englishName = "General Auspicious (Sarva Karya)",
        sanskritName = "सर्व कार्य सिद्धि",
        hindiName = "सामान्य शुभ कार्य",
        description = "General auspicious beginnings, new agreements, important decisions, and positive undertakings."
    ),
    TRAVEL(
        englishName = "Travel & Journey (Yatra)",
        sanskritName = "यात्रा आरम्भ",
        hindiName = "यात्रा एवं प्रस्थान",
        description = "Journeys, departure, pilgrimages, international trips, and relocations."
    ),
    EDUCATION(
        englishName = "Education & Study (Vidyarambha)",
        sanskritName = "विद्यारम्भ",
        hindiName = "शिक्षा एवं अध्ययन",
        description = "Commencing courses, admissions, competitive preparation, examinations, and new skills."
    ),
    BUSINESS(
        englishName = "Business & Commerce (Vyapar)",
        sanskritName = "व्यापार आरम्भ",
        hindiName = "व्यापार एवं व्यवसाय",
        description = "Inaugurations, new ventures, signing deals, commercial partnerships, and investments."
    ),
    PROPERTY_HOME(
        englishName = "Property & Home (Griha Pravesh / Vastu)",
        sanskritName = "गृह प्रवेश एवं वास्तु",
        hindiName = "गृह प्रवेश एवं भूमि क्रय",
        description = "House warming, land purchase, property registration, laying foundation, and shifting home."
    ),
    CEREMONY_PUJA(
        englishName = "Ceremony & Dev Puja (Sanskara)",
        sanskritName = "धार्मिक अनुष्ठान एवं पूजा",
        hindiName = "पूजा एवं अनुष्ठान",
        description = "Homa, sacred rites, idol consecration, spiritual initiation, and prayer sankalpa."
    ),
    VEHICLE_PURCHASE(
        englishName = "Vehicle Purchase (Vahan Kraya)",
        sanskritName = "वाहन क्रय",
        hindiName = "वाहन एवं मशीनरी खरीद",
        description = "Purchasing or taking delivery of cars, motorcycles, commercial vehicles, and machinery."
    ),
    MEDICAL_HEALING(
        englishName = "Medical & Healing (Chikitsa)",
        sanskritName = "चिकित्सा आरम्भ",
        hindiName = "स्वास्थ्य एवं चिकित्सा",
        description = "Commencing Ayurvedic therapies, wellness regimens, scheduling treatments, and recovery starts."
    );

    companion object {
        fun fromString(name: String?): MuhurtaActivityType {
            if (name == null) return GENERAL_AUSPICIOUS
            return entries.find {
                it.name.equals(name, ignoreCase = true) ||
                it.englishName.equals(name, ignoreCase = true) ||
                it.hindiName.equals(name, ignoreCase = true)
            } ?: GENERAL_AUSPICIOUS
        }
    }
}

/**
 * Result type indicating whether Muhurta is general (universal) or personalized to a birth chart.
 */
enum class MuhurtaResultType(val label: String) {
    GENERAL_MUHURTA("General Muhurta (Panchang-Based)"),
    PERSONALIZED_MUHURTA("Personalised Muhurta (Natal Tara & Chandra Bala)")
}

/**
 * Evaluation state of an individual candidate time window.
 */
enum class MuhurtaEvaluationState(val label: String, val hindiLabel: String) {
    FAVORABLE("Favorable", "उत्तम / शुभ"),
    CONDITIONALLY_FAVORABLE("Conditionally Favorable", "मध्यम शुभ"),
    MIXED("Mixed", "मिश्रित"),
    CAUTION("Caution / Inauspicious", "अशुभ / वर्जित"),
    INSUFFICIENT_DATA("Insufficient Data", "अपूर्ण डेटा"),
    CALCULATION_ERROR("Calculation Error", "गणना त्रुटि")
}

/**
 * Evidence-based ranking tier for ordering Muhurta windows.
 */
enum class MuhurtaRankTier(val rankWeight: Int, val title: String) {
    BEST_AVAILABLE(1, "Best Available"),
    FAVORABLE(2, "Favorable"),
    CONDITIONALLY_FAVORABLE(3, "Conditionally Favorable"),
    MIXED(4, "Mixed"),
    CAUTION(5, "Caution Required")
}

/**
 * Categorization of supporting or caution factors.
 */
enum class MuhurtaFactorCategory {
    PANCHANG_TITHI,
    PANCHANG_VARA,
    PANCHANG_NAKSHATRA,
    PANCHANG_YOGA,
    PANCHANG_KARANA,
    SOLAR_LUNAR_WINDOW,
    ACTIVITY_RULE,
    PERSONAL_TARA_BALA,
    PERSONAL_CHANDRA_BALA,
    CAUTION_PERIOD
}

/**
 * Structured supporting or caution factor with classical context.
 */
data class MuhurtaFactor(
    val category: MuhurtaFactorCategory,
    val title: String,
    val description: String,
    val isSupporting: Boolean,
    val classicalReference: String? = null
)

/**
 * Rule evaluation record documenting why a specific rule passed or failed.
 */
data class MuhurtaRuleEvidence(
    val ruleId: String,
    val ruleName: String,
    val passed: Boolean,
    val weight: Double,
    val details: String
)

/**
 * Personal astrological bala (strength) context when a user profile is provided.
 */
data class PersonalBalaContext(
    val profileName: String,
    val janmaNakshatra: Nakshatra,
    val janmaPada: Int,
    val taraName: String,
    val taraIndex: Int, // 1 to 9
    val isTaraFavorable: Boolean,
    val natalMoonRashi: Rashi,
    val transitMoonRashi: Rashi,
    val chandraBalaHouse: Int, // 1 to 12
    val isChandraBalaFavorable: Boolean,
    val isChandrashtama: Boolean,
    val balaSummary: String
)

/**
 * Time slot preference filter for searching candidate windows.
 */
enum class TimeSlotPreference(val displayName: String, val startHour: Int, val endHour: Int) {
    ALL_DAY("Full Day", 0, 24),
    MORNING("Morning (Sunrise - 12 PM)", 4, 12),
    AFTERNOON("Afternoon (12 PM - 5 PM)", 12, 17),
    EVENING("Evening (5 PM - 9 PM)", 17, 21),
    NIGHT("Night (9 PM - Sunrise)", 21, 28)
}

/**
 * Candidate time window for a specific date and time span.
 */
data class MuhurtaCandidateWindow(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sanskritName: String? = null,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val localDate: LocalDate,
    val evaluationState: MuhurtaEvaluationState,
    val rankTier: MuhurtaRankTier,
    val score: Double,
    val supportingFactors: List<MuhurtaFactor> = emptyList(),
    val cautionFactors: List<MuhurtaFactor> = emptyList(),
    val ruleEvidence: List<MuhurtaRuleEvidence> = emptyList(),
    val personalBalaContext: PersonalBalaContext? = null,
    val summary: String
)

/**
 * Request parameter object for Muhurta Engine.
 */
data class MuhurtaRequest(
    val activityType: MuhurtaActivityType = MuhurtaActivityType.GENERAL_AUSPICIOUS,
    val startDate: LocalDate,
    val endDate: LocalDate = startDate,
    val location: BirthLocation,
    val profile: UserProfile? = null,
    val preferredTimeSlot: TimeSlotPreference = TimeSlotPreference.ALL_DAY,
    val customConstraints: List<String> = emptyList()
)

/**
 * Overall Muhurta calculation execution state.
 */
enum class MuhurtaResultState {
    SUCCESS,
    LIMITED_DATA,
    INSUFFICIENT_DATA,
    CALCULATION_ERROR
}

/**
 * Authoritative, immutable result model returned by the Muhurta Engine.
 */
data class MuhurtaResult(
    val id: String = UUID.randomUUID().toString(),
    val activityType: MuhurtaActivityType,
    val resultType: MuhurtaResultType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val location: PanchangLocationContext,
    val calculationTimestamp: ZonedDateTime,
    val candidateWindows: List<MuhurtaCandidateWindow>,
    val bestWindow: MuhurtaCandidateWindow?,
    val overallSupportingFactors: List<MuhurtaFactor>,
    val overallCautionFactors: List<MuhurtaFactor>,
    val ruleEvaluations: List<MuhurtaRuleEvidence>,
    val resultState: MuhurtaResultState = MuhurtaResultState.SUCCESS,
    val calculationLimitations: List<String> = emptyList(),
    val calculationEngineVersion: String = "JyotirAI-Muhurta-v1.0-SwissEph",
    val metadata: CalculationMetadata
)

/**
 * Raw astronomical time window before rule evaluation.
 */
data class RawTimeWindow(
    val id: String,
    val name: String,
    val sanskritName: String? = null,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val localDate: LocalDate,
    val isInherentlyAuspicious: Boolean,
    val isCautionWindow: Boolean,
    val baseWeight: Double,
    val description: String
)

/**
 * Inauspicious period during the day (Rahukaal, Yamaganda, Gulika Kaal, Durmuhurta).
 */
data class InauspiciousInterval(
    val name: String,
    val sanskritName: String? = null,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val reason: String
)

