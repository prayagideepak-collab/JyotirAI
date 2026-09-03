package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.engine.SwissEphAstrologyEngine
import com.example.domain.engine.AstrologyEngine
import com.example.domain.models.AstrologyProfile
import com.example.domain.models.BirthData
import com.example.domain.models.BirthLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

sealed interface AstrologyUiState {
    data object Empty : AstrologyUiState
    data object Calculating : AstrologyUiState
    data class Success(val profile: AstrologyProfile) : AstrologyUiState
    data class Error(val message: String) : AstrologyUiState
}

class AstrologyViewModel(
    private val astrologyEngine: AstrologyEngine = SwissEphAstrologyEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AstrologyUiState>(AstrologyUiState.Empty)
    val uiState: StateFlow<AstrologyUiState> = _uiState.asStateFlow()

    private val _currentBirthData = MutableStateFlow<BirthData?>(null)
    val currentBirthData: StateFlow<BirthData?> = _currentBirthData.asStateFlow()

    fun calculateBirthChart(birthData: BirthData) {
        _currentBirthData.value = birthData
        _uiState.value = AstrologyUiState.Calculating

        viewModelScope.launch {
            val result = astrologyEngine.calculateProfile(birthData)
            result.fold(
                onSuccess = { profile ->
                    _uiState.value = AstrologyUiState.Success(profile)
                },
                onFailure = { error ->
                    _uiState.value = AstrologyUiState.Error(
                        error.message ?: "Failed to perform deterministic astrological calculation."
                    )
                }
            )
        }
    }

    fun clearProfile() {
        _currentBirthData.value = null
        _uiState.value = AstrologyUiState.Empty
    }

    /**
     * Helper to load a verified reference profile (e.g. Standard New Delhi reference)
     */
    fun loadReferenceProfile() {
        val referenceData = BirthData(
            name = "Aarav Sharma",
            date = LocalDate.of(1995, 8, 15),
            time = LocalTime.of(14, 30, 0),
            location = BirthLocation(
                latitude = 28.6139,
                longitude = 77.2090,
                placeName = "New Delhi, India"
            ),
            timeZone = ZoneId.of("Asia/Kolkata")
        )
        calculateBirthChart(referenceData)
    }
}
