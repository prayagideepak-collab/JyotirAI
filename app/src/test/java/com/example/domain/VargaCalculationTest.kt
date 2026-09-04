package com.example.domain

import com.example.data.engine.SwissEphAstrologyEngine
import com.example.domain.engine.VargaCalculator
import com.example.domain.models.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class VargaCalculationTest {

    private lateinit var engine: SwissEphAstrologyEngine

    private val sampleBirthData = BirthData(
        name = "Aarav Sharma",
        date = LocalDate.of(1995, 8, 15),
        time = LocalTime.of(14, 30, 0),
        location = BirthLocation(
            latitude = 28.6139,
            longitude = 77.2090,
            placeName = "New Delhi, India"
        ),
        timeZone = ZoneId.of("Asia/Kolkata")
    )

    @Before
    fun setUp() {
        engine = SwissEphAstrologyEngine()
    }

    @Test
    fun `D1 Rashi chart matches base planetary positions`() = runBlocking {
        val chartResult = engine.calculateChart(sampleBirthData, "D1")
        assertTrue(chartResult.isSuccess)

        val chart = chartResult.getOrThrow()
        assertEquals("D1", chart.type)
        assertEquals(VargaType.D1, chart.vargaType)
        assertEquals(9, chart.positions.size)

        val profile = engine.calculateProfile(sampleBirthData).getOrThrow()
        for (i in profile.planetPositions.indices) {
            val basePos = profile.planetPositions[i]
            val chartPos = chart.positions[i]
            assertEquals(basePos.planet, chartPos.planet)
            assertEquals(basePos.signIndex, chartPos.signIndex)
            assertEquals(basePos.house, chartPos.house)
            assertEquals(basePos.totalLongitude, chartPos.totalLongitude, 0.0001)
        }
    }

    @Test
    fun `D9 Navamsha calculations obey classical Parashari rules`() {
        // Test 1: Movable Sign (Aries = 0)
        // 0°00' - 3°20' (Pada 1): Aries (0)
        assertEquals(0, VargaCalculator.calculateVargaSign(1.0, VargaType.D9))
        // 3°20' - 6°40' (Pada 2): Taurus (1)
        assertEquals(1, VargaCalculator.calculateVargaSign(4.0, VargaType.D9))
        // 26°40' - 30°00' (Pada 9): Sagittarius (8)
        assertEquals(8, VargaCalculator.calculateVargaSign(28.0, VargaType.D9))

        // Test 2: Fixed Sign (Taurus = 1, starts from 9th = Capricorn / 9)
        // 0°00' - 3°20' in Taurus (30° - 33°20' total): Capricorn (9)
        assertEquals(9, VargaCalculator.calculateVargaSign(31.0, VargaType.D9))
        // 3°20' - 6°40' in Taurus (33°20' - 36°40' total): Aquarius (10)
        assertEquals(10, VargaCalculator.calculateVargaSign(34.0, VargaType.D9))

        // Test 3: Dual Sign (Gemini = 2, starts from 5th = Libra / 6)
        // 0°00' - 3°20' in Gemini (60° - 63°20' total): Libra (6)
        assertEquals(6, VargaCalculator.calculateVargaSign(61.0, VargaType.D9))
        // 3°20' - 6°40' in Gemini (63°20' - 66°40' total): Scorpio (7)
        assertEquals(7, VargaCalculator.calculateVargaSign(64.0, VargaType.D9))

        // Test 4: Final Pada of Zodiac (Revati Pada 4 in Pisces: 356°40' - 360°00'): Pisces (11)
        assertEquals(11, VargaCalculator.calculateVargaSign(358.5, VargaType.D9))
    }

    @Test
    fun `D10 Dashamsha calculations obey odd and even sign rules`() {
        // Test 1: Odd Sign (Aries = 0, starts from Aries = 0)
        // 0° - 3°: Aries (0)
        assertEquals(0, VargaCalculator.calculateVargaSign(1.5, VargaType.D10))
        // 3° - 6°: Taurus (1)
        assertEquals(1, VargaCalculator.calculateVargaSign(4.5, VargaType.D10))
        // 27° - 30°: Capricorn (9)
        assertEquals(9, VargaCalculator.calculateVargaSign(28.5, VargaType.D10))

        // Test 2: Even Sign (Taurus = 1, starts from 9th sign from Taurus = Capricorn / 9)
        // 0° - 3° in Taurus (30° - 33° total): Capricorn (9)
        assertEquals(9, VargaCalculator.calculateVargaSign(31.5, VargaType.D10))
        // 3° - 6° in Taurus (33° - 36° total): Aquarius (10)
        assertEquals(10, VargaCalculator.calculateVargaSign(34.5, VargaType.D10))
        // 27° - 30° in Taurus (57° - 60° total): Libra (6) -> (1 + 8 + 9) % 12 = 18 % 12 = 6 (Libra)
        assertEquals(6, VargaCalculator.calculateVargaSign(58.5, VargaType.D10))
    }

    @Test
    fun `D2 Hora calculations obey odd and even sign rules`() {
        // Odd sign (Aries = 0):
        // 0-15° -> Sun (Leo = 4)
        assertEquals(4, VargaCalculator.calculateVargaSign(7.5, VargaType.D2))
        // 15-30° -> Moon (Cancer = 3)
        assertEquals(3, VargaCalculator.calculateVargaSign(22.5, VargaType.D2))

        // Even sign (Taurus = 1):
        // 0-15° (30-45° total) -> Moon (Cancer = 3)
        assertEquals(3, VargaCalculator.calculateVargaSign(37.5, VargaType.D2))
        // 15-30° (45-60° total) -> Sun (Leo = 4)
        assertEquals(4, VargaCalculator.calculateVargaSign(52.5, VargaType.D2))
    }

    @Test
    fun `D3 Drekkana calculations obey 1st 5th 9th rules`() {
        // Aries (0):
        // 0-10°: Aries (0)
        assertEquals(0, VargaCalculator.calculateVargaSign(5.0, VargaType.D3))
        // 10-20°: Leo (4) [5th from Aries]
        assertEquals(4, VargaCalculator.calculateVargaSign(15.0, VargaType.D3))
        // 20-30°: Sagittarius (8) [9th from Aries]
        assertEquals(8, VargaCalculator.calculateVargaSign(25.0, VargaType.D3))
    }

    @Test
    fun `Varga charts maintain whole-sign house calculation from Varga Lagna`() = runBlocking {
        val d9Chart = engine.calculateChart(sampleBirthData, "D9").getOrThrow()
        assertEquals(VargaType.D9, d9Chart.vargaType)

        val d9LagnaSignIndex = d9Chart.ascendantSignIndex

        for (pos in d9Chart.positions) {
            val expectedHouse = ((pos.signIndex - d9LagnaSignIndex).mod(12)) + 1
            assertEquals("D9 house for ${pos.planet} must be whole-sign relative to D9 Lagna", expectedHouse, pos.house)
        }

        val d10Chart = engine.calculateChart(sampleBirthData, "D10").getOrThrow()
        assertEquals(VargaType.D10, d10Chart.vargaType)
        val d10LagnaSignIndex = d10Chart.ascendantSignIndex

        for (pos in d10Chart.positions) {
            val expectedHouse = ((pos.signIndex - d10LagnaSignIndex).mod(12)) + 1
            assertEquals("D10 house for ${pos.planet} must be whole-sign relative to D10 Lagna", expectedHouse, pos.house)
        }
    }

    @Test
    fun `Chart caching produces identical results on subsequent calls`() = runBlocking {
        val chart1 = engine.calculateChart(sampleBirthData, "D9").getOrThrow()
        val chart2 = engine.calculateChart(sampleBirthData, "D9").getOrThrow()

        assertSame("Subsequent calls must retrieve cached instance", chart1, chart2)
        assertEquals(chart1.ascendantSignIndex, chart2.ascendantSignIndex)
        assertEquals(chart1.positions.size, chart2.positions.size)
    }
}
