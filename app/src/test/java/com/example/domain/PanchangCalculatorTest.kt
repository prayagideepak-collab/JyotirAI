package com.example.domain

import com.example.domain.engine.PanchangCalculator
import com.example.domain.models.Paksha
import com.example.domain.models.Tithi
import com.example.domain.models.NakshatraContext
import com.example.domain.models.NityaYoga
import com.example.domain.models.Karana
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Method

class PanchangCalculatorTest {
    private fun invokeCalculateTithi(sunLon: Double, moonLon: Double): Tithi {
        val method: Method = PanchangCalculator::class.java.getDeclaredMethod("calculateTithi", Double::class.java, Double::class.java)
        method.isAccessible = true
        return method.invoke(PanchangCalculator, sunLon, moonLon) as Tithi
    }

    private fun invokeCalculateNakshatra(moonLon: Double): NakshatraContext {
        val method: Method = PanchangCalculator::class.java.getDeclaredMethod("calculateNakshatra", Double::class.java)
        method.isAccessible = true
        return method.invoke(PanchangCalculator, moonLon) as NakshatraContext
    }

    private fun invokeCalculateYoga(sunLon: Double, moonLon: Double): NityaYoga {
        val method: Method = PanchangCalculator::class.java.getDeclaredMethod("calculateYoga", Double::class.java, Double::class.java)
        method.isAccessible = true
        return method.invoke(PanchangCalculator, sunLon, moonLon) as NityaYoga
    }

    private fun invokeCalculateKarana(sunLon: Double, moonLon: Double): Karana {
        val method: Method = PanchangCalculator::class.java.getDeclaredMethod("calculateKarana", Double::class.java, Double::class.java)
        method.isAccessible = true
        return method.invoke(PanchangCalculator, sunLon, moonLon) as Karana
    }

    @Test
    fun `test Tithi Boundaries and Wraparound`() {
        // Amavasya end / Pratipada start
        val t1 = invokeCalculateTithi(0.0, 0.0)
        assertEquals(1, t1.index)
        assertEquals("Pratipada", t1.name)
        assertEquals(Paksha.SHUKLA, t1.paksha)
        assertEquals(1.0, t1.remainingPercentage, 0.001)

        val t2 = invokeCalculateTithi(0.0, 11.99)
        assertEquals(1, t2.index)
        assertEquals(0.0008, t2.remainingPercentage, 0.01)

        val t3 = invokeCalculateTithi(0.0, 12.0)
        assertEquals(2, t3.index)
        assertEquals("Dvitiya", t3.name)

        // Purnima
        val t4 = invokeCalculateTithi(0.0, 179.99)
        assertEquals(15, t4.index)
        assertEquals("Purnima", t4.name)
        assertEquals(Paksha.SHUKLA, t4.paksha)

        // Krishna Pratipada
        val t5 = invokeCalculateTithi(0.0, 180.0)
        assertEquals(16, t5.index)
        assertEquals("Pratipada", t5.name)
        assertEquals(Paksha.KRISHNA, t5.paksha)

        // Amavasya
        val t6 = invokeCalculateTithi(0.0, 359.99)
        assertEquals(30, t6.index)
        assertEquals("Amavasya", t6.name)
        assertEquals(Paksha.KRISHNA, t6.paksha)

        // Wraparound check (Moon = 2, Sun = 358 -> Diff = 4 -> Tithi 1)
        val t7 = invokeCalculateTithi(358.0, 2.0)
        assertEquals(1, t7.index)
        assertEquals(0.666, t7.remainingPercentage, 0.01)
    }

    @Test
    fun `test Nakshatra Boundaries and Pada`() {
        val n1 = invokeCalculateNakshatra(0.0)
        assertEquals(0, n1.nakshatra.index)
        assertEquals("Ashwini", n1.nakshatra.sanskritName)
        assertEquals(1, n1.pada)
        assertEquals(1.0, n1.remainingPercentage, 0.001)

        val n2 = invokeCalculateNakshatra(3.33)
        assertEquals(0, n2.nakshatra.index)
        assertEquals(1, n2.pada)

        val n3 = invokeCalculateNakshatra(3.34)
        assertEquals(0, n3.nakshatra.index)
        assertEquals(2, n3.pada)

        // End of Ashwini
        val n4 = invokeCalculateNakshatra(13.33)
        assertEquals(0, n4.nakshatra.index)
        assertEquals(4, n4.pada)
        
        // Start of Bharani
        val n5 = invokeCalculateNakshatra(13.34)
        assertEquals(1, n5.nakshatra.index)
        assertEquals("Bharani", n5.nakshatra.sanskritName)
        assertEquals(1, n5.pada)

        // Revati end
        val n6 = invokeCalculateNakshatra(359.99)
        assertEquals(26, n6.nakshatra.index)
        assertEquals("Revati", n6.nakshatra.sanskritName)
        assertEquals(4, n6.pada)
    }

    @Test
    fun `test Yoga Boundaries and Wraparound`() {
        val y1 = invokeCalculateYoga(0.0, 0.0)
        assertEquals(1, y1.index)
        assertEquals("Vishkambha", y1.name)

        val y2 = invokeCalculateYoga(10.0, 3.33)
        assertEquals(1, y2.index)

        val y3 = invokeCalculateYoga(10.0, 3.34)
        assertEquals(2, y3.index)
        assertEquals("Priti", y3.name)

        // Wraparound (Sun = 350, Moon = 15 -> sum 365 -> 5)
        val y4 = invokeCalculateYoga(350.0, 15.0)
        assertEquals(1, y4.index)
        
        // End of Vaidhriti
        val y5 = invokeCalculateYoga(180.0, 179.99)
        assertEquals(27, y5.index)
        assertEquals("Vaidhriti", y5.name)
    }

    @Test
    fun `test Karana Sequences and Boundaries`() {
        // 1. Kimstughna
        val k1 = invokeCalculateKarana(0.0, 0.0)
        assertEquals(1, k1.index)
        assertEquals("Kimstughna", k1.name)
        assertTrue(k1.isFixed)

        // 2. Bava
        val k2 = invokeCalculateKarana(0.0, 6.0)
        assertEquals(2, k2.index)
        assertEquals("Bava", k2.name)
        assertFalse(k2.isFixed)

        // 8. Vishti
        val k8 = invokeCalculateKarana(0.0, 42.0)
        assertEquals(8, k8.index)
        assertEquals("Vishti", k8.name)
        assertFalse(k8.isFixed)

        // 9. Bava (repeats)
        val k9 = invokeCalculateKarana(0.0, 48.0)
        assertEquals(9, k9.index)
        assertEquals("Bava", k9.name)

        // 57. Vishti
        val k57 = invokeCalculateKarana(0.0, 336.0)
        assertEquals(57, k57.index)
        assertEquals("Vishti", k57.name)

        // 58. Shakuni
        val k58 = invokeCalculateKarana(0.0, 342.0)
        assertEquals(58, k58.index)
        assertEquals("Shakuni", k58.name)
        assertTrue(k58.isFixed)

        // 59. Chatushpada
        val k59 = invokeCalculateKarana(0.0, 348.0)
        assertEquals(59, k59.index)
        assertEquals("Chatushpada", k59.name)
        assertTrue(k59.isFixed)

        // 60. Naga
        val k60 = invokeCalculateKarana(0.0, 354.0)
        assertEquals(60, k60.index)
        assertEquals("Naga", k60.name)
        assertTrue(k60.isFixed)
    }
}
