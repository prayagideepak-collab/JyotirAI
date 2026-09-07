package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun GlobalDynamicHeader(
    modifier: Modifier = Modifier,
    activeItemsCount: Int = 1,
    tickerSpeed: String = "धीमी",
    primaryInfo: String = "🔴 दशमी समाप्त होने में: 04:18:32"
) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalDateTime.now()
            delay(1000L)
        }
    }

    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a")
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")

    // Determine animation duration based on readable speed control
    val animDuration = when (tickerSpeed) {
        "तेज़" -> 7000
        "सामान्य" -> 14000
        else -> 24000 // धीमी (slow readable speed)
    }

    // Right to left ticker animation state for multiple items
    val infiniteTransition = rememberInfiniteTransition(label = "ticker")
    val tickerOffset by infiniteTransition.animateFloat(
        initialValue = 1000f,
        targetValue = -1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(animDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ticker_offset"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "JyotirAI Engine",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${currentTime.format(dateFormatter)} | ${currentTime.format(timeFormatter)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Smart Header Content Mode: Single Item vs Multiple Item Readable Speed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp),
                contentAlignment = if (activeItemsCount <= 1) Alignment.CenterStart else Alignment.CenterStart
            ) {
                if (activeItemsCount <= 1) {
                    // Single Item Mode: FIXED, NO SCROLL, NO MARQUEE
                    Text(
                        text = primaryInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentAmber,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                } else {
                    // Multiple Item Mode: Slow Readable Ticker with Speed Control
                    val tickerText = "✨ $primaryInfo • 🟢 एकादशी शुरू: 04:18:32 • ⭐ नक्षत्र परिवर्तन: 01:12:20 • "
                    Row(
                        modifier = Modifier.offset(x = tickerOffset.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tickerText + tickerText,
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentAmber,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
