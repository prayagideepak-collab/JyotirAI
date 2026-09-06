package com.example.domain.models

import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Standard Vedic Life Topics for Parashari contextual evaluation.
 */
enum class LifeTopic(
    val code: String,
    val displayName: String,
    val hindiName: String,
    val primaryHouses: List<Int>,
    val secondaryHouses: List<Int>,
    val naturalKarakaPlanets: List<String>
) {
    CAREER(
        code = "career",
        displayName = "Career & Profession",
        hindiName = "व्यवसाय एवं आजीविका (कर्म)",
        primaryHouses = listOf(10, 6),
        secondaryHouses = listOf(2, 11, 1),
        naturalKarakaPlanets = listOf("Saturn", "Sun", "Mercury", "Jupiter")
    ),
    MARRIAGE_RELATIONSHIPS(
        code = "marriage_relationships",
        displayName = "Marriage & Relationships",
        hindiName = "विवाह एवं वैवाहिक जीवन",
        primaryHouses = listOf(7),
        secondaryHouses = listOf(2, 8, 11, 4),
        naturalKarakaPlanets = listOf("Venus", "Jupiter")
    ),
    FINANCE(
        code = "finance",
        displayName = "Finance & Wealth",
        hindiName = "धन, संपत्ति एवं आर्थिक स्थिति",
        primaryHouses = listOf(2, 11),
        secondaryHouses = listOf(5, 9, 10),
        naturalKarakaPlanets = listOf("Jupiter", "Mercury", "Venus")
    ),
    FAMILY(
        code = "family",
        displayName = "Family & Domestic Harmony",
        hindiName = "पारिवारिक सुख एवं संबंध",
        primaryHouses = listOf(2, 4),
        secondaryHouses = listOf(3, 9, 1),
        naturalKarakaPlanets = listOf("Moon", "Jupiter", "Sun")
    ),
    EDUCATION(
        code = "education",
        displayName = "Education & Intellect",
        hindiName = "शिक्षा, ज्ञान एवं विद्या",
        primaryHouses = listOf(4, 5),
        secondaryHouses = listOf(9, 2),
        naturalKarakaPlanets = listOf("Mercury", "Jupiter")
    ),
    PROPERTY_HOME(
        code = "property_home",
        displayName = "Home, Property & Assets",
        hindiName = "गृह, भूमि, वाहन एवं अचल संपत्ति",
        primaryHouses = listOf(4),
        secondaryHouses = listOf(12, 11, 2),
        naturalKarakaPlanets = listOf("Mars", "Venus", "Moon")
    ),
    GENERAL_LIFE(
        code = "general_life",
        displayName = "General Life & Vitality",
        hindiName = "सामान्य जीवन, स्वास्थ्य एवं आत्मबल",
        primaryHouses = listOf(1, 9, 5),
        secondaryHouses = listOf(10, 11),
        naturalKarakaPlanets = listOf("Sun", "Moon", "Jupiter")
    );

    companion object {
        fun fromCode(code: String): LifeTopic =
            entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: GENERAL_LIFE
    }
}

/**
 * Evidence-based support classifications.
 * Replaces arbitrary percentages with strict astrological criteria.
 */
enum class PredictionSupportLevel(
    val displayName: String,
    val hindiName: String,
    val scoreValue: Int
) {
    STRONGLY_SUPPORTED("Strongly Supported", "अत्यंत अनुकूल एवं समर्थित", 3),
    SUPPORTED("Supported / Favorable", "अनुकूल प्रभाव", 2),
    MIXED_SIGNALS("Mixed Factors / Balanced", "मिश्रित फल / संतुलन आवश्यक", 1),
    CHALLENGING("Caution Advised / Challenging", "सावधानी एवं धैर्य अपेक्षित", -2),
    LIMITED_DATA("Limited Natal Indicators", "सीमित ग्रह संकेत", 0),
    INSUFFICIENT_DATA("Insufficient Data", "अपूर्ण ज्योतिषीय डेटा", 0)
}

/**
 * Directional trend type for a life topic during a specific period.
 */
enum class PredictionTrendType(
    val displayName: String,
    val hindiName: String
) {
    POSITIVE_GROWTH("Positive Growth & Progress", "प्रगति एवं उन्नति"),
    STABILITY("Stability & Steady Progress", "स्थिरता एवं निरंतरता"),
    RESTRUCTURING("Effort & Restructuring", "परिश्रम एवं पुनर्गठन"),
    CAUTION("Caution & Patience Required", "सतर्कता एवं सावधानी"),
    TRANSFORMATIVE("Transformative & Evolving", "परिवर्तनकारी प्रभाव")
}

/**
 * Evaluated Planet Context showing its specific functional role and timing influence.
 */
data class PlanetPredictionContext(
    val planetName: String,
    val functionalRole: String, // e.g. "10th Lord (Karaka for Career)", "Lagna Lord"
    val natalSign: String,
    val natalHouse: Int,
    val dignity: PlanetDignity,
    val isDashaLord: Boolean,
    val isAntardashaLord: Boolean,
    val transitSign: String? = null,
    val transitHouseFromLagna: Int? = null,
    val transitHouseFromMoon: Int? = null,
    val isRetrogradeInTransit: Boolean = false,
    val qualitativeInfluence: String // Deterministic explanation of this planet's impact
)

/**
 * Structured Time Context for the prediction analysis.
 */
data class PredictionTimeContext(
    val targetDate: LocalDate,
    val activeMahadasha: String,
    val activeAntardasha: String,
    val mahadashaEndDate: LocalDate? = null,
    val antardashaEndDate: LocalDate? = null,
    val transitSnapshotDate: LocalDate
)

/**
 * Complete, structured, deterministic Prediction Context for a single life topic.
 */
data class LifeTopicPrediction(
    val topic: LifeTopic,
    val supportLevel: PredictionSupportLevel,
    val trendType: PredictionTrendType,
    val keyPlanets: List<PlanetPredictionContext>,
    val primaryHousesInvolved: List<Int>,
    val natalKundliFactors: List<String>,
    val dashaTimelineFactors: String,
    val transitFactors: String,
    val relevantYogas: List<String>,
    val relevantDoshas: List<String>,
    val supportingFactors: List<String>,
    val cautionFactors: List<String>,
    val neutralFactors: List<String>,
    val classicalSynthesis: String,
    val limitations: String = "Based on Parashari principles under Lahiri Ayanamsa and Whole Sign Bhavas. Free will and individual karma actively shape outcomes."
)

/**
 * Complete immutable snapshot of Prediction Analysis across all life topics for a profile.
 */
data class PredictionSnapshot(
    val profileId: String,
    val profileName: String,
    val calculationTimestamp: Long,
    val timeContext: PredictionTimeContext,
    val topicPredictions: Map<LifeTopic, LifeTopicPrediction>,
    val overallLifeTrend: PredictionSupportLevel,
    val keyHighlightSummary: String
)
