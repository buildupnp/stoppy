package com.lifeforge.app.ui.screens.forge

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.app.data.model.DailyQuest
import com.lifeforge.app.data.local.database.entities.WeeklyChallenge
import com.lifeforge.app.data.repository.ActivityRepository
import com.lifeforge.app.data.repository.AuthRepository
import com.lifeforge.app.data.repository.CoinRepository
import com.lifeforge.app.data.repository.FeatureRepository
import com.lifeforge.app.service.StepCounterService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgeUiState(
    val todaySteps: Int = 0,
    val todayPushups: Int = 0,
    val todaySquats: Int = 0,
    val coinsEarnedToday: Int = 0,
    val streakDays: Int = 0,
    val isLoading: Boolean = false,
    val canSpinToday: Boolean = true,
    val lastResult: ActivityResult? = null,
    val dailyQuests: List<DailyQuest> = emptyList(),
    val activeChallenges: List<WeeklyChallenge> = emptyList()
)

data class ActivityResult(
    val type: String,
    val count: Int,
    val coinsEarned: Int
)

@HiltViewModel
class ForgeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val activityRepository: ActivityRepository,
    private val coinRepository: CoinRepository,
    private val featureRepository: FeatureRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ForgeUiState())
    val uiState: StateFlow<ForgeUiState> = _uiState.asStateFlow()
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    init {
        loadTodayStats()
        observeSteps()
        checkSpinStatus()
        loadFeatureData()
    }
    
    private fun loadFeatureData() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            // Generate/Ensure quests exist
            featureRepository.getDailyQuests(userId).collect { quests ->
                 _uiState.value = _uiState.value.copy(dailyQuests = quests)
            }
        }
        
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            featureRepository.getActiveChallenges(userId).collect { challenges ->
                _uiState.value = _uiState.value.copy(activeChallenges = challenges)
            }
        }
    }
    
    private fun checkSpinStatus() {
        val prefs = context.getSharedPreferences("lifeforge_prefs", Context.MODE_PRIVATE)
        val lastSpinEpoch = prefs.getLong("last_spin_date", 0L)
        val todayEpoch = java.time.LocalDate.now().toEpochDay()
        
        _uiState.value = _uiState.value.copy(canSpinToday = lastSpinEpoch < todayEpoch)
    }
    
    fun performSpin() {
        if (!_uiState.value.canSpinToday) return
        
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Random reward between 5 and 50
            val rewards = listOf(5, 10, 15, 20, 25, 50)
            val amount = rewards.random()
            
            coinRepository.earnCoins(
                userId = userId,
                amount = amount,
                type = "daily_spin",
                description = "Daily Lucky Spin reward"
            )
            
            // Update SharedPreferences
            val prefs = context.getSharedPreferences("lifeforge_prefs", Context.MODE_PRIVATE)
            prefs.edit().putLong("last_spin_date", java.time.LocalDate.now().toEpochDay()).apply()
            
            vibrateSuccess()
            
            _uiState.value = _uiState.value.copy(
                coinsEarnedToday = _uiState.value.coinsEarnedToday + amount,
                canSpinToday = false,
                isLoading = false,
                lastResult = ActivityResult(
                    type = "LUCKY SPIN",
                    count = 1,
                    coinsEarned = amount
                )
            )
        }
    }
    
    private fun loadTodayStats() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            val pushups = activityRepository.getTodayCountByType(userId, "pushups")
            val steps = activityRepository.getTodayCountByType(userId, "steps")
            val squats = activityRepository.getTodayCountByType(userId, "squats")
            val coins = activityRepository.getTodayCoinsEarned(userId)
            val streak = activityRepository.calculateStreak(userId)
            
            // Also get local streak as fallback
            val prefs = context.getSharedPreferences("lifeforge_prefs", Context.MODE_PRIVATE)
            val localStreak = prefs.getInt("app_streak", 0)
            val finalStreak = maxOf(streak, localStreak)
            
            _uiState.value = ForgeUiState(
                todaySteps = steps,
                todayPushups = pushups,
                todaySquats = squats,
                coinsEarnedToday = coins,
                streakDays = finalStreak
            )
        }
    }
    
    private fun observeSteps() {
        viewModelScope.launch {
            StepCounterService.stepCount.collect { steps ->
                _uiState.value = _uiState.value.copy(todaySteps = steps)
            }
        }
    }
    
    fun logPushups(count: Int) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = activityRepository.logPushups(userId, count)
            
            result.onSuccess { coinsEarned ->
                // Haptic feedback
                vibrateSuccess()
                
                _uiState.value = _uiState.value.copy(
                    todayPushups = _uiState.value.todayPushups + count,
                    coinsEarnedToday = _uiState.value.coinsEarnedToday + coinsEarned,
                    isLoading = false,
                    lastResult = ActivityResult(
                        type = "pushups",
                        count = count,
                        coinsEarned = coinsEarned
                    )
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun logSquats(count: Int) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = activityRepository.logSquats(userId, count)
            
            result.onSuccess { coinsEarned ->
                vibrateSuccess()
                
                _uiState.value = _uiState.value.copy(
                    todaySquats = _uiState.value.todaySquats + count,
                    coinsEarnedToday = _uiState.value.coinsEarnedToday + coinsEarned,
                    isLoading = false,
                    lastResult = ActivityResult(
                        type = "squats",
                        count = count,
                        coinsEarned = coinsEarned
                    )
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun logWisdom(coins: Int) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = activityRepository.logWisdom(userId, coins)
            
            result.onSuccess { coinsEarned ->
                vibrateSuccess()
                
                _uiState.value = _uiState.value.copy(
                    coinsEarnedToday = _uiState.value.coinsEarnedToday + coinsEarned,
                    isLoading = false,
                    lastResult = ActivityResult(
                        type = "wisdom",
                        count = 1,
                        coinsEarned = coinsEarned
                    )
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun syncSteps() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Get current steps from sensor
            val currentSteps = StepCounterService.stepCount.value
            
            // Log the steps
            val result = activityRepository.logSteps(userId, currentSteps)
            
            result.onSuccess { coinsEarned ->
                if (coinsEarned > 0) {
                    vibrateSuccess()
                    
                    // Reset steps in service since they are now synced
                    StepCounterService.resetSteps()
                    
                    _uiState.value = _uiState.value.copy(
                        todaySteps = 0, // Reflect reset in UI
                        coinsEarnedToday = _uiState.value.coinsEarnedToday + coinsEarned,
                        isLoading = false,
                        lastResult = ActivityResult(
                            type = "steps",
                            count = currentSteps,
                            coinsEarned = coinsEarned
                        )
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    private fun vibrateSuccess() {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(100)
            }
        }
    }
    
    fun clearResult() {
        _uiState.value = _uiState.value.copy(lastResult = null)
    }
}
