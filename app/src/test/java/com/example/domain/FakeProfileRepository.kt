package com.example.domain

import com.example.domain.models.UserProfile
import com.example.domain.profile.MAX_PROFILE_SLOTS
import com.example.domain.profile.ProfileRepository

class FakeProfileRepository : ProfileRepository {

    private val profiles = mutableListOf<UserProfile>()
    private var activeId: String? = null
    private var defaultId: String? = null

    override suspend fun getAllProfiles(): List<UserProfile> = profiles.toList()

    override suspend fun getProfileById(id: String): UserProfile? = profiles.find { it.id == id }

    override suspend fun saveProfile(profile: UserProfile): Result<Unit> {
        val existingIndex = profiles.indexOfFirst { it.id == profile.id }
        if (existingIndex >= 0) {
            profiles[existingIndex] = profile
        } else {
            if (profiles.size >= MAX_PROFILE_SLOTS) {
                return Result.failure(IllegalStateException("Maximum limit of $MAX_PROFILE_SLOTS profiles reached."))
            }
            profiles.add(profile)
        }

        if (defaultId == null || !profiles.any { it.id == defaultId }) {
            defaultId = profile.id
        }
        if (activeId == null || !profiles.any { it.id == activeId }) {
            activeId = profile.id
        }

        return Result.success(Unit)
    }

    override suspend fun deleteProfile(id: String): Result<Unit> {
        profiles.removeAll { it.id == id }
        if (defaultId == id) {
            defaultId = profiles.firstOrNull()?.id
        }
        if (activeId == id) {
            activeId = defaultId ?: profiles.firstOrNull()?.id
        }
        return Result.success(Unit)
    }

    override suspend fun getActiveProfileId(): String? {
        return if (activeId != null && profiles.any { it.id == activeId }) {
            activeId
        } else {
            getDefaultProfileId()
        }
    }

    override suspend fun setActiveProfileId(id: String?): Result<Unit> {
        if (id == null) {
            activeId = null
            return Result.success(Unit)
        }
        if (!profiles.any { it.id == id }) {
            return Result.failure(IllegalArgumentException("Profile $id not found"))
        }
        activeId = id
        return Result.success(Unit)
    }

    override suspend fun getDefaultProfileId(): String? {
        return if (defaultId != null && profiles.any { it.id == defaultId }) {
            defaultId
        } else if (profiles.isNotEmpty()) {
            defaultId = profiles.first().id
            defaultId
        } else {
            null
        }
    }

    override suspend fun setDefaultProfileId(id: String?): Result<Unit> {
        if (id == null) {
            defaultId = null
            return Result.success(Unit)
        }
        if (!profiles.any { it.id == id }) {
            return Result.failure(IllegalArgumentException("Profile $id not found"))
        }
        defaultId = id
        return Result.success(Unit)
    }

    override suspend fun getDefaultProfile(): UserProfile? {
        val defId = getDefaultProfileId() ?: return null
        return getProfileById(defId)
    }

    override suspend fun getActiveProfile(): UserProfile? {
        val actId = getActiveProfileId() ?: return null
        return getProfileById(actId)
    }

    override suspend fun getDefaultProfileForDailyPrediction(): UserProfile? = getDefaultProfile()

    override suspend fun getDefaultBirthDataForDailyPrediction(): com.example.domain.models.BirthData? = getDefaultProfile()?.birthData
}
