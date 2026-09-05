package com.example.domain

import com.example.data.engine.SwissEphAstrologyEngine
import com.example.domain.models.BirthData
import com.example.domain.models.BirthLocation
import com.example.domain.models.UserProfile
import com.example.domain.prediction.DailyPredictionEngineImpl
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
class DailyPredictionEngineTest {

    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var astrologyEngine: SwissEphAstrologyEngine
    private lateinit var dailyPredictionEngine: DailyPredictionEngineImpl

    private fun createSampleProfile(id: String, name: String, place: String, lat: Double, lon: Double): UserProfile {
        return UserProfile(
            id = id,
            birthData = BirthData(
                name = name,
                date = LocalDate.of(1992, 6, 15),
                time = LocalTime.of(10, 30, 0),
                location = BirthLocation(
                    latitude = lat,
                    longitude = lon,
                    placeName = place,
                    altitudeMeters = 200.0,
                    timeZoneId = "Asia/Kolkata",
                    isVerified = true,
                    source = "verified_test"
                ),
                timeZone = ZoneId.of("Asia/Kolkata")
            )
        )
    }

    @Before
    fun setup() {
        profileRepository = FakeProfileRepository()
        astrologyEngine = SwissEphAstrologyEngine()
        dailyPredictionEngine = DailyPredictionEngineImpl(profileRepository, astrologyEngine)
    }

    @Test
    fun testDailyPredictionContextRetrievesDefaultProfileNotActiveProfile() = runTest {
        val p1 = createSampleProfile("p1", "Profile One (Default)", "New Delhi", 28.6139, 77.2090)
        val p2 = createSampleProfile("p2", "Profile Two (Active)", "Mumbai", 19.0760, 72.8777)

        profileRepository.saveProfile(p1)
        profileRepository.saveProfile(p2)

        // Set p1 as default, but p2 as active
        profileRepository.setDefaultProfileId("p1")
        profileRepository.setActiveProfileId("p2")

        assertEquals("p1", profileRepository.getDefaultProfileId())
        assertEquals("p2", profileRepository.getActiveProfileId())

        val result = dailyPredictionEngine.getDailyPredictionContext(LocalDate.of(2026, 9, 5))
        assertTrue(result.isSuccess)

        val context = result.getOrThrow()
        // Must use DEFAULT profile p1, NOT active profile p2
        assertEquals("p1", context.defaultProfile.id)
        assertEquals("Profile One (Default)", context.defaultProfile.name)
        assertEquals(28.6139, context.defaultBirthData.location.latitude, 0.001)

        // Verifies calculated Panchange, Dasha, Transits
        assertNotNull(context.panchang)
        assertNotNull(context.transitSnapshot)
        assertNotNull(context.dashaTimeline)
        assertNotNull(context.brahmaMuhurta)
    }

    @Test
    fun testSwitchingDefaultProfileUpdatesDailyPredictionContext() = runTest {
        val p1 = createSampleProfile("p1", "Person A", "Delhi", 28.6139, 77.2090)
        val p2 = createSampleProfile("p2", "Person B", "Bengaluru", 12.9716, 77.5946)

        profileRepository.saveProfile(p1)
        profileRepository.saveProfile(p2)

        // Initially p1 is default
        assertEquals("p1", dailyPredictionEngine.getDailyPredictionContext().getOrThrow().defaultProfile.id)

        // Change default to p2
        profileRepository.setDefaultProfileId("p2")

        // Prediction context must now use p2
        val updatedContext = dailyPredictionEngine.getDailyPredictionContext().getOrThrow()
        assertEquals("p2", updatedContext.defaultProfile.id)
        assertEquals("Person B", updatedContext.defaultProfile.name)
        assertEquals(12.9716, updatedContext.defaultBirthData.location.latitude, 0.001)
    }

    @Test
    fun testBrahmaMuhurtaWindowCalculatedForDefaultProfileLocation() = runTest {
        val p = createSampleProfile("p1", "Person Delhi", "New Delhi", 28.6139, 77.2090)
        profileRepository.saveProfile(p)
        profileRepository.setDefaultProfileId("p1")

        val window = dailyPredictionEngine.getBrahmaMuhurtaScheduleWindow(LocalDate.of(2026, 9, 5)).getOrThrow()
        assertNotNull(window)
        assertEquals("Brahma Muhurta", window?.name)
        assertTrue(window!!.start.isBefore(window.end))
    }

    @Test
    fun testPersonalisedDailyRashifalGenerationAndDeterminism() = runTest {
        val p1 = createSampleProfile("p1", "Default User", "New Delhi", 28.6139, 77.2090)
        val p2 = createSampleProfile("p2", "Active User", "Kolkata", 22.5726, 88.3639)

        profileRepository.saveProfile(p1)
        profileRepository.saveProfile(p2)
        profileRepository.setDefaultProfileId("p1")
        profileRepository.setActiveProfileId("p2")

        val targetDate = LocalDate.of(2026, 9, 5)

        val result1 = dailyPredictionEngine.generatePersonalisedRashifal(targetDate)
        assertTrue(result1.isSuccess)

        val rashifal1 = result1.getOrThrow()
        assertEquals("p1", rashifal1.defaultProfileId)
        assertEquals("Default User", rashifal1.profileName)
        assertEquals("New Delhi", rashifal1.birthLocationName)
        assertTrue(rashifal1.energyScore in 0..100)
        assertTrue(rashifal1.keyInfluences.isNotEmpty())
        assertTrue(rashifal1.priorities.isNotEmpty())
        assertTrue(rashifal1.cautions.isNotEmpty())
        assertTrue(rashifal1.traditionalRemedies.isNotEmpty())

        // Determinism test: Generating again for the same date must produce the exact same outcome
        val result2 = dailyPredictionEngine.generatePersonalisedRashifal(targetDate)
        assertTrue(result2.isSuccess)
        val rashifal2 = result2.getOrThrow()

        assertEquals(rashifal1.energyScore, rashifal2.energyScore)
        assertEquals(rashifal1.dailyTheme, rashifal2.dailyTheme)
        assertEquals(rashifal1.taraBala.taraName, rashifal2.taraBala.taraName)
        assertEquals(rashifal1.lagna, rashifal2.lagna)
        assertEquals(rashifal1.moonSign, rashifal2.moonSign)
        assertEquals(rashifal1.currentMahadashaLord, rashifal2.currentMahadashaLord)
    }

    @Test
    fun testChangingActiveProfileDoesNotChangeRashifalSubject() = runTest {
        val p1 = createSampleProfile("p1", "Alice (Default)", "Chennai", 13.0827, 80.2707)
        val p2 = createSampleProfile("p2", "Bob (Active)", "Pune", 18.5204, 73.8567)

        profileRepository.saveProfile(p1)
        profileRepository.saveProfile(p2)
        profileRepository.setDefaultProfileId("p1")
        profileRepository.setActiveProfileId("p1")

        val rBefore = dailyPredictionEngine.generatePersonalisedRashifal().getOrThrow()
        assertEquals("p1", rBefore.defaultProfileId)
        assertEquals("Alice (Default)", rBefore.profileName)

        // Switch active profile to Bob
        profileRepository.setActiveProfileId("p2")

        val rAfter = dailyPredictionEngine.generatePersonalisedRashifal().getOrThrow()
        assertEquals("p1", rAfter.defaultProfileId)
        assertEquals("Alice (Default)", rAfter.profileName)
    }

    @Test
    fun testFailsGracefullyWhenNoProfilesConfigured() = runTest {
        val result = dailyPredictionEngine.getDailyPredictionContext()
        assertTrue(result.isFailure)
    }
}
