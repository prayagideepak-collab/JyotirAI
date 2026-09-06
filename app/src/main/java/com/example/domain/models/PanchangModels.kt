package com.example.domain.models

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Result state for Phase 9 Panchang Engine calculations.
 */
enum class PanchangResultState {
    SUCCESS,
    LIMITED_DATA,
    INSUFFICIENT_DATA,
    CALCULATION_ERROR
}

/**
 * Validated location context for location-sensitive Panchang calculations.
 */
data class PanchangLocationContext(
    val placeName: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double? = null,
    val timeZoneId: String,
    val calculationTimeZone: ZoneId = ZoneId.of(timeZoneId)
) {
    fun toBirthLocation(): BirthLocation = BirthLocation(
        latitude = latitude,
        longitude = longitude,
        placeName = placeName,
        altitudeMeters = altitudeMeters,
        timeZoneId = timeZoneId,
        isVerified = true,
        source = "panchang_context"
    )

    companion object {
        fun fromBirthLocation(location: BirthLocation): PanchangLocationContext {
            val tzId = location.timeZoneId ?: "UTC"
            val zone = try {
                ZoneId.of(tzId)
            } catch (e: Exception) {
                ZoneId.of("UTC")
            }
            return PanchangLocationContext(
                placeName = location.placeName.ifBlank { "Selected Location" },
                latitude = location.latitude,
                longitude = location.longitude,
                altitudeMeters = location.altitudeMeters,
                timeZoneId = tzId,
                calculationTimeZone = zone
            )
        }
    }
}

/**
 * Comprehensive Solar ephemeris context for Panchang.
 */
data class SunContext(
    val sign: Rashi,
    val longitude: Double,
    val degreeInSign: Double,
    val nakshatra: Nakshatra,
    val pada: Int,
    val speedDegPerDay: Double = 1.0,
    val isEclipsed: Boolean = false
)

/**
 * Comprehensive Lunar ephemeris context for Panchang.
 */
data class MoonContext(
    val sign: Rashi,
    val longitude: Double,
    val degreeInSign: Double,
    val nakshatra: Nakshatra,
    val pada: Int,
    val elongation: Double,
    val phaseName: String,
    val speedDegPerDay: Double = 13.2
)

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

    val muhurta: MuhurtaInfo?,
    val lunarObservance: LunarObservance?,
    val metadata: CalculationMetadata
)

/**
 * Full, authoritative Phase 9 Panchang Result Model with complete metadata,
 * timing boundaries, and execution status.
 */
data class PanchangResult(
    val id: String = UUID.randomUUID().toString(),
    val selectedDate: LocalDate,
    val calculationTimestamp: ZonedDateTime,
    val location: PanchangLocationContext,
    val vara: Vara,
    val tithi: Tithi,
    val paksha: Paksha,
    val nakshatra: NakshatraContext,
    val yoga: NityaYoga,
    val karana: Karana,
    val sunrise: ZonedDateTime?,
    val sunset: ZonedDateTime?,
    val sunContext: SunContext,
    val moonContext: MoonContext,
    val muhurta: MuhurtaInfo?,
    val lunarObservance: LunarObservance?,
    val resultState: PanchangResultState = PanchangResultState.SUCCESS,
    val calculationLimitations: List<String> = emptyList(),
    val calculationEngineVersion: String = "JyotirAI-Panchang-v2.0-SwissEph",
    val metadata: CalculationMetadata
) {
    fun toSnapshot(): PanchangSnapshot = PanchangSnapshot(
        requestedDateTime = calculationTimestamp,
        location = location.toBirthLocation(),
        vara = vara,
        tithi = tithi,
        paksha = paksha,
        nakshatra = nakshatra,
        yoga = yoga,
        karana = karana,
        sunrise = sunrise,
        sunset = sunset,
        moonSign = moonContext.sign,
        sunSign = sunContext.sign,
        muhurta = muhurta,
        lunarObservance = lunarObservance,
        metadata = metadata
    )
}

data class TimeInterval(
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val name: String,
    val description: String? = null
)

data class MuhurtaInfo(
    val rahukaal: TimeInterval?,
    val brahmaMuhurta: TimeInterval?,
    val abhijitMuhurta: TimeInterval? = null,
    val yamaganda: TimeInterval? = null,
    val gulikaKaal: TimeInterval? = null,
    val durmuhurta: TimeInterval? = null,
    val amritKaal: TimeInterval? = null,
    val additionalMuhurtas: List<TimeInterval> = emptyList()
)

data class LunarObservance(
    val isEkadashi: Boolean,
    val isPurnima: Boolean,
    val isAmavasya: Boolean,
    val isPradosh: Boolean = false,
    val isSankranti: Boolean = false,
    val description: String? = null
)

enum class Vara(val sanskritName: String, val englishName: String, val hindiName: String) {
    RAVIVARA("Ravivara", "Sunday", "रविवार"),
    SOMAVARA("Somavara", "Monday", "सोमवार"),
    MANGALAVARA("Mangalavara", "Tuesday", "मंगलवार"),
    BUDHAVARA("Budhavara", "Wednesday", "बुधवार"),
    GURUVARA("Guruvara", "Thursday", "गुरुवार"),
    SHUKRAVARA("Shukravara", "Friday", "शुक्रवार"),
    SHANIVARA("Shanivara", "Saturday", "शनिवार");

    // Backward compatibility constructor
    constructor(sanskritName: String, englishName: String) : this(sanskritName, englishName, sanskritName)
}

enum class Paksha(val hindiName: String) {
    SHUKLA("शुक्ल पक्ष"), // Waxing
    KRISHNA("कृष्ण पक्ष") // Waning
}

data class Tithi(
    val index: Int, // 1 to 30 (1-15 Shukla, 16-30 Krishna)
    val name: String, // e.g. "Pratipada", "Dvitiya"
    val paksha: Paksha,
    val remainingPercentage: Double,
    val startTime: ZonedDateTime? = null,
    val endTime: ZonedDateTime? = null,
    val hindiName: String = name
) {
    val isPurnima get() = index == 15
    val isAmavasya get() = index == 30
}

data class NityaYoga(
    val index: Int, // 1 to 27
    val name: String,
    val remainingPercentage: Double,
    val startTime: ZonedDateTime? = null,
    val endTime: ZonedDateTime? = null,
    val hindiName: String = name
)

data class Karana(
    val index: Int, // 1 to 60
    val name: String,
    val isFixed: Boolean,
    val remainingPercentage: Double,
    val startTime: ZonedDateTime? = null,
    val endTime: ZonedDateTime? = null,
    val hindiName: String = name
)

data class NakshatraContext(
    val nakshatra: Nakshatra,
    val pada: Int,
    val remainingPercentage: Double,
    val startTime: ZonedDateTime? = null,
    val endTime: ZonedDateTime? = null,
    val degreeInNakshatra: Double = 0.0
)
