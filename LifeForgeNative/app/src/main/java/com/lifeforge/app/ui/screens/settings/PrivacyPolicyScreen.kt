package com.lifeforge.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeforge.app.ui.components.GlassCard
import com.lifeforge.app.ui.theme.*

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    val themeMode = LocalThemeMode.current
    val isDark = when(themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
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
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(horizontal = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) White else TextPrimaryLight
                        )
                    }
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isDark) White else TextPrimaryLight,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Content
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        PolicySection(
                            title = "1. Data Collection & Usage",
                            content = "Stoppy (LifeForge) collects minimal user data to function. We store your email, name, and activity statistics (steps, workouts, coins) securely on Supabase. This data is used solely to synchronize your progress across devices and provide the app's core gamification features. We do NOT sell your data to third parties.",
                            isDark = isDark
                        )
                        
                        PolicySection(
                            title = "2. Accessibility Service",
                            content = "Our app uses the Android AccessibilityService API to detect when you open specific apps that you have chosen to block. This is the core functionality of the app (App Blocker). The service is used strictly to identify the foreground package name to determine if it matches a blocked app. We do NOT use this service to read your screen content, keystrokes, or any other personal information.",
                            isDark = isDark
                        )
                        
                        PolicySection(
                            title = "3. Camera Usage",
                            content = "The Camera is used exclusively for the AI Workout feature to count your push-ups and squats using pose detection. This processing happens entirely LOCALLY on your device using MediaPipe. No video or image data is ever recorded, stored, or transmitted to any server.",
                            isDark = isDark
                        )

                        PolicySection(
                            title = "4. App Usage Stats",
                            content = "We use the 'Usage Access' permission to track how much time you spend in apps to generate the 'Focus Impact' reports. This data is stored locally and synced to your own private database record for your personal statistics. It is not shared with anyone.",
                            isDark = isDark
                        )

                        PolicySection(
                            title = "5. Account Deletion",
                            content = "You have the right to request the deletion of your account and all associated data at any time. You can do this by contacting support or using the 'Delete Account' option in the settings if available.",
                            isDark = isDark
                        )
                        
                        Text(
                            text = "Last updated: January 2026",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isDark) TextSecondary else TextSecondaryLight,
                            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicySection(title: String, content: String, isDark: Boolean) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (isDark) White else TextPrimaryLight,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDark) TextSecondary else TextSecondaryLight,
            lineHeight = 22.sp
        )
    }
}
