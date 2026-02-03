package com.lifeforge.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.app.data.local.database.entities.ActivitySession
import com.lifeforge.app.data.remote.DailyStatsDto
import com.lifeforge.app.data.repository.ActivityRepository
import com.lifeforge.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatisticsUiState(
    val isLoading: Boolean = false,
    val dailyStats: List<DailyStatsDto> = emptyList(),
    val todaySessions: List<ActivitySession> = emptyList(),
    val selectedTimeframe: Timeframe = Timeframe.WEEKLY,
    val totalStepsAllTime: Int = 0,
    val totalPushupsAllTime: Int = 0,
    val totalSquatsAllTime: Int = 0,
    val error: String? = null
)

enum class Timeframe {
    DAILY, WEEKLY, ALL_TIME
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                // 1. Fetch History (Last 30 days)
                val history = activityRepository.getDailyStatsHistory(userId, 30)
                
                // 2. Fetch Today's Sessions (for Daily Breakdown)
                // Note: getTodayActivities returns a Flow, we need to collect it or just get one-shot.
                // The repo function returns Flow. For simplicity in this VM loop, we'll launch a separate collector or just use the history for now.
                // Ideally we want real-time updates for "Daily".
                
                launch {
                    activityRepository.getTodayActivities(userId).collect { sessions ->
                        _uiState.update { cit -> cit.copy(todaySessions = sessions) }
                    }
                }

                // Calculate Totals (Sum of history + potentially what's in local DB if not yet synced, but history from Supabase is good enough for "All Time" approximation for now)
                val totalSteps = history.sumOf { it.totalSteps }
                val totalPushups = history.sumOf { it.totalPushups }
                val totalSquats = history.sumOf { it.totalSquats }

                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        dailyStats = history,
                        totalStepsAllTime = totalSteps,
                        totalPushupsAllTime = totalPushups,
                        totalSquatsAllTime = totalSquats
                    ) 
                }
            } else {
                 _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
            }
        }
    }
    
    fun setTimeframe(timeframe: Timeframe) {
        _uiState.update { it.copy(selectedTimeframe = timeframe) }
    }
}
