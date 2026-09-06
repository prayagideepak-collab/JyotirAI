package com.example.domain.engine.prediction

import com.example.domain.models.*
import java.time.LocalDate

/**
 * Authoritative Phase 7 Deterministic Vedic Prediction Engine for JyotirAI.
 * Synthesizes multi-factor Parashari astrological evidence (Natal Bhava + Dasha Timing + Gochar Transits + Yogas/Doshas)
 * into structured, explainable life-topic predictions without arbitrary percentages or false certainty.
 */
object PredictionEngine {

    /**
     * Calculates the complete prediction snapshot across all life topics for a given profile, Dasha timeline,
     * Gochar transits, and Yoga/Dosha snapshot.
     */
    fun calculatePredictions(
        profile: AstrologyProfile,
        dashaTimeline: DashaTimeline?,
        transits: List<Transit>?,
        yogaDoshaSnapshot: YogaDoshaSnapshot?,
        targetDate: LocalDate = LocalDate.now()
    ): PredictionSnapshot {
        val validation = PredictionResultValidator.validateProfile(profile)
        if (!validation.isValid) {
            val emptyContext = PredictionTimeContext(
                targetDate = targetDate,
                activeMahadasha = "Unknown",
                activeAntardasha = "Unknown",
                transitSnapshotDate = targetDate
            )
            return PredictionSnapshot(
                profileId = profile.birthData.name,
                profileName = profile.birthData.name,
                calculationTimestamp = System.currentTimeMillis(),
                timeContext = emptyContext,
                topicPredictions = emptyMap(),
                overallLifeTrend = PredictionSupportLevel.INSUFFICIENT_DATA,
                keyHighlightSummary = "गणना संभव नहीं: ${validation.reason}"
            )
        }

        val dashaMaha = dashaTimeline?.currentMahadasha?.planet?.lord ?: dashaTimeline?.startingMahadasha?.lord ?: "Unknown"
        val dashaAntar = dashaTimeline?.currentAntardasha?.antardashaLord?.lord ?: dashaMaha

        val timeContext = PredictionTimeContext(
            targetDate = targetDate,
            activeMahadasha = dashaMaha,
            activeAntardasha = dashaAntar,
            mahadashaEndDate = dashaTimeline?.currentMahadasha?.endDate?.toLocalDate(),
            antardashaEndDate = dashaTimeline?.currentAntardasha?.endDate?.toLocalDate(),
            transitSnapshotDate = targetDate
        )

        val topicPredictions = mutableMapOf<LifeTopic, LifeTopicPrediction>()

        for (topic in LifeTopic.entries) {
            // 1. Natal Context
            val natalResult = NatalContextAnalyzer.analyze(
                topic = topic,
                profile = profile,
                dashaLord = dashaMaha,
                antardashaLord = dashaAntar,
                transits = transits
            )

            // 2. Dasha Timing Context
            val dashaResult = DashaContextAnalyzer.analyze(
                topic = topic,
                profile = profile,
                dashaTimeline = dashaTimeline,
                targetDate = targetDate
            )

            // 3. Transit Gochar Context
            val transitResult = TransitContextAnalyzer.analyze(
                topic = topic,
                profile = profile,
                transits = transits
            )

            // 4. Yoga & Dosha Context
            val yogaDoshaResult = YogaDoshaContextAnalyzer.analyze(
                topic = topic,
                yogaDoshaSnapshot = yogaDoshaSnapshot
            )

            // 5. Synthesis
            val prediction = PredictionEvidenceBuilder.buildTopicPrediction(
                topic = topic,
                natalResult = natalResult,
                dashaResult = dashaResult,
                transitResult = transitResult,
                yogaDoshaResult = yogaDoshaResult
            )

            topicPredictions[topic] = prediction
        }

        // Overall life trend
        val averageScore = topicPredictions.values.map { it.supportLevel.scoreValue }.average()
        val overallTrend = when {
            averageScore >= 2.0 -> PredictionSupportLevel.STRONGLY_SUPPORTED
            averageScore in 0.5..1.99 -> PredictionSupportLevel.SUPPORTED
            averageScore in -0.5..0.49 -> PredictionSupportLevel.MIXED_SIGNALS
            else -> PredictionSupportLevel.CHALLENGING
        }

        val dashaResultForHighlight = DashaContextAnalyzer.analyze(
            topic = LifeTopic.GENERAL_LIFE,
            profile = profile,
            dashaTimeline = dashaTimeline,
            targetDate = targetDate
        )

        val highlightSummary = PredictionEvidenceBuilder.buildOverallHighlight(
            topicPredictions = topicPredictions,
            dashaResult = dashaResultForHighlight
        )

        val rawSnapshot = PredictionSnapshot(
            profileId = profile.birthData.name,
            profileName = profile.birthData.name,
            calculationTimestamp = System.currentTimeMillis(),
            timeContext = timeContext,
            topicPredictions = topicPredictions,
            overallLifeTrend = overallTrend,
            keyHighlightSummary = highlightSummary
        )

        return PredictionResultValidator.sanitizeSnapshot(rawSnapshot)
    }
}
