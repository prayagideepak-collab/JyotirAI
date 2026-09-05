package com.example.domain

import com.example.data.engine.SwissEphAstrologyEngine
import com.example.domain.engine.VimshottariDashaCalculator
import com.example.domain.models.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class VimshottariDashaTest {

    private lateinit var engine: SwissEphAstrologyEngine

    private val sampleBirthData = BirthData(
        name = "Aarav Sharma",
        date = LocalDate.of(1995, 8, 15),
        time = LocalTime.of(14, 30, 0),
        location = BirthLocation(
            latitude = 28.6139,
            longitude = 77.2090,
            placeName = "New Delhi, India"
        ),
        timeZone = ZoneId.of("Asia/Kolkata")
    )

    @Before
    fun setUp() {
        engine = SwissEphAstrologyEngine()
    }

    // 1. Correct Vimshottari planet order
    @Test
    fun `test 1 - Correct Vimshottari planet order`() {
        val expectedOrder = listOf(
            DashaPlanet.KETU,
            DashaPlanet.VENUS,
            DashaPlanet.SUN,
            DashaPlanet.MOON,
            DashaPlanet.MARS,
            DashaPlanet.RAHU,
            DashaPlanet.JUPITER,
            DashaPlanet.SATURN,
            DashaPlanet.MERCURY
        )
        assertEquals(expectedOrder, DashaPlanet.VIMSHOTTARI_ORDER)
    }

    // 2. Correct standard Mahadasha durations
    @Test
    fun `test 2 - Correct standard Mahadasha durations`() {
        assertEquals(7, DashaPlanet.KETU.years)
        assertEquals(20, DashaPlanet.VENUS.years)
        assertEquals(6, DashaPlanet.SUN.years)
        assertEquals(10, DashaPlanet.MOON.years)
        assertEquals(7, DashaPlanet.MARS.years)
        assertEquals(18, DashaPlanet.RAHU.years)
        assertEquals(16, DashaPlanet.JUPITER.years)
        assertEquals(19, DashaPlanet.SATURN.years)
        assertEquals(17, DashaPlanet.MERCURY.years)
    }

    // 3. Total Vimshottari cycle = 120 years
    @Test
    fun `test 3 - Total Vimshottari cycle is 120 years`() {
        val sumYears = DashaPlanet.VIMSHOTTARI_ORDER.sumOf { it.years }
        assertEquals(120, sumYears)
        assertEquals(120, DashaPlanet.TOTAL_CYCLE_YEARS)
    }

    // 4. Nakshatra -> correct Dasha lord
    @Test
    fun `test 4 - Nakshatra to correct Dasha lord mapping`() {
        val expectedLords = listOf(
            "Ketu", "Venus", "Sun", "Moon", "Mars", "Rahu", "Jupiter", "Saturn", "Mercury", // 0-8
            "Ketu", "Venus", "Sun", "Moon", "Mars", "Rahu", "Jupiter", "Saturn", "Mercury", // 9-17
            "Ketu", "Venus", "Sun", "Moon", "Mars", "Rahu", "Jupiter", "Saturn", "Mercury"  // 18-26
        )
        assertEquals(27, Nakshatra.entries.size)
        Nakshatra.entries.forEachIndexed { index, nakshatra ->
            assertEquals("Lord mismatch at ${nakshatra.sanskritName}", expectedLords[index], nakshatra.lord)
            val planet = DashaPlanet.fromLord(nakshatra.lord)
            assertEquals(expectedLords[index].lowercase(), planet.lord.lowercase())
        }
    }

    // 5. Moon at Nakshatra beginning
    @Test
    fun `test 5 - Moon at Nakshatra beginning`() {
        // Ashwini starts at 0.0°. Test with 0.0001°
        val balance = VimshottariDashaCalculator.calculateStartingBalance(0.0001)
        assertEquals(Nakshatra.ASHWINI, balance.birthNakshatra)
        assertEquals(DashaPlanet.KETU, balance.startingPlanet)
        assertTrue(balance.fractionElapsed < 0.0001)
        assertTrue(balance.fractionRemaining > 0.9999)
        assertEquals(7.0, balance.remainingYears, 0.01)
        assertEquals(0.0, balance.elapsedYears, 0.01)
    }

    // 6. Moon at Nakshatra middle
    @Test
    fun `test 6 - Moon at Nakshatra middle`() {
        // Ashwini span is 13.333333333333334°. Midpoint is ~6.666666666666667°
        val midpoint = Nakshatra.SPAN_DEGREES / 2.0
        val balance = VimshottariDashaCalculator.calculateStartingBalance(midpoint)
        assertEquals(Nakshatra.ASHWINI, balance.birthNakshatra)
        assertEquals(DashaPlanet.KETU, balance.startingPlanet)
        assertEquals(0.5, balance.fractionElapsed, 0.0001)
        assertEquals(0.5, balance.fractionRemaining, 0.0001)
        assertEquals(3.5, balance.remainingYears, 0.001)
        assertEquals(3.5, balance.elapsedYears, 0.001)
    }

    // 7. Moon near Nakshatra ending
    @Test
    fun `test 7 - Moon near Nakshatra ending`() {
        // Just before Ashwini ends: 13.3333°
        val nearEnd = Nakshatra.SPAN_DEGREES - 0.001
        val balance = VimshottariDashaCalculator.calculateStartingBalance(nearEnd)
        assertEquals(Nakshatra.ASHWINI, balance.birthNakshatra)
        assertEquals(DashaPlanet.KETU, balance.startingPlanet)
        assertTrue(balance.fractionElapsed > 0.999)
        assertTrue(balance.fractionRemaining < 0.001)
        assertTrue(balance.remainingYears < 0.01)
        assertTrue(balance.remainingYears >= 0.0)
    }

    // 8. Starting Mahadasha balance
    @Test
    fun `test 8 - Starting Mahadasha balance precision`() {
        // Bharani (Index 1, ruled by Venus: 20 years). Span: 13.3333° to 26.6666°
        // 25% through Bharani = 13.333333333333334 + 0.25 * 13.333333333333334 = 16.666666666666668°
        val lon = Nakshatra.SPAN_DEGREES * 1.25
        val balance = VimshottariDashaCalculator.calculateStartingBalance(lon)
        assertEquals(Nakshatra.BHARANI, balance.birthNakshatra)
        assertEquals(DashaPlanet.VENUS, balance.startingPlanet)
        assertEquals(0.25, balance.fractionElapsed, 0.0001)
        assertEquals(0.75, balance.fractionRemaining, 0.0001)
        // 75% of 20 years = 15 years remaining
        assertEquals(15.0, balance.remainingYears, 0.001)
        assertEquals(5.0, balance.elapsedYears, 0.001)
        assertEquals(15, balance.startingBalance.years)
    }

    // 9. Mahadasha sequence generation
    @Test
    fun `test 9 - Mahadasha sequence generation`() = runBlocking {
        val timelineResult = engine.calculateDashaTimeline(sampleBirthData)
        assertTrue(timelineResult.isSuccess)

        val timeline = timelineResult.getOrThrow()
        assertEquals(9, timeline.mahadashaPeriods.size)

        // The first Mahadasha must be the birth Nakshatra lord
        assertEquals(timeline.startingMahadasha, timeline.mahadashaPeriods.first().planet)
        assertTrue(timeline.mahadashaPeriods.first().isBirthMahadasha)

        // Verify cyclical order of the 9 periods
        val expectedSequence = DashaPlanet.sequenceStartingFrom(timeline.startingMahadasha)
        for (i in 0 until 9) {
            assertEquals(expectedSequence[i], timeline.mahadashaPeriods[i].planet)
            assertEquals(expectedSequence[i].years.toDouble(), timeline.mahadashaPeriods[i].totalDurationYears, 0.01)
        }
    }

    // 10. Mahadasha date boundaries
    @Test
    fun `test 10 - Mahadasha date boundaries are continuous without gaps`() = runBlocking {
        val timeline = engine.calculateDashaTimeline(sampleBirthData).getOrThrow()
        for (i in 0 until timeline.mahadashaPeriods.size - 1) {
            val current = timeline.mahadashaPeriods[i]
            val next = timeline.mahadashaPeriods[i + 1]
            assertEquals("Boundary gap between ${current.planet} and ${next.planet}", current.endDate, next.startDate)
            assertTrue(current.endDate.isAfter(current.startDate))
        }
    }

    // 11. Antardasha ordering
    @Test
    fun `test 11 - Antardasha ordering starts with Mahadasha lord`() {
        val start = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
        val end = start.plusYears(16) // Jupiter (16 years)
        val antardashas = VimshottariDashaCalculator.calculateAntardashas(
            mahadashaLord = DashaPlanet.JUPITER,
            startDate = start,
            endDate = end,
            totalDurationYears = 16.0
        )

        assertEquals(9, antardashas.size)
        // Must start with Jupiter itself
        assertEquals(DashaPlanet.JUPITER, antardashas[0].antardashaLord)
        assertEquals(DashaPlanet.SATURN, antardashas[1].antardashaLord)
        assertEquals(DashaPlanet.MERCURY, antardashas[2].antardashaLord)
        assertEquals(DashaPlanet.KETU, antardashas[3].antardashaLord)
        assertEquals(DashaPlanet.VENUS, antardashas[4].antardashaLord)
        assertEquals(DashaPlanet.SUN, antardashas[5].antardashaLord)
        assertEquals(DashaPlanet.MOON, antardashas[6].antardashaLord)
        assertEquals(DashaPlanet.MARS, antardashas[7].antardashaLord)
        assertEquals(DashaPlanet.RAHU, antardashas[8].antardashaLord)
    }

    // 12. Antardasha proportional duration
    @Test
    fun `test 12 - Antardasha proportional duration formula`() {
        val start = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
        val end = start.plusYears(6) // Sun (6 years)
        val antardashas = VimshottariDashaCalculator.calculateAntardashas(
            mahadashaLord = DashaPlanet.SUN,
            startDate = start,
            endDate = end,
            totalDurationYears = 6.0
        )

        // Sun-Sun: 6 * 6 / 120 = 0.3 years
        assertEquals(0.3, antardashas[0].durationYears, 0.0001)
        // Sun-Moon: 6 * 10 / 120 = 0.5 years
        assertEquals(0.5, antardashas[1].durationYears, 0.0001)
        // Sun-Venus: 6 * 20 / 120 = 1.0 year
        val venusAntardasha = antardashas.first { it.antardashaLord == DashaPlanet.VENUS }
        assertEquals(1.0, venusAntardasha.durationYears, 0.0001)
    }

    // 13. Antardasha periods fitting parent Mahadasha
    @Test
    fun `test 13 - Antardasha periods fitting parent Mahadasha across multiple lords`() {
        val testLords = listOf(
            DashaPlanet.KETU,
            DashaPlanet.VENUS,
            DashaPlanet.SUN,
            DashaPlanet.RAHU,
            DashaPlanet.SATURN,
            DashaPlanet.MERCURY
        )

        val baseDate = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))

        for (lord in testLords) {
            val seconds = (lord.years * VimshottariDashaCalculator.SECONDS_PER_SOLAR_YEAR).toLong()
            val endDate = baseDate.plusSeconds(seconds)
            val antardashas = VimshottariDashaCalculator.calculateAntardashas(
                mahadashaLord = lord,
                startDate = baseDate,
                endDate = endDate,
                totalDurationYears = lord.years.toDouble()
            )

            assertEquals(9, antardashas.size)
            assertEquals("First Antardasha start must match Mahadasha start", baseDate, antardashas.first().startDate)
            assertEquals("Last Antardasha end must match Mahadasha end", endDate, antardashas.last().endDate)

            for (i in 0 until antardashas.size - 1) {
                assertEquals("Antardasha boundary gap at $i", antardashas[i].endDate, antardashas[i + 1].startDate)
            }

            val sumDuration = antardashas.sumOf { it.durationYears }
            assertEquals(lord.years.toDouble(), sumDuration, 0.0001)
        }
    }

    // 14. Current Mahadasha detection
    @Test
    fun `test 14 - Current Mahadasha detection`() = runBlocking {
        // Native born on 1995-08-15. Evaluate on 2026-09-04
        val targetDate = ZonedDateTime.of(2026, 9, 4, 12, 0, 0, 0, ZoneId.of("Asia/Kolkata"))
        val timeline = engine.calculateDashaTimeline(sampleBirthData, targetDate).getOrThrow()

        val currentMd = timeline.currentMahadasha
        assertNotNull("Current Mahadasha must not be null for active date", currentMd)
        assertTrue(currentMd!!.isCurrent)
        assertTrue(!targetDate.isBefore(currentMd.startDate) && targetDate.isBefore(currentMd.endDate))
    }

    // 15. Current Antardasha detection
    @Test
    fun `test 15 - Current Antardasha detection`() = runBlocking {
        val targetDate = ZonedDateTime.of(2026, 9, 4, 12, 0, 0, 0, ZoneId.of("Asia/Kolkata"))
        val timeline = engine.calculateDashaTimeline(sampleBirthData, targetDate).getOrThrow()

        val currentAd = timeline.currentAntardasha
        assertNotNull("Current Antardasha must not be null for active date", currentAd)
        assertTrue(currentAd!!.isCurrent)
        assertTrue(!targetDate.isBefore(currentAd.startDate) && targetDate.isBefore(currentAd.endDate))
        assertEquals(timeline.currentMahadasha!!.planet, currentAd.mahadashaLord)
    }

    // 16. Deterministic repeated calculation
    @Test
    fun `test 16 - Deterministic repeated calculation`() = runBlocking {
        val targetDate = ZonedDateTime.of(2025, 5, 20, 10, 0, 0, 0, ZoneId.of("Asia/Kolkata"))
        val result1 = engine.calculateDashaTimeline(sampleBirthData, targetDate).getOrThrow()
        val result2 = engine.calculateDashaTimeline(sampleBirthData, targetDate).getOrThrow()

        assertEquals(result1.startingMahadasha, result2.startingMahadasha)
        assertEquals(result1.startingBalance.totalYears, result2.startingBalance.totalYears, 0.0000001)
        assertEquals(result1.moonLongitude, result2.moonLongitude, 0.0000001)
        assertEquals(result1.currentMahadasha?.planet, result2.currentMahadasha?.planet)
        assertEquals(result1.currentAntardasha?.antardashaLord, result2.currentAntardasha?.antardashaLord)
    }

    // 17. Leap-year date handling
    @Test
    fun `test 17 - Leap-year date handling`() = runBlocking {
        // Native born on leap day 2000-02-29
        val leapBirth = BirthData(
            name = "Leap Child",
            date = LocalDate.of(2000, 2, 29),
            time = LocalTime.of(8, 0, 0),
            location = BirthLocation(28.6139, 77.2090, "New Delhi"),
            timeZone = ZoneId.of("Asia/Kolkata")
        )

        val timeline = engine.calculateDashaTimeline(leapBirth).getOrThrow()
        assertNotNull(timeline)
        assertEquals(9, timeline.mahadashaPeriods.size)

        // Check continuity across leap years
        for (i in 0 until timeline.mahadashaPeriods.size - 1) {
            assertEquals(timeline.mahadashaPeriods[i].endDate, timeline.mahadashaPeriods[i + 1].startDate)
        }
    }

    // 18. Exact transition boundary behavior
    @Test
    fun `test 18 - Exact transition boundary behavior`() {
        val start = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
        val transition = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
        val end = ZonedDateTime.of(2030, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))

        // Period 1: [start, transition)
        // Period 2: [transition, end)
        val beforeBoundary = transition.minusSeconds(1)
        val exactlyAtBoundary = transition
        val afterBoundary = transition.plusSeconds(1)

        // 1. Immediately before boundary
        assertTrue("Immediately before boundary must belong to Period 1",
            VimshottariDashaCalculator.isDateTimeInRange(beforeBoundary, start, transition, isLast = false))
        assertFalse("Immediately before boundary must not belong to Period 2",
            VimshottariDashaCalculator.isDateTimeInRange(beforeBoundary, transition, end, isLast = false))

        // 2. Exactly at boundary (half-open interval convention: transition belongs to Period 2, NOT Period 1)
        assertFalse("Transition timestamp should not belong to earlier half-open interval",
            VimshottariDashaCalculator.isDateTimeInRange(exactlyAtBoundary, start, transition, isLast = false))
        assertTrue("Transition timestamp must belong to the starting interval",
            VimshottariDashaCalculator.isDateTimeInRange(exactlyAtBoundary, transition, end, isLast = false))

        // 3. Immediately after boundary
        assertFalse("Immediately after boundary must not belong to Period 1",
            VimshottariDashaCalculator.isDateTimeInRange(afterBoundary, start, transition, isLast = false))
        assertTrue("Immediately after boundary must belong to Period 2",
            VimshottariDashaCalculator.isDateTimeInRange(afterBoundary, transition, end, isLast = false))
    }

    // 19. Invalid birth input
    @Test
    fun `test 19 - Invalid birth input handling`() = runBlocking {
        val invalidYearData = sampleBirthData.copy(
            date = LocalDate.of(500, 1, 1)
        )
        val result = engine.calculateDashaTimeline(invalidYearData)
        assertTrue("Calculation must fail gracefully for invalid year", result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.InvalidBirthData)
    }

    // 20. Dasha calculation failure handling
    @Test
    fun `test 20 - Dasha calculation failure handling`() = runBlocking {
        val outOfRangeData = sampleBirthData.copy(
            date = LocalDate.of(3500, 1, 1)
        )
        val result = engine.calculateDashaTimeline(outOfRangeData)
        assertTrue("Calculation must fail gracefully for out-of-range year", result.isFailure)
    }

    // 21. Cache correctness
    @Test
    fun `test 21 - Cache correctness`() = runBlocking {
        val targetDate = ZonedDateTime.of(2026, 9, 4, 12, 0, 0, 0, ZoneId.of("Asia/Kolkata"))
        val call1 = engine.calculateDashaTimeline(sampleBirthData, targetDate).getOrThrow()
        val call2 = engine.calculateDashaTimeline(sampleBirthData, targetDate).getOrThrow()

        // Cache must return the exact same instance reference
        assertSame("Cached instance should be returned on repeated call", call1, call2)
    }
}
