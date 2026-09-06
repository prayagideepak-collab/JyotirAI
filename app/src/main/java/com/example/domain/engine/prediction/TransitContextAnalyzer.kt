package com.example.domain.engine.prediction

import com.example.domain.models.*

/**
 * Deterministic Transit (Gochar) Context Analyzer for Parashari Life Topic evaluation.
 * Evaluates current transits relative to natal Lagna and Moon (Janma Rashi).
 */
object TransitContextAnalyzer {

    data class TransitEvaluationResult(
        val evidence: String,
        val score: Int,
        val supportingFactors: List<String>,
        val cautionFactors: List<String>,
        val relevantTransits: List<String>
    )

    fun analyze(
        topic: LifeTopic,
        profile: AstrologyProfile,
        transits: List<Transit>?
    ): TransitEvaluationResult {
        if (transits.isNullOrEmpty()) {
            return TransitEvaluationResult(
                evidence = "Transit positions not currently loaded.",
                score = 0,
                supportingFactors = emptyList(),
                cautionFactors = emptyList(),
                relevantTransits = emptyList()
            )
        }

        val lagnaSignIndex = profile.lagnaSignIndex
        val moonSignIndex = profile.moonSignIndex

        val supporting = mutableListOf<String>()
        val caution = mutableListOf<String>()
        val relevantTransits = mutableListOf<String>()
        var score = 0

        // Map each transit planet to its house from Lagna and house from Moon
        for (transit in transits) {
            val sign = Rashi.entries.firstOrNull {
                it.englishName.equals(transit.currentSign, ignoreCase = true) ||
                        it.sanskritName.equals(transit.currentSign, ignoreCase = true)
            } ?: continue

            val houseFromLagna = ((sign.index - lagnaSignIndex).mod(12)) + 1
            val houseFromMoon = ((sign.index - moonSignIndex).mod(12)) + 1

            val planetLower = transit.planet.lowercase().trim()

            // 1. Check if transiting over primary houses for this life topic
            if (houseFromLagna in topic.primaryHouses) {
                relevantTransits.add("${transit.planet} transiting House $houseFromLagna (${transit.currentSign})")
                val isBenefic = planetLower in setOf("jupiter", "venus", "mercury", "moon")
                if (isBenefic) {
                    score += 1
                    supporting.add("Benefic ${transit.planet} currently transiting House $houseFromLagna directly supports ${topic.displayName.lowercase()}.")
                } else if (planetLower in setOf("saturn", "rahu", "ketu", "mars")) {
                    score -= 1
                    caution.add("Malefic influence of ${transit.planet} transiting House $houseFromLagna advises steady discipline.")
                }
            }

            // 2. Specific Major Transits: Jupiter (Guru Gochar)
            if (planetLower == "jupiter") {
                // Classical favorable houses from Moon for Jupiter: 2, 5, 7, 9, 11
                if (houseFromMoon in setOf(2, 5, 7, 9, 11)) {
                    score += 2
                    supporting.add("Jupiter transiting the favorable $houseFromMoon${getOrdinalSuffix(houseFromMoon)} house from natal Moon brings auspicious blessings.")
                } else if (houseFromMoon in setOf(6, 8, 12)) {
                    caution.add("Jupiter in $houseFromMoon${getOrdinalSuffix(houseFromMoon)} from Moon indicates introspection and patience.")
                }
            }

            // 3. Specific Major Transits: Saturn (Shani Gochar)
            if (planetLower == "saturn") {
                // Check Sade Sati: Saturn in 12th, 1st, or 2nd from natal Moon
                val diffMoon = (sign.index - moonSignIndex).mod(12)
                if (diffMoon in setOf(11, 0, 1)) {
                    val phase = when (diffMoon) {
                        11 -> "Rising (12th from Moon)"
                        0 -> "Peak (over Natal Moon)"
                        1 -> "Setting (2nd from Moon)"
                        else -> ""
                    }
                    score -= 1
                    caution.add("Shani Sade Sati active ($phase); emphasizes patience, responsibility, and perseverance.")
                }
                // Check Ashtama Shani (8th from Moon) or Kantaka Shani (4th from Moon)
                if (diffMoon == 7) { // 8th house
                    score -= 2
                    caution.add("Ashtama Shani (Saturn 8th from Moon) encourages mindful decision-making and health awareness.")
                } else if (diffMoon == 3) { // 4th house
                    caution.add("Kantaka Shani (Saturn 4th from Moon) asks for extra care in domestic and emotional matters.")
                } else if (houseFromMoon in setOf(3, 6, 11)) {
                    score += 2
                    supporting.add("Saturn in $houseFromMoon${getOrdinalSuffix(houseFromMoon)} house from Moon is highly favorable for conquering challenges and practical success.")
                }
            }

            // 4. Rahu / Ketu Transit
            if (planetLower == "rahu") {
                if (houseFromMoon in setOf(3, 6, 11)) {
                    score += 1
                    supporting.add("Rahu transiting $houseFromMoon${getOrdinalSuffix(houseFromMoon)} from Moon enhances courage and material enterprise.")
                }
            }
        }

        val evidence = buildString {
            if (relevantTransits.isNotEmpty()) {
                append("Key planetary transits influencing this topic: ${relevantTransits.joinToString("; ")}. ")
            } else {
                append("General cosmic transits operate in supporting background houses. ")
            }
        }

        return TransitEvaluationResult(
            evidence = evidence.trim(),
            score = score,
            supportingFactors = supporting.distinct(),
            cautionFactors = caution.distinct(),
            relevantTransits = relevantTransits
        )
    }

    private fun getOrdinalSuffix(n: Int): String = when (n) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}
