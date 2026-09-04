package com.example.domain.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.example.domain.models.BirthLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidLocationResolver(private val context: Context) : LocationResolver {
    
    // Seed with known good presets so tests and common offline usage work without failing on API limits
    private val offlinePresets = listOf(
        BirthLocation(28.6139, 77.2090, "New Delhi", 216.0, "Asia/Kolkata", true, "preset"),
        BirthLocation(19.0760, 72.8777, "Mumbai", 14.0, "Asia/Kolkata", true, "preset"),
        BirthLocation(12.9716, 77.5946, "Bengaluru", 920.0, "Asia/Kolkata", true, "preset"),
        BirthLocation(25.3176, 82.9739, "Varanasi", 81.0, "Asia/Kolkata", true, "preset"),
        BirthLocation(51.5074, -0.1278, "London", 11.0, "Europe/London", true, "preset"),
        BirthLocation(40.7128, -74.0060, "New York", 10.0, "America/New_York", true, "preset"),
        BirthLocation(37.7749, -122.4194, "San Francisco", 16.0, "America/Los_Angeles", true, "preset"),
        BirthLocation(35.6762, 139.6503, "Tokyo", 40.0, "Asia/Tokyo", true, "preset")
    )

    override suspend fun resolveLocation(query: String): Result<List<BirthLocation>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Location query cannot be blank"))
        }

        // 1. Check presets first
        val presetMatch = offlinePresets.filter { it.placeName.equals(query, ignoreCase = true) }
        if (presetMatch.isNotEmpty()) {
            return@withContext Result.success(presetMatch)
        }

        // 2. Use Geocoder
        if (!Geocoder.isPresent()) {
            return@withContext Result.failure(Exception("Geocoder is not present on this device"))
        }

        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val addresses = suspendCoroutine { continuation ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(query, 5) { results ->
                        continuation.resume(results)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val results = geocoder.getFromLocationName(query, 5)
                    continuation.resume(results ?: emptyList())
                }
            }
            
            if (addresses.isEmpty()) {
                return@withContext Result.failure(Exception("Location not found for query: $query"))
            }

            // 3. Resolve timezone and elevation for the best matching addresses
            // To prevent hanging, we only process up to 3 candidates
            val locations = mutableListOf<BirthLocation>()
            for (addr in addresses.take(3)) {
                val parts = listOfNotNull(addr.locality, addr.adminArea, addr.countryName).filter { it.isNotBlank() }
                val name = if (parts.isNotEmpty()) parts.joinToString(", ") else query
                val (timezone, elevation) = OpenMeteoProvider.getTimezoneAndElevation(addr.latitude, addr.longitude)
                
                if (timezone != null) {
                    locations.add(
                        BirthLocation(
                            latitude = addr.latitude,
                            longitude = addr.longitude,
                            placeName = name,
                            altitudeMeters = elevation,
                            timeZoneId = timezone,
                            isVerified = true,
                            source = "geocoder+openmeteo"
                        )
                    )
                }
            }
            
            if (locations.isEmpty()) {
                return@withContext Result.failure(Exception("Could not determine authoritative timezone for location"))
            }
            
            Result.success(locations)
        } catch (e: IOException) {
            Result.failure(Exception("Network or geocoding error: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(Exception("Error resolving location: ${e.message}", e))
        }
    }
}
