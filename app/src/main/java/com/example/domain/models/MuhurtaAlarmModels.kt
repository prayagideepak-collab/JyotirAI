package com.example.domain.models

import java.time.ZonedDateTime

enum class MuhurtaAlarmType(
    val title: String,
    val sanskritName: String,
    val defaultDescription: String
) {
    BRAHMA_MUHURTA(
        title = "Brahma Muhurta",
        sanskritName = "ब्रह्म मुहूर्त",
        defaultDescription = "Auspicious dawn period for meditation, contemplation, and peaceful mental clarity."
    ),
    RAHUKAAL(
        title = "Rahukaal",
        sanskritName = "राहुकाल",
        defaultDescription = "Traditional interval of caution; best suited for routine tasks and mindful awareness."
    ),
    ABHIJIT_MUHURTA(
        title = "Abhijit Muhurta",
        sanskritName = "अभिजीत मुहूर्त",
        defaultDescription = "Auspicious midday window suitable for righteous and constructive activities."
    );

    companion object {
        fun fromString(value: String): MuhurtaAlarmType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: BRAHMA_MUHURTA
        }
    }
}

/**
 * Model representing a dynamically calculated and scheduled Muhurta alarm.
 */
data class MuhurtaAlarmConfig(
    val type: MuhurtaAlarmType,
    val isEnabled: Boolean,
    val profileId: String?,
    val location: BirthLocation,
    val scheduledStartEpochMillis: Long,
    val scheduledEndEpochMillis: Long,
    val formattedLocalTimeRange: String,
    val calculatedForDateIso: String,
    val lastUpdatedEpochMillis: Long = System.currentTimeMillis()
)
