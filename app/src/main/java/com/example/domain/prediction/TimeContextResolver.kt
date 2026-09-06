package com.example.domain.prediction

import com.example.domain.models.*
import java.time.LocalDate
import java.time.YearMonth

/**
 * Resolves exact temporal boundaries and detects Dasha/Transit transitions for Daily, Monthly, and Yearly contexts.
 */
object TimeContextResolver {

    fun resolve(
        periodType: PredictionPeriodType,
        targetDate: LocalDate,
        birthData: BirthData,
        dashaTimeline: DashaTimeline?
    ): PeriodTimeContext {
        val targetYear = targetDate.year
        val targetMonth = targetDate.monthValue
        val targetDay = targetDate.dayOfMonth

        val (startDate, endDate) = when (periodType) {
            PredictionPeriodType.DAILY -> {
                targetDate to targetDate
            }
            PredictionPeriodType.MONTHLY -> {
                val yearMonth = YearMonth.of(targetYear, targetMonth)
                val start = yearMonth.atDay(1)
                val end = yearMonth.atEndOfMonth()
                start to end
            }
            PredictionPeriodType.YEARLY -> {
                val start = LocalDate.of(targetYear, 1, 1)
                val end = LocalDate.of(targetYear, 12, 31)
                start to end
            }
        }

        val zoneId = birthData.timeZone

        val activeMaha = dashaTimeline?.currentMahadasha?.planet?.lord
            ?: dashaTimeline?.startingMahadasha?.lord
            ?: "Unknown"
        val activeAntar = dashaTimeline?.currentAntardasha?.antardashaLord?.lord
            ?: activeMaha

        val mahaEnd = dashaTimeline?.currentMahadasha?.endDate?.toLocalDate()
        val antarEnd = dashaTimeline?.currentAntardasha?.endDate?.toLocalDate()

        // Detect any Dasha / Antardasha transitions within the period [startDate, endDate]
        val transitions = mutableListOf<PeriodTransitionInfo>()
        if (dashaTimeline != null) {
            val periods = dashaTimeline.mahadashaPeriods
            periods.forEachIndexed { index, md ->
                val mdStart = md.startDate.toLocalDate()

                // Check Mahadasha transition
                if (!mdStart.isBefore(startDate) && !mdStart.isAfter(endDate) && index > 0) {
                    val prevLord = periods[index - 1].planet.lord
                    transitions.add(
                        PeriodTransitionInfo(
                            transitionDate = mdStart,
                            transitionType = "Mahadasha Ingress",
                            description = "Major shift from $prevLord Mahadasha to ${md.planet.lord} Mahadasha",
                            fromLordOrSign = prevLord,
                            toLordOrSign = md.planet.lord
                        )
                    )
                }

                // Check Antardashas within Mahadashas
                md.antardashas.forEachIndexed { aIndex, ad ->
                    val adStart = ad.startDate.toLocalDate()
                    if (!adStart.isBefore(startDate) && !adStart.isAfter(endDate) && (index > 0 || aIndex > 0)) {
                        val prevAdLord = if (aIndex > 0) {
                            md.antardashas[aIndex - 1].antardashaLord.lord
                        } else if (index > 0) {
                            periods[index - 1].antardashas.lastOrNull()?.antardashaLord?.lord ?: "Previous Lord"
                        } else "Previous Lord"

                        transitions.add(
                            PeriodTransitionInfo(
                                transitionDate = adStart,
                                transitionType = "Antardasha Transition",
                                description = "Sub-period transition into ${ad.antardashaLord.lord} Antardasha under ${md.planet.lord}",
                                fromLordOrSign = prevAdLord,
                                toLordOrSign = ad.antardashaLord.lord
                            )
                        )
                    }
                }
            }
        }

        return PeriodTimeContext(
            periodType = periodType,
            targetDate = targetDate,
            targetYear = targetYear,
            targetMonth = targetMonth,
            targetDay = targetDay,
            startDate = startDate,
            endDate = endDate,
            calculationTimeZone = zoneId,
            activeMahadasha = activeMaha,
            activeAntardasha = activeAntar,
            mahadashaEndDate = mahaEnd,
            antardashaEndDate = antarEnd,
            periodTransitions = transitions
        )
    }
}
