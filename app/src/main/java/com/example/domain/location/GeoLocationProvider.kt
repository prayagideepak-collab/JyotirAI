package com.example.domain.location

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/forecast?current_weather=false")
    suspend fun getTimezoneAndElevation(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}

data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String?,
    val elevation: Double?
)
