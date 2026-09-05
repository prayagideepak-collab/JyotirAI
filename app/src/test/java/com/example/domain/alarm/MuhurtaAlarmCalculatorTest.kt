package com.example.domain.alarm

import com.example.domain.models.BirthLocation
import com.example.domain.models.MuhurtaAlarmType
import de.thmac.swisseph.SwissEph
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class MuhurtaAlarmCalculatorTest {

    private val testLocation = BirthLocation(
        latitude = 28.6139,
        longitude = 77.2090,
        placeName = "New Delhi, India",
        isVerified = true,
        source = "verified",
        timeZoneId = "Asia/Kolkata"
    )

    private val swe = SwissEph()

    @Test
    fun testCalculateNextOccurrence_earlyMorning_schedulesToday() {
        val zone = ZoneId.of("Asia/Kolkata")
        // Reference time at 02:00 AM (before Brahma Muhurta)
        val earlyMorning = ZonedDateTime.of(LocalDate.of(2026, 9, 5), LocalTime.of(2, 0), zone)

        val config = MuhurtaAlarmCalculator.calculateNextOccurrence(
            type = MuhurtaAlarmType.BRAHMA_MUHURTA,
            location = testLocation,
            profileId = "profile_delhi",
            referenceDateTime = earlyMorning,
            swe = swe
        )

        assertEquals("Must be scheduled for today", "2026-09-05", config.calculatedForDateIso)
        assertTrue("Scheduled millis must be after reference time", config.scheduledStartEpochMillis > earlyMorning.toInstant().toEpochMilli())
        assertTrue("Formatted time range should not be empty", config.formattedLocalTimeRange.isNotEmpty())
    }

    @Test
    fun testCalculateNextOccurrence_afternoon_advancesToTomorrow() {
        val zone = ZoneId.of("Asia/Kolkata")
        // Reference time at 14:00 PM (after Brahma Muhurta and Rahukaal)
        val afternoon = ZonedDateTime.of(LocalDate.of(2026, 9, 5), LocalTime.of(14, 0), zone)

        val config = MuhurtaAlarmCalculator.calculateNextOccurrence(
            type = MuhurtaAlarmType.BRAHMA_MUHURTA,
            location = testLocation,
            profileId = "profile_delhi",
            referenceDateTime = afternoon,
            swe = swe
        )

        assertEquals("Must be dynamically scheduled for tomorrow", "2026-09-06", config.calculatedForDateIso)
        assertTrue("Scheduled millis must be in future", config.scheduledStartEpochMillis > afternoon.toInstant().toEpochMilli())
    }

    @Test
    fun testRahukaalInterval_isCalculatedDynamically() {
        val zone = ZoneId.of("Asia/Kolkata")
        val referenceTime = ZonedDateTime.of(LocalDate.of(2026, 9, 5), LocalTime.of(6, 0), zone)

        val config = MuhurtaAlarmCalculator.calculateNextOccurrence(
            type = MuhurtaAlarmType.RAHUKAAL,
            location = testLocation,
            profileId = "profile_delhi",
            referenceDateTime = referenceTime,
            swe = swe
        )

        assertNotNull(config)
        assertEquals(MuhurtaAlarmType.RAHUKAAL, config.type)
        assertTrue(config.isEnabled)
        assertTrue(config.scheduledEndEpochMillis > config.scheduledStartEpochMillis)
    }
}
