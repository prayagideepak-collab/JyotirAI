package com.example

import com.example.domain.engine.PredictionCalculator
import com.example.domain.engine.YogaDoshaCalculator
import com.example.domain.engine.prediction.*
import com.example.domain.models.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PredictionEngineTest {

    private fun pos(
        planet: String,
        rashi: Rashi,
        house: Int,
        degreeInSign: Double = 15.0,
        isRetrograde: Boolean = false,
        dignity: PlanetDignity = PlanetDignity.OWN_SIGN
    ): PlanetPosition {
        val totalDeg = (rashi.index * 30.0) + degreeInSign
        return PlanetPosition(
            planet = planet,
            sign = rashi.englishName,
            signIndex = rashi.index,
            totalLongitude = totalDeg,
            degreeInSign = degreeInSign,
            house = house,
            isRetrograde = isRetrograde,
            nakshatra = "Ashwini",
            nakshatraLord = "Ketu",
            nakshatraPada = 1,
            speed = 1.0,
            dignity = dignity
        )
    }

    private fun createSampleProfile(
        name: String = "Test Native",
        lagnaSignIndex: Int = 0, // Aries
        planets: List<PlanetPosition>
    ): AstrologyProfile {
        val birthData = BirthData(
            name = name,
            date = LocalDate.of(1995, 5, 15),
            time = LocalTime.of(10, 30),
            timeZone = ZoneId.of("Asia/Kolkata"),
            location = BirthLocation(
                latitude = 28.6139,
                longitude = 77.2090,
                placeName = "New Delhi",
                timeZoneId = "Asia/Kolkata"
            )
        )

        val chart = Chart(
            type = "D1",
            positions = planets,
            ascendantSign = Rashi.fromIndex(lagnaSignIndex).englishName,
            ascendantSignIndex = lagnaSignIndex,
            ascendantDegreeInSign = 12.0
        )

        val moon = planets.firstOrNull { it.planet.equals("moon", ignoreCase = true) }

        return AstrologyProfile(
            birthData = birthData,
            rashiChart = chart,
            lagna = Rashi.fromIndex(lagnaSignIndex).englishName,
            lagnaSignIndex = lagnaSignIndex,
            lagnaLongitude = lagnaSignIndex * 30.0 + 12.0,
            lagnaDegreeInSign = 12.0,
            lagnaNakshatra = "Ashwini",
            lagnaPada = 1,
            moonSign = moon?.sign ?: "Aries",
            moonSignIndex = moon?.signIndex ?: 0,
            nakshatra = moon?.nakshatra ?: "Ashwini",
            nakshatraPada = moon?.nakshatraPada ?: 1,
            nakshatraLord = moon?.nakshatraLord ?: "Ketu",
            planetPositions = planets,
            metadata = CalculationMetadata(
                ayanamsaDegree = 23.75,
                julianDayUt = 2449852.5,
                calculatedUtcIso = "1995-05-15T05:00:00Z"
            )
        )
    }

    private fun createSampleDashaTimeline(
        currentMaha: DashaPlanet = DashaPlanet.JUPITER,
        currentAntar: DashaPlanet = DashaPlanet.SATURN
    ): DashaTimeline {
        val now = ZonedDateTime.now()
        val antarList = listOf(
            AntardashaPeriod(
                mahadashaLord = currentMaha,
                antardashaLord = currentAntar,
                startDate = now.minusMonths(6),
                endDate = now.plusMonths(6),
                durationYears = 1.0,
                isCurrent = true
            )
        )
        val mahaList = listOf(
            MahadashaPeriod(
                planet = currentMaha,
                startDate = now.minusYears(2),
                endDate = now.plusYears(14),
                totalDurationYears = 16.0,
                isCurrent = true,
                antardashas = antarList
            )
        )
        return DashaTimeline(
            birthNakshatra = Nakshatra.ASHWINI,
            nakshatraLord = "Ketu",
            startingMahadasha = currentMaha,
            startingBalance = DashaBalance(5, 0, 0, 5.0),
            mahadashaPeriods = mahaList,
            currentMahadasha = mahaList.first(),
            currentAntardasha = antarList.first(),
            targetDateTime = now,
            moonLongitude = 10.0,
            fractionElapsed = 0.5,
            fractionRemaining = 0.5,
            metadata = CalculationMetadata(
                ayanamsaDegree = 23.75,
                julianDayUt = 2449852.5,
                calculatedUtcIso = "1995-05-15T05:00:00Z"
            )
        )
    }

    @Test
    fun testCareerPredictionStronglySupported() {
        // Aries Lagna: 10th house is Capricorn (ruled by Saturn), 6th is Virgo (ruled by Mercury)
        // Exalted Sun in 1st house (Aries), Exalted Saturn in 7th house (Libra - Sasa Yoga, aspecting 10th/1st)
        // Jupiter exalted in 4th house (Cancer - Hamsa Yoga, aspecting 10th)
        val planets = listOf(
            pos("Sun", Rashi.ARIES, 1, 10.0, dignity = PlanetDignity.EXALTED),
            pos("Moon", Rashi.CANCER, 4, 15.0, dignity = PlanetDignity.OWN_SIGN),
            pos("Mars", Rashi.ARIES, 1, 5.0, dignity = PlanetDignity.OWN_SIGN),
            pos("Mercury", Rashi.GEMINI, 3, 12.0, dignity = PlanetDignity.OWN_SIGN),
            pos("Jupiter", Rashi.CANCER, 4, 14.0, dignity = PlanetDignity.EXALTED),
            pos("Venus", Rashi.TAURUS, 2, 22.0, dignity = PlanetDignity.OWN_SIGN),
            pos("Saturn", Rashi.LIBRA, 7, 20.0, dignity = PlanetDignity.EXALTED),
            pos("Rahu", Rashi.SAGITTARIUS, 9, 15.0),
            pos("Ketu", Rashi.GEMINI, 3, 15.0)
        )

        val profile = createSampleProfile("High Achiever", lagnaSignIndex = 0, planets = planets)
        val dashaTimeline = createSampleDashaTimeline(DashaPlanet.SATURN, DashaPlanet.SUN)
        val yogaDosha = YogaDoshaCalculator.calculate(profile)

        val transits = listOf(
            Transit("Jupiter", "Aries", 15.0),
            Transit("Saturn", "Aquarius", 10.0)
        )

        val snapshot = PredictionCalculator.calculate(
            profile = profile,
            dashaTimeline = dashaTimeline,
            transits = transits,
            yogaDoshaSnapshot = yogaDosha
        )

        val career = snapshot.topicPredictions[LifeTopic.CAREER]
        assertNotNull("Career prediction should exist", career)
        assertTrue(
            "Career should be Strongly Supported or Supported",
            career!!.supportLevel == PredictionSupportLevel.STRONGLY_SUPPORTED ||
                    career.supportLevel == PredictionSupportLevel.SUPPORTED
        )
        assertTrue("Career key planets should include Saturn", career.keyPlanets.any { it.planetName.equals("Saturn", ignoreCase = true) })
        assertTrue("Supporting factors should be populated", career.supportingFactors.isNotEmpty())
        assertTrue("Classical synthesis should not be blank", career.classicalSynthesis.isNotBlank())
    }

    @Test
    fun testFinancePredictionWithDhanaYoga() {
        // Aries Lagna: 2nd lord Venus in 2nd house (Own Sign Taurus), 11th lord Saturn exalted in 7th
        val planets = listOf(
            pos("Sun", Rashi.LEO, 5, 10.0, dignity = PlanetDignity.OWN_SIGN),
            pos("Moon", Rashi.TAURUS, 2, 15.0, dignity = PlanetDignity.EXALTED),
            pos("Mars", Rashi.SCORPIO, 8, 5.0, dignity = PlanetDignity.OWN_SIGN),
            pos("Mercury", Rashi.VIRGO, 6, 12.0, dignity = PlanetDignity.EXALTED),
            pos("Jupiter", Rashi.SAGITTARIUS, 9, 14.0, dignity = PlanetDignity.OWN_SIGN),
            pos("Venus", Rashi.TAURUS, 2, 22.0, dignity = PlanetDignity.OWN_SIGN),
            pos("Saturn", Rashi.LIBRA, 7, 20.0, dignity = PlanetDignity.EXALTED),
            pos("Rahu", Rashi.AQUARIUS, 11, 15.0),
            pos("Ketu", Rashi.LEO, 5, 15.0)
        )

        val profile = createSampleProfile("Wealth Profile", lagnaSignIndex = 0, planets = planets)
        val dashaTimeline = createSampleDashaTimeline(DashaPlanet.VENUS, DashaPlanet.JUPITER)
        val yogaDosha = YogaDoshaCalculator.calculate(profile)

        val snapshot = PredictionCalculator.calculate(
            profile = profile,
            dashaTimeline = dashaTimeline,
            yogaDoshaSnapshot = yogaDosha
        )

        val finance = snapshot.topicPredictions[LifeTopic.FINANCE]
        assertNotNull(finance)
        assertTrue(
            "Finance support level should be positive",
            finance!!.supportLevel in listOf(PredictionSupportLevel.STRONGLY_SUPPORTED, PredictionSupportLevel.SUPPORTED)
        )
        assertTrue("Should include Venus or Jupiter in key planets", finance.keyPlanets.any { it.planetName in listOf("Venus", "Jupiter") })
    }

    @Test
    fun testMarriagePredictionWithManglikAwareness() {
        // Mars in 7th house (Libra for Aries Lagna) -> Manglik Dosha
        val planets = listOf(
            pos("Sun", Rashi.PISCES, 12, 10.0),
            pos("Moon", Rashi.PISCES, 12, 15.0),
            pos("Mars", Rashi.LIBRA, 7, 5.0, dignity = PlanetDignity.ENEMY),
            pos("Mercury", Rashi.AQUARIUS, 11, 12.0),
            pos("Jupiter", Rashi.CAPRICORN, 10, 14.0, dignity = PlanetDignity.DEBILITATED),
            pos("Venus", Rashi.VIRGO, 6, 22.0, dignity = PlanetDignity.DEBILITATED), // Debilitated Venus in 6th
            pos("Saturn", Rashi.ARIES, 1, 20.0, dignity = PlanetDignity.DEBILITATED),
            pos("Rahu", Rashi.SCORPIO, 8, 15.0),
            pos("Ketu", Rashi.TAURUS, 2, 15.0)
        )

        val profile = createSampleProfile("Challenging Marriage Chart", lagnaSignIndex = 0, planets = planets)
        val dashaTimeline = createSampleDashaTimeline(DashaPlanet.MARS, DashaPlanet.SATURN)
        val yogaDosha = YogaDoshaCalculator.calculate(profile)

        val snapshot = PredictionCalculator.calculate(
            profile = profile,
            dashaTimeline = dashaTimeline,
            yogaDoshaSnapshot = yogaDosha
        )

        val marriage = snapshot.topicPredictions[LifeTopic.MARRIAGE_RELATIONSHIPS]
        assertNotNull(marriage)
        assertTrue(
            "Marriage should advise caution or mixed signals due to 7th house Mars & debilitated Venus",
            marriage!!.supportLevel in listOf(PredictionSupportLevel.CHALLENGING, PredictionSupportLevel.MIXED_SIGNALS)
        )
        assertTrue("Caution factors should not be empty", marriage.cautionFactors.isNotEmpty())
    }

    @Test
    fun testDeterministicRepeatedCalculations() {
        val planets = listOf(
            pos("Sun", Rashi.LEO, 5, 10.0),
            pos("Moon", Rashi.CANCER, 4, 15.0),
            pos("Mars", Rashi.ARIES, 1, 5.0),
            pos("Mercury", Rashi.VIRGO, 6, 12.0),
            pos("Jupiter", Rashi.PISCES, 12, 14.0),
            pos("Venus", Rashi.LIBRA, 7, 22.0),
            pos("Saturn", Rashi.CAPRICORN, 10, 20.0),
            pos("Rahu", Rashi.GEMINI, 3, 15.0),
            pos("Ketu", Rashi.SAGITTARIUS, 9, 15.0)
        )
        val profile = createSampleProfile("Deterministic Native", lagnaSignIndex = 0, planets = planets)
        val dasha = createSampleDashaTimeline()
        val yogaDosha = YogaDoshaCalculator.calculate(profile)

        val run1 = PredictionCalculator.calculate(profile, dasha, null, yogaDosha)
        val run2 = PredictionCalculator.calculate(profile, dasha, null, yogaDosha)

        assertEquals("Same profile must produce identical overall trend", run1.overallLifeTrend, run2.overallLifeTrend)
        assertEquals("Same profile must produce identical topic count", run1.topicPredictions.size, run2.topicPredictions.size)
        for (topic in LifeTopic.entries) {
            val p1 = run1.topicPredictions[topic]
            val p2 = run2.topicPredictions[topic]
            assertEquals("Topic support level must match exactly", p1?.supportLevel, p2?.supportLevel)
            assertEquals("Topic synthesis must match exactly", p1?.classicalSynthesis, p2?.classicalSynthesis)
            assertEquals("Topic supporting factors must match", p1?.supportingFactors, p2?.supportingFactors)
        }
    }

    @Test
    fun testProfileIsolationBetweenProfiles() {
        val planetsA = listOf(
            pos("Sun", Rashi.ARIES, 1, 10.0, dignity = PlanetDignity.EXALTED),
            pos("Moon", Rashi.TAURUS, 2, 15.0, dignity = PlanetDignity.EXALTED),
            pos("Mars", Rashi.CAPRICORN, 10, 5.0, dignity = PlanetDignity.EXALTED),
            pos("Mercury", Rashi.VIRGO, 6, 12.0, dignity = PlanetDignity.EXALTED),
            pos("Jupiter", Rashi.CANCER, 4, 14.0, dignity = PlanetDignity.EXALTED),
            pos("Venus", Rashi.PISCES, 12, 22.0, dignity = PlanetDignity.EXALTED),
            pos("Saturn", Rashi.LIBRA, 7, 20.0, dignity = PlanetDignity.EXALTED),
            pos("Rahu", Rashi.GEMINI, 3, 15.0),
            pos("Ketu", Rashi.SAGITTARIUS, 9, 15.0)
        )
        val profileA = createSampleProfile("Profile A Exalted", lagnaSignIndex = 0, planets = planetsA)

        val planetsB = listOf(
            pos("Sun", Rashi.LIBRA, 7, 10.0, dignity = PlanetDignity.DEBILITATED),
            pos("Moon", Rashi.SCORPIO, 8, 15.0, dignity = PlanetDignity.DEBILITATED),
            pos("Mars", Rashi.CANCER, 4, 5.0, dignity = PlanetDignity.DEBILITATED),
            pos("Mercury", Rashi.PISCES, 12, 12.0, dignity = PlanetDignity.DEBILITATED),
            pos("Jupiter", Rashi.CAPRICORN, 10, 14.0, dignity = PlanetDignity.DEBILITATED),
            pos("Venus", Rashi.VIRGO, 6, 22.0, dignity = PlanetDignity.DEBILITATED),
            pos("Saturn", Rashi.ARIES, 1, 20.0, dignity = PlanetDignity.DEBILITATED),
            pos("Rahu", Rashi.SAGITTARIUS, 9, 15.0),
            pos("Ketu", Rashi.GEMINI, 3, 15.0)
        )
        val profileB = createSampleProfile("Profile B Debilitated", lagnaSignIndex = 0, planets = planetsB)

        val resA = PredictionCalculator.calculate(profileA)
        val resB = PredictionCalculator.calculate(profileB)

        assertNotEquals("Exalted chart and Debilitated chart must have different overall trends", resA.overallLifeTrend, resB.overallLifeTrend)
        assertEquals("Profile A ID should match Profile A name", profileA.birthData.name, resA.profileId)
        assertEquals("Profile B ID should match Profile B name", profileB.birthData.name, resB.profileId)
    }

    @Test
    fun testInsufficientDataValidation() {
        val emptyPlanets = emptyList<PlanetPosition>()
        val profile = createSampleProfile("Empty Profile", lagnaSignIndex = 0, planets = emptyPlanets)

        val snapshot = PredictionCalculator.calculate(profile)
        assertEquals("Insufficient data should result in INSUFFICIENT_DATA status", PredictionSupportLevel.INSUFFICIENT_DATA, snapshot.overallLifeTrend)
        assertTrue("Highlight summary should state calculation not possible", snapshot.keyHighlightSummary.contains("गणना संभव नहीं"))
    }
}
