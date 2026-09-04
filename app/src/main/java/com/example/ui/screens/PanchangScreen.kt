package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.models.*
import com.example.ui.viewmodel.AstrologyViewModel
import com.example.ui.viewmodel.PanchangUiState
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun PanchangScreen(viewModel: AstrologyViewModel) {
    val uiState by viewModel.panchangUiState.collectAsState()
    val dateTime by viewModel.panchangDateTime.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Date Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { viewModel.shiftPanchangDays(-1) }) { Text("-1D") }
            Text(
                text = dateTime?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "Loading...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = { viewModel.shiftPanchangDays(1) }) { Text("+1D") }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.resetPanchangToNow() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Today")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = uiState) {
            is PanchangUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PanchangUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            is PanchangUiState.Success -> {
                val panchang = state.snapshot
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Location: ${panchang.location.placeName}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("Panchang", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            PanchangRow("Vara (Weekday)", "${panchang.vara.sanskritName} (${panchang.vara.englishName})")
                            PanchangRow("Tithi", "${panchang.tithi.name} (${panchang.paksha}) - ${(panchang.tithi.remainingPercentage * 100).toInt()}% remaining")
                            PanchangRow("Nakshatra", "${panchang.nakshatra.nakshatra.sanskritName} (Pada ${panchang.nakshatra.pada})")
                            PanchangRow("Yoga", "${panchang.yoga.name} - ${(panchang.yoga.remainingPercentage * 100).toInt()}% remaining")
                            PanchangRow("Karana", "${panchang.karana.name} - ${(panchang.karana.remainingPercentage * 100).toInt()}% remaining")
                            
                            val lunar = panchang.lunarObservance
                            if (lunar != null && (lunar.isEkadashi || lunar.isPurnima || lunar.isAmavasya)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val obsName = when {
                                    lunar.isEkadashi -> "Ekadashi"
                                    lunar.isPurnima -> "Purnima"
                                    lunar.isAmavasya -> "Amavasya"
                                    else -> ""
                                }
                                Text("Lunar Observance: $obsName", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("Sun", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                            PanchangRow("Sunrise", panchang.sunrise?.format(timeFormatter) ?: "Unavailable")
                            PanchangRow("Sunset", panchang.sunset?.format(timeFormatter) ?: "Unavailable")
                            PanchangRow("Sun Sign", panchang.sunSign?.sanskritName ?: "Unavailable")
                            PanchangRow("Moon Sign", panchang.moonSign?.sanskritName ?: "Unavailable")
                            
                            panchang.muhurta?.let { muhurta ->
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Muhurta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                
                                muhurta.rahukaal?.let {
                                    PanchangRow("Rahukaal", "${it.start.format(timeFormatter)} - ${it.end.format(timeFormatter)}")
                                }
                                muhurta.brahmaMuhurta?.let {
                                    PanchangRow("Brahma Muhurta", "${it.start.format(timeFormatter)} - ${it.end.format(timeFormatter)}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PanchangRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(text = value, modifier = Modifier.weight(1f))
    }
}
