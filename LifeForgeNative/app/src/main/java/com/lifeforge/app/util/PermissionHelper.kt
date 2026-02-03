package com.lifeforge.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object PermissionHelper {

    fun hasAllPermissions(context: Context): Boolean {
        // 1. Runtime
        if (getMissingRuntimePermissions(context).isNotEmpty()) return false
        
        // 2. Overlay
        if (!android.provider.Settings.canDrawOverlays(context)) return false
        
        // 3. Usage Stats
        if (!hasUsageStatsPermission(context)) return false
        
        // 4. Battery - only strictly required if we want to ensure background survival
        if (!isIgnoringBatteryOptimizations(context)) return false
        
        // 5. Xiaomi - only if Xiaomi
        if (XiaomiPermissionHelper.isXiaomiDevice() && !isXiaomiAutostartShown(context)) return false
        
        // 6. Accessibility
        if (!isAccessibilityServiceEnabled(context)) return false
        
        return true
    }

    fun getMissingRuntimePermissions(context: Context): List<String> {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(android.Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissions.add(android.Manifest.permission.CAMERA)

        return permissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val currentTime = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                currentTime - 1000 * 60,
                currentTime
            )
            stats?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(android.os.PowerManager::class.java)
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isXiaomiAutostartShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("xiaomi_autostart_shown", false)
    }
    
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.id.contains(context.packageName) }
    }
}
