package com.lifeforge.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import com.lifeforge.app.ui.components.CoinBadge
import com.lifeforge.app.ui.components.GlassCard
import com.lifeforge.app.ui.components.GradientButton
import com.lifeforge.app.ui.components.LoginBonusDialog
import com.lifeforge.app.ui.components.PremiumSlideIn
import com.lifeforge.app.ui.theme.*
import java.util.Calendar

@Composable
fun ImpactChart(data: List<Int>, isDark: Boolean, onClick: () -> Unit = {}) {
    val accentColor = Accent
    val secondaryColor = AccentSecondary
    
    GlassCard(glowColor = accentColor.copy(alpha = 0.2f), modifier = Modifier.clickable { onClick() }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Focus Impact",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDark) White else Black,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Weekly Screen Time Saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) TextSecondary else TextSecondaryLight
                    )
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Success.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+${data.sum()}m",
                        color = Success,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Explicit CTA
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                 modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(vertical = 4.dp),
                 contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "View Statistics",
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.











                        ChevronRight,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                if (data.isEmpty()) return@Canvas
                
                val canvasWidth = size.width
                val canvasHeight = size.height
                val spacing = canvasWidth / (data.size - 1)
                val maxVal = (data.maxOrNull() ?: 1).toFloat()
                
                val points = data.mapIndexed { index, value ->
                    val x = index.toFloat() * spacing
                    val y = canvasHeight - (value.toFloat() / maxVal * canvasHeight)
                    Offset(x, y)
                }
                
                // Draw path
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        val p0 = points[i-1]
                        val p1 = points[i]
                        val controlPoint1 = Offset((p0.x + p1.x) / 2, p0.y)
                        val controlPoint2 = Offset((p0.x + p1.x) / 2, p1.y)
                        cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                    }
                }
                
                // Shadow/Glow
                drawPath(
                    path = path,
                    color = accentColor.copy(alpha = 0.3f),
                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                )
                
                // Main line
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(listOf(accentColor, secondaryColor)),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
                
                // Draw dots
                points.forEach { point ->
                    drawCircle(
                        color = if (isDark) Color(0xFF0F172A) else White,
                        radius = 6f,
                        center = point
                    )
                    drawCircle(
                        color = accentColor,
                        radius = 4f,
                        center = point,
                        style = Stroke(width = 2f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
                days.forEach { day ->
                    Text(
                        text = day,
                        fontSize = 10.sp,
                        color = if (isDark) TextSecondary else TextSecondaryLight,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showNotifications by remember { mutableStateOf(false) }
    
    // Determine effective theme for background
    val themeMode = LocalThemeMode.current
    val isDark = when(themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Show login bonus dialog
    if (uiState.showLoginBonus) {
        LoginBonusDialog(
            bonusAmount = uiState.loginBonusAmount,
            currentStreak = uiState.loginStreakDays,
            onDismiss = { viewModel.dismissLoginBonus() }
        )
    }
    
    // Notification Dialog logic removed: switching to dedicated page

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent // Allow background to show through
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = getAppBackgroundBrush(isDark))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding() / 2) // Reduce Top Gap
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp) // Optimized gaps
            ) {
            // 1. Header & Status
            PremiumSlideIn {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                Column {
                    Text(
                        text = getGreeting(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.userName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Pulse Badge & Notifications
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Notification Icon
                    IconButton(onClick = { onNavigate("notifications") }) {
                        Box {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                            if (uiState.hasUnreadNotifications) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Alert)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(uiState.pulseColor.copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(uiState.pulseColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
// Text removed for layout fix
                            Text(
                                text = uiState.pulseStatus.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = uiState.pulseColor
                            )
                        }
                    }
                }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 2. Primary Balance Display (Hero)
            PremiumSlideIn(delay = 100) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Text(
                    text = "CURRENT BALANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "${uiState.coinBalance}",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = White,
                        modifier = Modifier.shadow(
                            elevation = 20.dp,
                            shape = CircleShape,
                            spotColor = Accent,
                            ambientColor = Accent
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LC",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Accent,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                    )
                }
                Text(
                    text = "≈ ${uiState.screenTimeAvailable} total unlock capacity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), // Reduced opacity for cleaner look
                    letterSpacing = 0.5.sp
                )

                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 3. Daily Insight (Moved ABOVE Daily Progress)
            PremiumSlideIn(delay = 200) {
                GlassCard(
                    glowColor = Color.Transparent
                ) {
                    Text(
                        text = "\"${uiState.dailyQuote}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDark) White else Black,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // 4. Daily Progress
            PremiumSlideIn(delay = 300) {
                Column {
                    Text(
                        text = "Daily Progress",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    GlassCard(glowColor = Accent) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = "${uiState.todaySteps}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) White else Black
                                    )
                                    Text(
                                        text = "steps today",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isDark) TextSecondary else TextSecondaryLight
                                    )
                                }
                                CoinBadge(
                                    amount = uiState.todaySteps / 100,
                                    showPlus = true
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                thickness = 1.dp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = (uiState.todaySteps / 10000f).coerceAtMost(1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(Accent, AccentGlow)
                                            )
                                        )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = "Goal: 10,000 steps",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) TextSecondary.copy(alpha = 0.7f) else TextSecondaryLight,
                                modifier = Modifier.align(Alignment.End)
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Quick stats row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.DirectionsWalk,
                                        contentDescription = null,
                                        tint = if (isDark) TextSecondary else TextSecondaryLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${uiState.todayPushups} Push-ups",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isDark) TextPrimary else TextPrimaryLight,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🔥", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${uiState.streakDays} day streak",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isDark) TextPrimary else TextPrimaryLight,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 5. Daily Quests
            PremiumSlideIn(delay = 400) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Quests",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { onNavigate("forge") }) {
                            Text(
                                text = "More >",
                                color = Accent,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.dailyQuests.take(3).forEach { quest ->
                            QuestItem(quest, isDark) {
                               if (quest.id == "wisdom_1") {
                                   onNavigate("wisdom")
                               } else if (quest.id == "pushups_1") {
                                    onNavigate("forge")
                               }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // 6. Quick Access
            PremiumSlideIn(delay = 500) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Achievements Card
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate("achievements") }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "Achievements",
                                tint = Warning,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Achievements",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isDark) White else Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Challenges Card
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate("challenges") }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Challenges",
                                tint = Success,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Weekly Challenges",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isDark) White else Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // NEW: Screen Time Saved Chart (Zero State Handling)
            PremiumSlideIn(delay = 600) {
                if (uiState.weeklySavedMinutes.isEmpty() || uiState.weeklySavedMinutes.all { it == 0 }) {
                    // Zero State / Welcome State
                    GlassCard(glowColor = Accent.copy(alpha = 0.1f), modifier = Modifier.clickable { onNavigate("statistics") }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                             Text(
                                text = "Projected Impact",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isDark) White else Black,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Start blocking apps to see your score rise!",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) TextSecondary else TextSecondaryLight
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Simple Project Graph
                             Row(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Before
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(40.dp).height(80.dp).clip(RoundedCornerShape(4.dp)).background(Alert.copy(0.5f)))
                                    Text("Now", fontSize = 10.sp, color = if(isDark) TextSecondary else TextSecondaryLight)
                                }
                                // Arrow
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Accent.copy(0.5f), modifier = Modifier.size(16.dp))
                                // Goal
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(40.dp).height(30.dp).clip(RoundedCornerShape(4.dp)).background(Success))
                                    Text("Goal", fontSize = 10.sp, color = if(isDark) TextSecondary else TextSecondaryLight)
                                }
                            }
                        }
                    }
                } else {
                    ImpactChart(data = uiState.weeklySavedMinutes, isDark = isDark, onClick = { onNavigate("statistics") })
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 7. Your Apps
            PremiumSlideIn(delay = 700) {
                Column {
                    Text(
                        text = "Your Apps",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    GlassCard {
                        if (uiState.managedAppStats.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Accent,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No apps added yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isDark) White else Black,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Add apps in Guardian to start protecting your focus",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDark) TextSecondary else TextSecondaryLight,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                TextButton(onClick = { onNavigate("guardian") }) {
                                    Text("Go to Guardian", color = Accent, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Column {
                                uiState.managedAppStats.forEachIndexed { index, app ->
                                    ManagedAppItem(app, isDark)
                                    if (index < uiState.managedAppStats.size - 1) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // System Health Diagnostic
            PremiumSlideIn(delay = 800) {
                SystemHealthCard(uiState, isDark)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 8. Footer
            PremiumSlideIn(delay = 900) {
                Column {
                    GradientButton(
                        text = "Start Exercise",
                        onClick = { onNavigate("forge") },
                        icon = Icons.Default.PlayArrow
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TextButton(
                        onClick = { onNavigate("guardian") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View Blocked Apps",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning,"
        hour < 18 -> "Good Afternoon,"
        else -> "Good Evening,"
    }
}

// NotificationsDialog removed in favor of NotificationScreen.kt

@Composable
fun ManagedAppItem(stat: ManagedAppStat, isDark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                    val icon = packageManager.getApplicationIcon(stat.packageName)
                    imageView.setImageDrawable(icon)
                } catch (e: Exception) {
                    imageView.setImageResource(android.R.drawable.sym_def_app_icon)
                }
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stat.appName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDark) White else Black,
                fontWeight = FontWeight.SemiBold
            )
            
            // Time Available Logic
            if (stat.isUnlocked) {
                val totalMins = stat.timeAvailableMs / 1000 / 60
                val totalSecs = stat.timeAvailableMs / 1000
                
                val timeText = when {
                    totalMins >= 1 -> "Time Available: ${totalMins}m"
                    totalSecs > 0 -> "Time Available: < 1m"
                    else -> "Expired"
                }

                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Success
                )
            } else {
                 Text(
                    text = "No time available",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) TextSecondary else TextSecondaryLight
                )
            }
        }
        
        // Status Badge
        if (stat.isUnlocked) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Success.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "UNLOCKED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Success,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun QuestItem(quest: DailyQuest, isDark: Boolean, onClick: () -> Unit = {}) {
    GlassCard(
        glowColor = if (quest.isCompleted) Success.copy(alpha = 0.3f) else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = quest.icon,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDark) White else Black,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${(quest.progress * 100).toInt()}% Complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) TextSecondary else TextSecondaryLight
                )
            }
            
            if (quest.isCompleted) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "+${quest.reward} LC",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SystemHealthCard(uiState: DashboardUiState, isDark: Boolean) {
    GlassCard(
        glowColor = if (uiState.isServiceRunning) Success.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.2f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System Health",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDark) White else Black,
                    fontWeight = FontWeight.Bold
                )
                
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (uiState.isServiceRunning) Success else Color.Red)
                        .size(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Monitor Service:", fontSize = 12.sp, color = TextSecondary)
                Text(
                    if (uiState.isServiceRunning) "ACTIVE" else "OFFLINE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.isServiceRunning) Success else Color.Red
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Managed Apps:", fontSize = 12.sp, color = TextSecondary)
                Text("${uiState.managedAppsCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) White else Black)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tracking:", fontSize = 12.sp, color = TextSecondary)
                Text(
                    uiState.currentlyTrackedApp?.split(".")?.last() ?: "Idle",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Accent
                )
            }
            
            if (!uiState.isServiceRunning) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  "Warning: Accessibility service is disabled. App tracking and blocking will not work.",
                  fontSize = 10.sp,
                  color = Color.Red,
                  lineHeight = 14.sp
                )
            }
        }
    }
}
