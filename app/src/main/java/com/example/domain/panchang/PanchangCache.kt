package com.example.domain.panchang

import com.example.domain.models.PanchangLocationContext
import com.example.domain.models.PanchangResult
import java.time.LocalDate

/**
 * Isolated, thread-safe LRU Cache for Panchang results.
 * Strictly keys calculations on (Date, Latitude, Longitude, Timezone, EngineVersion)
 * to guarantee no cross-date or cross-location leakage.
 */
class PanchangCache(private val maxEntries: Int = 100) {

    private val cache = object : LinkedHashMap<String, PanchangResult>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PanchangResult>?): Boolean {
            return size > maxEntries
        }
    }

    private val lock = Any()

    fun get(date: LocalDate, location: PanchangLocationContext, engineVersion: String): PanchangResult? {
        val key = buildKey(date, location, engineVersion)
        synchronized(lock) {
            return cache[key]
        }
    }

    fun put(result: PanchangResult) {
        val key = buildKey(result.selectedDate, result.location, result.calculationEngineVersion)
        synchronized(lock) {
            cache[key] = result
        }
    }

    fun clear() {
        synchronized(lock) {
            cache.clear()
        }
    }

    fun size(): Int {
        synchronized(lock) {
            return cache.size
        }
    }

    private fun buildKey(date: LocalDate, location: PanchangLocationContext, engineVersion: String): String {
        val roundedLat = String.format(java.util.Locale.US, "%.4f", location.latitude)
        val roundedLon = String.format(java.util.Locale.US, "%.4f", location.longitude)
        return "${date}_${roundedLat}_${roundedLon}_${location.timeZoneId}_$engineVersion"
    }
}
