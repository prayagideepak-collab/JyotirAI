package com.example.domain.muhurta

import com.example.domain.models.*
import de.thmac.swisseph.SweConst
import de.thmac.swisseph.SweDate
import de.thmac.swisseph.SwissEph
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Calculates deterministic Vedic Personal Bala factors:
 * 1. Tara Bala (9-fold Star strength relative to Janma Nakshatra).
 * 2. Chandra Bala (Moon strength relative to Janma Rashi / Chandrashtama detection).
 */
object PersonalBalaCalculator {

    private val TARA_NAMES = listOf(
        "Janma (जन्म तारा)",       // 1: Mixed / Caution for physical strain
        "Sampat (सम्पत् तारा)",     // 2: Wealth & Prosperity (Favorable)
        "Vipat (विपत् तारा)",       // 3: Obstacles & Distress (Caution)
        "Kshema (क्षेम तारा)",       // 4: Well-being & Protection (Favorable)
        "Pratyak (प्रत्यक् तारा)",   // 5: Opposition & Blockades (Caution)
        "Sadhaka (साधक तारा)",     // 6: Achievement & Fulfillment (Favorable)
        "Vadha / Naidhana (वध तारा)", // 7: Strict Danger/Loss (Strict Caution)
        "Mitra (मित्र तारा)",       // 8: Friendly & Beneficial (Favorable)
        "Param Mitra (परम मित्र तारा)" // 9: Supreme Beneficence (Favorable)
    )

    private val FAVORABLE_TARA_INDICES = setOf(2, 4, 6, 8, 9)
    private val FAVORABLE_CHANDRA_HOUSES = setOf(1, 3, 6, 7, 10, 11)

    fun calculate(
        profile: UserProfile,
        transitMoonNakshatra: Nakshatra,
        transitMoonRashi: Rashi
    ): PersonalBalaContext {
        val birthData = profile.birthData
        val zonedDateTime = ZonedDateTime.of(birthData.date, birthData.time, birthData.timeZone)
        val utc = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC)
        val hourDecimalUt = utc.hour + (utc.minute / 60.0) + (utc.second / 3600.0) + (utc.nano / 3_600_000_000_000.0)
        val sweDate = SweDate(utc.year, utc.monthValue, utc.dayOfMonth, hourDecimalUt)
        val tjdUt = sweDate.julDay

        val swe = SwissEph().apply {
            swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0.0, 0.0)
        }
        val flags = SweConst.SEFLG_MOSEPH or SweConst.SEFLG_SIDEREAL or SweConst.SEFLG_SPEED
        val xx = DoubleArray(6)
        val serr = StringBuffer()
        swe.swe_calc_ut(tjdUt, SweConst.SE_MOON, flags, xx, serr)
        val natalMoonLon = (xx[0] % 360.0 + 360.0) % 360.0

        val (natalNakshatra, natalPada) = Nakshatra.fromLongitude(natalMoonLon)
        val natalRashi = Rashi.fromLongitude(natalMoonLon)

        // 1. Calculate Tara Bala
        val diff = (transitMoonNakshatra.index - natalNakshatra.index + 27) % 27
        val taraIndex = (diff % 9) + 1 // 1 to 9
        val taraName = TARA_NAMES[taraIndex - 1]
        val isTaraFavorable = taraIndex in FAVORABLE_TARA_INDICES

        // 2. Calculate Chandra Bala
        val chandraHouse = ((transitMoonRashi.index - natalRashi.index + 12) % 12) + 1
        val isChandraBalaFavorable = chandraHouse in FAVORABLE_CHANDRA_HOUSES
        val isChandrashtama = (chandraHouse == 8)

        val summary = buildString {
            append("Tara Bala: $taraName ")
            if (isTaraFavorable) append("(शुभ) | ") else append("(सावधानी) | ")
            append("Chandra Bala: ${chandraHouse}th House from ${natalRashi.sanskritName} ")
            if (isChandrashtama) {
                append("[चन्द्राष्टम - विशेष सावधानी]")
            } else if (isChandraBalaFavorable) {
                append("[शुभ चन्द्र बल]")
            } else {
                append("[मध्यम]")
            }
        }

        return PersonalBalaContext(
            profileName = profile.name,
            janmaNakshatra = natalNakshatra,
            janmaPada = natalPada,
            taraName = taraName,
            taraIndex = taraIndex,
            isTaraFavorable = isTaraFavorable,
            natalMoonRashi = natalRashi,
            transitMoonRashi = transitMoonRashi,
            chandraBalaHouse = chandraHouse,
            isChandraBalaFavorable = isChandraBalaFavorable,
            isChandrashtama = isChandrashtama,
            balaSummary = summary
        )
    }
}
