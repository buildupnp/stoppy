package com.lifeforge.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import com.lifeforge.app.service.NotificationWorker
import com.lifeforge.app.service.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class LifeForgeApp : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        // Create notification channels
        NotificationWorker.createNotificationChannels(this)
        // Initialize periodic workers
        scheduleSyncWorker()
        scheduleNotificationWorkers()
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES  // Sync every 15 minutes
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
    
    private fun scheduleNotificationWorkers() {
        // Schedule daily streak reminder (every 12 hours as approximation for evening)
        val streakReminderData = Data.Builder()
            .putString(NotificationWorker.NOTIFICATION_TYPE_KEY, NotificationWorker.TYPE_STREAK_REMINDER)
            .build()
        
        val streakReminderRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            12, TimeUnit.HOURS
        )
            .setInputData(streakReminderData)
            .setInitialDelay(8, TimeUnit.HOURS) // Start after 8 hours
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "streak_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            streakReminderRequest
        )
        
        // Schedule daily motivational quote (every 24 hours)
        val motivationalData = Data.Builder()
            .putString(NotificationWorker.NOTIFICATION_TYPE_KEY, NotificationWorker.TYPE_MOTIVATIONAL)
            .build()
        
        val motivationalRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            24, TimeUnit.HOURS
        )
            .setInputData(motivationalData)
            .setInitialDelay(6, TimeUnit.HOURS) // Morning motivation
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "motivational_quote",
            ExistingPeriodicWorkPolicy.KEEP,
            motivationalRequest
        )
    }
}

