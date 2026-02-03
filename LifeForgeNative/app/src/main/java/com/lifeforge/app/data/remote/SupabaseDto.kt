package com.lifeforge.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Objects for Supabase API.
 * These match your existing Supabase table schemas.
 */

@Serializable
data class UserProfileDto(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("full_name")
    val fullName: String?,
    val email: String?,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class CoinBalanceDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    val balance: Int,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class CoinTransactionDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    val amount: Int,
    val type: String,
    val description: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class ActivitySessionDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("activity_type")
    val activityType: String,
    val count: Int,
    @SerialName("coins_earned")
    val coinsEarned: Int,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class DailyStatsDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    val date: String,
    @SerialName("total_steps")
    val totalSteps: Int = 0,
    @SerialName("total_pushups")
    val totalPushups: Int = 0,
    @SerialName("total_squats")
    val totalSquats: Int = 0,
    @SerialName("coins_earned")
    val coinsEarned: Int = 0
)

@Serializable
data class ManagedAppDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("app_name")
    val appName: String,
    @SerialName("package_name")
    val packageName: String,
    @SerialName("icon_url")
    val iconUrl: String? = null,
    @SerialName("is_blocked")
    val isBlocked: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class AppUnlockDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("app_id")
    val appId: String,
    @SerialName("app_name")
    val appName: String,
    @SerialName("duration_minutes")
    val durationMinutes: Int,
    @SerialName("coins_spent")
    val coinsSpent: Int,
    @SerialName("expires_at")
    val expiresAt: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class UserSettingsDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("notifications_enabled")
    val notificationsEnabled: Boolean = true,
    @SerialName("haptics_enabled")
    val hapticsEnabled: Boolean = true,
    @SerialName("sound_enabled")
    val soundEnabled: Boolean = true
)
