package com.example.domain.engine

import androidx.annotation.VisibleForTesting
import com.example.domain.models.*
import com.example.domain.panchang.*
import de.thmac.swisseph.DblObj
import de.thmac.swisseph.SweConst
import de.thmac.swisseph.SweDate
import de.thmac.swisseph.SwissEph
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Vedic Panchang Calculator.
 * Maintained for direct access and backward compatibility while backed by Phase 9 Panchang Engine components.
 */
object PanchangCalculator {

    fun calculatePanchang(
        date: ZonedDateTime,
        location: BirthLocation,
        swe: SwissEph
    ): PanchangSnapshot {
        val locContext = LocationContextResolver.resolve(location, fallbackZoneId = date.zone)
        val targetZoned = date.withZoneSameInstant(locContext.calculationTimeZone)
        val targetDate = targetZoned.toLocalDate()

        // 1. Vara (Weekday) from local civil date
        val vara = VaraCalculator.calculate(targetDate)

        val tjdUt = PanchangDateResolver.toJulianDayUt(targetZoned)

        // Ensure Lahiri Ayanamsa
        swe.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0.0, 0.0)

        // Ephemeris calculation
        val sunContext = SolarCalculator.calculateSun(tjdUt, swe)
        val moonContext = LunarCalculator.calculateMoon(tjdUt, sunContext.longitude, swe)

        // 2. Tithi
        val tithi = TithiCalculator.calculate(
            sunLongitude = sunContext.longitude,
            moonLongitude = moonContext.longitude,
            tjdUt = tjdUt,
            zoneId = locContext.calculationTimeZone,
            swe = swe
        )
        val paksha = tithi.paksha

        // 3. Nakshatra
        val nakshatraContext = NakshatraCalculator.calculate(
            moonLongitude = moonContext.longitude,
            tjdUt = tjdUt,
            zoneId = locContext.calculationTimeZone,
            swe = swe
        )

        // 4. Yoga
        val yoga = PanchangYogaCalculator.calculate(
            sunLongitude = sunContext.longitude,
            moonLongitude = moonContext.longitude,
            tjdUt = tjdUt,
            zoneId = locContext.calculationTimeZone,
            swe = swe
        )

        // 5. Karana
        val karana = KaranaCalculator.calculate(
            sunLongitude = sunContext.longitude,
            moonLongitude = moonContext.longitude,
            tjdUt = tjdUt,
            zoneId = locContext.calculationTimeZone,
            swe = swe
        )

        // 6. Sunrise and Sunset for LOCAL CIVIL DATE
        val riseSet = SunriseSunsetCalculator.calculate(targetDate, locContext, swe)
        val sunrise = riseSet.sunrise
        val sunset = riseSet.sunset

        val ayanamsa = swe.swe_get_ayanamsa_ut(tjdUt)
        val utcDateTime = targetZoned.withZoneSameInstant(ZoneOffset.UTC)

        val metadata = CalculationMetadata(
            ephemerisEngine = "Swiss Ephemeris (Moshier Sidereal)",
            ayanamsaName = "Lahiri (Chitra Paksha)",
            ayanamsaDegree = ayanamsa,
            julianDayUt = tjdUt,
            calculatedUtcIso = utcDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            houseSystem = null
        )

        val muhurta = calculateMuhurta(sunrise, sunset, vara)
        val lunarObservance = LunarObservance(
            isEkadashi = tithi.index == 11 || tithi.index == 26,
            isPurnima = tithi.index == 15,
            isAmavasya = tithi.index == 30,
            isPradosh = tithi.index == 13 || tithi.index == 28,
            isSankranti = sunContext.degreeInSign < 1.0,
            description = null
        )

        return PanchangSnapshot(
            requestedDateTime = date,
            location = location,
            vara = vara,
            tithi = tithi,
            paksha = paksha,
            nakshatra = nakshatraContext,
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

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun normalizeDegree(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getVara(date: ZonedDateTime): Vara {
        return VaraCalculator.calculate(date)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun calculateTithi(sunLon: Double, moonLon: Double): Tithi {
        return TithiCalculator.calculate(sunLon, moonLon)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun calculateNakshatra(moonLon: Double): NakshatraContext {
        return NakshatraCalculator.calculate(moonLon)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun calculateYoga(sunLon: Double, moonLon: Double): NityaYoga {
        return PanchangYogaCalculator.calculate(sunLon, moonLon)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun calculateKarana(sunLon: Double, moonLon: Double): Karana {
        return KaranaCalculator.calculate(sunLon, moonLon)
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

        val rahukaal = TimeInterval(
            start = rahuStart,
            end = rahuEnd,
            name = "Rahukaal"
        )

        val brahmaStart = sunrise.minusMinutes(96)
        val brahmaEnd = sunrise.minusMinutes(48)

        val brahmaMuhurta = TimeInterval(
            start = brahmaStart,
            end = brahmaEnd,
            name = "Brahma Muhurta"
        )

        val abhijitPartMillis = daytimeMillis / 15
        val abhijitStart = sunrise.plusNanos(abhijitPartMillis * 7 * 1_000_000L)
        val abhijitEnd = sunrise.plusNanos(abhijitPartMillis * 8 * 1_000_000L)

        val abhijitMuhurta = TimeInterval(
            start = abhijitStart,
            end = abhijitEnd,
            name = "Abhijit Muhurta"
        )

        return MuhurtaInfo(
            rahukaal = rahukaal,
            brahmaMuhurta = brahmaMuhurta,
            abhijitMuhurta = abhijitMuhurta,
            additionalMuhurtas = emptyList()
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun convertJulianDayToZonedDateTime(jd: Double): ZonedDateTime? {
        return PanchangDateResolver.fromJulianDayUt(jd, ZoneOffset.UTC)
    }
}
