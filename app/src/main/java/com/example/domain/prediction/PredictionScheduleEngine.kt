package com.example.domain.prediction

import com.example.domain.engine.AstrologyEngine
import com.example.domain.models.*
import com.example.domain.profile.ProfileRepository
import java.time.LocalDate

/**
 * Unified Architectural Coordinator for Vedic Prediction Schedules (Daily, Monthly, Yearly).
 * Coordinates period engines, time context resolution, validation, and profile-isolated caching.
 */
interface PredictionScheduleEngine {
    suspend fun getDailyPrediction(
        profile: AstrologyProfile,
        date: LocalDate = LocalDate.now()
    ): Result<PeriodicPredictionResult>

    suspend fun getMonthlyPrediction(
        profile: AstrologyProfile,
        year: Int,
        month: Int
    ): Result<PeriodicPredictionResult>

    suspend fun getYearlyPrediction(
        profile: AstrologyProfile,
        year: Int
    ): Result<PeriodicPredictionResult>

    suspend fun getPrediction(
        profile: AstrologyProfile,
        periodType: PredictionPeriodType,
        date: LocalDate
    ): Result<PeriodicPredictionResult>

    fun clearCache(profileId: String? = null)
}

class PredictionScheduleEngineImpl(
    private val dailyEngine: DailyPredictionEngine,
    private val monthlyEngine: MonthlyPredictionEngine,
    private val yearlyEngine: YearlyPredictionEngine,
    private val cacheCoordinator: CacheCoordinator = CacheCoordinator()
) : PredictionScheduleEngine {

    companion object {
        fun create(
            profileRepository: ProfileRepository,
            astrologyEngine: AstrologyEngine
        ): PredictionScheduleEngineImpl {
            val contextBuilder = PredictionContextBuilder(astrologyEngine)
            val daily = DailyPredictionEngineImpl(profileRepository, astrologyEngine, contextBuilder)
            val monthly = MonthlyPredictionEngineImpl(astrologyEngine, contextBuilder)
            val yearly = YearlyPredictionEngineImpl(astrologyEngine, contextBuilder)
            return PredictionScheduleEngineImpl(daily, monthly, yearly)
        }
    }

    override suspend fun getDailyPrediction(
        profile: AstrologyProfile,
        date: LocalDate
    ): Result<PeriodicPredictionResult> {
        val profileId = profile.birthData.name
        val key = cacheCoordinator.buildKey(profileId, PredictionPeriodType.DAILY, date.year, date.monthValue, date.dayOfMonth)

        cacheCoordinator.get(key)?.let {
            return Result.success(it)
        }

        val result = dailyEngine.generateDailyPrediction(profile, date)
        result.onSuccess {
            cacheCoordinator.put(key, it)
        }
        return result
    }

    override suspend fun getMonthlyPrediction(
        profile: AstrologyProfile,
        year: Int,
        month: Int
    ): Result<PeriodicPredictionResult> {
        val profileId = profile.birthData.name
        val key = cacheCoordinator.buildKey(profileId, PredictionPeriodType.MONTHLY, year, month, 0)

        cacheCoordinator.get(key)?.let {
            return Result.success(it)
        }

        val result = monthlyEngine.generateMonthlyPrediction(profile, year, month)
        result.onSuccess {
            cacheCoordinator.put(key, it)
        }
        return result
    }

    override suspend fun getYearlyPrediction(
        profile: AstrologyProfile,
        year: Int
    ): Result<PeriodicPredictionResult> {
        val profileId = profile.birthData.name
        val key = cacheCoordinator.buildKey(profileId, PredictionPeriodType.YEARLY, year, 0, 0)

        cacheCoordinator.get(key)?.let {
            return Result.success(it)
        }

        val result = yearlyEngine.generateYearlyPrediction(profile, year)
        result.onSuccess {
            cacheCoordinator.put(key, it)
        }
        return result
    }

    override suspend fun getPrediction(
        profile: AstrologyProfile,
        periodType: PredictionPeriodType,
        date: LocalDate
    ): Result<PeriodicPredictionResult> {
        return when (periodType) {
            PredictionPeriodType.DAILY -> getDailyPrediction(profile, date)
            PredictionPeriodType.MONTHLY -> getMonthlyPrediction(profile, date.year, date.monthValue)
            PredictionPeriodType.YEARLY -> getYearlyPrediction(profile, date.year)
        }
    }

    override fun clearCache(profileId: String?) {
        if (profileId != null) {
            cacheCoordinator.clearForProfile(profileId)
        } else {
            cacheCoordinator.clearAll()
        }
    }
}
