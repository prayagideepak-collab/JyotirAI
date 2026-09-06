package com.example.domain.prediction

import com.example.domain.models.*

/**
 * Result validator for Phase 8 Periodic Prediction Engine.
 * Verifies completeness of input data, calculation validity, and ensures output constraints.
 */
object ResultValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val reason: String = ""
    )

    fun validateProfile(profile: AstrologyProfile?): ValidationResult {
        if (profile == null) {
            return ValidationResult(false, "Astrology profile is null")
        }
        if (profile.birthData.name.isBlank()) {
            return ValidationResult(false, "Profile name is blank")
        }
        if (profile.planetPositions.isEmpty()) {
            return ValidationResult(false, "Natal planetary positions are empty")
        }
        if (profile.lagna.isBlank() || profile.moonSign.isBlank()) {
            return ValidationResult(false, "Missing Lagna or Moon sign in profile")
        }
        return ValidationResult(true)
    }

    fun validateTimeContext(timeContext: PeriodTimeContext?): ValidationResult {
        if (timeContext == null) {
            return ValidationResult(false, "Time context is null")
        }
        if (timeContext.startDate.isAfter(timeContext.endDate)) {
            return ValidationResult(false, "Start date ${timeContext.startDate} is after end date ${timeContext.endDate}")
        }
        if (timeContext.targetYear < 1900 || timeContext.targetYear > 2100) {
            return ValidationResult(false, "Target year ${timeContext.targetYear} is outside supported calculation range (1900-2100)")
        }
        return ValidationResult(true)
    }

    fun sanitizeResult(result: PeriodicPredictionResult): PeriodicPredictionResult {
        // Ensure no empty topic predictions and consistent state
        val sanitizedTopics = result.topicPredictions.mapValues { (topic, pred) ->
            if (pred.synthesis.isBlank()) {
                pred.copy(synthesis = "Astrological factors for ${topic.displayName} are balanced. Maintain steady discernment.")
            } else {
                pred
            }
        }

        val sanitizedSummary = if (result.overallSummary.isBlank()) {
            "Comprehensive ${result.predictionType.displayName} synthesized from Natal Kundli, Dasha timing, and Planetary Gochar."
        } else {
            result.overallSummary
        }

        return result.copy(
            topicPredictions = sanitizedTopics,
            overallSummary = sanitizedSummary
        )
    }
}
