package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.engine.SwissEphAstrologyEngine
import com.example.domain.FakeProfileRepository
import com.example.domain.location.LocationRepository
import com.example.domain.location.LocationResolver
import com.example.domain.models.BirthData
import com.example.domain.models.BirthLocation
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.JyotishTheme
import com.example.ui.viewmodel.AstrologyViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ProfileUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var locationResolver: LocationResolver
    private lateinit var astrologyEngine: SwissEphAstrologyEngine
    private lateinit var viewModel: AstrologyViewModel

    @Before
    fun setup() {
        profileRepository = FakeProfileRepository()
        locationRepository = object : LocationRepository {
            private var loc: BirthLocation? = null
            override suspend fun saveVerifiedLocation(location: BirthLocation) { loc = location }
            override suspend fun getVerifiedLocation(): BirthLocation? = loc
        }
        locationResolver = object : LocationResolver {
            override suspend fun resolveLocation(query: String): Result<List<BirthLocation>> = Result.success(
                listOf(
                    BirthLocation(
                        latitude = 28.6139,
                        longitude = 77.2090,
                        placeName = "New Delhi, India",
                        isVerified = true,
                        source = "search"
                    )
                )
            )
        }
        astrologyEngine = SwissEphAstrologyEngine()
        viewModel = AstrologyViewModel(astrologyEngine, locationResolver, locationRepository, profileRepository)
    }

    @Test
    fun testEmptyStateDisplaysCorrectly() {
        composeTestRule.setContent {
            JyotishTheme {
                HomeScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("empty_birth_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("enter_birth_details_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("load_reference_profile_button").assertIsDisplayed()
    }

    @Test
    fun testLoadSampleProfilePopulatesProfileAndSlotBar() {
        composeTestRule.setContent {
            JyotishTheme {
                HomeScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("load_reference_profile_button")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Profile view and profile slots bar should now be displayed
        composeTestRule.onNodeWithTag("profile_slots_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("astrology_profile_view").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Aarav Sharma (Reference)").assertCountEquals(2)
    }

    @Test
    fun testProfileSwitchingAndManagement() {
        val p1 = BirthData(
            name = "Profile One",
            date = LocalDate.of(1990, 1, 1),
            time = LocalTime.of(10, 0, 0),
            location = BirthLocation(28.61, 77.20, "New Delhi", isVerified = true),
            timeZone = ZoneId.of("Asia/Kolkata")
        )

        composeTestRule.setContent {
            JyotishTheme {
                HomeScreen(viewModel = viewModel)
            }
        }

        composeTestRule.waitForIdle()

        // Load reference profile
        composeTestRule.onNodeWithTag("load_reference_profile_button")
            .performClick()
        composeTestRule.waitForIdle()

        // Save another profile
        viewModel.saveOrUpdateProfile(p1)
        composeTestRule.waitForIdle()

        // Verify both are present in slots bar and viewModel state
        composeTestRule.onNodeWithTag("profile_slots_bar").assertIsDisplayed()
        assertEquals(2, viewModel.savedProfiles.value.size)
    }
}
