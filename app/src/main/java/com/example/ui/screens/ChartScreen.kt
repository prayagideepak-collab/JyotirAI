package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.models.VargaType
import com.example.ui.components.ChartInfoPanel
import com.example.ui.components.ChartSelectorRow
import com.example.ui.components.NorthIndianKundliView
import com.example.ui.components.PlanetDetailDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AstrologyUiState
import com.example.ui.viewmodel.AstrologyViewModel

@Composable
fun ChartScreen(
    viewModel: AstrologyViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedVarga by viewModel.selectedVargaType.collectAsStateWithLifecycle()
    val currentChart by viewModel.currentChart.collectAsStateWithLifecycle()
    val selectedPlanetDetail by viewModel.selectedPlanetDetail.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("chart_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Kundli & Divisional Charts",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        Text(
            text = "Deterministic Vedic Vargas (D1 Rashi, D9 Navamsha, D10 Dashamsha & more)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when (val state = uiState) {
            is AstrologyUiState.Success -> {
                val profile = state.profile
                val activeChart = currentChart ?: profile.rashiChart

                // 1. Varga Selector
                ChartSelectorRow(
                    selectedVarga = selectedVarga,
                    onSelectVarga = { viewModel.selectVarga(it) },
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 2. North Indian Kundli Visual Representation
                NorthIndianKundliView(
                    chart = activeChart,
                    onPlanetClick = { viewModel.selectPlanetDetail(it) },
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // 3. Information Panel with Placements & Significations
                ChartInfoPanel(
                    chart = activeChart,
                    onPlanetClick = { viewModel.selectPlanetDetail(it) },
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // 4. Interactive Planet Detail Dialog
                selectedPlanetDetail?.let { planet ->
                    PlanetDetailDialog(
                        planet = planet,
                        chartTitle = activeChart.title,
                        onDismiss = { viewModel.selectPlanetDetail(null) }
                    )
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
                        .clip(RoundedCornerShape(28.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(28.dp))
                        .padding(32.dp)
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
                            text = "Configure birth details on the Home tab, or load a sample reference profile to view Rashi (D1), Navamsha (D9), and Dashamsha (D10) charts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.loadReferenceProfile() },
                            modifier = Modifier.testTag("chart_screen_load_sample_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Load Sample Profile")
                        }
                    }
                }
            }
        }
    }
}
