package com.example

import com.example.domain.engine.DoshaAnalysisEngine
import com.example.domain.engine.YogaAnalysisEngine
import com.example.domain.engine.YogaDoshaCalculator
import com.example.domain.engine.yogadosha.*
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
        name: String = "Test Native",
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
            name = name
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
        assertEquals(AnalysisStatus.DETECTED, gkYoga.status)
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
        assertEquals(AnalysisStatus.DETECTED, manglik.status)
    }

    @Test
    fun testManglikNegativeCase() {
        // Mars in Gemini (House 3 for Aries Lagna) -> Not in 1, 2, 4, 7, 8, 12 from Lagna (3), Moon (3), or Venus (11)
        val planets = listOf(
            pos("Mars", Rashi.GEMINI, 3, 10.0),
            pos("Moon", Rashi.ARIES, 1, 10.0),
            pos("Sun", Rashi.TAURUS, 2, 5.0),
            pos("Mercury", Rashi.GEMINI, 3, 12.0),
            pos("Jupiter", Rashi.PISCES, 12, 14.0),
            pos("Venus", Rashi.LEO, 5, 22.0), // Mars is in 11th from Venus (non-Manglik)
            pos("Saturn", Rashi.LIBRA, 7, 20.0),
            pos("Rahu", Rashi.LEO, 5, 15.0),
            pos("Ketu", Rashi.AQUARIUS, 11, 15.0)
        )
        val profile = createSampleProfile(lagnaSignIndex = 0, planets = planets)
        val doshas = DoshaAnalysisEngine.analyzeDoshas(profile)

        val manglik = doshas.firstOrNull { it.id == "manglik_dosha" }
        assertNotNull(manglik)
        assertFalse("Manglik Dosha should not be detected", manglik!!.isDetected)
        assertEquals(DoshaSeverity.NONE, manglik.severity)
        assertEquals(AnalysisStatus.NOT_DETECTED, manglik.status)
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
    fun testKaalSarpNegativeCase() {
        // Rahu in Aries (1), Ketu in Libra (7). Some planets on one side, some on the other
        val planets = listOf(
            pos("Rahu", Rashi.ARIES, 1, 10.0),
            pos("Ketu", Rashi.LIBRA, 7, 10.0),
            pos("Sun", Rashi.TAURUS, 2, 15.0),
            pos("Moon", Rashi.SAGITTARIUS, 9, 20.0), // On the other side!
            pos("Mars", Rashi.CANCER, 4, 25.0),
            pos("Mercury", Rashi.TAURUS, 2, 28.0),
            pos("Jupiter", Rashi.LEO, 5, 12.0),
            pos("Venus", Rashi.GEMINI, 3, 5.0),
            pos("Saturn", Rashi.CAPRICORN, 10, 18.0) // On the other side!
        )
        val profile = createSampleProfile(lagnaSignIndex = 0, planets = planets)
        val doshas = DoshaAnalysisEngine.analyzeDoshas(profile)

        val detectedKaalSarp = doshas.filter { it.category == DoshaCategory.KAAL_SARP && it.isDetected }
        assertTrue("No Kaal Sarp Dosha should be detected", detectedKaalSarp.isEmpty())
    }

    @Test
    fun testKemadrumaDoshaAndBhanga() {
        // Moon in Aries (House 1), 2nd (Taurus) and 12th (Pisces) empty of planets
        // But Jupiter in Kendra (Cancer, House 4) -> Kemadruma Bhanga (Cancellation)
        val planets = listOf(
            pos("Moon", Rashi.ARIES, 1, 15.0),
            pos("Jupiter", Rashi.CANCER, 4, 10.0),
            pos("Sun", Rashi.LEO, 5, 10.0),
            pos("Mercury", Rashi.LEO, 5, 15.0),
            pos("Mars", Rashi.VIRGO, 6, 20.0),
            pos("Venus", Rashi.LIBRA, 7, 12.0),
            pos("Saturn", Rashi.SCORPIO, 8, 5.0),
            pos("Rahu", Rashi.SAGITTARIUS, 9, 2.0),
            pos("Ketu", Rashi.GEMINI, 3, 2.0)
        )
        val profile = createSampleProfile(lagnaSignIndex = 0, planets = planets)
        val doshas = DoshaAnalysisEngine.analyzeDoshas(profile)

        val kemadruma = doshas.firstOrNull { it.id == "kemadruma_dosha" }
        assertNotNull(kemadruma)
        assertTrue("Kemadruma should be detected", kemadruma!!.isDetected)
        assertTrue("Kemadruma Bhanga should apply due to Kendra planets & Jupiter", kemadruma.isCancelled)
        assertEquals(DoshaSeverity.CANCELLED, kemadruma.severity)
    }

    @Test
    fun testGuruChandalPitraAndShrapitDoshas() {
        // Jupiter with Rahu -> Guru Chandal
        // Sun with Rahu -> Pitra Dosha
        // Saturn with Rahu -> Shrapit Dosha
        val planets = listOf(
            pos("Sun", Rashi.ARIES, 1),
            pos("Moon", Rashi.TAURUS, 2),
            pos("Jupiter", Rashi.ARIES, 1),
            pos("Saturn", Rashi.ARIES, 1),
            pos("Rahu", Rashi.ARIES, 1),
            pos("Ketu", Rashi.LIBRA, 7),
            pos("Mars", Rashi.LEO, 5),
            pos("Mercury", Rashi.GEMINI, 3),
            pos("Venus", Rashi.CANCER, 4)
        )
        val profile = createSampleProfile(lagnaSignIndex = 0, planets = planets)
        val doshas = DoshaAnalysisEngine.analyzeDoshas(profile)

        val guruChandal = doshas.firstOrNull { it.id == "guru_chandal_dosha" }
        val pitra = doshas.firstOrNull { it.id == "pitra_dosha" }
        val shrapit = doshas.firstOrNull { it.id == "shrapit_dosha" }

        assertNotNull(guruChandal)
        assertTrue("Guru Chandal should be detected", guruChandal!!.isDetected)

        assertNotNull(pitra)
        assertTrue("Pitra Dosha should be detected", pitra!!.isDetected)

        assertNotNull(shrapit)
        assertTrue("Shrapit Dosha should be detected", shrapit!!.isDetected)
    }

    @Test
    fun testGandmantaDosha() {
        val gandmoolMoon = pos("Moon", Rashi.ARIES, 1, 5.0, nakshatraName = "Ashwini", nakshatraPada = 1)
        val nonGandmoolMoon = pos("Moon", Rashi.TAURUS, 2, 15.0, nakshatraName = "Rohini", nakshatraPada = 2)

        val planets1 = listOf(
            gandmoolMoon,
            pos("Sun", Rashi.LEO, 5),
            pos("Mars", Rashi.SCORPIO, 8),
            pos("Mercury", Rashi.GEMINI, 3),
            pos("Jupiter", Rashi.SAGITTARIUS, 9),
            pos("Venus", Rashi.LIBRA, 7),
            pos("Saturn", Rashi.CAPRICORN, 10),
            pos("Rahu", Rashi.VIRGO, 6),
            pos("Ketu", Rashi.PISCES, 12)
        )
        val profile1 = createSampleProfile(lagnaSignIndex = 0, planets = planets1)
        val doshas1 = DoshaAnalysisEngine.analyzeDoshas(profile1)
        val gm1 = doshas1.firstOrNull { it.id.startsWith("gandmanta_dosha") }
        assertNotNull(gm1)
        assertTrue("Gandmool should be detected for Ashwini", gm1!!.isDetected)

        val planets2 = listOf(
            nonGandmoolMoon,
            pos("Sun", Rashi.LEO, 5),
            pos("Mars", Rashi.SCORPIO, 8),
            pos("Mercury", Rashi.GEMINI, 3),
            pos("Jupiter", Rashi.SAGITTARIUS, 9),
            pos("Venus", Rashi.LIBRA, 7),
            pos("Saturn", Rashi.CAPRICORN, 10),
            pos("Rahu", Rashi.VIRGO, 6),
            pos("Ketu", Rashi.PISCES, 12)
        )
        val profile2 = createSampleProfile(lagnaSignIndex = 0, planets = planets2)
        val doshas2 = DoshaAnalysisEngine.analyzeDoshas(profile2)
        val gm2 = doshas2.firstOrNull { it.id.startsWith("gandmanta_dosha") }
        assertNotNull(gm2)
        assertFalse("Gandmool should not be detected for Rohini", gm2!!.isDetected)
    }

    @Test
    fun testViparitaRajaYogas() {
        // Aries Lagna -> 6th lord is Mercury (Virgo), 8th lord is Mars (Scorpio), 12th lord is Jupiter (Pisces)
        // Place Mercury in 8th (Scorpio), Mars in 6th (Virgo), Jupiter in 12th (Pisces)
        val planets = listOf(
            pos("Sun", Rashi.ARIES, 1),
            pos("Moon", Rashi.TAURUS, 2),
            pos("Mercury", Rashi.SCORPIO, 8, customDignity = PlanetDignity.NEUTRAL), // 6th lord in 8th -> Harsha Yoga
            pos("Mars", Rashi.VIRGO, 6, customDignity = PlanetDignity.ENEMY),       // 8th lord in 6th -> Sarala Yoga
            pos("Jupiter", Rashi.PISCES, 12, customDignity = PlanetDignity.OWN_SIGN), // 12th lord in 12th -> Vimala Yoga
            pos("Venus", Rashi.GEMINI, 3),
            pos("Saturn", Rashi.CANCER, 4),
            pos("Rahu", Rashi.LEO, 5),
            pos("Ketu", Rashi.AQUARIUS, 11)
        )
        val profile = createSampleProfile(lagnaSignIndex = 0, planets = planets)
        val yogas = YogaAnalysisEngine.analyzeYogas(profile)

        val harsha = yogas.firstOrNull { it.id == "viparita_harsha_yoga" }
        val sarala = yogas.firstOrNull { it.id == "viparita_sarala_yoga" }
        val vimala = yogas.firstOrNull { it.id == "viparita_vimala_yoga" }

        assertNotNull("Harsha Yoga should be evaluated", harsha)
        assertTrue("Harsha Yoga should be detected", harsha!!.isDetected)

        assertNotNull("Sarala Yoga should be evaluated", sarala)
        assertTrue("Sarala Yoga should be detected", sarala!!.isDetected)

        assertNotNull("Vimala Yoga should be evaluated", vimala)
        assertTrue("Vimala Yoga should be detected", vimala!!.isDetected)
    }

    @Test
    fun testNeechaBhangaRajaYoga() {
        // Sun debilitated in Libra (House 7 for Aries Lagna)
        // Dispositor Venus in Taurus (House 2, Own sign) or in Cancer (Kendra from Lagna)
        val planets = listOf(
            pos("Sun", Rashi.LIBRA, 7, 10.0, customDignity = PlanetDignity.DEBILITATED),
            pos("Venus", Rashi.CANCER, 4, 15.0, customDignity = PlanetDignity.ENEMY), // Dispositor of Sun in Kendra from Lagna
            pos("Moon", Rashi.ARIES, 1, 10.0),
            pos("Mars", Rashi.ARIES, 1, 15.0),
            pos("Mercury", Rashi.VIRGO, 6, 12.0),
            pos("Jupiter", Rashi.SAGITTARIUS, 9, 20.0),
            pos("Saturn", Rashi.AQUARIUS, 11, 5.0),
            pos("Rahu", Rashi.LEO, 5, 2.0),
            pos("Ketu", Rashi.AQUARIUS, 11, 2.0)
        )
        val profile = createSampleProfile(lagnaSignIndex = 0, planets = planets)
        val yogas = YogaAnalysisEngine.analyzeYogas(profile)

        val neechaBhanga = yogas.firstOrNull { it.id == "neecha_bhanga_sun" }
        assertNotNull("Neecha Bhanga Sun should be detected", neechaBhanga)
        assertTrue(neechaBhanga!!.isDetected)
        assertEquals(YogaStrength.STRONG, neechaBhanga.strength)
    }

    @Test
    fun testProfileIsolationAndSwitching() {
        val planetsA = listOf(
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
        val profileA = createSampleProfile("Profile A", lagnaSignIndex = 0, planets = planetsA)
        val snapshotA = YogaDoshaEngine.calculate(profileA)

        val planetsB = listOf(
            pos("Mars", Rashi.GEMINI, 3, 15.0),
            pos("Moon", Rashi.CANCER, 4, 10.0),
            pos("Sun", Rashi.VIRGO, 6, 12.0),
            pos("Mercury", Rashi.VIRGO, 6, 16.0),
            pos("Jupiter", Rashi.CAPRICORN, 10, 20.0),
            pos("Venus", Rashi.SCORPIO, 8, 14.0),
            pos("Saturn", Rashi.TAURUS, 2, 5.0),
            pos("Rahu", Rashi.LIBRA, 7, 2.0),
            pos("Ketu", Rashi.ARIES, 1, 2.0)
        )
        val profileB = createSampleProfile("Profile B", lagnaSignIndex = 3, planets = planetsB)
        val snapshotB = YogaDoshaEngine.calculate(profileB)

        assertEquals("Profile A", snapshotA.profileName)
        assertEquals("Profile B", snapshotB.profileName)
        assertNotEquals(snapshotA.detectedYogas.map { it.id }, snapshotB.detectedYogas.map { it.id })
    }

    @Test
    fun testResultValidatorCompleteness() {
        val incompleteProfile = createSampleProfile(
            lagnaSignIndex = 0,
            planets = listOf(pos("Sun", Rashi.ARIES, 1)) // Missing remaining 8 planets
        )
        val validation = ResultValidator.validateProfileData(incompleteProfile)
        assertFalse(validation.isValid)
        assertTrue(validation.reason.contains("Missing essential planets"))

        val snapshot = YogaDoshaEngine.calculate(incompleteProfile)
        assertTrue(snapshot.detectedYogas.isEmpty())
        assertTrue(snapshot.summaryText.contains("गणना संभव नहीं"))
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
        val profile = createSampleProfile("Test Native", lagnaSignIndex = 0, planets = planets)
        val snapshot = YogaDoshaCalculator.calculate(profile)

        assertNotNull(snapshot)
        assertTrue(snapshot.detectedYogas.isNotEmpty())
        assertTrue(snapshot.summaryText.isNotEmpty())
        assertNotNull(snapshot.dominantYoga)
        assertEquals(profile.birthData.name, snapshot.profileName)
    }
}
