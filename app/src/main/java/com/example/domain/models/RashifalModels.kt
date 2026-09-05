package com.example.domain.models

import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Structured, deterministic domain model for Personalised Daily Rashifal.
 */
data class DailyRashifal(
    val defaultProfileId: String,
    val profileName: String,
    val targetDate: LocalDate,
    val targetDateTime: ZonedDateTime,
    val birthLocationName: String,

    // 1. Overall Daily Summary
    val dailyTheme: String,
    val energyScore: Int, // 0 to 100 deterministic rating
    val primaryFocus: String,

    // 2. Natal & Astrological Alignment (Traceability)
    val lagna: String,
    val moonSign: String,
    val birthNakshatra: String,
    val currentMahadashaLord: String,
    val currentAntardashaLord: String,
    val transitMoonHouseFromNatalMoon: Int?,
    val transitMoonHouseFromLagna: Int?,
    val taraBala: TaraBalaInfo,

    // 3. Structured Guidance Categories
    val keyInfluences: List<AstrologicalInfluence>,
    val priorities: List<DailyRecommendation>,
    val cautions: List<DailyCaution>,
    val timingGuidance: DailyTimingGuidance,
    val traditionalRemedies: List<TraditionalRemedy>,

    // 4. Panchang Snapshot Summary
    val varaName: String,
    val tithiName: String,
    val paksha: String,
    val nakshatraName: String,
    val yogaName: String,
    val karanaName: String,
    val sunriseFormatted: String?,
    val sunsetFormatted: String?,

    // 5. Astrological Attribution / Factor Explanation
    val astrologicalFactorsSummary: String,

    // 6. Ethical Disclaimer
    val ethicalDisclaimer: String = "Astrological guidance is based on traditional Jyotish principles (Vedic Panchang, Dasha, Gochar transits). It offers perspective and timing awareness, not guaranteed future events or scientific certainty."
)

data class AstrologicalInfluence(
    val title: String,
    val description: String,
    val contributingFactor: String,
    val impactType: ImpactType
)

enum class ImpactType {
    FAVORABLE,
    NEUTRAL,
    CAUTION
}

data class DailyRecommendation(
    val category: String, // e.g. Focus & Action, Relationships & Dialogue, Learning & Wisdom, Health & Routine
    val advice: String,
    val astrologicalReason: String
)

data class DailyCaution(
    val category: String,
    val warning: String,
    val astrologicalReason: String
)

data class DailyTimingGuidance(
    val brahmaMuhurtaWindow: String?,
    val brahmaMuhurtaAdvice: String?,
    val rahukaalWindow: String?,
    val rahukaalAdvice: String?,
    val abhijitMuhurtaWindow: String?,
    val favorableTimeSlots: List<String> = emptyList()
)

data class TraditionalRemedy(
    val title: String,
    val practice: String,
    val targetGrahaOrEnergy: String,
    val traditionalContext: String
)

/**
 * Traditional Vedic Navatara (Tara Bala) analysis:
 * Relationship between Daily Transit Nakshatra and Birth Nakshatra (1 to 9 cycles).
 */
data class TaraBalaInfo(
    val taraNumber: Int, // 1 to 9
    val taraName: String, // Janma, Sampat, Vipat, Kshema, Pratyak, Sadhana, Naidhana, Mitra, Parama Mitra
    val quality: String, // Favorable, Moderate, Caution
    val description: String
) {
    companion object {
        fun calculate(birthNakshatraIndex: Int, dailyNakshatraIndex: Int): TaraBalaInfo {
            val rawDiff = (dailyNakshatraIndex - birthNakshatraIndex + 27) % 27
            val taraIndex = (rawDiff % 9) + 1 // 1 to 9

            return when (taraIndex) {
                1 -> TaraBalaInfo(1, "Janma Tara", "Moderate", "Focus on physical vitality, mindset stability, and steady routine.")
                2 -> TaraBalaInfo(2, "Sampat Tara", "Highly Favorable", "Auspicious for material prosperity, progress, and financial decisions.")
                3 -> TaraBalaInfo(3, "Vipat Tara", "Caution Required", "Requires prudence; avoid rash risks, impulsive spending, or hasty arguments.")
                4 -> TaraBalaInfo(4, "Kshema Tara", "Very Favorable", "Brings protection, inner security, peaceful execution, and domestic comfort.")
                5 -> TaraBalaInfo(5, "Pratyak Tara", "Moderate Caution", "Possible hurdles or obstacles; handle negotiations with diplomacy and patience.")
                6 -> TaraBalaInfo(6, "Sadhana Tara", "Highly Favorable", "Excellent for accomplishment, dedicated work, spiritual practices, and skill mastery.")
                7 -> TaraBalaInfo(7, "Naidhana / Vadha Tara", "High Caution", "Requires maximum mindfulness; avoid high-risk ventures or confrontational interactions.")
                8 -> TaraBalaInfo(8, "Mitra Tara", "Favorable", "Promotes harmonious collaboration, friendly connections, and mutual support.")
                9 -> TaraBalaInfo(9, "Parama Mitra Tara", "Supreme Favor", "Highest goodwill, auspicious counsel, creative breakthroughs, and ease of effort.")
                else -> TaraBalaInfo(1, "Janma Tara", "Moderate", "Steady baseline energy.")
            }
        }
    }
}
