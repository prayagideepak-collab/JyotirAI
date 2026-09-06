package com.example.domain.muhurta

import com.example.domain.models.MuhurtaActivityType
import com.example.domain.models.MuhurtaRequest
import com.example.domain.models.MuhurtaResult
import java.time.LocalDate

/**
 * Authoritative Phase 10 Muhurta Engine Interface.
 * Computes deterministic, evidence-based auspicious time windows
 * for multiple activity categories across single days or date ranges.
 */
interface MuhurtaEngine {

    /**
     * Calculates candidate Muhurta windows for a given structured request.
     */
    suspend fun calculateMuhurta(request: MuhurtaRequest): Result<MuhurtaResult>

    /**
     * Quick evaluation convenience method for a single date.
     */
    suspend fun calculateDailyMuhurta(
        activityType: MuhurtaActivityType,
        date: LocalDate,
        location: com.example.domain.models.BirthLocation,
        profile: com.example.domain.models.UserProfile? = null
    ): Result<MuhurtaResult>

    /**
     * Clears internal cache.
     */
    fun clearCache()
}
