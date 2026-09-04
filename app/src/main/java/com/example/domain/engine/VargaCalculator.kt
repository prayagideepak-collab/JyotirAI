package com.example.domain.engine

import com.example.domain.models.*

/**
 * Deterministic Vedic Varga (Divisional Chart) Calculator.
 * 
 * Implements classical Parashari mathematical algorithms for all major Vargas:
 * - D1: Rashi (Natal Physical Body)
 * - D2: Hora (Wealth & Resources)
 * - D3: Drekkana (Siblings & Vitality)
 * - D4: Chaturthamsha (Fortune & Fixed Assets)
 * - D7: Saptamsha (Children & Creative Lineage)
 * - D9: Navamsha (Dharma, Soul Destiny & Partnerships)
 * - D10: Dashamsha (Career, Status, Karma & Accomplishments)
 * - D12: Dwadashamsha (Ancestry, Lineage & Parents)
 * 
 * All calculations consume the authoritative Nirayana Sidereal longitudes (Lahiri Ayanamsa)
 * computed by the core astrology engine, preserving astronomical invariants.
 */
object VargaCalculator {

    /**
     * Calculates any requested Varga chart for a given astrology profile.
     */
    fun calculateVargaChart(profile: AstrologyProfile, vargaType: VargaType): Chart {
        val vargaLagnaSignIndex = calculateVargaSign(profile.lagnaLongitude, vargaType)
        val vargaLagnaRashi = Rashi.fromIndex(vargaLagnaSignIndex)
        val vargaLagnaDegree = calculateVargaDegreeInSign(profile.lagnaLongitude, vargaType)
        val (vargaLagnaNakshatra, vargaLagnaPada) = Nakshatra.fromLongitude(profile.lagnaLongitude)

        val vargaPositions = profile.planetPositions.map { basePos ->
            val vargaSignIndex = calculateVargaSign(basePos.totalLongitude, vargaType)
            val vargaRashi = Rashi.fromIndex(vargaSignIndex)
            val vargaDegree = calculateVargaDegreeInSign(basePos.totalLongitude, vargaType)
            val vargaHouse = calculateWholeSignHouse(vargaSignIndex, vargaLagnaSignIndex)

            basePos.copy(
                sign = "${vargaRashi.sanskritName} (${vargaRashi.englishName})",
                signIndex = vargaSignIndex,
                degreeInSign = vargaDegree,
                house = vargaHouse
            )
        }

        return Chart(
            type = vargaType.code,
            positions = vargaPositions,
            vargaType = vargaType,
            title = "${vargaType.code} — ${vargaType.sanskritName}",
            sanskritTitle = vargaType.sanskritName,
            description = vargaType.significations,
            ascendantSign = "${vargaLagnaRashi.sanskritName} (${vargaLagnaRashi.englishName})",
            ascendantSignIndex = vargaLagnaSignIndex,
            ascendantDegreeInSign = vargaLagnaDegree,
            ascendantNakshatra = vargaLagnaNakshatra.sanskritName,
            ascendantPada = vargaLagnaPada
        )
    }

    /**
     * Computes the Varga Rashi (0..11) for a given sidereal longitude.
     */
    fun calculateVargaSign(longitude: Double, vargaType: VargaType): Int {
        val normLon = normalizeDegree(longitude)
        val baseSignIndex = (normLon / 30.0).toInt().coerceIn(0, 11)
        val degreeInSign = normLon - (baseSignIndex * 30.0)
        val isOddSign = baseSignIndex % 2 == 0 // 0=Aries (Odd), 1=Taurus (Even), 2=Gemini (Odd), ...

        return when (vargaType) {
            VargaType.D1 -> baseSignIndex

            VargaType.D2 -> {
                // Hora (15°):
                // Odd signs: 0-15° -> Sun (Leo/4), 15-30° -> Moon (Cancer/3)
                // Even signs: 0-15° -> Moon (Cancer/3), 15-30° -> Sun (Leo/4)
                if (isOddSign) {
                    if (degreeInSign < 15.0) 4 else 3
                } else {
                    if (degreeInSign < 15.0) 3 else 4
                }
            }

            VargaType.D3 -> {
                // Drekkana (10°):
                // 1st part (0-10°): same sign
                // 2nd part (10-20°): 5th from sign
                // 3rd part (20-30°): 9th from sign
                val part = (degreeInSign / 10.0).toInt().coerceIn(0, 2)
                (baseSignIndex + (part * 4)) % 12
            }

            VargaType.D4 -> {
                // Chaturthamsha (7°30'):
                // 1st part (0-7.5°): same sign
                // 2nd part: 4th from sign
                // 3rd part: 7th from sign
                // 4th part: 10th from sign
                val part = (degreeInSign / 7.5).toInt().coerceIn(0, 3)
                (baseSignIndex + (part * 3)) % 12
            }

            VargaType.D7 -> {
                // Saptamsha (4°17'08.57"):
                // Odd signs: starts from same sign
                // Even signs: starts from 7th from sign (signIndex + 6)
                val part = (degreeInSign / (30.0 / 7.0)).toInt().coerceIn(0, 6)
                if (isOddSign) {
                    (baseSignIndex + part) % 12
                } else {
                    ((baseSignIndex + 6) + part) % 12
                }
            }

            VargaType.D9 -> {
                // Navamsha (3°20' = 3.3333333333333335°):
                // In continuous zodiac: 108 padas of 3°20' each starting from Aries (0).
                // Mathematically: padaIndex = (normLon / 3.3333333333333335).toInt() % 12
                val padaIndex = (normLon / (360.0 / 108.0)).toInt().coerceIn(0, 107)
                padaIndex % 12
            }

            VargaType.D10 -> {
                // Dashamsha (3°00'):
                // Odd signs: starts from same sign
                // Even signs: starts from 9th sign from it (signIndex + 8)
                val part = (degreeInSign / 3.0).toInt().coerceIn(0, 9)
                if (isOddSign) {
                    (baseSignIndex + part) % 12
                } else {
                    ((baseSignIndex + 8) + part) % 12
                }
            }

            VargaType.D12 -> {
                // Dwadashamsha (2°30' = 2.5°):
                // Starts from same sign and progresses consecutively
                val part = (degreeInSign / 2.5).toInt().coerceIn(0, 11)
                (baseSignIndex + part) % 12
            }

            else -> baseSignIndex
        }
    }

    /**
     * Calculates the degree within the Varga sign (scaled to 0.0..<30.0 for visual fidelity).
     */
    fun calculateVargaDegreeInSign(longitude: Double, vargaType: VargaType): Double {
        val normLon = normalizeDegree(longitude)
        val baseSignIndex = (normLon / 30.0).toInt().coerceIn(0, 11)
        val degreeInSign = normLon - (baseSignIndex * 30.0)

        val divisionSpan = 30.0 / vargaType.division
        val degreeInDivision = degreeInSign % divisionSpan
        return (degreeInDivision / divisionSpan) * 30.0
    }

    private fun calculateWholeSignHouse(planetSignIndex: Int, lagnaSignIndex: Int): Int {
        return ((planetSignIndex - lagnaSignIndex).mod(12)) + 1
    }

    private fun normalizeDegree(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }
}
