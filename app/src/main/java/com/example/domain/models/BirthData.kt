package com.example.domain.models

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Foundation data model for astrological calculations.
 * Encapsulates the specific moment and location of birth.
 */
data class BirthData(
    val date: LocalDate,
    val time: LocalTime,
    val latitude: Double,
    val longitude: Double,
    val timeZone: ZoneId,
    val name: String = "User"
)
