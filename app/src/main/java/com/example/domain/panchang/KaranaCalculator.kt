package com.example.domain.panchang

import com.example.domain.models.AppError
import com.example.domain.models.Karana
import de.thmac.swisseph.SwissEph
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Calculates Vedic Karana (half of a Tithi, span 6°).
 * Properly resolves 4 Fixed Karanas (Shakuni, Chatushpada, Naga, Kimstughna)
 * and 7 Repeating Movable Karanas (Bava, Balava, Kaulava, Taitila, Gara, Vanija, Vishti).
 */
object KaranaCalculator {

    private val REPEATING_NAMES = listOf("Bava", "Balava", "Kaulava", "Taitila", "Gara", "Vanija", "Vishti")
    private val REPEATING_HINDI_NAMES = listOf("बव", "बालव", "कौलव", "तैतिल", "गर", "वणिज", "विष्टि (भद्रा)")

    fun calculate(
        sunLongitude: Double,
        moonLongitude: Double,
        tjdUt: Double? = null,
        zoneId: ZoneId? = null,
        swe: SwissEph? = null
    ): Karana {
        val elongation = normalizeDegree(moonLongitude - sunLongitude)
        val karanaIndex = (elongation / 6.0).toInt() + 1 // 1 to 60

        if (karanaIndex !in 1..60) {
            throw AppError.CalculationError("Invalid Karana index calculated: $karanaIndex")
        }

        val remainingPct = (1.0 - ((elongation % 6.0) / 6.0)).coerceIn(0.0, 1.0)

        val name: String
        val hindiName: String
        val isFixed: Boolean

        when (karanaIndex) {
            1 -> {
                name = "Kimstughna"
                hindiName = "किंस्तुघ्न"
                isFixed = true
            }
            58 -> {
                name = "Shakuni"
                hindiName = "शकुनि"
                isFixed = true
            }
            59 -> {
                name = "Chatushpada"
                hindiName = "चतुष्पाद"
                isFixed = true
            }
            60 -> {
                name = "Naga"
                hindiName = "नाग"
                isFixed = true
            }
            in 2..57 -> {
                isFixed = false
                val movableIndex = (karanaIndex - 2) % 7
                name = REPEATING_NAMES[movableIndex]
                hindiName = REPEATING_HINDI_NAMES[movableIndex]
            }
            else -> throw AppError.CalculationError("Invalid Karana index: $karanaIndex")
        }

        var startTime: ZonedDateTime? = null
        var endTime: ZonedDateTime? = null

        if (tjdUt != null && zoneId != null && swe != null) {
            try {
                val boundaries = PanchangBoundarySolver.findBoundaries(
                    tjdUt = tjdUt,
                    angaType = PanchangBoundarySolver.AngaType.KARANA,
                    zoneId = zoneId,
                    swe = swe
                )
                startTime = boundaries.first
                endTime = boundaries.second
            } catch (e: Exception) {
                // Keep null if boundary finding fails
            }
        }

        return Karana(
            index = karanaIndex,
            name = name,
            isFixed = isFixed,
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
