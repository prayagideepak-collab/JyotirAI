package com.example.domain.reading

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.example.domain.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Unified Camera Session Coordinator enforcing:
 * 1. Single-active camera reading mode (Mutex: Palm OR Face, never concurrent).
 * 2. Back Camera ONLY for Palm Reading, Front Camera ONLY for Face Reading.
 * 3. Dual Capture System: Method A (Auto Capture) & Method B (Manual User Capture).
 * 4. Real optical detection & quality validation (no fake brightness-only simulation).
 * 5. Immediate disposal and cleanup of temporary private image files after analysis (privacy & battery hardening).
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
    FACE_RESULT,
    ERROR
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

    private val isCapturingOrAnalyzing = AtomicBoolean(false)
    private var activeTempImageFile: File? = null

    @Synchronized
    fun startPalmSession(): Boolean {
        if (_sessionMode.value != ReadingSessionMode.IDLE) {
            return false
        }
        resetState()
        _sessionMode.value = ReadingSessionMode.PALM_GUIDANCE
        return true
    }

    @Synchronized
    fun startFaceSession(): Boolean {
        if (_sessionMode.value != ReadingSessionMode.IDLE) {
            return false
        }
        resetState()
        _sessionMode.value = ReadingSessionMode.FACE_GUIDANCE
        return true
    }

    fun getRequiredCameraForPalm(): Int = CameraSelector.LENS_FACING_BACK
    fun getRequiredCameraForFace(): Int = CameraSelector.LENS_FACING_FRONT

    @Synchronized
    fun processPalmFrame(
        handDetected: Boolean,
        lighting: Float,
        sharpness: Float,
        distanceRatio: Float
    ) {
        if (_sessionMode.value != ReadingSessionMode.PALM_GUIDANCE &&
            _sessionMode.value != ReadingSessionMode.PALM_CAPTURING
        ) return

        val isUsable = handDetected && lighting >= 0.35f && sharpness >= 0.35f && distanceRatio in 0.3f..0.9f
        val guidance = when {
            !handDetected -> "Place your open palm facing the camera"
            lighting < 0.35f -> "Lighting is dim. Move to a well-lit area"
            sharpness < 0.35f -> "Hold steady to reduce blur"
            distanceRatio < 0.3f -> "Bring palm closer to camera"
            distanceRatio > 0.9f -> "Move palm slightly further back"
            else -> "Palm detected. Tap Capture or hold steady for Auto-Capture"
        }

        if (isUsable) {
            _sessionMode.value = ReadingSessionMode.PALM_CAPTURING
        }

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
            captureCompletenessPercent = if (isUsable) 100 else 40
        )
    }

    @Synchronized
    fun processFaceFrame(
        faceDetected: Boolean,
        lighting: Float,
        sharpness: Float,
        symmetry: Float,
        distanceRatio: Float
    ) {
        if (_sessionMode.value != ReadingSessionMode.FACE_GUIDANCE &&
            _sessionMode.value != ReadingSessionMode.FACE_CAPTURING
        ) return

        val isUsable = faceDetected && lighting >= 0.35f && sharpness >= 0.35f && distanceRatio in 0.3f..0.9f
        val guidance = when {
            !faceDetected -> "Center your face in the oval guide"
            lighting < 0.35f -> "Lighting is dim. Face a light source"
            sharpness < 0.35f -> "Hold steady for optimal clarity"
            distanceRatio < 0.3f -> "Move closer to front camera"
            distanceRatio > 0.9f -> "Move slightly back from camera"
            else -> "Face aligned. Tap Capture or hold steady for Auto-Capture"
        }

        if (isUsable) {
            _sessionMode.value = ReadingSessionMode.FACE_CAPTURING
        }

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
            captureCompletenessPercent = if (isUsable) 100 else 40
        )
    }

    /**
     * Method B: Manual User Capture for Palm Reading.
     */
    fun captureManualPalm(
        imageCapture: ImageCapture,
        context: Context,
        onSuccess: (PalmReadingResult) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isCapturingOrAnalyzing.compareAndSet(false, true)) {
            return // Prevent duplicate concurrent captures
        }
        if (!_palmQuality.value.isUsable) {
            isCapturingOrAnalyzing.set(false)
            onError("Palm requirements not met. Please adjust position.")
            return
        }

        _sessionMode.value = ReadingSessionMode.PALM_ANALYZING
        executePalmCapturePipeline(imageCapture, context, onSuccess, onError)
    }

    /**
     * Method A / Auto Capture for Palm Reading.
     */
    fun triggerAutoPalmCapture(
        imageCapture: ImageCapture,
        context: Context,
        onSuccess: (PalmReadingResult) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isCapturingOrAnalyzing.compareAndSet(false, true)) {
            return
        }
        if (!_palmQuality.value.isUsable) {
            isCapturingOrAnalyzing.set(false)
            return
        }

        _sessionMode.value = ReadingSessionMode.PALM_ANALYZING
        executePalmCapturePipeline(imageCapture, context, onSuccess, onError)
    }

    private fun executePalmCapturePipeline(
        imageCapture: ImageCapture,
        context: Context,
        onSuccess: (PalmReadingResult) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val photoFile = File(context.cacheDir, "temp_palm_${UUID.randomUUID()}.jpg")
            activeTempImageFile = photoFile
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            _sessionMode.value = ReadingSessionMode.PALM_ANALYZING

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        var analysisSuccess = false
                        try {
                            if (!photoFile.exists() || photoFile.length() <= 0L) {
                                _sessionMode.value = ReadingSessionMode.PALM_GUIDANCE
                                isCapturingOrAnalyzing.set(false)
                                onError("हथेली की स्पष्ट पहचान नहीं हो सकी।\nकृपया दोबारा कैप्चर करें।")
                                return
                            }

                            val bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
                            if (bitmap == null) {
                                _sessionMode.value = ReadingSessionMode.PALM_GUIDANCE
                                isCapturingOrAnalyzing.set(false)
                                onError("हथेली की स्पष्ट पहचान नहीं हो सकी।\nकृपया दोबारा कैप्चर करें।")
                                return
                            }

                            val result = palmEngine.interpretPalmImage(bitmap, LocalDate.now())
                            bitmap.recycle()

                            _palmResult.value = result
                            _sessionMode.value = ReadingSessionMode.PALM_RESULT
                            analysisSuccess = true
                            onSuccess(result)
                        } catch (e: Exception) {
                            _sessionMode.value = ReadingSessionMode.PALM_GUIDANCE
                            _palmResult.value = null
                            onError("हथेली की स्पष्ट पहचान नहीं हो सकी।\nकृपया दोबारा कैप्चर करें।")
                        } finally {
                            cleanupTempFile()
                            isCapturingOrAnalyzing.set(false)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        cleanupTempFile()
                        isCapturingOrAnalyzing.set(false)
                        _sessionMode.value = ReadingSessionMode.PALM_GUIDANCE
                        _palmResult.value = null
                        onError("कैमरा कैप्चर विफल रहा। कृपया दोबारा प्रयास करें।")
                    }
                }
            )
        } catch (e: Exception) {
            cleanupTempFile()
            isCapturingOrAnalyzing.set(false)
            _sessionMode.value = ReadingSessionMode.PALM_GUIDANCE
            _palmResult.value = null
            onError("कैमरा सेटअप विफल रहा।")
        }
    }

    /**
     * Method B: Manual User Capture for Face Reading.
     */
    fun captureManualFace(
        imageCapture: ImageCapture,
        context: Context,
        onSuccess: (FaceReadingResult) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isCapturingOrAnalyzing.compareAndSet(false, true)) {
            return
        }
        if (!_faceQuality.value.isUsable) {
            isCapturingOrAnalyzing.set(false)
            onError("Face requirements not met. Please align inside guide.")
            return
        }

        _sessionMode.value = ReadingSessionMode.FACE_ANALYZING
        executeFaceCapturePipeline(imageCapture, context, onSuccess, onError)
    }

    /**
     * Method A / Auto Capture for Face Reading.
     */
    fun triggerAutoFaceCapture(
        imageCapture: ImageCapture,
        context: Context,
        onSuccess: (FaceReadingResult) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isCapturingOrAnalyzing.compareAndSet(false, true)) {
            return
        }
        if (!_faceQuality.value.isUsable) {
            isCapturingOrAnalyzing.set(false)
            return
        }

        _sessionMode.value = ReadingSessionMode.FACE_ANALYZING
        executeFaceCapturePipeline(imageCapture, context, onSuccess, onError)
    }

    private fun executeFaceCapturePipeline(
        imageCapture: ImageCapture,
        context: Context,
        onSuccess: (FaceReadingResult) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val photoFile = File(context.cacheDir, "temp_face_${UUID.randomUUID()}.jpg")
            activeTempImageFile = photoFile
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            _sessionMode.value = ReadingSessionMode.FACE_ANALYZING

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        try {
                            if (!photoFile.exists() || photoFile.length() <= 0L) {
                                _sessionMode.value = ReadingSessionMode.FACE_GUIDANCE
                                isCapturingOrAnalyzing.set(false)
                                onError("चेहरे की स्पष्ट पहचान नहीं हो सकी।\nकृपया सही रोशनी और सीधे कोण में दोबारा कैप्चर करें।")
                                return
                            }

                            val bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
                            if (bitmap == null) {
                                _sessionMode.value = ReadingSessionMode.FACE_GUIDANCE
                                isCapturingOrAnalyzing.set(false)
                                onError("चेहरे की स्पष्ट पहचान नहीं हो सकी।\nकृपया सही रोशनी और सीधे कोण में दोबारा कैप्चर करें।")
                                return
                            }

                            val result = faceEngine.interpretFaceImage(bitmap, LocalDate.now())
                            bitmap.recycle()

                            _faceResult.value = result
                            _sessionMode.value = ReadingSessionMode.FACE_RESULT
                            onSuccess(result)
                        } catch (e: Exception) {
                            _sessionMode.value = ReadingSessionMode.FACE_GUIDANCE
                            _faceResult.value = null
                            onError("चेहरे की स्पष्ट पहचान नहीं हो सकी।\nकृपया सही रोशनी और सीधे कोण में दोबारा कैप्चर करें।")
                        } finally {
                            cleanupTempFile()
                            isCapturingOrAnalyzing.set(false)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        cleanupTempFile()
                        isCapturingOrAnalyzing.set(false)
                        _sessionMode.value = ReadingSessionMode.FACE_GUIDANCE
                        _faceResult.value = null
                        onError("कैमरा कैप्चर विफल रहा। कृपया दोबारा प्रयास करें।")
                    }
                }
            )
        } catch (e: Exception) {
            cleanupTempFile()
            isCapturingOrAnalyzing.set(false)
            _sessionMode.value = ReadingSessionMode.FACE_GUIDANCE
            _faceResult.value = null
            onError("कैमरा सेटअप विफल रहा।")
        }
    }

    @Synchronized
    fun stopAndCleanup() {
        cleanupTempFile()
        isCapturingOrAnalyzing.set(false)
        resetState()
        _sessionMode.value = ReadingSessionMode.IDLE
    }

    @Synchronized
    fun discardReadingResults() {
        _palmResult.value = null
        _faceResult.value = null
        stopAndCleanup()
    }

    private fun cleanupTempFile() {
        try {
            activeTempImageFile?.let { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
        activeTempImageFile = null
    }

    private fun resetState() {
        isCapturingOrAnalyzing.set(false)
    }
}
