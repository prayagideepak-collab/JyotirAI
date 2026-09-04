package com.example.domain.engine

import com.example.domain.models.AstrologyProfile
import com.example.domain.models.BirthData
import com.example.domain.models.BirthLocation
import com.example.domain.models.Chart
import com.example.domain.models.DashaTimeline
import com.example.domain.models.TransitSnapshot
import java.time.ZonedDateTime

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

    /**
     * Calculates the deterministic Vimshottari Dasha timeline based on sidereal Moon longitude.
     */
    suspend fun calculateDashaTimeline(
        birthData: BirthData,
        targetDateTime: ZonedDateTime? = null
    ): Result<DashaTimeline>

    /**
     * Calculates deterministic planetary transits (Gochar) for a specific date, time, and location,
     * optionally evaluating relative house positions against a natal profile.
     */
    suspend fun calculateTransitSnapshot(
        transitDateTime: ZonedDateTime,
        location: BirthLocation,
        natalProfile: AstrologyProfile? = null
    ): Result<TransitSnapshot>
}

