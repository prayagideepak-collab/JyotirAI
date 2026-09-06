package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.domain.models.BirthData
import com.example.domain.models.BirthLocation
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.SurfaceCard
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthDataEntryDialog(
    initialData: BirthData? = null,
    onDismiss: () -> Unit,
    onSubmit: (BirthData) -> Unit,
    onResolveLocation: suspend (String) -> Result<List<BirthLocation>>
) {
    var name by remember { mutableStateOf(initialData?.name ?: "User") }
    var gender by remember { mutableStateOf(initialData?.gender ?: "पुरुष") }
    var year by remember { mutableStateOf(initialData?.date?.year?.toString() ?: "1995") }
    var month by remember { mutableStateOf(initialData?.date?.monthValue?.toString() ?: "8") }
    var day by remember { mutableStateOf(initialData?.date?.dayOfMonth?.toString() ?: "15") }
    var hour by remember { mutableStateOf(initialData?.time?.hour?.toString() ?: "14") }
    var minute by remember { mutableStateOf(initialData?.time?.minute?.toString() ?: "30") }
    var second by remember { mutableStateOf(initialData?.time?.second?.toString() ?: "0") }
    
    var placeNameQuery by remember { mutableStateOf(initialData?.location?.placeName ?: "") }
    var resolvedLocation by remember { mutableStateOf<BirthLocation?>(initialData?.location) }
    var resolvingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var ambiguousLocations by remember { mutableStateOf<List<BirthLocation>>(emptyList()) }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                .testTag("birth_data_dialog"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Birth Profile Details",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_close_button")) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Gender (लिंग)
                Text(
                    text = "लिंग (Gender)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("पुरुष", "महिला", "अन्य").forEach { option ->
                        val selected = gender == option
                        FilterChip(
                            selected = selected,
                            onClick = { gender = option },
                            label = { Text(option) },
                            modifier = Modifier.weight(1f).testTag("chip_gender_$option")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("YYYY") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("input_year"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                    OutlinedTextField(
                        value = month,
                        onValueChange = { month = it },
                        label = { Text("MM") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_month"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = { Text("DD") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_day"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it },
                        label = { Text("HH (0-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_hour"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { minute = it },
                        label = { Text("MM") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_minute"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                    OutlinedTextField(
                        value = second,
                        onValueChange = { second = it },
                        label = { Text("SS") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_second"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(16.dp))

                // Verified Location Resolution
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = placeNameQuery,
                        onValueChange = { 
                            placeNameQuery = it
                            resolvedLocation = null
                        },
                        label = { Text("City / Place") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_place"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        trailingIcon = {
                            if (resolvedLocation != null) {
                                Icon(Icons.Default.CheckCircle, "Verified", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                    
                    Button(
                        onClick = {
                            scope.launch {
                                resolvingLocation = true
                                locationError = null
                                ambiguousLocations = emptyList()
                                val result = onResolveLocation(placeNameQuery)
                                result.fold(
                                    onSuccess = { locations ->
                                        if (locations.size == 1) {
                                            resolvedLocation = locations.first()
                                            placeNameQuery = resolvedLocation!!.placeName
                                        } else if (locations.isEmpty()) {
                                            locationError = "Location not found."
                                        } else {
                                            ambiguousLocations = locations
                                        }
                                    },
                                    onFailure = { err ->
                                        locationError = err.message ?: "Failed to resolve location."
                                    }
                                )
                                resolvingLocation = false
                            }
                        },
                        enabled = !resolvingLocation && placeNameQuery.isNotBlank() && resolvedLocation == null,
                        modifier = Modifier.testTag("resolve_location_button")
                    ) {
                        if (resolvingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Resolve")
                        }
                    }
                }
                
                if (locationError != null) {
                    Text(
                        text = locationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (ambiguousLocations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Multiple matches found. Select one:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCard, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ambiguousLocations.forEach { loc ->
                            Text(
                                text = "${loc.placeName} (${loc.timeZoneId ?: "Unknown TZ"})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        resolvedLocation = loc
                                        placeNameQuery = loc.placeName
                                        ambiguousLocations = emptyList()
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }
                }

                if (resolvedLocation != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val elevationText = if (resolvedLocation!!.altitudeMeters != null) " • ${resolvedLocation!!.altitudeMeters?.toInt()}m elev." else ""
                    Text(
                        text = "Verified: ${resolvedLocation!!.placeName} (${resolvedLocation!!.timeZoneId ?: "System TZ"})$elevationText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                if (resolvedLocation == null) {
                                    throw IllegalArgumentException("Please resolve the location first.")
                                }
                                val parsedYear = year.toIntOrNull() ?: throw IllegalArgumentException("Invalid year")
                                val parsedMonth = month.toIntOrNull() ?: throw IllegalArgumentException("Invalid month")
                                val parsedDay = day.toIntOrNull() ?: throw IllegalArgumentException("Invalid day")
                                val parsedDate = LocalDate.of(parsedYear, parsedMonth, parsedDay)

                                val parsedHour = hour.toIntOrNull() ?: throw IllegalArgumentException("Invalid hour")
                                val parsedMinute = minute.toIntOrNull() ?: throw IllegalArgumentException("Invalid minute")
                                val parsedSecond = second.toIntOrNull() ?: 0
                                val parsedTime = LocalTime.of(parsedHour, parsedMinute, parsedSecond)

                                val zoneIdString = resolvedLocation!!.timeZoneId
                                val zone = try {
                                    if (zoneIdString != null) ZoneId.of(zoneIdString.trim()) else throw IllegalArgumentException("Location does not have a verified timezone.")
                                } catch (e: Exception) {
                                    throw IllegalArgumentException("Unknown TimeZone: $zoneIdString.")
                                }

                                val birthData = BirthData(
                                    name = name.ifBlank { "User" },
                                    date = parsedDate,
                                    time = parsedTime,
                                    location = resolvedLocation!!,
                                    timeZone = zone
                                )
                                onSubmit(birthData)
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Invalid input values"
                            }
                        },
                        modifier = Modifier.testTag("submit_birth_data_button")
                    ) {
                        Text("Calculate Chart")
                    }
                }
            }
        }
    }
}
