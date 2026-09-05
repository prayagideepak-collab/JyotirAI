package com.example.domain.models

import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Geometric Landmark Point for Face Reading (normalized 0.0 - 1.0).
 */
data class FaceLandmarkPoint(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val region: String
)

/**
 * Real-time frame quality assessment for Face Reading live guidance.
 */
data class FaceFrameQuality(
    val faceDetected: Boolean,
    val alignmentRollPitchYaw: Triple<Float, Float, Float>,
    val distanceScale: Float, // 0.0 (too far) to 1.0 (ideal)
    val lightingScore: Float, // 0.0 to 1.0
    val sharpnessScore: Float, // 0.0 to 1.0
    val symmetryScore: Float, // 0.0 to 1.0
    val landmarkCompletenessRatio: Float,
    val isUsable: Boolean,
    val guidanceMessage: String,
    val captureCompletenessPercent: Int
)

/**
 * Traditional Samudrika Shastra Facial Zones (Tri-Bhaga):
 * 1. Upper Zone (Mastaka / Forehead - Youth & Intellectual tendencies)
 * 2. Middle Zone (Nasa-Karna-Netra / Eyes & Nose - Middle years, Ambition & Vitality)
 * 3. Lower Zone (Chibuka-Mukha / Mouth & Chin - Stability, Later life & Willpower)
 */
data class FacialZoneAnalysis(
    val zoneName: String,
    val sanskritName: String,
    val traditionalSignification: String,
    val prominentTrait: String,
    val interpretation: String
)

/**
 * Specific Facial Feature Structure (Netra, Nasa, Lalaata, Chibuka, etc.)
 */
data class FacialFeatureAnalysis(
    val featureName: String,
    val sanskritName: String,
    val structuralTrait: String,
    val traditionalMeaning: String
)

/**
 * Structured Output for Face Reading (Mukh Samudrika Shastra).
 * Strictly framed as traditional / interpretive reading without fatalistic claims.
 */
data class FaceReadingResult(
    val readingId: String,
    val readingDate: LocalDate,
    val timestamp: ZonedDateTime,
    val aggregatedFramesCount: Int,
    val faceArchetype: String, // e.g. "Sattva-Rajasic Balanced Oval"
    val geometricModelType: String = "Multi-View Stabilized Facial Landmark Geometry (Optical 2.5D RGB, Non-Hardware Depth)",
    val temporalReading: ReadingTemporalAnalysis,
    val zones: List<FacialZoneAnalysis>,
    val features: List<FacialFeatureAnalysis>,
    val overallMukhSamudrikaGuidance: String,
    val ethicalDisclaimer: String = "This face reading interpretation is based on classical Vedic Mukh Samudrika Shastra traditions for character contemplation. It is not scientifically validated diagnostic or predictive advice."
)
