package com.example.domain.panchang

import com.example.domain.models.Nakshatra
import com.example.domain.models.NakshatraContext
import de.thmac.swisseph.SwissEph
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Calculates Vedic Nakshatra and Pada from Moon's sidereal longitude.
 * Accurately tracks span (13°20'), pada (3°20'), and temporal boundaries.
 */
object NakshatraCalculator {

    fun calculate(
        moonLongitude: Double,
        tjdUt: Double? = null,
        zoneId: ZoneId? = null,
        swe: SwissEph? = null
    ): NakshatraContext {
        val normLon = normalizeDegree(moonLongitude)
        val pair = Nakshatra.fromLongitude(normLon)
        val nakshatra = pair.first
        val pada = pair.second

        val nakshatraStartDegree = nakshatra.index * Nakshatra.SPAN_DEGREES
        var degreeWithinNakshatra = normLon - nakshatraStartDegree
        if (degreeWithinNakshatra < 0) degreeWithinNakshatra += 360.0

        val remainingPct = (1.0 - (degreeWithinNakshatra / Nakshatra.SPAN_DEGREES)).coerceIn(0.0, 1.0)

        var startTime: ZonedDateTime? = null
        var endTime: ZonedDateTime? = null

        if (tjdUt != null && zoneId != null && swe != null) {
            try {
                val boundaries = PanchangBoundarySolver.findBoundaries(
                    tjdUt = tjdUt,
                    angaType = PanchangBoundarySolver.AngaType.NAKSHATRA,
                    zoneId = zoneId,
                    swe = swe
                )
                startTime = boundaries.first
                endTime = boundaries.second
            } catch (e: Exception) {
                // Keep null if boundary finding fails
            }
        }

        return NakshatraContext(
            nakshatra = nakshatra,
            pada = pada,
            remainingPercentage = remainingPct,
            startTime = startTime,
            endTime = endTime,
            degreeInNakshatra = degreeWithinNakshatra
        )
    }

    private fun normalizeDegree(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }
}
