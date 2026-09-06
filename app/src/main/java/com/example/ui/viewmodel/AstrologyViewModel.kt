package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.alarm.MuhurtaAlarmRepository
import com.example.domain.alarm.MuhurtaAlarmScheduler
import com.example.domain.engine.AstrologyEngine
import com.example.domain.interpretation.*
import com.example.domain.location.LocationRepository
import com.example.domain.location.LocationResolver
import com.example.domain.models.*
import com.example.domain.pdf.KundliPdfExporter
import com.example.domain.prediction.DailyPredictionEngine
import com.example.domain.prediction.DailyPredictionEngineImpl
import com.example.domain.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

sealed interface AstrologyUiState {
    data object Empty : AstrologyUiState
    data object Calculating : AstrologyUiState
    data class Success(val profile: AstrologyProfile) : AstrologyUiState
    data class Error(val message: String) : AstrologyUiState
}

sealed interface AdvancedInterpretationUiState {
    data object Empty : AdvancedInterpretationUiState
    data object Calculating : AdvancedInterpretationUiState
    data class Success(val interpretation: AdvancedVedicInterpretation) : AdvancedInterpretationUiState
    data class Error(val message: String) : AdvancedInterpretationUiState
}

sealed interface YogaDoshaUiState {
    data object Empty : YogaDoshaUiState
    data object Loading : YogaDoshaUiState
    data class Success(val snapshot: YogaDoshaSnapshot) : YogaDoshaUiState
    data class NoResults(val profileName: String, val message: String) : YogaDoshaUiState
    data class InsufficientData(val reason: String) : YogaDoshaUiState
    data class Error(val message: String) : YogaDoshaUiState
}

sealed interface PredictionUiState {
    data object Empty : PredictionUiState
    data object Loading : PredictionUiState
    data class Success(val snapshot: PredictionSnapshot) : PredictionUiState
    data class InsufficientData(val reason: String) : PredictionUiState
    data class Error(val message: String) : PredictionUiState
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

sealed interface DailyRashifalUiState {
    data object Loading : DailyRashifalUiState
    data class Success(val rashifal: DailyRashifal, val defaultProfile: UserProfile) : DailyRashifalUiState
    data object NoDefaultProfile : DailyRashifalUiState
    data class Error(val message: String) : DailyRashifalUiState
}

class AstrologyViewModel(
    private val astrologyEngine: AstrologyEngine,
    private val locationResolver: LocationResolver,
    private val locationRepository: LocationRepository,
    private val profileRepository: ProfileRepository,
    private val muhurtaAlarmScheduler: MuhurtaAlarmScheduler? = null,
    private val muhurtaAlarmRepository: MuhurtaAlarmRepository? = null
) : ViewModel() {

    private val dailyPredictionEngine: DailyPredictionEngine = DailyPredictionEngineImpl(profileRepository, astrologyEngine)

    // Muhurta Alarms State
    val muhurtaAlarms: StateFlow<List<MuhurtaAlarmConfig>> =
        muhurtaAlarmRepository?.alarmsState ?: MutableStateFlow(emptyList())

    // Persistent User Profile State (Max 3 Slots)
    private val _savedProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val savedProfiles: StateFlow<List<UserProfile>> = _savedProfiles.asStateFlow()

    private val _activeProfileId = MutableStateFlow<String?>(null)
    val activeProfileId: StateFlow<String?> = _activeProfileId.asStateFlow()

    private val _defaultProfileId = MutableStateFlow<String?>(null)
    val defaultProfileId: StateFlow<String?> = _defaultProfileId.asStateFlow()

    private val _activeUserProfile = MutableStateFlow<UserProfile?>(null)
    val activeUserProfile: StateFlow<UserProfile?> = _activeUserProfile.asStateFlow()

    private val _defaultUserProfile = MutableStateFlow<UserProfile?>(null)
    val defaultUserProfile: StateFlow<UserProfile?> = _defaultUserProfile.asStateFlow()

    // Daily Rashifal State (Personalised Horoscope strictly for DEFAULT PROFILE)
    private val _dailyRashifalState = MutableStateFlow<DailyRashifalUiState>(DailyRashifalUiState.Loading)
    val dailyRashifalState: StateFlow<DailyRashifalUiState> = _dailyRashifalState.asStateFlow()

    private val _rashifalTargetDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val rashifalTargetDate: StateFlow<LocalDate> = _rashifalTargetDate.asStateFlow()

    // Location State
    private val _savedLocation = MutableStateFlow<BirthLocation?>(null)
    val savedLocation: StateFlow<BirthLocation?> = _savedLocation.asStateFlow()

    // Calculation / Chart State
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

    // Dasha State
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

    // Yoga & Dosha State (Phase 6)
    private val _yogaDoshaState = MutableStateFlow<YogaDoshaUiState>(YogaDoshaUiState.Empty)
    val yogaDoshaState: StateFlow<YogaDoshaUiState> = _yogaDoshaState.asStateFlow()
    private val yogaDoshaCache = java.util.concurrent.ConcurrentHashMap<String, YogaDoshaSnapshot>()

    // Prediction Engine State (Phase 7)
    private val _predictionState = MutableStateFlow<PredictionUiState>(PredictionUiState.Empty)
    val predictionState: StateFlow<PredictionUiState> = _predictionState.asStateFlow()
    private val predictionCache = java.util.concurrent.ConcurrentHashMap<String, PredictionSnapshot>()

    // Advanced Vedic Intelligence & Interpretation State (Phase 12 - Frozen)
    private val _advancedInterpretationState = MutableStateFlow<AdvancedInterpretationUiState>(AdvancedInterpretationUiState.Empty)
    val advancedInterpretationState: StateFlow<AdvancedInterpretationUiState> = _advancedInterpretationState.asStateFlow()

    init {
        viewModelScope.launch {
            refreshProfilesAndRestoreActive()
        }
    }

    private suspend fun refreshProfilesAndRestoreActive() {
        val profiles = profileRepository.getAllProfiles()
        _savedProfiles.value = profiles

        val defId = profileRepository.getDefaultProfileId()
        val actId = profileRepository.getActiveProfileId()

        _defaultProfileId.value = defId
        _activeProfileId.value = actId
        _defaultUserProfile.value = profileRepository.getDefaultProfile()

        val active = profileRepository.getActiveProfile()
        _activeUserProfile.value = active

        if (active != null) {
            _savedLocation.value = active.location
            active.location.timeZoneId?.let { tz ->
                try {
                    val zone = ZoneId.of(tz)
                    _panchangDateTime.value = ZonedDateTime.now(zone)
                    _transitDateTime.value = ZonedDateTime.now(zone)
                } catch (_: Exception) {}
            }
            calculateBirthChartInternal(active.birthData)
        } else {
            // No saved profile, check standalone saved location for Panchang / Transit
            val loc = locationRepository.getVerifiedLocation()
            _savedLocation.value = loc
            loc?.timeZoneId?.let { tz ->
                try {
                    val zone = ZoneId.of(tz)
                    _panchangDateTime.value = ZonedDateTime.now(zone)
                    _transitDateTime.value = ZonedDateTime.now(zone)
                } catch (_: Exception) {}
            }
            _uiState.value = AstrologyUiState.Empty
            loadPanchang()
            loadTransits()
        }
        loadDailyRashifalInternal(_rashifalTargetDate.value)
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
            location.timeZoneId?.let { tz ->
                try {
                    val zone = ZoneId.of(tz)
                    _panchangDateTime.value = _panchangDateTime.value?.withZoneSameInstant(zone) ?: ZonedDateTime.now(zone)
                    _transitDateTime.value = _transitDateTime.value?.withZoneSameInstant(zone) ?: ZonedDateTime.now(zone)
                } catch (_: Exception) {}
            }
            loadPanchang(location = location)
            loadTransits(location = location)
            muhurtaAlarmScheduler?.rescheduleAllActiveAlarms(location)
        }
    }

    /**
     * Saves or updates a persistent user profile (max 3).
     */
    fun saveOrUpdateProfile(
        birthData: BirthData,
        existingId: String? = null,
        onResult: ((Result<UserProfile>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val targetId = existingId ?: UUID.randomUUID().toString()
            val userProfile = UserProfile(id = targetId, birthData = birthData)

            val saveResult = profileRepository.saveProfile(userProfile)
            saveResult.fold(
                onSuccess = {
                    profileRepository.setActiveProfileId(userProfile.id)
                    locationRepository.saveVerifiedLocation(birthData.location)

                    // Refresh profile state
                    val profiles = profileRepository.getAllProfiles()
                    _savedProfiles.value = profiles
                    _activeProfileId.value = userProfile.id
                    _activeUserProfile.value = userProfile
                    _defaultProfileId.value = profileRepository.getDefaultProfileId()
                    _defaultUserProfile.value = profileRepository.getDefaultProfile()
                    _savedLocation.value = birthData.location

                    calculateBirthChartInternal(birthData)
                    loadDailyRashifalInternal(_rashifalTargetDate.value)
                    onResult?.invoke(Result.success(userProfile))
                },
                onFailure = { error ->
                    onResult?.invoke(Result.failure(error))
                }
            )
        }
    }

    /**
     * Switches the currently active profile for chart/dasha viewing without altering the default profile.
     */
    fun switchActiveProfile(profileId: String) {
        viewModelScope.launch {
            val setResult = profileRepository.setActiveProfileId(profileId)
            if (setResult.isSuccess) {
                _activeProfileId.value = profileId
                val profile = profileRepository.getProfileById(profileId)
                _activeUserProfile.value = profile

                if (profile != null) {
                    _savedLocation.value = profile.location
                    locationRepository.saveVerifiedLocation(profile.location)
                    profile.location.timeZoneId?.let { tz ->
                        try {
                            val zone = ZoneId.of(tz)
                            _panchangDateTime.value = _panchangDateTime.value?.withZoneSameInstant(zone) ?: ZonedDateTime.now(zone)
                            _transitDateTime.value = _transitDateTime.value?.withZoneSameInstant(zone) ?: ZonedDateTime.now(zone)
                        } catch (_: Exception) {}
                    }
                    calculateBirthChartInternal(profile.birthData)
                    muhurtaAlarmScheduler?.rescheduleAllActiveAlarms(profile.location)
                }
            }
        }
    }

    /**
     * Designates a saved profile as the canonical Default Profile for future Daily Predictions.
     */
    fun setDefaultProfile(profileId: String) {
        viewModelScope.launch {
            val setResult = profileRepository.setDefaultProfileId(profileId)
            if (setResult.isSuccess) {
                _defaultProfileId.value = profileId
                _defaultUserProfile.value = profileRepository.getProfileById(profileId)
                loadDailyRashifalInternal(_rashifalTargetDate.value)
            }
        }
    }

    /**
     * Deletes a saved profile, automatically repairing default and active profiles.
     */
    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profileId)

            val remainingProfiles = profileRepository.getAllProfiles()
            _savedProfiles.value = remainingProfiles
            _defaultProfileId.value = profileRepository.getDefaultProfileId()
            _defaultUserProfile.value = profileRepository.getDefaultProfile()

            val newActive = profileRepository.getActiveProfile()
            _activeProfileId.value = newActive?.id
            _activeUserProfile.value = newActive

            if (newActive != null) {
                _savedLocation.value = newActive.location
                locationRepository.saveVerifiedLocation(newActive.location)
                calculateBirthChartInternal(newActive.birthData)
            } else {
                clearProfileState()
            }
            loadDailyRashifalInternal(_rashifalTargetDate.value)
        }
    }

    /**
     * Authoritative method to obtain the canonical Default Profile for future Daily Predictions.
     */
    fun getDefaultProfile(): UserProfile? = _defaultUserProfile.value

    fun getDefaultProfileForDailyPrediction(): UserProfile? = _defaultUserProfile.value

    /**
     * Authoritative method to obtain the canonical BirthData for future Daily Predictions.
     */
    fun getDefaultBirthData(): BirthData? = _defaultUserProfile.value?.birthData

    fun getDefaultBirthDataForDailyPrediction(): BirthData? = _defaultUserProfile.value?.birthData

    /**
     * Loads the personalized Daily Rashifal for the given date (defaults to current target date).
     * Strictly targets the canonical DEFAULT PROFILE.
     */
    fun loadDailyRashifal(date: LocalDate? = null) {
        val target = date ?: _rashifalTargetDate.value
        _rashifalTargetDate.value = target
        viewModelScope.launch {
            loadDailyRashifalInternal(target)
        }
    }

    private suspend fun loadDailyRashifalInternal(date: LocalDate) {
        _dailyRashifalState.value = DailyRashifalUiState.Loading
        val defaultProfile = profileRepository.getDefaultProfileForDailyPrediction()
        if (defaultProfile == null) {
            _dailyRashifalState.value = DailyRashifalUiState.NoDefaultProfile
            return
        }

        val result = dailyPredictionEngine.generatePersonalisedRashifal(date)
        result.fold(
            onSuccess = { rashifal ->
                _dailyRashifalState.value = DailyRashifalUiState.Success(rashifal, defaultProfile)
            },
            onFailure = { err ->
                _dailyRashifalState.value = DailyRashifalUiState.Error(
                    err.message ?: "Failed to generate personalized daily prediction"
                )
            }
        )
    }

    fun shiftRashifalDays(days: Long) {
        val newDate = _rashifalTargetDate.value.plusDays(days)
        _rashifalTargetDate.value = newDate
        viewModelScope.launch {
            loadDailyRashifalInternal(newDate)
        }
    }

    fun resetRashifalToToday() {
        val today = LocalDate.now()
        _rashifalTargetDate.value = today
        viewModelScope.launch {
            loadDailyRashifalInternal(today)
        }
    }

    /**
     * Calculates natal chart from given birth data and persists as active profile.
     */
    fun calculateBirthChart(birthData: BirthData) {
        val existingActiveId = _activeProfileId.value
        // If active profile exists with same ID, update it; otherwise if slots remain, create new or update
        val targetId = if (existingActiveId != null && _savedProfiles.value.any { it.id == existingActiveId }) {
            existingActiveId
        } else if (_savedProfiles.value.size < 3) {
            UUID.randomUUID().toString()
        } else {
            _savedProfiles.value.firstOrNull()?.id ?: UUID.randomUUID().toString()
        }

        saveOrUpdateProfile(birthData, existingId = targetId)
    }

    private fun calculateBirthChartInternal(birthData: BirthData) {
        _currentBirthData.value = birthData
        _uiState.value = AstrologyUiState.Calculating
        _currentChart.value = null

        viewModelScope.launch {
            val result = astrologyEngine.calculateProfile(birthData)
            result.fold(
                onSuccess = { profile ->
                    _uiState.value = AstrologyUiState.Success(profile)
                    loadChartForVarga(birthData, _selectedVargaType.value)
                    loadDashaTimeline(birthData)
                    loadTransits(location = birthData.location, natalProfile = profile)
                    loadYogaDoshaAnalysis(profile)
                    loadPredictions(profile)
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
        if (vargaType == VargaType.D1) {
            val profile = (_uiState.value as? AstrologyUiState.Success)?.profile
            if (profile != null) {
                _currentChart.value = profile.rashiChart
                return
            }
        }
        _currentChart.value = null
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
        if (!vargaType.isCalculated) {
            _currentChart.value = null
            return
        }
        viewModelScope.launch {
            val chartResult = astrologyEngine.calculateChart(birthData, vargaType.code)
            if (_selectedVargaType.value == vargaType) {
                _currentChart.value = chartResult.getOrNull()
            }
        }
    }

    fun clearProfile() {
        val currentId = _activeProfileId.value
        if (currentId != null) {
            deleteProfile(currentId)
        } else {
            clearProfileState()
        }
    }

    private fun clearProfileState() {
        _currentBirthData.value = null
        _activeUserProfile.value = null
        _uiState.value = AstrologyUiState.Empty
        _currentChart.value = null
        _selectedPlanetDetail.value = null
        _dashaTimeline.value = null
        _dashaUiState.value = DashaUiState.Empty
        _expandedMahadashaPlanet.value = null
        _yogaDoshaState.value = YogaDoshaUiState.Empty
        _predictionState.value = PredictionUiState.Empty
        _advancedInterpretationState.value = AdvancedInterpretationUiState.Empty
        loadTransits(natalProfile = null)
    }

    /**
     * Calculates deterministic Vedic Yoga & Dosha analysis for the active profile (Phase 6).
     */
    fun loadYogaDoshaAnalysis(profile: AstrologyProfile) {
        val validation = com.example.domain.engine.yogadosha.ResultValidator.validateProfileData(profile)
        if (!validation.isValid) {
            _yogaDoshaState.value = YogaDoshaUiState.InsufficientData(validation.reason)
            return
        }

        val cacheKey = "${profile.birthData.name}_${profile.birthData.date}_${profile.birthData.time}_${profile.birthData.location.latitude}_${profile.birthData.location.longitude}_${profile.birthData.timeZone.id}"
        yogaDoshaCache[cacheKey]?.let { cached ->
            if (cached.detectedYogas.isEmpty() && cached.detectedDoshas.isEmpty()) {
                _yogaDoshaState.value = YogaDoshaUiState.NoResults(
                    profileName = profile.birthData.name,
                    message = "कुण्डली में कोई प्रमुख विशेष योग या दोष उपस्थित नहीं पाया गया।"
                )
            } else {
                _yogaDoshaState.value = YogaDoshaUiState.Success(cached)
            }
            return
        }

        _yogaDoshaState.value = YogaDoshaUiState.Loading
        viewModelScope.launch {
            try {
                val snapshot = com.example.domain.engine.YogaDoshaCalculator.calculate(profile)
                yogaDoshaCache[cacheKey] = snapshot
                if (snapshot.detectedYogas.isEmpty() && snapshot.detectedDoshas.isEmpty()) {
                    _yogaDoshaState.value = YogaDoshaUiState.NoResults(
                        profileName = profile.birthData.name,
                        message = "कुण्डली में कोई प्रमुख विशेष योग या दोष उपस्थित नहीं पाया गया।"
                    )
                } else {
                    _yogaDoshaState.value = YogaDoshaUiState.Success(snapshot)
                }
            } catch (e: Exception) {
                _yogaDoshaState.value = YogaDoshaUiState.Error(
                    e.message ?: "Failed to calculate Yoga and Dosha analysis."
                )
            }
        }
    }

    /**
     * Calculates deterministic multi-factor Vedic predictions across life topics (Phase 7).
     */
    fun loadPredictions(profile: AstrologyProfile, targetDate: LocalDate = LocalDate.now()) {
        val validation = com.example.domain.engine.prediction.PredictionResultValidator.validateProfile(profile)
        if (!validation.isValid) {
            _predictionState.value = PredictionUiState.InsufficientData(validation.reason)
            return
        }

        val cacheKey = "${profile.birthData.name}_${profile.birthData.date}_${profile.birthData.time}_${profile.birthData.location.latitude}_${profile.birthData.location.longitude}_${profile.birthData.timeZone.id}_${targetDate}"
        predictionCache[cacheKey]?.let { cached ->
            _predictionState.value = PredictionUiState.Success(cached)
            return
        }

        _predictionState.value = PredictionUiState.Loading
        viewModelScope.launch {
            try {
                val dashaTimeline = _dashaTimeline.value
                val transits = (_transitUiState.value as? TransitUiState.Success)?.snapshot?.positions?.map {
                    Transit(planet = it.planet, currentSign = it.sign, degree = it.degreeInSign)
                }
                val yogaDoshaSnapshot = (_yogaDoshaState.value as? YogaDoshaUiState.Success)?.snapshot
                    ?: com.example.domain.engine.YogaDoshaCalculator.calculate(profile)

                val snapshot = com.example.domain.engine.PredictionCalculator.calculate(
                    profile = profile,
                    dashaTimeline = dashaTimeline,
                    transits = transits,
                    yogaDoshaSnapshot = yogaDoshaSnapshot,
                    targetDate = targetDate
                )
                predictionCache[cacheKey] = snapshot
                _predictionState.value = PredictionUiState.Success(snapshot)
            } catch (e: Exception) {
                _predictionState.value = PredictionUiState.Error(
                    e.message ?: "Failed to calculate prediction context."
                )
            }
        }
    }

    /**
     * Recomputes the unified deterministic Advanced Vedic Interpretation.
     */
    fun refreshAdvancedInterpretation() {
        val profile = (_uiState.value as? AstrologyUiState.Success)?.profile ?: run {
            _advancedInterpretationState.value = AdvancedInterpretationUiState.Empty
            return
        }
        _advancedInterpretationState.value = AdvancedInterpretationUiState.Calculating
        viewModelScope.launch {
            try {
                val dasha = _dashaTimeline.value
                val transit = (_transitUiState.value as? TransitUiState.Success)?.snapshot
                val panchang = (_panchangUiState.value as? PanchangUiState.Success)?.snapshot
                val interpretation = VedicInterpretationEngine.interpret(
                    profile = profile,
                    dashaTimeline = dasha,
                    transitSnapshot = transit,
                    panchangSnapshot = panchang
                )
                _advancedInterpretationState.value = AdvancedInterpretationUiState.Success(interpretation)
            } catch (e: Exception) {
                _advancedInterpretationState.value = AdvancedInterpretationUiState.Error(
                    e.message ?: "Failed to generate Vedic interpretation."
                )
            }
        }
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

        val zone = ZoneId.of(targetLocation.timeZoneId)
        val finalTargetZoned = dateTime ?: _panchangDateTime.value?.withZoneSameInstant(zone) ?: ZonedDateTime.now(zone)
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

        val zone = ZoneId.of(targetLocation.timeZoneId)
        val finalTargetZoned = dateTime ?: _transitDateTime.value?.withZoneSameInstant(zone) ?: ZonedDateTime.now(zone)
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
            val zone = ZoneId.of(targetLocation.timeZoneId)
            loadTransits(dateTime = ZonedDateTime.now(zone))
        }
    }

    fun selectTransitPlanet(planet: TransitPosition?) {
        _selectedTransitPlanet.value = planet
    }

    /**
     * Toggles dynamic alarm for a specific Muhurta (Brahma Muhurta, Rahukaal, etc.).
     */
    fun toggleMuhurtaAlarm(type: MuhurtaAlarmType, isEnabled: Boolean) {
        val targetLocation = _currentBirthData.value?.location
            ?: _activeUserProfile.value?.location
            ?: _savedLocation.value
            ?: return

        val profileId = _activeProfileId.value

        if (isEnabled) {
            muhurtaAlarmScheduler?.scheduleAlarm(type, targetLocation, profileId)
        } else {
            muhurtaAlarmScheduler?.cancelAlarm(type, profileId)
        }
    }

    /**
     * Helper to load a verified reference profile (New Delhi reference).
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
