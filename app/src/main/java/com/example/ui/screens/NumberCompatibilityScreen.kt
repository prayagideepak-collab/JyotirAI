package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.numerology.*
import com.example.domain.speech.JyotirAiSpeechManager
import com.example.ui.components.AstrologySpeakerButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.AstrologyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberCompatibilityScreen(
    viewModel: AstrologyViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val speechManager = remember { JyotirAiSpeechManager(context) }
    DisposableEffect(Unit) {
        onDispose { speechManager.release() }
    }

    val activeProfile by viewModel.activeUserProfile.collectAsStateWithLifecycle()
    val defaultProfile by viewModel.defaultUserProfile.collectAsStateWithLifecycle()
    val profileToUse = activeProfile ?: defaultProfile

    val compatibilityResult by viewModel.numberCompatibilityResult.collectAsStateWithLifecycle()
    val comparisonResult by viewModel.numberComparisonResult.collectAsStateWithLifecycle()

    var rawInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(NumberCategory.MOBILE) }
    var customLabel by remember { mutableStateOf("") }

    // Selected purposes state
    val selectedPurposes = remember { mutableStateOf(setOf(
        CompatibilityPurpose.PERSONAL,
        CompatibilityPurpose.FINANCIAL,
        CompatibilityPurpose.BUSINESS,
        CompatibilityPurpose.VEHICLE
    )) }

    var isComparisonMode by remember { mutableStateOf(false) }
    var rawInputB by remember { mutableStateOf("") }
    var selectedCategoryB by remember { mutableStateOf(NumberCategory.VEHICLE) }
    var customLabelB by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "संख्या अनुकूलता और प्रभाव",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Number Compatibility & Impact Analysis",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("comp_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Profile Warning if missing
            if (profileToUse == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "कृपया पहले 'My Numerology Profile' में जन्म विवरण सेट करें।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                return@Scaffold
            }

            // Input Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "नंबर और श्रेणी दर्ज करें",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentAmber
                        )
                        TextButton(
                            onClick = { isComparisonMode = !isComparisonMode },
                            modifier = Modifier.testTag("toggle_comparison_button")
                        ) {
                            Text(if (isComparisonMode) "सिंगल विश्लेषण" else "तुलना करें (Compare)")
                        }
                    }

                    // Category Selection Chips
                    Text("श्रेणी (Category):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(NumberCategory.values().toList()) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.hindiName, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("cat_chip_${cat.code.lowercase()}")
                            )
                        }
                    }

                    // Number Input A
                    OutlinedTextField(
                        value = rawInput,
                        onValueChange = { rawInput = it; errorMessage = null },
                        label = { Text(if (isComparisonMode) "पहला नंबर (Number A)" else "नंबर दर्ज करें (Enter Number)") },
                        placeholder = { Text("जैसे: 9876543210 या DL01AB1234") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("number_input_a")
                    )

                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        label = { Text("कस्टम लेबल (वैकल्पिक, जैसे: Personal Mobile)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_label_a")
                    )

                    // Number Input B (if comparison mode)
                    if (isComparisonMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = BorderSubtle)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("तुलना के लिए दूसरा नंबर:", style = MaterialTheme.typography.labelMedium, color = AccentAmber)

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(NumberCategory.values().toList()) { cat ->
                                FilterChip(
                                    selected = selectedCategoryB == cat,
                                    onClick = { selectedCategoryB = cat },
                                    label = { Text(cat.hindiName, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = rawInputB,
                            onValueChange = { rawInputB = it },
                            label = { Text("दूसरा नंबर (Number B)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("number_input_b")
                        )

                        OutlinedTextField(
                            value = customLabelB,
                            onValueChange = { customLabelB = it },
                            label = { Text("दूसरा लेबल (जैसे: Office Mobile)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_label_b")
                        )
                    }

                    // Purpose Selection
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("विश्लेषण के उद्देश्य (Select Purposes):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    val allPurposes = CompatibilityPurpose.values().toList()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        allPurposes.chunked(2).forEach { rowPurposes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPurposes.forEach { purpose ->
                                    val isSelected = selectedPurposes.value.contains(purpose)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val current = selectedPurposes.value.toMutableSet()
                                            if (isSelected) current.remove(purpose) else current.add(purpose)
                                            selectedPurposes.value = current
                                        },
                                        label = { Text(purpose.hindiName, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowPurposes.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (!NumberNormalizationUtils.isValidInput(rawInput)) {
                                errorMessage = "कृपया वैध नंबर दर्ज करें।"
                                return@Button
                            }
                            if (selectedPurposes.value.isEmpty()) {
                                errorMessage = "कृपया कम से कम एक उद्देश्य (Purpose) चुनें।"
                                return@Button
                            }

                            if (isComparisonMode) {
                                if (!NumberNormalizationUtils.isValidInput(rawInputB)) {
                                    errorMessage = "कृपया तुलना के लिए दूसरा वैध नंबर दर्ज करें।"
                                    return@Button
                                }
                                viewModel.compareNumbers(
                                    rawInput, selectedCategory, customLabel.ifBlank { null },
                                    rawInputB, selectedCategoryB, customLabelB.ifBlank { null },
                                    selectedPurposes.value.toList()
                                )
                            } else {
                                viewModel.analyzeNumberCompatibility(
                                    rawInput, selectedCategory, customLabel.ifBlank { null },
                                    selectedPurposes.value.toList()
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("analyze_number_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isComparisonMode) "नंबरों की तुलना करें" else "संख्या अनुकूलता विश्लेषण करें",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            // Results Display
            if (isComparisonMode && comparisonResult != null) {
                ComparisonResultView(comparisonResult!!, speechManager)
            } else if (!isComparisonMode && compatibilityResult != null) {
                SingleCompatibilityResultView(compatibilityResult!!, speechManager)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SingleCompatibilityResultView(
    result: NumberCompatibilityResult,
    speechManager: JyotirAiSpeechManager
) {
    val norm = result.normalizedNumber
    val status = result.overallStatus

    val statusColor = when (status) {
        CompatibilityStatus.POSITIVE -> Color(0xFF4CAF50)
        CompatibilityStatus.NEUTRAL -> Color(0xFF2196F3)
        CompatibilityStatus.MIXED -> Color(0xFFFF9800)
        CompatibilityStatus.CHALLENGING -> Color(0xFFE91E63)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .testTag("compatibility_result_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = norm.customLabel ?: norm.category.hindiName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "मास्क्ड नंबर: ${norm.privacyMasked} | मूल अंक: ${norm.reducedValue}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = status.hindiName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Divider(color = BorderSubtle)

            Text(
                text = result.summaryHindi,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "मार्गदर्शन: ${result.guidanceHindi}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "उद्देश्य-वार प्रभाव विश्लेषण (Purpose-wise Impact):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AccentAmber
            )

            // Purpose reports list
            result.purposeReports.values.forEach { report ->
                PurposeReportCard(report)
            }

            // TTS Speaker Button
            val speechText = "${result.summaryHindi}. ${result.guidanceHindi}"
            AstrologySpeakerButton(
                speechManager = speechManager,
                hindiTextProvider = { speechText }
            )
        }
    }
}

@Composable
fun PurposeReportCard(report: PurposeAnalysis) {
    var expanded by remember { mutableStateOf(false) }
    val sColor = when (report.status) {
        CompatibilityStatus.POSITIVE -> Color(0xFF4CAF50)
        CompatibilityStatus.NEUTRAL -> Color(0xFF2196F3)
        CompatibilityStatus.MIXED -> Color(0xFFFF9800)
        CompatibilityStatus.CHALLENGING -> Color(0xFFE91E63)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.titleHindi,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = sColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = report.status.hindiName.split(" ")[0],
                            style = MaterialTheme.typography.labelSmall,
                            color = sColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Divider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(4.dp))

                if (report.positiveTendencies.isNotEmpty()) {
                    Text("सकारात्मक प्रवृत्तियाँ:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                    report.positiveTendencies.forEach { pos ->
                        Text("• $pos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (report.challengingTendencies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("सावधानी एवं चुनौतियाँ:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentAmber)
                    report.challengingTendencies.forEach { chal ->
                        Text("• $chal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonResultView(
    comp: NumberComparisonResult,
    speechManager: JyotirAiSpeechManager
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .testTag("comparison_result_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "संख्या तुलनात्मक विश्लेषण (Number Comparison)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AccentAmber
            )

            Text(
                text = comp.comparisonSummaryHindi,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SingleCompatibilityResultView(comp.resultA, speechManager)
                }
                Box(modifier = Modifier.weight(1f)) {
                    SingleCompatibilityResultView(comp.resultB, speechManager)
                }
            }
        }
    }
}
