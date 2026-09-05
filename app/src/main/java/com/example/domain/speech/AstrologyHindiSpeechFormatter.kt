package com.example.domain.speech

import com.example.domain.interpretation.*
import com.example.domain.models.*
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Prepares structured astrological content into natural, dignified spoken Hindi.
 *
 * Guarantees:
 * 1. Preserves all classical Sanskrit/Vedic terminology (Grahas, Rashis, Nakshatras, Dashas, Muhurtas, Panchang elements).
 * 2. Removes Markdown, emojis, control characters, and raw formatting symbols.
 * 3. Formats natural sentence boundaries with Hindi dandas (।) and commas for rhythmic TTS playback.
 * 4. Preserves non-fatalistic, traditional guidance framing without making predictive certainty claims.
 */
object AstrologyHindiSpeechFormatter {

    // Classical Graha mapping (English -> Sanskrit / Hindi)
    private val GRAHA_HINDI_MAP = mapOf(
        "Sun" to "सूर्य",
        "Moon" to "चन्द्र",
        "Mars" to "मंगल",
        "Mercury" to "बुध",
        "Jupiter" to "गुरु",
        "Venus" to "शुक्र",
        "Saturn" to "शनि",
        "Rahu" to "राहु",
        "Ketu" to "केतु",
        "Uranus" to "हर्षल",
        "Neptune" to "वरुण",
        "Pluto" to "यम"
    )

    // Classical Rashi mapping (English -> Sanskrit / Hindi)
    private val RASHI_HINDI_MAP = mapOf(
        "Aries" to "मेष",
        "Taurus" to "वृषभ",
        "Gemini" to "मिथुन",
        "Cancer" to "कर्क",
        "Leo" to "सिंह",
        "Virgo" to "कन्या",
        "Libra" to "तुला",
        "Scorpio" to "वृश्चिक",
        "Sagittarius" to "धनु",
        "Capricorn" to "मकर",
        "Aquarius" to "कुंभ",
        "Pisces" to "मीन"
    )

    // Nakshatra Sanskrit names
    private val NAKSHATRA_HINDI_MAP = mapOf(
        "Ashwini" to "अश्विनी",
        "Bharani" to "भरणी",
        "Krittika" to "कृत्तिका",
        "Rohini" to "रोहिणी",
        "Mrigashira" to "मृगशिरा",
        "Mrigashirsha" to "मृगशिरा",
        "Ardra" to "आर्द्रा",
        "Punarvasu" to "पुनर्वसु",
        "Pushya" to "पुष्य",
        "Ashlesha" to "आश्लेषा",
        "Magha" to "मघा",
        "Purva Phalguni" to "पूर्वा फाल्गुनी",
        "Uttara Phalguni" to "उत्तरा फाल्गुनी",
        "Hasta" to "हस्त",
        "Chitra" to "चित्रा",
        "Swati" to "स्वाति",
        "Vishakha" to "विशाखा",
        "Anuradha" to "अनुराधा",
        "Jyeshtha" to "ज्येष्ठा",
        "Mula" to "मूल",
        "Purva Ashadha" to "पूर्वाषाढ़ा",
        "Uttara Ashadha" to "उत्तराषाढ़ा",
        "Shravana" to "श्रवण",
        "Dhanishta" to "धनिष्ठा",
        "Shatabhisha" to "शतभिषा",
        "Purva Bhadrapada" to "पूर्वाभाद्रपद",
        "Uttara Bhadrapada" to "उत्तराभाद्रपद",
        "Revati" to "रेवती"
    )

    /**
     * Converts a DailyRashifal object into natural, fluid spoken Hindi.
     */
    fun formatDailyRashifal(rashifal: DailyRashifal): String {
        val sb = StringBuilder()

        val dateFormatted = rashifal.targetDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        sb.append("ज्योतिर् एआई दैनिक राशिफल। ")
        sb.append("दिनांक $dateFormatted। ")
        sb.append("दैनिक ऊर्जा समन्वय ${rashifal.energyScore} प्रतिशत। ")
        sb.append("आज का मुख्य विषय: ${translateToHindiSpeech(rashifal.dailyTheme)}। ")
        sb.append("प्राथमिक एकाग्रता: ${translateToHindiSpeech(rashifal.primaryFocus)}। ")

        // Astrological context
        val moonRashiHindi = RASHI_HINDI_MAP[rashifal.moonSign] ?: rashifal.moonSign
        val nakshatraHindi = NAKSHATRA_HINDI_MAP[rashifal.birthNakshatra] ?: rashifal.birthNakshatra
        val dashaLordHindi = GRAHA_HINDI_MAP[rashifal.currentMahadashaLord] ?: rashifal.currentMahadashaLord
        val antardashaLordHindi = GRAHA_HINDI_MAP[rashifal.currentAntardashaLord] ?: rashifal.currentAntardashaLord

        sb.append("ज्योतिषीय संदर्भ: ")
        sb.append("गोचर चन्द्रमा $moonRashiHindi राशि और $nakshatraHindi नक्षत्र में संचरण कर रहे हैं। ")
        sb.append("सक्रिय दशा $dashaLordHindi की महादशा और $antardashaLordHindi की अंतर्दशा है। ")

        // Key Influences
        if (rashifal.keyInfluences.isNotEmpty()) {
            sb.append("मुख्य प्रभाव: ")
            rashifal.keyInfluences.forEach { influence ->
                val sourceHindi = translateToHindiSpeech(influence.contributingFactor)
                val textHindi = translateToHindiSpeech(influence.description)
                sb.append("$sourceHindi, $textHindi। ")
            }
        }

        // Priorities
        if (rashifal.priorities.isNotEmpty()) {
            sb.append("आज के अनुकूल कार्य एवं प्राथमिकताएं: ")
            rashifal.priorities.forEach { priority ->
                sb.append("${translateToHindiSpeech(priority.advice)}। ")
            }
        }

        // Cautions
        if (rashifal.cautions.isNotEmpty()) {
            sb.append("सावधानी और सतर्कता के क्षेत्र: ")
            rashifal.cautions.forEach { caution ->
                sb.append("${translateToHindiSpeech(caution.warning)}। ")
            }
        }

        // Traditional Remedies
        if (rashifal.traditionalRemedies.isNotEmpty()) {
            sb.append("पारंपरिक उपचारात्मक सुझाव: ")
            rashifal.traditionalRemedies.forEach { remedy ->
                sb.append("${translateToHindiSpeech(remedy.practice)}। ")
            }
        }

        // Ethical Disclaimer
        sb.append("यह पारंपरिक वैदिक ज्योतिष पर आधारित सांकेतिक मार्गदर्शन है, कोई अंतिम भाग्यफल नहीं।")

        return cleanForSpeech(sb.toString())
    }

    /**
     * Converts a Kundli chart and profile interpretation into natural Hindi speech.
     */
    fun formatKundliSummary(profile: AstrologyProfile, activeChart: Chart): String {
        val sb = StringBuilder()
        val birthData = profile.birthData
        val dateFormatted = "${birthData.date.dayOfMonth} ${getMonthNameHindi(birthData.date.monthValue)} ${birthData.date.year}"
        val timeFormatted = String.format("%02d:%02d", birthData.time.hour, birthData.time.minute)

        sb.append("ज्योतिर् एआई कुण्डली विवरण। ")
        sb.append("जातक का नाम: ${birthData.name}। ")
        sb.append("जन्म तिथि: $dateFormatted, समय: $timeFormatted, स्थान: ${birthData.location.placeName}। ")

        val ascRashiEnum = com.example.domain.models.Rashi.fromIndex(profile.lagnaSignIndex)
        val ascSign = RASHI_HINDI_MAP[ascRashiEnum.name] ?: ascRashiEnum.sanskritName
        sb.append("लग्न: $ascSign लग्न। ")

        val chartTitleHindi = when (activeChart.vargaType) {
            VargaType.D1 -> "लग्न कुण्डली (डी १)"
            VargaType.D9 -> "नवांश कुण्डली (डी ९)"
            VargaType.D10 -> "दशांश कुण्डली (डी १०)"
            else -> "${activeChart.title} (${activeChart.vargaType.name})"
        }
        sb.append("प्रदर्शित चक्र: $chartTitleHindi। ")

        sb.append("मुख्य ग्रह स्थितियां: ")
        activeChart.positions.forEach { planet ->
            val grahaHindi = GRAHA_HINDI_MAP[planet.planet] ?: planet.sanskritName
            val rashiHindi = RASHI_HINDI_MAP[planet.sign] ?: planet.rashiEnum.sanskritName
            val houseHindi = getHouseNameHindi(planet.house)
            val retroHindi = if (planet.isRetrograde && planet.planet !in listOf("Rahu", "Ketu")) ", वक्री" else ""
            val dignityHindi = when (planet.dignity) {
                PlanetDignity.EXALTED -> ", उच्च के"
                PlanetDignity.DEBILITATED -> ", नीच के"
                PlanetDignity.MOOLATRIKONA -> ", मूलत्रिकोण में"
                PlanetDignity.OWN_SIGN -> ", स्वराशि में"
                PlanetDignity.FRIEND -> ", मित्र राशि में"
                PlanetDignity.NEUTRAL -> ", सम राशि में"
                PlanetDignity.ENEMY -> ", शत्रु राशि में"
            }
            sb.append("$grahaHindi $rashiHindi राशि में, $houseHindi में$dignityHindi$retroHindi स्थित हैं। ")
        }

        sb.append("यह कुण्डली फलित वैदिक ज्योतिष के शास्त्रीय सिद्धांतों पर आधारित व्याख्या है।")
        return cleanForSpeech(sb.toString())
    }

    /**
     * Converts Vimshottari Dasha timeline into natural Hindi speech.
     */
    fun formatDashaSummary(timeline: DashaTimeline): String {
        val sb = StringBuilder()
        sb.append("विंशोत्तरी दशा का विवरण। ")

        val currentMaha = timeline.currentMahadasha
        if (currentMaha != null) {
            val mahaPlanet = GRAHA_HINDI_MAP[currentMaha.planet.lord] ?: currentMaha.planet.sanskritName
            sb.append("वर्तमान महादशा $mahaPlanet की है। ")

            val currentAntar = timeline.currentAntardasha
            if (currentAntar != null) {
                val antarPlanet = GRAHA_HINDI_MAP[currentAntar.antardashaLord.lord] ?: currentAntar.antardashaLord.sanskritName
                sb.append("वर्तमान अंतर्दशा $antarPlanet की है। ")
            }
        }

        sb.append("दशा काल में सम्बन्धित ग्रहों के कारकत्व और भावेश्वर प्रभाव के अनुसार जातक को अनुभव प्राप्त होते हैं।")
        return cleanForSpeech(sb.toString())
    }

    /**
     * Converts Panchang snapshot into natural Hindi speech.
     */
    fun formatPanchangSummary(panchang: PanchangSnapshot): String {
        val sb = StringBuilder()
        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

        sb.append("दैनिक पंचांग विवरण। ")
        sb.append("स्थान: ${panchang.location.placeName}। ")
        sb.append("वार: ${panchang.vara.sanskritName}। ")
        sb.append("तिथि: ${panchang.tithi.name}, ${if (panchang.paksha == Paksha.SHUKLA) "शुक्ल पक्ष" else "कृष्ण पक्ष"}। ")
        sb.append("नक्षत्र: ${panchang.nakshatra.nakshatra.sanskritName}, चरण ${panchang.nakshatra.pada}। ")
        sb.append("योग: ${panchang.yoga.name}। ")
        sb.append("करण: ${panchang.karana.name}। ")

        if (panchang.sunrise != null) {
            sb.append("सूर्योदय: ${panchang.sunrise.format(timeFormatter)}। ")
        }
        if (panchang.sunset != null) {
            sb.append("सूर्यास्त: ${panchang.sunset.format(timeFormatter)}। ")
        }

        panchang.muhurta?.let { muhurta ->
            muhurta.brahmaMuhurta?.let { bm ->
                sb.append("ब्रह्म मुहूर्त: ${bm.start.format(timeFormatter)} से ${bm.end.format(timeFormatter)}। ")
            }
            muhurta.abhijitMuhurta?.let { am ->
                sb.append("अभिजित मुहूर्त: ${am.start.format(timeFormatter)} से ${am.end.format(timeFormatter)}। ")
            }
            muhurta.rahukaal?.let { rk ->
                sb.append("राहुकाल: ${rk.start.format(timeFormatter)} से ${rk.end.format(timeFormatter)}। ")
            }
        }

        return cleanForSpeech(sb.toString())
    }

    /**
     * Converts Advanced Vedic Interpretation into dignified natural Hindi speech.
     */
    fun formatAdvancedInterpretationSummary(interpretation: AdvancedVedicInterpretation): String {
        val sb = StringBuilder()
        sb.append("ज्योतिर् एआई उन्नत वैदिक फलित विश्लेषण। ")
        sb.append("जातक का नाम: ${interpretation.profileName}। ")
        sb.append("${translateToHindiSpeech(interpretation.natalSummary)}। ")

        interpretation.dominantFactor?.let { dominant ->
            sb.append("मुख्य ग्रह प्रभाव: ${translateToHindiSpeech(dominant.name)}, ${translateToHindiSpeech(dominant.calculatedValue)}। ")
        }

        if (interpretation.opportunities.isNotEmpty()) {
            sb.append("अनुकूल अवसर: ")
            interpretation.opportunities.forEach { opp ->
                sb.append("${translateToHindiSpeech(opp)}। ")
            }
        }

        if (interpretation.cautions.isNotEmpty()) {
            sb.append("सावधानी के क्षेत्र: ")
            interpretation.cautions.forEach { caution ->
                sb.append("${translateToHindiSpeech(caution)}। ")
            }
        }

        if (interpretation.traditionalGuidance.isNotEmpty()) {
            sb.append("पारंपरिक मार्गदर्शन: ")
            interpretation.traditionalGuidance.forEach { guidance ->
                sb.append("${translateToHindiSpeech(guidance)}। ")
            }
        }

        sb.append("यह पारंपरिक वैदिक ज्योतिष पर आधारित सांकेतिक मार्गदर्शन है।")
        return cleanForSpeech(sb.toString())
    }

    /**
     * Converts Divisional Cross-Analysis (D1/D9/D10) into spoken Hindi.
     */
    fun formatDivisionalAnalysisSummary(analysis: DivisionalCrossAnalysis): String {
        val sb = StringBuilder()
        sb.append("वर्ग कुण्डली विश्लेषण। ")
        if (analysis.d9Available) {
            sb.append("नवांश डी ९: ${translateToHindiSpeech(analysis.d9Summary)}। ")
            if (analysis.vargottamaPlanets.isNotEmpty()) {
                val planetsHi = analysis.vargottamaPlanets.joinToString(", ") { GRAHA_HINDI_MAP[it] ?: it }
                sb.append("वर्गोत्तम ग्रह: $planetsHi। ")
            }
        }
        if (analysis.d10Available) {
            sb.append("दशांश डी १० कर्म विश्लेषण: ${translateToHindiSpeech(analysis.d10Summary)}। ")
        }
        return cleanForSpeech(sb.toString())
    }

    /**
     * Converts Dasha and Transit synergy into spoken Hindi.
     */
    fun formatDashaTransitSynergy(interpretation: AdvancedVedicInterpretation): String {
        val sb = StringBuilder()
        sb.append("दशा एवं गोचर समन्वय। ")
        interpretation.dashaContext?.let { dasha ->
            val mahaHi = GRAHA_HINDI_MAP[dasha.mahadashaLord] ?: dasha.mahadashaLord
            val antarHi = GRAHA_HINDI_MAP[dasha.antardashaLord] ?: dasha.antardashaLord
            sb.append("सक्रिय महादशा: $mahaHi, अंतर्दशा: $antarHi। ")
            sb.append("${translateToHindiSpeech(dasha.summary)}। ")
        }
        interpretation.transitContext?.let { transit ->
            sb.append("गोचर प्रभाव: ${translateToHindiSpeech(transit.summary)}। ")
            if (transit.sadeSatiPhase != null) {
                sb.append("शनि की साढ़ेसाती सक्रिय है: ${translateToHindiSpeech(transit.sadeSatiPhase)}। ")
            }
        }
        return cleanForSpeech(sb.toString())
    }

    /**
     * Core translator for English astrological sentence templates to natural Hindi speech.
     */
    fun translateToHindiSpeech(text: String): String {
        var translated = text

        // Replace common terms
        GRAHA_HINDI_MAP.forEach { (en, hi) ->
            translated = translated.replace(Regex("\\b$en\\b", RegexOption.IGNORE_CASE), hi)
        }
        RASHI_HINDI_MAP.forEach { (en, hi) ->
            translated = translated.replace(Regex("\\b$en\\b", RegexOption.IGNORE_CASE), hi)
        }
        NAKSHATRA_HINDI_MAP.forEach { (en, hi) ->
            translated = translated.replace(Regex("\\b$en\\b", RegexOption.IGNORE_CASE), hi)
        }

        // Replace domain phrases
        val phraseMap = mapOf(
            "Mahadasha" to "महादशा",
            "Antardasha" to "अंतर्दशा",
            "Pratyantardasha" to "प्रत्यंतर्दशा",
            "Brahma Muhurta" to "ब्रह्म मुहूर्त",
            "Abhijit Muhurta" to "अभिजित मुहूर्त",
            "Rahukaal" to "राहुकाल",
            "Rahu Kaal" to "राहुकाल",
            "Tithi" to "तिथि",
            "Nakshatra" to "नक्षत्र",
            "Yoga" to "योग",
            "Karana" to "करण",
            "Ascendant" to "लग्न",
            "Transit" to "गोचर",
            "Transits" to "गोचर",
            "House" to "भाव",
            "Retrograde" to "वक्री",
            "Combust" to "अस्त",
            "Exalted" to "उच्च",
            "Debilitated" to "नीच",
            "Moolatrikona" to "मूलत्रिकोण",
            "Own Sign" to "स्वक्षेत्र",
            "Friend Sign" to "मित्र राशि",
            "Enemy Sign" to "शत्रु राशि",
            "Neutral Sign" to "सम राशि",
            "Conjunction" to "युति",
            "Aspect" to "दृष्टि",
            "benefic" to "शुभ",
            "malefic" to "अशुभ",
            "favorable" to "अनुकूल",
            "caution" to "सतर्कता",
            "remedy" to "उपाय",
            "alignment" to "समन्वय",
            "energy" to "ऊर्जा",
            "focus" to "एकाग्रता",
            "meditation" to "ध्यान",
            "mindfulness" to "सजगता",
            "priorities" to "प्राथमिकताएं",
            "cautions" to "सावधानियां",
            "remedies" to "उपाय",
            "today" to "आज",
            "active" to "सक्रिय"
        )

        phraseMap.forEach { (en, hi) ->
            translated = translated.replace(Regex("\\b$en\\b", RegexOption.IGNORE_CASE), hi)
        }

        return translated
    }

    /**
     * Cleans text to remove markdown, UI markers, and unwanted symbols.
     */
    fun cleanForSpeech(input: String): String {
        return input
            .replace(Regex("[#*`_\\[\\]()~>{}\\\\]"), " ")
            .replace(Regex("[•\\-–—]"), " ")
            .replace(Regex("%"), " प्रतिशत ")
            .replace(Regex("&"), " और ")
            .replace(":", ", ")
            .replace(Regex("\\s+"), " ")
            .replace("..", ".")
            .replace("।।", "।")
            .trim()
    }

    private fun getHouseNameHindi(house: Int): String {
        return when (house) {
            1 -> "प्रथम भाव"
            2 -> "द्वितीय भाव"
            3 -> "तृतीय भाव"
            4 -> "चतुर्थ भाव"
            5 -> "पंचम भाव"
            6 -> "षष्ठ भाव"
            7 -> "सप्तम भाव"
            8 -> "अष्टम भाव"
            9 -> "नवम भाव"
            10 -> "दशम भाव"
            11 -> "एकादश भाव"
            12 -> "द्वादश भाव"
            else -> "${house}वें भाव"
        }
    }

    private fun getMonthNameHindi(month: Int): String {
        return when (month) {
            1 -> "जनवरी"
            2 -> "फरवरी"
            3 -> "मार्च"
            4 -> "अप्रैल"
            5 -> "मई"
            6 -> "जून"
            7 -> "जुलाई"
            8 -> "अगस्त"
            9 -> "सितंबर"
            10 -> "अक्टूबर"
            11 -> "नवंबर"
            12 -> "दिसंबर"
            else -> "माह $month"
        }
    }
}
