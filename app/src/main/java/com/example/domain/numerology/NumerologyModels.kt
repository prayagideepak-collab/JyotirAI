package com.example.domain.numerology

import java.time.LocalDate

/**
 * Numerology methodology definition.
 */
enum class NumerologyMethodology(
    val code: String,
    val displayName: String,
    val hindiName: String,
    val description: String
) {
    CHALDEAN(
        code = "CHALDEAN",
        displayName = "Chaldean System (Vedic Integrated)",
        hindiName = "कील्डियन / वैदिक अंकशास्त्र",
        description = "Ancient vibrational sound system where numbers 1-8 are assigned to alphabets (9 is sacred). Birth number and Life Path number reduced with master number preservation."
    ),
    PYTHAGOREAN(
        code = "PYTHAGOREAN",
        displayName = "Pythagorean (Western Modern)",
        hindiName = "पाइथागोरियन प्रणाली",
        description = "Sequential 1-9 alphabetical assignment and foundational birth date reduction."
    )
}

/**
 * Lifecycle state of the Numerology calculation.
 */
enum class NumerologyCalculationState {
    SUCCESS,
    LIMITED_DATA,
    INSUFFICIENT_DATA,
    UNSUPPORTED_METHOD,
    CALCULATION_ERROR
}

/**
 * Detailed step in a reduction process for transparency and testability.
 */
data class NumberReductionStep(
    val stepIndex: Int,
    val expression: String,
    val inputTotal: Int,
    val reducedValue: Int,
    val isMasterNumber: Boolean
)

/**
 * Calculated core single number with reduction audit trail.
 */
data class NumerologyNumber(
    val title: String,
    val sanskritTitle: String,
    val hindiName: String,
    val finalNumber: Int,
    val rootSingleDigit: Int, // 1..9 (even if finalNumber is a master number 11/22/33)
    val isMasterNumber: Boolean,
    val rulingPlanet: String,
    val rulingPlanetHindi: String,
    val calculationExpression: String,
    val reductionSteps: List<NumberReductionStep> = emptyList(),
    val summaryHindi: String,
    val descriptionHindi: String,
    val keyStrengthsHindi: List<String> = emptyList(),
    val cautionaryGuidanceHindi: List<String> = emptyList()
)

/**
 * Complete Numerology Result Model adhering to Step 8.
 */
data class NumerologyResult(
    val resultId: String,
    val profileId: String?,
    val profileName: String,
    val birthDate: LocalDate,
    val inputName: String?,
    val methodology: NumerologyMethodology,
    val calculationEngineVersion: String = "1.0.0-phase12",
    val calculationState: NumerologyCalculationState,
    val birthNumber: NumerologyNumber?, // Moolank / Day Number (1..9 or Master)
    val lifePathNumber: NumerologyNumber?, // Bhagyank / Destined Path
    val nameNumber: NumerologyNumber? = null, // Namank (when valid name provided)
    val attitudeNumber: NumerologyNumber? = null, // Day + Month
    val favorableNumbers: List<Int> = emptyList(),
    val neutralNumbers: List<Int> = emptyList(),
    val challengingNumbers: List<Int> = emptyList(),
    val favorableDaysHindi: List<String> = emptyList(),
    val favorableColorsHindi: List<String> = emptyList(),
    val traditionalRemediesHindi: List<String> = emptyList(),
    val limitations: List<String> = emptyList(),
    val calculatedAtTimestamp: Long = System.currentTimeMillis()
)
