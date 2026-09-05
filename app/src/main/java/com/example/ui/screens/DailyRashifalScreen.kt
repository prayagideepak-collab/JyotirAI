package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daily Rashifal",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = AccentAmber
                        )
                        Text(
                            text = "Personalised Jyotish Engine",
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
                    val currentSuccessRashifal = (rashifalState as? DailyRashifalUiState.Success)?.rashifal
                    if (currentSuccessRashifal != null) {
                        AstrologySpeakerButton(
                            speechManager = speechManager,
                            hindiTextProvider = { AstrologyHindiSpeechFormatter.formatDailyRashifal(currentSuccessRashifal) },
                            buttonStyle = SpeakerButtonStyle.ICON_ONLY,
                            testTag = "rashifal_tts_speaker_button"
                        )
                    }

                    IconButton(
                        onClick = { viewModel.resetRashifalToToday() },
                        modifier = Modifier.testTag("rashifal_reset_today_button")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBackground)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Default Profile Banner & Date Navigator
            item {
                DateAndProfileHeader(
                    defaultProfile = defaultProfile,
                    targetDate = targetDate,
                    onPrevDay = { viewModel.shiftRashifalDays(-1) },
                    onNextDay = { viewModel.shiftRashifalDays(1) },
                    onResetToday = { viewModel.resetRashifalToToday() }
                )
            }

            // 2. Main Content State
            when (val state = rashifalState) {
                is DailyRashifalUiState.Loading -> {
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
                                    modifier = Modifier.testTag("rashifal_loading_indicator")
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Synthesizing Personalised Gochar & Dasha...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Evaluating Tara Bala, Lunar Transit & Panchang Energy",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is DailyRashifalUiState.NoDefaultProfile -> {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, AccentAmber.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                                .testTag("no_default_profile_card"),
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
                                    text = "Default Profile Required",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Daily Rashifal requires an explicit Default Profile to calculate precise Vedic transits, Tara Bala, and Dasha cycles.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onNavigateToHome,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = DeepNavy),
                                    modifier = Modifier.testTag("go_to_profiles_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Select or Create Profile", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                is DailyRashifalUiState.Error -> {
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
                                    text = "Calculation Error",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.loadDailyRashifal() }) {
                                    Text("Retry Calculation")
                                }
                            }
                        }
                    }
                }

                is DailyRashifalUiState.Success -> {
                    val r = state.rashifal

                    // 2. Daily Energy & Theme Banner
                    item {
                        DailyThemeCard(
                            rashifal = r,
                            speechManager = speechManager
                        )
                    }

                    // 3. Astrological Alignment & Traceability Matrix
                    item {
                        AstrologicalAlignmentCard(rashifal = r)
                    }

                    // 4. Key Influences
                    item {
                        KeyInfluencesSection(influences = r.keyInfluences)
                    }

                    // 5. Priorities & Favorable Actions
                    item {
                        PrioritiesSection(recommendations = r.priorities)
                    }

                    // 6. Cautions & Mindfulness Areas
                    item {
                        CautionsSection(cautions = r.cautions)
                    }

                    // 7. Local Auspicious Timing (Muhurta)
                    item {
                        TimingGuidanceCard(timing = r.timingGuidance, locationName = r.birthLocationName)
                    }

                    // 8. Traditional Upay / Remedies
                    item {
                        TraditionalRemediesSection(remedies = r.traditionalRemedies)
                    }

                    // 9. Full Astrological Factors Attribution & Disclaimer
                    item {
                        DisclaimerAndAttributionCard(rashifal = r)
                    }
                }
            }
        }
    }
}

@Composable
private fun DateAndProfileHeader(
    defaultProfile: UserProfile?,
    targetDate: LocalDate,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onResetToday: () -> Unit
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
            // Profile Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AccentAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = defaultProfile?.name ?: "No Default Profile",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "DEFAULT PROFILE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 9.sp),
                                color = AccentAmber
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "•  ${defaultProfile?.location?.placeName ?: ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(14.dp))

            // Date Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevDay,
                    modifier = Modifier.testTag("rashifal_prev_day_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Day",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                val isToday = targetDate == LocalDate.now()
                val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onResetToday() }
                ) {
                    Text(
                        text = targetDate.format(formatter),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isToday) AccentAmber else MaterialTheme.colorScheme.onSurface
                    )
                    if (isToday) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentAmber
                        )
                    } else {
                        Text(
                            text = "Tap to reset to Today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onNextDay,
                    modifier = Modifier.testTag("rashifal_next_day_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Day",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyThemeCard(
    rashifal: DailyRashifal,
    speechManager: JyotirAiSpeechManager
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, AccentAmber.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Daily Theme",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccentAmber
                    )
                }

                // Energy Score Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentAmber.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Alignment ${rashifal.energyScore}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AccentAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = rashifal.dailyTheme,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = rashifal.primaryFocus,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hindi Audio Player Button
            AstrologySpeakerButton(
                speechManager = speechManager,
                hindiTextProvider = { AstrologyHindiSpeechFormatter.formatDailyRashifal(rashifal) },
                buttonStyle = SpeakerButtonStyle.FILLED_CHIP,
                modifier = Modifier.fillMaxWidth(),
                testTag = "rashifal_listen_hindi_button"
            )
        }
    }
}

@Composable
private fun AstrologicalAlignmentCard(rashifal: DailyRashifal) {
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
                text = "Natal & Gochar Alignment",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlignmentBadge(label = "Lagna", value = rashifal.lagna, modifier = Modifier.weight(1f))
                AlignmentBadge(label = "Janma Rashi", value = rashifal.moonSign, modifier = Modifier.weight(1f))
                AlignmentBadge(label = "Janma Nakshatra", value = rashifal.birthNakshatra, modifier = Modifier.weight(1.2f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlignmentBadge(
                    label = "Active Dasha",
                    value = "${rashifal.currentMahadashaLord}-${rashifal.currentAntardashaLord}",
                    modifier = Modifier.weight(1f)
                )
                AlignmentBadge(
                    label = "Navatara",
                    value = rashifal.taraBala.taraName,
                    highlight = rashifal.taraBala.quality == "Highly Favorable" || rashifal.taraBala.quality == "Supreme Favor",
                    modifier = Modifier.weight(1f)
                )
                AlignmentBadge(
                    label = "Transit Moon",
                    value = rashifal.transitMoonHouseFromNatalMoon?.let { "Bhava $it" } ?: "Active",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AlignmentBadge(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .border(
                1.dp,
                if (highlight) AccentAmber.copy(alpha = 0.4f) else BorderSubtle,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (highlight) AccentAmber else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun KeyInfluencesSection(influences: List<AstrologicalInfluence>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Key Planetary Influences",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        influences.forEach { inf ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = inf.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val (chipBg, chipText) = when (inf.impactType) {
                            ImpactType.FAVORABLE -> Pair(Color(0xFF1B5E20).copy(alpha = 0.3f), Color(0xFF81C784))
                            ImpactType.CAUTION -> Pair(Color(0xFFB71C1C).copy(alpha = 0.3f), Color(0xFFE57373))
                            ImpactType.NEUTRAL -> Pair(SurfaceElevated, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = inf.impactType.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = chipText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = inf.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Factor: ${inf.contributingFactor}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = AccentAmber.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrioritiesSection(recommendations: List<DailyRecommendation>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Favourable Priorities & Actions",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        recommendations.forEach { rec ->
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
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rec.category,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AccentAmber
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = rec.advice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Astrological Basis: ${rec.astrologicalReason}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CautionsSection(cautions: List<DailyCaution>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Mindfulness & Caution Areas",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        cautions.forEach { c ->
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
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC62828).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = c.category,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFE57373)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = c.warning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reason: ${c.astrologicalReason}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimingGuidanceCard(timing: DailyTimingGuidance, locationName: String) {
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
                    text = "Auspicious Timing (Muhurta)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = locationName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Brahma Muhurta
            timing.brahmaMuhurtaWindow?.let { win ->
                TimingRow(
                    icon = Icons.Default.Brightness5,
                    title = "Brahma Muhurta",
                    window = win,
                    description = timing.brahmaMuhurtaAdvice ?: "Early morning auspicious contemplation window.",
                    iconTint = AccentAmber
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Rahukaal
            timing.rahukaalWindow?.let { win ->
                TimingRow(
                    icon = Icons.Default.AccessTime,
                    title = "Rahukaal (Inauspicious)",
                    window = win,
                    description = timing.rahukaalAdvice ?: "Avoid initiating major ventures during this period.",
                    iconTint = Color(0xFFE57373)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Abhijit Muhurta
            timing.abhijitMuhurtaWindow?.let { win ->
                TimingRow(
                    icon = Icons.Default.Flare,
                    title = "Abhijit Muhurta",
                    window = win,
                    description = "Midday window generally auspicious for executing significant actions.",
                    iconTint = Color(0xFF81C784)
                )
            }
        }
    }
}

@Composable
private fun TimingRow(
    icon: ImageVector,
    title: String,
    window: String,
    description: String,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = window,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = iconTint
                )
            }
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
private fun TraditionalRemediesSection(remedies: List<TraditionalRemedy>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Traditional Astrological Upay (Remedies)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        remedies.forEach { r ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = r.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccentAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = r.practice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Target: ${r.targetGrahaOrEnergy} • ${r.traditionalContext}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = rashifal.ethicalDisclaimer,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Justify
            )
        }
    }
}
