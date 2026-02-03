package com.lifeforge.app.ui.screens.guardian

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.app.data.repository.AppLockRepository
import com.lifeforge.app.data.repository.AuthRepository
import com.lifeforge.app.data.repository.CoinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuardianUiState(
    val coinBalance: Int = 0,
    val isServiceRunning: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val blockedAppsCount: Int = 0,
    val managedApps: List<ManagedApp> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class GuardianViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLockRepository: AppLockRepository,
    private val coinRepository: CoinRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GuardianUiState())
    val uiState: StateFlow<GuardianUiState> = _uiState.asStateFlow()
    
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    
    val filteredApps = kotlinx.coroutines.flow.combine(
        _installedApps,
        _uiState
    ) { apps, state ->
        if (state.searchQuery.isBlank()) {
            apps
        } else {
            apps.filter { 
                it.name.contains(state.searchQuery, ignoreCase = true) || 
                it.packageName.contains(state.searchQuery, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    data class AppInfo(val name: String, val packageName: String)
    
    val commonApps = appLockRepository.commonApps // Keep for reference if needed

    init {
        loadData()
        observeData()
        loadInstalledApps() // Fetch installed apps on init
    }
    
    private fun loadInstalledApps() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val mainIntent = Intent(Intent.ACTION_MAIN, null)
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                
                val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
                
                val apps = resolveInfos.map { resolveInfo ->
                    val appName = resolveInfo.loadLabel(pm).toString()
                    val packageName = resolveInfo.activityInfo.packageName
                    AppInfo(appName, packageName)
                }.distinctBy { it.packageName }.sortedBy { it.name }
                
                _installedApps.value = apps
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    

    fun resetAppTimer(packageName: String) {
        viewModelScope.launch {
            appLockRepository.resetUnlock(packageName)
            com.lifeforge.app.accessibility.AppDetectorService.clearUnlock(packageName)
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            // Observe accessibility service status
            launch {
                com.lifeforge.app.accessibility.AppDetectorService.isRunning.collect { running ->
                    _uiState.value = _uiState.value.copy(
                        isServiceRunning = running,
                        isOverlayGranted = isOverlayPermissionGranted()
                    )
                }
            }

            // Observe coin balance
            launch {
                coinRepository.getBalanceFlow(userId).collectLatest { balance ->
                    _uiState.value = _uiState.value.copy(coinBalance = balance)
                }
            }
            
            // Observe managed apps & unlocks
            launch {
                kotlinx.coroutines.flow.combine(
                    appLockRepository.getAllManagedApps(),
                    appLockRepository.getActiveUnlocks()
                ) { apps, unlocks -> Pair(apps, unlocks) }
                .collectLatest { (entities, unlocks) ->
                    val currentTime = System.currentTimeMillis()
                    val managedAppsList = entities.map { entity ->
                        val unlock = unlocks.find { it.packageName == entity.packageName }
                        val isUnlocked = unlock != null && (
                            (unlock.isUsageBased && unlock.remainingTimeMs > 0) ||
                            (!unlock.isUsageBased && unlock.expiresAt > currentTime)
                        )
                        
                        ManagedApp(
                            id = entity.packageName,
                            name = entity.appName,
                            packageName = entity.packageName,
                            isBlocked = entity.isBlocked,
                            isCurrentlyUnlocked = isUnlocked,
                            unlockExpiresAt = unlock?.expiresAt
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        managedApps = managedAppsList,
                        blockedAppsCount = managedAppsList.count { it.isBlocked && !it.isCurrentlyUnlocked }
                    )
                }
            }
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isServiceRunning = checkAccessibilityService(),
                isOverlayGranted = isOverlayPermissionGranted()
            )
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            // Sync from backend
            appLockRepository.syncFromSupabase(userId)
        }
    }
    
    private fun checkAccessibilityService(): Boolean {
        var accessibilityEnabled = 0
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
        
        if (accessibilityEnabled == 1) {
            val service = "${context.packageName}/${com.lifeforge.app.accessibility.AppDetectorService::class.java.name}"
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return settingValue?.contains(service) == true
        }
        return false
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun isOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(context)
    }
    
    fun openOverlaySettings() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    fun toggleAppBlock(app: ManagedApp) {
        viewModelScope.launch {
            val isBlocked = !app.isBlocked
            appLockRepository.toggleBlock(app.packageName, isBlocked)
            if (isBlocked) {
                com.lifeforge.app.accessibility.AppDetectorService.addBlockedPackage(app.packageName)
            } else {
                com.lifeforge.app.accessibility.AppDetectorService.removeBlockedPackage(app.packageName)
            }
        }
    }
    
    fun unlockApp(app: ManagedApp, minutes: Int) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            val tempEntity = com.lifeforge.app.data.local.database.entities.ManagedAppEntity(
                packageName = app.packageName,
                appName = app.name,
                isBlocked = app.isBlocked
            )
            
            val result = appLockRepository.unlockApp(userId, tempEntity, minutes)
            
            if (result.isSuccess) {
                // UI will update automatically
            }
        }
    }
    
    fun removeApp(app: ManagedApp) {
        viewModelScope.launch {
             val tempEntity = com.lifeforge.app.data.local.database.entities.ManagedAppEntity(
                packageName = app.packageName,
                appName = app.name,
                isBlocked = app.isBlocked
            )
            appLockRepository.removeApp(tempEntity)
        }
    }
    
    fun addApp(name: String, packageName: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            appLockRepository.addApp(userId, name, packageName)
        }
    }


    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
}
