package com.example.domain.numerology

/**
 * Utility for normalizing phone numbers, vehicle registration numbers, house/flat numbers,
 * and custom numbers, ensuring consistent formatting, reduction, and privacy masking.
 */
object NumberNormalizationUtils {

    /**
     * Normalizes raw user input into a structured [NormalizedNumber].
     */
    fun normalize(
        rawInput: String,
        category: NumberCategory,
        customLabel: String? = null
    ): NormalizedNumber {
        val trimmed = rawInput.trim()
        val digitsOnly = trimmed.filter { it.isDigit() }
        
        // Fallback if no digits found (e.g. alphabetical or special custom strings)
        val numericValueToReduce = if (digitsOnly.isNotEmpty()) {
            digitsOnly.toIntOrNull() ?: digitsOnly.map { it.digitToIntOrNull() ?: 1 }.sum()
        } else {
            // Convert characters to basic sum if purely non-numeric
            trimmed.codePoints().sum().coerceAtLeast(1)
        }

        val (reduced, _) = NumberReducer.reduce(numericValueToReduce, preserveMasterNumbers = true)
        val root = NumberReducer.getRootSingleDigit(reduced)

        val masked = maskSensitiveNumber(trimmed, digitsOnly, category)

        return NormalizedNumber(
            originalInput = trimmed,
            normalizedValue = digitsOnly.ifEmpty { trimmed },
            reducedValue = reduced,
            rootSingleDigit = root,
            category = category,
            customLabel = customLabel,
            privacyMasked = masked
        )
    }

    /**
     * Masks sensitive numbers (like mobile numbers, financial references) for logs and UI display,
     * showing only the last 4 digits where appropriate.
     */
    fun maskSensitiveNumber(rawInput: String, digitsOnly: String, category: NumberCategory): String {
        val isSensitive = category == NumberCategory.MOBILE || category == NumberCategory.BUSINESS || category == NumberCategory.PERSONAL
        if (!isSensitive) {
            return rawInput
        }
        if (digitsOnly.length <= 4) {
            return "****"
        }
        val last4 = digitsOnly.takeLast(4)
        val prefixLength = (digitsOnly.length - 4).coerceAtLeast(2)
        val maskedPrefix = "X".repeat(prefixLength)
        return "$maskedPrefix$last4"
    }

    /**
     * Validates if raw input is a valid number entry.
     */
    fun isValidInput(rawInput: String): Boolean {
        return rawInput.trim().isNotEmpty() && rawInput.trim().any { it.isLetterOrDigit() }
    }
}
