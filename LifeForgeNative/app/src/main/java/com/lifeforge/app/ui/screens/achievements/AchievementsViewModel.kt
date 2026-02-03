package com.lifeforge.app.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.app.data.local.database.entities.Achievement
import com.lifeforge.app.data.repository.AuthRepository
import com.lifeforge.app.data.repository.FeatureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AchievementsUiState(
    val achievements: List<Achievement> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val totalCoinsEarned: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val featureRepository: FeatureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        loadAchievements()
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            // Initialize achievements if needed
            featureRepository.initializeAchievements(userId)
            
            // Observe achievements
            featureRepository.getAllAchievements(userId).collect { achievements ->
                val unlocked = achievements.filter { it.isUnlocked }
                val coinsEarned = unlocked.sumOf { it.coinReward }
                
                _uiState.value = AchievementsUiState(
                    achievements = achievements,
                    unlockedCount = unlocked.size,
                    totalCount = achievements.size,
                    totalCoinsEarned = coinsEarned,
                    isLoading = false
                )
            }
        }
    }
}
