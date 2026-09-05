package com.example.domain.speech

import com.example.domain.models.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class AstrologyHindiSpeechFormatterTest {

    @Test
    fun testFormatDailyRashifal_preservesSanskritTermsAndRemovesMarkdown() {
        val rashifal = DailyRashifal(
            profileId = "profile_1",
            profileName = "Aarav",
            date = LocalDate.of(2026, 9, 5),
            dailyTheme = "Constructive Communication & Career Focus",
            primaryFocus = "Express clarity in discussions with Saturn's guidance",
            energyScore = 84,
            astrologicalAlignment = AstrologicalAlignment(
                moonRashi = "Gemini",
                transitNakshatra = "Punarvasu",
                activeMahadashaLord = "Jupiter",
                activeAntardashaLord = "Mercury",
                isMoonInDusthana = false,
                taraBalaCategory = "Mitra",
                isTaraBalaFavorable = true,
                ashtakavargaScore = 32
            ),
            keyInfluences = listOf(
                KeyInfluence(
                    category = "Dasha",
                    astrologicalSource = "Jupiter Mahadasha and Mercury Antardasha",
                    interpretationText = "Intellectual pursuits and communication align favorably."
                )
            ),
            priorities = listOf("Focus on scholarly research and financial planning"),
            cautions = listOf("Avoid hasty arguments during afternoon hours"),
            timingGuidance = TimingGuidance(
                brahmaMuhurta = "04:35 AM - 05:22 AM",
                abhijitMuhurta = "11:50 AM - 12:40 PM",
                rahukaal = "09:10 AM - 10:45 AM",
                recommendedTimeWindow = "Morning hours for contemplation"
            ),
            traditionalRemedies = listOf("Practice Vishnu Sahasranama or mindful contemplation"),
            birthLocationName = "New Delhi, India"
        )

        val speechText = AstrologyHindiSpeechFormatter.formatDailyRashifal(rashifal)

        // Check Vedic terms translated
        assertTrue("Speech should contain Surya or Chandra", speechText.contains("चन्द्रमा") || speechText.contains("चन्द्र"))
        assertTrue("Speech should contain Mithuna (Gemini)", speechText.contains("मिथुन"))
        assertTrue("Speech should contain Punarvasu", speechText.contains("पुनर्वसु"))
        assertTrue("Speech should contain Guru (Jupiter)", speechText.contains("गुरु"))
        assertTrue("Speech should contain Budha (Mercury)", speechText.contains("बुध"))
        assertTrue("Speech should contain Mahadasha", speechText.contains("महादशा"))
        assertTrue("Speech should contain Antardasha", speechText.contains("अंतर्दशा"))

        // Check non-fatalistic disclaimer
        assertTrue("Speech should contain traditional guidance disclaimer", speechText.contains("सांकेतिक मार्गदर्शन"))

        // Check no Markdown or formatting artifacts
        assertFalse("Speech should not contain markdown hashes", speechText.contains("#"))
        assertFalse("Speech should not contain markdown asterisks", speechText.contains("*"))
        assertFalse("Speech should not contain bullet dots", speechText.contains("•"))
    }

    @Test
    fun testFormatKundliSummary_containsAscendantAndGrahaPositions() {
        val birthData = BirthData(
            name = "Rohan Verma",
            date = LocalDate.of(1990, 5, 20),
            time = LocalTime.of(10, 15),
            location = BirthLocation(28.6139, 77.2090, "New Delhi", true, "verified"),
            timeZone = ZoneId.of("Asia/Kolkata")
        )
        val rashiChart = Chart(
            title = "Lagna Chart (D1)",
            type = VargaType.D1,
            planets = listOf(
                PlanetPosition(
                    name = "Sun",
                    sanskritName = "Surya",
                    longitude = 35.5,
                    rashi = Rashi.TAURUS,
                    rashiDegree = 5.5,
                    house = 11,
                    nakshatra = Nakshatra.KRITTIKA,
                    nakshatraPada = 3,
                    isRetrograde = false,
                    speed = 1.0,
                    dignity = PlanetDignity.FRIEND
                )
            ),
            houses = emptyList(),
            ascendantRashi = Rashi.CANCER
        )
        val profile = AstrologyProfile(
            birthData = birthData,
            rashiChart = rashiChart,
            divisionalCharts = mapOf(VargaType.D1 to rashiChart),
            ascendant = PlanetPosition(
                name = "Ascendant",
                sanskritName = "Lagna",
                longitude = 95.0,
                rashi = Rashi.CANCER,
                rashiDegree = 5.0,
                house = 1,
                nakshatra = Nakshatra.PUSHYA,
                nakshatraPada = 1,
                isRetrograde = false,
                speed = 0.0,
                dignity = PlanetDignity.NEUTRAL
            ),
            moonSign = Rashi.PISCES,
            sunSign = Rashi.TAURUS,
            nakshatra = NakshatraInfo(Nakshatra.PUSHYA, 1, 0.5),
            ayanamsaName = "Chitra Paksha (Lahiri)",
            ayanamsaValue = 23.7
        )

        val speechText = AstrologyHindiSpeechFormatter.formatKundliSummary(profile, rashiChart)

        assertTrue(speechText.contains("Rohan Verma"))
        assertTrue(speechText.contains("सूर्य"))
        assertTrue(speechText.contains("वृषभ"))
        assertTrue(speechText.contains("लग्न"))
        assertFalse(speechText.contains("*"))
    }
}
