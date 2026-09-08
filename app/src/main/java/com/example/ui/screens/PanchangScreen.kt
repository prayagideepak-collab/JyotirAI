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
    val tithiVoiceEnabled by viewModel.tithiVoiceEnabled.collectAsStateWithLifecycle()
    val nightSilenceEnabled by viewModel.nightSilenceEnabled.collectAsStateWithLifecycle()
    val locationSource by viewModel.locationSource.collectAsStateWithLifecycle()
    val isLocationConfirmed by viewModel.isLocationConfirmed.collectAsStateWithLifecycle()
    val tickerSpeed by viewModel.tickerSpeed.collectAsStateWithLifecycle()

    var showPermissionExplanation by remember { mutableStateOf(false) }
    var showLocationConfirmation by remember { mutableStateOf(false) }
    var showManualCityDialog by remember { mutableStateOf(false) }
    var cityQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<BirthLocation>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine) {
            viewModel.setLocationSource("GPS", true)
        } else if (coarse) {
            viewModel.setLocationSource("अनुमानित नेटवर्क स्थान", false)
            showLocationConfirmation = true
        } else {
            viewModel.setLocationSource("अनुमानित स्थान", false)
            showLocationConfirmation = true
        }
    }

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
        containerColor = CosmicBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBackground)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Screen Title & Action Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Panchang (पंचांग केंद्र)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AccentAmber
                    )
                    Text(
                        text = "वैदिक पंचांग • Astronomical Almanac",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val panchangSnapshot = (uiState as? PanchangUiState.Success)?.snapshot
                    if (panchangSnapshot != null) {
                        AstrologySpeakerButton(
                            speechManager = speechManager,
                            hindiTextProvider = { PanchangHindiPresenter.formatSpeechSummary(panchangSnapshot) },
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
                }
            }

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

                    val zone = panchang.location.timeZoneId?.let { java.time.ZoneId.of(it) } ?: java.time.ZoneId.of("UTC")
                    var currentZonedDateTime by remember { mutableStateOf(java.time.ZonedDateTime.now(zone)) }
                    LaunchedEffect(zone) {
                        while (true) {
                            kotlinx.coroutines.delay(1000L)
                            currentZonedDateTime = java.time.ZonedDateTime.now(zone)
                        }
                    }

                    val clockFormatter = DateTimeFormatter.ofPattern("HH : mm : ss")
                    val currentTimeStr = currentZonedDateTime.format(clockFormatter)

                    val events = remember(panchang, currentZonedDateTime) {
                        com.example.domain.panchang.VedicEventEngine.generateTimeline(panchang, currentZonedDateTime)
                    }
                    val endingEvent = remember(events, currentZonedDateTime) {
                        com.example.domain.panchang.VedicEventEngine.getEndingSoonEvent(events, currentZonedDateTime)
                    }
                    val nearestEvent = remember(events, currentZonedDateTime) {
                        com.example.domain.panchang.VedicEventEngine.getNearestUpcomingEvent(events, currentZonedDateTime)
                    }
                    val vikramSamvat = com.example.domain.panchang.VedicEventEngine.calculateVikramSamvat(panchang.requestedDateTime.toLocalDate())

                    val endingCountdownStr = if (endingEvent?.endTime != null) {
                        val duration = java.time.Duration.between(currentZonedDateTime, endingEvent.endTime)
                        val millis = maxOf(0L, duration.toMillis())
                        val h = (millis / (1000 * 60 * 60))
                        val m = (millis / (1000 * 60)) % 60
                        val s = (millis / 1000) % 60
                        String.format("%02d : %02d : %02d", h, m, s)
                    } else "00 : 00 : 00"

                    val upcomingCountdownStr = if (nearestEvent?.startTime != null) {
                        val duration = java.time.Duration.between(currentZonedDateTime, nearestEvent.startTime)
                        val millis = maxOf(0L, duration.toMillis())
                        val h = (millis / (1000 * 60 * 60))
                        val m = (millis / (1000 * 60)) % 60
                        val s = (millis / 1000) % 60
                        String.format("%02d : %02d : %02d", h, m, s)
                    } else "00 : 00 : 00"

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Live Clock & Vikram Samvat & Time Systems Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, AccentAmber.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
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
                                            text = "🕒 वर्तमान समय (Live Clock) • विक्रम संवत $vikramSamvat",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AccentAmber
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = currentTimeStr,
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(28.dp))
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = BorderSubtle)
                                Spacer(modifier = Modifier.height(12.dp))

                                // Current Active Event Countdown (Red State)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🔴 ${endingEvent?.displayName ?: panchang.tithi.hindiName}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "समाप्त होने में: $endingCountdownStr",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "सक्रिय (Active)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(10.dp))

                                // Upcoming Event Countdown (Green State)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🟢 ${nearestEvent?.displayName ?: "अगला मुहूर्त"}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF4CAF50)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "शुरू होने में: $upcomingCountdownStr",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "आगामी (Upcoming)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF4CAF50),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Voice Announcement & Night Silence Settings Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "तिथि सूचना एवं सेटिंग्स (Voice & Silence)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AccentAmber
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = BorderSubtle)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🔊 तिथि परिवर्तन आवाज़ सूचना",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "तिथि बदलने पर ऑडियो घोषणा",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = tithiVoiceEnabled,
                                        onCheckedChange = { viewModel.toggleTithiVoice(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = DeepNavy, checkedTrackColor = AccentAmber)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🌙 नाइट साइलेंट (Night Silence)",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "रात (22:00 से 06:00) में आवाज़ बंद रहेगी",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = nightSilenceEnabled,
                                        onCheckedChange = { viewModel.toggleNightSilence(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = DeepNavy, checkedTrackColor = AccentAmber)
                                    )
                                }
                            }
                        }

                        // Location Banner with Source & Change Button
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
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
                                                text = "📍 ${panchang.location.placeName}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "स्रोत: $locationSource • ${String.format("%.4f", panchang.location.latitude)}°N, ${String.format("%.4f", panchang.location.longitude)}°E",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    TextButton(onClick = { showManualCityDialog = true }) {
                                        Text("स्थान बदलें", color = AccentAmber)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isLocationConfirmed) "✓ सत्यापित स्थान" else "⚠️ अनुमानित स्थान - पुष्टि आवश्यक",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isLocationConfirmed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                    )
                                    OutlinedButton(
                                        onClick = { showPermissionExplanation = true },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("GPS अनुमति", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        // Ticker Speed Settings Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "सूचना प्रदर्शन गति (Ticker Speed)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AccentAmber
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf("धीमी", "सामान्य", "तेज़").forEach { speed ->
                                        FilterChip(
                                            selected = tickerSpeed == speed,
                                            onClick = { viewModel.setTickerSpeed(speed) },
                                            label = { Text(speed) }
                                        )
                                    }
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
                                            title = "⏰ ब्रह्म मुहूर्त (Brahma Muhurta)",
                                            timeRange = "${bm.start.format(timeFormatter)} - ${bm.end.format(timeFormatter)}",
                                            description = "ब्रह्म मुहूर्त का समय शुरू हो रहा है。",
                                            isAlarmEnabled = isBmAlarmOn,
                                            onToggleAlarm = { enabled ->
                                                viewModel.toggleMuhurtaAlarm(MuhurtaAlarmType.BRAHMA_MUHURTA, enabled)
                                                Toast.makeText(
                                                    context,
                                                    if (enabled) "मुहूर्त सूचना चालू है" else "मुहूर्त सूचना बंद है",
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
                                        val isRkAlarmOn = alarms.any { it.type == MuhurtaAlarmType.RAHUKAAL_START && it.isEnabled }
                                        MuhurtaAlarmRow(
                                            title = "⏰ राहुकाल शुरू (Rahukaal Start)",
                                            timeRange = "${rk.start.format(timeFormatter)} - ${rk.end.format(timeFormatter)}",
                                            description = "राहुकाल शुरू हो रहा है。",
                                            isAlarmEnabled = isRkAlarmOn,
                                            onToggleAlarm = { enabled ->
                                                viewModel.toggleMuhurtaAlarm(MuhurtaAlarmType.RAHUKAAL_START, enabled)
                                                Toast.makeText(
                                                    context,
                                                    if (enabled) "मुहूर्त सूचना चालू है" else "मुहूर्त सूचना बंद है",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            testTag = "rahukaal_start_alarm_toggle"
                                         )
                                     }

                                     Spacer(modifier = Modifier.height(10.dp))

                                     // Rahukaal End Alarm Toggle
                                     muhurta.rahukaal?.let { rk ->
                                         val isRkEndAlarmOn = alarms.any { it.type == MuhurtaAlarmType.RAHUKAAL_END && it.isEnabled }
                                         MuhurtaAlarmRow(
                                             title = "⏰ राहुकाल समाप्त (Rahukaal End)",
                                             timeRange = "${rk.start.format(timeFormatter)} - ${rk.end.format(timeFormatter)}",
                                             description = "राहुकाल समाप्त हो गया है。",
                                             isAlarmEnabled = isRkEndAlarmOn,
                                             onToggleAlarm = { enabled ->
                                                 viewModel.toggleMuhurtaAlarm(MuhurtaAlarmType.RAHUKAAL_END, enabled)
                                                 Toast.makeText(
                                                     context,
                                                     if (enabled) "मुहूर्त सूचना चालू है" else "मुहूर्त सूचना बंद है",
                                                     Toast.LENGTH_SHORT
                                                 ).show()
                                             },
                                             testTag = "rahukaal_end_alarm_toggle"
                                         )
                                    }
                                }
                            }
                        }

                        // Tomorrow's Advance Information Card
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
                                        text = "कल की जानकारी (Tomorrow's Preview)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = AccentAmber
                                    )
                                    Icon(Icons.Default.Upcoming, contentDescription = null, tint = AccentAmber)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = BorderSubtle)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "कल: आगामी तिथि एवं नक्षत्र संक्रमण",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "वर्तमान तिथि (${panchang.tithi.name}) के पूर्ण होने के पश्चात अगली तिथि का आरंभ होगा। सभी व्रत, उपवास एवं पर्व स्थानीय सूर्योदय और तिथि व्याप्ति पर आधारित होते हैं।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // All Tithis Reference Guide Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "सभी तिथियाँ एवं महत्व (Tithi Reference Guide)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AccentAmber
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = BorderSubtle)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "• प्रतिपदा (Pratipada): नए कार्यों और संकल्पों के लिए शुभ\n• एकादशी (Ekadashi): श्री हरि विष्णु व्रत एवं उपवास\n• पूर्णिमा (Purnima): सत्यनारायण व्रत, चंद्र दर्शन एवं पूर्ण ऊर्जा\n• अमावस्या (Amavasya): पितृ तर्पण, दान-पुण्य एवं साधना",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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

    // Permission Explanation Dialog
    if (showPermissionExplanation) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanation = false },
            title = { Text("स्थान अनुमति (Location Permission)", fontWeight = FontWeight.Bold, color = AccentAmber) },
            text = { Text("स्थान की जानकारी पंचांग और मुहूर्त की सही गणना के लिए उपयोग की जाती है।", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionExplanation = false
                        permissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                ) {
                    Text("अनुमति दें", color = DeepNavy)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionExplanation = false }) {
                    Text("रद्द करें", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Location Confirmation Dialog
    if (showLocationConfirmation) {
        AlertDialog(
            onDismissRequest = { showLocationConfirmation = false },
            title = { Text("क्या आपका स्थान सही है?", fontWeight = FontWeight.Bold, color = AccentAmber) },
            text = { Text("अनुमानित नेटवर्क स्थान (Approximate Location) का उपयोग किया जा रहा है। क्या यह सही है?", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationConfirmation = false
                        viewModel.setLocationSource(locationSource, true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("हाँ (Confirm)", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showLocationConfirmation = false
                    showManualCityDialog = true
                }) {
                    Text("स्थान बदलें", color = AccentAmber)
                }
            }
        )
    }

    // Manual City Selection Dialog
    if (showManualCityDialog) {
        AlertDialog(
            onDismissRequest = { showManualCityDialog = false },
            title = { Text("स्थान खोजें (Search City)", fontWeight = FontWeight.Bold, color = AccentAmber) },
            text = {
                Column {
                    OutlinedTextField(
                        value = cityQuery,
                        onValueChange = { cityQuery = it },
                        label = { Text("शहर का नाम (e.g., Delhi, Varanasi)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            isSearching = true
                            viewModel.resolveLocation(cityQuery) { res ->
                                isSearching = false
                                searchResults = res.getOrElse { emptyList() }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                    ) {
                        Text(if (isSearching) "खोज रहा है..." else "खोजें (Search)", color = DeepNavy)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (searchResults.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.height(150.dp)) {
                            items(searchResults.size) { index ->
                                val loc = searchResults[index]
                                TextButton(
                                    onClick = {
                                        viewModel.saveVerifiedLocation(loc)
                                        showManualCityDialog = false
                                        searchResults = emptyList()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("📍 ${loc.placeName} (${loc.timeZoneId})", color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManualCityDialog = false }) {
                    Text("बंद करें")
                }
            }
        )
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
