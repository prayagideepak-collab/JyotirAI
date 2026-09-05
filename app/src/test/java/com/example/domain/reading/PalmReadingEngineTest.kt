package com.example.domain.reading

import com.example.domain.models.PalmLandmarkPoint
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PalmReadingEngineTest {

    private val sampleLandmarks = listOf(
        PalmLandmarkPoint(0.5f, 0.85f, 0f, "WRIST"),
        PalmLandmarkPoint(0.35f, 0.55f, 0.1f, "INDEX_BASE"),
        PalmLandmarkPoint(0.48f, 0.5f, 0.12f, "MIDDLE_BASE"),
        PalmLandmarkPoint(0.6f, 0.52f, 0.1f, "RING_BASE"),
        PalmLandmarkPoint(0.72f, 0.58f, 0.08f, "PINKY_BASE"),
        PalmLandmarkPoint(0.2f, 0.65f, 0.05f, "THUMB_BASE"),
        PalmLandmarkPoint(0.12f, 0.55f, 0.02f, "THUMB_TIP"),
        PalmLandmarkPoint(0.32f, 0.25f, 0.05f, "INDEX_TIP"),
        PalmLandmarkPoint(0.48f, 0.18f, 0.05f, "MIDDLE_TIP"),
        PalmLandmarkPoint(0.62f, 0.24f, 0.05f, "RING_TIP"),
        PalmLandmarkPoint(0.75f, 0.35f, 0.03f, "PINKY_TIP"),
        PalmLandmarkPoint(0.48f, 0.68f, 0.15f, "PALM_CENTER")
    )

    @Test
    fun testPalmReading_generatesStructuredOutputWithAllPillars() {
        val date = LocalDate.of(2026, 9, 5)
        val result = PalmReadingEngine.interpretPalmGeometry(
            landmarks = sampleLandmarks,
            aggregatedFrameCount = 12,
            targetDate = date
        )

        assertNotNull(result)
        assertEquals(date, result.readingDate)
        assertEquals(12, result.aggregatedFramesCount)
        assertTrue(result.handType.isNotEmpty())

        // Verify temporal structure
        assertTrue(result.temporalReading.todayFocus.isNotEmpty())
        assertTrue(result.temporalReading.pastInfluence.isNotEmpty())
        assertTrue(result.temporalReading.futurePotential.isNotEmpty())

        // Verify Major Lines (Life, Head, Heart, Fate)
        assertTrue(result.majorLines.size >= 4)
        val lineNames = result.majorLines.map { it.lineName }
        assertTrue(lineNames.contains("Life Line"))
        assertTrue(lineNames.contains("Head Line"))
        assertTrue(lineNames.contains("Heart Line"))
        assertTrue(lineNames.contains("Fate / Karma Line"))

        // Verify Mounts
        assertTrue(result.mounts.isNotEmpty())
        val mountNames = result.mounts.map { it.mountName }
        assertTrue(mountNames.contains("Mount of Jupiter"))
        assertTrue(mountNames.contains("Mount of Venus"))

        // Verify Ethical Framing (no fatalism)
        assertTrue(result.ethicalDisclaimer.contains("Samudrika Shastra"))
    }
}
