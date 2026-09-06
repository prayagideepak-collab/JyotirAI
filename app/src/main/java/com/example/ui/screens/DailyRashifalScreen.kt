package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.viewmodel.DailyRashifalUiState
import com.example.ui.viewmodel.PeriodicPredictionUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyRashifalScreen(
    viewModel: AstrologyViewModel,
    onNavigateToHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val speechManager = remember { JyotirAiSpeechManager(context) }
    DisposableEffect(Unit) {
        onDispose {
            speechManager.release()
        }
    }

    val rashifalState by viewModel.dailyRashifalState.collectAsStateWithLifecycle()
    val targetDate by viewModel.rashifalTargetDate.collectAsStateWithLifecycle()
    val defaultProfile by viewModel.defaultUserProfile.collectAsStateWithLifecycle()

    val periodicState by viewModel.periodicPredictionState.collectAsStateWithLifecycle()
    val periodicPeriodType by viewModel.periodicPeriodType.collectAsStateWithLifecycle()
    val periodicTargetDate by viewModel.periodicTargetDate.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (periodicPeriodType) {
                                PredictionPeriodType.DAILY -> "Daily Predictions"
                                PredictionPeriodType.MONTHLY -> "Monthly Predictions"
                                PredictionPeriodType.YEARLY -> "Yearly Predictions"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = AccentAmber
                        )
                        Text(
                            text = "Deterministic Vedic Predictions (Phase 8)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            speechManager.stop()
                            onNavigateToHome()
                        },
                        modifier = Modifier.testTag("rashifal_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate to Home",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    val currentPeriodicResult = (periodicState as? PeriodicPredictionUiState.Success)?.result
                    if (currentPeriodicResult != null) {
                        AstrologySpeakerButton(
                            speechManager = speechManager,
                            hindiTextProvider = { AstrologyHindiSpeechFormatter.formatPeriodicPrediction(currentPeriodicResult) },
                            buttonStyle = SpeakerButtonStyle.ICON_ONLY,
                            testTag = "periodic_tts_speaker_button"
                        )
                    } else {
                        val currentSuccessRashifal = (rashifalState as? DailyRashifalUiState.Success)?.rashifal
                        if (currentSuccessRashifal != null) {
                            AstrologySpeakerButton(
                                speechManager = speechManager,
                                hindiTextProvider = { AstrologyHindiSpeechFormatter.formatDailyRashifal(currentSuccessRashifal) },
                                buttonStyle = SpeakerButtonStyle.ICON_ONLY,
                                testTag = "rashifal_tts_speaker_button"
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.resetRashifalToToday()
                            viewModel.resetPeriodicPeriodToNow()
                        },
                        modifier = Modifier.testTag("rashifal_reset_today_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Reset to Current Period",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBackground)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Period Selector Tab Row (Daily / Monthly / Yearly)
            item {
                PeriodSelectorTabRow(
                    selectedType = periodicPeriodType,
                    onSelect = { viewModel.setPeriodicPeriodType(it) }
                )
            }

            // 2. Profile Banner & Period Navigator
            item {
                PeriodicNavigationHeader(
                    periodType = periodicPeriodType,
                    defaultProfile = defaultProfile,
                    targetDate = if (periodicPeriodType == PredictionPeriodType.DAILY) targetDate else periodicTargetDate,
                    onPrev = {
                        if (periodicPeriodType == PredictionPeriodType.DAILY) {
                            viewModel.shiftRashifalDays(-1)
                            viewModel.shiftPeriodicPeriod(-1)
                        } else {
                            viewModel.shiftPeriodicPeriod(-1)
                        }
                    },
                    onNext = {
                        if (periodicPeriodType == PredictionPeriodType.DAILY) {
                            viewModel.shiftRashifalDays(1)
                            viewModel.shiftPeriodicPeriod(1)
                        } else {
                            viewModel.shiftPeriodicPeriod(1)
                        }
                    },
                    onReset = {
                        viewModel.resetRashifalToToday()
                        viewModel.resetPeriodicPeriodToNow()
                    }
                )
            }

            // 3. Periodic Main Content State
            when (val pState = periodicState) {
                is PeriodicPredictionUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(SurfaceCard)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = AccentAmber,
                                    modifier = Modifier.testTag("periodic_loading_indicator")
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Synthesizing ${periodicPeriodType.displayName}...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Correlating Vimshottari Dasha, Gochar Transits & Bhava Significations",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is PeriodicPredictionUiState.Success -> {
                    val result = pState.result

                    // 1. Overall Horizon & Energy Summary Card
                    item {
                        PeriodicHorizonCard(result)
                    }

                    // 2. Dasha & Period Transition Alerts (if any)
                    if (result.importantPeriodChanges.isNotEmpty()) {
                        item {
                            PeriodTransitionsCard(result.importantPeriodChanges)
                        }
                    }

                    // 3. Gochar / Transit Evidence Card
                    item {
                        PeriodicTransitCard(result.transitEvidence)
                    }

                    // 4. Supporting & Caution Themes
                    item {
                        ThemesSummaryCard(
                            supportingThemes = result.supportingThemes,
                            cautionThemes = result.cautionThemes
                        )
                    }

                    // 5. Life Topics Breakdown Section Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Life Domain Predictions",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "7 Classical Domains",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentAmber
                            )
                        }
                    }

                    // 6. Topic Cards (Career, Marriage, Finance, Family, Education, Property, General Life)
                    items(LifeTopic.entries) { topic ->
                        val topicPred = result.topicPredictions[topic]
                        if (topicPred != null) {
                            PeriodicTopicCard(topicPred)
                        }
                    }

                    // 7. Limitations & Ephemeris Traceability Card
                    item {
                        PeriodicLimitationsCard(result.limitations)
                    }
                }

                is PeriodicPredictionUiState.InsufficientData -> {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, AccentAmber.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                                .testTag("no_profile_card"),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonSearch,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Birth Profile Required",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pState.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.loadReferenceProfile() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentAmber,
                                        contentColor = DeepNavy
                                    )
                                ) {
                                    Text("Load Sample Profile", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                is PeriodicPredictionUiState.Error -> {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(24.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Prediction Calculation Error",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                PeriodicPredictionUiState.Empty -> {
                    // Fallback to Daily Rashifal state if periodic state is empty
                    when (val state = rashifalState) {
                        is DailyRashifalUiState.Success -> {
                            val r = state.rashifal
                            item {
                                DailyEnergyOverviewCard(r)
                            }
                            item {
                                TaraBalaCard(r.taraBala)
                            }
                            item {
                                PrioritiesAndCautionsCard(
                                    priorities = r.priorities,
                                    cautions = r.cautions
                                )
                            }
                            item {
                                TraditionalRemediesCard(r.traditionalRemedies)
                            }
                            item {
                                DisclaimerAndAttributionCard(r)
                            }
                        }
                        else -> {
                            item {
                                Text(
                                    text = "Ready to generate predictions.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelectorTabRow(
    selectedType: PredictionPeriodType,
    onSelect: (PredictionPeriodType) -> Unit
) {
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
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PredictionPeriodType.entries.forEach { type ->
                val isSelected = type == selectedType
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AccentAmber else Color.Transparent)
                        .clickable { onSelect(type) }
                        .padding(vertical = 10.dp)
                        .testTag("period_tab_${type.code}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = type.displayName.substringBefore(" "),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) DeepNavy else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = type.hindiName.substringBefore(" "),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isSelected) DeepNavy.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodicNavigationHeader(
    periodType: PredictionPeriodType,
    defaultProfile: UserProfile?,
    targetDate: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Target Subject
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = defaultProfile?.name ?: "Current Profile",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                defaultProfile?.location?.placeName?.let { place ->
                    Text(
                        text = place.substringBefore(","),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrev,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceNavy)
                        .testTag("periodic_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Period",
                        tint = AccentAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }

                val periodLabel = when (periodType) {
                    PredictionPeriodType.DAILY -> targetDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
                    PredictionPeriodType.MONTHLY -> targetDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                    PredictionPeriodType.YEARLY -> "Year ${targetDate.year}"
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = periodType.hindiName,
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentAmber
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceNavy)
                        .testTag("periodic_next_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Period",
                        tint = AccentAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodicHorizonCard(result: PeriodicPredictionResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .testTag("periodic_horizon_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Vedic Astrological Horizon",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = AccentAmber
                    )
                    Text(
                        text = "Active Dasha: ${result.dashaEvidence.mahadashaLord}-${result.dashaEvidence.antardashaLord}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SupportLevelBadge(result.overallSupportLevel)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = result.overallSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun PeriodTransitionsCard(transitions: List<PeriodTransitionInfo>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, AccentAmber.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceNavy)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ChangeCircle,
                    contentDescription = null,
                    tint = AccentAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Key Period Transitions",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AccentAmber
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            transitions.forEach { t ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t.transitionType,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = t.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = t.transitionDate.format(DateTimeFormatter.ofPattern("d MMM")),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AccentAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodicTransitCard(transitEvidence: PeriodicTransitEvidence) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = AccentAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gochar (Planetary Transit) Influence",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = transitEvidence.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (transitEvidence.retrogradePlanets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Retrograde Grahas: ${transitEvidence.retrogradePlanets.joinToString(", ")} (Vakri - reflective energy)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = AccentAmber
                )
            }
        }
    }
}

@Composable
private fun ThemesSummaryCard(
    supportingThemes: List<String>,
    cautionThemes: List<String>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Supporting
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Supportive Areas",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF4CAF50)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                supportingThemes.take(3).forEach { theme ->
                    Text(
                        text = "• $theme",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        // Caution
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Caution Areas",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFF9800)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                cautionThemes.take(3).forEach { theme ->
                    Text(
                        text = "• $theme",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodicTopicCard(topicPred: PeriodicTopicPrediction) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded }
            .testTag("topic_card_${topicPred.topic.code}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getTopicIcon(topicPred.topic),
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = topicPred.topic.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = topicPred.topic.hindiName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SupportLevelBadge(topicPred.supportLevel)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = topicPred.synthesis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = BorderSubtle)
                    Spacer(modifier = Modifier.height(8.dp))

                    topicPred.timingGuidance?.let { timing ->
                        Text(
                            text = "Timing Guidance: $timing",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentAmber
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (topicPred.supportingFactors.isNotEmpty()) {
                        Text(
                            text = "Supporting: ${topicPred.supportingFactors.joinToString("; ")}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF4CAF50)
                        )
                    }

                    if (topicPred.cautionFactors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Caution: ${topicPred.cautionFactors.joinToString("; ")}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodicLimitationsCard(limitations: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = "Methodological Framework & Free Will",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = limitations,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Justify
            )
        }
    }
}

private fun getTopicIcon(topic: LifeTopic): ImageVector {
    return when (topic) {
        LifeTopic.CAREER -> Icons.Default.Work
        LifeTopic.MARRIAGE_RELATIONSHIPS -> Icons.Default.Favorite
        LifeTopic.FINANCE -> Icons.Default.AccountBalance
        LifeTopic.FAMILY -> Icons.Default.Group
        LifeTopic.EDUCATION -> Icons.Default.School
        LifeTopic.PROPERTY_HOME -> Icons.Default.Home
        LifeTopic.GENERAL_LIFE -> Icons.Default.SelfImprovement
    }
}

@Composable
private fun SupportLevelBadge(supportLevel: PredictionSupportLevel) {
    val (bgColor, textColor) = when (supportLevel) {
        PredictionSupportLevel.STRONGLY_SUPPORTED -> Color(0xFF4CAF50).copy(alpha = 0.15f) to Color(0xFF4CAF50)
        PredictionSupportLevel.SUPPORTED -> Color(0xFF2196F3).copy(alpha = 0.15f) to Color(0xFF64B5F6)
        PredictionSupportLevel.MIXED_SIGNALS -> Color(0xFFFF9800).copy(alpha = 0.15f) to Color(0xFFFFB74D)
        PredictionSupportLevel.CHALLENGING -> Color(0xFFE91E63).copy(alpha = 0.15f) to Color(0xFFF06292)
        PredictionSupportLevel.LIMITED_DATA,
        PredictionSupportLevel.INSUFFICIENT_DATA -> Color(0xFF9E9E9E).copy(alpha = 0.15f) to Color(0xFFBDBDBD)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = supportLevel.displayName,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = textColor
        )
    }
}

@Composable
private fun DailyEnergyOverviewCard(rashifal: DailyRashifal) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
            .testTag("rashifal_overview_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Jyotish Alignment",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tara Bala & Gochar Synthesis",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AccentAmber.copy(alpha = 0.12f))
                        .border(1.5.dp, AccentAmber, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${rashifal.energyScore}%",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = AccentAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = rashifal.dailyTheme,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Primary Focus: ${rashifal.primaryFocus}",
                style = MaterialTheme.typography.bodySmall,
                color = AccentAmber
            )
        }
    }
}

@Composable
private fun TaraBalaCard(taraBala: TaraBalaInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tara Bala (Lunar Nakshatra Force)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AccentAmber
                )
                Text(
                    text = "${taraBala.taraName} (${taraBala.quality})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (taraBala.quality.contains("Favorable", ignoreCase = true)) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = taraBala.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrioritiesAndCautionsCard(
    priorities: List<DailyRecommendation>,
    cautions: List<DailyCaution>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Priorities",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(6.dp))
                priorities.forEach { p ->
                    Text(
                        text = "• ${p.category}: ${p.advice}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Cautions",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.height(6.dp))
                cautions.forEach { c ->
                    Text(
                        text = "• ${c.category}: ${c.warning}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TraditionalRemediesCard(remedies: List<TraditionalRemedy>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Traditional Astrological Upayas",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AccentAmber
            )
            Spacer(modifier = Modifier.height(10.dp))
            remedies.forEach { r ->
                Text(
                    text = "${r.title}: ${r.practice}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DisclaimerAndAttributionCard(rashifal: DailyRashifal) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = "Calculation Traceability",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rashifal.astrologicalFactorsSummary,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = rashifal.ethicalDisclaimer,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
