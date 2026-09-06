package com.example.domain.panchang

import com.example.domain.models.PanchangLocationContext
import de.thmac.swisseph.DblObj
import de.thmac.swisseph.SweConst
import de.thmac.swisseph.SwissEph
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Calculates high-accuracy location-specific Sunrise and Sunset for a given local date.
 * Handles daylight boundaries, atmospheric refraction, and high-latitude edge cases.
 */
object SunriseSunsetCalculator {

    data class RiseSetResult(
        val sunrise: ZonedDateTime?,
        val sunset: ZonedDateTime?,
        val isPolarDay: Boolean = false,
        val isPolarNight: Boolean = false
    )

    fun calculate(
        localDate: LocalDate,
        location: PanchangLocationContext,
        swe: SwissEph
    ): RiseSetResult {
        val midnightZoned = PanchangDateResolver.resolveLocalMidnight(localDate, location.calculationTimeZone)
        val tjdUtMidnight = PanchangDateResolver.toJulianDayUt(midnightZoned)

        val sunrise = calculateRiseSetEvent(
            tjdUtMidnight = tjdUtMidnight,
            location = location,
            swe = swe,
            rsmi = SweConst.SE_CALC_RISE,
            targetDate = localDate
        )

        val sunset = calculateRiseSetEvent(
            tjdUtMidnight = tjdUtMidnight,
            location = location,
            swe = swe,
            rsmi = SweConst.SE_CALC_SET,
            targetDate = localDate
        )

        val isPolar = sunrise == null && sunset == null && kotlin.math.abs(location.latitude) > 65.0

        return RiseSetResult(
            sunrise = sunrise,
            sunset = sunset,
            isPolarDay = isPolar && location.latitude > 0 && (localDate.monthValue in 4..9),
            isPolarNight = isPolar && location.latitude > 0 && (localDate.monthValue !in 4..9)
        )
    }

    private fun calculateRiseSetEvent(
        tjdUtMidnight: Double,
        location: PanchangLocationContext,
        swe: SwissEph,
        rsmi: Int, // SweConst.SE_CALC_RISE or SweConst.SE_CALC_SET
        targetDate: LocalDate
    ): ZonedDateTime? {
        val geopos = doubleArrayOf(location.longitude, location.latitude, location.altitudeMeters ?: 0.0)
        val flags = SweConst.SEFLG_MOSEPH

        var currentTjd = tjdUtMidnight - 0.5
        var iterations = 0

        while (iterations < 5) {
            val tret = DblObj()
            val serr = StringBuffer()

            val res = swe.swe_rise_trans(
                currentTjd,
                SweConst.SE_SUN,
                null,
                flags,
                rsmi,
                geopos,
                1013.25,
                15.0,
                tret,
                serr
            )

            if (res == -1 || res == -2 || res < 0) {
                return null // Event not found (e.g. polar region)
            }

            val eventJd = tret.`val`
            val eventZoned = PanchangDateResolver.fromJulianDayUt(eventJd, location.calculationTimeZone) ?: return null
            val eventDate = eventZoned.toLocalDate()

            if (eventDate == targetDate) {
                return eventZoned
            } else if (eventDate.isAfter(targetDate)) {
                return null
            }

            currentTjd = eventJd + 0.01
            iterations++
        }

        return null
    }
}
