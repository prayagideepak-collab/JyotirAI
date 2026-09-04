package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.models.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AstrologyViewModel
import com.example.ui.viewmodel.DashaUiState
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashaScreen(
    viewModel: AstrologyViewModel,
    modifier: Modifier = Modifier,
    onNavigateToHome: () -> Unit = {}
) {
    val dashaState by viewModel.dashaUiState.collectAsStateWithLifecycle()
    val currentBirthData by viewModel.currentBirthData.collectAsStateWithLifecycle()
    val expandedPlanet by viewModel.expandedMahadashaPlanet.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Vimshottari Dasha",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "120-Year Vedic Planetary Progression",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        },
        containerColor = BackgroundDark,
        modifier = modifier.fillMaxSize().testTag("dasha_screen")
    ) { innerPadding ->
        when (val state = dashaState) {
            is DashaUiState.Empty -> {
                DashaEmptyState(
                    onLoadSample = { viewModel.loadReferenceProfile() },
                    onNavigateToHome = onNavigateToHome,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is DashaUiState.Calculating -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = AccentGold,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Computing Vimshottari Timeline...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
            is DashaUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.Red.copy(alpha = 0.5f))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Calculation Error",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    currentBirthData?.let { viewModel.loadDashaTimeline(it) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                            ) {
                                Text("Retry Calculation", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            is DashaUiState.Success -> {
                val timeline = state.timeline
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // 1. Current Dasha Active Card (Section C)
                    item {
                        CurrentDashaCard(
                            timeline = timeline,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 2. Dasha Summary Card (Section A)
                    item {
                        DashaSummaryCard(
                            timeline = timeline,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 3. Section Header for Mahadasha Timeline (Section B)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Mahadasha Sequence (120 Years)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Tap to view Antardashas",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // 4. Mahadasha Cards with Expandable Antardashas (Section B & D)
                    items(timeline.mahadashaPeriods, key = { it.planet.name }) { mahadasha ->
                        MahadashaItemCard(
                            mahadasha = mahadasha,
                            isExpanded = (expandedPlanet == mahadasha.planet),
                            onToggleExpand = { viewModel.toggleExpandMahadasha(mahadasha.planet) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section C: Highlights the active Mahadasha & Antardasha with elapsed progress.
 */
@Composable
fun CurrentDashaCard(
    timeline: DashaTimeline,
    modifier: Modifier = Modifier
) {
    val currentMd = timeline.currentMahadasha
    val currentAd = timeline.currentAntardasha

    Card(
        modifier = modifier.testTag("current_dasha_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(AccentGold.copy(alpha = 0.6f)),
            width = 1.5.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AccentGold)
                    )
                    Text(
                        text = "ACTIVE DASHA PERIOD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "As of ${DATE_FORMATTER.format(timeline.targetDateTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (currentMd != null && currentAd != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${currentMd.planet.lord} Mahadasha",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${currentAd.antardashaLord.lord} Antardasha (${currentMd.planet.abbreviation} - ${currentAd.antardashaLord.abbreviation})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentAmber
                        )
                    }

                    Surface(
                        color = Color(currentMd.planet.colorHex).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(currentMd.planet.colorHex).copy(alpha = 0.5f))
                        )
                    ) {
                        Text(
                            text = currentMd.planet.sanskritName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(currentMd.planet.colorHex),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                // Dates and remaining durations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Mahadasha Span",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${DATE_FORMATTER.format(currentMd.startDate)} → ${DATE_FORMATTER.format(currentMd.endDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }

                    val totalSec = ChronoUnit.SECONDS.between(currentMd.startDate, currentMd.endDate).coerceAtLeast(1L)
                    val elapsedSec = ChronoUnit.SECONDS.between(currentMd.startDate, timeline.targetDateTime).coerceAtLeast(0L)
                    val progressFraction = (elapsedSec.toDouble() / totalSec.toDouble()).coerceIn(0.0, 1.0)

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "MD Remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        val remSec = (totalSec - elapsedSec).coerceAtLeast(0L)
                        val remYears = remSec.toDouble() / com.example.domain.engine.VimshottariDashaCalculator.SECONDS_PER_SOLAR_YEAR
                        val balance = DashaBalance.fromYears(remYears)
                        Text(
                            text = balance.formatted,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                    }
                }

                // Antardasha Span
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Antardasha Span",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${DATE_FORMATTER.format(currentAd.startDate)} → ${DATE_FORMATTER.format(currentAd.endDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "AD Remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        val adTotalSec = ChronoUnit.SECONDS.between(currentAd.startDate, currentAd.endDate).coerceAtLeast(1L)
                        val adElapsedSec = ChronoUnit.SECONDS.between(currentAd.startDate, timeline.targetDateTime).coerceAtLeast(0L)
                        val adRemSec = (adTotalSec - adElapsedSec).coerceAtLeast(0L)
                        val adRemYears = adRemSec.toDouble() / com.example.domain.engine.VimshottariDashaCalculator.SECONDS_PER_SOLAR_YEAR
                        val adBalance = DashaBalance.fromYears(adRemYears)
                        Text(
                            text = adBalance.formatted,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentAmber
                        )
                    }
                }
            } else {
                Text(
                    text = "Current date is outside the calculated 120-year timeline interval.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Section A: Dasha Summary showing birth Nakshatra, starting lord, and starting balance.
 */
@Composable
fun DashaSummaryCard(
    timeline: DashaTimeline,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("dasha_summary_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Natal Dasha Foundation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryMetricItem(
                    label = "Birth Nakshatra",
                    value = timeline.birthNakshatra.sanskritName,
                    subtext = "Lord: ${timeline.nakshatraLord}",
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricItem(
                    label = "Starting Mahadasha",
                    value = timeline.startingMahadasha.lord,
                    subtext = "Total: ${timeline.startingMahadasha.years} Years",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryMetricItem(
                    label = "Balance at Birth",
                    value = timeline.startingBalance.formatted,
                    subtext = "${(timeline.fractionRemaining * 100).toInt()}% unelapsed",
                    modifier = Modifier.weight(1f),
                    highlight = true
                )
                SummaryMetricItem(
                    label = "Moon Sidereal Pos",
                    value = String.format("%.2f°", timeline.moonLongitude),
                    subtext = "${(timeline.fractionElapsed * 100).toInt()}% Nakshatra elapsed",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricItem(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Surface(
        modifier = modifier,
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (highlight) AccentGold.copy(alpha = 0.4f) else BorderSubtle)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlight) AccentGold else TextPrimary
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Section B & D: Individual Mahadasha Card with expandable Antardasha details.
 */
@Composable
fun MahadashaItemCard(
    mahadasha: MahadashaPeriod,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val planetColor = Color(mahadasha.planet.colorHex)

    Card(
        modifier = modifier
            .testTag("mahadasha_item_${mahadasha.planet.lord}")
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggleExpand() },
        colors = CardDefaults.cardColors(
            containerColor = if (mahadasha.isCurrent) SurfaceElevated else SurfaceCard
        ),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (mahadasha.isCurrent) AccentGold else BorderSubtle
            ),
            width = if (mahadasha.isCurrent) 1.5.dp else 1.dp
        )
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
                // Planet badge and name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(planetColor.copy(alpha = 0.15f))
                            .border(1.dp, planetColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mahadasha.planet.abbreviation,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = planetColor
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = mahadasha.planet.lord,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            if (mahadasha.isCurrent) {
                                Surface(
                                    color = AccentGold,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "CURRENT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (mahadasha.isBirthMahadasha) {
                                Surface(
                                    color = Color(0xFF334155),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "AT BIRTH",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${DATE_FORMATTER.format(mahadasha.startDate)} → ${DATE_FORMATTER.format(mahadasha.endDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Duration and Expand Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (mahadasha.isBirthMahadasha && mahadasha.birthBalance != null) {
                                "Bal: ${mahadasha.birthBalance.formatted}"
                            } else {
                                "${mahadasha.planet.years} Years"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (mahadasha.isCurrent) AccentGold else TextPrimary
                        )
                        Text(
                            text = "${mahadasha.planet.sanskritName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("expand_button_${mahadasha.planet.lord}")
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse Antardashas" else "Expand Antardashas",
                            tint = if (mahadasha.isCurrent) AccentGold else TextSecondary
                        )
                    }
                }
            }

            // Section D: Expandable Antardasha Breakdown Table
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                    Text(
                        text = "Antardasha (Bhukti) Sequence",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentAmber,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    mahadasha.antardashas.forEach { antardasha ->
                        AntardashaRow(antardasha = antardasha)
                    }
                }
            }
        }
    }
}

/**
 * Renders an individual Antardasha row with start/end date, duration, and active indicator.
 */
@Composable
private fun AntardashaRow(
    antardasha: AntardashaPeriod,
    modifier: Modifier = Modifier
) {
    val aColor = Color(antardasha.antardashaLord.colorHex)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("antardasha_row_${antardasha.antardashaLord.lord}"),
        color = if (antardasha.isCurrent) AccentGold.copy(alpha = 0.1f) else SurfaceDark,
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (antardasha.isCurrent) AccentGold.copy(alpha = 0.5f) else BorderSubtle
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(aColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = antardasha.antardashaLord.abbreviation,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = aColor,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = "${antardasha.mahadashaLord.abbreviation} - ${antardasha.antardashaLord.lord}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (antardasha.isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (antardasha.isCurrent) AccentGold else TextPrimary
                )

                if (antardasha.isCurrent) {
                    Surface(
                        color = AccentGold,
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${DATE_FORMATTER.format(antardasha.startDate)} → ${DATE_FORMATTER.format(antardasha.endDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = antardasha.durationBalance.formatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (antardasha.isCurrent) AccentGold else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Empty state when birth data has not been configured or loaded yet.
 */
@Composable
private fun DashaEmptyState(
    onLoadSample: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("empty_dasha_view"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AccentAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Dasha Clock",
                        tint = AccentAmber,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "No Birth Profile Loaded",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "Vimshottari Dasha requires the authoritative sidereal Moon position and birth Nakshatra from your birth chart.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onLoadSample,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("load_sample_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Load Sample Profile (New Delhi)", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onNavigateToHome,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Enter Custom Birth Details")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
