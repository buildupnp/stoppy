package com.lifeforge.app.ui.screens.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.app.data.local.database.entities.WeeklyChallenge
import com.lifeforge.app.data.repository.AuthRepository
import com.lifeforge.app.data.repository.FeatureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChallengesUiState(
    val challenges: List<WeeklyChallenge> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ChallengesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val featureRepository: FeatureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChallengesUiState())
    val uiState: StateFlow<ChallengesUiState> = _uiState.asStateFlow()

    init {
        loadChallenges()
    }

    private fun loadChallenges() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            // Generate challenges if needed
            featureRepository.generateWeeklyChallenges(userId)
            
            // Observe active challenges
            featureRepository.getActiveChallenges(userId).collect { challenges ->
                _uiState.value = ChallengesUiState(
                    challenges = challenges,
                    isLoading = false
                )
            }
        }
    }
    
    fun claimReward(challengeId: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            featureRepository.claimChallengeReward(userId, challengeId)
        }
    }
}
