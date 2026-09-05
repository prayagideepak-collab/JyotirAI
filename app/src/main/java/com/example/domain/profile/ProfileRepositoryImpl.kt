package com.example.domain.profile

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.models.BirthData
import com.example.domain.models.BirthLocation
import com.example.domain.models.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ProfileRepositoryImpl(context: Context) : ProfileRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("jyotirai_profiles_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val SCHEMA_VERSION = 1
        private const val KEY_SCHEMA_VERSION = "schema_version"
        private const val KEY_PROFILE_IDS = "profile_ids"
        private const val KEY_DEFAULT_PROFILE_ID = "default_profile_id"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
        private const val PREFIX_PROFILE = "profile_"
    }

    init {
        val currentVersion = prefs.getInt(KEY_SCHEMA_VERSION, 0)
        if (currentVersion < SCHEMA_VERSION) {
            prefs.edit().putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION).apply()
        }
    }

    override suspend fun getAllProfiles(): List<UserProfile> = withContext(Dispatchers.IO) {
        val idList = getProfileIdsInternal()
        val validProfiles = mutableListOf<UserProfile>()

        for (id in idList) {
            val profile = readProfileInternal(id)
            if (profile != null) {
                validProfiles.add(profile)
            }
        }
        validProfiles
    }

    override suspend fun getProfileById(id: String): UserProfile? = withContext(Dispatchers.IO) {
        readProfileInternal(id)
    }

    override suspend fun saveProfile(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingIds = getProfileIdsInternal().toMutableList()
            val isNewProfile = !existingIds.contains(profile.id)

            if (isNewProfile && existingIds.size >= MAX_PROFILE_SLOTS) {
                return@withContext Result.failure(
                    IllegalStateException("Maximum limit of $MAX_PROFILE_SLOTS profiles reached. Please edit or delete an existing profile.")
                )
            }

            val json = serializeProfile(profile)
            val editor = prefs.edit()
            editor.putString(PREFIX_PROFILE + profile.id, json)

            if (isNewProfile) {
                existingIds.add(profile.id)
                editor.putString(KEY_PROFILE_IDS, JSONArray(existingIds).toString())
            }

            // If this is the first profile, automatically designate as default and active
            val currentDefault = prefs.getString(KEY_DEFAULT_PROFILE_ID, null)
            if (currentDefault.isNullOrBlank() || !existingIds.contains(currentDefault)) {
                editor.putString(KEY_DEFAULT_PROFILE_ID, profile.id)
            }

            val currentActive = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)
            if (currentActive.isNullOrBlank() || !existingIds.contains(currentActive)) {
                editor.putString(KEY_ACTIVE_PROFILE_ID, profile.id)
            }

            editor.apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProfile(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingIds = getProfileIdsInternal().toMutableList()
            if (!existingIds.contains(id)) {
                return@withContext Result.success(Unit)
            }

            existingIds.remove(id)
            val editor = prefs.edit()
            editor.remove(PREFIX_PROFILE + id)
            editor.putString(KEY_PROFILE_IDS, JSONArray(existingIds).toString())

            // Repair default profile if deleted
            val currentDefault = prefs.getString(KEY_DEFAULT_PROFILE_ID, null)
            if (currentDefault == id) {
                val newDefault = existingIds.firstOrNull()
                if (newDefault != null) {
                    editor.putString(KEY_DEFAULT_PROFILE_ID, newDefault)
                } else {
                    editor.remove(KEY_DEFAULT_PROFILE_ID)
                }
            }

            // Repair active profile if deleted
            val currentActive = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)
            if (currentActive == id) {
                val newActive = existingIds.firstOrNull()
                if (newActive != null) {
                    editor.putString(KEY_ACTIVE_PROFILE_ID, newActive)
                } else {
                    editor.remove(KEY_ACTIVE_PROFILE_ID)
                }
            }

            editor.apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveProfileId(): String? = withContext(Dispatchers.IO) {
        val activeId = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)
        val validIds = getProfileIdsInternal()
        if (activeId != null && validIds.contains(activeId)) {
            activeId
        } else {
            getDefaultProfileId()
        }
    }

    override suspend fun setActiveProfileId(id: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (id == null) {
                prefs.edit().remove(KEY_ACTIVE_PROFILE_ID).apply()
                return@withContext Result.success(Unit)
            }
            val validIds = getProfileIdsInternal()
            if (!validIds.contains(id)) {
                return@withContext Result.failure(IllegalArgumentException("Profile ID $id does not exist"))
            }
            prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, id).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDefaultProfileId(): String? = withContext(Dispatchers.IO) {
        val defId = prefs.getString(KEY_DEFAULT_PROFILE_ID, null)
        val validIds = getProfileIdsInternal()
        if (defId != null && validIds.contains(defId)) {
            defId
        } else if (validIds.isNotEmpty()) {
            val fallback = validIds.first()
            prefs.edit().putString(KEY_DEFAULT_PROFILE_ID, fallback).apply()
            fallback
        } else {
            null
        }
    }

    override suspend fun setDefaultProfileId(id: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (id == null) {
                prefs.edit().remove(KEY_DEFAULT_PROFILE_ID).apply()
                return@withContext Result.success(Unit)
            }
            val validIds = getProfileIdsInternal()
            if (!validIds.contains(id)) {
                return@withContext Result.failure(IllegalArgumentException("Profile ID $id does not exist"))
            }
            prefs.edit().putString(KEY_DEFAULT_PROFILE_ID, id).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDefaultProfile(): UserProfile? = withContext(Dispatchers.IO) {
        val defId = getDefaultProfileId() ?: return@withContext null
        readProfileInternal(defId)
    }

    override suspend fun getActiveProfile(): UserProfile? = withContext(Dispatchers.IO) {
        val activeId = getActiveProfileId() ?: return@withContext null
        readProfileInternal(activeId)
    }

    override suspend fun getDefaultProfileForDailyPrediction(): UserProfile? = getDefaultProfile()

    override suspend fun getDefaultBirthDataForDailyPrediction(): BirthData? = getDefaultProfile()?.birthData

    private fun getProfileIdsInternal(): List<String> {
        val jsonStr = prefs.getString(KEY_PROFILE_IDS, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val id = array.optString(i)
                if (!id.isNullOrBlank()) {
                    list.add(id)
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun readProfileInternal(id: String): UserProfile? {
        val jsonStr = prefs.getString(PREFIX_PROFILE + id, null) ?: return null
        return try {
            deserializeProfile(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    private fun serializeProfile(profile: UserProfile): String {
        val root = JSONObject()
        root.put("id", profile.id)
        root.put("name", profile.birthData.name)
        root.put("year", profile.birthData.date.year)
        root.put("month", profile.birthData.date.monthValue)
        root.put("day", profile.birthData.date.dayOfMonth)
        root.put("hour", profile.birthData.time.hour)
        root.put("minute", profile.birthData.time.minute)
        root.put("second", profile.birthData.time.second)
        root.put("nano", profile.birthData.time.nano)
        root.put("placeName", profile.birthData.location.placeName)
        root.put("latBits", profile.birthData.location.latitude.toRawBits())
        root.put("lonBits", profile.birthData.location.longitude.toRawBits())
        root.put("tz", profile.birthData.location.timeZoneId ?: profile.birthData.timeZone.id)
        root.put("isVerified", profile.birthData.location.isVerified)
        root.put("source", profile.birthData.location.source)
        root.put("createdAt", profile.createdAt)

        if (profile.birthData.location.altitudeMeters != null) {
            root.put("hasAlt", true)
            root.put("altBits", profile.birthData.location.altitudeMeters.toRawBits())
        } else {
            root.put("hasAlt", false)
        }

        return root.toString()
    }

    private fun deserializeProfile(jsonStr: String): UserProfile {
        val root = JSONObject(jsonStr)
        val id = root.getString("id")
        val name = root.getString("name")
        val year = root.getInt("year")
        val month = root.getInt("month")
        val day = root.getInt("day")
        val hour = root.getInt("hour")
        val minute = root.getInt("minute")
        val second = root.optInt("second", 0)
        val nano = root.optInt("nano", 0)
        val placeName = root.getString("placeName")

        val latitude = if (root.has("latBits")) {
            Double.fromBits(root.getLong("latBits"))
        } else {
            root.getDouble("latitude")
        }

        val longitude = if (root.has("lonBits")) {
            Double.fromBits(root.getLong("lonBits"))
        } else {
            root.getDouble("longitude")
        }

        val altitudeMeters = if (root.optBoolean("hasAlt", false)) {
            if (root.has("altBits")) Double.fromBits(root.getLong("altBits")) else root.optDouble("altitudeMeters")
        } else {
            null
        }

        val tzId = root.optString("tz", "Asia/Kolkata").ifBlank { "Asia/Kolkata" }
        val isVerified = root.optBoolean("isVerified", false)
        val source = root.optString("source", "manual")
        val createdAt = root.optLong("createdAt", System.currentTimeMillis())

        val birthLocation = BirthLocation(
            latitude = latitude,
            longitude = longitude,
            placeName = placeName,
            altitudeMeters = altitudeMeters,
            timeZoneId = tzId,
            isVerified = isVerified,
            source = source
        )

        val birthData = BirthData(
            name = name,
            date = LocalDate.of(year, month, day),
            time = LocalTime.of(hour, minute, second, nano),
            location = birthLocation,
            timeZone = ZoneId.of(tzId)
        )

        return UserProfile(
            id = id,
            birthData = birthData,
            createdAt = createdAt
        )
    }
}
