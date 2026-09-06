package com.example.domain.panchang

import com.example.domain.models.AppError
import com.example.domain.models.NityaYoga
import de.thmac.swisseph.SwissEph
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Calculates Panchang Nitya Yoga (1 to 27) from Sun and Moon longitude sum.
 * NOTE: Strictly architecturally distinct from Natal Yoga Analysis (Phase 6).
 */
object PanchangYogaCalculator {

    private val YOGA_NAMES = listOf(
        "Vishkambha", "Priti", "Ayushman", "Saubhagya", "Shobhana",
        "Atiganda", "Sukarma", "Dhriti", "Shula", "Ganda",
        "Vriddhi", "Dhruva", "Vyaghata", "Harshana", "Vajra",
        "Siddhi", "Vyatipata", "Variyan", "Parigha", "Shiva",
        "Siddha", "Sadhya", "Shubha", "Shukla", "Brahma",
        "Indra", "Vaidhriti"
    )

    private val YOGA_HINDI_NAMES = listOf(
        "विष्कुम्भ", "प्रीति", "आयुष्मान", "सौभाग्य", "शोभन",
        "अतिगण्ड", "सुकर्मा", "धृति", "शूल", "गण्ड",
        "वृद्धि", "ध्रुव", "व्याघात", "हर्षण", "वज्र",
        "सिद्धि", "व्यतीपात", "वरीयान", "परिघ", "शिव",
        "सिद्ध", "साध्य", "शुभ", "शुक्ल", "ब्रह्म",
        "इन्द्र", "वैधृति"
    )

    fun calculate(
        sunLongitude: Double,
        moonLongitude: Double,
        tjdUt: Double? = null,
        zoneId: ZoneId? = null,
        swe: SwissEph? = null
    ): NityaYoga {
        val yogaLon = normalizeDegree(sunLongitude + moonLongitude)
        val span = 360.0 / 27.0
        val yogaIndex = (yogaLon / span).toInt() + 1 // 1 to 27

        if (yogaIndex !in 1..27) {
            throw AppError.CalculationError("Invalid Panchang Yoga index calculated: $yogaIndex")
        }

        val remainingPct = (1.0 - ((yogaLon % span) / span)).coerceIn(0.0, 1.0)
        val name = YOGA_NAMES[yogaIndex - 1]
        val hindiName = YOGA_HINDI_NAMES[yogaIndex - 1]

        var startTime: ZonedDateTime? = null
        var endTime: ZonedDateTime? = null

        if (tjdUt != null && zoneId != null && swe != null) {
            try {
                val boundaries = PanchangBoundarySolver.findBoundaries(
                    tjdUt = tjdUt,
                    angaType = PanchangBoundarySolver.AngaType.YOGA,
                    zoneId = zoneId,
                    swe = swe
                )
                startTime = boundaries.first
                endTime = boundaries.second
            } catch (e: Exception) {
                // Keep null if boundary finding fails
            }
        }

        return NityaYoga(
            index = yogaIndex,
            name = name,
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
