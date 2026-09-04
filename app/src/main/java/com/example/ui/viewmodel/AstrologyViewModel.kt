package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.engine.SwissEphAstrologyEngine
import com.example.domain.engine.AstrologyEngine
import com.example.domain.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

sealed interface AstrologyUiState {
    data object Empty : AstrologyUiState
    data object Calculating : AstrologyUiState
    data class Success(val profile: AstrologyProfile) : AstrologyUiState
    data class Error(val message: String) : AstrologyUiState
}

sealed interface DashaUiState {
    data object Empty : DashaUiState
    data object Calculating : DashaUiState
    data class Success(val timeline: DashaTimeline) : DashaUiState
    data class Error(val message: String) : DashaUiState
}

sealed interface TransitUiState {
    data object Loading : TransitUiState
    data class Success(val snapshot: TransitSnapshot) : TransitUiState
    data class Error(val message: String) : TransitUiState
}

class AstrologyViewModel(
    private val astrologyEngine: AstrologyEngine = SwissEphAstrologyEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AstrologyUiState>(AstrologyUiState.Empty)
    val uiState: StateFlow<AstrologyUiState> = _uiState.asStateFlow()

    private val _currentBirthData = MutableStateFlow<BirthData?>(null)
    val currentBirthData: StateFlow<BirthData?> = _currentBirthData.asStateFlow()

    private val _selectedVargaType = MutableStateFlow(VargaType.D1)
    val selectedVargaType: StateFlow<VargaType> = _selectedVargaType.asStateFlow()

    private val _currentChart = MutableStateFlow<Chart?>(null)
    val currentChart: StateFlow<Chart?> = _currentChart.asStateFlow()

    private val _selectedPlanetDetail = MutableStateFlow<PlanetPosition?>(null)
    val selectedPlanetDetail: StateFlow<PlanetPosition?> = _selectedPlanetDetail.asStateFlow()

    private val _dashaUiState = MutableStateFlow<DashaUiState>(DashaUiState.Empty)
    val dashaUiState: StateFlow<DashaUiState> = _dashaUiState.asStateFlow()

    private val _dashaTimeline = MutableStateFlow<DashaTimeline?>(null)
    val dashaTimeline: StateFlow<DashaTimeline?> = _dashaTimeline.asStateFlow()

    private val _expandedMahadashaPlanet = MutableStateFlow<DashaPlanet?>(null)
    val expandedMahadashaPlanet: StateFlow<DashaPlanet?> = _expandedMahadashaPlanet.asStateFlow()

    // Transit State
    private val _transitUiState = MutableStateFlow<TransitUiState>(TransitUiState.Loading)
    val transitUiState: StateFlow<TransitUiState> = _transitUiState.asStateFlow()

    private val _transitDateTime = MutableStateFlow<ZonedDateTime>(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")))
    val transitDateTime: StateFlow<ZonedDateTime> = _transitDateTime.asStateFlow()

    private val _selectedTransitPlanet = MutableStateFlow<TransitPosition?>(null)
    val selectedTransitPlanet: StateFlow<TransitPosition?> = _selectedTransitPlanet.asStateFlow()

    init {
        loadTransits()
    }

    fun calculateBirthChart(birthData: BirthData) {
        _currentBirthData.value = birthData
        _uiState.value = AstrologyUiState.Calculating

        viewModelScope.launch {
            val result = astrologyEngine.calculateProfile(birthData)
            result.fold(
                onSuccess = { profile ->
                    _uiState.value = AstrologyUiState.Success(profile)
                    loadChartForVarga(birthData, _selectedVargaType.value)
                    loadDashaTimeline(birthData)
                    loadTransits(location = birthData.location, natalProfile = profile)
                },
                onFailure = { error ->
                    _uiState.value = AstrologyUiState.Error(
                        error.message ?: "Failed to perform deterministic astrological calculation."
                    )
                    _currentChart.value = null
                    _dashaUiState.value = DashaUiState.Error(
                        error.message ?: "Failed to calculate Vimshottari Dasha."
                    )
                    _dashaTimeline.value = null
                }
            )
        }
    }

    fun selectVarga(vargaType: VargaType) {
        _selectedVargaType.value = vargaType
        val birthData = _currentBirthData.value ?: return
        loadChartForVarga(birthData, vargaType)
    }

    fun selectPlanetDetail(planet: PlanetPosition?) {
        _selectedPlanetDetail.value = planet
    }

    fun toggleExpandMahadasha(planet: DashaPlanet) {
        _expandedMahadashaPlanet.value = if (_expandedMahadashaPlanet.value == planet) null else planet
    }

    fun loadDashaTimeline(birthData: BirthData, targetDateTime: ZonedDateTime? = null) {
        viewModelScope.launch {
            _dashaUiState.value = DashaUiState.Calculating
            val result = astrologyEngine.calculateDashaTimeline(birthData, targetDateTime)
            result.fold(
                onSuccess = { timeline ->
                    _dashaTimeline.value = timeline
                    _dashaUiState.value = DashaUiState.Success(timeline)
                    if (_expandedMahadashaPlanet.value == null) {
                        _expandedMahadashaPlanet.value = timeline.currentMahadasha?.planet ?: timeline.startingMahadasha
                    }
                },
                onFailure = { error ->
                    _dashaUiState.value = DashaUiState.Error(
                        error.message ?: "Failed to calculate Vimshottari Dasha timeline."
                    )
                    _dashaTimeline.value = null
                }
            )
        }
    }

    private fun loadChartForVarga(birthData: BirthData, vargaType: VargaType) {
        viewModelScope.launch {
            val chartResult = astrologyEngine.calculateChart(birthData, vargaType.code)
            if (chartResult.isSuccess) {
                _currentChart.value = chartResult.getOrNull()
            }
        }
    }

    fun clearProfile() {
        _currentBirthData.value = null
        _uiState.value = AstrologyUiState.Empty
        _currentChart.value = null
        _selectedPlanetDetail.value = null
        _dashaTimeline.value = null
        _dashaUiState.value = DashaUiState.Empty
        _expandedMahadashaPlanet.value = null
        loadTransits(natalProfile = null)
    }

    fun loadTransits(
        dateTime: ZonedDateTime? = null,
        location: BirthLocation? = null,
        natalProfile: AstrologyProfile? = null
    ) {
        val targetZoned = dateTime ?: _transitDateTime.value
        _transitDateTime.value = targetZoned

        val targetLocation = location
            ?: _currentBirthData.value?.location
            ?: BirthLocation(28.6139, 77.2090, "New Delhi, India")

        val profileToUse = natalProfile ?: (_uiState.value as? AstrologyUiState.Success)?.profile

        viewModelScope.launch {
            _transitUiState.value = TransitUiState.Loading
            val result = astrologyEngine.calculateTransitSnapshot(
                transitDateTime = targetZoned,
                location = targetLocation,
                natalProfile = profileToUse
            )
            result.fold(
                onSuccess = { snapshot ->
                    _transitUiState.value = TransitUiState.Success(snapshot)
                },
                onFailure = { error ->
                    _transitUiState.value = TransitUiState.Error(
                        error.message ?: "Failed to calculate planetary transits."
                    )
                }
            )
        }
    }

    fun setTransitDateTime(dateTime: ZonedDateTime) {
        loadTransits(dateTime = dateTime)
    }

    fun shiftTransitDays(days: Long) {
        val updated = _transitDateTime.value.plusDays(days)
        loadTransits(dateTime = updated)
    }

    fun shiftTransitMonths(months: Long) {
        val updated = _transitDateTime.value.plusMonths(months)
        loadTransits(dateTime = updated)
    }

    fun resetTransitToNow() {
        val zone = _currentBirthData.value?.timeZone ?: _transitDateTime.value.zone ?: ZoneId.of("Asia/Kolkata")
        val now = ZonedDateTime.now(zone)
        loadTransits(dateTime = now)
    }

    fun selectTransitPlanet(planet: TransitPosition?) {
        _selectedTransitPlanet.value = planet
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

