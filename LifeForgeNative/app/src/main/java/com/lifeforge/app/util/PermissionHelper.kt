package com.lifeforge.app.util

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process

object PermissionHelper {

    fun hasAllPermissions(context: Context): Boolean {
        // 1. Runtime
        if (getMissingRuntimePermissions(context).isNotEmpty()) return false
        
        // 2. Overlay
        if (!android.provider.Settings.canDrawOverlays(context)) return false
        
        // 3. Usage Stats
        if (!hasUsageStatsPermission(context)) return false
        
        // 4. Xiaomi - only if Xiaomi (MIUI requires extra steps to keep services alive)
        if (XiaomiPermissionHelper.isXiaomiDevice()) {
            if (!isXiaomiAutostartShown(context)) return false
            if (!isXiaomiPopupShown(context)) return false
        }

        // 5. Accessibility
        if (!isAccessibilityServiceEnabled(context)) return false
        
        return true
    }

    fun getMissingRuntimePermissions(context: Context): List<String> {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(android.Manifest.permission.ACTIVITY_RECOGNITION)
        }
        // POST_NOTIFICATIONS is optional for core functionality; do not block the app on it.
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            permissions.add(android.Manifest.permission.CAMERA)
        }

        return permissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun isXiaomiAutostartShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("xiaomi_autostart_shown", false)
    }

    fun isXiaomiPopupShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("xiaomi_popup_shown", false)
    }
    
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedId = "${context.packageName}/${com.lifeforge.app.accessibility.AppDetectorService::class.java.name}"
        return enabledServices.any { it.id == expectedId || it.id.contains(context.packageName) }
    }
}
