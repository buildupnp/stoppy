package com.lifeforge.app.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifeforge.app.data.repository.CoinRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager Worker for syncing pending coin transactions to Supabase.
 * Runs periodically in the background when network is available.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val coinRepository: CoinRepository,
    private val appLockRepository: com.lifeforge.app.data.repository.AppLockRepository,
    private val authRepository: com.lifeforge.app.data.repository.AuthRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val userId = authRepository.getCurrentUserId()
            
            // Sync Coins
            coinRepository.syncPendingTransactions()
            
            if (userId != null) {
                // Sync Apps (Not fully implemented yet in AppLockRepo but we can put a placeholder or call sync)
                 appLockRepository.syncFromSupabase(userId)
            }
            
            Result.success()
        } catch (e: Exception) {
            if (com.lifeforge.app.BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            // Retry on failure
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "lifeforge_sync_worker"
    }
}
