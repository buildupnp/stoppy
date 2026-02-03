package com.lifeforge.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.app.data.repository.ActivityRepository
import com.lifeforge.app.data.repository.AppLockRepository
import com.lifeforge.app.data.repository.AuthRepository
import com.lifeforge.app.data.repository.CoinRepository
import com.lifeforge.app.data.repository.FeatureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter

import com.lifeforge.app.util.UsageStatsHelper
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

data class ManagedAppStat(
    val packageName: String,
    val appName: String,
    val usageTimeMs: Long,
    val timeAvailableMs: Long, // Remaining unlock time
    val isBlocked: Boolean,
    val isUnlocked: Boolean
)

data class DailyQuest(
    val id: String,
    val title: String,
    val description: String,
    val reward: Int,
    val progress: Float, // 0.0 to 1.0
    val isCompleted: Boolean,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

data class DashboardUiState(
    val userName: String = "Warrior",
    val coinBalance: Int = 0,
    val screenTimeAvailable: String = "0 min",
    val todaySteps: Int = 0,
    val todayPushups: Int = 0,
    val streakDays: Int = 0,
    val managedAppStats: List<ManagedAppStat> = emptyList(),
    val dailyQuests: List<DailyQuest> = emptyList(),
    val pulseStatus: String = "Calm", // Calm, Active, On Fire, Elite
    val pulseColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF64B5F6),
    val dailyQuote: String = "Discipline is choosing between what you want now and what you want most.",
    val isLoading: Boolean = false,
    val weeklySavedMinutes: List<Int> = listOf(45, 60, 30, 90, 120, 80, 55), // Mock data for last 7 days
    // Login bonus
    val showLoginBonus: Boolean = false,
    val loginBonusAmount: Int = 0,
    val loginStreakDays: Int = 0,
    // Diagnostics
    val isServiceRunning: Boolean = false,
    val currentlyTrackedApp: String? = null,
    val managedAppsCount: Int = 0,
    val hasUnreadNotifications: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val coinRepository: CoinRepository,
    private val activityRepository: ActivityRepository,
    private val appLockRepository: AppLockRepository,
    private val featureRepository: FeatureRepository,
    private val usageStatsHelper: UsageStatsHelper,
    private val notificationRepository: com.lifeforge.app.data.repository.NotificationRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        observeUnreadNotifications() // New Observation
        updateLocalStreak()
        loadDashboardData()
        observeData()
        startTimerTicker()
        checkLoginBonus()
        initializeFeatures()
        viewModelScope.launch {
            appLockRepository.mergeDuplicateUnlocks()
        }
    }
    
// Store unlocks locally for frequent UI updates
            private var currentUnlocks: List<com.lifeforge.app.data.local.database.entities.AppUnlock> = emptyList()

            private fun startTimerTicker() {
                viewModelScope.launch {
                    while (true) {
                        delay(2000) // Refresh every 2 seconds for a snappier countdown feel
                        refreshAppStats()
                    }
                }
            }

            private fun refreshAppStats() {
                val currentState = _uiState.value
                val currentTime = System.currentTimeMillis()
                
                // Only update if we have managed apps
                if (currentState.managedAppStats.isEmpty()) return

                val updatedStats = currentState.managedAppStats.map { stat ->
                    val unlockInfo = currentUnlocks.find { it.packageName == stat.packageName }
                    val isUnlocked = unlockInfo != null && (
                        (unlockInfo.isUsageBased && unlockInfo.remainingTimeMs > 0) ||
                        (!unlockInfo.isUsageBased && unlockInfo.expiresAt > currentTime)
                    )
                    val timeAvailable = if (isUnlocked) {
                        if (unlockInfo!!.isUsageBased) unlockInfo.remainingTimeMs 
                        else (unlockInfo.expiresAt - currentTime).coerceAtLeast(0)
                    } else {
                        0L
                    }
                    stat.copy(
                        timeAvailableMs = timeAvailable,
                        isUnlocked = isUnlocked
                    )
                }
                
                _uiState.value = currentState.copy(managedAppStats = updatedStats)
            }

    private fun updateLocalStreak() {
        val prefs = context.getSharedPreferences("lifeforge_prefs", android.content.Context.MODE_PRIVATE)
        val lastUpdateEpoch = prefs.getLong("last_streak_update", 0L)
        var currentStreak = prefs.getInt("app_streak", 0)
        
        val today = java.time.LocalDate.now()
        val todayEpoch = today.toEpochDay()
        
        if (lastUpdateEpoch == 0L) {
            // First time opening the app
            currentStreak = 1
        } else {
            val lastDate = java.time.LocalDate.ofEpochDay(lastUpdateEpoch)
            val diff = todayEpoch - lastUpdateEpoch
            
            if (diff == 1L) {
                // Consecutive day
                currentStreak++
            } else if (diff > 1L) {
                // Streak broken
                currentStreak = 1
            }
            // If diff == 0, it's the same day, don't change streak
        }
        
        prefs.edit()
            .putLong("last_streak_update", todayEpoch)
            .putInt("app_streak", currentStreak)
            .apply()
            
        _uiState.value = _uiState.value.copy(streakDays = currentStreak)
    }

    private fun getPulseStatus(steps: Int, pushups: Int): Pair<String, androidx.compose.ui.graphics.Color> {
        return when {
            steps > 8000 || pushups > 40 -> "Elite" to androidx.compose.ui.graphics.Color(0xFFF50057) // Pink-Red
            steps > 4000 || pushups > 20 -> "On Fire" to androidx.compose.ui.graphics.Color(0xFFFF9100) // Orange
            steps > 1000 || pushups > 5 -> "Active" to androidx.compose.ui.graphics.Color(0xFF00E676) // Green
            else -> "Calm" to androidx.compose.ui.graphics.Color(0xFF64B5F6) // Blue
        }
    }

    private fun getDailyQuests(steps: Int, pushups: Int, wisdom: Int): List<DailyQuest> {
        return listOf(
            DailyQuest(
                id = "steps_1",
                title = "Walker's Path",
                description = "Reach 5,000 steps",
                reward = 20,
                progress = (steps / 5000f).coerceAtMost(1f),
                isCompleted = steps >= 5000,
                icon = Icons.Default.DirectionsWalk
            ),
            DailyQuest(
                id = "pushups_1",
                title = "Forge Strength",
                description = "Complete 20 pushups",
                reward = 15,
                progress = (pushups / 20f).coerceAtMost(1f),
                isCompleted = pushups >= 20,
                icon = Icons.Default.FitnessCenter
            ),
            DailyQuest(
                id = "wisdom_1",
                title = "Seeker of Truth",
                description = "Read 1 wisdom lesson",
                reward = 10,
                progress = (wisdom / 1f).coerceAtMost(1f),
                isCompleted = wisdom >= 1,
                icon = Icons.Default.AutoStories
            )
        )
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                // Ensure bonus coins are granted (safe check)
                authRepository.ensureInitialBalance(userId)
                
                val pushups = activityRepository.getTodayCountByType(userId, "pushups")
                val steps = activityRepository.getTodayCountByType(userId, "steps")
                val wisdom = activityRepository.getTodayCountByType(userId, "wisdom_reading")
                
                // Get Activity Streak but fallback to Local Streak if Activity Streak is 0
                val activityStreak = activityRepository.calculateStreak(userId)
                val currentLocalStreak = _uiState.value.streakDays
                val finalStreak = maxOf(activityStreak, currentLocalStreak)
                
                val (status, color) = getPulseStatus(steps, pushups)
                val quests = getDailyQuests(steps, pushups, wisdom)

                _uiState.value = _uiState.value.copy(
                    todayPushups = pushups,
                    todaySteps = steps,
                    streakDays = finalStreak,
                    pulseStatus = status,
                    pulseColor = color,
                    dailyQuests = quests,
                    dailyQuote = getRandomQuote()
                )
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    private fun refreshStats() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val pushups = activityRepository.getTodayCountByType(userId, "pushups")
            val steps = activityRepository.getTodayCountByType(userId, "steps")
            val wisdom = activityRepository.getTodayCountByType(userId, "wisdom_reading")
            
            val (status, color) = getPulseStatus(steps, pushups)
            val quests = getDailyQuests(steps, pushups, wisdom)

            _uiState.value = _uiState.value.copy(
                todayPushups = pushups,
                todaySteps = steps,
                pulseStatus = status,
                pulseColor = color,
                dailyQuests = quests
            )
        }
    }

    private fun getRandomQuote(): String {
        val quotes = listOf(
            "Discipline is choosing between what you want now and what you want most.",
            "The pain of discipline is far less than the pain of regret.",
            "Focus on the process, not the outcome.",
            "You are what you consistently do.",
            "Small steps every day add up to big results.",
            "Distraction is the enemy of progress.",
            "Master your time, master your life."
        )
        return quotes.random()
    }

            fun refreshData() = loadDashboardData()

            private fun observeData() {
                 viewModelScope.launch {
                    val userId = authRepository.getCurrentUserId() ?: return@launch
                    
                    combine(
                        coinRepository.getBalanceFlow(userId),
                        appLockRepository.getAllManagedApps(),
                        appLockRepository.getActiveUnlocks()
                    ) { balance, managedApps, activeUnlocks ->
                        Triple(balance, managedApps, activeUnlocks)
                    }.collectLatest { (balance, managedApps, activeUnlocks) ->
                        
                        currentUnlocks = activeUnlocks // Update local cache
                        val currentTime = System.currentTimeMillis()
                        
                        // Calculate total active screen time available across all apps
                        val totalTimeAvailableMs = activeUnlocks
                            .map { 
                                if (it.isUsageBased) it.remainingTimeMs 
                                else (it.expiresAt - currentTime).coerceAtLeast(0) 
                            }
                            .sumOf { it }
                        
                        val minutes = (totalTimeAvailableMs / (1000 * 60)).toInt()
                        val screenTime = if (minutes > 0) "${minutes} min" else "0 min"
                        
                        val packageNames = managedApps.map { it.packageName }
                        val usageMap = usageStatsHelper.getTodayUsage(packageNames)
                        
                        val appStats = managedApps
                            .filter { it.isBlocked } // Only show apps that are currently ENABLED for blocking
                            .map { app ->
                                val unlockInfo = activeUnlocks.find { it.packageName == app.packageName }
                                val isUnlocked = unlockInfo != null && (
                                    (unlockInfo.isUsageBased && unlockInfo.remainingTimeMs > 0) ||
                                    (!unlockInfo.isUsageBased && unlockInfo.expiresAt > currentTime)
                                )
                                val timeAvailable = if (isUnlocked) {
                                    if (unlockInfo!!.isUsageBased) unlockInfo.remainingTimeMs 
                                    else unlockInfo.expiresAt - currentTime
                                } else {
                                    0L
                                }
                                
                                ManagedAppStat(
                                    packageName = app.packageName,
                                    appName = app.appName,
                                    usageTimeMs = usageMap[app.packageName] ?: 0L,
                                    timeAvailableMs = timeAvailable,
                                    isBlocked = app.isBlocked,
                                    isUnlocked = isUnlocked
                                )
                            }
                        
                        _uiState.value = _uiState.value.copy(
                            coinBalance = balance,
                            screenTimeAvailable = screenTime,
                            managedAppStats = appStats
                        )
                        
                        // Also trigger stats refresh when activity might have changed
                        refreshStats()
                    }
                }
        
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { userState ->
                if (userState is AuthRepository.UserState.Authenticated) {
                    val name = userState.fullName
                    _uiState.value = _uiState.value.copy(
                        userName = if (name.isNullOrBlank()) "Warrior" else name
                    )
                }
            }
        }

        viewModelScope.launch {
            com.lifeforge.app.accessibility.AppDetectorService.isRunning.collect { running ->
                _uiState.value = _uiState.value.copy(isServiceRunning = running)
            }
        }
        
        viewModelScope.launch {
            com.lifeforge.app.accessibility.AppDetectorService.currentlyTracked.collect { tracked ->
                _uiState.value = _uiState.value.copy(currentlyTrackedApp = tracked)
            }
        }
        
        viewModelScope.launch {
            com.lifeforge.app.accessibility.AppDetectorService.registeredAppsCount.collect { count ->
                _uiState.value = _uiState.value.copy(managedAppsCount = count)
            }
        }
    }
    
    private fun checkLoginBonus() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val bonusAmount = featureRepository.checkAndGrantLoginBonus(userId)
            if (bonusAmount > 0) {
                val (streakDays, _) = featureRepository.getLoginStreakInfo(userId)
                _uiState.value = _uiState.value.copy(
                    showLoginBonus = true,
                    loginBonusAmount = bonusAmount,
                    loginStreakDays = streakDays
                )
            }
        }
    }
    
    private fun initializeFeatures() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            // Initialize achievements if not already done
            featureRepository.initializeAchievements(userId)
            // Generate weekly challenges if needed
            featureRepository.generateWeeklyChallenges(userId)
        }
    }
    
    fun dismissLoginBonus() {
        _uiState.value = _uiState.value.copy(showLoginBonus = false)
    }

    private fun observeUnreadNotifications() {
        viewModelScope.launch {
            notificationRepository.unreadCount.collectLatest { count ->
                _uiState.value = _uiState.value.copy(hasUnreadNotifications = count > 0)
            }
        }
    }
}

