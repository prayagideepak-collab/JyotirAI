package com.example.domain.engine

import com.example.domain.models.BirthData

/**
 * Interface defining the boundary for the astrology calculation engine.
 * Ensures that UI and App layers do not depend directly on calculation specifics.
 * 
 * Future phases will implement this interface deterministically.
 */
interface AstrologyEngine {
    /**
     * Calculates the Rashi (Moon sign) and Nakshatra for a given birth moment.
     */
    suspend fun getBasicDetails(birthData: BirthData): Result<String>
    
    /**
     * Generates a structural representation of the Rashi Chart (D1).
     */
    suspend fun getRashiChart(birthData: BirthData): Result<String>
    
    // Future placeholders for getDashas, getTransits, getYogas, etc.
}
