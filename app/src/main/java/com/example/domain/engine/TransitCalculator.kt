package com.example.domain.engine

import com.example.domain.models.TransitPosition
import com.example.domain.models.TransitRelation

/**
 * Pure calculation utilities for Vedic Planetary Transits (Gochar).
 * 
 * Rules & Conventions:
 * 1. Sign-Based Gochar: Relative house position is calculated whole-sign from reference:
 *    house = ((transitSignIndex - referenceSignIndex).mod(12)) + 1
 *    where index 0 is Aries (Mesha) ... index 11 is Pisces (Meena).
 * 2. Retrograde Determinism:
 *    - Sun & Moon: Never retrograde (always direct).
 *    - Rahu & Ketu: Mean lunar nodes are uniformly retrograde in Vedic astrology.
 *    - Mars, Mercury, Jupiter, Venus, Saturn: Retrograde if and only if sidereal daily speed < 0.
 * 3. Longitude Normalization: All coordinates map to [0°, 360°).
 */
object TransitCalculator {

    /**
     * Calculates the whole-sign relative house (1 to 12) from a reference sign.
     */
    fun calculateRelativeHouse(transitSignIndex: Int, referenceSignIndex: Int): Int {
        return ((transitSignIndex - referenceSignIndex).mod(12)) + 1
    }

    /**
     * Centralized rule to determine planetary retrograde status.
     */
    fun isPlanetRetrograde(planet: String, speed: Double): Boolean = when (planet.lowercase().trim()) {
        "sun", "surya" -> false
        "moon", "chandra" -> false
        "rahu", "ketu" -> true
        else -> speed < 0.0
    }

    /**
     * Normalizes an angular degree into the standard astronomical interval [0.0, 360.0).
     */
    fun normalizeDegree(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0.0) d += 360.0
        if (d >= 360.0) d = 0.0
        return d
    }

    /**
     * Returns the English ordinal string for a house number (1 -> "1st", 2 -> "2nd", etc.)
     */
    fun getHouseOrdinal(house: Int): String = when (house) {
        1 -> "1st"
        2 -> "2nd"
        3 -> "3rd"
        4 -> "4th"
        5 -> "5th"
        6 -> "6th"
        7 -> "7th"
        8 -> "8th"
        9 -> "9th"
        10 -> "10th"
        11 -> "11th"
        12 -> "12th"
        else -> "${house}th"
    }

    /**
     * Traditional Vedic significance title for house positions in Gochar.
     */
    fun getVedicHouseSignification(house: Int): String = when (house) {
        1 -> "Janma / Tanu (Self & Identity)"
        2 -> "Dhana (Wealth & Speech)"
        3 -> "Bhratri / Sahaja (Effort & Courage)"
        4 -> "Sukha / Bandhu (Home & Mind)"
        5 -> "Putra (Intellect & Creativity)"
        6 -> "Ari / Shatru (Obstacles & Service)"
        7 -> "Yuvati / Jaya (Partnership & Relations)"
        8 -> "Randhra / Ashtama (Transformation)"
        9 -> "Dharma / Bhagya (Fortune & Wisdom)"
        10 -> "Karma (Status & Action)"
        11 -> "Labha (Gains & Fulfillment)"
        12 -> "Vyaya (Expenditure & Solitude)"
        else -> "Bhava $house"
    }

    /**
     * Builds a structured TransitRelation descriptor from a TransitPosition.
     */
    fun buildTransitRelation(position: TransitPosition): TransitRelation {
        val moonDesc = position.houseFromMoon?.let {
            "${getHouseOrdinal(it)} House (${getVedicHouseSignification(it)}) from Moon"
        }
        val lagnaDesc = position.houseFromLagna?.let {
            "${getHouseOrdinal(it)} House (${getVedicHouseSignification(it)}) from Lagna"
        }
        return TransitRelation(
            planet = position.planet,
            transitSign = position.sign,
            transitDegreeFormatted = position.formattedDegree,
            houseFromMoon = position.houseFromMoon,
            houseFromLagna = position.houseFromLagna,
            isRetrograde = position.isRetrograde,
            moonRelationDescription = moonDesc,
            lagnaRelationDescription = lagnaDesc
        )
    }
}
