package com.lifeforge.app.data.repository

import com.lifeforge.app.data.local.database.dao.ActivityDao
import com.lifeforge.app.data.local.database.entities.ActivitySession
import com.lifeforge.app.data.remote.ActivitySessionDto
import com.lifeforge.app.data.remote.DailyStatsDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing activity sessions (push-ups, steps, etc.).
 */
@Singleton
class ActivityRepository @Inject constructor(
    private val activityDao: ActivityDao,
    private val coinRepository: CoinRepository,
    private val authRepository: AuthRepository,
    private val postgrest: io.github.jan.supabase.postgrest.Postgrest,
    private val soundManager: com.lifeforge.app.util.SoundManager,
    private val notificationRepository: NotificationRepository
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun logPushups(userId: String, count: Int): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // ... (calc coins) ...
            val coinsEarned = count / CoinRepository.PUSHUP_TO_COINS

             // Save activity locally
            val session = ActivitySession(
                userId = userId,
                type = "pushups",
                count = count,
                coinsEarned = coinsEarned,
                isSynced = false
            )
            activityDao.insert(session)
            
            // Earn coins
            coinRepository.earnCoins(
                userId = userId,
                amount = coinsEarned,
                type = "earned_pushups",
                description = "Earned $coinsEarned LC from $count push-ups"
            )

            // Play Sound
            if (coinsEarned > 0) {
                 soundManager.playCoinSound()
                 notificationRepository.addNotification(
                     title = "Workout Complete",
                     description = "Logged $count push-ups (+$coinsEarned LC).",
                     icon = "💪",
                     category = "Coins"
                 )
            }
            
            // Try to sync to Supabase (unchanged)
            try {
                syncActivityToSupabase(userId, "pushups", count, coinsEarned)
                updateDailyStatsOnSupabase(userId, pushups = count, coins = coinsEarned)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            Result.success(coinsEarned)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logSquats(userId: String, count: Int): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val coinsEarned = count / CoinRepository.SQUATS_TO_COINS
            
            // Save activity locally
            val session = ActivitySession(
                userId = userId,
                type = "squats",
                count = count,
                coinsEarned = coinsEarned,
                isSynced = false
            )
            activityDao.insert(session)
            
            // Earn coins
            coinRepository.earnCoins(
                userId = userId,
                amount = coinsEarned,
                type = "earned_squats",
                description = "Earned $coinsEarned LC from $count squats"
            )

            // Play Sound
            if (coinsEarned > 0) {
                 soundManager.playCoinSound()
                 notificationRepository.addNotification(
                     title = "Workout Complete",
                     description = "Logged $count squats (+$coinsEarned LC).",
                     icon = "🏋️",
                     category = "Coins"
                 )
            }
            
            // Sync to Supabase
            try {
                syncActivityToSupabase(userId, "squats", count, coinsEarned)
                updateDailyStatsOnSupabase(userId, squats = count, coins = coinsEarned)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            Result.success(coinsEarned)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logSteps(userId: String, steps: Int): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // ...
             val coinsEarned = steps / CoinRepository.STEPS_TO_COINS
            
            if (coinsEarned <= 0) {
                return@withContext Result.success(0)
            }
            
            // ...
            val session = ActivitySession(
                userId = userId,
                type = "steps",
                count = steps,
                coinsEarned = coinsEarned,
                isSynced = false
            )
            activityDao.insert(session)
            
            // Earn coins
            coinRepository.earnCoins(
                userId = userId,
                amount = coinsEarned,
                type = "earned_steps",
                description = "Earned $coinsEarned LC from $steps steps"
            )

            // Avoid spamming: notify only on meaningful step milestones (>= 10 coins = 1,000 steps).
            if (coinsEarned >= 10) {
                notificationRepository.addNotification(
                    title = "Steps Synced",
                    description = "You earned +$coinsEarned LC from walking.",
                    icon = "👟",
                    category = "Coins"
                )
            }

            // Play Sound
            if (coinsEarned > 0) {
                 soundManager.playCoinSound()
            }

            // Sync
            try {
                syncActivityToSupabase(userId, "steps", steps, coinsEarned)
                updateDailyStatsOnSupabase(userId, steps = steps, coins = coinsEarned)
            } catch (e: Exception) {
                 e.printStackTrace()
            }
            
            Result.success(coinsEarned)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logWisdom(userId: String, coinsEarned: Int): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = 1 // 1 session
            
            // Save activity locally
            val session = ActivitySession(
                userId = userId,
                type = "wisdom_reading",
                count = count,
                coinsEarned = coinsEarned,
                isSynced = false
            )
            activityDao.insert(session)
            
            // Earn coins
            coinRepository.earnCoins(
                userId = userId,
                amount = coinsEarned,
                type = "earned_wisdom",
                description = "Earned $coinsEarned LC from wisdom reading"
            )

            // Play Sound
            if (coinsEarned > 0) {
                 soundManager.playCoinSound()
            }
            
            // Sync to Supabase
            try {
                syncActivityToSupabase(userId, "wisdom_reading", count, coinsEarned)
                // We don't update daily stats as there is no column for wisdom, 
                // but we might want to track coins there
                updateDailyStatsOnSupabase(userId, coins = coinsEarned)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            Result.success(coinsEarned)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get today's activities.
     */
    fun getTodayActivities(userId: String): Flow<List<ActivitySession>> {
        val startOfDay = getStartOfDayTimestamp()
        return activityDao.getTodayActivities(userId, startOfDay)
    }
    
    /**
     * Get today's count for a specific activity type.
     */
    suspend fun getTodayCountByType(userId: String, type: String): Int {
        val startOfDay = getStartOfDayTimestamp()
        return activityDao.getTodayCountByType(userId, type, startOfDay) ?: 0
    }
    
    /**
     * Get today's total coins earned.
     */
    suspend fun getTodayCoinsEarned(userId: String): Int {
        val startOfDay = getStartOfDayTimestamp()
        return activityDao.getTodayCoinsEarned(userId, startOfDay) ?: 0
    }
    
    /**
     * Calculate current streak (consecutive days with activity).
     */
    suspend fun calculateStreak(userId: String): Int = withContext(Dispatchers.IO) {
        try {
            // Fetch from Supabase
            val result = postgrest["daily_stats"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(30)
                }
                .decodeList<DailyStatsDto>()
            
            var streak = 0
            val today = dateFormat.format(Date())
            val calendar = Calendar.getInstance()
            
            val lastReset = authRepository.getLastStreakResetTimestamp()
            
            for (i in result.indices) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val expectedDate = dateFormat.format(calendar.time)
                
                // Safety: Check if this date is BEFORE the reset.
                val dateCheck = Calendar.getInstance().apply {
                    time = dateFormat.parse(expectedDate)!!
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                }.timeInMillis
                
                if (lastReset > 0 && dateCheck < lastReset) {
                     break
                }
                
                val dayStats = result.find { it.date == expectedDate }
                if (dayStats != null && (dayStats.totalSteps > 0 || dayStats.totalPushups > 0)) {
                    streak++
                } else if (i > 0) { // Allow today to be incomplete
                    break
                }
            }
            
            streak
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Get daily stats history for charts.
     * Tries Supabase first, falls back to efficient local aggregation.
     */
    suspend fun getDailyStatsHistory(userId: String, days: Int = 30): List<DailyStatsDto> = withContext(Dispatchers.IO) {
        // Try Supabase first
        val remoteData = try {
            postgrest["daily_stats"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(days.toLong())
                }
                .decodeList<DailyStatsDto>()
        } catch (e: Exception) {
            emptyList()
        }

        if (remoteData.isNotEmpty()) return@withContext remoteData
        
        // Fallback: Aggregate from local activity_sessions
        try {
            val stats = mutableListOf<DailyStatsDto>()
            val calendar = Calendar.getInstance()
            
            // Get data for the requested range (e.g. 30 days)
            calendar.time = Date()
            val endRange = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -days)
            val startRange = calendar.timeInMillis
            
            // Fetch All raw sessions in this big range (efficient enough for local DB)
            val sessions = activityDao.getActivitiesInRange(userId, startRange, endRange)
            
            // Group by Day
            val grouped = sessions.groupBy { session ->
                 val c = Calendar.getInstance()
                 c.timeInMillis = session.timestamp
                 dateFormat.format(c.time)
            }
            
            // Build DTOs
             for (i in 0 until days) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val dateStr = dateFormat.format(calendar.time)
                
                val dailySessions = grouped[dateStr] ?: emptyList()
                val totalSteps = dailySessions.filter { it.type == "steps" }.sumOf { it.count }
                val totalPushups = dailySessions.filter { it.type == "pushups" }.sumOf { it.count }
                val totalSquats = dailySessions.filter { it.type == "squats" }.sumOf { it.count }
                val totalCoins = dailySessions.sumOf { it.coinsEarned }
                
                if (totalSteps > 0 || totalPushups > 0 || totalSquats > 0) {
                     stats.add(DailyStatsDto(
                        userId = userId,
                        date = dateStr,
                        totalSteps = totalSteps,
                        totalPushups = totalPushups,
                        totalSquats = totalSquats,
                        coinsEarned = totalCoins
                     ))
                }
            }
            stats
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private suspend fun syncActivityToSupabase(
        userId: String,
        type: String,
        count: Int,
        coinsEarned: Int
    ) {
        postgrest["activity_sessions"]
            .insert(ActivitySessionDto(
                userId = userId,
                activityType = type,
                count = count,
                coinsEarned = coinsEarned
            ))
    }
    
    private suspend fun updateDailyStatsOnSupabase(
        userId: String,
        steps: Int = 0,
        pushups: Int = 0,
        squats: Int = 0,
        coins: Int = 0
    ) {
        val today = dateFormat.format(Date())
        
        // Try to get existing stats
        val existing = try {
            postgrest["daily_stats"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("date", today)
                    }
                }
                .decodeSingleOrNull<DailyStatsDto>()
        } catch (e: Exception) {
            null
        }
        
        if (existing != null) {
            // Update existing
            postgrest["daily_stats"]
                .update({
                    set("total_steps", existing.totalSteps + steps)
                    set("total_pushups", existing.totalPushups + pushups)
                    set("total_squats", existing.totalSquats + squats)
                    set("coins_earned", existing.coinsEarned + coins)
                }) {
                    filter {
                        eq("user_id", userId)
                        eq("date", today)
                    }
                }
        } else {
            // Insert new
            postgrest["daily_stats"]
                .insert(DailyStatsDto(
                    userId = userId,
                    date = today,
                    totalSteps = steps,
                    totalPushups = pushups,
                    totalSquats = squats,
                    coinsEarned = coins
                ))
        }
    }
    
    private fun getStartOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
