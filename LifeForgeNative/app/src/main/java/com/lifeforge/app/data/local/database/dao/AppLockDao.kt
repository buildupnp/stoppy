package com.lifeforge.app.data.local.database.dao

import androidx.room.*
import com.lifeforge.app.data.local.database.entities.AppUnlock
import com.lifeforge.app.data.local.database.entities.ManagedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLockDao {
    
    // Managed Apps
    @Query("SELECT * FROM managed_apps ORDER BY appName ASC")
    fun getAllManagedApps(): Flow<List<ManagedAppEntity>>
    
    @Query("SELECT * FROM managed_apps WHERE isBlocked = 1")
    fun getBlockedApps(): Flow<List<ManagedAppEntity>>
    
    @Query("SELECT * FROM managed_apps WHERE packageName = :packageName")
    suspend fun getAppByPackage(packageName: String): ManagedAppEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: ManagedAppEntity)
    
    @Update
    suspend fun updateApp(app: ManagedAppEntity)
    
    @Delete
    suspend fun deleteApp(app: ManagedAppEntity)
    
    @Query("UPDATE managed_apps SET isBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun setBlocked(packageName: String, isBlocked: Boolean)
    
    // App Unlocks
    // App Unlocks - Modified for usage tracking
    // For legacy/time-window unlocks: expiresAt > currentTime
    // For usage-based unlocks: remainingTimeMs > 0
    @Query("""
        SELECT * FROM app_unlocks 
        WHERE (isUsageBased = 0 AND expiresAt > :currentTime)
           OR (isUsageBased = 1 AND remainingTimeMs > 0)
        ORDER BY remainingTimeMs DESC
    """)
    fun getActiveUnlocks(currentTime: Long): Flow<List<AppUnlock>>
    
    @Query("""
        SELECT * FROM app_unlocks 
        WHERE packageName = :packageName 
        AND (
            (isUsageBased = 0 AND expiresAt > :currentTime)
            OR (isUsageBased = 1 AND remainingTimeMs > 0)
        )
        LIMIT 1
    """)
    suspend fun getActiveUnlockForApp(packageName: String, currentTime: Long): AppUnlock?
    
    @Query("UPDATE app_unlocks SET remainingTimeMs = :newTimeMs WHERE id = :id")
    suspend fun updateRemainingTimeById(id: Long, newTimeMs: Long)

    @Query("SELECT * FROM app_unlocks WHERE packageName = :packageName")
    suspend fun getUnlocksByPackage(packageName: String): List<AppUnlock>

    @Insert
    suspend fun insertUnlock(unlock: AppUnlock): Long
    
    @Update
    suspend fun updateUnlock(unlock: AppUnlock)
    
    @Query("DELETE FROM app_unlocks WHERE (isUsageBased = 0 AND expiresAt < :currentTime) OR (isUsageBased = 1 AND remainingTimeMs <= 0)")
    suspend fun deleteExpiredUnlocks(currentTime: Long)
    
    @Query("UPDATE app_unlocks SET remainingTimeMs = 0, expiresAt = 0 WHERE packageName = :packageName")
    suspend fun resetUnlockForPackage(packageName: String)
}
