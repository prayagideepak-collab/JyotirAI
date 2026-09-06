package com.example.domain.engine.yogadosha

import com.example.domain.models.*

/**
 * Deterministic Evidence and Natural Explanation Builder for Phase 6 Yoga and Dosha Analysis.
 * Strictly separates calculation rules from explanation layer and presentation formatting.
 */
object EvidenceBuilder {

    fun buildYogaEvidence(
        ruleName: String,
        participatingPlanets: List<PlanetPosition>,
        participatingHouses: List<Int>,
        details: String
    ): String {
        val planetInfo = participatingPlanets.joinToString(", ") { p ->
            "${p.planet} in ${p.sign} (House ${p.house}, ${p.dignity.displayName})"
        }
        val houseInfo = if (participatingHouses.isNotEmpty()) "Houses involved: ${participatingHouses.joinToString(", ")}." else ""
        return buildString {
            if (planetInfo.isNotBlank()) append("Planetary Configuration: $planetInfo. ")
            if (houseInfo.isNotBlank()) append("$houseInfo ")
            append(details)
        }.trim()
    }

    fun buildDoshaEvidence(
        participatingPlanets: List<PlanetPosition>,
        participatingHouses: List<Int>,
        details: String,
        cancellations: List<String> = emptyList()
    ): String {
        val planetInfo = participatingPlanets.joinToString(", ") { p ->
            "${p.planet} in ${p.sign} (House ${p.house}, ${p.dignity.displayName})"
        }
        val houseInfo = if (participatingHouses.isNotEmpty()) "Houses involved: ${participatingHouses.joinToString(", ")}." else ""
        return buildString {
            if (planetInfo.isNotBlank()) append("Planetary Alignment: $planetInfo. ")
            if (houseInfo.isNotBlank()) append("$houseInfo ")
            append(details)
            if (cancellations.isNotEmpty()) {
                append(" Classical Mitigations/Cancellations: ")
                append(cancellations.joinToString("; "))
                append(".")
            }
        }.trim()
    }

    fun buildHindiSummary(
        detectedYogas: List<YogaAnalysisResult>,
        detectedDoshas: List<DoshaAnalysisResult>,
        dominantYoga: YogaAnalysisResult?
    ): String = buildString {
        append("कुण्डली में ${detectedYogas.size} मुख्य शास्त्रीय शुभ योग एवं ${detectedDoshas.size} दोष कारक पाए गए। ")
        dominantYoga?.let {
            append("सर्वाधिक प्रभावी योग: ${it.sanskritName} (${it.strength.displayName})। ")
        }
        val activeDoshas = detectedDoshas.filter { !it.isCancelled }
        if (activeDoshas.isEmpty()) {
            append("कुण्डली में कोई तीव्र अनिष्फल दोष सक्रिय नहीं है।")
        } else {
            val names = activeDoshas.joinToString(", ") { it.sanskritName }
            append("सक्रिय दोष विचार: $names।")
        }
    }
}
