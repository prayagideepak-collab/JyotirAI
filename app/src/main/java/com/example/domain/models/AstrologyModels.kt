package com.example.domain.models

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Standard 16 Vedic Divisional Charts (Shodashavarga) and key Vargas
 */
enum class VargaType(
    val code: String,
    val sanskritName: String,
    val englishName: String,
    val significations: String,
    val division: Int,
    val isCalculated: Boolean = true
) {
    D1("D1", "Rashi", "Natal / Physical Body", "Foundational life blueprint, physical constitution, vitality & general existence", 1, true),
    D2("D2", "Hora", "Wealth & Resources", "Financial prosperity, accumulated assets, speech & family wealth", 2, true),
    D3("D3", "Drekkana", "Siblings & Courage", "Siblings, vitality, courage, drive, energy & third house matters", 3, true),
    D4("D4", "Chaturthamsha", "Fortune & Property", "Fixed property, land, conveyances, domestic happiness & general luck", 4, true),
    D7("D7", "Saptamsha", "Children & Progeny", "Children, grandchildren, creative fertility, partnerships & legacy", 7, true),
    D9("D9", "Navamsha", "Dharma & Marriage", "Spouse, married life, inner spiritual potential, soul purpose & destiny", 9, true),
    D10("D10", "Dashamsha", "Profession & Status", "Career, professional accomplishments, status, power, leadership & karma", 10, true),
    D12("D12", "Dwadashamsha", "Parents & Lineage", "Parents, ancestral karma, lineage, heritage & past-life influences", 12, true),
    D16("D16", "Shodashamsha", "Vehicles & Comforts", "Vehicles, luxuries, general comforts, happiness & pleasures", 16, false),
    D20("D20", "Vimshamsha", "Spiritual Progress", "Religious inclination, devotion, upasana, meditation & spiritual depth", 20, false),
    D24("D24", "Chaturvimshamsha", "Learning & Knowledge", "Higher academic education, intellect, skills, learning & expertise", 24, false),
    D27("D27", "Saptavimshamsha", "Strengths & Weaknesses", "General subconscious strengths, weaknesses, stamina & vital force", 27, false),
    D30("D30", "Trimshamsha", "Misfortunes & Evils", "Hidden liabilities, health challenges, evils, obstacles & arishta", 30, false),
    D40("D40", "Khavedamsha", "Auspicious Events", "Auspicious and inauspicious karmic fruits and auspicious timings", 40, false),
    D45("D45", "Akshavedamsha", "General Character", "Moral integrity, soul purity, general character & fine nuances", 45, false),
    D60("D60", "Shashtiamsha", "Root Karma & Past Life", "Micro-destiny, past life karma, root causes & all matters", 60, false);

    companion object {
        fun fromCode(code: String): VargaType {
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: D1
        }
    }
}

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
        fun fromLongitude(longitude: Double): Rashi {
            val normalized = (longitude % 360.0 + 360.0) % 360.0
            val idx = (normalized / 30.0).toInt().coerceIn(0, 11)
            return entries[idx]
        }
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
 * Classical Parashari Planetary Dignity (Avastha / Sthana Bala aspect)
 */
enum class PlanetDignity(val displayName: String, val sanskritName: String) {
    EXALTED("Exalted", "उच्च"),
    MOOLATRIKONA("Moolatrikona", "मूलत्रिकोण"),
    OWN_SIGN("Own Sign", "स्वक्षेत्र"),
    FRIEND("Friend", "मित्र"),
    NEUTRAL("Neutral", "सम"),
    ENEMY("Enemy", "शत्रु"),
    DEBILITATED("Debilitated", "नीच");

    companion object {
        fun calculate(planet: String, signIndex: Int, degreeInSign: Double = 0.0): PlanetDignity {
            val p = planet.lowercase().trim()
            return when (p) {
                "sun", "surya" -> when (signIndex) {
                    0 -> EXALTED // Aries (Deep exaltation at 10°)
                    6 -> DEBILITATED // Libra (Deep debilitation at 10°)
                    4 -> if (degreeInSign <= 20.0) MOOLATRIKONA else OWN_SIGN // Leo
                    3, 7, 8, 11 -> FRIEND // Cancer, Scorpio, Sagittarius, Pisces
                    2, 5 -> NEUTRAL // Gemini, Virgo
                    1, 9, 10 -> ENEMY // Taurus, Capricorn, Aquarius
                    else -> NEUTRAL
                }
                "moon", "chandra" -> when (signIndex) {
                    1 -> if (degreeInSign <= 3.0) EXALTED else MOOLATRIKONA // Taurus
                    7 -> DEBILITATED // Scorpio
                    3 -> OWN_SIGN // Cancer
                    0, 2, 4, 5 -> FRIEND // Aries, Gemini, Leo, Virgo
                    else -> NEUTRAL
                }
                "mars", "mangala" -> when (signIndex) {
                    9 -> EXALTED // Capricorn (Deep exaltation at 28°)
                    3 -> DEBILITATED // Cancer (Deep debilitation at 28°)
                    0 -> if (degreeInSign <= 12.0) MOOLATRIKONA else OWN_SIGN // Aries
                    7 -> OWN_SIGN // Scorpio
                    4, 8, 11 -> FRIEND // Leo, Sagittarius, Pisces
                    1, 6 -> NEUTRAL // Taurus, Libra
                    2, 5 -> ENEMY // Gemini, Virgo
                    else -> NEUTRAL
                }
                "mercury", "budha" -> when (signIndex) {
                    5 -> if (degreeInSign <= 15.0) EXALTED else if (degreeInSign <= 20.0) MOOLATRIKONA else OWN_SIGN // Virgo
                    11 -> DEBILITATED // Pisces (Deep debilitation at 15°)
                    2 -> OWN_SIGN // Gemini
                    1, 4, 6 -> FRIEND // Taurus, Leo, Libra
                    0, 7, 9, 10 -> NEUTRAL // Aries, Scorpio, Capricorn, Aquarius
                    3 -> ENEMY // Cancer
                    else -> NEUTRAL
                }
                "jupiter", "guru" -> when (signIndex) {
                    3 -> EXALTED // Cancer (Deep exaltation at 5°)
                    9 -> DEBILITATED // Capricorn (Deep debilitation at 5°)
                    8 -> if (degreeInSign <= 10.0) MOOLATRIKONA else OWN_SIGN // Sagittarius
                    11 -> OWN_SIGN // Pisces
                    0, 4, 7 -> FRIEND // Aries, Leo, Scorpio
                    10 -> NEUTRAL // Aquarius
                    1, 2, 5, 6 -> ENEMY // Taurus, Gemini, Virgo, Libra
                    else -> NEUTRAL
                }
                "venus", "shukra" -> when (signIndex) {
                    11 -> EXALTED // Pisces (Deep exaltation at 27°)
                    5 -> DEBILITATED // Virgo (Deep debilitation at 27°)
                    6 -> if (degreeInSign <= 15.0) MOOLATRIKONA else OWN_SIGN // Libra
                    1 -> OWN_SIGN // Taurus
                    2, 9, 10 -> FRIEND // Gemini, Capricorn, Aquarius
                    7, 8 -> NEUTRAL // Scorpio, Sagittarius
                    0, 3, 4 -> ENEMY // Aries, Cancer, Leo
                    else -> NEUTRAL
                }
                "saturn", "shani" -> when (signIndex) {
                    6 -> EXALTED // Libra (Deep exaltation at 20°)
                    0 -> DEBILITATED // Aries (Deep debilitation at 20°)
                    10 -> if (degreeInSign <= 20.0) MOOLATRIKONA else OWN_SIGN // Aquarius
                    9 -> OWN_SIGN // Capricorn
                    1, 2, 5 -> FRIEND // Taurus, Gemini, Virgo
                    8, 11 -> NEUTRAL // Sagittarius, Pisces
                    3, 4, 7 -> ENEMY // Cancer, Leo, Scorpio
                    else -> NEUTRAL
                }
                "rahu" -> when (signIndex) {
                    1, 2 -> EXALTED // Taurus / Gemini
                    7, 8 -> DEBILITATED // Scorpio / Sagittarius
                    10 -> OWN_SIGN // Aquarius co-lord
                    else -> NEUTRAL
                }
                "ketu" -> when (signIndex) {
                    7, 8 -> EXALTED // Scorpio / Sagittarius
                    1, 2 -> DEBILITATED // Taurus / Gemini
                    else -> NEUTRAL
                }
                else -> NEUTRAL
            }
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
    val speed: Double,
    val abbreviation: String = defaultPlanetAbbreviation(planet),
    val sanskritName: String = defaultPlanetSanskritName(planet),
    val dignity: PlanetDignity = PlanetDignity.calculate(planet, signIndex, degreeInSign)
) {
    val formattedDegree: String
        get() {
            val d = degreeInSign.toInt()
            val m = ((degreeInSign - d) * 60).toInt()
            val s = ((((degreeInSign - d) * 60) - m) * 60).toInt()
            return "%02d° %02d' %02d\"".format(d, m, s)
        }

    val rashiEnum: Rashi
        get() = Rashi.fromIndex(signIndex)

    companion object {
        fun defaultPlanetAbbreviation(planet: String): String = when (planet.lowercase().trim()) {
            "sun" -> "Su"
            "moon" -> "Mo"
            "mars" -> "Ma"
            "mercury" -> "Me"
            "jupiter" -> "Ju"
            "venus" -> "Ve"
            "saturn" -> "Sa"
            "rahu" -> "Ra"
            "ketu" -> "Ke"
            "ascendant", "lagna" -> "Asc"
            else -> planet.take(2).replaceFirstChar { it.uppercase() }
        }

        fun defaultPlanetSanskritName(planet: String): String = when (planet.lowercase().trim()) {
            "sun" -> "Surya"
            "moon" -> "Chandra"
            "mars" -> "Mangala"
            "mercury" -> "Budha"
            "jupiter" -> "Guru"
            "venus" -> "Shukra"
            "saturn" -> "Shani"
            "rahu" -> "Rahu"
            "ketu" -> "Ketu"
            "ascendant", "lagna" -> "Lagna"
            else -> planet
        }
    }
}

/**
 * Calculation Metadata for full auditability and reproducibility
 */
data class CalculationMetadata(
    val ephemerisEngine: String = "Swiss Ephemeris (Moshier Sidereal)",
    val ayanamsaName: String = "Lahiri (Chitra Paksha)",
    val ayanamsaDegree: Double,
    val houseSystem: String? = "Vedic Whole Sign (Rashi Bhava)",
    val julianDayUt: Double,
    val calculatedUtcIso: String
)

data class Chart(
    val type: String,
    val positions: List<PlanetPosition>,
    val vargaType: VargaType = VargaType.fromCode(type),
    val title: String = "${VargaType.fromCode(type).code} — ${VargaType.fromCode(type).sanskritName}",
    val sanskritTitle: String = VargaType.fromCode(type).sanskritName,
    val description: String = VargaType.fromCode(type).significations,
    val ascendantSign: String = "",
    val ascendantSignIndex: Int = 0,
    val ascendantDegreeInSign: Double = 0.0,
    val ascendantNakshatra: String = "",
    val ascendantPada: Int = 1
) {
    fun getPlanetsInHouse(houseNumber: Int): List<PlanetPosition> =
        positions.filter { it.house == houseNumber }

    fun getPlanetsInSign(signIndex: Int): List<PlanetPosition> =
        positions.filter { it.signIndex == signIndex }
}

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
