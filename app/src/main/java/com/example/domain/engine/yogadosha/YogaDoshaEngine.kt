package com.example.domain.engine.yogadosha

import com.example.domain.models.*

/**
 * Authoritative Phase 6 Yoga and Dosha Calculation Engine for JyotirAI.
 * Orchestrates pure deterministic Vedic analysis conforming strictly to Brihat Parashara Hora Shastra,
 * Phaladeepika, and Saravali without external simulation or non-deterministic generators.
 */
object YogaDoshaEngine {

    /**
     * Calculates complete immutable Yoga & Dosha snapshot for a given AstrologyProfile.
     */
    fun calculate(profile: AstrologyProfile): YogaDoshaSnapshot {
        val validation = ResultValidator.validateProfileData(profile)
        if (!validation.isValid) {
            return YogaDoshaSnapshot(
                profileId = profile.birthData.name,
                profileName = profile.birthData.name,
                calculatedAtEpochMillis = System.currentTimeMillis(),
                detectedYogas = emptyList(),
                allEvaluatedYogas = emptyList(),
                detectedDoshas = emptyList(),
                allEvaluatedDoshas = emptyList(),
                summaryText = "गणना संभव नहीं: ${validation.reason}",
                dominantYoga = null,
                activeDoshaCount = 0
            )
        }

        // 1. Evaluate Yogas deterministically
        val evaluatedYogas = YogaRuleEngine.evaluateAll(profile)
        val detectedYogas = evaluatedYogas.filter { it.isDetected && it.strength != YogaStrength.INACTIVE }

        // 2. Evaluate Doshas deterministically
        val evaluatedDoshas = DoshaRuleEngine.evaluateAll(profile)
        val detectedDoshas = evaluatedDoshas.filter { it.isDetected }

        // 3. Identify dominant Yoga
        val dominantYoga = detectedYogas.maxByOrNull { it.strength.scoreMultiplier }

        // 4. Build Hindi summary explanation
        val summaryText = EvidenceBuilder.buildHindiSummary(
            detectedYogas = detectedYogas,
            detectedDoshas = detectedDoshas,
            dominantYoga = dominantYoga
        )

        return YogaDoshaSnapshot(
            profileId = profile.birthData.name,
            profileName = profile.birthData.name,
            calculatedAtEpochMillis = System.currentTimeMillis(),
            detectedYogas = detectedYogas,
            allEvaluatedYogas = evaluatedYogas,
            detectedDoshas = detectedDoshas,
            allEvaluatedDoshas = evaluatedDoshas,
            summaryText = summaryText,
            dominantYoga = dominantYoga,
            activeDoshaCount = detectedDoshas.count { it.isDetected && !it.isCancelled }
        )
    }
}
