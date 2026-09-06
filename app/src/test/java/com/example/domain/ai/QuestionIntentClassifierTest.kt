package com.example.domain.ai

import org.junit.Assert.*
import org.junit.Test

class QuestionIntentClassifierTest {

    @Test
    fun testIntentClassificationKeywords() {
        assertEquals(
            AstrologerIntent.CAREER_AND_PROFESSION,
            QuestionIntentClassifier.classify("मेरी नौकरी और करियर में पदोन्नति कब होगी?").first
        )

        assertEquals(
            AstrologerIntent.MARRIAGE_AND_RELATIONSHIPS,
            QuestionIntentClassifier.classify("मेरा विवाह कब होगा और जीवनसाथी कैसा मिलेगा?").first
        )

        assertEquals(
            AstrologerIntent.DASHA_EXPLANATION,
            QuestionIntentClassifier.classify("मेरी वर्तमान महादशा और अंतरदशा का क्या प्रभाव चल रहा है?").first
        )

        assertEquals(
            AstrologerIntent.TRANSIT_GOCHAR_EXPLANATION,
            QuestionIntentClassifier.classify("शनि की साढ़ेसाती और गुरु गोचर का फल बताएं").first
        )

        assertEquals(
            AstrologerIntent.YOGA_AND_DOSHA_EXPLANATION,
            QuestionIntentClassifier.classify("क्या मेरी कुण्डली में कालसर्प या मांगलिक दोष है?").first
        )

        assertEquals(
            AstrologerIntent.NUMEROLOGY_ANALYSIS,
            QuestionIntentClassifier.classify("मेरा मूलांक और भाग्यांक क्या बताता है?").first
        )

        assertEquals(
            AstrologerIntent.PANCHANG_AND_TIMING,
            QuestionIntentClassifier.classify("आज का पंचांग और तिथि क्या है?").first
        )

        assertEquals(
            AstrologerIntent.GENERAL_HOROSCOPE,
            QuestionIntentClassifier.classify("मेरी कुंडली का सम्पूर्ण विश्लेषण और भविष्य फल बताइए").first
        )
    }
}
