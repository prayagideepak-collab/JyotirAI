package com.example.domain

import com.example.domain.engine.PanchangCalculator
import com.example.domain.models.Paksha
import com.example.domain.models.Tithi
import com.example.domain.models.NakshatraContext
import com.example.domain.models.NityaYoga
import com.example.domain.models.Karana
import org.junit.Assert.*
import org.junit.Test

class PanchangCalculatorTest {

    @Test
    fun `test all 30 Tithis programmatically`() {
        val expectedNames = listOf(
            "Pratipada", "Dvitiya", "Tritiya", "Chaturthi", "Panchami",
            "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
            "Ekadashi", "Dvadashi", "Trayodashi", "Chaturdashi", "Purnima",
            "Pratipada", "Dvitiya", "Tritiya", "Chaturthi", "Panchami",
            "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
            "Ekadashi", "Dvadashi", "Trayodashi", "Chaturdashi", "Amavasya"
        )
        for (i in 1..30) {
            val elongation = (i - 1) * 12.0 + 6.0
            val tithi = PanchangCalculator.calculateTithi(0.0, elongation)
            assertEquals(i, tithi.index)
            assertEquals(expectedNames[i - 1], tithi.name)
            if (i <= 15) {
                assertEquals(Paksha.SHUKLA, tithi.paksha)
            } else {
                assertEquals(Paksha.KRISHNA, tithi.paksha)
            }
        }
    }

    @Test
    fun `test all 27 Yogas programmatically`() {
        val expectedNames = listOf(
            "Vishkambha", "Priti", "Ayushman", "Saubhagya", "Shobhana",
            "Atiganda", "Sukarma", "Dhriti", "Shula", "Ganda",
            "Vriddhi", "Dhruva", "Vyaghata", "Harshana", "Vajra",
            "Siddhi", "Vyatipata", "Variyan", "Parigha", "Shiva",
            "Siddha", "Sadhya", "Shubha", "Shukla", "Brahma",
            "Indra", "Vaidhriti"
        )
        val span = 360.0 / 27.0
        for (i in 1..27) {
            val sum = (i - 1) * span + (span / 2.0)
            val yoga = PanchangCalculator.calculateYoga(0.0, sum)
            assertEquals(i, yoga.index)
            assertEquals(expectedNames[i - 1], yoga.name)
        }
    }

    @Test
    fun `test all 27 Nakshatras and 4 Padas`() {
        val span = 360.0 / 27.0
        val padaSpan = span / 4.0
        for (i in 0..26) {
            for (p in 1..4) {
                val lon = i * span + (p - 1) * padaSpan + (padaSpan / 2.0)
                val nak = PanchangCalculator.calculateNakshatra(lon)
                assertEquals(i, nak.nakshatra.index)
                assertEquals(p, nak.pada)
            }
        }
    }

    @Test
    fun `test all 60 Karana indices`() {
        val expectedMovable = listOf("Bava", "Balava", "Kaulava", "Taitila", "Gara", "Vanija", "Vishti")
        for (i in 1..60) {
            val elongation = (i - 1) * 6.0 + 3.0
            val karana = PanchangCalculator.calculateKarana(0.0, elongation)
            assertEquals(i, karana.index)
            when (i) {
                1 -> {
                    assertEquals("Kimstughna", karana.name)
                    assertTrue(karana.isFixed)
                }
                58 -> {
                    assertEquals("Shakuni", karana.name)
                    assertTrue(karana.isFixed)
                }
                59 -> {
                    assertEquals("Chatushpada", karana.name)
                    assertTrue(karana.isFixed)
                }
                60 -> {
                    assertEquals("Naga", karana.name)
                    assertTrue(karana.isFixed)
                }
                else -> {
                    val expectedName = expectedMovable[(i - 2) % 7]
                    assertEquals(expectedName, karana.name)
                    assertFalse(karana.isFixed)
                }
            }
        }
    }

    @Test
    fun `test Tithi Boundaries and Wraparound`() {
        val t1 = PanchangCalculator.calculateTithi(0.0, 0.0)
        assertEquals(1, t1.index)
        assertEquals("Pratipada", t1.name)
        assertEquals(Paksha.SHUKLA, t1.paksha)
        assertEquals(1.0, t1.remainingPercentage, 0.001)

        val t2 = PanchangCalculator.calculateTithi(0.0, 11.99)
        assertEquals(1, t2.index)
        assertEquals(0.0008, t2.remainingPercentage, 0.01)

        val t3 = PanchangCalculator.calculateTithi(0.0, 12.0)
        assertEquals(2, t3.index)
        assertEquals("Dvitiya", t3.name)

        val t4 = PanchangCalculator.calculateTithi(0.0, 179.99)
        assertEquals(15, t4.index)
        assertEquals("Purnima", t4.name)
        assertEquals(Paksha.SHUKLA, t4.paksha)

        val t5 = PanchangCalculator.calculateTithi(0.0, 180.0)
        assertEquals(16, t5.index)
        assertEquals("Pratipada", t5.name)
        assertEquals(Paksha.KRISHNA, t5.paksha)

        val t6 = PanchangCalculator.calculateTithi(0.0, 359.99)
        assertEquals(30, t6.index)
        assertEquals("Amavasya", t6.name)
        assertEquals(Paksha.KRISHNA, t6.paksha)

        val t7 = PanchangCalculator.calculateTithi(358.0, 2.0)
        assertEquals(1, t7.index)
        assertEquals(0.666, t7.remainingPercentage, 0.01)
    }

    @Test
    fun `test Nakshatra Boundaries and Pada`() {
        val n1 = PanchangCalculator.calculateNakshatra(0.0)
        assertEquals(0, n1.nakshatra.index)
        assertEquals("Ashwini", n1.nakshatra.sanskritName)
        assertEquals(1, n1.pada)
        assertEquals(1.0, n1.remainingPercentage, 0.001)

        val n2 = PanchangCalculator.calculateNakshatra(3.33)
        assertEquals(0, n2.nakshatra.index)
        assertEquals(1, n2.pada)

        val n3 = PanchangCalculator.calculateNakshatra(3.34)
        assertEquals(0, n3.nakshatra.index)
        assertEquals(2, n3.pada)

        val n4 = PanchangCalculator.calculateNakshatra(13.33)
        assertEquals(0, n4.nakshatra.index)
        assertEquals(4, n4.pada)
        
        val n5 = PanchangCalculator.calculateNakshatra(13.34)
        assertEquals(1, n5.nakshatra.index)
        assertEquals("Bharani", n5.nakshatra.sanskritName)
        assertEquals(1, n5.pada)

        val n6 = PanchangCalculator.calculateNakshatra(359.99)
        assertEquals(26, n6.nakshatra.index)
        assertEquals("Revati", n6.nakshatra.sanskritName)
        assertEquals(4, n6.pada)
    }

    @Test
    fun `test Yoga Boundaries and Wraparound`() {
        val y1 = PanchangCalculator.calculateYoga(0.0, 0.0)
        assertEquals(1, y1.index)
        assertEquals("Vishkambha", y1.name)

        val y2 = PanchangCalculator.calculateYoga(10.0, 3.33)
        assertEquals(1, y2.index)

        val y3 = PanchangCalculator.calculateYoga(10.0, 3.34)
        assertEquals(2, y3.index)
        assertEquals("Priti", y3.name)

        val y4 = PanchangCalculator.calculateYoga(350.0, 15.0)
        assertEquals(1, y4.index)
        
        val y5 = PanchangCalculator.calculateYoga(180.0, 179.99)
        assertEquals(27, y5.index)
        assertEquals("Vaidhriti", y5.name)
    }
}
