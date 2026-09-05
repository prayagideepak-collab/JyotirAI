package com.example.domain.alarm

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.models.BirthLocation
import com.example.domain.models.MuhurtaAlarmConfig
import com.example.domain.models.MuhurtaAlarmType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface MuhurtaAlarmRepository {
    fun getAlarms(): List<MuhurtaAlarmConfig>
    fun getAlarm(type: MuhurtaAlarmType, profileId: String?): MuhurtaAlarmConfig?
    fun saveAlarm(config: MuhurtaAlarmConfig)
    fun removeAlarm(type: MuhurtaAlarmType, profileId: String?)
    val alarmsState: StateFlow<List<MuhurtaAlarmConfig>>
}

class MuhurtaAlarmRepositoryImpl(
    context: Context,
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
) : MuhurtaAlarmRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("jyotirai_muhurta_alarms_prefs", Context.MODE_PRIVATE)

    private val adapter = moshi.adapter(MuhurtaAlarmConfig::class.java)

    private val _alarmsState = MutableStateFlow<List<MuhurtaAlarmConfig>>(loadAllAlarms())
    override val alarmsState: StateFlow<List<MuhurtaAlarmConfig>> = _alarmsState.asStateFlow()

    private fun getKey(type: MuhurtaAlarmType, profileId: String?): String {
        return "alarm_${type.name}_${profileId ?: "default"}"
    }

    private fun loadAllAlarms(): List<MuhurtaAlarmConfig> {
        val list = mutableListOf<MuhurtaAlarmConfig>()
        val allEntries = prefs.all
        for ((key, value) in allEntries) {
            if (key.startsWith("alarm_") && value is String) {
                try {
                    val config = adapter.fromJson(value)
                    if (config != null) {
                        list.add(config)
                    }
                } catch (_: Exception) {}
            }
        }
        return list
    }

    override fun getAlarms(): List<MuhurtaAlarmConfig> {
        return loadAllAlarms()
    }

    override fun getAlarm(type: MuhurtaAlarmType, profileId: String?): MuhurtaAlarmConfig? {
        val json = prefs.getString(getKey(type, profileId), null) ?: return null
        return try {
            adapter.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    override fun saveAlarm(config: MuhurtaAlarmConfig) {
        val json = adapter.toJson(config)
        prefs.edit().putString(getKey(config.type, config.profileId), json).apply()
        _alarmsState.value = loadAllAlarms()
    }

    override fun removeAlarm(type: MuhurtaAlarmType, profileId: String?) {
        prefs.edit().remove(getKey(type, profileId)).apply()
        _alarmsState.value = loadAllAlarms()
    }
}
