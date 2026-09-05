package com.example.domain

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.engine.SwissEphAstrologyEngine
import com.example.domain.location.LocationRepositoryImpl
import com.example.domain.models.BirthData
import com.example.domain.models.BirthLocation
import com.example.ui.components.AstrologyProfileView
import com.example.ui.components.BirthDataEntryDialog
import com.example.ui.theme.JyotishTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class LocationAuthorityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // 1. LOCATION VALIDATION
    @Test
    fun testValidLocationValidation() {
        val location = BirthLocation(
            latitude = 28.6139391,
            longitude = 77.2090212,
            placeName = "New Delhi",
            altitudeMeters = 216.0,
            timeZoneId = "Asia/Kolkata",
            isVerified = true,
            source = "geocoder"
        )
        assertEquals(28.6139391, location.latitude, 0.0)
        assertEquals(77.2090212, location.longitude, 0.0)
        assertEquals("New Delhi", location.placeName)
        assertEquals(216.0, location.altitudeMeters!!, 0.0)
        assertEquals("Asia/Kolkata", location.timeZoneId)
        assertTrue(location.isVerified)
        assertEquals("geocoder", location.source)
    }

    @Test
    fun testInvalidLatitudeRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(90.0001, 0.0, "Invalid Lat")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(-90.0001, 0.0, "Invalid Lat")
        }
    }

    @Test
    fun testInvalidLongitudeRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(0.0, 180.0001, "Invalid Lon")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(0.0, -180.0001, "Invalid Lon")
        }
    }

    @Test
    fun testNonFiniteValuesRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(Double.NaN, 0.0, "NaN Lat")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(0.0, Double.POSITIVE_INFINITY, "Inf Lon")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(0.0, 0.0, "Inf Alt", altitudeMeters = Double.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun testBlankPlaceNameRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(10.0, 20.0, "")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(10.0, 20.0, "   ")
        }
    }

    @Test
    fun testTimeZoneValidation() {
        val valid = BirthLocation(10.0, 20.0, "Valid", timeZoneId = "America/New_York")
        assertEquals("America/New_York", valid.timeZoneId)

        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(10.0, 20.0, "Invalid", timeZoneId = "Mars/Olympus")
        }
    }

    // 2. PERSISTENCE & BIT-EXACT PRECISION
    @Test
    fun testPersistenceAndBitExactPrecision() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LocationRepositoryImpl(context)

        val originalLocation = BirthLocation(
            latitude = 28.61393912345678,
            longitude = 77.20902123456789,
            placeName = "New Delhi, India",
            altitudeMeters = 216.54321,
            timeZoneId = "Asia/Kolkata",
            isVerified = true,
            source = "authoritative_resolver"
        )

        repo.saveVerifiedLocation(originalLocation)

        val retrievedLocation = repo.getVerifiedLocation()
        assertNotNull(retrievedLocation)
        assertEquals(originalLocation.latitude, retrievedLocation!!.latitude, 0.0)
        assertEquals(originalLocation.longitude, retrievedLocation.longitude, 0.0)
        assertEquals(originalLocation.altitudeMeters, retrievedLocation.altitudeMeters)
        assertEquals(originalLocation.placeName, retrievedLocation.placeName)
        assertEquals(originalLocation.timeZoneId, retrievedLocation.timeZoneId)
        assertEquals(originalLocation.isVerified, retrievedLocation.isVerified)
        assertEquals(originalLocation.source, retrievedLocation.source)
    }

    @Test
    fun testLocationReplacementInPersistence() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LocationRepositoryImpl(context)

        val loc1 = BirthLocation(28.6139, 77.2090, "New Delhi", 216.0, "Asia/Kolkata", true, "manual")
        repo.saveVerifiedLocation(loc1)
        assertEquals("New Delhi", repo.getVerifiedLocation()?.placeName)

        val loc2 = BirthLocation(35.6762, 139.6503, "Tokyo", 40.0, "Asia/Tokyo", true, "manual")
        repo.saveVerifiedLocation(loc2)
        val loaded = repo.getVerifiedLocation()
        assertNotNull(loaded)
        assertEquals("Tokyo", loaded?.placeName)
        assertEquals("Asia/Tokyo", loaded?.timeZoneId)
        assertEquals(35.6762, loaded!!.latitude, 0.0)
        assertEquals(139.6503, loaded.longitude, 0.0)
    }

    // 3. AUTHORITATIVE DETERMINISTIC PANCHANG INTEGRATION
    @Test
    fun testPanchangUsesAuthoritativeLocationAndZone() = runBlocking {
        val engine = SwissEphAstrologyEngine()

        val delhiLocation = BirthLocation(28.6139, 77.2090, "New Delhi", 216.0, "Asia/Kolkata", true)
        val tokyoLocation = BirthLocation(35.6762, 139.6503, "Tokyo", 40.0, "Asia/Tokyo", true)

        val delhiDate = ZonedDateTime.of(LocalDate.of(2026, 9, 4), LocalTime.of(12, 0), ZoneId.of("Asia/Kolkata"))
        val tokyoDate = ZonedDateTime.of(LocalDate.of(2026, 9, 4), LocalTime.of(12, 0), ZoneId.of("Asia/Tokyo"))

        val delhiPanchang1 = engine.calculatePanchang(delhiDate, delhiLocation).getOrThrow()
        val delhiPanchang2 = engine.calculatePanchang(delhiDate, delhiLocation).getOrThrow()

        // Determinism: Identical inputs produce identical calculations
        assertEquals(delhiPanchang1.vara, delhiPanchang2.vara)
        assertEquals(delhiPanchang1.tithi.index, delhiPanchang2.tithi.index)
        assertEquals(delhiPanchang1.sunrise, delhiPanchang2.sunrise)
        assertEquals(delhiPanchang1.sunset, delhiPanchang2.sunset)

        // Location change produces distinct, authoritative local results
        val tokyoPanchang = engine.calculatePanchang(tokyoDate, tokyoLocation).getOrThrow()
        assertEquals("Asia/Tokyo", tokyoPanchang.sunrise?.zone?.id)
        assertNotEquals(delhiPanchang1.sunrise, tokyoPanchang.sunrise)
        assertEquals("Tokyo", tokyoPanchang.location.placeName)
    }

    // 4. UI: RAW COORDINATES NOT DISPLAYED
    @Test
    fun testAstrologyProfileViewDoesNotExposeRawCoordinates() {
        val engine = SwissEphAstrologyEngine()
        val birthData = BirthData(
            name = "Aarav Sharma",
            date = LocalDate.of(1995, 8, 15),
            time = LocalTime.of(14, 30),
            location = BirthLocation(
                latitude = 28.6139391,
                longitude = 77.2090212,
                placeName = "New Delhi",
                altitudeMeters = 216.0,
                timeZoneId = "Asia/Kolkata",
                isVerified = true
            ),
            timeZone = ZoneId.of("Asia/Kolkata")
        )

        val sampleProfile = runBlocking {
            engine.calculateProfile(birthData).getOrThrow()
        }

        composeTestRule.setContent {
            JyotishTheme {
                AstrologyProfileView(
                    profile = sampleProfile,
                    onEditClick = {},
                    onClearClick = {}
                )
            }
        }

        // Verify human-readable place name is shown
        composeTestRule.onNodeWithText("New Delhi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Asia/Kolkata").assertIsDisplayed()

        // Verify raw coordinate strings like "28.6139" or "77.2090" are NOT displayed
        composeTestRule.onAllNodesWithText("28.6139", substring = true).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("77.2090", substring = true).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Coordinates", substring = true).assertCountEquals(0)
    }

    @Test
    fun testBirthDataEntryDialogShowsHumanReadableVerifiedPlace() {
        val initialData = BirthData(
            name = "Test User",
            date = LocalDate.of(1995, 8, 15),
            time = LocalTime.of(14, 30),
            location = BirthLocation(
                latitude = 28.6139,
                longitude = 77.2090,
                placeName = "New Delhi",
                altitudeMeters = 216.0,
                timeZoneId = "Asia/Kolkata",
                isVerified = true
            ),
            timeZone = ZoneId.of("Asia/Kolkata")
        )

        composeTestRule.setContent {
            JyotishTheme {
                BirthDataEntryDialog(
                    initialData = initialData,
                    onDismiss = {},
                    onSubmit = {},
                    onResolveLocation = { Result.success(listOf(initialData.location)) }
                )
            }
        }

        // Human readable verified location label is shown
        composeTestRule.onNodeWithText("Verified: New Delhi (Asia/Kolkata) • 216m elev.").assertIsDisplayed()

        // Raw coordinates are not displayed in the verified label
        composeTestRule.onAllNodesWithText("28.6139, 77.2090", substring = true).assertCountEquals(0)
    }
}
