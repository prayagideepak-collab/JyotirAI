package com.example.ui.navigation

import android.app.Application
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.domain.reading.CameraReadingCoordinator
import com.example.ui.screens.*
import com.example.ui.viewmodel.AstrologyViewModel
import com.example.ui.viewmodel.AstrologyViewModelFactory

@Composable
fun JyotishNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val astrologyViewModel: AstrologyViewModel = viewModel(
        factory = AstrologyViewModelFactory(application)
    )
    val cameraCoordinator = remember { CameraReadingCoordinator() }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = astrologyViewModel,
                onNavigateToRashifal = { navController.navigate(Screen.Predictions.route) },
                onNavigateToCompatibility = { navController.navigate(Screen.Compatibility.route) },
                onNavigateToNumerology = { navController.navigate(Screen.Numerology.route) },
                onNavigateToAssistant = { navController.navigate(Screen.Assistant.route) },
                onNavigateToPalmReading = { navController.navigate(Screen.PalmReading.route) },
                onNavigateToFaceReading = { navController.navigate(Screen.FaceReading.route) }
            )
        }
        composable(Screen.Chart.route) { ChartScreen(viewModel = astrologyViewModel) }
        composable(Screen.Transit.route) {
            TransitScreen(
                viewModel = astrologyViewModel,
                onNavigateToHome = { navController.navigate(Screen.Home.route) }
            )
        }
        composable(Screen.Assistant.route) {
            AssistantScreen(
                viewModel = astrologyViewModel,
                onNavigateToHome = { navController.navigate(Screen.Home.route) }
            )
        }
        composable(Screen.Predictions.route) {
            DailyRashifalScreen(
                viewModel = astrologyViewModel,
                onNavigateToHome = { navController.navigate(Screen.Home.route) }
            )
        }
        composable(Screen.Dasha.route) {
            DashaScreen(
                viewModel = astrologyViewModel,
                onNavigateToHome = { navController.navigate(Screen.Home.route) }
            )
        }
        composable(Screen.Panchang.route) { PanchangScreen(viewModel = astrologyViewModel) }
        composable(Screen.Muhurta.route) { MuhurtaScreen(viewModel = astrologyViewModel) }
        composable(Screen.Compatibility.route) { CompatibilityScreen(viewModel = astrologyViewModel) }
        composable(Screen.Numerology.route) {
            NumerologyScreen(
                viewModel = astrologyViewModel,
                onNavigateToHome = { navController.navigate(Screen.Home.route) }
            )
        }
        composable(Screen.PalmReading.route) {
            PalmReadingScreen(
                coordinator = cameraCoordinator,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.FaceReading.route) {
            FaceReadingScreen(
                coordinator = cameraCoordinator,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) { SettingsScreen() }
    }
}
