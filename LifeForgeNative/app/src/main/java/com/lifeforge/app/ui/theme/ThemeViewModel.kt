package com.lifeforge.app.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.app.data.repository.AuthRepository
import com.lifeforge.app.data.repository.FeatureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing app-wide theme state.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val featureRepository: FeatureRepository
) : ViewModel() {

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        observeUserTheme()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeUserTheme() {
        viewModelScope.launch {
            authRepository.currentUser
                .map { state ->
                    (state as? AuthRepository.UserState.Authenticated)?.userId
                }
                .distinctUntilChanged()
                .flatMapLatest { userId ->
                    if (userId != null) {
                        featureRepository.getPreferencesFlow(userId)
                    } else {
                        flowOf(null)
                    }
                }
                .collect { prefs ->
                    if (prefs != null) {
                        _themeMode.value = when (prefs.themeMode) {
                            "light" -> ThemeMode.LIGHT
                            "system" -> ThemeMode.SYSTEM
                            else -> ThemeMode.DARK
                        }
                    }
                }
        }
    }
    
    fun setTheme(mode: ThemeMode) {
        // Immediate local update for responsiveness
        _themeMode.value = mode
        
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val modeString = when (mode) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.SYSTEM -> "system"
                ThemeMode.DARK -> "dark"
            }
            featureRepository.updateTheme(userId, modeString)
        }
    }
}
