package com.example.domain.models

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Validated geographical coordinates and place name.
 */
data class BirthLocation(
    val latitude: Double,
    val longitude: Double,
    val placeName: String
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
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
    val name: String = "User"
)
