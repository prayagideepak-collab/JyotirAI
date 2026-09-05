package com.example.domain.interpretation

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
import java.time.ZonedDateTime

class VedicInterpretationEngineTest {

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

    private val sampleTransitMoment = ZonedDateTime.of(
        LocalDate.of(2025, 6, 1),
        LocalTime.of(12, 0, 0),
        ZoneId.of("Asia/Kolkata")
    )

    @Before
    fun setUp() {
        engine = SwissEphAstrologyEngine()
    }

    @Test
    fun `interpretation is 100 percent deterministic for identical astrological inputs`() = runBlocking {
        val profile = engine.calculateProfile(sampleBirthData).getOrThrow()
        val dashaTimeline = engine.calculateDashaTimeline(sampleBirthData).getOrThrow()
        val transitSnapshot = engine.calculateTransitSnapshot(
            transitDateTime = sampleTransitMoment,
            location = sampleBirthData.location,
            natalProfile = profile
        ).getOrThrow()
        val panchangSnapshot = engine.calculatePanchang(
            date = sampleTransitMoment,
            location = sampleBirthData.location
        ).getOrThrow()

        val result1 = VedicInterpretationEngine.interpret(
            profile = profile,
            dashaTimeline = dashaTimeline,
            transitSnapshot = transitSnapshot,
            panchangSnapshot = panchangSnapshot
        )

        val result2 = VedicInterpretationEngine.interpret(
            profile = profile,
            dashaTimeline = dashaTimeline,
            transitSnapshot = transitSnapshot,
            panchangSnapshot = panchangSnapshot
        )

        assertEquals(result1.profileName, result2.profileName)
        assertEquals(result1.dominantFactor?.name, result2.dominantFactor?.name)
        assertEquals(result1.dominantFactor?.calculatedValue, result2.dominantFactor?.calculatedValue)
        assertEquals(result1.opportunities.size, result2.opportunities.size)
        assertEquals(result1.cautions.size, result2.cautions.size)
        assertEquals(result1.natalSummary, result2.natalSummary)
        assertEquals(result1.divisionalAnalysis.vargottamaPlanets, result2.divisionalAnalysis.vargottamaPlanets)
        assertEquals(result1.allInterpretationItems.size, result2.allInterpretationItems.size)
    }

    @Test
    fun `ethical boundaries and disclaimer are non-empty and non-fatalistic`() = runBlocking {
        val profile = engine.calculateProfile(sampleBirthData).getOrThrow()
        val result = VedicInterpretationEngine.interpret(profile = profile)

        assertNotNull(result.disclaimer)
        assertTrue(result.disclaimer.isNotBlank())
        assertTrue(result.disclaimer.contains("classical", ignoreCase = true))

        // Ensure claim boundaries are respected (no fatalistic assertions)
        val allText = (result.opportunities + result.cautions + result.traditionalGuidance + listOf(result.natalSummary)).joinToString(" ")
        assertFalse("Should not assert fatal outcomes", allText.contains("guaranteed death", ignoreCase = true))
        assertFalse("Should not assert guaranteed divorce", allText.contains("guaranteed divorce", ignoreCase = true))
        assertFalse("Should not assert fatal illness", allText.contains("fatal illness", ignoreCase = true))
    }

    @Test
    fun `D1 and D9 cross-interpretation correctly identifies vargottama planets`() = runBlocking {
        val profile = engine.calculateProfile(sampleBirthData).getOrThrow()
        val result = VedicInterpretationEngine.interpret(profile = profile)

        val divAnalysis = result.divisionalAnalysis
        assertTrue(divAnalysis.d9Available)
        assertNotNull(divAnalysis.d9Summary)

        // Verify Vargottama definition: planet in same sign in D1 and D9
        val d9Chart = VargaCalculator.calculateVargaChart(profile, VargaType.D9)
        assertNotNull(d9Chart)

        val expectedVargottama = mutableListOf<String>()
        profile.planetPositions.forEach { p1 ->
            val p9 = d9Chart.positions.firstOrNull { it.planet.equals(p1.planet, ignoreCase = true) }
            if (p9 != null && p1.signIndex == p9.signIndex) {
                expectedVargottama.add(p1.planet)
            }
        }

        assertEquals(expectedVargottama, divAnalysis.vargottamaPlanets)
    }

    @Test
    fun `D1 and D10 career analysis reflects tenth house and dignity`() = runBlocking {
        val profile = engine.calculateProfile(sampleBirthData).getOrThrow()
        val result = VedicInterpretationEngine.interpret(profile = profile)

        val divAnalysis = result.divisionalAnalysis
        assertTrue(divAnalysis.d10Available)
        assertTrue(divAnalysis.d10Summary.isNotBlank())
        assertTrue(divAnalysis.d10Summary.contains("10th House", ignoreCase = true) || divAnalysis.d10Summary.contains("D10", ignoreCase = true))
    }

    @Test
    fun `dasha and transit synergy is calculated when provided`() = runBlocking {
        val profile = engine.calculateProfile(sampleBirthData).getOrThrow()
        val dashaTimeline = engine.calculateDashaTimeline(sampleBirthData).getOrThrow()
        val transitSnapshot = engine.calculateTransitSnapshot(
            transitDateTime = sampleTransitMoment,
            location = sampleBirthData.location,
            natalProfile = profile
        ).getOrThrow()

        val result = VedicInterpretationEngine.interpret(
            profile = profile,
            dashaTimeline = dashaTimeline,
            transitSnapshot = transitSnapshot
        )

        assertNotNull(result.dashaContext)
        assertEquals(dashaTimeline.currentMahadasha?.planet?.lord, result.dashaContext?.mahadashaLord)
        assertNotNull(result.transitContext)
        assertEquals(profile.moonSign, result.transitContext?.referenceMoonSign)
    }

    @Test
    fun `truthful behavior when optional inputs are absent`() = runBlocking {
        val profile = engine.calculateProfile(sampleBirthData).getOrThrow()

        // Null dasha, transit, panchang
        val result = VedicInterpretationEngine.interpret(
            profile = profile,
            dashaTimeline = null,
            transitSnapshot = null,
            panchangSnapshot = null
        )

        assertNull(result.dashaContext)
        assertNull(result.transitContext)
        assertNull(result.panchangContext)

        // D1, Lagna, and Moon should still be richly interpreted
        assertTrue(result.natalSummary.isNotBlank())
        assertNotNull(result.dominantFactor)
        assertTrue(result.opportunities.isNotEmpty())
    }

    @Test
    fun `classical rules evaluate natural relationships correctly`() {
        // Sun friends: Moon, Mars, Jupiter
        assertEquals("Natural Friend (Mitra)", VedicInterpretationRules.getNaturalRelationship("Sun", "Moon"))
        assertEquals("Natural Friend (Mitra)", VedicInterpretationRules.getNaturalRelationship("Sun", "Mars"))
        assertEquals("Natural Friend (Mitra)", VedicInterpretationRules.getNaturalRelationship("Sun", "Jupiter"))

        // Sun enemies: Venus, Saturn
        assertEquals("Natural Adversary (Shatru)", VedicInterpretationRules.getNaturalRelationship("Sun", "Venus"))
        assertEquals("Natural Adversary (Shatru)", VedicInterpretationRules.getNaturalRelationship("Sun", "Saturn"))

        // Sun neutral: Mercury
        assertEquals("Neutral (Sama)", VedicInterpretationRules.getNaturalRelationship("Sun", "Mercury"))
    }
}
