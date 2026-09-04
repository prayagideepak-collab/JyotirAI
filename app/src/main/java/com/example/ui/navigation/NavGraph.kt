package com.example.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.screens.*
import com.example.ui.viewmodel.AstrologyViewModel

@Composable
fun JyotishNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    astrologyViewModel: AstrologyViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(Screen.Home.route) { HomeScreen(viewModel = astrologyViewModel) }
        composable(Screen.Chart.route) { ChartScreen(viewModel = astrologyViewModel) }
        composable(Screen.Transit.route) { TransitScreen() }
        composable(Screen.Assistant.route) { AssistantScreen() }
        composable(Screen.Predictions.route) { PredictionsScreen() }
        composable(Screen.Dasha.route) {
            DashaScreen(
                viewModel = astrologyViewModel,
                onNavigateToHome = { navController.navigate(Screen.Home.route) }
            )
        }
        composable(Screen.Panchang.route) { PanchangScreen() }
        composable(Screen.Muhurta.route) { MuhurtaScreen() }
        composable(Screen.Compatibility.route) { CompatibilityScreen() }
        composable(Screen.Numerology.route) { NumerologyScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }
    }
}
