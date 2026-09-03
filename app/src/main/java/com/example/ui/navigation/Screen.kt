package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Chart : Screen("chart")
    object Transit : Screen("transit")
    object Assistant : Screen("assistant")
}
