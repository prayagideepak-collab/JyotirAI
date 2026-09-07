package com.example.domain.numerology

/**
 * Categories of numbers eligible for compatibility and impact analysis.
 */
enum class NumberCategory(
    val code: String,
    val displayName: String,
    val hindiName: String
) {
    PERSONAL("PERSONAL", "Personal Number", "व्यक्तिगत नंबर"),
    MOBILE("MOBILE", "Mobile Number", "मोबाइल नंबर"),
    VEHICLE("VEHICLE", "Vehicle Number", "वाहन नंबर"),
    HOUSE("HOUSE", "House Number", "मकान नंबर"),
    FLAT("FLAT", "Flat Number", "फ्लैट नंबर"),
    BUSINESS("BUSINESS", "Business Number", "व्यवसायिक नंबर"),
    SHOP("SHOP", "Shop Number", "दुकान नंबर"),
    OFFICE("OFFICE", "Office Number", "कार्यालय नंबर"),
    CUSTOM("CUSTOM", "Custom Number", "कस्टम नंबर")
}

/**
 * Purposes of analysis for a number.
 */
enum class CompatibilityPurpose(
    val code: String,
    val displayName: String,
    val hindiName: String
) {
    PERSONAL("PERSONAL", "Personal Life", "व्यक्तिगत जीवन"),
    FINANCIAL("FINANCIAL", "Financial", "आर्थिक एवं धन"),
    BUSINESS("BUSINESS", "Business", "व्यवसाय एवं व्यापार"),
    PROFESSIONAL("PROFESSIONAL", "Professional", "पेशेवर एवं करियर"),
    COMMUNICATION("COMMUNICATION", "Communication", "संचार एवं संपर्क"),
    VEHICLE("VEHICLE", "Vehicle Travel", "वाहन एवं यात्रा"),
    PROPERTY("PROPERTY", "Property & Real Estate", "संपत्ति एवं आवास"),
    GENERAL("GENERAL", "General Impact", "सामान्य प्रभाव")
}

/**
 * Result classification status.
 */
enum class CompatibilityStatus(
    val code: String,
    val displayName: String,
    val hindiName: String,
    val colorHex: String
) {
    POSITIVE("POSITIVE", "Positive", "सकारात्मक (Positive)", "#4CAF50"),
    NEUTRAL("NEUTRAL", "Neutral", "तटस्थ (Neutral)", "#2196F3"),
    MIXED("MIXED", "Mixed", "मिश्रित (Mixed)", "#FF9800"),
    CHALLENGING("CHALLENGING", "Challenging", "चुनौतीपूर्ण (Challenging)", "#E91E63")
}

/**
 * Normalized representation of an input number with privacy masking.
 */
data class NormalizedNumber(
    val originalInput: String,
    val normalizedValue: String, // digits only
    val reducedValue: Int,       // single digit or master number after reduction
    val rootSingleDigit: Int,    // 1..9 root
    val category: NumberCategory,
    val customLabel: String?,
    val privacyMasked: String    // e.g. XXXXXX1234
)

/**
 * Detailed analysis report for a specific purpose.
 */
data class PurposeAnalysis(
    val purpose: CompatibilityPurpose,
    val status: CompatibilityStatus,
    val titleHindi: String,
    val positiveTendencies: List<String>,
    val challengingTendencies: List<String>,
    val guidanceHindi: String
)

/**
 * Complete Number Compatibility and Impact Result.
 */
data class NumberCompatibilityResult(
    val resultId: String,
    val profileId: String?,
    val profileName: String,
    val normalizedNumber: NormalizedNumber,
    val overallStatus: CompatibilityStatus,
    val purposeReports: Map<CompatibilityPurpose, PurposeAnalysis>,
    val summaryHindi: String,
    val guidanceHindi: String,
    val calculatedAtTimestamp: Long = System.currentTimeMillis()
)

/**
 * Comparison result between two numbers.
 */
data class NumberComparisonResult(
    val labelA: String,
    val labelB: String,
    val resultA: NumberCompatibilityResult,
    val resultB: NumberCompatibilityResult,
    val comparisonSummaryHindi: String
)
