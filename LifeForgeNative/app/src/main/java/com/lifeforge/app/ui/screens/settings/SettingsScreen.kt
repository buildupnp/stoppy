package com.lifeforge.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeforge.app.ui.components.GlassCard
import com.lifeforge.app.ui.components.GradientButton
import com.lifeforge.app.ui.components.PremiumSlideIn
import com.lifeforge.app.ui.theme.*

import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val isDark = when(LocalThemeMode.current) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = getAppBackgroundBrush(isDark)
            )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
            // Header
            PremiumSlideIn {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Profile Section
            PremiumSlideIn(delay = 100) {
                GlassCard(glowColor = Color.Transparent) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.displayName.firstOrNull()?.toString()?.uppercase() ?: "U",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uiState.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        var showNameDialog by remember { mutableStateOf(false) }
                        var tempName by remember { mutableStateOf("") }
                        
                        if (showNameDialog) {
                            AlertDialog(
                                onDismissRequest = { showNameDialog = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                                title = { Text("Update Name", color = MaterialTheme.colorScheme.onSurface) },
                                text = {
                                    OutlinedTextField(
                                        value = tempName,
                                        onValueChange = { tempName = it },
                                        label = { Text("Full Name") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            cursorColor = MaterialTheme.colorScheme.primary,
                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                confirmButton = {
                                    GradientButton(
                                        text = "Update",
                                        onClick = { 
                                            viewModel.updateDisplayName(tempName)
                                            showNameDialog = false 
                                        },
                                        modifier = Modifier.width(100.dp)
                                    )
                                },
                                dismissButton = {
                                    TextButton(onClick = { showNameDialog = false }) {
                                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            )
                        }
    
                        IconButton(onClick = { 
                            tempName = uiState.displayName
                            showNameDialog = true 
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Support Section
            PremiumSlideIn(delay = 200) {
                Column {
                    Text(
                        text = "Support",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    GlassCard(glowColor = Color.Transparent) {
                        SettingsActionItem(
                            icon = Icons.Outlined.Email,
                            title = "Send Feedback",
                            subtitle = "Help us improve Stoppy",
                            onClick = { onNavigate("feedback") }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Preferences Section
            PremiumSlideIn(delay = 300) {
                Column {
                    Text(
                        text = "Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassCard(glowColor = Color.Transparent) {
                            SettingsToggleItem(
                                icon = Icons.Outlined.Notifications,
                                title = "Notifications",
                                subtitle = "Get reminders and updates",
                                isChecked = uiState.notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                            )
                        }
                        
                        GlassCard(glowColor = Color.Transparent) {
                            SettingsToggleItem(
                                icon = Icons.Outlined.Vibration,
                                title = "Haptic Feedback",
                                subtitle = "Vibrate on actions",
                                isChecked = uiState.hapticsEnabled,
                                onCheckedChange = { viewModel.setHapticsEnabled(it) }
                            )
                        }
                        
                        GlassCard(glowColor = Color.Transparent) {
                            SettingsToggleItem(
                                icon = Icons.Default.VolumeUp,
                                title = "Sound Effects",
                                subtitle = "Play sounds on coin earn",
                                isChecked = uiState.soundEnabled,
                                onCheckedChange = { viewModel.setSoundEnabled(it) }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Appearance Section
            PremiumSlideIn(delay = 400) {
                Column {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    GlassCard(glowColor = Color.Transparent) {
                        Column {
                            var showThemeDialog by remember { mutableStateOf(false) }
                            
                            if (showThemeDialog) {
                                AlertDialog(
                                    onDismissRequest = { showThemeDialog = false },
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    title = { Text("Choose Theme", color = MaterialTheme.colorScheme.onSurface) },
                                    text = {
                                        Column {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = uiState.themeMode == "dark",
                                                    onClick = { viewModel.setTheme("dark") },
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = MaterialTheme.colorScheme.primary,
                                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("Dark Mode", color = MaterialTheme.colorScheme.onSurface)
                                                    Text(
                                                        "Dark glassmorphism theme",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = uiState.themeMode == "light",
                                                    onClick = { viewModel.setTheme("light") },
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = MaterialTheme.colorScheme.primary,
                                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("Light Mode", color = MaterialTheme.colorScheme.onSurface)
                                                    Text(
                                                        "Bright and clean theme",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = uiState.themeMode == "system",
                                                    onClick = { viewModel.setTheme("system") },
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = MaterialTheme.colorScheme.primary,
                                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("System", color = MaterialTheme.colorScheme.onSurface)
                                                    Text(
                                                        "Follow system settings",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showThemeDialog = false }) {
                                            Text("Done", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                            
                            SettingsActionItem(
                                icon = Icons.Outlined.DarkMode,
                                title = "Theme",
                                subtitle = when (uiState.themeMode) {
                                    "light" -> "Light Mode"
                                    "system" -> "System Default"
                                    else -> "Dark Mode"
                                },
                                onClick = { showThemeDialog = true }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Emergency Unlock Section
            PremiumSlideIn(delay = 500) {
                Column {
                    Text(
                        text = "Emergency Unlock",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    GlassCard(glowColor = Color.Transparent) {
                        var showEmergencyDialog by remember { mutableStateOf(false) }
                        
                        if (showEmergencyDialog) {
                            AlertDialog(
                                onDismissRequest = { showEmergencyDialog = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                                title = { Text("Emergency Unlock Limit", color = MaterialTheme.colorScheme.onSurface) },
                                text = {
                                    Column {
                                        Text(
                                            text = "Set maximum emergency unlocks allowed per day. Resets at midnight.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        // Options 0-10
                                        (0..10).forEach { limit ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = uiState.emergencyUnlocksPerDay == limit,
                                                    onClick = { viewModel.updateEmergencyUnlockLimit(limit) },
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = MaterialTheme.colorScheme.primary,
                                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (limit == 0) "Disabled" else "$limit ${if (limit == 1) "time" else "times"} per day",
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showEmergencyDialog = false }) {
                                        Text("Done", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                        
                        SettingsActionItem(
                            icon = Icons.Outlined.Warning,
                            title = "Daily Emergency Unlock Limit",
                            subtitle = if (uiState.emergencyUnlocksPerDay == 0) "Disabled" 
                                       else "${uiState.emergencyUnlocksPerDay} ${if (uiState.emergencyUnlocksPerDay == 1) "time" else "times"} per day",
                            onClick = { showEmergencyDialog = true }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Permissions Section
            PremiumSlideIn(delay = 600) {
                Column {
                    Text(
                        text = "Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassCard(glowColor = Color.Transparent) {
                            SettingsActionItem(
                                icon = Icons.Outlined.Accessibility,
                                title = "Accessibility Service",
                                subtitle = "Required for app blocking",
                                onClick = { 
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.util.Log.w("SettingsScreen", "Failed to open accessibility settings: ${e.message}")
                                    }
                                }
                            )
                        }
                        
                        GlassCard(glowColor = Color.Transparent) {
                            SettingsActionItem(
                                icon = Icons.Outlined.AdminPanelSettings,
                                title = "Usage Access",
                                subtitle = "Required for app monitoring",
                                onClick = { 
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.util.Log.w("SettingsScreen", "Failed to open usage access settings: ${e.message}")
                                    }
                                }
                            )
                        }
                        
                        GlassCard(glowColor = Color.Transparent) {
                            SettingsActionItem(
                                icon = Icons.Outlined.Layers,
                                title = "Overlay Permission",
                                subtitle = "Required for lock screen",
                                onClick = { 
                                     val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                     intent.data = android.net.Uri.parse("package:${context.packageName}")
                                     intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                     try {
                                         context.startActivity(intent)
                                     } catch (e: Exception) {
                                         android.util.Log.w("SettingsScreen", "Failed to open overlay permission settings: ${e.message}")
                                     }
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Account Section
            PremiumSlideIn(delay = 700) {
                Column {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassCard(glowColor = Color.Transparent) {
                            SettingsActionItem(
                                icon = Icons.Outlined.Sync,
                                title = "Sync Data",
                                subtitle = "Last synced: Just now",
                                onClick = { /* Sync */ }
                            )
                        }
                        
                        GlassCard(glowColor = Color.Transparent) {
                            SettingsActionItem(
                                icon = Icons.Outlined.DeleteForever,
                                title = "Clear Local Data",
                                subtitle = "Reset app data",
                                onClick = { /* Clear data */ },
                                isDestructive = true
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Developer Options
            PremiumSlideIn(delay = 800) {
                Column {
                    Text(
                        text = "Developer Options",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    GlassCard(glowColor = Color.Transparent) {
                        SettingsActionItem(
                            icon = Icons.Outlined.Science,
                            title = "Load Demo Data (6 Months)",
                            subtitle = "Populates local DB with sample history",
                            onClick = { viewModel.seedDemoData() }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Legal Section
            PremiumSlideIn(delay = 900) {
                Column {
                    Text(
                        text = "Legal",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    GlassCard(glowColor = Color.Transparent) {
                        SettingsActionItem(
                            icon = Icons.Outlined.PrivacyTip,
                            title = "Privacy Policy",
                            subtitle = "Read our data usage policy",
                            onClick = { onNavigate("privacy_policy") }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sign Out Button
            PremiumSlideIn(delay = 950) {
                Button(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Out",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Version info
            PremiumSlideIn(delay = 1000) {
                Text(
                    text = "Stoppy v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else Accent,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
