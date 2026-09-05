package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.domain.interpretation.*
import com.example.domain.speech.AstrologyHindiSpeechFormatter
import com.example.domain.speech.JyotirAiSpeechManager
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdvancedInterpretationUiState

@Composable
fun VedicInterpretationView(
    state: AdvancedInterpretationUiState,
    speechManager: JyotirAiSpeechManager,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
            .testTag("vedic_interpretation_panel"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            when (state) {
                is AdvancedInterpretationUiState.Calculating -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentGold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Synthesizing Vedic interpretation from astronomical data...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is AdvancedInterpretationUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onRefresh) {
                            Text("Retry Interpretation")
                        }
                    }
                }
                is AdvancedInterpretationUiState.Success -> {
                    InterpretationContent(
                        interpretation = state.interpretation,
                        speechManager = speechManager,
                        onRefresh = onRefresh
                    )
                }
                is AdvancedInterpretationUiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Calculate a chart to view the Vedic Intelligence & Interpretation analysis.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InterpretationContent(
    interpretation: AdvancedVedicInterpretation,
    speechManager: JyotirAiSpeechManager,
    onRefresh: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Synthesis" to "समन्वय",
        "Lagna & Grahas" to "लग्न व ग्रह",
        "Vargas" to "वर्ग",
        "Dasha & Transit" to "दशा व गोचर",
        "Panchang" to "पंचांग"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Title & Audio Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Vedic Intelligence & Interpretation",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold
                )
                Text(
                    text = "उन्नत वैदिक फलित (Deterministic Parashari Analysis)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AstrologySpeakerButton(
                    speechManager = speechManager,
                    hindiTextProvider = {
                        AstrologyHindiSpeechFormatter.formatAdvancedInterpretationSummary(interpretation)
                    },
                    buttonStyle = SpeakerButtonStyle.FILLED_CHIP,
                    testTag = "interpretation_audio_button"
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_interpretation_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Interpretation",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Navigation Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            contentColor = AccentGold,
            divider = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, (en, hi) ->
                val isSelected = selectedTab == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag("interpretation_tab_${index}"),
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = en,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = hi,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) AccentGold.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content
        when (selectedTab) {
            0 -> SynthesisTab(interpretation)
            1 -> LagnaAndPlanetsTab(interpretation)
            2 -> VargasTab(interpretation)
            3 -> DashaAndTransitTab(interpretation)
            4 -> PanchangTab(interpretation)
        }
    }
}

@Composable
private fun SynthesisTab(interpretation: AdvancedVedicInterpretation) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Dominant Factor Card
        interpretation.dominantFactor?.let { dominant ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, AccentGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .testTag("interpretation_dominant_factor_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PRIMARY DRIVER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                        PolarityBadge(dominant.polarity)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dominant.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dominant.calculatedValue,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Opportunities & Supportive Factors
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Key Opportunities & Harmonious Strengths",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AccentEmerald
                )
                Spacer(modifier = Modifier.height(8.dp))
                interpretation.opportunities.forEach { opp ->
                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                        Text("• ", color = AccentEmerald, fontWeight = FontWeight.Bold)
                        Text(
                            text = opp,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cautions & Mindful Navigation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Mindful Considerations & Growth Edges",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AccentAmber
                )
                Spacer(modifier = Modifier.height(8.dp))
                interpretation.cautions.forEach { caution ->
                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                        Text("• ", color = AccentAmber, fontWeight = FontWeight.Bold)
                        Text(
                            text = caution,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Traditional Classical Guidance
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Traditional Vedic Guidance (Dharmic Principles)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                interpretation.traditionalGuidance.forEach { guide ->
                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                        Text("• ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(
                            text = guide,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Ethical Boundaries & Disclaimer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .testTag("interpretation_disclaimer_banner"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = interpretation.disclaimer,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LagnaAndPlanetsTab(interpretation: AdvancedVedicInterpretation) {
    val lagnaItem = interpretation.allInterpretationItems.firstOrNull { it.id == "natal_lagna" }
    val moonItem = interpretation.allInterpretationItems.firstOrNull { it.id == "moon_and_mind" }
    val planetItem = interpretation.allInterpretationItems.firstOrNull { it.id == "planetary_dignity" }

    Column(modifier = Modifier.fillMaxWidth()) {
        lagnaItem?.let { InterpretationItemCard(it) }
        Spacer(modifier = Modifier.height(12.dp))
        moonItem?.let { InterpretationItemCard(it) }
        Spacer(modifier = Modifier.height(12.dp))
        planetItem?.let { InterpretationItemCard(it) }
    }
}

@Composable
private fun VargasTab(interpretation: AdvancedVedicInterpretation) {
    val d9Item = interpretation.allInterpretationItems.firstOrNull { it.id == "varga_d9_navamsha" }
    val d10Item = interpretation.allInterpretationItems.firstOrNull { it.id == "varga_d10_dashamsha" }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (d9Item != null) {
            InterpretationItemCard(d9Item)
        } else {
            TruthfulUnavailableCard("D9 Navamsha Chart", "D9 Navamsha calculation is currently unavailable.")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (d10Item != null) {
            InterpretationItemCard(d10Item)
        } else {
            TruthfulUnavailableCard("D10 Dashamsha Chart", "D10 Dashamsha calculation is currently unavailable.")
        }
    }
}

@Composable
private fun DashaAndTransitTab(interpretation: AdvancedVedicInterpretation) {
    val dashaItem = interpretation.allInterpretationItems.firstOrNull { it.id == "dasha_timing" }
    val transitItem = interpretation.allInterpretationItems.firstOrNull { it.id == "transit_influence" }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (dashaItem != null) {
            InterpretationItemCard(dashaItem)
        } else {
            TruthfulUnavailableCard("Vimshottari Dasha", "Dasha timeline not currently loaded for this chart.")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (transitItem != null) {
            InterpretationItemCard(transitItem)
        } else {
            TruthfulUnavailableCard("Planetary Transits (Gochar)", "Transit snapshot not currently calculated for this reference time.")
        }
    }
}

@Composable
private fun PanchangTab(interpretation: AdvancedVedicInterpretation) {
    val panchangItem = interpretation.allInterpretationItems.firstOrNull { it.id == "panchang_alignment" }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (panchangItem != null) {
            InterpretationItemCard(panchangItem)
        } else {
            TruthfulUnavailableCard("Panchang Alignment", "Panchang data not calculated for current coordinates.")
        }
    }
}

@Composable
private fun InterpretationItemCard(item: InterpretationItem) {
    var expandedEvidence by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .testTag("item_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.sanskritTitle,
                style = MaterialTheme.typography.labelSmall,
                color = AccentGold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.detailedDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            item.traditionalGuidance?.let { guide ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = guide,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Expandable Evidence Section
            item.evidence?.let { evidence ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expandedEvidence = !expandedEvidence }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (expandedEvidence) "Hide Calculation Evidence" else "Inspect Verifiable Evidence",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentGold
                    )
                    Icon(
                        imageVector = if (expandedEvidence) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = expandedEvidence) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DeepNavy.copy(alpha = 0.5f))
                            .padding(10.dp)
                            .testTag("evidence_box_${item.id}")
                    ) {
                        Text(
                            text = evidence.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        evidence.metrics.forEach { (k, v) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(k, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(v, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = evidence.astronomicalBasis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TruthfulUnavailableCard(title: String, message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PolarityBadge(polarity: InterpretationFactorPolarity) {
    val (bgColor, textColor, label) = when (polarity) {
        InterpretationFactorPolarity.SUPPORTIVE -> Triple(AccentEmerald.copy(alpha = 0.15f), AccentEmerald, "SUPPORTIVE")
        InterpretationFactorPolarity.CHALLENGING -> Triple(AccentAmber.copy(alpha = 0.15f), AccentAmber, "CHALLENGING")
        InterpretationFactorPolarity.NEUTRAL -> Triple(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), MaterialTheme.colorScheme.onSurfaceVariant, "BALANCED")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
