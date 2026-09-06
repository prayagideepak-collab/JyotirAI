package com.example.domain.engine

import com.example.domain.models.*

/**
 * Deterministic Parashari Yoga Analysis Engine.
 * Evaluates classical Vedic planetary combinations based on astronomical planetary positions,
 * Whole-Sign Bhava houses, sign lordships, and planetary dignities.
 */
object YogaAnalysisEngine {

    /**
     * Evaluates all supported classical Vedic Yogas for the provided profile.
     */
    fun analyzeYogas(profile: AstrologyProfile): List<YogaAnalysisResult> {
        val rashiChart = profile.rashiChart
        val planets = profile.planetPositions
        val lagnaSignIndex = profile.lagnaSignIndex
        val lagnaRashi = Rashi.fromIndex(lagnaSignIndex)
        val moonPos = planets.firstOrNull { it.planet.equals("moon", ignoreCase = true) }

        val planetByHouse = mutableMapOf<Int, MutableList<PlanetPosition>>()
        val planetByName = mutableMapOf<String, PlanetPosition>()
        planets.forEach { p ->
            val normPlanet = normalizePlanetName(p.planet)
            planetByName[normPlanet] = p
            planetByHouse.getOrPut(p.house) { mutableListOf() }.add(p)
        }

        // Calculate house lords for all 12 houses based on Whole Sign Lagna
        val houseLords = (1..12).associateWith { house ->
            val signIndex = (lagnaSignIndex + (house - 1)).mod(12)
            Rashi.fromIndex(signIndex).lord
        }

        val results = mutableListOf<YogaAnalysisResult>()

        // 1. Pancha Mahapurusha Yogas
        results.addAll(evaluatePanchaMahapurushaYogas(planets))

        // 2. Gaja Kesari Yoga
        evaluateGajaKesariYoga(planetByName, moonPos)?.let { results.add(it) }

        // 3. Budhaditya Yoga
        evaluateBudhadityaYoga(planetByName)?.let { results.add(it) }

        // 4. Chandra-Mangala Yoga
        evaluateChandraMangalaYoga(planetByName, moonPos)?.let { results.add(it) }

        // 5. Parashari Kendra-Trikona Raja Yogas
        results.addAll(evaluateRajaYogas(houseLords, planetByName, planetByHouse, lagnaRashi))

        // 6. Dhana Yogas (Wealth Combinations)
        results.addAll(evaluateDhanaYogas(houseLords, planetByName, planetByHouse))

        // 7. Viparita Raja Yogas (Harsha, Sarala, Vimala)
        results.addAll(evaluateViparitaRajaYogas(houseLords, planetByName))

        // 8. Neecha Bhanga Raja Yoga
        results.addAll(evaluateNeechaBhangaYogas(planets, lagnaSignIndex, moonPos?.signIndex ?: lagnaSignIndex))

        // 9. Classical Benefic Yogas (Amala, Saraswati, Lakshmi)
        evaluateAmalaYoga(planetByHouse, moonPos)?.let { results.add(it) }
        evaluateSaraswatiYoga(planetByName)?.let { results.add(it) }
        evaluateLakshmiYoga(houseLords, planetByName, lagnaRashi)?.let { results.add(it) }

        // 10. Chandra Yogas (Sunafa, Anafa, Durudhara)
        evaluateChandraYogas(planetByName, moonPos)?.let { results.add(it) }

        // Deterministic sorting: Detected first, then highest strength, then alphabetical ID
        return results.sortedWith(
            compareByDescending<YogaAnalysisResult> { it.isDetected }
                .thenByDescending { it.strength.scoreMultiplier }
                .thenBy { it.id }
        )
    }

    private fun evaluatePanchaMahapurushaYogas(planets: List<PlanetPosition>): List<YogaAnalysisResult> {
        val list = mutableListOf<YogaAnalysisResult>()
        val kendraHouses = setOf(1, 4, 7, 10)

        // Mars -> Ruchaka Yoga (Own sign Aries, Scorpio or Exalted Capricorn in Kendra)
        val mars = planets.firstOrNull { it.planet.equals("mars", ignoreCase = true) }
        val isRuchaka = mars != null && mars.house in kendraHouses &&
                (mars.signIndex in setOf(0, 7, 9)) // Aries, Scorpio, Capricorn
        list.add(
            YogaAnalysisResult(
                id = "mahapurusha_ruchaka",
                name = "Ruchaka Yoga (Pancha Mahapurusha)",
                sanskritName = "रुचक महापुरुष योग",
                category = YogaCategory.MAHAPURUSHA_YOGA,
                isDetected = isRuchaka,
                strength = if (isRuchaka) calculateMahapurushaStrength(mars!!) else YogaStrength.INACTIVE,
                participatingPlanets = if (mars != null) listOf("Mars") else emptyList(),
                participatingHouses = if (isRuchaka) listOf(mars!!.house) else emptyList(),
                participatingSigns = if (isRuchaka) listOf(mars!!.sign) else emptyList(),
                evidence = if (isRuchaka) "Mars is placed in Kendra House ${mars!!.house} in ${mars.sign} (${mars.dignity.displayName})."
                else "Mars is not in a Kendra in own or exaltation sign.",
                classicalRule = "Mars in Kendra (1, 4, 7, 10) in own sign (Mesha, Vrishchika) or exaltation sign (Makara) forms Ruchaka Yoga.",
                calculationBasis = "Evaluated Mars house placement (${mars?.house}) and sign (${mars?.signIndex}).",
                positiveImpact = "Bestows extraordinary valor, physical leadership, administrative command, wealth, and executive authority."
            )
        )

        // Mercury -> Bhadra Yoga (Own sign Gemini, Virgo or Exalted Virgo in Kendra)
        val mercury = planets.firstOrNull { it.planet.equals("mercury", ignoreCase = true) }
        val isBhadra = mercury != null && mercury.house in kendraHouses &&
                (mercury.signIndex in setOf(2, 5)) // Gemini, Virgo
        list.add(
            YogaAnalysisResult(
                id = "mahapurusha_bhadra",
                name = "Bhadra Yoga (Pancha Mahapurusha)",
                sanskritName = "भद्र महापुरुष योग",
                category = YogaCategory.MAHAPURUSHA_YOGA,
                isDetected = isBhadra,
                strength = if (isBhadra) calculateMahapurushaStrength(mercury!!) else YogaStrength.INACTIVE,
                participatingPlanets = if (mercury != null) listOf("Mercury") else emptyList(),
                participatingHouses = if (isBhadra) listOf(mercury!!.house) else emptyList(),
                participatingSigns = if (isBhadra) listOf(mercury!!.sign) else emptyList(),
                evidence = if (isBhadra) "Mercury is placed in Kendra House ${mercury!!.house} in ${mercury.sign} (${mercury.dignity.displayName})."
                else "Mercury is not in a Kendra in own or exaltation sign.",
                classicalRule = "Mercury in Kendra (1, 4, 7, 10) in own sign (Mithuna) or exaltation/own sign (Kanya) forms Bhadra Yoga.",
                calculationBasis = "Evaluated Mercury house placement (${mercury?.house}) and sign (${mercury?.signIndex}).",
                positiveImpact = "Endows razor-sharp intellect, eloquence, mastery of commerce, writing, scholarship, and longevity."
            )
        )

        // Jupiter -> Hamsa Yoga (Own sign Sagittarius, Pisces or Exalted Cancer in Kendra)
        val jupiter = planets.firstOrNull { it.planet.equals("jupiter", ignoreCase = true) }
        val isHamsa = jupiter != null && jupiter.house in kendraHouses &&
                (jupiter.signIndex in setOf(3, 8, 11)) // Cancer, Sagittarius, Pisces
        list.add(
            YogaAnalysisResult(
                id = "mahapurusha_hamsa",
                name = "Hamsa Yoga (Pancha Mahapurusha)",
                sanskritName = "हंस महापुरुष योग",
                category = YogaCategory.MAHAPURUSHA_YOGA,
                isDetected = isHamsa,
                strength = if (isHamsa) calculateMahapurushaStrength(jupiter!!) else YogaStrength.INACTIVE,
                participatingPlanets = if (jupiter != null) listOf("Jupiter") else emptyList(),
                participatingHouses = if (isHamsa) listOf(jupiter!!.house) else emptyList(),
                participatingSigns = if (isHamsa) listOf(jupiter!!.sign) else emptyList(),
                evidence = if (isHamsa) "Jupiter is placed in Kendra House ${jupiter!!.house} in ${jupiter.sign} (${jupiter.dignity.displayName})."
                else "Jupiter is not in a Kendra in own or exaltation sign.",
                classicalRule = "Jupiter in Kendra (1, 4, 7, 10) in own sign (Dhanu, Meena) or exaltation sign (Karka) forms Hamsa Yoga.",
                calculationBasis = "Evaluated Jupiter house placement (${jupiter?.house}) and sign (${jupiter?.signIndex}).",
                positiveImpact = "Blesses with righteous wisdom, spiritual honor, high social standing, ethical leadership, and divine grace."
            )
        )

        // Venus -> Malavya Yoga (Own sign Taurus, Libra or Exalted Pisces in Kendra)
        val venus = planets.firstOrNull { it.planet.equals("venus", ignoreCase = true) }
        val isMalavya = venus != null && venus.house in kendraHouses &&
                (venus.signIndex in setOf(1, 6, 11)) // Taurus, Libra, Pisces
        list.add(
            YogaAnalysisResult(
                id = "mahapurusha_malavya",
                name = "Malavya Yoga (Pancha Mahapurusha)",
                sanskritName = "मालव्य महापुरुष योग",
                category = YogaCategory.MAHAPURUSHA_YOGA,
                isDetected = isMalavya,
                strength = if (isMalavya) calculateMahapurushaStrength(venus!!) else YogaStrength.INACTIVE,
                participatingPlanets = if (venus != null) listOf("Venus") else emptyList(),
                participatingHouses = if (isMalavya) listOf(venus!!.house) else emptyList(),
                participatingSigns = if (isMalavya) listOf(venus!!.sign) else emptyList(),
                evidence = if (isMalavya) "Venus is placed in Kendra House ${venus!!.house} in ${venus.sign} (${venus.dignity.displayName})."
                else "Venus is not in a Kendra in own or exaltation sign.",
                classicalRule = "Venus in Kendra (1, 4, 7, 10) in own sign (Vrishabha, Tula) or exaltation sign (Meena) forms Malavya Yoga.",
                calculationBasis = "Evaluated Venus house placement (${venus?.house}) and sign (${venus?.signIndex}).",
                positiveImpact = "Confers aesthetic refinement, vehicles, luxury, domestic bliss, artistic eminence, and magnetism."
            )
        )

        // Saturn -> Sasa Yoga (Own sign Capricorn, Aquarius or Exalted Libra in Kendra)
        val saturn = planets.firstOrNull { it.planet.equals("saturn", ignoreCase = true) }
        val isSasa = saturn != null && saturn.house in kendraHouses &&
                (saturn.signIndex in setOf(6, 9, 10)) // Libra, Capricorn, Aquarius
        list.add(
            YogaAnalysisResult(
                id = "mahapurusha_sasa",
                name = "Sasa Yoga (Pancha Mahapurusha)",
                sanskritName = "शश महापुरुष योग",
                category = YogaCategory.MAHAPURUSHA_YOGA,
                isDetected = isSasa,
                strength = if (isSasa) calculateMahapurushaStrength(saturn!!) else YogaStrength.INACTIVE,
                participatingPlanets = if (saturn != null) listOf("Saturn") else emptyList(),
                participatingHouses = if (isSasa) listOf(saturn!!.house) else emptyList(),
                participatingSigns = if (isSasa) listOf(saturn!!.sign) else emptyList(),
                evidence = if (isSasa) "Saturn is placed in Kendra House ${saturn!!.house} in ${saturn.sign} (${saturn.dignity.displayName})."
                else "Saturn is not in a Kendra in own or exaltation sign.",
                classicalRule = "Saturn in Kendra (1, 4, 7, 10) in own sign (Makara, Kumbha) or exaltation sign (Tula) forms Sasa Yoga.",
                calculationBasis = "Evaluated Saturn house placement (${saturn?.house}) and sign (${saturn?.signIndex}).",
                positiveImpact = "Grants commanding authority over masses, organizational resilience, enduring wealth, and political mastery."
            )
        )

        return list
    }

    private fun evaluateGajaKesariYoga(
        planetByName: Map<String, PlanetPosition>,
        moonPos: PlanetPosition?
    ): YogaAnalysisResult? {
        val jupiter = planetByName["jupiter"] ?: return null
        val moon = moonPos ?: return null

        val kendraDistances = setOf(1, 4, 7, 10)
        val distance = ((jupiter.signIndex - moon.signIndex).mod(12)) + 1
        val isGk = distance in kendraDistances

        val strength = when {
            !isGk -> YogaStrength.INACTIVE
            jupiter.dignity == PlanetDignity.EXALTED || jupiter.dignity == PlanetDignity.OWN_SIGN -> YogaStrength.EXCELLENT
            jupiter.dignity == PlanetDignity.FRIEND || jupiter.dignity == PlanetDignity.MOOLATRIKONA -> YogaStrength.STRONG
            jupiter.dignity == PlanetDignity.DEBILITATED -> YogaStrength.WEAK
            else -> YogaStrength.MODERATE
        }

        return YogaAnalysisResult(
            id = "gaja_kesari_yoga",
            name = "Gaja Kesari Yoga",
            sanskritName = "गजकेसरी योग",
            category = YogaCategory.AUSPICIOUS_COMBINATION,
            isDetected = isGk,
            strength = strength,
            participatingPlanets = listOf("Jupiter", "Moon"),
            participatingHouses = if (isGk) listOf(moon.house, jupiter.house).distinct() else emptyList(),
            participatingSigns = if (isGk) listOf(moon.sign, jupiter.sign).distinct() else emptyList(),
            evidence = if (isGk) "Jupiter in ${jupiter.sign} (House ${jupiter.house}) is in a Kendra (${distance}th house) from Moon in ${moon.sign} (House ${moon.house}). Jupiter dignity: ${jupiter.dignity.displayName}."
            else "Jupiter is in ${distance}th house from Moon (not 1, 4, 7, or 10).",
            classicalRule = "Jupiter in Kendra (1, 4, 7, 10) from the Moon forms Gaja Kesari Yoga.",
            calculationBasis = "Calculated relative whole-sign angular distance from Moon (${moon.signIndex}) to Jupiter (${jupiter.signIndex}) = $distance.",
            positiveImpact = "Bestows distinguished repute, scholarly intellect, lasting respect, wealth, moral leadership, and victory over adversaries."
        )
    }

    private fun evaluateBudhadityaYoga(planetByName: Map<String, PlanetPosition>): YogaAnalysisResult? {
        val sun = planetByName["sun"] ?: return null
        val mercury = planetByName["mercury"] ?: return null

        val isConjoined = sun.signIndex == mercury.signIndex
        val degreeDiff = kotlin.math.abs(sun.totalLongitude - mercury.totalLongitude)
        val isCombust = isConjoined && degreeDiff <= 3.5

        val strength = when {
            !isConjoined -> YogaStrength.INACTIVE
            isCombust -> YogaStrength.MILD
            mercury.dignity in setOf(PlanetDignity.EXALTED, PlanetDignity.OWN_SIGN, PlanetDignity.MOOLATRIKONA) -> YogaStrength.EXCELLENT
            mercury.dignity == PlanetDignity.FRIEND -> YogaStrength.STRONG
            mercury.dignity == PlanetDignity.DEBILITATED -> YogaStrength.WEAK
            else -> YogaStrength.MODERATE
        }

        return YogaAnalysisResult(
            id = "budhaditya_yoga",
            name = "Budhaditya Yoga",
            sanskritName = "बुधादित्य योग",
            category = YogaCategory.SURYA_YOGA,
            isDetected = isConjoined,
            strength = strength,
            participatingPlanets = listOf("Sun", "Mercury"),
            participatingHouses = if (isConjoined) listOf(sun.house) else emptyList(),
            participatingSigns = if (isConjoined) listOf(sun.sign) else emptyList(),
            evidence = if (isConjoined) {
                val combustNote = if (isCombust) " (Mercury is deeply combust within ${"%.2f".format(degreeDiff)}° of Sun)" else " (Separation: ${"%.2f".format(degreeDiff)}°)"
                "Sun and Mercury are conjoined in House ${sun.house} (${sun.sign})$combustNote."
            } else "Sun and Mercury are in different signs.",
            classicalRule = "Sun and Mercury conjoined in the same sign forms Budhaditya Yoga, conferring sharp intellect.",
            calculationBasis = "Sun sign (${sun.signIndex}) vs Mercury sign (${mercury.signIndex}), degree separation = ${"%.2f".format(degreeDiff)}°.",
            limitations = if (isCombust) "Mercury is combust within 3.5° of the Sun." else null,
            positiveImpact = "Sharpens analytical prowess, administrative acumen, communication skills, learning capacity, and repute."
        )
    }

    private fun evaluateChandraMangalaYoga(
        planetByName: Map<String, PlanetPosition>,
        moonPos: PlanetPosition?
    ): YogaAnalysisResult? {
        val mars = planetByName["mars"] ?: return null
        val moon = moonPos ?: return null

        val isConjoined = mars.signIndex == moon.signIndex
        val isOpposite = ((mars.signIndex - moon.signIndex).mod(12)) == 6

        val isDetected = isConjoined || isOpposite
        val strength = when {
            !isDetected -> YogaStrength.INACTIVE
            mars.dignity == PlanetDignity.EXALTED || moon.dignity == PlanetDignity.EXALTED -> YogaStrength.EXCELLENT
            mars.dignity == PlanetDignity.OWN_SIGN || moon.dignity == PlanetDignity.OWN_SIGN -> YogaStrength.STRONG
            mars.dignity == PlanetDignity.DEBILITATED || moon.dignity == PlanetDignity.DEBILITATED -> YogaStrength.MILD
            else -> YogaStrength.MODERATE
        }

        return YogaAnalysisResult(
            id = "chandra_mangala_yoga",
            name = "Chandra-Mangala Yoga",
            sanskritName = "चन्द्र-मंगल योग",
            category = YogaCategory.CHANDRA_YOGA,
            isDetected = isDetected,
            strength = strength,
            participatingPlanets = listOf("Moon", "Mars"),
            participatingHouses = if (isDetected) listOf(moon.house, mars.house).distinct() else emptyList(),
            participatingSigns = if (isDetected) listOf(moon.sign, mars.sign).distinct() else emptyList(),
            evidence = if (isConjoined) "Moon and Mars are conjoined in House ${moon.house} (${moon.sign})."
            else if (isOpposite) "Moon (House ${moon.house}) and Mars (House ${mars.house}) are in mutual 7th aspect."
            else "Moon and Mars are neither conjoined nor in mutual aspect.",
            classicalRule = "Moon and Mars in conjunction or mutual 7th aspect forms Chandra-Mangala Yoga, driving financial enterprise.",
            calculationBasis = "Evaluated Moon sign (${moon.signIndex}) and Mars sign (${mars.signIndex}).",
            positiveImpact = "Generates strong commercial drive, financial accumulation, property acquisition, and bold initiative."
        )
    }

    private fun evaluateRajaYogas(
        houseLords: Map<Int, String>,
        planetByName: Map<String, PlanetPosition>,
        planetByHouse: Map<Int, List<PlanetPosition>>,
        lagnaRashi: Rashi
    ): List<YogaAnalysisResult> {
        val list = mutableListOf<YogaAnalysisResult>()
        val kendras = listOf(1, 4, 7, 10)
        val trikonas = listOf(1, 5, 9)

        // Generate combinations of Kendra and Trikona lords
        val examinedPairs = mutableSetOf<Pair<Int, Int>>()
        for (k in kendras) {
            for (t in trikonas) {
                if (k == t) continue
                val pair = if (k < t) Pair(k, t) else Pair(t, k)
                if (examinedPairs.contains(pair)) continue
                examinedPairs.add(pair)

                val lord1Name = houseLords[pair.first]?.lowercase() ?: continue
                val lord2Name = houseLords[pair.second]?.lowercase() ?: continue

                // If same planet owns both Kendra and Trikona (Yogakaraka!)
                if (lord1Name == lord2Name) {
                    val p = planetByName[lord1Name]
                    if (p != null) {
                        val isFavorable = p.house in kendras || p.house in trikonas || p.house in setOf(2, 11)
                        list.add(
                            YogaAnalysisResult(
                                id = "raja_yoga_yogakaraka_${lord1Name}",
                                name = "Yogakaraka Single-Planet Raja Yoga (${p.planet})",
                                sanskritName = "योगकारक राजयोग (${p.sanskritName})",
                                category = YogaCategory.RAJA_YOGA,
                                isDetected = true,
                                strength = if (p.dignity in setOf(PlanetDignity.EXALTED, PlanetDignity.OWN_SIGN, PlanetDignity.MOOLATRIKONA)) YogaStrength.EXCELLENT else if (isFavorable) YogaStrength.STRONG else YogaStrength.MODERATE,
                                participatingPlanets = listOf(p.planet),
                                participatingHouses = listOf(p.house),
                                participatingSigns = listOf(p.sign),
                                evidence = "${p.planet} simultaneously owns Kendra House ${pair.first} and Trikona House ${pair.second} for ${lagnaRashi.sanskritName} Lagna, placed in House ${p.house} (${p.sign}).",
                                classicalRule = "A single planet owning both a Kendra (1, 4, 7, 10) and a Trikona (5, 9) becomes a functional Yogakaraka Raja Yoga producer.",
                                calculationBasis = "House ${pair.first} lord and House ${pair.second} lord = ${p.planet}.",
                                positiveImpact = "Provides supreme rise in status, authority, executive power, prosperity, and continuous good fortune."
                            )
                        )
                    }
                    continue
                }

                val p1 = planetByName[lord1Name] ?: continue
                val p2 = planetByName[lord2Name] ?: continue

                // Conjunction: both in same house
                val isConjunction = p1.house == p2.house
                // Mutual aspect: 7 houses apart
                val isMutualAspect = ((p1.house - p2.house).mod(12)) == 6
                // Parivartana: p1 in p2's house and p2 in p1's house
                val isParivartana = (p1.house == pair.second && p2.house == pair.first)

                if (isConjunction || isMutualAspect || isParivartana) {
                    val relType = when {
                        isConjunction -> "Conjoined in House ${p1.house}"
                        isMutualAspect -> "In mutual 7th aspect between House ${p1.house} and House ${p2.house}"
                        else -> "Parivartana (Exchange of House ${pair.first} and ${pair.second})"
                    }
                    val isDharmaKarma = (pair.first == 9 && pair.second == 10) || (pair.first == 10 && pair.second == 9)
                    val yogaName = if (isDharmaKarma) "Dharma-Karmadhipati Raja Yoga (9th & 10th Lords)"
                    else "Raja Yoga (${pair.first}th & ${pair.second}th Lords)"
                    val sanskritTitle = if (isDharmaKarma) "धर्मा-कर्माधिपति राजयोग"
                    else "केन्द्र-त्रिकोण राजयोग (${pair.first}-${pair.second})"

                    val avgDignity = if (p1.dignity == PlanetDignity.EXALTED || p2.dignity == PlanetDignity.EXALTED) YogaStrength.EXCELLENT
                    else if (p1.dignity in setOf(PlanetDignity.OWN_SIGN, PlanetDignity.FRIEND) && p2.dignity in setOf(PlanetDignity.OWN_SIGN, PlanetDignity.FRIEND)) YogaStrength.STRONG
                    else if (p1.dignity == PlanetDignity.DEBILITATED || p2.dignity == PlanetDignity.DEBILITATED) YogaStrength.MILD
                    else YogaStrength.MODERATE

                    list.add(
                        YogaAnalysisResult(
                            id = "raja_yoga_${pair.first}_${pair.second}",
                            name = yogaName,
                            sanskritName = sanskritTitle,
                            category = YogaCategory.RAJA_YOGA,
                            isDetected = true,
                            strength = avgDignity,
                            participatingPlanets = listOf(p1.planet, p2.planet),
                            participatingHouses = listOf(p1.house, p2.house).distinct(),
                            participatingSigns = listOf(p1.sign, p2.sign).distinct(),
                            evidence = "${pair.first}th lord ${p1.planet} and ${pair.second}th lord ${p2.planet} are $relType.",
                            classicalRule = "Association between a Kendra lord (1, 4, 7, 10) and a Trikona lord (1, 5, 9) produces a powerful Parashari Raja Yoga.",
                            calculationBasis = "Evaluated association of ${p1.planet} (House ${pair.first} lord) and ${p2.planet} (House ${pair.second} lord).",
                            positiveImpact = "Elevates life standing, grants professional leadership, high reputation, and executive success."
                        )
                    )
                }
            }
        }
        return list
    }

    private fun evaluateDhanaYogas(
        houseLords: Map<Int, String>,
        planetByName: Map<String, PlanetPosition>,
        planetByHouse: Map<Int, List<PlanetPosition>>
    ): List<YogaAnalysisResult> {
        val list = mutableListOf<YogaAnalysisResult>()
        val dhanaHouses = listOf(Pair(2, 11), Pair(1, 2), Pair(5, 9), Pair(2, 5), Pair(2, 9), Pair(5, 11), Pair(9, 11))

        for (pair in dhanaHouses) {
            val lord1Name = houseLords[pair.first]?.lowercase() ?: continue
            val lord2Name = houseLords[pair.second]?.lowercase() ?: continue
            if (lord1Name == lord2Name) continue

            val p1 = planetByName[lord1Name] ?: continue
            val p2 = planetByName[lord2Name] ?: continue

            val isConjunction = p1.house == p2.house
            val isMutualAspect = ((p1.house - p2.house).mod(12)) == 6
            val isParivartana = (p1.house == pair.second && p2.house == pair.first)

            if (isConjunction || isMutualAspect || isParivartana) {
                val relType = when {
                    isConjunction -> "Conjoined in House ${p1.house}"
                    isMutualAspect -> "Mutual 7th aspect between House ${p1.house} and ${p2.house}"
                    else -> "Mutual exchange of Houses ${pair.first} and ${pair.second}"
                }
                list.add(
                    YogaAnalysisResult(
                        id = "dhana_yoga_${pair.first}_${pair.second}",
                        name = "Dhana Yoga (${pair.first}th & ${pair.second}th Wealth Lords)",
                        sanskritName = "धन योग (${pair.first}-${pair.second} भाव स्वामी)",
                        category = YogaCategory.DHANA_YOGA,
                        isDetected = true,
                        strength = if (p1.dignity == PlanetDignity.EXALTED || p2.dignity == PlanetDignity.EXALTED) YogaStrength.EXCELLENT else YogaStrength.STRONG,
                        participatingPlanets = listOf(p1.planet, p2.planet),
                        participatingHouses = listOf(p1.house, p2.house).distinct(),
                        participatingSigns = listOf(p1.sign, p2.sign).distinct(),
                        evidence = "${pair.first}th house lord ${p1.planet} and ${pair.second}th house lord ${p2.planet} are $relType.",
                        classicalRule = "Connection between wealth houses (2nd, 11th) and fortune houses (1st, 5th, 9th) creates Dhana Yoga.",
                        calculationBasis = "Evaluated wealth lord connection for houses ${pair.first} and ${pair.second}.",
                        positiveImpact = "Blesses with substantial asset accumulation, steady financial gains, and material stability."
                    )
                )
            }
        }
        return list
    }

    private fun evaluateViparitaRajaYogas(
        houseLords: Map<Int, String>,
        planetByName: Map<String, PlanetPosition>
    ): List<YogaAnalysisResult> {
        val list = mutableListOf<YogaAnalysisResult>()
        val trikHouses = setOf(6, 8, 12)

        // 1. Harsha Yoga: 6th lord in 6th, 8th, or 12th
        val lord6Name = houseLords[6]?.lowercase()
        val p6 = lord6Name?.let { planetByName[it] }
        val isHarsha = p6 != null && p6.house in trikHouses
        list.add(
            YogaAnalysisResult(
                id = "viparita_harsha",
                name = "Harsha Yoga (Viparita Raja Yoga)",
                sanskritName = "हर्ष विपरीत राजयोग",
                category = YogaCategory.VIPARITA_RAJA_YOGA,
                isDetected = isHarsha,
                strength = if (isHarsha) YogaStrength.STRONG else YogaStrength.INACTIVE,
                participatingPlanets = if (p6 != null) listOf(p6.planet) else emptyList(),
                participatingHouses = if (isHarsha) listOf(p6!!.house) else emptyList(),
                participatingSigns = if (isHarsha) listOf(p6!!.sign) else emptyList(),
                evidence = if (isHarsha) "6th lord ${p6!!.planet} is placed in dusthana House ${p6.house} (${p6.sign})."
                else "6th lord is not in a dusthana house (6, 8, 12).",
                classicalRule = "6th lord placed in 6th, 8th, or 12th house forms Harsha Yoga, conferring triumph over obstacles and enemies.",
                calculationBasis = "Evaluated 6th lord (${p6?.planet}) placement in house ${p6?.house}.",
                positiveImpact = "Grants immunity against rivals, rapid recovery from difficulties, unyielding health resilience, and victorious outcome."
            )
        )

        // 2. Sarala Yoga: 8th lord in 6th, 8th, or 12th
        val lord8Name = houseLords[8]?.lowercase()
        val p8 = lord8Name?.let { planetByName[it] }
        val isSarala = p8 != null && p8.house in trikHouses
        list.add(
            YogaAnalysisResult(
                id = "viparita_sarala",
                name = "Sarala Yoga (Viparita Raja Yoga)",
                sanskritName = "सरल विपरीत राजयोग",
                category = YogaCategory.VIPARITA_RAJA_YOGA,
                isDetected = isSarala,
                strength = if (isSarala) YogaStrength.STRONG else YogaStrength.INACTIVE,
                participatingPlanets = if (p8 != null) listOf(p8.planet) else emptyList(),
                participatingHouses = if (isSarala) listOf(p8!!.house) else emptyList(),
                participatingSigns = if (isSarala) listOf(p8!!.sign) else emptyList(),
                evidence = if (isSarala) "8th lord ${p8!!.planet} is placed in dusthana House ${p8.house} (${p8.sign})."
                else "8th lord is not in a dusthana house (6, 8, 12).",
                classicalRule = "8th lord placed in 6th, 8th, or 12th house forms Sarala Yoga, conferring long life, fearlessness, and sudden gains.",
                calculationBasis = "Evaluated 8th lord (${p8?.planet}) placement in house ${p8?.house}.",
                positiveImpact = "Confers fearlessness in crises, longevity, unexpected turnaround of fortunes, and scholarly depth."
            )
        )

        // 3. Vimala Yoga: 12th lord in 6th, 8th, or 12th
        val lord12Name = houseLords[12]?.lowercase()
        val p12 = lord12Name?.let { planetByName[it] }
        val isVimala = p12 != null && p12.house in trikHouses
        list.add(
            YogaAnalysisResult(
                id = "viparita_vimala",
                name = "Vimala Yoga (Viparita Raja Yoga)",
                sanskritName = "विमल विपरीत राजयोग",
                category = YogaCategory.VIPARITA_RAJA_YOGA,
                isDetected = isVimala,
                strength = if (isVimala) YogaStrength.STRONG else YogaStrength.INACTIVE,
                participatingPlanets = if (p12 != null) listOf(p12.planet) else emptyList(),
                participatingHouses = if (isVimala) listOf(p12!!.house) else emptyList(),
                participatingSigns = if (isVimala) listOf(p12!!.sign) else emptyList(),
                evidence = if (isVimala) "12th lord ${p12!!.planet} is placed in dusthana House ${p12.house} (${p12.sign})."
                else "12th lord is not in a dusthana house (6, 8, 12).",
                classicalRule = "12th lord placed in 6th, 8th, or 12th house forms Vimala Yoga, conferring righteous independent character and financial conservation.",
                calculationBasis = "Evaluated 12th lord (${p12?.planet}) placement in house ${p12?.house}.",
                positiveImpact = "Preserves wealth from frivolous expenditures, endows noble character, self-reliance, and spiritual tranquility."
            )
        )

        return list
    }

    private fun evaluateNeechaBhangaYogas(
        planets: List<PlanetPosition>,
        lagnaSignIndex: Int,
        moonSignIndex: Int
    ): List<YogaAnalysisResult> {
        val list = mutableListOf<YogaAnalysisResult>()
        val debilitatedPlanets = planets.filter { it.dignity == PlanetDignity.DEBILITATED }

        for (debPlanet in debilitatedPlanets) {
            val debSignIndex = debPlanet.signIndex
            val debLordName = Rashi.fromIndex(debSignIndex).lord.lowercase()
            val debLord = planets.firstOrNull { it.planet.equals(debLordName, ignoreCase = true) }

            val exaltationSignIndex = getExaltationSignIndex(debPlanet.planet)
            val exaltLordName = if (exaltationSignIndex != null) Rashi.fromIndex(exaltationSignIndex).lord.lowercase() else null
            val exaltLord = exaltLordName?.let { name -> planets.firstOrNull { it.planet.equals(name, ignoreCase = true) } }

            val isDebLordInKendraFromLagna = debLord != null && debLord.house in setOf(1, 4, 7, 10)
            val isDebLordInKendraFromMoon = debLord != null && ((((debLord.signIndex - moonSignIndex).mod(12)) + 1) in setOf(1, 4, 7, 10))
            val isExaltLordInKendraFromLagna = exaltLord != null && exaltLord.house in setOf(1, 4, 7, 10)
            val isPlanetItselfInKendra = debPlanet.house in setOf(1, 4, 7, 10)

            val isNeechaBhanga = isDebLordInKendraFromLagna || isDebLordInKendraFromMoon || isExaltLordInKendraFromLagna || isPlanetItselfInKendra

            if (isNeechaBhanga) {
                val reasons = mutableListOf<String>()
                if (isDebLordInKendraFromLagna) reasons.add("Debilitation sign lord ${debLord?.planet} is in Kendra (House ${debLord?.house}) from Lagna")
                if (isDebLordInKendraFromMoon) reasons.add("Debilitation sign lord ${debLord?.planet} is in Kendra from Moon")
                if (isExaltLordInKendraFromLagna) reasons.add("Exaltation sign lord ${exaltLord?.planet} is in Kendra from Lagna")
                if (isPlanetItselfInKendra) reasons.add("${debPlanet.planet} is itself in Kendra House ${debPlanet.house}")

                list.add(
                    YogaAnalysisResult(
                        id = "neecha_bhanga_${debPlanet.planet.lowercase()}",
                        name = "Neecha Bhanga Raja Yoga (${debPlanet.planet})",
                        sanskritName = "नीचभंग राजयोग (${debPlanet.sanskritName})",
                        category = YogaCategory.NEECHA_BHANGA_RAJA_YOGA,
                        isDetected = true,
                        strength = YogaStrength.STRONG,
                        participatingPlanets = listOfNotNull(debPlanet.planet, debLord?.planet, exaltLord?.planet).distinct(),
                        participatingHouses = listOfNotNull(debPlanet.house, debLord?.house, exaltLord?.house).distinct(),
                        participatingSigns = listOfNotNull(debPlanet.sign, debLord?.sign, exaltLord?.sign).distinct(),
                        evidence = "Debilitated ${debPlanet.planet} has its debility cancelled: ${reasons.joinToString("; ")}.",
                        classicalRule = "When the dispositor of a debilitated planet or its exaltation lord occupies a Kendra from Lagna or Moon, Neecha Bhanga Raja Yoga transforms adversity into royal status.",
                        calculationBasis = "Evaluated cancellation conditions for debilitated ${debPlanet.planet} in ${debPlanet.sign}.",
                        positiveImpact = "Transforms initial struggles into extraordinary long-term resilience, mastery, and towering triumph."
                    )
                )
            }
        }
        return list
    }

    private fun evaluateAmalaYoga(
        planetByHouse: Map<Int, List<PlanetPosition>>,
        moonPos: PlanetPosition?
    ): YogaAnalysisResult? {
        val benefics = setOf("jupiter", "venus", "mercury")
        val house10Planets = planetByHouse[10] ?: emptyList()
        val beneficIn10FromLagna = house10Planets.filter { it.planet.lowercase() in benefics && it.dignity != PlanetDignity.DEBILITATED }

        var isAmala = beneficIn10FromLagna.isNotEmpty()
        var supportingPlanet = beneficIn10FromLagna.firstOrNull()

        if (!isAmala && moonPos != null) {
            val house10FromMoonSign = (moonPos.signIndex + 9).mod(12)
            val planetIn10FromMoon = planetByHouse.values.flatten().firstOrNull { it.signIndex == house10FromMoonSign && it.planet.lowercase() in benefics && it.dignity != PlanetDignity.DEBILITATED }
            if (planetIn10FromMoon != null) {
                isAmala = true
                supportingPlanet = planetIn10FromMoon
            }
        }

        return YogaAnalysisResult(
            id = "amala_yoga",
            name = "Amala Yoga",
            sanskritName = "अमल योग",
            category = YogaCategory.AUSPICIOUS_COMBINATION,
            isDetected = isAmala,
            strength = if (isAmala) YogaStrength.STRONG else YogaStrength.INACTIVE,
            participatingPlanets = if (supportingPlanet != null) listOf(supportingPlanet.planet) else emptyList(),
            participatingHouses = if (supportingPlanet != null) listOf(supportingPlanet.house) else emptyList(),
            participatingSigns = if (supportingPlanet != null) listOf(supportingPlanet.sign) else emptyList(),
            evidence = if (isAmala) "Natural benefic ${supportingPlanet!!.planet} occupies 10th house (${supportingPlanet.sign})."
            else "No unblemished natural benefic occupies the 10th house from Lagna or Moon.",
            classicalRule = "Natural benefic (Guru, Shukra, Budha) occupying the 10th house from Lagna or Moon creates Amala Yoga, conferring unblemished career reputation.",
            calculationBasis = "Evaluated 10th house occupants for natural benefics.",
            positiveImpact = "Bestows spotless reputation, ethical career achievements, lasting philanthropy, and social honor."
        )
    }

    private fun evaluateSaraswatiYoga(planetByName: Map<String, PlanetPosition>): YogaAnalysisResult? {
        val jupiter = planetByName["jupiter"] ?: return null
        val venus = planetByName["venus"] ?: return null
        val mercury = planetByName["mercury"] ?: return null

        val auspiciousHouses = setOf(1, 2, 4, 5, 7, 9, 10)
        val allInAuspicious = jupiter.house in auspiciousHouses && venus.house in auspiciousHouses && mercury.house in auspiciousHouses
        val jupiterStrong = jupiter.dignity in setOf(PlanetDignity.EXALTED, PlanetDignity.OWN_SIGN, PlanetDignity.MOOLATRIKONA, PlanetDignity.FRIEND)

        val isSaraswati = allInAuspicious && jupiterStrong
        return YogaAnalysisResult(
            id = "saraswati_yoga",
            name = "Saraswati Yoga",
            sanskritName = "सरस्वती योग",
            category = YogaCategory.AUSPICIOUS_COMBINATION,
            isDetected = isSaraswati,
            strength = if (isSaraswati) YogaStrength.EXCELLENT else YogaStrength.INACTIVE,
            participatingPlanets = listOf("Jupiter", "Venus", "Mercury"),
            participatingHouses = if (isSaraswati) listOf(jupiter.house, venus.house, mercury.house).distinct() else emptyList(),
            participatingSigns = if (isSaraswati) listOf(jupiter.sign, venus.sign, mercury.sign).distinct() else emptyList(),
            evidence = if (isSaraswati) "Jupiter (House ${jupiter.house}), Venus (House ${venus.house}), and Mercury (House ${mercury.house}) occupy Kendra/Trikona/2nd houses with strong Jupiter (${jupiter.dignity.displayName})."
            else "Jupiter, Venus, and Mercury do not all occupy Kendra, Trikona, or 2nd houses.",
            classicalRule = "Jupiter, Venus, and Mercury in Kendra, Trikona, or 2nd house with strong Jupiter forms Saraswati Yoga, conferring goddess-like learning and wisdom.",
            calculationBasis = "Evaluated placements and dignities of Jupiter, Venus, and Mercury.",
            positiveImpact = "Confers encyclopedic learning, poetic elegance, musical/artistic mastery, oratory genius, and scholastic fame."
        )
    }

    private fun evaluateLakshmiYoga(
        houseLords: Map<Int, String>,
        planetByName: Map<String, PlanetPosition>,
        lagnaRashi: Rashi
    ): YogaAnalysisResult? {
        val lord9Name = houseLords[9]?.lowercase() ?: return null
        val lord9 = planetByName[lord9Name] ?: return null
        val lord1Name = houseLords[1]?.lowercase() ?: return null
        val lord1 = planetByName[lord1Name] ?: return null

        val isLord9InKendraOrTrikona = lord9.house in setOf(1, 4, 5, 7, 9, 10)
        val isLord9InOwnOrExalt = lord9.dignity in setOf(PlanetDignity.EXALTED, PlanetDignity.OWN_SIGN, PlanetDignity.MOOLATRIKONA)
        val isLord1Strong = lord1.dignity != PlanetDignity.DEBILITATED && lord1.house !in setOf(6, 8, 12)

        val isLakshmi = isLord9InKendraOrTrikona && isLord9InOwnOrExalt && isLord1Strong

        return YogaAnalysisResult(
            id = "lakshmi_yoga",
            name = "Lakshmi Yoga",
            sanskritName = "लक्ष्मी योग",
            category = YogaCategory.DHANA_YOGA,
            isDetected = isLakshmi,
            strength = if (isLakshmi) YogaStrength.EXCELLENT else YogaStrength.INACTIVE,
            participatingPlanets = listOf(lord9.planet, lord1.planet).distinct(),
            participatingHouses = if (isLakshmi) listOf(lord9.house, lord1.house).distinct() else emptyList(),
            participatingSigns = if (isLakshmi) listOf(lord9.sign, lord1.sign).distinct() else emptyList(),
            evidence = if (isLakshmi) "9th lord ${lord9.planet} is placed in House ${lord9.house} in ${lord9.sign} (${lord9.dignity.displayName}) and Lagna lord ${lord1.planet} is well-placed in House ${lord1.house}."
            else "9th lord is not in own/exaltation sign in Kendra/Trikona with strong Lagna lord.",
            classicalRule = "9th lord in own or exaltation sign in Kendra/Trikona with a strong Lagna lord creates Lakshmi Yoga.",
            calculationBasis = "Evaluated 9th lord (${lord9.planet}) and Lagna lord (${lord1.planet}) status.",
            positiveImpact = "Blesses with abundant fortune, continuous affluence, high virtue, noble lineage, and supreme comfort."
        )
    }

    private fun evaluateChandraYogas(
        planetByName: Map<String, PlanetPosition>,
        moonPos: PlanetPosition?
    ): YogaAnalysisResult? {
        val moon = moonPos ?: return null
        val ignoredPlanets = setOf("sun", "moon", "rahu", "ketu", "ascendant", "lagna")

        val secondSign = (moon.signIndex + 1).mod(12)
        val twelfthSign = (moon.signIndex - 1).mod(12)

        val planetsIn2nd = planetByName.values.filter { it.signIndex == secondSign && it.planet.lowercase() !in ignoredPlanets }
        val planetsIn12th = planetByName.values.filter { it.signIndex == twelfthSign && it.planet.lowercase() !in ignoredPlanets }

        val has2nd = planetsIn2nd.isNotEmpty()
        val has12th = planetsIn12th.isNotEmpty()

        return when {
            has2nd && has12th -> {
                YogaAnalysisResult(
                    id = "durudhara_yoga",
                    name = "Durudhara Yoga (Lunar)",
                    sanskritName = "दुरुधरा योग",
                    category = YogaCategory.CHANDRA_YOGA,
                    isDetected = true,
                    strength = YogaStrength.STRONG,
                    participatingPlanets = (listOf("Moon") + planetsIn2nd.map { it.planet } + planetsIn12th.map { it.planet }).distinct(),
                    participatingHouses = (listOf(moon.house) + planetsIn2nd.map { it.house } + planetsIn12th.map { it.house }).distinct(),
                    participatingSigns = listOf(Rashi.fromIndex(secondSign).sanskritName, Rashi.fromIndex(twelfthSign).sanskritName),
                    evidence = "Planets flank Moon on both sides: 2nd from Moon (${planetsIn2nd.joinToString { it.planet }}) and 12th from Moon (${planetsIn12th.joinToString { it.planet }}).",
                    classicalRule = "Planets (other than Sun/Rahu/Ketu) on both 2nd and 12th from Moon form Durudhara Yoga.",
                    calculationBasis = "Evaluated 2nd and 12th signs from Moon.",
                    positiveImpact = "Provides balance, material comforts, generosity, steady fortune, and versatile capabilities."
                )
            }
            has2nd -> {
                YogaAnalysisResult(
                    id = "sunafa_yoga",
                    name = "Sunafa Yoga (Lunar)",
                    sanskritName = "सुनफा योग",
                    category = YogaCategory.CHANDRA_YOGA,
                    isDetected = true,
                    strength = YogaStrength.MODERATE,
                    participatingPlanets = (listOf("Moon") + planetsIn2nd.map { it.planet }).distinct(),
                    participatingHouses = (listOf(moon.house) + planetsIn2nd.map { it.house }).distinct(),
                    participatingSigns = listOf(Rashi.fromIndex(secondSign).sanskritName),
                    evidence = "Planets in 2nd from Moon: ${planetsIn2nd.joinToString { it.planet }}.",
                    classicalRule = "Planets (other than Sun/Rahu/Ketu) in the 2nd from Moon form Sunafa Yoga.",
                    calculationBasis = "Evaluated 2nd sign from Moon.",
                    positiveImpact = "Bestows self-earned wealth, intellectual proficiency, and honorable livelihood."
                )
            }
            has12th -> {
                YogaAnalysisResult(
                    id = "anafa_yoga",
                    name = "Anafa Yoga (Lunar)",
                    sanskritName = "अनफा योग",
                    category = YogaCategory.CHANDRA_YOGA,
                    isDetected = true,
                    strength = YogaStrength.MODERATE,
                    participatingPlanets = (listOf("Moon") + planetsIn12th.map { it.planet }).distinct(),
                    participatingHouses = (listOf(moon.house) + planetsIn12th.map { it.house }).distinct(),
                    participatingSigns = listOf(Rashi.fromIndex(twelfthSign).sanskritName),
                    evidence = "Planets in 12th from Moon: ${planetsIn12th.joinToString { it.planet }}.",
                    classicalRule = "Planets (other than Sun/Rahu/Ketu) in the 12th from Moon form Anafa Yoga.",
                    calculationBasis = "Evaluated 12th sign from Moon.",
                    positiveImpact = "Endows attractive physique, moral rectitude, spiritual detachment, and peace of mind."
                )
            }
            else -> null
        }
    }

    private fun calculateMahapurushaStrength(planet: PlanetPosition): YogaStrength {
        return when (planet.dignity) {
            PlanetDignity.EXALTED -> YogaStrength.EXCELLENT
            PlanetDignity.OWN_SIGN, PlanetDignity.MOOLATRIKONA -> YogaStrength.STRONG
            else -> YogaStrength.MODERATE
        }
    }

    private fun getExaltationSignIndex(planet: String): Int? = when (planet.lowercase().trim()) {
        "sun", "surya" -> 0 // Aries
        "moon", "chandra" -> 1 // Taurus
        "mars", "mangala" -> 9 // Capricorn
        "mercury", "budha" -> 5 // Virgo
        "jupiter", "guru" -> 3 // Cancer
        "venus", "shukra" -> 11 // Pisces
        "saturn", "shani" -> 6 // Libra
        else -> null
    }

    private fun normalizePlanetName(name: String): String = when (name.lowercase().trim()) {
        "sun", "surya" -> "sun"
        "moon", "chandra" -> "moon"
        "mars", "mangala" -> "mars"
        "mercury", "budha" -> "mercury"
        "jupiter", "guru" -> "jupiter"
        "venus", "shukra" -> "venus"
        "saturn", "shani" -> "saturn"
        "rahu" -> "rahu"
        "ketu" -> "ketu"
        "ascendant", "lagna" -> "lagna"
        else -> name.lowercase().trim()
    }
}
