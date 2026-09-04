package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.VargaType
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BorderSubtle

@Composable
fun ChartSelectorRow(
    selectedVarga: VargaType,
    onSelectVarga: (VargaType) -> Unit,
    modifier: Modifier = Modifier
) {
    // Primary core vargas first, followed by other Shodashavargas
    val availableVargas = listOf(
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
        VargaType.D60
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (varga in availableVargas) {
            val isSelected = varga == selectedVarga
            FilterChip(
                selected = isSelected,
                onClick = { onSelectVarga(varga) },
                label = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "${varga.code} ${varga.sanskritName}",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Text(
                            text = when (varga) {
                                VargaType.D1 -> "Physical / Natal"
                                VargaType.D9 -> "Soul / Marriage"
                                VargaType.D10 -> "Career / Status"
                                VargaType.D2 -> "Wealth"
                                VargaType.D3 -> "Courage"
                                VargaType.D4 -> "Assets"
                                VargaType.D7 -> "Progeny"
                                VargaType.D12 -> "Parents"
                                else -> "Divisional"
                            },
                            fontSize = 10.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = BorderSubtle,
                    selectedBorderColor = AccentGold
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("varga_chip_${varga.code.lowercase()}")
            )
        }
    }
}
