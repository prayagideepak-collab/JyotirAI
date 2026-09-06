package com.example.domain.panchang

import com.example.data.engine.SwissEphAstrologyEngine
import com.example.domain.models.*
import de.thmac.swisseph.SwissEph
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PanchangEngineTest {

    private lateinit var panchangEngine: PanchangEngineImpl
    private lateinit var swissEphEngine: SwissEphAstrologyEngine

    private val delhiLocation = BirthLocation(
        latitude = 28.6139,
        longitude = 77.2090,
        placeName = "New Delhi",
        timeZoneId = "Asia/Kolkata",
        isVerified = true,
        source = "test"
    )

    private val tokyoLocation = BirthLocation(
        latitude = 35.6762,
        longitude = 139.6503,
        placeName = "Tokyo",
        timeZoneId = "Asia/Tokyo",
        isVerified = true,
        source = "test"
    )

    private val losAngelesLocation = BirthLocation(
        latitude = 34.0522,
        longitude = -118.2437,
        placeName = "Los Angeles",
        timeZoneId = "America/Los_Angeles",
        isVerified = true,
        source = "test"
    )

    private val tromsoPolarLocation = BirthLocation(
        latitude = 69.6492,
        longitude = 18.9553,
        placeName = "Tromso",
        timeZoneId = "Europe/Oslo",
        isVerified = true,
        source = "test"
    )

    @Before
    fun setup() {
        panchangEngine = PanchangEngineImpl()
        swissEphEngine = SwissEphAstrologyEngine()
    }

    @Test
    fun testVaraCalculationAcrossTimezoneBoundaries() {
        // Friday in India, while still Thursday in Los Angeles
        val kolkataZone = ZoneId.of("Asia/Kolkata")
        val laZone = ZoneId.of("America/Los_Angeles")

        val fridayMorningIndia = ZonedDateTime.of(LocalDate.of(2026, 9, 4), LocalTime.of(3, 0), kolkataZone)
        val correspondingLaTime = fridayMorningIndia.withZoneSameInstant(laZone) // 2026-09-03 14:30 (Thursday)

        val varaIndia = VaraCalculator.calculate(fridayMorningIndia)
        val varaLA = VaraCalculator.calculate(correspondingLaTime)

        assertEquals(Vara.SHUKRAVARA, varaIndia) // Friday
        assertEquals(Vara.GURUVARA, varaLA)    // Thursday
    }

    @Test
    fun testAllSevenVarasCorrectlyMapped() {
        val monday = LocalDate.of(2026, 9, 7) // Monday
        assertEquals(Vara.SOMAVARA, VaraCalculator.calculate(monday))
        assertEquals(Vara.MANGALAVARA, VaraCalculator.calculate(monday.plusDays(1)))
        assertEquals(Vara.BUDHAVARA, VaraCalculator.calculate(monday.plusDays(2)))
        assertEquals(Vara.GURUVARA, VaraCalculator.calculate(monday.plusDays(3)))
        assertEquals(Vara.SHUKRAVARA, VaraCalculator.calculate(monday.plusDays(4)))
        assertEquals(Vara.SHANIVARA, VaraCalculator.calculate(monday.plusDays(5)))
        assertEquals(Vara.RAVIVARA, VaraCalculator.calculate(monday.plusDays(6)))
    }

    @Test
    fun testTithiElongationAndPaksha() {
        // Amavasya (0° elongation)
        val tithiAmavasya = TithiCalculator.calculate(sunLongitude = 100.0, moonLongitude = 100.0)
        assertEquals(1, tithiAmavasya.index) // Pratipada begins at 0°
        assertEquals(Paksha.SHUKLA, tithiAmavasya.paksha)

        // Exact Full Moon (180° elongation) -> Purnima (index 15)
        val tithiPurnima = TithiCalculator.calculate(sunLongitude = 0.0, moonLongitude = 180.0)
        assertEquals(16, tithiPurnima.index) // 180° begins Krishna Pratipada
        assertEquals(Paksha.KRISHNA, tithiPurnima.paksha)

        // Just before Full Moon (179.99° elongation) -> Shukla Chaturdashi / Purnima transition
        val tithiJustBeforePurnima = TithiCalculator.calculate(sunLongitude = 0.0, moonLongitude = 179.99)
        assertEquals(15, tithiJustBeforePurnima.index)
        assertEquals("Purnima", tithiJustBeforePurnima.name)
        assertEquals(Paksha.SHUKLA, tithiJustBeforePurnima.paksha)
        assertTrue(tithiJustBeforePurnima.isPurnima)

        // Just before Amavasya (359.99° elongation) -> Krishna Amavasya (index 30)
        val tithiJustBeforeAmavasya = TithiCalculator.calculate(sunLongitude = 0.0, moonLongitude = 359.99)
        assertEquals(30, tithiJustBeforeAmavasya.index)
        assertEquals("Amavasya", tithiJustBeforeAmavasya.name)
        assertEquals(Paksha.KRISHNA, tithiJustBeforeAmavasya.paksha)
        assertTrue(tithiJustBeforeAmavasya.isAmavasya)
    }

    @Test
    fun testNakshatraAllSpansAndPadas() {
        // Ashwini (0° - 13°20')
        val nakAshwiniPada1 = NakshatraCalculator.calculate(0.0)
        assertEquals(0, nakAshwiniPada1.nakshatra.index)
        assertEquals("Ashwini", nakAshwiniPada1.nakshatra.sanskritName)
        assertEquals(1, nakAshwiniPada1.pada)

        val nakAshwiniPada4 = NakshatraCalculator.calculate(11.0)
        assertEquals(0, nakAshwiniPada4.nakshatra.index)
        assertEquals(4, nakAshwiniPada4.pada)

        // Bharani (13°20' - 26°40')
        val nakBharani = NakshatraCalculator.calculate(14.0)
        assertEquals(1, nakBharani.nakshatra.index)
        assertEquals("Bharani", nakBharani.nakshatra.sanskritName)

        // Revati (346°40' - 360°)
        val nakRevati = NakshatraCalculator.calculate(359.5)
        assertEquals(26, nakRevati.nakshatra.index)
        assertEquals("Revati", nakRevati.nakshatra.sanskritName)
        assertEquals(4, nakRevati.pada)
    }

    @Test
    fun testNityaYogaCalculationAndSeparation() {
        // Vishkambha (0° to 13°20')
        val yoga1 = PanchangYogaCalculator.calculate(sunLongitude = 0.0, moonLongitude = 5.0)
        assertEquals(1, yoga1.index)
        assertEquals("Vishkambha", yoga1.name)

        // Vaidhriti (346°40' to 360°)
        val yoga27 = PanchangYogaCalculator.calculate(sunLongitude = 180.0, moonLongitude = 175.0)
        assertEquals(27, yoga27.index)
        assertEquals("Vaidhriti", yoga27.name)
    }

    @Test
    fun testKaranaFixedAndMovableClassification() {
        // 1st Karana: Kimstughna (Fixed)
        val k1 = KaranaCalculator.calculate(sunLongitude = 0.0, moonLongitude = 1.0)
        assertEquals(1, k1.index)
        assertEquals("Kimstughna", k1.name)
        assertTrue(k1.isFixed)

        // 2nd Karana: Bava (Movable)
        val k2 = KaranaCalculator.calculate(sunLongitude = 0.0, moonLongitude = 7.0)
        assertEquals(2, k2.index)
        assertEquals("Bava", k2.name)
        assertFalse(k2.isFixed)

        // 58th Karana: Shakuni (Fixed)
        val k58 = KaranaCalculator.calculate(sunLongitude = 0.0, moonLongitude = 343.0)
        assertEquals(58, k58.index)
        assertEquals("Shakuni", k58.name)
        assertTrue(k58.isFixed)

        // 59th Karana: Chatushpada (Fixed)
        val k59 = KaranaCalculator.calculate(sunLongitude = 0.0, moonLongitude = 349.0)
        assertEquals(59, k59.index)
        assertEquals("Chatushpada", k59.name)
        assertTrue(k59.isFixed)

        // 60th Karana: Naga (Fixed)
        val k60 = KaranaCalculator.calculate(sunLongitude = 0.0, moonLongitude = 355.0)
        assertEquals(60, k60.index)
        assertEquals("Naga", k60.name)
        assertTrue(k60.isFixed)
    }

    @Test
    fun testSunriseSunsetAccuracyMultipleLocations() = runBlocking {
        val testDate = LocalDate.of(2026, 6, 21) // Summer Solstice

        val resultDelhi = panchangEngine.calculatePanchangForDate(testDate, delhiLocation).getOrThrow()
        val resultTokyo = panchangEngine.calculatePanchangForDate(testDate, tokyoLocation).getOrThrow()
        val resultLA = panchangEngine.calculatePanchangForDate(testDate, losAngelesLocation).getOrThrow()

        // Delhi
        assertNotNull(resultDelhi.sunrise)
        assertNotNull(resultDelhi.sunset)
        assertEquals(testDate, resultDelhi.sunrise!!.toLocalDate())
        assertEquals("Asia/Kolkata", resultDelhi.sunrise!!.zone.id)
        assertTrue(resultDelhi.sunrise!!.isBefore(resultDelhi.sunset))

        // Tokyo
        assertNotNull(resultTokyo.sunrise)
        assertNotNull(resultTokyo.sunset)
        assertEquals(testDate, resultTokyo.sunrise!!.toLocalDate())
        assertEquals("Asia/Tokyo", resultTokyo.sunrise!!.zone.id)
        assertTrue(resultTokyo.sunrise!!.isBefore(resultTokyo.sunset))

        // Los Angeles
        assertNotNull(resultLA.sunrise)
        assertNotNull(resultLA.sunset)
        assertEquals(testDate, resultLA.sunrise!!.toLocalDate())
        assertEquals("America/Los_Angeles", resultLA.sunrise!!.zone.id)
        assertTrue(resultLA.sunrise!!.isBefore(resultLA.sunset))

        // Ensure sunrise instants are distinct across distinct geographical longitudes
        assertNotEquals(resultDelhi.sunrise!!.toInstant(), resultTokyo.sunrise!!.toInstant())
    }

    @Test
    fun testSelectedDateIsolationNoDataReuse() = runBlocking {
        val date1 = LocalDate.of(2026, 1, 15)
        val date2 = LocalDate.of(2026, 8, 15)
        val date3 = LocalDate.of(2027, 3, 20)

        val res1 = panchangEngine.calculatePanchangForDate(date1, delhiLocation).getOrThrow()
        val res2 = panchangEngine.calculatePanchangForDate(date2, delhiLocation).getOrThrow()
        val res3 = panchangEngine.calculatePanchangForDate(date3, delhiLocation).getOrThrow()

        // Verify that different dates yield distinct planetary configurations and dates
        assertEquals(date1, res1.selectedDate)
        assertEquals(date2, res2.selectedDate)
        assertEquals(date3, res3.selectedDate)

        assertNotEquals(res1.sunContext.longitude, res2.sunContext.longitude)
        assertNotEquals(res2.sunContext.longitude, res3.sunContext.longitude)
        assertNotEquals(res1.sunrise, res2.sunrise)
    }

    @Test
    fun testLocationIsolationNoDataReuse() = runBlocking {
        val date = LocalDate.of(2026, 9, 6)

        val resDelhi = panchangEngine.calculatePanchangForDate(date, delhiLocation).getOrThrow()
        val resTokyo = panchangEngine.calculatePanchangForDate(date, tokyoLocation).getOrThrow()

        assertEquals("New Delhi", resDelhi.location.placeName)
        assertEquals("Tokyo", resTokyo.location.placeName)

        assertNotEquals(resDelhi.sunrise, resTokyo.sunrise)
        assertNotEquals(resDelhi.sunset, resTokyo.sunset)
    }

    @Test
    fun testCacheSafetyAndIsolation() = runBlocking {
        val date = LocalDate.of(2026, 9, 6)

        panchangEngine.clearCache()
        val res1 = panchangEngine.calculatePanchangForDate(date, delhiLocation).getOrThrow()
        val res2 = panchangEngine.calculatePanchangForDate(date, delhiLocation).getOrThrow()

        // Same result served from cache
        assertEquals(res1.id, res2.id)

        // Requesting another location creates a distinct result
        val resTokyo = panchangEngine.calculatePanchangForDate(date, tokyoLocation).getOrThrow()
        assertNotEquals(res1.id, resTokyo.id)
    }

    @Test
    fun testPolarRegionGracefulLimitedData() = runBlocking {
        // Tromso in June (Midnight sun - Polar Day)
        val summerDate = LocalDate.of(2026, 6, 21)
        val polarResult = panchangEngine.calculatePanchangForDate(summerDate, tromsoPolarLocation).getOrThrow()

        // Core 5 Angas must still compute accurately
        assertTrue(polarResult.tithi.index in 1..30)
        assertTrue(polarResult.nakshatra.nakshatra.index in 0..26)
        assertTrue(polarResult.yoga.index in 1..27)
        assertTrue(polarResult.karana.index in 1..60)

        // Result state reflects limited data due to polar condition
        assertEquals(PanchangResultState.LIMITED_DATA, polarResult.resultState)
        assertTrue(polarResult.calculationLimitations.isNotEmpty())
    }

    @Test
    fun testInvalidInputRejection() = runBlocking {
        // Year before 1900
        val ancientDate = LocalDate.of(1850, 1, 1)
        val ancientResult = panchangEngine.calculatePanchangForDate(ancientDate, delhiLocation)
        assertTrue(ancientResult.isFailure)

        // Year after 2100
        val farFutureDate = LocalDate.of(2150, 1, 1)
        val futureResult = panchangEngine.calculatePanchangForDate(farFutureDate, delhiLocation)
        assertTrue(futureResult.isFailure)

        // Invalid coordinates rejected by Location validation
        assertFalse(LocationContextResolver.isValidCoordinates(120.0, 77.0))
        assertFalse(LocationContextResolver.isValidCoordinates(28.0, 200.0))
    }

    @Test
    fun testCrossEngineConsistencyWithKundliAndTransit() = runBlocking {
        val testInstant = ZonedDateTime.of(LocalDate.of(2026, 9, 6), LocalTime.of(12, 0), ZoneId.of("Asia/Kolkata"))

        val panchang = panchangEngine.calculatePanchangResult(testInstant, delhiLocation).getOrThrow()
        val transit = swissEphEngine.calculateTransitSnapshot(testInstant, delhiLocation, null).getOrThrow()

        // Sun & Moon Longitudes in Panchang must match Transit Engine within 0.05 degree precision
        val transitSun = transit.positions.first { it.planet.equals("Sun", ignoreCase = true) }
        val transitMoon = transit.positions.first { it.planet.equals("Moon", ignoreCase = true) }

        assertEquals(transitSun.totalLongitude, panchang.sunContext.longitude, 0.05)
        assertEquals(transitMoon.totalLongitude, panchang.moonContext.longitude, 0.05)

        // Signs must match identically
        assertEquals(transitSun.rashiEnum, panchang.sunContext.sign)
        assertEquals(transitMoon.rashiEnum, panchang.moonContext.sign)
    }

    @Test
    fun testHindiPresentationFormatting() = runBlocking {
        val date = LocalDate.of(2026, 9, 6)
        val panchang = panchangEngine.calculatePanchangForDate(date, delhiLocation).getOrThrow()

        val formattedTithi = PanchangHindiPresenter.formatTithi(panchang)
        val formattedVara = PanchangHindiPresenter.formatVara(panchang)
        val speech = PanchangHindiPresenter.formatSpeechSummary(panchang)

        assertTrue(formattedTithi.isNotEmpty())
        assertTrue(formattedVara.contains("वार"))
        assertTrue(speech.contains("दैनिक पंचांग विवरण"))
        assertTrue(speech.contains("New Delhi"))
    }
}
