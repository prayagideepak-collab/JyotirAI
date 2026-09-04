package com.example.domain

import com.example.data.engine.SwissEphAstrologyEngine
import com.example.domain.engine.TransitCalculator
import com.example.domain.models.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

class TransitTest {

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

    private val transitLocation = BirthLocation(
        latitude = 28.6139,
        longitude = 77.2090,
        placeName = "New Delhi, India"
    )

    private val sampleTransitMoment = ZonedDateTime.of(
        LocalDate.of(2026, 9, 4),
        LocalTime.of(12, 0, 0),
        ZoneId.of("Asia/Kolkata")
    )

    @Before
    fun setUp() {
        engine = SwissEphAstrologyEngine()
    }

    // 1. Transit snapshot returns 9 standard Vedic planets (Navagraha)
    @Test
    fun `test 1 - Transit snapshot returns 9 standard Vedic planets`() = runBlocking {
        val result = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation)
        assertTrue(result.isSuccess)
        val snapshot = result.getOrThrow()
        assertEquals(9, snapshot.positions.size)
    }

    // 2. Correct astronomical planetary ordering (Sun, Moon, Mars, Mercury, Jupiter, Venus, Saturn, Rahu, Ketu)
    @Test
    fun `test 2 - Correct astronomical planetary ordering`() = runBlocking {
        val result = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation)
        val snapshot = result.getOrThrow()
        val expectedPlanets = listOf("Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn", "Rahu", "Ketu")
        val actualPlanets = snapshot.positions.map { it.planet }
        assertEquals(expectedPlanets, actualPlanets)
    }

    // 3. Rahu and Ketu are always exactly 180 degrees opposite
    @Test
    fun `test 3 - Rahu and Ketu are always exactly 180 degrees opposite`() = runBlocking {
        val result = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation)
        val snapshot = result.getOrThrow()
        val rahu = snapshot.positions.first { it.planet == "Rahu" }
        val ketu = snapshot.positions.first { it.planet == "Ketu" }

        val diff = abs(rahu.totalLongitude - ketu.totalLongitude)
        val angularDist = if (diff > 180.0) 360.0 - diff else diff
        assertEquals(180.0, angularDist, 0.0001)
    }

    // 4. Rahu and Ketu are always flagged as retrograde
    @Test
    fun `test 4 - Rahu and Ketu are always flagged as retrograde`() = runBlocking {
        val result = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation)
        val snapshot = result.getOrThrow()
        val rahu = snapshot.positions.first { it.planet == "Rahu" }
        val ketu = snapshot.positions.first { it.planet == "Ketu" }
        assertTrue(rahu.isRetrograde)
        assertTrue(ketu.isRetrograde)
    }

    // 5. Sun is never flagged as retrograde
    @Test
    fun `test 5 - Sun is never flagged as retrograde`() = runBlocking {
        val result = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation)
        val snapshot = result.getOrThrow()
        val sun = snapshot.positions.first { it.planet == "Sun" }
        assertFalse(sun.isRetrograde)
    }

    // 6. Moon is never flagged as retrograde
    @Test
    fun `test 6 - Moon is never flagged as retrograde`() = runBlocking {
        val result = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation)
        val snapshot = result.getOrThrow()
        val moon = snapshot.positions.first { it.planet == "Moon" }
        assertFalse(moon.isRetrograde)
    }

    // 7. Planetary longitude is strictly normalized within 0 to 360 degrees
    @Test
    fun `test 7 - Planetary longitude is strictly normalized within 0 to 360 degrees`() = runBlocking {
        val result = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation)
        val snapshot = result.getOrThrow()
        snapshot.positions.forEach { pos ->
            assertTrue("${pos.planet} longitude out of range", pos.totalLongitude in 0.0..<360.0)
        }
    }

    // 8. Degree in sign is strictly within 0 to 30 degrees for all planets
    @Test
    fun `test 8 - Degree in sign is strictly within 0 to 30 degrees for all planets`() = runBlocking {
        val result = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation)
        val snapshot = result.getOrThrow()
        snapshot.positions.forEach { pos ->
            assertTrue("${pos.planet} degreeInSign out of range", pos.degreeInSign in 0.0..<30.0)
        }
    }

    // 9. Rashi index is strictly within 0 to 11 for all planets
    @Test
    fun `test 9 - Rashi index is strictly within 0 to 11 for all planets`() = runBlocking {
        val result = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation)
        val snapshot = result.getOrThrow()
        snapshot.positions.forEach { pos ->
            assertTrue("${pos.planet} signIndex out of range", pos.signIndex in 0..11)
        }
    }

    // 10. Whole-sign relative house calculation from same sign yields 1st house
    @Test
    fun `test 10 - Whole-sign relative house calculation from same sign yields 1st house`() {
        for (sign in 0..11) {
            assertEquals(1, TransitCalculator.calculateRelativeHouse(sign, sign))
        }
    }

    // 11. Whole-sign relative house calculation across sign boundary wraps correctly
    @Test
    fun `test 11 - Whole-sign relative house calculation across sign boundary wraps correctly`() {
        // Pisces (index 11) to Aries (index 0) -> Aries is 2nd house from Pisces
        assertEquals(2, TransitCalculator.calculateRelativeHouse(0, 11))
        // Aquarius (index 10) to Aries (index 0) -> Aries is 3rd house from Aquarius
        assertEquals(3, TransitCalculator.calculateRelativeHouse(0, 10))
    }

    // 12. Whole-sign relative house calculation 12th house
    @Test
    fun `test 12 - Whole-sign relative house calculation 12th house`() {
        // Aries (index 0) from Taurus (index 1) -> 12th house
        assertEquals(12, TransitCalculator.calculateRelativeHouse(0, 1))
        // Pisces (index 11) from Aries (index 0) -> 12th house
        assertEquals(12, TransitCalculator.calculateRelativeHouse(11, 0))
    }

    // 13. House calculation formula works for all 12 houses
    @Test
    fun `test 13 - House calculation formula works for all 12 houses`() {
        val ref = 3 // Cancer
        for (i in 0..11) {
            val transitSign = (ref + i) % 12
            val house = TransitCalculator.calculateRelativeHouse(transitSign, ref)
            assertEquals(i + 1, house)
        }
    }

    // 14. Natal reference populated when natal profile provided
    @Test
    fun `test 14 - Natal reference populated when natal profile provided`() = runBlocking {
        val profileResult = engine.calculateProfile(sampleBirthData)
        val profile = profileResult.getOrThrow()

        val transitResult = engine.calculateTransitSnapshot(
            sampleTransitMoment,
            transitLocation,
            profile
        )
        val snapshot = transitResult.getOrThrow()
        assertNotNull(snapshot.natalReference)
        assertEquals("Aarav Sharma", snapshot.natalReference?.nativeName)
        assertEquals(profile.moonSign, snapshot.natalReference?.moonSign)
        assertEquals(profile.lagna, snapshot.natalReference?.lagnaSign)
    }

    // 15. Natal reference is null when no natal profile provided
    @Test
    fun `test 15 - Natal reference is null when no natal profile provided`() = runBlocking {
        val transitResult = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation, null)
        val snapshot = transitResult.getOrThrow()
        assertNull(snapshot.natalReference)
    }

    // 16. Relative house from Moon is populated when natal profile provided
    @Test
    fun `test 16 - Relative house from Moon is populated when natal profile provided`() = runBlocking {
        val profileResult = engine.calculateProfile(sampleBirthData)
        val profile = profileResult.getOrThrow()

        val transitResult = engine.calculateTransitSnapshot(
            sampleTransitMoment,
            transitLocation,
            profile
        )
        val snapshot = transitResult.getOrThrow()
        snapshot.positions.forEach { pos ->
            assertNotNull("${pos.planet} should have houseFromMoon", pos.houseFromMoon)
            assertTrue(pos.houseFromMoon!! in 1..12)
        }
    }

    // 17. Relative house from Lagna is populated when natal profile provided
    @Test
    fun `test 17 - Relative house from Lagna is populated when natal profile provided`() = runBlocking {
        val profileResult = engine.calculateProfile(sampleBirthData)
        val profile = profileResult.getOrThrow()

        val transitResult = engine.calculateTransitSnapshot(
            sampleTransitMoment,
            transitLocation,
            profile
        )
        val snapshot = transitResult.getOrThrow()
        snapshot.positions.forEach { pos ->
            assertNotNull("${pos.planet} should have houseFromLagna", pos.houseFromLagna)
            assertTrue(pos.houseFromLagna!! in 1..12)
        }
    }

    // 18. Relative houses are null when no natal profile provided
    @Test
    fun `test 18 - Relative houses are null when no natal profile provided`() = runBlocking {
        val transitResult = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation, null)
        val snapshot = transitResult.getOrThrow()
        snapshot.positions.forEach { pos ->
            assertNull("${pos.planet} houseFromMoon should be null", pos.houseFromMoon)
            assertNull("${pos.planet} houseFromLagna should be null", pos.houseFromLagna)
        }
    }

    // 19. Calculation metadata includes Swiss Ephemeris and Lahiri Ayanamsa
    @Test
    fun `test 19 - Calculation metadata includes Swiss Ephemeris and Lahiri Ayanamsa`() = runBlocking {
        val transitResult = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation)
        val snapshot = transitResult.getOrThrow()
        assertTrue(snapshot.metadata.ephemerisEngine.contains("Swiss Ephemeris"))
        assertTrue(snapshot.metadata.ayanamsaName.contains("Lahiri"))
        assertTrue(snapshot.metadata.ayanamsaDegree > 20.0) // Lahiri is ~24° in modern times
    }

    // 20. Deterministic results: same transit timestamp produces identical longitudes
    @Test
    fun `test 20 - Deterministic results - same transit timestamp produces identical longitudes`() = runBlocking {
        val result1 = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation)
        val result2 = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation)

        val pos1 = result1.getOrThrow().positions
        val pos2 = result2.getOrThrow().positions

        for (i in pos1.indices) {
            assertEquals(pos1[i].planet, pos2[i].planet)
            assertEquals(pos1[i].totalLongitude, pos2[i].totalLongitude, 0.0)
            assertEquals(pos1[i].isRetrograde, pos2[i].isRetrograde)
        }
    }

    // 21. Caching works: repeated calls return cached snapshot
    @Test
    fun `test 21 - Caching works - repeated calls return identical cached instance`() = runBlocking {
        val result1 = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation)
        val result2 = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation)
        assertSame(result1.getOrThrow(), result2.getOrThrow())
    }

    // 22. Transit snapshot includes valid transit Lagna (Ascendant) for given location
    @Test
    fun `test 22 - Transit snapshot includes valid transit Lagna`() = runBlocking {
        val result = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation)
        val snapshot = result.getOrThrow()
        assertNotNull(snapshot.transitAscendantSign)
        assertNotNull(snapshot.transitAscendantSignIndex)
        assertTrue(snapshot.transitAscendantSignIndex!! in 0..11)
        assertNotNull(snapshot.transitAscendantDegree)
        assertTrue(snapshot.transitAscendantDegree!! in 0.0..<30.0)
    }

    // 23. Invalid latitude outside -90 to 90 fails gracefully
    @Test
    fun `test 23 - Invalid latitude fails gracefully`() {
        try {
            BirthLocation(95.0, 77.0, "Invalid Place")
            fail("Expected IllegalArgumentException for latitude 95.0")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Latitude") == true)
        }
    }

    // 24. Invalid longitude outside -180 to 180 fails gracefully
    @Test
    fun `test 24 - Invalid longitude fails gracefully`() {
        try {
            BirthLocation(28.0, 195.0, "Invalid Place")
            fail("Expected IllegalArgumentException for longitude 195.0")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Longitude") == true)
        }
    }

    // 24b. Invalid transit year outside astronomical range fails gracefully
    @Test
    fun `test 24b - Invalid transit year fails gracefully`() = runBlocking {
        val outOfRangeMoment = ZonedDateTime.of(LocalDate.of(500, 1, 1), LocalTime.of(12, 0), ZoneId.of("Asia/Kolkata"))
        val result = engine.calculateTransitSnapshot(outOfRangeMoment, transitLocation)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.InvalidBirthData)
    }

    // 25. TransitCalculator house ordinals formatting
    @Test
    fun `test 25 - TransitCalculator house ordinals formatting`() {
        assertEquals("1st", TransitCalculator.getHouseOrdinal(1))
        assertEquals("2nd", TransitCalculator.getHouseOrdinal(2))
        assertEquals("3rd", TransitCalculator.getHouseOrdinal(3))
        assertEquals("4th", TransitCalculator.getHouseOrdinal(4))
        assertEquals("8th", TransitCalculator.getHouseOrdinal(8))
        assertEquals("12th", TransitCalculator.getHouseOrdinal(12))
    }

    // 26. Nakshatra and Pada calculation is valid
    @Test
    fun `test 26 - Nakshatra and Pada calculation is valid`() = runBlocking {
        val result = engine.calculateTransitSnapshot(sampleTransitMoment, transitLocation)
        val snapshot = result.getOrThrow()
        snapshot.positions.forEach { pos ->
            assertTrue(pos.nakshatra.isNotBlank())
            assertTrue(pos.nakshatraLord.isNotBlank())
            assertTrue(pos.nakshatraPada in 1..4)
        }
    }

    // 27. Natal data is strictly unmutated / independent from transit snapshot data
    @Test
    fun `test 27 - Natal data is strictly unmutated and independent`() = runBlocking {
        val profileResult = engine.calculateProfile(sampleBirthData)
        val originalProfile = profileResult.getOrThrow()

        // Capture initial natal state
        val originalNatalMoonLon = originalProfile.planetPositions.first { it.planet == "Moon" }.totalLongitude
        val originalNatalLagnaLon = originalProfile.lagnaLongitude

        // Calculate a transit for a future moment
        val futureTransit = ZonedDateTime.of(2035, 1, 1, 0, 0, 0, 0, ZoneId.of("Asia/Kolkata"))
        val transitResult = engine.calculateTransitSnapshot(futureTransit, transitLocation, originalProfile)
        assertTrue(transitResult.isSuccess)

        // Verify natal state remains identical
        val afterNatalMoonLon = originalProfile.planetPositions.first { it.planet == "Moon" }.totalLongitude
        val afterNatalLagnaLon = originalProfile.lagnaLongitude

        assertEquals(originalNatalMoonLon, afterNatalMoonLon, 0.0)
        assertEquals(originalNatalLagnaLon, afterNatalLagnaLon, 0.0)
    }

    // 28. Date changes produce different planetary positions
    @Test
    fun `test 28 - Date changes produce different planetary positions`() = runBlocking {
        val moment1 = ZonedDateTime.of(2026, 9, 4, 12, 0, 0, 0, ZoneId.of("Asia/Kolkata"))
        val moment2 = moment1.plusDays(10) // 10 days later

        val res1 = engine.calculateTransitSnapshot(moment1, transitLocation).getOrThrow()
        val res2 = engine.calculateTransitSnapshot(moment2, transitLocation).getOrThrow()

        val moon1 = res1.positions.first { it.planet == "Moon" }.totalLongitude
        val moon2 = res2.positions.first { it.planet == "Moon" }.totalLongitude

        assertNotEquals(moon1, moon2, 0.01)
    }

    // 29. Same instant in different timezones produces identical planetary positions
    @Test
    fun `test 29 - Same instant represented in different timezones produces identical planetary positions`() = runBlocking {
        // 12:00 UTC on 2026-09-04 is 17:30 IST (+05:30)
        val momentUtc = ZonedDateTime.of(2026, 9, 4, 12, 0, 0, 0, ZoneId.of("UTC"))
        val momentIst = momentUtc.withZoneSameInstant(ZoneId.of("Asia/Kolkata"))

        val resUtc = engine.calculateTransitSnapshot(momentUtc, transitLocation).getOrThrow()
        val resIst = engine.calculateTransitSnapshot(momentIst, transitLocation).getOrThrow()

        for (i in resUtc.positions.indices) {
            assertEquals(resUtc.positions[i].planet, resIst.positions[i].planet)
            assertEquals(
                "Planet ${resUtc.positions[i].planet} longitude differs across equivalent timezones",
                resUtc.positions[i].totalLongitude,
                resIst.positions[i].totalLongitude,
                0.0001
            )
        }
    }

    // 30. Cache distinguishes different date, time, and locations
    @Test
    fun `test 30 - Cache distinguishes different dates and locations`() = runBlocking {
        val moment1 = ZonedDateTime.of(2026, 9, 4, 12, 0, 0, 0, ZoneId.of("Asia/Kolkata"))
        val moment2 = ZonedDateTime.of(2026, 9, 5, 12, 0, 0, 0, ZoneId.of("Asia/Kolkata"))
        val loc2 = BirthLocation(13.0827, 80.2707, "Chennai, India")

        val res1 = engine.calculateTransitSnapshot(moment1, transitLocation).getOrThrow()
        val res2 = engine.calculateTransitSnapshot(moment2, transitLocation).getOrThrow()
        val res3 = engine.calculateTransitSnapshot(moment1, loc2).getOrThrow()

        assertNotSame(res1, res2)
        assertNotSame(res1, res3)
    }

    // 31. Daylight Saving Time (DST) transition handling
    @Test
    fun `test 31 - Daylight Saving Time transition handles UTC conversion deterministically`() = runBlocking {
        val nyZone = ZoneId.of("America/New_York")
        // In New York, DST begins second Sunday of March (e.g. March 8, 2026)
        val winterMoment = ZonedDateTime.of(2026, 1, 15, 12, 0, 0, 0, nyZone) // UTC-5
        val summerMoment = ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, nyZone) // UTC-4

        val locNy = BirthLocation(40.7128, -74.0060, "New York, USA")

        val resWinter = engine.calculateTransitSnapshot(winterMoment, locNy)
        val resSummer = engine.calculateTransitSnapshot(summerMoment, locNy)

        assertTrue(resWinter.isSuccess)
        assertTrue(resSummer.isSuccess)
    }
}
