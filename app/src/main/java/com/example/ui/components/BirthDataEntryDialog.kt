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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class CityPreset(
    val name: String,
    val lat: Double,
    val lon: Double,
    val timeZone: String
)

val CITY_PRESETS = listOf(
    CityPreset("New Delhi", 28.6139, 77.2090, "Asia/Kolkata"),
    CityPreset("Mumbai", 19.0760, 72.8777, "Asia/Kolkata"),
    CityPreset("Bengaluru", 12.9716, 77.5946, "Asia/Kolkata"),
    CityPreset("Varanasi", 25.3176, 82.9739, "Asia/Kolkata"),
    CityPreset("London", 51.5074, -0.1278, "Europe/London"),
    CityPreset("New York", 40.7128, -74.0060, "America/New_York"),
    CityPreset("San Francisco", 37.7749, -122.4194, "America/Los_Angeles"),
    CityPreset("Tokyo", 35.6762, 139.6503, "Asia/Tokyo")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthDataEntryDialog(
    initialData: BirthData? = null,
    onDismiss: () -> Unit,
    onSubmit: (BirthData) -> Unit
) {
    var name by remember { mutableStateOf(initialData?.name ?: "User") }
    var year by remember { mutableStateOf(initialData?.date?.year?.toString() ?: "1995") }
    var month by remember { mutableStateOf(initialData?.date?.monthValue?.toString() ?: "8") }
    var day by remember { mutableStateOf(initialData?.date?.dayOfMonth?.toString() ?: "15") }

    var hour by remember { mutableStateOf(initialData?.time?.hour?.toString() ?: "14") }
    var minute by remember { mutableStateOf(initialData?.time?.minute?.toString() ?: "30") }
    var second by remember { mutableStateOf(initialData?.time?.second?.toString() ?: "0") }

    var placeName by remember { mutableStateOf(initialData?.location?.placeName ?: "New Delhi, India") }
    var latitude by remember { mutableStateOf(initialData?.location?.latitude?.toString() ?: "28.6139") }
    var longitude by remember { mutableStateOf(initialData?.location?.longitude?.toString() ?: "77.2090") }
    var timeZoneId by remember { mutableStateOf(initialData?.timeZone?.id ?: "Asia/Kolkata") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

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

                Spacer(modifier = Modifier.height(16.dp))

                // Date of Birth
                Text(
                    text = "Date of Birth (YYYY - MM - DD)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Year") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("input_year"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                    OutlinedTextField(
                        value = month,
                        onValueChange = { month = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Month") },
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
                        onValueChange = { day = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Day") },
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

                Spacer(modifier = Modifier.height(16.dp))

                // Time of Birth
                Text(
                    text = "Time of Birth (24-Hour: HH : MM : SS)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Hour") },
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
                        onValueChange = { minute = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Min") },
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
                        onValueChange = { second = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Sec") },
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

                // Quick City Presets
                Text(
                    text = "Quick City Presets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CITY_PRESETS) { preset ->
                        FilterChip(
                            selected = placeName.startsWith(preset.name),
                            onClick = {
                                placeName = "${preset.name}"
                                latitude = preset.lat.toString()
                                longitude = preset.lon.toString()
                                timeZoneId = preset.timeZone
                            },
                            label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Location Details
                OutlinedTextField(
                    value = placeName,
                    onValueChange = { placeName = it },
                    label = { Text("Place / City Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_place"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude (-90..90)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_latitude"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude (-180..180)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_longitude"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = timeZoneId,
                    onValueChange = { timeZoneId = it },
                    label = { Text("Time Zone (e.g. Asia/Kolkata, UTC)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_timezone"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

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
                                val parsedYear = year.toIntOrNull() ?: throw IllegalArgumentException("Invalid year")
                                val parsedMonth = month.toIntOrNull() ?: throw IllegalArgumentException("Invalid month")
                                val parsedDay = day.toIntOrNull() ?: throw IllegalArgumentException("Invalid day")
                                val parsedDate = LocalDate.of(parsedYear, parsedMonth, parsedDay)

                                val parsedHour = hour.toIntOrNull() ?: throw IllegalArgumentException("Invalid hour")
                                val parsedMinute = minute.toIntOrNull() ?: throw IllegalArgumentException("Invalid minute")
                                val parsedSecond = second.toIntOrNull() ?: 0
                                val parsedTime = LocalTime.of(parsedHour, parsedMinute, parsedSecond)

                                val parsedLat = latitude.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid latitude")
                                val parsedLon = longitude.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid longitude")
                                val location = BirthLocation(parsedLat, parsedLon, placeName.ifBlank { "Birth Location" })

                                val zone = try {
                                    ZoneId.of(timeZoneId.trim())
                                } catch (e: Exception) {
                                    throw IllegalArgumentException("Unknown TimeZone: $timeZoneId. Use formats like Asia/Kolkata, UTC, America/New_York.")
                                }

                                val birthData = BirthData(
                                    name = name.ifBlank { "User" },
                                    date = parsedDate,
                                    time = parsedTime,
                                    location = location,
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
