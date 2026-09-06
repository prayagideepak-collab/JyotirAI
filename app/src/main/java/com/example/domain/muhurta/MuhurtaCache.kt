package com.example.domain.muhurta

import com.example.domain.models.MuhurtaRequest
import com.example.domain.models.MuhurtaResult
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe LRU-like cache for Muhurta calculations.
 * Ensures zero recalculation for identical search parameters.
 */
class MuhurtaCache(private val maxCapacity: Int = 100) {

    private val cacheMap = ConcurrentHashMap<String, MuhurtaResult>()
    private val keyOrder = java.util.concurrent.ConcurrentLinkedDeque<String>()

    fun get(request: MuhurtaRequest, engineVersion: String): MuhurtaResult? {
        val key = buildKey(request, engineVersion)
        return cacheMap[key]
    }

    fun put(request: MuhurtaRequest, engineVersion: String, result: MuhurtaResult) {
        val key = buildKey(request, engineVersion)
        if (cacheMap.size >= maxCapacity) {
            val oldest = keyOrder.pollFirst()
            if (oldest != null) {
                cacheMap.remove(oldest)
            }
        }
        cacheMap[key] = result
        keyOrder.addLast(key)
    }

    fun clear() {
        cacheMap.clear()
        keyOrder.clear()
    }

    private fun buildKey(request: MuhurtaRequest, engineVersion: String): String {
        val profileId = request.profile?.id ?: "general"
        val timePref = request.preferredTimeSlot.name
        val lat = String.format(java.util.Locale.US, "%.4f", request.location.latitude)
        val lon = String.format(java.util.Locale.US, "%.4f", request.location.longitude)
        val tz = request.location.timeZoneId ?: "UTC"
        return "${request.activityType.name}_${request.startDate}_${request.endDate}_${lat}_${lon}_${tz}_${profileId}_${timePref}_$engineVersion"
    }
}
