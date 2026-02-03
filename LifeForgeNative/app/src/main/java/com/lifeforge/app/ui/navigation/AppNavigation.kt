package com.lifeforge.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import com.lifeforge.app.ui.screens.MainScreen
import com.lifeforge.app.ui.screens.auth.AuthScreen

sealed class Screen(val route: String) {
    object Auth : Screen("auth?isLogin={isLogin}&name={name}") {
        fun createRoute(isLogin: Boolean, name: String = "") = "auth?isLogin=$isLogin&name=$name"
    }
    object Main : Screen("main")
    object Onboarding : Screen("onboarding")
    object PermissionIntro : Screen("permission_intro")
}

@Composable
fun AppNavigation(
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val authViewModel: com.lifeforge.app.ui.screens.auth.AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            
            // Local error state for dialog
            var showGlobalError by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
            
            // Collect auth state
            val authState by authViewModel.uiState.collectAsState()
            
            // Handle Side Effects (Navigation & Errors)
            androidx.compose.runtime.LaunchedEffect(authState) {
                if (authState.isAuthenticated) {
                    val prefs = context.getSharedPreferences("stoppy_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("onboarding_completed", true).apply()
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
                
                if (authState.error != null) {
                   showGlobalError = authState.error
                   authViewModel.clearError()
                }
            }

            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                com.lifeforge.app.ui.screens.onboarding.OnboardingScreen(
                    onFinish = {
                        val prefs = context.getSharedPreferences("stoppy_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("onboarding_completed", true).apply()
                        navController.navigate(Screen.Auth.createRoute(false, it)) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    onGoogleSignIn = { name ->
                        scope.launch {
                            try {
                                val googleHelper = com.lifeforge.app.util.GoogleAuthHelper(context)
                                val token = googleHelper.getGoogleIdToken(context)
                                if (token != null) {
                                    authViewModel.signInWithGoogle(token, name)
                                } else {
                                    showGlobalError = "Google Sign-In cancelled or failed"
                                }
                            } catch (e: Exception) {
                                showGlobalError = "Error initializing Google Sign-In: ${e.message}"
                            }
                        }
                    },
                    onSignUpEmail = {
                        navController.navigate(Screen.Auth.createRoute(false, it))
                    },
                    onLogin = {
                        navController.navigate(Screen.Auth.createRoute(true))
                    },
                    onPrivacyPolicyClick = {
                        navController.navigate("privacy_policy")
                    }
                )
                
                if (authState.isLoading) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                            .clickable(
                                interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
                                indication = null
                            ) { }
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                            color = com.lifeforge.app.ui.theme.Accent
                        )
                    }
                }
                
                // Global Error Dialog
                if (showGlobalError != null) {
                    com.lifeforge.app.ui.components.AuthErrorDialog(
                        message = showGlobalError!!,
                        onDismiss = { showGlobalError = null }
                    )
                }
            }
        }

        composable(
            route = Screen.Auth.route,
            arguments = listOf(
                androidx.navigation.navArgument("isLogin") { 
                    type = androidx.navigation.NavType.BoolType
                    defaultValue = false
                },
                androidx.navigation.navArgument("name") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val isLogin = backStackEntry.arguments?.getBoolean("isLogin") ?: false
            val name = backStackEntry.arguments?.getString("name") ?: ""
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onPrivacyPolicyClick = {
                    navController.navigate("privacy_policy")
                },
                initialIsLogin = isLogin,
                initialName = name
            )
        }
        composable("privacy_policy") {
            com.lifeforge.app.ui.screens.settings.PrivacyPolicyScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.PermissionIntro.route) {
            com.lifeforge.app.ui.screens.permissions.PermissionIntroScreen(
                onAllGranted = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.PermissionIntro.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Main.route) {
            MainScreen()
        }
    }
}
