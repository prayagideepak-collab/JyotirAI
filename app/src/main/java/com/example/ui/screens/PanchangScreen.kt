package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.models.*
import com.example.domain.speech.AstrologyHindiSpeechFormatter
import com.example.domain.speech.JyotirAiSpeechManager
import com.example.ui.components.AstrologySpeakerButton
import com.example.ui.components.SpeakerButtonStyle
import com.example.ui.theme.*
import com.example.ui.viewmodel.AstrologyViewModel
import com.example.ui.viewmodel.PanchangUiState
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanchangScreen(viewModel: AstrologyViewModel) {
    val context = LocalContext.current
    val speechManager = remember { JyotirAiSpeechManager(context) }
    DisposableEffect(Unit) {
        onDispose {
            speechManager.release()
        }
    }

    val uiState by viewModel.panchangUiState.collectAsStateWithLifecycle()
    val dateTime by viewModel.panchangDateTime.collectAsStateWithLifecycle()
    val alarms by viewModel.muhurtaAlarms.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daily Panchang",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = AccentAmber
                        )
                        Text(
                            text = "Vedic Astronomical Almanac",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    val panchang = (uiState as? PanchangUiState.Success)?.snapshot
                    if (panchang != null) {
                        AstrologySpeakerButton(
                            speechManager = speechManager,
                            hindiTextProvider = { AstrologyHindiSpeechFormatter.formatPanchangSummary(panchang) },
                            buttonStyle = SpeakerButtonStyle.ICON_ONLY,
                            testTag = "panchang_tts_speaker_button"
                        )
                    }

                    IconButton(
                        onClick = { viewModel.resetPanchangToNow() },
                        modifier = Modifier.testTag("panchang_reset_today_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Reset to Today",
                            tint = AccentAmber
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceNavy
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBackground)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Date Controls Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.shiftPanchangDays(-1) },
                        modifier = Modifier.testTag("panchang_prev_day_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Day",
                            tint = AccentAmber
                        )
                    }

                    Text(
                        text = dateTime?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) ?: "Loading...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { viewModel.shiftPanchangDays(1) },
                        modifier = Modifier.testTag("panchang_next_day_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Day",
                            tint = AccentAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is PanchangUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentAmber)
                    }
                }
                is PanchangUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is PanchangUiState.Success -> {
                    val panchang = state.snapshot
                    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Location Banner
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = panchang.location.placeName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${String.format("%.2f", panchang.location.latitude)}°N, ${String.format("%.2f", panchang.location.longitude)}°E • ${panchang.location.timeZoneId ?: "UTC"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Core Panchanga Elements
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Panchanga Elements (पंचांग)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AccentAmber
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = BorderSubtle)
                                Spacer(modifier = Modifier.height(12.dp))

                                PanchangRow("Vara (वार)", "${panchang.vara.sanskritName} (${panchang.vara.englishName})")
                                PanchangRow("Tithi (तिथि)", "${panchang.tithi.name} (${if (panchang.paksha == Paksha.SHUKLA) "शुक्ल पक्ष" else "कृष्ण पक्ष"})")
                                PanchangRow("Nakshatra (नक्षत्र)", "${panchang.nakshatra.nakshatra.sanskritName} (चरण ${panchang.nakshatra.pada})")
                                PanchangRow("Yoga (योग)", panchang.yoga.name)
                                PanchangRow("Karana (करण)", panchang.karana.name)

                                val lunar = panchang.lunarObservance
                                if (lunar != null && (lunar.isEkadashi || lunar.isPurnima || lunar.isAmavasya)) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val obsName = when {
                                        lunar.isEkadashi -> "Ekadashi (एकादशी)"
                                        lunar.isPurnima -> "Purnima (पूर्णिमा)"
                                        lunar.isAmavasya -> "Amavasya (अमावस्या)"
                                        else -> ""
                                    }
                                    Text(
                                        text = "Lunar Observance: $obsName",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = AccentAmber
                                    )
                                }
                            }
                        }

                        // Solar & Lunar Ephemeris
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Solar & Lunar Timings",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AccentAmber
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = BorderSubtle)
                                Spacer(modifier = Modifier.height(12.dp))

                                PanchangRow("Sunrise (सूर्योदय)", panchang.sunrise?.format(timeFormatter) ?: "Unavailable")
                                PanchangRow("Sunset (सूर्यास्त)", panchang.sunset?.format(timeFormatter) ?: "Unavailable")
                                PanchangRow("Sun Sign (सूर्य राशि)", panchang.sunSign?.sanskritName ?: "Unavailable")
                                PanchangRow("Moon Sign (चन्द्र राशि)", panchang.moonSign?.sanskritName ?: "Unavailable")
                            }
                        }

                        // Dynamic Muhurta & Alarms Section
                        panchang.muhurta?.let { muhurta ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(1.dp, AccentAmber.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Dynamic Muhurta & Alarms",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = AccentAmber
                                            )
                                            Text(
                                                text = "Daily exact dawn & caution window alerts",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Alarm,
                                            contentDescription = null,
                                            tint = AccentAmber,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = BorderSubtle)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Brahma Muhurta Card with Alarm Toggle
                                    muhurta.brahmaMuhurta?.let { bm ->
                                        val isBmAlarmOn = alarms.any { it.type == MuhurtaAlarmType.BRAHMA_MUHURTA && it.isEnabled }
                                        MuhurtaAlarmRow(
                                            title = "Brahma Muhurta (ब्रह्म मुहूर्त)",
                                            timeRange = "${bm.start.format(timeFormatter)} - ${bm.end.format(timeFormatter)}",
                                            description = "Auspicious dawn period for meditation & study",
                                            isAlarmEnabled = isBmAlarmOn,
                                            onToggleAlarm = { enabled ->
                                                viewModel.toggleMuhurtaAlarm(MuhurtaAlarmType.BRAHMA_MUHURTA, enabled)
                                                Toast.makeText(
                                                    context,
                                                    if (enabled) "Brahma Muhurta dynamic alarm scheduled" else "Brahma Muhurta alarm canceled",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            testTag = "brahma_muhurta_alarm_toggle"
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Rahukaal Card with Alarm Toggle
                                    muhurta.rahukaal?.let { rk ->
                                        val isRkAlarmOn = alarms.any { it.type == MuhurtaAlarmType.RAHUKAAL && it.isEnabled }
                                        MuhurtaAlarmRow(
                                            title = "Rahukaal (राहुकाल)",
                                            timeRange = "${rk.start.format(timeFormatter)} - ${rk.end.format(timeFormatter)}",
                                            description = "Caution window for routine activities",
                                            isAlarmEnabled = isRkAlarmOn,
                                            onToggleAlarm = { enabled ->
                                                viewModel.toggleMuhurtaAlarm(MuhurtaAlarmType.RAHUKAAL, enabled)
                                                Toast.makeText(
                                                    context,
                                                    if (enabled) "Rahukaal dynamic alarm scheduled" else "Rahukaal alarm canceled",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            testTag = "rahukaal_alarm_toggle"
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MuhurtaAlarmRow(
    title: String,
    timeRange: String,
    description: String,
    isAlarmEnabled: Boolean,
    onToggleAlarm: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceElevated)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeRange,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AccentAmber
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = isAlarmEnabled,
            onCheckedChange = onToggleAlarm,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = DeepNavy,
                checkedTrackColor = AccentAmber,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = SurfaceCard
            )
        )
    }
}

@Composable
fun PanchangRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.2f)
        )
    }
}
