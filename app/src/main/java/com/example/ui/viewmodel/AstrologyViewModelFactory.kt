package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.domain.location.AndroidLocationResolver
import com.example.domain.location.LocationRepository
import com.example.domain.location.LocationRepositoryImpl
import com.example.domain.profile.ProfileRepository
import com.example.domain.profile.ProfileRepositoryImpl
import com.example.data.engine.SwissEphAstrologyEngine

class AstrologyViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AstrologyViewModel::class.java)) {
            val locationResolver = AndroidLocationResolver(application)
            val locationRepository: LocationRepository = LocationRepositoryImpl(application)
            val profileRepository: ProfileRepository = ProfileRepositoryImpl(application)
            val engine = SwissEphAstrologyEngine() // Should ideally be singleton, but keeping it as is
            @Suppress("UNCHECKED_CAST")
            return AstrologyViewModel(engine, locationResolver, locationRepository, profileRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
