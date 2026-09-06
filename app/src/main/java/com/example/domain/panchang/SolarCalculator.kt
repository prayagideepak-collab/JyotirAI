package com.example.domain.panchang

import com.example.domain.models.AppError
import com.example.domain.models.Nakshatra
import com.example.domain.models.Rashi
import com.example.domain.models.SunContext
import de.thmac.swisseph.SweConst
import de.thmac.swisseph.SwissEph

/**
 * Calculates geocentric Sidereal Solar ephemeris parameters for Panchang.
 */
object SolarCalculator {

    fun calculateSun(tjdUt: Double, swe: SwissEph): SunContext {
        swe.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0.0, 0.0)
        val flags = SweConst.SEFLG_MOSEPH or SweConst.SEFLG_SIDEREAL or SweConst.SEFLG_SPEED

        val res = DoubleArray(6)
        val serr = StringBuffer()
        val flag = swe.swe_calc_ut(tjdUt, SweConst.SE_SUN, flags, res, serr)
        if (flag < 0) {
            throw AppError.CalculationError("Error calculating Sun position: $serr")
        }

        val lon = normalizeDegree(res[0])
        val speed = res[3]

        val sign = Rashi.fromLongitude(lon)
        val degInSign = lon % 30.0
        val nakshatraPair = Nakshatra.fromLongitude(lon)

        return SunContext(
            sign = sign,
            longitude = lon,
            degreeInSign = degInSign,
            nakshatra = nakshatraPair.first,
            pada = nakshatraPair.second,
            speedDegPerDay = speed
        )
    }

    fun normalizeDegree(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }
}
