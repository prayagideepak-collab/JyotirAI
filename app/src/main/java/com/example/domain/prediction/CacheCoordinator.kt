package com.example.domain.prediction

import com.example.domain.models.PeriodTimeContext
import com.example.domain.models.PeriodicPredictionResult
import com.example.domain.models.PredictionPeriodType
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe memory cache coordinator for Phase 8 predictions.
 * Enforces strict profile isolation and guarantees no cross-profile cache leakage.
 */
class CacheCoordinator {

    private val cache = ConcurrentHashMap<String, PeriodicPredictionResult>()

    /**
     * Builds a unique, deterministic, isolated cache key.
     */
    fun buildKey(profileId: String, periodType: PredictionPeriodType, timeContext: PeriodTimeContext): String {
        return buildKey(
            profileId = profileId,
            periodType = periodType,
            year = timeContext.targetYear,
            month = timeContext.targetMonth ?: 0,
            day = timeContext.targetDay ?: 0
        )
    }

    fun buildKey(profileId: String, periodType: PredictionPeriodType, year: Int, month: Int = 0, day: Int = 0): String {
        return "${profileId.trim()}_${periodType.code}_${year}_${month}_${day}"
    }

    fun get(key: String): PeriodicPredictionResult? {
        return cache[key]
    }

    fun put(key: String, result: PeriodicPredictionResult) {
        cache[key] = result
    }

    fun clearForProfile(profileId: String) {
        val prefix = "${profileId.trim()}_"
        val keysToRemove = cache.keys().toList().filter { it.startsWith(prefix) }
        keysToRemove.forEach { cache.remove(it) }
    }

    fun clearAll() {
        cache.clear()
    }

    fun size(): Int = cache.size
}
