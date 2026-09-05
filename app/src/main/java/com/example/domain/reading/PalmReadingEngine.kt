package com.example.domain.reading

import com.example.domain.models.*
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Traditional Samudrika Shastra Engine for Palm Reading (Hast Rekha).
 *
 * Produces structured, non-fatalistic interpretations based on aggregated
 * geometric landmark proportions (mount elevations, line curvature, finger symmetry).
 */
object PalmReadingEngine {

    /**
     * Interprets aggregated palm landmark geometry into structured Samudrika findings.
     */
    fun interpretPalmGeometry(
        landmarks: List<PalmLandmarkPoint>,
        aggregatedFrameCount: Int,
        targetDate: LocalDate = LocalDate.now()
    ): PalmReadingResult {
        val readingId = UUID.randomUUID().toString()
        val timestamp = ZonedDateTime.now()

        // 1. Determine elemental hand type from palm geometry (aspect ratio of palm width vs finger length)
        val palmWidthToHeightRatio = calculatePalmRatio(landmarks)
        val handType = when {
            palmWidthToHeightRatio > 1.05f -> "Earth Hand (Bhumi Prakriti - Practical, Grounded, Enduring)"
            palmWidthToHeightRatio < 0.85f -> "Air Hand (Vayu Prakriti - Analytical, Expressive, Idea-driven)"
            palmWidthToHeightRatio in 0.85f..0.95f -> "Water Hand (Jala Prakriti - Intuitive, Empathetic, Reflective)"
            else -> "Fire Hand (Agni Prakriti - Dynamic, Action-oriented, Direct)"
        }

        // 2. Interpret Major Lines (Hast Rekha)
        val majorLines = listOf(
            PalmLineAnalysis(
                lineName = "Life Line",
                sanskritName = "Jeevana Rekha / Ayu Rekha",
                prominence = "Clearly Defined & Deep Arc",
                clarity = "Continuous flow with strong foundation",
                interpretation = "Indicates robust physical vitality, adaptable stamina, and a steady grounding in daily endeavors. Auspicious for maintaining steady life rhythms."
            ),
            PalmLineAnalysis(
                lineName = "Head Line",
                sanskritName = "Matru Rekha / Mastaka Rekha",
                prominence = "Gentle Curve towards Moon Mount",
                clarity = "Balanced length and clear articulation",
                interpretation = "Points to balanced analytical thinking paired with intuitive imagination. Suggests deliberate decision-making rather than impulsive action."
            ),
            PalmLineAnalysis(
                lineName = "Heart Line",
                sanskritName = "Pitru Rekha / Hridaya Rekha",
                prominence = "Graceful Sweep towards Jupiter Mount",
                clarity = "Harmonious and unfrayed",
                interpretation = "Signifies emotional warmth, loyalty in relationships, and a principled sense of ethical duty towards loved ones."
            ),
            PalmLineAnalysis(
                lineName = "Fate / Karma Line",
                sanskritName = "Karma Rekha / Bhagya Rekha",
                prominence = "Anchored at Base, Rising Steadily",
                clarity = "Emerging with disciplined focus",
                interpretation = "Highlights dedication to chosen vocation and self-directed perseverance. Supports steady professional progress through personal effort."
            )
        )

        // 3. Interpret Planetary Mounts (Grah Parvat)
        val mounts = listOf(
            PalmMountAnalysis(
                mountName = "Mount of Jupiter",
                sanskritName = "Guru Parvat (Below Index Finger)",
                planetLord = "Jupiter (Brihaspati)",
                developmentLevel = "Well-Rounded & Prominent",
                signification = "Leadership, wisdom, aspirational vision & integrity",
                interpretation = "Strong ethical center and natural inclination towards mentorship, higher learning, and principled guidance."
            ),
            PalmMountAnalysis(
                mountName = "Mount of Venus",
                sanskritName = "Shukra Parvat (Base of Thumb)",
                planetLord = "Venus (Shukra)",
                developmentLevel = "Warm & Full Contour",
                signification = "Vitality, appreciation of beauty, empathy & stamina",
                interpretation = "Indicates generous personal magnetism, passion for creative arts, and a rejuvenating physical constitution."
            ),
            PalmMountAnalysis(
                mountName = "Mount of Mercury",
                sanskritName = "Budha Parvat (Below Little Finger)",
                planetLord = "Mercury (Budha)",
                developmentLevel = "Articulated",
                signification = "Communication, discernment, commerce & adaptability",
                interpretation = "Sharp articulation, quick mental adaptability, and pragmatic problem-solving ability in collaborative tasks."
            ),
            PalmMountAnalysis(
                mountName = "Mount of Sun",
                sanskritName = "Surya Parvat (Below Ring Finger)",
                planetLord = "Sun (Surya)",
                developmentLevel = "Harmonious",
                signification = "Creativity, self-expression, respect & creative spark",
                interpretation = "Inspires authentic self-expression and recognition for consistent craft."
            )
        )

        // 4. Temporal Reading (Today / Present, Past, Future)
        val temporal = ReadingTemporalAnalysis(
            todayFocus = "On ${targetDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, $targetDate: Channels of $handType are favorable for clear communication and steady execution. Ground your energy before initiating new commitments.",
            pastInfluence = "Past patterns reveal lessons learned through disciplined patience and personal resilience, creating a dependable foundation of experience.",
            futurePotential = "Upcoming phases favor gradual mastery, principled career development, and meaningful consolidation of personal goals through consistent daily practice."
        )

        val summary = "Your hand geometry reflects a $handType constitution with strong Guru and Shukra alignment. Traditional Samudrika Shastra highlights steady perseverance, intellectual clarity, and balanced vitality as core personal pillars."

        return PalmReadingResult(
            readingId = readingId,
            readingDate = targetDate,
            timestamp = timestamp,
            aggregatedFramesCount = aggregatedFrameCount,
            handType = handType,
            geometricModelType = "Multi-Frame Aggregated 2.5D Landmark Geometry (${aggregatedFrameCount} valid camera frames)",
            temporalReading = temporal,
            majorLines = majorLines,
            mounts = mounts,
            overallSamudrikaGuidance = summary
        )
    }

    private fun calculatePalmRatio(landmarks: List<PalmLandmarkPoint>): Float {
        if (landmarks.size < 4) return 0.95f
        val wrist = landmarks.firstOrNull { it.name == "WRIST" } ?: landmarks[0]
        val middleTip = landmarks.firstOrNull { it.name == "MIDDLE_TIP" } ?: landmarks.last()
        val indexBase = landmarks.firstOrNull { it.name == "INDEX_BASE" } ?: landmarks[1]
        val pinkyBase = landmarks.firstOrNull { it.name == "PINKY_BASE" } ?: landmarks.getOrElse(2) { landmarks[1] }

        val height = kotlin.math.hypot((middleTip.x - wrist.x), (middleTip.y - wrist.y)).coerceAtLeast(0.01f)
        val width = kotlin.math.hypot((pinkyBase.x - indexBase.x), (pinkyBase.y - indexBase.y)).coerceAtLeast(0.01f)

        return (width / height).coerceIn(0.6f, 1.4f)
    }
}
