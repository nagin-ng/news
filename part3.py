import os

files = {
"app/src/main/java/com/nh/electricitybillcalculator/ui/viewmodel/ViewModels.kt": """package com.nh.electricitybillcalculator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nh.electricitybillcalculator.data.local.entity.ApplianceEntity
import com.nh.electricitybillcalculator.data.local.entity.BillCalculationEntity
import com.nh.electricitybillcalculator.data.local.entity.MeterReadingEntity
import com.nh.electricitybillcalculator.data.local.entity.TariffSlabEntity
import com.nh.electricitybillcalculator.data.repository.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    val allBills = repository.allBills.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allReadings = repository.allReadings.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allAppliances = repository.allAppliances.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allSlabs = repository.allSlabs.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Initial dummy data if no slabs exist
    init {
        viewModelScope.launch {
            repository.allSlabs.collect { slabs ->
                if (slabs.isEmpty()) {
                    val defaultSlabs = listOf(
                        TariffSlabEntity(minUnits = 0.0, maxUnits = 50.0, rate = 3.0),
                        TariffSlabEntity(minUnits = 51.0, maxUnits = 100.0, rate = 4.0),
                        TariffSlabEntity(minUnits = 101.0, maxUnits = 200.0, rate = 5.5),
                        TariffSlabEntity(minUnits = 201.0, maxUnits = 300.0, rate = 6.5),
                        TariffSlabEntity(minUnits = 301.0, maxUnits = -1.0, rate = 7.5)
                    )
                    repository.insertAllSlabs(defaultSlabs)
                }
            }
        }
    }

    fun insertBill(bill: BillCalculationEntity) {
        viewModelScope.launch { repository.insertBill(bill) }
    }
    fun deleteBill(bill: BillCalculationEntity) {
        viewModelScope.launch { repository.deleteBill(bill) }
    }

    fun insertReading(reading: MeterReadingEntity) {
        viewModelScope.launch { repository.insertReading(reading) }
    }
    fun deleteReading(reading: MeterReadingEntity) {
        viewModelScope.launch { repository.deleteReading(reading) }
    }

    fun insertAppliance(appliance: ApplianceEntity) {
        viewModelScope.launch { repository.insertAppliance(appliance) }
    }
    fun deleteAppliance(appliance: ApplianceEntity) {
        viewModelScope.launch { repository.deleteAppliance(appliance) }
    }

    fun insertSlab(slab: TariffSlabEntity) {
        viewModelScope.launch { repository.insertSlab(slab) }
    }
    fun deleteSlab(slab: TariffSlabEntity) {
        viewModelScope.launch { repository.deleteSlab(slab) }
    }
    
    fun clearAllData() {
        viewModelScope.launch { repository.clearAllData() }
    }
}

class MainViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
""",

"app/src/main/java/com/nh/electricitybillcalculator/ui/navigation/AppNavigation.kt": """package com.nh.electricitybillcalculator.ui.navigation

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
""",

"app/src/main/java/com/nh/electricitybillcalculator/MainActivity.kt": """package com.nh.electricitybillcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nh.electricitybillcalculator.ui.navigation.AppNavigation
import com.nh.electricitybillcalculator.ui.navigation.Screen
import com.nh.electricitybillcalculator.ui.theme.ElectricityBillCalculatorProTheme
import com.nh.electricitybillcalculator.ui.viewmodel.MainViewModel
import com.nh.electricitybillcalculator.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ElectricityBillCalculatorProTheme {
                val app = application as ElectricityBillApp
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(app.repository)
                )
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        Screen.Home to Icons.Default.Home,
        Screen.Calculator to Icons.Default.Calculate,
        Screen.Appliances to Icons.Default.List,
        Screen.History to Icons.Default.History,
        Screen.Settings to Icons.Default.Settings
    )

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.first.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { (screen, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(screen.route.replaceFirstChar { it.uppercase() }) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AppNavigation(
            navController = navController,
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
""",

"app/src/main/java/com/nh/electricitybillcalculator/ui/screens/SplashScreen.kt": """package com.nh.electricitybillcalculator.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToHome: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500)
        )
        delay(1500)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.ElectricBolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale.value)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Electricity Bill Calculator Pro",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(alpha.value)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Smart Energy • Smart Calculation",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}
"""
}

for path, content in files.items():
    full_path = os.path.join('/data/data/com.termux/files/home/projects/ElectricityBillCalculatorPro', path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Part 3 created successfully!")
