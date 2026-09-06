package com.example.domain.prediction

import com.example.domain.engine.AstrologyEngine
import com.example.domain.engine.YogaDoshaCalculator
import com.example.domain.engine.prediction.PredictionEngine
import com.example.domain.models.*
import java.time.LocalDate

/**
 * Data bundle carrying all calculated astrological inputs for a specific profile and date.
 */
data class AggregatedAstrologicalContext(
    val profile: AstrologyProfile,
    val targetDate: LocalDate,
    val dashaTimeline: DashaTimeline?,
    val transitSnapshot: TransitSnapshot?,
    val yogaDoshaSnapshot: YogaDoshaSnapshot?,
    val phase7PredictionSnapshot: PredictionSnapshot
)

/**
 * Coordinates fetching and calculating all required Vedic engines for building prediction contexts.
 */
class PredictionContextBuilder(
    private val astrologyEngine: AstrologyEngine
) {

    suspend fun buildContext(
        profile: AstrologyProfile,
        targetDate: LocalDate
    ): Result<AggregatedAstrologicalContext> {
        val birthData = profile.birthData
        val location = birthData.location
        val zone = birthData.timeZone

        val targetZoned = targetDate.atStartOfDay(zone).plusHours(12)

        // 1. Dasha Timeline
        val dashaResult = astrologyEngine.calculateDashaTimeline(birthData, targetZoned)
        val dashaTimeline = dashaResult.getOrNull()

        // 2. Transit Snapshot
        val transitResult = astrologyEngine.calculateTransitSnapshot(targetZoned, location, profile)
        val transitSnapshot = transitResult.getOrNull()

        // 3. Yoga & Dosha Snapshot
        val yogaDoshaSnapshot = try {
            YogaDoshaCalculator.calculate(profile)
        } catch (_: Exception) {
            null
        }

        // 4. Map Transits for Phase 7 Prediction Engine
        val transitsList = transitSnapshot?.positions?.map {
            Transit(planet = it.planet, currentSign = it.sign, degree = it.degreeInSign)
        }

        // 5. Phase 7 Prediction Snapshot
        val phase7Snapshot = PredictionEngine.calculatePredictions(
            profile = profile,
            dashaTimeline = dashaTimeline,
            transits = transitsList,
            yogaDoshaSnapshot = yogaDoshaSnapshot,
            targetDate = targetDate
        )

        return Result.success(
            AggregatedAstrologicalContext(
                profile = profile,
                targetDate = targetDate,
                dashaTimeline = dashaTimeline,
                transitSnapshot = transitSnapshot,
                yogaDoshaSnapshot = yogaDoshaSnapshot,
                phase7PredictionSnapshot = phase7Snapshot
            )
        )
    }
}
