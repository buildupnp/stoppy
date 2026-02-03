package com.lifeforge.app.data.repository

import com.lifeforge.app.data.local.database.dao.CoinDao
import com.lifeforge.app.data.local.database.entities.CoinTransaction
import com.lifeforge.app.data.remote.CoinBalanceDto
import com.lifeforge.app.data.remote.CoinTransactionDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing coin balance and transactions.
 * Handles both local (Room) and remote (Supabase) data.
 */
@Singleton
class CoinRepository @Inject constructor(
    private val coinDao: CoinDao,
    private val postgrest: io.github.jan.supabase.postgrest.Postgrest
) {
    companion object {
        // Coin earning rates
        const val PUSHUP_TO_COINS = 2 // 2 pushups = 1 LC
        const val STEPS_TO_COINS = 100 // 100 steps = 1 LC
        const val SQUATS_TO_COINS = 2 // 2 squats = 1 LC
        
        // Unlock costs
        val UNLOCK_COSTS = mapOf(
            15 to 10,  // 15 min = 10 LC
            30 to 18,  // 30 min = 18 LC
            60 to 30   // 60 min = 30 LC
        )
    }
    
    /**
     * Get current coin balance for a user (from local DB).
     */
    fun getBalanceFlow(userId: String): Flow<Int> {
        return coinDao.getBalance(userId).map { it ?: 0 }
    }
    
    /**
     * Get current balance synchronously.
     */
    suspend fun getBalance(userId: String): Int {
        return coinDao.getBalanceSync(userId) ?: 0
    }
    
    /**
     * Earn coins from an activity.
     */
    suspend fun earnCoins(
        userId: String,
        amount: Int,
        type: String,
        description: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Save locally
            val transaction = CoinTransaction(
                userId = userId,
                amount = amount,
                type = type,
                description = description,
                isSynced = false
            )
            coinDao.insert(transaction)
            
            // Try to sync to Supabase
            try {
                syncEarnToSupabase(userId, amount, type, description)
            } catch (e: Exception) {
                // Will sync later
                e.printStackTrace()
            }
            
            val newBalance = getBalance(userId)
            Result.success(newBalance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Spend coins (for unlocking apps).
     */
    suspend fun spendCoins(
        userId: String,
        amount: Int,
        description: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val currentBalance = getBalance(userId)
            if (currentBalance < amount) {
                return@withContext Result.failure(Exception("Insufficient balance"))
            }
            
            // Save locally (negative amount for spending)
            val transaction = CoinTransaction(
                userId = userId,
                amount = -amount, // Negative for spending
                type = "spent_unlock",
                description = description,
                isSynced = false
            )
            coinDao.insert(transaction)
            
            // Try to sync to Supabase
            try {
                syncSpendToSupabase(userId, amount, description)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            val newBalance = getBalance(userId)
            Result.success(newBalance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get recent transactions.
     */
    fun getRecentTransactions(userId: String, limit: Int = 50): Flow<List<CoinTransaction>> {
        return coinDao.getRecentTransactions(userId, limit)
    }
    
    /**
     * Sync to Supabase - Earn coins.
     */
    private suspend fun syncEarnToSupabase(
        userId: String,
        amount: Int,
        type: String,
        description: String
    ) {
        // First update balance in coin_balances table
        val currentRemoteBalance = fetchRemoteBalance(userId)
        val newBalance = currentRemoteBalance + amount
        
        postgrest["coin_balances"]
            .upsert(CoinBalanceDto(userId = userId, balance = newBalance)) 
        
        // Then log the transaction
        postgrest["coin_transactions"]
            .insert(CoinTransactionDto(
                userId = userId,
                amount = amount,
                type = type,
                description = description
            ))
    }
    
    /**
     * Sync to Supabase - Spend coins.
     */
    private suspend fun syncSpendToSupabase(
        userId: String,
        amount: Int,
        description: String
    ) {
        val currentRemoteBalance = fetchRemoteBalance(userId)
        val newBalance = currentRemoteBalance - amount
        
        postgrest["coin_balances"]
            .upsert(CoinBalanceDto(userId = userId, balance = newBalance))
        
        postgrest["coin_transactions"]
            .insert(CoinTransactionDto(
                userId = userId,
                amount = -amount,
                type = "spent_unlock",
                description = description
            ))
    }
    
    /**
     * Fetch balance from Supabase.
     */
    private suspend fun fetchRemoteBalance(userId: String): Int {
        return try {
            val result = postgrest["coin_balances"]
                .select(columns = Columns.list("balance")) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<CoinBalanceDto>()
            result?.balance ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Sync all unsynced transactions to Supabase.
     */
    suspend fun syncPendingTransactions() {
        val unsynced = coinDao.getUnsyncedTransactions()
        for (transaction in unsynced) {
            try {
                if (transaction.amount > 0) {
                    syncEarnToSupabase(
                        transaction.userId,
                        transaction.amount,
                        transaction.type,
                        transaction.description
                    )
                } else {
                    syncSpendToSupabase(
                        transaction.userId,
                        -transaction.amount,
                        transaction.description
                    )
                }
                coinDao.markAsSynced(transaction.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
