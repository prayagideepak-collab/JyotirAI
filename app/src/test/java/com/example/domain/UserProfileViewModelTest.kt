package com.example.domain

import com.example.data.engine.MockAstrologyEngine
import com.example.data.engine.SwissEphAstrologyEngine
import com.example.domain.location.LocationRepository
import com.example.domain.location.LocationResolver
import com.example.domain.models.BirthData
import com.example.domain.models.BirthLocation
import com.example.ui.viewmodel.AstrologyUiState
import com.example.ui.viewmodel.AstrologyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var locationResolver: LocationResolver
    private lateinit var astrologyEngine: SwissEphAstrologyEngine
    private lateinit var viewModel: AstrologyViewModel

    private fun createSampleBirthData(
        name: String = "Test User",
        place: String = "New Delhi"
    ): BirthData {
        return BirthData(
            name = name,
            date = LocalDate.of(1995, 8, 15),
            time = LocalTime.of(14, 30, 0),
            location = BirthLocation(
                latitude = 28.6139,
                longitude = 77.2090,
                placeName = place,
                isVerified = true,
                source = "manual"
            ),
            timeZone = ZoneId.of("Asia/Kolkata")
        )
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepository()
        locationRepository = object : LocationRepository {
            private var loc: BirthLocation? = null
            override suspend fun saveVerifiedLocation(location: BirthLocation) { loc = location }
            override suspend fun getVerifiedLocation(): BirthLocation? = loc
        }
        locationResolver = object : LocationResolver {
            override suspend fun resolveLocation(query: String): Result<List<BirthLocation>> = Result.success(emptyList())
        }
        astrologyEngine = SwissEphAstrologyEngine()
        viewModel = AstrologyViewModel(astrologyEngine, locationResolver, locationRepository, profileRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialEmptyState() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertTrue(viewModel.savedProfiles.value.isEmpty())
        assertNull(viewModel.activeProfileId.value)
        assertNull(viewModel.defaultProfileId.value)
        assertTrue(viewModel.uiState.value is AstrologyUiState.Empty)
    }

    @Test
    fun testSaveProfilePopulatesStateAndCalculatesChart() = runTest(testDispatcher) {
        advanceUntilIdle()
        val data = createSampleBirthData(name = "Aarav Sharma")

        var successCalled = false
        viewModel.saveOrUpdateProfile(data) { result ->
            if (result.isSuccess) successCalled = true
        }
        advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals(1, viewModel.savedProfiles.value.size)
        val activeId = viewModel.activeProfileId.value
        assertNotNull(activeId)
        assertEquals(activeId, viewModel.defaultProfileId.value)
        assertEquals("Aarav Sharma", viewModel.activeUserProfile.value?.name)
        assertTrue(viewModel.uiState.value is AstrologyUiState.Success)
    }

    @Test
    fun testSwitchActiveProfileCalculatesNewProfile() = runTest(testDispatcher) {
        advanceUntilIdle()
        val p1 = createSampleBirthData(name = "Person 1")
        val p2 = createSampleBirthData(name = "Person 2")

        viewModel.saveOrUpdateProfile(p1)
        advanceUntilIdle()
        val p1Id = viewModel.activeProfileId.value!!

        viewModel.saveOrUpdateProfile(p2)
        advanceUntilIdle()
        val p2Id = viewModel.activeProfileId.value!!

        assertEquals(2, viewModel.savedProfiles.value.size)
        assertEquals(p1Id, viewModel.defaultProfileId.value) // p1 was first, so default
        assertEquals(p2Id, viewModel.activeProfileId.value) // p2 just saved, so active

        // Switch back to p1
        viewModel.switchActiveProfile(p1Id)
        advanceUntilIdle()

        assertEquals(p1Id, viewModel.activeProfileId.value)
        assertEquals("Person 1", viewModel.activeUserProfile.value?.name)
        assertEquals(p1Id, viewModel.defaultProfileId.value) // default still p1
    }

    @Test
    fun testSetDefaultProfileDoesNotChangeActiveProfile() = runTest(testDispatcher) {
        advanceUntilIdle()
        val p1 = createSampleBirthData(name = "Person 1")
        val p2 = createSampleBirthData(name = "Person 2")

        viewModel.saveOrUpdateProfile(p1)
        advanceUntilIdle()
        val p1Id = viewModel.activeProfileId.value!!

        viewModel.saveOrUpdateProfile(p2)
        advanceUntilIdle()
        val p2Id = viewModel.activeProfileId.value!!

        // Active is p2, Default is p1
        assertEquals(p2Id, viewModel.activeProfileId.value)
        assertEquals(p1Id, viewModel.defaultProfileId.value)

        // Change default to p2
        viewModel.setDefaultProfile(p2Id)
        advanceUntilIdle()

        assertEquals(p2Id, viewModel.defaultProfileId.value)
        assertEquals(p2Id, viewModel.activeProfileId.value)
        assertEquals("Person 2", viewModel.getDefaultProfile()?.name)
    }

    @Test
    fun testDeleteProfile() = runTest(testDispatcher) {
        advanceUntilIdle()
        val p1 = createSampleBirthData(name = "Person 1")
        val p2 = createSampleBirthData(name = "Person 2")

        viewModel.saveOrUpdateProfile(p1)
        advanceUntilIdle()
        val p1Id = viewModel.activeProfileId.value!!

        viewModel.saveOrUpdateProfile(p2)
        advanceUntilIdle()
        val p2Id = viewModel.activeProfileId.value!!

        // Delete active profile (p2)
        viewModel.deleteProfile(p2Id)
        advanceUntilIdle()

        assertEquals(1, viewModel.savedProfiles.value.size)
        assertEquals(p1Id, viewModel.activeProfileId.value)
        assertEquals(p1Id, viewModel.defaultProfileId.value)

        // Delete remaining profile (p1)
        viewModel.deleteProfile(p1Id)
        advanceUntilIdle()

        assertTrue(viewModel.savedProfiles.value.isEmpty())
        assertNull(viewModel.activeProfileId.value)
        assertNull(viewModel.defaultProfileId.value)
        assertTrue(viewModel.uiState.value is AstrologyUiState.Empty)
    }

    @Test
    fun testThreeProfilesIndependentSwitchingAndPredictionSubject() = runTest(testDispatcher) {
        advanceUntilIdle()
        val p1 = createSampleBirthData(name = "Person 1")
        val p2 = createSampleBirthData(name = "Person 2")
        val p3 = createSampleBirthData(name = "Person 3")

        viewModel.saveOrUpdateProfile(p1)
        advanceUntilIdle()
        val p1Id = viewModel.savedProfiles.value.first { it.name == "Person 1" }.id

        viewModel.saveOrUpdateProfile(p2)
        advanceUntilIdle()
        val p2Id = viewModel.savedProfiles.value.first { it.name == "Person 2" }.id

        viewModel.saveOrUpdateProfile(p3)
        advanceUntilIdle()
        val p3Id = viewModel.savedProfiles.value.first { it.name == "Person 3" }.id

        assertEquals(3, viewModel.savedProfiles.value.size)

        // Set Profile 2 as Default for Daily Predictions
        viewModel.setDefaultProfile(p2Id)
        advanceUntilIdle()

        assertEquals(p2Id, viewModel.defaultProfileId.value)
        assertEquals("Person 2", viewModel.getDefaultProfileForDailyPrediction()?.name)

        // Open/view Profile 3
        viewModel.switchActiveProfile(p3Id)
        advanceUntilIdle()

        assertEquals(p3Id, viewModel.activeProfileId.value)
        assertEquals("Person 3", viewModel.activeUserProfile.value?.name)
        // Daily Horoscope subject MUST still be Profile 2
        assertEquals(p2Id, viewModel.defaultProfileId.value)
        assertEquals("Person 2", viewModel.getDefaultProfileForDailyPrediction()?.name)

        // Open/view Profile 1
        viewModel.switchActiveProfile(p1Id)
        advanceUntilIdle()

        assertEquals(p1Id, viewModel.activeProfileId.value)
        assertEquals("Person 1", viewModel.activeUserProfile.value?.name)
        // Daily Horoscope subject MUST still be Profile 2
        assertEquals(p2Id, viewModel.defaultProfileId.value)
        assertEquals("Person 2", viewModel.getDefaultProfileForDailyPrediction()?.name)
    }
}
