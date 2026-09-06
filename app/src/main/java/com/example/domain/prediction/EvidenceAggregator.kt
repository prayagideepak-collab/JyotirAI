package com.example.domain.prediction

import com.example.domain.engine.TransitCalculator
import com.example.domain.models.*
import java.util.UUID

/**
 * Deterministic Evidence Aggregator for Phase 8 Periodic Predictions.
 * Synthesizes Phase 7 prediction snapshots with period-specific transit dynamics,
 * Dasha timeline overlaps, and Yoga/Dosha factors into structured, explainable results.
 */
object EvidenceAggregator {

    fun aggregate(
        profile: AstrologyProfile,
        periodType: PredictionPeriodType,
        timeContext: PeriodTimeContext,
        phase7Snapshot: PredictionSnapshot,
        transitSnapshot: TransitSnapshot?,
        yogaDoshaSnapshot: YogaDoshaSnapshot?
    ): PeriodicPredictionResult {
        val profileId = profile.birthData.name
        val profileName = profile.birthData.name

        // 1. Build Periodic Transit Evidence
        val transitPositions = transitSnapshot?.positions ?: emptyList()
        val retrogradePlanets = transitPositions.filter { it.isRetrograde }.map { it.planet }
        val supportiveTransits = mutableListOf<String>()
        val challengingTransits = mutableListOf<String>()

        transitPositions.forEach { tp ->
            val hFromMoon = tp.houseFromMoon
            val hFromLagna = tp.houseFromLagna
            if (hFromMoon != null) {
                val ordinal = TransitCalculator.getHouseOrdinal(hFromMoon)
                if (hFromMoon in listOf(3, 6, 10, 11)) {
                    supportiveTransits.add("${tp.planet} in $ordinal House from Moon (${tp.sign})")
                } else if (hFromMoon in listOf(8, 12)) {
                    challengingTransits.add("${tp.planet} in $ordinal House from Moon (${tp.sign})")
                }
            }
        }

        val transitSummary = when (periodType) {
            PredictionPeriodType.DAILY -> "Daily planetary Gochar highlights Moon in ${transitPositions.firstOrNull { it.planet.equals("Moon", ignoreCase = true) }?.sign ?: "active sign"}" +
                    if (retrogradePlanets.isNotEmpty()) ", with ${retrogradePlanets.joinToString(", ")} retrograde." else "."
            PredictionPeriodType.MONTHLY -> "Monthly Gochar indicates key planetary activations with Jupiter in ${transitPositions.firstOrNull { it.planet.equals("Jupiter", ignoreCase = true) }?.sign ?: "sign"}" +
                    " and Saturn in ${transitPositions.firstOrNull { it.planet.equals("Saturn", ignoreCase = true) }?.sign ?: "sign"}."
            PredictionPeriodType.YEARLY -> "Annual Gochar dynamics governed by slow-moving grahas: Jupiter in ${transitPositions.firstOrNull { it.planet.equals("Jupiter", ignoreCase = true) }?.sign ?: "sign"}," +
                    " Saturn in ${transitPositions.firstOrNull { it.planet.equals("Saturn", ignoreCase = true) }?.sign ?: "sign"}, and Rahu/Ketu axis."
        }

        val periodicTransitEvidence = PeriodicTransitEvidence(
            majorPlanets = phase7Snapshot.topicPredictions.values.flatMap { it.keyPlanets }.distinctBy { it.planetName },
            retrogradePlanets = retrogradePlanets,
            supportiveTransits = supportiveTransits,
            challengingTransits = challengingTransits,
            summary = transitSummary
        )

        // 2. Build Periodic Dasha Evidence
        val isTransitioning = timeContext.periodTransitions.isNotEmpty()
        val dashaSummary = when (periodType) {
            PredictionPeriodType.DAILY -> "Operating under ${timeContext.activeMahadasha} Mahadasha and ${timeContext.activeAntardasha} Antardasha."
            PredictionPeriodType.MONTHLY -> if (isTransitioning) {
                "Monthly Dasha window encompasses a sub-period transition: ${timeContext.periodTransitions.first().description}."
            } else {
                "Monthly Dasha continues stably under ${timeContext.activeMahadasha}-${timeContext.activeAntardasha}."
            }
            PredictionPeriodType.YEARLY -> if (isTransitioning) {
                "Year ${timeContext.targetYear} features ${timeContext.periodTransitions.size} key planetary period transition(s), notably ${timeContext.periodTransitions.first().description}."
            } else {
                "Year ${timeContext.targetYear} unfolds under sustained ${timeContext.activeMahadasha} Mahadasha and ${timeContext.activeAntardasha} Antardasha."
            }
        }

        val periodicDashaEvidence = PeriodicDashaEvidence(
            mahadashaLord = timeContext.activeMahadasha,
            antardashaLord = timeContext.activeAntardasha,
            isTransitioning = isTransitioning,
            dashaSignification = "Vimshottari activation governing primary karmic manifestations for the period.",
            summary = dashaSummary
        )

        // 3. Build Topic Predictions adapted to the period timeframe
        val topicPredictions = mutableMapOf<LifeTopic, PeriodicTopicPrediction>()

        for (topic in LifeTopic.entries) {
            val phase7Topic = phase7Snapshot.topicPredictions[topic]
            val supportLevel = phase7Topic?.supportLevel ?: PredictionSupportLevel.MIXED_SIGNALS
            val trendType = phase7Topic?.trendType ?: PredictionTrendType.STABILITY

            val periodSynthesis = formatTopicSynthesis(
                periodType = periodType,
                topic = topic,
                supportLevel = supportLevel,
                trendType = trendType,
                dashaLord = timeContext.activeMahadasha,
                antardashaLord = timeContext.activeAntardasha,
                baseSynthesis = phase7Topic?.classicalSynthesis ?: ""
            )

            val timingGuidance = when (periodType) {
                PredictionPeriodType.DAILY -> "Optimal execution during morning hours or Abhijit window; exercise measured speech during Rahukaal."
                PredictionPeriodType.MONTHLY -> if (supportLevel == PredictionSupportLevel.STRONGLY_SUPPORTED || supportLevel == PredictionSupportLevel.SUPPORTED) {
                    "Favorable lunar cycles during Shukla Paksha present ideal windows for initiating new steps in this domain."
                } else {
                    "Middle of the month suggests consolidation and review before committing to major irreversible decisions."
                }
                PredictionPeriodType.YEARLY -> if (isTransitioning) {
                    "Target transitions in the second half of the year for major directional adjustments."
                } else {
                    "Consistent diligence across Q2 and Q3 yields the most balanced progress."
                }
            }

            topicPredictions[topic] = PeriodicTopicPrediction(
                topic = topic,
                supportLevel = supportLevel,
                trendType = trendType,
                primaryHousesInvolved = phase7Topic?.primaryHousesInvolved ?: topic.primaryHouses,
                keyPlanets = phase7Topic?.keyPlanets ?: emptyList(),
                synthesis = periodSynthesis,
                supportingFactors = phase7Topic?.supportingFactors ?: emptyList(),
                cautionFactors = phase7Topic?.cautionFactors ?: emptyList(),
                timingGuidance = timingGuidance
            )
        }

        // 4. Extract Overall Themes
        val allSupporting = phase7Snapshot.topicPredictions.values.flatMap { it.supportingFactors }.distinct()
        val allCaution = phase7Snapshot.topicPredictions.values.flatMap { it.cautionFactors }.distinct()

        val supportingThemes = if (allSupporting.isNotEmpty()) {
            allSupporting.take(5)
        } else {
            listOf("Benefic planetary placements in natal chart", "Supportive Dasha alignment")
        }

        val cautionThemes = if (allCaution.isNotEmpty()) {
            allCaution.take(5)
        } else {
            listOf("Avoid hasty assumptions during unfamiliar planetary hours", "Maintain balanced deliberation")
        }

        // 5. Yogas & Doshas Evidence
        val yogaEvidence = yogaDoshaSnapshot?.detectedYogas?.map { "${it.name}: ${it.evidence}" } ?: emptyList()
        val doshaEvidence = yogaDoshaSnapshot?.detectedDoshas?.map { "${it.name}: ${it.evidence}" } ?: emptyList()

        // 6. Overall Summary
        val periodHeader = when (periodType) {
            PredictionPeriodType.DAILY -> "Daily Vedic Horizon for ${timeContext.targetDate}"
            PredictionPeriodType.MONTHLY -> "Monthly Vedic Horizon for ${timeContext.targetMonth}/${timeContext.targetYear}"
            PredictionPeriodType.YEARLY -> "Annual Vedic Horizon for Year ${timeContext.targetYear}"
        }

        val overallSummary = "$periodHeader: Operating under ${timeContext.activeMahadasha}-${timeContext.activeAntardasha} Dasha. " +
                "${phase7Snapshot.keyHighlightSummary} " +
                if (isTransitioning) "Active sub-period changes require adaptive planning." else "Planetary energies provide steady continuity."

        val resultId = "${profileId}_${periodType.code}_${timeContext.targetYear}_${timeContext.targetMonth ?: 0}_${timeContext.targetDay ?: 0}"

        return PeriodicPredictionResult(
            id = resultId,
            profileId = profileId,
            profileName = profileName,
            predictionType = periodType,
            timeContext = timeContext,
            calculationTimestamp = System.currentTimeMillis(),
            state = PeriodicPredictionState.SUCCESS,
            overallSupportLevel = phase7Snapshot.overallLifeTrend,
            overallTrend = if (phase7Snapshot.overallLifeTrend == PredictionSupportLevel.STRONGLY_SUPPORTED) PredictionTrendType.POSITIVE_GROWTH
                else if (phase7Snapshot.overallLifeTrend == PredictionSupportLevel.CHALLENGING) PredictionTrendType.CAUTION
                else PredictionTrendType.STABILITY,
            overallSummary = overallSummary,
            topicPredictions = topicPredictions,
            supportingThemes = supportingThemes,
            cautionThemes = cautionThemes,
            importantPeriodChanges = timeContext.periodTransitions,
            dashaEvidence = periodicDashaEvidence,
            transitEvidence = periodicTransitEvidence,
            yogaEvidence = yogaEvidence,
            doshaEvidence = doshaEvidence
        )
    }

    private fun formatTopicSynthesis(
        periodType: PredictionPeriodType,
        topic: LifeTopic,
        supportLevel: PredictionSupportLevel,
        trendType: PredictionTrendType,
        dashaLord: String,
        antardashaLord: String,
        baseSynthesis: String
    ): String {
        val prefix = when (periodType) {
            PredictionPeriodType.DAILY -> "Daily Outlook: "
            PredictionPeriodType.MONTHLY -> "Monthly Trend: "
            PredictionPeriodType.YEARLY -> "Annual Projection: "
        }

        return if (baseSynthesis.isNotBlank()) {
            "$prefix$baseSynthesis"
        } else {
            "$prefix${topic.displayName} is evaluated as ${supportLevel.displayName} under ${dashaLord}-${antardashaLord} Dasha influence."
        }
    }
}
