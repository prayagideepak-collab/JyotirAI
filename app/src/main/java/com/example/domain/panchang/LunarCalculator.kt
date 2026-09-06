package com.example.domain.panchang

import com.example.domain.models.AppError
import com.example.domain.models.MoonContext
import com.example.domain.models.Nakshatra
import com.example.domain.models.Rashi
import de.thmac.swisseph.SweConst
import de.thmac.swisseph.SwissEph

/**
 * Calculates geocentric Sidereal Lunar ephemeris parameters for Panchang.
 */
object LunarCalculator {

    fun calculateMoon(tjdUt: Double, sunLongitude: Double, swe: SwissEph): MoonContext {
        swe.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0.0, 0.0)
        val flags = SweConst.SEFLG_MOSEPH or SweConst.SEFLG_SIDEREAL or SweConst.SEFLG_SPEED

        val res = DoubleArray(6)
        val serr = StringBuffer()
        val flag = swe.swe_calc_ut(tjdUt, SweConst.SE_MOON, flags, res, serr)
        if (flag < 0) {
            throw AppError.CalculationError("Error calculating Moon position: $serr")
        }

        val lon = normalizeDegree(res[0])
        val speed = res[3]

        val sign = Rashi.fromLongitude(lon)
        val degInSign = lon % 30.0
        val nakshatraPair = Nakshatra.fromLongitude(lon)

        val elongation = normalizeDegree(lon - sunLongitude)
        val phaseName = when {
            elongation < 12.0 -> "Amavasya (New Moon)"
            elongation < 90.0 -> "Waxing Crescent"
            elongation < 102.0 -> "First Quarter"
            elongation < 180.0 -> "Waxing Gibbous"
            elongation < 192.0 -> "Purnima (Full Moon)"
            elongation < 270.0 -> "Waning Gibbous"
            elongation < 282.0 -> "Third Quarter"
            else -> "Waning Crescent"
        }

        return MoonContext(
            sign = sign,
            longitude = lon,
            degreeInSign = degInSign,
            nakshatra = nakshatraPair.first,
            pada = nakshatraPair.second,
            elongation = elongation,
            phaseName = phaseName,
            speedDegPerDay = speed
        )
    }

    fun normalizeDegree(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }
}
