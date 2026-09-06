package com.example.domain.muhurta

import com.example.domain.models.*
import com.example.domain.panchang.PanchangEngineImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class MuhurtaEngineTest {

    private lateinit var muhurtaEngine: MuhurtaEngine
    private lateinit var sampleLocation: BirthLocation
    private lateinit var sampleProfile: UserProfile

    @Before
    fun setUp() {
        muhurtaEngine = MuhurtaEngineImpl(
            panchangEngine = PanchangEngineImpl()
        )

        sampleLocation = BirthLocation(
            latitude = 28.6139,
            longitude = 77.2090,
            placeName = "New Delhi, India",
            isVerified = true,
            source = "test",
            timeZoneId = "Asia/Kolkata"
        )

        val birthData = BirthData(
            name = "Aarav Test",
            date = LocalDate.of(1995, 8, 15),
            time = LocalTime.of(14, 30),
            location = sampleLocation,
            timeZone = ZoneId.of("Asia/Kolkata")
        )

        sampleProfile = UserProfile(
            id = "profile_1",
            birthData = birthData
        )
    }

    @Test
    fun testRequestValidator_validRequestPasses() {
        val req = MuhurtaRequest(
            activityType = MuhurtaActivityType.GENERAL_AUSPICIOUS,
            startDate = LocalDate.of(2026, 9, 6),
            endDate = LocalDate.of(2026, 9, 8),
            location = sampleLocation
        )
        // Should not throw
        MuhurtaRequestValidator.validate(req)
    }

    @Test(expected = AppError.InvalidBirthData::class)
    fun testRequestValidator_invalidDateRangeFails() {
        val req = MuhurtaRequest(
            activityType = MuhurtaActivityType.TRAVEL,
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 5), // End before start
            location = sampleLocation
        )
        MuhurtaRequestValidator.validate(req)
    }

    @Test(expected = AppError.InvalidBirthData::class)
    fun testRequestValidator_excessiveDateRangeFails() {
        val req = MuhurtaRequest(
            activityType = MuhurtaActivityType.TRAVEL,
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 11, 1), // > 31 days
            location = sampleLocation
        )
        MuhurtaRequestValidator.validate(req)
    }

    @Test
    fun testDateRangeResolver() {
        val start = LocalDate.of(2026, 9, 1)
        val end = LocalDate.of(2026, 9, 3)
        val dates = DateRangeResolver.resolveDates(start, end)
        assertEquals(3, dates.size)
        assertEquals(start, dates[0])
        assertEquals(LocalDate.of(2026, 9, 2), dates[1])
        assertEquals(end, dates[2])
    }

    @Test
    fun testPersonalBalaCalculator_computesTaraAndChandraBala() {
        val context = PersonalBalaCalculator.calculate(
            profile = sampleProfile,
            transitMoonNakshatra = Nakshatra.ROHINI,
            transitMoonRashi = Rashi.TAURUS
        )

        assertNotNull(context)
        assertNotNull(context.janmaNakshatra)
        assertNotNull(context.natalMoonRashi)
        assertTrue(context.taraIndex in 1..9)
        assertTrue(context.chandraBalaHouse in 1..12)
        assertNotNull(context.balaSummary)
    }

    @Test
    fun testPersonalBalaCalculator_detectsChandrashtama() {
        // First get natal rashi
        val initial = PersonalBalaCalculator.calculate(
            profile = sampleProfile,
            transitMoonNakshatra = Nakshatra.ASHWINI,
            transitMoonRashi = Rashi.ARIES
        )
        val natalRashi = initial.natalMoonRashi
        val eighthRashi = Rashi.entries[(natalRashi.index + 7) % 12]

        val context = PersonalBalaCalculator.calculate(
            profile = sampleProfile,
            transitMoonNakshatra = Nakshatra.ASHWINI,
            transitMoonRashi = eighthRashi
        )

        assertEquals(8, context.chandraBalaHouse)
        assertTrue(context.isChandrashtama)
    }

    @Test
    fun testCalculateDailyMuhurta_generalAuspicious() = runBlocking {
        val result = muhurtaEngine.calculateDailyMuhurta(
            activityType = MuhurtaActivityType.GENERAL_AUSPICIOUS,
            date = LocalDate.of(2026, 9, 6),
            location = sampleLocation
        )

        assertTrue(result.isSuccess)
        val muhurta = result.getOrThrow()
        assertEquals(MuhurtaActivityType.GENERAL_AUSPICIOUS, muhurta.activityType)
        assertEquals(MuhurtaResultType.GENERAL_MUHURTA, muhurta.resultType)
        assertTrue(muhurta.candidateWindows.isNotEmpty())
        assertNotNull(muhurta.bestWindow)

        // Verify that Brahma Muhurta and Abhijit Muhurta are present among candidates
        val hasBrahma = muhurta.candidateWindows.any { it.name.contains("Brahma", ignoreCase = true) }
        val hasAbhijit = muhurta.candidateWindows.any { it.name.contains("Abhijit", ignoreCase = true) }
        assertTrue("Brahma Muhurta should be generated", hasBrahma)
        assertTrue("Abhijit Muhurta should be generated", hasAbhijit)
    }

    @Test
    fun testCalculateMuhurta_personalizedMultiDay() = runBlocking {
        val req = MuhurtaRequest(
            activityType = MuhurtaActivityType.BUSINESS,
            startDate = LocalDate.of(2026, 9, 6),
            endDate = LocalDate.of(2026, 9, 8), // 3 days
            location = sampleLocation,
            profile = sampleProfile
        )

        val result = muhurtaEngine.calculateMuhurta(req)
        assertTrue(result.isSuccess)
        val muhurta = result.getOrThrow()
        assertEquals(MuhurtaResultType.PERSONALIZED_MUHURTA, muhurta.resultType)
        assertTrue(muhurta.candidateWindows.size >= 10)

        // Each window should have personal bala context attached
        val personalizedWindows = muhurta.candidateWindows.filter { it.personalBalaContext != null }
        assertTrue(personalizedWindows.isNotEmpty())
    }

    @Test
    fun testActivityContextResolver_allActivitiesConfigured() {
        MuhurtaActivityType.entries.forEach { activity ->
            val profile = ActivityContextResolver.getRuleProfile(activity)
            assertNotNull(profile)
            assertTrue(profile.favorableTithis.isNotEmpty())
            assertTrue(profile.favorableNakshatras.isNotEmpty())
            assertTrue(profile.favorableVaras.isNotEmpty())
            assertTrue(profile.classicalNotes.isNotBlank())
        }
    }

    @Test
    fun testMuhurtaCache_preventsRedundantCalculations() = runBlocking {
        val cache = MuhurtaCache()
        val engine = MuhurtaEngineImpl(PanchangEngineImpl(), cache)

        val req = MuhurtaRequest(
            activityType = MuhurtaActivityType.EDUCATION,
            startDate = LocalDate.of(2026, 9, 6),
            location = sampleLocation
        )

        val res1 = engine.calculateMuhurta(req).getOrThrow()
        val res2 = engine.calculateMuhurta(req).getOrThrow()

        // Same ID retrieved from cache
        assertEquals(res1.id, res2.id)
    }
}
