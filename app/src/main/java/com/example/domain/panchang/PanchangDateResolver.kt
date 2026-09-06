package com.example.domain.panchang

import com.example.domain.models.AppError
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Handles date and temporal boundaries for Panchang calculation.
 * Accurately translates civil dates, midnight, noon, and UTC Julian Day parameters.
 */
object PanchangDateResolver {

    const val MIN_SUPPORTED_YEAR = 1900
    const val MAX_SUPPORTED_YEAR = 2100

    fun validateDate(date: LocalDate) {
        if (date.year < MIN_SUPPORTED_YEAR || date.year > MAX_SUPPORTED_YEAR) {
            throw AppError.InvalidBirthData("Date year ${date.year} is outside supported calculation range ($MIN_SUPPORTED_YEAR-$MAX_SUPPORTED_YEAR).")
        }
    }

    /**
     * Resolves calculation instant for a given target date.
     * If no time is specified, defaults to midday (12:00:00 local time) for consistent Anga determination.
     */
    fun resolveCalculationInstant(
        targetDate: LocalDate,
        zoneId: ZoneId,
        time: LocalTime? = null
    ): ZonedDateTime {
        validateDate(targetDate)
        val localTime = time ?: LocalTime.of(12, 0, 0)
        return targetDate.atTime(localTime).atZone(zoneId)
    }

    /**
     * Resolves local civil midnight (00:00:00) in the given timezone.
     */
    fun resolveLocalMidnight(date: LocalDate, zoneId: ZoneId): ZonedDateTime {
        return date.atStartOfDay(zoneId)
    }

    /**
     * Converts a ZonedDateTime to Julian Day (UT) for Swiss Ephemeris.
     */
    fun toJulianDayUt(zdt: ZonedDateTime): Double {
        val utc = zdt.withZoneSameInstant(ZoneOffset.UTC)
        val hourDecimalUt = utc.hour +
                (utc.minute / 60.0) +
                (utc.second / 3600.0) +
                (utc.nano / 3_600_000_000_000.0)

        val sweDate = de.thmac.swisseph.SweDate(
            utc.year,
            utc.monthValue,
            utc.dayOfMonth,
            hourDecimalUt
        )
        return sweDate.julDay
    }

    /**
     * Converts a Julian Day (UT) back to a ZonedDateTime in the specified timezone.
     */
    fun fromJulianDayUt(jd: Double, zoneId: ZoneId): ZonedDateTime? {
        return try {
            // Astronomical Julian Day 2440587.5 corresponds exactly to Unix epoch 1970-01-01T00:00:00 UTC.
            val days = jd - 2440587.5
            val totalSeconds = days * 86400.0
            val epochSecond = kotlin.math.floor(totalSeconds).toLong()
            val nanoFraction = kotlin.math.round((totalSeconds - epochSecond) * 1_000_000_000.0).toLong()
            val normalizedSecond = if (nanoFraction >= 1_000_000_000L) epochSecond + 1 else epochSecond
            val normalizedNanos = if (nanoFraction >= 1_000_000_000L) 0L else nanoFraction.coerceAtLeast(0L)
            val instant = java.time.Instant.ofEpochSecond(normalizedSecond, normalizedNanos)
            instant.atZone(zoneId)
        } catch (e: Exception) {
            null
        }
    }
}
