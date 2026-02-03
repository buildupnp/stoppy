package com.lifeforge.app.ui.screens.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.app.data.local.database.entities.WeeklyChallenge
import com.lifeforge.app.ui.components.GlassCard
import com.lifeforge.app.ui.components.GradientButton
import com.lifeforge.app.ui.theme.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

@Composable
fun ChallengesScreen(
    viewModel: ChallengesViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
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
                    brush = if (isDark) {
                        Brush.verticalGradient(colors = listOf(PrimaryGradientStart, PrimaryGradientEnd))
                    } else {
                        Brush.verticalGradient(colors = listOf(PrimaryLightGradientStart, PrimaryLightGradientEnd))
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding() / 2)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if(isDark) White else Black
                    )
                }
                
                Column {
                    Text(
                        text = "Weekly Challenges",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Resets every Monday",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (uiState.challenges.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No challenges this week",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(uiState.challenges) { challenge ->
                        ChallengeCard(
                            challenge = challenge,
                            onClaimReward = { viewModel.claimReward(challenge.id) }
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun ChallengeCard(
    challenge: WeeklyChallenge,
    onClaimReward: () -> Unit
) {
    val icon = when (challenge.type) {
        "steps" -> Icons.Outlined.DirectionsWalk
        "pushups" -> Icons.Outlined.FitnessCenter
        "wisdom" -> Icons.Outlined.AutoStories
        else -> Icons.Outlined.EmojiEvents
    }
    
    val progress = if (challenge.targetValue > 0) {
        (challenge.currentValue.toFloat() / challenge.targetValue).coerceIn(0f, 1f)
    } else 0f
    
    val progressColor = when {
        challenge.isCompleted -> Success
        progress > 0.7f -> Warning
        progress > 0.3f -> Accent
        else -> TextSecondary
    }
    
    GlassCard(
        glowColor = if (challenge.isCompleted && !challenge.isRewardClaimed) 
            Success.copy(alpha = 0.3f) else Transparent
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
                        .size(48.dp)
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
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = challenge.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
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
                    Text(
                        text = "LC",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${challenge.currentValue} / ${challenge.targetValue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = progressColor,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Claim button if completed but not claimed
            if (challenge.isCompleted && !challenge.isRewardClaimed) {
                Spacer(modifier = Modifier.height(12.dp))
                GradientButton(
                    text = "Claim Reward",
                    onClick = onClaimReward,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (challenge.isRewardClaimed) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reward Claimed",
                        style = MaterialTheme.typography.labelMedium,
                        color = Success
                    )
                }
            }
        }
    }
}
