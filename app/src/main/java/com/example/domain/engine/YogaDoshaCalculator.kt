package com.example.domain.engine

import com.example.domain.models.*

/**
 * High-level deterministic orchestrator for Vedic Yoga and Dosha Analysis (Phase 6).
 */
object YogaDoshaCalculator {

    /**
     * Generates a complete YogaDoshaSnapshot for the given astrological profile.
     */
    fun calculate(profile: AstrologyProfile): YogaDoshaSnapshot {
        val allYogas = YogaAnalysisEngine.analyzeYogas(profile)
        val detectedYogas = allYogas.filter { it.isDetected }

        val allDoshas = DoshaAnalysisEngine.analyzeDoshas(profile)
        val detectedDoshas = allDoshas.filter { it.isDetected }

        val dominantYoga = detectedYogas.firstOrNull { it.strength == YogaStrength.EXCELLENT }
            ?: detectedYogas.firstOrNull { it.strength == YogaStrength.STRONG }
            ?: detectedYogas.firstOrNull()

        val activeDoshas = detectedDoshas.filter { !it.isCancelled }

        val summaryText = buildString {
            append("कुण्डली में ${detectedYogas.size} मुख्य शुभ योग एवं ${detectedDoshas.size} दोष कारक पाए गए। ")
            dominantYoga?.let {
                append("प्रमुख प्रभावशाली योग: ${it.sanskritName} (${it.strength.displayName})। ")
            }
            if (activeDoshas.isEmpty()) {
                append("कोई तीव्र अनिष्फल दोष सक्रिय नहीं है।")
            } else {
                val names = activeDoshas.joinToString(", ") { it.sanskritName }
                append("सक्रिय दोष विचार: $names।")
            }
        }

        return YogaDoshaSnapshot(
            profileName = profile.birthData.name,
            calculatedAtEpochMillis = System.currentTimeMillis(),
            detectedYogas = detectedYogas,
            allEvaluatedYogas = allYogas,
            detectedDoshas = detectedDoshas,
            allEvaluatedDoshas = allDoshas,
            summaryText = summaryText,
            dominantYoga = dominantYoga,
            activeDoshaCount = activeDoshas.size
        )
    }
}
