package com.example.domain.engine.prediction

import com.example.domain.models.*
import java.time.LocalDate

/**
 * Deterministic Dasha Timing Context Analyzer for Parashari Life Topic evaluation.
 * Evaluates active Mahadasha & Antardasha lords, their house associations, and mutual planetary geometry.
 */
object DashaContextAnalyzer {

    data class DashaEvaluationResult(
        val activeMahadasha: String,
        val activeAntardasha: String,
        val mahadashaEndDate: LocalDate?,
        val antardashaEndDate: LocalDate?,
        val evidence: String,
        val score: Int,
        val supportingFactors: List<String>,
        val cautionFactors: List<String>,
        val isActivatingTopic: Boolean
    )

    fun analyze(
        topic: LifeTopic,
        profile: AstrologyProfile,
        dashaTimeline: DashaTimeline?,
        targetDate: LocalDate = LocalDate.now()
    ): DashaEvaluationResult {
        if (dashaTimeline == null) {
            return DashaEvaluationResult(
                activeMahadasha = "Unknown",
                activeAntardasha = "Unknown",
                mahadashaEndDate = null,
                antardashaEndDate = null,
                evidence = "Dasha timeline data is not initialized.",
                score = 0,
                supportingFactors = emptyList(),
                cautionFactors = emptyList(),
                isActivatingTopic = false
            )
        }

        val activeMaha = dashaTimeline.currentMahadasha
        val mahaPlanet = activeMaha?.planet?.lord ?: dashaTimeline.startingMahadasha.lord
        val activeAntar = dashaTimeline.currentAntardasha
        val antarPlanet = activeAntar?.antardashaLord?.lord ?: mahaPlanet

        val lagnaSignIndex = profile.lagnaSignIndex
        val planets = profile.planetPositions
        val planetByName = planets.associateBy { it.planet.lowercase().trim() }

        // House lords based on Whole Sign Lagna
        val houseLords = (1..12).associateWith { house ->
            val signIndex = (lagnaSignIndex + (house - 1)).mod(12)
            Rashi.fromIndex(signIndex).lord
        }

        val mahaPos = planetByName[mahaPlanet.lowercase()]
        val antarPos = planetByName[antarPlanet.lowercase()]

        val supporting = mutableListOf<String>()
        val caution = mutableListOf<String>()
        var score = 0
        var isActivatingTopic = false

        // 1. Check houses ruled by Mahadasha and Antardasha lords
        val mahaRuledHouses = houseLords.filterValues { it.equals(mahaPlanet, ignoreCase = true) }.keys.toList()
        val antarRuledHouses = houseLords.filterValues { it.equals(antarPlanet, ignoreCase = true) }.keys.toList()

        val mahaActivates = mahaRuledHouses.any { it in topic.primaryHouses } || (mahaPos?.house in topic.primaryHouses)
        val antarActivates = antarRuledHouses.any { it in topic.primaryHouses } || (antarPos?.house in topic.primaryHouses)

        if (mahaActivates || antarActivates) {
            isActivatingTopic = true
            score += 2
            supporting.add("Active Dasha lords ($mahaPlanet / $antarPlanet) directly rule or occupy key houses (${topic.primaryHouses.joinToString(", ")}) for ${topic.displayName}.")
        }

        // 2. Check dignity of Dasha Lords
        if (mahaPos != null) {
            when (mahaPos.dignity) {
                PlanetDignity.EXALTED, PlanetDignity.OWN_SIGN, PlanetDignity.MOOLATRIKONA -> {
                    score += 2
                    supporting.add("Mahadasha lord $mahaPlanet is strong in natal chart (${mahaPos.dignity.displayName}).")
                }
                PlanetDignity.DEBILITATED -> {
                    score -= 2
                    caution.add("Mahadasha lord $mahaPlanet is debilitated; requires steady focus and realistic pacing.")
                }
                else -> {}
            }
        }

        if (antarPos != null && antarPlanet != mahaPlanet) {
            when (antarPos.dignity) {
                PlanetDignity.EXALTED, PlanetDignity.OWN_SIGN, PlanetDignity.MOOLATRIKONA -> {
                    score += 1
                    supporting.add("Antardasha lord $antarPlanet possesses favorable dignity (${antarPos.dignity.displayName}).")
                }
                PlanetDignity.DEBILITATED -> {
                    score -= 1
                    caution.add("Antardasha lord $antarPlanet is debilitated natally.")
                }
                else -> {}
            }
        }

        // 3. Check mutual house relationship (Sambandha) between Mahadasha and Antardasha
        if (mahaPos != null && antarPos != null && mahaPlanet != antarPlanet) {
            val dist = ((antarPos.house - mahaPos.house).mod(12)) + 1
            when (dist) {
                1, 5, 9, 3, 11 -> {
                    score += 1
                    supporting.add("Harmonious mutual relationship (${dist}th house alignment) between Mahadasha lord $mahaPlanet and Antardasha lord $antarPlanet.")
                }
                6, 8 -> {
                    score -= 2
                    caution.add("Shadashtaka (6-8 axis) relationship between $mahaPlanet and $antarPlanet; indicates transformative shifts or temporary hurdles.")
                }
                2, 12 -> {
                    score -= 1
                    caution.add("Dwirdwadashta (2-12 axis) relationship between $mahaPlanet and $antarPlanet; suggests careful resource management.")
                }
                else -> {}
            }
        }

        val evidence = buildString {
            append("Active Period: $mahaPlanet Mahadasha, $antarPlanet Antardasha. ")
            if (mahaRuledHouses.isNotEmpty()) {
                append("$mahaPlanet rules House(s) ${mahaRuledHouses.joinToString(", ")}. ")
            }
            if (antarPlanet != mahaPlanet && antarRuledHouses.isNotEmpty()) {
                append("$antarPlanet rules House(s) ${antarRuledHouses.joinToString(", ")}. ")
            }
            if (isActivatingTopic) {
                append("Directly activates foundational houses for ${topic.displayName.lowercase()}.")
            } else {
                append("Indirect background timing influence for ${topic.displayName.lowercase()}.")
            }
        }

        return DashaEvaluationResult(
            activeMahadasha = mahaPlanet,
            activeAntardasha = antarPlanet,
            mahadashaEndDate = activeMaha?.endDate?.toLocalDate(),
            antardashaEndDate = activeAntar?.endDate?.toLocalDate(),
            evidence = evidence,
            score = score,
            supportingFactors = supporting,
            cautionFactors = caution,
            isActivatingTopic = isActivatingTopic
        )
    }
}
