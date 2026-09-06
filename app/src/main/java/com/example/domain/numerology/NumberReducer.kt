package com.example.domain.numerology

/**
 * Reusable, deterministic Number Reduction utility.
 * Handles single digits, multi-digits, zeros, master numbers (11, 22, 33), and preserves step traces.
 */
object NumberReducer {

    private val MASTER_NUMBERS = setOf(11, 22, 33)

    /**
     * Reduces an integer sum down to a single digit (1..9) or preserves Master Numbers (11, 22, 33)
     * if [preserveMasterNumbers] is true.
     */
    fun reduce(
        number: Int,
        preserveMasterNumbers: Boolean = true
    ): Pair<Int, List<NumberReductionStep>> {
        if (number <= 0) {
            return Pair(0, listOf(NumberReductionStep(1, "0 -> 0", 0, 0, false)))
        }

        var current = number
        val steps = mutableListOf<NumberReductionStep>()
        var stepIndex = 1

        while (true) {
            val isMaster = preserveMasterNumbers && MASTER_NUMBERS.contains(current)
            if (current in 1..9 || isMaster) {
                // Already reduced
                if (steps.isEmpty()) {
                    steps.add(
                        NumberReductionStep(
                            stepIndex = stepIndex,
                            expression = "$current",
                            inputTotal = current,
                            reducedValue = current,
                            isMasterNumber = isMaster
                        )
                    )
                }
                return Pair(current, steps)
            }

            // Sum digits
            val digits = current.toString().map { it.digitToInt() }
            val digitSum = digits.sum()
            val expr = digits.joinToString(" + ") + " = $digitSum"
            val isSumMaster = preserveMasterNumbers && MASTER_NUMBERS.contains(digitSum)

            steps.add(
                NumberReductionStep(
                    stepIndex = stepIndex++,
                    expression = expr,
                    inputTotal = current,
                    reducedValue = digitSum,
                    isMasterNumber = isSumMaster
                )
            )

            current = digitSum
        }
    }

    /**
     * Resolves the ultimate single-digit root (1..9) for a number even if it is a Master Number.
     * e.g. 11 -> 2, 22 -> 4, 33 -> 6.
     */
    fun getRootSingleDigit(number: Int): Int {
        if (number <= 0) return 0
        var n = number
        while (n > 9) {
            n = n.toString().sumOf { it.digitToInt() }
        }
        return n
    }

    /**
     * Reduces a date string component or array of integers.
     */
    fun reduceSum(
        values: List<Int>,
        preserveMasterNumbers: Boolean = true
    ): Pair<Int, List<NumberReductionStep>> {
        val total = values.sum()
        return reduce(total, preserveMasterNumbers)
    }
}
