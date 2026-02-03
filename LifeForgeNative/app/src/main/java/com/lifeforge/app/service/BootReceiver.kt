package com.lifeforge.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lifeforge.app.service.StepCounterService

/**
 * Broadcast receiver that starts the monitoring service on device boot.
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start the monitoring service
            val serviceIntent = Intent(context, AppMonitorService::class.java)
            val stepIntent = Intent(context, StepCounterService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
                context.startForegroundService(stepIntent)
            } else {
                context.startService(serviceIntent)
                context.startService(stepIntent)
            }
        }
    }
}
