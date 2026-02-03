package com.lifeforge.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.app.ui.screens.dashboard.DashboardScreen
import com.lifeforge.app.ui.screens.forge.ForgeScreen
import com.lifeforge.app.ui.screens.forge.ForgeViewModel
import com.lifeforge.app.ui.screens.guardian.GuardianScreen
import com.lifeforge.app.ui.screens.settings.SettingsScreen
import com.lifeforge.app.ui.screens.achievements.AchievementsScreen
import com.lifeforge.app.ui.screens.challenges.ChallengesScreen
import com.lifeforge.app.ui.theme.Accent
import com.lifeforge.app.ui.theme.CardDark
import com.lifeforge.app.ui.theme.Primary
import com.lifeforge.app.ui.theme.TextSecondary

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    object Forge : BottomNavItem(
        route = "forge",
        title = "Forge",
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter
    )
    object Guardian : BottomNavItem(
        route = "guardian",
        title = "Guardian",
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Outlined.Security
    )
    object Settings : BottomNavItem(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Forge,
        BottomNavItem.Guardian,
        BottomNavItem.Settings
    )
    
    val showBottomBar = currentDestination?.route != "ai_workout"
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    items.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                DashboardScreen(
                    onNavigate = { route ->
                        navController.navigate(route)
                    }
                )
            }
            composable(BottomNavItem.Forge.route) { backStackEntry ->
                // Get ViewModel scoped to this navigation entry
                val viewModel: ForgeViewModel = hiltViewModel()
                
                // Check for result from AI workout
                val result = backStackEntry.savedStateHandle.get<Int>("ai_workout_result")
                
                LaunchedEffect(result) {
                    if (result != null && result > 0) {
                        viewModel.logPushups(result)
                        backStackEntry.savedStateHandle.remove<Int>("ai_workout_result")
                    }
                }
                
                ForgeScreen(
                    viewModel = viewModel,
                    onNavigateToCamera = {
                        navController.navigate("ai_workout")
                    },
                    onNavigateToWisdom = {
                        navController.navigate("wisdom")
                    }
                )
            }
            composable(BottomNavItem.Guardian.route) {
                GuardianScreen()
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable("ai_workout") {
                com.lifeforge.app.ui.screens.forge.AIWorkoutScreen(
                    onClose = { navController.popBackStack() },
                    onFinish = { count ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("ai_workout_result", count)
                            
                        navController.popBackStack()
                    }
                )
            }
            composable("wisdom") {
                val viewModel: ForgeViewModel = hiltViewModel()
                com.lifeforge.app.ui.screens.forge.WisdomScreen(
                    onClose = { navController.popBackStack() },
                    onReward = { coins ->
                        viewModel.logWisdom(coins)
                    }
                )
            }
            composable("achievements") {
                AchievementsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("challenges") {
                ChallengesScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("notifications") {
                com.lifeforge.app.ui.screens.notifications.NotificationScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("feedback") {
                com.lifeforge.app.ui.screens.settings.FeedbackScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("privacy_policy") {
                com.lifeforge.app.ui.screens.settings.PrivacyPolicyScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("statistics") {
                com.lifeforge.app.ui.screens.stats.StatisticsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
