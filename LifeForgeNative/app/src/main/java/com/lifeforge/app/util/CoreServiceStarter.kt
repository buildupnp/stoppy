package com.lifeforge.app.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.lifeforge.app.service.AppMonitorService
import com.lifeforge.app.service.StepCounterService

/**
 * Starts core foreground services that keep monitoring and step tracking alive.
 * Safe to call multiple times; the system will reuse existing services.
 */
object CoreServiceStarter {
    private const val TAG = "CoreServiceStarter"

    fun startCoreServices(context: Context) {
        val appContext = context.applicationContext
        startServiceSafely(appContext, Intent(appContext, StepCounterService::class.java), "StepCounterService")
        startServiceSafely(appContext, Intent(appContext, AppMonitorService::class.java), "AppMonitorService")
    }

    private fun startServiceSafely(context: Context, intent: Intent, label: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start $label", e)
        }
    }
}
