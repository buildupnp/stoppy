package com.lifeforge.app.ui.screens.forge

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.app.data.model.DailyQuest
import com.lifeforge.app.data.local.database.entities.WeeklyChallenge
import com.lifeforge.app.ui.components.GlassCard
import com.lifeforge.app.ui.components.GradientButton
import com.lifeforge.app.ui.components.LargeCoinBadge
import com.lifeforge.app.ui.components.CoinBadge
import com.lifeforge.app.ui.components.PremiumSlideIn
import com.lifeforge.app.ui.components.PremiumScaleIn
import com.lifeforge.app.ui.theme.*

@Composable
fun ForgeScreen(
    viewModel: ForgeViewModel = hiltViewModel(),
    onNavigateToCamera: () -> Unit = {},
    onNavigateToWisdom: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPushupAI by remember { mutableStateOf(false) }
    var showSquatAI by remember { mutableStateOf(false) }
    
    // Theme support
    val themeMode = LocalThemeMode.current
    val isDark = when(themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    val colorScheme = if (isDark) {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(PrimaryGradientStart, PrimaryGradientEnd)
        )
    } else {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(PrimaryLightGradientStart, PrimaryLightGradientEnd)
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = getAppBackgroundBrush(isDark))
    ) {
        if (showPushupAI) {
            AIWorkoutScreen(
                workoutType = "pushups",
                onClose = { showPushupAI = false },
                onFinish = { count ->
                    viewModel.logPushups(count)
                    showPushupAI = false
                }
            )
        } else if (showSquatAI) {
            AIWorkoutScreen(
                workoutType = "squats",
                onClose = { showSquatAI = false },
                onFinish = { count ->
                    viewModel.logSquats(count)
                    showSquatAI = false
                }
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding()) // Use full system top padding
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = padding.calculateBottomPadding() + 12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "The Forge",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Forge your path to digital freedom",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        CoinBadge(amount = uiState.coinsEarnedToday)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            label = "Points Today",
                            value = "${uiState.coinsEarnedToday}",
                            icon = Icons.Default.Bolt,
                            color = Success,
                            modifier = Modifier.weight(1f),
                            isDark = isDark
                        )
                        StatCard(
                            label = "Active Streak",
                            value = "${uiState.streakDays}",
                            icon = Icons.Default.LocalFireDepartment,
                            color = Alert,
                            modifier = Modifier.weight(1f),
                            isDark = isDark
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Daily Quests
                    if (uiState.dailyQuests.isNotEmpty()) {
                        Column {
                            Text(
                                text = "Daily Quests",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                uiState.dailyQuests.forEach { quest ->
                                    ForgeQuestItem(quest, isDark) {
                                       if (quest.id == "wisdom_1") {
                                           onNavigateToWisdom()
                                       } else if (quest.id == "pushups_1") {
                                            showPushupAI = true
                                       }
                                    }
                                }
                            }
                        }
                    }
                        
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Weekly Challenges
                    if (uiState.activeChallenges.isNotEmpty()) {
                        Column {
                            Text(
                                text = "Weekly Challenges",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                uiState.activeChallenges.forEach { challenge ->
                                    ForgeChallengeItem(challenge, isDark)
                                }
                            }
                        }
                    }
                        
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Training Exercises
                    Text(
                        text = "Training Exercises",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Workout Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        ActivityCard(
                            title = "Push-ups",
                            subtitle = "1 LC / 2 reps",
                            icon = Icons.Default.FitnessCenter,
                            color = Accent,
                            onClick = { showPushupAI = true },
                            modifier = Modifier.weight(1f).height(140.dp),
                            isDark = isDark
                        )
                        ActivityCard(
                            title = "Squats",
                            subtitle = "1 LC / 2 reps",
                            icon = Icons.Default.Accessibility,
                            color = Success,
                            onClick = { showSquatAI = true },
                            modifier = Modifier.weight(1f).height(140.dp),
                            isDark = isDark
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Passive & Special Activities
                    StepTracker(
                        steps = uiState.todaySteps,
                        onSync = { viewModel.syncSteps() },
                        isDark = isDark
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    WisdomTask(
                        onStart = onNavigateToWisdom,
                        isDark = isDark
                    )
                    
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }

    // Success Popup
    uiState.lastResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearResult() },
            containerColor = if(isDark) CardDark else CardLight,
            tonalElevation = 8.dp,
            title = {
                Text("Workout Complete!", color = if(isDark) White else Black, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "+${result.coinsEarned} LC",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Success
                    )
                    Text(
                        text = "Great job! You finished ${result.count} ${result.type.lowercase()}.",
                        color = if(isDark) TextSecondary else TextSecondaryLight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearResult() },
                    colors = ButtonDefaults.buttonColors(containerColor = Success),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("AWESOME", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}



@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    GlassCard(modifier = modifier, glowColor = Color.Transparent) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp) // Proper centering and spacing
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = if(isDark) White else Black, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = if(isDark) TextSecondary else TextSecondaryLight)
        }
    }
}

@Composable
private fun ActivityCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    Box(
        modifier = modifier
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
                    },
                    onTap = { onClick() }
                )
            }
    ) {
        GlassCard(glowColor = Color.Transparent, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(8.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp).align(Alignment.Center))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(title, color = if(isDark) White else Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = if(isDark) TextSecondary else TextSecondaryLight, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StepTracker(steps: Int, onSync: () -> Unit, isDark: Boolean) {
    GlassCard {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Step Counting", color = if(isDark) White else Black, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("$steps", fontSize = 56.sp, fontWeight = FontWeight.Black, color = if(isDark) White else Black)
            Text("steps captured today", color = if(isDark) TextSecondary else TextSecondaryLight, style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onSync,
                colors = ButtonDefaults.buttonColors(containerColor = Accent.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SYNC TO COINS", color = Accent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WisdomTask(onStart: () -> Unit, isDark: Boolean) {
    GlassCard(glowColor = Success.copy(alpha = 0.2f)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onStart() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoStories, contentDescription = null, tint = Success)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Path of Wisdom", color = if(isDark) White else Black, fontWeight = FontWeight.Bold)
                Text("Focus for 5 mins to earn 20 LC", color = if(isDark) TextSecondary else TextSecondaryLight, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = if(isDark) TextSecondary else TextSecondaryLight)
        }
    }
}

@Composable
fun ForgeQuestItem(quest: DailyQuest, isDark: Boolean, onClick: () -> Unit = {}) {
    val icon = when {
        quest.title.contains("Why", ignoreCase = true) || quest.title.contains("Wisdom", ignoreCase = true) -> Icons.Default.AutoStories
        quest.title.contains("Push", ignoreCase = true) -> Icons.Default.FitnessCenter
        quest.title.contains("Squat", ignoreCase = true) -> Icons.Default.Accessibility
        quest.title.contains("Step", ignoreCase = true) -> Icons.Default.DirectionsWalk
        else -> Icons.Default.Star
    }

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
                    imageVector = icon,
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
                    color = if(isDark) White else Black,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${(quest.progress * 100).toInt()}% Complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = if(isDark) TextSecondary else TextSecondaryLight
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
fun ForgeChallengeItem(challenge: WeeklyChallenge, isDark: Boolean) {
    val icon = when (challenge.type) {
        "steps" -> Icons.Outlined.DirectionsWalk
        "pushups" -> Icons.Outlined.FitnessCenter
        "wisdom" -> Icons.Outlined.AutoStories
        else -> Icons.Outlined.Star
    }
    
    val progress = if (challenge.targetValue > 0) {
        (challenge.currentValue.toFloat() / challenge.targetValue).coerceIn(0f, 1f)
    } else 0f
    
    val progressColor = when {
        challenge.isCompleted -> Success
        progress > 0.7f -> Warning
        progress > 0.3f -> Accent
        else -> if(isDark) TextSecondary else TextSecondaryLight
    }
    
    GlassCard(
        glowColor = if (challenge.isCompleted && !challenge.isRewardClaimed) 
            Success.copy(alpha = 0.3f) else Color.Transparent
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (challenge.isCompleted) 
                                Success.copy(alpha = 0.2f) 
                            else 
                                Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (challenge.isCompleted) Success else Accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if(isDark) White else Black,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(progress * 100).toInt()}% • ${challenge.currentValue}/${challenge.targetValue}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if(isDark) TextSecondary else TextSecondaryLight
                    )
                }
                
                // Reward
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "+${challenge.coinReward}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (challenge.isCompleted) Success else Accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Mini Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(progressColor)
                )
            }
        }
    }
}
