package com.example.domain.engine

import com.example.domain.models.AstrologyProfile
import com.example.domain.models.Prediction

/**
 * Architectural boundary for AI Interpretation.
 * 
 * The AI layer ONLY receives structured, deterministic calculation results (AstrologyProfile).
 * It NEVER performs raw astronomical calculations itself.
 */
interface AIAstrologerEngine {
    /**
     * Interprets deterministic astrology data to generate personalized, explainable predictions.
     */
    suspend fun generatePredictions(profile: AstrologyProfile): Result<List<Prediction>>
    
    /**
     * Answers natural language questions based strictly on the verified astrological chart.
     */
    suspend fun answerQuestion(profile: AstrologyProfile, question: String): Result<String>
}
