package com.example.domain.engine.prediction

import com.example.domain.models.*

/**
 * Deterministic Parashari Evidence and Synthesis Builder for Phase 7 Prediction Engine.
 * Constructs objective, explainable interpretations anchored strictly in classical astrological evidence
 * without generative hallucinations or false certainty.
 */
object PredictionEvidenceBuilder {

    fun buildTopicPrediction(
        topic: LifeTopic,
        natalResult: NatalContextAnalyzer.NatalEvaluationResult,
        dashaResult: DashaContextAnalyzer.DashaEvaluationResult,
        transitResult: TransitContextAnalyzer.TransitEvaluationResult,
        yogaDoshaResult: YogaDoshaContextAnalyzer.YogaDoshaEvaluationResult
    ): LifeTopicPrediction {
        val totalScore = natalResult.score + dashaResult.score + transitResult.score + yogaDoshaResult.score

        val supportLevel = when {
            totalScore >= 6 -> PredictionSupportLevel.STRONGLY_SUPPORTED
            totalScore in 2..5 -> PredictionSupportLevel.SUPPORTED
            totalScore in -1..1 -> PredictionSupportLevel.MIXED_SIGNALS
            else -> PredictionSupportLevel.CHALLENGING
        }

        val trendType = when (supportLevel) {
            PredictionSupportLevel.STRONGLY_SUPPORTED -> PredictionTrendType.POSITIVE_GROWTH
            PredictionSupportLevel.SUPPORTED -> if (dashaResult.isActivatingTopic) PredictionTrendType.POSITIVE_GROWTH else PredictionTrendType.STABILITY
            PredictionSupportLevel.MIXED_SIGNALS -> PredictionTrendType.RESTRUCTURING
            PredictionSupportLevel.CHALLENGING -> PredictionTrendType.CAUTION
            PredictionSupportLevel.LIMITED_DATA -> PredictionTrendType.STABILITY
            PredictionSupportLevel.INSUFFICIENT_DATA -> PredictionTrendType.CAUTION
        }

        val allSupporting = (natalResult.supportingFactors + dashaResult.supportingFactors +
                transitResult.supportingFactors + yogaDoshaResult.supportingFactors).distinct()

        val allCaution = (natalResult.cautionFactors + dashaResult.cautionFactors +
                transitResult.cautionFactors + yogaDoshaResult.cautionFactors).distinct()

        val allNeutral = natalResult.neutralFactors.distinct()

        val synthesis = buildString {
            append("वैदिक विश्लेषण (${topic.displayName}): ")
            when (supportLevel) {
                PredictionSupportLevel.STRONGLY_SUPPORTED -> {
                    append("इस क्षेत्र में ग्रह स्थिति एवं दशा गोचर का प्रबल समर्थन प्राप्त हो रहा है। ")
                    append("जन्म कुण्डली में भाव स्वामियों की स्थिति तथा सक्रिय ${dashaResult.activeMahadasha} महादशा एवं ${dashaResult.activeAntardasha} अंतर्दशा प्रगति के अनुकूल अवसर निर्मित कर रही है।")
                }
                PredictionSupportLevel.SUPPORTED -> {
                    append("सकारात्मक एवं स्थिर प्रभाव दृष्टिगोचर है। ")
                    append("सक्रिय दशा एवं गोचरीय ग्रहों का संरेखण सामान्य प्रगति तथा संतुलन को बढ़ावा देता है।")
                }
                PredictionSupportLevel.MIXED_SIGNALS -> {
                    append("मिश्रित ग्रहों के प्रभाव के कारण संतुलित दृष्टिकोण और नियमित प्रयास आवश्यक हैं। ")
                    append("एक ओर जहाँ कुछ ग्रह सहायता प्रदान कर रहे हैं, वहीं अन्य ग्रह धैर्य एवं सतर्कता की मांग करते हैं।")
                }
                PredictionSupportLevel.CHALLENGING -> {
                    append("वर्तमान समय में अतिरिक्त सावधानी, धैर्य एवं सुविचारित निर्णय लेने की आवश्यकता है। ")
                    append("ग्रहों का गोचर अथवा दशा प्रभाव कुछ अवरोध अथवा पुनर्विचार के संकेत दे रहा है।")
                }
                else -> {
                    append("ज्योतिषीय संकेत सामान्य हैं। कर्म एवं सजगता से कार्यों को आगे बढ़ाएं।")
                }
            }
        }

        return LifeTopicPrediction(
            topic = topic,
            supportLevel = supportLevel,
            trendType = trendType,
            keyPlanets = natalResult.keyPlanets,
            primaryHousesInvolved = natalResult.involvedHouses,
            natalKundliFactors = natalResult.evidenceList,
            dashaTimelineFactors = dashaResult.evidence,
            transitFactors = transitResult.evidence,
            relevantYogas = yogaDoshaResult.relevantYogas,
            relevantDoshas = yogaDoshaResult.relevantDoshas,
            supportingFactors = allSupporting,
            cautionFactors = allCaution,
            neutralFactors = allNeutral,
            classicalSynthesis = synthesis
        )
    }

    fun buildOverallHighlight(
        topicPredictions: Map<LifeTopic, LifeTopicPrediction>,
        dashaResult: DashaContextAnalyzer.DashaEvaluationResult
    ): String {
        val strongestTopic = topicPredictions.values.maxByOrNull { it.supportLevel.scoreValue }
        val challengingTopic = topicPredictions.values.minByOrNull { it.supportLevel.scoreValue }

        return buildString {
            append("सक्रिय समय चक्र: ${dashaResult.activeMahadasha} महादशा / ${dashaResult.activeAntardasha} अंतर्दशा। ")
            strongestTopic?.let {
                if (it.supportLevel == PredictionSupportLevel.STRONGLY_SUPPORTED || it.supportLevel == PredictionSupportLevel.SUPPORTED) {
                    append("सर्वाधिक अनुकूल क्षेत्र: ${it.topic.displayName} (${it.supportLevel.displayName})। ")
                }
            }
            challengingTopic?.let {
                if (it.supportLevel == PredictionSupportLevel.CHALLENGING) {
                    append("विशेष सावधानी अपेक्षित क्षेत्र: ${it.topic.displayName}। ")
                }
            }
            append("पराशर ज्योतिष सिद्धांतों के अनुसार कर्म एवं विवेक से निर्णय लें।")
        }
    }
}
