package com.example.domain.engine

import com.example.domain.models.*
import com.example.domain.engine.yogadosha.DoshaRuleEngine

/**
 * Deterministic Parashari Dosha Analysis Engine.
 * Delegates to [DoshaRuleEngine] for evaluation.
 */
object DoshaAnalysisEngine {

    /**
     * Evaluates all supported classical Vedic Doshas for the provided profile.
     */
    fun analyzeDoshas(profile: AstrologyProfile): List<DoshaAnalysisResult> {
        return DoshaRuleEngine.evaluateAll(profile)
    }
}
