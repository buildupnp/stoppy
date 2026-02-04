package com.lifeforge.app.data.repository

import android.util.Log
import com.lifeforge.app.accessibility.AppDetectorService
import com.lifeforge.app.data.local.database.dao.AppLockDao
import com.lifeforge.app.data.local.database.entities.AppUnlock
import com.lifeforge.app.data.local.database.entities.ManagedAppEntity
import com.lifeforge.app.data.remote.AppUnlockDto
import com.lifeforge.app.data.remote.ManagedAppDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.*
import io.github.jan.supabase.postgrest.query.filter.*
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing blocked apps and unlock sessions.
 */
@Singleton
class AppLockRepository @Inject constructor(
    private val appLockDao: AppLockDao,
    private val coinRepository: CoinRepository,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val postgrest: io.github.jan.supabase.postgrest.Postgrest,
    private val database: com.lifeforge.app.data.local.database.AppDatabase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Common apps that can be blocked.
     */
    val commonApps = listOf(
        CommonApp("Instagram", "com.instagram.android"),
        CommonApp("TikTok", "com.zhiliaoapp.musically"),
        CommonApp("YouTube", "com.google.android.youtube"),
        CommonApp("Facebook", "com.facebook.katana"),
        CommonApp("Twitter/X", "com.twitter.android"),
        CommonApp("Snapchat", "com.snapchat.android"),
        CommonApp("Reddit", "com.reddit.frontpage"),
        CommonApp("Netflix", "com.netflix.mediaclient"),
        CommonApp("Twitch", "tv.twitch.android.app"),
        CommonApp("Discord", "com.discord"),
        CommonApp("WhatsApp", "com.whatsapp"),
        CommonApp("Messenger", "com.facebook.orca")
    )
    
    data class CommonApp(val name: String, val packageName: String)
    
    /**
     * Get all managed apps.
     */
    fun getAllManagedApps(): Flow<List<ManagedAppEntity>> {
        return appLockDao.getAllManagedApps()
    }
    
    /**
     * Get only blocked apps.
     */
    fun getBlockedApps(): Flow<List<ManagedAppEntity>> {
        return appLockDao.getBlockedApps()
    }
    
    /**
     * Get active unlock sessions.
     * Use a ticker to refresh the query periodically (time-window expiry)
     * while still reacting INSTANTLY to database changes via flatMapLatest.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getActiveUnlocks(): kotlinx.coroutines.flow.Flow<List<AppUnlock>> {
        val ticker = kotlinx.coroutines.flow.flow {
            while (true) {
                emit(System.currentTimeMillis())
                kotlinx.coroutines.delay(5000) // Refresh time-window filter every 5s

            }
        }
        return ticker.flatMapLatest { currentTime ->
            appLockDao.getActiveUnlocks(currentTime)
        }
    }
    
    /**
     * Add an app to manage.
     */
    suspend fun addApp(userId: String, name: String, packageName: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                // Save locally
                val app = ManagedAppEntity(
                    packageName = packageName,
                    appName = name,
                    isBlocked = true
                )
                appLockDao.insertApp(app)
                
                // Update accessibility service
                AppDetectorService.addBlockedPackage(packageName)

                notificationRepository.addNotification(
                    title = "App Blocked",
                    description = "$name is now protected by Guardian.",
                    icon = "🛡️",
                    category = "Guardian"
                )
                
                // Sync to Supabase
                try {
                    postgrest["managed_apps"]
                        .insert(ManagedAppDto(
                            userId = userId,
                            appName = name,
                            packageName = packageName,
                            isBlocked = true
                        ))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Toggle block status for an app.
     */
    suspend fun toggleBlock(packageName: String, isBlocked: Boolean): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                appLockDao.setBlocked(packageName, isBlocked)
                
                // Update accessibility service
                if (isBlocked) {
                    AppDetectorService.addBlockedPackage(packageName)
                    notificationRepository.addNotification(
                        title = "Blocking Enabled",
                        description = "Guardian is now blocking ${packageName.split('.').last()}",
                        icon = "🔒",
                        category = "Guardian"
                    )
                } else {
                    AppDetectorService.removeBlockedPackage(packageName)
                    notificationRepository.addNotification(
                        title = "Blocking Disabled",
                        description = "Guardian is no longer blocking ${packageName.split('.').last()}",
                        icon = "🔓",
                        category = "Guardian"
                    )
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Remove an app from management.
     */
    suspend fun removeApp(app: ManagedAppEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            appLockDao.deleteApp(app)
            AppDetectorService.removeBlockedPackage(app.packageName)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Unlock an app via workout (no coin cost).
     */
    suspend fun workoutUnlock(
        userId: String,
        packageName: String,
        appName: String,
        durationMinutes: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val durationMs = durationMinutes * 60 * 1000L
            
            val existing = appLockDao.getActiveUnlockForApp(packageName, System.currentTimeMillis())
            if (existing != null && existing.isUsageBased) {
                // Add to existing
                val updated = existing.copy(
                    remainingTimeMs = existing.remainingTimeMs + durationMs,
                    durationMinutes = existing.durationMinutes + durationMinutes
                )
                appLockDao.updateUnlock(updated)
            } else {
                // Save unlock locally
                val unlock = AppUnlock(
                    packageName = packageName,
                    appName = appName,
                    durationMinutes = durationMinutes,
                    coinsCost = 0,
                    expiresAt = System.currentTimeMillis() + durationMs, // Kept for reference
                    remainingTimeMs = durationMs, // USAGE BASED
                    isUsageBased = true
                )
                appLockDao.insertUnlock(unlock)
            }
            
            // Immediate update to service state
            com.lifeforge.app.accessibility.AppDetectorService.unlockPackage(packageName, durationMs)

            notificationRepository.addNotification(
                title = "Time Earned",
                description = "$appName unlocked for $durationMinutes minutes (workout).",
                icon = "⏱️",
                category = "Guardian"
            )
            
            val expiresAt = System.currentTimeMillis() + durationMs
            
            // Sync to Supabase
            try {
                postgrest["app_unlocks"]
                    .insert(AppUnlockDto(
                        userId = userId,
                        appId = packageName,
                        appName = appName,
                        durationMinutes = durationMinutes,
                        coinsSpent = 0,
                        expiresAt = isoFormat.format(Date(expiresAt))
                    ))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Consolidate any duplicates immediately
            mergeDuplicateUnlocks()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Unlock an app for a specified duration.
     * @return Result with remaining balance
     */
    suspend fun unlockApp(
        userId: String,
        app: ManagedAppEntity,
        durationMinutes: Int
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Get cost
            val cost = CoinRepository.UNLOCK_COSTS[durationMinutes] 
                ?: return@withContext Result.failure(Exception("Invalid duration"))
            
            // Check balance first (outside transaction for quick fail)
            val currentBalance = coinRepository.getBalance(userId)
            if (currentBalance < cost) {
                return@withContext Result.failure(Exception("Insufficient balance"))
            }

            // ATOMIC LOCAL TRANSACTION
            database.withTransaction {
                // 1. Deduct coins locally
                val transaction = com.lifeforge.app.data.local.database.entities.CoinTransaction(
                    userId = userId,
                    amount = -cost,
                    type = "spent_unlock",
                    description = "Unlock ${app.appName} for ${durationMinutes}min",
                    isSynced = false
                )
                database.coinDao().insert(transaction)

                // 2. Create/Update unlock record
                val durationMs = durationMinutes * 60 * 1000L
                val existing = appLockDao.getActiveUnlockForApp(app.packageName, System.currentTimeMillis())
                
                if (existing != null && existing.isUsageBased) {
                    val updated = existing.copy(
                        remainingTimeMs = existing.remainingTimeMs + durationMs,
                        durationMinutes = existing.durationMinutes + durationMinutes,
                        coinsCost = existing.coinsCost + cost
                    )
                    appLockDao.updateUnlock(updated)
                } else {
                    val unlock = AppUnlock(
                        packageName = app.packageName,
                        appName = app.appName,
                        durationMinutes = durationMinutes,
                        coinsCost = cost,
                        expiresAt = System.currentTimeMillis() + durationMs,
                        remainingTimeMs = durationMs,
                        isUsageBased = true
                    )
                    appLockDao.insertUnlock(unlock)
                }
            }

            // POST-TRANSACTION: UI & NETWORK
            
            // Calculate duration ms for service
            val durationMs = durationMinutes * 60 * 1000L
            
            // Immediate update to service state
            com.lifeforge.app.accessibility.AppDetectorService.unlockPackage(app.packageName, durationMs)

            notificationRepository.addNotification(
                title = "Unlocked",
                description = "${app.appName} unlocked for $durationMinutes min (-$cost LC).",
                icon = "🔓",
                category = "Guardian"
            )
            
            val expiresAt = System.currentTimeMillis() + durationMs
            
            // Async sync to Supabase (non-blocking)
            repositoryScope.launch {
                try {
                    // Sync coins
                    coinRepository.syncPendingTransactions()
                    
                    // Sync unlock session
                    postgrest["app_unlocks"]
                        .insert(AppUnlockDto(
                            userId = userId,
                            appId = app.packageName,
                            appName = app.appName,
                            durationMinutes = durationMinutes,
                            coinsSpent = cost,
                            expiresAt = isoFormat.format(Date(expiresAt))
                        ))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Consolidate any duplicates
            mergeDuplicateUnlocks()
            
            val newBalance = coinRepository.getBalance(userId)
            Result.success(newBalance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Emergency unlock - unlocks a specific app for 15 min.
     * This should reset the user's streak.
     * Now enforces daily limit (configurable 0-10, default 3).
     */
    suspend fun emergencyUnlock(packageName: String, appName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Check daily limit
            val limit = getEmergencyUnlockLimit()
            if (limit == 0) {
                return@withContext Result.failure(Exception("Emergency unlock is disabled"))
            }
            
            val count = getEmergencyUnlockCount()
            if (count >= limit) {
                return@withContext Result.failure(Exception("Daily emergency unlock limit reached ($limit/$limit). Resets at midnight."))
            }

            // Add a 24-hour cooldown between emergency unlocks to prevent exploitation
            val lastReset = authRepository.getLastStreakResetTimestamp()
            val twentyFourHours = 24 * 60 * 60 * 1000L
            if (System.currentTimeMillis() - lastReset < twentyFourHours) {
                val remaining = twentyFourHours - (System.currentTimeMillis() - lastReset)
                val remainingHours = remaining / (60 * 60 * 1000)
                val remainingMin = (remaining % (60 * 60 * 1000)) / (60 * 1000)
                return@withContext Result.failure(Exception("Emergency unlock is on cooldown. Please wait $remainingHours hours and $remainingMin minutes."))
            }
            
            val userId = authRepository.getCurrentUserId() ?: "unknown"
            
            // Only unlock the specific app
            workoutUnlock(userId, packageName, appName, 15)
            
            // Increment counter
            incrementEmergencyUnlockCount()
            
            // Reset streak
            authRepository.setLastStreakResetTimestamp(System.currentTimeMillis())
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if an app is currently unlocked.
     */
    suspend fun isAppUnlocked(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val unlock = appLockDao.getActiveUnlockForApp(packageName, System.currentTimeMillis())
        unlock != null
    }
    
    /**
     * Clean up expired unlocks.
     */
    suspend fun cleanupExpiredUnlocks() = withContext(Dispatchers.IO) {
        appLockDao.deleteExpiredUnlocks(System.currentTimeMillis())
    }
    
    /**
     * Sync managed apps from Supabase.
     */
    suspend fun syncFromSupabase(userId: String) = withContext(Dispatchers.IO) {
        try {
            val remoteApps = postgrest["managed_apps"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<ManagedAppDto>()
            
            remoteApps.forEach { dto ->
                val entity = ManagedAppEntity(
                    packageName = dto.packageName,
                    appName = dto.appName,
                    isBlocked = dto.isBlocked
                )
                appLockDao.insertApp(entity)
                
                if (dto.isBlocked) {
                    AppDetectorService.addBlockedPackage(dto.packageName)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    /**
     * Consume time from an active unlock.
     */
    suspend fun consumeTime(packageName: String, amountMs: Long) = withContext(Dispatchers.IO) {
        try {
            val unlock = appLockDao.getActiveUnlockForApp(packageName, System.currentTimeMillis())
            if (unlock != null && unlock.remainingTimeMs > 0) {
                val newTime = (unlock.remainingTimeMs - amountMs).coerceAtLeast(0)
                Log.d("AppLockRepository", "Consuming $amountMs ms for $packageName. New time: $newTime")
                appLockDao.updateRemainingTimeById(unlock.id, newTime)
                
                // If it was the last of its kind or we're just syncing, 
                // run a quick merge to ensure we don't have multiple records fighting.
                mergeDuplicateUnlocks()
            } else {
                Log.d("AppLockRepository", "No active usage-based unlock found for $packageName to consume time.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Merge duplicate unlock records for the same package.
     * This fixes inconsistent counts from old bugs.
     */
    suspend fun mergeDuplicateUnlocks() = withContext(Dispatchers.IO) {
        try {
            val allApps = appLockDao.getAllManagedApps().first()
            val currentTime = System.currentTimeMillis()
            
            allApps.forEach { app ->
                val unlocks = appLockDao.getUnlocksByPackage(app.packageName)
                if (unlocks.size > 1) {
                    // Find all ACTIVE unlocks (usage based > 0 OR legacy not expired)
                    val activeUnlocks = unlocks.filter { 
                        (it.isUsageBased && it.remainingTimeMs > 0) ||
                        (!it.isUsageBased && it.expiresAt > currentTime)
                    }
                    
                    if (activeUnlocks.size > 1) {
                        Log.d("AppLockRepository", "Merging ${activeUnlocks.size} unlocks for ${app.packageName}")
                        
                        var totalRemainingMs = 0L
                        var totalMinutes = 0
                        var totalCost = 0
                        
                        activeUnlocks.forEach { u ->
                            val time = if (u.isUsageBased) u.remainingTimeMs else (u.expiresAt - currentTime).coerceAtLeast(0)
                            totalRemainingMs += time
                            totalMinutes += u.durationMinutes
                            totalCost += u.coinsCost
                        }
                        
                        // Create one consolidated usage-based record
                        val keeper = activeUnlocks.first().copy(
                            remainingTimeMs = totalRemainingMs,
                            durationMinutes = totalMinutes,
                            coinsCost = totalCost,
                            isUsageBased = true,
                            expiresAt = currentTime + totalRemainingMs // For reference
                        )
                        
                        appLockDao.updateUnlock(keeper)
                        
                        // Delete all others
                        activeUnlocks.drop(1).forEach { 
                             appLockDao.updateRemainingTimeById(it.id, 0) // Effectively expires it
                        }
                        
                        cleanupExpiredUnlocks()
                        Log.d("AppLockRepository", "Merged total for ${app.packageName}: ${totalRemainingMs/1000}s")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     * Reset an app's unlock state.
     */
    suspend fun resetUnlock(packageName: String) = withContext(Dispatchers.IO) {
        try {
            appLockDao.resetUnlockForPackage(packageName)
            // Cleanup expired entries immediately
            appLockDao.deleteExpiredUnlocks(System.currentTimeMillis())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Emergency Unlock Limit Tracking
    
    /**
     * Get the configured daily emergency unlock limit.
     * Range: 0-10, Default: 3
     */
    fun getEmergencyUnlockLimit(): Int {
        val prefs = context.getSharedPreferences("lifeforge_applock_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getInt("emergency_unlock_limit", 3).coerceIn(0, 10)
    }
    
    /**
     * Set the daily emergency unlock limit.
     */
    fun setEmergencyUnlockLimit(limit: Int) {
        val prefs = context.getSharedPreferences("lifeforge_applock_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putInt("emergency_unlock_limit", limit.coerceIn(0, 10)).apply()
    }
    
    /**
     * Get the current emergency unlock count for today.
     * Automatically resets if date has changed.
     */
    fun getEmergencyUnlockCount(): Int {
        val prefs = context.getSharedPreferences("lifeforge_applock_prefs", android.content.Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val lastDate = prefs.getString("emergency_unlock_date", "")
        
        // Reset counter if it's a new day
        if (lastDate != today) {
            prefs.edit()
                .putInt("emergency_unlock_count", 0)
                .putString("emergency_unlock_date", today)
                .apply()
            return 0
        }
        
        return prefs.getInt("emergency_unlock_count", 0)
    }
    
    /**
     * Increment the emergency unlock counter.
     */
    private fun incrementEmergencyUnlockCount() {
        val prefs = context.getSharedPreferences("lifeforge_applock_prefs", android.content.Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val currentCount = getEmergencyUnlockCount()
        
        prefs.edit()
            .putInt("emergency_unlock_count", currentCount + 1)
            .putString("emergency_unlock_date", today)
            .apply()
    }
}

