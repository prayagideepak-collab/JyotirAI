package com.example.domain.location

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.models.BirthLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationRepositoryImpl(context: Context) : LocationRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("jyotirai_location_prefs", Context.MODE_PRIVATE)

    override suspend fun saveVerifiedLocation(location: BirthLocation) = withContext(Dispatchers.IO) {
        val editor = prefs.edit()
            .putLong("lat_bits", location.latitude.toRawBits())
            .putLong("lon_bits", location.longitude.toRawBits())
            .putString("placeName", location.placeName)
            .putString("tz", location.timeZoneId)
            .putBoolean("verified", location.isVerified)
            .putString("source", location.source)
        
        if (location.altitudeMeters != null) {
            editor.putLong("alt_bits", location.altitudeMeters.toRawBits())
            editor.putBoolean("has_alt", true)
        } else {
            editor.remove("alt_bits")
            editor.putBoolean("has_alt", false)
        }
        editor.apply()
    }

    override suspend fun getVerifiedLocation(): BirthLocation? = withContext(Dispatchers.IO) {
        if (!prefs.contains("lat_bits")) return@withContext null
        
        try {
            val altitudeMeters = if (prefs.getBoolean("has_alt", false)) {
                Double.fromBits(prefs.getLong("alt_bits", 0L))
            } else {
                null
            }

            BirthLocation(
                latitude = Double.fromBits(prefs.getLong("lat_bits", 0L)),
                longitude = Double.fromBits(prefs.getLong("lon_bits", 0L)),
                placeName = prefs.getString("placeName", "") ?: "",
                altitudeMeters = altitudeMeters,
                timeZoneId = prefs.getString("tz", null),
                isVerified = prefs.getBoolean("verified", false),
                source = prefs.getString("source", "unknown") ?: "unknown"
            )
        } catch (e: Exception) {
            null
        }
    }
}
