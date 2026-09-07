package com.example.domain.reading

import androidx.camera.core.CameraSelector
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

        coordinator.processFaceFrame(
            faceDetected = true,
            lighting = 0.8f,
            sharpness = 0.8f,
            symmetry = 0.9f,
            distanceRatio = 0.6f
        )

        // Discard results must clear temporary session buffers and state
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
