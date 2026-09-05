package com.example.domain.reading

import com.example.domain.models.FaceLandmarkPoint
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FaceReadingEngineTest {

    private val sampleLandmarks = listOf(
        FaceLandmarkPoint(0.5f, 0.2f, 0.05f, "FOREHEAD_TOP"),
        FaceLandmarkPoint(0.35f, 0.35f, 0.08f, "LEFT_EYEBROW"),
        FaceLandmarkPoint(0.65f, 0.35f, 0.08f, "RIGHT_EYEBROW"),
        FaceLandmarkPoint(0.38f, 0.42f, 0.1f, "LEFT_EYE"),
        FaceLandmarkPoint(0.62f, 0.42f, 0.1f, "RIGHT_EYE"),
        FaceLandmarkPoint(0.5f, 0.55f, 0.2f, "NOSE_TIP"),
        FaceLandmarkPoint(0.5f, 0.7f, 0.12f, "MOUTH_CENTER"),
        FaceLandmarkPoint(0.5f, 0.85f, 0.05f, "CHIN_TIP"),
        FaceLandmarkPoint(0.25f, 0.6f, 0f, "LEFT_CHEEK"),
        FaceLandmarkPoint(0.75f, 0.6f, 0f, "RIGHT_CHEEK")
    )

    @Test
    fun testFaceReading_generatesStructuredTriBhagaOutput() {
        val date = LocalDate.of(2026, 9, 5)
        val result = FaceReadingEngine.interpretFaceGeometry(
            landmarks = sampleLandmarks,
            aggregatedFrameCount = 10,
            targetDate = date
        )

        assertNotNull(result)
        assertEquals(date, result.readingDate)
        assertEquals(10, result.aggregatedFramesCount)
        assertTrue(result.faceArchetype.isNotEmpty())

        // Verify temporal structure
        assertTrue(result.temporalReading.todayFocus.isNotEmpty())
        assertTrue(result.temporalReading.pastInfluence.isNotEmpty())
        assertTrue(result.temporalReading.futurePotential.isNotEmpty())

        // Verify Three Classical Zones (Tri-Bhaga)
        assertEquals(3, result.zones.size)
        val zoneNames = result.zones.map { it.zoneName }
        assertTrue(zoneNames.any { it.contains("Upper Zone") })
        assertTrue(zoneNames.any { it.contains("Middle Zone") })
        assertTrue(zoneNames.any { it.contains("Lower Zone") })

        // Verify Features
        assertTrue(result.features.isNotEmpty())
        val featureNames = result.features.map { it.featureName }
        assertTrue(featureNames.any { it.contains("Eyes") })
        assertTrue(featureNames.any { it.contains("Nose") })

        // Verify Ethical Framing
        assertTrue(result.ethicalDisclaimer.contains("Mukh Samudrika"))
    }
}
