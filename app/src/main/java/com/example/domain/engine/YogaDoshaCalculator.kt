package com.example.domain.engine

import com.example.domain.models.*
import com.example.domain.engine.yogadosha.YogaDoshaEngine

/**
 * High-level deterministic orchestrator for Vedic Yoga and Dosha Analysis (Phase 6).
 */
object YogaDoshaCalculator {

    /**
     * Generates a complete YogaDoshaSnapshot for the given astrological profile.
     */
    fun calculate(profile: AstrologyProfile): YogaDoshaSnapshot {
        return YogaDoshaEngine.calculate(profile)
    }
}

