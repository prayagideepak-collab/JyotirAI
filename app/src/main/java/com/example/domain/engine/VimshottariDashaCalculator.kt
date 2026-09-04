package com.example.domain.engine

import com.example.domain.models.*
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

/**
 * Authoritative, deterministic calculation engine for the Vedic Vimshottari Dasha system.
 *
 * Calculation Rules & Conventions:
 * 1. Zodiac: Moon's sidereal longitude (Nirayana, Lahiri Ayanamsa) from Swiss Ephemeris.
 * 2. Nakshatra Span: Exactly 360° / 27 = 13° 20' = 13.333333333333334°.
 * 3. Starting Lord: Ruled by birth Nakshatra's Vimshottari lord.
 * 4. Starting Balance: Proportional to the unelapsed portion of the birth Nakshatra:
 *    balance = planet_years * (1.0 - (longitude_within_nakshatra / nakshatra_span))
 * 5. Sequence: Fixed cyclic 120-year order: Ketu (7) → Venus (20) → Sun (6) → Moon (10) →
 *    Mars (7) → Rahu (18) → Jupiter (16) → Saturn (19) → Mercury (17).
 * 6. Solar Year Standard: Standard astronomical mean Gregorian solar year of 365.2425 days
 *    (31,556,952.0 seconds), systematically accounting for 97 leap years per 400-year cycle.
 * 7. Boundary Inclusivity: Intervals are half-open [startDate, endDate), where the period transition
 *    timestamp is shared identically between period[i].endDate and period[i+1].startDate.
 * 8. Antardasha Fit: Sum of 9 Antardashas fits parent Mahadasha duration with zero seconds drift.
 */
object VimshottariDashaCalculator {

    const val DAYS_PER_SOLAR_YEAR = 365.2425
    const val SECONDS_PER_SOLAR_YEAR = DAYS_PER_SOLAR_YEAR * 86400.0 // 31,556,952.0 seconds

    data class StartingBalanceCalculation(
        val birthNakshatra: Nakshatra,
        val nakshatraLord: String,
        val startingPlanet: DashaPlanet,
        val nakshatraStartLongitude: Double,
        val moonLongitudeWithinNakshatra: Double,
        val fractionElapsed: Double,
        val fractionRemaining: Double,
        val elapsedYears: Double,
        val remainingYears: Double,
        val startingBalance: DashaBalance
    )

    /**
     * Calculates Nakshatra bounds and starting Mahadasha balance from the sidereal Moon longitude.
     */
    fun calculateStartingBalance(moonLongitude: Double): StartingBalanceCalculation {
        val normalizedLon = moonLongitude.mod(360.0)
        val span = Nakshatra.SPAN_DEGREES

        val rawIndex = (normalizedLon / span).toInt()
        val nakshatraIndex = rawIndex.coerceIn(0, 26)
        val birthNakshatra = Nakshatra.entries[nakshatraIndex]
        val nakshatraLord = birthNakshatra.lord
        val startingPlanet = DashaPlanet.fromLord(nakshatraLord)

        val nakshatraStartLon = nakshatraIndex * span
        val lonWithin = (normalizedLon - nakshatraStartLon).coerceIn(0.0, span)

        val fractionElapsed = (lonWithin / span).coerceIn(0.0, 1.0)
        val fractionRemaining = (1.0 - fractionElapsed).coerceIn(0.0, 1.0)

        val remainingYears = startingPlanet.years * fractionRemaining
        val elapsedYears = startingPlanet.years * fractionElapsed

        return StartingBalanceCalculation(
            birthNakshatra = birthNakshatra,
            nakshatraLord = nakshatraLord,
            startingPlanet = startingPlanet,
            nakshatraStartLongitude = nakshatraStartLon,
            moonLongitudeWithinNakshatra = lonWithin,
            fractionElapsed = fractionElapsed,
            fractionRemaining = fractionRemaining,
            elapsedYears = elapsedYears,
            remainingYears = remainingYears,
            startingBalance = DashaBalance.fromYears(remainingYears)
        )
    }

    /**
     * Generates the proportional 9 Antardasha periods for a given Mahadasha.
     * Guaranteed to fit the parent Mahadasha period exactly without boundary gaps.
     */
    fun calculateAntardashas(
        mahadashaLord: DashaPlanet,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime,
        totalDurationYears: Double,
        targetDateTime: ZonedDateTime? = null,
        isParentLastPeriod: Boolean = false
    ): List<AntardashaPeriod> {
        val antardashaLords = DashaPlanet.sequenceStartingFrom(mahadashaLord)
        val totalSeconds = ChronoUnit.SECONDS.between(startDate, endDate)
        val antardashas = mutableListOf<AntardashaPeriod>()

        var accumulatedYears = 0.0
        var currentStart = startDate

        for (i in 0 until 9) {
            val aLord = antardashaLords[i]
            val aYears = totalDurationYears * (aLord.years.toDouble() / DashaPlanet.TOTAL_CYCLE_YEARS)
            accumulatedYears += aLord.years

            val endFraction = accumulatedYears / DashaPlanet.TOTAL_CYCLE_YEARS
            val currentEnd = if (i == 8) {
                endDate
            } else {
                startDate.plusSeconds((totalSeconds * endFraction).roundToLong())
            }

            val isLastAntardasha = (i == 8) && isParentLastPeriod
            val isCurrent = if (targetDateTime != null) {
                isDateTimeInRange(targetDateTime, currentStart, currentEnd, isLast = isLastAntardasha)
            } else false

            antardashas.add(
                AntardashaPeriod(
                    mahadashaLord = mahadashaLord,
                    antardashaLord = aLord,
                    startDate = currentStart,
                    endDate = currentEnd,
                    durationYears = aYears,
                    isCurrent = isCurrent
                )
            )
            currentStart = currentEnd
        }

        return antardashas
    }

    /**
     * Computes the complete 120-year Vimshottari Dasha timeline starting from the birth Mahadasha.
     */
    fun calculateTimeline(
        birthData: BirthData,
        moonLongitude: Double,
        targetDateTime: ZonedDateTime,
        metadata: CalculationMetadata
    ): DashaTimeline {
        val balanceResult = calculateStartingBalance(moonLongitude)
        val birthZoned = ZonedDateTime.of(birthData.date, birthData.time, birthData.timeZone)

        val startingPlanet = balanceResult.startingPlanet
        val cyclePlanets = DashaPlanet.sequenceStartingFrom(startingPlanet)

        // Starting Mahadasha full cycle start and end
        val remSeconds = (balanceResult.remainingYears * SECONDS_PER_SOLAR_YEAR).roundToLong()
        val elapsedSeconds = (balanceResult.elapsedYears * SECONDS_PER_SOLAR_YEAR).roundToLong()

        val birthMahadashaEnd = birthZoned.plusSeconds(remSeconds)
        val birthMahadashaStart = birthZoned.minusSeconds(elapsedSeconds)

        val mahadashas = mutableListOf<MahadashaPeriod>()
        var periodStart = birthMahadashaStart

        for (i in 0 until 9) {
            val planet = cyclePlanets[i]
            val isBirthMahadasha = (i == 0)

            val periodEnd = if (isBirthMahadasha) {
                birthMahadashaEnd
            } else {
                val durationSec = (planet.years * SECONDS_PER_SOLAR_YEAR).roundToLong()
                periodStart.plusSeconds(durationSec)
            }

            val isLast = (i == 8)
            val isCurrent = isDateTimeInRange(targetDateTime, periodStart, periodEnd, isLast = isLast)

            val antardashas = calculateAntardashas(
                mahadashaLord = planet,
                startDate = periodStart,
                endDate = periodEnd,
                totalDurationYears = planet.years.toDouble(),
                targetDateTime = targetDateTime,
                isParentLastPeriod = isLast
            )

            mahadashas.add(
                MahadashaPeriod(
                    planet = planet,
                    startDate = periodStart,
                    endDate = periodEnd,
                    totalDurationYears = planet.years.toDouble(),
                    isCurrent = isCurrent,
                    antardashas = antardashas,
                    isBirthMahadasha = isBirthMahadasha,
                    birthBalance = if (isBirthMahadasha) balanceResult.startingBalance else null,
                    birthDateTime = birthZoned
                )
            )

            periodStart = periodEnd
        }

        // Identify current Mahadasha and Antardasha based on target date
        val currentMahadasha = mahadashas.firstOrNull { it.isCurrent }
        val currentAntardasha = currentMahadasha?.antardashas?.firstOrNull { it.isCurrent }

        return DashaTimeline(
            birthNakshatra = balanceResult.birthNakshatra,
            nakshatraLord = balanceResult.nakshatraLord,
            startingMahadasha = balanceResult.startingPlanet,
            startingBalance = balanceResult.startingBalance,
            mahadashaPeriods = mahadashas,
            currentMahadasha = currentMahadasha,
            currentAntardasha = currentAntardasha,
            targetDateTime = targetDateTime,
            moonLongitude = moonLongitude,
            fractionElapsed = balanceResult.fractionElapsed,
            fractionRemaining = balanceResult.fractionRemaining,
            metadata = metadata
        )
    }

    /**
     * Reusable range comparison with half-open interval semantics [start, end)
     * and optional inclusive endpoint coverage for the final cycle boundary.
     */
    fun isDateTimeInRange(
        target: ZonedDateTime,
        start: ZonedDateTime,
        end: ZonedDateTime,
        isLast: Boolean = false
    ): Boolean {
        return if (isLast) {
            !target.isBefore(start) && !target.isAfter(end)
        } else {
            !target.isBefore(start) && target.isBefore(end)
        }
    }
}
