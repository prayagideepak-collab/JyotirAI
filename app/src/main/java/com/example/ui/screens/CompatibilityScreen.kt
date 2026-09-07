package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.compatibility.CompatibilityMode
import com.example.ui.theme.*
import com.example.ui.viewmodel.AstrologyViewModel
import com.example.ui.viewmodel.CompatibilityUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatibilityScreen(
    viewModel: AstrologyViewModel,
    onNavigateToHome: () -> Unit = {}
) {
    val compatibilityState by viewModel.compatibilityUiState.collectAsStateWithLifecycle()
    val savedProfiles by viewModel.savedProfiles.collectAsStateWithLifecycle()
    val profileA by viewModel.selectedProfileA.collectAsStateWithLifecycle()
    val profileB by viewModel.selectedProfileB.collectAsStateWithLifecycle()
    val currentMode by viewModel.compatibilityMode.collectAsStateWithLifecycle()

    var showProfileADialog by remember { mutableStateOf(false) }
    var showProfileBDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "कुंडली मिलान (Compatibility)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Ashtakoota & Synergy Analysis (Phase 11)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateToHome,
                        modifier = Modifier.testTag("compatibility_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Mode Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Select Analysis Mode",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompatibilityMode.entries.forEach { mode ->
                            val selected = currentMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setCompatibilityMode(mode) },
                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.testTag("compatibility_mode_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Profile Selection Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile A
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showProfileADialog = true }
                        .testTag("select_profile_a_card"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AccentAmber)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Profile A", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = profileA?.birthData?.name ?: "Select Native 1",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Swap Button
                IconButton(
                    onClick = { viewModel.swapCompatibilityProfiles() },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .testTag("swap_profiles_button")
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Profiles", tint = AccentAmber)
                }

                // Profile B
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showProfileBDialog = true }
                        .testTag("select_profile_b_card"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AccentAmber)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Profile B", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = profileB?.birthData?.name ?: "Select Native 2",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Calculate Button
            Button(
                onClick = { viewModel.calculateCompatibility() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("calculate_compatibility_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Calculate Compatibility", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            // State Display
            when (val state = compatibilityState) {
                is CompatibilityUiState.SelectProfiles -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Please select two profiles above and tap Calculate Compatibility.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is CompatibilityUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentAmber)
                    }
                }
                is CompatibilityUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = state.message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is CompatibilityUiState.Success -> {
                    val res = state.result
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Compatibility Score",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = String.format("%.1f", res.score),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentAmber
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = res.explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            if (res.pros.isNotEmpty()) {
                                Text(text = "Pros & Strengths:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                res.pros.forEach { pro ->
                                    Text(text = "• $pro", style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            if (res.cons.isNotEmpty()) {
                                Text(text = "Caution Factors:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                res.cons.forEach { con ->
                                    Text(text = "• $con", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for Profile A
    if (showProfileADialog) {
        AlertDialog(
            onDismissRequest = { showProfileADialog = false },
            title = { Text("Select Profile A") },
            text = {
                Column {
                    savedProfiles.forEach { p ->
                        TextButton(
                            onClick = {
                                viewModel.selectCompatibilityProfileA(p)
                                showProfileADialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(p.birthData.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileADialog = false }) { Text("Close") }
            }
        )
    }

    // Dialog for Profile B
    if (showProfileBDialog) {
        AlertDialog(
            onDismissRequest = { showProfileBDialog = false },
            title = { Text("Select Profile B") },
            text = {
                Column {
                    savedProfiles.forEach { p ->
                        TextButton(
                            onClick = {
                                viewModel.selectCompatibilityProfileB(p)
                                showProfileBDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(p.birthData.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileBDialog = false }) { Text("Close") }
            }
        )
    }
}
