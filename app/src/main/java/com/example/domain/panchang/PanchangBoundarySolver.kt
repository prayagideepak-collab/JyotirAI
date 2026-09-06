package com.example.domain.panchang

import de.thmac.swisseph.SweConst
import de.thmac.swisseph.SwissEph
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * High-precision astronomical boundary solver for Panchang Angas (Tithi, Nakshatra, Yoga, Karana).
 * Uses iterative root-finding with Swiss Ephemeris to locate the exact start and end instants.
 */
object PanchangBoundarySolver {

    private const val MAX_ITERATIONS = 5
    private const val TOLERANCE_DAYS = 0.0001 // ~8.6 seconds

    enum class AngaType {
        TITHI,      // (MoonLon - SunLon) % 360, span = 12°
        NAKSHATRA,  // MoonLon % 360, span = 13°20' (13.333333°)
        YOGA,       // (SunLon + MoonLon) % 360, span = 13°20'
        KARANA      // (MoonLon - SunLon) % 360, span = 6°
    }

    /**
     * Calculates the exact start and end ZonedDateTime for the active Anga.
     */
    fun findBoundaries(
        tjdUt: Double,
        angaType: AngaType,
        zoneId: ZoneId,
        swe: SwissEph
    ): Pair<ZonedDateTime?, ZonedDateTime?> {
        val span = when (angaType) {
            AngaType.TITHI -> 12.0
            AngaType.NAKSHATRA -> 360.0 / 27.0
            AngaType.YOGA -> 360.0 / 27.0
            AngaType.KARANA -> 6.0
        }

        val currentVal = getAngaAngle(tjdUt, angaType, swe)
        val currentIndex = (currentVal / span).toInt()
        val startTarget = currentIndex * span
        val endTarget = (currentIndex + 1) * span

        // Average angular speed (degrees per day)
        val approxSpeed = when (angaType) {
            AngaType.TITHI, AngaType.KARANA -> 12.19
            AngaType.NAKSHATRA -> 13.176
            AngaType.YOGA -> 14.16
        }

        // Estimate JD offsets
        val degFromStart = normalizeDifference(currentVal - startTarget)
        val degToEnd = normalizeDifference(endTarget - currentVal)

        val jdStartEstimate = tjdUt - (degFromStart / approxSpeed)
        val jdEndEstimate = tjdUt + (degToEnd / approxSpeed)

        val jdStart = refineBoundary(jdStartEstimate, startTarget, angaType, swe)
        val jdEnd = refineBoundary(jdEndEstimate, endTarget, angaType, swe)

        val startZdt = jdStart?.let { PanchangDateResolver.fromJulianDayUt(it, zoneId) }
        val endZdt = jdEnd?.let { PanchangDateResolver.fromJulianDayUt(it, zoneId) }

        return Pair(startZdt, endZdt)
    }

    private fun refineBoundary(
        initialJd: Double,
        targetAngle: Double,
        angaType: AngaType,
        swe: SwissEph
    ): Double? {
        var jd = initialJd
        val normTarget = normalizeDegree(targetAngle)

        for (i in 0 until MAX_ITERATIONS) {
            val angle = getAngaAngle(jd, angaType, swe)
            val diff = angleDifference(angle, normTarget)

            if (kotlin.math.abs(diff) < 0.001) { // ~0.001 deg is ~7 seconds
                return jd
            }

            val approxSpeed = when (angaType) {
                AngaType.TITHI, AngaType.KARANA -> 12.19
                AngaType.NAKSHATRA -> 13.176
                AngaType.YOGA -> 14.16
            }

            val deltaJd = -diff / approxSpeed
            jd += deltaJd

            if (kotlin.math.abs(deltaJd) < TOLERANCE_DAYS) {
                return jd
            }
        }
        return jd
    }

    private fun getAngaAngle(tjdUt: Double, angaType: AngaType, swe: SwissEph): Double {
        swe.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0.0, 0.0)
        val flags = SweConst.SEFLG_MOSEPH or SweConst.SEFLG_SIDEREAL

        val sunRes = DoubleArray(6)
        val moonRes = DoubleArray(6)
        val serr = StringBuffer()

        val sunLon = if (angaType != AngaType.NAKSHATRA) {
            swe.swe_calc_ut(tjdUt, SweConst.SE_SUN, flags, sunRes, serr)
            normalizeDegree(sunRes[0])
        } else 0.0

        swe.swe_calc_ut(tjdUt, SweConst.SE_MOON, flags, moonRes, serr)
        val moonLon = normalizeDegree(moonRes[0])

        return when (angaType) {
            AngaType.TITHI, AngaType.KARANA -> normalizeDegree(moonLon - sunLon)
            AngaType.NAKSHATRA -> moonLon
            AngaType.YOGA -> normalizeDegree(sunLon + moonLon)
        }
    }

    private fun normalizeDegree(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    private fun normalizeDifference(diff: Double): Double {
        var d = diff % 360.0
        if (d < 0) d += 360.0
        return d
    }

    private fun angleDifference(a: Double, b: Double): Double {
        var diff = a - b
        while (diff > 180.0) diff -= 360.0
        while (diff < -180.0) diff += 360.0
        return diff
    }
}
