package com.example.domain.reading

import androidx.camera.core.CameraSelector
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
        assertEquals(CameraSelector.LENS_FACING_BACK, coordinator.getRequiredCameraForPalm())
        assertEquals(CameraSelector.LENS_FACING_FRONT, coordinator.getRequiredCameraForFace())
    }

    @Test
    fun testQualityFiltering_rejectsLowQualityFrames() {
        coordinator.startPalmSession()

        // Send unusable frame (poor lighting & low sharpness)
        coordinator.processPalmFrame(
            handDetected = true,
            lighting = 0.2f,
            sharpness = 0.2f,
            distanceRatio = 0.6f
        )

        val quality = coordinator.palmQuality.value
        assertFalse(quality.isUsable)
        assertNull(coordinator.palmResult.value)
    }

    @Test
    fun testQualityFiltering_acceptsUsableFrames() {
        coordinator.startPalmSession()

        coordinator.processPalmFrame(
            handDetected = true,
            lighting = 0.8f,
            sharpness = 0.8f,
            distanceRatio = 0.6f
        )

        val quality = coordinator.palmQuality.value
        assertTrue(quality.isUsable)
        assertEquals(ReadingSessionMode.PALM_CAPTURING, coordinator.sessionMode.value)
    }

    @Test
    fun testFaceQualityFiltering_acceptsUsableFrames() {
        coordinator.startFaceSession()

        coordinator.processFaceFrame(
            faceDetected = true,
            lighting = 0.8f,
            sharpness = 0.8f,
            symmetry = 0.9f,
            distanceRatio = 0.6f
        )

        val quality = coordinator.faceQuality.value
        assertTrue(quality.isUsable)
        assertEquals(ReadingSessionMode.FACE_CAPTURING, coordinator.sessionMode.value)
    }

    @Test
    fun testCleanup_clearsBuffersAndResetsStateToIdle() {
        coordinator.startPalmSession()
        coordinator.processPalmFrame(
            handDetected = true,
            lighting = 0.8f,
            sharpness = 0.8f,
            distanceRatio = 0.6f
        )

        coordinator.stopAndCleanup()
        assertEquals(ReadingSessionMode.IDLE, coordinator.sessionMode.value)

        coordinator.discardReadingResults()
        assertNull(coordinator.palmResult.value)
        assertNull(coordinator.faceResult.value)
    }
}
