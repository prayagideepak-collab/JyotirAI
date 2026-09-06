package com.example

import com.example.domain.engine.DoshaAnalysisEngine
import com.example.domain.engine.YogaAnalysisEngine
import com.example.domain.engine.YogaDoshaCalculator
import com.example.domain.models.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class YogaDoshaEngineTest {

    private fun pos(
        planet: String,
        rashi: Rashi,
        house: Int,
        degreeInSign: Double = 10.0,
        nakshatraName: String = "Ashwini",
        nakshatraLord: String = "Ketu",
        nakshatraPada: Int = 1,
        isRetrograde: Boolean = false,
        speed: Double = 1.0,
        customDignity: PlanetDignity? = null
    ): PlanetPosition {
        val signIndex = rashi.index
        val totalLongitude = (signIndex * 30.0) + degreeInSign
        val calculatedDignity = customDignity ?: PlanetDignity.calculate(planet, signIndex, degreeInSign)
        return PlanetPosition(
            planet = planet,
            sign = rashi.englishName,
            signIndex = signIndex,
            totalLongitude = totalLongitude,
            degreeInSign = degreeInSign,
            house = house,
            isRetrograde = isRetrograde,
            nakshatra = nakshatraName,
            nakshatraLord = nakshatraLord,
            nakshatraPada = nakshatraPada,
            speed = speed,
            dignity = calculatedDignity
        )
    }

    private fun createSampleProfile(
        lagnaSignIndex: Int = 0, // Aries
        planets: List<PlanetPosition>
    ): AstrologyProfile {
        val location = BirthLocation(
            latitude = 28.6139,
            longitude = 77.2090,
            placeName = "New Delhi",
            timeZoneId = "Asia/Kolkata"
        )
        val birthData = BirthData(
            date = LocalDate.of(1995, 5, 15),
            time = LocalTime.of(14, 30),
            location = location,
            timeZone = ZoneId.of("Asia/Kolkata"),
            name = "Test Native"
        )
        val chart = Chart(
            type = "D1",
            positions = planets,
            ascendantSign = Rashi.fromIndex(lagnaSignIndex).englishName,
            ascendantSignIndex = lagnaSignIndex,
            ascendantDegreeInSign = 15.0
        )
        val metadata = CalculationMetadata(
            ephemerisEngine = "Swiss Ephemeris",
            ayanamsaName = "Lahiri",
            ayanamsaDegree = 23.85,
            julianDayUt = 2449853.5,
            calculatedUtcIso = "1995-05-15T09:00:00Z"
        )
        return AstrologyProfile(
            birthData = birthData,
            rashiChart = chart,
            lagna = Rashi.fromIndex(lagnaSignIndex).englishName,
            lagnaSignIndex = lagnaSignIndex,
            lagnaLongitude = (lagnaSignIndex * 30.0) + 15.0,
            lagnaDegreeInSign = 15.0,
            lagnaNakshatra = Nakshatra.ASHWINI.sanskritName,
            lagnaPada = 1,
            moonSign = planets.firstOrNull { it.planet.equals("moon", ignoreCase = true) }?.sign ?: "Aries",
            moonSignIndex = planets.firstOrNull { it.planet.equals("moon", ignoreCase = true) }?.signIndex ?: 0,
            nakshatra = planets.firstOrNull { it.planet.equals("moon", ignoreCase = true) }?.nakshatra ?: "Ashwini",
            nakshatraPada = 1,
            nakshatraLord = "Ketu",
            planetPositions = planets,
            metadata = metadata
        )
    }

    @Test
    fun testGajaKesariYogaDetection() {
        // Moon in Aries (House 1), Jupiter in Cancer (House 4 -> Kendra from Moon, Exalted)
        val planets = listOf(
            pos("Sun", Rashi.TAURUS, 2),
            pos("Moon", Rashi.ARIES, 1, 10.0),
            pos("Jupiter", Rashi.CANCER, 4, 12.0),
            pos("Mars", Rashi.GEMINI, 3),
            pos("Mercury", Rashi.TAURUS, 2),
            pos("Venus", Rashi.GEMINI, 3),
            pos("Saturn", Rashi.PISCES, 12),
            pos("Rahu", Rashi.LIBRA, 7),
            pos("Ketu", Rashi.ARIES, 1)
        )
        val profile = createSampleProfile(lagnaSignIndex = 0, planets = planets)
        val yogas = YogaAnalysisEngine.analyzeYogas(profile)

        val gkYoga = yogas.firstOrNull { it.id == "gaja_kesari_yoga" }
        assertNotNull(gkYoga)
        assertTrue("Gaja Kesari Yoga should be detected", gkYoga!!.isDetected)
        assertEquals(YogaStrength.EXCELLENT, gkYoga.strength)
    }

    @Test
    fun testRuchakaMahapurushaYogaDetection() {
        // Mars in Aries (House 1 -> Kendra + Own sign)
        val planets = listOf(
            pos("Mars", Rashi.ARIES, 1, 15.0),
            pos("Moon", Rashi.TAURUS, 2, 10.0),
            pos("Sun", Rashi.LEO, 5, 12.0),
            pos("Mercury", Rashi.LEO, 5, 16.0),
            pos("Jupiter", Rashi.SAGITTARIUS, 9, 20.0),
            pos("Venus", Rashi.LIBRA, 7, 14.0),
            pos("Saturn", Rashi.AQUARIUS, 11, 5.0),
            pos("Rahu", Rashi.VIRGO, 6, 2.0),
            pos("Ketu", Rashi.PISCES, 12, 2.0)
        )
        val profile = createSampleProfile(lagnaSignIndex = 0, planets = planets)
        val yogas = YogaAnalysisEngine.analyzeYogas(profile)

        val ruchaka = yogas.firstOrNull { it.id == "mahapurusha_ruchaka" }
        assertNotNull(ruchaka)
        assertTrue("Ruchaka Yoga should be detected", ruchaka!!.isDetected)
        assertEquals(YogaStrength.STRONG, ruchaka.strength)

        // Budhaditya in 5th house
        val budhaditya = yogas.firstOrNull { it.id == "budhaditya_yoga" }
        assertNotNull(budhaditya)
        assertTrue("Budhaditya Yoga should be detected", budhaditya!!.isDetected)
    }

    @Test
    fun testManglikDoshaAndCancellation() {
        // Mars in Capricorn (House 7 -> Exalted in 7th house -> Manglik with classical cancellation)
        val planets = listOf(
            pos("Mars", Rashi.CAPRICORN, 7, 28.0),
            pos("Moon", Rashi.ARIES, 10, 10.0),
            pos("Sun", Rashi.TAURUS, 11, 5.0),
            pos("Mercury", Rashi.GEMINI, 12, 12.0),
            pos("Jupiter", Rashi.PISCES, 9, 14.0),
            pos("Venus", Rashi.TAURUS, 11, 22.0),
            pos("Saturn", Rashi.LIBRA, 4, 20.0),
            pos("Rahu", Rashi.LEO, 2, 15.0),
            pos("Ketu", Rashi.AQUARIUS, 8, 15.0)
        )
        val profile = createSampleProfile(lagnaSignIndex = 3, planets = planets) // Cancer Lagna -> Capricorn is 7th house
        val doshas = DoshaAnalysisEngine.analyzeDoshas(profile)

        val manglik = doshas.firstOrNull { it.id == "manglik_dosha" }
        assertNotNull(manglik)
        assertTrue("Manglik Dosha should be detected", manglik!!.isDetected)
        assertTrue("Manglik Dosha should be cancelled due to exaltation in 7th", manglik.isCancelled)
        assertEquals(DoshaSeverity.CANCELLED, manglik.severity)
    }

    @Test
    fun testKaalSarpDoshaDetection() {
        // Rahu in House 1 (Aries 10°), Ketu in House 7 (Libra 10°)
        // All other 7 planets enclosed between Aries and Libra (Taurus, Gemini, Cancer, Leo, Virgo)
        val planets = listOf(
            pos("Rahu", Rashi.ARIES, 1, 10.0),
            pos("Ketu", Rashi.LIBRA, 7, 10.0),
            pos("Sun", Rashi.TAURUS, 2, 15.0),
            pos("Moon", Rashi.GEMINI, 3, 20.0),
            pos("Mars", Rashi.CANCER, 4, 25.0),
            pos("Mercury", Rashi.TAURUS, 2, 28.0),
            pos("Jupiter", Rashi.LEO, 5, 12.0),
            pos("Venus", Rashi.GEMINI, 3, 5.0),
            pos("Saturn", Rashi.VIRGO, 6, 18.0)
        )
        val profile = createSampleProfile(lagnaSignIndex = 0, planets = planets)
        val doshas = DoshaAnalysisEngine.analyzeDoshas(profile)

        val kaalSarp = doshas.firstOrNull { it.category == DoshaCategory.KAAL_SARP && it.isDetected }
        assertNotNull("Kaal Sarp Dosha should be detected", kaalSarp)
        assertTrue(kaalSarp!!.name.contains("Anant Kaal Sarp"))
        assertEquals(DoshaSeverity.HIGH, kaalSarp.severity)
    }

    @Test
    fun testYogaDoshaCalculatorSnapshot() {
        val planets = listOf(
            pos("Sun", Rashi.ARIES, 1, 10.0),
            pos("Moon", Rashi.CANCER, 4, 15.0),
            pos("Mars", Rashi.CAPRICORN, 10, 28.0),
            pos("Mercury", Rashi.ARIES, 1, 12.0),
            pos("Jupiter", Rashi.CANCER, 4, 5.0),
            pos("Venus", Rashi.PISCES, 12, 27.0),
            pos("Saturn", Rashi.LIBRA, 7, 20.0),
            pos("Rahu", Rashi.TAURUS, 2, 15.0),
            pos("Ketu", Rashi.SCORPIO, 8, 15.0)
        )
        val profile = createSampleProfile(lagnaSignIndex = 0, planets = planets)
        val snapshot = YogaDoshaCalculator.calculate(profile)

        assertNotNull(snapshot)
        assertTrue(snapshot.detectedYogas.isNotEmpty())
        assertTrue(snapshot.summaryText.isNotEmpty())
        assertNotNull(snapshot.dominantYoga)
    }
}
