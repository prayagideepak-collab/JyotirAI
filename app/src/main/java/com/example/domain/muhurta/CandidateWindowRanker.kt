package com.example.domain.muhurta

import com.example.domain.models.MuhurtaCandidateWindow

/**
 * Ranks candidate Muhurta windows deterministically based on rank tiers,
 * computed scores, and chronological time boundaries.
 */
object CandidateWindowRanker {

    fun rank(windows: List<MuhurtaCandidateWindow>): List<MuhurtaCandidateWindow> {
        return windows.sortedWith(
            compareBy<MuhurtaCandidateWindow> { it.rankTier.rankWeight }
                .thenByDescending { it.score }
                .thenBy { it.startTime }
        )
    }

    fun findBestWindow(windows: List<MuhurtaCandidateWindow>): MuhurtaCandidateWindow? {
        val ranked = rank(windows)
        // Prefer the highest tier non-caution window
        return ranked.firstOrNull { it.evaluationState != com.example.domain.models.MuhurtaEvaluationState.CAUTION }
            ?: ranked.firstOrNull()
    }
}
