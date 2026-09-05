package com.example.domain.prediction

import com.example.domain.engine.TransitCalculator
import com.example.domain.models.*
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Deterministic domain-level Personalisation Engine for Daily Rashifal.
 *
 * Combines 5 canonical pillars:
 * 1. Natal Kundli (Lagna, Moon sign, Birth Nakshatra, D1 house placements)
 * 2. Current Vimshottari Dasha & Antardasha
 * 3. Planetary Transits (Gochar relative to Natal Moon and Natal Lagna)
 * 4. Daily Panchang (Tithi, Vara, Nakshatra Tara Bala, Nitya Yoga, Karana)
 * 5. Daily Muhurta (Brahma Muhurta, Rahukaal, Abhijit Muhurta)
 */
object PersonalisedRashifalEngine {

    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    fun generateRashifal(
        context: DailyPredictionContext,
        natalProfile: AstrologyProfile
    ): DailyRashifal {
        val defaultProfile = context.defaultProfile
        val birthData = defaultProfile.birthData
        val date = context.targetDate
        val panchang = context.panchang
        val transit = context.transitSnapshot
        val dasha = context.dashaTimeline

        // 1. Natal & Astrological Baseline
        val lagnaSign = natalProfile.lagna
        val moonSign = natalProfile.moonSign
        val birthNakshatra = natalProfile.nakshatra
        val birthNakshatraEnum = Nakshatra.entries.firstOrNull { it.sanskritName.equals(birthNakshatra, ignoreCase = true) }
        val birthNakshatraIndex = birthNakshatraEnum?.ordinal ?: (natalProfile.planetPositions.firstOrNull { it.planet == "Moon" }?.totalLongitude?.let { (it / 13.333333333333334).toInt().coerceIn(0, 26) } ?: 0)

        // 2. Daily Transit Moon & Tara Bala
        val transitMoon = transit?.positions?.firstOrNull { it.planet.equals("Moon", ignoreCase = true) }
        val dailyNakshatraName = panchang?.nakshatra?.nakshatra?.sanskritName ?: transitMoon?.nakshatra ?: "Ashwini"
        val dailyNakshatraEnum = Nakshatra.entries.firstOrNull { it.sanskritName.equals(dailyNakshatraName, ignoreCase = true) }
        val dailyNakshatraIndex = dailyNakshatraEnum?.ordinal ?: 0

        val taraBala = TaraBalaInfo.calculate(birthNakshatraIndex, dailyNakshatraIndex)

        // Transit Moon House from Moon and Lagna
        val houseFromMoon = transitMoon?.houseFromMoon ?: natalProfile.moonSignIndex.let { moonIdx ->
            transitMoon?.signIndex?.let { tSign -> TransitCalculator.calculateRelativeHouse(tSign, moonIdx) }
        }
        val houseFromLagna = transitMoon?.houseFromLagna ?: natalProfile.lagnaSignIndex.let { lagnaIdx ->
            transitMoon?.signIndex?.let { tSign -> TransitCalculator.calculateRelativeHouse(tSign, lagnaIdx) }
        }

        // 3. Current Dasha & Antardasha
        val currentMaha = dasha?.currentMahadasha?.planet?.lord ?: natalProfile.nakshatraLord
        val currentAntar = dasha?.currentAntardasha?.antardashaLord?.lord ?: currentMaha

        // 4. Panchang Elements
        val vara = panchang?.vara ?: Vara.RAVIVARA
        val tithi = panchang?.tithi ?: Tithi(index = 1, name = "Pratipada", paksha = Paksha.SHUKLA, remainingPercentage = 100.0)
        val paksha = panchang?.paksha?.name ?: "SHUKLA"
        val yoga = panchang?.yoga ?: NityaYoga(index = 1, name = "Vishkambha", remainingPercentage = 100.0)
        val karana = panchang?.karana ?: Karana(index = 1, name = "Bava", isFixed = false, remainingPercentage = 100.0)

        // 5. Compute Deterministic Energy Score (0 to 100)
        var score = 65 // baseline

        // Tara Bala adjustment
        when (taraBala.taraNumber) {
            2, 4, 6, 8, 9 -> score += 15 // Benefic Taras (Sampat, Kshema, Sadhana, Mitra, Parama Mitra)
            1 -> score += 5 // Janma Tara
            3, 5 -> score -= 10 // Vipat, Pratyak
            7 -> score -= 18 // Naidhana / Vadha
        }

        // Transit Moon House adjustment (Gochar from Moon: 3, 6, 10, 11 are most auspicious)
        when (houseFromMoon) {
            3, 6, 10, 11 -> score += 12
            1, 2, 7, 9 -> score += 4
            4, 5 -> score -= 4
            8, 12 -> score -= 14
        }

        // Nitya Yoga adjustment
        val yogaLower = yoga.name.lowercase()
        val beneficYogas = listOf("siddhi", "shubha", "brahma", "shiva", "siddha", "amrita", "harshana", "vriddhi", "sukarma", "shobhana", "dhriti")
        val maleficYogas = listOf("vaidhriti", "vyatipata", "atiganda", "shula", "ganda", "vishkambha", "vajra", "vyaghata")
        if (beneficYogas.any { yogaLower.contains(it) }) {
            score += 8
        } else if (maleficYogas.any { yogaLower.contains(it) }) {
            score -= 10
        }

        val energyScore = score.coerceIn(25, 98)

        // 6. Theme and Primary Focus
        val (dailyTheme, primaryFocus) = generateThemeAndFocus(taraBala, houseFromMoon, currentMaha, currentAntar, energyScore)

        // 7. Key Influences (traceable)
        val keyInfluences = mutableListOf<AstrologicalInfluence>()

        // Influence A: Tara Bala
        keyInfluences.add(
            AstrologicalInfluence(
                title = "${taraBala.taraName} (${taraBala.quality})",
                description = "Daily Nakshatra ($dailyNakshatraName) activates the ${taraBala.taraName} cycle from your birth star ($birthNakshatra). ${taraBala.description}",
                contributingFactor = "Vedic Navatara: Distance ${((dailyNakshatraIndex - birthNakshatraIndex + 27) % 27)} stars from Janma Nakshatra",
                impactType = if (taraBala.taraNumber in listOf(2, 4, 6, 8, 9)) ImpactType.FAVORABLE else if (taraBala.taraNumber in listOf(3, 5, 7)) ImpactType.CAUTION else ImpactType.NEUTRAL
            )
        )

        // Influence B: Gochar Moon House
        val moonHouseDesc = houseFromMoon?.let { h ->
            val vedicName = TransitCalculator.getVedicHouseSignification(h)
            val ordinal = TransitCalculator.getHouseOrdinal(h)
            "Transiting Moon moves through your $ordinal house ($vedicName) relative to your Janma Rashi ($moonSign)."
        } ?: "Transiting Moon is active in ${transitMoon?.sign ?: "the celestial sphere"}."

        keyInfluences.add(
            AstrologicalInfluence(
                title = "Lunar Transit (Gochar) in ${houseFromMoon?.let { TransitCalculator.getHouseOrdinal(it) } ?: "Active"} Bhava",
                description = moonHouseDesc,
                contributingFactor = "Planetary Gochar: Moon at ${transitMoon?.formattedDegree ?: "current degrees"} in ${transitMoon?.sign ?: "sign"}",
                impactType = when (houseFromMoon) {
                    3, 6, 10, 11 -> ImpactType.FAVORABLE
                    8, 12 -> ImpactType.CAUTION
                    else -> ImpactType.NEUTRAL
                }
            )
        )

        // Influence C: Vimshottari Dasha Current Period
        keyInfluences.add(
            AstrologicalInfluence(
                title = "Active Dasha: $currentMaha Mahadasha — $currentAntar Antardasha",
                description = "Your operating timeline is currently energized by the planetary archetype of $currentMaha with the active sub-influence of $currentAntar.",
                contributingFactor = "Vimshottari Dasha cycle calculated from natal sidereal Moon longitude (${String.format("%.2f", natalProfile.planetPositions.firstOrNull { it.planet == "Moon" }?.totalLongitude ?: 0.0)}°)",
                impactType = ImpactType.NEUTRAL
            )
        )

        // Influence D: Major Planetary Transits (Jupiter, Saturn, Mars)
        transit?.positions?.firstOrNull { it.planet.equals("Jupiter", ignoreCase = true) }?.let { jup ->
            val jupHouse = jup.houseFromMoon
            if (jupHouse != null) {
                val jupImpact = if (jupHouse in listOf(2, 5, 7, 9, 11)) ImpactType.FAVORABLE else ImpactType.NEUTRAL
                keyInfluences.add(
                    AstrologicalInfluence(
                        title = "Guru Gochar: Jupiter in ${TransitCalculator.getHouseOrdinal(jupHouse)} House",
                        description = "Jupiter's transit through ${jup.sign} (${TransitCalculator.getHouseOrdinal(jupHouse)} from Moon) provides ${if (jupImpact == ImpactType.FAVORABLE) "favorable guidance, wisdom, and auspicious support" else "steady expansion and reflective growth"}.",
                        contributingFactor = "Gochar Jupiter at ${jup.formattedDegree} in ${jup.sign}",
                        impactType = jupImpact
                    )
                )
            }
        }

        // 8. Priorities & Recommendations
        val priorities = generatePriorities(taraBala, houseFromMoon, currentMaha, currentAntar, yoga, tithi)

        // 9. Cautions
        val cautions = generateCautions(taraBala, houseFromMoon, currentMaha, currentAntar, panchang?.muhurta?.rahukaal)

        // 10. Timing Guidance
        val timingGuidance = DailyTimingGuidance(
            brahmaMuhurtaWindow = panchang?.muhurta?.brahmaMuhurta?.let {
                "${it.start.format(timeFormatter)} – ${it.end.format(timeFormatter)}"
            },
            brahmaMuhurtaAdvice = "Calculated specifically for ${defaultProfile.name}'s location (${birthData.location.placeName}). Ideal interval for quiet reflection, meditation, goal calibration, and conscious planning.",
            rahukaalWindow = panchang?.muhurta?.rahukaal?.let {
                "${it.start.format(timeFormatter)} – ${it.end.format(timeFormatter)}"
            },
            rahukaalAdvice = "Traditional inauspicious planetary hour. Avoid signing irrevocable legal documents or initiating critical new financial risks during this window.",
            abhijitMuhurtaWindow = panchang?.muhurta?.abhijitMuhurta?.let {
                "${it.start.format(timeFormatter)} – ${it.end.format(timeFormatter)}"
            } ?: panchang?.sunrise?.let { sr ->
                val midday = sr.plusHours(6)
                "${midday.minusMinutes(24).format(timeFormatter)} – ${midday.plusMinutes(24).format(timeFormatter)}"
            },
            favorableTimeSlots = listOfNotNull(
                panchang?.muhurta?.brahmaMuhurta?.let { "Brahma Muhurta: ${it.start.format(timeFormatter)} – ${it.end.format(timeFormatter)}" },
                panchang?.muhurta?.abhijitMuhurta?.let { "Abhijit Muhurta: ${it.start.format(timeFormatter)} – ${it.end.format(timeFormatter)}" }
            )
        )

        // 11. Traditional Astrological Upay / Remedies
        val traditionalRemedies = generateRemedies(currentMaha, currentAntar, taraBala, houseFromMoon, vara)

        // 12. Astrological Factors Summary
        val factorsSummary = "Personalised calculation for ${defaultProfile.name} on $date synthesized from: " +
                "Lagna ($lagnaSign), Janma Rashi ($moonSign), Janma Nakshatra ($birthNakshatra); " +
                "Vimshottari Dasha ($currentMaha-$currentAntar); " +
                "Gochar Moon in ${transitMoon?.sign ?: "active sign"} (${houseFromMoon?.let { TransitCalculator.getHouseOrdinal(it) } ?: "–"} Bhava from Moon); " +
                "Panchang (${vara.sanskritName}, ${tithi.name}, $dailyNakshatraName, ${yoga.name} Yoga, ${karana.name} Karana); " +
                "and authentic local Muhurta windows computed for ${birthData.location.placeName}."

        return DailyRashifal(
            defaultProfileId = defaultProfile.id,
            profileName = defaultProfile.name,
            targetDate = date,
            targetDateTime = panchang?.requestedDateTime ?: date.atStartOfDay(birthData.timeZone).plusHours(6),
            birthLocationName = birthData.location.placeName,
            dailyTheme = dailyTheme,
            energyScore = energyScore,
            primaryFocus = primaryFocus,
            lagna = lagnaSign,
            moonSign = moonSign,
            birthNakshatra = birthNakshatra,
            currentMahadashaLord = currentMaha,
            currentAntardashaLord = currentAntar,
            transitMoonHouseFromNatalMoon = houseFromMoon,
            transitMoonHouseFromLagna = houseFromLagna,
            taraBala = taraBala,
            keyInfluences = keyInfluences,
            priorities = priorities,
            cautions = cautions,
            timingGuidance = timingGuidance,
            traditionalRemedies = traditionalRemedies,
            varaName = "${vara.sanskritName} (${vara.englishName})",
            tithiName = tithi.name,
            paksha = paksha,
            nakshatraName = dailyNakshatraName,
            yogaName = yoga.name,
            karanaName = karana.name,
            sunriseFormatted = panchang?.sunrise?.format(timeFormatter),
            sunsetFormatted = panchang?.sunset?.format(timeFormatter),
            astrologicalFactorsSummary = factorsSummary
        )
    }

    private fun generateThemeAndFocus(
        taraBala: TaraBalaInfo,
        houseFromMoon: Int?,
        mahaLord: String,
        antarLord: String,
        score: Int
    ): Pair<String, String> {
        val theme = when {
            score >= 80 -> "Harmonious Manifestation & Auspicious Action"
            score >= 65 -> "Focused Progress with Purposeful Execution"
            score >= 50 -> "Balanced Diligence & Mindful Communication"
            else -> "Reflective Self-Care & Strategic Patience"
        }

        val focus = when (taraBala.taraNumber) {
            2 -> "Material growth, financial organization, and tangible gains."
            4 -> "Home tranquility, emotional grounding, and security."
            6 -> "Professional accomplishment, spiritual discipline, and skill cultivation."
            8, 9 -> "Collaborative ventures, partnership goodwill, and creative flow."
            3 -> "Careful deliberation, avoiding hasty commitments, and risk mitigation."
            5 -> "Overcoming obstacles with diplomatic tact and patience."
            7 -> "Rest, inward introspection, and avoiding high-stakes friction."
            else -> "Physical well-being, foundational clarity, and steady personal rhythm."
        }

        return Pair(theme, focus)
    }

    private fun generatePriorities(
        taraBala: TaraBalaInfo,
        houseFromMoon: Int?,
        mahaLord: String,
        antarLord: String,
        yoga: NityaYoga,
        tithi: Tithi
    ): List<DailyRecommendation> {
        val list = mutableListOf<DailyRecommendation>()

        // 1. Focus & Career Priority
        when (houseFromMoon) {
            10, 11, 3, 6 -> list.add(
                DailyRecommendation(
                    category = "Professional Action & Goals",
                    advice = "Channel initiative into high-priority tasks and visible projects. Lunar Gochar strongly supports practical execution and tangible results.",
                    astrologicalReason = "Transit Moon activates Upachaya house ($houseFromMoon from Moon) and supportive Tara Bala."
                )
            )
            else -> list.add(
                DailyRecommendation(
                    category = "Strategy & Workflow",
                    advice = "Prioritize steady, well-structured responsibilities over sudden expansion. Double-check details and maintain clear documentation.",
                    astrologicalReason = "Current Gochar favors methodical organization and consolidating existing progress."
                )
            )
        }

        // 2. Intellectual / Communication Priority
        list.add(
            DailyRecommendation(
                category = "Dialogue & Decision Making",
                advice = "Engage in constructive listening and articulate ideas with calm clarity. Benefic yoga energies support mutual understanding.",
                astrologicalReason = "Aligned with ${yoga.name} Nitya Yoga and active $antarLord sub-period vibration."
            )
        )

        // 3. Spiritual / Health Priority
        list.add(
            DailyRecommendation(
                category = "Mindset & Wellness",
                advice = "Dedicate time to grounding practices, hydration, and mindful breathing during morning hours.",
                astrologicalReason = "Harmonizes the subtle lunar influence and revitalizes physical vitality."
            )
        )

        return list
    }

    private fun generateCautions(
        taraBala: TaraBalaInfo,
        houseFromMoon: Int?,
        mahaLord: String,
        antarLord: String,
        rahukaal: TimeInterval?
    ): List<DailyCaution> {
        val list = mutableListOf<DailyCaution>()

        // Caution 1: Timing caution (Rahukaal)
        rahukaal?.let {
            list.add(
                DailyCaution(
                    category = "Timing & Auspicious Beginnings",
                    warning = "Avoid signing binding contracts or initiating critical new life events during the Rahukaal interval (${it.start.format(timeFormatter)} – ${it.end.format(timeFormatter)}).",
                    astrologicalReason = "Vedic Rahukaal is traditionally reserved for routine tasks rather than auspicious commencements."
                )
            )
        }

        // Caution 2: Risk / Impulsiveness caution
        if (taraBala.taraNumber in listOf(3, 5, 7) || houseFromMoon in listOf(8, 12)) {
            list.add(
                DailyCaution(
                    category = "Financial & Emotional Prudence",
                    warning = "Steer clear of speculative financial bets, unvetted investments, or entering into heated arguments.",
                    astrologicalReason = "Activated by ${taraBala.taraName} and Gochar Moon in the ${houseFromMoon?.let { TransitCalculator.getHouseOrdinal(it) } ?: "sensitive"} Bhava."
                )
            )
        } else {
            list.add(
                DailyCaution(
                    category = "Energy Conservation",
                    warning = "Do not overcommit to external requests at the expense of personal rest and core priorities.",
                    astrologicalReason = "Preserves the vital Prana balance across the day's planetary shifts."
                )
            )
        }

        return list
    }

    private fun generateRemedies(
        mahaLord: String,
        antarLord: String,
        taraBala: TaraBalaInfo,
        houseFromMoon: Int?,
        vara: Vara
    ): List<TraditionalRemedy> {
        val list = mutableListOf<TraditionalRemedy>()

        // Remedy 1: Sun / Morning Alignment
        list.add(
            TraditionalRemedy(
                title = "Surya Arghya & Pratah Smaran",
                practice = "Offer clean water to the morning sun at sunrise while cultivating gratitude and inner clarity.",
                targetGrahaOrEnergy = "Surya (Sun) & Vital Energy (Prana)",
                traditionalContext = "Traditional daily Vedic practice to harmonize the solar vitality that energizes all planetary transits."
            )
        )

        // Remedy 2: Tailored to active Dasha Lord
        when (mahaLord.lowercase().trim()) {
            "jupiter", "guru" -> list.add(
                TraditionalRemedy(
                    title = "Guru Upasana & Knowledge Reverence",
                    practice = "Engage with sacred learning or express respect to teachers/elders; apply a subtle Chandan/Haldi tilak.",
                    targetGrahaOrEnergy = "Brihaspati (Jupiter)",
                    traditionalContext = "Harmonizes the active Jupiter Mahadasha, fostering wisdom, moral strength, and spiritual growth."
                )
            )
            "saturn", "shani" -> list.add(
                TraditionalRemedy(
                    title = "Shani Shanti & Seva",
                    practice = "Practice quiet patience, assist an elderly person or service worker, or light a sesame oil lamp in the evening.",
                    targetGrahaOrEnergy = "Shani (Saturn)",
                    traditionalContext = "Calms the discipline-testing energies of Saturn Dasha, transforming hurdles into endurance."
                )
            )
            "mercury", "budha" -> list.add(
                TraditionalRemedy(
                    title = "Budha Upasana & Green Offerings",
                    practice = "Water a green Tulsi or indoor plant, practice clear mindful speech, and avoid deceit.",
                    targetGrahaOrEnergy = "Budha (Mercury)",
                    traditionalContext = "Strengthens intellect, speech clarity, and business acuity under Mercury's active period."
                )
            )
            "venus", "shukra" -> list.add(
                TraditionalRemedy(
                    title = "Shukra Upasana & Aesthetic Harmony",
                    practice = "Cultivate kindness, wear clean comfortable attire, and respect women and artists in your circle.",
                    targetGrahaOrEnergy = "Shukra (Venus)",
                    traditionalContext = "Enhances contentment, artistic refinement, and relationship harmony during Venus period."
                )
            )
            "mars", "mangala" -> list.add(
                TraditionalRemedy(
                    title = "Mangala Shanti & Hanuman Chalisa",
                    practice = "Recite or contemplate the Hanuman Chalisa, maintain disciplined physical exercise, and control anger.",
                    targetGrahaOrEnergy = "Mangala (Mars)",
                    traditionalContext = "Channels Martian courage positively into constructive action and protects against hasty conflict."
                )
            )
            "moon", "chandra" -> list.add(
                TraditionalRemedy(
                    title = "Chandra Shanti & Mental Serenity",
                    practice = "Hydrate well, spend quiet time under moonlight or by water, and practice evening pranayama.",
                    targetGrahaOrEnergy = "Chandra (Moon)",
                    traditionalContext = "Soothes emotional fluctuations and nurtures intuitive clarity during Moon Dasha."
                )
            )
            else -> list.add(
                TraditionalRemedy(
                    title = "Navagraha Smaran & Gayatri Dhyana",
                    practice = "Recite the universal Gayatri Mantra or Navagraha Stotram with sincere focus during morning twilight.",
                    targetGrahaOrEnergy = "Universal Planetary Harmony",
                    traditionalContext = "Traditional Vedic invocations that balance planetary polarities across all nine Grahas."
                )
            )
        }

        return list
    }
}
