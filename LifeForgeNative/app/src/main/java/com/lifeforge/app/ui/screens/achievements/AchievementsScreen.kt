package com.lifeforge.app.ui.screens.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.app.data.local.database.entities.Achievement
import com.lifeforge.app.ui.components.GlassCard
import com.lifeforge.app.ui.theme.*
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun AchievementsScreen(
    viewModel: AchievementsViewModel = hiltViewModel(),
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
                Spacer(modifier = Modifier.height(16.dp))
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    Text(
                        text = "Achievements",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Stats summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBox(
                        label = "Unlocked",
                        value = "${uiState.unlockedCount}/${uiState.totalCount}",
                        color = Success,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    StatBox(
                        label = "Coins Earned",
                        value = "${uiState.totalCoinsEarned}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Category filter
                val categories = listOf("All", "Fitness", "Streak", "Guardian", "Wisdom")
                var selectedCategory by remember { mutableStateOf("All") }
                
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Achievements grid
                val filteredAchievements = if (selectedCategory == "All") {
                    uiState.achievements
                } else {
                    uiState.achievements.filter { it.category.equals(selectedCategory, ignoreCase = true) }
                }
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredAchievements) { achievement ->
                        AchievementCard(achievement = achievement)
                    }
                }
            }
        }
    }
}



@Composable
private fun StatBox(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.height(80.dp),
        cornerRadius = 16.dp,
        glowColor = color.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement) {
    val icon = getIconForName(achievement.iconName)
    val progress = if (achievement.requiredValue > 0) {
        (achievement.currentValue.toFloat() / achievement.requiredValue).coerceIn(0f, 1f)
    } else 0f
    
    GlassCard(
        glowColor = if (achievement.isUnlocked) Success.copy(alpha = 0.3f) else Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked) 
                            Success.copy(alpha = 0.2f) 
                        else 
                            Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (achievement.isUnlocked) Success else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
            if (!achievement.isUnlocked) {
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
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                
                Text(
                    text = "${achievement.currentValue}/${achievement.requiredValue}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Reward badge
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+${achievement.coinReward}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Success,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " LC",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getIconForName(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "directionswalk" -> Icons.Outlined.DirectionsWalk
        "directionsrun" -> Icons.Outlined.DirectionsRun
        "fitnesscenter" -> Icons.Outlined.FitnessCenter
        "emojievents" -> Icons.Outlined.EmojiEvents
        "localfiredepartment" -> Icons.Outlined.LocalFireDepartment
        "star" -> Icons.Outlined.Star
        "shield" -> Icons.Outlined.Shield
        "security" -> Icons.Outlined.Security
        "autostories" -> Icons.Outlined.AutoStories
        "lightbulb" -> Icons.Outlined.Lightbulb
        else -> Icons.Outlined.EmojiEvents
    }
}
