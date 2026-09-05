package com.example

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.engine.MockAstrologyEngine
import com.example.domain.models.*
import com.example.ui.components.*
import com.example.ui.screens.ChartScreen
import com.example.ui.theme.JyotishTheme
import com.example.ui.viewmodel.AstrologyViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ChartUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createSampleChart(): Chart {
        val positions = listOf(
            PlanetPosition(
                planet = "Sun",
                sign = "Aries",
                signIndex = 0,
                totalLongitude = 14.5,
                degreeInSign = 14.5,
                house = 1,
                isRetrograde = false,
                nakshatra = "Ashwini",
                nakshatraLord = "Ketu",
                nakshatraPada = 1,
                speed = 0.985,
                abbreviation = "Su",
                sanskritName = "Surya"
            ),
            PlanetPosition(
                planet = "Moon",
                sign = "Taurus",
                signIndex = 1,
                totalLongitude = 52.3,
                degreeInSign = 22.3,
                house = 2,
                isRetrograde = false,
                nakshatra = "Rohini",
                nakshatraLord = "Moon",
                nakshatraPada = 2,
                speed = 13.2,
                abbreviation = "Mo",
                sanskritName = "Chandra"
            ),
            PlanetPosition(
                planet = "Mars",
                sign = "Capricorn",
                signIndex = 9,
                totalLongitude = 298.0,
                degreeInSign = 28.0,
                house = 10,
                isRetrograde = false,
                nakshatra = "Dhanishta",
                nakshatraLord = "Mars",
                nakshatraPada = 3,
                speed = 0.5,
                abbreviation = "Ma",
                sanskritName = "Mangala"
            ),
            PlanetPosition(
                planet = "Mercury",
                sign = "Aries",
                signIndex = 0,
                totalLongitude = 18.2,
                degreeInSign = 18.2,
                house = 1,
                isRetrograde = true,
                nakshatra = "Bharani",
                nakshatraLord = "Venus",
                nakshatraPada = 1,
                speed = -0.2,
                abbreviation = "Me",
                sanskritName = "Budha"
            ),
            PlanetPosition(
                planet = "Jupiter",
                sign = "Cancer",
                signIndex = 3,
                totalLongitude = 95.5,
                degreeInSign = 5.5,
                house = 4,
                isRetrograde = false,
                nakshatra = "Pushya",
                nakshatraLord = "Saturn",
                nakshatraPada = 1,
                speed = 0.1,
                abbreviation = "Ju",
                sanskritName = "Guru"
            ),
            PlanetPosition(
                planet = "Venus",
                sign = "Pisces",
                signIndex = 11,
                totalLongitude = 357.0,
                degreeInSign = 27.0,
                house = 12,
                isRetrograde = false,
                nakshatra = "Revati",
                nakshatraLord = "Mercury",
                nakshatraPada = 4,
                speed = 1.2,
                abbreviation = "Ve",
                sanskritName = "Shukra"
            ),
            PlanetPosition(
                planet = "Saturn",
                sign = "Aquarius",
                signIndex = 10,
                totalLongitude = 315.0,
                degreeInSign = 15.0,
                house = 11,
                isRetrograde = true,
                nakshatra = "Shatabhisha",
                nakshatraLord = "Rahu",
                nakshatraPada = 3,
                speed = -0.05,
                abbreviation = "Sa",
                sanskritName = "Shani"
            ),
            PlanetPosition(
                planet = "Rahu",
                sign = "Taurus",
                signIndex = 1,
                totalLongitude = 42.0,
                degreeInSign = 12.0,
                house = 2,
                isRetrograde = true,
                nakshatra = "Rohini",
                nakshatraLord = "Moon",
                nakshatraPada = 1,
                speed = -0.05,
                abbreviation = "Ra",
                sanskritName = "Rahu"
            ),
            PlanetPosition(
                planet = "Ketu",
                sign = "Scorpio",
                signIndex = 7,
                totalLongitude = 222.0,
                degreeInSign = 12.0,
                house = 8,
                isRetrograde = true,
                nakshatra = "Anuradha",
                nakshatraLord = "Saturn",
                nakshatraPada = 3,
                speed = -0.05,
                abbreviation = "Ke",
                sanskritName = "Ketu"
            )
        )
        return Chart(
            type = "D1",
            positions = positions,
            vargaType = VargaType.D1,
            title = "D1 — Rashi (Natal Chart)",
            description = "Root physical existence, vitality, and general life pattern",
            ascendantSign = "Aries",
            ascendantSignIndex = 0,
            ascendantDegreeInSign = 10.5
        )
    }

    @Test
    fun testVargaSelectorRendersAllVargasAndHandlesSelection() {
        val selectedVarga = mutableStateOf(VargaType.D1)

        composeTestRule.setContent {
            JyotishTheme {
                ChartSelectorRow(
                    selectedVarga = selectedVarga.value,
                    onSelectVarga = { selectedVarga.value = it }
                )
            }
        }

        // Verify selector exists
        composeTestRule.onNodeWithTag("chart_selector_row").assertIsDisplayed()

        // Verify D1 chip exists and is selected
        composeTestRule.onNodeWithTag("varga_chip_d1")
            .assertIsDisplayed()
            .assertIsSelected()

        // Verify D9 chip exists, click it, verify selection updates
        composeTestRule.onNodeWithTag("varga_chip_d9")
            .assertIsDisplayed()
            .performClick()

        assertEquals(VargaType.D9, selectedVarga.value)

        // Verify D60 chip is present in ALL_AVAILABLE_VARGAS
        assertTrue(ALL_AVAILABLE_VARGAS.contains(VargaType.D60))
        assertEquals(16, ALL_AVAILABLE_VARGAS.size)
    }

    @Test
    fun testNorthIndianKundliViewDisplaysAscendantAndPlanets() {
        val chart = createSampleChart()
        var clickedPlanet: PlanetPosition? = null

        composeTestRule.setContent {
            JyotishTheme {
                NorthIndianKundliView(
                    chart = chart,
                    onPlanetClick = { clickedPlanet = it }
                )
            }
        }

        // Verify Kundli Chart root node exists
        composeTestRule.onNodeWithTag("north_indian_kundli_chart").assertIsDisplayed()

        // Verify Ascendant indicator in House 1 is rendered (Asc 10°)
        composeTestRule.onNodeWithText("Asc 10°").assertIsDisplayed()

        // Verify Sun badge in House 1
        composeTestRule.onNodeWithTag("planet_badge_sun")
            .assertIsDisplayed()
            .performClick()

        assertNotNull(clickedPlanet)
        assertEquals("Sun", clickedPlanet?.planet)

        // Verify retrograde indicator on Mercury [R]
        composeTestRule.onNodeWithTag("planet_badge_mercury").assertIsDisplayed()
        composeTestRule.onNodeWithText("Me [R] 18°").assertIsDisplayed()
    }

    @Test
    fun testChartInfoPanelDisplaysPlacementsAndSummary() {
        val chart = createSampleChart()
        var clickedPlanet: PlanetPosition? = null

        composeTestRule.setContent {
            JyotishTheme {
                ChartInfoPanel(
                    chart = chart,
                    onPlanetClick = { clickedPlanet = it }
                )
            }
        }

        // Verify Chart Info Panel exists
        composeTestRule.onNodeWithTag("chart_info_panel").assertIsDisplayed()

        // Verify Title and Lagna info
        composeTestRule.onNodeWithText("D1 — Rashi (Natal Chart)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aries").assertIsDisplayed()

        // Verify tap on planet row
        composeTestRule.onNodeWithTag("planet_row_sun")
            .assertIsDisplayed()
            .performClick()

        assertNotNull(clickedPlanet)
        assertEquals("Sun", clickedPlanet?.planet)
    }

    @Test
    fun testPlanetDetailDialogDisplaysAttributesAndDismisses() {
        val planet = PlanetPosition(
            planet = "Jupiter",
            sign = "Cancer",
            signIndex = 3,
            totalLongitude = 95.5,
            degreeInSign = 5.5,
            house = 4,
            isRetrograde = false,
            nakshatra = "Pushya",
            nakshatraLord = "Saturn",
            nakshatraPada = 1,
            speed = 0.083,
            abbreviation = "Ju",
            sanskritName = "Guru"
        )
        var dismissed = false

        composeTestRule.setContent {
            JyotishTheme {
                PlanetDetailDialog(
                    planet = planet,
                    chartTitle = "D1 Rashi",
                    onDismiss = { dismissed = true }
                )
            }
        }

        // Verify dialog is displayed
        composeTestRule.onNodeWithTag("planet_detail_dialog").assertIsDisplayed()

        // Verify Sanskrit name and sign lord
        composeTestRule.onNodeWithText("Jupiter (Guru)").assertIsDisplayed()
        composeTestRule.onNodeWithText("D1 Rashi Placement").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pushya (Pada 1)").assertExists()
        composeTestRule.onNodeWithText("Marga (Direct)").assertIsDisplayed()

        // Test close icon button
        composeTestRule.onNodeWithTag("planet_detail_dialog_close")
            .assertIsDisplayed()
            .performClick()

        assertTrue(dismissed)
    }

    @Test
    fun testChartScreenShowsEmptyStateAndLoadsSampleProfile() {
        val viewModel = AstrologyViewModel(MockAstrologyEngine(), FakeLocationResolver(), FakeLocationRepository(), com.example.domain.FakeProfileRepository())

        composeTestRule.setContent {
            JyotishTheme {
                ChartScreen(viewModel = viewModel)
            }
        }

        // Verify empty state is displayed initially
        composeTestRule.onNodeWithTag("chart_empty_state").assertIsDisplayed()

        // Click "Load Sample Profile"
        composeTestRule.onNodeWithTag("chart_screen_load_sample_button")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.waitForIdle()

        // Verify chart is calculated and displayed
        composeTestRule.onNodeWithTag("chart_screen").assertIsDisplayed()
    }
}
