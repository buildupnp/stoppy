package com.lifeforge.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.app.data.repository.AuthRepository
import com.lifeforge.app.data.repository.FeatureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val email: String = "Loading...",
    val displayName: String = "User",
    val themeMode: String = "dark",
    val emergencyUnlocksPerDay: Int = 3,
    val notificationsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val isLoading: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val featureRepository: FeatureRepository,
    private val demoDataSeeder: com.lifeforge.app.util.DemoDataSeeder,
    private val appLockRepository: com.lifeforge.app.data.repository.AppLockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
        loadPreferences()
        loadEmergencyUnlockLimit()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            authRepository.currentUser.collect { userState ->
                if (userState is AuthRepository.UserState.Authenticated) {
                    _uiState.value = _uiState.value.copy(
                        email = userState.email ?: "No Email",
                        displayName = userState.fullName ?: "User"
                    )
                } else {
                     _uiState.value = _uiState.value.copy(
                        email = "Not signed in",
                        displayName = "Guest"
                    )
                }
            }
        }
    }
    
    private fun loadPreferences() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            featureRepository.getPreferencesFlow(userId).collect { prefs ->
                if (prefs != null) {
                    _uiState.value = _uiState.value.copy(
                        themeMode = prefs.themeMode,
                        notificationsEnabled = prefs.notificationsEnabled,
                        hapticsEnabled = prefs.hapticsEnabled,
                        soundEnabled = prefs.soundEnabled
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            // Navigation should handle auth state changes elsewhere (e.g. MainActivity observing session)
        }
    }

    fun updateDisplayName(newName: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                authRepository.updateProfileName(userId, newName)
                // Profile flow in AuthRepository should update the currentUser flow, 
                // but we can refresh local UI state for immediacy
                _uiState.value = _uiState.value.copy(displayName = newName)
            }
        }
    }
    
    fun setTheme(themeMode: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            featureRepository.updateTheme(userId, themeMode)
            _uiState.value = _uiState.value.copy(themeMode = themeMode)
        }
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            featureRepository.updateNotifications(userId, enabled)
            _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
        }
    }
    
    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            featureRepository.updateHaptics(userId, enabled)
            _uiState.value = _uiState.value.copy(hapticsEnabled = enabled)
        }
    }
    
    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            featureRepository.updateSound(userId, enabled)
            _uiState.value = _uiState.value.copy(soundEnabled = enabled)
        }
    }
    
    fun seedDemoData() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true)
            demoDataSeeder.seedDemoData(userId)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    private fun loadEmergencyUnlockLimit() {
        viewModelScope.launch {
            val limit = appLockRepository.getEmergencyUnlockLimit()
            _uiState.value = _uiState.value.copy(emergencyUnlocksPerDay = limit)
        }
    }
    
    fun updateEmergencyUnlockLimit(limit: Int) {
        viewModelScope.launch {
            appLockRepository.setEmergencyUnlockLimit(limit)
            _uiState.value = _uiState.value.copy(emergencyUnlocksPerDay = limit)
        }
    }
}

