package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.domain.panchang.PanchangHindiPresenter
import com.example.domain.speech.AstrologyHindiSpeechFormatter
import com.example.domain.speech.JyotirAiSpeechManager
import com.example.ui.components.AstrologySpeakerButton
import com.example.ui.components.SpeakerButtonStyle
import com.example.ui.theme.*
import com.example.ui.viewmodel.AstrologyViewModel
import com.example.ui.viewmodel.PanchangUiState
import java.time.LocalDate
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

    // Date Picker Dialog Launcher
    val onPickDateClick = {
        val currentLocal = dateTime?.toLocalDate() ?: LocalDate.now()
        val dpd = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = LocalDate.of(year, month + 1, dayOfMonth)
                val currentZoned = dateTime ?: java.time.ZonedDateTime.now()
                val updated = picked.atTime(currentZoned.toLocalTime()).atZone(currentZoned.zone)
                viewModel.setPanchangDateTime(updated)
            },
            currentLocal.year,
            currentLocal.monthValue - 1,
            currentLocal.dayOfMonth
        )
        dpd.show()
    }

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
                            text = "वैदिक पंचांग • Astronomical Almanac",
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
                            hindiTextProvider = { PanchangHindiPresenter.formatSpeechSummary(panchang) },
                            buttonStyle = SpeakerButtonStyle.ICON_ONLY,
                            testTag = "panchang_tts_speaker_button"
                        )
                    }

                    IconButton(
                        onClick = onPickDateClick,
                        modifier = Modifier.testTag("panchang_calendar_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Select Date",
                            tint = AccentAmber
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

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onPickDateClick() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = dateTime?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) ?: "Loading...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap to choose date",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentAmber
                        )
                    }

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

            Spacer(modifier = Modifier.height(14.dp))

            when (val state = uiState) {
                is PanchangUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentAmber)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Calculating astronomical almanac...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                is PanchangUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Panchang Error", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(state.message, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.resetPanchangToNow() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                                ) {
                                    Text("Retry Today", color = DeepNavy)
                                }
                            }
                        }
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
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = panchang.location.placeName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${String.format("%.4f", panchang.location.latitude)}°N, ${String.format("%.4f", panchang.location.longitude)}°E • ${panchang.location.timeZoneId ?: "UTC"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Core Five Angas (पञ्चाङ्ग)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "The Five Angas (पञ्चाङ्ग)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = AccentAmber
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (panchang.paksha == Paksha.SHUKLA) AccentAmber.copy(alpha = 0.2f) else DeepNavy
                                    ) {
                                        Text(
                                            text = if (panchang.paksha == Paksha.SHUKLA) "शुक्ल पक्ष (Waxing)" else "कृष्ण पक्ष (Waning)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = AccentAmber,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = BorderSubtle)
                                Spacer(modifier = Modifier.height(12.dp))

                                // 1. Vara
                                AngaItem(
                                    label = "1. Vara (वार)",
                                    sanskrit = panchang.vara.sanskritName,
                                    hindi = panchang.vara.hindiName,
                                    detail = "Day of ${panchang.vara.englishName}",
                                    progress = null,
                                    timing = null
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // 2. Tithi
                                AngaItem(
                                    label = "2. Tithi (तिथि)",
                                    sanskrit = panchang.tithi.name,
                                    hindi = panchang.tithi.hindiName,
                                    detail = "Index: ${panchang.tithi.index}/30 • ${(panchang.tithi.remainingPercentage * 100).toInt()}% remaining",
                                    progress = panchang.tithi.remainingPercentage.toFloat(),
                                    timing = formatTiming(panchang.tithi.startTime, panchang.tithi.endTime, timeFormatter)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // 3. Nakshatra
                                AngaItem(
                                    label = "3. Nakshatra (नक्षत्र)",
                                    sanskrit = panchang.nakshatra.nakshatra.sanskritName,
                                    hindi = panchang.nakshatra.nakshatra.sanskritName,
                                    detail = "Pada ${panchang.nakshatra.pada}/4 • Lord: ${panchang.nakshatra.nakshatra.lord}",
                                    progress = panchang.nakshatra.remainingPercentage.toFloat(),
                                    timing = formatTiming(panchang.nakshatra.startTime, panchang.nakshatra.endTime, timeFormatter)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // 4. Yoga
                                AngaItem(
                                    label = "4. Yoga (योग)",
                                    sanskrit = panchang.yoga.name,
                                    hindi = panchang.yoga.hindiName,
                                    detail = "Nitya Yoga ${panchang.yoga.index}/27",
                                    progress = panchang.yoga.remainingPercentage.toFloat(),
                                    timing = formatTiming(panchang.yoga.startTime, panchang.yoga.endTime, timeFormatter)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // 5. Karana
                                AngaItem(
                                    label = "5. Karana (करण)",
                                    sanskrit = panchang.karana.name,
                                    hindi = panchang.karana.hindiName,
                                    detail = if (panchang.karana.isFixed) "Fixed Karana (${panchang.karana.index}/60)" else "Movable Karana (${panchang.karana.index}/60)",
                                    progress = panchang.karana.remainingPercentage.toFloat(),
                                    timing = formatTiming(panchang.karana.startTime, panchang.karana.endTime, timeFormatter)
                                )

                                val lunar = panchang.lunarObservance
                                if (lunar != null && (lunar.isEkadashi || lunar.isPurnima || lunar.isAmavasya || lunar.isPradosh || lunar.isSankranti)) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Celebration, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Special Lunar Observance",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AccentAmber
                                                )
                                                Text(
                                                    text = lunar.description ?: when {
                                                        lunar.isEkadashi -> "Ekadashi Vrata (एकादशी)"
                                                        lunar.isPurnima -> "Purnima Vrata (पूर्णिमा)"
                                                        lunar.isAmavasya -> "Amavasya Pitru Tarpan (अमावस्या)"
                                                        lunar.isPradosh -> "Pradosha Vrata (प्रदोष)"
                                                        else -> "Sankranti Transition (संक्रांति)"
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
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
                                    text = "Solar & Lunar Ephemeris",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AccentAmber
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = BorderSubtle)
                                Spacer(modifier = Modifier.height(12.dp))

                                PanchangRow("Sunrise (सूर्योदय)", panchang.sunrise?.format(timeFormatter) ?: "Polar Day/Night")
                                PanchangRow("Sunset (सूर्यास्त)", panchang.sunset?.format(timeFormatter) ?: "Polar Day/Night")
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
                                                text = "Auspicious & Caution Windows",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = AccentAmber
                                            )
                                            Text(
                                                text = "Exact dawn & caution window alerts",
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

                                    // Abhijit Muhurta
                                    muhurta.abhijitMuhurta?.let { am ->
                                        Spacer(modifier = Modifier.height(10.dp))
                                        MuhurtaInfoDisplayRow(
                                            title = "Abhijit Muhurta (अभिजित मुहूर्त)",
                                            timeRange = "${am.start.format(timeFormatter)} - ${am.end.format(timeFormatter)}",
                                            description = "Midday window for general auspicious activities"
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

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

                        // Engine Metadata Badge
                        Text(
                            text = "${panchang.metadata.ephemerisEngine} • Ayanamsa: ${panchang.metadata.ayanamsaName} (${String.format("%.4f", panchang.metadata.ayanamsaDegree)}°)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AngaItem(
    label: String,
    sanskrit: String,
    hindi: String,
    detail: String,
    progress: Float?,
    timing: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$sanskrit ($hindi)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = AccentAmber
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (timing != null) {
                Text(
                    text = timing,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (progress != null) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = AccentAmber,
                trackColor = SurfaceNavy
            )
        }
    }
}

@Composable
private fun MuhurtaInfoDisplayRow(
    title: String,
    timeRange: String,
    description: String
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

private fun formatTiming(
    start: java.time.ZonedDateTime?,
    end: java.time.ZonedDateTime?,
    formatter: DateTimeFormatter
): String? {
    return when {
        start != null && end != null -> "${start.format(formatter)} - ${end.format(formatter)}"
        end != null -> "Ends: ${end.format(formatter)}"
        start != null -> "Starts: ${start.format(formatter)}"
        else -> null
    }
}
