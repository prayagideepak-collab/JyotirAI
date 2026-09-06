package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.speech.JyotirAiSpeechManager
import com.example.domain.speech.SpeechState
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated

/**
 * Reusable M3 Speaker / Read Aloud button for astrological content.
 * Automatically converts structured content to Hindi and streams via JyotirAiSpeechManager.
 */
@Composable
fun AstrologySpeakerButton(
    speechManager: JyotirAiSpeechManager,
    hindiTextProvider: () -> String,
    modifier: Modifier = Modifier,
    buttonStyle: SpeakerButtonStyle = SpeakerButtonStyle.FILLED_CHIP,
    testTag: String = "astrology_speaker_button"
) {
    val context = LocalContext.current
    val speechState by speechManager.speechState.collectAsStateWithLifecycle()
    val isSpeaking = speechState is SpeechState.Speaking
    val isPaused = speechState is SpeechState.Paused

    // Handle toast for unavailable/error states
    LaunchedEffect(speechState) {
        when (val state = speechState) {
            is SpeechState.Unavailable -> {
                Toast.makeText(context, state.reason, Toast.LENGTH_LONG).show()
            }
            is SpeechState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    // Pulse animation while speaking
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSpeaking) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    when (buttonStyle) {
        SpeakerButtonStyle.ICON_ONLY -> {
            IconButton(
                onClick = {
                    val text = hindiTextProvider()
                    speechManager.toggleSpeak(text)
                },
                modifier = modifier
                    .size(48.dp)
                    .testTag(testTag)
            ) {
                Icon(
                    imageVector = if (isSpeaking || isPaused) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isSpeaking) "Stop Spoken Reading" else "Listen in Hindi",
                    tint = if (isSpeaking || isPaused) MaterialTheme.colorScheme.error else AccentAmber,
                    modifier = Modifier
                        .size(24.dp)
                        .scale(pulseScale)
                )
            }
        }

        SpeakerButtonStyle.FILLED_CHIP -> {
            Surface(
                onClick = {
                    val text = hindiTextProvider()
                    speechManager.toggleSpeak(text)
                },
                modifier = modifier
                    .heightIn(min = 40.dp)
                    .testTag(testTag),
                shape = RoundedCornerShape(16.dp),
                color = if (isSpeaking || isPaused) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else AccentAmber.copy(alpha = 0.15f),
                border = BorderStroke(
                    1.dp,
                    if (isSpeaking || isPaused) MaterialTheme.colorScheme.error else AccentAmber
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSpeaking || isPaused) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = if (isSpeaking || isPaused) MaterialTheme.colorScheme.error else AccentAmber,
                        modifier = Modifier
                            .size(18.dp)
                            .scale(pulseScale)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSpeaking) "Stop Hindi Audio" else if (isPaused) "Resume / Stop" else "Listen in Hindi (सुनें)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSpeaking || isPaused) MaterialTheme.colorScheme.error else AccentAmber
                    )
                }
            }
        }
    }
}

/**
 * Reusable audio control bar with live-read status and pause/resume/stop buttons.
 */
@Composable
fun AstrologyAudioBar(
    speechManager: JyotirAiSpeechManager,
    modifier: Modifier = Modifier
) {
    val speechState by speechManager.speechState.collectAsStateWithLifecycle()
    val activeSegment by speechManager.activeSegmentIndex.collectAsStateWithLifecycle()
    val segments by speechManager.segments.collectAsStateWithLifecycle()
    val isSpeaking = speechState is SpeechState.Speaking
    val isPaused = speechState is SpeechState.Paused

    if (isSpeaking || isPaused || activeSegment >= 0) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("astrology_audio_bar"),
            shape = RoundedCornerShape(12.dp),
            color = AccentAmber.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isPaused) "ऑडियो रुका हुआ है (Paused)" else "अभी यह हिस्सा पढ़ा जा रहा है...",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AccentAmber
                        )
                        if (activeSegment in segments.indices) {
                            Text(
                                text = segments[activeSegment].text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSpeaking) {
                        IconButton(onClick = { speechManager.pause() }, modifier = Modifier.size(36.dp).testTag("tts_pause_button")) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = AccentAmber, modifier = Modifier.size(18.dp))
                        }
                    } else if (isPaused) {
                        IconButton(onClick = { speechManager.resume() }, modifier = Modifier.size(36.dp).testTag("tts_resume_button")) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = AccentAmber, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = { speechManager.stop() }, modifier = Modifier.size(36.dp).testTag("tts_stop_button")) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

enum class SpeakerButtonStyle {
    ICON_ONLY,
    FILLED_CHIP
}
