package com.lifeforge.app.data.local.database.dao

import androidx.room.*
import com.lifeforge.app.data.local.database.entities.ActivitySession
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    
    @Query("SELECT * FROM activity_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllActivities(userId: String): Flow<List<ActivitySession>>
    
    @Query("SELECT * FROM activity_sessions WHERE userId = :userId AND timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getActivitiesInRange(userId: String, startTime: Long, endTime: Long): List<ActivitySession>
    
    @Query("SELECT * FROM activity_sessions WHERE userId = :userId AND timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getTodayActivities(userId: String, startOfDay: Long): Flow<List<ActivitySession>>
    
    @Query("SELECT SUM(count) FROM activity_sessions WHERE userId = :userId AND type = :type AND timestamp >= :startOfDay")
    suspend fun getTodayCountByType(userId: String, type: String, startOfDay: Long): Int?
    
    @Query("SELECT SUM(coinsEarned) FROM activity_sessions WHERE userId = :userId AND timestamp >= :startOfDay")
    suspend fun getTodayCoinsEarned(userId: String, startOfDay: Long): Int?
    
    @Insert
    suspend fun insert(session: ActivitySession): Long
    
    @Query("SELECT * FROM activity_sessions WHERE isSynced = 0")
    suspend fun getUnsyncedActivities(): List<ActivitySession>
    
    @Query("UPDATE activity_sessions SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)
}
