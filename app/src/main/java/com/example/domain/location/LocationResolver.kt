package com.example.domain.location

import com.example.domain.models.BirthLocation

interface LocationResolver {
    /**
     * Resolves a free-text place name into a verified BirthLocation.
     */
    suspend fun resolveLocation(query: String): Result<List<BirthLocation>>
}
