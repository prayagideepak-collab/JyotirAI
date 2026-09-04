package com.example.data.engine

import com.example.domain.engine.AstrologyEngine
import com.example.domain.models.*
import java.time.ZonedDateTime

/**
 * Test / Fallback mock for unit test environments where ephemeris is not required.
 *
 * Note: The production engine is [SwissEphAstrologyEngine].
 */
class MockAstrologyEngine : AstrologyEngine {
    override suspend fun calculateProfile(birthData: BirthData): Result<AstrologyProfile> {
        return Result.failure(AppError.CalculationError("Mock engine: For testing only. Use SwissEphAstrologyEngine for production calculations."))
    }

    override suspend fun calculateChart(birthData: BirthData, chartType: String): Result<Chart> {
        return Result.failure(AppError.CalculationError("Mock engine: For testing only. Use SwissEphAstrologyEngine for production calculations."))
    }

    override suspend fun calculateDashaTimeline(
        birthData: BirthData,
        targetDateTime: ZonedDateTime?
    ): Result<DashaTimeline> {
        return Result.failure(AppError.CalculationError("Mock engine: For testing only. Use SwissEphAstrologyEngine for production calculations."))
    }

    override suspend fun calculateTransitSnapshot(
        transitDateTime: ZonedDateTime,
        location: BirthLocation,
        natalProfile: AstrologyProfile?
    ): Result<TransitSnapshot> {
        return Result.failure(AppError.CalculationError("Mock engine: For testing only. Use SwissEphAstrologyEngine for production calculations."))
    }

    override suspend fun calculatePanchang(
        date: ZonedDateTime,
        location: BirthLocation
    ): Result<com.example.domain.models.PanchangSnapshot> {
        return Result.failure(AppError.CalculationError("Mock engine: For testing only."))
    }
}
