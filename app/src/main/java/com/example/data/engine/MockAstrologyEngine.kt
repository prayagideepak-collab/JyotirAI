package com.example.data.engine

import com.example.domain.engine.AstrologyEngine
import com.example.domain.models.AppError
import com.example.domain.models.AstrologyProfile
import com.example.domain.models.BirthData
import com.example.domain.models.Chart

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
}
