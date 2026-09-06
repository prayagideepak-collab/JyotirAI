package com.example.domain.numerology

/**
 * Classical Planetary Rulerships, Vibrations, Strengths, Cautions, and Remedies for Root Numbers 1..9 & Master Numbers.
 */
object NumerologyInterpretations {

    data class NumberSignification(
        val rootNumber: Int,
        val rulingPlanet: String,
        val rulingPlanetHindi: String,
        val titleHindi: String,
        val summaryHindi: String,
        val descriptionHindi: String,
        val strengthsHindi: List<String>,
        val cautionsHindi: List<String>,
        val favorableNumbers: List<Int>,
        val neutralNumbers: List<Int>,
        val challengingNumbers: List<Int>,
        val favorableDaysHindi: List<String>,
        val favorableColorsHindi: List<String>,
        val remediesHindi: List<String>
    )

    private val SIGNIFICATIONS: Map<Int, NumberSignification> = mapOf(
        1 to NumberSignification(
            rootNumber = 1,
            rulingPlanet = "Sun",
            rulingPlanetHindi = "सूर्य (Surya)",
            titleHindi = "मूलांक १ — नेतृत्व, आत्मबल एवं आत्मविश्वास",
            summaryHindi = "अंक १ सूर्य का प्रतीक है, जो जीवन में स्पष्ट दृष्टि, स्वाभिमान और नेतृत्व की प्रेरणा देता है।",
            descriptionHindi = "आप स्वभाव से स्वतंत्र विचार वाले, मौलिक और आत्मविश्वासी हैं। किसी भी कार्य की शुरुआत करने में आगे रहते हैं तथा अपने निर्णयों पर दृढ़ रहते हैं।",
            strengthsHindi = listOf("नेतृत्व क्षमता", "आत्मविश्वास व स्पष्टता", "स्वावलंबन", "उदारता"),
            cautionsHindi = listOf("अहंकार अथवा अति-आत्मविश्वास से बचें", "दूसरों के विचारों को भी सम्मान दें", "हठ न करें"),
            favorableNumbers = listOf(1, 2, 3, 9),
            neutralNumbers = listOf(4, 7),
            challengingNumbers = listOf(6, 8),
            favorableDaysHindi = listOf("रविवार", "सोमवार"),
            favorableColorsHindi = listOf("सुनहरा (गोल्डन)", "नारंगी", "पीला"),
            remediesHindi = listOf("प्रातः सूर्य को अर्घ्य दें", "गायत्री मंत्र का जप करें", "पिता का आदर करें")
        ),
        2 to NumberSignification(
            rootNumber = 2,
            rulingPlanet = "Moon",
            rulingPlanetHindi = "चन्द्र (Chandra)",
            titleHindi = "मूलांक २ — संवेदनशीलता, सौम्यता एवं सहयोग",
            summaryHindi = "अंक २ चन्द्रमा का प्रतीक है, जो गहन भावनात्मक समझ, शांति और कल्पनाशीलता प्रदान करता है।",
            descriptionHindi = "आप मिलनसार, कलात्मक, सौम्य और दूसरों की भावनाओं को समझने वाले हैं। शांतिप्रिय स्वभाव के कारण विवादों से दूर रहना पसंद करते हैं।",
            strengthsHindi = listOf("सहानुभूति व अंतर्ज्ञान", "कलात्मक रुचि", "शांतिप्रियता", "उत्कृष्ट सहयोगी"),
            cautionsHindi = listOf("मन के उतार-चढ़ाव और असमंजस से बचें", "अति-भावुकता पर नियंत्रण रखें", "नकारात्मक विचारों से दूर रहें"),
            favorableNumbers = listOf(1, 2, 4, 7),
            neutralNumbers = listOf(3, 6),
            challengingNumbers = listOf(8, 9),
            favorableDaysHindi = listOf("सोमवार", "रविवार"),
            favorableColorsHindi = listOf("सफेद", "क्रीम", "हल्का हरा"),
            remediesHindi = listOf("माताजी का आशीर्वाद लें", "पूर्णिमा के दिन ध्यान करें", "चांदी के पात्र से जल पिएं")
        ),
        3 to NumberSignification(
            rootNumber = 3,
            rulingPlanet = "Jupiter",
            rulingPlanetHindi = "बृहस्पति / गुरु (Guru)",
            titleHindi = "मूलांक ३ — ज्ञान, रचनात्मकता एवं मार्गदर्शन",
            summaryHindi = "अंक ३ देवगुरु बृहस्पति का प्रतीक है, जो ज्ञान, उच्च विचार और सामाजिक प्रतिष्ठा का सूचक है।",
            descriptionHindi = "आप बुद्धिमत्ता, आशावादी सोच और ज्ञान साझा करने की प्रवृत्ति से संपन्न हैं। शिक्षा, परामर्श, साहित्य अथवा उच्च उत्तरदायित्व के क्षेत्रों में प्रभावशाली रहते हैं।",
            strengthsHindi = listOf("विशाल दृष्टिकोण", "ज्ञानवान व आशावादी", "प्रभावशाली संवाद", "धार्मिक व नैतिक निष्ठा"),
            cautionsHindi = listOf("अति-उत्साह में व्यय न करें", "दूसरों पर अपनी राय थोपने से बचें", "अनुशासन बनाए रखें"),
            favorableNumbers = listOf(1, 3, 5, 9),
            neutralNumbers = listOf(2, 7),
            challengingNumbers = listOf(6),
            favorableDaysHindi = listOf("गुरुवार", "मंगलवार"),
            favorableColorsHindi = listOf("पीला", "केसरिया", "सुनहरा"),
            remediesHindi = listOf("गुरुजनों व संतों का सम्मान करें", "विष्णु सहस्रनाम का पाठ करें", "केसर का तिलक लगाएं")
        ),
        4 to NumberSignification(
            rootNumber = 4,
            rulingPlanet = "Rahu",
            rulingPlanetHindi = "राहु (Rahu)",
            titleHindi = "मूलांक ४ — व्यावहारिक अनुशासन, नवीनता एवं संगठन",
            summaryHindi = "अंक ४ राहु का अंक है, जो तार्किक बुद्धि, लीक से हटकर सोचने की क्षमता और कर्मठता देता है।",
            descriptionHindi = "आप यथार्थवादी, योजनाबद्ध और दृढ़ निश्चयी हैं। कठिन परिस्थितियों में भी व्यावहारिक समाधान खोजने की आपकी क्षमता अद्भुत होती है।",
            strengthsHindi = listOf("उच्च संगठन क्षमता", "तार्किक सोच", "कठिन परिश्रम", "नवीन विचार"),
            cautionsHindi = listOf("अचानक आए संशय अथवा असमंजस से बचें", "हठी स्वभाव न रखें", "नियमों व कानूनों का आदर करें"),
            favorableNumbers = listOf(1, 4, 7, 8),
            neutralNumbers = listOf(5, 6),
            challengingNumbers = listOf(2, 9),
            favorableDaysHindi = listOf("शनिवार", "रविवार"),
            favorableColorsHindi = listOf("नीला", "धूसर (ग्रे)", "खाकी"),
            remediesHindi = listOf("प्रातः पक्षियों को दाना डालें", "शनिवार को भैरव जी या शिव जी की आराधना करें", "घर में सफाई रखें")
        ),
        5 to NumberSignification(
            rootNumber = 5,
            rulingPlanet = "Mercury",
            rulingPlanetHindi = "बुध (Budha)",
            titleHindi = "मूलांक ५ — बौद्धिक चपलता, संवाद एवं व्यापारिक कुशलता",
            summaryHindi = "अंक ५ बुध ग्रह का प्रतीक है, जो तीव्र बुद्धि, परिवर्तनशीलता, बहुमुखी प्रतिभा और अनुकूलनशीलता प्रदान करता है।",
            descriptionHindi = "आप त्वरित निर्णय लेने वाले, मिलनसार, विनोदी और नए अनुभवों के प्रेमी हैं। व्यापार, संचार और नेटवर्किंग में आपकी रुचि स्वाभाविक होती है।",
            strengthsHindi = listOf("तीव्र बुद्धि व हास्यबोध", "बहुमुखी प्रतिभा", "उत्कृष्ट संवाद कौशल", "त्वरित अनुकूलन"),
            cautionsHindi = listOf("जल्दबाजी व बेचैनी से बचें", "एकाग्रता बनाए रखें", "एक समय में एक लक्ष्य पर स्थिर रहें"),
            favorableNumbers = listOf(1, 3, 5, 6),
            neutralNumbers = listOf(7, 8),
            challengingNumbers = listOf(2, 4),
            favorableDaysHindi = listOf("बुधवार", "शुक्रवार"),
            favorableColorsHindi = listOf("हरा", "हल्का फिरोजी", "सफेद"),
            remediesHindi = listOf("तुलसी के पौधे को जल दें", "गाय को हरा चारा खिलाएं", "बुधवार को गणेश जी को दूर्वा अर्पित करें")
        ),
        6 to NumberSignification(
            rootNumber = 6,
            rulingPlanet = "Venus",
            rulingPlanetHindi = "शुक्र (Shukra)",
            titleHindi = "मूलांक ६ — सौंदर्य, सामंजस्य, कला एवं आकर्षण",
            summaryHindi = "अंक ६ शुक्र का प्रतीक है, जो सौंदर्य, प्रेम, भौतिक सुख, कलात्मक दृष्टि और पारिवारिक सौहार्द देता है।",
            descriptionHindi = "आप सुरुचिपूर्ण, आकर्षक, दयालु और परिवार तथा मित्रों के प्रति समर्पित हैं। सुख-सुविधाओं और कलात्मक वातावरण का निर्माण करना आपकी विशेषता है।",
            strengthsHindi = listOf("कलात्मक व सुरुचिपूर्ण स्वभाव", "पारिवारिक समर्पण", "आकर्षण व मधुर वाणी", "सामंजस्य निर्माण"),
            cautionsHindi = listOf("अति-विलासिता और दिखावे से बचें", "भावनाओं में अधिक निर्भर न हों", "स्वयं के स्वास्थ्य का ध्यान रखें"),
            favorableNumbers = listOf(5, 6, 8),
            neutralNumbers = listOf(2, 4, 7),
            challengingNumbers = listOf(1, 3),
            favorableDaysHindi = listOf("शुक्रवार", "बुधवार"),
            favorableColorsHindi = listOf("गुलाबी", "सफेद", "हल्का नीला"),
            remediesHindi = listOf("माता लक्ष्मी की स्तुति करें", "सुगंधित वस्तुओं व इत्र का सात्विक प्रयोग करें", "स्त्रियों का आदर करें")
        ),
        7 to NumberSignification(
            rootNumber = 7,
            rulingPlanet = "Ketu",
            rulingPlanetHindi = "केतु (Ketu)",
            titleHindi = "मूलांक ७ — अनुसंधान, अंतर्ज्ञान, आध्यात्मिकता एवं रहस्य",
            summaryHindi = "अंक ७ केतु का अंक है, जो गहन चिंतन, शोध, दार्शनिक सोच और आध्यात्मिक अन्वेषण का कारक है।",
            descriptionHindi = "आप एकांतप्रिय, विश्लेषणात्मक और स्वतंत्र चिंतक हैं। भौतिक जगत के पीछे छुपे रहस्यों और जीवन के उच्च सत्यों को जानने की आपकी तीव्र इच्छा होती है।",
            strengthsHindi = listOf("तीक्ष्ण अंतर्ज्ञान", "शोध व विश्लेषण", "दार्शनिक गहराई", "आत्म-निरीक्षण"),
            cautionsHindi = listOf("अकेलेपन व निराशावादी सोच से बचें", "दूसरों से अत्यधिक अपेक्षा न रखें", "व्यावहारिक बने रहें"),
            favorableNumbers = listOf(1, 2, 4, 7),
            neutralNumbers = listOf(5, 8),
            challengingNumbers = listOf(9),
            favorableDaysHindi = listOf("रविवार", "सोमवार"),
            favorableColorsHindi = listOf("हल्का पीला", "सफेद", "हल्का हरा"),
            remediesHindi = listOf("प्रतिदिन ध्यान व प्राणायाम करें", "कुत्ते को रोटी खिलाएं", "गणेश अथर्वशीर्ष का पाठ करें")
        ),
        8 to NumberSignification(
            rootNumber = 8,
            rulingPlanet = "Saturn",
            rulingPlanetHindi = "शनि (Shani)",
            titleHindi = "मूलांक ८ — धैर्य, न्याय, कठिन परिश्रम एवं संगठन",
            summaryHindi = "अंक ८ न्यायप्रिय शनि का प्रतीक है, जो दीर्घकालिक सफलता, अदम्य धैर्य, अनुशासन और उत्तरदायित्व प्रदान करता है।",
            descriptionHindi = "आप गंभीर, न्यायप्रिय, संघर्षशील और निरंतर प्रयास करने वाले हैं। जीवन में सफलता चाहे धीमी मिले, परंतु वह अत्यंत सुदृढ़ और स्थायी होती है।",
            strengthsHindi = listOf("अटूट धैर्य व सहनशीलता", "न्यायप्रियता", "दीर्घकालिक योजना", "दृढ़ इच्छाशक्ति"),
            cautionsHindi = listOf("नकारात्मकता व अलगाव से बचें", "कर्मचारियों व सहायकों के साथ मधुर व्यवहार रखें", "उदासीन न हों"),
            favorableNumbers = listOf(4, 5, 6, 8),
            neutralNumbers = listOf(3, 7),
            challengingNumbers = listOf(1, 2, 9),
            favorableDaysHindi = listOf("शनिवार", "शुक्रवार"),
            favorableColorsHindi = listOf("गहरा नीला", "काला", "धूसर"),
            remediesHindi = listOf("शनिवार को पीपल के वृक्ष के नीचे सरसों के तेल का दीपक जलाएं", "गरीबों की सहायता करें", "हनुमान चालीसा का पाठ करें")
        ),
        9 to NumberSignification(
            rootNumber = 9,
            rulingPlanet = "Mars",
            rulingPlanetHindi = "मंगल (Mangala)",
            titleHindi = "मूलांक ९ — पराक्रम, ऊर्जा, साहस एवं सेवा",
            summaryHindi = "अंक ९ मंगल ग्रह का प्रतीक है, जो असीम ऊर्जा, निडरता, साहसिक कार्य और परोपकार की भावना देता है।",
            descriptionHindi = "आप उत्साही, साहसी, स्पष्टवादी और दूसरों की रक्षा व सहायता हेतु तत्पर रहने वाले हैं। चुनौतियों का सामना पूरे आत्मबल के साथ करते हैं।",
            strengthsHindi = listOf("अद्भुत साहस व पराक्रम", "परोपकारी व स्पष्ट दृष्टिकोण", "त्वरित कर्म", "नेतृत्व की क्षमता"),
            cautionsHindi = listOf("क्रोध व उत्तेजना पर नियंत्रण रखें", "विवादों में अनावश्यक न उलझें", "धैर्य से निर्णय लें"),
            favorableNumbers = listOf(1, 3, 9),
            neutralNumbers = listOf(2, 5),
            challengingNumbers = listOf(4, 6, 8),
            favorableDaysHindi = listOf("मंगलवार", "गुरुवार"),
            favorableColorsHindi = listOf("लाल", "गुलाबी", "केसरिया"),
            remediesHindi = listOf("हनुमान जी की उपासना करें", "सुंदरकांड का पाठ करें", "रक्तदान अथवा सेवा कार्य करें")
        )
    )

    fun getSignification(rootNumber: Int): NumberSignification {
        val safeRoot = ((rootNumber - 1).mod(9)) + 1
        return SIGNIFICATIONS[safeRoot] ?: SIGNIFICATIONS[1]!!
    }

    fun getMasterNumberDescription(masterNumber: Int): Pair<String, String> {
        return when (masterNumber) {
            11 -> Pair(
                "मास्टर अंक ११ — आध्यात्मिक प्रकाश एवं अंतर्ज्ञान",
                "अंक ११ उच्च अंतर्ज्ञान और प्रेरणा का प्रतीक है। यह व्यावहारिक २ (चन्द्रमा) के गुणों को गहन आध्यात्मिक चेतना के साथ जोड़ता है।"
            )
            22 -> Pair(
                "मास्टर अंक २२ — महान निर्माता (Master Builder)",
                "अंक २२ व्यावहारिक संगठन और महान लक्ष्यों को यथार्थ में बदलने की असाधारण क्षमता दर्शाता है। यह अंक ४ (राहु/स्थिरता) की सर्वोच्च अभिव्यक्ति है।"
            )
            33 -> Pair(
                "मास्टर अंक ३३ — विश्व शिक्षक एवं करुणामय मार्गदर्शन",
                "अंक ३३ निस्वार्थ सेवा, आध्यात्मिक करुणा और सार्वभौमिक प्रेम का प्रतीक है। यह अंक ६ (शुक्र/समर्पण) की सर्वोच्च पराकाष्ठा है।"
            )
            else -> Pair("विशिष्ट अंक $masterNumber", "विशेष ऊर्जा संपन्न अंक।")
        }
    }
}
