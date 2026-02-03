package com.lifeforge.app.ui.screens.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.app.ui.components.CoinBadge
import com.lifeforge.app.ui.components.GlassCard
import com.lifeforge.app.ui.components.GradientButton
import com.lifeforge.app.ui.theme.*
import androidx.compose.foundation.isSystemInDarkTheme
import com.lifeforge.app.ui.components.PremiumSlideIn

@Composable
fun GuardianScreen(
    viewModel: GuardianViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddAppDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var showOverlayDialog by remember { mutableStateOf(false) }
    var selectedAppForUnlock by remember { mutableStateOf<ManagedApp?>(null) }
    var appForReset by remember { mutableStateOf<ManagedApp?>(null) }
    
    val coinBalance = uiState.coinBalance
    // Theme support
    val themeMode = LocalThemeMode.current
    val isDark = when(themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = getAppBackgroundBrush(isDark)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding() / 2)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
            // Header
            PremiumSlideIn {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Guardian",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your Digital Fortress",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    CoinBadge(amount = uiState.coinBalance)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 1. Protection Summary Card (New Polish)
            PremiumSlideIn(delay = 100) {
                GlassCard(glowColor = if (uiState.isServiceRunning) Color.Transparent else Alert) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.isServiceRunning) Icons.Default.Shield else Icons.Default.ShieldMoon,
                                    contentDescription = null,
                                    tint = if (uiState.isServiceRunning) Success else Alert,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (uiState.isServiceRunning) "Protection Active" else "Shield Offline",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (uiState.isServiceRunning) "Monitoring ${uiState.blockedAppsCount} restricted apps" else "Enable service to start blocking",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            if (!uiState.isServiceRunning) {
                                Button(
                                    onClick = { showAccessibilityDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text("ACTIVATE", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            
            if (uiState.isServiceRunning && !uiState.isOverlayGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                // More compact permission warning
                PremiumSlideIn(delay = 200) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Transparent)
                            .border(1.dp, Alert.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .clickable { showOverlayDialog = true }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Alert, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Overlay Permission Missing - Tap to fix",
                                style = MaterialTheme.typography.bodySmall,
                                color = White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Managed Apps Header
            PremiumSlideIn(delay = 300) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Managed Apps",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showAddAppDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Accent.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add app",
                            tint = Accent
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Apps List
            if (uiState.managedApps.isEmpty()) {
                PremiumSlideIn(delay = 400) {
                    GlassCard(glowColor = Color.Transparent) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No apps blocked yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isDark) White else Black,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add apps to start protecting your focus",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) TextSecondary else TextSecondaryLight,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(uiState.managedApps) { index, app ->
                        PremiumSlideIn(delay = 300 + (index * 100)) {
                            AppListItem(
                                app = app,
                                onToggleBlock = { viewModel.toggleAppBlock(app) },
                                onUnlock = { selectedAppForUnlock = app },
                                onReset = { appForReset = app },
                                onRemove = { viewModel.removeApp(app) }
                            )
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
    
    // Unlock Dialog
    selectedAppForUnlock?.let { app ->
        UnlockDialog(
            app = app,
            coinBalance = uiState.coinBalance,
            onDismiss = { selectedAppForUnlock = null },
            onUnlock = { minutes ->
                viewModel.unlockApp(app, minutes)
                selectedAppForUnlock = null
            }
        )
    }
    
    // Reset Confirmation Dialog
    appForReset?.let { app ->
        AlertDialog(
            onDismissRequest = { appForReset = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Reset ${app.name}?",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will set your remaining time to 0 and re-block the app immediately.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAppTimer(app.packageName)
                        appForReset = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Alert)
                ) {
                    Text("RESET", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { appForReset = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // Add App Dialog from Installed Apps list
    if (showAddAppDialog) {
        // Observe data
        val filteredApps by viewModel.filteredApps.collectAsState(initial = emptyList())
        
        AlertDialog(
            onDismissRequest = { showAddAppDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { 
                Text(
                    text = "Add App to Block",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Column {
                    Text(
                        text = "Select an app to manage:",
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        placeholder = { Text("Search apps...", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Accent) },
                        trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Close, null, tint = TextSecondary)
                                }
                            }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    

                    if (filteredApps.isEmpty() && uiState.searchQuery.isEmpty()) {
                         Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                             Text("No apps added yet", color = TextSecondary)
                         }
                    } else if (filteredApps.isEmpty()) {
                         Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                             Text("No apps found", color = TextSecondary)
                         }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .height(300.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredApps) { app: com.lifeforge.app.ui.screens.guardian.GuardianViewModel.AppInfo ->
                                val isAlreadyManaged = uiState.managedApps.any { it.packageName == app.packageName }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isAlreadyManaged) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                        .clickable(enabled = !isAlreadyManaged) {
                                            viewModel.addApp(app.name, app.packageName)
                                            showAddAppDialog = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // App Icon
                                    AndroidView(
                                        factory = { context ->
                                            android.widget.ImageView(context).apply {
                                                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                            }
                                        },
                                        update = { imageView ->
                                             try {
                                                val packageManager = imageView.context.packageManager
                                                val icon = packageManager.getApplicationIcon(app.packageName)
                                                imageView.setImageDrawable(icon)
                                             } catch (e: Exception) {
                                                imageView.setImageResource(android.R.drawable.sym_def_app_icon)
                                             }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Text(
                                        text = app.name,
                                        color = if (isAlreadyManaged) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isAlreadyManaged) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Added",
                                            tint = Success,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add",
                                            tint = Accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddAppDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    // Permission Dialogs Logic
    val context = androidx.compose.ui.platform.LocalContext.current
    
    if (showAccessibilityDialog) {
        AccessibilityDialog(
            onDismiss = { showAccessibilityDialog = false },
            onConfirm = {
                showAccessibilityDialog = false
                val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback or log
                }
            }
        )
    }

    if (showOverlayDialog) {
        OverlayDialog(
            onDismiss = { showOverlayDialog = false },
            onConfirm = {
                showOverlayDialog = false
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = android.net.Uri.parse("package:${context.packageName}")
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback
                }
            }
        )
    }
    }
}


@Composable
private fun AppListItem(
    app: ManagedApp,
    onToggleBlock: () -> Unit,
    onUnlock: () -> Unit,
    onReset: () -> Unit,
    onRemove: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "scale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            isPressed = false
                        }
                    }
                )
            }
    ) {
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon
                 AndroidView(
                    factory = { context ->
                        android.widget.ImageView(context).apply {
                            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                        }
                    },
                    update = { imageView ->
                            try {
                            val packageManager = imageView.context.packageManager
                            val icon = packageManager.getApplicationIcon(app.packageName)
                            imageView.setImageDrawable(icon)
                            } catch (e: Exception) {
                            imageView.setImageResource(android.R.drawable.sym_def_app_icon)
                            }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (app.isCurrentlyUnlocked) "Unlocked" else if (app.isBlocked) "Blocked" else "Not blocking",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (app.isCurrentlyUnlocked) Success else if (app.isBlocked) Alert else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Unlock button (if blocked and not unlocked)
                if (app.isBlocked && !app.isCurrentlyUnlocked) {
                    IconButton(
                        onClick = onUnlock,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Accent.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LockOpen,
                            contentDescription = "Unlock",
                            tint = Accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                // Reset button (if unlocked)
                if (app.isCurrentlyUnlocked) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Alert.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = Alert,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
    
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Remove",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Block toggle
                Switch(
                    checked = app.isBlocked,
                    onCheckedChange = { onToggleBlock() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun UnlockDialog(
    app: ManagedApp,
    coinBalance: Int,
    onDismiss: () -> Unit,
    onUnlock: (Int) -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(30) }
    val unlockOptions = listOf(15 to 10, 30 to 18, 60 to 30) // minutes to coins
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Unlock ${app.name}",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Select unlock duration:",
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    unlockOptions.forEach { (minutes, coins) ->
                        val isSelected = selectedMinutes == minutes
                        val canAfford = coinBalance >= coins
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Accent.copy(alpha = 0.3f)
                                    else CardDark.copy(alpha = 0.5f)
                                )
                                .clickable(enabled = canAfford) { selectedMinutes = minutes }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${minutes}m",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (canAfford) White else TextSecondary.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$coins LC",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (canAfford) Accent else TextSecondary.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Your balance: ",
                        color = TextSecondary
                    )
                    Text(
                        text = "$coinBalance LC",
                        color = Accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            val cost = unlockOptions.find { it.first == selectedMinutes }?.second ?: 0
            GradientButton(
                text = "Unlock for $cost LC",
                onClick = { onUnlock(selectedMinutes) },
                enabled = coinBalance >= cost,
                modifier = Modifier.width(160.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun AccessibilityDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { 
            Text(
                "Enable App Monitoring", 
                color = MaterialTheme.colorScheme.onSurface, 
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Text(
                "To block distracting apps, LifeForge needs Accessibility permission.\n\n" +
                "1. On the next screen, find 'LifeForge'\n" +
                "2. Tap it\n" +
                "3. Switch ON 'Use LifeForge'",
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Go to Settings", color = Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun OverlayDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { 
            Text(
                "Enable Lock Screen", 
                color = MaterialTheme.colorScheme.onSurface, 
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Text(
                "To show the 'Stop' screen over blocked apps, LifeForge needs permission to display over other apps.\n\n" +
                "On the next screen, find 'LifeForge' and switch it ON.",
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Go to Settings", color = Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

