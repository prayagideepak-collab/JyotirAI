package com.example.domain.engine.yogadosha

import com.example.domain.models.*

/**
 * Deterministic Parashari Yoga Rule Engine.
 * Evaluates classical Vedic planetary combinations based on astronomical planetary positions,
 * Whole-Sign Bhava houses, sign lordships, and planetary dignities.
 */
object YogaRuleEngine {

    /**
     * Evaluates all supported classical Vedic Yogas for the provided profile.
     */
    fun evaluateAll(profile: AstrologyProfile): List<YogaAnalysisResult> {
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

        // 5. Parashari Kendra-Trikona Raja Yogas (including Dharma-Karmadhipati & Yogakarakas)
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

        return ResultValidator.sanitizeAndOrderYogas(results)
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

        val examinedPairs = mutableSetOf<Pair<Int, Int>>()
        for (k in kendras) {
            for (t in trikonas) {
                if (k == t) continue
                val pair = if (k < t) Pair(k, t) else Pair(t, k)
                if (examinedPairs.contains(pair)) continue
                examinedPairs.add(pair)

                val lord1Name = houseLords[pair.first]?.lowercase() ?: continue
                val lord2Name = houseLords[pair.second]?.lowercase() ?: continue

                // Yogakaraka (Same planet owning both Kendra and Trikona)
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
                                evidence = "${p.planet} owns Kendra House ${pair.first} and Trikona House ${pair.second} simultaneously as Yogakaraka in House ${p.house} (${p.sign}).",
                                classicalRule = "A single planet owning both a Kendra and Trikona house is a functional Yogakaraka, creating an independent Raja Yoga.",
                                calculationBasis = "Lagna ${lagnaRashi.englishName}: ${p.planet} owns Houses ${pair.first} & ${pair.second}.",
                                positiveImpact = "Grants effortless upward social mobility, royal patronage, career triumph, and auspicious opportunities."
                            )
                        )
                    }
                    continue
                }

                val p1 = planetByName[lord1Name]
                val p2 = planetByName[lord2Name]
                if (p1 == null || p2 == null) continue

                val isConjoined = p1.house == p2.house
                val isMutualAspect = ((p1.house - p2.house).mod(12)) == 6
                val isExchange = (p1.house == pair.second && p2.house == pair.first) || (p1.house == pair.first && p2.house == pair.second)

                if (isConjoined || isMutualAspect || isExchange) {
                    val isDharmaKarma = (pair.first == 10 && pair.second == 9) || (pair.first == 9 && pair.second == 10)
                    val yogaName = if (isDharmaKarma) "Dharma-Karmadhipati Raja Yoga (${p1.planet} & ${p2.planet})"
                    else "Kendra-Trikona Raja Yoga (${p1.planet} & ${p2.planet})"
                    val sanskritName = if (isDharmaKarma) "धर्म-कर्माधिपति राजयोग" else "केन्द्र-त्रिकोण राजयोग"

                    val relationType = when {
                        isExchange -> "Parivartana (Mutual Sign Exchange)"
                        isConjoined -> "Conjunction in House ${p1.house}"
                        else -> "Mutual 7th Aspect"
                    }

                    list.add(
                        YogaAnalysisResult(
                            id = "raja_yoga_${pair.first}_${pair.second}_${lord1Name}_${lord2Name}",
                            name = yogaName,
                            sanskritName = sanskritName,
                            category = YogaCategory.RAJA_YOGA,
                            isDetected = true,
                            strength = if (isDharmaKarma) YogaStrength.EXCELLENT else YogaStrength.STRONG,
                            participatingPlanets = listOf(p1.planet, p2.planet),
                            participatingHouses = listOf(p1.house, p2.house).distinct(),
                            participatingSigns = listOf(p1.sign, p2.sign).distinct(),
                            evidence = "${p1.planet} (Lord of House ${pair.first}) and ${p2.planet} (Lord of House ${pair.second}) form $relationType.",
                            classicalRule = "Association between a Kendra lord (1, 4, 7, 10) and a Trikona lord (1, 5, 9) produces Parashari Raja Yoga.",
                            calculationBasis = "Lord of ${pair.first} (${p1.planet}) + Lord of ${pair.second} (${p2.planet}) via $relationType.",
                            positiveImpact = "Confers eminent career success, political status, societal honor, high executive authority, and renown."
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
        val wealthHouses = listOf(2, 11)
        val fortuneHouses = listOf(1, 5, 9)

        for (w in wealthHouses) {
            val wLord = houseLords[w]?.lowercase() ?: continue
            val wp = planetByName[wLord] ?: continue

            for (f in fortuneHouses) {
                val fLord = houseLords[f]?.lowercase() ?: continue
                val fp = planetByName[fLord] ?: continue

                if (wLord == fLord) {
                    list.add(
                        YogaAnalysisResult(
                            id = "dhana_yoga_single_${w}_${f}_${wLord}",
                            name = "Dhana Yoga (Wealth - ${wp.planet})",
                            sanskritName = "धन योग (${wp.sanskritName})",
                            category = YogaCategory.DHANA_YOGA,
                            isDetected = true,
                            strength = YogaStrength.STRONG,
                            participatingPlanets = listOf(wp.planet),
                            participatingHouses = listOf(wp.house),
                            participatingSigns = listOf(wp.sign),
                            evidence = "${wp.planet} owns Wealth House $w and Fortune House $f simultaneously, placed in House ${wp.house}.",
                            classicalRule = "Planetary lordship connecting the 2nd/11th house of wealth with the 1st/5th/9th house of fortune creates Dhana Yoga.",
                            calculationBasis = "${wp.planet} rules Houses $w and $f.",
                            positiveImpact = "Accelerates wealth accumulation, prosperity, financial stability, and family abundance."
                        )
                    )
                    continue
                }

                val isConjoined = wp.house == fp.house
                val isExchange = wp.house == f && fp.house == w
                if (isConjoined || isExchange) {
                    val relation = if (isExchange) "Mutual Exchange (Parivartana)" else "Conjunction in House ${wp.house}"
                    list.add(
                        YogaAnalysisResult(
                            id = "dhana_yoga_${w}_${f}_${wLord}_${fLord}",
                            name = "Dhana Yoga (Wealth - ${wp.planet} & ${fp.planet})",
                            sanskritName = "धन योग (${wp.sanskritName}-${fp.sanskritName})",
                            category = YogaCategory.DHANA_YOGA,
                            isDetected = true,
                            strength = YogaStrength.STRONG,
                            participatingPlanets = listOf(wp.planet, fp.planet),
                            participatingHouses = listOf(wp.house, fp.house).distinct(),
                            participatingSigns = listOf(wp.sign, fp.sign).distinct(),
                            evidence = "${wp.planet} (Lord of $w) and ${fp.planet} (Lord of $f) form $relation.",
                            classicalRule = "Mutual sambandha between 2nd/11th lords and 1st/5th/9th lords produces extensive wealth and assets.",
                            calculationBasis = "Lord of $w (${wp.planet}) + Lord of $f (${fp.planet}) in $relation.",
                            positiveImpact = "Grants prosperous business ventures, investment gains, financial security, and liquid wealth."
                        )
                    )
                }
            }
        }
        return list
    }

    private fun evaluateViparitaRajaYogas(
        houseLords: Map<Int, String>,
        planetByName: Map<String, PlanetPosition>
    ): List<YogaAnalysisResult> {
        val list = mutableListOf<YogaAnalysisResult>()
        val dusthanaHouses = setOf(6, 8, 12)

        // 1. Harsha Yoga (6th lord in 6, 8, or 12)
        val lord6Name = houseLords[6]?.lowercase()
        val p6 = lord6Name?.let { planetByName[it] }
        if (p6 != null && p6.house in dusthanaHouses) {
            list.add(
                YogaAnalysisResult(
                    id = "viparita_harsha_yoga",
                    name = "Harsha Viparita Raja Yoga",
                    sanskritName = "हर्ष विपरीत राजयोग",
                    category = YogaCategory.VIPARITA_RAJA_YOGA,
                    isDetected = true,
                    strength = YogaStrength.STRONG,
                    participatingPlanets = listOf(p6.planet),
                    participatingHouses = listOf(p6.house),
                    participatingSigns = listOf(p6.sign),
                    evidence = "6th lord (${p6.planet}) is situated in Dusthana House ${p6.house} (${p6.sign}).",
                    classicalRule = "6th lord occupying the 6th, 8th, or 12th house forms Harsha Yoga, granting triumph over adversaries and illness.",
                    calculationBasis = "6th lord ${p6.planet} placed in house ${p6.house}.",
                    positiveImpact = "Guarantees victory over competitors, strong immunity, resilience in adversity, and sudden luck through obstacles."
                )
            )
        }

        // 2. Sarala Yoga (8th lord in 6, 8, or 12)
        val lord8Name = houseLords[8]?.lowercase()
        val p8 = lord8Name?.let { planetByName[it] }
        if (p8 != null && p8.house in dusthanaHouses) {
            list.add(
                YogaAnalysisResult(
                    id = "viparita_sarala_yoga",
                    name = "Sarala Viparita Raja Yoga",
                    sanskritName = "सरल विपरीत राजयोग",
                    category = YogaCategory.VIPARITA_RAJA_YOGA,
                    isDetected = true,
                    strength = YogaStrength.STRONG,
                    participatingPlanets = listOf(p8.planet),
                    participatingHouses = listOf(p8.house),
                    participatingSigns = listOf(p8.sign),
                    evidence = "8th lord (${p8.planet}) is situated in Dusthana House ${p8.house} (${p8.sign}).",
                    classicalRule = "8th lord occupying the 6th, 8th, or 12th house forms Sarala Yoga, conferring fearless nature and longevity.",
                    calculationBasis = "8th lord ${p8.planet} placed in house ${p8.house}.",
                    positiveImpact = "Endows courage, longevity, victory in disputes, academic insight, and unexpected fortune."
                )
            )
        }

        // 3. Vimala Yoga (12th lord in 6, 8, or 12)
        val lord12Name = houseLords[12]?.lowercase()
        val p12 = lord12Name?.let { planetByName[it] }
        if (p12 != null && p12.house in dusthanaHouses) {
            list.add(
                YogaAnalysisResult(
                    id = "viparita_vimala_yoga",
                    name = "Vimala Viparita Raja Yoga",
                    sanskritName = "विमल विपरीत राजयोग",
                    category = YogaCategory.VIPARITA_RAJA_YOGA,
                    isDetected = true,
                    strength = YogaStrength.STRONG,
                    participatingPlanets = listOf(p12.planet),
                    participatingHouses = listOf(p12.house),
                    participatingSigns = listOf(p12.sign),
                    evidence = "12th lord (${p12.planet}) is situated in Dusthana House ${p12.house} (${p12.sign}).",
                    classicalRule = "12th lord occupying the 6th, 8th, or 12th house forms Vimala Yoga, ensuring noble conduct and wealth accumulation.",
                    calculationBasis = "12th lord ${p12.planet} placed in house ${p12.house}.",
                    positiveImpact = "Minimizes unnecessary expenditure, bestows pious character, independent career standing, and inner contentment."
                )
            )
        }

        return list
    }

    private fun evaluateNeechaBhangaYogas(
        planets: List<PlanetPosition>,
        lagnaSignIndex: Int,
        moonSignIndex: Int
    ): List<YogaAnalysisResult> {
        val list = mutableListOf<YogaAnalysisResult>()
        val debilitatedPlanets = planets.filter { it.dignity == PlanetDignity.DEBILITATED }
        val kendraSignsFromLagna = setOf(lagnaSignIndex, (lagnaSignIndex + 3) % 12, (lagnaSignIndex + 6) % 12, (lagnaSignIndex + 9) % 12)
        val kendraSignsFromMoon = setOf(moonSignIndex, (moonSignIndex + 3) % 12, (moonSignIndex + 6) % 12, (moonSignIndex + 9) % 12)

        for (debPlanet in debilitatedPlanets) {
            val debSignRashi = Rashi.fromIndex(debPlanet.signIndex)
            val debLord = debSignRashi.lord.lowercase()
            val debDispositor = planets.firstOrNull { it.planet.equals(debLord, ignoreCase = true) }

            val cancellationReasons = mutableListOf<String>()

            // Cancellation 1: Dispositor in Kendra from Lagna or Moon
            if (debDispositor != null) {
                if (debDispositor.signIndex in kendraSignsFromLagna) {
                    cancellationReasons.add("Dispositor ${debDispositor.planet} is in Kendra from Lagna (House ${debDispositor.house})")
                }
                if (debDispositor.signIndex in kendraSignsFromMoon) {
                    cancellationReasons.add("Dispositor ${debDispositor.planet} is in Kendra from Moon")
                }
                if (debDispositor.dignity == PlanetDignity.EXALTED) {
                    cancellationReasons.add("Dispositor ${debDispositor.planet} is exalted in ${debDispositor.sign}")
                }
            }

            if (cancellationReasons.isNotEmpty()) {
                list.add(
                    YogaAnalysisResult(
                        id = "neecha_bhanga_${debPlanet.planet.lowercase()}",
                        name = "Neecha Bhanga Raja Yoga (${debPlanet.planet})",
                        sanskritName = "नीचभंग राजयोग (${debPlanet.sanskritName})",
                        category = YogaCategory.NEECHA_BHANGA_RAJA_YOGA,
                        isDetected = true,
                        strength = YogaStrength.STRONG,
                        participatingPlanets = listOfNotNull(debPlanet.planet, debDispositor?.planet),
                        participatingHouses = listOfNotNull(debPlanet.house, debDispositor?.house).distinct(),
                        participatingSigns = listOfNotNull(debPlanet.sign, debDispositor?.sign).distinct(),
                        evidence = "${debPlanet.planet} is debilitated in ${debPlanet.sign}, but debilitation is cancelled because: ${cancellationReasons.joinToString("; ")}.",
                        classicalRule = "When a debilitated planet's dispositor is in a Kendra from Lagna or Moon, the debilitation cancels, forming Neecha Bhanga Raja Yoga.",
                        calculationBasis = "Evaluated ${debPlanet.planet} in sign ${debPlanet.signIndex} and dispositor ${debLord}.",
                        positiveImpact = "Transforms initial career struggles into monumental long-term breakthroughs, resilience, and high authority."
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
        val naturalBenefics = setOf("jupiter", "venus", "mercury")
        val house10Planets = planetByHouse[10] ?: emptyList()
        val beneficIn10th = house10Planets.filter { it.planet.lowercase() in naturalBenefics }

        val isDetected = beneficIn10th.isNotEmpty()
        return YogaAnalysisResult(
            id = "amala_yoga",
            name = "Amala Yoga",
            sanskritName = "अमला योग",
            category = YogaCategory.AUSPICIOUS_COMBINATION,
            isDetected = isDetected,
            strength = if (isDetected) YogaStrength.STRONG else YogaStrength.INACTIVE,
            participatingPlanets = beneficIn10th.map { it.planet },
            participatingHouses = if (isDetected) listOf(10) else emptyList(),
            participatingSigns = beneficIn10th.map { it.sign }.distinct(),
            evidence = if (isDetected) "Natural benefic (${beneficIn10th.joinToString { it.planet }}) occupies the 10th house of profession without blemish."
            else "No natural benefic in the 10th house.",
            classicalRule = "An unblemished natural benefic planet (Jupiter, Venus, or Mercury) in the 10th house from Lagna forms Amala Yoga.",
            calculationBasis = "Evaluated 10th house occupants.",
            positiveImpact = "Confers spotless reputation, ethical career achievements, enduring public esteem, and lasting prosperity."
        )
    }

    private fun evaluateSaraswatiYoga(planetByName: Map<String, PlanetPosition>): YogaAnalysisResult? {
        val jupiter = planetByName["jupiter"] ?: return null
        val venus = planetByName["venus"] ?: return null
        val mercury = planetByName["mercury"] ?: return null

        val kendraTrikona2 = setOf(1, 2, 4, 5, 7, 9, 10)
        val isJupValid = jupiter.house in kendraTrikona2 && jupiter.dignity != PlanetDignity.DEBILITATED
        val isVenValid = venus.house in kendraTrikona2
        val isMercValid = mercury.house in kendraTrikona2

        val isDetected = isJupValid && isVenValid && isMercValid
        return YogaAnalysisResult(
            id = "saraswati_yoga",
            name = "Saraswati Yoga",
            sanskritName = "सरस्वती योग",
            category = YogaCategory.AUSPICIOUS_COMBINATION,
            isDetected = isDetected,
            strength = if (isDetected) YogaStrength.EXCELLENT else YogaStrength.INACTIVE,
            participatingPlanets = listOf("Jupiter", "Venus", "Mercury"),
            participatingHouses = if (isDetected) listOf(jupiter.house, venus.house, mercury.house).distinct() else emptyList(),
            participatingSigns = if (isDetected) listOf(jupiter.sign, venus.sign, mercury.sign).distinct() else emptyList(),
            evidence = if (isDetected) "Jupiter (House ${jupiter.house}), Venus (House ${venus.house}), and Mercury (House ${mercury.house}) occupy Kendra/Trikona/2nd houses in strength."
            else "All three wisdom planets (Jupiter, Venus, Mercury) are not placed in Kendra/Trikona/2nd houses simultaneously.",
            classicalRule = "Jupiter, Venus, and Mercury occupying Kendra, Trikona, or 2nd house with Jupiter in strength creates Saraswati Yoga.",
            calculationBasis = "Evaluated Jupiter (${jupiter.house}), Venus (${venus.house}), Mercury (${mercury.house}).",
            positiveImpact = "Bestows supreme literary talent, poetic genius, oratorical mastery, academic distinction, and artistic recognition."
        )
    }

    private fun evaluateLakshmiYoga(
        houseLords: Map<Int, String>,
        planetByName: Map<String, PlanetPosition>,
        lagnaRashi: Rashi
    ): YogaAnalysisResult? {
        val lord9Name = houseLords[9]?.lowercase() ?: return null
        val lord9 = planetByName[lord9Name] ?: return null
        val lagnaLordName = lagnaRashi.lord.lowercase()
        val lagnaLord = planetByName[lagnaLordName] ?: return null

        val is9thStrong = lord9.dignity in setOf(PlanetDignity.EXALTED, PlanetDignity.OWN_SIGN, PlanetDignity.MOOLATRIKONA)
        val is9thInKendraTrikona = lord9.house in setOf(1, 4, 5, 7, 9, 10)
        val isLagnaLordStrong = lagnaLord.dignity != PlanetDignity.DEBILITATED

        val isDetected = is9thStrong && is9thInKendraTrikona && isLagnaLordStrong
        return YogaAnalysisResult(
            id = "lakshmi_yoga",
            name = "Lakshmi Yoga",
            sanskritName = "लक्ष्मी योग",
            category = YogaCategory.DHANA_YOGA,
            isDetected = isDetected,
            strength = if (isDetected) YogaStrength.EXCELLENT else YogaStrength.INACTIVE,
            participatingPlanets = listOf(lord9.planet, lagnaLord.planet).distinct(),
            participatingHouses = if (isDetected) listOf(lord9.house, lagnaLord.house).distinct() else emptyList(),
            participatingSigns = if (isDetected) listOf(lord9.sign, lagnaLord.sign).distinct() else emptyList(),
            evidence = if (isDetected) "9th lord (${lord9.planet}) is strong in ${lord9.sign} (House ${lord9.house}) and Lagna lord (${lagnaLord.planet}) is well-disposed."
            else "9th lord is not in own/exaltation sign in Kendra/Trikona.",
            classicalRule = "9th lord in own or exaltation sign in Kendra or Trikona while Lagna lord is strong forms Lakshmi Yoga.",
            calculationBasis = "Evaluated 9th lord ${lord9.planet} and Lagna lord ${lagnaLord.planet}.",
            positiveImpact = "Blesses native with abundant fortune, high nobility, beauty, grace, multi-source wealth, and benevolent character."
        )
    }

    private fun evaluateChandraYogas(
        planetByName: Map<String, PlanetPosition>,
        moonPos: PlanetPosition?
    ): YogaAnalysisResult? {
        if (moonPos == null) return null
        val excludedPlanets = setOf("sun", "rahu", "ketu", "moon")
        val otherPlanets = planetByName.filter { it.key !in excludedPlanets }.values

        val house2FromMoon = ((moonPos.signIndex + 1) % 12)
        val house12FromMoon = ((moonPos.signIndex + 11) % 12)

        val planetsIn2nd = otherPlanets.filter { it.signIndex == house2FromMoon }
        val planetsIn12th = otherPlanets.filter { it.signIndex == house12FromMoon }

        return when {
            planetsIn2nd.isNotEmpty() && planetsIn12th.isNotEmpty() -> {
                YogaAnalysisResult(
                    id = "durudhara_yoga",
                    name = "Durudhara Yoga (Lunar)",
                    sanskritName = "दुरुधरा योग",
                    category = YogaCategory.CHANDRA_YOGA,
                    isDetected = true,
                    strength = YogaStrength.STRONG,
                    participatingPlanets = (planetsIn2nd + planetsIn12th).map { it.planet },
                    participatingHouses = listOf(moonPos.house),
                    participatingSigns = listOf(Rashi.fromIndex(house2FromMoon).englishName, Rashi.fromIndex(house12FromMoon).englishName),
                    evidence = "Planets exist in both 2nd (${planetsIn2nd.joinToString { it.planet }}) and 12th (${planetsIn12th.joinToString { it.planet }}) from Moon.",
                    classicalRule = "Planets other than Sun in both 2nd and 12th from Moon form Durudhara Yoga.",
                    calculationBasis = "Planets in 2nd and 12th from Moon (${moonPos.sign}).",
                    positiveImpact = "Endows generous spirit, wealth, vehicles, loyal friends, and balanced temperament."
                )
            }
            planetsIn2nd.isNotEmpty() -> {
                YogaAnalysisResult(
                    id = "sunafa_yoga",
                    name = "Sunafa Yoga (Lunar)",
                    sanskritName = "सुनफा योग",
                    category = YogaCategory.CHANDRA_YOGA,
                    isDetected = true,
                    strength = YogaStrength.MODERATE,
                    participatingPlanets = planetsIn2nd.map { it.planet },
                    participatingHouses = listOf(moonPos.house),
                    participatingSigns = listOf(Rashi.fromIndex(house2FromMoon).englishName),
                    evidence = "Planets (${planetsIn2nd.joinToString { it.planet }}) occupy the 2nd house from Moon.",
                    classicalRule = "Planets other than Sun in the 2nd house from Moon form Sunafa Yoga.",
                    calculationBasis = "Planets in 2nd from Moon.",
                    positiveImpact = "Confers self-earned wealth, intellectual abilities, peace of mind, and good fortune."
                )
            }
            planetsIn12th.isNotEmpty() -> {
                YogaAnalysisResult(
                    id = "anafa_yoga",
                    name = "Anafa Yoga (Lunar)",
                    sanskritName = "अनफा योग",
                    category = YogaCategory.CHANDRA_YOGA,
                    isDetected = true,
                    strength = YogaStrength.MODERATE,
                    participatingPlanets = planetsIn12th.map { it.planet },
                    participatingHouses = listOf(moonPos.house),
                    participatingSigns = listOf(Rashi.fromIndex(house12FromMoon).englishName),
                    evidence = "Planets (${planetsIn12th.joinToString { it.planet }}) occupy the 12th house from Moon.",
                    classicalRule = "Planets other than Sun in the 12th house from Moon form Anafa Yoga.",
                    calculationBasis = "Planets in 12th from Moon.",
                    positiveImpact = "Blesses with magnetic personality, generous nature, good health, and freedom from grief."
                )
            }
            else -> null
        }
    }

    private fun calculateMahapurushaStrength(planet: PlanetPosition): YogaStrength {
        return when (planet.dignity) {
            PlanetDignity.EXALTED -> YogaStrength.EXCELLENT
            PlanetDignity.OWN_SIGN, PlanetDignity.MOOLATRIKONA -> YogaStrength.STRONG
            PlanetDignity.FRIEND -> YogaStrength.MODERATE
            else -> YogaStrength.MILD
        }
    }

    private fun normalizePlanetName(name: String): String = when (name.lowercase().trim()) {
        "sun", "surya" -> "sun"
        "moon", "chandra" -> "moon"
        "mars", "mangal" -> "mars"
        "mercury", "budh", "budha" -> "mercury"
        "jupiter", "guru", "brihaspati" -> "jupiter"
        "venus", "shukra" -> "venus"
        "saturn", "shani" -> "saturn"
        "rahu" -> "rahu"
        "ketu" -> "ketu"
        else -> name.lowercase().trim()
    }
}
