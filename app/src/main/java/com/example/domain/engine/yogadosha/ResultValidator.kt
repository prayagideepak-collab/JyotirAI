package com.example.domain.engine.yogadosha

import com.example.domain.models.*

/**
 * Deterministic Result Validator for Phase 6 Yoga and Dosha Analysis.
 * Ensures data completeness, validates calculations against classical requirements,
 * prevents duplicates, enforces deterministic ordering, and isolates birth profiles.
 */
object ResultValidator {

    /**
     * Validates whether an AstrologyProfile has sufficient astronomical data for accurate Yoga/Dosha evaluation.
     */
    fun validateProfileData(profile: AstrologyProfile?): DataCompletenessResult {
        if (profile == null) {
            return DataCompletenessResult(
                isValid = false,
                reason = "Astrology profile is null or uninitialized."
            )
        }
        if (profile.planetPositions.isEmpty()) {
            return DataCompletenessResult(
                isValid = false,
                reason = "Planetary positions list is empty."
            )
        }
        val requiredPlanets = setOf("Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn", "Rahu", "Ketu")
        val availablePlanets = profile.planetPositions.map { it.planet.lowercase() }.toSet()
        val missingPlanets = requiredPlanets.filter { it.lowercase() !in availablePlanets }
        if (missingPlanets.isNotEmpty()) {
            return DataCompletenessResult(
                isValid = false,
                reason = "Missing essential planets for calculation: ${missingPlanets.joinToString(", ")}."
            )
        }
        if (profile.lagnaSignIndex !in 0..11) {
            return DataCompletenessResult(
                isValid = false,
                reason = "Invalid Ascendant (Lagna) sign index: ${profile.lagnaSignIndex}."
            )
        }
        return DataCompletenessResult(isValid = true, reason = "Profile data complete and verified.")
    }

    /**
     * Sorts and deduplicates Yoga results deterministically.
     * Detected items first, followed by strength multiplier, followed by alphabetical ID.
     */
    fun sanitizeAndOrderYogas(yogas: List<YogaAnalysisResult>): List<YogaAnalysisResult> {
        val uniqueMap = LinkedHashMap<String, YogaAnalysisResult>()
        for (yoga in yogas) {
            if (!uniqueMap.containsKey(yoga.id)) {
                uniqueMap[yoga.id] = yoga
            }
        }
        return uniqueMap.values.sortedWith(
            compareByDescending<YogaAnalysisResult> { it.isDetected }
                .thenByDescending { it.strength.scoreMultiplier }
                .thenBy { it.id }
        )
    }

    /**
     * Sorts and deduplicates Dosha results deterministically.
     * Active detected items first, followed by detected & cancelled, followed by non-detected, then by ID.
     */
    fun sanitizeAndOrderDoshas(doshas: List<DoshaAnalysisResult>): List<DoshaAnalysisResult> {
        val uniqueMap = LinkedHashMap<String, DoshaAnalysisResult>()
        for (dosha in doshas) {
            if (!uniqueMap.containsKey(dosha.id)) {
                uniqueMap[dosha.id] = dosha
            }
        }
        return uniqueMap.values.sortedWith(
            compareByDescending<DoshaAnalysisResult> { it.isDetected && !it.isCancelled }
                .thenByDescending { it.isDetected }
                .thenBy { it.id }
        )
    }

    data class DataCompletenessResult(
        val isValid: Boolean,
        val reason: String
    )
}
