package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.models.*
import com.example.domain.muhurta.MuhurtaHindiPresenter
import com.example.domain.speech.JyotirAiSpeechManager
import com.example.ui.components.AstrologySpeakerButton
import com.example.ui.components.SpeakerButtonStyle
import com.example.ui.theme.*
import com.example.ui.viewmodel.AstrologyViewModel
import com.example.ui.viewmodel.MuhurtaUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuhurtaScreen(viewModel: AstrologyViewModel) {
    val context = LocalContext.current
    val speechManager = remember { JyotirAiSpeechManager(context) }
    DisposableEffect(Unit) {
        onDispose {
            speechManager.release()
        }
    }

    val uiState by viewModel.muhurtaUiState.collectAsStateWithLifecycle()
    val selectedActivity by viewModel.selectedMuhurtaActivity.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedMuhurtaDate.collectAsStateWithLifecycle()
    val daysSpan by viewModel.muhurtaDaysSpan.collectAsStateWithLifecycle()
    val timeSlot by viewModel.selectedMuhurtaTimeSlot.collectAsStateWithLifecycle()
    val isPersonalized by viewModel.isMuhurtaPersonalized.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeUserProfile.collectAsStateWithLifecycle()
    val alarms by viewModel.muhurtaAlarms.collectAsStateWithLifecycle()

    var showActivityDropdown by remember { mutableStateOf(false) }

    // Date Picker Dialog Launcher
    val onPickDateClick = {
        val dpd = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = LocalDate.of(year, month + 1, dayOfMonth)
                viewModel.setMuhurtaDate(picked)
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )
        dpd.show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Vedic Muhurta Explorer",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = AccentAmber
                        )
                        Text(
                            text = "शुभ मुहूर्त • Auspicious Timing Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    val result = (uiState as? MuhurtaUiState.Success)?.result
                    if (result != null) {
                        AstrologySpeakerButton(
                            speechManager = speechManager,
                            hindiTextProvider = { MuhurtaHindiPresenter.formatSpeechNarration(result) },
                            buttonStyle = SpeakerButtonStyle.ICON_ONLY,
                            testTag = "muhurta_tts_speaker_button"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Activity Selection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("muhurta_activity_selector_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Purpose / Activity",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box {
                            FilledTonalButton(
                                onClick = { showActivityDropdown = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("muhurta_activity_dropdown_button")
                            ) {
                                Text(
                                    text = selectedActivity.hindiName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Activity")
                            }

                            DropdownMenu(
                                expanded = showActivityDropdown,
                                onDismissRequest = { showActivityDropdown = false }
                            ) {
                                MuhurtaActivityType.entries.forEach { act ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = act.hindiName,
                                                    fontWeight = if (act == selectedActivity) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (act == selectedActivity) AccentAmber else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = act.englishName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.setMuhurtaActivity(act)
                                            showActivityDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedActivity.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Date Navigation & Search Span Toolbar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("muhurta_date_toolbar_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.shiftMuhurtaDays(-1) },
                            modifier = Modifier.testTag("muhurta_prev_day_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier
                                .clickable { onPickDateClick() }
                                .padding(4.dp)
                                .testTag("muhurta_date_picker_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = "Pick Date",
                                    tint = AccentAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.ENGLISH)),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.shiftMuhurtaDays(1) },
                            modifier = Modifier.testTag("muhurta_next_day_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                        }
                    }

                    // Search Span & Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1 to "1 Day", 3 to "3 Days", 7 to "7 Days").forEach { (days, label) ->
                                FilterChip(
                                    selected = daysSpan == days,
                                    onClick = { viewModel.setMuhurtaDaysSpan(days) },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentAmber.copy(alpha = 0.2f),
                                        selectedLabelColor = AccentAmber
                                    )
                                )
                            }
                        }

                        // Personalization Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Tara Bala",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPersonalized) AccentAmber else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Switch(
                                checked = isPersonalized,
                                onCheckedChange = { viewModel.toggleMuhurtaPersonalization(it) },
                                modifier = Modifier
                                    .scale(0.8f)
                                    .testTag("muhurta_personalization_toggle")
                            )
                        }
                    }
                }
            }

            // 3. Main Calculation Content
            when (val state = uiState) {
                is MuhurtaUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentAmber)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Evaluating Vedic Muhurta & Panchang Shuddhi...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is MuhurtaUiState.Error -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("muhurta_error_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Calculation Issue",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.loadMuhurta() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Retry Calculation")
                            }
                        }
                    }
                }

                is MuhurtaUiState.Success -> {
                    MuhurtaSuccessContent(
                        result = state.result,
                        viewModel = viewModel,
                        alarms = alarms
                    )
                }
            }
        }
    }
}

@Composable
private fun MuhurtaSuccessContent(
    result: MuhurtaResult,
    viewModel: AstrologyViewModel,
    alarms: List<MuhurtaAlarmConfig>
) {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

    // 1. Best Window Hero Card
    if (result.bestWindow != null) {
        val best = result.bestWindow
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("muhurta_best_window_hero_card"),
            colors = CardDefaults.cardColors(
                containerColor = if (best.evaluationState == MuhurtaEvaluationState.FAVORABLE) {
                    AccentEmerald.copy(alpha = 0.15f)
                } else {
                    AccentAmber.copy(alpha = 0.15f)
                }
            ),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (best.evaluationState == MuhurtaEvaluationState.FAVORABLE) AccentEmerald else AccentAmber
            )
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (best.evaluationState == MuhurtaEvaluationState.FAVORABLE) AccentEmerald else AccentAmber,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Text(
                            text = "TOP RECOMMENDED WINDOW",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = if (best.evaluationState == MuhurtaEvaluationState.FAVORABLE) AccentEmerald else AccentAmber
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = "${best.localDate.format(DateTimeFormatter.ofPattern("d MMM"))}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = best.sanskritName ?: best.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = "Time",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${best.startTime.format(timeFormatter)} - ${best.endTime.format(timeFormatter)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    StatusBadge(state = best.evaluationState)
                }

                if (best.personalBalaContext != null) {
                    val pb = best.personalBalaContext
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = pb.balaSummary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // 2. All Candidate Windows List
    Text(
        text = "All Evaluated Time Windows (${result.candidateWindows.size})",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        result.candidateWindows.forEach { window ->
            MuhurtaWindowCard(window = window, timeFormatter = timeFormatter)
        }
    }

    // 3. Brahma Muhurta & Abhijit Quick Alarm Cards
    Text(
        text = "Standard Daily Muhurta Alarms",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val brahmaAlarm = alarms.find { it.type == MuhurtaAlarmType.BRAHMA_MUHURTA }
        val isBrahmaEnabled = brahmaAlarm?.isEnabled == true
        MuhurtaAlarmCard(
            modifier = Modifier.weight(1f),
            title = "Brahma Muhurta",
            subtitle = "Pre-Dawn Sattva",
            isEnabled = isBrahmaEnabled,
            onToggle = { viewModel.toggleMuhurtaAlarm(MuhurtaAlarmType.BRAHMA_MUHURTA, it) }
        )

        val abhijitAlarm = alarms.find { it.type == MuhurtaAlarmType.ABHIJIT_MUHURTA }
        val isAbhijitEnabled = abhijitAlarm?.isEnabled == true
        MuhurtaAlarmCard(
            modifier = Modifier.weight(1f),
            title = "Abhijit Muhurta",
            subtitle = "Midday Victory",
            isEnabled = isAbhijitEnabled,
            onToggle = { viewModel.toggleMuhurtaAlarm(MuhurtaAlarmType.ABHIJIT_MUHURTA, it) }
        )
    }

    // 4. Overall Astrological Factors
    if (result.overallSupportingFactors.isNotEmpty() || result.overallCautionFactors.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("muhurta_factors_summary_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Astronomical Factors & Panchang Shuddhi",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (result.overallSupportingFactors.isNotEmpty()) {
                    Text(
                        text = "Supporting Auspicious Factors:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccentEmerald
                    )
                    result.overallSupportingFactors.take(3).forEach { factor ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("•", color = AccentEmerald, fontWeight = FontWeight.Bold)
                            Column {
                                Text(factor.title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text(factor.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (result.overallCautionFactors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cautionary Factors:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccentCrimson
                    )
                    result.overallCautionFactors.take(3).forEach { factor ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("•", color = AccentCrimson, fontWeight = FontWeight.Bold)
                            Column {
                                Text(factor.title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text(factor.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MuhurtaWindowCard(
    window: MuhurtaCandidateWindow,
    timeFormatter: DateTimeFormatter
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("muhurta_window_card_${window.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = window.sanskritName ?: window.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${window.localDate.format(DateTimeFormatter.ofPattern("d MMM"))} • ${window.startTime.format(timeFormatter)} - ${window.endTime.format(timeFormatter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(state = window.evaluationState)
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    if (window.personalBalaContext != null) {
                        Text(
                            text = window.personalBalaContext.balaSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentAmber
                        )
                    }

                    if (window.supportingFactors.isNotEmpty()) {
                        window.supportingFactors.forEach { factor ->
                            Text(
                                text = "✓ ${factor.title}: ${factor.description}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentEmerald
                            )
                        }
                    }

                    if (window.cautionFactors.isNotEmpty()) {
                        window.cautionFactors.forEach { factor ->
                            Text(
                                text = "⚠ ${factor.title}: ${factor.description}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentCrimson
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MuhurtaAlarmCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnabled) "Active" else "Off",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.scale(0.75f)
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(state: MuhurtaEvaluationState) {
    val (bgColor, textColor, text) = when (state) {
        MuhurtaEvaluationState.FAVORABLE -> Triple(AccentEmerald.copy(alpha = 0.2f), AccentEmerald, "शुभ / Favorable")
        MuhurtaEvaluationState.CONDITIONALLY_FAVORABLE -> Triple(AccentAmber.copy(alpha = 0.2f), AccentAmber, "मध्यम / Conditional")
        MuhurtaEvaluationState.MIXED -> Triple(Color(0xFF9E9E9E).copy(alpha = 0.2f), Color(0xFFE0E0E0), "मिश्रित / Mixed")
        MuhurtaEvaluationState.CAUTION -> Triple(AccentCrimson.copy(alpha = 0.2f), AccentCrimson, "वर्जित / Caution")
        MuhurtaEvaluationState.INSUFFICIENT_DATA -> Triple(Color.Gray.copy(alpha = 0.2f), Color.Gray, "No Data")
        MuhurtaEvaluationState.CALCULATION_ERROR -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, "Error")
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.size(width = (48 * scale).dp, height = (28 * scale).dp)
)
