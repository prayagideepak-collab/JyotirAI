package com.example.domain.muhurta

import com.example.domain.models.*
import com.example.domain.panchang.LocationContextResolver
import com.example.domain.panchang.PanchangEngine
import com.example.domain.panchang.PanchangEngineImpl
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Production implementation of the Phase 10 Muhurta Engine.
 * Highly robust, power-efficient, cached, and fully integrated with Phase 9 Panchang.
 */
class MuhurtaEngineImpl(
    private val panchangEngine: PanchangEngine = PanchangEngineImpl(),
    private val cache: MuhurtaCache = MuhurtaCache()
) : MuhurtaEngine {

    private val engineVersion = "JyotirAI-Muhurta-v1.0-SwissEph"

    override suspend fun calculateDailyMuhurta(
        activityType: MuhurtaActivityType,
        date: LocalDate,
        location: BirthLocation,
        profile: UserProfile?
    ): Result<MuhurtaResult> {
        val request = MuhurtaRequest(
            activityType = activityType,
            startDate = date,
            endDate = date,
            location = location,
            profile = profile
        )
        return calculateMuhurta(request)
    }

    override suspend fun calculateMuhurta(request: MuhurtaRequest): Result<MuhurtaResult> {
        return try {
            // 1. Validate Request
            MuhurtaRequestValidator.validate(request)

            // 2. Check Cache
            cache.get(request, engineVersion)?.let { return Result.success(it) }

            // 3. Resolve Location and Dates
            val locContext = LocationContextResolver.resolve(request.location)
            val dates = DateRangeResolver.resolveDates(request.startDate, request.endDate)
            val ruleProfile = ActivityContextResolver.getRuleProfile(request.activityType)

            val candidateWindows = mutableListOf<MuhurtaCandidateWindow>()
            val nowUtc = ZonedDateTime.now(ZoneOffset.UTC)
            val calculationTimestamp = ZonedDateTime.now(locContext.calculationTimeZone)

            // 4. Iterate dates deterministically
            for (date in dates) {
                val panchangResult = panchangEngine.calculatePanchangForDate(date, request.location)
                if (panchangResult.isFailure) {
                    continue
                }
                val panchang = panchangResult.getOrThrow()

                // Calculate personal bala if profile present
                val personalBala = if (request.profile != null) {
                    val transitMoonNakshatra = panchang.nakshatra.nakshatra
                    val transitMoonRashi = panchang.moonContext.sign
                    PersonalBalaCalculator.calculate(
                        profile = request.profile,
                        transitMoonNakshatra = transitMoonNakshatra,
                        transitMoonRashi = transitMoonRashi
                    )
                } else null

                // Compute time windows and inauspicious intervals
                val (rawWindows, inauspiciousIntervals) = TimeWindowCalculator.calculateWindowsForDay(
                    panchang = panchang,
                    preferredTimeSlot = request.preferredTimeSlot
                )

                // Evaluate each candidate window
                for (rawWindow in rawWindows) {
                    val evaluatedWindow = MuhurtaRuleEvaluator.evaluateWindow(
                        window = rawWindow,
                        panchang = panchang,
                        ruleProfile = ruleProfile,
                        inauspiciousIntervals = inauspiciousIntervals,
                        personalBala = personalBala
                    )
                    candidateWindows.add(evaluatedWindow)
                }
            }

            val metadata = CalculationMetadata(
                ephemerisEngine = "Swiss Ephemeris (Moshier Sidereal)",
                ayanamsaName = "Lahiri (Chitra Paksha)",
                ayanamsaDegree = 24.1,
                houseSystem = "Vedic Whole Sign (Rashi Bhava)",
                julianDayUt = 0.0,
                calculatedUtcIso = nowUtc.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            )

            // 5. Build Result
            val result = MuhurtaResultBuilder.build(
                request = request,
                candidateWindows = candidateWindows,
                locationContext = locContext,
                calculationTimestamp = calculationTimestamp,
                metadata = metadata
            )

            // 6. Cache Result
            cache.put(request, engineVersion, result)

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun clearCache() {
        cache.clear()
    }
}
