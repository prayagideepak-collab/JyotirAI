package com.example.domain.prediction

import com.example.domain.engine.AstrologyEngine
import com.example.domain.models.*
import com.example.domain.profile.ProfileRepository
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Architectural foundation for the future Personalised Daily Horoscope / Rashifal engine.
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
 * Contract for generating the future personalised Daily Horoscope context.
 * Guarantees that only the canonical DEFAULT PROFILE is used as the prediction subject.
 */
interface DailyPredictionEngine {
    /**
     * Resolves the full personalised daily prediction context strictly using the DEFAULT profile.
     */
    suspend fun getDailyPredictionContext(
        date: LocalDate = LocalDate.now()
    ): Result<DailyPredictionContext>

    /**
     * Calculates the local Brahma Muhurta scheduling window based on the DEFAULT profile's location and timezone.
     */
    suspend fun getBrahmaMuhurtaScheduleWindow(
        date: LocalDate = LocalDate.now()
    ): Result<TimeInterval?>
}

class DailyPredictionEngineImpl(
    private val profileRepository: ProfileRepository,
    private val astrologyEngine: AstrologyEngine
) : DailyPredictionEngine {

    override suspend fun getDailyPredictionContext(date: LocalDate): Result<DailyPredictionContext> {
        val defaultProfile = profileRepository.getDefaultProfileForDailyPrediction()
            ?: return Result.failure(IllegalStateException("No default profile configured for daily prediction"))

        val birthData = defaultProfile.birthData
        val location = birthData.location
        val zone = birthData.timeZone
        val targetZoned = date.atStartOfDay(zone).plusHours(6)

        val panchang = astrologyEngine.calculatePanchang(targetZoned, location).getOrNull()
        val transit = astrologyEngine.calculateTransitSnapshot(targetZoned, location).getOrNull()
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

    override suspend fun getBrahmaMuhurtaScheduleWindow(date: LocalDate): Result<TimeInterval?> {
        val defaultProfile = profileRepository.getDefaultProfileForDailyPrediction()
            ?: return Result.failure(IllegalStateException("No default profile configured for daily prediction"))

        val birthData = defaultProfile.birthData
        val location = birthData.location
        val zone = birthData.timeZone
        val targetZoned = date.atStartOfDay(zone).plusHours(6)

        val panchangResult = astrologyEngine.calculatePanchang(targetZoned, location)
        return panchangResult.map { it.muhurta?.brahmaMuhurta }
    }
}
