package com.example.domain

import com.example.data.engine.SwissEphAstrologyEngine
import com.example.domain.models.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SwissEphAstrologyEngineTest {

    private lateinit var engine: SwissEphAstrologyEngine

    @Before
    fun setUp() {
        engine = SwissEphAstrologyEngine()
    }

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

    @Test
    fun `calculateProfile produces valid profile with 9 planetary bodies`() = runBlocking {
        val result = engine.calculateProfile(sampleBirthData)
        assertTrue("Calculation should succeed", result.isSuccess)

        val profile = result.getOrThrow()
        assertEquals(sampleBirthData, profile.birthData)
        assertEquals(9, profile.planetPositions.size)

        val expectedPlanets = listOf("Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn", "Rahu", "Ketu")
        val calculatedPlanets = profile.planetPositions.map { it.planet }
        assertEquals(expectedPlanets, calculatedPlanets)
    }

    @Test
    fun `Rahu and Ketu are exactly 180 degrees opposite`() = runBlocking {
        val profile = engine.calculateProfile(sampleBirthData).getOrThrow()
        val rahu = profile.planetPositions.first { it.planet == "Rahu" }
        val ketu = profile.planetPositions.first { it.planet == "Ketu" }

        val diff = (ketu.totalLongitude - rahu.totalLongitude + 360.0).mod(360.0)
        assertEquals(180.0, diff, 0.0001)
    }

    @Test
    fun `Planetary signs and degrees maintain mathematical invariants`() = runBlocking {
        val profile = engine.calculateProfile(sampleBirthData).getOrThrow()

        for (pos in profile.planetPositions) {
            assertTrue("Sign index must be 0..11", pos.signIndex in 0..11)
            assertTrue("Degree in sign must be >= 0.0", pos.degreeInSign >= 0.0)
            assertTrue("Degree in sign must be < 30.0", pos.degreeInSign < 30.0)
            assertTrue("House must be 1..12", pos.house in 1..12)
            assertTrue("Nakshatra pada must be 1..4", pos.nakshatraPada in 1..4)

            val reconstructedLongitude = (pos.signIndex * 30.0) + pos.degreeInSign
            assertEquals("Total longitude must match sign + degree", pos.totalLongitude, reconstructedLongitude, 0.0001)
        }
    }

    @Test
    fun `Ascendant and Whole Sign houses are consistent`() = runBlocking {
        val profile = engine.calculateProfile(sampleBirthData).getOrThrow()
        assertTrue("Lagna sign index in 0..11", profile.lagnaSignIndex in 0..11)
        assertTrue("Lagna degree in sign < 30", profile.lagnaDegreeInSign in 0.0..<30.0)

        for (pos in profile.planetPositions) {
            val expectedHouse = ((pos.signIndex - profile.lagnaSignIndex).mod(12)) + 1
            assertEquals("House for ${pos.planet} must follow Whole Sign from Lagna", expectedHouse, pos.house)
        }
    }

    @Test
    fun `Ayanamsa is calculated deterministically with Lahiri system`() = runBlocking {
        val profile = engine.calculateProfile(sampleBirthData).getOrThrow()
        val ayanamsa = profile.metadata.ayanamsaDegree

        // For year 1995, Lahiri Ayanamsa is approximately ~23.8° (23° 47')
        assertTrue("Ayanamsa in 1995 should be between 23.0 and 24.5", ayanamsa in 23.0..24.5)
        assertEquals("Lahiri (Chitra Paksha)", profile.metadata.ayanamsaName)
    }

    @Test
    fun `Timezone conversion to UTC produces equivalent astronomical result`() = runBlocking {
        // 1995-08-15 14:30:00 Asia/Kolkata (+05:30) is exactly 1995-08-15 09:00:00 UTC
        val utcBirthData = BirthData(
            name = "Aarav Sharma UTC",
            date = LocalDate.of(1995, 8, 15),
            time = LocalTime.of(9, 0, 0),
            location = BirthLocation(
                latitude = 28.6139,
                longitude = 77.2090,
                placeName = "New Delhi, India"
            ),
            timeZone = ZoneId.of("UTC")
        )

        val profileIST = engine.calculateProfile(sampleBirthData).getOrThrow()
        val profileUTC = engine.calculateProfile(utcBirthData).getOrThrow()

        // Julian days must be identical
        assertEquals(profileIST.metadata.julianDayUt, profileUTC.metadata.julianDayUt, 0.000001)

        // Planetary longitudes must be identical
        for (i in profileIST.planetPositions.indices) {
            val p1 = profileIST.planetPositions[i]
            val p2 = profileUTC.planetPositions[i]
            assertEquals(p1.planet, p2.planet)
            assertEquals("Longitude for ${p1.planet} must match across equivalent time zones", p1.totalLongitude, p2.totalLongitude, 0.0001)
        }
    }

    @Test
    fun `Deterministic calculations are 100 percent repeatable`() = runBlocking {
        val profile1 = engine.calculateProfile(sampleBirthData).getOrThrow()
        val profile2 = engine.calculateProfile(sampleBirthData).getOrThrow()

        assertEquals(profile1.lagnaLongitude, profile2.lagnaLongitude, 0.0)
        assertEquals(profile1.metadata.julianDayUt, profile2.metadata.julianDayUt, 0.0)

        for (i in profile1.planetPositions.indices) {
            val p1 = profile1.planetPositions[i]
            val p2 = profile2.planetPositions[i]
            assertEquals(p1.totalLongitude, p2.totalLongitude, 0.0)
            assertEquals(p1.speed, p2.speed, 0.0)
        }
    }

    @Test
    fun `Invalid coordinates throw validation error`() = runBlocking {
        // Note: BirthLocation init requires latitude in -90..90 and longitude in -180..180
        val latException = runCatching {
            BirthLocation(latitude = 95.0, longitude = 77.0, placeName = "Invalid Lat")
        }
        assertTrue("Out of range latitude must fail init", latException.isFailure)

        val lonException = runCatching {
            BirthLocation(latitude = 28.0, longitude = 195.0, placeName = "Invalid Lon")
        }
        assertTrue("Out of range longitude must fail init", lonException.isFailure)
    }

    @Test
    fun `Nakshatra companion methods compute correct nakshatra and pada`() {
        // Ashwini 1st pada is 0°00' to 3°20'
        val (n1, p1) = Nakshatra.fromLongitude(1.5)
        assertEquals(Nakshatra.ASHWINI, n1)
        assertEquals(1, p1)

        // Ashwini 4th pada is 10°00' to 13°20'
        val (n2, p2) = Nakshatra.fromLongitude(11.0)
        assertEquals(Nakshatra.ASHWINI, n2)
        assertEquals(4, p2)

        // Bharani 1st pada is 13°20' to 16°40'
        val (n3, p3) = Nakshatra.fromLongitude(14.0)
        assertEquals(Nakshatra.BHARANI, n3)
        assertEquals(1, p3)
    }
}
