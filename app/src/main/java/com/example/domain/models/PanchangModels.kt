package com.example.domain.models

import java.time.ZonedDateTime

/**
 * Deterministic snapshot of Vedic Panchang elements for a specific time and location.
 */
data class PanchangSnapshot(
    val requestedDateTime: ZonedDateTime,
    val location: BirthLocation,
    val vara: Vara,
    val tithi: Tithi,
    val paksha: Paksha,
    val nakshatra: NakshatraContext,
    val yoga: NityaYoga,
    val karana: Karana,
    val sunrise: ZonedDateTime?,
    val sunset: ZonedDateTime?,
    val moonSign: Rashi?,
    val sunSign: Rashi?,
    val metadata: CalculationMetadata
)

enum class Vara(val sanskritName: String, val englishName: String) {
    RAVIVARA("Ravivara", "Sunday"),
    SOMAVARA("Somavara", "Monday"),
    MANGALAVARA("Mangalavara", "Tuesday"),
    BUDHAVARA("Budhavara", "Wednesday"),
    GURUVARA("Guruvara", "Thursday"),
    SHUKRAVARA("Shukravara", "Friday"),
    SHANIVARA("Shanivara", "Saturday")
}

enum class Paksha {
    SHUKLA, // Waxing
    KRISHNA // Waning
}

data class Tithi(
    val index: Int, // 1 to 30 (1-15 Shukla, 16-30 Krishna)
    val name: String, // e.g. "Pratipada", "Dvitiya"
    val paksha: Paksha,
    val remainingPercentage: Double,
    val endTime: ZonedDateTime? = null
) {
    val isPurnima get() = index == 15
    val isAmavasya get() = index == 30
}

data class NityaYoga(
    val index: Int, // 1 to 27
    val name: String,
    val remainingPercentage: Double,
    val endTime: ZonedDateTime? = null
)

data class Karana(
    val index: Int, // 1 to 60
    val name: String,
    val isFixed: Boolean,
    val remainingPercentage: Double,
    val endTime: ZonedDateTime? = null
)

data class NakshatraContext(
    val nakshatra: Nakshatra,
    val pada: Int,
    val remainingPercentage: Double,
    val endTime: ZonedDateTime? = null
)
