package com.example.domain.panchang

import com.example.domain.models.AppError
import com.example.domain.models.Paksha
import com.example.domain.models.Tithi
import de.thmac.swisseph.SwissEph
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Calculates Vedic Tithi from the angular elongation between Moon and Sun.
 * Support 30 Tithis across Shukla Paksha and Krishna Paksha with start/end transitions.
 */
object TithiCalculator {

    private val TITHI_NAMES = listOf(
        "Pratipada", "Dvitiya", "Tritiya", "Chaturthi", "Panchami",
        "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
        "Ekadashi", "Dvadashi", "Trayodashi", "Chaturdashi", "Purnima", // Shukla 1-15
        "Pratipada", "Dvitiya", "Tritiya", "Chaturthi", "Panchami",
        "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
        "Ekadashi", "Dvadashi", "Trayodashi", "Chaturdashi", "Amavasya" // Krishna 16-30
    )

    private val TITHI_HINDI_NAMES = listOf(
        "प्रतिपदा", "द्वितीया", "तृतीया", "चतुर्थी", "पंचमी",
        "षष्ठी", "सप्तमी", "अष्टमी", "नवमी", "दशमी",
        "एकादशी", "द्वादशी", "त्रयोदशी", "चतुर्दशी", "पूर्णिमा",
        "प्रतिपदा", "द्वितीया", "तृतीया", "चतुर्थी", "पंचमी",
        "षष्ठी", "सप्तमी", "अष्टमी", "नवमी", "दशमी",
        "एकादशी", "द्वादशी", "त्रयोदशी", "चतुर्दशी", "अमावस्या"
    )

    fun calculate(
        sunLongitude: Double,
        moonLongitude: Double,
        tjdUt: Double? = null,
        zoneId: ZoneId? = null,
        swe: SwissEph? = null
    ): Tithi {
        val elongation = normalizeDegree(moonLongitude - sunLongitude)
        val tithiIndex = (elongation / 12.0).toInt() + 1 // 1 to 30

        if (tithiIndex !in 1..30) {
            throw AppError.CalculationError("Invalid Tithi index calculated: $tithiIndex")
        }

        val paksha = if (tithiIndex <= 15) Paksha.SHUKLA else Paksha.KRISHNA
        val remainingPct = (1.0 - ((elongation % 12.0) / 12.0)).coerceIn(0.0, 1.0)

        val name = TITHI_NAMES[tithiIndex - 1]
        val hindiName = TITHI_HINDI_NAMES[tithiIndex - 1]

        var startTime: ZonedDateTime? = null
        var endTime: ZonedDateTime? = null

        if (tjdUt != null && zoneId != null && swe != null) {
            try {
                val boundaries = PanchangBoundarySolver.findBoundaries(
                    tjdUt = tjdUt,
                    angaType = PanchangBoundarySolver.AngaType.TITHI,
                    zoneId = zoneId,
                    swe = swe
                )
                startTime = boundaries.first
                endTime = boundaries.second
            } catch (e: Exception) {
                // Keep null if boundary finding fails
            }
        }

        return Tithi(
            index = tithiIndex,
            name = name,
            paksha = paksha,
            remainingPercentage = remainingPct,
            startTime = startTime,
            endTime = endTime,
            hindiName = hindiName
        )
    }

    private fun normalizeDegree(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }
}
