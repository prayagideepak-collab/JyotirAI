package com.example.domain

import com.example.data.engine.SwissEphAstrologyEngine
import com.example.domain.models.BirthLocation
import com.example.domain.models.Paksha
import com.example.domain.models.Vara
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.ZoneOffset

class PanchangTest {

    private lateinit var classUnderTest: SwissEphAstrologyEngine

    @Before
    fun setup() {
        classUnderTest = SwissEphAstrologyEngine()
    }

    @Test
    fun `test 1 - Vara calculation corresponds to local date`() = runBlocking {
        val date = ZonedDateTime.of(LocalDate.of(2026, 9, 4), LocalTime.of(12, 0), ZoneId.of("Asia/Kolkata"))
        val location = BirthLocation(28.6139, 77.2090, "New Delhi")
        val result = classUnderTest.calculatePanchang(date, location).getOrThrow()

        assertEquals(Vara.SHUKRAVARA, result.vara)
    }

    @Test
    fun `test Sunrise and Sunset exactly match local date without rolling over`() = runBlocking {
        val date = ZonedDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0), ZoneId.of("Asia/Kolkata"))
        val location = BirthLocation(28.6139, 77.2090, "New Delhi")
        val result = classUnderTest.calculatePanchang(date, location).getOrThrow()
        
        assertNotNull(result.sunrise)
        assertEquals("Sunrise should be on the requested civil date", 
            date.toLocalDate(), result.sunrise?.toLocalDate())
            
        assertNotNull(result.sunset)
        assertEquals("Sunset should be on the requested civil date", 
            date.toLocalDate(), result.sunset?.toLocalDate())
            
        assertTrue("Sunrise should be before sunset", result.sunrise!!.isBefore(result.sunset))
    }

    @Test
    fun `test Sunrise and Sunset in a different timezone (UTC-8)`() = runBlocking {
        val laZone = ZoneId.of("America/Los_Angeles")
        val date = ZonedDateTime.of(LocalDate.of(2026, 7, 4), LocalTime.of(12, 0), laZone)
        val location = BirthLocation(34.0522, -118.2437, "Los Angeles")
        val result = classUnderTest.calculatePanchang(date, location).getOrThrow()
        
        assertNotNull(result.sunrise)
        assertEquals(date.toLocalDate(), result.sunrise?.toLocalDate())
        assertEquals(laZone, result.sunrise?.zone)
        
        assertNotNull(result.sunset)
        assertEquals(date.toLocalDate(), result.sunset?.toLocalDate())
        assertEquals(laZone, result.sunset?.zone)
    }

    @Test
    fun `test Full Integration Pipeline for calculatePanchang`() = runBlocking {
        val date = ZonedDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0), ZoneId.of("Asia/Kolkata"))
        val location = BirthLocation(28.6139, 77.2090, "New Delhi")
        val result = classUnderTest.calculatePanchang(date, location).getOrThrow()
        
        assertEquals(date, result.requestedDateTime)
        assertEquals(location, result.location)
        
        assertTrue(result.tithi.index in 1..30)
        assertNotNull(result.tithi.name)
        assertTrue(result.tithi.remainingPercentage in 0.0..1.0)
        
        assertTrue(result.nakshatra.nakshatra.index in 0..26)
        assertTrue(result.nakshatra.pada in 1..4)
        assertTrue(result.nakshatra.remainingPercentage in 0.0..1.0)
        
        assertTrue(result.yoga.index in 1..27)
        assertTrue(result.yoga.remainingPercentage in 0.0..1.0)
        
        assertTrue(result.karana.index in 1..60)
        assertTrue(result.karana.remainingPercentage in 0.0..1.0)
        
        assertNotNull(result.moonSign)
        assertNotNull(result.sunSign)
        
        assertNotNull(result.metadata)
        assertTrue(result.metadata.ephemerisEngine.contains("Swiss Ephemeris"))
        assertTrue(result.metadata.ayanamsaName.contains("Lahiri"))
    }

    @Test
    fun `test Privacy check PanchangSnapshot has no PII`() = runBlocking {
        val date = ZonedDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0), ZoneId.of("Asia/Kolkata"))
        val location = BirthLocation(28.6139, 77.2090, "New Delhi")
        val result = classUnderTest.calculatePanchang(date, location).getOrThrow()
        
        val fields = result::class.java.declaredFields
        fields.forEach { field ->
            val name = field.name.lowercase()
            assertFalse(name.contains("nativename"))
            assertFalse(name.contains("fullname"))
        }
    }

    @Test
    fun `test Muhurta calculation - Rahukaal and Brahma Muhurta`() = runBlocking {
        val date = ZonedDateTime.of(LocalDate.of(2026, 9, 4), LocalTime.of(12, 0), ZoneId.of("Asia/Kolkata")) // Friday
        val location = BirthLocation(28.6139, 77.2090, "New Delhi")
        val result = classUnderTest.calculatePanchang(date, location).getOrThrow()
        
        assertNotNull(result.muhurta)
        val rahukaal = result.muhurta?.rahukaal
        assertNotNull(rahukaal)
        
        assertTrue(rahukaal!!.start.isAfter(result.sunrise) || rahukaal.start.isEqual(result.sunrise))
        assertTrue(rahukaal.end.isBefore(result.sunset) || rahukaal.end.isEqual(result.sunset))
        
        val brahma = result.muhurta?.brahmaMuhurta
        assertNotNull(brahma)
        assertEquals(result.sunrise!!.minusMinutes(96), brahma!!.start)
        assertEquals(result.sunrise!!.minusMinutes(48), brahma.end)
    }

    @Test
    fun `test Lunar Observance detection`() = runBlocking {
        val date = ZonedDateTime.of(LocalDate.of(2026, 2, 17), LocalTime.of(12, 0), ZoneId.of("Asia/Kolkata")) 
        val location = BirthLocation(28.6139, 77.2090, "New Delhi")
        val result = classUnderTest.calculatePanchang(date, location).getOrThrow()
        
        assertNotNull(result.lunarObservance)
        val obs = result.lunarObservance!!
        assertEquals(result.tithi.index == 15, obs.isPurnima)
        assertEquals(result.tithi.index == 30, obs.isAmavasya)
        assertEquals(result.tithi.index == 11 || result.tithi.index == 26, obs.isEkadashi)
    }
}
