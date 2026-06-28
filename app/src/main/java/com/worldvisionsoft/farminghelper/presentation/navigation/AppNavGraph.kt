package com.worldvisionsoft.farminghelper.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.worldvisionsoft.farminghelper.presentation.camera.CameraScreen
import com.worldvisionsoft.farminghelper.presentation.history.HistoryScreen
import com.worldvisionsoft.farminghelper.presentation.home.HomeScreen
import com.worldvisionsoft.farminghelper.presentation.permission.PermissionGateScreen
import com.worldvisionsoft.farminghelper.presentation.result.ResultScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Route.PermissionGate,
    ) {
        composable<Route.PermissionGate> {
            PermissionGateScreen(
                onPermissionGranted = {
                    navController.navigate(Route.Home) {
                        popUpTo<Route.PermissionGate> { inclusive = true }
                    }
                }
            )
        }

        composable<Route.Home> {
            HomeScreen(
                onLeafDiseaseClick = { navController.navigate(Route.Camera) },
                onPestClick = { navController.navigate(Route.Camera) },
                onHistoryClick = { navController.navigate(Route.History) },
            )
        }

        composable<Route.Camera> {
            CameraScreen(
                onImageCaptured = { navController.navigate(Route.Result) }
            )
        }

        composable<Route.Result> {
            ResultScreen(
                onSaveAndViewHistory = {
                    navController.navigate(Route.History) {
                        popUpTo<Route.Home> { inclusive = false }
                    }
                },
                onRetake = { navController.popBackStack() },
            )
        }

        composable<Route.History> {
            HistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
