package com.example.domain.muhurta

import java.time.LocalDate

/**
 * Resolves a date range into a sequence of individual LocalDates to evaluate independently.
 */
object DateRangeResolver {

    fun resolveDates(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        if (endDate.isBefore(startDate)) {
            return listOf(startDate)
        }

        val dates = mutableListOf<LocalDate>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            dates.add(current)
            current = current.plusDays(1)
        }
        return dates
    }
}
