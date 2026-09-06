package com.example.domain.panchang

import com.example.domain.models.AppError
import com.example.domain.models.BirthLocation
import com.example.domain.models.PanchangLocationContext
import java.time.ZoneId

/**
 * Resolves, validates, and standardizes location context for Panchang calculations.
 * Ensures strict privacy compliance (no continuous background tracking).
 */
object LocationContextResolver {

    fun resolve(location: BirthLocation?, fallbackZoneId: ZoneId? = null): PanchangLocationContext {
        if (location == null) {
            throw AppError.InvalidBirthData("Location must be provided for Panchang calculation.")
        }

        if (location.latitude < -90.0 || location.latitude > 90.0) {
            throw AppError.InvalidBirthData("Latitude ${location.latitude} is invalid. Must be between -90 and +90.")
        }

        if (location.longitude < -180.0 || location.longitude > 180.0) {
            throw AppError.InvalidBirthData("Longitude ${location.longitude} is invalid. Must be between -180 and +180.")
        }

        val tzId = location.timeZoneId?.ifBlank { null } ?: fallbackZoneId?.id ?: "UTC"
        val zoneId = try {
            ZoneId.of(tzId)
        } catch (e: Exception) {
            fallbackZoneId ?: ZoneId.of("UTC")
        }

        return PanchangLocationContext(
            placeName = location.placeName.ifBlank { "Selected Location" },
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = location.altitudeMeters,
            timeZoneId = tzId,
            calculationTimeZone = zoneId
        )
    }

    fun isValidCoordinates(lat: Double, lon: Double): Boolean {
        return lat in -90.0..90.0 && lon in -180.0..180.0
    }
}
