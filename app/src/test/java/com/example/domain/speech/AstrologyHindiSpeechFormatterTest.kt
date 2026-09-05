package com.example.domain.speech

import com.example.domain.models.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AstrologyHindiSpeechFormatterTest {

    @Test
    fun testFormatDailyRashifal_preservesSanskritTermsAndRemovesMarkdown() {
        val rashifal = DailyRashifal(
            defaultProfileId = "profile_1",
            profileName = "Aarav",
            targetDate = LocalDate.of(2026, 9, 5),
            targetDateTime = ZonedDateTime.now(),
            birthLocationName = "New Delhi, India",
            dailyTheme = "Constructive Communication & Career Focus",
            energyScore = 84,
            primaryFocus = "Express clarity in discussions with Saturn's guidance",
            lagna = "Aries",
            moonSign = "Gemini",
            birthNakshatra = "Punarvasu",
            currentMahadashaLord = "Jupiter",
            currentAntardashaLord = "Mercury",
            transitMoonHouseFromNatalMoon = 1,
            transitMoonHouseFromLagna = 1,
            taraBala = TaraBalaInfo(taraNumber = 8, taraName = "Mitra", quality = "Favorable", description = "Harmonious collaboration"),
            keyInfluences = listOf(
                AstrologicalInfluence(
                    title = "Dasha",
                    description = "Intellectual pursuits and communication align favorably.",
                    contributingFactor = "Jupiter Mahadasha and Mercury Antardasha",
                    impactType = ImpactType.FAVORABLE
                )
            ),
            priorities = listOf(DailyRecommendation("Focus", "Focus on scholarly research and financial planning", "Astrological")),
            cautions = listOf(DailyCaution("Caution", "Avoid hasty arguments during afternoon hours", "Astrological")),
            timingGuidance = DailyTimingGuidance(
                brahmaMuhurtaWindow = "04:35 AM - 05:22 AM",
                brahmaMuhurtaAdvice = null,
                rahukaalWindow = "09:10 AM - 10:45 AM",
                rahukaalAdvice = null,
                abhijitMuhurtaWindow = "11:50 AM - 12:40 PM"
            ),
            traditionalRemedies = listOf(TraditionalRemedy("Remedy", "Practice Vishnu Sahasranama or mindful contemplation", "Jupiter", "Context")),
            varaName = "Somavara",
            tithiName = "Pratipada",
            paksha = "Shukla",
            nakshatraName = "Punarvasu",
            yogaName = "Vishkambha",
            karanaName = "Bava",
            sunriseFormatted = "06:00 AM",
            sunsetFormatted = "06:00 PM",
            astrologicalFactorsSummary = "Summary",
            ethicalDisclaimer = "Disclaimer"
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
            location = BirthLocation(28.6139, 77.2090, "New Delhi", isVerified = true),
            timeZone = ZoneId.of("Asia/Kolkata")
        )
        val rashiChart = Chart(
            title = "Lagna Chart (D1)",
            type = "D1",
            vargaType = VargaType.D1,
            positions = listOf(
                PlanetPosition(
                    planet = "Sun",
                    sanskritName = "Surya",
                    totalLongitude = 35.5,
                    sign = "Taurus",
                    signIndex = 1,
                    degreeInSign = 5.5,
                    house = 11,
                    nakshatra = "Krittika",
                    nakshatraLord = "Sun",
                    nakshatraPada = 3,
                    isRetrograde = false,
                    speed = 1.0,
                    abbreviation = "Su"
                )
            )
        )
        val profile = AstrologyProfile(
            birthData = birthData,
            rashiChart = rashiChart,
            lagna = "Cancer",
            lagnaSignIndex = 3,
            lagnaLongitude = 95.0,
            lagnaDegreeInSign = 5.0,
            lagnaNakshatra = "Pushya",
            lagnaPada = 1,
            moonSign = "Pisces",
            moonSignIndex = 11,
            nakshatra = "Pushya",
            nakshatraPada = 1,
            nakshatraLord = "Saturn",
            planetPositions = emptyList(),
            metadata = CalculationMetadata(
                ephemerisEngine = "Swiss",
                ayanamsaName = "Lahiri",
                ayanamsaDegree = 23.7,
                julianDayUt = 2451545.0,
                calculatedUtcIso = "2026-09-05T00:00:00Z"
            )
        )

        val speechText = AstrologyHindiSpeechFormatter.formatKundliSummary(profile, rashiChart)

        assertTrue(speechText.contains("Rohan Verma"))
        assertTrue(speechText.contains("सूर्य"))
        assertTrue(speechText.contains("वृषभ"))
        assertTrue(speechText.contains("लग्न"))
        assertFalse(speechText.contains("*"))
    }
}
