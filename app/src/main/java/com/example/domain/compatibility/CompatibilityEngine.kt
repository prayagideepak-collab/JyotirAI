package com.example.domain.compatibility

import com.example.domain.models.CompatibilityResult
import com.example.domain.models.UserProfile

interface CompatibilityEngine {
    suspend fun evaluateCompatibility(
        profileA: UserProfile,
        profileB: UserProfile,
        mode: CompatibilityMode
    ): Result<CompatibilityResult>

    fun invalidateProfile(profileId: String)
}
