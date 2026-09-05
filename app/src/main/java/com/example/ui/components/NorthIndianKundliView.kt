package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.Chart
import com.example.domain.models.PlanetPosition
import com.example.domain.models.Rashi
import com.example.ui.theme.*

/**
 * High-contrast, Vedic-inspired color palette for planets in the chart.
 */
fun getPlanetColor(planet: String): Color = when (planet.lowercase().trim()) {
    "sun", "su" -> Color(0xFFFFB300) // Amber Gold
    "moon", "mo" -> Color(0xFF81D4FA) // Pearl Blue
    "mars", "ma" -> Color(0xFFFF5252) // Coral Red
    "mercury", "me" -> Color(0xFF69F0AE) // Emerald Green
    "jupiter", "ju" -> Color(0xFFFFD740) // Yellow Gold
    "venus", "ve" -> Color(0xFFF48FB1) // Rose Pink
    "saturn", "sa" -> Color(0xFF90CAF9) // Celestial Blue
    "rahu", "ra" -> Color(0xFFB388FF) // Purple
    "ketu", "ke" -> Color(0xFFFFAB91) // Smoke Orange
    "asc", "ascendant", "lagna" -> Color(0xFF80DEEA) // Cyan
    else -> Color(0xFFE0E0E0)
}

/**
 * Deterministic, responsive North Indian Diamond Vedic Kundli Chart Component.
 * Supports dense planetary clusters, large font scale, and adaptive layout.
 */
@Composable
fun NorthIndianKundliView(
    chart: Chart,
    onPlanetClick: (PlanetPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .aspectRatio(1.0f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F1424))
                .border(1.5.dp, AccentGold.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                .testTag("north_indian_kundli_chart")
        ) {
            val sizePx = maxWidth

            // 1. Draw classical Vedic geometric grid lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val strokeWidth = 1.5.dp.toPx()
                val gridColor = Color(0xFFD4AF37).copy(alpha = 0.45f) // Vedic Gold

                // Outer Boundary
                drawRect(
                    color = gridColor,
                    topLeft = Offset(0f, 0f),
                    size = size,
                    style = Stroke(width = strokeWidth)
                )

                // Main Diagonals: (0,0)->(w,h) and (w,0)->(0,h)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = gridColor,
                    start = Offset(w, 0f),
                    end = Offset(0f, h),
                    strokeWidth = strokeWidth
                )

                // Inner Diamond: midpoints of the 4 outer sides
                val diamondPath = Path().apply {
                    moveTo(w / 2f, 0f)
                    lineTo(w, h / 2f)
                    lineTo(w / 2f, h)
                    lineTo(0f, h / 2f)
                    close()
                }
                drawPath(
                    path = diamondPath,
                    color = gridColor,
                    style = Stroke(width = strokeWidth)
                )
            }

            // 2. Relative positioning coordinates for all 12 Houses
            val lagnaSign = chart.ascendantSignIndex

            val houseRelativePositions = listOf(
                HouseCoords(1, 0.50f, 0.22f, 0.50f, 0.38f), // H1 (Top Diamond)
                HouseCoords(2, 0.27f, 0.12f, 0.38f, 0.05f), // H2 (Top Left Triangle)
                HouseCoords(3, 0.12f, 0.27f, 0.05f, 0.38f), // H3 (Left Top Triangle)
                HouseCoords(4, 0.22f, 0.50f, 0.38f, 0.50f), // H4 (Left Diamond)
                HouseCoords(5, 0.12f, 0.73f, 0.05f, 0.62f), // H5 (Left Bottom Triangle)
                HouseCoords(6, 0.27f, 0.88f, 0.38f, 0.95f), // H6 (Bottom Left Triangle)
                HouseCoords(7, 0.50f, 0.78f, 0.50f, 0.62f), // H7 (Bottom Diamond)
                HouseCoords(8, 0.73f, 0.88f, 0.62f, 0.95f), // H8 (Bottom Right Triangle)
                HouseCoords(9, 0.88f, 0.73f, 0.95f, 0.62f), // H9 (Right Bottom Triangle)
                HouseCoords(10, 0.78f, 0.50f, 0.62f, 0.50f), // H10 (Right Diamond)
                HouseCoords(11, 0.88f, 0.27f, 0.95f, 0.38f), // H11 (Right Top Triangle)
                HouseCoords(12, 0.73f, 0.12f, 0.62f, 0.05f)  // H12 (Top Right Triangle)
            )

            // Render each house content (Sign Number + Occupying Planets)
            for (hc in houseRelativePositions) {
                val houseNumber = hc.houseNumber
                val signIndexForHouse = (lagnaSign + (houseNumber - 1)) % 12
                val planetsInHouse = chart.getPlanetsInHouse(houseNumber)

                // House Sign Number indicator (Vedic Rashi 1..12)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.TopStart)
                        .offset(
                            x = sizePx * hc.signX - 10.dp,
                            y = sizePx * hc.signY - 10.dp
                        )
                ) {
                    Text(
                        text = "${signIndexForHouse + 1}",
                        color = AccentGold.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Planets placed in this house
                val boxWidth = if (houseNumber in listOf(1, 4, 7, 10)) 88.dp else 76.dp
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.TopStart)
                        .offset(
                            x = sizePx * hc.contentX - (boxWidth / 2),
                            y = sizePx * hc.contentY - 24.dp
                        )
                        .width(boxWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (houseNumber == 1) {
                            // Ascendant indicator in House 1
                            val ascDeg = chart.ascendantDegreeInSign.toInt()
                            Text(
                                text = "Asc $ascDeg°",
                                color = Color(0xFF80DEEA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        // Cluster handling for planets in house
                        if (planetsInHouse.size > 2) {
                            // Multi-planet 2-column compact grid
                            val rows = planetsInHouse.chunked(2)
                            for (row in rows) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (planet in row) {
                                        PlanetBadge(planet = planet, isCompact = true, onClick = onPlanetClick)
                                    }
                                }
                            }
                        } else {
                            // 1-2 planets stacked vertically
                            for (planet in planetsInHouse) {
                                PlanetBadge(planet = planet, isCompact = false, onClick = onPlanetClick)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanetBadge(
    planet: PlanetPosition,
    isCompact: Boolean,
    onClick: (PlanetPosition) -> Unit
) {
    val planetColor = getPlanetColor(planet.planet)
    val retroText = if (planet.isRetrograde && planet.planet != "Rahu" && planet.planet != "Ketu") " [R]" else ""
    val label = "${planet.abbreviation}$retroText"
    val degreeText = "${planet.degreeInSign.toInt()}°"

    Box(
        modifier = Modifier
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(planetColor.copy(alpha = 0.2f))
            .clickable { onClick(planet) }
            .padding(horizontal = if (isCompact) 3.dp else 5.dp, vertical = 1.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${planet.planet}, ${planet.formattedDegree}, ${if (planet.isRetrograde) "retrograde" else "direct"}"
            }
            .testTag("planet_badge_${planet.planet.lowercase()}")
    ) {
        Text(
            text = "$label $degreeText",
            color = planetColor,
            fontSize = if (isCompact) 9.sp else 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
    }
}

private data class HouseCoords(
    val houseNumber: Int,
    val contentX: Float,
    val contentY: Float,
    val signX: Float,
    val signY: Float
)

