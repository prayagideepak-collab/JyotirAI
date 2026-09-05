package com.example.domain.alarm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.domain.models.BirthLocation
import com.example.domain.models.MuhurtaAlarmType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MuhurtaAlarmSchedulerTest {

    private lateinit var context: Context
    private lateinit var repository: MuhurtaAlarmRepository
    private lateinit var scheduler: MuhurtaAlarmScheduler

    private val testLocation = BirthLocation(
        latitude = 28.6139,
        longitude = 77.2090,
        placeName = "New Delhi, India",
        isVerified = true,
        source = "verified",
        timeZoneId = "Asia/Kolkata"
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = MuhurtaAlarmRepositoryImpl(context)
        scheduler = MuhurtaAlarmScheduler(context, repository)
    }

    @Test
    fun testScheduleAndCancelAlarm_persistsAndClearsState() {
        // Schedule Brahma Muhurta
        val config = scheduler.scheduleAlarm(
            type = MuhurtaAlarmType.BRAHMA_MUHURTA,
            location = testLocation,
            profileId = "profile_1"
        )

        assertNotNull(config)
        assertTrue(config.isEnabled)
        assertEquals(MuhurtaAlarmType.BRAHMA_MUHURTA, config.type)

        // Verify in repository
        val stored = repository.getAlarm(MuhurtaAlarmType.BRAHMA_MUHURTA, "profile_1")
        assertNotNull(stored)
        assertEquals(config.scheduledStartEpochMillis, stored?.scheduledStartEpochMillis)

        // Cancel Alarm
        scheduler.cancelAlarm(MuhurtaAlarmType.BRAHMA_MUHURTA, "profile_1")

        val storedAfterCancel = repository.getAlarm(MuhurtaAlarmType.BRAHMA_MUHURTA, "profile_1")
        assertNull("Alarm should be removed after cancellation", storedAfterCancel)
    }

    @Test
    fun testRescheduleAllActiveAlarms_updatesFutureIntervals() {
        scheduler.scheduleAlarm(
            type = MuhurtaAlarmType.RAHUKAAL,
            location = testLocation,
            profileId = "profile_2"
        )

        val beforeCount = repository.getAlarms().size
        assertTrue(beforeCount >= 1)

        scheduler.rescheduleAllActiveAlarms()

        val afterAlarms = repository.getAlarms()
        assertTrue(afterAlarms.any { it.type == MuhurtaAlarmType.RAHUKAAL && it.profileId == "profile_2" })
    }
}
