package com.example.domain.ai

import com.example.domain.interpretation.InterpretationItem
import com.example.domain.models.*
import com.example.domain.numerology.NumerologyResult
import java.time.LocalDate

/**
 * Structured User Question Intent categories.
 */
enum class AstrologerIntent(
    val code: String,
    val hindiTitle: String,
    val description: String
) {
    GENERAL_HOROSCOPE("GENERAL_HOROSCOPE", "समग्र कुण्डली फलित", "Overall birth chart strengths and life direction"),
    TODAY_GUIDANCE("TODAY_GUIDANCE", "आज का मार्गदर्शन एवं पंचांग", "Daily planetary alignment, transits and diurnal guidance"),
    CAREER_AND_PROFESSION("CAREER_AND_PROFESSION", "करियर एवं आजीविका", "10th house, D10 Dashamsha, Dashamsha lord, profession"),
    EDUCATION_AND_STUDIES("EDUCATION_AND_STUDIES", "शिक्षा एवं ज्ञान", "5th house, Jupiter/Mercury status, academic focus"),
    MARRIAGE_AND_RELATIONSHIPS("MARRIAGE_AND_RELATIONSHIPS", "विवाह एवं संबंध", "7th house, Venus/Jupiter, D9 Navamsha, relationship harmony"),
    FINANCES_AND_WEALTH("FINANCES_AND_WEALTH", "धन एवं संपत्ति", "2nd & 11th houses, wealth yogas, Dhana karakas"),
    DASHA_EXPLANATION("DASHA_EXPLANATION", "दशा विश्लेषण", "Active Mahadasha, Antardasha and transition period meaning"),
    TRANSIT_GOCHAR_EXPLANATION("TRANSIT_GOCHAR_EXPLANATION", "गोचर प्रभाव", "Planetary transits over Natal Moon/Lagna, Sade Sati"),
    YOGA_AND_DOSHA_EXPLANATION("YOGA_AND_DOSHA_EXPLANATION", "योग एवं दोष विश्लेषण", "Detailed understanding of active Yogas and classical Doshas"),
    PANCHANG_AND_TIMING("PANCHANG_AND_TIMING", "पंचांग एवं शुभ समय", "Tithi, Vara, Nakshatra, Karana, Yoga, Rahukaal, Abhijit"),
    MUHURTA_GUIDANCE("MUHURTA_GUIDANCE", "मुहूर्त परामर्श", "Auspicious timing for specific actions or ceremonies"),
    NUMEROLOGY_ANALYSIS("NUMEROLOGY_ANALYSIS", "अंकशास्त्र फलित", "Birth number (Moolank), Life Path (Bhagyank), Name vibration"),
    COMPATIBILITY_GUIDANCE("COMPATIBILITY_GUIDANCE", "कुंडली मिलान / गुण मिलान", "Ashtakoota compatibility analysis"),
    REMEDY_AND_GUIDANCE("REMEDY_AND_GUIDANCE", "पारंपरिक उपाय एवं शांति", "Vedic remedies, mantras, daan, and lifestyle harmony"),
    GENERAL_ASTROLOGY_EXPLANATION("GENERAL_ASTROLOGY_EXPLANATION", "ज्योतिष ज्ञान एवं सामान्य प्रश्न", "Conceptual understanding of Vedic astrological principles")
}

/**
 * Structured Evidence Reference backing each factual assertion.
 */
data class AIAstrologerEvidence(
    val factorName: String,
    val sourceEngine: String, // e.g. "Phase 2 Kundli (D1)", "Phase 4 Vimshottari Dasha"
    val calculatedValue: String,
    val astronomicalBasis: String
)

/**
 * Ordered structured sections for natural reading and Voice / TTS narration.
 */
data class AIAstrologerSection(
    val sectionId: String,
    val sectionTitleHindi: String,
    val narrationTextHindi: String,
    val displayMarkdownHindi: String,
    val evidences: List<AIAstrologerEvidence> = emptyList()
)

/**
 * Full AI Astrologer Response Model adhering to Step 19.
 */
data class AIAstrologerResult(
    val responseId: String,
    val profileId: String?,
    val profileName: String,
    val isPersonalized: Boolean,
    val userQuestion: String,
    val detectedIntent: AstrologerIntent,
    val intentConfidence: Double, // 0.0 to 1.0
    val mainHeadlineHindi: String,
    val simpleMeaningHindi: String,
    val currentInfluenceHindi: String,
    val cautionsHindi: String,
    val practicalRemediesHindi: List<String> = emptyList(),
    val orderedSections: List<AIAstrologerSection> = emptyList(),
    val verifiedEvidences: List<AIAstrologerEvidence> = emptyList(),
    val missingContextNotes: List<String> = emptyList(),
    val limitations: List<String> = emptyList(),
    val generationTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Synthesizes clean narration text ready for Text-To-Speech without markdown clutter.
     */
    val ttsNarrationText: String
        get() = buildString {
            append(mainHeadlineHindi).append(". ")
            append(simpleMeaningHindi).append(" ")
            if (currentInfluenceHindi.isNotBlank()) {
                append("वर्तमान प्रभाव: ").append(currentInfluenceHindi).append(" ")
            }
            if (cautionsHindi.isNotBlank()) {
                append("ध्यान रखने योग्य बातें: ").append(cautionsHindi).append(" ")
            }
            if (practicalRemediesHindi.isNotEmpty()) {
                append("पारंपरिक मार्गदर्शन: ").append(practicalRemediesHindi.joinToString(", ")).append(".")
            }
        }.trim()
}
