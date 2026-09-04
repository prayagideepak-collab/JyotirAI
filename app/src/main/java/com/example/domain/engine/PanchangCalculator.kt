package com.example.domain.engine

import com.example.domain.models.*
import de.thmac.swisseph.DblObj
import de.thmac.swisseph.SweConst
import de.thmac.swisseph.SwissEph
import de.thmac.swisseph.SweDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object PanchangCalculator {

    fun calculatePanchang(
        date: ZonedDateTime,
        location: BirthLocation,
        swe: SwissEph
    ): PanchangSnapshot {
        // 1. Vara (Weekday) from local civil date
        val vara = getVara(date)
        
        val utcDateTime = date.withZoneSameInstant(ZoneOffset.UTC)
        val hourDecimalUt = utcDateTime.hour +
                (utcDateTime.minute / 60.0) +
                (utcDateTime.second / 3600.0) +
                (utcDateTime.nano / 3_600_000_000_000.0)
                
        val sweDate = SweDate(
            utcDateTime.year,
            utcDateTime.monthValue,
            utcDateTime.dayOfMonth,
            hourDecimalUt
        )
        val tjdUt = sweDate.julDay

        // Ensure Lahiri Ayanamsa is configured for this ThreadLocal SwissEph instance
        swe.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0.0, 0.0)

        // Flags matches the deterministic Vedic engine configuration
        val flags = SweConst.SEFLG_MOSEPH or SweConst.SEFLG_SIDEREAL or SweConst.SEFLG_SPEED
        
        val sunRes = DoubleArray(6)
        val sunErr = StringBuffer()
        val sunFlag = swe.swe_calc_ut(tjdUt, SweConst.SE_SUN, flags, sunRes, sunErr)
        if (sunFlag < 0) {
            throw AppError.CalculationError("Error calculating Sun position: $sunErr")
        }
        val sunLon = normalizeDegree(sunRes[0])
        if (sunLon.isNaN() || sunLon.isInfinite()) {
            throw AppError.CalculationError("Sun longitude is invalid: $sunLon")
        }

        val moonRes = DoubleArray(6)
        val moonErr = StringBuffer()
        val moonFlag = swe.swe_calc_ut(tjdUt, SweConst.SE_MOON, flags, moonRes, moonErr)
        if (moonFlag < 0) {
            throw AppError.CalculationError("Error calculating Moon position: $moonErr")
        }
        val moonLon = normalizeDegree(moonRes[0])
        if (moonLon.isNaN() || moonLon.isInfinite()) {
            throw AppError.CalculationError("Moon longitude is invalid: $moonLon")
        }

        // 2. Tithi
        val tithi = calculateTithi(sunLon, moonLon)
        
        // 3. Paksha
        val paksha = tithi.paksha

        // 4. Nakshatra
        val nakshatraContext = calculateNakshatra(moonLon)
        
        // 5. Yoga
        val yoga = calculateYoga(sunLon, moonLon)

        // 6. Karana
        val karana = calculateKarana(sunLon, moonLon)

        // 7. Sunrise and Sunset for LOCAL CIVIL DATE
        // Find local midnight (start of the day)
        val localMidnight = date.toLocalDate().atStartOfDay(date.zone)
        val localMidnightUtc = localMidnight.withZoneSameInstant(ZoneOffset.UTC)
        val localMidnightHourDecimal = localMidnightUtc.hour +
                (localMidnightUtc.minute / 60.0) +
                (localMidnightUtc.second / 3600.0)
        val localMidnightSweDate = SweDate(
            localMidnightUtc.year,
            localMidnightUtc.monthValue,
            localMidnightUtc.dayOfMonth,
            localMidnightHourDecimal
        )
        val tjdUtMidnight = localMidnightSweDate.julDay

        val sunrise = calculateRiseSet(tjdUtMidnight, location, swe, SweConst.SE_CALC_RISE, date)
        val sunset = calculateRiseSet(tjdUtMidnight, location, swe, SweConst.SE_CALC_SET, date)

        val moonSign = Rashi.fromLongitude(moonLon)
        val sunSign = Rashi.fromLongitude(sunLon)

        val ayanamsa = swe.swe_get_ayanamsa_ut(tjdUt)

        val metadata = CalculationMetadata(
            ephemerisEngine = "Swiss Ephemeris (Moshier Sidereal)",
            ayanamsaName = "Lahiri (Chitra Paksha)",
            ayanamsaDegree = ayanamsa,
            julianDayUt = tjdUt,
            calculatedUtcIso = utcDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            houseSystem = null
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
            moonSign = moonSign,
            sunSign = sunSign,
            metadata = metadata
        )
    }

    private fun normalizeDegree(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    private fun getVara(date: ZonedDateTime): Vara {
        return when (date.dayOfWeek.value) {
            1 -> Vara.SOMAVARA // Monday
            2 -> Vara.MANGALAVARA
            3 -> Vara.BUDHAVARA
            4 -> Vara.GURUVARA
            5 -> Vara.SHUKRAVARA
            6 -> Vara.SHANIVARA
            7 -> Vara.RAVIVARA // Sunday
            else -> Vara.RAVIVARA
        }
    }

    private fun calculateTithi(sunLon: Double, moonLon: Double): Tithi {
        val elongation = normalizeDegree(moonLon - sunLon)
        val tithiIndex = (elongation / 12.0).toInt() + 1 // 1 to 30
        
        val paksha = if (tithiIndex <= 15) Paksha.SHUKLA else Paksha.KRISHNA
        val remainingPct = 1.0 - ((elongation % 12.0) / 12.0)

        val tithiName = when (tithiIndex) {
            1, 16 -> "Pratipada"
            2, 17 -> "Dvitiya"
            3, 18 -> "Tritiya"
            4, 19 -> "Chaturthi"
            5, 20 -> "Panchami"
            6, 21 -> "Shashthi"
            7, 22 -> "Saptami"
            8, 23 -> "Ashtami"
            9, 24 -> "Navami"
            10, 25 -> "Dashami"
            11, 26 -> "Ekadashi"
            12, 27 -> "Dvadashi"
            13, 28 -> "Trayodashi"
            14, 29 -> "Chaturdashi"
            15 -> "Purnima"
            30 -> "Amavasya"
            else -> throw AppError.CalculationError("Invalid Tithi index: $tithiIndex")
        }

        return Tithi(tithiIndex, tithiName, paksha, remainingPct.coerceIn(0.0, 1.0))
    }

    private fun calculateNakshatra(moonLon: Double): NakshatraContext {
        val pair = Nakshatra.fromLongitude(moonLon)
        val nakshatra = pair.first
        val pada = pair.second
        
        // Mathematically correct calculation relative to current Nakshatra boundaries
        val nakshatraStartDegree = nakshatra.index * Nakshatra.SPAN_DEGREES
        var degreeWithinNakshatra = moonLon - nakshatraStartDegree
        if (degreeWithinNakshatra < 0) degreeWithinNakshatra += 360.0
        
        val remainingPct = 1.0 - (degreeWithinNakshatra / Nakshatra.SPAN_DEGREES)

        return NakshatraContext(nakshatra, pada, remainingPct.coerceIn(0.0, 1.0))
    }

    private fun calculateYoga(sunLon: Double, moonLon: Double): NityaYoga {
        val yogaLon = normalizeDegree(sunLon + moonLon)
        val span = 360.0 / 27.0
        val yogaIndex = (yogaLon / span).toInt() + 1 // 1 to 27
        
        val remainingPct = 1.0 - ((yogaLon % span) / span)

        val yogaNames = listOf(
            "Vishkambha", "Priti", "Ayushman", "Saubhagya", "Shobhana",
            "Atiganda", "Sukarma", "Dhriti", "Shula", "Ganda",
            "Vriddhi", "Dhruva", "Vyaghata", "Harshana", "Vajra",
            "Siddhi", "Vyatipata", "Variyan", "Parigha", "Shiva",
            "Siddha", "Sadhya", "Shubha", "Shukla", "Brahma",
            "Indra", "Vaidhriti"
        )
        
        val name = yogaNames.getOrNull(yogaIndex - 1) ?: throw AppError.CalculationError("Invalid Yoga index: $yogaIndex")
        
        return NityaYoga(yogaIndex, name, remainingPct.coerceIn(0.0, 1.0))
    }

    private fun calculateKarana(sunLon: Double, moonLon: Double): Karana {
        val elongation = normalizeDegree(moonLon - sunLon)
        val karanaIndex = (elongation / 6.0).toInt() + 1 // 1 to 60
        val remainingPct = 1.0 - ((elongation % 6.0) / 6.0)

        // 60 Karanas in a lunar month
        // 1st is Kimstughna (Fixed)
        // 2nd to 57th are Movable (7 repeating: Bava, Balava, Kaulava, Taitila, Gara, Vanija, Vishti)
        // 58th is Shakuni (Fixed)
        // 59th is Chatushpada (Fixed)
        // 60th is Naga (Fixed)
        val name: String
        val isFixed: Boolean

        when (karanaIndex) {
            1 -> { name = "Kimstughna"; isFixed = true }
            58 -> { name = "Shakuni"; isFixed = true }
            59 -> { name = "Chatushpada"; isFixed = true }
            60 -> { name = "Naga"; isFixed = true }
            in 2..57 -> {
                isFixed = false
                val movableIndex = (karanaIndex - 2) % 7
                name = when (movableIndex) {
                    0 -> "Bava"
                    1 -> "Balava"
                    2 -> "Kaulava"
                    3 -> "Taitila"
                    4 -> "Gara"
                    5 -> "Vanija"
                    6 -> "Vishti"
                    else -> throw AppError.CalculationError("Invalid Karana movable index: $movableIndex")
                }
            }
            else -> throw AppError.CalculationError("Invalid Karana index: $karanaIndex")
        }

        return Karana(karanaIndex, name, isFixed, remainingPct.coerceIn(0.0, 1.0))
    }

    private fun calculateRiseSet(
        tjdUtMidnight: Double,
        location: BirthLocation,
        swe: SwissEph,
        rsmi: Int, // SweConst.SE_CALC_RISE or SET
        originalDate: ZonedDateTime
    ): ZonedDateTime? {
        val geopos = doubleArrayOf(location.longitude, location.latitude, 0.0)
        val flags = SweConst.SEFLG_MOSEPH
        val targetDate = originalDate.toLocalDate()
        
        // Start search slightly before local midnight to avoid boundary misses
        var currentTjd = tjdUtMidnight - 0.5
        var iterations = 0
        
        while (iterations < 5) { // Bounded search
            val tret = DblObj()
            val serr = StringBuffer()
            
            val res = swe.swe_rise_trans(currentTjd, SweConst.SE_SUN, null, flags, rsmi, geopos, 1013.25, 15.0, tret, serr)
            
            if (res == -1 || res == -2 || res < 0) {
                return null // Event not found (e.g., polar region) or error
            }
            
            val eventJulianDay = tret.`val`
            val eventUtc = convertJulianDayToZonedDateTime(eventJulianDay) ?: return null
            val eventLocal = eventUtc.withZoneSameInstant(originalDate.zone)
            val eventLocalDate = eventLocal.toLocalDate()
            
            if (eventLocalDate == targetDate) {
                return eventLocal
            } else if (eventLocalDate.isAfter(targetDate)) {
                // Passed the target date, event does not happen on this day
                return null
            }
            
            // Advance search time just past this event
            currentTjd = eventJulianDay + 0.01
            iterations++
        }
        
        return null
    }
    
    private fun convertJulianDayToZonedDateTime(jd: Double): ZonedDateTime? {
        return try {
            val sweDate = SweDate(jd)
            val year = sweDate.year
            val month = sweDate.month
            val day = sweDate.day
            val hour = sweDate.hour.toInt()
            val min = ((sweDate.hour - hour) * 60.0).toInt()
            val sec = ((((sweDate.hour - hour) * 60.0) - min) * 60.0).toInt()
            ZonedDateTime.of(year, month, day, hour, min, sec, 0, ZoneOffset.UTC)
        } catch (e: Exception) {
            null
        }
    }
}
