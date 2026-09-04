package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.domain.engine.TransitCalculator
import com.example.domain.models.TransitPosition
import com.example.domain.models.TransitSnapshot
import com.example.ui.components.TransitDateTimePickerDialog
import com.example.ui.components.TransitDetailDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AstrologyViewModel
import com.example.ui.viewmodel.TransitUiState
import java.time.format.DateTimeFormatter

@Composable
fun TransitScreen(
    viewModel: AstrologyViewModel,
    onNavigateToHome: () -> Unit = {}
) {
    val transitUiState by viewModel.transitUiState.collectAsState()
    val transitDateTime by viewModel.transitDateTime.collectAsState()
    val selectedTransitPlanet by viewModel.selectedTransitPlanet.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("transit_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Planetary Transits (Gochar)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Nirayana Sidereal Ephemeris • Lahiri Ayanamsa",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onNavigateToHome,
                modifier = Modifier.testTag("transit_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Navigate Home",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Date & Time Navigator Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transit_date_selector")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Target Moment",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = transitDateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Zone: ${transitDateTime.zone.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentAmber
                        )
                    }

                    FilledTonalButton(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("transit_pick_datetime_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick Date & Time",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change", style = MaterialTheme.typography.labelMedium)
                    }
                }

                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                // Quick Navigation Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.shiftTransitMonths(-1) },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("transit_prev_month_button")
                    ) {
                        Text("-1M", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { viewModel.shiftTransitDays(-1) },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("transit_prev_day_button")
                    ) {
                        Text("-1D", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = { viewModel.resetTransitToNow() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("transit_now_button")
                    ) {
                        Text("Now", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { viewModel.shiftTransitDays(1) },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("transit_next_day_button")
                    ) {
                        Text("+1D", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { viewModel.shiftTransitMonths(1) },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("transit_next_month_button")
                    ) {
                        Text("+1M", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Content Area based on TransitUiState
        when (val state = transitUiState) {
            is TransitUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = AccentAmber)
                        Text(
                            text = "Computing Sidereal Gochar...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is TransitUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Transit Calculation Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.loadTransits() },
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Retry Calculation")
                        }
                    }
                }
            }

            is TransitUiState.Success -> {
                val snapshot = state.snapshot

                // Natal Reference Status Banner
                if (snapshot.natalReference != null) {
                    val natal = snapshot.natalReference
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("natal_reference_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Active Natal Reference: ${natal.nativeName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentAmber
                                )
                                Text(
                                    text = "Vedic Gochar Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentEmerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Janma Rashi (Moon): ${natal.moonSign}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Lagna (Ascendant): ${natal.lagnaSign}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("natal_reference_banner_empty")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "No Natal Reference Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Transits are computed in the sidereal Nirayana zodiac. Enter birth details or load sample data on Home to evaluate Gochar from your natal Moon and Lagna.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { viewModel.loadReferenceProfile() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .testTag("transit_load_sample_profile_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Load Sample Profile (New Delhi)", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Planetary Transits Table
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transit_table")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sidereal Planetary Positions (Navagraha)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap any planet to inspect exact coordinates & Gochar relationships",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Scrollable table container
                        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            Column(modifier = Modifier.width(620.dp)) {
                                // Table Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Planet",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(110.dp)
                                    )
                                    Text(
                                        text = "Transit Sign",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(140.dp)
                                    )
                                    Text(
                                        text = "Degree",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(100.dp)
                                    )
                                    Text(
                                        text = "Motion",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Text(
                                        text = "From Moon",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(95.dp)
                                    )
                                    Text(
                                        text = "From Lagna",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(95.dp)
                                    )
                                }

                                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                                // Rows
                                snapshot.positions.forEach { planet ->
                                    TransitPlanetRow(
                                        position = planet,
                                        onClick = { viewModel.selectTransitPlanet(planet) }
                                    )
                                    HorizontalDivider(
                                        color = BorderSubtle.copy(alpha = 0.5f),
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }
                }

                // Astrological Methodological Disclaimer Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Authoritative Vedic Calculation Note",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "All Gochar coordinates are calculated using the high-precision Swiss Ephemeris under Lahiri (Chitra Paksha) ayanamsa. House relationships follow traditional Vedic whole-sign principles. Transits represent traditional astronomical alignments rather than empirical life certainties.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Active Transit Detail Dialog
                selectedTransitPlanet?.let { selected ->
                    TransitDetailDialog(
                        position = selected,
                        snapshot = snapshot,
                        onDismiss = { viewModel.selectTransitPlanet(null) }
                    )
                }
            }
        }
    }

    // Date & Time Picker Dialog
    if (showDatePicker) {
        TransitDateTimePickerDialog(
            initialDateTime = transitDateTime,
            onDismiss = { showDatePicker = false },
            onConfirm = { newDateTime ->
                showDatePicker = false
                viewModel.setTransitDateTime(newDateTime)
            }
        )
    }
}

@Composable
private fun TransitPlanetRow(
    position: TransitPosition,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .testTag("transit_row_${position.planet}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Planet Name & Sanskrit
        Column(modifier = Modifier.width(110.dp)) {
            Text(
                text = position.planet,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = position.sanskritName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Transit Sign
        Text(
            text = position.sign,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(140.dp)
        )

        // Degree
        Text(
            text = position.formattedDegree,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = AccentAmber,
            modifier = Modifier.width(100.dp)
        )

        // Motion Status (Retrograde / Direct)
        Box(modifier = Modifier.width(80.dp)) {
            Surface(
                color = if (position.isRetrograde) AccentCrimson.copy(alpha = 0.15f) else AccentEmerald.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (position.isRetrograde) "R" else "Dir",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (position.isRetrograde) AccentCrimson else AccentEmerald,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // From Natal Moon
        Text(
            text = position.houseFromMoon?.let {
                TransitCalculator.getHouseOrdinal(it)
            } ?: "—",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (position.houseFromMoon != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(95.dp)
        )

        // From Natal Lagna
        Text(
            text = position.houseFromLagna?.let {
                TransitCalculator.getHouseOrdinal(it)
            } ?: "—",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (position.houseFromLagna != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(95.dp)
        )
    }
}
