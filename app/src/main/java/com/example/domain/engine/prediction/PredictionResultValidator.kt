package com.example.domain.engine.prediction

import com.example.domain.models.*

/**
 * Deterministic Validator for Phase 7 Prediction Engine.
 * Verifies profile data completeness, checks bounds, and prevents invalid or incomplete evaluation states.
 */
object PredictionResultValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val reason: String
    )

    fun validateProfile(profile: AstrologyProfile?): ValidationResult {
        if (profile == null) {
            return ValidationResult(isValid = false, reason = "Astrology profile is null or uninitialized.")
        }
        if (profile.planetPositions.isEmpty()) {
            return ValidationResult(isValid = false, reason = "Planetary positions are empty.")
        }
        val requiredPlanets = setOf("sun", "moon", "mars", "mercury", "jupiter", "venus", "saturn", "rahu", "ketu")
        val available = profile.planetPositions.map { it.planet.lowercase().trim() }.toSet()
        val missing = requiredPlanets.filter { it !in available }
        if (missing.isNotEmpty()) {
            return ValidationResult(isValid = false, reason = "Missing required planets for prediction: ${missing.joinToString(", ")}.")
        }
        if (profile.lagnaSignIndex !in 0..11) {
            return ValidationResult(isValid = false, reason = "Invalid Lagna sign index: ${profile.lagnaSignIndex}.")
        }
        return ValidationResult(isValid = true, reason = "Profile is valid for prediction calculation.")
    }

    fun sanitizeSnapshot(snapshot: PredictionSnapshot): PredictionSnapshot {
        val sanitizedTopics = snapshot.topicPredictions.mapValues { (_, prediction) ->
            prediction.copy(
                supportingFactors = prediction.supportingFactors.distinct(),
                cautionFactors = prediction.cautionFactors.distinct(),
                neutralFactors = prediction.neutralFactors.distinct(),
                keyPlanets = prediction.keyPlanets.distinctBy { it.planetName }
            )
        }
        return snapshot.copy(topicPredictions = sanitizedTopics)
    }
}
