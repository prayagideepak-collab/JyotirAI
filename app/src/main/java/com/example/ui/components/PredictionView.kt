package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.*
import com.example.ui.viewmodel.PredictionUiState

/**
 * Phase 7 Deterministic Vedic Prediction Engine UI Component.
 * Presents multi-factor Parashari life-topic predictions with full evidence chains and no false certainty.
 */
@Composable
fun PredictionView(
    predictionState: PredictionUiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("prediction_view_container")
    ) {
        when (predictionState) {
            is PredictionUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.AutoGraph,
                            contentDescription = "Prediction Engine",
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "वैदिक भविष्यफल एवं जीवन क्षेत्र विश्लेषण",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "कुण्डली की गणना के उपरांत पराशर सिद्धांतों पर आधारित बहु-कारकीय फलकथन यहाँ प्रदर्शित होगा।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is PredictionUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.testTag("prediction_loading_indicator"),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "भाव, दशा, गोचर एवं योग-दोष का समग्र विश्लेषण हो रहा है...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is PredictionUiState.InsufficientData -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Insufficient Data",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "अपूर्ण ज्योतिषीय डेटा",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = predictionState.reason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            is PredictionUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "त्रुटि: ${predictionState.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.testTag("prediction_retry_button")
                        ) {
                            Text("पुनः प्रयास करें")
                        }
                    }
                }
            }

            is PredictionUiState.Success -> {
                PredictionContent(snapshot = predictionState.snapshot)
            }
        }
    }
}

@Composable
private fun PredictionContent(snapshot: PredictionSnapshot) {
    var selectedTopicCode by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("prediction_list_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Header Card
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prediction_summary_card"),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "समग्र जीवन समय चक्र (Overview)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        SupportLevelBadge(supportLevel = snapshot.overallLifeTrend)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "दशा: ${snapshot.timeContext.activeMahadasha} महादशा / ${snapshot.timeContext.activeAntardasha} अंतर्दशा",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = snapshot.keyHighlightSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "जीवन क्षेत्र विश्लेषण (Life Topics)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Topic Cards
        items(snapshot.topicPredictions.values.toList(), key = { it.topic.code }) { topicPrediction ->
            LifeTopicCard(
                prediction = topicPrediction,
                isExpanded = selectedTopicCode == topicPrediction.topic.code,
                onToggleExpand = {
                    selectedTopicCode = if (selectedTopicCode == topicPrediction.topic.code) null else topicPrediction.topic.code
                }
            )
        }

        // Vedic Methodology Footer
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Methodology",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "फलकथन विशुद्ध पराशरीय गणना (भाव, दशा एवं गोचर संरेखण) पर आधारित है। कोई काल्पनिक निश्चितता नहीं दी जाती।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LifeTopicCard(
    prediction: LifeTopicPrediction,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("life_topic_card_${prediction.topic.code}")
            .clickable { onToggleExpand() },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Topic name & badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = prediction.topic.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = prediction.topic.hindiName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SupportLevelBadge(supportLevel = prediction.supportLevel)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Trend & Houses Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrendBadge(trendType = prediction.trendType)
                Text(
                    text = "भाव: ${prediction.primaryHousesInvolved.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Synthesis Summary
            Text(
                text = prediction.classicalSynthesis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Expanded Detailed Astrological Evidence
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "ज्योतिषीय कारक एवं साक्ष्य (Astrological Evidence)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dasha Timing
                    EvidenceSection(
                        title = "दशा संरेखण (Dasha Timing)",
                        content = prediction.dashaTimelineFactors,
                        icon = Icons.Outlined.Schedule
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Transit Influence
                    EvidenceSection(
                        title = "गोचर प्रभाव (Transits)",
                        content = prediction.transitFactors,
                        icon = Icons.Outlined.Navigation
                    )

                    // Supporting Factors
                    if (prediction.supportingFactors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "अनुकूल कारक (Supporting Factors):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        prediction.supportingFactors.forEach { factor ->
                            Text(
                                text = "• $factor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }

                    // Caution Factors
                    if (prediction.cautionFactors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "सावधानी कारक (Caution Factors):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        prediction.cautionFactors.forEach { factor ->
                            Text(
                                text = "• $factor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }

                    // Relevant Yogas & Doshas
                    if (prediction.relevantYogas.isNotEmpty() || prediction.relevantDoshas.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "संबंधित योग एवं दोष:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        prediction.relevantYogas.forEach { yoga ->
                            Text(
                                text = "✦ योग: $yoga",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                        prediction.relevantDoshas.forEach { dosha ->
                            Text(
                                text = "▲ दोष: $dosha",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "विस्तृत साक्ष्य संकुचित करें ▲" else "विस्तृत साक्ष्य देखें ▼",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EvidenceSection(
    title: String,
    content: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, top = 2.dp)
        )
    }
}

@Composable
private fun SupportLevelBadge(supportLevel: PredictionSupportLevel) {
    val (containerColor, contentColor) = when (supportLevel) {
        PredictionSupportLevel.STRONGLY_SUPPORTED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        PredictionSupportLevel.SUPPORTED -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        PredictionSupportLevel.MIXED_SIGNALS -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        PredictionSupportLevel.CHALLENGING -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        PredictionSupportLevel.LIMITED_DATA, PredictionSupportLevel.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = supportLevel.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
private fun TrendTypeBadge(trendType: PredictionTrendType) {
    TrendBadge(trendType = trendType)
}

@Composable
private fun TrendBadge(trendType: PredictionTrendType) {
    val (icon, tint) = when (trendType) {
        PredictionTrendType.POSITIVE_GROWTH -> Icons.Default.TrendingUp to MaterialTheme.colorScheme.primary
        PredictionTrendType.STABILITY -> Icons.Default.HorizontalRule to MaterialTheme.colorScheme.secondary
        PredictionTrendType.RESTRUCTURING -> Icons.Default.Sync to MaterialTheme.colorScheme.tertiary
        PredictionTrendType.CAUTION -> Icons.Default.Warning to MaterialTheme.colorScheme.error
        PredictionTrendType.TRANSFORMATIVE -> Icons.Default.ChangeCircle to MaterialTheme.colorScheme.primary
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = trendType.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}
