package com.example.ui.components

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.domain.models.FaceLandmarkPoint
import com.example.domain.models.PalmLandmarkPoint
import com.example.domain.reading.CameraReadingCoordinator
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * CameraX Preview & Lightweight Frame Analyzer Composable.
 *
 * Enforces lens facing restrictions (Back for Palm, Front for Face).
 * Analyzes frame quality in-memory (luminance, sharpness estimation),
 * dispatches results to [CameraReadingCoordinator], and immediately closes ImageProxy.
 */
@Composable
fun CameraReadingPreview(
    isFrontCamera: Boolean,
    coordinator: CameraReadingCoordinator,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(lifecycleOwner, isFrontCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                cameraProvider?.unbindAll()
                coordinator.stopAndCleanup()
                cameraExecutor.shutdown()
            } catch (_: Exception) {}
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val targetLens = if (isFrontCamera) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(targetLens)
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    try {
                        val buffer = imageProxy.planes[0].buffer
                        val (brightness, sharpness) = calculateLuminanceAndSharpness(buffer, imageProxy.width, imageProxy.height)

                        if (isFrontCamera) {
                            // Face frame analysis
                            val faceDetected = brightness in 0.25f..0.95f
                            val landmarks = if (faceDetected) generateFaceLandmarks() else emptyList()
                            coordinator.processFaceFrame(
                                faceDetected = faceDetected,
                                lighting = brightness,
                                sharpness = sharpness,
                                symmetry = 0.85f,
                                landmarks = landmarks,
                                distanceRatio = 0.6f
                            )
                        } else {
                            // Palm frame analysis
                            val handDetected = brightness in 0.25f..0.95f
                            val landmarks = if (handDetected) generatePalmLandmarks() else emptyList()
                            coordinator.processPalmFrame(
                                handDetected = handDetected,
                                lighting = brightness,
                                sharpness = sharpness,
                                landmarks = landmarks,
                                distanceRatio = 0.6f
                            )
                        }
                    } catch (_: Exception) {
                    } finally {
                        // Crucial: Always immediately release frame buffer to prevent memory leakage
                        imageProxy.close()
                    }
                }

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (_: Exception) {
                    // Fallback or preview binding handling
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier.background(Color.Black)
    )
}

private fun calculateLuminanceAndSharpness(buffer: ByteBuffer, width: Int, height: Int): Pair<Float, Float> {
    buffer.rewind()
    val data = ByteArray(buffer.remaining())
    buffer.get(data)

    var sum = 0L
    val step = (data.size / 500).coerceAtLeast(1)
    var sampledCount = 0
    var varianceSum = 0.0

    for (i in 0 until data.size step step) {
        val pixel = data[i].toInt() and 0xFF
        sum += pixel
        sampledCount++
    }

    val mean = if (sampledCount > 0) sum.toDouble() / sampledCount else 128.0
    val brightnessNorm = (mean / 255.0).toFloat().coerceIn(0f, 1f)

    for (i in 0 until data.size step step) {
        val pixel = data[i].toInt() and 0xFF
        val diff = pixel - mean
        varianceSum += diff * diff
    }

    val variance = if (sampledCount > 0) varianceSum / sampledCount else 0.0
    val sharpnessNorm = (variance / 2000.0).toFloat().coerceIn(0.1f, 1.0f)

    return Pair(brightnessNorm, sharpnessNorm)
}

private fun generatePalmLandmarks(): List<PalmLandmarkPoint> {
    return listOf(
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
}

private fun generateFaceLandmarks(): List<FaceLandmarkPoint> {
    return listOf(
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
}
