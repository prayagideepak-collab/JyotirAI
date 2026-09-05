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
    fun testFailsGracefullyWhenNoProfilesConfigured() = runTest {
        val result = dailyPredictionEngine.getDailyPredictionContext()
        assertTrue(result.isFailure)
    }
}
