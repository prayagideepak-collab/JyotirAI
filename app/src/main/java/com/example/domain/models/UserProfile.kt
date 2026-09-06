package com.example.domain.models

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Persisted user profile containing canonical birth data required for deterministic astrological calculations.
 */
data class UserProfile(
    val id: String,
    val birthData: BirthData,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(id.isNotBlank()) { "Profile ID must not be blank" }
        require(birthData.name.isNotBlank()) { "Profile name must not be blank" }
    }

    val name: String get() = birthData.name
    val date: LocalDate get() = birthData.date
    val time: LocalTime get() = birthData.time
    val location: BirthLocation get() = birthData.location
    val timeZone: ZoneId get() = birthData.timeZone
    val gender: String get() = birthData.gender
}
