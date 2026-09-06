package com.example.domain.muhurta

import com.example.domain.models.*

/**
 * Activity rule profile defining favorable astronomical elements,
 * classical constraints, and directional guidelines.
 */
data class ActivityRuleProfile(
    val activityType: MuhurtaActivityType,
    val favorableTithis: Set<Int>, // 1 to 30
    val avoidedTithis: Set<Int>, // Rikta (4, 9, 14, 19, 24, 29), Amavasya (30) etc.
    val favorableNakshatras: Set<Nakshatra>,
    val favorableVaras: Set<Vara>,
    val avoidedVaras: Set<Vara>,
    val requiresShuklaPakshaPreference: Boolean = false,
    val prohibitedDirectionsForTravel: Map<Vara, String> = emptyMap(), // Disha Shool
    val classicalNotes: String
)

/**
 * Resolves classical Vedic astrological rule criteria for each specific activity.
 */
object ActivityContextResolver {

    // Rikta Tithis (4, 9, 14 in Shukla; 19, 24, 29 in Krishna) and Amavasya (30)
    val RIKTA_TITHIS = setOf(4, 9, 14, 19, 24, 29)
    val AMAVASYA_TITHI = setOf(30)
    val STANDARD_AVOIDED_TITHIS = RIKTA_TITHIS + AMAVASYA_TITHI

    // 27 Vedic Nakshatra Groups
    // Sthira (Fixed): Rohini, Uttara Phalguni, Uttara Ashadha, Uttara Bhadrapada
    val STHIRA_NAKSHATRAS = setOf(
        Nakshatra.ROHINI,
        Nakshatra.UTTARA_PHALGUNI,
        Nakshatra.UTTARA_ASHADHA,
        Nakshatra.UTTARA_BHADRAPADA
    )

    // Chara (Movable): Punarvasu, Swati, Shravana, Dhanishta, Shatabhisha
    val CHARA_NAKSHATRAS = setOf(
        Nakshatra.PUNARVASU,
        Nakshatra.SWATI,
        Nakshatra.SHRAVANA,
        Nakshatra.DHANISHTA,
        Nakshatra.SHATABHISHA
    )

    // Laghu / Kshipra (Quick/Light): Ashwini, Pushya, Hasta
    val LAGHU_NAKSHATRAS = setOf(
        Nakshatra.ASHWINI,
        Nakshatra.PUSHYA,
        Nakshatra.HASTA
    )

    // Mridu (Soft/Gentle): Mrigashirsha, Chitra, Anuradha, Revati
    val MRIDU_NAKSHATRAS = setOf(
        Nakshatra.MRIGASHIRSHA,
        Nakshatra.CHITRA,
        Nakshatra.ANURADHA,
        Nakshatra.REVATI
    )

    // Mishra (Mixed): Krittika, Vishakha
    val MISHRA_NAKSHATRAS = setOf(
        Nakshatra.KRITTIKA,
        Nakshatra.VISHAKHA
    )

    // Inauspicious Nitya Yogas (Panchang)
    val INAUSPICIOUS_YOGAS = setOf(
        1,  // Vishkambha (first 3 ghatis)
        6,  // Atiganda
        9,  // Shula
        10, // Ganda
        13, // Vyaghata
        15, // Vajra
        17, // Vyatipata
        19, // Parigha (first half)
        27  // Vaidhriti
    )

    // Disha Shool (Inauspicious travel direction by Day)
    private val DISHA_SHOOL_MAP = mapOf(
        Vara.RAVIVARA to "West (पश्चिम)",
        Vara.SOMAVARA to "East (पूर्व)",
        Vara.MANGALAVARA to "North (उत्तर)",
        Vara.BUDHAVARA to "North (उत्तर)",
        Vara.GURUVARA to "South (दक्षिण)",
        Vara.SHUKRAVARA to "West (पश्चिम)",
        Vara.SHANIVARA to "East (पूर्व)"
    )

    fun getRuleProfile(activityType: MuhurtaActivityType): ActivityRuleProfile {
        return when (activityType) {
            MuhurtaActivityType.GENERAL_AUSPICIOUS -> ActivityRuleProfile(
                activityType = activityType,
                favorableTithis = setOf(2, 3, 5, 7, 10, 11, 12, 13, 15, 17, 18, 20, 22, 25, 27, 28),
                avoidedTithis = STANDARD_AVOIDED_TITHIS,
                favorableNakshatras = STHIRA_NAKSHATRAS + CHARA_NAKSHATRAS + LAGHU_NAKSHATRAS + MRIDU_NAKSHATRAS,
                favorableVaras = setOf(Vara.SOMAVARA, Vara.BUDHAVARA, Vara.GURUVARA, Vara.SHUKRAVARA, Vara.RAVIVARA),
                avoidedVaras = emptySet(),
                classicalNotes = "General auspicious acts thrive during Shubha tithis and favorable lunar mansions, avoiding Rahukaal, Vishti Karana, and Rikta tithis."
            )

            MuhurtaActivityType.TRAVEL -> ActivityRuleProfile(
                activityType = activityType,
                favorableTithis = setOf(2, 3, 5, 7, 10, 11, 13, 17, 18, 20, 22, 25, 27, 28),
                avoidedTithis = STANDARD_AVOIDED_TITHIS + setOf(8, 12),
                favorableNakshatras = CHARA_NAKSHATRAS + LAGHU_NAKSHATRAS + setOf(
                    Nakshatra.MRIGASHIRSHA,
                    Nakshatra.ANURADHA,
                    Nakshatra.REVATI
                ),
                favorableVaras = setOf(Vara.SOMAVARA, Vara.BUDHAVARA, Vara.GURUVARA, Vara.SHUKRAVARA),
                avoidedVaras = setOf(Vara.SHANIVARA, Vara.MANGALAVARA),
                prohibitedDirectionsForTravel = DISHA_SHOOL_MAP,
                classicalNotes = "Travel requires Chara (movable) and Laghu (swift) Nakshatras. Check Disha Shool directional prohibitions for the day."
            )

            MuhurtaActivityType.EDUCATION -> ActivityRuleProfile(
                activityType = activityType,
                favorableTithis = setOf(2, 3, 5, 7, 10, 11, 12, 13, 15, 17, 18, 20, 22, 25, 27),
                avoidedTithis = STANDARD_AVOIDED_TITHIS + setOf(1, 8),
                favorableNakshatras = LAGHU_NAKSHATRAS + MRIDU_NAKSHATRAS + STHIRA_NAKSHATRAS + setOf(
                    Nakshatra.PUNARVASU,
                    Nakshatra.SWATI,
                    Nakshatra.SHRAVANA,
                    Nakshatra.DHANISHTA,
                    Nakshatra.SHATABHISHA
                ),
                favorableVaras = setOf(Vara.BUDHAVARA, Vara.GURUVARA, Vara.SHUKRAVARA, Vara.RAVIVARA),
                avoidedVaras = setOf(Vara.MANGALAVARA),
                classicalNotes = "Vidyarambha benefits from Jupiter (Guru) and Mercury (Budha) days, Swift/Gentle Nakshatras, and avoiding Rikta tithis."
            )

            MuhurtaActivityType.BUSINESS -> ActivityRuleProfile(
                activityType = activityType,
                favorableTithis = setOf(2, 3, 5, 7, 10, 11, 12, 13, 15, 17, 18, 20, 22, 25, 27),
                avoidedTithis = STANDARD_AVOIDED_TITHIS,
                favorableNakshatras = STHIRA_NAKSHATRAS + LAGHU_NAKSHATRAS + setOf(
                    Nakshatra.CHITRA,
                    Nakshatra.SWATI,
                    Nakshatra.ANURADHA,
                    Nakshatra.SHRAVANA,
                    Nakshatra.REVATI
                ),
                favorableVaras = setOf(Vara.BUDHAVARA, Vara.GURUVARA, Vara.SHUKRAVARA, Vara.SOMAVARA),
                avoidedVaras = setOf(Vara.SHANIVARA),
                requiresShuklaPakshaPreference = true,
                classicalNotes = "Vyapar Arambha prospers in Shukla Paksha, Sthira/Laghu Nakshatras, and during Mercury/Jupiter favorable hours."
            )

            MuhurtaActivityType.PROPERTY_HOME -> ActivityRuleProfile(
                activityType = activityType,
                favorableTithis = setOf(2, 3, 5, 7, 10, 11, 12, 13, 15, 17, 18, 20, 22, 25, 27),
                avoidedTithis = STANDARD_AVOIDED_TITHIS + setOf(1, 6, 8),
                favorableNakshatras = STHIRA_NAKSHATRAS + setOf(
                    Nakshatra.MRIGASHIRSHA,
                    Nakshatra.CHITRA,
                    Nakshatra.ANURADHA,
                    Nakshatra.REVATI,
                    Nakshatra.PUSHYA
                ),
                favorableVaras = setOf(Vara.GURUVARA, Vara.SHUKRAVARA, Vara.SOMAVARA, Vara.BUDHAVARA),
                avoidedVaras = setOf(Vara.MANGALAVARA, Vara.SHANIVARA, Vara.RAVIVARA),
                requiresShuklaPakshaPreference = true,
                classicalNotes = "Griha Pravesha and Vastu ceremonies require Fixed (Sthira) Nakshatras, Shukla Paksha, and auspicious planetary days."
            )

            MuhurtaActivityType.CEREMONY_PUJA -> ActivityRuleProfile(
                activityType = activityType,
                favorableTithis = setOf(2, 3, 5, 7, 10, 11, 12, 13, 15, 17, 18, 20, 22, 25, 27, 28),
                avoidedTithis = setOf(4, 9, 14, 19, 24, 29), // Rikta
                favorableNakshatras = STHIRA_NAKSHATRAS + MRIDU_NAKSHATRAS + LAGHU_NAKSHATRAS + setOf(
                    Nakshatra.PUNARVASU,
                    Nakshatra.SHRAVANA
                ),
                favorableVaras = setOf(Vara.GURUVARA, Vara.SOMAVARA, Vara.RAVIVARA, Vara.BUDHAVARA, Vara.SHUKRAVARA),
                avoidedVaras = emptySet(),
                classicalNotes = "Sacred ceremonies and worship excel in Brahma Muhurta, Abhijit Muhurta, and during Shukla Paksha auspicious Tithis."
            )

            MuhurtaActivityType.VEHICLE_PURCHASE -> ActivityRuleProfile(
                activityType = activityType,
                favorableTithis = setOf(2, 3, 5, 7, 10, 11, 12, 13, 15, 17, 18, 20, 22, 25, 27),
                avoidedTithis = STANDARD_AVOIDED_TITHIS + setOf(8),
                favorableNakshatras = CHARA_NAKSHATRAS + LAGHU_NAKSHATRAS + setOf(
                    Nakshatra.MRIGASHIRSHA,
                    Nakshatra.CHITRA,
                    Nakshatra.ANURADHA,
                    Nakshatra.REVATI
                ),
                favorableVaras = setOf(Vara.BUDHAVARA, Vara.GURUVARA, Vara.SHUKRAVARA, Vara.SOMAVARA),
                avoidedVaras = setOf(Vara.MANGALAVARA, Vara.SHANIVARA),
                classicalNotes = "Vahan Kraya favors Movable (Chara) and Swift (Laghu) Nakshatras, especially on Venus (Shukra) and Mercury (Budha) days."
            )

            MuhurtaActivityType.MEDICAL_HEALING -> ActivityRuleProfile(
                activityType = activityType,
                favorableTithis = setOf(2, 3, 5, 7, 8, 10, 11, 12, 13, 15, 17, 18, 20, 22, 23, 25, 27),
                avoidedTithis = setOf(4, 9, 14, 19, 24, 29, 30),
                favorableNakshatras = LAGHU_NAKSHATRAS + CHARA_NAKSHATRAS + setOf(
                    Nakshatra.MRIGASHIRSHA,
                    Nakshatra.CHITRA,
                    Nakshatra.ANURADHA,
                    Nakshatra.REVATI,
                    Nakshatra.ROHINI
                ),
                favorableVaras = setOf(Vara.RAVIVARA, Vara.SOMAVARA, Vara.BUDHAVARA, Vara.GURUVARA),
                avoidedVaras = setOf(Vara.MANGALAVARA, Vara.SHANIVARA),
                classicalNotes = "Commencing health recovery and medical treatments thrives under Ashwini (the divine physicians), Pushya, and Swift constellations."
            )
        }
    }
}
