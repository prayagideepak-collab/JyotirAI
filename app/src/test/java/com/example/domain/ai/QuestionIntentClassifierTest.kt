package com.example.domain.ai

import org.junit.Assert.*
import org.junit.Test

class QuestionIntentClassifierTest {

    @Test
    fun testIntentClassificationKeywords() {
        assertEquals(
            AstrologerIntent.CAREER,
            QuestionIntentClassifier.classify("मेरी नौकरी और करियर में पदोन्नति कब होगी?")
        )

        assertEquals(
            AstrologerIntent.MARRIAGE,
            QuestionIntentClassifier.classify("मेरा विवाह कब होगा और जीवनसाथी कैसा मिलेगा?")
        )

        assertEquals(
            AstrologerIntent.DASHA,
            QuestionIntentClassifier.classify("मेरी वर्तमान महादशा और अंतरदशा का क्या प्रभाव चल रहा है?")
        )

        assertEquals(
            AstrologerIntent.TRANSIT,
            QuestionIntentClassifier.classify("शनि की साढ़ेसाती और गुरु गोचर का फल बताएं")
        )

        assertEquals(
            AstrologerIntent.YOGA_DOSHA,
            QuestionIntentClassifier.classify("क्या मेरी कुण्डली में कालसर्प या मांगलिक दोष है?")
        )

        assertEquals(
            AstrologerIntent.NUMEROLOGY,
            QuestionIntentClassifier.classify("मेरा मूलांक और भाग्यांक क्या बताता है?")
        )

        assertEquals(
            AstrologerIntent.PANCHANG_MUHURTA,
            QuestionIntentClassifier.classify("आज का पंचांग, शुभ मुहूर्त और चौघड़िया क्या है?")
        )

        assertEquals(
            AstrologerIntent.GENERAL_KUNDLI,
            QuestionIntentClassifier.classify("मेरी कुंडली का सम्पूर्ण विश्लेषण और भविष्य फल बताइए")
        )
    }
}
