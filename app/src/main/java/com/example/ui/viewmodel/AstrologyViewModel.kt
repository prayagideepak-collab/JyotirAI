package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.domain.location.LocationResolver
import com.example.domain.location.LocationRepository
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

sealed interface PanchangUiState {
    data object Loading : PanchangUiState
    data class Success(val snapshot: PanchangSnapshot) : PanchangUiState
    data class Error(val message: String) : PanchangUiState
}

sealed interface TransitUiState {
    data object Loading : TransitUiState
    data class Success(val snapshot: TransitSnapshot) : TransitUiState
    data class Error(val message: String) : TransitUiState
}

class AstrologyViewModel(
    private val astrologyEngine: AstrologyEngine,
    private val locationResolver: LocationResolver,
    private val locationRepository: LocationRepository
) : ViewModel() {
//
    

    private val _savedLocation = MutableStateFlow<BirthLocation?>(null)
    val savedLocation: StateFlow<BirthLocation?> = _savedLocation.asStateFlow()

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

    private val _transitDateTime = MutableStateFlow<ZonedDateTime?>(null)
    val transitDateTime: StateFlow<ZonedDateTime?> = _transitDateTime.asStateFlow()

    private val _selectedTransitPlanet = MutableStateFlow<TransitPosition?>(null)
    val selectedTransitPlanet: StateFlow<TransitPosition?> = _selectedTransitPlanet.asStateFlow()

    // Panchang State
    private val _panchangUiState = MutableStateFlow<PanchangUiState>(PanchangUiState.Loading)
    val panchangUiState: StateFlow<PanchangUiState> = _panchangUiState.asStateFlow()
    private val _panchangDateTime = MutableStateFlow<ZonedDateTime?>(null)
    val panchangDateTime: StateFlow<ZonedDateTime?> = _panchangDateTime.asStateFlow()

    init {
        viewModelScope.launch {
            val loc = locationRepository.getVerifiedLocation()
            _savedLocation.value = loc
            
            // Re-initialize timezones if location found
            loc?.timeZoneId?.let { tz ->
                val zone = ZoneId.of(tz)
                _panchangDateTime.value = ZonedDateTime.now(zone)
                _transitDateTime.value = ZonedDateTime.now(zone)
            }
            
            loadPanchang()
            loadTransits()
        }
    }
    
    fun resolveLocation(query: String, onResult: (Result<List<BirthLocation>>) -> Unit) {
        viewModelScope.launch {
            val res = locationResolver.resolveLocation(query)
            onResult(res)
        }
    }
    
    fun saveVerifiedLocation(location: BirthLocation) {
        viewModelScope.launch {
            locationRepository.saveVerifiedLocation(location)
            _savedLocation.value = location
            // Reload with new location
            loadPanchang(location = location)
            loadTransits(location = location)
        }
    }


    fun calculateBirthChart(birthData: BirthData) {
        _currentBirthData.value = birthData
        _uiState.value = AstrologyUiState.Calculating
        
        saveVerifiedLocation(birthData.location)

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

    fun loadPanchang(
        dateTime: ZonedDateTime? = null,
        location: BirthLocation? = null
    ) {
        val targetLocation = location
            ?: _currentBirthData.value?.location
            ?: _savedLocation.value
            
        if (targetLocation == null || targetLocation.timeZoneId == null) {
            _panchangUiState.value = PanchangUiState.Error("Location not set or missing timezone. Please select a valid location.")
            return
        }
        
        val zone = java.time.ZoneId.of(targetLocation.timeZoneId)
        val finalTargetZoned = dateTime ?: _panchangDateTime.value ?: java.time.ZonedDateTime.now(zone)
        _panchangDateTime.value = finalTargetZoned
        
        viewModelScope.launch {
            _panchangUiState.value = PanchangUiState.Loading
            val result = astrologyEngine.calculatePanchang(finalTargetZoned, targetLocation)
            result.fold(
                onSuccess = { snapshot -> _panchangUiState.value = PanchangUiState.Success(snapshot) },
                onFailure = { error -> _panchangUiState.value = PanchangUiState.Error(error.message ?: "Failed") }
            )
        }
    }

    fun setPanchangDateTime(dateTime: ZonedDateTime) { loadPanchang(dateTime = dateTime) }
    fun shiftPanchangDays(days: Long) { 
        _panchangDateTime.value?.let { current -> 
            loadPanchang(dateTime = current.plusDays(days)) 
        } 
    }
    fun resetPanchangToNow() {
        val targetLocation = _currentBirthData.value?.location ?: _savedLocation.value
        if (targetLocation?.timeZoneId != null) {
            val zone = ZoneId.of(targetLocation.timeZoneId)
            loadPanchang(dateTime = ZonedDateTime.now(zone))
        }
    }

    fun loadTransits(
        dateTime: ZonedDateTime? = null,
        location: BirthLocation? = null,
        natalProfile: AstrologyProfile? = null
    ) {
        val targetLocation = location
            ?: _currentBirthData.value?.location
            ?: _savedLocation.value
            
        if (targetLocation == null || targetLocation.timeZoneId == null) {
            _transitUiState.value = TransitUiState.Error("Location not set or missing timezone. Please select a valid location.")
            return
        }
        
        val zone = java.time.ZoneId.of(targetLocation.timeZoneId)
        val finalTargetZoned = dateTime ?: _transitDateTime.value ?: java.time.ZonedDateTime.now(zone)
        _transitDateTime.value = finalTargetZoned

        val profileToUse = natalProfile ?: (_uiState.value as? AstrologyUiState.Success)?.profile

        viewModelScope.launch {
            _transitUiState.value = TransitUiState.Loading
            val result = astrologyEngine.calculateTransitSnapshot(
                transitDateTime = finalTargetZoned,
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
        _transitDateTime.value?.let { current ->
            loadTransits(dateTime = current.plusDays(days))
        }
    }

    fun shiftTransitMonths(months: Long) {
        _transitDateTime.value?.let { current ->
            loadTransits(dateTime = current.plusMonths(months))
        }
    }

    fun resetTransitToNow() {
        val targetLocation = _currentBirthData.value?.location ?: _savedLocation.value
        if (targetLocation?.timeZoneId != null) {
            val zone = java.time.ZoneId.of(targetLocation.timeZoneId)
            loadTransits(dateTime = java.time.ZonedDateTime.now(zone))
        }
    }

    fun selectTransitPlanet(planet: TransitPosition?) {
        _selectedTransitPlanet.value = planet
    }

    /**
     * Helper to load a verified reference profile (e.g. Standard New Delhi reference)
     */
    fun loadReferenceProfile() {
        val referenceData = BirthData(
            name = "Aarav Sharma (Reference)",
            date = LocalDate.of(1995, 8, 15),
            time = LocalTime.of(14, 30, 0),
            location = BirthLocation(
                latitude = 28.6139,
                longitude = 77.2090,
                placeName = "New Delhi, India",
                isVerified = true,
                source = "reference"
            ),
            timeZone = ZoneId.of("Asia/Kolkata")
        )
        calculateBirthChart(referenceData)
    }
}

