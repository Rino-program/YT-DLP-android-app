package com.example.ytdlpapp.ui.navigation

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object BinaryManager : Screen("binary_manager")
    object Settings : Screen("settings")
    object BatchProcessing : Screen("batch_processing")
    object ProxySettings : Screen("proxy_settings")
    object Statistics : Screen("statistics")
}
