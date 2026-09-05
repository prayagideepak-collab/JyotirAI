package com.example.domain.interpretation

import com.example.domain.engine.VargaCalculator
import com.example.domain.models.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Authoritative deterministic Vedic Astrology interpretation engine.
 *
 * Implements classical Parashari principles without generative hallucination:
 * 1. Natal Kundli Interpretation (Lagna, Moon sign, Nakshatra, Dignities, Retrograde, Whole Sign House placements).
 * 2. D1 ↔ D9 Navamsha Cross-Analysis (Vargottama detection, Navamsha dignity shifts, spiritual orientation).
 * 3. D1 ↔ D10 Dashamsha Career Synthesis (10th house, 10th lord, professional tendencies).
 * 4. Vimshottari Dasha Intelligence (Mahadasha & Antardasha planetary lords, houses, natural relationships).
 * 5. Transit (Gochar) Intelligence (Evaluated relative to Natal Moon and Lagna; Sade Sati, Kantaka Shani, auspicious houses).
 * 6. Panchang & Muhurta Intelligence (Diurnal alignment, Brahma Muhurta, Rahukaal, Abhijit Muhurta).
 * 7. Deterministic Conflict Resolution (Strict hierarchical weighting).
 * 8. Ethical Boundaries (Non-fatalistic, zero medical/legal/lifespan certainty claims).
 */
object VedicInterpretationEngine {

    fun interpret(
        profile: AstrologyProfile,
        dashaTimeline: DashaTimeline? = null,
        transitSnapshot: TransitSnapshot? = null,
        panchangSnapshot: PanchangSnapshot? = null
    ): AdvancedVedicInterpretation {
        val interpretationItems = mutableListOf<InterpretationItem>()
        val allFactors = mutableListOf<InterpretationFactor>()

        // 1. NATAL LAGNA (ASCENDANT) & LAGNA LORD
        val lagnaRashi = Rashi.fromIndex(profile.lagnaSignIndex)
        val (lagnaTitle, lagnaDesc) = VedicInterpretationRules.getLagnaInterpretation(lagnaRashi)
        val lagnaLordPlanet = lagnaRashi.lord
        val lagnaLordPos = profile.planetPositions.firstOrNull { it.planet.equals(lagnaLordPlanet, ignoreCase = true) }

        val lagnaFactor = InterpretationFactor(
            name = "Lagna in ${lagnaRashi.sanskritName} (${lagnaRashi.englishName})",
            category = InterpretationCategory.NATAL_LAGNA,
            source = "Natal Kundli (D1)",
            calculatedValue = "${profile.lagna} at ${String.format("%.2f°", profile.lagnaDegreeInSign)}",
            polarity = InterpretationFactorPolarity.SUPPORTIVE,
            priority = InterpretationPriority.PRIMARY,
            weight = 8
        )
        allFactors.add(lagnaFactor)

        val lagnaLordDesc = if (lagnaLordPos != null) {
            val (llTitle, llText) = VedicInterpretationRules.getLagnaLordInHouseInterpretation(lagnaLordPlanet, lagnaLordPos.house)
            val llDignityText = VedicInterpretationRules.getDignitySummary(lagnaLordPlanet, lagnaLordPos.dignity)
            val llPolarity = when (lagnaLordPos.dignity) {
                PlanetDignity.EXALTED, PlanetDignity.MOOLATRIKONA, PlanetDignity.OWN_SIGN -> InterpretationFactorPolarity.SUPPORTIVE
                PlanetDignity.ENEMY, PlanetDignity.DEBILITATED -> InterpretationFactorPolarity.CHALLENGING
                else -> InterpretationFactorPolarity.NEUTRAL
            }
            allFactors.add(
                InterpretationFactor(
                    name = "Lagna Lord ($lagnaLordPlanet) in House ${lagnaLordPos.house}",
                    category = InterpretationCategory.NATAL_LAGNA,
                    source = "Natal Kundli (D1)",
                    calculatedValue = "$lagnaLordPlanet in ${lagnaLordPos.sign} (${lagnaLordPos.dignity.displayName}) House ${lagnaLordPos.house}",
                    polarity = llPolarity,
                    priority = InterpretationPriority.PRIMARY,
                    weight = if (llPolarity == InterpretationFactorPolarity.SUPPORTIVE) 8 else 7
                )
            )
            "$llText $llDignityText"
        } else {
            "Lagna Lord $lagnaLordPlanet governs the vital constitution and physical direction."
        }

        interpretationItems.add(
            InterpretationItem(
                id = "natal_lagna",
                title = lagnaTitle,
                sanskritTitle = "लग्न एवं स्वभाव",
                category = InterpretationCategory.NATAL_LAGNA,
                summary = "The Ascendant in ${lagnaRashi.sanskritName} establishes your primary approach to the world.",
                detailedDescription = "$lagnaDesc\n\n$lagnaLordDesc",
                factors = listOf(lagnaFactor),
                evidence = InterpretationEvidence(
                    title = "Ascendant Calculation Basis",
                    metrics = mapOf(
                        "Sign" to "${lagnaRashi.sanskritName} (${lagnaRashi.englishName})",
                        "Degree" to String.format("%.2f°", profile.lagnaDegreeInSign),
                        "Lagna Lord" to lagnaLordPlanet,
                        "Lord House" to (lagnaLordPos?.house?.toString() ?: "N/A"),
                        "Lord Dignity" to (lagnaLordPos?.dignity?.displayName ?: "N/A")
                    ),
                    astronomicalBasis = "Computed using high-precision Swiss Ephemeris Nirayana sidereal coordinates with Lahiri Ayanamsa."
                ),
                traditionalGuidance = "Strengthen personal vitality by honoring regular morning routines, physical exercise, and mindful self-respect."
            )
        )

        // 2. MOON SIGN (CHANDRA RASHI) & NAKSHATRA
        val moonPos = profile.planetPositions.firstOrNull { it.planet.equals("Moon", ignoreCase = true) }
        val moonRashi = Rashi.fromIndex(profile.moonSignIndex)
        val (moonTitle, moonDesc) = VedicInterpretationRules.getMoonSignInterpretation(moonRashi)
        val birthNakshatraEnum = Nakshatra.entries.firstOrNull { it.sanskritName.equals(profile.nakshatra, ignoreCase = true) }
        val nakshatraSignification = birthNakshatraEnum?.let { VedicInterpretationRules.getNakshatraSignification(it) }
            ?: "Birth Nakshatra ${profile.nakshatra} Pada ${profile.nakshatraPada} governed by ${profile.nakshatraLord}."

        val moonPolarity = when (moonPos?.dignity) {
            PlanetDignity.EXALTED, PlanetDignity.MOOLATRIKONA, PlanetDignity.OWN_SIGN -> InterpretationFactorPolarity.SUPPORTIVE
            PlanetDignity.DEBILITATED -> InterpretationFactorPolarity.CHALLENGING
            else -> InterpretationFactorPolarity.NEUTRAL
        }
        val moonFactor = InterpretationFactor(
            name = "Moon in ${moonRashi.sanskritName} (${moonRashi.englishName})",
            category = InterpretationCategory.MOON_AND_MIND,
            source = "Natal Kundli (D1)",
            calculatedValue = "Moon at ${String.format("%.2f°", moonPos?.degreeInSign ?: 0.0)} in ${moonPos?.sign ?: profile.moonSign} (${moonPos?.dignity?.displayName ?: "Neutral"})",
            polarity = moonPolarity,
            priority = InterpretationPriority.PRIMARY,
            weight = 8
        )
        allFactors.add(moonFactor)

        interpretationItems.add(
            InterpretationItem(
                id = "moon_and_mind",
                title = moonTitle,
                sanskritTitle = "चन्द्र एवं मनोदशा",
                category = InterpretationCategory.MOON_AND_MIND,
                summary = "The Moon governs mental state (Manas), emotional rhythm, and memory.",
                detailedDescription = "$moonDesc\n\nNakshatra Signification:\n$nakshatraSignification",
                factors = listOf(moonFactor),
                evidence = InterpretationEvidence(
                    title = "Chandra & Nakshatra Evidence",
                    metrics = mapOf(
                        "Moon Sign" to "${moonRashi.sanskritName} (${moonRashi.englishName})",
                        "House" to (moonPos?.house?.toString() ?: "N/A"),
                        "Nakshatra" to "${profile.nakshatra} (Pada ${profile.nakshatraPada})",
                        "Nakshatra Lord" to profile.nakshatraLord,
                        "Dignity" to (moonPos?.dignity?.displayName ?: "N/A")
                    ),
                    astronomicalBasis = "Accurate lunar sidereal longitude computed with topocentric correction."
                ),
                traditionalGuidance = "Sustain emotional clarity through quiet evening contemplation, hydration, and nurturing reciprocal relationships."
            )
        )

        // 3. PLANETARY DIGNITIES & RETROGRADE MOVEMENTS
        val prominentDignityFactors = mutableListOf<InterpretationFactor>()
        val dignityDescriptions = mutableListOf<String>()

        profile.planetPositions.forEach { planet ->
            val pName = planet.planet
            val isExaltedOrOwn = planet.dignity in listOf(PlanetDignity.EXALTED, PlanetDignity.MOOLATRIKONA, PlanetDignity.OWN_SIGN)
            val isDebilitated = planet.dignity == PlanetDignity.DEBILITATED
            val isEnemy = planet.dignity == PlanetDignity.ENEMY

            if (isExaltedOrOwn || isDebilitated || isEnemy) {
                val pol = if (isExaltedOrOwn) InterpretationFactorPolarity.SUPPORTIVE else InterpretationFactorPolarity.CHALLENGING
                val w = if (isExaltedOrOwn) 7 else 6
                val factor = InterpretationFactor(
                    name = "$pName Dignity (${planet.dignity.displayName})",
                    category = InterpretationCategory.PLANETARY_DIGNITY,
                    source = "Natal Dignity (D1)",
                    calculatedValue = "$pName in ${planet.sign} House ${planet.house} (${planet.dignity.displayName})",
                    polarity = pol,
                    priority = InterpretationPriority.SECONDARY,
                    weight = w
                )
                prominentDignityFactors.add(factor)
                allFactors.add(factor)
                dignityDescriptions.add(VedicInterpretationRules.getDignitySummary(pName, planet.dignity))
            }

            if (planet.isRetrograde && pName !in listOf("Rahu", "Ketu")) {
                val retroFactor = InterpretationFactor(
                    name = "$pName Retrograde (Vakri)",
                    category = InterpretationCategory.PLANETARY_DIGNITY,
                    source = "Natal Motion (D1)",
                    calculatedValue = "$pName Vakri in House ${planet.house} at ${String.format("%.2f°", planet.degreeInSign)}",
                    polarity = InterpretationFactorPolarity.NEUTRAL,
                    priority = InterpretationPriority.SECONDARY,
                    weight = 6
                )
                allFactors.add(retroFactor)
                dignityDescriptions.add(VedicInterpretationRules.getRetrogradeInterpretation(pName))
            }
        }

        if (dignityDescriptions.isNotEmpty()) {
            interpretationItems.add(
                InterpretationItem(
                    id = "planetary_dignity",
                    title = "Planetary Strengths, Dignities & Retrogrades",
                    sanskritTitle = "ग्रह स्थिति, बल एवं वक्र गति",
                    category = InterpretationCategory.PLANETARY_DIGNITY,
                    summary = "Evaluates planetary dignity (Uchcha, Neecha, Swakshetra) and inward retrograde (Vakri) focus.",
                    detailedDescription = dignityDescriptions.joinToString("\n\n"),
                    factors = prominentDignityFactors,
                    evidence = InterpretationEvidence(
                        title = "Planetary Dignity Audit",
                        metrics = profile.planetPositions.associate { it.planet to "${it.dignity.displayName} (H${it.house})" },
                        astronomicalBasis = "Classical Parashari sign boundaries and deep exaltation / debilitation degrees."
                    ),
                    traditionalGuidance = "Challenging placements benefit from discipline and patience; elevated placements flourish when shared generously."
                )
            )
        }

        // 4. D1 ↔ D9 NAVAMSHA CROSS-ANALYSIS
        val d9ChartResult = runCatching { VargaCalculator.calculateVargaChart(profile, VargaType.D9) }
        val d9Chart = d9ChartResult.getOrNull()
        val vargottamaPlanets = mutableListOf<String>()
        val strengthenedInD9 = mutableListOf<String>()
        val weakenedInD9 = mutableListOf<String>()

        val divisionalAnalysis = if (d9Chart != null) {
            d9Chart.positions.forEach { d9Pos ->
                val d1Pos = profile.planetPositions.firstOrNull { it.planet.equals(d9Pos.planet, ignoreCase = true) }
                if (d1Pos != null) {
                    if (d1Pos.signIndex == d9Pos.signIndex) {
                        vargottamaPlanets.add(d9Pos.planet)
                        allFactors.add(
                            InterpretationFactor(
                                name = "${d9Pos.planet} Vargottama",
                                category = InterpretationCategory.VARGA_D9_NAVAMSHA,
                                source = "D1 ↔ D9 Varga",
                                calculatedValue = "${d9Pos.planet} in identical sign ${d1Pos.sign} across D1 and D9",
                                polarity = InterpretationFactorPolarity.SUPPORTIVE,
                                priority = InterpretationPriority.SECONDARY,
                                weight = 8
                            )
                        )
                    }

                    val d1Dignity = d1Pos.dignity
                    val d9Dignity = d9Pos.dignity
                    if (d9Dignity in listOf(PlanetDignity.EXALTED, PlanetDignity.OWN_SIGN) &&
                        d1Dignity !in listOf(PlanetDignity.EXALTED, PlanetDignity.OWN_SIGN)
                    ) {
                        strengthenedInD9.add("${d9Pos.planet} (D9 ${d9Dignity.displayName})")
                        allFactors.add(
                            InterpretationFactor(
                                name = "${d9Pos.planet} Navamsha Dignity Elevation",
                                category = InterpretationCategory.VARGA_D9_NAVAMSHA,
                                source = "D1 ↔ D9 Varga",
                                calculatedValue = "D1 ${d1Dignity.displayName} -> D9 ${d9Dignity.displayName}",
                                polarity = InterpretationFactorPolarity.SUPPORTIVE,
                                priority = InterpretationPriority.SECONDARY,
                                weight = 7
                            )
                        )
                    } else if (d9Dignity == PlanetDignity.DEBILITATED && d1Dignity != PlanetDignity.DEBILITATED) {
                        weakenedInD9.add("${d9Pos.planet} (D9 Debilitated)")
                        allFactors.add(
                            InterpretationFactor(
                                name = "${d9Pos.planet} Navamsha Debilitation",
                                category = InterpretationCategory.VARGA_D9_NAVAMSHA,
                                source = "D1 ↔ D9 Varga",
                                calculatedValue = "D1 ${d1Dignity.displayName} -> D9 Debilitated",
                                polarity = InterpretationFactorPolarity.CHALLENGING,
                                priority = InterpretationPriority.SECONDARY,
                                weight = 6
                            )
                        )
                    }
                }
            }

            val d9Summary = VedicInterpretationRules.interpretD1D9Comparative(
                vargottamaPlanets = vargottamaPlanets,
                strengthenedInD9 = strengthenedInD9,
                weakenedInD9 = weakenedInD9,
                d1Lagna = profile.lagna,
                d9Lagna = d9Chart.ascendantSign
            )

            // D10 Dashamsha Career Cross-Analysis
            val d10ChartResult = runCatching { VargaCalculator.calculateVargaChart(profile, VargaType.D10) }
            val d10Chart = d10ChartResult.getOrNull()
            val d10Available = d10Chart != null
            val (d10Themes, d10Summary) = if (d10Chart != null) {
                val d1TenthHousePlanets = profile.planetPositions.filter { it.house == 10 }.map { it.planet }
                val d10TenthHousePlanets = d10Chart.positions.filter { it.house == 10 }.map { it.planet }
                val d1TenthLord = Rashi.fromIndex((profile.lagnaSignIndex + 9).mod(12)).lord
                val themes = VedicInterpretationRules.interpretD10CareerThemes(
                    tenthHouseLord = d1TenthLord,
                    tenthHousePlanetsD1 = d1TenthHousePlanets,
                    tenthHousePlanetsD10 = d10TenthHousePlanets,
                    keyPlanetsD10 = d10Chart.positions.filter { it.dignity in listOf(PlanetDignity.EXALTED, PlanetDignity.OWN_SIGN) }.map { it.planet }
                )
                val summary = "D10 Dashamsha complements D1 by mapping sustained professional endeavor (Karma), executive discipline, and public duty. 10th House Lord: $d1TenthLord."
                Pair(themes, summary)
            } else {
                Pair(
                    emptyList<String>(),
                    "D10 Dashamsha calculation is currently unavailable for this chart."
                )
            }

            DivisionalCrossAnalysis(
                d9Available = true,
                d9LagnaSign = d9Chart.ascendantSign,
                d9LagnaRelationship = "D9 Ascendant ${d9Chart.ascendantSign}",
                vargottamaPlanets = vargottamaPlanets,
                strengthenedPlanetsInD9 = strengthenedInD9,
                weakenedPlanetsInD9 = weakenedInD9,
                d9Summary = d9Summary,
                d10Available = d10Available,
                d10LagnaSign = d10Chart?.ascendantSign,
                d10TenthLordPlacement = if (d10Available) Rashi.fromIndex((profile.lagnaSignIndex + 9).mod(12)).lord else null,
                d10KeyPlanets = d10Chart?.positions?.filter { it.dignity in listOf(PlanetDignity.EXALTED, PlanetDignity.OWN_SIGN) }?.map { it.planet } ?: emptyList(),
                d10CareerThemes = d10Themes,
                d10Summary = d10Summary
            )
        } else {
            DivisionalCrossAnalysis(
                d9Available = false,
                d9Summary = "D9 Navamsha calculation is currently unavailable.",
                d10Available = false,
                d10Summary = "D10 Dashamsha calculation is currently unavailable."
            )
        }

        if (divisionalAnalysis.d9Available) {
            interpretationItems.add(
                InterpretationItem(
                    id = "varga_d9_navamsha",
                    title = "D1 ↔ D9 Navamsha Soul & Destiny Synthesis",
                    sanskritTitle = "डी १ एवं डी ९ नवांश विश्लेषण",
                    category = InterpretationCategory.VARGA_D9_NAVAMSHA,
                    summary = "The Navamsha (D9) reveals inner fruitfulness, spiritual dharma, and latent planetary strength.",
                    detailedDescription = divisionalAnalysis.d9Summary,
                    factors = allFactors.filter { it.category == InterpretationCategory.VARGA_D9_NAVAMSHA },
                    evidence = InterpretationEvidence(
                        title = "D9 Navamsha Cross-Reference",
                        metrics = mapOf(
                            "D1 Lagna" to profile.lagna,
                            "D9 Lagna" to (divisionalAnalysis.d9LagnaSign ?: "N/A"),
                            "Vargottama Planets" to if (vargottamaPlanets.isEmpty()) "None" else vargottamaPlanets.joinToString(", "),
                            "D9 Strengthened" to if (strengthenedInD9.isEmpty()) "None" else strengthenedInD9.joinToString(", "),
                            "D9 Weakened" to if (weakenedInD9.isEmpty()) "None" else weakenedInD9.joinToString(", ")
                        ),
                        astronomicalBasis = "Precise 9-fold division (3° 20' per Navamsha) based on Nirayana longitude."
                    ),
                    traditionalGuidance = "Cultivate inner integrity; planets elevated in D9 manifest their greatest potential through ethical maturity."
                )
            )
        }

        if (divisionalAnalysis.d10Available) {
            interpretationItems.add(
                InterpretationItem(
                    id = "varga_d10_dashamsha",
                    title = "D1 ↔ D10 Dashamsha Professional Guidance",
                    sanskritTitle = "डी १ एवं डी १० दशांश कर्म विश्लेषण",
                    category = InterpretationCategory.VARGA_D10_DASHAMSHA,
                    summary = "Dashamsha (D10) indicates occupational tendencies, social responsibility, and durable achievement.",
                    detailedDescription = divisionalAnalysis.d10Summary + "\n\nKey Professional Themes:\n" +
                            divisionalAnalysis.d10CareerThemes.joinToString("\n• ", prefix = "• "),
                    factors = emptyList(),
                    evidence = InterpretationEvidence(
                        title = "D10 Career Evidence",
                        metrics = mapOf(
                            "10th Lord" to (divisionalAnalysis.d10TenthLordPlacement ?: "N/A"),
                            "D10 Lagna" to (divisionalAnalysis.d10LagnaSign ?: "N/A"),
                            "Prominent D10 Grahas" to if (divisionalAnalysis.d10KeyPlanets.isEmpty()) "Balanced" else divisionalAnalysis.d10KeyPlanets.joinToString(", ")
                        ),
                        astronomicalBasis = "10-fold division (3° per Dashamsha) based on Nirayana longitude."
                    ),
                    traditionalGuidance = "Commit to excellence and ethical leadership; lasting accomplishments require steady patience."
                )
            )
        }

        // 5. VIMSHOTTARI DASHA INTELLIGENCE
        val dashaContext = if (dashaTimeline != null) {
            val currentMaha = dashaTimeline.currentMahadasha
            val currentAntar = dashaTimeline.currentAntardasha
            if (currentMaha != null) {
                val mahaLordName = currentMaha.planet.lord
                val antarLordName = currentAntar?.antardashaLord?.lord ?: mahaLordName
                val relationship = VedicInterpretationRules.getNaturalRelationship(mahaLordName, antarLordName)
                val isFriend = VedicInterpretationRules.isFriendlyRelationship(mahaLordName, antarLordName)
                val isEnemy = VedicInterpretationRules.isEnemyRelationship(mahaLordName, antarLordName)

                val mahaNatalPos = profile.planetPositions.firstOrNull { it.planet.equals(mahaLordName, ignoreCase = true) }
                val antarNatalPos = profile.planetPositions.firstOrNull { it.planet.equals(antarLordName, ignoreCase = true) }

                val dashaPolarity = if (isFriend) InterpretationFactorPolarity.SUPPORTIVE else if (isEnemy) InterpretationFactorPolarity.CHALLENGING else InterpretationFactorPolarity.NEUTRAL
                val dashaFactor = InterpretationFactor(
                    name = "Active Dasha: $mahaLordName / $antarLordName",
                    category = InterpretationCategory.DASHA_TIMING,
                    source = "Vimshottari Dasha",
                    calculatedValue = "$mahaLordName Mahadasha with $antarLordName Antardasha ($relationship)",
                    polarity = dashaPolarity,
                    priority = InterpretationPriority.DOMINANT,
                    weight = 9
                )
                allFactors.add(dashaFactor)

                val periodFormatted = "${currentMaha.startDate.year} to ${currentMaha.endDate.year}"
                val mahaDesc = "Currently progressing through the Mahadasha of $mahaLordName (placed in House ${mahaNatalPos?.house ?: "N/A"} in ${mahaNatalPos?.sign ?: "N/A"}). " +
                        "This sets the primary 120-year cycle backdrop. The sub-period is ruled by $antarLordName (House ${antarNatalPos?.house ?: "N/A"}), creating a relationship of $relationship."

                val activeThemes = mutableListOf<String>()
                activeThemes.add("Primary Life Focus: Themes governed by House ${mahaNatalPos?.house ?: 1} and House ${antarNatalPos?.house ?: 1}")
                if (isFriend) {
                    activeThemes.add("Harmonious Period: Cooperative synergy between the major and minor planetary rulers encourages steady execution.")
                } else if (isEnemy) {
                    activeThemes.add("Contrasting Impulses: Creative tension between $mahaLordName and $antarLordName requires balanced prioritization.")
                } else {
                    activeThemes.add("Steady Progression: Moderate, methodical developments through structured responsibility.")
                }

                interpretationItems.add(
                    InterpretationItem(
                        id = "dasha_timing",
                        title = "Vimshottari Dasha: $mahaLordName Mahadasha ($antarLordName Antardasha)",
                        sanskritTitle = "विंशोत्तरी दशा काल",
                        category = InterpretationCategory.DASHA_TIMING,
                        summary = "The active planetary ruler determines the psychological lens and environmental timing.",
                        detailedDescription = mahaDesc + "\n\n" + activeThemes.joinToString("\n• ", prefix = "• "),
                        factors = listOf(dashaFactor),
                        evidence = InterpretationEvidence(
                            title = "Vimshottari Dasha Metrics",
                            metrics = mapOf(
                                "Mahadasha Lord" to mahaLordName,
                                "Antardasha Lord" to antarLordName,
                                "Lord Relationship" to relationship,
                                "Maha House" to (mahaNatalPos?.house?.toString() ?: "N/A"),
                                "Antar House" to (antarNatalPos?.house?.toString() ?: "N/A"),
                                "Mahadasha Span" to periodFormatted
                            ),
                            astronomicalBasis = "Classical 120-year Vimshottari dasha cycle anchored to birth Moon nakshatra longitude."
                        ),
                        traditionalGuidance = "Align your conscious efforts with the active Dasha rulers ($mahaLordName and $antarLordName) to move in harmony with cosmic timing."
                    )
                )

                DashaInterpretationContext(
                    mahadashaLord = mahaLordName,
                    antardashaLord = antarLordName,
                    periodDates = periodFormatted,
                    lordRelationship = relationship,
                    mahadashaNatalHouse = mahaNatalPos?.house ?: 1,
                    antardashaNatalHouse = antarNatalPos?.house ?: 1,
                    summary = mahaDesc,
                    activeThemes = activeThemes
                )
            } else null
        } else null

        // 6. TRANSIT (GOCHAR) INTELLIGENCE
        val transitContext = if (transitSnapshot != null) {
            val transitSaturn = transitSnapshot.positions.firstOrNull { it.planet.equals("Saturn", ignoreCase = true) }
            val transitJupiter = transitSnapshot.positions.firstOrNull { it.planet.equals("Jupiter", ignoreCase = true) }

            val (sadeSatiPhase, isKantaka, isAshtama) = if (transitSaturn != null) {
                VedicInterpretationRules.evaluateSaturnSadeSati(profile.moonSignIndex, transitSaturn.signIndex)
            } else Triple(null, false, false)

            val beneficTransits = mutableListOf<String>()
            val challengingTransits = mutableListOf<String>()

            if (sadeSatiPhase != null) {
                val factor = InterpretationFactor(
                    name = "Saturn Sade Sati Active",
                    category = InterpretationCategory.TRANSIT_INFLUENCE,
                    source = "Gochar (Transit)",
                    calculatedValue = "Saturn in House ${transitSaturn?.houseFromMoon} from Moon: $sadeSatiPhase",
                    polarity = InterpretationFactorPolarity.CHALLENGING,
                    priority = InterpretationPriority.SECONDARY,
                    weight = 8
                )
                allFactors.add(factor)
                challengingTransits.add("Sade Sati ($sadeSatiPhase)")
            } else if (isKantaka) {
                val factor = InterpretationFactor(
                    name = "Kantaka Shani (4th from Moon)",
                    category = InterpretationCategory.TRANSIT_INFLUENCE,
                    source = "Gochar (Transit)",
                    calculatedValue = "Saturn 4th from Moon",
                    polarity = InterpretationFactorPolarity.CHALLENGING,
                    priority = InterpretationPriority.SECONDARY,
                    weight = 7
                )
                allFactors.add(factor)
                challengingTransits.add("Kantaka Shani: Domestic obligations and patient perseverance required.")
            } else if (isAshtama) {
                val factor = InterpretationFactor(
                    name = "Ashtama Shani (8th from Moon)",
                    category = InterpretationCategory.TRANSIT_INFLUENCE,
                    source = "Gochar (Transit)",
                    calculatedValue = "Saturn 8th from Moon",
                    polarity = InterpretationFactorPolarity.CHALLENGING,
                    priority = InterpretationPriority.SECONDARY,
                    weight = 8
                )
                allFactors.add(factor)
                challengingTransits.add("Ashtama Shani: Deep introspection, measured pace, and caution against impulsiveness.")
            }

            if (transitJupiter != null) {
                val jupHouse = transitJupiter.houseFromMoon ?: ((transitJupiter.signIndex - profile.moonSignIndex).mod(12) + 1)
                val (jupPol, jupDesc) = VedicInterpretationRules.evaluateJupiterTransitFromMoon(jupHouse)
                val factor = InterpretationFactor(
                    name = "Jupiter Transit (House $jupHouse from Moon)",
                    category = InterpretationCategory.TRANSIT_INFLUENCE,
                    source = "Gochar (Transit)",
                    calculatedValue = "Jupiter in ${transitJupiter.sign} (House $jupHouse from Moon)",
                    polarity = jupPol,
                    priority = InterpretationPriority.SECONDARY,
                    weight = 7
                )
                allFactors.add(factor)
                if (jupPol == InterpretationFactorPolarity.SUPPORTIVE) {
                    beneficTransits.add(jupDesc)
                }
            }

            val saturnHouseFromMoon = transitSaturn?.houseFromMoon ?: transitSaturn?.let { ((it.signIndex - profile.moonSignIndex).mod(12)) + 1 }
            val jupiterHouseFromMoon = transitJupiter?.houseFromMoon ?: transitJupiter?.let { ((it.signIndex - profile.moonSignIndex).mod(12)) + 1 }

            val transitSummary = buildString {
                append("Transits evaluated from Natal Moon (${profile.moonSign}) and Natal Lagna (${profile.lagna}). ")
                if (sadeSatiPhase != null) append("Saturn is currently in Sade Sati: $sadeSatiPhase. ")
                if (jupiterHouseFromMoon != null) append("Jupiter is transiting House $jupiterHouseFromMoon from Moon. ")
            }

            interpretationItems.add(
                InterpretationItem(
                    id = "transit_influence",
                    title = "Planetary Transits (Gochar Dynamics)",
                    sanskritTitle = "ग्रह गोचर प्रभाव",
                    category = InterpretationCategory.TRANSIT_INFLUENCE,
                    summary = "Transits provide real-time atmospheric shifts activating natal potentials.",
                    detailedDescription = transitSummary + "\n\nAuspicious Transit Influences:\n" +
                            (if (beneficTransits.isEmpty()) "• General supportive diurnal stability." else beneficTransits.joinToString("\n• ", prefix = "• ")) +
                            "\n\nTransformative Transit Lessons:\n" +
                            (if (challengingTransits.isEmpty()) "• No severe Saturn or nodal pressures currently active." else challengingTransits.joinToString("\n• ", prefix = "• ")),
                    factors = allFactors.filter { it.category == InterpretationCategory.TRANSIT_INFLUENCE },
                    evidence = InterpretationEvidence(
                        title = "Transit Calculation Data",
                        metrics = mapOf(
                            "Moon Reference" to profile.moonSign,
                            "Saturn House from Moon" to (saturnHouseFromMoon?.toString() ?: "N/A"),
                            "Jupiter House from Moon" to (jupiterHouseFromMoon?.toString() ?: "N/A"),
                            "Sade Sati Active" to (sadeSatiPhase ?: "None")
                        ),
                        astronomicalBasis = "Real-time planetary coordinates computed with Lahiri Ayanamsa relative to natal Moon sign."
                    ),
                    traditionalGuidance = "Transits act as weather; dress suitably. Sade Sati teaches maturity and humility; Jupiter expands wisdom and dharmic pursuits."
                )
            )

            TransitInterpretationContext(
                referenceMoonSign = profile.moonSign,
                referenceLagnaSign = profile.lagna,
                sadeSatiPhase = sadeSatiPhase,
                isKantakaShani = isKantaka,
                isAshtamaShani = isAshtama,
                beneficTransits = beneficTransits,
                challengingTransits = challengingTransits,
                summary = transitSummary
            )
        } else null

        // 7. PANCHANG & MUHURTA INTELLIGENCE
        val panchangContext = if (panchangSnapshot != null) {
            val varaName = panchangSnapshot.vara.sanskritName
            val tithiName = "${panchangSnapshot.tithi.paksha.name} ${panchangSnapshot.tithi.name}"
            val nakshatraName = panchangSnapshot.nakshatra.nakshatra.sanskritName
            val yogaName = panchangSnapshot.yoga.name
            val karanaName = panchangSnapshot.karana.name

            val brahmaStr = panchangSnapshot.muhurta?.brahmaMuhurta?.let { "${it.start.format(DateTimeFormatter.ofPattern("hh:mm a"))} - ${it.end.format(DateTimeFormatter.ofPattern("hh:mm a"))}" }
            val abhijitStr = panchangSnapshot.muhurta?.abhijitMuhurta?.let { "${it.start.format(DateTimeFormatter.ofPattern("hh:mm a"))} - ${it.end.format(DateTimeFormatter.ofPattern("hh:mm a"))}" }
            val rahukaalStr = panchangSnapshot.muhurta?.rahukaal?.let { "${it.start.format(DateTimeFormatter.ofPattern("hh:mm a"))} - ${it.end.format(DateTimeFormatter.ofPattern("hh:mm a"))}" }

            val pSummary = "Panchang alignment for $varaName: Tithi $tithiName, Nakshatra $nakshatraName, Yoga $yogaName, Karana $karanaName. " +
                    "Diurnal energies are favorable during Brahma Muhurta ($brahmaStr) and Abhijit Muhurta ($abhijitStr); exercise mindful deliberation during Rahukaal ($rahukaalStr)."

            val panchangFactor = InterpretationFactor(
                name = "Panchang Alignment ($varaName / $tithiName)",
                category = InterpretationCategory.PANCHANG_ALIGNMENT,
                source = "Daily Panchang",
                calculatedValue = "$varaName, $tithiName, $nakshatraName",
                polarity = InterpretationFactorPolarity.SUPPORTIVE,
                priority = InterpretationPriority.SUBTLE,
                weight = 5
            )
            allFactors.add(panchangFactor)

            interpretationItems.add(
                InterpretationItem(
                    id = "panchang_alignment",
                    title = "Panchang Alignment & Auspicious Timing",
                    sanskritTitle = "पंचांग शुद्धि एवं मुहूर्त",
                    category = InterpretationCategory.PANCHANG_ALIGNMENT,
                    summary = "Daily cosmic rhythm guiding optimal timing for study, worship, and action.",
                    detailedDescription = pSummary,
                    factors = listOf(panchangFactor),
                    evidence = InterpretationEvidence(
                        title = "Panchang Elements",
                        metrics = mapOf(
                            "Vara" to varaName,
                            "Tithi" to tithiName,
                            "Nakshatra" to nakshatraName,
                            "Yoga" to yogaName,
                            "Karana" to karanaName,
                            "Brahma Muhurta" to (brahmaStr ?: "N/A"),
                            "Abhijit Muhurta" to (abhijitStr ?: "N/A"),
                            "Rahukaal" to (rahukaalStr ?: "N/A")
                        ),
                        astronomicalBasis = "Classical 5-fold Panchanga calculation based on exact solar-lunar angular relationships."
                    ),
                    traditionalGuidance = "Use Brahma Muhurta for meditation and study; pause critical fresh commitments during Rahukaal."
                )
            )

            PanchangInterpretationContext(
                vara = varaName,
                tithi = tithiName,
                paksha = panchangSnapshot.tithi.paksha.name,
                nakshatra = nakshatraName,
                yoga = yogaName,
                karana = karanaName,
                brahmaMuhurta = brahmaStr,
                abhijitMuhurta = abhijitStr,
                rahukaal = rahukaalStr,
                summary = pSummary
            )
        } else null

        // 8. CONFLICT RESOLUTION & SYNTHESIS (PART I)
        // Hierarchy: DOMINANT (Dasha) > PRIMARY (Lagna / Moon) > SECONDARY (D9 / Transits) > SUBTLE (Panchang)
        val sortedFactors = allFactors.sortedWith(
            compareByDescending<InterpretationFactor> { it.priority.ranking }
                .thenByDescending { it.weight }
        )

        val supportiveFactors = sortedFactors.filter { it.polarity == InterpretationFactorPolarity.SUPPORTIVE }
        val challengingFactors = sortedFactors.filter { it.polarity == InterpretationFactorPolarity.CHALLENGING }
        val dominantFactor = sortedFactors.firstOrNull()

        val opportunities = mutableListOf<String>()
        val cautions = mutableListOf<String>()
        val traditionalGuidance = mutableListOf<String>()

        if (supportiveFactors.isNotEmpty()) {
            supportiveFactors.take(3).forEach { f ->
                opportunities.add("Capitalize on ${f.name}: ${f.calculatedValue}")
            }
        } else {
            opportunities.add("Cultivate patient, incremental mastery in daily routines.")
        }

        if (challengingFactors.isNotEmpty()) {
            challengingFactors.take(3).forEach { f ->
                cautions.add("Mindful navigation around ${f.name}: Exercise prudence rather than hasty reaction.")
            }
        } else {
            cautions.add("Maintain balanced humility and avoid over-extension.")
        }

        traditionalGuidance.add("Svadharma (Mindful Duty): Focus on doing what is right and duty-bound without anxiety over immediate fruits.")
        traditionalGuidance.add("Satyam & Ahimsa: Cultivate truthful communication and non-harming consideration in personal relations.")
        traditionalGuidance.add("Daily Routine (Dinacharya): Ground emotional stability with sunrise mindfulness, wholesome diet, and sufficient rest.")

        val natalSummary = "Kundli for ${profile.birthData.name}: ${profile.lagna} Lagna with Moon in ${profile.moonSign} (${profile.nakshatra} Nakshatra). " +
                "Core planetary configuration exhibits ${supportiveFactors.size} supportive indicators and ${challengingFactors.size} developmental growth areas."

        return AdvancedVedicInterpretation(
            profileId = profile.birthData.name,
            profileName = profile.birthData.name,
            calculationTimestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            natalSummary = natalSummary,
            strongestCurrentFactors = sortedFactors.take(5),
            dashaContext = dashaContext,
            transitContext = transitContext,
            panchangContext = panchangContext,
            divisionalAnalysis = divisionalAnalysis,
            allInterpretationItems = interpretationItems,
            supportiveFactors = supportiveFactors,
            challengingFactors = challengingFactors,
            dominantFactor = dominantFactor,
            opportunities = opportunities,
            cautions = cautions,
            traditionalGuidance = traditionalGuidance,
            disclaimer = AdvancedVedicInterpretation.ETHICAL_DISCLAIMER
        )
    }
}
