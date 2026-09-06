package com.example.domain.engine

import com.example.domain.engine.prediction.PredictionEngine
import com.example.domain.models.*
import java.time.LocalDate

/**
 * Public facade for Phase 7 Prediction Engine.
 */
object PredictionCalculator {

    fun calculate(
        profile: AstrologyProfile,
        dashaTimeline: DashaTimeline? = null,
        transits: List<Transit>? = null,
        yogaDoshaSnapshot: YogaDoshaSnapshot? = null,
        targetDate: LocalDate = LocalDate.now()
    ): PredictionSnapshot = PredictionEngine.calculatePredictions(
        profile = profile,
        dashaTimeline = dashaTimeline,
        transits = transits,
        yogaDoshaSnapshot = yogaDoshaSnapshot,
        targetDate = targetDate
    )
}
