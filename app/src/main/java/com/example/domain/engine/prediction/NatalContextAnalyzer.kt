package com.example.domain.engine.prediction

import com.example.domain.models.*

/**
 * Deterministic Natal Chart Context Analyzer for Parashari Life Topic evaluation.
 * Evaluates house lordships, house occupants, natural karakas, and functional dignities.
 */
object NatalContextAnalyzer {

    data class NatalEvaluationResult(
        val keyPlanets: List<PlanetPredictionContext>,
        val involvedHouses: List<Int>,
        val evidenceList: List<String>,
        val score: Int, // Positive indicates supportive, negative indicates challenging
        val supportingFactors: List<String>,
        val cautionFactors: List<String>,
        val neutralFactors: List<String>
    )

    fun analyze(
        topic: LifeTopic,
        profile: AstrologyProfile,
        dashaLord: String?,
        antardashaLord: String?,
        transits: List<Transit>? = null
    ): NatalEvaluationResult {
        val lagnaSignIndex = profile.lagnaSignIndex
        val planets = profile.planetPositions

        val planetByName = planets.associateBy { it.planet.lowercase().trim() }
        val planetByHouse = mutableMapOf<Int, MutableList<PlanetPosition>>()
        planets.forEach { p ->
            planetByHouse.getOrPut(p.house) { mutableListOf() }.add(p)
        }

        // House lords based on Whole Sign Lagna
        val houseLords = (1..12).associateWith { house ->
            val signIndex = (lagnaSignIndex + (house - 1)).mod(12)
            Rashi.fromIndex(signIndex).lord
        }

        val keyPlanets = mutableListOf<PlanetPredictionContext>()
        val involvedHouses = (topic.primaryHouses + topic.secondaryHouses).distinct()
        val evidenceList = mutableListOf<String>()
        val supportingFactors = mutableListOf<String>()
        val cautionFactors = mutableListOf<String>()
        val neutralFactors = mutableListOf<String>()
        var score = 0

        // 1. Evaluate Primary House Lords
        for (house in topic.primaryHouses) {
            val lordName = houseLords[house] ?: continue
            val lordPos = planetByName[lordName.lowercase()]
            if (lordPos != null) {
                val isDasha = lordName.equals(dashaLord, ignoreCase = true)
                val isAntar = lordName.equals(antardashaLord, ignoreCase = true)
                val transitMatch = transits?.firstOrNull { it.planet.equals(lordName, ignoreCase = true) }

                val (dignityScore, dignityDesc) = evaluateDignityScore(lordPos)
                val housePlacementScore = evaluateHousePlacementScore(lordPos.house)
                val combinedLordScore = dignityScore + housePlacementScore
                score += combinedLordScore

                val role = "$house${getOrdinalSuffix(house)} House Lord (${lordPos.dignity.displayName})"
                val influence = buildString {
                    append("$lordName rules House $house and sits in House ${lordPos.house} in ${lordPos.sign}. ")
                    if (lordPos.dignity == PlanetDignity.EXALTED || lordPos.dignity == PlanetDignity.OWN_SIGN || lordPos.dignity == PlanetDignity.MOOLATRIKONA) {
                        append("Placed with high dignity, providing foundational strength to ${topic.displayName.lowercase()}.")
                    } else if (lordPos.dignity == PlanetDignity.DEBILITATED) {
                        append("Placed in debilitation; requires constructive effort and remedial awareness.")
                    } else if (lordPos.house in setOf(6, 8, 12)) {
                        append("Placed in a Dusthana (House ${lordPos.house}), indicating obstacles, delays, or transformational effort.")
                    } else if (lordPos.house in setOf(1, 4, 7, 10, 5, 9)) {
                        append("Placed auspiciously in a Kendra/Trikona (House ${lordPos.house}), strengthening results.")
                    }
                }

                keyPlanets.add(
                    PlanetPredictionContext(
                        planetName = lordName,
                        functionalRole = role,
                        natalSign = lordPos.sign,
                        natalHouse = lordPos.house,
                        dignity = lordPos.dignity,
                        isDashaLord = isDasha,
                        isAntardashaLord = isAntar,
                        transitSign = transitMatch?.currentSign,
                        transitHouseFromLagna = transitMatch?.let { calculateHouseFromSign(lagnaSignIndex, it.currentSign) },
                        qualitativeInfluence = influence
                    )
                )

                val evidenceMsg = "$lordName (Lord of House $house) is in House ${lordPos.house} (${lordPos.sign}, ${lordPos.dignity.displayName})."
                evidenceList.add(evidenceMsg)

                if (combinedLordScore > 0) {
                    supportingFactors.add("Lord of House $house ($lordName) is favorably placed in House ${lordPos.house} with ${lordPos.dignity.displayName} status.")
                } else if (combinedLordScore < 0) {
                    cautionFactors.add("Lord of House $house ($lordName) encounters challenging placement in House ${lordPos.house} (${lordPos.dignity.displayName}).")
                } else {
                    neutralFactors.add("Lord of House $house ($lordName) is moderately positioned in House ${lordPos.house}.")
                }
            }
        }

        // 2. Evaluate Primary House Occupants
        for (house in topic.primaryHouses) {
            val occupants = planetByHouse[house] ?: emptyList()
            for (occ in occupants) {
                val isDasha = occ.planet.equals(dashaLord, ignoreCase = true)
                val isAntar = occ.planet.equals(antardashaLord, ignoreCase = true)
                val transitMatch = transits?.firstOrNull { it.planet.equals(occ.planet, ignoreCase = true) }

                val (dignityScore, _) = evaluateDignityScore(occ)
                val isBenefic = occ.planet.lowercase() in setOf("jupiter", "venus", "mercury", "moon")
                val occupantScore = if (isBenefic) (dignityScore + 1).coerceAtLeast(0) else (dignityScore - 1)
                score += occupantScore

                val role = "Occupant of House $house (${occ.dignity.displayName})"
                val influence = "${occ.planet} occupies House $house in ${occ.sign}. Influences matters of ${topic.displayName.lowercase()} through its natural nature and dignity."

                keyPlanets.add(
                    PlanetPredictionContext(
                        planetName = occ.planet,
                        functionalRole = role,
                        natalSign = occ.sign,
                        natalHouse = occ.house,
                        dignity = occ.dignity,
                        isDashaLord = isDasha,
                        isAntardashaLord = isAntar,
                        transitSign = transitMatch?.currentSign,
                        transitHouseFromLagna = transitMatch?.let { calculateHouseFromSign(lagnaSignIndex, it.currentSign) },
                        qualitativeInfluence = influence
                    )
                )

                evidenceList.add("${occ.planet} occupies House $house in ${occ.sign} (${occ.dignity.displayName}).")
                if (occupantScore > 0) {
                    supportingFactors.add("Auspicious occupant ${occ.planet} reinforces House $house.")
                } else if (occupantScore < 0) {
                    cautionFactors.add("${occ.planet} occupying House $house suggests caution or discipline.")
                }
            }
        }

        // 3. Evaluate Natural Karakas
        for (karakaName in topic.naturalKarakaPlanets) {
            val karakaPos = planetByName[karakaName.lowercase()]
            if (karakaPos != null && keyPlanets.none { it.planetName.equals(karakaName, ignoreCase = true) }) {
                val (dignityScore, _) = evaluateDignityScore(karakaPos)
                val placementScore = evaluateHousePlacementScore(karakaPos.house)
                val karakaScore = dignityScore + placementScore
                score += karakaScore

                val isDasha = karakaName.equals(dashaLord, ignoreCase = true)
                val isAntar = karakaName.equals(antardashaLord, ignoreCase = true)
                val transitMatch = transits?.firstOrNull { it.planet.equals(karakaName, ignoreCase = true) }

                val role = "Natural Karaka for ${topic.displayName}"
                val influence = "$karakaName is the natural significator for ${topic.displayName.lowercase()}, placed in House ${karakaPos.house} in ${karakaPos.sign}."

                keyPlanets.add(
                    PlanetPredictionContext(
                        planetName = karakaName,
                        functionalRole = role,
                        natalSign = karakaPos.sign,
                        natalHouse = karakaPos.house,
                        dignity = karakaPos.dignity,
                        isDashaLord = isDasha,
                        isAntardashaLord = isAntar,
                        transitSign = transitMatch?.currentSign,
                        transitHouseFromLagna = transitMatch?.let { calculateHouseFromSign(lagnaSignIndex, it.currentSign) },
                        qualitativeInfluence = influence
                    )
                )

                evidenceList.add("Natural Karaka $karakaName is in House ${karakaPos.house} (${karakaPos.sign}, ${karakaPos.dignity.displayName}).")
                if (karakaScore > 0) {
                    supportingFactors.add("Significator $karakaName is strong in ${karakaPos.sign} (House ${karakaPos.house}).")
                } else if (karakaScore < 0) {
                    cautionFactors.add("Significator $karakaName experiences friction in House ${karakaPos.house}.")
                }
            }
        }

        return NatalEvaluationResult(
            keyPlanets = keyPlanets.distinctBy { it.planetName },
            involvedHouses = involvedHouses,
            evidenceList = evidenceList,
            score = score,
            supportingFactors = supportingFactors.distinct(),
            cautionFactors = cautionFactors.distinct(),
            neutralFactors = neutralFactors.distinct()
        )
    }

    private fun evaluateDignityScore(pos: PlanetPosition): Pair<Int, String> = when (pos.dignity) {
        PlanetDignity.EXALTED -> 3 to "Exalted"
        PlanetDignity.MOOLATRIKONA -> 2 to "Moolatrikona"
        PlanetDignity.OWN_SIGN -> 2 to "Own Sign"
        PlanetDignity.FRIEND -> 1 to "Friend's Sign"
        PlanetDignity.NEUTRAL -> 0 to "Neutral Sign"
        PlanetDignity.ENEMY -> -1 to "Enemy's Sign"
        PlanetDignity.DEBILITATED -> -3 to "Debilitated"
    }

    private fun evaluateHousePlacementScore(house: Int): Int = when (house) {
        1, 5, 9 -> 2 // Trikonas & Lagna
        4, 7, 10 -> 1 // Kendras
        2, 11 -> 1 // Dhana & Labha
        3 -> 0 // Upachaya (moderate)
        6 -> -1 // Dusthana / Upachaya
        8, 12 -> -2 // Deep Dusthana
        else -> 0
    }

    private fun calculateHouseFromSign(lagnaSignIndex: Int, signName: String): Int {
        val sign = Rashi.entries.firstOrNull { it.englishName.equals(signName, ignoreCase = true) || it.sanskritName.equals(signName, ignoreCase = true) }
            ?: return 1
        return ((sign.index - lagnaSignIndex).mod(12)) + 1
    }

    private fun getOrdinalSuffix(n: Int): String = when (n) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}
