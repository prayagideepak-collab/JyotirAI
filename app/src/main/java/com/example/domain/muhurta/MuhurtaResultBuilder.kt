package com.example.domain.muhurta

import com.example.domain.models.*
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Builds immutable MuhurtaResult objects after all dates and windows are evaluated.
 */
object MuhurtaResultBuilder {

    fun build(
        request: MuhurtaRequest,
        candidateWindows: List<MuhurtaCandidateWindow>,
        locationContext: PanchangLocationContext,
        calculationTimestamp: ZonedDateTime,
        metadata: CalculationMetadata
    ): MuhurtaResult {
        val rankedWindows = CandidateWindowRanker.rank(candidateWindows)
        val bestWindow = CandidateWindowRanker.findBestWindow(rankedWindows)

        val resultType = if (request.profile != null) {
            MuhurtaResultType.PERSONALIZED_MUHURTA
        } else {
            MuhurtaResultType.GENERAL_MUHURTA
        }

        // Aggregate unique overall supporting and caution factors
        val allSupporting = rankedWindows.flatMap { it.supportingFactors }
            .distinctBy { it.title }
        val allCaution = rankedWindows.flatMap { it.cautionFactors }
            .distinctBy { it.title }
        val allRules = rankedWindows.flatMap { it.ruleEvidence }
            .distinctBy { it.ruleId }

        val preliminaryResult = MuhurtaResult(
            id = UUID.randomUUID().toString(),
            activityType = request.activityType,
            resultType = resultType,
            startDate = request.startDate,
            endDate = request.endDate,
            location = locationContext,
            calculationTimestamp = calculationTimestamp,
            candidateWindows = rankedWindows,
            bestWindow = bestWindow,
            overallSupportingFactors = allSupporting,
            overallCautionFactors = allCaution,
            ruleEvaluations = allRules,
            resultState = if (rankedWindows.isNotEmpty()) MuhurtaResultState.SUCCESS else MuhurtaResultState.INSUFFICIENT_DATA,
            metadata = metadata
        )

        val validationLimitations = MuhurtaResultValidator.validate(preliminaryResult)
        return preliminaryResult.copy(calculationLimitations = validationLimitations)
    }
}
