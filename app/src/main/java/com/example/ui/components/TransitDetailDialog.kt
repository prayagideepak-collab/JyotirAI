package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.domain.engine.TransitCalculator
import com.example.domain.models.TransitPosition
import com.example.domain.models.TransitSnapshot
import com.example.ui.theme.*

@Composable
fun TransitDetailDialog(
    position: TransitPosition,
    snapshot: TransitSnapshot,
    onDismiss: () -> Unit
) {
    val relation = TransitCalculator.buildTransitRelation(position)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                .testTag("transit_detail_dialog"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = position.planet,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                color = if (position.isRetrograde) AccentCrimson.copy(alpha = 0.2f) else AccentEmerald.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (position.isRetrograde) AccentCrimson else AccentEmerald
                                )
                            ) {
                                Text(
                                    text = if (position.isRetrograde) "RETROGRADE (Vakri)" else "DIRECT (Marga)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (position.isRetrograde) AccentCrimson else AccentEmerald,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "${position.sanskritName} • Sidereal Gochar",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("transit_detail_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Transit Details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                // Transit Position Data Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Transit Coordinates",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = AccentAmber
                        )

                        DetailItem(label = "Transit Sign (Rashi)", value = position.sign)
                        DetailItem(label = "Sign Longitude", value = position.formattedDegree)
                        DetailItem(
                            label = "Total Sidereal Longitude",
                            value = "%.4f°".format(position.totalLongitude)
                        )
                        DetailItem(
                            label = "Lunar Mansion (Nakshatra)",
                            value = "${position.nakshatra} (Pada ${position.nakshatraPada}, Lord: ${position.nakshatraLord})"
                        )
                        DetailItem(
                            label = "Daily Sidereal Motion",
                            value = "%.4f° / day".format(position.speed)
                        )
                        DetailItem(
                            label = "Snapshot Moment",
                            value = snapshot.formattedDateTime
                        )
                    }
                }

                // Natal Reference Comparison (Gochar)
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Vedic Gochar References",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = AccentAmber
                        )

                        if (snapshot.natalReference != null) {
                            val natal = snapshot.natalReference
                            DetailItem(
                                label = "From Natal Moon (${natal.moonSign})",
                                value = relation.moonRelationDescription ?: "N/A"
                            )
                            DetailItem(
                                label = "From Natal Lagna (${natal.lagnaSign})",
                                value = relation.lagnaRelationDescription ?: "N/A"
                            )
                            DetailItem(
                                label = "Natal Reference Native",
                                value = natal.nativeName
                            )
                        } else {
                            Text(
                                text = "No natal chart is currently active. Enter birth details on the Home screen to view relative Gochar positions from your natal Moon and Lagna.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Methodology & Boundary Notice
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Calculated deterministically using Swiss Ephemeris under Nirayana Sidereal zodiac (Lahiri / Chitra Paksha Ayanamsa) with Whole Sign houses. This presents traditional Vedic Gochar positioning rather than empirical predictions.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("transit_detail_dismiss_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Dismiss", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
