package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
                    imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (isSpeaking) "Stop Spoken Reading" else "Listen in Hindi",
                    tint = if (isSpeaking) MaterialTheme.colorScheme.error else AccentAmber,
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
                color = if (isSpeaking) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else AccentAmber.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSpeaking) MaterialTheme.colorScheme.error else AccentAmber
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = if (isSpeaking) MaterialTheme.colorScheme.error else AccentAmber,
                        modifier = Modifier
                            .size(18.dp)
                            .scale(pulseScale)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSpeaking) "Stop Hindi Audio" else "Listen in Hindi (सुनें)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSpeaking) MaterialTheme.colorScheme.error else AccentAmber
                    )
                }
            }
        }
    }
}

enum class SpeakerButtonStyle {
    ICON_ONLY,
    FILLED_CHIP
}
