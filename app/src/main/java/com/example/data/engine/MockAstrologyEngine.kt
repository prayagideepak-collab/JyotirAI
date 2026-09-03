package com.example.data.engine

import com.example.domain.engine.AstrologyEngine
import com.example.domain.models.BirthData
import kotlinx.coroutines.delay

/**
 * Placeholder implementation of AstrologyEngine for Phase 1.
 * DO NOT fabricate accurate planetary calculations here.
 * This will be replaced by a deterministic engine in Phase 2.
 */
class MockAstrologyEngine : AstrologyEngine {
    override suspend fun getBasicDetails(birthData: BirthData): Result<String> {
        delay(500) // Simulate computation time
        return Result.success("Phase 1: Foundation. Exact calculations pending Phase 2 integration.")
    }

    override suspend fun getRashiChart(birthData: BirthData): Result<String> {
        delay(500)
        return Result.success("Phase 1: Empty Chart placeholder. Structure to be built in Phase 3.")
    }
}
