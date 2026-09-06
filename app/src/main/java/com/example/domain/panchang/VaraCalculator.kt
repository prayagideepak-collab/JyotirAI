package com.example.domain.panchang

import com.example.domain.models.Vara
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Calculates Vedic Vara (weekday) from the local civil date in the resolved timezone.
 */
object VaraCalculator {

    fun calculate(localDate: LocalDate): Vara {
        return when (localDate.dayOfWeek.value) {
            1 -> Vara.SOMAVARA     // Monday
            2 -> Vara.MANGALAVARA  // Tuesday
            3 -> Vara.BUDHAVARA    // Wednesday
            4 -> Vara.GURUVARA     // Thursday
            5 -> Vara.SHUKRAVARA   // Friday
            6 -> Vara.SHANIVARA    // Saturday
            7 -> Vara.RAVIVARA     // Sunday
            else -> Vara.RAVIVARA
        }
    }

    fun calculate(zonedDateTime: ZonedDateTime): Vara {
        return calculate(zonedDateTime.toLocalDate())
    }
}
