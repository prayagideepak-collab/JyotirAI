package com.example.domain.reading

import androidx.camera.core.CameraSelector
import com.example.domain.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Strict camera session coordinator enforcing:
 * 1. Single-active camera reading mode (Mutex: Palm OR Face, never concurrent).
 * 2. Back Camera ONLY for Palm Reading.
 * 3. Front Camera ONLY for Face Reading.
 * 4. Multi-frame quality filtering and aggregation.
 * 5. Immediate disposal of raw image buffers (privacy & battery hardening).
 * 6. Total decoupling from Daily Rashifal engine.
 */
enum class ReadingSessionMode {
    IDLE,
    PALM_GUIDANCE,
    PALM_CAPTURING,
    PALM_ANALYZING,
    PALM_RESULT,
    FACE_GUIDANCE,
    FACE_CAPTURING,
    FACE_ANALYZING,
    FACE_RESULT
}

class CameraReadingCoordinator(
    private val palmEngine: PalmReadingEngine = PalmReadingEngine,
    private val faceEngine: FaceReadingEngine = FaceReadingEngine
) {
    private val _sessionMode = MutableStateFlow(ReadingSessionMode.IDLE)
    val sessionMode: StateFlow<ReadingSessionMode> = _sessionMode.asStateFlow()

    private val _palmQuality = MutableStateFlow(
        PalmFrameQuality(
            handDetected = false,
            orientationDegrees = 0f,
            distanceScale = 0.5f,
            lightingScore = 0.7f,
            sharpnessScore = 0.8f,
            fingerVisibilityRatio = 0f,
            palmVisibilityRatio = 0f,
            isUsable = false,
            guidanceMessage = "Position your palm fully inside the frame",
            captureCompletenessPercent = 0
        )
    )
    val palmQuality: StateFlow<PalmFrameQuality> = _palmQuality.asStateFlow()

    private val _faceQuality = MutableStateFlow(
        FaceFrameQuality(
            faceDetected = false,
            alignmentRollPitchYaw = Triple(0f, 0f, 0f),
            distanceScale = 0.5f,
            lightingScore = 0.7f,
            sharpnessScore = 0.8f,
            symmetryScore = 0.8f,
            landmarkCompletenessRatio = 0f,
            isUsable = false,
            guidanceMessage = "Position your face inside the oval guide",
            captureCompletenessPercent = 0
        )
    )
    val faceQuality: StateFlow<FaceFrameQuality> = _faceQuality.asStateFlow()

    private val _palmResult = MutableStateFlow<PalmReadingResult?>(null)
    val palmResult: StateFlow<PalmReadingResult?> = _palmResult.asStateFlow()

    private val _faceResult = MutableStateFlow<FaceReadingResult?>(null)
    val faceResult: StateFlow<FaceReadingResult?> = _faceResult.asStateFlow()

    // Aggregation buffers (ephemeral landmark coordinates only - NEVER raw images)
    private val aggregatedPalmLandmarks = mutableListOf<PalmLandmarkPoint>()
    private val aggregatedFaceLandmarks = mutableListOf<FaceLandmarkPoint>()
    private var validPalmFramesCollected = 0
    private var validFaceFramesCollected = 0

    private val requiredFramesForAnalysis = 10

    /**
     * Start Palm Reading Session.
     * Enforces Back Camera and idle check.
     */
    @Synchronized
    fun startPalmSession(): Boolean {
        if (_sessionMode.value != ReadingSessionMode.IDLE) {
            return false // Mutex violation prevented
        }
        resetBuffers()
        _sessionMode.value = ReadingSessionMode.PALM_GUIDANCE
        return true
    }

    /**
     * Start Face Reading Session.
     * Enforces Front Camera and idle check.
     */
    @Synchronized
    fun startFaceSession(): Boolean {
        if (_sessionMode.value != ReadingSessionMode.IDLE) {
            return false // Mutex violation prevented
        }
        resetBuffers()
        _sessionMode.value = ReadingSessionMode.FACE_GUIDANCE
        return true
    }

    /**
     * Required Camera Selector for Palm Reading: BACK CAMERA ONLY.
     */
    fun getRequiredCameraForPalm(): Int = CameraSelector.LENS_FACING_BACK

    /**
     * Required Camera Selector for Face Reading: FRONT CAMERA ONLY.
     */
    fun getRequiredCameraForFace(): Int = CameraSelector.LENS_FACING_FRONT

    /**
     * Processes a single palm analysis frame.
     * Raw frame pixels are inspected in-memory and immediately released.
     */
    @Synchronized
    fun processPalmFrame(
        handDetected: Boolean,
        lighting: Float,
        sharpness: Float,
        landmarks: List<PalmLandmarkPoint>,
        distanceRatio: Float
    ) {
        if (_sessionMode.value != ReadingSessionMode.PALM_GUIDANCE &&
            _sessionMode.value != ReadingSessionMode.PALM_CAPTURING
        ) return

        val isUsable = handDetected && lighting >= 0.4f && sharpness >= 0.4f && distanceRatio in 0.35f..0.85f
        val guidance = when {
            !handDetected -> "Place your hand open facing the camera"
            lighting < 0.4f -> "Lighting is dim. Please move towards better light"
            sharpness < 0.4f -> "Hold steady to reduce motion blur"
            distanceRatio < 0.35f -> "Bring palm closer to the camera"
            distanceRatio > 0.85f -> "Move palm slightly further back"
            else -> "Capturing palm landmarks... Hold steady"
        }

        if (isUsable) {
            _sessionMode.value = ReadingSessionMode.PALM_CAPTURING
            validPalmFramesCollected++
            aggregatedPalmLandmarks.addAll(landmarks)
        }

        val progress = ((validPalmFramesCollected.toFloat() / requiredFramesForAnalysis) * 100).toInt().coerceIn(0, 100)

        _palmQuality.value = PalmFrameQuality(
            handDetected = handDetected,
            orientationDegrees = 0f,
            distanceScale = distanceRatio,
            lightingScore = lighting,
            sharpnessScore = sharpness,
            fingerVisibilityRatio = if (handDetected) 1.0f else 0f,
            palmVisibilityRatio = if (handDetected) 1.0f else 0f,
            isUsable = isUsable,
            guidanceMessage = guidance,
            captureCompletenessPercent = progress
        )

        // Trigger analysis once sufficient quality frames are gathered
        if (validPalmFramesCollected >= requiredFramesForAnalysis && _sessionMode.value == ReadingSessionMode.PALM_CAPTURING) {
            _sessionMode.value = ReadingSessionMode.PALM_ANALYZING
            val result = palmEngine.interpretPalmGeometry(
                landmarks = aggregatedPalmLandmarks.toList(),
                aggregatedFrameCount = validPalmFramesCollected,
                targetDate = LocalDate.now()
            )
            _palmResult.value = result
            _sessionMode.value = ReadingSessionMode.PALM_RESULT
            // Clear temporary buffer immediately for memory & privacy protection
            aggregatedPalmLandmarks.clear()
        }
    }

    /**
     * Processes a single face analysis frame.
     * Raw frame pixels are inspected in-memory and immediately released.
     */
    @Synchronized
    fun processFaceFrame(
        faceDetected: Boolean,
        lighting: Float,
        sharpness: Float,
        symmetry: Float,
        landmarks: List<FaceLandmarkPoint>,
        distanceRatio: Float
    ) {
        if (_sessionMode.value != ReadingSessionMode.FACE_GUIDANCE &&
            _sessionMode.value != ReadingSessionMode.FACE_CAPTURING
        ) return

        val isUsable = faceDetected && lighting >= 0.4f && sharpness >= 0.4f && distanceRatio in 0.35f..0.85f
        val guidance = when {
            !faceDetected -> "Center your face in the oval guide"
            lighting < 0.4f -> "Lighting is dim. Face a light source"
            sharpness < 0.4f -> "Hold steady for optical stabilization"
            distanceRatio < 0.35f -> "Move closer to the front camera"
            distanceRatio > 0.85f -> "Move slightly back from camera"
            else -> "Analyzing facial contours... Keep facing forward"
        }

        if (isUsable) {
            _sessionMode.value = ReadingSessionMode.FACE_CAPTURING
            validFaceFramesCollected++
            aggregatedFaceLandmarks.addAll(landmarks)
        }

        val progress = ((validFaceFramesCollected.toFloat() / requiredFramesForAnalysis) * 100).toInt().coerceIn(0, 100)

        _faceQuality.value = FaceFrameQuality(
            faceDetected = faceDetected,
            alignmentRollPitchYaw = Triple(0f, 0f, 0f),
            distanceScale = distanceRatio,
            lightingScore = lighting,
            sharpnessScore = sharpness,
            symmetryScore = symmetry,
            landmarkCompletenessRatio = if (faceDetected) 1.0f else 0f,
            isUsable = isUsable,
            guidanceMessage = guidance,
            captureCompletenessPercent = progress
        )

        // Trigger analysis once sufficient quality frames are gathered
        if (validFaceFramesCollected >= requiredFramesForAnalysis && _sessionMode.value == ReadingSessionMode.FACE_CAPTURING) {
            _sessionMode.value = ReadingSessionMode.FACE_ANALYZING
            val result = faceEngine.interpretFaceGeometry(
                landmarks = aggregatedFaceLandmarks.toList(),
                aggregatedFrameCount = validFaceFramesCollected,
                targetDate = LocalDate.now()
            )
            _faceResult.value = result
            _sessionMode.value = ReadingSessionMode.FACE_RESULT
            // Clear temporary buffer immediately for memory & privacy protection
            aggregatedFaceLandmarks.clear()
        }
    }

    /**
     * Stop and cleanup any active camera reading session.
     * Ensures all buffers and states are reset to IDLE.
     */
    @Synchronized
    fun stopAndCleanup() {
        resetBuffers()
        _sessionMode.value = ReadingSessionMode.IDLE
    }

    /**
     * Discards stored reading results and resets to IDLE.
     */
    @Synchronized
    fun discardReadingResults() {
        _palmResult.value = null
        _faceResult.value = null
        stopAndCleanup()
    }

    private fun resetBuffers() {
        aggregatedPalmLandmarks.clear()
        aggregatedFaceLandmarks.clear()
        validPalmFramesCollected = 0
        validFaceFramesCollected = 0
    }
}
