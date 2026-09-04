package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.BorderSubtle

@Composable
fun PlaceholderScreen(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(32.dp))
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PredictionsScreen() = PlaceholderScreen("Predictions", "Phase 7 & 8 feature. Requires deterministic calculation engine.")

@Composable
fun PanchangScreen() = PlaceholderScreen("Daily Panchang", "Phase 9 feature. Tithi, Yoga, Karana, and specific Muhurtas.")

@Composable
fun MuhurtaScreen() = PlaceholderScreen("Muhurta", "Phase 10 feature. Astrological time selection.")

@Composable
fun CompatibilityScreen() = PlaceholderScreen("Compatibility", "Phase 11 feature. Kundli matching and Guna Milan.")

@Composable
fun NumerologyScreen() = PlaceholderScreen("Numerology", "Phase 12 feature. Path and Destiny calculations.")

@Composable
fun SettingsScreen() = PlaceholderScreen("Settings", "App preferences and privacy controls.")
