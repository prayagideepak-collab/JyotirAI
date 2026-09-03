package com.example.domain.engine

import com.example.domain.models.AstrologyProfile
import com.example.domain.models.BirthData
import com.example.domain.models.Chart

/**
 * Interface defining the boundary for the deterministic astrology calculation engine.
 * Ensures that UI and App layers do not depend directly on calculation specifics.
 * 
 * Future phases will implement this interface deterministically using verified ephemeris data.
 */
interface AstrologyEngine {
    /**
     * Calculates the complete foundational astrology profile for a given birth moment.
     */
    suspend fun calculateProfile(birthData: BirthData): Result<AstrologyProfile>
    
    /**
     * Generates a structural representation of the Rashi Chart (D1) or other divisional charts.
     */
    suspend fun calculateChart(birthData: BirthData, chartType: String = "D1"): Result<Chart>
    
    // Future placeholders for getDashas, getTransits, getYogas, etc.
}

