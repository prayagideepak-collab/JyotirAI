package com.example.domain.numerology

/**
 * Deterministic, Unicode-safe Name-to-Number calculation resolver.
 * Supports Chaldean (1-8 sound vibrations) and Pythagorean (1-9 sequential) mappings.
 * Unmapped characters are cleanly reported and validated.
 */
object NameNumerologyResolver {

    // Chaldean Mapping (9 is sacred in Chaldean and never assigned to alphabets)
    private val CHALDEAN_MAP: Map<Char, Int> = mapOf(
        'A' to 1, 'I' to 1, 'J' to 1, 'Q' to 1, 'Y' to 1,
        'B' to 2, 'K' to 2, 'R' to 2,
        'C' to 3, 'G' to 3, 'L' to 3, 'S' to 3,
        'D' to 4, 'M' to 4, 'T' to 4,
        'E' to 5, 'H' to 5, 'N' to 5, 'X' to 5,
        'U' to 6, 'V' to 6, 'W' to 6,
        'O' to 7, 'Z' to 7,
        'F' to 8, 'P' to 8
    )

    // Pythagorean Mapping (1-9 cyclic)
    private val PYTHAGOREAN_MAP: Map<Char, Int> = mapOf(
        'A' to 1, 'J' to 1, 'S' to 1,
        'B' to 2, 'K' to 2, 'T' to 2,
        'C' to 3, 'L' to 3, 'U' to 3,
        'D' to 4, 'M' to 4, 'V' to 4,
        'E' to 5, 'N' to 5, 'W' to 5,
        'F' to 6, 'O' to 6, 'X' to 6,
        'G' to 7, 'P' to 7, 'Y' to 7,
        'H' to 8, 'Q' to 8, 'Z' to 8,
        'I' to 9, 'R' to 9
    )

    data class NameCalculationResult(
        val cleanName: String,
        val characterValues: List<Pair<Char, Int>>,
        val unmappedCharacters: List<Char>,
        val rawSum: Int,
        val reducedNumber: Int,
        val rootSingleDigit: Int,
        val isMasterNumber: Boolean,
        val reductionSteps: List<NumberReductionStep>
    )

    fun calculate(
        name: String,
        methodology: NumerologyMethodology
    ): NameCalculationResult? {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return null

        val mapping = when (methodology) {
            NumerologyMethodology.CHALDEAN -> CHALDEAN_MAP
            NumerologyMethodology.PYTHAGOREAN -> PYTHAGOREAN_MAP
        }

        val charValues = mutableListOf<Pair<Char, Int>>()
        val unmapped = mutableListOf<Char>()
        var sum = 0

        for (char in trimmed.uppercase()) {
            if (char.isWhitespace() || char == '-' || char == '.') continue
            val value = mapping[char]
            if (value != null) {
                charValues.add(Pair(char, value))
                sum += value
            } else {
                unmapped.add(char)
            }
        }

        if (charValues.isEmpty()) {
            return null
        }

        val (reduced, steps) = NumberReducer.reduce(sum, preserveMasterNumbers = true)
        val root = NumberReducer.getRootSingleDigit(reduced)

        return NameCalculationResult(
            cleanName = trimmed,
            characterValues = charValues,
            unmappedCharacters = unmapped,
            rawSum = sum,
            reducedNumber = reduced,
            rootSingleDigit = root,
            isMasterNumber = reduced in setOf(11, 22, 33),
            reductionSteps = steps
        )
    }
}
