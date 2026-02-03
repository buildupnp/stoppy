package com.lifeforge.app.data.local.database.dao

import androidx.room.*
import com.lifeforge.app.data.local.database.entities.Achievement
import com.lifeforge.app.data.local.database.entities.UserPreferences
import com.lifeforge.app.data.local.database.entities.WeeklyChallenge
import kotlinx.coroutines.flow.Flow

/**
 * DAO for achievements, challenges, and user preferences.
 */
@Dao
interface FeatureDao {

    // ============ Achievements ============
    
    @Query("SELECT * FROM achievements WHERE userId = :userId")
    fun getAllAchievements(userId: String): Flow<List<Achievement>>
    
    @Query("SELECT * FROM achievements WHERE userId = :userId AND isUnlocked = 1")
    fun getUnlockedAchievements(userId: String): Flow<List<Achievement>>
    
    @Query("SELECT * FROM achievements WHERE id = :id AND userId = :userId")
    suspend fun getAchievementById(id: String, userId: String): Achievement?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<Achievement>)
    
    @Update
    suspend fun updateAchievement(achievement: Achievement)
    
    @Query("UPDATE achievements SET currentValue = :value, isUnlocked = :isUnlocked, unlockedAt = :unlockedAt WHERE id = :id AND userId = :userId")
    suspend fun updateAchievementProgress(id: String, userId: String, value: Int, isUnlocked: Boolean, unlockedAt: Long?)
    
    // ============ Weekly Challenges ============
    
    @Query("SELECT * FROM weekly_challenges WHERE userId = :userId AND endDate > :currentTime ORDER BY startDate DESC")
    fun getActiveChallenges(userId: String, currentTime: Long): Flow<List<WeeklyChallenge>>
    
    @Query("SELECT * FROM weekly_challenges WHERE userId = :userId ORDER BY startDate DESC LIMIT 10")
    fun getRecentChallenges(userId: String): Flow<List<WeeklyChallenge>>
    
    @Query("SELECT * FROM weekly_challenges WHERE id = :id AND userId = :userId")
    suspend fun getChallengeById(id: String, userId: String): WeeklyChallenge?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: WeeklyChallenge)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenges(challenges: List<WeeklyChallenge>)
    
    @Update
    suspend fun updateChallenge(challenge: WeeklyChallenge)
    
    @Query("UPDATE weekly_challenges SET currentValue = :value, isCompleted = :isCompleted WHERE id = :id AND userId = :userId")
    suspend fun updateChallengeProgress(id: String, userId: String, value: Int, isCompleted: Boolean)
    
    @Query("UPDATE weekly_challenges SET isRewardClaimed = 1 WHERE id = :id AND userId = :userId")
    suspend fun markRewardClaimed(id: String, userId: String)
    
    // ============ User Preferences ============
    
    @Query("SELECT * FROM user_preferences WHERE userId = :userId LIMIT 1")
    suspend fun getPreferences(userId: String): UserPreferences?
    
    @Query("SELECT * FROM user_preferences WHERE userId = :userId LIMIT 1")
    fun getPreferencesFlow(userId: String): Flow<UserPreferences?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(preferences: UserPreferences)
    
    @Update
    suspend fun updatePreferences(preferences: UserPreferences)
    
    @Query("UPDATE user_preferences SET themeMode = :themeMode WHERE userId = :userId")
    suspend fun updateTheme(userId: String, themeMode: String)
    
    @Query("UPDATE user_preferences SET notificationsEnabled = :enabled WHERE userId = :userId")
    suspend fun updateNotifications(userId: String, enabled: Boolean)
    
    @Query("UPDATE user_preferences SET hapticsEnabled = :enabled WHERE userId = :userId")
    suspend fun updateHaptics(userId: String, enabled: Boolean)
    
    @Query("UPDATE user_preferences SET soundEnabled = :enabled WHERE userId = :userId")
    suspend fun updateSound(userId: String, enabled: Boolean)
    
    @Query("UPDATE user_preferences SET lastLoginDate = :date, consecutiveLoginDays = :days WHERE userId = :userId")
    suspend fun updateLoginStreak(userId: String, date: Long, days: Int)
    
    @Query("UPDATE user_preferences SET lastLoginBonusClaimed = :timestamp WHERE userId = :userId")
    suspend fun updateLastLoginBonusClaimed(userId: String, timestamp: Long)
}
