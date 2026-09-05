package com.example.domain.models

import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Geometric Landmark Point for Palm Reading (normalized 0.0 - 1.0).
 */
data class PalmLandmarkPoint(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val name: String
)

/**
 * Real-time frame quality assessment for Palm Reading live guidance.
 */
data class PalmFrameQuality(
    val handDetected: Boolean,
    val orientationDegrees: Float,
    val distanceScale: Float, // 0.0 (too far) to 1.0 (ideal)
    val lightingScore: Float, // 0.0 to 1.0
    val sharpnessScore: Float, // 0.0 to 1.0
    val fingerVisibilityRatio: Float,
    val palmVisibilityRatio: Float,
    val isUsable: Boolean,
    val guidanceMessage: String,
    val captureCompletenessPercent: Int
)

/**
 * Traditional Samudrika Shastra Hast Rekha (Major Lines) representation.
 */
data class PalmLineAnalysis(
    val lineName: String,
    val sanskritName: String,
    val prominence: String,
    val clarity: String,
    val interpretation: String
)

/**
 * Traditional Samudrika Shastra Mounts (Grah Parvat) representation.
 */
data class PalmMountAnalysis(
    val mountName: String,
    val sanskritName: String,
    val planetLord: String,
    val developmentLevel: String,
    val signification: String,
    val interpretation: String
)

/**
 * Temporal breakdown requested by the user: Present / Today, Past-oriented, Future-oriented.
 */
data class ReadingTemporalAnalysis(
    val todayFocus: String,
    val pastInfluence: String,
    val futurePotential: String
)

/**
 * Structured Output for Palm Reading.
 * Strictly framed as traditional / interpretive reading without fatalistic claims.
 */
data class PalmReadingResult(
    val readingId: String,
    val readingDate: LocalDate,
    val timestamp: ZonedDateTime,
    val aggregatedFramesCount: Int,
    val handType: String, // e.g. "Practical / Earth Hand"
    val geometricModelType: String = "Multi-Frame Aggregated Landmark Geometry (Optical/Non-Hardware Depth)",
    val temporalReading: ReadingTemporalAnalysis,
    val majorLines: List<PalmLineAnalysis>,
    val mounts: List<PalmMountAnalysis>,
    val overallSamudrikaGuidance: String,
    val ethicalDisclaimer: String = "This palmistry interpretation is grounded in traditional Vedic Samudrika Shastra and Hast Rekha principles for personal self-reflection. It is not scientifically validated diagnostic or predictive advice."
)
