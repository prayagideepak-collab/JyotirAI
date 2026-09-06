package com.example.domain.prediction

import com.example.domain.engine.AstrologyEngine
import com.example.domain.models.*
import com.example.domain.profile.ProfileRepository
import java.time.LocalDate

/**
 * Architectural foundation for the Personalised Daily Horoscope / Rashifal engine.
 * Conceptually and strictly enforces using the DEFAULT PROFILE (never the active/viewed profile).
 */
data class DailyPredictionContext(
    val defaultProfile: UserProfile,
    val targetDate: LocalDate,
    val panchang: PanchangSnapshot?,
    val transitSnapshot: TransitSnapshot?,
    val dashaTimeline: DashaTimeline?,
    val brahmaMuhurta: TimeInterval?
) {
    val defaultBirthData: BirthData get() = defaultProfile.birthData
}

/**
 * Contract for generating the personalised Daily Horoscope context and predictions.
 */
interface DailyPredictionEngine {
    /**
     * Resolves the full personalised daily prediction context strictly using the DEFAULT profile.
     */
    suspend fun getDailyPredictionContext(
        date: LocalDate = LocalDate.now()
    ): Result<DailyPredictionContext>

    /**
     * Generates the comprehensive personalised Daily Rashifal strictly for the DEFAULT profile.
     */
    suspend fun generatePersonalisedRashifal(
        date: LocalDate = LocalDate.now()
    ): Result<DailyRashifal>

    /**
     * Calculates the local Brahma Muhurta scheduling window based on the DEFAULT profile's location and timezone.
     */
    suspend fun getBrahmaMuhurtaScheduleWindow(
        date: LocalDate = LocalDate.now()
    ): Result<TimeInterval?>

    /**
     * Generates structured Phase 8 Daily PeriodicPredictionResult for a specific AstrologyProfile.
     */
    suspend fun generateDailyPrediction(
        profile: AstrologyProfile,
        date: LocalDate = LocalDate.now()
    ): Result<PeriodicPredictionResult>

    /**
     * Generates structured Phase 8 Daily PeriodicPredictionResult for the canonical DEFAULT profile.
     */
    suspend fun generateDailyPrediction(
        date: LocalDate = LocalDate.now()
    ): Result<PeriodicPredictionResult>
}

class DailyPredictionEngineImpl(
    private val profileRepository: ProfileRepository,
    private val astrologyEngine: AstrologyEngine,
    private val contextBuilder: PredictionContextBuilder = PredictionContextBuilder(astrologyEngine)
) : DailyPredictionEngine {

    override suspend fun getDailyPredictionContext(date: LocalDate): Result<DailyPredictionContext> {
        val defaultProfile = profileRepository.getDefaultProfileForDailyPrediction()
            ?: return Result.failure(IllegalStateException("Default profile required for daily prediction"))

        val birthData = defaultProfile.birthData
        val location = birthData.location
        val zone = birthData.timeZone

        // 1. Calculate Panchang first at local midday to accurately identify sunrise for that date
        val middayZoned = date.atStartOfDay(zone).plusHours(12)
        val panchang = astrologyEngine.calculatePanchang(middayZoned, location).getOrNull()

        // 2. Use calculated local sunrise as the Vedic day anchor (or fallback to morning in local timezone)
        val targetZoned = panchang?.sunrise ?: date.atStartOfDay(zone).plusHours(6)

        val profileResult = astrologyEngine.calculateProfile(birthData)
        val natalProfile = profileResult.getOrNull()

        val transit = astrologyEngine.calculateTransitSnapshot(targetZoned, location, natalProfile).getOrNull()
        val dasha = astrologyEngine.calculateDashaTimeline(birthData, targetZoned).getOrNull()
        val brahmaMuhurta = panchang?.muhurta?.brahmaMuhurta

        return Result.success(
            DailyPredictionContext(
                defaultProfile = defaultProfile,
                targetDate = date,
                panchang = panchang,
                transitSnapshot = transit,
                dashaTimeline = dasha,
                brahmaMuhurta = brahmaMuhurta
            )
        )
    }

    override suspend fun generatePersonalisedRashifal(date: LocalDate): Result<DailyRashifal> {
        val defaultProfile = profileRepository.getDefaultProfileForDailyPrediction()
            ?: return Result.failure(IllegalStateException("Default profile required for daily prediction"))

        val birthData = defaultProfile.birthData
        val location = birthData.location
        val zone = birthData.timeZone

        // Obtain natal profile
        val profileResult = astrologyEngine.calculateProfile(birthData)
        if (profileResult.isFailure) {
            return Result.failure(profileResult.exceptionOrNull() ?: IllegalStateException("Failed to calculate natal profile"))
        }
        val natalProfile = profileResult.getOrThrow()

        // Midday Panchang to determine local solar day & sunrise
        val middayZoned = date.atStartOfDay(zone).plusHours(12)
        val panchang = astrologyEngine.calculatePanchang(middayZoned, location).getOrNull()

        val targetZoned = panchang?.sunrise ?: date.atStartOfDay(zone).plusHours(6)
        val transit = astrologyEngine.calculateTransitSnapshot(targetZoned, location, natalProfile).getOrNull()
        val dasha = astrologyEngine.calculateDashaTimeline(birthData, targetZoned).getOrNull()
        val brahmaMuhurta = panchang?.muhurta?.brahmaMuhurta

        val context = DailyPredictionContext(
            defaultProfile = defaultProfile,
            targetDate = date,
            panchang = panchang,
            transitSnapshot = transit,
            dashaTimeline = dasha,
            brahmaMuhurta = brahmaMuhurta
        )

        val rashifal = PersonalisedRashifalEngine.generateRashifal(context, natalProfile)
        return Result.success(rashifal)
    }

    override suspend fun getBrahmaMuhurtaScheduleWindow(date: LocalDate): Result<TimeInterval?> {
        val defaultProfile = profileRepository.getDefaultProfileForDailyPrediction()
            ?: return Result.failure(IllegalStateException("Default profile required for daily prediction"))

        val birthData = defaultProfile.birthData
        val location = birthData.location
        val zone = birthData.timeZone
        val middayZoned = date.atStartOfDay(zone).plusHours(12)

        val panchangResult = astrologyEngine.calculatePanchang(middayZoned, location)
        return panchangResult.map { it.muhurta?.brahmaMuhurta }
    }

    override suspend fun generateDailyPrediction(
        profile: AstrologyProfile,
        date: LocalDate
    ): Result<PeriodicPredictionResult> {
        val profileValidation = ResultValidator.validateProfile(profile)
        if (!profileValidation.isValid) {
            return Result.failure(IllegalArgumentException(profileValidation.reason))
        }

        val contextResult = contextBuilder.buildContext(profile, date)
        if (contextResult.isFailure) {
            return Result.failure(contextResult.exceptionOrNull() ?: IllegalStateException("Failed to build daily context"))
        }
        val context = contextResult.getOrThrow()

        val timeContext = TimeContextResolver.resolve(
            periodType = PredictionPeriodType.DAILY,
            targetDate = date,
            birthData = profile.birthData,
            dashaTimeline = context.dashaTimeline
        )

        val rawResult = EvidenceAggregator.aggregate(
            profile = profile,
            periodType = PredictionPeriodType.DAILY,
            timeContext = timeContext,
            phase7Snapshot = context.phase7PredictionSnapshot,
            transitSnapshot = context.transitSnapshot,
            yogaDoshaSnapshot = context.yogaDoshaSnapshot
        )

        val sanitized = ResultValidator.sanitizeResult(rawResult)
        return Result.success(sanitized)
    }

    override suspend fun generateDailyPrediction(date: LocalDate): Result<PeriodicPredictionResult> {
        val defaultProfile = profileRepository.getDefaultProfileForDailyPrediction()
            ?: return Result.failure(IllegalStateException("Default profile required for daily prediction"))

        val profileResult = astrologyEngine.calculateProfile(defaultProfile.birthData)
        if (profileResult.isFailure) {
            return Result.failure(profileResult.exceptionOrNull() ?: IllegalStateException("Failed to calculate default natal profile"))
        }
        return generateDailyPrediction(profileResult.getOrThrow(), date)
    }
}
