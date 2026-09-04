package com.example.domain.models

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Detailed planetary transit position in the sidereal (Nirayana) zodiac.
 */
data class TransitPosition(
    val planet: String,
    val totalLongitude: Double,
    val sign: String,
    val signIndex: Int,
    val degreeInSign: Double,
    val isRetrograde: Boolean,
    val speed: Double,
    val nakshatra: String,
    val nakshatraLord: String,
    val nakshatraPada: Int,
    val houseFromMoon: Int? = null,
    val houseFromLagna: Int? = null,
    val abbreviation: String = defaultPlanetAbbreviation(planet),
    val sanskritName: String = defaultPlanetSanskritName(planet)
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
 * Natal Reference Context against which planetary transits (Gochar) are compared.
 */
data class NatalTransitReference(
    val nativeName: String,
    val moonSign: String,
    val moonSignIndex: Int,
    val moonNakshatra: String,
    val lagnaSign: String,
    val lagnaSignIndex: Int,
    val lagnaDegreeInSign: Double
)

/**
 * Snapshot of planetary transits for a specified date, time, and location context.
 */
data class TransitSnapshot(
    val transitDateTime: ZonedDateTime,
    val location: BirthLocation,
    val positions: List<TransitPosition>,
    val metadata: CalculationMetadata,
    val natalReference: NatalTransitReference? = null,
    val transitAscendantSign: String? = null,
    val transitAscendantSignIndex: Int? = null,
    val transitAscendantDegree: Double? = null
) {
    val formattedDateTime: String
        get() = transitDateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a (z)"))
}

/**
 * Explicit relationship descriptor for Gochar relative to a natal reference.
 */
data class TransitRelation(
    val planet: String,
    val transitSign: String,
    val transitDegreeFormatted: String,
    val houseFromMoon: Int?,
    val houseFromLagna: Int?,
    val isRetrograde: Boolean,
    val moonRelationDescription: String?,
    val lagnaRelationDescription: String?
)
