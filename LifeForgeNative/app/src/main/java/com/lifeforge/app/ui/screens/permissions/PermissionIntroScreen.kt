package com.lifeforge.app.ui.screens.permissions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeforge.app.ui.components.GradientButton
import com.lifeforge.app.ui.theme.*
import com.lifeforge.app.util.CoreServiceStarter
import com.lifeforge.app.util.XiaomiPermissionHelper

@Composable
fun PermissionIntroScreen(
    onAllGranted: () -> Unit,
    viewModel: PermissionViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentPermission by viewModel.currentPermission.collectAsState()
    val allGranted by viewModel.allGranted.collectAsState()
    var showRestrictedDialog by remember { mutableStateOf(false) }
    var startedServices by remember { mutableStateOf(false) }

    // Observe lifecycle effectively to re-check permissions when coming back from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Initial check
    LaunchedEffect(Unit) {
        viewModel.checkPermissions()
    }

    LaunchedEffect(allGranted) {
        if (allGranted && !startedServices) {
            CoreServiceStarter.startCoreServices(context)
            startedServices = true
            onAllGranted()
        }
    }

    // Runtime Permission Launcher
    val runtimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.checkPermissions()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Dark Blue Background
    ) {
        // Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E293B)
                        )
                    )
                )
        )
        
        // Progress steps (Decorative)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(8) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .padding(horizontal = 2.dp)
                        .background(
                            if (it < 4) Accent else Color.White.copy(alpha = 0.1f), 
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "SYSTEM DEPLOYMENT",
                color = Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Enable the core guard protocols.",
                color = TextSecondary,
                fontSize = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            // Permission Card
            currentPermission?.let { permission ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Accent.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(20.dp))
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = permission.title,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = permission.description,
                                color = TextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            } ?: run {
                 // Fallback or Loading
                 CircularProgressIndicator(color = Accent)
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Action Button
            Button(
                onClick = { 
                     currentPermission?.let { permission ->
                        when(permission.type) {
                            PermissionType.RUNTIME -> {
                                val missing = viewModel.getMissingRuntimePermissions(context)
                                runtimeLauncher.launch(missing.toTypedArray())
                            }
                            PermissionType.OVERLAY -> {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                context.startActivity(intent)
                            }
                            PermissionType.USAGE_STATS -> {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            }
                            PermissionType.BATTERY -> {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                                try {
                                    context.startActivity(intent)
                                } catch(e: Exception) {
                                     context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                }
                            }
                            PermissionType.XIAOMI_AUTOSTART -> {
                                viewModel.markXiaomiShown()
                                val intent = XiaomiPermissionHelper.getAutostartIntent(context)
                                if (intent != null) context.startActivity(intent) else viewModel.checkPermissions()
                            }
                            PermissionType.XIAOMI_POPUP -> {
                                viewModel.markXiaomiPopupShown()
                                val intent = XiaomiPermissionHelper.getBackgroundPopupIntent(context)
                                if (intent != null) context.startActivity(intent) else viewModel.checkPermissions()
                            }
                            PermissionType.ACCESSIBILITY -> {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                     }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Next Step",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // "Trouble enabling?" link for Accessibility
            if (currentPermission?.type == PermissionType.ACCESSIBILITY) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { showRestrictedDialog = true }
                ) {
                    Text(
                        text = "Trouble enabling? (Restricted Settings)",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
    
    if (showRestrictedDialog) {
        RestrictedSettingsDialog(onDismiss = { showRestrictedDialog = false })
    }
}

@Composable
fun RestrictedSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Text(
                text = "Restricted Settings",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "If you see 'Restricted Settings' preventing you from enabling accessibility:",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "1. Click 'Open App Info' below.",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "2. Tap on the 3 dots in top-right corner OR scroll down.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "3. Select 'Allow restricted settings'.",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "4. Return here and try again.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "This only happens for manually installed apps (APK). Play Store users won't see this.",
                    color = Accent,
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) {
                Text("Open App Info", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
