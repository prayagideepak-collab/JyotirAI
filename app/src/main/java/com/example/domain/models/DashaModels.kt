package com.example.domain.models

import java.time.ZonedDateTime

/**
 * Standard 9 Grahas (Planetary Lords) governing Vimshottari Dasha cycles.
 * Standard Order: Ketu → Venus → Sun → Moon → Mars → Rahu → Jupiter → Saturn → Mercury
 * Total cycle = 120 years.
 */
enum class DashaPlanet(
    val lord: String,
    val sanskritName: String,
    val years: Int,
    val abbreviation: String,
    val element: String = "",
    val colorHex: Long = 0xFFFFFFFF
) {
    KETU("Ketu", "Ketu", 7, "Ke", "Fire", 0xFF9E9E9E),
    VENUS("Venus", "Shukra", 20, "Ve", "Water", 0xFFE91E63),
    SUN("Sun", "Surya", 6, "Su", "Fire", 0xFFFF9800),
    MOON("Moon", "Chandra", 10, "Mo", "Water", 0xFF81D4FA),
    MARS("Mars", "Mangala", 7, "Ma", "Fire", 0xFFE53935),
    RAHU("Rahu", "Rahu", 18, "Ra", "Air", 0xFF78909C),
    JUPITER("Jupiter", "Guru", 16, "Ju", "Ether", 0xFFFFD54F),
    SATURN("Saturn", "Shani", 19, "Sa", "Air", 0xFF3F51B5),
    MERCURY("Mercury", "Budha", 17, "Me", "Earth", 0xFF4CAF50);

    companion object {
        const val TOTAL_CYCLE_YEARS = 120

        val VIMSHOTTARI_ORDER: List<DashaPlanet> = entries.toList()

        fun fromLord(lordName: String): DashaPlanet {
            val trimmed = lordName.trim()
            return entries.firstOrNull { it.lord.equals(trimmed, ignoreCase = true) }
                ?: entries.firstOrNull { it.sanskritName.equals(trimmed, ignoreCase = true) }
                ?: entries.firstOrNull { it.abbreviation.equals(trimmed, ignoreCase = true) }
                ?: KETU
        }

        fun sequenceStartingFrom(startLord: DashaPlanet): List<DashaPlanet> {
            val startIndex = entries.indexOf(startLord)
            return (0 until entries.size).map { entries[(startIndex + it) % entries.size] }
        }
    }
}

/**
 * Representation of astrological time duration in Years, Months, and Days.
 */
data class DashaBalance(
    val years: Int,
    val months: Int,
    val days: Int,
    val totalYears: Double
) {
    val formatted: String
        get() = when {
            years > 0 && months > 0 -> "$years y, $months m, $days d"
            years > 0 -> "$years y, $days d"
            months > 0 -> "$months m, $days d"
            else -> "$days d"
        }

    val detailedFormatted: String
        get() = "$years Years, $months Months, $days Days"

    companion object {
        fun fromYears(totalYears: Double): DashaBalance {
            val safeYears = totalYears.coerceAtLeast(0.0)
            val y = safeYears.toInt()
            val remMonths = (safeYears - y) * 12.0
            val m = remMonths.toInt()
            val remDays = (remMonths - m) * (365.2425 / 12.0)
            val d = Math.round(remDays).toInt().coerceAtLeast(0)
            return DashaBalance(y, m, d, safeYears)
        }
    }
}

/**
 * Phase 4 Antardasha (Bhukti) Sub-period model.
 */
data class AntardashaPeriod(
    val mahadashaLord: DashaPlanet,
    val antardashaLord: DashaPlanet,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val durationYears: Double,
    val isCurrent: Boolean,
    val subPeriods: List<PratyantardashaPeriod> = emptyList()
) {
    val durationBalance: DashaBalance
        get() = DashaBalance.fromYears(durationYears)

    val label: String
        get() = "${mahadashaLord.lord} - ${antardashaLord.lord}"
}

/**
 * Phase 4 Mahadasha Major-period model.
 */
data class MahadashaPeriod(
    val planet: DashaPlanet,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val totalDurationYears: Double,
    val isCurrent: Boolean,
    val antardashas: List<AntardashaPeriod> = emptyList(),
    val isBirthMahadasha: Boolean = false,
    val birthBalance: DashaBalance? = null,
    val birthDateTime: ZonedDateTime? = null
) {
    /**
     * Active start date during native's life: birth date for starting Mahadasha, startDate for subsequent periods.
     */
    val activeStartDate: ZonedDateTime
        get() = if (isBirthMahadasha && birthDateTime != null) birthDateTime else startDate

    val durationBalance: DashaBalance
        get() = DashaBalance.fromYears(totalDurationYears)
}

/**
 * Minimal architecture model for future Pratyantardasha expansion (Phase 5+).
 */
data class PratyantardashaPeriod(
    val mahadashaLord: DashaPlanet,
    val antardashaLord: DashaPlanet,
    val pratyantardashaLord: DashaPlanet,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val durationYears: Double,
    val isCurrent: Boolean
)

/**
 * Comprehensive Vimshottari Dasha timeline containing full 120-year progression and metadata.
 */
data class DashaTimeline(
    val birthNakshatra: Nakshatra,
    val nakshatraLord: String,
    val startingMahadasha: DashaPlanet,
    val startingBalance: DashaBalance,
    val mahadashaPeriods: List<MahadashaPeriod>,
    val currentMahadasha: MahadashaPeriod?,
    val currentAntardasha: AntardashaPeriod?,
    val targetDateTime: ZonedDateTime,
    val moonLongitude: Double,
    val fractionElapsed: Double,
    val fractionRemaining: Double,
    val metadata: CalculationMetadata
)
