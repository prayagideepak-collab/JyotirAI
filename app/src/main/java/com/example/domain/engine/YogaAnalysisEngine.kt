package com.example.domain.engine

import com.example.domain.models.*
import com.example.domain.engine.yogadosha.YogaRuleEngine

/**
 * Deterministic Parashari Yoga Analysis Engine.
 * Delegates to [YogaRuleEngine] for evaluation.
 */
object YogaAnalysisEngine {

    /**
     * Evaluates all supported classical Vedic Yogas for the provided profile.
     */
    fun analyzeYogas(profile: AstrologyProfile): List<YogaAnalysisResult> {
        return YogaRuleEngine.evaluateAll(profile)
    }
}
