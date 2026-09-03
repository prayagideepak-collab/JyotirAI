package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Chart : Screen("chart")
    object Transit : Screen("transit")
    object Assistant : Screen("assistant")
    object Predictions : Screen("predictions")
    object Dasha : Screen("dasha")
    object Panchang : Screen("panchang")
    object Muhurta : Screen("muhurta")
    object Compatibility : Screen("compatibility")
    object Numerology : Screen("numerology")
    object Settings : Screen("settings")
}
