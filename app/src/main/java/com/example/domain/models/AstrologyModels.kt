package com.example.domain.models

import java.time.LocalDate
import java.time.LocalDateTime

// Core Astrology Models
data class PlanetPosition(val planet: String, val sign: String, val degree: Double, val house: Int)
data class Chart(val type: String, val positions: List<PlanetPosition>)

data class AstrologyProfile(
    val birthData: BirthData,
    val rashiChart: Chart,
    val lagna: String,
    val moonSign: String,
    val nakshatra: String
)

data class DashaPeriod(
    val planet: String, 
    val startDate: LocalDate, 
    val endDate: LocalDate, 
    val subPeriods: List<DashaPeriod> = emptyList()
)

data class Transit(val planet: String, val currentSign: String, val degree: Double)

data class Prediction(
    val category: String, 
    val interpretation: String, 
    val intensity: String // e.g., "strong indication", "moderate indication"
)

data class PanchangData(
    val tithi: String, 
    val yoga: String, 
    val karana: String, 
    val sunrise: LocalDateTime, 
    val sunset: LocalDateTime
)

data class Muhurta(
    val activityName: String, 
    val isFavorable: Boolean, 
    val explanation: String
)

data class CompatibilityResult(
    val score: Double, 
    val pros: List<String>, 
    val cons: List<String>, 
    val explanation: String
)

data class NumerologyResult(
    val lifePathNumber: Int, 
    val destinyNumber: Int
)

data class UserProfile(
    val id: String, 
    val primaryBirthData: BirthData?
)
