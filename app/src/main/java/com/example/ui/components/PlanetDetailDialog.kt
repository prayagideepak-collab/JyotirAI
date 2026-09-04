package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.models.PlanetPosition
import com.example.domain.models.Rashi
import com.example.ui.theme.*

fun getHouseSignification(houseNumber: Int): String = when (houseNumber) {
    1 -> "Tanu Bhava (Physical Body, Self, Vitality & Appearance)"
    2 -> "Dhana Bhava (Wealth, Family, Speech & Possessions)"
    3 -> "Sahaja Bhava (Courage, Siblings, Communication & Drive)"
    4 -> "Sukha Bhava (Mother, Home, Conveyances & Emotional Peace)"
    5 -> "Putra Bhava (Children, Intellect, Creativity & Purva Punya)"
    6 -> "Ari Bhava (Debts, Enemies, Health, Daily Service & Obstacles)"
    7 -> "Kalatra Bhava (Spouse, Marriage, Partnerships & Trade)"
    8 -> "Ayu Bhava (Longevity, Transformation, Occult & Hidden Truths)"
    9 -> "Bhagya Bhava (Dharma, Fortune, Higher Wisdom, Father & Gurus)"
    10 -> "Karma Bhava (Career, Social Status, Authority & Achievements)"
    11 -> "Labha Bhava (Gains, Fulfillment of Desires, Income & Network)"
    12 -> "Vyaya Bhava (Expenditure, Liberation/Moksha, Foreign Lands & Sleep)"
    else -> "House $houseNumber"
}

@Composable
fun PlanetDetailDialog(
    planet: PlanetPosition,
    chartTitle: String = "Natal Chart",
    onDismiss: () -> Unit
) {
    val planetColor = getPlanetColor(planet.planet)
    val rashi = Rashi.fromIndex(planet.signIndex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceElevated)
                .border(1.dp, planetColor.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                .testTag("planet_detail_dialog"),
            color = SurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with planet badge, name, and close icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            planetColor.copy(alpha = 0.35f),
                                            planetColor.copy(alpha = 0.08f)
                                        )
                                    )
                                )
                                .border(1.5.dp, planetColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = planet.abbreviation,
                                color = planetColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${planet.planet} (${planet.sanskritName})",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                text = "$chartTitle Placement",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("planet_detail_dialog_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(16.dp))

                // Motion & Dignity Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val motionText = if (planet.isRetrograde && planet.planet != "Rahu" && planet.planet != "Ketu") "Vakri (Retrograde)" else "Marga (Direct)"
                    val motionColor = if (planet.isRetrograde && planet.planet != "Rahu" && planet.planet != "Ketu") AccentAmber else Color(0xFF69F0AE)
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(motionColor.copy(alpha = 0.15f))
                            .border(1.dp, motionColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = motionText,
                            color = motionColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF202938))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Speed: ${"%.3f".format(planet.speed)}° / day",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detail Rows Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(BorderSubtle, BorderSubtle)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailItem(
                            label = "Zodiac Sign (Rashi)",
                            value = "${rashi.sanskritName} (${rashi.englishName}) • ${rashi.element} Element",
                            highlight = true
                        )
                        DetailItem(
                            label = "Sign Lord (Rashi Swami)",
                            value = rashi.lord
                        )
                        DetailItem(
                            label = "House Placement (Bhava)",
                            value = "House ${planet.house} — ${getHouseSignification(planet.house)}"
                        )
                        DetailItem(
                            label = "Degree in Sign",
                            value = planet.formattedDegree,
                            highlight = true
                        )
                        DetailItem(
                            label = "Total Nirayana Longitude",
                            value = "${"%.4f".format(planet.totalLongitude)}°"
                        )
                        DetailItem(
                            label = "Nakshatra (Lunar Mansion)",
                            value = "${planet.nakshatra} (Pada ${planet.nakshatraPada})"
                        )
                        DetailItem(
                            label = "Nakshatra Lord (Dasha Swami)",
                            value = planet.nakshatraLord
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("planet_detail_dialog_dismiss_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) AccentGold else MaterialTheme.colorScheme.onBackground
        )
    }
}
