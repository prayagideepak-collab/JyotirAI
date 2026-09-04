package com.example.domain.engine

import com.example.domain.models.*
import de.thmac.swisseph.DblObj
import de.thmac.swisseph.SweConst
import de.thmac.swisseph.SwissEph
import java.time.ZoneOffset
import java.time.ZonedDateTime

object PanchangCalculator {

    fun calculatePanchang(
        date: ZonedDateTime,
        location: BirthLocation,
        swe: SwissEph,
        metadataBuilder: (String) -> CalculationMetadata
    ): PanchangSnapshot {
        // 1. Vara (Weekday) from local civil date
        val vara = getVara(date)

        val utcDateTime = date.withZoneSameInstant(ZoneOffset.UTC)
        val hourDecimalUt = utcDateTime.hour +
                (utcDateTime.minute / 60.0) +
                (utcDateTime.second / 3600.0)
        
        val sweDate = de.thmac.swisseph.SweDate(
            utcDateTime.year,
            utcDateTime.monthValue,
            utcDateTime.dayOfMonth,
            hourDecimalUt
        )
        val tjdUt = sweDate.getJulDay()

        // Get Sun and Moon Sidereal Longitudes
        val flags = SweConst.SEFLG_SWIEPH or SweConst.SEFLG_SIDEREAL or SweConst.SEFLG_SPEED
        
        val sunRes = DoubleArray(6)
        val sunErr = StringBuffer()
        swe.swe_calc_ut(tjdUt, SweConst.SE_SUN, flags, sunRes, sunErr)
        val sunLon = sunRes[0]

        val moonRes = DoubleArray(6)
        val moonErr = StringBuffer()
        swe.swe_calc_ut(tjdUt, SweConst.SE_MOON, flags, moonRes, moonErr)
        val moonLon = moonRes[0]

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

        // 7. Sunrise and Sunset
        val sunrise = calculateRiseSet(tjdUt, location, swe, SweConst.SE_CALC_RISE, date)
        val sunset = calculateRiseSet(tjdUt, location, swe, SweConst.SE_CALC_SET, date)

        val moonSign = Rashi.fromLongitude(moonLon)
        val sunSign = Rashi.fromLongitude(sunLon)

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
            metadata = metadataBuilder(utcDateTime.toString())
        )
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
        val elongation = (moonLon - sunLon + 360.0) % 360.0
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
            else -> "Unknown"
        }

        return Tithi(tithiIndex, tithiName, paksha, remainingPct)
    }

    private fun calculateNakshatra(moonLon: Double): NakshatraContext {
        val pair = Nakshatra.fromLongitude(moonLon)
        val nakshatra = pair.first
        val pada = pair.second
        
        val degreeWithinNakshatra = (moonLon % 360.0) - (nakshatra.index * Nakshatra.SPAN_DEGREES)
        val remainingPct = 1.0 - (degreeWithinNakshatra / Nakshatra.SPAN_DEGREES)

        return NakshatraContext(nakshatra, pada, remainingPct)
    }

    private fun calculateYoga(sunLon: Double, moonLon: Double): NityaYoga {
        val yogaLon = (sunLon + moonLon) % 360.0
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
        
        return NityaYoga(yogaIndex, yogaNames.getOrElse(yogaIndex - 1) { "Unknown" }, remainingPct)
    }

    private fun calculateKarana(sunLon: Double, moonLon: Double): Karana {
        val elongation = (moonLon - sunLon + 360.0) % 360.0
        val karanaIndex = (elongation / 6.0).toInt() + 1 // 1 to 60

        val remainingPct = 1.0 - ((elongation % 6.0) / 6.0)

        // 60 Karanas in a lunar month
        // 1st is Kinstughna (Fixed)
        // 2nd to 57th are Movable (7 repeating: Bava, Balava, Kaulava, Taitila, Gara, Vanija, Vishti)
        // 58th is Shakuni (Fixed)
        // 59th is Chatushpada (Fixed)
        // 60th is Naga (Fixed)

        val name: String
        val isFixed: Boolean
        when (karanaIndex) {
            1 -> { name = "Kinstughna"; isFixed = true }
            58 -> { name = "Shakuni"; isFixed = true }
            59 -> { name = "Chatushpada"; isFixed = true }
            60 -> { name = "Naga"; isFixed = true }
            else -> {
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
                    else -> "Unknown"
                }
            }
        }
        return Karana(karanaIndex, name, isFixed, remainingPct)
    }

    private fun calculateRiseSet(
        tjdUt: Double,
        location: BirthLocation,
        swe: SwissEph,
        rsmi: Int, // SweConst.SE_CALC_RISE or SET
        originalDate: ZonedDateTime
    ): ZonedDateTime? {
        val geopos = doubleArrayOf(location.longitude, location.latitude, 0.0)
        val tret = DblObj()
        val serr = StringBuffer()

        val flags = SweConst.SEFLG_SWIEPH
        // Note: For sunrise/sunset we use the actual Sun (tropical or sidereal doesn't matter for horizon, but ephe flag is needed)
        // Actually, SE_BIT_DISC_CENTER or similar can be used, but default is standard.
        // We will pass rsmi as SE_CALC_RISE or SE_CALC_SET
        // Wait, the signature requires a StringBuffer for starname.
        val res = swe.swe_rise_trans(tjdUt, SweConst.SE_SUN, null, flags, rsmi, geopos, 1013.25, 15.0, tret, serr)

        if (res == -1 || res == -2) {
            return null // Sunrise/sunset not found (e.g., polar regions)
        }

        // tret.val contains Julian day in UT
        val riseSetSweDate = de.thmac.swisseph.SweDate(tret.`val`)
        val year = riseSetSweDate.year
        val month = riseSetSweDate.month
        val day = riseSetSweDate.day
        val hour = riseSetSweDate.hour.toInt()
        val min = ((riseSetSweDate.hour - hour) * 60.0).toInt()
        val sec = ((((riseSetSweDate.hour - hour) * 60.0) - min) * 60.0).toInt()

        return try {
            ZonedDateTime.of(year, month, day, hour, min, sec, 0, ZoneOffset.UTC)
                .withZoneSameInstant(originalDate.zone)
        } catch (e: Exception) {
            null
        }
    }
}
