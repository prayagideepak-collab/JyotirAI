package com.example.domain.engine.yogadosha

import com.example.domain.models.*

/**
 * Deterministic Parashari Dosha Rule Engine.
 * Evaluates classical Vedic planetary doshas, afflictions, and classical cancellation (Bhanga) conditions.
 */
object DoshaRuleEngine {

    /**
     * Evaluates all supported classical Vedic Doshas for the provided profile.
     */
    fun evaluateAll(profile: AstrologyProfile): List<DoshaAnalysisResult> {
        val planets = profile.planetPositions
        val lagnaSignIndex = profile.lagnaSignIndex
        val moonPos = planets.firstOrNull { it.planet.equals("moon", ignoreCase = true) }
        val venusPos = planets.firstOrNull { it.planet.equals("venus", ignoreCase = true) }

        val planetByName = mutableMapOf<String, PlanetPosition>()
        val planetByHouse = mutableMapOf<Int, MutableList<PlanetPosition>>()
        planets.forEach { p ->
            val normPlanet = normalizePlanetName(p.planet)
            planetByName[normPlanet] = p
            planetByHouse.getOrPut(p.house) { mutableListOf() }.add(p)
        }

        val results = mutableListOf<DoshaAnalysisResult>()

        // 1. Manglik / Kuja Dosha (evaluated from Lagna, Moon, Venus)
        evaluateManglikDosha(planetByName, lagnaSignIndex, moonPos, venusPos)?.let { results.add(it) }

        // 2. Kaal Sarp Dosha (12 Variations: Anant to Sheshnag, Purna vs Khandit)
        evaluateKaalSarpDosha(planets, lagnaSignIndex)?.let { results.add(it) }

        // 3. Kemadruma Dosha & Kemadruma Bhanga (Cancellation)
        evaluateKemadrumaDosha(planetByName, moonPos, lagnaSignIndex)?.let { results.add(it) }

        // 4. Guru Chandal Dosha (Jupiter afflicted by Rahu/Ketu)
        evaluateGuruChandalDosha(planetByName)?.let { results.add(it) }

        // 5. Pitra Dosha (Sun / 9th Lord afflicted by Rahu / Saturn)
        evaluatePitraDosha(planetByName, lagnaSignIndex)?.let { results.add(it) }

        // 6. Shrapit Dosha (Saturn-Rahu Conjunction)
        evaluateShrapitDosha(planetByName)?.let { results.add(it) }

        // 7. Gandmanta / Gandmool Dosha (Moon in Nakshatra Sandhi)
        evaluateGandmantaDosha(moonPos)?.let { results.add(it) }

        return ResultValidator.sanitizeAndOrderDoshas(results)
    }

    private fun evaluateManglikDosha(
        planetByName: Map<String, PlanetPosition>,
        lagnaSignIndex: Int,
        moonPos: PlanetPosition?,
        venusPos: PlanetPosition?
    ): DoshaAnalysisResult? {
        val mars = planetByName["mars"] ?: return null
        val manglikHouses = setOf(1, 2, 4, 7, 8, 12)

        val isFromLagna = mars.house in manglikHouses
        val houseFromMoon = moonPos?.let { ((mars.signIndex - it.signIndex).mod(12)) + 1 } ?: 0
        val isFromMoon = houseFromMoon in manglikHouses

        val houseFromVenus = venusPos?.let { ((mars.signIndex - it.signIndex).mod(12)) + 1 } ?: 0
        val isFromVenus = houseFromVenus in manglikHouses

        val isDetected = isFromLagna || isFromMoon || isFromVenus
        if (!isDetected) {
            return DoshaAnalysisResult(
                id = "manglik_dosha",
                name = "Manglik / Kuja Dosha",
                sanskritName = "मांगलिक / कुज दोष",
                category = DoshaCategory.MANGLIK,
                status = AnalysisStatus.NOT_DETECTED,
                severity = DoshaSeverity.NONE,
                isCancelled = false,
                cancellationReason = null,
                participatingPlanets = listOf("Mars"),
                participatingHouses = listOf(mars.house),
                participatingSigns = listOf(mars.sign),
                evidence = "Mars is placed in House ${mars.house} (${mars.sign}), which is not a Kuja Dosha house (1, 2, 4, 7, 8, 12) from Lagna, Moon, or Venus.",
                classicalRule = "Mars placed in 1st, 2nd, 4th, 7th, 8th, or 12th house causes Kuja Dosha.",
                remedialGuidance = emptyList(),
                calculationBasis = "Mars in house ${mars.house}, house from Moon: $houseFromMoon, house from Venus: $houseFromVenus."
            )
        }

        // Classical Manglik Cancellations (BPHS & Muhurta Chintamani)
        val cancellations = mutableListOf<String>()

        // 1. Mars in own sign Aries (House 1) or Scorpio (House 8) or exalted Capricorn (House 7)
        if (mars.house == 1 && mars.signIndex == 0) cancellations.add("Mars in Aries in 1st house (Own Sign)")
        if (mars.house == 4 && mars.signIndex == 7) cancellations.add("Mars in Scorpio in 4th house (Own Sign)")
        if (mars.house == 7 && mars.signIndex == 9) cancellations.add("Mars in Capricorn in 7th house (Exalted)")
        if (mars.house == 8 && (mars.signIndex == 3 || mars.signIndex == 10)) cancellations.add("Mars in Cancer/Aquarius in 8th house")
        if (mars.house == 2 && (mars.signIndex == 2 || mars.signIndex == 5)) cancellations.add("Mars in Gemini/Virgo in 2nd house")
        if (mars.house == 12 && (mars.signIndex == 1 || mars.signIndex == 6)) cancellations.add("Mars in Taurus/Libra in 12th house")

        // 2. Jupiter aspecting Mars or conjoined with Mars/Moon
        val jupiter = planetByName["jupiter"]
        if (jupiter != null) {
            val distJupToMars = (mars.signIndex - jupiter.signIndex).mod(12)
            if (distJupToMars == 0 || distJupToMars == 4 || distJupToMars == 6 || distJupToMars == 8) {
                cancellations.add("Benevolent Jupiter aspects or conjoins Mars (${distJupToMars + 1}th aspect)")
            }
        }

        // 3. Moon in Kendra with Mars (Chandra-Mangala)
        if (moonPos != null && (moonPos.house == mars.house || ((mars.house - moonPos.house).mod(12) in setOf(3, 6, 9)))) {
            cancellations.add("Moon is placed in Kendra with Mars")
        }

        val isCancelled = cancellations.isNotEmpty()
        val severity = when {
            isCancelled -> DoshaSeverity.CANCELLED
            isFromLagna && isFromMoon -> DoshaSeverity.HIGH
            isFromLagna -> DoshaSeverity.MODERATE
            else -> DoshaSeverity.LOW
        }

        val remedies = if (!isCancelled) listOf(
            "Recite Hanuman Chalisa daily or Sunderkand on Tuesdays.",
            "Offer red lentils (Masoor Dal) or jaggery on Tuesday mornings.",
            "Maintain patience and open communication in relationship matters."
        ) else listOf(
            "Dosha is cancelled by classical Parashari planetary positions."
        )

        return DoshaAnalysisResult(
            id = "manglik_dosha",
            name = "Manglik / Kuja Dosha",
            sanskritName = "मांगलिक / कुज दोष",
            category = DoshaCategory.MANGLIK,
            status = AnalysisStatus.DETECTED,
            severity = severity,
            isCancelled = isCancelled,
            cancellationReason = if (isCancelled) cancellations.joinToString("; ") else null,
            participatingPlanets = listOf("Mars"),
            participatingHouses = listOf(mars.house),
            participatingSigns = listOf(mars.sign),
            evidence = if (isCancelled) "Mars is in House ${mars.house} (${mars.sign}), but classical cancellation applies: ${cancellations.joinToString("; ")}."
            else "Mars is in House ${mars.house} (${mars.sign}) creating Kuja Dosha from Lagna.",
            classicalRule = "Mars in 1st, 2nd, 4th, 7th, 8th, or 12th house causes Kuja Dosha, subject to canonical cancellations.",
            remedialGuidance = remedies,
            calculationBasis = "Mars house: ${mars.house}, Moon relative: $houseFromMoon, Venus relative: $houseFromVenus."
        )
    }

    private fun evaluateKaalSarpDosha(planets: List<PlanetPosition>, lagnaSignIndex: Int): DoshaAnalysisResult? {
        val rahu = planets.firstOrNull { it.planet.equals("rahu", ignoreCase = true) } ?: return null
        val ketu = planets.firstOrNull { it.planet.equals("ketu", ignoreCase = true) } ?: return null

        val otherPlanets = planets.filter { !it.planet.equals("rahu", ignoreCase = true) && !it.planet.equals("ketu", ignoreCase = true) }
        if (otherPlanets.size < 7) return null

        val rahuSign = rahu.signIndex
        val ketuSign = ketu.signIndex

        var allOnOneSide = true
        var allOnOtherSide = true

        for (p in otherPlanets) {
            val s = p.signIndex
            val distFromRahu = (s - rahuSign).mod(12)
            if (distFromRahu > 6) allOnOneSide = false
            val distFromKetu = (s - ketuSign).mod(12)
            if (distFromKetu > 6) allOnOtherSide = false
        }

        val isKaalSarp = allOnOneSide || allOnOtherSide
        val isPurna = isKaalSarp

        val kaalSarpNames = listOf(
            "Anant Kaal Sarp (1st-7th House)",
            "Kulik Kaal Sarp (2nd-8th House)",
            "Vasuki Kaal Sarp (3rd-9th House)",
            "Shankhpal Kaal Sarp (4th-10th House)",
            "Padma Kaal Sarp (5th-11th House)",
            "Mahapadma Kaal Sarp (6th-12th House)",
            "Takshak Kaal Sarp (7th-1st House)",
            "Karkotak Kaal Sarp (8th-2nd House)",
            "Shankhachood Kaal Sarp (9th-3rd House)",
            "Ghatak Kaal Sarp (10th-4th House)",
            "Vishdhar Kaal Sarp (11th-5th House)",
            "Sheshnag Kaal Sarp (12th-6th House)"
        )
        val typeIndex = (rahu.house - 1).coerceIn(0, 11)
        val typeName = kaalSarpNames[typeIndex]

        if (!isKaalSarp) {
            return DoshaAnalysisResult(
                id = "kaal_sarp_dosha",
                name = "Kaal Sarp Dosha",
                sanskritName = "कालसर्प दोष",
                category = DoshaCategory.KAAL_SARP,
                status = AnalysisStatus.NOT_DETECTED,
                severity = DoshaSeverity.NONE,
                isCancelled = false,
                cancellationReason = null,
                participatingPlanets = listOf("Rahu", "Ketu"),
                participatingHouses = listOf(rahu.house, ketu.house),
                participatingSigns = listOf(rahu.sign, ketu.sign),
                evidence = "Planets are distributed on both sides of the Rahu-Ketu nodal axis (No Kaal Sarp Dosha).",
                classicalRule = "When all 7 physical planets are hemmed between Rahu and Ketu, Kaal Sarp Dosha is formed.",
                remedialGuidance = emptyList(),
                calculationBasis = "Rahu in sign ${rahu.signIndex}, Ketu in sign ${ketu.signIndex}."
            )
        }

        return DoshaAnalysisResult(
            id = "kaal_sarp_dosha_${typeIndex + 1}",
            name = typeName,
            sanskritName = "कालसर्प दोष (${typeName.substringBefore(" ")})",
            category = DoshaCategory.KAAL_SARP,
            status = AnalysisStatus.DETECTED,
            severity = if (isPurna) DoshaSeverity.HIGH else DoshaSeverity.MODERATE,
            isCancelled = false,
            cancellationReason = null,
            participatingPlanets = listOf("Rahu", "Ketu") + otherPlanets.map { it.planet },
            participatingHouses = listOf(rahu.house, ketu.house),
            participatingSigns = listOf(rahu.sign, ketu.sign),
            evidence = "All 7 planets are bounded between Rahu (House ${rahu.house}) and Ketu (House ${ketu.house}), forming $typeName.",
            classicalRule = "Enclosure of all seven planets between Rahu and Ketu constitutes Kaal Sarp Yoga.",
            remedialGuidance = listOf(
                "Chant Maha Mrityunjaya Mantra or Om Namah Shivaya regularly.",
                "Perform Jalabhisheka on Shivling on Mondays or Pradosh Vrat.",
                "Feed birds and practice altruism on Saturday/Rahu Kalam."
            ),
            calculationBasis = "Rahu at house ${rahu.house} (${rahu.sign}), Ketu at house ${ketu.house} (${ketu.sign})."
        )
    }

    private fun evaluateKemadrumaDosha(
        planetByName: Map<String, PlanetPosition>,
        moonPos: PlanetPosition?,
        lagnaSignIndex: Int
    ): DoshaAnalysisResult? {
        val moon = moonPos ?: return null
        val excludedPlanets = setOf("sun", "rahu", "ketu", "moon")
        val activePlanets = planetByName.filter { it.key !in excludedPlanets }.values

        val sign2ndFromMoon = (moon.signIndex + 1) % 12
        val sign12thFromMoon = (moon.signIndex + 11) % 12

        val has2ndPlanet = activePlanets.any { it.signIndex == sign2ndFromMoon }
        val has12thPlanet = activePlanets.any { it.signIndex == sign12thFromMoon }

        val isKemadruma = !has2ndPlanet && !has12thPlanet
        if (!isKemadruma) {
            return DoshaAnalysisResult(
                id = "kemadruma_dosha",
                name = "Kemadruma Dosha",
                sanskritName = "केमद्रुम दोष",
                category = DoshaCategory.KEMADRUMA,
                status = AnalysisStatus.NOT_DETECTED,
                severity = DoshaSeverity.NONE,
                isCancelled = false,
                cancellationReason = null,
                participatingPlanets = listOf("Moon"),
                participatingHouses = listOf(moon.house),
                participatingSigns = listOf(moon.sign),
                evidence = "Moon has supporting planets in either the 2nd or 12th house (No Kemadruma Dosha).",
                classicalRule = "No planet other than Sun, Rahu, or Ketu in the 2nd and 12th house from Moon produces Kemadruma Dosha.",
                remedialGuidance = emptyList(),
                calculationBasis = "Evaluated 2nd ($sign2ndFromMoon) and 12th ($sign12thFromMoon) from Moon ($moon)."
            )
        }

        // Classical Kemadruma Bhanga (Cancellation) rules
        val cancellations = mutableListOf<String>()

        // 1. Planets in Kendra from Lagna (Houses 1, 4, 7, 10)
        val kendraFromLagnaPlanets = activePlanets.filter { it.house in setOf(1, 4, 7, 10) }
        if (kendraFromLagnaPlanets.isNotEmpty()) {
            cancellations.add("Planets (${kendraFromLagnaPlanets.joinToString { it.planet }}) occupy Kendra from Lagna")
        }

        // 2. Planets in Kendra from Moon
        val kendraSignsFromMoon = setOf(moon.signIndex, (moon.signIndex + 3) % 12, (moon.signIndex + 6) % 12, (moon.signIndex + 9) % 12)
        val kendraFromMoonPlanets = activePlanets.filter { it.signIndex in kendraSignsFromMoon }
        if (kendraFromMoonPlanets.isNotEmpty()) {
            cancellations.add("Planets (${kendraFromMoonPlanets.joinToString { it.planet }}) occupy Kendra from Moon")
        }

        // 3. Moon aspected by Jupiter
        val jupiter = planetByName["jupiter"]
        if (jupiter != null) {
            val distJupToMoon = (moon.signIndex - jupiter.signIndex).mod(12)
            if (distJupToMoon == 0 || distJupToMoon == 4 || distJupToMoon == 6 || distJupToMoon == 8) {
                cancellations.add("Jupiter casts a benevolent aspect on the Moon")
            }
        }

        val isCancelled = cancellations.isNotEmpty()
        return DoshaAnalysisResult(
            id = "kemadruma_dosha",
            name = "Kemadruma Dosha",
            sanskritName = "केमद्रुम दोष",
            category = DoshaCategory.KEMADRUMA,
            status = AnalysisStatus.DETECTED,
            severity = if (isCancelled) DoshaSeverity.CANCELLED else DoshaSeverity.MODERATE,
            isCancelled = isCancelled,
            cancellationReason = if (isCancelled) cancellations.joinToString("; ") else null,
            participatingPlanets = listOf("Moon"),
            participatingHouses = listOf(moon.house),
            participatingSigns = listOf(moon.sign),
            evidence = if (isCancelled) "2nd and 12th from Moon are empty, but Kemadruma Bhanga applies: ${cancellations.joinToString("; ")}."
            else "Moon in ${moon.sign} has no supporting planets in 2nd or 12th houses.",
            classicalRule = "Kemadruma occurs when Moon is isolated, but cancels if planets occupy Kendra from Lagna/Moon or Jupiter aspects Moon.",
            remedialGuidance = if (!isCancelled) listOf(
                "Worship Lord Shiva with milk offering on Mondays.",
                "Recite Chandra Beej Mantra: Om Shram Shreem Shrom Sah Chandramase Namah.",
                "Respect mother and elderly maternal figures."
            ) else listOf("Dosha is nullified by Kemadruma Bhanga auspicious planetary alignments."),
            calculationBasis = "Evaluated 2nd and 12th house from Moon with Kendra cancellations."
        )
    }

    private fun evaluateGuruChandalDosha(planetByName: Map<String, PlanetPosition>): DoshaAnalysisResult? {
        val jupiter = planetByName["jupiter"] ?: return null
        val rahu = planetByName["rahu"]
        val ketu = planetByName["ketu"]

        val isWithRahu = rahu != null && jupiter.signIndex == rahu.signIndex
        val isWithKetu = ketu != null && jupiter.signIndex == ketu.signIndex

        val isDetected = isWithRahu || isWithKetu
        val nodePlanet = if (isWithRahu) "Rahu" else if (isWithKetu) "Ketu" else ""

        if (!isDetected) {
            return DoshaAnalysisResult(
                id = "guru_chandal_dosha",
                name = "Guru Chandal Dosha",
                sanskritName = "गुरु चांडाल दोष",
                category = DoshaCategory.PLANETARY_AFFLICTION,
                status = AnalysisStatus.NOT_DETECTED,
                severity = DoshaSeverity.NONE,
                isCancelled = false,
                cancellationReason = null,
                participatingPlanets = listOf("Jupiter"),
                participatingHouses = listOf(jupiter.house),
                participatingSigns = listOf(jupiter.sign),
                evidence = "Jupiter is free from conjunction with Rahu or Ketu.",
                classicalRule = "Conjunction of Jupiter with Rahu or Ketu forms Guru Chandal Dosha.",
                remedialGuidance = emptyList(),
                calculationBasis = "Jupiter sign (${jupiter.signIndex}) vs Rahu (${rahu?.signIndex}) / Ketu (${ketu?.signIndex})."
            )
        }

        return DoshaAnalysisResult(
            id = "guru_chandal_dosha",
            name = "Guru Chandal Dosha",
            sanskritName = "गुरु चांडाल दोष",
            category = DoshaCategory.PLANETARY_AFFLICTION,
            status = AnalysisStatus.DETECTED,
            severity = DoshaSeverity.MODERATE,
            isCancelled = false,
            cancellationReason = null,
            participatingPlanets = listOf("Jupiter", nodePlanet),
            participatingHouses = listOf(jupiter.house),
            participatingSigns = listOf(jupiter.sign),
            evidence = "Jupiter and $nodePlanet are conjoined in House ${jupiter.house} (${jupiter.sign}).",
            classicalRule = "Jupiter conjoined with lunar node Rahu/Ketu creates Guru Chandal Dosha.",
            remedialGuidance = listOf(
                "Apply saffron/turmeric tilak on forehead daily.",
                "Offer yellow flowers and gram dal to Vishnu on Thursdays.",
                "Respect teachers, gurus, and spiritual preceptors."
            ),
            calculationBasis = "Jupiter and $nodePlanet occupy the same sign (${jupiter.sign})."
        )
    }

    private fun evaluatePitraDosha(
        planetByName: Map<String, PlanetPosition>,
        lagnaSignIndex: Int
    ): DoshaAnalysisResult? {
        val sun = planetByName["sun"] ?: return null
        val rahu = planetByName["rahu"]
        val saturn = planetByName["saturn"]

        val isSunWithRahu = rahu != null && sun.signIndex == rahu.signIndex
        val isSunWithSaturn = saturn != null && sun.signIndex == saturn.signIndex
        val is9thAfflicted = sun.house == 9 && (isSunWithRahu || isSunWithSaturn)

        val isDetected = isSunWithRahu || (sun.house == 9 && isSunWithSaturn)
        if (!isDetected) {
            return DoshaAnalysisResult(
                id = "pitra_dosha",
                name = "Pitra Dosha",
                sanskritName = "पितृ दोष",
                category = DoshaCategory.PLANETARY_AFFLICTION,
                status = AnalysisStatus.NOT_DETECTED,
                severity = DoshaSeverity.NONE,
                isCancelled = false,
                cancellationReason = null,
                participatingPlanets = listOf("Sun"),
                participatingHouses = listOf(sun.house),
                participatingSigns = listOf(sun.sign),
                evidence = "Sun and the 9th house of dharma are unblemished by nodal affliction.",
                classicalRule = "Affliction of the Sun or 9th house by Rahu/Saturn indicates Pitra Dosha.",
                remedialGuidance = emptyList(),
                calculationBasis = "Sun sign (${sun.signIndex}) vs Rahu (${rahu?.signIndex})."
            )
        }

        val afflictingPlanet = if (isSunWithRahu) "Rahu" else "Saturn"
        return DoshaAnalysisResult(
            id = "pitra_dosha",
            name = "Pitra Dosha",
            sanskritName = "पितृ दोष",
            category = DoshaCategory.PLANETARY_AFFLICTION,
            status = AnalysisStatus.DETECTED,
            severity = DoshaSeverity.MODERATE,
            isCancelled = false,
            cancellationReason = null,
            participatingPlanets = listOf("Sun", afflictingPlanet),
            participatingHouses = listOf(sun.house),
            participatingSigns = listOf(sun.sign),
            evidence = "Sun is conjoined with $afflictingPlanet in House ${sun.house} (${sun.sign}).",
            classicalRule = "Sun conjoined with Rahu or Saturn causes Pitra Dosha.",
            remedialGuidance = listOf(
                "Perform Tarpan or charity in the name of ancestors on Amavasya.",
                "Offer Arghya (water) to the Rising Sun daily with Gayatri Mantra.",
                "Plant a Peepal or Banyan tree and nurture it."
            ),
            calculationBasis = "Sun conjoined with $afflictingPlanet in house ${sun.house}."
        )
    }

    private fun evaluateShrapitDosha(planetByName: Map<String, PlanetPosition>): DoshaAnalysisResult? {
        val saturn = planetByName["saturn"] ?: return null
        val rahu = planetByName["rahu"] ?: return null

        val isConjoined = saturn.signIndex == rahu.signIndex
        if (!isConjoined) {
            return DoshaAnalysisResult(
                id = "shrapit_dosha",
                name = "Shrapit Dosha (Saturn-Rahu)",
                sanskritName = "श्रापित दोष (शनि-राहु)",
                category = DoshaCategory.PLANETARY_AFFLICTION,
                status = AnalysisStatus.NOT_DETECTED,
                severity = DoshaSeverity.NONE,
                isCancelled = false,
                cancellationReason = null,
                participatingPlanets = listOf("Saturn", "Rahu"),
                participatingHouses = listOf(saturn.house, rahu.house),
                participatingSigns = listOf(saturn.sign, rahu.sign),
                evidence = "Saturn and Rahu are placed in different signs (No Shrapit Dosha).",
                classicalRule = "Saturn and Rahu conjoined in the same sign forms Shrapit Dosha.",
                remedialGuidance = emptyList(),
                calculationBasis = "Saturn sign (${saturn.signIndex}) vs Rahu sign (${rahu.signIndex})."
            )
        }

        return DoshaAnalysisResult(
            id = "shrapit_dosha",
            name = "Shrapit Dosha (Saturn-Rahu)",
            sanskritName = "श्रापित दोष (शनि-राहु)",
            category = DoshaCategory.PLANETARY_AFFLICTION,
            status = AnalysisStatus.DETECTED,
            severity = DoshaSeverity.HIGH,
            isCancelled = false,
            cancellationReason = null,
            participatingPlanets = listOf("Saturn", "Rahu"),
            participatingHouses = listOf(saturn.house),
            participatingSigns = listOf(saturn.sign),
            evidence = "Saturn and Rahu are conjoined in House ${saturn.house} (${saturn.sign}).",
            classicalRule = "Saturn-Rahu conjunction creates Shrapit Yoga, requiring karmic remedies.",
            remedialGuidance = listOf(
                "Chant Shani Mantra and Rahu Stotra on Saturdays.",
                "Feed black dogs, cows, or crows on Saturday evenings.",
                "Perform Rudrabhishek for harmony and peace."
            ),
            calculationBasis = "Saturn and Rahu both in sign ${saturn.sign}."
        )
    }

    private fun evaluateGandmantaDosha(moonPos: PlanetPosition?): DoshaAnalysisResult? {
        val moon = moonPos ?: return null
        val gandmoolNakshatras = setOf("Ashwini", "Ashlesha", "Aslesha", "Magha", "Jyeshtha", "Jyeshta", "Mula", "Moola", "Revati")

        val isGandmool = moon.nakshatra in gandmoolNakshatras
        if (!isGandmool) {
            return DoshaAnalysisResult(
                id = "gandmanta_dosha",
                name = "Gandmanta / Gandmool Dosha",
                sanskritName = "गंडमूल / संधि दोष",
                category = DoshaCategory.NAKSHATRA_JUNCTION,
                status = AnalysisStatus.NOT_DETECTED,
                severity = DoshaSeverity.NONE,
                isCancelled = false,
                cancellationReason = null,
                participatingPlanets = listOf("Moon"),
                participatingHouses = listOf(moon.house),
                participatingSigns = listOf(moon.sign),
                evidence = "Moon is in ${moon.nakshatra} Pada ${moon.nakshatraPada}, which is not a Gandmool sandhi junction.",
                classicalRule = "Moon placed in Ashwini, Aslesha, Magha, Jyeshtha, Mula, or Revati creates Gandmool Dosha.",
                remedialGuidance = emptyList(),
                calculationBasis = "Moon in Nakshatra ${moon.nakshatra}."
            )
        }

        return DoshaAnalysisResult(
            id = "gandmanta_dosha",
            name = "Gandmanta / Gandmool Dosha (${moon.nakshatra})",
            sanskritName = "गंडमूल दोष (${moon.nakshatra})",
            category = DoshaCategory.NAKSHATRA_JUNCTION,
            status = AnalysisStatus.DETECTED,
            severity = DoshaSeverity.LOW,
            isCancelled = false,
            cancellationReason = null,
            participatingPlanets = listOf("Moon"),
            participatingHouses = listOf(moon.house),
            participatingSigns = listOf(moon.sign),
            evidence = "Moon is placed in Gandmool Nakshatra ${moon.nakshatra} (Pada ${moon.nakshatraPada}) at sign junction.",
            classicalRule = "Moon in Rashi-Nakshatra sandhi (Gandmool) indicates energetic transition requiring peaceful propitiation.",
            remedialGuidance = listOf(
                "Perform Nakshatra Shanti or Gandmool Shanti pooja.",
                "Recite Gayatri Mantra daily.",
                "Donate green grams (Moong) or white items on Wednesday/Monday."
            ),
            calculationBasis = "Moon in Gandmool Nakshatra ${moon.nakshatra}."
        )
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
