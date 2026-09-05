package com.example.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.models.BirthData
import com.example.domain.models.BirthLocation
import com.example.domain.models.UserProfile
import com.example.domain.profile.ProfileRepositoryImpl
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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProfileRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var repository: ProfileRepositoryImpl

    private fun createSampleProfile(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test User",
        lat: Double = 28.6139391,
        lon: Double = 77.2090212,
        place: String = "New Delhi"
    ): UserProfile {
        val location = BirthLocation(
            latitude = lat,
            longitude = lon,
            placeName = place,
            altitudeMeters = 216.0,
            timeZoneId = "Asia/Kolkata",
            isVerified = true,
            source = "manual"
        )
        val birthData = BirthData(
            name = name,
            date = LocalDate.of(1995, 8, 15),
            time = LocalTime.of(14, 30, 45),
            location = location,
            timeZone = ZoneId.of("Asia/Kolkata")
        )
        return UserProfile(id = id, birthData = birthData)
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("jyotirai_profiles_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        repository = ProfileRepositoryImpl(context)
    }

    @Test
    fun testEmptyRepositoryState() = runTest {
        assertTrue(repository.getAllProfiles().isEmpty())
        assertNull(repository.getActiveProfileId())
        assertNull(repository.getDefaultProfileId())
        assertNull(repository.getActiveProfile())
        assertNull(repository.getDefaultProfile())
    }

    @Test
    fun testFirstSavedProfileBecomesDefaultAndActive() = runTest {
        val p1 = createSampleProfile(name = "Person 1")
        val result = repository.saveProfile(p1)
        assertTrue(result.isSuccess)

        val all = repository.getAllProfiles()
        assertEquals(1, all.size)
        assertEquals(p1.id, all[0].id)
        assertEquals(p1.name, all[0].name)

        assertEquals(p1.id, repository.getDefaultProfileId())
        assertEquals(p1.id, repository.getActiveProfileId())
        assertEquals(p1.id, repository.getDefaultProfile()?.id)
        assertEquals(p1.id, repository.getActiveProfile()?.id)
    }

    @Test
    fun testEnforcesMax3ProfilesLimit() = runTest {
        val p1 = createSampleProfile(name = "Person 1")
        val p2 = createSampleProfile(name = "Person 2")
        val p3 = createSampleProfile(name = "Person 3")
        val p4 = createSampleProfile(name = "Person 4")

        assertTrue(repository.saveProfile(p1).isSuccess)
        assertTrue(repository.saveProfile(p2).isSuccess)
        assertTrue(repository.saveProfile(p3).isSuccess)

        assertEquals(3, repository.getAllProfiles().size)

        // 4th profile should fail
        val fourthResult = repository.saveProfile(p4)
        assertTrue(fourthResult.isFailure)
        assertEquals(3, repository.getAllProfiles().size)
    }

    @Test
    fun testUpdateExistingProfileDoesNotConsumeExtraSlot() = runTest {
        val p1 = createSampleProfile(name = "Person 1")
        val p2 = createSampleProfile(name = "Person 2")
        val p3 = createSampleProfile(name = "Person 3")

        repository.saveProfile(p1)
        repository.saveProfile(p2)
        repository.saveProfile(p3)

        // Updating p2
        val updatedP2 = p2.copy(
            birthData = p2.birthData.copy(name = "Person 2 Updated")
        )
        val updateResult = repository.saveProfile(updatedP2)
        assertTrue(updateResult.isSuccess)

        val all = repository.getAllProfiles()
        assertEquals(3, all.size)
        val fetched = repository.getProfileById(p2.id)
        assertNotNull(fetched)
        assertEquals("Person 2 Updated", fetched?.name)
    }

    @Test
    fun testSwitchActiveProfilePreservesDefaultProfile() = runTest {
        val p1 = createSampleProfile(name = "Person 1")
        val p2 = createSampleProfile(name = "Person 2")

        repository.saveProfile(p1)
        repository.saveProfile(p2)

        assertEquals(p1.id, repository.getDefaultProfileId())
        assertEquals(p1.id, repository.getActiveProfileId())

        // Switch active to p2
        val switchRes = repository.setActiveProfileId(p2.id)
        assertTrue(switchRes.isSuccess)

        assertEquals(p2.id, repository.getActiveProfileId())
        assertEquals(p1.id, repository.getDefaultProfileId()) // Default remains p1!
    }

    @Test
    fun testSetDefaultProfile() = runTest {
        val p1 = createSampleProfile(name = "Person 1")
        val p2 = createSampleProfile(name = "Person 2")

        repository.saveProfile(p1)
        repository.saveProfile(p2)

        repository.setDefaultProfileId(p2.id)

        assertEquals(p2.id, repository.getDefaultProfileId())
        assertEquals(p2.id, repository.getDefaultProfile()?.id)
        assertEquals(p1.id, repository.getActiveProfileId()) // Active still p1
    }

    @Test
    fun testDeleteDefaultProfileRepairsDefaultToAnotherProfile() = runTest {
        val p1 = createSampleProfile(name = "Person 1")
        val p2 = createSampleProfile(name = "Person 2")

        repository.saveProfile(p1)
        repository.saveProfile(p2)

        // p1 is default
        assertEquals(p1.id, repository.getDefaultProfileId())

        // Delete p1
        repository.deleteProfile(p1.id)

        assertEquals(1, repository.getAllProfiles().size)
        // Should automatically repair default and active to p2
        assertEquals(p2.id, repository.getDefaultProfileId())
        assertEquals(p2.id, repository.getActiveProfileId())
    }

    @Test
    fun testDeleteAllProfilesClearsState() = runTest {
        val p1 = createSampleProfile(name = "Person 1")
        repository.saveProfile(p1)
        assertEquals(1, repository.getAllProfiles().size)

        repository.deleteProfile(p1.id)
        assertTrue(repository.getAllProfiles().isEmpty())
        assertNull(repository.getDefaultProfileId())
        assertNull(repository.getActiveProfileId())
        assertNull(repository.getDefaultProfile())
        assertNull(repository.getActiveProfile())
    }

    @Test
    fun testExact64BitCoordinatePrecisionRestoration() = runTest {
        val exactLat = 28.613939102938475
        val exactLon = 77.209021203948576
        val exactAlt = 216.54321

        val p = createSampleProfile(name = "Precision Test", lat = exactLat, lon = exactLon).copy(
            birthData = createSampleProfile().birthData.copy(
                location = BirthLocation(
                    latitude = exactLat,
                    longitude = exactLon,
                    placeName = "New Delhi High Precision",
                    altitudeMeters = exactAlt,
                    timeZoneId = "Asia/Kolkata",
                    isVerified = true,
                    source = "verified_test"
                )
            )
        )

        repository.saveProfile(p)

        val restored = repository.getProfileById(p.id)
        assertNotNull(restored)
        assertEquals(exactLat, restored!!.location.latitude, 0.0)
        assertEquals(exactLon, restored.location.longitude, 0.0)
        assertEquals(exactAlt, restored.location.altitudeMeters!!, 0.0)
    }

    @Test
    fun testCorruptedRecordHandling() = runTest {
        val p1 = createSampleProfile(name = "Valid Profile 1")
        repository.saveProfile(p1)

        // Inject corrupted JSON
        val prefs = context.getSharedPreferences("jyotirai_profiles_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("profile_corrupted", "{ malformed_json::: 123 }").apply()
        prefs.edit().putString("profile_ids", "[\"${p1.id}\", \"corrupted\"]").apply()

        val all = repository.getAllProfiles()
        // Should gracefully skip corrupted record and return valid p1
        assertEquals(1, all.size)
        assertEquals(p1.id, all[0].id)
    }

    @Test
    fun testRestartRestoresAllProfilesAndPreservesIndependentDefaultAndActive() = runTest {
        val p1 = createSampleProfile(name = "Profile One")
        val p2 = createSampleProfile(name = "Profile Two")
        val p3 = createSampleProfile(name = "Profile Three")

        repository.saveProfile(p1)
        repository.saveProfile(p2)
        repository.saveProfile(p3)

        // Set p2 as Default, and p3 as Active
        repository.setDefaultProfileId(p2.id)
        repository.setActiveProfileId(p3.id)

        // Simulate app restart by constructing new repository instance
        val restartedRepo = ProfileRepositoryImpl(context)

        val restoredProfiles = restartedRepo.getAllProfiles()
        assertEquals(3, restoredProfiles.size)
        assertEquals(p2.id, restartedRepo.getDefaultProfileId())
        assertEquals(p3.id, restartedRepo.getActiveProfileId())
        assertEquals(p2.id, restartedRepo.getDefaultProfileForDailyPrediction()?.id)
        assertEquals(p2.id, restartedRepo.getDefaultProfile()?.id)
        assertEquals(p3.id, restartedRepo.getActiveProfile()?.id)

        // Opening Profile 1 on restarted repo does not alter Default Profile 2
        restartedRepo.setActiveProfileId(p1.id)
        assertEquals(p1.id, restartedRepo.getActiveProfileId())
        assertEquals(p2.id, restartedRepo.getDefaultProfileId())
        assertEquals(p2.id, restartedRepo.getDefaultProfileForDailyPrediction()?.id)
    }

    @Test
    fun testProfileEditPreservesIdAndChangesOnlyTargetProfile() = runTest {
        val p1 = createSampleProfile(name = "Original P1")
        val p2 = createSampleProfile(name = "Original P2")

        repository.saveProfile(p1)
        repository.saveProfile(p2)

        val editedP1 = p1.copy(
            birthData = p1.birthData.copy(name = "Updated P1")
        )
        val updateRes = repository.saveProfile(editedP1)
        assertTrue(updateRes.isSuccess)

        val fetchedP1 = repository.getProfileById(p1.id)
        val fetchedP2 = repository.getProfileById(p2.id)

        assertNotNull(fetchedP1)
        assertNotNull(fetchedP2)
        assertEquals(p1.id, fetchedP1!!.id)
        assertEquals("Updated P1", fetchedP1.name)
        // P2 must remain untouched
        assertEquals(p2.id, fetchedP2!!.id)
        assertEquals("Original P2", fetchedP2.name)
    }

    @Test
    fun testSchemaMigrationPreservesAllProfiles() = runTest {
        val p1 = createSampleProfile(name = "Legacy Profile")
        repository.saveProfile(p1)

        // Manually set old schema version in preferences
        val prefs = context.getSharedPreferences("jyotirai_profiles_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("schema_version", 0).apply()

        // Create new repo instance simulating app update
        val updatedRepo = ProfileRepositoryImpl(context)

        val all = updatedRepo.getAllProfiles()
        assertEquals(1, all.size)
        assertEquals(p1.id, all[0].id)
        assertEquals("Legacy Profile", all[0].name)
    }
}
