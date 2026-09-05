package com.example.domain.profile

import com.example.domain.models.UserProfile

const val MAX_PROFILE_SLOTS = 3

interface ProfileRepository {
    suspend fun getAllProfiles(): List<UserProfile>
    suspend fun getProfileById(id: String): UserProfile?
    suspend fun saveProfile(profile: UserProfile): Result<Unit>
    suspend fun deleteProfile(id: String): Result<Unit>
    suspend fun getActiveProfileId(): String?
    suspend fun setActiveProfileId(id: String?): Result<Unit>
    suspend fun getDefaultProfileId(): String?
    suspend fun setDefaultProfileId(id: String?): Result<Unit>
    suspend fun getDefaultProfile(): UserProfile?
    suspend fun getActiveProfile(): UserProfile?
    suspend fun getDefaultProfileForDailyPrediction(): UserProfile?
    suspend fun getDefaultBirthDataForDailyPrediction(): com.example.domain.models.BirthData?
}
