package com.example.domain.muhurta

import com.example.domain.models.MuhurtaCandidateWindow
import com.example.domain.models.MuhurtaResult
import com.example.domain.models.MuhurtaResultState

/**
 * Validates constructed Muhurta results for structural integrity,
 * correct boundary ordering, and anomaly reporting.
 */
object MuhurtaResultValidator {

    fun validate(result: MuhurtaResult): List<String> {
        val limitations = mutableListOf<String>()

        if (result.candidateWindows.isEmpty()) {
            limitations.add("No candidate time windows found within the requested parameters.")
        }

        if (result.bestWindow == null && result.candidateWindows.isNotEmpty()) {
            limitations.add("All candidate time windows contain cautionary factors; proceed with remedial measures.")
        }

        result.candidateWindows.forEach { window ->
            if (window.endTime.isBefore(window.startTime)) {
                limitations.add("Time window anomaly detected in ${window.name}: end time precedes start time.")
            }
        }

        return limitations
    }
}
