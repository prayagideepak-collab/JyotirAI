package com.example.domain.muhurta

import com.example.domain.models.*

/**
 * Evaluates candidate time windows deterministically against Panchang Shuddhi,
 * Activity-specific requirements, Personal Bala (if personalized), and inauspicious overlaps.
 */
object MuhurtaRuleEvaluator {

    fun evaluateWindow(
        window: RawTimeWindow,
        panchang: PanchangResult,
        ruleProfile: ActivityRuleProfile,
        inauspiciousIntervals: List<InauspiciousInterval>,
        personalBala: PersonalBalaContext?
    ): MuhurtaCandidateWindow {
        val supportingFactors = mutableListOf<MuhurtaFactor>()
        val cautionFactors = mutableListOf<MuhurtaFactor>()
        val ruleEvidence = mutableListOf<MuhurtaRuleEvidence>()

        var score = window.baseWeight * 50.0 // Base score 0 to 100

        val tithiNumber = panchang.tithi.index
        val tithiName = panchang.tithi.name
        val vara = panchang.vara
        val nakshatra = panchang.nakshatra.nakshatra
        val yogaIndex = panchang.yoga.index
        val karanaName = panchang.karana.name
        val isVishtiKarana = karanaName.contains("Vishti", ignoreCase = true) || karanaName.contains("भद्रा", ignoreCase = true)

        // 1. Evaluate Window Inherent Type
        if (window.name.contains("Abhijit", ignoreCase = true)) {
            if (vara == Vara.BUDHAVARA) {
                cautionFactors.add(
                    MuhurtaFactor(
                        category = MuhurtaFactorCategory.SOLAR_LUNAR_WINDOW,
                        title = "Abhijit on Wednesday (बुधवार अभिजित् दोष)",
                        description = "On Wednesdays, Abhijit Muhurta is subjected to planetary dosha according to classical texts (Muhurta Chintamani).",
                        isSupporting = false,
                        classicalReference = "Muhurta Chintamani"
                    )
                )
                ruleEvidence.add(MuhurtaRuleEvidence("RULE_ABHIJIT_WED", "Abhijit Wednesday Dosha", false, -15.0, "Wednesday Abhijit reduced weight."))
                score -= 15.0
            } else {
                supportingFactors.add(
                    MuhurtaFactor(
                        category = MuhurtaFactorCategory.SOLAR_LUNAR_WINDOW,
                        title = "Abhijit Muhurta (सर्व कार्य सिद्धि)",
                        description = "Abhijit Muhurta is universally celebrated for destroying major doshas and ensuring success.",
                        isSupporting = true,
                        classicalReference = "Narada Samhita"
                    )
                )
                ruleEvidence.add(MuhurtaRuleEvidence("RULE_ABHIJIT", "Abhijit Auspiciousness", true, 25.0, "Midday solar noon victory window."))
                score += 25.0
            }
        } else if (window.name.contains("Brahma", ignoreCase = true)) {
            supportingFactors.add(
                MuhurtaFactor(
                    category = MuhurtaFactorCategory.SOLAR_LUNAR_WINDOW,
                    title = "Brahma Muhurta (ब्राह्म मुहूर्त)",
                    description = "Sacred pre-dawn period (48-96 mins before sunrise) imbued with pure sattvic vibrations.",
                    isSupporting = true,
                    classicalReference = "Manusmriti / Ashtanga Hridaya"
                )
            )
            ruleEvidence.add(MuhurtaRuleEvidence("RULE_BRAHMA", "Brahma Muhurta Sattva", true, 20.0, "Supreme sattvic dawn window."))
            score += 20.0
        }

        // 2. Evaluate Tithi Shuddhi
        if (ruleProfile.avoidedTithis.contains(tithiNumber)) {
            val isRikta = ActivityContextResolver.RIKTA_TITHIS.contains(tithiNumber)
            val isAmavasya = ActivityContextResolver.AMAVASYA_TITHI.contains(tithiNumber)
            val desc = when {
                isRikta -> "Rikta Tithi ($tithiName - 4th/9th/14th): Unfavorable for new beginnings and investments."
                isAmavasya -> "Amavasya ($tithiName): Dark Moon tithi cautioned for secular ventures and beginnings."
                else -> "Tithi $tithiName is cautioned for ${ruleProfile.activityType.englishName}."
            }
            cautionFactors.add(
                MuhurtaFactor(
                    category = MuhurtaFactorCategory.PANCHANG_TITHI,
                    title = "Tithi Caution ($tithiName)",
                    description = desc,
                    isSupporting = false,
                    classicalReference = "Brihat Samhita"
                )
            )
            ruleEvidence.add(MuhurtaRuleEvidence("RULE_TITHI", "Tithi Shuddhi", false, -20.0, desc))
            score -= 20.0
        } else if (ruleProfile.favorableTithis.contains(tithiNumber)) {
            supportingFactors.add(
                MuhurtaFactor(
                    category = MuhurtaFactorCategory.PANCHANG_TITHI,
                    title = "Favorable Tithi ($tithiName)",
                    description = "Tithi $tithiName aligns well with ${ruleProfile.activityType.englishName}.",
                    isSupporting = true,
                    classicalReference = "Muhurta Ganapati"
                )
            )
            ruleEvidence.add(MuhurtaRuleEvidence("RULE_TITHI", "Tithi Shuddhi", true, 15.0, "Tithi $tithiName is favorable."))
            score += 15.0
        }

        // 3. Evaluate Vara Suitability
        if (ruleProfile.avoidedVaras.contains(vara)) {
            val desc = "Weekday ${vara.sanskritName} (${vara.name}) is cautioned for ${ruleProfile.activityType.englishName}."
            cautionFactors.add(
                MuhurtaFactor(
                    category = MuhurtaFactorCategory.PANCHANG_VARA,
                    title = "Vara Caution (${vara.sanskritName})",
                    description = desc,
                    isSupporting = false
                )
            )
            ruleEvidence.add(MuhurtaRuleEvidence("RULE_VARA", "Vara Shuddhi", false, -15.0, desc))
            score -= 15.0
        } else if (ruleProfile.favorableVaras.contains(vara)) {
            supportingFactors.add(
                MuhurtaFactor(
                    category = MuhurtaFactorCategory.PANCHANG_VARA,
                    title = "Auspicious Vara (${vara.sanskritName})",
                    description = "${vara.sanskritName} is naturally favorable for ${ruleProfile.activityType.englishName}.",
                    isSupporting = true
                )
            )
            ruleEvidence.add(MuhurtaRuleEvidence("RULE_VARA", "Vara Shuddhi", true, 12.0, "Vara ${vara.sanskritName} is favorable."))
            score += 12.0
        }

        // Travel Disha Shool check
        if (ruleProfile.activityType == MuhurtaActivityType.TRAVEL && ruleProfile.prohibitedDirectionsForTravel.containsKey(vara)) {
            val dir = ruleProfile.prohibitedDirectionsForTravel[vara]!!
            cautionFactors.add(
                MuhurtaFactor(
                    category = MuhurtaFactorCategory.ACTIVITY_RULE,
                    title = "Disha Shool Caution ($dir)",
                    description = "Travel towards $dir is prohibited on ${vara.sanskritName}. If unavoidable, consume prescribed antidote (curd/jaggery/ghee) before departure.",
                    isSupporting = false,
                    classicalReference = "Vedic Yatra Shastra"
                )
            )
            ruleEvidence.add(MuhurtaRuleEvidence("RULE_DISHA_SHOOL", "Disha Shool Direction", false, -10.0, "Directional caution towards $dir on ${vara.sanskritName}."))
        }

        // 4. Evaluate Nakshatra Compatibility
        if (ruleProfile.favorableNakshatras.contains(nakshatra)) {
            supportingFactors.add(
                MuhurtaFactor(
                    category = MuhurtaFactorCategory.PANCHANG_NAKSHATRA,
                    title = "Favorable Nakshatra (${nakshatra.sanskritName})",
                    description = "${nakshatra.sanskritName} (${nakshatra.name}) is an auspicious lunar mansion for this undertaking.",
                    isSupporting = true,
                    classicalReference = "Kalaprakasika"
                )
            )
            ruleEvidence.add(MuhurtaRuleEvidence("RULE_NAKSHATRA", "Nakshatra Shuddhi", true, 18.0, "Nakshatra ${nakshatra.sanskritName} matches activity."))
            score += 18.0
        }

        // 5. Evaluate Karana (Bhadra/Vishti check)
        if (isVishtiKarana) {
            cautionFactors.add(
                MuhurtaFactor(
                    category = MuhurtaFactorCategory.PANCHANG_KARANA,
                    title = "Vishti (Bhadra) Karana Active (भद्रा)",
                    description = "Bhadra Karana is strictly inauspicious for commencement of travel, commercial deals, weddings, and Griha Pravesha.",
                    isSupporting = false,
                    classicalReference = "Muhurta Darpana"
                )
            )
            ruleEvidence.add(MuhurtaRuleEvidence("RULE_BHADRA", "Bhadra Karana Shuddhi", false, -30.0, "Bhadra is active."))
            score -= 30.0
        }

        // 6. Evaluate Yoga Shuddhi
        if (ActivityContextResolver.INAUSPICIOUS_YOGAS.contains(yogaIndex)) {
            cautionFactors.add(
                MuhurtaFactor(
                    category = MuhurtaFactorCategory.PANCHANG_YOGA,
                    title = "Cautionary Nitya Yoga (${panchang.yoga.name})",
                    description = "${panchang.yoga.name} is classified among the cautionary daily yogas in Panchang.",
                    isSupporting = false
                )
            )
            ruleEvidence.add(MuhurtaRuleEvidence("RULE_YOGA", "Yoga Shuddhi", false, -10.0, "Yoga ${panchang.yoga.name} is inauspicious."))
            score -= 10.0
        }

        // 7. Evaluate Overlap with Inauspicious Intervals (Rahukaal, Yamaganda, Gulika, Durmuhurta)
        inauspiciousIntervals.forEach { interval ->
            if (TimeWindowCalculator.hasOverlap(window.startTime, window.endTime, interval.startTime, interval.endTime)) {
                val penalty = when (interval.name) {
                    "Rahukaal" -> 35.0
                    "Yamaganda Kaal" -> 25.0
                    "Gulika Kaal" -> 15.0
                    else -> 20.0
                }
                cautionFactors.add(
                    MuhurtaFactor(
                        category = MuhurtaFactorCategory.CAUTION_PERIOD,
                        title = "Overlaps with ${interval.sanskritName} (${interval.name})",
                        description = interval.reason,
                        isSupporting = false
                    )
                )
                ruleEvidence.add(
                    MuhurtaRuleEvidence(
                        ruleId = "OVERLAP_${interval.name.uppercase()}",
                        ruleName = "${interval.name} Overlap Exclusion",
                        passed = false,
                        weight = -penalty,
                        details = "Window spans during ${interval.name} (${interval.startTime.toLocalTime()} - ${interval.endTime.toLocalTime()})."
                    )
                )
                score -= penalty
            }
        }

        // 8. Evaluate Personal Bala (Tara Bala & Chandra Bala) if provided
        if (personalBala != null) {
            if (personalBala.isTaraFavorable) {
                supportingFactors.add(
                    MuhurtaFactor(
                        category = MuhurtaFactorCategory.PERSONAL_TARA_BALA,
                        title = "Personal Tara Bala: ${personalBala.taraName} (शुभ)",
                        description = "Transit Moon is in ${personalBala.taraName} from your birth star (${personalBala.janmaNakshatra.sanskritName}), ensuring protective energy.",
                        isSupporting = true
                    )
                )
                ruleEvidence.add(MuhurtaRuleEvidence("RULE_TARA_BALA", "Personal Tara Bala", true, 20.0, personalBala.taraName))
                score += 20.0
            } else {
                val penalty = if (personalBala.taraIndex == 7) 30.0 else 15.0
                cautionFactors.add(
                    MuhurtaFactor(
                        category = MuhurtaFactorCategory.PERSONAL_TARA_BALA,
                        title = "Personal Tara Caution: ${personalBala.taraName}",
                        description = "Transit Moon falls in ${personalBala.taraName} (Star ${personalBala.taraIndex} of 9), requiring extra vigilance and remedial sankalpa.",
                        isSupporting = false
                    )
                )
                ruleEvidence.add(MuhurtaRuleEvidence("RULE_TARA_BALA", "Personal Tara Bala", false, -penalty, personalBala.taraName))
                score -= penalty
            }

            if (personalBala.isChandrashtama) {
                cautionFactors.add(
                    MuhurtaFactor(
                        category = MuhurtaFactorCategory.PERSONAL_CHANDRA_BALA,
                        title = "Chandrashtama Alert (चन्द्राष्टम दोष)",
                        description = "Transit Moon is in the 8th house from your natal Moon (${personalBala.natalMoonRashi.sanskritName}). Classical texts strongly advise postponing high-stakes beginnings.",
                        isSupporting = false,
                        classicalReference = "Jataka Parijata"
                    )
                )
                ruleEvidence.add(MuhurtaRuleEvidence("RULE_CHANDRASHTAMA", "Chandrashtama Check", false, -35.0, "Transit Moon in 8th house from natal Moon."))
                score -= 35.0
            } else if (personalBala.isChandraBalaFavorable) {
                supportingFactors.add(
                    MuhurtaFactor(
                        category = MuhurtaFactorCategory.PERSONAL_CHANDRA_BALA,
                        title = "Favorable Chandra Bala (${personalBala.chandraBalaHouse}th House)",
                        description = "Transit Moon is positioned auspiciously in house ${personalBala.chandraBalaHouse} from your natal Moon.",
                        isSupporting = true
                    )
                )
                ruleEvidence.add(MuhurtaRuleEvidence("RULE_CHANDRA_BALA", "Chandra Bala Strength", true, 15.0, "${personalBala.chandraBalaHouse}th House"))
                score += 15.0
            }
        }

        // 9. Derive Evaluation State & Rank Tier
        val normalizedScore = score.coerceIn(0.0, 100.0)
        val hasSevereCaution = cautionFactors.any {
            it.title.contains("Rahukaal", ignoreCase = true) ||
            it.title.contains("Bhadra", ignoreCase = true) ||
            it.title.contains("Chandrashtama", ignoreCase = true)
        }

        val evalState = when {
            hasSevereCaution || normalizedScore < 35.0 -> MuhurtaEvaluationState.CAUTION
            normalizedScore >= 75.0 && cautionFactors.isEmpty() -> MuhurtaEvaluationState.FAVORABLE
            normalizedScore >= 60.0 -> MuhurtaEvaluationState.CONDITIONALLY_FAVORABLE
            normalizedScore >= 40.0 -> MuhurtaEvaluationState.MIXED
            else -> MuhurtaEvaluationState.CAUTION
        }

        val rankTier = when {
            evalState == MuhurtaEvaluationState.FAVORABLE && normalizedScore >= 85.0 -> MuhurtaRankTier.BEST_AVAILABLE
            evalState == MuhurtaEvaluationState.FAVORABLE -> MuhurtaRankTier.FAVORABLE
            evalState == MuhurtaEvaluationState.CONDITIONALLY_FAVORABLE -> MuhurtaRankTier.CONDITIONALLY_FAVORABLE
            evalState == MuhurtaEvaluationState.MIXED -> MuhurtaRankTier.MIXED
            else -> MuhurtaRankTier.CAUTION
        }

        val summary = buildString {
            append("${window.name} (${window.startTime.toLocalTime()} - ${window.endTime.toLocalTime()}) is evaluated as ${evalState.label}.")
            if (supportingFactors.isNotEmpty()) {
                append(" Supporting: ${supportingFactors.first().title}.")
            }
            if (cautionFactors.isNotEmpty()) {
                append(" Caution: ${cautionFactors.first().title}.")
            }
        }

        return MuhurtaCandidateWindow(
            id = window.id,
            name = window.name,
            sanskritName = window.sanskritName,
            startTime = window.startTime,
            endTime = window.endTime,
            localDate = window.localDate,
            evaluationState = evalState,
            rankTier = rankTier,
            score = normalizedScore,
            supportingFactors = supportingFactors,
            cautionFactors = cautionFactors,
            ruleEvidence = ruleEvidence,
            personalBalaContext = personalBala,
            summary = summary
        )
    }
}
