package com.lifeforge.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifeforge.app.MainActivity
import com.lifeforge.app.R
import com.lifeforge.app.data.repository.AuthRepository
import com.lifeforge.app.data.repository.FeatureRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager Worker for sending scheduled notifications.
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val authRepository: AuthRepository,
    private val featureRepository: FeatureRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "lifeforge_notification_worker"
        const val CHANNEL_ID_REMINDERS = "lifeforge_reminders"
        const val CHANNEL_ID_ACHIEVEMENTS = "lifeforge_achievements"
        
        const val NOTIFICATION_TYPE_KEY = "notification_type"
        const val TYPE_STREAK_REMINDER = "streak_reminder"
        const val TYPE_MOTIVATIONAL = "motivational"
        const val TYPE_CHALLENGE_ENDING = "challenge_ending"
        
        private val MOTIVATIONAL_QUOTES = listOf(
            "Your body can stand almost anything. It's your mind that you have to convince.",
            "The only bad workout is the one that didn't happen.",
            "Success is what happens after you've survived all your mistakes.",
            "Discipline is choosing between what you want now and what you want most.",
            "The pain you feel today is the strength you feel tomorrow.",
            "Don't count the days. Make the days count.",
            "Your future self is watching you right now through memories.",
            "Every expert was once a beginner."
        )
        
        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                
                // Reminders channel
                val remindersChannel = NotificationChannel(
                    CHANNEL_ID_REMINDERS,
                    "Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Streak reminders and daily motivation"
                }
                notificationManager.createNotificationChannel(remindersChannel)
                
                // Achievements channel
                val achievementsChannel = NotificationChannel(
                    CHANNEL_ID_ACHIEVEMENTS,
                    "Achievements",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Achievement unlocked notifications"
                }
                notificationManager.createNotificationChannel(achievementsChannel)
            }
        }
    }

    override suspend fun doWork(): Result {
        val notificationType = inputData.getString(NOTIFICATION_TYPE_KEY) ?: TYPE_STREAK_REMINDER
        
        return try {
            when (notificationType) {
                TYPE_STREAK_REMINDER -> sendStreakReminder()
                TYPE_MOTIVATIONAL -> sendMotivationalQuote()
                TYPE_CHALLENGE_ENDING -> sendChallengeEndingReminder()
                else -> sendStreakReminder()
            }
            Result.success()
        } catch (e: Exception) {
            if (com.lifeforge.app.BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            Result.retry()
        }
    }
    
    private suspend fun sendStreakReminder() {
        val userId = authRepository.getCurrentUserId() ?: return
        val (streakDays, _) = featureRepository.getLoginStreakInfo(userId)
        
        val title = if (streakDays > 0) {
            "Don't break your $streakDays-day streak! 🔥"
        } else {
            "Start your streak today! 💪"
        }
        
        val message = "Open LifeForge to claim your daily bonus and keep earning!"
        
        sendNotification(
            id = 1001,
            channelId = CHANNEL_ID_REMINDERS,
            title = title,
            message = message
        )
    }
    
    private fun sendMotivationalQuote() {
        val quote = MOTIVATIONAL_QUOTES.random()
        
        sendNotification(
            id = 1002,
            channelId = CHANNEL_ID_REMINDERS,
            title = "Daily Motivation 💡",
            message = quote
        )
    }
    
    private fun sendChallengeEndingReminder() {
        sendNotification(
            id = 1003,
            channelId = CHANNEL_ID_REMINDERS,
            title = "Weekly Challenge Ending Soon! ⏰",
            message = "Complete your challenges before they reset on Monday!"
        )
    }
    
    private fun sendNotification(
        id: Int,
        channelId: String,
        title: String,
        message: String
    ) {
        // Create intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use app icon in production
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Permission not granted
            if (com.lifeforge.app.BuildConfig.DEBUG) {
                e.printStackTrace()
            }
        }
    }
}

/**
 * Helper object for sending instant notifications (achievements, etc.)
 */
object NotificationHelper {
    
    fun sendAchievementUnlocked(
        context: Context,
        achievementTitle: String,
        coinsEarned: Int
    ) {
        NotificationWorker.createNotificationChannels(context)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, NotificationWorker.CHANNEL_ID_ACHIEVEMENTS)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("Achievement Unlocked! 🏆")
            .setContentText("$achievementTitle - Earned +$coinsEarned LC")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(
                System.currentTimeMillis().toInt(),
                notification
            )
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}
