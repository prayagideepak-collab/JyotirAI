package com.example.domain.numerology

/**
 * Engine for analyzing number compatibility and purpose-wise impact against a user's Numerology Profile.
 */
class NumberCompatibilityEngine {

    /**
     * Analyzes a normalized number against the user's [NumerologyResult] for the specified [purposes].
     */
    fun analyze(
        normalizedNumber: NormalizedNumber,
        numerologyResult: NumerologyResult,
        purposes: List<CompatibilityPurpose>
    ): NumberCompatibilityResult {
        val profileId = numerologyResult.profileId
        val profileName = numerologyResult.profileName
        val reduced = normalizedNumber.reducedValue
        val root = normalizedNumber.rootSingleDigit

        // Compare against profile's favorable / challenging numbers
        val favorable = numerologyResult.favorableNumbers
        val challenging = numerologyResult.challengingNumbers
        val lifePathRoot = numerologyResult.lifePathNumber?.rootSingleDigit ?: 1

        val isFavorable = favorable.contains(root) || favorable.contains(reduced)
        val isChallenging = challenging.contains(root) || challenging.contains(reduced)
        val matchesLifePath = root == lifePathRoot

        val overallStatus = when {
            isFavorable && matchesLifePath -> CompatibilityStatus.POSITIVE
            isFavorable -> CompatibilityStatus.POSITIVE
            isChallenging -> CompatibilityStatus.CHALLENGING
            reduced in setOf(11, 22, 33) -> CompatibilityStatus.POSITIVE
            else -> CompatibilityStatus.NEUTRAL
        }

        val purposeReports = mutableMapOf<CompatibilityPurpose, PurposeAnalysis>()
        for (purpose in purposes) {
            purposeReports[purpose] = generatePurposeAnalysis(
                purpose = purpose,
                normalizedNumber = normalizedNumber,
                isFavorable = isFavorable,
                isChallenging = isChallenging,
                matchesLifePath = matchesLifePath,
                overallStatus = overallStatus
            )
        }

        val summaryHindi = buildSummaryHindi(normalizedNumber, overallStatus, profileName)
        val guidanceHindi = buildGuidanceHindi(overallStatus, normalizedNumber.category)

        return NumberCompatibilityResult(
            resultId = "COMP-${normalizedNumber.normalizedValue}-${System.currentTimeMillis()}",
            profileId = profileId,
            profileName = profileName,
            normalizedNumber = normalizedNumber,
            overallStatus = overallStatus,
            purposeReports = purposeReports,
            summaryHindi = summaryHindi,
            guidanceHindi = guidanceHindi
        )
    }

    /**
     * Compares two numbers and generates purpose-wise comparison insights.
     */
    fun compare(
        numberA: NormalizedNumber,
        numberB: NormalizedNumber,
        numerologyResult: NumerologyResult,
        purposes: List<CompatibilityPurpose>
    ): NumberComparisonResult {
        val resultA = analyze(numberA, numerologyResult, purposes)
        val resultB = analyze(numberB, numerologyResult, purposes)

        val labelA = numberA.customLabel ?: numberA.privacyMasked
        val labelB = numberB.customLabel ?: numberB.privacyMasked

        val compSummary = "संख्या '$labelA' (अंक ${numberA.reducedValue}, ${resultA.overallStatus.hindiName}) और " +
                "संख्या '$labelB' (अंक ${numberB.reducedValue}, ${resultB.overallStatus.hindiName}) का तुलनात्मक विश्लेषण " +
                "प्रदर्शित है। दोनों का ऊर्जा कंपन अलग-अलग उद्देश्यों के लिए उपयुक्त हो सकता है।"

        return NumberComparisonResult(
            labelA = labelA,
            labelB = labelB,
            resultA = resultA,
            resultB = resultB,
            comparisonSummaryHindi = compSummary
        )
    }

    private fun generatePurposeAnalysis(
        purpose: CompatibilityPurpose,
        normalizedNumber: NormalizedNumber,
        isFavorable: Boolean,
        isChallenging: Boolean,
        matchesLifePath: Boolean,
        overallStatus: CompatibilityStatus
    ): PurposeAnalysis {
        val root = normalizedNumber.rootSingleDigit
        val reduced = normalizedNumber.reducedValue

        val status = when {
            isFavorable -> CompatibilityStatus.POSITIVE
            isChallenging -> CompatibilityStatus.CHALLENGING
            overallStatus == CompatibilityStatus.NEUTRAL -> CompatibilityStatus.NEUTRAL
            else -> CompatibilityStatus.MIXED
        }

        val titleHindi = "${purpose.hindiName} संदर्भ (${normalizedNumber.category.hindiName})"

        val positives = mutableListOf<String>()
        val challenges = mutableListOf<String>()

        when (purpose) {
            CompatibilityPurpose.PERSONAL -> {
                positives.add("यह संख्या व्यक्तिगत स्वभाव और ऊर्जा के साथ सहज सामंजस्य रखती है।")
                if (isFavorable) positives.add("दैनिक जीवन में मानसिक शांति और सकारात्मकता को बढ़ावा देती है।")
                challenges.add("अतिरिक्त संवेदनशील होने पर छोटी बातों पर अधिक विचार से बचें।")
                challenges.add("आत्मविश्वास और संयम का संतुलित दृष्टिकोण रखें।")
            }
            CompatibilityPurpose.FINANCIAL -> {
                positives.add("धन प्रबंधन और वित्तीय नियोजन में स्थिरता का संकेत देती है।")
                if (isFavorable) positives.add("नियोजित निवेश और बचत के लिए अनुकूल कंपन।")
                challenges.add("आवेगपूर्ण (Impulsive) खर्चों से सावधान रहें।")
                challenges.add("वित्तीय निर्णय लेते समय व्यावहारिक विश्लेषण अवश्य करें।")
            }
            CompatibilityPurpose.BUSINESS -> {
                positives.add("व्यावसायिक संपर्कों और ग्राहक सेवा में सकारात्मक ऊर्जा प्रदान करती है।")
                if (isFavorable) positives.add("व्यापारिक विस्तार और नए समझौतों के लिए शुभ।")
                challenges.add("प्रतिस्पर्धा के दौरान धैर्य और कूटनीति बनाए रखें।")
                challenges.add("साझेदारी में स्पष्टता और पारदर्शिता आवश्यक है।")
            }
            CompatibilityPurpose.PROFESSIONAL -> {
                positives.add("करियर में प्रगति और कार्यकुशलता को समर्थन देती है।")
                challenges.add("कार्यस्थल पर सहकर्मियों के साथ संवाद में स्पष्टता रखें।")
            }
            CompatibilityPurpose.COMMUNICATION -> {
                positives.add("संचार, नेटवर्किंग और सूचना आदान-प्रदान के लिए प्रभावी संख्या।")
                challenges.add("गलतफहमी से बचने के लिए संदेशों की स्पष्टता जांच लें।")
            }
            CompatibilityPurpose.VEHICLE -> {
                positives.add("यात्रा के दौरान मानसिक एकाग्रता और सुरक्षा का भाव बनाए रखती है।")
                challenges.add("वाहन चलाते समय यातायात नियमों का कड़ाई से पालन करें।")
            }
            CompatibilityPurpose.PROPERTY -> {
                positives.add("आवास या संपत्ति के माहौल में सकारात्मक और स्थिर ऊर्जा प्रदान करती है।")
                challenges.add("दस्तावेज़ों और कानूनी पहलुओं की जांच सावधानी से करें।")
            }
            CompatibilityPurpose.GENERAL -> {
                positives.add("समग्र रूप से संतुलित कंपन प्रदान करती है।")
                challenges.add("व्यक्तिगत प्राथमिकताओं के अनुसार उपयोग करें।")
            }
        }

        val guidance = "यह विश्लेषण अंकशास्त्र पर आधारित व्यावहारिक मार्गदर्शन है। इसे निश्चित भविष्यवाणियों के रूप में न लें, बल्कि अपनी सूझबूझ के साथ उपयोग करें।"

        return PurposeAnalysis(
            purpose = purpose,
            status = status,
            titleHindi = titleHindi,
            positiveTendencies = positives,
            challengingTendencies = challenges,
            guidanceHindi = guidance
        )
    }

    private fun buildSummaryHindi(normalizedNumber: NormalizedNumber, status: CompatibilityStatus, profileName: String): String {
        val catName = normalizedNumber.category.hindiName
        val masked = normalizedNumber.privacyMasked
        val red = normalizedNumber.reducedValue
        return "$profileName के लिए $catName ($masked, मूलांक/कंपन $red) का विश्लेषण '${status.hindiName}' श्रेणी में पाया गया है।"
    }

    private fun buildGuidanceHindi(status: CompatibilityStatus, category: NumberCategory): String {
        return when (status) {
            CompatibilityStatus.POSITIVE -> "यह संख्या आपकी प्रोफाइल के साथ उत्कृष्ट सामंजस्य रखती है। आप इसे आत्मविश्वास के साथ उपयोग कर सकते हैं।"
            CompatibilityStatus.NEUTRAL -> "यह संख्या एक तटस्थ प्रभाव रखती है। यह आपके दैनिक कार्यों में सामान्य रूप से सहायक है।"
            CompatibilityStatus.MIXED -> "इस संख्या के सकारात्मक और चुनौतीपूर्ण दोनों पहलू हैं। अपने विवेक और प्राथमिकताओं के अनुसार निर्णय लें।"
            CompatibilityStatus.CHALLENGING -> "यह संख्या कुछ मामलों में कम अनुकूल हो सकती है। किसी भी बड़े वित्तीय या व्यावसायिक निर्णय में व्यावहारिक सावधानी बरतें।"
        }
    }
}
