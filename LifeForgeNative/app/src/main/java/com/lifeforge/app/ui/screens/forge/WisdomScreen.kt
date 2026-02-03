package com.lifeforge.app.ui.screens.forge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lifeforge.app.ui.components.GlassCard
import com.lifeforge.app.ui.components.GradientButton
import com.lifeforge.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun WisdomScreen(
    onClose: () -> Unit,
    onReward: (Int) -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(60) } // 1 minute in seconds for testing/mindfulness
    var isRunning by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var reflectionText by remember { mutableStateOf("") }
    var showReflectionInput by remember { mutableStateOf(false) }
    var isRewarded by remember { mutableStateOf(false) }
    
    val quote = remember {
        listOf(
            "The soul that is within me no man can degrade. I am the master of my fate, I am the captain of my soul.",
            "Character is destiny. What we do in our private moments defines the impact we make on the world. Discipline is the bridge between goals and accomplishment.",
            "Do not pray for an easy life, pray for the strength to endure a difficult one. Great things are never achieved in comfort zones.",
            "Wealth consists not in having great possessions, but in having few wants. True freedom is found in self-mastery."
        ).random()
    }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            showReflectionInput = true
            isRunning = false
        }
    }

    fun handleReward() {
        if (reflectionText.length > 5 && !isRewarded) {
            isRewarded = true
            isFinished = true
            onReward(20)
        }
    }

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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                }
                Text(
                    text = "Path of Wisdom",
                    style = MaterialTheme.typography.titleLarge,
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Progress area
            GlassCard(glowColor = Accent) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isFinished) "Wisdom Attained" else String.format("%02d:%02d", timeLeft / 60, timeLeft % 60),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isFinished) Success else White
                    )
                    Text(
                        text = when {
                            isFinished -> "You earned 20 coins"
                            showReflectionInput -> "Reflect on the words below"
                            else -> "Focus on the words for 1 minute"
                        },
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (isFinished) {
                        Spacer(modifier = Modifier.height(24.dp))
                        GradientButton(
                            text = "Claim & Finish",
                            onClick = onClose,
                            modifier = Modifier.width(200.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (showReflectionInput && !isFinished) {
                GlassCard(glowColor = Success) {
                    Column {
                        Text(
                            text = "What did you learn from this?",
                            color = if(isDark) White else Black,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = reflectionText,
                            onValueChange = { reflectionText = it },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            placeholder = { Text("Write your reflection here...", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Success,
                                unfocusedBorderColor = White.copy(alpha = 0.1f),
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GradientButton(
                            text = "Submit Reflection",
                            onClick = { handleReward() },
                            enabled = reflectionText.trim().length > 10,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (reflectionText.trim().length <= 10) {
                            Text(
                                text = "Please write at least one full sentence.",
                                color = Alert,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // The Reading Content
            GlassCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = Accent.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = quote,
                        style = MaterialTheme.typography.headlineSmall,
                        color = if(isDark) White else Black,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!isRunning && !isFinished) {
                GradientButton(
                    text = "Start Mindful Reading",
                    onClick = { isRunning = true },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (isRunning) {
                Text(
                    text = "Deep focus in progress...",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            }
        }
    }
}
