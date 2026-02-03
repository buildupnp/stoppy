package com.lifeforge.app.util

import com.lifeforge.app.data.local.database.dao.ActivityDao
import com.lifeforge.app.data.local.database.dao.AppLockDao
import com.lifeforge.app.data.local.database.dao.CoinDao
import com.lifeforge.app.data.local.database.entities.ActivitySession
import com.lifeforge.app.data.local.database.entities.AppUnlock
import com.lifeforge.app.data.local.database.entities.ManagedAppEntity
import com.lifeforge.app.data.repository.CoinRepository
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class DemoDataSeeder @Inject constructor(
    private val activityDao: ActivityDao,
    private val appLockDao: AppLockDao,
    private val coinDao: CoinDao
) {

    suspend fun seedDemoData(userId: String) {
        val calendar = Calendar.getInstance()
        
        // 1. Seed Activities (Last 6 Months)
        val activities = mutableListOf<ActivitySession>()
        val types = listOf("steps", "pushups", "squats")
        
        // Go back 180 days
        for (i in 0..180) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = calendar.timeInMillis
            
            // Randomly skip some days to look real
            if (Random.nextFloat() > 0.8) continue
            
            // Steps
            val steps = Random.nextInt(2000, 15000)
            val stepsCoins = steps / CoinRepository.STEPS_TO_COINS
            activities.add(
                ActivitySession(
                    userId = userId,
                    type = "steps",
                    count = steps,
                    coinsEarned = stepsCoins,
                    timestamp = date,
                    isSynced = false
                )
            )
            
            // Pushups (Maybe 3 times a week?)
            if (Random.nextFloat() > 0.4) {
                val pushups = Random.nextInt(10, 60)
                val pushupsCoins = pushups / CoinRepository.PUSHUP_TO_COINS
                activities.add(
                    ActivitySession(
                        userId = userId,
                        type = "pushups",
                        count = pushups,
                        coinsEarned = pushupsCoins,
                        timestamp = date,
                        isSynced = false
                    )
                )
            }
            
            // Squats
             if (Random.nextFloat() > 0.6) {
                val squats = Random.nextInt(10, 40)
                val squatsCoins = squats / CoinRepository.SQUATS_TO_COINS
                activities.add(
                    ActivitySession(
                        userId = userId,
                        type = "squats",
                        count = squats,
                        coinsEarned = squatsCoins,
                        timestamp = date,
                        isSynced = false
                    )
                )
            }
        }
        
        // Insert all activities
        activities.forEach { activityDao.insert(it) }
        
        // 2. Seed Managed Apps
        val apps = listOf(
            ManagedAppEntity(packageName = "com.instagram.android", appName = "Instagram", isBlocked = true),
            ManagedAppEntity(packageName = "com.zhiliaoapp.musically", appName = "TikTok", isBlocked = true),
            ManagedAppEntity(packageName = "com.google.android.youtube", appName = "YouTube", isBlocked = true)
        )
        apps.forEach { appLockDao.insertApp(it) }
        
        // 3. Seed Unlocks (Recent history)
        // Simulate some unlocks in the last few days
        for (i in 0..5) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = calendar.timeInMillis
            
            if (Random.nextBoolean()) {
                val app = apps.random()
                val duration = arrayOf(15, 30, 45).random()
                val cost = CoinRepository.UNLOCK_COSTS[duration] ?: 10
                
                appLockDao.insertUnlock(
                    AppUnlock(
                        packageName = app.packageName,
                        appName = app.appName,
                        durationMinutes = duration,
                        coinsCost = cost,
                        startedAt = date,
                        expiresAt = date + (duration * 60 * 1000),
                        remainingTimeMs = 0, // Expired
                        isUsageBased = true,
                        isSynced = false
                    )
                )
            }
        }
        
        // 4. Give some coins balance
        // Add a substantial demo bonus
        coinDao.insert(
            com.lifeforge.app.data.local.database.entities.CoinTransaction(
                userId = userId,
                amount = 5000,
                type = "demo_bonus",
                description = "Demo Mode Bonus",
                timestamp = System.currentTimeMillis(),
                isSynced = false
            )
        )
        
        // ... Logic is fine, just rely on the activities we added to show charts. 
        // We might want to "force" the balance in UI to be high.
        // The ActivityRepository sums up DB or Supabase. 
        // Note: The stats screen pulls from Supabase daily_stats. 
        // PROBLEM: My charts use `getDailyStatsHistory` which queries Supabase, NOT local DAO.
        // FIX: I need to update `ActivityRepository` or `StatisticsViewModel` to fallback to LOCAL DAO if online fails, 
        // OR better yet, I should generate the "DailyStatsDto" objects from the local DB for the purpose of the charts if we are in this "offline demo mode".
        // Actually, since I can't write to Supabase easily without being the user, the charts WON'T show this data unless I change the repository to look at local data too.
        
        // Let's modify ActivityRepository.getDailyStatsHistory to aggregate local `activity_sessions` if Supabase returns nothing?
        // Or simpler: Just tell the user "This demo only populates local data, charts might need a sync logic update".
        // Let's UPDATE ActivityRepository right now to aggregate local data for the charts.
    }
}
