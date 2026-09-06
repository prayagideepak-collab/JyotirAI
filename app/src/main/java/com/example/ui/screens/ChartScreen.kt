package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.models.VargaType
import com.example.domain.pdf.KundliPdfExporter
import com.example.domain.speech.AstrologyHindiSpeechFormatter
import com.example.domain.speech.JyotirAiSpeechManager
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AstrologyUiState
import com.example.ui.viewmodel.AstrologyViewModel

@Composable
fun ChartScreen(
    viewModel: AstrologyViewModel
) {
    val context = LocalContext.current
    val speechManager = remember { JyotirAiSpeechManager(context) }
    DisposableEffect(Unit) {
        onDispose {
            speechManager.release()
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedVarga by viewModel.selectedVargaType.collectAsStateWithLifecycle()
    val currentChart by viewModel.currentChart.collectAsStateWithLifecycle()
    val selectedPlanetDetail by viewModel.selectedPlanetDetail.collectAsStateWithLifecycle()
    val dashaTimeline by viewModel.dashaTimeline.collectAsStateWithLifecycle()
    val yogaDoshaState by viewModel.yogaDoshaState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("chart_screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Kundli & Divisional Charts",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Deterministic Vedic Vargas (D1 Rashi, D9 Navamsha, D10 Dashamsha & more)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when (val state = uiState) {
                is AstrologyUiState.Success -> {
                    val profile = state.profile
                    val isD1 = selectedVarga == VargaType.D1
                    val isCalculatedVarga = selectedVarga.isCalculated
                    val activeChart = if (isD1) {
                        currentChart ?: profile.rashiChart
                    } else if (isCalculatedVarga && currentChart?.type == selectedVarga.code) {
                        currentChart
                    } else {
                        null
                    }

                    // 1. Kundli Action Bar: PDF Export & Hindi Audio
                    KundliActionBar(
                        onExportPdf = {
                            try {
                                val chartToExport = activeChart ?: profile.rashiChart
                                val pdfFile = KundliPdfExporter.generateKundliPdf(
                                    context = context,
                                    profile = profile,
                                    activeChart = chartToExport,
                                    dashaTimeline = dashaTimeline
                                )
                                val shareIntent = KundliPdfExporter.createSharePdfIntent(context, pdfFile)
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share or Save Kundli PDF"))
                                Toast.makeText(context, "Kundli PDF generated with JyotirAI watermark", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to export PDF: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        speechManager = speechManager,
                        hindiTextProvider = {
                            val chartForSpeech = activeChart ?: profile.rashiChart
                            AstrologyHindiSpeechFormatter.formatKundliSummary(profile, chartForSpeech)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Horizontal Varga Selector
                    ChartSelectorRow(
                        selectedVarga = selectedVarga,
                        onSelectVarga = { viewModel.selectVarga(it) },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (!isCalculatedVarga) {
                        // Truthful unavailable state - NO fabricated fallback to D1
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                                .padding(24.dp)
                                .testTag("varga_unavailable_state"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${selectedVarga.code} ${selectedVarga.sanskritName} (${selectedVarga.englishName})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "This divisional chart is currently not supported for calculation in this release. JyotirAI strictly adheres to astronomical accuracy and never fabricates uncalculated planetary placements.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else if (activeChart == null) {
                        // Loading state while divisional chart calculates
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AccentGold)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Calculating ${selectedVarga.code} ${selectedVarga.sanskritName}...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // 3. North Indian Kundli Visual Sacred Geometry
                        NorthIndianKundliView(
                            chart = activeChart,
                            onPlanetClick = { viewModel.selectPlanetDetail(it) },
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        // 4. Detailed Information Panel with Placements & Significations
                        ChartInfoPanel(
                            chart = activeChart,
                            onPlanetClick = { viewModel.selectPlanetDetail(it) },
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        // 5. Vedic Yoga & Dosha Analysis (Phase 6)
                        YogaDoshaView(
                            state = yogaDoshaState,
                            speechManager = speechManager,
                            onRefresh = { (uiState as? AstrologyUiState.Success)?.profile?.let { viewModel.loadYogaDoshaAnalysis(it) } },
                            modifier = Modifier.padding(bottom = 32.dp)
                        )

                        // 6. Interactive Modal Planet Detail Dialog
                        selectedPlanetDetail?.let { planet ->
                            PlanetDetailDialog(
                                planet = planet,
                                chartTitle = activeChart.title,
                                onDismiss = { viewModel.selectPlanetDetail(null) }
                            )
                        }
                    }
                }
                is AstrologyUiState.Calculating -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentGold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Calculating high-precision Swiss Ephemeris chart...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                            .padding(28.dp)
                            .testTag("chart_empty_state")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Chart Calculated",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Configure birth details on the Home tab, or load a reference profile to inspect Rashi (D1), Navamsha (D9), Dashamsha (D10), and all 16 Shodashavarga charts.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { viewModel.loadReferenceProfile() },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("chart_screen_load_sample_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Load Sample Profile", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun KundliActionBar(
    onExportPdf: () -> Unit,
    speechManager: JyotirAiSpeechManager,
    hindiTextProvider: () -> String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PDF Export Action
            Button(
                onClick = onExportPdf,
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .testTag("export_kundli_pdf_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentAmber,
                    contentColor = DeepNavy
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Export PDF",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Hindi Audio Speaker Control
            AstrologySpeakerButton(
                speechManager = speechManager,
                hindiTextProvider = hindiTextProvider,
                buttonStyle = SpeakerButtonStyle.FILLED_CHIP,
                testTag = "kundli_listen_hindi_button"
            )
        }
    }
}


