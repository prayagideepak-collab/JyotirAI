package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.JyotishNavGraph
import com.example.ui.navigation.Screen
import com.example.ui.theme.JyotishTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlobalDynamicHeader
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.AstrologyViewModel
import com.example.ui.viewmodel.AstrologyViewModelFactory
import com.example.ui.viewmodel.PanchangUiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JyotishTheme {
                JyotirAIApp()
            }
        }
    }
}
@Composable
fun JyotirAIApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val viewModel: AstrologyViewModel = viewModel(factory = AstrologyViewModelFactory(application))
    val tickerSpeed by viewModel.tickerSpeed.collectAsStateWithLifecycle()
    val panchangUiState by viewModel.panchangUiState.collectAsStateWithLifecycle()
    val snapshot = (panchangUiState as? PanchangUiState.Success)?.snapshot
    val events = remember(snapshot) {
        if (snapshot != null) com.example.domain.panchang.VedicEventEngine.generateTimeline(snapshot) else emptyList()
    }
    val now = java.time.ZonedDateTime.now(snapshot?.location?.timeZoneId?.let { java.time.ZoneId.of(it) } ?: java.time.ZoneId.systemDefault())
    val nearestEvent = remember(events, now) {
        com.example.domain.panchang.VedicEventEngine.getNearestUpcomingEvent(events, now)
    }
    val endingEvent = remember(events, now) {
        com.example.domain.panchang.VedicEventEngine.getEndingSoonEvent(events, now)
    }

    val primaryInfo = when {
        endingEvent != null -> {
            val remaining = java.time.Duration.between(now, endingEvent.endTime)
            val h = maxOf(0L, remaining.toHours())
            val m = maxOf(0L, remaining.toMinutes() % 60)
            val s = maxOf(0L, remaining.seconds % 60)
            "🔴 ${endingEvent.displayName} समाप्त होने में: %02d:%02d:%02d".format(h, m, s)
        }
        nearestEvent != null -> {
            val remaining = java.time.Duration.between(now, nearestEvent.startTime)
            val h = maxOf(0L, remaining.toHours())
            val m = maxOf(0L, remaining.toMinutes() % 60)
            val s = maxOf(0L, remaining.seconds % 60)
            "🟢 आगामी ${nearestEvent.displayName} शुरू: %02d:%02d:%02d".format(h, m, s)
        }
        snapshot != null -> {
            val samvat = com.example.domain.panchang.VedicEventEngine.calculateVikramSamvat(snapshot.requestedDateTime.toLocalDate())
            "विक्रम संवत $samvat • ${snapshot.vara.hindiName} • ${snapshot.tithi.hindiName}"
        }
        else -> "🟢 वैदिक पंचांग एवं मुहूर्त सक्रिय"
    }

    val activeItemsCount = if (events.size > 1) 2 else 1

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            GlobalDynamicHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                activeItemsCount = activeItemsCount,
                tickerSpeed = tickerSpeed,
                primaryInfo = primaryInfo
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.drawBehind {
                    val borderSize = 1f
                    drawLine(
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = borderSize
                    )
                }
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val items = listOf(
                    Triple(Screen.Home, "Home", Icons.Filled.Home),
                    Triple(Screen.Chart, "Kundli", Icons.Filled.Star),
                    Triple(Screen.Panchang, "पंचांग", Icons.Filled.CalendarMonth),
                    Triple(Screen.Dasha, "Dasha", Icons.Filled.Schedule),
                    Triple(Screen.Assistant, "Astrologer", Icons.Filled.AutoAwesome)
                )

                items.forEach { (screen, label, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        JyotishNavGraph(
            navController = navController,
            innerPadding = innerPadding
        )
    }
}
