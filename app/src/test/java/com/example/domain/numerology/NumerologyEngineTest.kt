package com.example.domain.numerology

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class NumerologyEngineTest {

    private val engine = NumerologyEngineImpl()

    @Test
    fun testRootDigitReduction() {
        assertEquals(1, NumberReducer.getRootSingleDigit(10))
        assertEquals(7, NumberReducer.getRootSingleDigit(25))
        assertEquals(9, NumberReducer.getRootSingleDigit(99))
        assertEquals(3, NumberReducer.getRootSingleDigit(12))
    }

    @Test
    fun testMasterNumberPreservation() {
        val red11 = NumberReducer.reduce(29, preserveMasterNumbers = true)
        assertEquals(11, red11.first)

        val red22 = NumberReducer.reduce(22, preserveMasterNumbers = true)
        assertEquals(22, red22.first)

        val red33 = NumberReducer.reduce(33, preserveMasterNumbers = true)
        assertEquals(33, red33.first)
    }

    @Test
    fun testChaldeanNameNumerology() {
        // "RAM" -> R=2, A=1, M=4 -> Total = 7
        val steps = NameNumerologyResolver.calculate("RAM", NumerologyMethodology.CHALDEAN)
        assertEquals(7, steps?.reducedNumber)
    }

    @Test
    fun testPythagoreanNameNumerology() {
        // "A" -> 1, "B" -> 2, "C" -> 3 -> Total = 6
        val steps = NameNumerologyResolver.calculate("ABC", NumerologyMethodology.PYTHAGOREAN)
        assertEquals(6, steps?.reducedNumber)
    }

    @Test
    fun testFullNumerologyCalculation() {
        val birthDate = LocalDate.of(1990, 5, 15) // Birth Day 15 -> 6 (Venus)
        // 15 + 5 + 1990 = 1+5+5+1+9+9+0 = 30 -> 3 (Jupiter)
        val result = engine.calculate(
            birthDate = birthDate,
            profileName = "Deepak",
            methodology = NumerologyMethodology.CHALDEAN
        )

        assertNotNull(result.birthNumber)
        assertEquals(6, result.birthNumber?.finalNumber)
        assertEquals("शुक्र (Shukra)", result.birthNumber?.rulingPlanetHindi)

        assertNotNull(result.lifePathNumber)
        assertEquals(3, result.lifePathNumber?.finalNumber)
        assertEquals("बृहस्पति / गुरु (Guru)", result.lifePathNumber?.rulingPlanetHindi)

        assertNotNull(result.nameNumber)
        assertTrue(result.favorableNumbers.isNotEmpty())
        assertTrue(result.favorableDaysHindi.isNotEmpty())
        assertTrue(result.traditionalRemediesHindi.isNotEmpty())
    }
}
