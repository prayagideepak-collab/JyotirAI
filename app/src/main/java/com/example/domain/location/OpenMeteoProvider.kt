package com.example.domain.location

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OpenMeteoProvider {
    suspend fun getTimezoneAndElevation(lat: Double, lon: Double): Pair<String?, Double?> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=false&timezone=auto"
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val timezone = if (json.has("timezone")) json.getString("timezone") else null
                val elevation = if (json.has("elevation")) json.getDouble("elevation") else null
                Pair(timezone, elevation)
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            Pair(null, null)
        } finally {
            connection?.disconnect()
        }
    }
}
