package com.example.domain.location
import com.example.domain.models.BirthLocation
interface LocationRepository {
    suspend fun saveVerifiedLocation(location: BirthLocation)
    suspend fun getVerifiedLocation(): BirthLocation?
}
