package com.lifeforge.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.gotrue.Auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: com.lifeforge.app.data.repository.AuthRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val prefs by lazy { 
        context.getSharedPreferences("stoppy_prefs", android.content.Context.MODE_PRIVATE)
    }

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            // First check session quickly (or rely on repository init)
            authRepository.initialize()
            
            authRepository.currentUser.collect { userState ->
                when (userState) {
                    is com.lifeforge.app.data.repository.AuthRepository.UserState.Authenticated -> {
                        _startDestination.value = if (com.lifeforge.app.util.PermissionHelper.hasAllPermissions(context)) {
                            com.lifeforge.app.ui.navigation.Screen.Main.route
                        } else {
                            com.lifeforge.app.ui.navigation.Screen.PermissionIntro.route
                        }
                    }
                    is com.lifeforge.app.data.repository.AuthRepository.UserState.NotAuthenticated,
                    is com.lifeforge.app.data.repository.AuthRepository.UserState.Error -> {
                        val authToken = prefs.getString("auth_token", null)
                        val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
                        
                        // If we have a token, we are likely authenticated but state hasn't caught up or session restoration needs time.
                        // However, if we trust the token, we should treat it as authenticated for start destination purposes.
                        
                        if (authToken != null && onboardingCompleted) {
                             if (com.lifeforge.app.util.PermissionHelper.hasAllPermissions(context)) {
                                 _startDestination.value = com.lifeforge.app.ui.navigation.Screen.Main.route
                             } else {
                                 _startDestination.value = com.lifeforge.app.ui.navigation.Screen.PermissionIntro.route
                             }
                        } else if (!onboardingCompleted) {
                            _startDestination.value = com.lifeforge.app.ui.navigation.Screen.Onboarding.route
                        } else {
                            _startDestination.value = com.lifeforge.app.ui.navigation.Screen.Auth.route
                        }
                    }
                    else -> {
                        // Loading, do nothing yet
                    }
                }
            }
        }
    }
}
