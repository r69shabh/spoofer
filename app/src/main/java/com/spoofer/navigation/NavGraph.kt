package com.spoofer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.model.LatLng
import com.spoofer.ui.screen.HistoryScreen
import com.spoofer.ui.screen.MapScreen
import com.spoofer.ui.screen.SettingsScreen
import com.spoofer.viewmodel.MapViewModel
import com.spoofer.viewmodel.SpoofViewModel

object Routes {
    const val MAP = "map"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.MAP,
    ) {
        composable(Routes.MAP) { entry ->
            val mapViewModel: MapViewModel = hiltViewModel()
            val spoofViewModel: SpoofViewModel = hiltViewModel()

            val historyLat = entry.savedStateHandle.get<Double>("history_lat")
            val historyLng = entry.savedStateHandle.get<Double>("history_lng")
            LaunchedEffect(historyLat, historyLng) {
                if (historyLat != null && historyLng != null) {
                    mapViewModel.setTarget(LatLng(historyLat, historyLng))
                    entry.savedStateHandle.remove<Double>("history_lat")
                    entry.savedStateHandle.remove<Double>("history_lng")
                }
            }

            MapScreen(
                mapViewModel = mapViewModel,
                spoofViewModel = spoofViewModel,
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onSelectEntry = { entry ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("history_lat", entry.targetLatitude)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("history_lng", entry.targetLongitude)
                    navController.popBackStack()
                },
            )
        }
    }
}
