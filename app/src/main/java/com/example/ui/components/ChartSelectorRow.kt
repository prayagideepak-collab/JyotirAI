package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.VargaType
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BorderSubtle

/**
 * Standard ordered list of all 16 Vedic Divisional Charts (Shodashavarga)
 * Prioritizing foundational charts (D1, D9, D10) followed by the classical progression.
 */
val ALL_AVAILABLE_VARGAS = listOf(
    VargaType.D1,
    VargaType.D9,
    VargaType.D10,
    VargaType.D2,
    VargaType.D3,
    VargaType.D4,
    VargaType.D7,
    VargaType.D12,
    VargaType.D16,
    VargaType.D20,
    VargaType.D24,
    VargaType.D27,
    VargaType.D30,
    VargaType.D40,
    VargaType.D45,
    VargaType.D60
)

/**
 * Meaningful, concise astrological subtitle for each Varga chip.
 */
fun getVargaSubtitle(varga: VargaType): String = when (varga) {
    VargaType.D1 -> "Physical / Natal"
    VargaType.D2 -> "Wealth / Hora"
    VargaType.D3 -> "Siblings / Courage"
    VargaType.D4 -> "Fortune / Assets"
    VargaType.D7 -> "Progeny / Children"
    VargaType.D9 -> "Soul / Marriage"
    VargaType.D10 -> "Career / Status"
    VargaType.D12 -> "Parents / Lineage"
    VargaType.D16 -> "Vehicles / Luxuries"
    VargaType.D20 -> "Spiritual / Upasana"
    VargaType.D24 -> "Knowledge / Learning"
    VargaType.D27 -> "Strength / Vitality"
    VargaType.D30 -> "Arishta / Obstacles"
    VargaType.D40 -> "Auspicious Fruits"
    VargaType.D45 -> "Moral Character"
    VargaType.D60 -> "Past Karma / Root"
}

@Composable
fun ChartSelectorRow(
    selectedVarga: VargaType,
    onSelectVarga: (VargaType) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Smoothly ensure the selected Varga is visible and centered on selection
    LaunchedEffect(selectedVarga) {
        val selectedIndex = ALL_AVAILABLE_VARGAS.indexOf(selectedVarga)
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_selector_row"),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(ALL_AVAILABLE_VARGAS, key = { _, varga -> varga.code }) { _, varga ->
            val isSelected = varga == selectedVarga
            val vargaSubtitle = getVargaSubtitle(varga)
            val accessibilityLabel = "${varga.code} ${varga.sanskritName}, ${varga.englishName}, $vargaSubtitle, ${if (isSelected) "selected" else "not selected"}"

            FilterChip(
                selected = isSelected,
                onClick = { onSelectVarga(varga) },
                label = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${varga.code} ${varga.sanskritName}",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = vargaSubtitle,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = BorderSubtle,
                    selectedBorderColor = AccentGold,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.5.dp
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        selected = isSelected
                        role = Role.Tab
                        contentDescription = accessibilityLabel
                    }
                    .testTag("varga_chip_${varga.code.lowercase()}")
            )
        }
    }
}

