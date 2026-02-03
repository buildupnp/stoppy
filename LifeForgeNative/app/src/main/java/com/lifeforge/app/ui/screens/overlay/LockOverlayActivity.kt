package com.lifeforge.app.ui.screens.overlay

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import com.lifeforge.app.ui.theme.StoppyTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.BorderStroke
import com.lifeforge.app.accessibility.AppDetectorService
import com.lifeforge.app.data.repository.AppLockRepository
import com.lifeforge.app.data.repository.AuthRepository
import com.lifeforge.app.data.repository.CoinRepository
import com.lifeforge.app.ui.components.GlassCard
import com.lifeforge.app.ui.components.GradientButton
import com.lifeforge.app.ui.screens.forge.AIWorkoutScreen
import com.lifeforge.app.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Full-screen overlay shown when a blocked app is detected.
 * This activity cannot be dismissed with back button.
 * User must spend coins or go to Forge to earn more.
 */
@AndroidEntryPoint
class LockOverlayActivity : ComponentActivity() {
    
    @Inject lateinit var coinRepository: CoinRepository
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var appLockRepository: AppLockRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // MIUI & General Lock Screen Handling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Critical for MIUI to display over other apps reliably
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                @Suppress("DEPRECATION")
                window.setType(android.view.WindowManager.LayoutParams.TYPE_PHONE)
            }
        } catch (e: Exception) {
            // If permission not granted, we skip this but activity might not show fully over everything
            android.util.Log.e("LockOverlay", "Failed to set window type", e)
        }

        val appName = intent.getStringExtra("app_name") ?: "App"
        val packageName = intent.getStringExtra("package_name") ?: ""
        
        // Get app icon
        val appIcon: Drawable? = try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
        
        setContent {
            StoppyTheme {
                val coinBalance by coinRepository.getBalanceFlow(authRepository.getCurrentUserId() ?: "").collectAsState(initial = 0)
                val emergencyUnlockLimit = appLockRepository.getEmergencyUnlockLimit()
                val emergencyUnlockCount = appLockRepository.getEmergencyUnlockCount()
                
                LockOverlayScreen(
                    appName = appName,
                    packageName = packageName,
                    appIcon = appIcon,
                    coinBalance = coinBalance,
                    emergencyUnlockLimit = emergencyUnlockLimit,
                    emergencyUnlockCount = emergencyUnlockCount,
                    onUnlock = { minutes, cost -> 
                        lifecycleScope.launch {
                            val userId = authRepository.getCurrentUserId() ?: return@launch
                            
                            // Get the managed app entity from the Flow
                            val managedApps = appLockRepository.getAllManagedApps().first()
                            val managedApp = managedApps.find { it.packageName == packageName }
                            
                            if (managedApp != null) {
                                // Use the repository method which creates the unlock record
                                val result = appLockRepository.unlockApp(userId, managedApp, minutes)
                                
                                if (result.isSuccess) {
                                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                    launchIntent?.let { startActivity(it) }
                                    finish()
                                } else {
                                    // Handle error (insufficient coins, etc.)
                                    android.util.Log.e("LockOverlay", "Unlock failed: ${result.exceptionOrNull()?.message}")
                                }
                            } else {
                                // Fallback: just unlock via service (shouldn't happen)
                                coinRepository.spendCoins(userId, cost, "Unlocked $appName for ${minutes}m")
                                AppDetectorService.unlockPackage(packageName, minutes * 60 * 1000L)
                                finish()
                            }
                        }
                    },
                    onEarnedTime = { minutes ->
                        lifecycleScope.launch {
                            val userId = authRepository.getCurrentUserId() ?: ""
                            appLockRepository.workoutUnlock(userId, packageName, appName, minutes)
                            // Reward message is handled inside LockOverlayScreen state, 
                            // we just need to wait a bit before finish if screen handles it.
                            // But actually let's handle the delay here for safety.
                            delay(2000) 
                            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                            launchIntent?.let { startActivity(it) }
                            finish()
                        }
                    },
                    onEmergencyUnlock = {
                        lifecycleScope.launch {
                            val result = appLockRepository.emergencyUnlock(packageName, appName)
                            
                            if (result.isSuccess) {
                                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                launchIntent?.let { startActivity(it) }
                                finish()
                            } else {
                                // Show error message
                                android.widget.Toast.makeText(
                                    this@LockOverlayActivity,
                                    result.exceptionOrNull()?.message ?: "Emergency unlock failed",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

data class RippleData(
    val center: androidx.compose.ui.geometry.Offset,
    val startTime: Long
)

@Composable
fun LockOverlayScreen(
    appName: String,
    packageName: String,
    appIcon: android.graphics.drawable.Drawable?,
    coinBalance: Int,
    emergencyUnlockLimit: Int,
    emergencyUnlockCount: Int,
    onUnlock: (Int, Int) -> Unit,
    onEarnedTime: (Int) -> Unit,
    onEmergencyUnlock: () -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(30) }
    var showWorkoutMode by remember { mutableStateOf(false) }
    var workoutType by remember { mutableStateOf("pushups") }
    
    // Entrance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    val slideOffset by animateIntOffsetAsState(
        targetValue = if (isVisible) IntOffset(0, 0) else IntOffset(0, 100),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "slide"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "fade"
    )
    
    // State for water ripples and feedback
    val ripples = remember { mutableStateListOf<RippleData>() }
    var rewardMessage by remember { mutableStateOf<String?>(null) }
    var showUnlockConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val unlockOptions = listOf(
        Triple(15, 10, false),   // minutes, cost, isBest
        Triple(30, 18, true),    // Best value
        Triple(60, 30, false)
    )

    if (showWorkoutMode) {
        AIWorkoutScreen(
            workoutType = workoutType,
            onClose = { showWorkoutMode = false },
            onFinish = { count ->
                // 1 pushup/squat = 1 minute of unlock time (user requested 1:1 ratio)
                val earnedMinutes = count
                if (earnedMinutes > 0) {
                    rewardMessage = "Awesome! You earned $earnedMinutes minutes."
                    onEarnedTime(earnedMinutes)
                } else {
                    showWorkoutMode = false
                }
            }

        )
        return
    }
    
    // Animation trigger for smooth redraws - Optimized: Only run when ripples exist
    val activeRipples by remember { derivedStateOf { ripples.isNotEmpty() } }
    
    val frameTrigger by if (activeRipples) {
        val infiniteTransition = rememberInfiniteTransition()
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    
    // Cleanup aged ripples & reward message
    LaunchedEffect(Unit) {
        while(true) {
            delay(100)
            val currentTime = System.currentTimeMillis()
            ripples.removeAll { currentTime - it.startTime > 800 } 
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = slideOffset.y.toFloat()
                this.alpha = alpha
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> 
                        ripples.add(RippleData(offset, System.currentTimeMillis()))
                    },
                    onDrag = { change, _ -> 
                        ripples.add(RippleData(change.position, System.currentTimeMillis()))
                    }
                )
            }
            .background(
                brush = getAppBackgroundBrush(true) // Lock screen is usually dark
            )
    ) {
        // Wavy Water Ripples - Optimized Drawing with Frame Trigger
        Canvas(modifier = Modifier.fillMaxSize().drawWithCache {
            onDrawBehind {
                // Accessing frameTrigger forces redraw every frame
                val _trigger = frameTrigger 
                val currentTime = System.currentTimeMillis()
                ripples.forEach { ripple ->
                    val progress = (currentTime - ripple.startTime) / 800f
                    if (progress in 0f..1f) {
                        val alpha = 1f - progress
                        val radius = progress * 250f
                        drawCircle(
                            color = Accent.copy(alpha = alpha * 0.3f),
                            radius = radius,
                            center = ripple.center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }
        }) { }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Reward Overlay message
            AnimatedVisibility(
                visible = rewardMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                rewardMessage?.let { msg ->
                    GlassCard(
                        modifier = Modifier.padding(top = 16.dp),
                        glowColor = Success
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Success)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(msg, color = White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            // Top Section - Lock Icon & App Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(White.copy(alpha = 0.05f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        androidx.compose.foundation.Image(
                            bitmap = appIcon.toBitmap().asImageBitmap(),
                            contentDescription = appName,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Alert,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp)) // Increased Gap
                
                Text(
                    text = appName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = White,
                    fontWeight = FontWeight.Black
                )
                
                Text(
                    text = "BLOCKED BY STOPPY",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Is this temporary distraction worth your long-term goals?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
            
            // Center Section - Balance & Unlock Options
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BALANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$coinBalance",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LC",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Accent,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Unlock Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    unlockOptions.forEach { (minutes, cost, isBest) ->
                        val isSelected = selectedMinutes == minutes
                        val canAfford = coinBalance >= cost
                        
                        UnlockOptionCard(
                            minutes = minutes,
                            cost = cost,
                            isBest = isBest,
                            isSelected = isSelected,
                            canAfford = canAfford,
                            onClick = { if (canAfford) selectedMinutes = minutes },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Bottom Section - Actions
            Column(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val selectedCost = unlockOptions.find { it.first == selectedMinutes }?.second ?: 0
                val canAffordSelected = coinBalance >= selectedCost
                
                if (canAffordSelected) {
                    Button(
                        onClick = { showUnlockConfirm = true }, // Trigger confirm dialog first
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Alert),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Unlock for $selectedCost LC", color = White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "NOT ENOUGH COINS",
                        color = Alert,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Dual Workout Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { 
                            workoutType = "pushups"
                            showWorkoutMode = true 
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Accent.copy(alpha = 0.3f))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text("PUSH-UPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { 
                            workoutType = "squats"
                            showWorkoutMode = true 
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = ButtonDefaults.buttonColors(containerColor = Success.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Success.copy(alpha = 0.3f))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Accessibility, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text("SQUATS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Emergency unlock - show remaining count with pulse animation
                val emergencyRemaining = emergencyUnlockLimit - emergencyUnlockCount
                if (emergencyUnlockLimit > 0) {
                    // Subtle pulse animation when available
                    val infiniteTransition = rememberInfiniteTransition(label = "emergency_pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.7f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )
                    
                    TextButton(
                        onClick = onEmergencyUnlock,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = emergencyRemaining > 0
                    ) {
                        Text(
                            text = if (emergencyRemaining > 0) 
                                "Emergency Unlock (15m) - $emergencyRemaining left today" 
                            else 
                                "Emergency Unlock - Daily limit reached",
                            color = if (emergencyRemaining > 0) TextSecondary.copy(alpha = pulseAlpha) else Alert.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                        )
                    }
                }
            }
        }
    }

    // Unlock Confirmation Dialog
    if (showUnlockConfirm) {
        val selectedCost = unlockOptions.find { it.first == selectedMinutes }?.second ?: 0
        
        // Animated crying emoji
        val infiniteTransition = rememberInfiniteTransition(label = "crying")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        val offsetY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -4f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bounce"
        )
        
        AlertDialog(
            onDismissRequest = { showUnlockConfirm = false },
            containerColor = CardDark,
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated crying emoji
                    Text(
                        text = "😢",
                        fontSize = 48.sp,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationY = offsetY
                            }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Are you sure?",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            text = {
                Text(
                    text = "Think about your long-term goals before you proceed. This distraction is only temporary, but your progress is forever.",
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showUnlockConfirm = false
                        onUnlock(selectedMinutes, selectedCost) 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Alert)
                ) {
                    Text("I'M SURE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockConfirm = false }) {
                    Text("STAY FOCUSED", color = Accent, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun UnlockOptionCard(
    minutes: Int,
    cost: Int,
    isBest: Boolean,
    isSelected: Boolean,
    canAfford: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (canAfford) 1f else 0.5f
    
    Box(modifier = modifier) {
        Button(
            onClick = onClick,
            enabled = canAfford,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isBest) Modifier.height(96.dp) else Modifier.height(88.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) Accent.copy(alpha = 0.2f) else CardDark.copy(alpha = 0.5f),
                disabledContainerColor = CardDark.copy(alpha = 0.3f)
            ),
            border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${minutes}m",
                    style = MaterialTheme.typography.titleMedium,
                    color = White.copy(alpha = alpha),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$cost LC",
                    style = MaterialTheme.typography.bodySmall,
                    color = Accent.copy(alpha = alpha)
                )
            }
        }
        
        // Best badge
        if (isBest) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp),
                color = Accent,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "BEST",
                    style = MaterialTheme.typography.labelSmall,
                    color = White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
