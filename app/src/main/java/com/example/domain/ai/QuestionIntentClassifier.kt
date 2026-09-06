package com.example.domain.ai

/**
 * Deterministic classifier that categorizes natural language user questions into structured intents.
 * Works with Hindi, English, and Hinglish queries.
 */
object QuestionIntentClassifier {

    fun classify(question: String): Pair<AstrologerIntent, Double> {
        val q = question.lowercase().trim()

        if (q.isBlank()) {
            return Pair(AstrologerIntent.GENERAL_HOROSCOPE, 0.5)
        }

        // Keywords classification with hierarchical precedence
        return when {
            // Numerology Intent
            containsAny(q, listOf("अंकशास्त्र", "मूलांक", "भाग्यांक", "नामांक", "numerology", "mulank", "bhagyank", "life path", "destiny number", "lucky number", "lucky color")) ->
                Pair(AstrologerIntent.NUMEROLOGY_ANALYSIS, 0.95)

            // Muhurta Intent
            containsAny(q, listOf("मुहूर्त", "शुभ समय", "चौघड़िया", "गृह प्रवेश", "खरीदारी", "muhurta", "shubh muhurat", "auspicious time", "abhijit", "choghadiya")) ->
                Pair(AstrologerIntent.MUHURTA_GUIDANCE, 0.95)

            // Panchang Intent
            containsAny(q, listOf("पंचांग", "तिथि", "राहुकाल", "नक्षत्र", "सूर्योदय", "panchang", "tithi", "rahukaal", "rahu kaal", "sunrise", "sunset", "karana", "yoga")) ->
                Pair(AstrologerIntent.PANCHANG_AND_TIMING, 0.92)

            // Compatibility Intent
            containsAny(q, listOf("कुंडली मिलान", "गुण मिलान", "विवाह मिलान", "compatibility", "match making", "gun milan", "kundli milan", "ashtakoota")) ->
                Pair(AstrologerIntent.COMPATIBILITY_GUIDANCE, 0.95)

            // Dasha Intent
            containsAny(q, listOf("महादशा", "अंतर्दशा", "दशा कब", "दशा का प्रभाव", "dasha", "mahadasha", "antardasha", "shani dasha", "rahu dasha", "guru dasha")) ->
                Pair(AstrologerIntent.DASHA_EXPLANATION, 0.92)

            // Transit / Gochar / Sade Sati Intent
            containsAny(q, listOf("गोचर", "साढ़े साती", "ढैय्या", "शनि की साढ़े साती", "transit", "gochar", "sade sati", "kantaka shani", "dhaiya")) ->
                Pair(AstrologerIntent.TRANSIT_GOCHAR_EXPLANATION, 0.92)

            // Yoga / Dosha Intent
            containsAny(q, listOf("योग", "दोष", "मांगलिक", "कालसर्प", "गजकेसरी", "राजयोग", "yoga", "dosha", "manglik", "kalsarp", "gajakesari", "raj yoga", "kemadruma")) ->
                Pair(AstrologerIntent.YOGA_AND_DOSHA_EXPLANATION, 0.90)

            // Career & Profession
            containsAny(q, listOf("करियर", "नौकरी", "व्यापार", "व्यवसाय", "पदोन्नति", "career", "job", "business", "promotion", "profession", "work", "office", "interview")) ->
                Pair(AstrologerIntent.CAREER_AND_PROFESSION, 0.88)

            // Education & Studies
            containsAny(q, listOf("शिक्षा", "पढ़ाई", "परीक्षा", "ज्ञान", "education", "studies", "exam", "college", "school", "degree", "competitive exam")) ->
                Pair(AstrologerIntent.EDUCATION_AND_STUDIES, 0.88)

            // Marriage & Relationships
            containsAny(q, listOf("विवाह", "शादी", "जीवनसाथी", "संबंध", "प्रेम", "marriage", "wedding", "spouse", "partner", "relationship", "love", "divorce")) ->
                Pair(AstrologerIntent.MARRIAGE_AND_RELATIONSHIPS, 0.88)

            // Finances & Wealth
            containsAny(q, listOf("धन", "पैसा", "संपत्ति", "आर्थिक", "कर्ज", "निवेश", "finance", "money", "wealth", "income", "debt", "loan", "investment", "rich")) ->
                Pair(AstrologerIntent.FINANCES_AND_WEALTH, 0.88)

            // Remedies & Guidance
            containsAny(q, listOf("उपाय", "पूजा", "शांति", "मंत्र", "रत्न", "दान", "remedy", "remedies", "gemstone", "puja", "mantra", "daan", "rudraksha")) ->
                Pair(AstrologerIntent.REMEDY_AND_GUIDANCE, 0.85)

            // Today / Daily guidance
            containsAny(q, listOf("आज का दिन", "आज क्या करें", "आज का राशिफल", "today", "aaj ka din", "daily guidance", "daily rashifal")) ->
                Pair(AstrologerIntent.TODAY_GUIDANCE, 0.85)

            // General Astrology concepts
            containsAny(q, listOf("क्या होता है", "किसे कहते हैं", "का अर्थ", "what is", "how does", "meaning of", "explain")) ->
                Pair(AstrologerIntent.GENERAL_ASTROLOGY_EXPLANATION, 0.80)

            else -> Pair(AstrologerIntent.GENERAL_HOROSCOPE, 0.65)
        }
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }
}
