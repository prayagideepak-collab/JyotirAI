package com.example.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.ChartScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.TransitScreen

@Composable
fun JyotishNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
        }
        composable(Screen.Chart.route) {
            ChartScreen()
        }
        composable(Screen.Transit.route) {
            TransitScreen()
        }
        composable(Screen.Assistant.route) {
            AssistantScreen()
        }
    }
}
