package com.example.domain.prediction

import com.example.domain.engine.AstrologyEngine
import com.example.domain.models.*
import java.time.LocalDate

/**
 * Contract for calculating personalized Yearly Predictions.
 */
interface YearlyPredictionEngine {
    suspend fun generateYearlyPrediction(
        profile: AstrologyProfile,
        year: Int
    ): Result<PeriodicPredictionResult>
}

class YearlyPredictionEngineImpl(
    private val astrologyEngine: AstrologyEngine,
    private val contextBuilder: PredictionContextBuilder = PredictionContextBuilder(astrologyEngine)
) : YearlyPredictionEngine {

    override suspend fun generateYearlyPrediction(
        profile: AstrologyProfile,
        year: Int
    ): Result<PeriodicPredictionResult> {
        // 1. Validate Profile
        val profileValidation = ResultValidator.validateProfile(profile)
        if (!profileValidation.isValid) {
            return Result.failure(IllegalArgumentException(profileValidation.reason))
        }

        if (year !in 1900..2100) {
            return Result.failure(IllegalArgumentException("Year $year is outside supported range (1900-2100)."))
        }

        // 2. Mid-year anchor date for representative annual slow-moving transits
        val anchorDate = LocalDate.of(year, 7, 1)

        // 3. Build Astrological Context
        val contextResult = contextBuilder.buildContext(profile, anchorDate)
        if (contextResult.isFailure) {
            return Result.failure(contextResult.exceptionOrNull() ?: IllegalStateException("Failed to build yearly context"))
        }
        val context = contextResult.getOrThrow()

        // 4. Resolve Time Context (bounds, year boundaries, dasha transitions across full year)
        val timeContext = TimeContextResolver.resolve(
            periodType = PredictionPeriodType.YEARLY,
            targetDate = anchorDate,
            birthData = profile.birthData,
            dashaTimeline = context.dashaTimeline
        )

        // 5. Aggregate Evidence
        val rawResult = EvidenceAggregator.aggregate(
            profile = profile,
            periodType = PredictionPeriodType.YEARLY,
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
