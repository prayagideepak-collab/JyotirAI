package com.example.domain.reading

import androidx.camera.core.CameraSelector
import com.example.domain.models.FaceLandmarkPoint
import com.example.domain.models.PalmLandmarkPoint
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CameraReadingPrivacyTest {

    private lateinit var coordinator: CameraReadingCoordinator

    @Before
    fun setUp() {
        coordinator = CameraReadingCoordinator()
    }

    @Test
    fun testSessionIsolationAndMutex() {
        assertEquals(ReadingSessionMode.IDLE, coordinator.sessionMode.value)

        // Start Palm session
        assertTrue(coordinator.startPalmSession())
        assertEquals(ReadingSessionMode.PALM_GUIDANCE, coordinator.sessionMode.value)

        // Attempt concurrent Face session -> Must be rejected (Mutex & Isolation)
        assertFalse(coordinator.startFaceSession())

        coordinator.stopAndCleanup()
        assertEquals(ReadingSessionMode.IDLE, coordinator.sessionMode.value)

        // Now start Face session
        assertTrue(coordinator.startFaceSession())
        assertEquals(ReadingSessionMode.FACE_GUIDANCE, coordinator.sessionMode.value)

        // Attempt concurrent Palm session -> Must be rejected
        assertFalse(coordinator.startPalmSession())
    }

    @Test
    fun testRawImageAndLandmarkPrivacyDisposal() {
        coordinator.startFaceSession()

        val dummyFaceLandmarks = listOf(
            FaceLandmarkPoint(0.5f, 0.2f, 0.05f, "FOREHEAD_TOP"),
            FaceLandmarkPoint(0.5f, 0.55f, 0.2f, "NOSE_TIP")
        )

        // Stream 10 usable frames to trigger analysis
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

        // Analysis completed successfully, result produced
        assertEquals(ReadingSessionMode.FACE_RESULT, coordinator.sessionMode.value)
        assertNotNull(coordinator.faceResult.value)

        // Recapture / Discard results must clear temporary session buffers and state
        coordinator.discardReadingResults()
        assertEquals(ReadingSessionMode.IDLE, coordinator.sessionMode.value)
        assertNull(coordinator.faceResult.value)
    }

    @Test
    fun testLensFacingStrictness() {
        assertEquals(CameraSelector.LENS_FACING_BACK, coordinator.getRequiredCameraForPalm())
        assertEquals(CameraSelector.LENS_FACING_FRONT, coordinator.getRequiredCameraForFace())
    }
}
