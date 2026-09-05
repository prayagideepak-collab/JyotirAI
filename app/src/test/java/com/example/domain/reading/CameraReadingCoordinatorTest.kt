package com.example.domain.reading

import androidx.camera.core.CameraSelector
import com.example.domain.models.FaceLandmarkPoint
import com.example.domain.models.PalmLandmarkPoint
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CameraReadingCoordinatorTest {

    private lateinit var coordinator: CameraReadingCoordinator

    @Before
    fun setUp() {
        coordinator = CameraReadingCoordinator()
    }

    @Test
    fun testMutex_cannotStartPalmAndFaceConcurrently() {
        assertEquals(ReadingSessionMode.IDLE, coordinator.sessionMode.value)

        // Start Palm session
        val palmStarted = coordinator.startPalmSession()
        assertTrue(palmStarted)
        assertEquals(ReadingSessionMode.PALM_GUIDANCE, coordinator.sessionMode.value)

        // Attempt to start Face session while Palm is active -> MUST FAIL
        val faceStarted = coordinator.startFaceSession()
        assertFalse(faceStarted)
        assertEquals(ReadingSessionMode.PALM_GUIDANCE, coordinator.sessionMode.value)

        // Clean up session
        coordinator.stopAndCleanup()
        assertEquals(ReadingSessionMode.IDLE, coordinator.sessionMode.value)

        // Now start Face session
        val faceStartedAfterCleanup = coordinator.startFaceSession()
        assertTrue(faceStartedAfterCleanup)
        assertEquals(ReadingSessionMode.FACE_GUIDANCE, coordinator.sessionMode.value)

        // Attempt to start Palm session while Face is active -> MUST FAIL
        val palmStartedWhileFaceActive = coordinator.startPalmSession()
        assertFalse(palmStartedWhileFaceActive)
    }

    @Test
    fun testLensConstraints_backForPalm_frontForFace() {
        // Enforce Lens Constraints
        assertEquals(CameraSelector.LENS_FACING_BACK, coordinator.getRequiredCameraForPalm())
        assertEquals(CameraSelector.LENS_FACING_FRONT, coordinator.getRequiredCameraForFace())
    }

    @Test
    fun testQualityFiltering_rejectsLowQualityFrames() {
        coordinator.startPalmSession()

        // Send unusable frame (poor lighting & low sharpness)
        coordinator.processPalmFrame(
            handDetected = true,
            lighting = 0.2f, // too low (< 0.4)
            sharpness = 0.2f, // too low (< 0.4)
            landmarks = emptyList(),
            distanceRatio = 0.6f
        )

        val quality = coordinator.palmQuality.value
        assertFalse(quality.isUsable)
        assertEquals(0, quality.captureCompletenessPercent)
        assertNull(coordinator.palmResult.value)
    }

    @Test
    fun testMultiFrameAggregation_triggersResultOnThreshold() {
        coordinator.startPalmSession()

        val dummyLandmarks = listOf(
            PalmLandmarkPoint(0.5f, 0.85f, 0f, "WRIST"),
            PalmLandmarkPoint(0.35f, 0.55f, 0.1f, "INDEX_BASE"),
            PalmLandmarkPoint(0.48f, 0.5f, 0.12f, "MIDDLE_BASE"),
            PalmLandmarkPoint(0.6f, 0.52f, 0.1f, "RING_BASE"),
            PalmLandmarkPoint(0.72f, 0.58f, 0.08f, "PINKY_BASE")
        )

        // Stream 10 usable frames
        for (i in 1..10) {
            coordinator.processPalmFrame(
                handDetected = true,
                lighting = 0.8f,
                sharpness = 0.8f,
                landmarks = dummyLandmarks,
                distanceRatio = 0.6f
            )
        }

        assertEquals(ReadingSessionMode.PALM_RESULT, coordinator.sessionMode.value)
        assertNotNull(coordinator.palmResult.value)
        assertEquals(10, coordinator.palmResult.value?.aggregatedFramesCount)
    }

    @Test
    fun testFaceMultiFrameAggregation_triggersResultOnThreshold() {
        coordinator.startFaceSession()

        val dummyFaceLandmarks = listOf(
            FaceLandmarkPoint(0.5f, 0.2f, 0.05f, "FOREHEAD_TOP"),
            FaceLandmarkPoint(0.38f, 0.42f, 0.1f, "LEFT_EYE"),
            FaceLandmarkPoint(0.62f, 0.42f, 0.1f, "RIGHT_EYE"),
            FaceLandmarkPoint(0.5f, 0.55f, 0.2f, "NOSE_TIP"),
            FaceLandmarkPoint(0.5f, 0.7f, 0.12f, "MOUTH_CENTER")
        )

        // Stream 10 usable frames
        for (i in 1..10) {
            coordinator.processFaceFrame(
                faceDetected = true,
                lighting = 0.8f,
                sharpness = 0.8f,
                symmetry = 0.9f,
                landmarks = dummyFaceLandmarks,
                distanceRatio = 0.6f
            )
        }

        assertEquals(ReadingSessionMode.FACE_RESULT, coordinator.sessionMode.value)
        assertNotNull(coordinator.faceResult.value)
        assertEquals(10, coordinator.faceResult.value?.aggregatedFramesCount)
    }

    @Test
    fun testCleanup_clearsBuffersAndResetsStateToIdle() {
        coordinator.startPalmSession()
        coordinator.processPalmFrame(
            handDetected = true,
            lighting = 0.8f,
            sharpness = 0.8f,
            landmarks = emptyList(),
            distanceRatio = 0.6f
        )

        coordinator.stopAndCleanup()
        assertEquals(ReadingSessionMode.IDLE, coordinator.sessionMode.value)

        coordinator.discardReadingResults()
        assertNull(coordinator.palmResult.value)
        assertNull(coordinator.faceResult.value)
    }
}
