package com.example.domain.prediction

import com.example.domain.engine.AstrologyEngine
import com.example.domain.models.*
import java.time.LocalDate

/**
 * Contract for calculating personalized Monthly Predictions.
 */
interface MonthlyPredictionEngine {
    suspend fun generateMonthlyPrediction(
        profile: AstrologyProfile,
        year: Int,
        month: Int
    ): Result<PeriodicPredictionResult>
}

class MonthlyPredictionEngineImpl(
    private val astrologyEngine: AstrologyEngine,
    private val contextBuilder: PredictionContextBuilder = PredictionContextBuilder(astrologyEngine)
) : MonthlyPredictionEngine {

    override suspend fun generateMonthlyPrediction(
        profile: AstrologyProfile,
        year: Int,
        month: Int
    ): Result<PeriodicPredictionResult> {
        // 1. Validate Profile
        val profileValidation = ResultValidator.validateProfile(profile)
        if (!profileValidation.isValid) {
            return Result.failure(IllegalArgumentException(profileValidation.reason))
        }

        if (month !in 1..12) {
            return Result.failure(IllegalArgumentException("Invalid month $month. Expected 1..12."))
        }

        // 2. Mid-month anchor date for representative monthly transit snapshot
        val anchorDate = LocalDate.of(year, month, 15)

        // 3. Build Astrological Context
        val contextResult = contextBuilder.buildContext(profile, anchorDate)
        if (contextResult.isFailure) {
            return Result.failure(contextResult.exceptionOrNull() ?: IllegalStateException("Failed to build monthly context"))
        }
        val context = contextResult.getOrThrow()

        // 4. Resolve Time Context (bounds, month boundaries, dasha transitions within month)
        val timeContext = TimeContextResolver.resolve(
            periodType = PredictionPeriodType.MONTHLY,
            targetDate = anchorDate,
            birthData = profile.birthData,
            dashaTimeline = context.dashaTimeline
        )

        // 5. Aggregate Evidence
        val rawResult = EvidenceAggregator.aggregate(
            profile = profile,
            periodType = PredictionPeriodType.MONTHLY,
            timeContext = timeContext,
            phase7Snapshot = context.phase7PredictionSnapshot,
            transitSnapshot = context.transitSnapshot,
            yogaDoshaSnapshot = context.yogaDoshaSnapshot
        )

        // 6. Sanitize & Return
        val sanitized = ResultValidator.sanitizeResult(rawResult)
        return Result.success(sanitized)
    }
}
