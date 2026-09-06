package com.example.domain.panchang

import com.example.domain.models.BirthLocation
import com.example.domain.models.PanchangResult
import com.example.domain.models.PanchangSnapshot
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Authoritative Phase 9 Panchang Engine Interface.
 * Supplies clean, decoupled astronomical almanac calculations for UI, Predictions,
 * and future Phase 10 Muhurta engine.
 */
interface PanchangEngine {

    suspend fun calculatePanchang(
        date: ZonedDateTime,
        location: BirthLocation
    ): Result<PanchangSnapshot>

    suspend fun calculatePanchangResult(
        dateTime: ZonedDateTime,
        location: BirthLocation
    ): Result<PanchangResult>

    suspend fun calculatePanchangForDate(
        targetDate: LocalDate,
        location: BirthLocation
    ): Result<PanchangResult>

    fun clearCache()
}
