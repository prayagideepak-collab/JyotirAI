package com.example.domain.alarm

import com.example.domain.engine.PanchangCalculator
import com.example.domain.models.AppError
import com.example.domain.models.BirthLocation
import com.example.domain.models.MuhurtaAlarmConfig
import com.example.domain.models.MuhurtaAlarmType
import com.example.domain.models.TimeInterval
import de.thmac.swisseph.SwissEph
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Pure calculation engine for dynamic Muhurta alarm intervals.
 * Dynamically computes the next valid occurrence for Brahma Muhurta, Rahukaal, etc.,
 * adjusting for daily sunrise/sunset shifts.
 *
 * Guarantees:
 * 1. 100% dynamic interval derivation via Swiss Ephemeris sunrise/sunset and daytime division.
 * 2. Zero hardcoded universal alarm schedules (no fixed 4:30 AM or arbitrary timings).
 * 3. Autonomous shift tracking: dynamically recomputes next occurrence when interval has passed.
 */
object MuhurtaAlarmCalculator {

    private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    /**
     * Calculates the next upcoming Muhurta interval dynamically.
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
                ?: throw AppError.CalculationError(
                    "Cannot compute dynamic astronomical ${type.title} for location ${location.placeName}. " +
                            "Sunrise/sunset calculations are unavailable for the given coordinates."
                )
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
     * Computes the exact Muhurta interval dynamically for a given date and location.
     */
    fun getMuhurtaIntervalForDate(
        type: MuhurtaAlarmType,
        location: BirthLocation,
        dateTime: ZonedDateTime,
        swe: SwissEph = SwissEph()
    ): TimeInterval? {
        val panchang = PanchangCalculator.calculatePanchang(dateTime, location, swe)
        val muhurta = panchang.muhurta ?: return null

        val interval = when (type) {
            MuhurtaAlarmType.BRAHMA_MUHURTA -> muhurta.brahmaMuhurta
            MuhurtaAlarmType.RAHUKAAL_START, MuhurtaAlarmType.RAHUKAAL -> muhurta.rahukaal
            MuhurtaAlarmType.RAHUKAAL_END -> muhurta.rahukaal
            MuhurtaAlarmType.ABHIJIT_MUHURTA -> muhurta.abhijitMuhurta
        } ?: return null

        return if (type == MuhurtaAlarmType.RAHUKAAL_END) {
            TimeInterval(interval.end, interval.end.plusMinutes(1), interval.name, interval.description)
        } else {
            interval
        }
    }

    private fun getZoneId(timeZoneId: String?): ZoneId {
        return try {
            timeZoneId?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }
    }
}
