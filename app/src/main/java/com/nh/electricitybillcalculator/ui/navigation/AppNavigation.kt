package com.nh.electricitybillcalculator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nh.electricitybillcalculator.ui.screens.*
import com.nh.electricitybillcalculator.ui.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Calculator : Screen("calculator")
    object Appliances : Screen("appliances")
    object History : Screen("history")
    object Settings : Screen("settings")
    object MeterReadings : Screen("meter_readings")
    object TariffSettings : Screen("tariff_settings")
    object PowerCalculator : Screen("power_calculator")
    object Analytics : Screen("analytics")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onNavigateToHome = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.Calculator.route) {
            CalculatorScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.Appliances.route) {
            ApplianceScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.History.route) {
            HistoryScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.MeterReadings.route) {
            MeterReadingScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.TariffSettings.route) {
            TariffSettingsScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.PowerCalculator.route) {
            PowerCalculatorScreen(navController = navController)
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen(navController = navController, viewModel = viewModel)
        }
    }
}
