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
                            content = "LifeForge (Stoppy) is designed to work with minimal data. If you create an account, we may store your email address, display name, and app-specific progress (coins, activities, blocked apps, unlock sessions, streaks, achievements, and preferences). This data is used to provide the core app experience and (when enabled) to sync your progress across devices. We do not sell your personal data.",
                            isDark = isDark
                        )
                        
                        PolicySection(
                            title = "2. Accessibility Service",
                            content = "LifeForge uses the Android AccessibilityService API to detect the currently foreground app package name so we can enforce blocks you configured. We use it to: (a) detect when a blocked app is opened, and (b) display the lock overlay so you can unlock with coins or earn time via exercise. We do not use Accessibility to read typed text, passwords, messages, or the contents of other apps.",
                            isDark = isDark
                        )
                        
                        PolicySection(
                            title = "3. Camera Usage",
                            content = "Camera access is used only when you start an AI Workout session (push-ups/squats) to detect reps. Pose detection runs on-device. LifeForge does not record video, store photos, or upload camera frames for this feature.",
                            isDark = isDark
                        )

                        PolicySection(
                            title = "4. Usage Access (App Usage Stats)",
                            content = "If you grant Usage Access, LifeForge can read app usage statistics so it can show app usage time and related insights (e.g., usage today). This information is used only for the features inside the app and is not sold.",
                            isDark = isDark
                        )

                        PolicySection(
                            title = "5. Overlay & Foreground Services",
                            content = "LifeForge may request permission to display over other apps (overlay) to show the lock screen on top of blocked apps. It may also run foreground services to keep step tracking and app monitoring reliable in the background (these may show persistent notifications depending on Android version and device settings).",
                            isDark = isDark
                        )

                        PolicySection(
                            title = "6. Activity Recognition (Step Tracking)",
                            content = "If you enable step tracking, LifeForge uses activity recognition/step sensors to estimate steps so you can earn coins. Step counting is used for the app’s reward system and statistics. You can disable these permissions in Android settings at any time.",
                            isDark = isDark
                        )

                        PolicySection(
                            title = "7. In-App Notifications",
                            content = "LifeForge shows in-app notifications related to your actions and progress (for example: welcome, app blocked/unblocked, unlock purchased, workout completed, achievements). These notifications are app content; they are not the same as Android push notifications. They are stored on-device and may be cleared by clearing app data or using in-app controls when available.",
                            isDark = isDark
                        )

                        PolicySection(
                            title = "8. Data Storage, Sharing, and Security",
                            content = "LifeForge stores some information locally on your device (for example: cached progress, timers, and notifications). If you sign in, some of your data may also be stored in our backend (Supabase) to support syncing and account features. Data is transmitted over encrypted network connections (HTTPS). We do not sell your personal information. We may use third-party infrastructure providers (such as Supabase) to operate the service.",
                            isDark = isDark
                        )

                        PolicySection(
                            title = "9. Your Choices & Account Deletion",
                            content = "You can disable permissions (Accessibility, Usage Access, Overlay, Camera, Activity Recognition) at any time in Android settings, which may limit features. You may request account deletion by contacting the developer using the support contact shown in the app listing (Google Play).",
                            isDark = isDark
                        )
                        
                        Text(
                            text = "Last updated: February 4, 2026",
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
