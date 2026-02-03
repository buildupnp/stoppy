package com.lifeforge.app.ui.screens.permissions

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.lifeforge.app.util.XiaomiPermissionHelper

data class PermissionState(
    val type: PermissionType,
    val isGranted: Boolean,
    val title: String,
    val description: String,
    val iconRes: Int? = null // Optional if we use vector icons in UI
)

enum class PermissionType {
    RUNTIME, // Notification, Activity Recognition, Camera
    OVERLAY,
    USAGE_STATS,
    BATTERY,
    XIAOMI_AUTOSTART,
    XIAOMI_POPUP, // Critical for background start
    ACCESSIBILITY
}

class PermissionViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentPermission = MutableStateFlow<PermissionState?>(null)
    val currentPermission = _currentPermission.asStateFlow()

    private val _allGranted = MutableStateFlow(false)
    val allGranted = _allGranted.asStateFlow()
    
    // Track if we are waiting for a return from settings
    private var isChecking = false

    fun checkPermissions() {
        // Removed isChecking guard to ensure we always get a fresh check onResume
        viewModelScope.launch {
            val context = getApplication<Application>()
            
            // 1. Runtime Permissions (Grouped)
            val missingRuntime = com.lifeforge.app.util.PermissionHelper.getMissingRuntimePermissions(context)
            if (missingRuntime.isNotEmpty()) {
                _currentPermission.value = PermissionState(
                    type = PermissionType.RUNTIME,
                    isGranted = false,
                    title = "System Access",
                    description = "Allow notifications and activity tracking to enable the core loop."
                )
                return@launch
            }

            // 2. Overlay
            if (!android.provider.Settings.canDrawOverlays(context)) {
                _currentPermission.value = PermissionState(
                    type = PermissionType.OVERLAY,
                    isGranted = false,
                    title = "Display Overlay",
                    description = "Required for the Shield lock UI to block apps."
                )
                return@launch
            }

            // 3. Usage Stats
            if (!com.lifeforge.app.util.PermissionHelper.hasUsageStatsPermission(context)) {
                _currentPermission.value = PermissionState(
                    type = PermissionType.USAGE_STATS,
                    isGranted = false,
                    title = "Usage Access",
                    description = "Required to detect when you open blocked apps."
                )
                return@launch
            }

            // 4. Battery Optimization - REMOVED per user request (was causing stuck state)
            // if (!com.lifeforge.app.util.PermissionHelper.isIgnoringBatteryOptimizations(context)) { ... }

            // 5. Xiaomi Autostart
            if (XiaomiPermissionHelper.isXiaomiDevice() && !com.lifeforge.app.util.PermissionHelper.isXiaomiAutostartShown(context)) {
                _currentPermission.value = PermissionState(
                    type = PermissionType.XIAOMI_AUTOSTART,
                    isGranted = false,
                    title = "Autostart",
                    description = "Required on Xiaomi devices to prevent the system from killing the app."
                )
                return@launch
            }

            // 5.1 Xiaomi Background Popups (Critical)
            if (XiaomiPermissionHelper.isXiaomiDevice() && !isXiaomiPopupShown(context)) {
                _currentPermission.value = PermissionState(
                    type = PermissionType.XIAOMI_POPUP,
                    isGranted = false,
                    title = "Background Popup",
                    description = "Go to 'Other Permissions' and enable 'Display pop-up windows while running in the background'."
                )
                return@launch
            }

            // 6. Accessibility
            if (!com.lifeforge.app.util.PermissionHelper.isAccessibilityServiceEnabled(context)) {
                _currentPermission.value = PermissionState(
                    type = PermissionType.ACCESSIBILITY,
                    isGranted = false,
                    title = "Accessibility Service",
                    description = "The core engine that detects app launches. Find 'LifeForge' and turn it ON."
                )
                return@launch
            }

            // All Good
            _currentPermission.value = null
            _allGranted.value = true
        }
    }

    fun getMissingRuntimePermissions(context: Context): List<String> {
        return com.lifeforge.app.util.PermissionHelper.getMissingRuntimePermissions(context)
    }

    fun markXiaomiShown() {
         val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
         prefs.edit().putBoolean("xiaomi_autostart_shown", true).apply()
    }

    fun markXiaomiPopupShown() {
         val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
         prefs.edit().putBoolean("xiaomi_popup_shown", true).apply()
    }

    private fun isXiaomiPopupShown(context: Context): Boolean {
         val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
         return prefs.getBoolean("xiaomi_popup_shown", false)
    }
}
