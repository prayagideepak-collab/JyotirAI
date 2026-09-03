package com.example.domain.models

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Standard 12 Vedic Rashi (Zodiac Signs)
 */
enum class Rashi(
    val index: Int,
    val sanskritName: String,
    val englishName: String,
    val lord: String,
    val element: String
) {
    ARIES(0, "Mesha", "Aries", "Mars", "Fire"),
    TAURUS(1, "Vrishabha", "Taurus", "Venus", "Earth"),
    GEMINI(2, "Mithuna", "Gemini", "Mercury", "Air"),
    CANCER(3, "Karka", "Cancer", "Moon", "Water"),
    LEO(4, "Simha", "Leo", "Sun", "Fire"),
    VIRGO(5, "Kanya", "Virgo", "Mercury", "Earth"),
    LIBRA(6, "Tula", "Libra", "Venus", "Air"),
    SCORPIO(7, "Vrishchika", "Scorpio", "Mars", "Water"),
    SAGITTARIUS(8, "Dhanu", "Sagittarius", "Jupiter", "Fire"),
    CAPRICORN(9, "Makara", "Capricorn", "Saturn", "Earth"),
    AQUARIUS(10, "Kumbha", "Aquarius", "Saturn", "Air"),
    PISCES(11, "Meena", "Pisces", "Jupiter", "Water");

    companion object {
        fun fromIndex(index: Int): Rashi = entries[index.mod(12)]
        fun fromLongitude(longitude: Double): Rashi = fromIndex((longitude / 30.0).toInt())
    }
}

/**
 * The 27 Vedic Nakshatras
 */
enum class Nakshatra(
    val index: Int,
    val sanskritName: String,
    val lord: String
) {
    ASHWINI(0, "Ashwini", "Ketu"),
    BHARANI(1, "Bharani", "Venus"),
    KRITTIKA(2, "Krittika", "Sun"),
    ROHINI(3, "Rohini", "Moon"),
    MRIGASHIRSHA(4, "Mrigashirsha", "Mars"),
    ARDRA(5, "Ardra", "Rahu"),
    PUNARVASU(6, "Punarvasu", "Jupiter"),
    PUSHYA(7, "Pushya", "Saturn"),
    ASHLESHA(8, "Ashlesha", "Mercury"),
    MAGHA(9, "Magha", "Ketu"),
    PURVA_PHALGUNI(10, "Purva Phalguni", "Venus"),
    UTTARA_PHALGUNI(11, "Uttara Phalguni", "Sun"),
    HASTA(12, "Hasta", "Moon"),
    CHITRA(13, "Chitra", "Mars"),
    SWATI(14, "Swati", "Rahu"),
    VISHAKHA(15, "Vishakha", "Jupiter"),
    ANURADHA(16, "Anuradha", "Saturn"),
    JYESHTHA(17, "Jyeshtha", "Mercury"),
    MULA(18, "Mula", "Ketu"),
    PURVA_ASHADHA(19, "Purva Ashadha", "Venus"),
    UTTARA_ASHADHA(20, "Uttara Ashadha", "Sun"),
    SHRAVANA(21, "Shravana", "Moon"),
    DHANISHTA(22, "Dhanishta", "Mars"),
    SHATABHISHA(23, "Shatabhisha", "Rahu"),
    PURVA_BHADRAPADA(24, "Purva Bhadrapada", "Jupiter"),
    UTTARA_BHADRAPADA(25, "Uttara Bhadrapada", "Saturn"),
    REVATI(26, "Revati", "Mercury");

    companion object {
        const val SPAN_DEGREES = 360.0 / 27.0 // 13.333333333333334° (13° 20')
        const val PADA_SPAN_DEGREES = SPAN_DEGREES / 4.0 // 3.3333333333333335° (3° 20')

        fun fromLongitude(longitude: Double): Pair<Nakshatra, Int> {
            val normalized = longitude.mod(360.0)
            val nakshatraIndex = (normalized / SPAN_DEGREES).toInt().coerceIn(0, 26)
            val degreeWithinNakshatra = normalized - (nakshatraIndex * SPAN_DEGREES)
            val pada = (degreeWithinNakshatra / PADA_SPAN_DEGREES).toInt() + 1
            return Pair(entries[nakshatraIndex], pada.coerceIn(1, 4))
        }
    }
}

/**
 * Detailed planet position structure
 */
data class PlanetPosition(
    val planet: String,
    val sign: String,
    val signIndex: Int,
    val totalLongitude: Double,
    val degreeInSign: Double,
    val house: Int,
    val isRetrograde: Boolean,
    val nakshatra: String,
    val nakshatraLord: String,
    val nakshatraPada: Int,
    val speed: Double
) {
    val formattedDegree: String
        get() {
            val d = degreeInSign.toInt()
            val m = ((degreeInSign - d) * 60).toInt()
            val s = ((((degreeInSign - d) * 60) - m) * 60).toInt()
            return "%02d° %02d' %02d\"".format(d, m, s)
        }
}

/**
 * Calculation Metadata for full auditability and reproducibility
 */
data class CalculationMetadata(
    val ephemerisEngine: String = "Swiss Ephemeris (Moshier Sidereal)",
    val ayanamsaName: String = "Lahiri (Chitra Paksha)",
    val ayanamsaDegree: Double,
    val houseSystem: String = "Vedic Whole Sign (Rashi Bhava)",
    val julianDayUt: Double,
    val calculatedUtcIso: String
)

data class Chart(
    val type: String,
    val positions: List<PlanetPosition>
)

data class AstrologyProfile(
    val birthData: BirthData,
    val rashiChart: Chart,
    val lagna: String,
    val lagnaSignIndex: Int,
    val lagnaLongitude: Double,
    val lagnaDegreeInSign: Double,
    val lagnaNakshatra: String,
    val lagnaPada: Int,
    val moonSign: String,
    val moonSignIndex: Int,
    val nakshatra: String,
    val nakshatraPada: Int,
    val nakshatraLord: String,
    val planetPositions: List<PlanetPosition>,
    val metadata: CalculationMetadata
)

data class DashaPeriod(
    val planet: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val subPeriods: List<DashaPeriod> = emptyList()
)

data class Transit(val planet: String, val currentSign: String, val degree: Double)

data class Prediction(
    val category: String,
    val interpretation: String,
    val intensity: String
)

data class PanchangData(
    val tithi: String,
    val yoga: String,
    val karana: String,
    val sunrise: LocalDateTime,
    val sunset: LocalDateTime
)

data class Muhurta(
    val activityName: String,
    val isFavorable: Boolean,
    val explanation: String
)

data class CompatibilityResult(
    val score: Double,
    val pros: List<String>,
    val cons: List<String>,
    val explanation: String
)

data class NumerologyResult(
    val lifePathNumber: Int,
    val destinyNumber: Int
)

data class UserProfile(
    val id: String,
    val primaryBirthData: BirthData?
)
