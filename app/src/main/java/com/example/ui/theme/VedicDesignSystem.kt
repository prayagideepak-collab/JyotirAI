package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * JyotirAI Vedic Design System tokens and reusable layout components.
 * Embodies a premium, modern, calm, and trustworthy celestial aesthetic.
 */
object VedicSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object VedicRadius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 18.dp
    val xl = 24.dp
}

/**
 * Premium Cosmic Card container with subtle gold or subtle white border and elevated dark background.
 */
@Composable
fun CosmicCard(
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceCard,
    borderColor: Color = BorderGold,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VedicRadius.lg))
            .border(1.dp, borderColor, RoundedCornerShape(VedicRadius.lg)),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(VedicSpacing.lg),
            content = content
        )
    }
}

/**
 * Astrological Badge for Planets, Houses, Nakshatras, and Zodiac signs.
 */
@Composable
fun AstrologicalBadge(
    text: String,
    modifier: Modifier = Modifier,
    badgeColor: Color = AccentAmber,
    containerColor: Color = SurfaceElevated
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(VedicRadius.sm)),
        color = containerColor,
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = badgeColor,
            modifier = Modifier.padding(horizontal = VedicSpacing.sm, vertical = 4.dp)
        )
    }
}

/**
 * Cosmic Section Header with icon, title, and subtitle.
 */
@Composable
fun CosmicHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VedicSpacing.md)
        ) {
            if (icon != null) {
                Surface(
                    shape = RoundedCornerShape(VedicRadius.sm),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, BorderGold)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = TextPrimary
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Elegant Gold Divider for separating astronomical data attributes.
 */
@Composable
fun GoldDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier.padding(vertical = VedicSpacing.md),
        thickness = 1.dp,
        color = BorderGold.copy(alpha = 0.4f)
    )
}

/**
 * Key-Value Planetary or Astrological Attribute Row.
 */
@Composable
fun PlanetaryAttributeRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = VedicSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
    }
}
