package com.lifeforge.app.ui.screens.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.app.data.local.database.entities.ActivitySession
import com.lifeforge.app.data.remote.DailyStatsDto
import com.lifeforge.app.ui.components.GlassCard
import com.lifeforge.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    val themeMode = LocalThemeMode.current
    val isDark = when(themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    val textColor = if (isDark) White else Black
    val secondaryTextColor = if (isDark) TextSecondary else TextSecondaryLight

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Your Progress",
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = getAppBackgroundBrush(isDark))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                // Timeframe Selector
                TimeframeSelector(
                    selected = uiState.selectedTimeframe,
                    onSelect = { viewModel.setTimeframe(it) },
                    isDark = isDark
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Summary Cards
                SummaryGrid(uiState, isDark)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Main Chart
                Text(
                    text = "Activity Trends",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                GlassCard(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    glowColor = Accent.copy(alpha = 0.1f)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        when (uiState.selectedTimeframe) {
                            Timeframe.DAILY -> DailyActivityChart(uiState.todaySessions, isDark)
                            Timeframe.WEEKLY -> WeeklyBarChart(uiState.dailyStats.take(7), isDark)
                            Timeframe.ALL_TIME -> WeeklyBarChart(uiState.dailyStats, isDark) // Reusing bar chart for now (showing last 30 days)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Distribution Pie Chart
                Text(
                    text = "Exercise Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                GlassCard(glowColor = Color.Transparent) {
                    Column(
                         modifier = Modifier.padding(16.dp),
                         horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ExerciseDistributionChart(uiState, isDark)
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun TimeframeSelector(
    selected: Timeframe,
    onSelect: (Timeframe) -> Unit,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Timeframe.values().forEach { timeframe ->
            val isSelected = selected == timeframe
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Accent else Color.Transparent)
                    .clickable { onSelect(timeframe) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when(timeframe) {
                        Timeframe.DAILY -> "Daily"
                        Timeframe.WEEKLY -> "Weekly"
                        Timeframe.ALL_TIME -> "All Time"
                    },
                    color = if (isSelected) White else (if (isDark) TextSecondary else TextSecondaryLight),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun SummaryGrid(state: StatisticsUiState, isDark: Boolean) {
    val stats = when (state.selectedTimeframe) {
        Timeframe.DAILY -> Triple(
            state.todaySessions.filter { it.type == "steps" }.sumOf { it.count },
            state.todaySessions.filter { it.type == "pushups" }.sumOf { it.count },
            state.todaySessions.filter { it.type == "squats" }.sumOf { it.count }
        )
        Timeframe.WEEKLY -> Triple(
            state.dailyStats.take(7).sumOf { it.totalSteps },
            state.dailyStats.take(7).sumOf { it.totalPushups },
            state.dailyStats.take(7).sumOf { it.totalSquats }
        )
        Timeframe.ALL_TIME -> Triple(
            state.totalStepsAllTime,
            state.totalPushupsAllTime,
            state.totalSquatsAllTime
        )
    }

    Row(
         modifier = Modifier.fillMaxWidth(),
         horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Steps
        SummaryCard(
            title = "Steps",
            value = stats.first.toString(),
            icon = Icons.Outlined.DirectionsWalk,
            color = Accent,
            modifier = Modifier.weight(1f),
            isDark = isDark
        )
        // Pushups
        SummaryCard(
            title = "Pushups",
            value = stats.second.toString(),
            icon = Icons.Outlined.FitnessCenter,
            color = Warning, // Orange
            modifier = Modifier.weight(1f),
            isDark = isDark
        )
        // Squats
        SummaryCard(
            title = "Squats",
            value = stats.third.toString(),
            icon = Icons.Outlined.TrendingUp,
            color = Alert, // Red
            modifier = Modifier.weight(1f),
            isDark = isDark
        )
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    GlassCard(
        modifier = modifier.height(130.dp), // Increased height for vertical layout
        glowColor = color.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween, // Distribute space
            horizontalAlignment = Alignment.CenterHorizontally // Center align everything
        ) {
             // 1. Icon at top centered
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            
            // 2. Value in middle, large
             // Auto-scale text slightly
            val fontSize = if (value.length > 5) 20.sp else 24.sp
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = fontSize),
                fontWeight = FontWeight.Bold,
                color = if (isDark) White else Black,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            
            // 3. Label at bottom
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) TextSecondary else TextSecondaryLight,
                maxLines = 1,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun WeeklyBarChart(data: List<DailyStatsDto>, isDark: Boolean) {
    if (data.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text("No data available yet", color = if(isDark) TextSecondary else TextSecondaryLight)
        }
        return
    }

    val reversedData = data.reversed() // Oldest to newest
    val maxSteps = reversedData.maxOfOrNull { it.totalSteps }?.coerceAtLeast(1) ?: 1
    val maxPushups = reversedData.maxOfOrNull { it.totalPushups }?.coerceAtLeast(1) ?: 1
    
    // Animation
    var animationPlayed by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    
    LaunchedEffect(key1 = true) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
        animationPlayed = true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            LegendItem("Steps", Accent, isDark)
            Spacer(modifier = Modifier.width(16.dp))
            LegendItem("Pushups", Warning, isDark)
            Spacer(modifier = Modifier.width(16.dp))
            LegendItem("Squats", Alert, isDark)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = size.width / (reversedData.size * 2f)
            val spacing = size.width / (reversedData.size)
            val maxHeight = size.height - 40f // Activity space
            
            reversedData.forEachIndexed { index, stat ->
                val x = index * spacing + spacing / 4
                
                // Draw Steps Bar
                val stepsHeight = (stat.totalSteps.toFloat() / maxSteps) * maxHeight * progress.value
                drawRoundRect(
                    color = Accent,
                    topLeft = Offset(x, maxHeight - stepsHeight),
                    size = Size(barWidth / 2, stepsHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                )
                
                // Draw Pushups Bar (Stacked or next to it? Let's do simplified: Overlaid or small next to it?)
                // Let's draw Pushups next to Steps for clarity
                
                // Correction: Scaling pushups on the same axis as steps is hard because Steps are ~5000 and Pushups ~50.
                // Solution: Normalize each to their own % achievement? Or just show Steps mainly and toggle?
                // Let's try Multi-Axis visualization by using Max% of that specific metric.
                
                val pushupsHeight = (stat.totalPushups.toFloat() / maxPushups * 0.8f) * maxHeight * progress.value // Scale pushups slightly smaller visually
                drawRoundRect(
                    color = Warning,
                    topLeft = Offset(x + barWidth / 2, maxHeight - pushupsHeight),
                    size = Size(barWidth / 2, pushupsHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                )
                
                 val squatsHeight = (stat.totalSquats.toFloat() / maxPushups * 0.8f) * maxHeight * progress.value // Approx scaling
                drawRoundRect(
                    color = Alert,
                    topLeft = Offset(x + barWidth, maxHeight - squatsHeight),
                    size = Size(barWidth / 2, squatsHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                )

                // Date Label
                val dateStr = try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val date = sdf.parse(stat.date)
                    val outFormat = SimpleDateFormat("dd", Locale.US) // Just Day
                    outFormat.format(date)
                } catch(e: Exception) { "" }
                
                 drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        dateStr,
                        x + barWidth / 2,
                        size.height,
                        android.graphics.Paint().apply {
                            color = if (isDark) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                            textSize = 30f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DailyActivityChart(sessions: List<ActivitySession>, isDark: Boolean) {
    // Group by type
    val steps = sessions.filter { it.type == "steps" }.sumOf { it.count }
    val pushups = sessions.filter { it.type == "pushups" }.sumOf { it.count }
    val squats = sessions.filter { it.type == "squats" }.sumOf { it.count }

    // Goals
    val stepGoal = 10000f
    val pushupGoal = 50f
    val squatGoal = 50f
    
    // Normalized Progress (0.0 to 1.0+)
    val stepProgress = (steps / stepGoal)
    val pushupProgress = (pushups / pushupGoal)
    val squatProgress = (squats / squatGoal)
    
    // Max value for chart scaling (allow overshooting goal up to 120% visually, else clamp)
    val maxProgress = maxOf(stepProgress, pushupProgress, squatProgress, 1.2f) 

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Legend / Title
        Text(
            text = "Today's Goals Impact",
            style = MaterialTheme.typography.bodySmall,
            color = if(isDark) TextSecondary else TextSecondaryLight,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
        )
    
        Canvas(modifier = Modifier.fillMaxSize()) {
            val chartWidth = size.width
            val chartHeight = size.height - 40f // Leave space for labels
            val barWidth = chartWidth / 9f // 3 bars, spacing equal to roughly half bar? 
            // Layout:  Space | Bar(S) | Space | Bar(P) | Space | Bar(Sq) | Space
            
            val spacing = chartWidth / 4f
            
            // Function to draw bar
            fun drawMetricBar(index: Int, progress: Float, actualValue: Int, color: Color, label: String) {
                val cx = (index + 1) * spacing - (spacing/2) // Center X
                val barH = (progress / maxProgress) * chartHeight
                
                // Track Background (Goal 100% reference line?)
                // Optional: Draw a faint line at 100% goal height
                val goalY = chartHeight - (1f / maxProgress) * chartHeight
                drawLine(
                    color = if(isDark) Color.DarkGray.copy(alpha=0.5f) else Color.LightGray.copy(alpha=0.5f),
                    start = Offset(cx - barWidth, goalY),
                    end = Offset(cx + barWidth, goalY),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )

                // The Bar
                drawRoundRect(
                    color = color,
                    topLeft = Offset(cx - barWidth/2, chartHeight - barH),
                    size = Size(barWidth, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f)
                )
                
                // Value Label (on top of bar)
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        actualValue.toString(),
                        cx,
                        chartHeight - barH - 10f,
                        android.graphics.Paint().apply {
                            this.color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                            textSize = 32f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                    )
                    
                    // X-Axis Label (icon/text)
                    drawText(
                        label,
                        cx,
                        size.height,
                        android.graphics.Paint().apply {
                            this.color = if (isDark) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
            
            drawMetricBar(0, stepProgress, steps, Accent, "Steps")
            drawMetricBar(1, pushupProgress, pushups, Warning, "Pushups")
            drawMetricBar(2, squatProgress, squats, Alert, "Squats")
        }
    }
}

@Composable
fun ActivityBarRow(label: String, value: Int, max: Float, color: Color, isDark: Boolean) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = if(isDark) TextSecondary else TextSecondaryLight)
            Text(value.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if(isDark) White else Black)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if(isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (value.toFloat() / max).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun ExerciseDistributionChart(state: StatisticsUiState, isDark: Boolean) {
    val stats = when (state.selectedTimeframe) {
        Timeframe.DAILY -> Triple(
            state.todaySessions.filter { it.type == "steps" }.sumOf { it.count },
            state.todaySessions.filter { it.type == "pushups" }.sumOf { it.count },
            state.todaySessions.filter { it.type == "squats" }.sumOf { it.count }
        )
        // For general distribution, just sum everything for the period
        else -> Triple(
            state.dailyStats.sumOf { it.totalSteps },
            state.dailyStats.sumOf { it.totalPushups },
            state.dailyStats.sumOf { it.totalSquats }
        )
    }
    
    // Normalize: Steps are huge, so we can't compare directly 1:1 in a pie chart with pushups.
    // Usually "Calories" is the common unit. Let's estimate Calories?
    // Steps ~ 0.04 cal. Pushups ~ 0.5 cal. Squats ~ 0.4 cal. 
    val calSteps = stats.first * 0.04f
    val calPushups = stats.second * 0.5f
    val calSquats = stats.third * 0.4f
    
    PieChart(
        data = listOf(
             PieData("Steps (Cal)", calSteps, Accent),
             PieData("Pushups (Cal)", calPushups, Warning),
             PieData("Squats (Cal)", calSquats, Alert)
        ),
        isDark = isDark
    )
}

data class PieData(val label: String, val value: Float, val color: Color)

@Composable
fun PieChart(data: List<PieData>, isDark: Boolean) {
    val total = data.sumOf { it.value.toDouble() }.toFloat()
    if (total == 0f) return

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(150.dp)) {
                var startAngle = -90f
                data.forEach { slice ->
                    val sweepAngle = (slice.value / total) * 360f
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 30f, cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle
                }
            }
            Text(
                text = "Impact",
                style = MaterialTheme.typography.labelMedium,
                color = if (isDark) TextSecondary else TextSecondaryLight
            )
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        Column {
            data.forEach { slice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(slice.color, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = slice.label,
                        color = if (isDark) White else Black,
                        style = MaterialTheme.typography.bodySmall
                    )
                     Spacer(modifier = Modifier.width(4.dp))
                     Text(
                        text = "${((slice.value / total) * 100).toInt()}%",
                        color = if (isDark) TextSecondary else TextSecondaryLight,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun LegendItem(text: String, color: Color, isDark: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 10.sp, color = if (isDark) TextSecondary else TextSecondaryLight)
    }
}
