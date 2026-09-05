package com.example.domain.interpretation

import com.example.domain.models.*

/**
 * High-level categories for Vedic astrological interpretations.
 */
enum class InterpretationCategory(val displayName: String, val sanskritName: String) {
    NATAL_LAGNA("Lagna & Constitution", "लग्न एवं स्वभाव"),
    MOON_AND_MIND("Moon & Consciousness", "चन्द्र एवं मनोदशा"),
    PLANETARY_DIGNITY("Planetary Dignity & Strength", "ग्रह स्थिति एवं बल"),
    HOUSE_SIGNIFICATION("House Placements", "भाव स्थिति"),
    VARGA_D9_NAVAMSHA("D1 ↔ D9 Navamsha Analysis", "डी १ एवं डी ९ नवांश विश्लेषण"),
    VARGA_D10_DASHAMSHA("D1 ↔ D10 Dashamsha Career Synthesis", "डी १ एवं डी १० दशांश कर्म विश्लेषण"),
    DASHA_TIMING("Vimshottari Dasha Timing", "विंशोत्तरी दशा काल"),
    TRANSIT_INFLUENCE("Gochar (Planetary Transits)", "ग्रह गोचर प्रभाव"),
    PANCHANG_ALIGNMENT("Panchang & Muhurta Timing", "पंचांग शुद्धि एवं मुहूर्त"),
    SYNTHESIS_AND_BALANCE("Overall Synthesis & Guidance", "समन्वित फलित")
}

/**
 * Astrological factor polarity for deterministic balancing and conflict resolution.
 */
enum class InterpretationFactorPolarity(val label: String, val sanskritLabel: String) {
    SUPPORTIVE("Supportive", "अनुकूल"),
    CHALLENGING("Challenging", "सतर्कता"),
    NEUTRAL("Neutral", "तटस्थ")
}

/**
 * Astrological influence priority for conflict resolution.
 * Priority hierarchy: DOMINANT (e.g. Mahadasha Lord) > PRIMARY (Lagna / Moon) > SECONDARY (Transits / D9) > SUBTLE (Panchang).
 */
enum class InterpretationPriority(val ranking: Int, val label: String) {
    DOMINANT(4, "Dominant Factor"),
    PRIMARY(3, "Primary Factor"),
    SECONDARY(2, "Secondary Factor"),
    SUBTLE(1, "Subtle Context")
}

/**
 * Atomic calculated factor providing mathematical basis for an interpretation.
 */
data class InterpretationFactor(
    val name: String,
    val category: InterpretationCategory,
    val source: String,
    val calculatedValue: String,
    val polarity: InterpretationFactorPolarity,
    val priority: InterpretationPriority,
    val weight: Int // 1 to 10 for deterministic resolution
)

/**
 * Verifiable calculation evidence supporting an interpretation.
 */
data class InterpretationEvidence(
    val title: String,
    val metrics: Map<String, String>,
    val astronomicalBasis: String
)

/**
 * Individual structured interpretation unit.
 */
data class InterpretationItem(
    val id: String,
    val title: String,
    val sanskritTitle: String,
    val category: InterpretationCategory,
    val summary: String,
    val detailedDescription: String,
    val factors: List<InterpretationFactor>,
    val evidence: InterpretationEvidence? = null,
    val traditionalGuidance: String? = null,
    val cautions: String? = null
)

/**
 * Structured D1 (Rashi) ↔ D9 (Navamsha) and D1 ↔ D10 (Dashamsha) comparative analysis.
 */
data class DivisionalCrossAnalysis(
    val d9Available: Boolean,
    val d9LagnaSign: String? = null,
    val d9LagnaRelationship: String? = null,
    val vargottamaPlanets: List<String> = emptyList(),
    val strengthenedPlanetsInD9: List<String> = emptyList(),
    val weakenedPlanetsInD9: List<String> = emptyList(),
    val d9Summary: String,
    val d10Available: Boolean,
    val d10LagnaSign: String? = null,
    val d10TenthLordPlacement: String? = null,
    val d10KeyPlanets: List<String> = emptyList(),
    val d10CareerThemes: List<String> = emptyList(),
    val d10Summary: String
)

/**
 * Structured context for active Vimshottari Dasha period.
 */
data class DashaInterpretationContext(
    val mahadashaLord: String,
    val antardashaLord: String,
    val periodDates: String,
    val lordRelationship: String,
    val mahadashaNatalHouse: Int,
    val antardashaNatalHouse: Int,
    val summary: String,
    val activeThemes: List<String>
)

/**
 * Structured context for planetary transits (Gochar).
 */
data class TransitInterpretationContext(
    val referenceMoonSign: String,
    val referenceLagnaSign: String,
    val sadeSatiPhase: String?,
    val isKantakaShani: Boolean,
    val isAshtamaShani: Boolean,
    val beneficTransits: List<String>,
    val challengingTransits: List<String>,
    val summary: String
)

/**
 * Structured context for daily Panchang elements.
 */
data class PanchangInterpretationContext(
    val vara: String,
    val tithi: String,
    val paksha: String,
    val nakshatra: String,
    val yoga: String,
    val karana: String,
    val brahmaMuhurta: String?,
    val abhijitMuhurta: String?,
    val rahukaal: String?,
    val summary: String
)

/**
 * Comprehensive composable Vedic Interpretation model uniting all calculated dimensions.
 */
data class AdvancedVedicInterpretation(
    val profileId: String,
    val profileName: String,
    val calculationTimestamp: String,
    val natalSummary: String,
    val strongestCurrentFactors: List<InterpretationFactor>,
    val dashaContext: DashaInterpretationContext?,
    val transitContext: TransitInterpretationContext?,
    val panchangContext: PanchangInterpretationContext?,
    val divisionalAnalysis: DivisionalCrossAnalysis,
    val allInterpretationItems: List<InterpretationItem>,
    val supportiveFactors: List<InterpretationFactor>,
    val challengingFactors: List<InterpretationFactor>,
    val dominantFactor: InterpretationFactor?,
    val opportunities: List<String>,
    val cautions: List<String>,
    val traditionalGuidance: List<String>,
    val disclaimer: String = ETHICAL_DISCLAIMER
) {
    companion object {
        const val ETHICAL_DISCLAIMER =
            "All interpretations are grounded in classical Vedic astrology (Brihat Parashara Hora Shastra) " +
            "principles as symbolic, contextual indicators of tendencies and cycles. JyotirAI strictly avoids " +
            "fatalistic predictions, medical diagnoses, guaranteed outcomes, or deterministic claims regarding " +
            "marriage, lifespan, litigation, or wealth. Use this wisdom for conscious personal reflection."
    }
}
