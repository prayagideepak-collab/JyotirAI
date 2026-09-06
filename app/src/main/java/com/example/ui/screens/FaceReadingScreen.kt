package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.models.FaceReadingResult
import com.example.domain.models.FacialFeatureAnalysis
import com.example.domain.models.FacialZoneAnalysis
import com.example.domain.reading.CameraReadingCoordinator
import com.example.domain.reading.ReadingSessionMode
import com.example.ui.components.CameraReadingPreview
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceReadingScreen(
    coordinator: CameraReadingCoordinator,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionMode by coordinator.sessionMode.collectAsStateWithLifecycle()
    val faceQuality by coordinator.faceQuality.collectAsStateWithLifecycle()
    val faceResult by coordinator.faceResult.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            coordinator.startFaceSession()
        }
    }

    DisposableEffect(Unit) {
        if (hasCameraPermission) {
            coordinator.startFaceSession()
        }
        onDispose {
            coordinator.stopAndCleanup()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Face Reading (Mukh Samudrika)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Traditional Vedic Feature Contemplation", style = MaterialTheme.typography.labelSmall, color = AccentAmber)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            coordinator.stopAndCleanup()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("face_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Face, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Front Camera Only", style = MaterialTheme.typography.labelSmall, color = AccentAmber)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark, titleContentColor = TextPrimary)
            )
        },
        containerColor = BackgroundDark,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                !hasCameraPermission -> {
                    FacePermissionRequiredContent(
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                }

                sessionMode == ReadingSessionMode.FACE_RESULT && faceResult != null -> {
                    FaceResultContent(
                        result = faceResult!!,
                        onRecapture = {
                            coordinator.discardReadingResults()
                            coordinator.startFaceSession()
                        }
                    )
                }

                else -> {
                    FaceCameraCaptureContent(
                        coordinator = coordinator,
                        quality = faceQuality,
                        sessionMode = sessionMode
                    )
                }
            }
        }
    }
}

@Composable
fun FacePermissionRequiredContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Face,
            contentDescription = null,
            tint = AccentAmber,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Front Camera Permission Required",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Face Reading analyzes facial contours and symmetry using your device's Front Camera. Camera access is active exclusively while this screen is in use, and raw images are never recorded, saved, or uploaded.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = DeepNavy),
            modifier = Modifier.testTag("request_face_camera_permission_button")
        ) {
            Text("Enable Front Camera", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FaceCameraCaptureContent(
    coordinator: CameraReadingCoordinator,
    quality: com.example.domain.models.FaceFrameQuality,
    sessionMode: ReadingSessionMode
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Live Camera Preview (Front Camera Only)
        CameraReadingPreview(
            isFrontCamera = true,
            coordinator = coordinator,
            modifier = Modifier
                .fillMaxSize()
                .testTag("face_camera_preview")
        )

        // 2. Oval Face Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radiusX = size.width * 0.35f
            val radiusY = size.height * 0.28f

            drawOval(
                color = if (quality.isUsable) Color(0xFF10B981) else Color(0xFFFBBF24).copy(alpha = 0.6f),
                topLeft = Offset(centerX - radiusX, centerY - radiusY),
                size = androidx.compose.ui.geometry.Size(radiusX * 2, radiusY * 2),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // 3. Live Guidance Banner & Quality Indicators
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = quality.guidanceMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (quality.isUsable) AccentEmerald else AccentAmber
                        )
                        Text(
                            text = "${quality.captureCompletenessPercent}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = AccentAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { quality.captureCompletenessPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentAmber,
                        trackColor = SurfaceElevated
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QualityStatusChip(
                            label = if (quality.faceDetected) "Face Centered" else "No Face Detected",
                            isOk = quality.faceDetected
                        )
                        QualityStatusChip(
                            label = if (quality.lightingScore >= 0.4f) "Good Light" else "Low Light",
                            isOk = quality.lightingScore >= 0.4f
                        )
                        QualityStatusChip(
                            label = if (quality.sharpnessScore >= 0.4f) "Steady" else "Motion Blur",
                            isOk = quality.sharpnessScore >= 0.4f
                        )
                    }
                }
            }
        }

        // 4. Bottom Instructions Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Look straight into the front camera. The system accumulates multi-view landmark geometry across 10 stable frames to evaluate the classical three Vedic zones (Tri-Bhaga).",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FaceResultContent(
    result: FaceReadingResult,
    onRecapture: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("face_result_content")
    ) {
        // Header Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mukh Samudrika Shastra",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccentAmber
                    )
                    Text(
                        text = "${result.readingDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.faceArchetype,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.geometricModelType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = result.overallMukhSamudrikaGuidance,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Temporal Analysis (Today, Past, Future)
        Text(
            text = "Temporal Interpretation (Kaala Samiksha)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        TemporalCard(
            title = "TODAY / PRESENT FOCUS",
            content = result.temporalReading.todayFocus,
            accentColor = AccentAmber
        )
        Spacer(modifier = Modifier.height(8.dp))
        TemporalCard(
            title = "PAST EXPERIENTIAL FOUNDATION",
            content = result.temporalReading.pastInfluence,
            accentColor = Color(0xFF64B5F6)
        )
        Spacer(modifier = Modifier.height(8.dp))
        TemporalCard(
            title = "FUTURE POTENTIAL & ALIGNMENT",
            content = result.temporalReading.futurePotential,
            accentColor = Color(0xFF81C784)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Three Classical Zones (Tri-Bhaga)
        Text(
            text = "Classical Three Zones (Tri-Bhaga)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        result.zones.forEach { zone ->
            FacialZoneCard(zone = zone)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feature-Specific Findings
        Text(
            text = "Facial Feature Findings (Samudrika Lakshana)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        result.features.forEach { feature ->
            FacialFeatureCard(feature = feature)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ethical Disclaimer
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AccentAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = result.ethicalDisclaimer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onRecapture,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("face_recapture_button"),
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = TextPrimary)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Recapture / Scan Face Again")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun FacialZoneCard(zone: FacialZoneAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = zone.zoneName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = zone.sanskritName,
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentAmber
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = zone.prominentTrait,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = AccentEmerald
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = zone.interpretation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FacialFeatureCard(feature: FacialFeatureAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = feature.featureName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = feature.sanskritName,
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = feature.structuralTrait,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = feature.traditionalMeaning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
