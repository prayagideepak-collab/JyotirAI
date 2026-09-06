package com.example.domain.engine.yogadosha

import com.example.domain.models.*

/**
 * Deterministic Parashari Dosha Rule Engine.
 * Evaluates classical Vedic afflictions, planetary doshas, cancellations, and mitigations
 * based on pure astronomical positions and Whole-Sign houses.
 */
object DoshaRuleEngine {

    /**
     * Evaluates all supported classical Vedic Doshas for the provided profile.
     */
    fun evaluateAll(profile: AstrologyProfile): List<DoshaAnalysisResult> {
        val planets = profile.planetPositions
        val lagnaSignIndex = profile.lagnaSignIndex
        val lagnaRashi = Rashi.fromIndex(lagnaSignIndex)
        val moonPos = planets.firstOrNull { it.planet.equals("moon", ignoreCase = true) }

        val planetByName = mutableMapOf<String, PlanetPosition>()
        val planetByHouse = mutableMapOf<Int, MutableList<PlanetPosition>>()
        planets.forEach { p ->
            val norm = normalizePlanetName(p.planet)
            planetByName[norm] = p
            planetByHouse.getOrPut(p.house) { mutableListOf() }.add(p)
        }

        val results = mutableListOf<DoshaAnalysisResult>()

        // 1. Manglik / Kuja Dosha (Lagna & Chandra Kundli with classical cancellations)
        results.add(evaluateManglikDosha(planetByName, moonPos))

        // 2. Kaal Sarp Dosha (All 12 classical types & Full vs Partial)
        evaluateKaalSarpDosha(planetByName)?.let { results.add(it) }

        // 3. Kemadruma Dosha (with Kemadruma Bhanga cancellation)
        evaluateKemadrumaDosha(planetByName, moonPos, planetByHouse)?.let { results.add(it) }

        // 4. Guru Chandal Dosha (Jupiter-Rahu/Ketu conjunction)
        evaluateGuruChandalDosha(planetByName)?.let { results.add(it) }

        // 5. Pitra Dosha (Solar affliction in 9th or with Rahu/Ketu)
        evaluatePitraDosha(planetByName)?.let { results.add(it) }

        // 6. Shrapit Dosha (Saturn-Rahu conjunction)
        evaluateShrapitDosha(planetByName)?.let { results.add(it) }

        // 7. Gandmanta Dosha (Nakshatra sandhi junction)
        evaluateGandmantaDosha(moonPos)?.let { results.add(it) }

        return ResultValidator.sanitizeAndOrderDoshas(results)
    }

    private fun evaluateManglikDosha(
        planetByName: Map<String, PlanetPosition>,
        moonPos: PlanetPosition?
    ): DoshaAnalysisResult {
        val mars = planetByName["mars"]
        if (mars == null) {
            return DoshaAnalysisResult(
                id = "manglik_dosha",
                name = "Manglik / Kuja Dosha",
                sanskritName = "मांगलिक / कुज दोष",
                category = DoshaCategory.MANGLIK,
                isDetected = false,
                severity = DoshaSeverity.NONE,
                isCancelled = false,
                participatingPlanets = emptyList(),
                participatingHouses = emptyList(),
                participatingSigns = emptyList(),
                evidence = "Mars position unavailable.",
                classicalRule = "Mars in houses 1, 4, 7, 8, 12 from Lagna or Moon causes Manglik Dosha.",
                calculationBasis = "No Mars position found."
            )
        }

        val manglikHouses = setOf(1, 4, 7, 8, 12)
        val isLagnaManglik = mars.house in manglikHouses

        val houseFromMoon = if (moonPos != null) (((mars.signIndex - moonPos.signIndex).mod(12)) + 1) else 0
        val isChandraManglik = houseFromMoon in manglikHouses

        val isDetected = isLagnaManglik || isChandraManglik

        if (!isDetected) {
            return DoshaAnalysisResult(
                id = "manglik_dosha",
                name = "Manglik / Kuja Dosha",
                sanskritName = "मांगलिक / कुज दोष",
                category = DoshaCategory.MANGLIK,
                isDetected = false,
                severity = DoshaSeverity.NONE,
                isCancelled = false,
                participatingPlanets = listOf("Mars"),
                participatingHouses = listOf(mars.house),
                participatingSigns = listOf(mars.sign),
                evidence = "Mars is in House ${mars.house} from Lagna and House $houseFromMoon from Moon (neither in 1, 4, 7, 8, or 12).",
                classicalRule = "Mars in 1st, 4th, 7th, 8th, or 12th house from Lagna or Moon produces Kuja Dosha.",
                calculationBasis = "Lagna house = ${mars.house}, Moon house = $houseFromMoon.",
                remedialGuidance = emptyList()
            )
        }

        // Classical Parashari Cancellation & Mitigation Rules (Brihat Parashara & Phaladeepika)
        val cancellationReasons = mutableListOf<String>()

        // 1. Mars in own sign or exaltation in specific houses
        if (mars.house == 1 && mars.signIndex == 0) cancellationReasons.add("Mars in 1st house in own sign Aries (Mesha)")
        if (mars.house == 4 && mars.signIndex == 7) cancellationReasons.add("Mars in 4th house in own sign Scorpio (Vrishchika)")
        if (mars.house == 7 && mars.signIndex == 9) cancellationReasons.add("Mars in 7th house in exaltation sign Capricorn (Makara)")
        if (mars.house == 8 && (mars.signIndex == 3 || mars.signIndex == 8)) cancellationReasons.add("Mars in 8th house in Cancer or Sagittarius")
        if (mars.house == 12 && (mars.signIndex == 11 || mars.signIndex == 2)) cancellationReasons.add("Mars in 12th house in Pisces or Gemini")

        // 2. Benefic Jupiter Aspect or Conjunction
        val jupiter = planetByName["jupiter"]
        if (jupiter != null) {
            val isJupiterConjoined = jupiter.house == mars.house
            val aspectDistance = ((mars.house - jupiter.house).mod(12)) + 1
            val isJupiterAspected = aspectDistance in setOf(5, 7, 9) // Jupiter casts 5th, 7th, 9th drishti
            if (isJupiterConjoined) cancellationReasons.add("Jupiter is conjoined with Mars in House ${mars.house}")
            if (isJupiterAspected) cancellationReasons.add("Benefic Jupiter casts special ${aspectDistance}th aspect on Mars")
        }

        // 3. Chandra-Mangala Conjunction
        if (moonPos != null && moonPos.house == mars.house) {
            cancellationReasons.add("Moon and Mars are conjoined forming Chandra-Mangala Yoga")
        }

        val isCancelled = cancellationReasons.isNotEmpty()
        val severity = when {
            isCancelled -> DoshaSeverity.CANCELLED
            isLagnaManglik && isChandraManglik -> DoshaSeverity.HIGH
            mars.house == 7 || mars.house == 8 -> DoshaSeverity.HIGH
            else -> DoshaSeverity.MODERATE
        }

        val evidenceStr = buildString {
            if (isLagnaManglik && isChandraManglik) append("Mars is placed in House ${mars.house} (Lagna Manglik) and House $houseFromMoon from Moon (Chandra Manglik). ")
            else if (isLagnaManglik) append("Mars is placed in House ${mars.house} from Lagna (Lagna Manglik). ")
            else append("Mars is placed in House $houseFromMoon from Moon (Chandra Manglik). ")

            if (isCancelled) {
                append("Dosha is mitigated/cancelled by classical rules: ${cancellationReasons.joinToString("; ")}.")
            }
        }

        val remedies = if (!isCancelled) {
            listOf(
                "Recitation of Hanuman Chalisa or Mangal Gayatri Mantra.",
                "Charity of red lentils (masoor dal) or copper on Tuesdays.",
                "Matching of horoscopes with compatibility verification before marriage."
            )
        } else {
            listOf("Classical cancellations apply; maintain positive communication in relationships.")
        }

        return DoshaAnalysisResult(
            id = "manglik_dosha",
            name = "Manglik / Kuja Dosha",
            sanskritName = "मांगलिक / कुज दोष",
            category = DoshaCategory.MANGLIK,
            isDetected = true,
            severity = severity,
            isCancelled = isCancelled,
            cancellationReason = if (isCancelled) cancellationReasons.joinToString("; ") else null,
            participatingPlanets = listOf("Mars") + if (jupiter != null && (jupiter.house == mars.house || ((mars.house - jupiter.house).mod(12)) + 1 in setOf(5, 7, 9))) listOf("Jupiter") else emptyList(),
            participatingHouses = listOf(mars.house),
            participatingSigns = listOf(mars.sign),
            evidence = evidenceStr,
            classicalRule = "Mars placed in 1st, 4th, 7th, 8th, or 12th house from Lagna or Moon causes Kuja Dosha, with classical cancellations for specific sign placements and Jupiter's benefic aspect.",
            remedialGuidance = remedies,
            calculationBasis = "Mars in house ${mars.house} (${mars.sign}), house from Moon = $houseFromMoon."
        )
    }

    private fun evaluateKaalSarpDosha(planetByName: Map<String, PlanetPosition>): DoshaAnalysisResult? {
        val rahu = planetByName["rahu"] ?: return null
        val ketu = planetByName["ketu"] ?: return null

        val sevenPlanets = listOfNotNull(
            planetByName["sun"],
            planetByName["moon"],
            planetByName["mars"],
            planetByName["mercury"],
            planetByName["jupiter"],
            planetByName["venus"],
            planetByName["saturn"]
        )
        if (sevenPlanets.size < 7) return null

        val rahuLong = rahu.totalLongitude
        val ketuLong = ketu.totalLongitude

        // Check span from Rahu to Ketu clockwise vs Ketu to Rahu clockwise
        var insideCountForward = 0
        var insideCountBackward = 0

        for (p in sevenPlanets) {
            val pLong = p.totalLongitude
            if (isLongitudeBetween(pLong, rahuLong, ketuLong)) insideCountForward++
            if (isLongitudeBetween(pLong, ketuLong, rahuLong)) insideCountBackward++
        }

        val isFullForward = insideCountForward == 7
        val isFullBackward = insideCountBackward == 7
        val isFull = isFullForward || isFullBackward
        val isPartial = !isFull && (insideCountForward == 6 || insideCountBackward == 6)

        val isDetected = isFull || isPartial
        if (!isDetected) {
            return DoshaAnalysisResult(
                id = "kaal_sarp_dosha",
                name = "Kaal Sarp Dosha",
                sanskritName = "कालसर्प दोष",
                category = DoshaCategory.KAAL_SARP,
                isDetected = false,
                severity = DoshaSeverity.NONE,
                isCancelled = false,
                participatingPlanets = listOf("Rahu", "Ketu"),
                participatingHouses = listOf(rahu.house, ketu.house),
                participatingSigns = listOf(rahu.sign, ketu.sign),
                evidence = "Planets are distributed on both sides of the Rahu-Ketu axis ($insideCountForward vs $insideCountBackward planets).",
                classicalRule = "When all 7 physical planets are enclosed between the nodal axis of Rahu and Ketu, Kaal Sarp Dosha is formed.",
                calculationBasis = "Rahu at ${"%.2f".format(rahuLong)}°, Ketu at ${"%.2f".format(ketuLong)}°.",
                remedialGuidance = emptyList()
            )
        }

        val (typeName, sanskritTypeName) = getKaalSarpTypeName(rahu.house)
        val subtype = if (isFull) "Full (Purna / पूर्ण)" else "Partial (Khandit / आंशिक)"
        val severity = if (isFull) DoshaSeverity.HIGH else DoshaSeverity.MODERATE

        val evidenceStr = if (isFull) {
            "All 7 physical planets are enclosed entirely on one side of the Rahu-Ketu axis (Rahu in House ${rahu.house}, Ketu in House ${ketu.house}). Forms $subtype $typeName."
        } else {
            "6 of 7 physical planets are enclosed between Rahu (House ${rahu.house}) and Ketu (House ${ketu.house}), forming $subtype $typeName."
        }

        val remedies = listOf(
            "Recitation of Maha Mrityunjaya Mantra daily.",
            "Worship of Lord Shiva (Rudrabhisheka) on Mondays or Pradosh days.",
            "Watering Peepal or Banyan tree on Saturdays without touching."
        )

        return DoshaAnalysisResult(
            id = "kaal_sarp_${rahu.house}",
            name = "$typeName ($subtype)",
            sanskritName = "$sanskritTypeName ($subtype)",
            category = DoshaCategory.KAAL_SARP,
            isDetected = true,
            severity = severity,
            isCancelled = false,
            participatingPlanets = listOf("Rahu", "Ketu") + sevenPlanets.map { it.planet },
            participatingHouses = (listOf(rahu.house, ketu.house) + sevenPlanets.map { it.house }).distinct(),
            participatingSigns = (listOf(rahu.sign, ketu.sign) + sevenPlanets.map { it.sign }).distinct(),
            evidence = evidenceStr,
            classicalRule = "All 7 planets hemmed between Rahu and Ketu forms Kaal Sarp Dosha. Type is determined by Rahu's house placement (1st: Anant, 2nd: Kulik, etc.).",
            remedialGuidance = remedies,
            calculationBasis = "Rahu house ${rahu.house}, Ketu house ${ketu.house}, enclosed planets = ${if (isFullForward) insideCountForward else insideCountBackward}/7."
        )
    }

    private fun evaluateKemadrumaDosha(
        planetByName: Map<String, PlanetPosition>,
        moonPos: PlanetPosition?,
        planetByHouse: Map<Int, List<PlanetPosition>>
    ): DoshaAnalysisResult? {
        val moon = moonPos ?: return null
        val ignoredPlanets = setOf("sun", "moon", "rahu", "ketu", "ascendant", "lagna")

        val secondSign = (moon.signIndex + 1).mod(12)
        val twelfthSign = (moon.signIndex - 1).mod(12)

        val planetsIn2nd = planetByName.values.filter { it.signIndex == secondSign && it.planet.lowercase() !in ignoredPlanets }
        val planetsIn12th = planetByName.values.filter { it.signIndex == twelfthSign && it.planet.lowercase() !in ignoredPlanets }

        val isRawKemadruma = planetsIn2nd.isEmpty() && planetsIn12th.isEmpty()

        if (!isRawKemadruma) {
            return DoshaAnalysisResult(
                id = "kemadruma_dosha",
                name = "Kemadruma Dosha",
                sanskritName = "केमद्रुम दोष",
                category = DoshaCategory.KEMADRUMA,
                isDetected = false,
                severity = DoshaSeverity.NONE,
                isCancelled = false,
                participatingPlanets = listOf("Moon"),
                participatingHouses = listOf(moon.house),
                participatingSigns = listOf(moon.sign),
                evidence = "Moon is flanked by planets in 2nd/12th from it (not isolated).",
                classicalRule = "No planets (excluding Sun/Rahu/Ketu) in 2nd or 12th from Moon causes Kemadruma Dosha.",
                calculationBasis = "2nd from Moon has ${planetsIn2nd.size} planets, 12th from Moon has ${planetsIn12th.size} planets."
            )
        }

        // Kemadruma Bhanga (Cancellation) Conditions (Brihat Parashara Hora Shastra)
        val bhangaReasons = mutableListOf<String>()

        // 1. Moon in Kendra (1, 4, 7, 10) from Lagna
        if (moon.house in setOf(1, 4, 7, 10)) {
            bhangaReasons.add("Moon occupies Kendra House ${moon.house} from Lagna")
        }

        // 2. Any planet in Kendra from Lagna
        val kendraPlanets = (1..4).flatMap { k ->
            val h = when (k) { 1 -> 1; 2 -> 4; 3 -> 7; else -> 10 }
            planetByHouse[h]?.filter { it.planet.lowercase() !in setOf("rahu", "ketu", "ascendant", "lagna") } ?: emptyList()
        }
        if (kendraPlanets.isNotEmpty()) {
            bhangaReasons.add("Planets (${kendraPlanets.joinToString { it.planet }}) occupy Kendra houses from Lagna")
        }

        // 3. Jupiter aspects or conjoins Moon
        val jupiter = planetByName["jupiter"]
        if (jupiter != null) {
            val dist = ((moon.house - jupiter.house).mod(12)) + 1
            if (dist in setOf(1, 5, 7, 9)) {
                bhangaReasons.add("Benefic Jupiter aspects/conjoins Moon")
            }
        }

        val isCancelled = bhangaReasons.isNotEmpty()
        val severity = if (isCancelled) DoshaSeverity.CANCELLED else DoshaSeverity.MODERATE

        val evidenceStr = buildString {
            append("No physical planets (except Sun/nodes) occupy the 2nd or 12th house from Moon in ${moon.sign}. ")
            if (isCancelled) {
                append("Dosha is cancelled (Kemadruma Bhanga): ${bhangaReasons.joinToString("; ")}.")
            }
        }

        val remedies = if (!isCancelled) {
            listOf(
                "Worship of Goddess Lakshmi and Lord Shiva.",
                "Offering milk to Shivling on Mondays.",
                "Wearing a silver ornament or keeping a square silver piece."
            )
        } else {
            listOf("Kemadruma cancellation applies; mental resilience is preserved.")
        }

        return DoshaAnalysisResult(
            id = "kemadruma_dosha",
            name = "Kemadruma Dosha",
            sanskritName = "केमद्रुम दोष",
            category = DoshaCategory.KEMADRUMA,
            isDetected = true,
            severity = severity,
            isCancelled = isCancelled,
            cancellationReason = if (isCancelled) bhangaReasons.joinToString("; ") else null,
            participatingPlanets = listOf("Moon"),
            participatingHouses = listOf(moon.house),
            participatingSigns = listOf(moon.sign),
            evidence = evidenceStr,
            classicalRule = "When there are no planets (excluding Sun, Rahu, Ketu) in the 2nd and 12th houses from Moon, Kemadruma Dosha occurs, but is cancelled if Moon or other planets occupy Kendras.",
            remedialGuidance = remedies,
            calculationBasis = "Evaluated lunar isolation and Kendra bhanga factors."
        )
    }

    private fun evaluateGuruChandalDosha(planetByName: Map<String, PlanetPosition>): DoshaAnalysisResult? {
        val jupiter = planetByName["jupiter"] ?: return null
        val rahu = planetByName["rahu"]
        val ketu = planetByName["ketu"]

        val withRahu = rahu != null && rahu.signIndex == jupiter.signIndex
        val withKetu = ketu != null && ketu.signIndex == jupiter.signIndex

        if (!withRahu && !withKetu) return null

        val nodeName = if (withRahu) "Rahu" else "Ketu"
        val nodePos = if (withRahu) rahu!! else ketu!!
        val degreeDiff = kotlin.math.abs(jupiter.totalLongitude - nodePos.totalLongitude)

        val isStrongMitigation = jupiter.dignity in setOf(PlanetDignity.EXALTED, PlanetDignity.OWN_SIGN) || degreeDiff > 10.0
        val severity = if (isStrongMitigation) DoshaSeverity.LOW else DoshaSeverity.MODERATE

        val evidenceStr = "Jupiter and $nodeName are conjoined in House ${jupiter.house} (${jupiter.sign}) with ${"%.2f".format(degreeDiff)}° separation. Jupiter dignity: ${jupiter.dignity.displayName}."

        val remedies = listOf(
            "Recitation of Guru Beej Mantra ('Om Gram Greem Grom Sah Gurave Namah').",
            "Respect and service to spiritual preceptors, teachers, and elders.",
            "Donation of yellow items (turmeric, yellow cloth, gram dal) on Thursdays."
        )

        return DoshaAnalysisResult(
            id = "guru_chandal_dosha",
            name = "Guru Chandal Dosha",
            sanskritName = "गुरु चांडाल दोष",
            category = DoshaCategory.PLANETARY_AFFLICTION,
            isDetected = true,
            severity = severity,
            isCancelled = isStrongMitigation && degreeDiff > 12.0,
            cancellationReason = if (isStrongMitigation) "Mitigated by Jupiter's dignity (${jupiter.dignity.displayName}) and wide separation (${"%.2f".format(degreeDiff)}°)" else null,
            participatingPlanets = listOf("Jupiter", nodeName),
            participatingHouses = listOf(jupiter.house),
            participatingSigns = listOf(jupiter.sign),
            evidence = evidenceStr,
            classicalRule = "Jupiter conjoined with Rahu or Ketu in the same house creates Guru Chandal Dosha, impacting clarity and moral guidance.",
            remedialGuidance = remedies,
            calculationBasis = "Jupiter and $nodeName in sign index ${jupiter.signIndex}."
        )
    }

    private fun evaluatePitraDosha(planetByName: Map<String, PlanetPosition>): DoshaAnalysisResult? {
        val sun = planetByName["sun"] ?: return null
        val rahu = planetByName["rahu"]
        val ketu = planetByName["ketu"]

        val withRahu = rahu != null && rahu.signIndex == sun.signIndex
        val withKetu = ketu != null && ketu.signIndex == sun.signIndex
        val isIn9th = sun.house == 9 && (withRahu || withKetu)

        val isDetected = isIn9th || (withRahu && (sun.house in setOf(1, 5, 9)))
        if (!isDetected) return null

        val nodeName = if (withRahu) "Rahu" else "Ketu"
        val severity = if (sun.house == 9) DoshaSeverity.HIGH else DoshaSeverity.MODERATE

        val evidenceStr = "Sun is conjoined with $nodeName in House ${sun.house} (${sun.sign}), afflicting the solar parental/dharma karaka."

        val remedies = listOf(
            "Offering water (Arghya) with red flowers and jaggery to the Sun at sunrise with Gayatri Mantra.",
            "Performing ancestral tarpan / charitable donations on Amavasya days.",
            "Showing reverence to father, elders, and lineage gurus."
        )

        return DoshaAnalysisResult(
            id = "pitra_dosha",
            name = "Pitra Dosha (Solar Affliction)",
            sanskritName = "पितृ दोष",
            category = DoshaCategory.PLANETARY_AFFLICTION,
            isDetected = true,
            severity = severity,
            isCancelled = false,
            participatingPlanets = listOf("Sun", nodeName),
            participatingHouses = listOf(sun.house),
            participatingSigns = listOf(sun.sign),
            evidence = evidenceStr,
            classicalRule = "Sun afflicted by Rahu/Ketu in dharma houses (especially 9th house) indicates ancestral karmic debts (Pitra Dosha).",
            remedialGuidance = remedies,
            calculationBasis = "Sun and $nodeName conjunction in house ${sun.house}."
        )
    }

    private fun evaluateShrapitDosha(planetByName: Map<String, PlanetPosition>): DoshaAnalysisResult? {
        val saturn = planetByName["saturn"] ?: return null
        val rahu = planetByName["rahu"] ?: return null

        val isConjoined = saturn.signIndex == rahu.signIndex
        if (!isConjoined) return null

        val degreeDiff = kotlin.math.abs(saturn.totalLongitude - rahu.totalLongitude)
        val evidenceStr = "Saturn and Rahu are conjoined in House ${saturn.house} (${saturn.sign}) with ${"%.2f".format(degreeDiff)}° separation."

        val remedies = listOf(
            "Recitation of Hanuman Chalisa daily and Dasharatha Shani Stotram on Saturdays.",
            "Serving physically challenged individuals or doing selfless community service.",
            "Lighting a mustard oil lamp under a Peepal tree on Saturday evenings."
        )

        return DoshaAnalysisResult(
            id = "shrapit_dosha",
            name = "Shrapit Dosha (Saturn-Rahu Conjunction)",
            sanskritName = "श्रापित दोष (शनि-राहु युति)",
            category = DoshaCategory.PLANETARY_AFFLICTION,
            isDetected = true,
            severity = DoshaSeverity.MODERATE,
            isCancelled = false,
            participatingPlanets = listOf("Saturn", "Rahu"),
            participatingHouses = listOf(saturn.house),
            participatingSigns = listOf(saturn.sign),
            evidence = evidenceStr,
            classicalRule = "Saturn and Rahu conjoined in the same sign produces Shrapit Dosha, indicating obstacles that require persistent effort.",
            remedialGuidance = remedies,
            calculationBasis = "Saturn and Rahu conjunction in sign index ${saturn.signIndex}."
        )
    }

    private fun evaluateGandmantaDosha(moonPos: PlanetPosition?): DoshaAnalysisResult? {
        val moon = moonPos ?: return null

        val nakshatraName = moon.nakshatra.lowercase().trim()
        val pada = moon.nakshatraPada

        val isGandmanta = when (nakshatraName) {
            "ashwini" -> pada == 1
            "ashlesha" -> pada == 4
            "magha" -> pada == 1
            "jyeshtha" -> pada == 4
            "mula", "moola" -> pada == 1
            "revati" -> pada == 4
            else -> false
        }

        if (!isGandmanta) return null

        val junctionName = "${moon.nakshatra} (Pada $pada)"
        val evidenceStr = "Moon is positioned in critical Nakshatra sandhi junction $junctionName at ${moon.formattedDegree} in ${moon.sign}."

        val remedies = listOf(
            "Performing traditional Gandmool Shanti on the 27th day when the Moon returns to the same nakshatra.",
            "Worship of Lord Ganesha and daily chanting of Maha Mrityunjaya Mantra.",
            "Charity of green vegetables, grains, or silver on auspicious nakshatra days."
        )

        return DoshaAnalysisResult(
            id = "gandmanta_dosha",
            name = "Gandmanta Dosha ($junctionName)",
            sanskritName = "गंडमूल दोष ($junctionName)",
            category = DoshaCategory.NAKSHATRA_JUNCTION,
            isDetected = true,
            severity = DoshaSeverity.MODERATE,
            isCancelled = false,
            participatingPlanets = listOf("Moon"),
            participatingHouses = listOf(moon.house),
            participatingSigns = listOf(moon.sign),
            evidence = evidenceStr,
            classicalRule = "Birth when the Moon is in the transition zone between water and fire signs/nakshatras produces Gandmanta Dosha.",
            remedialGuidance = remedies,
            calculationBasis = "Moon in ${moon.nakshatra} pada $pada."
        )
    }

    private fun isLongitudeBetween(p: Double, start: Double, end: Double): Boolean {
        val normP = (p % 360.0 + 360.0) % 360.0
        val normStart = (start % 360.0 + 360.0) % 360.0
        val normEnd = (end % 360.0 + 360.0) % 360.0

        return if (normStart < normEnd) {
            normP in normStart..normEnd
        } else {
            normP >= normStart || normP <= normEnd
        }
    }

    private fun getKaalSarpTypeName(rahuHouse: Int): Pair<String, String> = when (rahuHouse) {
        1 -> Pair("Anant Kaal Sarp Dosha", "अनंत कालसर्प दोष")
        2 -> Pair("Kulik Kaal Sarp Dosha", "कुलिक कालसर्प दोष")
        3 -> Pair("Vasuki Kaal Sarp Dosha", "वासुकी कालसर्प दोष")
        4 -> Pair("Shankhpal Kaal Sarp Dosha", "शंखपाल कालसर्प दोष")
        5 -> Pair("Padma Kaal Sarp Dosha", "पद्म कालसर्प दोष")
        6 -> Pair("Mahapadma Kaal Sarp Dosha", "महापद्म कालसर्प दोष")
        7 -> Pair("Takshak Kaal Sarp Dosha", "तक्षक कालसर्प दोष")
        8 -> Pair("Karkotak Kaal Sarp Dosha", "कर्कोटक कालसर्प दोष")
        9 -> Pair("Shankhachood Kaal Sarp Dosha", "शंखचूड़ कालसर्प दोष")
        10 -> Pair("Ghatak Kaal Sarp Dosha", "घातक कालसर्प दोष")
        11 -> Pair("Vishdhar Kaal Sarp Dosha", "विषधर कालसर्प दोष")
        12 -> Pair("Sheshnag Kaal Sarp Dosha", "शेषनाग कालसर्प दोष")
        else -> Pair("Kaal Sarp Dosha", "कालसर्प दोष")
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
