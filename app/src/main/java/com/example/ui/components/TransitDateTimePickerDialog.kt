package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BorderSubtle
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

@Composable
fun TransitDateTimePickerDialog(
    initialDateTime: ZonedDateTime,
    onDismiss: () -> Unit,
    onConfirm: (ZonedDateTime) -> Unit
) {
    var year by remember { mutableStateOf(initialDateTime.year.toString()) }
    var month by remember { mutableStateOf(initialDateTime.monthValue.toString()) }
    var day by remember { mutableStateOf(initialDateTime.dayOfMonth.toString()) }
    var hour by remember { mutableStateOf(initialDateTime.hour.toString()) }
    var minute by remember { mutableStateOf(initialDateTime.minute.toString()) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                .testTag("transit_date_time_picker_dialog"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Transit Date & Time",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("date_picker_close_button")) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Picker",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                // Date Fields
                Text(
                    text = "Date (Year / Month / Day)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it; errorMessage = null },
                        label = { Text("Year") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("transit_year_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = month,
                        onValueChange = { month = it; errorMessage = null },
                        label = { Text("Month") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transit_month_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it; errorMessage = null },
                        label = { Text("Day") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transit_day_input"),
                        singleLine = true
                    )
                }

                // Time Fields
                Text(
                    text = "Time (24h Format: Hour / Minute)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it; errorMessage = null },
                        label = { Text("Hour (0-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transit_hour_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { minute = it; errorMessage = null },
                        label = { Text("Minute (0-59)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transit_minute_input"),
                        singleLine = true
                    )
                }

                // Quick "Now" Action
                OutlinedButton(
                    onClick = {
                        val now = ZonedDateTime.now(initialDateTime.zone)
                        year = now.year.toString()
                        month = now.monthValue.toString()
                        day = now.dayOfMonth.toString()
                        hour = now.hour.toString()
                        minute = now.minute.toString()
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("transit_set_now_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset to Current Moment")
                }

                // Error Display
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            try {
                                val y = year.toInt()
                                val m = month.toInt()
                                val d = day.toInt()
                                val hr = hour.toInt()
                                val mn = minute.toInt()

                                if (y !in 1000..3000) {
                                    errorMessage = "Year must be between 1000 and 3000"
                                    return@Button
                                }
                                val parsedDate = LocalDate.of(y, m, d)
                                val parsedTime = LocalTime.of(hr, mn, 0)
                                val newDateTime = ZonedDateTime.of(parsedDate, parsedTime, initialDateTime.zone)
                                onConfirm(newDateTime)
                            } catch (e: Exception) {
                                errorMessage = "Invalid date or time entered: ${e.message}"
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("transit_apply_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Calculate", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
