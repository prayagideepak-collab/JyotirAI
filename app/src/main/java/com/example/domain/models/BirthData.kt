package com.example.domain.models

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Validated geographical coordinates, place name, and timezone.
 */
data class BirthLocation(
    val latitude: Double,
    val longitude: Double,
    val placeName: String,
    val altitudeMeters: Double? = null,
    val timeZoneId: String? = null,
    val isVerified: Boolean = false,
    val source: String = "manual"
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
        require(!latitude.isNaN() && !latitude.isInfinite()) { "Latitude must be finite" }
        require(!longitude.isNaN() && !longitude.isInfinite()) { "Longitude must be finite" }
        altitudeMeters?.let {
            require(!it.isNaN() && !it.isInfinite()) { "Altitude must be finite" }
        }
        require(placeName.isNotBlank()) { "Place name must not be blank" }
        timeZoneId?.let {
            requireCatchingZoneId(it)
        }
    }
    
    private fun requireCatchingZoneId(zoneId: String) {
        try {
            ZoneId.of(zoneId)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid timezone ID: $zoneId")
        }
    }
}

/**
 * Foundation data model for astrological calculations.
 * Encapsulates the specific moment and location of birth.
 */
data class BirthData(
    val date: LocalDate,
    val time: LocalTime,
    val location: BirthLocation,
    val timeZone: ZoneId,
    val name: String = "User",
    val gender: String = "अन्य"
)
