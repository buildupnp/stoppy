package com.lifeforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lifeforge.app.ui.navigation.AppNavigation
import com.lifeforge.app.ui.theme.StoppyTheme
import com.lifeforge.app.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val mainViewModel: MainViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle splash screen
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            // Collect start destination
            val startDestination by mainViewModel.startDestination.collectAsState()
            // Collect theme mode
            val themeMode by themeViewModel.themeMode.collectAsState()
            
            // Start services if we are logged in AND have permissions
            // Removed auto-check to prevent background loops. Permission flow is now manual via PermissionIntroScreen.
            androidx.compose.runtime.LaunchedEffect(startDestination) {
                // We will handle permission checks in the UI flow (PermissionIntroScreen)
            }
            
            StoppyTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (startDestination != null) {
                        AppNavigation(startDestination = startDestination!!)
                    }
                    // Removed loading indicator - app will show splash screen instead
                }
            }
        }
    }
    
    private fun checkPermissionsAndStart() {
        // 1. Runtime Permissions
        if (!checkRuntimePermissions()) return

        // 2. Special Permissions (Chained)
        if (!checkOverlayPermission()) return
        if (!checkUsageStatsPermission()) return
        if (!checkBatteryOptimization()) return
        if (!checkXiaomiAutostart()) return
        
        // 3. Accessibility Service Check
        if (!isAccessibilityServiceEnabled()) {
             showAccessibilityDialog()
             return
        }

        // All good -> Start Services
        startServices()
    }
    
    private fun checkRuntimePermissions(): Boolean {
        val permissions = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            permissions.add(android.Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        // Add Camera if not already granted (for AI Workout)
        permissions.add(android.Manifest.permission.CAMERA)

        val missing = permissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        
        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 1001)
            return false
        }
        return true
    }
    
    // Callback for runtime permissions
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Manual handling in PermissionIntroScreen now
    }

    private fun checkOverlayPermission(): Boolean {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            showToast(getString(R.string.permission_desc_overlay))
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 2001)
            return false
        }
        return true
    }

    private fun checkUsageStatsPermission(): Boolean {
        if (!hasUsageStatsPermission()) {
            showToast(getString(R.string.permission_desc_usage))
            startActivityForResult(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS), 2002)
            return false
        }
        return true
    }
    
    private fun checkBatteryOptimization(): Boolean {
        val pm = getSystemService(android.os.PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            showToast(getString(R.string.permission_desc_battery))
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                android.net.Uri.parse("package:$packageName")
            )
            try {
                 startActivityForResult(intent, 2003)
            } catch (e: Exception) {
                // Some devices crash on this intent, fallback to generic settings
                 startActivityForResult(android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), 2003)
            }
            return false
        }
        return true
    }
    
    private fun checkXiaomiAutostart(): Boolean {
        // Xiaomi Autostart (Prevent killing on MIUI)
        if (com.lifeforge.app.util.XiaomiPermissionHelper.isXiaomiDevice()) {
             // We can't easily check if it's ENABLED, but we can use a SharedPref to show it only once.
             val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
             if (!prefs.getBoolean("xiaomi_autostart_shown", false)) {
                 val autostartIntent = com.lifeforge.app.util.XiaomiPermissionHelper.getAutostartIntent(this)
                 if (autostartIntent != null) {
                     showToast(getString(R.string.permission_autostart_msg))
                     startActivityForResult(autostartIntent, 2004)
                     prefs.edit().putBoolean("xiaomi_autostart_shown", true).apply()
                     return false
                 }
             }
        }
        return true
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.id.contains(packageName) }
    }
    
    private fun showAccessibilityDialog() {
         // Show a dialog or toast directing them
         showToast(getString(R.string.accessibility_service_description))
         val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
         startActivityForResult(intent, 2005)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Re-check flow handled by UI observation, not auto-loop
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val usageStatsManager = getSystemService(android.content.Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
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
    
    private fun showToast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show()
    }
    
    private fun startServices() {
        try {
            val stepIntent = android.content.Intent(this, com.lifeforge.app.service.StepCounterService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(stepIntent)
            } else {
                startService(stepIntent)
            }
            
            val monitorIntent = android.content.Intent(this, com.lifeforge.app.service.AppMonitorService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(monitorIntent)
            } else {
                startService(monitorIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
