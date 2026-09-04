package com.example.domain.location

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.models.BirthLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationRepositoryImpl(context: android.content.Context) : LocationRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("jyotirai_location_prefs", Context.MODE_PRIVATE)

    override suspend fun saveVerifiedLocation(location: BirthLocation) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putFloat("lat", location.latitude.toFloat())
            .putFloat("lon", location.longitude.toFloat())
            .putString("placeName", location.placeName)
            .putFloat("alt", location.altitudeMeters.toFloat())
            .putString("tz", location.timeZoneId)
            .putBoolean("verified", location.isVerified)
            .putString("source", location.source)
            .apply()
    }

    override suspend fun getVerifiedLocation(): BirthLocation? = withContext(Dispatchers.IO) {
        if (!prefs.contains("lat")) return@withContext null
        
        try {
            BirthLocation(
                latitude = prefs.getFloat("lat", 0f).toDouble(),
                longitude = prefs.getFloat("lon", 0f).toDouble(),
                placeName = prefs.getString("placeName", "") ?: "",
                altitudeMeters = prefs.getFloat("alt", 0f).toDouble(),
                timeZoneId = prefs.getString("tz", null),
                isVerified = prefs.getBoolean("verified", false),
                source = prefs.getString("source", "unknown") ?: "unknown"
            )
        } catch (e: Exception) {
            null
        }
    }
}
