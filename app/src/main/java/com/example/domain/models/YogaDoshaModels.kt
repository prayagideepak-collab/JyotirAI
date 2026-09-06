package com.example.domain.models

/**
 * Deterministic Analysis Status according to Parashari evaluation principles.
 */
enum class AnalysisStatus(val displayName: String, val hindiName: String) {
    DETECTED("Detected", "सक्रिय / उपस्थित"),
    NOT_DETECTED("Not Detected", "अनुपस्थित"),
    INSUFFICIENT_DATA("Insufficient Data", "अपूर्ण डेटा"),
    CALCULATION_ERROR("Calculation Error", "गणना त्रुटि")
}

/**
 * Vedic Yoga Category classifications according to classical texts (BPHS, Phaladeepika, Saravali)
 */
enum class YogaCategory(val displayName: String, val sanskritName: String) {
    RAJA_YOGA("Raja Yoga", "राजयोग"),
    DHANA_YOGA("Dhana Yoga (Wealth)", "धनयोग"),
    MAHAPURUSHA_YOGA("Pancha Mahapurusha Yoga", "पंच महापुरुष योग"),
    VIPARITA_RAJA_YOGA("Viparita Raja Yoga", "विपरीत राजयोग"),
    NEECHA_BHANGA_RAJA_YOGA("Neecha Bhanga Raja Yoga", "नीचभंग राजयोग"),
    CHANDRA_YOGA("Chandra Yoga (Lunar)", "चन्द्र योग"),
    SURYA_YOGA("Surya Yoga (Solar)", "सूर्य योग"),
    AUSPICIOUS_COMBINATION("Auspicious Combination", "शुभ योग")
}

/**
 * Strength and vitality of a evaluated Vedic Yoga
 */
enum class YogaStrength(val displayName: String, val scoreMultiplier: Double) {
    EXCELLENT("Excellent (उच्च फल)", 1.0),
    STRONG("Strong (पूर्ण फल)", 0.8),
    MODERATE("Moderate (मध्यम फल)", 0.6),
    MILD("Mild (अल्प फल)", 0.4),
    WEAK("Weak (अति अल्प)", 0.2),
    INACTIVE("Inactive (अनुपस्थित)", 0.0),
    INSUFFICIENT_DATA("Insufficient Data (अपूर्ण डेटा)", 0.0)
}

/**
 * Structured, deterministic representation of a calculated Vedic Yoga
 */
data class YogaAnalysisResult(
    val id: String,
    val name: String,
    val sanskritName: String,
    val category: YogaCategory,
    val status: AnalysisStatus = AnalysisStatus.NOT_DETECTED,
    val strength: YogaStrength,
    val participatingPlanets: List<String>,
    val participatingHouses: List<Int>,
    val participatingSigns: List<String>,
    val evidence: String,
    val classicalRule: String,
    val calculationBasis: String,
    val limitations: String? = null,
    val positiveImpact: String = ""
) {
    val isDetected: Boolean get() = status == AnalysisStatus.DETECTED
}

/**
 * Vedic Dosha Category classifications
 */
enum class DoshaCategory(val displayName: String, val sanskritName: String) {
    MANGLIK("Manglik / Kuja Dosha", "मांगलिक / कुज दोष"),
    KAAL_SARP("Kaal Sarp Dosha", "कालसर्प दोष"),
    KEMADRUMA("Kemadruma Dosha", "केमद्रुम दोष"),
    PLANETARY_AFFLICTION("Planetary Affliction", "ग्रह दोष"),
    LUNAR_AFFLICTION("Lunar Affliction", "चन्द्र पीड़ा"),
    NAKSHATRA_JUNCTION("Gandmanta Junction", "गंडमूल / संधि दोष")
}

/**
 * Severity and cancellation status of a calculated Vedic Dosha
 */
enum class DoshaSeverity(val displayName: String, val isSevere: Boolean) {
    NONE("None (दोष मुक्त)", false),
    LOW("Low (अल्प दोष)", false),
    MODERATE("Moderate (मध्यम)", true),
    HIGH("High (तीव्र)", true),
    SEVERE("Severe (अति तीव्र)", true),
    CANCELLED("Cancelled / Bhanga (दोष भंग)", false),
    INSUFFICIENT_DATA("Insufficient Data (अपूर्ण डेटा)", false)
}

/**
 * Structured, deterministic representation of a calculated Vedic Dosha
 */
data class DoshaAnalysisResult(
    val id: String,
    val name: String,
    val sanskritName: String,
    val category: DoshaCategory,
    val status: AnalysisStatus = AnalysisStatus.NOT_DETECTED,
    val severity: DoshaSeverity,
    val isCancelled: Boolean,
    val cancellationReason: String? = null,
    val participatingPlanets: List<String>,
    val participatingHouses: List<Int>,
    val participatingSigns: List<String>,
    val evidence: String,
    val classicalRule: String,
    val remedialGuidance: List<String> = emptyList(),
    val calculationBasis: String = ""
) {
    val isDetected: Boolean get() = status == AnalysisStatus.DETECTED
}

/**
 * Complete immutable snapshot of Yoga and Dosha analysis for a profile
 */
data class YogaDoshaSnapshot(
    val profileId: String = "",
    val profileName: String,
    val calculatedAtEpochMillis: Long,
    val detectedYogas: List<YogaAnalysisResult>,
    val allEvaluatedYogas: List<YogaAnalysisResult>,
    val detectedDoshas: List<DoshaAnalysisResult>,
    val allEvaluatedDoshas: List<DoshaAnalysisResult>,
    val summaryText: String,
    val dominantYoga: YogaAnalysisResult? = null,
    val activeDoshaCount: Int = detectedDoshas.count { it.isDetected && !it.isCancelled }
)
