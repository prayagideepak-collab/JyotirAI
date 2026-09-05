package com.example.domain.alarm

import com.example.domain.engine.PanchangCalculator
import com.example.domain.models.BirthLocation
import com.example.domain.models.MuhurtaAlarmConfig
import com.example.domain.models.MuhurtaAlarmType
import com.example.domain.models.TimeInterval
import de.thmac.swisseph.SwissEph
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Pure calculation engine for dynamic Muhurta alarm intervals.
 * Dynamically computes the next valid occurrence for Brahma Muhurta, Rahukaal, etc.,
 * adjusting for daily sunrise/sunset shifts.
 */
object MuhurtaAlarmCalculator {

    private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    /**
     * Calculates the next upcoming Muhurta interval.
     * If today's interval is in the past, advances to tomorrow to account for sunrise shift.
     */
    fun calculateNextOccurrence(
        type: MuhurtaAlarmType,
        location: BirthLocation,
        profileId: String?,
        referenceDateTime: ZonedDateTime = ZonedDateTime.now(getZoneId(location.timeZoneId)),
        swe: SwissEph = SwissEph()
    ): MuhurtaAlarmConfig {
        val todayInterval = getMuhurtaIntervalForDate(type, location, referenceDateTime, swe)

        val targetInterval = if (todayInterval != null && todayInterval.start.isAfter(referenceDateTime)) {
            todayInterval
        } else {
            // Today's interval already started/passed, calculate tomorrow's interval dynamically
            val tomorrowDateTime = referenceDateTime.plusDays(1)
            getMuhurtaIntervalForDate(type, location, tomorrowDateTime, swe)
                ?: fallbackInterval(type, tomorrowDateTime)
        }

        val startMillis = targetInterval.start.toInstant().toEpochMilli()
        val endMillis = targetInterval.end.toInstant().toEpochMilli()
        val formattedRange = "${targetInterval.start.format(TIME_FORMATTER)} - ${targetInterval.end.format(TIME_FORMATTER)}"

        return MuhurtaAlarmConfig(
            type = type,
            isEnabled = true,
            profileId = profileId,
            location = location,
            scheduledStartEpochMillis = startMillis,
            scheduledEndEpochMillis = endMillis,
            formattedLocalTimeRange = formattedRange,
            calculatedForDateIso = targetInterval.start.toLocalDate().toString()
        )
    }

    /**
     * Computes the exact Muhurta interval for a given date and location.
     */
    fun getMuhurtaIntervalForDate(
        type: MuhurtaAlarmType,
        location: BirthLocation,
        dateTime: ZonedDateTime,
        swe: SwissEph = SwissEph()
    ): TimeInterval? {
        val panchang = PanchangCalculator.calculatePanchang(dateTime, location, swe)
        val muhurta = panchang.muhurta ?: return null

        return when (type) {
            MuhurtaAlarmType.BRAHMA_MUHURTA -> muhurta.brahmaMuhurta
            MuhurtaAlarmType.RAHUKAAL -> muhurta.rahukaal
            MuhurtaAlarmType.ABHIJIT_MUHURTA -> muhurta.abhijitMuhurta
        }
    }

    private fun fallbackInterval(type: MuhurtaAlarmType, dateTime: ZonedDateTime): TimeInterval {
        val start = when (type) {
            MuhurtaAlarmType.BRAHMA_MUHURTA -> dateTime.toLocalDate().atTime(4, 30).atZone(dateTime.zone)
            MuhurtaAlarmType.RAHUKAAL -> dateTime.toLocalDate().atTime(9, 0).atZone(dateTime.zone)
            MuhurtaAlarmType.ABHIJIT_MUHURTA -> dateTime.toLocalDate().atTime(11, 45).atZone(dateTime.zone)
        }
        val end = start.plusMinutes(48)
        return TimeInterval(start = start, end = end, name = type.title)
    }

    private fun getZoneId(timeZoneId: String?): ZoneId {
        return try {
            timeZoneId?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }
    }
}
