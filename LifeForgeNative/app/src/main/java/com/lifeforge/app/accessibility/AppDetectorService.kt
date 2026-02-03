package com.lifeforge.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.lifeforge.app.ui.screens.overlay.LockOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.lifeforge.app.util.XiaomiPermissionHelper

/**
 * Accessibility Service that monitors which app is in the foreground.
 * When a blocked app is detected, it shows the lock overlay.
 * 
 * User must enable this in Settings > Accessibility > Stoppy
 */
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppDetectorService : AccessibilityService() {
    
    companion object {
        private const val TAG = "AppDetectorService"
        
        // Observable state for whether service is running
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning
        
        private val _currentlyTracked = MutableStateFlow<String?>(null)
        val currentlyTracked: StateFlow<String?> = _currentlyTracked
        
        private val _registeredAppsCount = MutableStateFlow(0)
        val registeredAppsCount: StateFlow<Int> = _registeredAppsCount
        
        // Blocked packages - thread safe
        private val blockedPackages = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        
        // Usage tracking
        private val activeUnlocks = java.util.concurrent.ConcurrentHashMap<String, Long>() // Package -> Remaining Time Ms
        private val pendingDeductions = java.util.concurrent.ConcurrentHashMap<String, Long>() // Package -> Un-synced Ms
        private val lastKnownDbTotal = java.util.concurrent.ConcurrentHashMap<String, Long>() // Package -> Last synced DB total
        
        fun addBlockedPackage(packageName: String) {
            blockedPackages.add(packageName)
        }
        
        fun removeBlockedPackage(packageName: String) {
            blockedPackages.remove(packageName)
        }
        
        fun unlockPackage(packageName: String, durationMs: Long) {
            val current = activeUnlocks[packageName] ?: 0L
            activeUnlocks[packageName] = current + durationMs
        }
        
        fun isPackageUnlocked(packageName: String): Boolean {
            val remaining = activeUnlocks[packageName] ?: 0L
            return remaining > 0
        }
        
        fun clearUnlock(packageName: String) {
            activeUnlocks[packageName] = 0L
        }
    }
    
    @javax.inject.Inject
    lateinit var appLockRepository: com.lifeforge.app.data.repository.AppLockRepository
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastDetectedPackage: String? = null
    private var usageStartTime: Long = 0
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        if (com.lifeforge.app.BuildConfig.DEBUG) {
            Log.d(TAG, "Accessibility Service connected")
        }
        _isRunning.value = true
        
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            // Remove flags that intercept touch/keys and cause hangs
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 200
        }
        
        loadBlockedPackages()
        loadActiveUnlocks()
        startPeriodicCheck()
        detectCurrentAppOnStartup()
    }
    
    private fun detectCurrentAppOnStartup() {
        serviceScope.launch {
            // Wait a bit for the connection to fully stabilize
            delay(500)
            repeat(5) { 
                val root = rootInActiveWindow
                val pkg = root?.packageName?.toString()
                if (pkg != null && pkg != com.lifeforge.app.BuildConfig.APPLICATION_ID && !isSystemPackage(pkg)) {
                    lastDetectedPackage = pkg
                    usageStartTime = System.currentTimeMillis()
                    if (com.lifeforge.app.BuildConfig.DEBUG) {
                        Log.d(TAG, "Startup detection successful: $pkg")
                    }
                    return@launch
                }
                delay(2000)
            }
        }
    }
    
    private fun startPeriodicCheck() {
        serviceScope.launch {
            while (true) {
                // optimization: smart polling
                if (!isScreenOn() || isDeviceLocked()) {
                    delay(5000) // Sleep if screen is off
                } else {
                    val pkg = lastDetectedPackage
                    if (pkg == null || !blockedPackages.contains(pkg)) {
                        delay(2000) // Relaxed check (2s) if we aren't currently draining time
                    } else {
                        delay(1000) // maintain 1s precision for countdowns
                    }
                }
                updateUsage()
            }
        }
    }
    
    private fun isScreenOn(): Boolean {
        val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isInteractive
    }

    private fun isDeviceLocked(): Boolean {
        val keyguardManager = getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager?
        return keyguardManager?.isKeyguardLocked ?: false
    }

    private fun updateUsage() {
        if (!isScreenOn() || isDeviceLocked()) {
            usageStartTime = 0L
            _currentlyTracked.value = null
            return
        }
        
        var currentPackage = lastDetectedPackage
        
        if (currentPackage == null) {
            val rootPkg = rootInActiveWindow?.packageName?.toString()
            if (rootPkg != null) {
                if (rootPkg == com.lifeforge.app.BuildConfig.APPLICATION_ID || isSystemPackage(rootPkg)) {
                    // It's a system/own app, don't track but clear tracking state
                    _currentlyTracked.value = null
                    return
                } else {
                    currentPackage = rootPkg
                    lastDetectedPackage = currentPackage
                    usageStartTime = System.currentTimeMillis()
                }
            } else {
                _currentlyTracked.value = null
                return
            }
        } else {
            // Safety Check: Is the package we think is active STILL the one in the root window?
            // If the user went home or opened another app but we didn't get an event (rare), this catches it.
            val rootPkg = rootInActiveWindow?.packageName?.toString()
            if (rootPkg != null && rootPkg != currentPackage && !isTransientSystemPackage(rootPkg)) {
                if (com.lifeforge.app.BuildConfig.DEBUG) {
                    Log.d(TAG, "Usage safety trigger: Detected $rootPkg while tracking $currentPackage. Stopping tracking.")
                }
                lastDetectedPackage = rootPkg
                usageStartTime = System.currentTimeMillis()
                _currentlyTracked.value = if (rootPkg == com.lifeforge.app.BuildConfig.APPLICATION_ID || isSystemPackage(rootPkg)) null else rootPkg
                return
            }
        }
        
        _currentlyTracked.value = currentPackage
        
        // KEY CHECK: Is this app in our blocked list?
        if (!blockedPackages.contains(currentPackage)) {
            // Heartbeat for non-blocked apps (every 10s)
            if (System.currentTimeMillis() % 10000 < 1000) {
                if (com.lifeforge.app.BuildConfig.DEBUG) {
                    Log.d(TAG, "Monitoring: $currentPackage is active but NOT managed.")
                }
            }
            return
        }
        
        val now = System.currentTimeMillis()
        if (usageStartTime == 0L) usageStartTime = now
        
        val elapsed = now - usageStartTime
        usageStartTime = now // advancing window
        
        // Deduct time if unlocked
        val remaining = activeUnlocks[currentPackage] ?: 0L
        
        if (remaining > 0) {
            val newRemaining = (remaining - elapsed).coerceAtLeast(0)
            activeUnlocks[currentPackage] = newRemaining
            
            // Track pending deduction for this specific package
            val currentPending = pendingDeductions[currentPackage] ?: 0L
            val totalPending = currentPending + elapsed
            
            // Log intermittently to help debug without spamming
            if (System.currentTimeMillis() % 10000 < 1000) {
                if (com.lifeforge.app.BuildConfig.DEBUG) {
                    Log.d(TAG, "Usage trace: $currentPackage has ${newRemaining/1000}s left. Pending sync: ${totalPending/1000}s")
                }
            }

            // Sync to DB when we hit 15 seconds of cumulative usage for THIS package (Optimization: reduced form 3s)
            if (totalPending >= 15000) {
                if (com.lifeforge.app.BuildConfig.DEBUG) {
                    Log.d(TAG, "Syncing $totalPending ms for $currentPackage to DB.")
                }
                serviceScope.launch {
                     appLockRepository.consumeTime(currentPackage, totalPending)
                }
                pendingDeductions[currentPackage] = 0L
            } else {
                pendingDeductions[currentPackage] = totalPending
            }
            
            if (newRemaining <= 0) {
                if (com.lifeforge.app.BuildConfig.DEBUG) {
                    Log.d(TAG, "Time expired for $currentPackage")
                }
                showLockOverlay(currentPackage)
            }
        } else {
            // Already zero/blocked
            if (blockedPackages.contains(currentPackage)) {
                 showLockOverlay(currentPackage)
            }
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        
        val packageName = event.packageName?.toString() ?: return
        
        if (packageName == lastDetectedPackage) return
        
        // Ignore TRANSIENT system packages (keyboard, volume, notifications) from switching the tracking state.
        // This prevents the clock from stopping when the keyboard or a system dialog pops up.
        // ANY other app switch (Launcher, Settings, Stoppy, or other 3rd party apps) DO trigger a switch, 
        // which stops deduction for the previously active app.
        if (isTransientSystemPackage(packageName)) {
            if (com.lifeforge.app.BuildConfig.DEBUG) {
                Log.d(TAG, "Ignoring transient package: $packageName")
            }
            return
        }

        // Before we switch packages, account for the time spent on the PREVIOUS package
        updateUsage()

        // Package changed - handle transition
        lastDetectedPackage?.let { oldPkg ->
             // Sync any pending deductions for the old package immediately
             val pending = pendingDeductions[oldPkg] ?: 0L
             if (pending > 0) {
                 if (com.lifeforge.app.BuildConfig.DEBUG) {
                     Log.d(TAG, "Syncing final $pending ms for $oldPkg before switch")
                 }
                 serviceScope.launch {
                     appLockRepository.consumeTime(oldPkg, pending)
                 }
                 pendingDeductions[oldPkg] = 0L
             }
        }

        
        lastDetectedPackage = packageName
        usageStartTime = System.currentTimeMillis()
        
        if (com.lifeforge.app.BuildConfig.DEBUG) {
            Log.d(TAG, "Foreground package changed: $packageName")
        }
        
        // Immediate check
        if (blockedPackages.contains(packageName)) {
            if (!isPackageUnlocked(packageName)) {
                if (com.lifeforge.app.BuildConfig.DEBUG) {
                    Log.d(TAG, "Blocked app detected: $packageName")
                }
                showLockOverlay(packageName)
            } else {
                if (com.lifeforge.app.BuildConfig.DEBUG) {
                    Log.d(TAG, "App usage started: $packageName")
                }
            }
            return
        }

        if (packageName == com.lifeforge.app.BuildConfig.APPLICATION_ID) return
        if (isSystemPackage(packageName)) return
    }
    
    override fun onInterrupt() { }
    
    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
    }
    
    private fun loadBlockedPackages() {
        serviceScope.launch {
            appLockRepository.getBlockedApps().collect { apps ->
                blockedPackages.clear()
                apps.forEach { app -> blockedPackages.add(app.packageName) }
                _registeredAppsCount.value = apps.size
            }
        }
    }
    
     private fun loadActiveUnlocks() {
         serviceScope.launch {
             appLockRepository.getActiveUnlocks().collect { unlocks ->
                 // Group duplicates by package and sum them up
                 val currentDbTotals = unlocks.groupBy { it.packageName }
                     .mapValues { (_, records) ->
                         records.sumOf { 
                             if (it.isUsageBased) it.remainingTimeMs 
                             else (it.expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
                         }
                     }

                 currentDbTotals.forEach { (pkg, dbTotal) ->
                     val lastDb = lastKnownDbTotal[pkg] ?: -1L
                     val currentLocal = activeUnlocks[pkg] ?: 0L
                     
                     // UPDATE logic:
                     // 1. Initial load (lastDb == -1)
                     // 2. Significant INCREASE in DB (purchase/workout)
                     // 3. DB dropped to 0 (reset/expiration)
                     
                     if (lastDb == -1L || dbTotal > lastDb + 10000 || (dbTotal == 0L && currentLocal > 0)) {
                         if (com.lifeforge.app.BuildConfig.DEBUG) {
                             Log.d(TAG, "Syncing local for $pkg: $currentLocal -> $dbTotal ms (DB changed/init)")
                         }
                         activeUnlocks[pkg] = dbTotal
                         lastKnownDbTotal[pkg] = dbTotal
                     } else if (dbTotal < lastDb) {
                         // This is likely our own deduction coming back from DB sync.
                         // Just update lastDb so we don't think it's a "change" next time.
                         lastKnownDbTotal[pkg] = dbTotal
                     }
                 }
                 
                 // Remove expired ones from maps
                 val activePkgs = currentDbTotals.keys
                 activeUnlocks.keys.removeIf { !activePkgs.contains(it) }
                 lastKnownDbTotal.keys.removeIf { !activePkgs.contains(it) }
             }
         }
    }

    
    private fun showLockOverlay(packageName: String) {
        val appName = getAppName(packageName)
        
        // MIUI sometimes ignores the first start activity call from background
        // We retry a few times to ensure it shows up
        serviceScope.launch {
            val maxRetries = if (XiaomiPermissionHelper.isXiaomiDevice()) 3 else 1
            
            repeat(maxRetries) { attempt ->
                try {
                    val intent = Intent(this@AppDetectorService, LockOverlayActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                                Intent.FLAG_ACTIVITY_NO_ANIMATION or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK // Ensure fresh task
                        putExtra("app_name", appName)
                        putExtra("package_name", packageName)
                    }
                    startActivity(intent)
                    
                    if (attempt < maxRetries - 1) {
                         delay(100)
                    }
                } catch (e: Exception) {
                    if (com.lifeforge.app.BuildConfig.DEBUG) {
                        Log.e(TAG, "Failed to show overlay (attempt $attempt)", e)
                    }
                }
            }
        }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            // Fallback for system apps or uninstalled apps that might still trigger events
            try {
                packageName.split(".").last().replaceFirstChar { it.uppercase() }
            } catch (e2: Exception) {
                "Unknown App"
            }
        }
    }
    
    private fun isTransientSystemPackage(packageName: String): Boolean {
        val transientPrefixes = listOf(
            "com.samsung.android.honeyboard", // Samsung Keyboard
            "com.google.android.inputmethod.latin", // Gboard
            "com.android.systemui", // Notification shade, volume, etc
            "android" // System dialogs
        )
        return transientPrefixes.any { packageName.startsWith(it) }
    }

    private fun isStableSystemPackage(packageName: String): Boolean {
        val stablePrefixes = listOf(
            "com.android.settings",
            "com.android.launcher",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.huawei.android.launcher",
            "com.oppo.launcher",
            "com.miui.home"
        )
        return stablePrefixes.any { packageName.startsWith(it) }
    }

    private fun isSystemPackage(packageName: String): Boolean {
        return isTransientSystemPackage(packageName) || isStableSystemPackage(packageName)
    }
}
