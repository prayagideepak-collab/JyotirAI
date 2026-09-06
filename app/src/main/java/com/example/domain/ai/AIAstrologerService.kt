package com.example.domain.ai

import com.example.domain.interpretation.AdvancedVedicInterpretation
import com.example.domain.interpretation.VedicInterpretationEngine
import com.example.domain.models.*
import com.example.domain.numerology.NumerologyEngine
import com.example.domain.numerology.NumerologyEngineImpl
import com.example.domain.numerology.NumerologyMethodology
import com.example.domain.numerology.NumerologyResult
import java.time.LocalDate

/**
 * Service orchestrating AI Astrologer responses backed strictly by deterministic calculations.
 */
interface AIAstrologerService {

    /**
     * Answers a natural language user question using verified astrological & numerological context.
     */
    suspend fun answerQuestion(
        question: String,
        profile: UserProfile? = null,
        astrologyProfile: AstrologyProfile? = null,
        dashaTimeline: DashaTimeline? = null,
        transitSnapshot: TransitSnapshot? = null,
        panchangSnapshot: PanchangSnapshot? = null,
        yogaDoshaSnapshot: YogaDoshaSnapshot? = null
    ): AIAstrologerResult

    /**
     * Invalidates cached responses for a profile.
     */
    fun invalidateCache(profileId: String?)

    /**
     * Clears all AI astrologer response cache.
     */
    fun clearCache()
}

class AIAstrologerServiceImpl(
    private val numerologyEngine: NumerologyEngine = NumerologyEngineImpl()
) : AIAstrologerService {

    private val responseCache = mutableMapOf<String, AIAstrologerResult>()

    override suspend fun answerQuestion(
        question: String,
        profile: UserProfile?,
        astrologyProfile: AstrologyProfile?,
        dashaTimeline: DashaTimeline?,
        transitSnapshot: TransitSnapshot?,
        panchangSnapshot: PanchangSnapshot?,
        yogaDoshaSnapshot: YogaDoshaSnapshot?
    ): AIAstrologerResult {
        val (intent, confidence) = QuestionIntentClassifier.classify(question)
        val profileId = profile?.id
        val cacheKey = buildCacheKey(profileId, question, intent)

        synchronized(responseCache) {
            responseCache[cacheKey]?.let { return it }
        }

        val isPersonalized = profile != null && astrologyProfile != null
        val responseId = "AI-ASTRO-${System.currentTimeMillis()}-${(1000..9999).random()}"
        val verifiedEvidences = mutableListOf<AIAstrologerEvidence>()
        val missingContextNotes = mutableListOf<String>()
        val limitations = mutableListOf<String>()

        // Deterministic interpretation context
        val vedicInterpretation: AdvancedVedicInterpretation? = if (astrologyProfile != null) {
            VedicInterpretationEngine.interpret(
                profile = astrologyProfile,
                dashaTimeline = dashaTimeline,
                transitSnapshot = transitSnapshot,
                panchangSnapshot = panchangSnapshot
            )
        } else null

        // Numerology context where relevant or when requested
        val numerologyResult: NumerologyResult? = if (profile != null) {
            numerologyEngine.calculateForProfile(profile, NumerologyMethodology.CHALDEAN)
        } else null

        val response = when (intent) {
            AstrologerIntent.CAREER_AND_PROFESSION -> buildCareerResponse(
                question, profile, astrologyProfile, dashaTimeline, transitSnapshot, vedicInterpretation, verifiedEvidences, missingContextNotes
            )
            AstrologerIntent.EDUCATION_AND_STUDIES -> buildEducationResponse(
                question, profile, astrologyProfile, dashaTimeline, vedicInterpretation, verifiedEvidences, missingContextNotes
            )
            AstrologerIntent.MARRIAGE_AND_RELATIONSHIPS -> buildMarriageResponse(
                question, profile, astrologyProfile, dashaTimeline, transitSnapshot, yogaDoshaSnapshot, vedicInterpretation, verifiedEvidences, missingContextNotes
            )
            AstrologerIntent.FINANCES_AND_WEALTH -> buildFinanceResponse(
                question, profile, astrologyProfile, dashaTimeline, transitSnapshot, vedicInterpretation, verifiedEvidences, missingContextNotes
            )
            AstrologerIntent.DASHA_EXPLANATION -> buildDashaResponse(
                question, profile, astrologyProfile, dashaTimeline, verifiedEvidences, missingContextNotes
            )
            AstrologerIntent.TRANSIT_GOCHAR_EXPLANATION -> buildTransitResponse(
                question, profile, astrologyProfile, transitSnapshot, verifiedEvidences, missingContextNotes
            )
            AstrologerIntent.YOGA_AND_DOSHA_EXPLANATION -> buildYogaDoshaResponse(
                question, profile, astrologyProfile, yogaDoshaSnapshot, verifiedEvidences, missingContextNotes
            )
            AstrologerIntent.PANCHANG_AND_TIMING, AstrologerIntent.MUHURTA_GUIDANCE -> buildPanchangMuhurtaResponse(
                question, profile, panchangSnapshot, verifiedEvidences, missingContextNotes
            )
            AstrologerIntent.NUMEROLOGY_ANALYSIS -> buildNumerologyResponse(
                question, profile, numerologyResult, verifiedEvidences, missingContextNotes
            )
            AstrologerIntent.COMPATIBILITY_GUIDANCE -> buildCompatibilityGuidanceResponse(
                question, profile, verifiedEvidences, missingContextNotes
            )
            AstrologerIntent.REMEDY_AND_GUIDANCE -> buildRemedyResponse(
                question, profile, astrologyProfile, dashaTimeline, yogaDoshaSnapshot, verifiedEvidences, missingContextNotes
            )
            AstrologerIntent.TODAY_GUIDANCE -> buildTodayGuidanceResponse(
                question, profile, astrologyProfile, transitSnapshot, panchangSnapshot, verifiedEvidences, missingContextNotes
            )
            AstrologerIntent.GENERAL_ASTROLOGY_EXPLANATION -> buildGeneralAstrologyResponse(
                question, verifiedEvidences
            )
            AstrologerIntent.GENERAL_HOROSCOPE -> buildGeneralHoroscopeResponse(
                question, profile, astrologyProfile, dashaTimeline, transitSnapshot, vedicInterpretation, numerologyResult, verifiedEvidences, missingContextNotes
            )
        }

        val finalResult = response.copy(
            responseId = responseId,
            profileId = profileId,
            profileName = profile?.name ?: "अतिथि (Guest)",
            isPersonalized = isPersonalized,
            userQuestion = question,
            detectedIntent = intent,
            intentConfidence = confidence,
            verifiedEvidences = verifiedEvidences,
            missingContextNotes = missingContextNotes,
            limitations = limitations
        )

        synchronized(responseCache) {
            responseCache[cacheKey] = finalResult
        }

        return finalResult
    }

    override fun invalidateCache(profileId: String?) {
        if (profileId == null) return
        synchronized(responseCache) {
            responseCache.keys.filter { it.startsWith("pid=$profileId|") }.forEach { responseCache.remove(it) }
        }
    }

    override fun clearCache() {
        synchronized(responseCache) {
            responseCache.clear()
        }
    }

    private fun buildCacheKey(profileId: String?, question: String, intent: AstrologerIntent): String {
        return "pid=${profileId ?: "anon"}|intent=${intent.code}|q=${question.trim().lowercase()}"
    }

    // --- Specific Intent Response Builders ---

    private fun buildCareerResponse(
        question: String,
        profile: UserProfile?,
        astrologyProfile: AstrologyProfile?,
        dashaTimeline: DashaTimeline?,
        transitSnapshot: TransitSnapshot?,
        interpretation: AdvancedVedicInterpretation?,
        evidences: MutableList<AIAstrologerEvidence>,
        missing: MutableList<String>
    ): AIAstrologerResult {
        if (astrologyProfile == null) {
            missing.add("जन्म कुंडली का विवरण उपलब्ध नहीं है।")
            return AIAstrologerResult(
                responseId = "",
                profileId = null,
                profileName = "",
                isPersonalized = false,
                userQuestion = question,
                detectedIntent = AstrologerIntent.CAREER_AND_PROFESSION,
                intentConfidence = 0.88,
                mainHeadlineHindi = "करियर एवं आजीविका का सामान्य वैदिक सिद्धांत",
                simpleMeaningHindi = "वैदिक ज्योतिष में करियर का विचार लग्न, दशम भाव (कर्म भाव), दशमेश, दशमांश (D10) चार्ट तथा सक्रिय विंशोत्तरी दशा से किया जाता है।",
                currentInfluenceHindi = "व्यक्तिगत विश्लेषण के लिए कृपया अपनी जन्म प्रोफाइल का चयन करें।",
                cautionsHindi = "सामान्य नियमों के आधार पर कोई भी व्यक्तिगत निर्णय न लें।",
                practicalRemediesHindi = listOf("दैनिक कर्मठता व अनुशासन बनाए रखें", "शनिवार को कर्म कारक शनि देव का स्मरण करें")
            )
        }

        val tenthHousePlanets = astrologyProfile.rashiChart.getPlanetsInHouse(10)
        val lagnaRashi = Rashi.fromIndex(astrologyProfile.lagnaSignIndex)
        val tenthSignIndex = (astrologyProfile.lagnaSignIndex + 9) % 12
        val tenthRashi = Rashi.fromIndex(tenthSignIndex)
        val tenthLord = tenthRashi.lord
        val activeMahadasha = dashaTimeline?.currentMahadasha?.planet?.lord ?: "अज्ञात"

        evidences.add(
            AIAstrologerEvidence(
                factorName = "दशम भाव (कर्म भाव)",
                sourceEngine = "Phase 2 Kundli (D1)",
                calculatedValue = "दशम राशि: ${tenthRashi.sanskritName} (${tenthRashi.englishName}), दशमेश: $tenthLord",
                astronomicalBasis = "निरयण भाव गणना (Whole Sign)"
            )
        )

        if (dashaTimeline?.currentMahadasha != null) {
            evidences.add(
                AIAstrologerEvidence(
                    factorName = "सक्रिय महादशा",
                    sourceEngine = "Phase 4 Vimshottari Dasha",
                    calculatedValue = "महादशा स्वामी: $activeMahadasha",
                    astronomicalBasis = "विंशोत्तरी दशा चक्र (१२० वर्ष)"
                )
            )
        }

        val planetsIn10Text = if (tenthHousePlanets.isNotEmpty()) {
            "दशम भाव में ${tenthHousePlanets.joinToString(", ") { "${it.sanskritName} (${it.dignity.sanskritName})" }} स्थित हैं।"
        } else {
            "दशम भाव पर उसके स्वामी $tenthLord का आधिपत्य है।"
        }

        val d10Text = interpretation?.divisionalAnalysis?.d10Summary
            ?: "दशमांश विश्लेषण के अनुसार कार्यक्षेत्र में अनुशासन और निरंतरता आवश्यक है।"

        val sections = listOf(
            AIAstrologerSection(
                sectionId = "career_status",
                sectionTitleHindi = "मुख्य स्थिति (कर्म भाव)",
                narrationTextHindi = "आपकी कुंडली में लग्न ${lagnaRashi.sanskritName} है और दशम भाव में ${tenthRashi.sanskritName} राशि है। $planetsIn10Text",
                displayMarkdownHindi = "**लग्न**: ${lagnaRashi.sanskritName}\n**दशम भाव (कर्म)**: ${tenthRashi.sanskritName} (${tenthRashi.englishName})\n**दशमेश**: $tenthLord\n$planetsIn10Text"
            ),
            AIAstrologerSection(
                sectionId = "career_dasha",
                sectionTitleHindi = "वर्तमान दशा प्रभाव",
                narrationTextHindi = "वर्तमान में $activeMahadasha की महादशा का प्रभाव है जो कार्यक्षेत्र में आपकी प्राथमिकताओं को दिशा दे रहा है।",
                displayMarkdownHindi = "**सक्रिय महादशा**: $activeMahadasha\n\n$d10Text"
            )
        )

        return AIAstrologerResult(
            responseId = "",
            profileId = profile?.id,
            profileName = profile?.name ?: "",
            isPersonalized = true,
            userQuestion = question,
            detectedIntent = AstrologerIntent.CAREER_AND_PROFESSION,
            intentConfidence = 0.90,
            mainHeadlineHindi = "करियर एवं कार्यक्षेत्र विश्लेषण — ${profile?.name ?: ""}",
            simpleMeaningHindi = "आपकी कुंडली में दशम भाव ${tenthRashi.sanskritName} राशि में है जिसके स्वामी $tenthLord हैं। $planetsIn10Text",
            currentInfluenceHindi = "वर्तमान $activeMahadasha महादशा के अंतर्गत कार्यक्षेत्र में कौशल उन्नयन और उत्तरदायित्वों का निर्वहन महत्वपूर्ण है।",
            cautionsHindi = "कार्यस्थल पर अनावश्यक विवादों से बचें और योजनाओं को समयबद्ध रूप से पूरा करें।",
            practicalRemediesHindi = listOf(
                "दशमेश $tenthLord के अनुकूल दैनिक कर्म अनुशासन बनाए रखें",
                "प्रातः गायत्री मंत्र या सूर्य नमस्कार का अभ्यास करें"
            ),
            orderedSections = sections
        )
    }

    private fun buildEducationResponse(
        question: String,
        profile: UserProfile?,
        astrologyProfile: AstrologyProfile?,
        dashaTimeline: DashaTimeline?,
        interpretation: AdvancedVedicInterpretation?,
        evidences: MutableList<AIAstrologerEvidence>,
        missing: MutableList<String>
    ): AIAstrologerResult {
        if (astrologyProfile == null) {
            missing.add("जन्म कुंडली का विवरण उपलब्ध नहीं है।")
            return AIAstrologerResult(
                responseId = "",
                profileId = null,
                profileName = "",
                isPersonalized = false,
                userQuestion = question,
                detectedIntent = AstrologerIntent.EDUCATION_AND_STUDIES,
                intentConfidence = 0.88,
                mainHeadlineHindi = "शिक्षा एवं विद्या का वैदिक सिद्धांत",
                simpleMeaningHindi = "शिक्षा के लिए पंचम भाव (बुद्धि), चतुर्थ भाव (प्राथमिक विद्या), तथा गुरु व बुध की स्थिति प्रमुख मानी जाती है।",
                currentInfluenceHindi = "व्यक्तिगत विश्लेषण के लिए कृपया प्रोफाइल चुनें।",
                cautionsHindi = "एकाग्रता और नियमित स्वाध्याय पर ध्यान दें।",
                practicalRemediesHindi = listOf("प्रतिदिन सरस्वती वंदना या गायत्री मंत्र का पाठ करें", "उत्तर-पूर्व दिशा में अध्ययन स्थल रखें")
            )
        }

        val fifthSignIndex = (astrologyProfile.lagnaSignIndex + 4) % 12
        val fifthRashi = Rashi.fromIndex(fifthSignIndex)
        val fifthLord = fifthRashi.lord
        val jupiterPos = astrologyProfile.planetPositions.firstOrNull { it.planet.equals("Jupiter", ignoreCase = true) }
        val mercuryPos = astrologyProfile.planetPositions.firstOrNull { it.planet.equals("Mercury", ignoreCase = true) }

        evidences.add(
            AIAstrologerEvidence(
                factorName = "पंचम भाव (बुद्धि एवं विद्या)",
                sourceEngine = "Phase 2 Kundli (D1)",
                calculatedValue = "पंचम राशि: ${fifthRashi.sanskritName}, पंचमेश: $fifthLord",
                astronomicalBasis = "निरयण भाव गणना"
            )
        )

        val meaning = "आपकी कुंडली में पंचम भाव ${fifthRashi.sanskritName} राशि में है और विद्या कारक गुरु ${jupiterPos?.sign ?: "अज्ञात"} में तथा बुध ${mercuryPos?.sign ?: "अज्ञात"} में स्थित हैं।"

        return AIAstrologerResult(
            responseId = "",
            profileId = profile?.id,
            profileName = profile?.name ?: "",
            isPersonalized = true,
            userQuestion = question,
            detectedIntent = AstrologerIntent.EDUCATION_AND_STUDIES,
            intentConfidence = 0.88,
            mainHeadlineHindi = "शिक्षा एवं बौद्धिक विकास मार्गदर्शन",
            simpleMeaningHindi = meaning,
            currentInfluenceHindi = "पंचमेश $fifthLord और ज्ञान कारक ग्रहों की स्थिति विश्लेषणात्मक व व्यावहारिक अध्ययन के अनुकूल है।",
            cautionsHindi = "अध्ययन के समय मन के भटकाव और डिजिटल विचलितताओं से बचें।",
            practicalRemediesHindi = listOf(
                "बुधवार को भगवान गणेश को दूर्वा अर्पित करें",
                "प्रातः अध्ययन से पूर्व 'ॐ ऐं सरस्वत्यै नमः' का जप करें"
            )
        )
    }

    private fun buildMarriageResponse(
        question: String,
        profile: UserProfile?,
        astrologyProfile: AstrologyProfile?,
        dashaTimeline: DashaTimeline?,
        transitSnapshot: TransitSnapshot?,
        yogaDoshaSnapshot: YogaDoshaSnapshot?,
        interpretation: AdvancedVedicInterpretation?,
        evidences: MutableList<AIAstrologerEvidence>,
        missing: MutableList<String>
    ): AIAstrologerResult {
        if (astrologyProfile == null) {
            missing.add("जन्म कुंडली का विवरण उपलब्ध नहीं है।")
            return AIAstrologerResult(
                responseId = "",
                profileId = null,
                profileName = "",
                isPersonalized = false,
                userQuestion = question,
                detectedIntent = AstrologerIntent.MARRIAGE_AND_RELATIONSHIPS,
                intentConfidence = 0.88,
                mainHeadlineHindi = "विवाह एवं वैवाहिक सामंजस्य का वैदिक सिद्धांत",
                simpleMeaningHindi = "विवाह व दांपत्य जीवन के लिए सप्तम भाव (कलत्र भाव), सप्तमेश, शुक्र/गुरु और D9 नवांश कुंडली का विश्लेषण किया जाता है।",
                currentInfluenceHindi = "सटीक व्यक्तिगत फलित के लिए कृपया जन्म कुंडली लोड करें।",
                cautionsHindi = "रिश्तों में आपसी समझ, संवाद और धैर्य सर्वोपरि हैं।",
                practicalRemediesHindi = listOf("पारस्परिक सम्मान व सामंजस्य बनाए रखें", "शुक्रवार को सात्विक आचरण रखें")
            )
        }

        val seventhSignIndex = (astrologyProfile.lagnaSignIndex + 6) % 12
        val seventhRashi = Rashi.fromIndex(seventhSignIndex)
        val seventhLord = seventhRashi.lord
        val venusPos = astrologyProfile.planetPositions.firstOrNull { it.planet.equals("Venus", ignoreCase = true) }

        evidences.add(
            AIAstrologerEvidence(
                factorName = "सप्तम भाव (दांपत्य भाव)",
                sourceEngine = "Phase 2 Kundli (D1)",
                calculatedValue = "सप्तम राशि: ${seventhRashi.sanskritName}, सप्तमेश: $seventhLord, शुक्र: ${venusPos?.sign ?: "N/A"}",
                astronomicalBasis = "निरयण लग्न चक्र"
            )
        )

        val manglikDosha = yogaDoshaSnapshot?.detectedDoshas?.firstOrNull { it.name.contains("Manglik", ignoreCase = true) }
        val manglikNote = if (manglikDosha != null) {
            "कुंडली में मांगलिक प्रभाव का संकेत है: ${manglikDosha.evidence}।"
        } else {
            "सप्तम भाव संतुलित अवस्था में है।"
        }

        return AIAstrologerResult(
            responseId = "",
            profileId = profile?.id,
            profileName = profile?.name ?: "",
            isPersonalized = true,
            userQuestion = question,
            detectedIntent = AstrologerIntent.MARRIAGE_AND_RELATIONSHIPS,
            intentConfidence = 0.90,
            mainHeadlineHindi = "विवाह एवं दांपत्य जीवन विश्लेषण",
            simpleMeaningHindi = "आपकी कुंडली में सप्तम भाव ${seventhRashi.sanskritName} राशि में है जिसके स्वामी $seventhLord हैं। $manglikNote",
            currentInfluenceHindi = "विवाह और संबंधों में आपसी विश्वास, खुला संवाद और परस्पर सहयोग से सुखद सामंजस्य बनता है।",
            cautionsHindi = "अहंकार अथवा जल्दबाजी में प्रतिक्रिया देने से बचें।",
            practicalRemediesHindi = listOf(
                "शुक्रवार को माता लक्ष्मी की स्तुति करें",
                "दांपत्य जीवन में परस्पर आदर और समझदारी रखें"
            )
        )
    }

    private fun buildFinanceResponse(
        question: String,
        profile: UserProfile?,
        astrologyProfile: AstrologyProfile?,
        dashaTimeline: DashaTimeline?,
        transitSnapshot: TransitSnapshot?,
        interpretation: AdvancedVedicInterpretation?,
        evidences: MutableList<AIAstrologerEvidence>,
        missing: MutableList<String>
    ): AIAstrologerResult {
        if (astrologyProfile == null) {
            missing.add("जन्म कुंडली का विवरण उपलब्ध नहीं है।")
            return AIAstrologerResult(
                responseId = "",
                profileId = null,
                profileName = "",
                isPersonalized = false,
                userQuestion = question,
                detectedIntent = AstrologerIntent.FINANCES_AND_WEALTH,
                intentConfidence = 0.88,
                mainHeadlineHindi = "धन एवं आर्थिक समृद्धि का वैदिक सिद्धांत",
                simpleMeaningHindi = "आर्थिक स्थिति का आकलन द्वितीय भाव (संचित धन), एकादश भाव (आय/लाभ), नवम भाव (भाग्य) एवं गुरु/शुक्र से किया जाता है।",
                currentInfluenceHindi = "व्यक्तिगत विश्लेषण के लिए कृपया प्रोफाइल का चयन करें।",
                cautionsHindi = "सट्टेबाजी और अनियोजित निवेश से बचें।",
                practicalRemediesHindi = listOf("नियमित बचत की आदत डालें", "शुक्रवार या गुरुवार को सात्विक दान करें")
            )
        }

        val secondSignIndex = (astrologyProfile.lagnaSignIndex + 1) % 12
        val secondRashi = Rashi.fromIndex(secondSignIndex)
        val secondLord = secondRashi.lord
        val eleventhSignIndex = (astrologyProfile.lagnaSignIndex + 10) % 12
        val eleventhRashi = Rashi.fromIndex(eleventhSignIndex)
        val eleventhLord = eleventhRashi.lord

        evidences.add(
            AIAstrologerEvidence(
                factorName = "द्वितीय भाव (धन) व एकादश भाव (लाभ)",
                sourceEngine = "Phase 2 Kundli (D1)",
                calculatedValue = "द्वितीयेश: $secondLord (${secondRashi.sanskritName}), एकादशेश: $eleventhLord (${eleventhRashi.sanskritName})",
                astronomicalBasis = "निरयण भाव गणना"
            )
        )

        return AIAstrologerResult(
            responseId = "",
            profileId = profile?.id,
            profileName = profile?.name ?: "",
            isPersonalized = true,
            userQuestion = question,
            detectedIntent = AstrologerIntent.FINANCES_AND_WEALTH,
            intentConfidence = 0.88,
            mainHeadlineHindi = "आर्थिक स्थिति एवं धन संचय विश्लेषण",
            simpleMeaningHindi = "आपकी कुंडली में धन भाव (द्वितीय) के स्वामी $secondLord तथा लाभ भाव (एकादश) के स्वामी $eleventhLord हैं।",
            currentInfluenceHindi = "स्थिर निवेश और योजनाबद्ध वित्तीय प्रबंधन से दीर्घकालिक संचय सुदृढ़ रहेगा।",
            cautionsHindi = "अनावश्यक दिखावे अथवा जोखिम भरे निवेश में बिना शोध के पूंजी न लगाएं।",
            practicalRemediesHindi = listOf(
                "गुरुवार को श्री विष्णु सहस्रनाम या महालक्ष्मी अष्टकम का पाठ करें",
                "धन के आदान-प्रदान में ईमानदारी और स्पष्ट हिसाब-किताब रखें"
            )
        )
    }

    private fun buildDashaResponse(
        question: String,
        profile: UserProfile?,
        astrologyProfile: AstrologyProfile?,
        dashaTimeline: DashaTimeline?,
        evidences: MutableList<AIAstrologerEvidence>,
        missing: MutableList<String>
    ): AIAstrologerResult {
        if (dashaTimeline == null || dashaTimeline.currentMahadasha == null) {
            missing.add("विंशोत्तरी दशा का विवरण उपलब्ध नहीं है।")
            return AIAstrologerResult(
                responseId = "",
                profileId = null,
                profileName = "",
                isPersonalized = false,
                userQuestion = question,
                detectedIntent = AstrologerIntent.DASHA_EXPLANATION,
                intentConfidence = 0.92,
                mainHeadlineHindi = "विंशोत्तरी दशा चक्र सिद्धांत",
                simpleMeaningHindi = "विंशोत्तरी दशा १२० वर्षों का वैदिक काल चक्र है, जिसमें प्रत्येक ग्रह की महादशा जीवन के विशेष क्षेत्रों और अनुभवों को सक्रिय करती है।",
                currentInfluenceHindi = "व्यक्तिगत दशा की जानकारी के लिए जन्म कुंडली चुनें।",
                cautionsHindi = "दशा स्वामी ग्रह के स्वभाव के अनुसार आचरण रखें।",
                practicalRemediesHindi = listOf("दशा स्वामी ग्रह के बीज मंत्र का जप करें", "सात्विक दिनचर्या अपनाएं")
            )
        }

        val maha = dashaTimeline.currentMahadasha.planet.lord
        val antar = dashaTimeline.currentAntardasha?.antardashaLord?.lord ?: "अज्ञात"
        val pratyantar = "सहायक"

        evidences.add(
            AIAstrologerEvidence(
                factorName = "सक्रिय विंशोत्तरी दशा",
                sourceEngine = "Phase 4 Dasha Engine",
                calculatedValue = "महादशा: $maha, अंतर्दशा: $antar, प्रत्यंतर्दशा: $pratyantar",
                astronomicalBasis = "जन्म नक्षत्र के चंद्रमा भुक्त अंश आधारित"
            )
        )

        return AIAstrologerResult(
            responseId = "",
            profileId = profile?.id,
            profileName = profile?.name ?: "",
            isPersonalized = true,
            userQuestion = question,
            detectedIntent = AstrologerIntent.DASHA_EXPLANATION,
            intentConfidence = 0.95,
            mainHeadlineHindi = "सक्रिय विंशोत्तरी दशा: $maha महादशा — $antar अंतर्दशा",
            simpleMeaningHindi = "वर्तमान में आपकी कुंडली में $maha ग्रह की महादशा में $antar की अंतर्दशा चल रही है।",
            currentInfluenceHindi = "$maha और $antar का संयुक्त प्रभाव आपके जीवन के प्रमुख निर्णयों, मानसिक दृष्टिकोण और प्राथमिकताओं को संचालित कर रहा है।",
            cautionsHindi = "$maha ग्रह के कारकतत्वों का सम्मान करें और इस अवधि में संयम से काम लें।",
            practicalRemediesHindi = listOf(
                "$maha ग्रह से संबंधित सात्विक दान अथवा मंत्र जप करें",
                "नियमित ध्यान व आत्म-चिंतन करें"
            )
        )
    }

    private fun buildTransitResponse(
        question: String,
        profile: UserProfile?,
        astrologyProfile: AstrologyProfile?,
        transitSnapshot: TransitSnapshot?,
        evidences: MutableList<AIAstrologerEvidence>,
        missing: MutableList<String>
    ): AIAstrologerResult {
        if (astrologyProfile == null || transitSnapshot == null) {
            missing.add("गोचर अथवा जन्म कुंडली का विवरण उपलब्ध नहीं है।")
            return AIAstrologerResult(
                responseId = "",
                profileId = null,
                profileName = "",
                isPersonalized = false,
                userQuestion = question,
                detectedIntent = AstrologerIntent.TRANSIT_GOCHAR_EXPLANATION,
                intentConfidence = 0.92,
                mainHeadlineHindi = "ग्रह गोचर (Transit) का वैदिक सिद्धांत",
                simpleMeaningHindi = "गोचर का अर्थ है ग्रहों का वर्तमान राशि भ्रमण। इसका फलित जन्मकालीन चंद्रमा और लग्न के सापेक्ष आंका जाता है।",
                currentInfluenceHindi = "सटीक गोचर फलित के लिए अपनी जन्म कुंडली लोड करें।",
                cautionsHindi = "गोचर का फल स्थायी नहीं होता, यह समयानुसार परिवर्तित होता रहता है।",
                practicalRemediesHindi = listOf("दैनिक शांति हेतु पंचांग व शुभ समय का ध्यान रखें")
            )
        }

        val moonSign = astrologyProfile.moonSign
        val saturnPos = transitSnapshot.positions.firstOrNull { it.planet.equals("Saturn", ignoreCase = true) }
        val jupiterPos = transitSnapshot.positions.firstOrNull { it.planet.equals("Jupiter", ignoreCase = true) }

        evidences.add(
            AIAstrologerEvidence(
                factorName = "गोचर स्थिति (Transit)",
                sourceEngine = "Phase 5 Transit Engine",
                calculatedValue = "जन्म चंद्र राशि: $moonSign, गोचर शनि: ${saturnPos?.sign ?: "N/A"}, गोचर गुरु: ${jupiterPos?.sign ?: "N/A"}",
                astronomicalBasis = "स्विट्जरलैंड एफिमेरिस निरयण निर्देशांक"
            )
        )

        return AIAstrologerResult(
            responseId = "",
            profileId = profile?.id,
            profileName = profile?.name ?: "",
            isPersonalized = true,
            userQuestion = question,
            detectedIntent = AstrologerIntent.TRANSIT_GOCHAR_EXPLANATION,
            intentConfidence = 0.92,
            mainHeadlineHindi = "वर्तमान ग्रह गोचर एवं चंद्र राशि प्रभाव",
            simpleMeaningHindi = "आपकी जन्म चंद्र राशि $moonSign है। वर्तमान में गुरु ${jupiterPos?.sign ?: ""} में तथा शनि ${saturnPos?.sign ?: ""} में गोचर कर रहे हैं।",
            currentInfluenceHindi = "गोचर ग्रह आपके वर्तमान वातावरण, अवसरों और तात्कालिक परिस्थितियों को प्रभावित कर रहे हैं।",
            cautionsHindi = "महत्वपूर्ण निर्णय लेते समय तात्कालिक उत्तेजना से बचें।",
            practicalRemediesHindi = listOf(
                "शनिवार को पीपल के नीचे दीप प्रज्वलित करें",
                "गुरुवार को पीली वस्तुओं का दान अथवा सात्विक आहार लें"
            )
        )
    }

    private fun buildYogaDoshaResponse(
        question: String,
        profile: UserProfile?,
        astrologyProfile: AstrologyProfile?,
        yogaDoshaSnapshot: YogaDoshaSnapshot?,
        evidences: MutableList<AIAstrologerEvidence>,
        missing: MutableList<String>
    ): AIAstrologerResult {
        if (yogaDoshaSnapshot == null) {
            missing.add("योग एवं दोष विश्लेषण डेटा उपलब्ध नहीं है।")
            return AIAstrologerResult(
                responseId = "",
                profileId = null,
                profileName = "",
                isPersonalized = false,
                userQuestion = question,
                detectedIntent = AstrologerIntent.YOGA_AND_DOSHA_EXPLANATION,
                intentConfidence = 0.90,
                mainHeadlineHindi = "वैदिक योग एवं दोष सिद्धांत",
                simpleMeaningHindi = "कुंडली में शुभ ग्रहों के संबंध से राजयोग, धनयोग आदि बनते हैं, जबकि पाप ग्रहों के विशिष्ट विन्यास से दोष निर्मित होते हैं।",
                currentInfluenceHindi = "व्यक्तिगत विश्लेषण के लिए कुंडली चुनें।",
                cautionsHindi = "किसी भी दोष से भयभीत न हों; सात्विक कर्म और उपाय से शांति संभव है।",
                practicalRemediesHindi = listOf("नियमित शिव उपासना व महामृत्युंजय मंत्र का पाठ करें")
            )
        }

        val yogas = yogaDoshaSnapshot.detectedYogas
        val doshas = yogaDoshaSnapshot.detectedDoshas

        evidences.add(
            AIAstrologerEvidence(
                factorName = "योग एवं दोष गणना",
                sourceEngine = "Phase 6 Yoga/Dosha Engine",
                calculatedValue = "शुभ योग: ${yogas.size} (${yogas.take(2).joinToString { it.name }}), दोष: ${doshas.size}",
                astronomicalBasis = "पाराशरी शास्त्रीय नियम"
            )
        )

        val yogaText = if (yogas.isNotEmpty()) {
            "आपकी कुंडली में ${yogas.take(3).joinToString(", ") { it.name }} जैसे शुभ योग विद्यमान हैं।"
        } else {
            "कुंडली में सामान्य संतुलित ग्रह विन्यास है।"
        }

        val doshaText = if (doshas.isNotEmpty()) {
            "प्रमुख दोष/सतर्कता बिंदु: ${doshas.joinToString(", ") { it.name }}।"
        } else {
            "कुंडली में कोई गंभीर शास्त्रीय दोष उपस्थित नहीं है।"
        }

        return AIAstrologerResult(
            responseId = "",
            profileId = profile?.id,
            profileName = profile?.name ?: "",
            isPersonalized = true,
            userQuestion = question,
            detectedIntent = AstrologerIntent.YOGA_AND_DOSHA_EXPLANATION,
            intentConfidence = 0.92,
            mainHeadlineHindi = "कुंडली के प्रमुख योग एवं शास्त्रीय विश्लेषण",
            simpleMeaningHindi = "$yogaText $doshaText",
            currentInfluenceHindi = "शुभ योगों की ऊर्जा जीवन में प्रगति देती है जबकि चिन्हित दोषों के प्रति सतर्कता और संयम आवश्यक है।",
            cautionsHindi = "दोषों के प्रभाव को कम करने के लिए सात्विक दिनचर्या और नैतिक आचरण बनाए रखें।",
            practicalRemediesHindi = listOf(
                "प्रतिदिन ॐ नमः शिवाय का १०८ बार जप करें",
                "जरूरतमंदों की सहायता करें और नियमित ध्यान करें"
            )
        )
    }

    private fun buildPanchangMuhurtaResponse(
        question: String,
        profile: UserProfile?,
        panchangSnapshot: PanchangSnapshot?,
        evidences: MutableList<AIAstrologerEvidence>,
        missing: MutableList<String>
    ): AIAstrologerResult {
        if (panchangSnapshot == null) {
            missing.add("आज का पंचांग डेटा उपलब्ध नहीं है।")
            return AIAstrologerResult(
                responseId = "",
                profileId = null,
                profileName = "",
                isPersonalized = false,
                userQuestion = question,
                detectedIntent = AstrologerIntent.PANCHANG_AND_TIMING,
                intentConfidence = 0.92,
                mainHeadlineHindi = "पंचांग शुद्धि एवं मुहूर्त का वैदिक सिद्धांत",
                simpleMeaningHindi = "पंचांग के पांच अंग हैं: तिथि, वार, नक्षत्र, योग और करण। शुभ मुहूर्त में किए गए कार्य में सफलता की संभावना बढ़ती है।",
                currentInfluenceHindi = "राहुकाल में शुभ कार्यों की शुरुआत से बचना चाहिए, जबकि अभिजित मुहूर्त सर्वमान्य शुभ माना जाता है।",
                cautionsHindi = "अशुभ काल (जैसे राहुकाल या भद्रा) में नवीन कार्य प्रारंभ न करें।",
                practicalRemediesHindi = listOf("प्रातः काल पंचांग का श्रवण करें", "शुभ कार्य से पहले गणेश स्मरण करें")
            )
        }

        val p = panchangSnapshot
        val tithiStr = p.tithi.hindiName
        val nakStr = p.nakshatra.nakshatra.sanskritName
        val varaStr = p.vara.hindiName
        val yogaStr = p.yoga.hindiName
        val rahukaalStr = if (p.muhurta?.rahukaal != null) "${p.muhurta.rahukaal.start.toLocalTime()} - ${p.muhurta.rahukaal.end.toLocalTime()}" else "N/A"
        val abhijitStr = if (p.muhurta?.abhijitMuhurta != null) "${p.muhurta.abhijitMuhurta.start.toLocalTime()} - ${p.muhurta.abhijitMuhurta.end.toLocalTime()}" else "N/A"

        evidences.add(
            AIAstrologerEvidence(
                factorName = "दैनिक पंचांग",
                sourceEngine = "Phase 9 Panchang Engine",
                calculatedValue = "तिथि: $tithiStr, नक्षत्र: $nakStr, वार: $varaStr, राहुकाल: $rahukaalStr",
                astronomicalBasis = "सूर्य-चंद्र निरयण देशांतर"
            )
        )

        return AIAstrologerResult(
            responseId = "",
            profileId = profile?.id,
            profileName = profile?.name ?: "",
            isPersonalized = false,
            userQuestion = question,
            detectedIntent = AstrologerIntent.PANCHANG_AND_TIMING,
            intentConfidence = 0.95,
            mainHeadlineHindi = "आज का पंचांग एवं शुभ मुहूर्त विवरण",
            simpleMeaningHindi = "आज $varaStr को $tithiStr तिथि, $nakStr नक्षत्र तथा $yogaStr योग है।",
            currentInfluenceHindi = "अभिजित मुहूर्त: $abhijitStr तक। राहुकाल: $rahukaalStr तक।",
            cautionsHindi = "राहुकाल ($rahukaalStr) के दौरान नए सौदे या शुभ कार्य प्रारंभ न करें।",
            practicalRemediesHindi = listOf(
                "महत्वपूर्ण कार्यों के लिए अभिजित मुहूर्त का चयन करें",
                "कार्य प्रारंभ से पहले विघ्नहर्ता गणेश का स्मरण करें"
            )
        )
    }

    private fun buildNumerologyResponse(
        question: String,
        profile: UserProfile?,
        numerologyResult: NumerologyResult?,
        evidences: MutableList<AIAstrologerEvidence>,
        missing: MutableList<String>
    ): AIAstrologerResult {
        if (numerologyResult == null || numerologyResult.birthNumber == null || numerologyResult.lifePathNumber == null) {
            missing.add("अंकशास्त्र हेतु जन्म तिथि उपलब्ध नहीं है।")
            return AIAstrologerResult(
                responseId = "",
                profileId = null,
                profileName = "",
                isPersonalized = false,
                userQuestion = question,
                detectedIntent = AstrologerIntent.NUMEROLOGY_ANALYSIS,
                intentConfidence = 0.95,
                mainHeadlineHindi = "अंकशास्त्र (Numerology) का सिद्धांत",
                simpleMeaningHindi = "अंकशास्त्र में जन्म दिवस से मूलांक (Birth Number) और कुल जन्म तिथि के योग से भाग्यांक (Life Path) निकलता है।",
                currentInfluenceHindi = "व्यक्तिगत अंक फलित के लिए प्रोफाइल चुनें या जन्म तिथि दर्ज करें।",
                cautionsHindi = "अंकों की ऊर्जा का संतुलन बनाए रखें।",
                practicalRemediesHindi = listOf("अपने अनुकूल रंगों व अंकों का विवेकपूर्ण उपयोग करें")
            )
        }

        val bNum = numerologyResult.birthNumber
        val lNum = numerologyResult.lifePathNumber
        val nameNum = numerologyResult.nameNumber

        evidences.add(
            AIAstrologerEvidence(
                factorName = "मूलांक एवं भाग्यांक (Chaldean/Vedic)",
                sourceEngine = "Phase 12 Numerology Engine",
                calculatedValue = "मूलांक: ${bNum.finalNumber} (${bNum.rulingPlanetHindi}), भाग्यांक: ${lNum.finalNumber} (${lNum.rulingPlanetHindi})",
                astronomicalBasis = "जन्म दिवस व पूर्ण तिथि योग रिडक्शन"
            )
        )

        val nameText = if (nameNum != null) {
            "तथा नामांक ${nameNum.finalNumber} (${nameNum.rulingPlanetHindi}) है।"
        } else ""

        return AIAstrologerResult(
            responseId = "",
            profileId = profile?.id,
            profileName = profile?.name ?: "",
            isPersonalized = true,
            userQuestion = question,
            detectedIntent = AstrologerIntent.NUMEROLOGY_ANALYSIS,
            intentConfidence = 0.95,
            mainHeadlineHindi = "अंकशास्त्र विश्लेषण: मूलांक ${bNum.finalNumber} | भाग्यांक ${lNum.finalNumber}",
            simpleMeaningHindi = "आपका मूलांक ${bNum.finalNumber} (${bNum.rulingPlanetHindi}) है जो आपके स्वभाव को दर्शाता है, और भाग्यांक ${lNum.finalNumber} (${lNum.rulingPlanetHindi}) है जो आपकी जीवन दिशा को इंगित करता है $nameText",
            currentInfluenceHindi = "अनुकूल अंक: ${numerologyResult.favorableNumbers.joinToString(", ")}। अनुकूल दिन: ${numerologyResult.favorableDaysHindi.joinToString(", ")}।",
            cautionsHindi = bNum.cautionaryGuidanceHindi.firstOrNull() ?: "अति-उत्साह या हठ से बचें।",
            practicalRemediesHindi = numerologyResult.traditionalRemediesHindi.take(2),
            limitations = numerologyResult.limitations
        )
    }

    private fun buildCompatibilityGuidanceResponse(
        question: String,
        profile: UserProfile?,
        evidences: MutableList<AIAstrologerEvidence>,
        missing: MutableList<String>
    ): AIAstrologerResult {
        missing.add("पूर्ण गुण मिलान हेतु दो प्रोफाइल की आवश्यकता होती है।")
        return AIAstrologerResult(
            responseId = "",
            profileId = profile?.id,
            profileName = profile?.name ?: "",
            isPersonalized = false,
            userQuestion = question,
            detectedIntent = AstrologerIntent.COMPATIBILITY_GUIDANCE,
            intentConfidence = 0.95,
            mainHeadlineHindi = "अष्टकूट गुण मिलान सिद्धांत",
            simpleMeaningHindi = "वैदिक ज्योतिष में विवाह हेतु ३६ गुणों का मिलान (वर्ण, वश्य, तारा, योनि, ग्रहमैत्री, गण, भकूट, नाड़ी) किया जाता है। १८ या अधिक गुण अनुकूल माने जाते हैं।",
            currentInfluenceHindi = "विस्तृत ३६ गुण मिलान के लिए 'कुंडली मिलान' स्क्रीन में दोनों प्रोफाइल का चयन करें।",
            cautionsHindi = "केवल गुण मिलान ही नहीं, बल्कि दोनों की कुंडलियों में सप्तम भाव और ग्रहों का सामंजस्य भी महत्वपूर्ण होता है।",
            practicalRemediesHindi = listOf("पारस्परिक समझ और सम्मान को सर्वोच्च प्राथमिकता दें")
        )
    }

    private fun buildRemedyResponse(
        question: String,
        profile: UserProfile?,
        astrologyProfile: AstrologyProfile?,
        dashaTimeline: DashaTimeline?,
        yogaDoshaSnapshot: YogaDoshaSnapshot?,
        evidences: MutableList<AIAstrologerEvidence>,
        missing: MutableList<String>
    ): AIAstrologerResult {
        val maha = dashaTimeline?.currentMahadasha?.planet?.lord ?: "ग्रह"
        return AIAstrologerResult(
            responseId = "",
            profileId = profile?.id,
            profileName = profile?.name ?: "",
            isPersonalized = astrologyProfile != null,
            userQuestion = question,
            detectedIntent = AstrologerIntent.REMEDY_AND_GUIDANCE,
            intentConfidence = 0.88,
            mainHeadlineHindi = "वैदिक सात्विक उपाय एवं जीवन संतुलन",
            simpleMeaningHindi = "वैदिक ज्योतिष में उपायों का उद्देश्य ग्रहों की अनुकूल ऊर्जा को बढ़ाना और मन को शांत व स्थिर रखना है।",
            currentInfluenceHindi = "सक्रिय महादशा ($maha) और ग्रह स्थिति के अनुसार सात्विक मंत्र जप, दान और प्रकृति की सेवा सर्वाधिक फलदायी होती है।",
            cautionsHindi = "अंधविश्वास और भय से दूर रहें; सात्विक कर्म ही सर्वश्रेष्ठ उपाय है।",
            practicalRemediesHindi = listOf(
                "प्रतिदिन गायत्री मंत्र या 'ॐ नमः शिवाय' का जप करें",
                "प्रातः पक्षियों को दाना व जल अर्पित करें",
                "माता-पिता और गुरुजनों का नित्य आशीर्वाद लें"
            )
        )
    }

    private fun buildTodayGuidanceResponse(
        question: String,
        profile: UserProfile?,
        astrologyProfile: AstrologyProfile?,
        transitSnapshot: TransitSnapshot?,
        panchangSnapshot: PanchangSnapshot?,
        evidences: MutableList<AIAstrologerEvidence>,
        missing: MutableList<String>
    ): AIAstrologerResult {
        val p = panchangSnapshot
        val tithiText = if (p != null) "आज ${p.vara.hindiName}, ${p.tithi.hindiName} तिथि (${p.nakshatra.nakshatra.sanskritName} नक्षत्र) है।" else "आज का पंचांग अनुकूल है।"
        return AIAstrologerResult(
            responseId = "",
            profileId = profile?.id,
            profileName = profile?.name ?: "",
            isPersonalized = astrologyProfile != null,
            userQuestion = question,
            detectedIntent = AstrologerIntent.TODAY_GUIDANCE,
            intentConfidence = 0.88,
            mainHeadlineHindi = "आज का दिन एवं मार्गदर्शक सुझाव",
            simpleMeaningHindi = tithiText,
            currentInfluenceHindi = "आज अपने महत्वपूर्ण कार्यों को योजनाबद्ध ढंग से करें और दिन के शुभ मुहूर्तों का लाभ उठाएं।",
            cautionsHindi = "राहुकाल के समय कोई भी नया या जोखिम भरा कार्य प्रारंभ न करें।",
            practicalRemediesHindi = listOf(
                "दिन की शुरुआत सकारात्मक संकल्प के साथ करें",
                "किसी जरूरतमंद की निस्वार्थ सहायता करें"
            )
        )
    }

    private fun buildGeneralAstrologyResponse(
        question: String,
        evidences: MutableList<AIAstrologerEvidence>
    ): AIAstrologerResult {
        return AIAstrologerResult(
            responseId = "",
            profileId = null,
            profileName = "",
            isPersonalized = false,
            userQuestion = question,
            detectedIntent = AstrologerIntent.GENERAL_ASTROLOGY_EXPLANATION,
            intentConfidence = 0.80,
            mainHeadlineHindi = "वैदिक ज्योतिष सिद्धांत",
            simpleMeaningHindi = "वैदिक ज्योतिष (Jyotish) वेदों का नेत्र माना जाता है। यह खगोलीय ग्रह स्थितियों और मानव चेतना के संबंध का विज्ञान है।",
            currentInfluenceHindi = "कुंडली में १२ भाव, १२ राशियां, ९ ग्रह और २७ नक्षत्र मिलकर जीवन के विभिन्न पहलुओं का मानचित्र प्रस्तुत करते हैं।",
            cautionsHindi = "ज्योतिष मार्गदर्शन का साधन है, यह कर्म और पुरुषार्थ का विकल्प नहीं है।",
            practicalRemediesHindi = listOf("सदा सत्कर्म और अनुशासन का पालन करें")
        )
    }

    private fun buildGeneralHoroscopeResponse(
        question: String,
        profile: UserProfile?,
        astrologyProfile: AstrologyProfile?,
        dashaTimeline: DashaTimeline?,
        transitSnapshot: TransitSnapshot?,
        interpretation: AdvancedVedicInterpretation?,
        numerologyResult: NumerologyResult?,
        evidences: MutableList<AIAstrologerEvidence>,
        missing: MutableList<String>
    ): AIAstrologerResult {
        if (astrologyProfile == null) {
            missing.add("जन्म कुंडली का विवरण उपलब्ध नहीं है।")
            return AIAstrologerResult(
                responseId = "",
                profileId = null,
                profileName = "",
                isPersonalized = false,
                userQuestion = question,
                detectedIntent = AstrologerIntent.GENERAL_HOROSCOPE,
                intentConfidence = 0.70,
                mainHeadlineHindi = "समग्र वैदिक कुण्डली फलित सिद्धांत",
                simpleMeaningHindi = "जन्म कुंडली व्यक्ति के जन्म समय के आकाशीय ग्रहों की स्थिति का दर्पण है। इससे स्वभाव, क्षमता और जीवन पथ का बोध होता है।",
                currentInfluenceHindi = "व्यक्तिगत विश्लेषण के लिए कृपया अपनी जन्म प्रोफाइल चुनें।",
                cautionsHindi = "सकारात्मक सोच और पुरुषार्थ के साथ आगे बढ़ें।",
                practicalRemediesHindi = listOf("दैनिक ध्यान और ईश्वर स्मरण करें")
            )
        }

        val lagnaRashi = Rashi.fromIndex(astrologyProfile.lagnaSignIndex)
        val moonRashi = Rashi.fromIndex(astrologyProfile.moonSignIndex)
        val activeMahadasha = dashaTimeline?.currentMahadasha?.planet?.lord ?: "अज्ञात"

        evidences.add(
            AIAstrologerEvidence(
                factorName = "लग्न एवं चंद्र राशि",
                sourceEngine = "Phase 2 Kundli Engine",
                calculatedValue = "लग्न: ${lagnaRashi.sanskritName}, चंद्र राशि: ${moonRashi.sanskritName}, नक्षत्र: ${astrologyProfile.nakshatra}",
                astronomicalBasis = "निरयण गणना (Lahiri Ayanamsa)"
            )
        )

        return AIAstrologerResult(
            responseId = "",
            profileId = profile?.id,
            profileName = profile?.name ?: "",
            isPersonalized = true,
            userQuestion = question,
            detectedIntent = AstrologerIntent.GENERAL_HOROSCOPE,
            intentConfidence = 0.85,
            mainHeadlineHindi = "समग्र कुण्डली सार — ${profile?.name ?: ""}",
            simpleMeaningHindi = "आपका जन्म ${lagnaRashi.sanskritName} लग्न और ${moonRashi.sanskritName} चंद्र राशि में हुआ है। नक्षत्र: ${astrologyProfile.nakshatra}।",
            currentInfluenceHindi = "वर्तमान में $activeMahadasha महादशा का प्रभाव है जो आपके जीवन की मुख्य प्राथमिकताओं को आकार दे रहा है।",
            cautionsHindi = "अपने मूल स्वभाव के अनुसार धैर्य और संयम बनाए रखें।",
            practicalRemediesHindi = listOf(
                "लग्न व चंद्र के अनुकूल प्रातः ध्यान करें",
                "माता-पिता का सम्मान करें"
            )
        )
    }
}
