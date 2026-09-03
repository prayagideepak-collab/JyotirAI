package com.example.data.engine

import com.example.domain.engine.AstrologyEngine
import com.example.domain.models.AstrologyProfile
import com.example.domain.models.BirthData
import com.example.domain.models.Chart
import com.example.domain.models.AppError
import kotlinx.coroutines.delay

/**
 * Placeholder implementation of AstrologyEngine for Phase 1.
 * DO NOT fabricate accurate planetary calculations here.
 * This will be replaced by a deterministic engine in Phase 2.
 */
class MockAstrologyEngine : AstrologyEngine {
    override suspend fun calculateProfile(birthData: BirthData): Result<AstrologyProfile> {
        delay(500) // Simulate computation time
        return Result.failure(AppError.CalculationError("Phase 1: Deterministic calculations pending Phase 2 integration."))
    }

    override suspend fun calculateChart(birthData: BirthData, chartType: String): Result<Chart> {
        delay(500)
        return Result.failure(AppError.CalculationError("Phase 1: Chart generation pending Phase 3 integration."))
    }
}

