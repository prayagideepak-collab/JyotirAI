package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.engine.SwissEphAstrologyEngine
import com.example.domain.alarm.MuhurtaAlarmRepository
import com.example.domain.alarm.MuhurtaAlarmRepositoryImpl
import com.example.domain.alarm.MuhurtaAlarmScheduler
import com.example.domain.location.AndroidLocationResolver
import com.example.domain.location.LocationRepository
import com.example.domain.location.LocationRepositoryImpl
import com.example.domain.profile.ProfileRepository
import com.example.domain.profile.ProfileRepositoryImpl

class AstrologyViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AstrologyViewModel::class.java)) {
            val locationResolver = AndroidLocationResolver(application)
            val locationRepository: LocationRepository = LocationRepositoryImpl(application)
            val profileRepository: ProfileRepository = ProfileRepositoryImpl(application)
            val alarmRepository: MuhurtaAlarmRepository = MuhurtaAlarmRepositoryImpl(application)
            val alarmScheduler = MuhurtaAlarmScheduler(application, alarmRepository)
            val engine = SwissEphAstrologyEngine() // Should ideally be singleton, but keeping it as is
            @Suppress("UNCHECKED_CAST")
            return AstrologyViewModel(
                astrologyEngine = engine,
                locationResolver = locationResolver,
                locationRepository = locationRepository,
                profileRepository = profileRepository,
                muhurtaAlarmScheduler = alarmScheduler,
                muhurtaAlarmRepository = alarmRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

