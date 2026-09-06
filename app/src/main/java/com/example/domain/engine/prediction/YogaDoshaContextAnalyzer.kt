package com.example.domain.engine.prediction

import com.example.domain.models.*

/**
 * Deterministic Yoga & Dosha Context Analyzer for Parashari Life Topic evaluation.
 * Evaluates how detected natal Yogas and Doshas specifically affect each life topic.
 */
object YogaDoshaContextAnalyzer {

    data class YogaDoshaEvaluationResult(
        val relevantYogas: List<String>,
        val relevantDoshas: List<String>,
        val score: Int,
        val supportingFactors: List<String>,
        val cautionFactors: List<String>
    )

    fun analyze(
        topic: LifeTopic,
        yogaDoshaSnapshot: YogaDoshaSnapshot?
    ): YogaDoshaEvaluationResult {
        if (yogaDoshaSnapshot == null) {
            return YogaDoshaEvaluationResult(
                relevantYogas = emptyList(),
                relevantDoshas = emptyList(),
                score = 0,
                supportingFactors = emptyList(),
                cautionFactors = emptyList()
            )
        }

        val supporting = mutableListOf<String>()
        val caution = mutableListOf<String>()
        val relevantYogas = mutableListOf<String>()
        val relevantDoshas = mutableListOf<String>()
        var score = 0

        val detectedYogas = yogaDoshaSnapshot.detectedYogas
        val detectedDoshas = yogaDoshaSnapshot.detectedDoshas

        // 1. Map Yogas by Topic
        for (yoga in detectedYogas) {
            val isRelevant = when (topic) {
                LifeTopic.CAREER -> yoga.category in setOf(
                    YogaCategory.RAJA_YOGA,
                    YogaCategory.MAHAPURUSHA_YOGA,
                    YogaCategory.VIPARITA_RAJA_YOGA,
                    YogaCategory.NEECHA_BHANGA_RAJA_YOGA
                ) || yoga.id in setOf("amala_yoga", "budhaditya_yoga", "gaja_kesari_yoga")
                LifeTopic.FINANCE -> yoga.category in setOf(
                    YogaCategory.DHANA_YOGA,
                    YogaCategory.RAJA_YOGA
                ) || yoga.id in setOf("chandra_mangala_yoga", "lakshmi_yoga", "gaja_kesari_yoga")
                LifeTopic.MARRIAGE_RELATIONSHIPS -> yoga.id in setOf(
                    "mahapurusha_malavya",
                    "lakshmi_yoga",
                    "gaja_kesari_yoga"
                ) || yoga.participatingHouses.any { it in setOf(7, 2, 8, 11) }
                LifeTopic.EDUCATION -> yoga.id in setOf(
                    "saraswati_yoga",
                    "budhaditya_yoga",
                    "mahapurusha_bhadra",
                    "mahapurusha_hamsa"
                ) || yoga.participatingHouses.any { it in setOf(4, 5, 9) }
                LifeTopic.PROPERTY_HOME -> yoga.participatingHouses.any { it == 4 } ||
                        yoga.id in setOf("mahapurusha_ruchaka", "mahapurusha_malavya")
                LifeTopic.FAMILY -> yoga.participatingHouses.any { it in setOf(2, 4) } ||
                        yoga.id in setOf("gaja_kesari_yoga", "lakshmi_yoga")
                LifeTopic.GENERAL_LIFE -> true
            }

            if (isRelevant) {
                val weight = (yoga.strength.scoreMultiplier * 2).toInt().coerceAtLeast(1)
                score += weight
                relevantYogas.add("${yoga.name} (${yoga.strength.displayName})")
                supporting.add("Active auspicious ${yoga.sanskritName} energizes ${topic.displayName.lowercase()}.")
            }
        }

        // 2. Map Doshas by Topic
        for (dosha in detectedDoshas) {
            val isRelevant = when (topic) {
                LifeTopic.MARRIAGE_RELATIONSHIPS -> dosha.category == DoshaCategory.MANGLIK ||
                        dosha.id == "shrapit_dosha"
                LifeTopic.FINANCE -> dosha.category == DoshaCategory.KEMADRUMA ||
                        dosha.id == "guru_chandal_dosha"
                LifeTopic.CAREER -> dosha.id in setOf("guru_chandal_dosha", "shrapit_dosha", "kaal_sarp_dosha")
                LifeTopic.FAMILY -> dosha.id in setOf("pitra_dosha", "gandmanta_dosha")
                LifeTopic.EDUCATION -> dosha.id in setOf("guru_chandal_dosha", "gandmanta_dosha")
                LifeTopic.PROPERTY_HOME -> dosha.participatingHouses.any { it == 4 }
                LifeTopic.GENERAL_LIFE -> true
            }

            if (isRelevant) {
                relevantDoshas.add("${dosha.name} (${dosha.severity.displayName})")
                if (dosha.isCancelled) {
                    supporting.add("${dosha.sanskritName} is mitigated / cancelled by Parashari planetary combinations.")
                } else {
                    score -= if (dosha.severity.isSevere) 2 else 1
                    caution.add("${dosha.sanskritName} (${dosha.severity.displayName}) suggests mindful attention in ${topic.displayName.lowercase()}.")
                }
            }
        }

        return YogaDoshaEvaluationResult(
            relevantYogas = relevantYogas.distinct(),
            relevantDoshas = relevantDoshas.distinct(),
            score = score,
            supportingFactors = supporting.distinct(),
            cautionFactors = caution.distinct()
        )
    }
}
