package com.lifeforge.app.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeforge.app.ui.components.GlassCard
import com.lifeforge.app.ui.theme.*

data class NotificationItem(
    val title: String,
    val description: String,
    val icon: String,
    val time: String,
    val category: String = "General"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit
) {
    val isDark = true // LifeForge uses dark theme primarily for neon aesthetic
    
    // Mock data for now
    val notifications = listOf(
        NotificationItem("Streak Milestone!", "You've maintained a 5-day streak. Keep it going!", "🔥", "2m ago", "Streak"),
        NotificationItem("Daily Bonus Claimed", "67 LifeCoins added to your balance.", "💰", "1h ago", "Coins"),
        NotificationItem("Focus Session Complete", "You saved 45 minutes of screen time today.", "🛡️", "3h ago", "Guardian"),
        NotificationItem("New Challenge Available", "Check out the Weekly Walker challenge.", "👣", "5h ago", "Challenge"),
        NotificationItem("App Blocked", "Instagram was blocked after 15 minutes of use.", "🚫", "Yesterday", "Guardian")
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "NOTIFICATIONS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = White,
                    navigationIconContentColor = White
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Primary, Color(0xFF020617))))
                .padding(padding)
        ) {
            if (notifications.isEmpty()) {
                EmptyNotifications()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationCard(notification, isDark)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: NotificationItem, isDark: Boolean) {
    GlassCard(
        glowColor = when(notification.category) {
            "Streak" -> Alert
            "Coins" -> Warning
            "Guardian" -> Success
            else -> Accent
        }.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Text(notification.icon, fontSize = 24.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                    Text(
                        text = notification.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyNotifications() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Nothing yet",
            style = MaterialTheme.typography.titleMedium,
            color = White
        )
        Text(
            "We'll notify you about your progress here.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
