package com.example.domain

import com.example.data.engine.SwissEphAstrologyEngine
import com.example.domain.models.*
import com.example.domain.prediction.*
import com.example.domain.speech.AstrologyHindiSpeechFormatter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PeriodicPredictionEngineTest {

    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var astrologyEngine: SwissEphAstrologyEngine
    private lateinit var scheduleEngine: PredictionScheduleEngine

    private fun createSampleBirthData(name: String, year: Int = 1995, month: Int = 8, day: Int = 15): BirthData {
        return BirthData(
            name = name,
            date = LocalDate.of(year, month, day),
            time = LocalTime.of(14, 30, 0),
            location = BirthLocation(
                latitude = 28.6139,
                longitude = 77.2090,
                placeName = "New Delhi, India",
                altitudeMeters = 216.0,
                timeZoneId = "Asia/Kolkata",
                isVerified = true,
                source = "test"
            ),
            timeZone = ZoneId.of("Asia/Kolkata")
        )
    }

    @Before
    fun setup() {
        profileRepository = FakeProfileRepository()
        astrologyEngine = SwissEphAstrologyEngine()
        scheduleEngine = PredictionScheduleEngineImpl.create(profileRepository, astrologyEngine)
    }

    @Test
    fun testDailyPredictionGenerationAndAllTopics() = runTest {
        val birthData = createSampleBirthData("Aarav Sharma")
        val profile = astrologyEngine.calculateProfile(birthData).getOrThrow()

        val targetDate = LocalDate.of(2026, 9, 6)
        val result = scheduleEngine.getDailyPrediction(profile, targetDate)

        assertTrue(result.isSuccess)
        val prediction = result.getOrThrow()

        assertEquals("Aarav Sharma", prediction.profileName)
        assertEquals(PredictionPeriodType.DAILY, prediction.predictionType)
        assertEquals(targetDate, prediction.timeContext.targetDate)
        assertEquals(PeriodicPredictionState.SUCCESS, prediction.state)

        // Verifies all 7 classical life topics are present
        assertEquals(7, prediction.topicPredictions.size)
        LifeTopic.entries.forEach { topic ->
            val topicPred = prediction.topicPredictions[topic]
            assertNotNull("Topic $topic should be present", topicPred)
            assertEquals(topic, topicPred!!.topic)
            assertNotNull(topicPred.supportLevel)
            assertNotNull(topicPred.trendType)
            assertTrue(topicPred.synthesis.isNotBlank())
            assertNotNull(topicPred.timingGuidance)
        }

        // Verifies Dasha and Transit evidence
        assertTrue(prediction.dashaEvidence.mahadashaLord.isNotBlank())
        assertTrue(prediction.dashaEvidence.antardashaLord.isNotBlank())
        assertNotNull(prediction.transitEvidence.summary)
        assertTrue(prediction.limitations.isNotBlank())
    }

    @Test
    fun testMonthlyPredictionGenerationAndDashaTransitionDetection() = runTest {
        val birthData = createSampleBirthData("Priya Verma")
        val profile = astrologyEngine.calculateProfile(birthData).getOrThrow()

        val year = 2026
        val month = 9
        val result = scheduleEngine.getMonthlyPrediction(profile, year, month)

        assertTrue(result.isSuccess)
        val prediction = result.getOrThrow()

        assertEquals(PredictionPeriodType.MONTHLY, prediction.predictionType)
        assertEquals(year, prediction.timeContext.targetYear)
        assertEquals(month, prediction.timeContext.targetMonth)
        assertEquals(LocalDate.of(2026, 9, 1), prediction.timeContext.startDate)
        assertEquals(LocalDate.of(2026, 9, 30), prediction.timeContext.endDate)

        assertEquals(7, prediction.topicPredictions.size)
        assertTrue(prediction.overallSummary.isNotBlank())
        assertTrue(prediction.transitEvidence.summary.contains("Monthly Gochar"))
    }

    @Test
    fun testYearlyPredictionGenerationAndLongTermTransits() = runTest {
        val birthData = createSampleBirthData("Rohan Gupta")
        val profile = astrologyEngine.calculateProfile(birthData).getOrThrow()

        val year = 2026
        val result = scheduleEngine.getYearlyPrediction(profile, year)

        assertTrue(result.isSuccess)
        val prediction = result.getOrThrow()

        assertEquals(PredictionPeriodType.YEARLY, prediction.predictionType)
        assertEquals(year, prediction.timeContext.targetYear)
        assertEquals(LocalDate.of(2026, 1, 1), prediction.timeContext.startDate)
        assertEquals(LocalDate.of(2026, 12, 31), prediction.timeContext.endDate)

        assertEquals(7, prediction.topicPredictions.size)
        assertTrue(prediction.overallSummary.isNotBlank())
        assertTrue(prediction.transitEvidence.summary.contains("Annual Gochar"))
    }

    @Test
    fun testTimeContextResolverLeapYearsAndBoundaries() {
        val birthData = createSampleBirthData("Test User")

        // Leap Year February 2024
        val leapFeb = TimeContextResolver.resolve(
            periodType = PredictionPeriodType.MONTHLY,
            targetDate = LocalDate.of(2024, 2, 10),
            birthData = birthData,
            dashaTimeline = null
        )
        assertEquals(LocalDate.of(2024, 2, 1), leapFeb.startDate)
        assertEquals(LocalDate.of(2024, 2, 29), leapFeb.endDate)

        // Non-Leap Year February 2026
        val nonLeapFeb = TimeContextResolver.resolve(
            periodType = PredictionPeriodType.MONTHLY,
            targetDate = LocalDate.of(2026, 2, 10),
            birthData = birthData,
            dashaTimeline = null
        )
        assertEquals(LocalDate.of(2026, 2, 1), nonLeapFeb.startDate)
        assertEquals(LocalDate.of(2026, 2, 28), nonLeapFeb.endDate)

        // Yearly context
        val yearly = TimeContextResolver.resolve(
            periodType = PredictionPeriodType.YEARLY,
            targetDate = LocalDate.of(2026, 6, 1),
            birthData = birthData,
            dashaTimeline = null
        )
        assertEquals(LocalDate.of(2026, 1, 1), yearly.startDate)
        assertEquals(LocalDate.of(2026, 12, 31), yearly.endDate)
    }

    @Test
    fun testCacheCoordinatorProfileIsolation() = runTest {
        val birthData1 = createSampleBirthData("Person A")
        val birthData2 = createSampleBirthData("Person B")

        val profile1 = astrologyEngine.calculateProfile(birthData1).getOrThrow()
        val profile2 = astrologyEngine.calculateProfile(birthData2).getOrThrow()

        val date = LocalDate.of(2026, 9, 6)

        // Compute for Person A
        val resA = scheduleEngine.getDailyPrediction(profile1, date).getOrThrow()
        assertEquals("Person A", resA.profileName)

        // Compute for Person B
        val resB = scheduleEngine.getDailyPrediction(profile2, date).getOrThrow()
        assertEquals("Person B", resB.profileName)

        // Ensure clear cache for A does not affect B
        scheduleEngine.clearCache("Person A")

        val resB2 = scheduleEngine.getDailyPrediction(profile2, date).getOrThrow()
        assertEquals("Person B", resB2.profileName)
    }

    @Test
    fun testDeterministicOutputAcrossMultipleRuns() = runTest {
        val birthData = createSampleBirthData("Deterministic Check")
        val profile = astrologyEngine.calculateProfile(birthData).getOrThrow()

        val date = LocalDate.of(2026, 9, 6)

        val run1 = scheduleEngine.getDailyPrediction(profile, date).getOrThrow()
        scheduleEngine.clearCache() // Clear memory cache to force re-calculation
        val run2 = scheduleEngine.getDailyPrediction(profile, date).getOrThrow()

        assertEquals(run1.overallSupportLevel, run2.overallSupportLevel)
        assertEquals(run1.overallTrend, run2.overallTrend)
        assertEquals(run1.overallSummary, run2.overallSummary)
        assertEquals(run1.dashaEvidence.mahadashaLord, run2.dashaEvidence.mahadashaLord)
        assertEquals(run1.dashaEvidence.antardashaLord, run2.dashaEvidence.antardashaLord)

        LifeTopic.entries.forEach { topic ->
            assertEquals(run1.topicPredictions[topic]?.supportLevel, run2.topicPredictions[topic]?.supportLevel)
            assertEquals(run1.topicPredictions[topic]?.synthesis, run2.topicPredictions[topic]?.synthesis)
        }
    }

    @Test
    fun testResultValidatorGracefulHandlingOfInvalidInputs() = runTest {
        val emptyBirthData = BirthData(
            name = "",
            date = LocalDate.of(1995, 8, 15),
            time = LocalTime.of(14, 30, 0),
            location = BirthLocation(0.0, 0.0, "Greenwich", timeZoneId = "UTC", isVerified = true, source = "test"),
            timeZone = ZoneId.of("UTC")
        )
        val invalidProfile = AstrologyProfile(
            birthData = emptyBirthData,
            rashiChart = Chart("D1", emptyList()),
            lagna = "",
            lagnaSignIndex = 0,
            lagnaLongitude = 0.0,
            lagnaDegreeInSign = 0.0,
            lagnaNakshatra = "",
            lagnaPada = 1,
            moonSign = "",
            moonSignIndex = 0,
            nakshatra = "",
            nakshatraPada = 1,
            nakshatraLord = "",
            planetPositions = emptyList(),
            metadata = CalculationMetadata(ayanamsaDegree = 24.0, julianDayUt = 2450000.0, calculatedUtcIso = "2026-09-06T00:00:00Z")
        )

        val validation = ResultValidator.validateProfile(invalidProfile)
        assertFalse(validation.isValid)

        val result = scheduleEngine.getDailyPrediction(invalidProfile, LocalDate.of(2026, 9, 6))
        assertTrue(result.isFailure)
    }

    @Test
    fun testHindiSpeechFormatterPeriodicPrediction() = runTest {
        val birthData = createSampleBirthData("Divya Singh")
        val profile = astrologyEngine.calculateProfile(birthData).getOrThrow()

        val dailyResult = scheduleEngine.getDailyPrediction(profile, LocalDate.of(2026, 9, 6)).getOrThrow()
        val speechDaily = AstrologyHindiSpeechFormatter.formatPeriodicPrediction(dailyResult)

        assertTrue(speechDaily.contains("ज्योतिर् एआई दैनिक भविष्यफल"))
        assertTrue(speechDaily.contains("Divya Singh"))
        assertFalse(speechDaily.contains("#"))
        assertFalse(speechDaily.contains("*"))

        val monthlyResult = scheduleEngine.getMonthlyPrediction(profile, 2026, 9).getOrThrow()
        val speechMonthly = AstrologyHindiSpeechFormatter.formatPeriodicPrediction(monthlyResult)
        assertTrue(speechMonthly.contains("मासिक भविष्यफल"))
        assertTrue(speechMonthly.contains("सितंबर 2026"))

        val yearlyResult = scheduleEngine.getYearlyPrediction(profile, 2026).getOrThrow()
        val speechYearly = AstrologyHindiSpeechFormatter.formatPeriodicPrediction(yearlyResult)
        assertTrue(speechYearly.contains("वार्षिक भविष्यफल"))
        assertTrue(speechYearly.contains("वर्ष 2026"))
    }
}
