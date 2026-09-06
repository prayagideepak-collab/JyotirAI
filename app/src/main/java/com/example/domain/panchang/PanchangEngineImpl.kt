package com.example.domain.panchang

import com.example.domain.models.*
import de.thmac.swisseph.SweConst
import de.thmac.swisseph.SwissEph
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Robust, production implementation of the Phase 9 Panchang Engine.
 * Thread-safe, cached, highly accurate Swiss Ephemeris based astrological almanac.
 */
class PanchangEngineImpl(
    private val swe: SwissEph = SwissEph(),
    private val cache: PanchangCache = PanchangCache()
) : PanchangEngine {

    private val engineLock = Any()
    private val engineVersion = "JyotirAI-Panchang-v2.0-SwissEph"

    override suspend fun calculatePanchang(
        date: ZonedDateTime,
        location: BirthLocation
    ): Result<PanchangSnapshot> {
        return calculatePanchangResult(date, location).map { it.toSnapshot() }
    }

    override suspend fun calculatePanchangForDate(
        targetDate: LocalDate,
        location: BirthLocation
    ): Result<PanchangResult> {
        return try {
            val locContext = LocationContextResolver.resolve(location)
            val calculationInstant = PanchangDateResolver.resolveCalculationInstant(
                targetDate = targetDate,
                zoneId = locContext.calculationTimeZone
            )
            calculatePanchangInternal(calculationInstant, locContext)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun calculatePanchangResult(
        dateTime: ZonedDateTime,
        location: BirthLocation
    ): Result<PanchangResult> {
        return try {
            val locContext = LocationContextResolver.resolve(location, fallbackZoneId = dateTime.zone)
            val targetZoned = dateTime.withZoneSameInstant(locContext.calculationTimeZone)
            calculatePanchangInternal(targetZoned, locContext)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun clearCache() {
        cache.clear()
    }

    private fun calculatePanchangInternal(
        calculationInstant: ZonedDateTime,
        location: PanchangLocationContext
    ): Result<PanchangResult> {
        val targetDate = calculationInstant.toLocalDate()

        // 1. Check Cache
        val cached = cache.get(targetDate, location, engineVersion)
        if (cached != null) {
            return Result.success(cached)
        }

        // 2. Validate Inputs
        val inputValidation = PanchangValidator.validateInput(targetDate, location)
        if (!inputValidation.isValid) {
            return Result.failure(
                AppError.InvalidBirthData(inputValidation.errorMessage ?: "Invalid Panchang input parameters.")
            )
        }

        return synchronized(engineLock) {
            try {
                swe.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0.0, 0.0)

                // 3. Temporal Resolution & Julian Day
                val tjdUt = PanchangDateResolver.toJulianDayUt(calculationInstant)

                // 4. Vara (Weekday) from local civil date
                val vara = VaraCalculator.calculate(targetDate)

                // 5. Sun Ephemeris
                val sunContext = SolarCalculator.calculateSun(tjdUt, swe)

                // 6. Moon Ephemeris
                val moonContext = LunarCalculator.calculateMoon(tjdUt, sunContext.longitude, swe)

                // 7. Five Angas
                val tithi = TithiCalculator.calculate(
                    sunLongitude = sunContext.longitude,
                    moonLongitude = moonContext.longitude,
                    tjdUt = tjdUt,
                    zoneId = location.calculationTimeZone,
                    swe = swe
                )
                val paksha = tithi.paksha

                val nakshatraContext = NakshatraCalculator.calculate(
                    moonLongitude = moonContext.longitude,
                    tjdUt = tjdUt,
                    zoneId = location.calculationTimeZone,
                    swe = swe
                )

                val yoga = PanchangYogaCalculator.calculate(
                    sunLongitude = sunContext.longitude,
                    moonLongitude = moonContext.longitude,
                    tjdUt = tjdUt,
                    zoneId = location.calculationTimeZone,
                    swe = swe
                )

                val karana = KaranaCalculator.calculate(
                    sunLongitude = sunContext.longitude,
                    moonLongitude = moonContext.longitude,
                    tjdUt = tjdUt,
                    zoneId = location.calculationTimeZone,
                    swe = swe
                )

                // 8. Sunrise and Sunset for Local Date
                val riseSetResult = SunriseSunsetCalculator.calculate(targetDate, location, swe)
                val sunrise = riseSetResult.sunrise
                val sunset = riseSetResult.sunset

                // 9. Muhurta Calculations
                val muhurta = calculateMuhurta(sunrise, sunset, vara)

                // 10. Lunar Observances
                val lunarObservance = LunarObservance(
                    isEkadashi = tithi.index == 11 || tithi.index == 26,
                    isPurnima = tithi.index == 15,
                    isAmavasya = tithi.index == 30,
                    isPradosh = tithi.index == 13 || tithi.index == 28,
                    isSankranti = sunContext.degreeInSign < 1.0,
                    description = buildObservanceDescription(tithi.index)
                )

                // 11. Calculation Metadata
                val ayanamsa = swe.swe_get_ayanamsa_ut(tjdUt)
                val utcDateTime = calculationInstant.withZoneSameInstant(ZoneOffset.UTC)
                val metadata = CalculationMetadata(
                    ephemerisEngine = "Swiss Ephemeris (Moshier Sidereal)",
                    ayanamsaName = "Lahiri (Chitra Paksha)",
                    ayanamsaDegree = ayanamsa,
                    julianDayUt = tjdUt,
                    calculatedUtcIso = utcDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    houseSystem = null
                )

                val limitations = inputValidation.limitations.toMutableList()
                if (riseSetResult.isPolarDay) {
                    limitations.add("Polar Day active (continuous daylight). Sunrise/Sunset unavailable.")
                }
                if (riseSetResult.isPolarNight) {
                    limitations.add("Polar Night active (continuous night). Sunrise/Sunset unavailable.")
                }

                var state = inputValidation.state
                if (limitations.isNotEmpty()) {
                    state = PanchangResultState.LIMITED_DATA
                }

                val result = PanchangResult(
                    id = UUID.randomUUID().toString(),
                    selectedDate = targetDate,
                    calculationTimestamp = calculationInstant,
                    location = location,
                    vara = vara,
                    tithi = tithi,
                    paksha = paksha,
                    nakshatra = nakshatraContext,
                    yoga = yoga,
                    karana = karana,
                    sunrise = sunrise,
                    sunset = sunset,
                    sunContext = sunContext,
                    moonContext = moonContext,
                    muhurta = muhurta,
                    lunarObservance = lunarObservance,
                    resultState = state,
                    calculationLimitations = limitations,
                    calculationEngineVersion = engineVersion,
                    metadata = metadata
                )

                // 12. Cache Result
                cache.put(result)

                Result.success(result)
            } catch (e: Exception) {
                Result.failure(AppError.CalculationError(e.message ?: "Failed to calculate Panchang"))
            }
        }
    }

    private fun calculateMuhurta(
        sunrise: ZonedDateTime?,
        sunset: ZonedDateTime?,
        vara: Vara
    ): MuhurtaInfo? {
        if (sunrise == null || sunset == null || !sunrise.isBefore(sunset)) {
            return null
        }

        val daytimeMillis = java.time.Duration.between(sunrise, sunset).toMillis()
        val partMillis = daytimeMillis / 8

        // Rahukaal: 8-part division of daytime
        val rahukaalIndex = when (vara) {
            Vara.SOMAVARA -> 1 // 2nd part
            Vara.SHANIVARA -> 2 // 3rd part
            Vara.SHUKRAVARA -> 3 // 4th part
            Vara.BUDHAVARA -> 4 // 5th part
            Vara.GURUVARA -> 5 // 6th part
            Vara.MANGALAVARA -> 6 // 7th part
            Vara.RAVIVARA -> 7 // 8th part
        }

        val rahuStart = sunrise.plusNanos(partMillis * rahukaalIndex * 1_000_000L)
        val rahuEnd = rahuStart.plusNanos(partMillis * 1_000_000L)
        val rahukaal = TimeInterval(start = rahuStart, end = rahuEnd, name = "Rahukaal", description = "Inauspicious caution window")

        // Yamaganda: 8-part division of daytime
        val yamagandaIndex = when (vara) {
            Vara.SOMAVARA -> 3
            Vara.SHANIVARA -> 0
            Vara.SHUKRAVARA -> 1
            Vara.BUDHAVARA -> 2
            Vara.GURUVARA -> 4
            Vara.MANGALAVARA -> 5
            Vara.RAVIVARA -> 4
        }
        val yamaStart = sunrise.plusNanos(partMillis * yamagandaIndex * 1_000_000L)
        val yamaEnd = yamaStart.plusNanos(partMillis * 1_000_000L)
        val yamaganda = TimeInterval(start = yamaStart, end = yamaEnd, name = "Yamaganda", description = "Inauspicious period for beginnings")

        // Gulika Kaal: 8-part division of daytime
        val gulikaIndex = when (vara) {
            Vara.SOMAVARA -> 5
            Vara.SHANIVARA -> 6
            Vara.SHUKRAVARA -> 4
            Vara.BUDHAVARA -> 3
            Vara.GURUVARA -> 2
            Vara.MANGALAVARA -> 1
            Vara.RAVIVARA -> 0
        }
        val gulikaStart = sunrise.plusNanos(partMillis * gulikaIndex * 1_000_000L)
        val gulikaEnd = gulikaStart.plusNanos(partMillis * 1_000_000L)
        val gulikaKaal = TimeInterval(start = gulikaStart, end = gulikaEnd, name = "Gulika Kaal", description = "Saturnine window suitable for routine tasks")

        // Brahma Muhurta: 96 to 48 minutes before sunrise (2 Muhurtas = 2 * 48m)
        val brahmaStart = sunrise.minusMinutes(96)
        val brahmaEnd = sunrise.minusMinutes(48)
        val brahmaMuhurta = TimeInterval(start = brahmaStart, end = brahmaEnd, name = "Brahma Muhurta", description = "Most auspicious dawn period for meditation & study")

        // Abhijit Muhurta: 8th part of 15 daytime parts (approx solar noon)
        val abhijitPartMillis = daytimeMillis / 15
        val abhijitStart = sunrise.plusNanos(abhijitPartMillis * 7 * 1_000_000L)
        val abhijitEnd = sunrise.plusNanos(abhijitPartMillis * 8 * 1_000_000L)
        val abhijitMuhurta = TimeInterval(start = abhijitStart, end = abhijitEnd, name = "Abhijit Muhurta", description = "Highly auspicious midday window")

        return MuhurtaInfo(
            rahukaal = rahukaal,
            brahmaMuhurta = brahmaMuhurta,
            abhijitMuhurta = abhijitMuhurta,
            yamaganda = yamaganda,
            gulikaKaal = gulikaKaal,
            additionalMuhurtas = listOf(yamaganda, gulikaKaal)
        )
    }

    private fun buildObservanceDescription(tithiIndex: Int): String? {
        return when (tithiIndex) {
            11, 26 -> "Ekadashi Vrata (एकादशी व्रत)"
            13, 28 -> "Pradosha Vrata (प्रदोष व्रत)"
            14, 29 -> "Masa Shivaratri / Chaturdashi"
            15 -> "Purnima Vrata (पूर्णिमा व्रत)"
            30 -> "Amavasya Pitru Tarpan (दर्श/अमावस्या)"
            else -> null
        }
    }
}
