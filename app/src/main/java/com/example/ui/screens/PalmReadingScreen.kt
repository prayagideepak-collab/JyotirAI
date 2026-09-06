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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.models.PalmLineAnalysis
import com.example.domain.models.PalmMountAnalysis
import com.example.domain.models.PalmReadingResult
import com.example.domain.reading.CameraReadingCoordinator
import com.example.domain.reading.ReadingSessionMode
import com.example.ui.components.CameraReadingPreview
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalmReadingScreen(
    coordinator: CameraReadingCoordinator,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionMode by coordinator.sessionMode.collectAsStateWithLifecycle()
    val palmQuality by coordinator.palmQuality.collectAsStateWithLifecycle()
    val palmResult by coordinator.palmResult.collectAsStateWithLifecycle()

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
            coordinator.startPalmSession()
        }
    }

    DisposableEffect(Unit) {
        if (hasCameraPermission) {
            coordinator.startPalmSession()
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
                        Text("Palm Reading (Hast Rekha)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Traditional Samudrika Shastra", style = MaterialTheme.typography.labelSmall, color = AccentAmber)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            coordinator.stopAndCleanup()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("palm_back_button")
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
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back Camera Only", style = MaterialTheme.typography.labelSmall, color = AccentAmber)
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
                    PalmPermissionRequiredContent(
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                }

                sessionMode == ReadingSessionMode.PALM_RESULT && palmResult != null -> {
                    PalmResultContent(
                        result = palmResult!!,
                        onRecapture = {
                            coordinator.discardReadingResults()
                            coordinator.startPalmSession()
                        }
                    )
                }

                else -> {
                    PalmCameraCaptureContent(
                        coordinator = coordinator,
                        quality = palmQuality,
                        sessionMode = sessionMode
                    )
                }
            }
        }
    }
}

@Composable
fun PalmPermissionRequiredContent(
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
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = AccentAmber,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Palm Reading analyzes live hand landmark contours using your device's Back Camera. Camera access is strictly active only while this screen is open, and raw frames are never stored or uploaded.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = DeepNavy),
            modifier = Modifier.testTag("request_camera_permission_button")
        ) {
            Text("Enable Back Camera", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PalmCameraCaptureContent(
    coordinator: CameraReadingCoordinator,
    quality: com.example.domain.models.PalmFrameQuality,
    sessionMode: ReadingSessionMode
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Live Camera Preview (Back Camera Only)
        CameraReadingPreview(
            isFrontCamera = false,
            coordinator = coordinator,
            modifier = Modifier
                .fillMaxSize()
                .testTag("palm_camera_preview")
        )

        // 2. Hand Outline Guidance Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val boxWidth = size.width * 0.7f
            val boxHeight = size.height * 0.55f

            // Frame bounding box
            drawRoundRect(
                color = if (quality.isUsable) Color(0xFF10B981) else Color(0xFFFBBF24).copy(alpha = 0.6f),
                topLeft = Offset(centerX - boxWidth / 2, centerY - boxHeight / 2),
                size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(32f, 32f),
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
                            label = if (quality.handDetected) "Palm In Frame" else "No Hand Detected",
                            isOk = quality.handDetected
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
                    text = "Keep your dominant palm open and flat facing the back camera. Multi-frame optical landmark aggregation will automatically analyze major lines once 10 stable frames are registered.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QualityStatusChip(label: String, isOk: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isOk) AccentEmerald.copy(alpha = 0.15f) else AccentCrimson.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isOk) AccentEmerald else AccentCrimson)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isOk) AccentEmerald else AccentCrimson
        )
    }
}

@Composable
fun PalmResultContent(
    result: PalmReadingResult,
    onRecapture: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("palm_result_content")
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
                        text = "Samudrika Hast Rekha",
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
                    text = result.handType,
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
                    text = result.overallSamudrikaGuidance,
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

        // Major Lines (Hast Rekha)
        Text(
            text = "Major Palm Lines (Mukhya Rekha)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        result.majorLines.forEach { line ->
            PalmLineCard(line = line)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Planetary Mounts (Grah Parvat)
        Text(
            text = "Planetary Mounts (Grah Parvat)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        result.mounts.forEach { mount ->
            PalmMountCard(mount = mount)
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
                .testTag("palm_recapture_button"),
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = TextPrimary)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Recapture / Scan Again")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun TemporalCard(title: String, content: String, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PalmLineCard(line: PalmLineAnalysis) {
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
                    text = line.lineName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = line.sanskritName,
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentAmber
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = line.prominence,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = AccentEmerald
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = line.interpretation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PalmMountCard(mount: PalmMountAnalysis) {
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
                    text = mount.mountName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = mount.planetLord,
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = mount.signification,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = mount.interpretation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
