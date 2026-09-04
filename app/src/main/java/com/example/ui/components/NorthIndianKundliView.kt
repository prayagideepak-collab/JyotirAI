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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.Chart
import com.example.domain.models.PlanetPosition
import com.example.domain.models.Rashi
import com.example.ui.theme.*

/**
 * Color palette for planets in the Vedic chart
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
 * High-precision, responsive North Indian Diamond Vedic Kundli Chart Component.
 */
@Composable
fun NorthIndianKundliView(
    chart: Chart,
    onPlanetClick: (PlanetPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.0f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0F1424))
            .border(1.5.dp, AccentGold.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .testTag("north_indian_kundli_chart")
    ) {
        val sizePx = maxWidth

        // 1. Draw classical geometric grid lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeWidth = 2.dp.toPx()
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
        // Lagna sign index determines the sign in House 1, and consecutive signs flow 1..12
        val lagnaSign = chart.ascendantSignIndex

        val houseRelativePositions = listOf(
            HouseCoords(1, 0.50f, 0.23f, 0.50f, 0.40f), // H1 (Top Diamond)
            HouseCoords(2, 0.28f, 0.12f, 0.40f, 0.05f), // H2 (Top Left Triangle)
            HouseCoords(3, 0.12f, 0.28f, 0.05f, 0.40f), // H3 (Left Top Triangle)
            HouseCoords(4, 0.23f, 0.50f, 0.40f, 0.50f), // H4 (Left Diamond)
            HouseCoords(5, 0.12f, 0.72f, 0.05f, 0.60f), // H5 (Left Bottom Triangle)
            HouseCoords(6, 0.28f, 0.88f, 0.40f, 0.95f), // H6 (Bottom Left Triangle)
            HouseCoords(7, 0.50f, 0.77f, 0.50f, 0.60f), // H7 (Bottom Diamond)
            HouseCoords(8, 0.72f, 0.88f, 0.60f, 0.95f), // H8 (Bottom Right Triangle)
            HouseCoords(9, 0.88f, 0.72f, 0.95f, 0.60f), // H9 (Right Bottom Triangle)
            HouseCoords(10, 0.77f, 0.50f, 0.60f, 0.50f), // H10 (Right Diamond)
            HouseCoords(11, 0.88f, 0.28f, 0.95f, 0.40f), // H11 (Right Top Triangle)
            HouseCoords(12, 0.72f, 0.12f, 0.60f, 0.05f)  // H12 (Top Right Triangle)
        )

        // Render each house content (Sign Number + Occupying Planets)
        for (hc in houseRelativePositions) {
            val houseNumber = hc.houseNumber
            val signIndexForHouse = (lagnaSign + (houseNumber - 1)) % 12
            val rashi = Rashi.fromIndex(signIndexForHouse)
            val planetsInHouse = chart.getPlanetsInHouse(houseNumber)

            // House Sign Number indicator
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
                    color = AccentGold.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Planets placed in this house
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.TopStart)
                    .offset(
                        x = sizePx * hc.contentX - 36.dp,
                        y = sizePx * hc.contentY - 24.dp
                    )
                    .width(72.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (houseNumber == 1) {
                        // Ascendant tag in House 1
                        Text(
                            text = "Asc ${chart.ascendantDegreeInSign.toInt()}°",
                            color = Color(0xFF80DEEA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    for (planet in planetsInHouse) {
                        val planetColor = getPlanetColor(planet.planet)
                        val retroText = if (planet.isRetrograde && planet.planet != "Rahu" && planet.planet != "Ketu") " [R]" else ""
                        val label = "${planet.abbreviation}$retroText"

                        Box(
                            modifier = Modifier
                                .padding(vertical = 1.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(planetColor.copy(alpha = 0.18f))
                                .clickable { onPlanetClick(planet) }
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                .testTag("planet_badge_${planet.planet.lowercase()}")
                        ) {
                            Text(
                                text = "$label ${planet.degreeInSign.toInt()}°",
                                color = planetColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class HouseCoords(
    val houseNumber: Int,
    val contentX: Float,
    val contentY: Float,
    val signX: Float,
    val signY: Float
)
