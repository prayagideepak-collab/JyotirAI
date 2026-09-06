package com.example.domain.muhurta

import com.example.domain.models.MuhurtaCandidateWindow
import com.example.domain.models.MuhurtaEvaluationState
import com.example.domain.models.MuhurtaResult
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Creates natural, grammatically refined Hindi summaries and TTS narration scripts
 * for Muhurta evaluations (Phase 10 & Phase 12 integration).
 */
object MuhurtaHindiPresenter {

    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

    fun formatSpeechNarration(result: MuhurtaResult): String {
        return buildString {
            append("नमस्ते। ${result.activityType.hindiName} के लिए मुहूर्त विश्लेषण। ")
            append("स्थान: ${result.location.placeName}। ")

            val best = result.bestWindow
            if (best != null && best.evaluationState != MuhurtaEvaluationState.CAUTION) {
                append("सर्वश्रेष्ठ अनुशंसित समय: ")
                append("${best.sanskritName ?: best.name}, ")
                append("समय: ${best.startTime.format(TIME_FORMATTER)} से ${best.endTime.format(TIME_FORMATTER)} तक। ")
                append("यह समय ${best.evaluationState.hindiLabel} श्रेणी में आता है। ")
            } else {
                append("वर्तमान चयनित अवधि में सभी समय अंतरालों में कुछ सावधानी कारक उपस्थित हैं। ")
            }

            if (result.overallSupportingFactors.isNotEmpty()) {
                val topSupporting = result.overallSupportingFactors.first()
                append("मुख्य अनुकूल कारक: ${topSupporting.title}। ")
            }

            if (result.overallCautionFactors.isNotEmpty()) {
                val topCaution = result.overallCautionFactors.first()
                append("सावधानी: ${topCaution.title}। ")
            }

            if (best?.personalBalaContext != null) {
                val pb = best.personalBalaContext
                append("व्यक्तिगत तारा बल: ${pb.taraName}, ")
                append("तथा चन्द्र बल: ${pb.chandraBalaHouse}वां भाव। ")
            }

            append("शुभ कार्य की सफलता हेतु संकल्पपूर्वक आरम्भ करें।")
        }
    }

    fun formatWindowSummaryHindi(window: MuhurtaCandidateWindow): String {
        return buildString {
            append("${window.sanskritName ?: window.name}: ")
            append("${window.startTime.format(TIME_FORMATTER)} - ${window.endTime.format(TIME_FORMATTER)} ")
            append("(${window.evaluationState.hindiLabel})")
        }
    }
}
