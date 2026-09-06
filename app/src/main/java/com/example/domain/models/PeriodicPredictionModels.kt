package com.example.domain.models

import java.time.LocalDate
import java.time.ZoneId

/**
 * Period classification for Vedic predictions.
 */
enum class PredictionPeriodType(
    val code: String,
    val displayName: String,
    val hindiName: String
) {
    DAILY("daily", "Daily Prediction", "दैनिक भविष्यफल"),
    MONTHLY("monthly", "Monthly Prediction", "मासिक भविष्यफल"),
    YEARLY("yearly", "Yearly Prediction", "वार्षिक भविष्यफल")
}

/**
 * Execution state of periodic prediction calculation.
 */
enum class PeriodicPredictionState {
    SUCCESS,
    LIMITED_DATA,
    INSUFFICIENT_DATA,
    CALCULATION_ERROR
}

/**
 * Planetary or Dasha period transition occurring within a prediction timeframe.
 */
data class PeriodTransitionInfo(
    val transitionDate: LocalDate,
    val transitionType: String, // e.g. "Antardasha Change", "Transit Ingress"
    val description: String,
    val fromLordOrSign: String,
    val toLordOrSign: String
)

/**
 * Structured Time Context for period-based prediction.
 */
data class PeriodTimeContext(
    val periodType: PredictionPeriodType,
    val targetDate: LocalDate,
    val targetYear: Int,
    val targetMonth: Int? = null, // 1..12
    val targetDay: Int? = null,   // 1..31
    val startDate: LocalDate,
    val endDate: LocalDate,
    val calculationTimeZone: ZoneId,
    val activeMahadasha: String,
    val activeAntardasha: String,
    val mahadashaEndDate: LocalDate? = null,
    val antardashaEndDate: LocalDate? = null,
    val periodTransitions: List<PeriodTransitionInfo> = emptyList()
)

/**
 * Detailed Gochar / Transit evidence summarized for a given period.
 */
data class PeriodicTransitEvidence(
    val majorPlanets: List<PlanetPredictionContext> = emptyList(),
    val retrogradePlanets: List<String> = emptyList(),
    val supportiveTransits: List<String> = emptyList(),
    val challengingTransits: List<String> = emptyList(),
    val summary: String
)

/**
 * Vimshottari Dasha evidence evaluated for a given period.
 */
data class PeriodicDashaEvidence(
    val mahadashaLord: String,
    val antardashaLord: String,
    val isTransitioning: Boolean = false,
    val dashaSignification: String,
    val summary: String
)

/**
 * Life topic prediction evaluated specifically for a time period (Daily, Monthly, or Yearly).
 */
data class PeriodicTopicPrediction(
    val topic: LifeTopic,
    val supportLevel: PredictionSupportLevel,
    val trendType: PredictionTrendType,
    val primaryHousesInvolved: List<Int>,
    val keyPlanets: List<PlanetPredictionContext>,
    val synthesis: String,
    val supportingFactors: List<String>,
    val cautionFactors: List<String>,
    val timingGuidance: String? = null
)

/**
 * Comprehensive structured result of a Daily, Monthly, or Yearly prediction calculation.
 */
data class PeriodicPredictionResult(
    val id: String,
    val profileId: String,
    val profileName: String,
    val predictionType: PredictionPeriodType,
    val timeContext: PeriodTimeContext,
    val calculationTimestamp: Long = System.currentTimeMillis(),
    val state: PeriodicPredictionState = PeriodicPredictionState.SUCCESS,
    val overallSupportLevel: PredictionSupportLevel,
    val overallTrend: PredictionTrendType,
    val overallSummary: String,
    val topicPredictions: Map<LifeTopic, PeriodicTopicPrediction>,
    val supportingThemes: List<String>,
    val cautionThemes: List<String>,
    val importantPeriodChanges: List<PeriodTransitionInfo> = emptyList(),
    val dashaEvidence: PeriodicDashaEvidence,
    val transitEvidence: PeriodicTransitEvidence,
    val yogaEvidence: List<String> = emptyList(),
    val doshaEvidence: List<String> = emptyList(),
    val limitations: String = "Based on Parashari principles under Lahiri Ayanamsa and Whole Sign Bhavas. Free will, personal choices, and individual karma actively shape outcomes."
)
