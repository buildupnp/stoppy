package com.lifeforge.app.data.local.database.dao

import androidx.room.*
import com.lifeforge.app.data.local.database.entities.CoinTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinDao {
    
    @Query("SELECT SUM(amount) FROM coin_transactions WHERE userId = :userId")
    fun getBalance(userId: String): Flow<Int?>
    
    @Query("SELECT SUM(amount) FROM coin_transactions WHERE userId = :userId")
    suspend fun getBalanceSync(userId: String): Int?
    
    @Query("SELECT * FROM coin_transactions WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(userId: String, limit: Int = 50): Flow<List<CoinTransaction>>
    
    @Insert
    suspend fun insert(transaction: CoinTransaction): Long
    
    @Query("SELECT * FROM coin_transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<CoinTransaction>
    
    @Query("UPDATE coin_transactions SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)
}
