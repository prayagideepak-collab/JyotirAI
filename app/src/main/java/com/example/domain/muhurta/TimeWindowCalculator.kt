package com.example.domain.muhurta

import com.example.domain.models.*
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Calculates astronomical time windows for candidate Muhurtas:
 * 1. Brahma Muhurta (Pre-dawn sattva period).
 * 2. Abhijit Muhurta (Midday victory window, ~8th muhurta).
 * 3. Day Choghadiya segments (Amrit, Shubh, Labh, Char, Rog, Kaal, Udveg).
 * 4. Inauspicious segments (Rahukaal, Yamaganda, Gulika Kaal, Durmuhurta).
 */
object TimeWindowCalculator {

    enum class ChoghadiyaType(val hindi: String, val english: String, val isAuspicious: Boolean, val weight: Double) {
        AMRIT("अमृत चौघड़िया", "Amrit", true, 1.3),
        SHUBH("शुभ चौघड़िया", "Shubh", true, 1.25),
        LABH("लाभ चौघड़िया", "Labh", true, 1.2),
        CHAR("चर चौघड़िया", "Char", true, 1.05),
        ROG("रोग चौघड़िया", "Rog", false, 0.4),
        KAAL("काल चौघड़िया", "Kaal", false, 0.3),
        UDVEG("उद्वेग चौघड़िया", "Udveg", false, 0.35)
    }

    // Day Choghadiya sequences by weekday (Vara)
    private val DAY_CHOGHADIYA_ORDER = mapOf(
        Vara.RAVIVARA to listOf(
            ChoghadiyaType.UDVEG, ChoghadiyaType.CHAR, ChoghadiyaType.LABH,
            ChoghadiyaType.AMRIT, ChoghadiyaType.KAAL, ChoghadiyaType.SHUBH,
            ChoghadiyaType.ROG, ChoghadiyaType.UDVEG
        ),
        Vara.SOMAVARA to listOf(
            ChoghadiyaType.AMRIT, ChoghadiyaType.KAAL, ChoghadiyaType.SHUBH,
            ChoghadiyaType.ROG, ChoghadiyaType.UDVEG, ChoghadiyaType.CHAR,
            ChoghadiyaType.LABH, ChoghadiyaType.AMRIT
        ),
        Vara.MANGALAVARA to listOf(
            ChoghadiyaType.ROG, ChoghadiyaType.UDVEG, ChoghadiyaType.CHAR,
            ChoghadiyaType.LABH, ChoghadiyaType.AMRIT, ChoghadiyaType.KAAL,
            ChoghadiyaType.SHUBH, ChoghadiyaType.ROG
        ),
        Vara.BUDHAVARA to listOf(
            ChoghadiyaType.LABH, ChoghadiyaType.AMRIT, ChoghadiyaType.KAAL,
            ChoghadiyaType.SHUBH, ChoghadiyaType.ROG, ChoghadiyaType.UDVEG,
            ChoghadiyaType.CHAR, ChoghadiyaType.LABH
        ),
        Vara.GURUVARA to listOf(
            ChoghadiyaType.SHUBH, ChoghadiyaType.ROG, ChoghadiyaType.UDVEG,
            ChoghadiyaType.CHAR, ChoghadiyaType.LABH, ChoghadiyaType.AMRIT,
            ChoghadiyaType.KAAL, ChoghadiyaType.SHUBH
        ),
        Vara.SHUKRAVARA to listOf(
            ChoghadiyaType.CHAR, ChoghadiyaType.LABH, ChoghadiyaType.AMRIT,
            ChoghadiyaType.KAAL, ChoghadiyaType.SHUBH, ChoghadiyaType.ROG,
            ChoghadiyaType.UDVEG, ChoghadiyaType.CHAR
        ),
        Vara.SHANIVARA to listOf(
            ChoghadiyaType.KAAL, ChoghadiyaType.SHUBH, ChoghadiyaType.ROG,
            ChoghadiyaType.UDVEG, ChoghadiyaType.CHAR, ChoghadiyaType.LABH,
            ChoghadiyaType.AMRIT, ChoghadiyaType.KAAL
        )
    )

    fun calculateWindowsForDay(
        panchang: PanchangResult,
        preferredTimeSlot: TimeSlotPreference = TimeSlotPreference.ALL_DAY
    ): Pair<List<RawTimeWindow>, List<InauspiciousInterval>> {
        val sunrise = panchang.sunrise ?: panchang.calculationTimestamp.toLocalDate().atTime(6, 0).atZone(panchang.calculationTimestamp.zone)
        val sunset = panchang.sunset ?: panchang.calculationTimestamp.toLocalDate().atTime(18, 0).atZone(panchang.calculationTimestamp.zone)
        val dayDuration = Duration.between(sunrise, sunset)
        val dayDurationMillis = dayDuration.toMillis().coerceAtLeast(1000L)
        val localDate = panchang.selectedDate
        val vara = panchang.vara

        val rawWindows = mutableListOf<RawTimeWindow>()
        val inauspiciousIntervals = mutableListOf<InauspiciousInterval>()

        // 1. Brahma Muhurta (96m to 48m before sunrise)
        val brahmaStart = sunrise.minusMinutes(96)
        val brahmaEnd = sunrise.minusMinutes(48)
        rawWindows.add(
            RawTimeWindow(
                id = "brahma_${localDate}",
                name = "Brahma Muhurta",
                sanskritName = "ब्रह्म मुहूर्त",
                startTime = brahmaStart,
                endTime = brahmaEnd,
                localDate = localDate,
                isInherentlyAuspicious = true,
                isCautionWindow = false,
                baseWeight = 1.35,
                description = "Sacred dawn window (96m-48m before sunrise). Supreme for meditation, study, spiritual rites, and health regimens."
            )
        )

        // 2. Pratah Sandhya / Dawn Window
        val dawnStart = sunrise.minusMinutes(48)
        rawWindows.add(
            RawTimeWindow(
                id = "dawn_${localDate}",
                name = "Pratah Sandhya",
                sanskritName = "प्रातः सन्ध्या",
                startTime = dawnStart,
                endTime = sunrise,
                localDate = localDate,
                isInherentlyAuspicious = true,
                isCautionWindow = false,
                baseWeight = 1.15,
                description = "Twilight hour preceding sunrise, favorable for morning worship and contemplation."
            )
        )

        // 3. Abhijit Muhurta (8th of 15 day divisions)
        val muhurtaDurationMillis = dayDurationMillis / 15
        val abhijitStart = sunrise.plus(Duration.ofMillis(muhurtaDurationMillis * 7))
        val abhijitEnd = sunrise.plus(Duration.ofMillis(muhurtaDurationMillis * 8))

        // Note: Classical exception: Abhijit is deemed dosha-affected on Wednesday (Budhavara)
        val isAbhijitFlawedOnWednesday = (vara == Vara.BUDHAVARA)
        rawWindows.add(
            RawTimeWindow(
                id = "abhijit_${localDate}",
                name = "Abhijit Muhurta",
                sanskritName = "अभिजित् मुहूर्त",
                startTime = abhijitStart,
                endTime = abhijitEnd,
                localDate = localDate,
                isInherentlyAuspicious = !isAbhijitFlawedOnWednesday,
                isCautionWindow = false,
                baseWeight = if (isAbhijitFlawedOnWednesday) 0.9 else 1.4,
                description = if (isAbhijitFlawedOnWednesday) {
                    "Midday Abhijit Muhurta. On Wednesdays, classical texts caution against travel/beginnings due to Budha-Abhijit dosha."
                } else {
                    "Most potent midday auspicious window. Removes multiple Panchang defects for all major beginnings."
                }
            )
        )

        // 4. Inauspicious Intervals:
        // Rahukaal
        val (rahuStart, rahuEnd) = calculateOctantInterval(sunrise, dayDurationMillis, getRahuOctantIndex(vara))
        inauspiciousIntervals.add(
            InauspiciousInterval(
                name = "Rahukaal",
                sanskritName = "राहुकाल",
                startTime = rahuStart,
                endTime = rahuEnd,
                reason = "Inauspicious planetary hour ruled by Rahu. Avoid commencing new ventures, travel, or signing contracts."
            )
        )

        // Yamaganda
        val (yamaStart, yamaEnd) = calculateOctantInterval(sunrise, dayDurationMillis, getYamaOctantIndex(vara))
        inauspiciousIntervals.add(
            InauspiciousInterval(
                name = "Yamaganda Kaal",
                sanskritName = "यमगण्ड काल",
                startTime = yamaStart,
                endTime = yamaEnd,
                reason = "Inauspicious hour ruled by Yama. Inauspicious for starting projects or financial investments."
            )
        )

        // Gulika Kaal
        val (gulikaStart, gulikaEnd) = calculateOctantInterval(sunrise, dayDurationMillis, getGulikaOctantIndex(vara))
        inauspiciousIntervals.add(
            InauspiciousInterval(
                name = "Gulika Kaal",
                sanskritName = "गुलिक काल",
                startTime = gulikaStart,
                endTime = gulikaEnd,
                reason = "Hour ruled by Gulika (son of Saturn). Neutral for routine ongoing work, avoided for sacred samskaras."
            )
        )

        // Durmuhurta
        val durmuhurtaWindows = calculateDurmuhurta(sunrise, muhurtaDurationMillis, vara, localDate)
        durmuhurtaWindows.forEach { dur ->
            inauspiciousIntervals.add(
                InauspiciousInterval(
                    name = dur.name,
                    sanskritName = dur.sanskritName,
                    startTime = dur.startTime,
                    endTime = dur.endTime,
                    reason = "Unfavorable weekday muhurta division (दुर्मुहूर्त). Avoid new beginnings during this span."
                )
            )
        }

        // 5. Day Choghadiya Windows (8 segments from sunrise to sunset)
        val choghadiyaList = DAY_CHOGHADIYA_ORDER[vara] ?: DAY_CHOGHADIYA_ORDER[Vara.RAVIVARA]!!
        val choghadiyaDurationMillis = dayDurationMillis / 8

        for (i in 0 until 8) {
            val type = choghadiyaList[i]
            val cStart = sunrise.plus(Duration.ofMillis(choghadiyaDurationMillis * i))
            val cEnd = sunrise.plus(Duration.ofMillis(choghadiyaDurationMillis * (i + 1)))

            rawWindows.add(
                RawTimeWindow(
                    id = "choghadiya_${i}_${localDate}",
                    name = "${type.english} Choghadiya",
                    sanskritName = type.hindi,
                    startTime = cStart,
                    endTime = cEnd,
                    localDate = localDate,
                    isInherentlyAuspicious = type.isAuspicious,
                    isCautionWindow = !type.isAuspicious,
                    baseWeight = type.weight,
                    description = "${type.hindi} day segment. ${if (type.isAuspicious) "Favorable for active execution and beginnings." else "Cautionary span; perform routine tasks only."}"
                )
            )
        }

        // 6. Filter by preferred time slot if requested
        val filteredWindows = if (preferredTimeSlot == TimeSlotPreference.ALL_DAY) {
            rawWindows
        } else {
            rawWindows.filter { window ->
                val hour = window.startTime.hour
                hour in preferredTimeSlot.startHour until preferredTimeSlot.endHour
            }
        }

        return Pair(filteredWindows, inauspiciousIntervals)
    }

    private fun calculateOctantInterval(
        sunrise: ZonedDateTime,
        dayDurationMillis: Long,
        octantIndex: Int // 0 to 7
    ): Pair<ZonedDateTime, ZonedDateTime> {
        val octantMillis = dayDurationMillis / 8
        val start = sunrise.plus(Duration.ofMillis(octantMillis * octantIndex))
        val end = sunrise.plus(Duration.ofMillis(octantMillis * (octantIndex + 1)))
        return Pair(start, end)
    }

    private fun getRahuOctantIndex(vara: Vara): Int {
        return when (vara) {
            Vara.RAVIVARA -> 7
            Vara.SOMAVARA -> 1
            Vara.MANGALAVARA -> 6
            Vara.BUDHAVARA -> 4
            Vara.GURUVARA -> 5
            Vara.SHUKRAVARA -> 3
            Vara.SHANIVARA -> 2
        }
    }

    private fun getYamaOctantIndex(vara: Vara): Int {
        return when (vara) {
            Vara.RAVIVARA -> 4
            Vara.SOMAVARA -> 3
            Vara.MANGALAVARA -> 2
            Vara.BUDHAVARA -> 1
            Vara.GURUVARA -> 0
            Vara.SHUKRAVARA -> 6
            Vara.SHANIVARA -> 5
        }
    }

    private fun getGulikaOctantIndex(vara: Vara): Int {
        return when (vara) {
            Vara.RAVIVARA -> 6
            Vara.SOMAVARA -> 5
            Vara.MANGALAVARA -> 4
            Vara.BUDHAVARA -> 3
            Vara.GURUVARA -> 2
            Vara.SHUKRAVARA -> 1
            Vara.SHANIVARA -> 0
        }
    }

    private fun calculateDurmuhurta(
        sunrise: ZonedDateTime,
        muhurtaMillis: Long,
        vara: Vara,
        localDate: LocalDate
    ): List<RawTimeWindow> {
        val indices = when (vara) {
            Vara.RAVIVARA -> listOf(13)
            Vara.SOMAVARA -> listOf(7, 8)
            Vara.MANGALAVARA -> listOf(3, 10)
            Vara.BUDHAVARA -> listOf(7)
            Vara.GURUVARA -> listOf(11, 12)
            Vara.SHUKRAVARA -> listOf(3, 8)
            Vara.SHANIVARA -> listOf(1, 2)
        }

        return indices.map { index ->
            val start = sunrise.plus(Duration.ofMillis(muhurtaMillis * index))
            val end = sunrise.plus(Duration.ofMillis(muhurtaMillis * (index + 1)))
            RawTimeWindow(
                id = "durmuhurta_${index}_${localDate}",
                name = "Durmuhurta (${index + 1}th)",
                sanskritName = "दुर्मुहूर्त",
                startTime = start,
                endTime = end,
                localDate = localDate,
                isInherentlyAuspicious = false,
                isCautionWindow = true,
                baseWeight = 0.2,
                description = "Durmuhurta division on ${vara.sanskritName}."
            )
        }
    }

    fun hasOverlap(start1: ZonedDateTime, end1: ZonedDateTime, start2: ZonedDateTime, end2: ZonedDateTime): Boolean {
        return start1.isBefore(end2) && end1.isAfter(start2)
    }
}
