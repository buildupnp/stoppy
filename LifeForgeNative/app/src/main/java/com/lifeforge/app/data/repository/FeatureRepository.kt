package com.lifeforge.app.data.repository

import com.lifeforge.app.data.local.database.dao.FeatureDao
import com.lifeforge.app.data.local.database.entities.Achievement
import com.lifeforge.app.data.local.database.entities.UserPreferences
import com.lifeforge.app.data.local.database.entities.WeeklyChallenge
import com.lifeforge.app.data.model.DailyQuest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing achievements, weekly challenges, daily login bonus, and user preferences.
 */
@Singleton
class FeatureRepository @Inject constructor(
    private val featureDao: FeatureDao,
    private val coinRepository: CoinRepository,
    private val notificationRepository: NotificationRepository
) {

    companion object {
        // Daily login bonus amounts (by consecutive day)
        // User requested 67 total on first day. Signup gets 67, so Day 1 login is 0.
        // Baseline 62 starts from Day 2.
        val LOGIN_BONUS_AMOUNTS = listOf(0, 62, 70, 75, 80, 90, 100) // Day 1-7 (Day 1 is 0 because of signup bonus)
        const val MAX_LOGIN_STREAK = 7
        
        // Predefined achievements
        val DEFAULT_ACHIEVEMENTS = listOf(
            AchievementDef("first_steps", "First Steps", "Complete your first 100 steps", "DirectionsWalk", "fitness", 100, 10),
            AchievementDef("marathon_walker", "Marathon Walker", "Walk 10,000 steps in a day", "DirectionsRun", "fitness", 10000, 50),
            AchievementDef("pushup_beginner", "Pushup Beginner", "Complete 50 pushups total", "FitnessCenter", "fitness", 50, 15),
            AchievementDef("pushup_master", "Pushup Master", "Complete 500 pushups total", "EmojiEvents", "fitness", 500, 100),
            AchievementDef("week_warrior", "Week Warrior", "Maintain a 7-day streak", "LocalFireDepartment", "streak", 7, 50),
            AchievementDef("month_champion", "Month Champion", "Maintain a 30-day streak", "Star", "streak", 30, 200),
            AchievementDef("first_block", "Digital Guardian", "Block your first app", "Shield", "guardian", 1, 10),
            AchievementDef("focus_master", "Focus Master", "Block 5 apps", "Security", "guardian", 5, 30),
            AchievementDef("wisdom_seeker", "Wisdom Seeker", "Complete 5 wisdom lessons", "AutoStories", "wisdom", 5, 25),
            AchievementDef("enlightened", "Enlightened One", "Complete 20 wisdom lessons", "Lightbulb", "wisdom", 20, 75)
        )
    }
    
    data class AchievementDef(
        val id: String,
        val title: String,
        val description: String,
        val iconName: String,
        val category: String,
        val requiredValue: Int,
        val coinReward: Int
    )

    // ============ Daily Login Bonus ============
    
    /**
     * Check and grant daily login bonus.
     * Returns the bonus amount if granted, 0 if already claimed today.
     */
    suspend fun checkAndGrantLoginBonus(userId: String): Int = withContext(Dispatchers.IO) {
        val prefs = featureDao.getPreferences(userId) ?: run {
            // Create default preferences
            val newPrefs = UserPreferences(userId = userId)
            featureDao.insertPreferences(newPrefs)
            newPrefs
        }
        
        val today = LocalDate.now()
        val todayEpoch = today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        val lastLoginEpoch = prefs.lastLoginDate
        val lastLoginDate = if (lastLoginEpoch > 0) {
            java.time.Instant.ofEpochMilli(lastLoginEpoch).atZone(ZoneId.systemDefault()).toLocalDate()
        } else null
        
        // Already claimed today
        if (lastLoginDate == today) {
            return@withContext 0
        }
        
        // Calculate new streak
        val newConsecutiveDays = when {
            lastLoginDate == null -> 1
            lastLoginDate.plusDays(1) == today -> {
                // Consecutive day
                if (prefs.consecutiveLoginDays >= MAX_LOGIN_STREAK) 1 else prefs.consecutiveLoginDays + 1
            }
            else -> 1 // Streak broken
        }
        
        // Get bonus amount
        val bonusIndex = (newConsecutiveDays - 1).coerceIn(0, LOGIN_BONUS_AMOUNTS.size - 1)
        val bonusAmount = LOGIN_BONUS_AMOUNTS[bonusIndex]
        
        // Update preferences
        featureDao.updateLoginStreak(userId, todayEpoch, newConsecutiveDays)
        featureDao.updateLastLoginBonusClaimed(userId, System.currentTimeMillis())
        
        // Grant coins
        coinRepository.earnCoins(
            userId = userId,
            amount = bonusAmount,
            type = "login_bonus",
            description = "Day $newConsecutiveDays login bonus"
        )
        
        bonusAmount
    }
    
    /**
     * Get current login streak info.
     */
    suspend fun getLoginStreakInfo(userId: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val prefs = featureDao.getPreferences(userId)
        val consecutiveDays = prefs?.consecutiveLoginDays ?: 0
        val nextBonusIndex = consecutiveDays.coerceIn(0, LOGIN_BONUS_AMOUNTS.size - 1)
        val nextBonus = LOGIN_BONUS_AMOUNTS[nextBonusIndex]
        Pair(consecutiveDays, nextBonus)
    }

    // ============ Achievements ============
    
    /**
     * Initialize achievements for a user.
     */
    suspend fun initializeAchievements(userId: String) = withContext(Dispatchers.IO) {
        val existing = featureDao.getAllAchievements(userId)
        // This is a flow, so we just insert if not exists
        DEFAULT_ACHIEVEMENTS.forEach { def ->
            val existingAchievement = featureDao.getAchievementById(def.id, userId)
            if (existingAchievement == null) {
                featureDao.insertAchievement(
                    Achievement(
                        id = def.id,
                        userId = userId,
                        title = def.title,
                        description = def.description,
                        iconName = def.iconName,
                        category = def.category,
                        requiredValue = def.requiredValue,
                        coinReward = def.coinReward
                    )
                )
            }
        }
    }
    
    fun getAllAchievements(userId: String): Flow<List<Achievement>> {
        return featureDao.getAllAchievements(userId)
    }
    
    fun getUnlockedAchievements(userId: String): Flow<List<Achievement>> {
        return featureDao.getUnlockedAchievements(userId)
    }
    
    /**
     * Update achievement progress and check if unlocked.
     * Returns coin reward if newly unlocked, 0 otherwise.
     */
    suspend fun updateAchievementProgress(
        userId: String,
        achievementId: String,
        newValue: Int
    ): Int = withContext(Dispatchers.IO) {
        val achievement = featureDao.getAchievementById(achievementId, userId) ?: return@withContext 0
        
        if (achievement.isUnlocked) return@withContext 0
        
        val isNowUnlocked = newValue >= achievement.requiredValue
        val unlockedAt = if (isNowUnlocked) System.currentTimeMillis() else null
        
        featureDao.updateAchievementProgress(
            id = achievementId,
            userId = userId,
            value = newValue,
            isUnlocked = isNowUnlocked,
            unlockedAt = unlockedAt
        )
        
        if (isNowUnlocked && achievement.coinReward > 0) {
            coinRepository.earnCoins(
                userId = userId,
                amount = achievement.coinReward,
                type = "achievement",
                description = "Achievement: ${achievement.title}"
            )
            notificationRepository.addNotification(
                com.lifeforge.app.ui.screens.notifications.NotificationItem(
                    title = "Achievement Unlocked!",
                    description = "You earned ${achievement.coinReward} coins: ${achievement.title}",
                    icon = "🏆",
                    time = "Just now",
                    category = "Achievement"
                )
            )
            return@withContext achievement.coinReward
        }
        
        0
    }

    // ============ Weekly Challenges ============
    
    /**
     * Generate weekly challenges for the current week.
     */
    suspend fun generateWeeklyChallenges(userId: String) = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        
        val startEpoch = monday.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        val endEpoch = sunday.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000
        
        val weekId = "${monday.year}-W${monday.get(java.time.temporal.WeekFields.ISO.weekOfYear())}"
        
        // Check if challenges already exist for this week
        val existingSteps = featureDao.getChallengeById("${weekId}_steps", userId)
        if (existingSteps != null) return@withContext // Already generated
        
        val challenges = listOf(
            WeeklyChallenge(
                id = "${weekId}_steps",
                userId = userId,
                title = "Weekly Walker",
                description = "Walk 50,000 steps this week",
                targetValue = 50000,
                coinReward = 100,
                type = "steps",
                startDate = startEpoch,
                endDate = endEpoch
            ),
            WeeklyChallenge(
                id = "${weekId}_pushups",
                userId = userId,
                title = "Strength Week",
                description = "Complete 200 pushups this week",
                targetValue = 200,
                coinReward = 75,
                type = "pushups",
                startDate = startEpoch,
                endDate = endEpoch
            ),
            WeeklyChallenge(
                id = "${weekId}_wisdom",
                userId = userId,
                title = "Wisdom Week",
                description = "Complete 7 wisdom lessons this week",
                targetValue = 7,
                coinReward = 50,
                type = "wisdom",
                startDate = startEpoch,
                endDate = endEpoch
            )
        )
        
        featureDao.insertChallenges(challenges)
    }
    
    fun getActiveChallenges(userId: String): Flow<List<WeeklyChallenge>> {
        return featureDao.getActiveChallenges(userId, System.currentTimeMillis())
    }
    
    /**
     * Update challenge progress.
     */
    suspend fun updateChallengeProgress(
        userId: String,
        challengeId: String,
        newValue: Int
    ) = withContext(Dispatchers.IO) {
        val challenge = featureDao.getChallengeById(challengeId, userId) ?: return@withContext
        val isCompleted = newValue >= challenge.targetValue
        featureDao.updateChallengeProgress(challengeId, userId, newValue, isCompleted)
    }
    
    /**
     * Claim challenge reward.
     */
    suspend fun claimChallengeReward(userId: String, challengeId: String): Int = withContext(Dispatchers.IO) {
        val challenge = featureDao.getChallengeById(challengeId, userId) ?: return@withContext 0
        
        if (!challenge.isCompleted || challenge.isRewardClaimed) return@withContext 0
        
        featureDao.markRewardClaimed(challengeId, userId)
        
        coinRepository.earnCoins(
            userId = userId,
            amount = challenge.coinReward,
            type = "weekly_challenge",
            description = "Challenge: ${challenge.title}"
        )
        
        challenge.coinReward
    }

    // ============ Daily Quests ============
    
    fun getDailyQuests(userId: String): Flow<List<DailyQuest>> = flow {
        // In a real app, track this in DB. For now return static list.
        val quests = listOf(
            DailyQuest(
                id = "pushups_1",
                title = "Morning Pump",
                description = "Do 20 pushups",
                reward = 10,
                progress = 0f,
                isCompleted = false,
                type = "fitness"
            ),
             DailyQuest(
                id = "wisdom_1",
                title = "Wisdom Seeker",
                description = "Read a lesson",
                reward = 20,
                progress = 0f,
                isCompleted = false,
                type = "wisdom"
            )
        )
        emit(quests)
    }

    // ============ User Preferences ============
    
    fun getPreferencesFlow(userId: String): Flow<UserPreferences?> {
        return featureDao.getPreferencesFlow(userId)
    }
    
    suspend fun getPreferences(userId: String): UserPreferences? = withContext(Dispatchers.IO) {
        featureDao.getPreferences(userId)
    }
    
    suspend fun updateTheme(userId: String, themeMode: String) = withContext(Dispatchers.IO) {
        val prefs = featureDao.getPreferences(userId)
        if (prefs == null) {
            featureDao.insertPreferences(UserPreferences(userId = userId, themeMode = themeMode))
        } else {
            featureDao.updateTheme(userId, themeMode)
        }
    }
    
    suspend fun updateNotifications(userId: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        featureDao.updateNotifications(userId, enabled)
    }
    
    suspend fun updateHaptics(userId: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        featureDao.updateHaptics(userId, enabled)
    }
    
    suspend fun updateSound(userId: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        featureDao.updateSound(userId, enabled)
    }
    
    suspend fun updatePreferences(preferences: UserPreferences) = withContext(Dispatchers.IO) {
        featureDao.updatePreferences(preferences)
    }
}
