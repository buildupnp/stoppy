package com.lifeforge.app.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_sessions")
data class ActivitySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val type: String, // "pushups", "steps", "squats", etc.
    val count: Int,
    val coinsEarned: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "coin_transactions")
data class CoinTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val amount: Int, // positive for earn, negative for spend
    val type: String, // "earned_pushups", "earned_steps", "spent_unlock", etc.
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "managed_apps")
data class ManagedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "app_unlocks")
data class AppUnlock(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val durationMinutes: Int,
    val coinsCost: Int,
    val startedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val remainingTimeMs: Long = 0, // For usage-based tracking
    val isUsageBased: Boolean = true,
    val isSynced: Boolean = false
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val iconName: String, // Material icon name
    val category: String, // "fitness", "streak", "guardian", "wisdom"
    val requiredValue: Int, // e.g., 100 for "100 pushups"
    val currentValue: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val coinReward: Int = 0
)

@Entity(tableName = "weekly_challenges")
data class WeeklyChallenge(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val targetValue: Int,
    val currentValue: Int = 0,
    val coinReward: Int,
    val type: String, // "steps", "pushups", "wisdom", "app_block"
    val startDate: Long, // Monday of the week
    val endDate: Long, // Sunday of the week
    val isCompleted: Boolean = false,
    val isRewardClaimed: Boolean = false
)

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val id: String = "default",
    val userId: String,
    val themeMode: String = "dark", // "dark", "light", "system"
    val notificationsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val streakRemindersEnabled: Boolean = true,
    val motivationalQuotesEnabled: Boolean = true,
    val lastLoginDate: Long = 0,
    val consecutiveLoginDays: Int = 0,
    val lastLoginBonusClaimed: Long = 0
)

