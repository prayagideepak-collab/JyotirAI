package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.Chart
import com.example.domain.models.PlanetPosition
import com.example.domain.models.Rashi
import com.example.ui.theme.*

@Composable
fun ChartInfoPanel(
    chart: Chart,
    onPlanetClick: (PlanetPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_info_panel"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Varga Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(BorderSubtle, BorderSubtle))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = chart.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                        Text(
                            text = chart.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Lagna (Ascendant)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = chart.ascendantSign,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (chart.ascendantDegreeInSign > 0) {
                            val degInt = chart.ascendantDegreeInSign.toInt()
                            val minInt = ((chart.ascendantDegreeInSign - degInt) * 60).toInt()
                            Text(
                                text = "$degInt° $minInt' in sign",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentGold
                            )
                        }
                    }

                    val moonPos = chart.positions.firstOrNull { it.planet.equals("Moon", ignoreCase = true) }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Chandra (Moon)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = moonPos?.sign ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (moonPos != null) {
                            Text(
                                text = "House ${moonPos.house} • ${moonPos.formattedDegree}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF81D4FA)
                            )
                        }
                    }
                }
            }
        }

        // 2. Planets Placement Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(BorderSubtle, BorderSubtle))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Planetary Placements (${chart.vargaType.code})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Tap any planet for comprehensive astrological properties",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                HorizontalDivider(color = BorderSubtle)

                for (planet in chart.positions) {
                    val planetColor = getPlanetColor(planet.planet)
                    val rashi = Rashi.fromIndex(planet.signIndex)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlanetClick(planet) }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .testTag("planet_row_${planet.planet.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Planet Badge + Name
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(planetColor.copy(alpha = 0.2f))
                                    .border(1.dp, planetColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = planet.abbreviation,
                                    color = planetColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = planet.planet,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (planet.isRetrograde && planet.planet != "Rahu" && planet.planet != "Ketu") {
                                        Text(
                                            text = " [R]",
                                            color = AccentAmber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Text(
                                    text = "${rashi.sanskritName} (${rashi.englishName})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Right: House + Degree + Nakshatra
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "House ${planet.house}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            )
                            Text(
                                text = "${planet.formattedDegree} • ${planet.nakshatra} (${planet.nakshatraPada})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
                }
            }
        }
    }
}
