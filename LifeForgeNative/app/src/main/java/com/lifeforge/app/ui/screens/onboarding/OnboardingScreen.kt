package com.lifeforge.app.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.lifeforge.app.ui.components.PremiumSlideIn
import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeforge.app.ui.components.GlassCard
import com.lifeforge.app.ui.components.GradientButton
import com.lifeforge.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.lifeforge.app.R

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onFinish: (String) -> Unit,
    onGoogleSignIn: (String) -> Unit,
    onSignUpEmail: (String) -> Unit,
    onLogin: () -> Unit,
    onPrivacyPolicyClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val pagerState = rememberPagerState(pageCount = { 9 })
    val scope = rememberCoroutineScope()
    
    // Haptic Feedback on Page Change
    LaunchedEffect(pagerState.currentPage) {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress) 
    }
    
    // Intake States
    var userName by remember { mutableStateOf("") }
    var selectedApps by remember { mutableStateOf(setOf<String>()) }
    var avgScreenTime by remember { mutableFloatStateOf(6f) }
    var isPolicyAccepted by remember { mutableStateOf(false) }
    
    // Smoothed Values for UI display
    val animatedAvgTime by animateFloatAsState(
        targetValue = avgScreenTime,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "avgTime"
    )

    // Touch Interaction State (Shield Orb)
    var touchPos by remember { mutableStateOf(Offset(200f, 200f)) }
    var isTouching by remember { mutableStateOf(false) }
    val orbPos by animateOffsetAsState(
        targetValue = touchPos,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "orbPos"
    )
    val orbAlpha by animateFloatAsState(if (isTouching) 0.8f else 0.4f, label = "orbAlpha")
    val reactorScale by animateFloatAsState(if (isTouching) 1.05f else 1f, label = "reactorScale")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = getAppBackgroundBrush(isDark = true))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { 
                        touchPos = it
                        isTouching = true 
                    },
                    onDragEnd = { isTouching = false },
                    onDragCancel = { isTouching = false },
                    onDrag = { change, _ ->
                        touchPos = change.position
                    }
                )
            }
    ) {
        // 1. Organic Liquid Shield (Wave interaction)
        LiquidShield(orbPos, orbAlpha, isTouching)

        // 2. Full Screen Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false
        ) { page ->
            val isVisible = pagerState.currentPage == page
            
            if (page == 0) {
                ShieldIntroSlide(
                    isVisible = isVisible,
                    onNext = {
                        if (!pagerState.isScrollInProgress) {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 80.dp, bottom = 100.dp) // Space for Header & Footer
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (page) {
                        1 -> NameIntakeSlide(isVisible, userName) { userName = it }
                        2 -> VulnerabilitySlide(isVisible, selectedApps) { selectedApps = it }
                        3 -> ScreenTimeBaselineSlide(isVisible, avgScreenTime) { avgScreenTime = it }
                        4 -> ShockMathSlide(isVisible, animatedAvgTime)
                        5 -> ReclamationVisualSlide(isVisible, animatedAvgTime)
                        6 -> CommunitySlide(isVisible)
                        7 -> SecuritySetupSlide(isVisible)
                        8 -> AuthGateSlide(
                            isVisible = isVisible,
                            onGoogleSignIn = { if (isPolicyAccepted) onGoogleSignIn(userName) else android.widget.Toast.makeText(context, "Please accept Privacy Policy", android.widget.Toast.LENGTH_SHORT).show() },
                            onSignUpEmail = { if (isPolicyAccepted) onSignUpEmail(userName) else android.widget.Toast.makeText(context, "Please accept Privacy Policy", android.widget.Toast.LENGTH_SHORT).show() },
                            onLogin = onLogin,
                            isPolicyAccepted = isPolicyAccepted,
                            onPolicyChange = { isPolicyAccepted = it },
                            onPrivacyPolicyClick = onPrivacyPolicyClick
                        )
                    }
                }
            }
        }

        // 3. Floating Header (Top)
        // Only show header background/interaction if needed, or strictly overlay
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            OnboardingHeader(
                currentPage = pagerState.currentPage,
                onBack = {
                    if (pagerState.currentPage > 0) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }
                }
            )
        }

        // 4. Floating Footer (Bottom) - Global Continue Button
        if (pagerState.currentPage in 1..7) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
            ) {
                GradientButton(
                    text = "Next Step",
                    onClick = {
                        if (!pagerState.isScrollInProgress) {
                            scope.launch { 
                                pagerState.animateScrollToPage(pagerState.currentPage + 1) 
                            }
                        }
                    },
                    enabled = when(pagerState.currentPage) {
                        1 -> userName.isNotBlank() && userName.length <= 8
                        else -> true
                    }
                )
            }
        }
    }
}

@Composable
fun LiquidShield(pos: Offset, alpha: Float, isTouching: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveScale"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val baseRadius = 180f
        val touchImpact = if (isTouching) 1.5f else 1f
        
        // 1. Deep Core Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Accent.copy(alpha = alpha * 0.4f), Color.Transparent),
                center = pos,
                radius = 400f * touchImpact
            )
        )

        // 2. Organic Waves
        for (i in 0 until 3) {
            val scale = (1f + i * 0.3f) * waveScale * touchImpact
            val layerAlpha = alpha * (1f - i * 0.3f)
            
            drawCircle(
                color = Accent.copy(alpha = layerAlpha * 0.2f),
                center = pos,
                radius = baseRadius * scale,
                style = Stroke(width = 2f)
            )
            
            // Subtle White Highlight Ring
            drawCircle(
                color = White.copy(alpha = layerAlpha * 0.1f),
                center = pos,
                radius = (baseRadius + 5f) * scale,
                style = Stroke(width = 1f)
            )
        }

        // 3. Central Pulse Core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(White.copy(alpha = alpha * 0.6f), Color.Transparent),
                center = pos,
                radius = 40f * touchImpact
            )
        )
    }
}



@Composable
fun OnboardingHeader(currentPage: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentPage > 0) {
            Icon(
                Icons.Default.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = White,
                modifier = Modifier.size(32.dp).clickable { onBack() }
            )
        } else {
            Spacer(modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(9) { i ->
                val active = i <= currentPage
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(if (active) Accent else White.copy(0.1f))
                )
            }
        }
    }
}

// Replaced local AnimateFadeUp with shared PremiumSlideIn

@Composable
fun ShieldIntroSlide(isVisible: Boolean = true, onNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617)) // Solid base
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A).copy(alpha = 0.4f), // Blue Glow
                        Color.Transparent
                    ),
                    center = Offset(0f, Float.POSITIVE_INFINITY), // Bottom Leftish glow
                    radius = 2500f
                )
            )
    ) {
        // 3. Main Typography Content
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 32.dp)
                .offset(y = (-40).dp)
        ) {
                PremiumSlideIn(visible = isVisible, duration = 500, delay = 0) {
                    Text(
                        text = "FORGE",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp
                )
            }
            PremiumSlideIn(visible = isVisible, duration = 500, delay = 50) {
                Text(
                    text = "YOUR",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp
                )
            }
            
            // "DESTINY" Row
            PremiumSlideIn(visible = isVisible, duration = 500, delay = 100) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DESTINY",
                        color = Color(0xFF3B82F6), // Blue Accent
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Subtitle
            PremiumSlideIn(visible = isVisible, duration = 500, delay = 150) {
                Text(
                    text = "Turn your daily tasks into XP. Level up your productivity and conquer your goals with the ultimate gamified system.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }
        }

        // 4. Bottom "Get Started" Button Area
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 60.dp, end = 24.dp)
        ) {
            PremiumSlideIn(visible = isVisible, duration = 500, delay = 200) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
                        .clickable { onNext() }
                        .padding(start = 24.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    Text(
                        text = "Get Started",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Circle Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF2563EB), CircleShape), // Blue Button
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallMade, // Arrow Up Right
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NameIntakeSlide(isVisible: Boolean = true, name: String, onNameChange: (String) -> Unit) {
    val nameLimit = 8
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxHeight().fillMaxWidth()
    ) {
        PremiumSlideIn(visible = isVisible, duration = 500) {
            Text("WHO ARE YOU?", color = Accent, fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(32.dp))
        GlassCard(modifier = Modifier.padding(horizontal = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                TextField(
                    value = name,
                    onValueChange = { if (it.length <= nameLimit) onNameChange(it) },
                    placeholder = { Text("Identifier Name...", color = White.copy(0.2f)) },
                    textStyle = LocalTextStyle.current.copy(color = White, fontSize = 24.sp, textAlign = TextAlign.Center),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = Accent,
                        focusedIndicatorColor = Accent,
                        unfocusedIndicatorColor = White.copy(0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Limit: ${name.length}/$nameLimit", color = if (name.length == nameLimit) Alert else TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("This name will be your call-sign in the forge.", color = TextSecondary, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VulnerabilitySlide(isVisible: Boolean = true, selected: Set<String>, onSelectionChange: (Set<String>) -> Unit) {
    val apps = listOf("TikTok", "Instagram", "Reels", "Snapchat", "YouTube", "Twitter", "Others")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxHeight().fillMaxWidth()
    ) {
        PremiumSlideIn(visible = isVisible, duration = 500) {
            Text("ACTIVE VULNERABILITIES", color = Alert, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }
        PremiumSlideIn(visible = isVisible, duration = 500, delay = 50) {
            Text("Select the black holes of your attention.", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            apps.forEachIndexed { index, app ->
                val isSelected = selected.contains(app)
                PremiumSlideIn(visible = isVisible, duration = 500, delay = 100 + (index * 30)) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(if (isSelected) Alert.copy(0.2f) else CardDark)
                            .border(1.dp, if (isSelected) Alert else White.copy(0.1f), RoundedCornerShape(30.dp))
                            .clickable { onSelectionChange(if (isSelected) selected - app else selected + app) }
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        Text(app, color = if (isSelected) White else TextSecondary, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenTimeBaselineSlide(isVisible: Boolean = true, avg: Float, onValueChange: (Float) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxHeight().fillMaxWidth()
    ) {
        PremiumSlideIn(visible = isVisible, duration = 500) {
            Text("THE COST OF PIXELS", color = Accent, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Custom Vertical Heat Slider
        VerticalHeatSlider(
            value = avg,
            onValueChange = onValueChange,
            range = 1f..14f
        )
    }
}

@Composable
fun VerticalHeatSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>
) {
    val height = 300.dp
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    // Derived values
    val normalized = (value - range.start) / (range.endInclusive - range.start)
    val colorStops = arrayOf(
        0.0f to Color(0xFF4ADE80), // Green (Low usage)
        0.4f to Color(0xFFFACC15), // Yellow (Med usage)
        0.8f to Color(0xFFEF4444), // Red (High usage)
        1.0f to Color(0xFF7F1D1D)  // Dark Red (Extreme)
    )
    
    val animatedValue by animateFloatAsState(targetValue = value, label = "value")
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Value Indicator
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedValue.roundToInt()}h",
                color = White,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black
            )
            Text("Daily", color = TextSecondary, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Heat Bar
        Box(
            modifier = Modifier
                .height(height)
                .width(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color.Black.copy(0.3f))
                .border(1.dp, White.copy(0.1f), RoundedCornerShape(30.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val y = change.position.y.coerceIn(0f, size.height.toFloat())
                        // Invert Y because drag 0 is top, but we want 0 value at bottom
                        val percent = 1f - (y / size.height.toFloat())
                        val newValue = (range.start + (percent * (range.endInclusive - range.start)))
                            .coerceIn(range.start, range.endInclusive)
                        
                        // Haptic feedback on interval change
                        if (newValue.roundToInt() != value.roundToInt()) {
                             haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        }
                        
                        onValueChange(newValue)
                    }
                }
        ) {
            // Fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(normalized)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(colorStops = colorStops))
            )
            
            // Thumb/Handle
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .offset(y = (-25).dp) // Center vertically on the fill line
                    // Calculate offset from bottom based on normalized value
                    // Since align is BottomCenter, positive Y moves down (out), so we need layout alignment or percentage offset
                    // Simpler to just use absolute offset? No, let's use alignment logic
            )
            
            // Grid Lines
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(7) {
                    Box(modifier = Modifier.width(20.dp).height(2.dp).background(White.copy(0.1f)))
                }
            }
        }
    }
}

@Composable
fun ShockMathSlide(isVisible: Boolean = true, avg: Float) {
    val yearsSpent = ((avg / 16f) * 60f).roundToInt()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(horizontal = 24.dp)
    ) {
        PremiumSlideIn(visible = isVisible, duration = 500) {
            Text("THE LIFETIME WAGE", color = Alert, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }
        Spacer(modifier = Modifier.height(48.dp))
        
        PremiumSlideIn(visible = isVisible, duration = 500, delay = 100) {
            GlassCard(glowColor = Alert.copy(0.2f), modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, 
                    modifier = Modifier.padding(vertical = 48.dp, horizontal = 24.dp)
                ) {
                    Text("In the next 60 years, you will lose", color = White.copy(0.6f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Flexible text scaling
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Text("$yearsSpent Years", color = Alert, fontSize = 64.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 70.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("staring at glass.", color = White.copy(0.6f), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun ReclamationVisualSlide(isVisible: Boolean = true, avg: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxHeight().fillMaxWidth()
    ) {
        PremiumSlideIn(visible = isVisible, duration = 500) {
            Text("THE RECLAMATION", color = Success, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }
        Spacer(modifier = Modifier.height(64.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PremiumSlideIn(visible = isVisible, duration = 500, delay = 50) { ImpactBar("Without Shield", avg, Alert) }
            PremiumSlideIn(visible = isVisible, duration = 500, delay = 100) { Icon(Icons.Default.Shield, contentDescription = null, tint = Accent, modifier = Modifier.padding(bottom = 90.dp).size(44.dp)) }
            PremiumSlideIn(visible = isVisible, duration = 500, delay = 150) { ImpactBar("Stoppy Shielded", avg * 0.35f, Success) }
        }
    }
}

@Composable
fun ImpactBar(label: String, value: Float, color: Color) {
    // Custom animation for growing bar
    var animatedHeight by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = value,
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        ) { value, _ -> animatedHeight = value }
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${value.roundToInt()}h", color = color, fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.width(50.dp).height((animatedHeight * 12).dp).clip(RoundedCornerShape(8.dp)).background(color))
        Text(label, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun CommunitySlide(isVisible: Boolean = true) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxHeight().fillMaxWidth()
    ) {
        PremiumSlideIn(visible = isVisible, duration = 500) {
            Text("DISCIPLINE SHARED", color = Accent, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }
        Spacer(modifier = Modifier.height(32.dp))
        repeat(2) { i ->
            PremiumSlideIn(visible = isVisible, duration = 500, delay = 50 + (i * 50)) {
                GlassCard(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            if (i == 0) "\"The pushup barrier is the filter. Only the focused survive.\"" else "\"Screen time dropped 5 hours. My productivity exploded.\"",
                            color = White, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 17.sp,
                            lineHeight = 24.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("- User ${1024 + i}", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.End))
                    }
                }
            }
        }
    }
}

@Composable
fun SecuritySetupSlide(isVisible: Boolean = true) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        PremiumSlideIn(visible = isVisible, duration = 500) {
            Text("SYSTEM DEPLOYMENT", color = Accent, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }
        PremiumSlideIn(visible = isVisible, duration = 500, delay = 50) {
            Text("Required Permissions Protocol", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val items = listOf(
                Triple("Display Overlay", "Required to show the Shield and block apps.") {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                    context.startActivity(intent)
                },
                Triple("Usage Access", "Required to detect when you open distracted apps.") {
                    context.startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                },
                Triple("Background Activity", "Required to keep the shield active at all times.") {
                    val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                },
                Triple("Notifications", "Required for persistence and alerts.") {
                    val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                },
                Triple("Accessibility", "The core engine that powers the blocking mechanism.") {
                    context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
            
            items.forEachIndexed { index, item ->
                PremiumSlideIn(visible = isVisible, duration = 500, delay = 100 + (index * 30)) {
                    SecurityItem(item.first, item.second, item.third)
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SecurityItem(title: String, desc: String, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Accent.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Accent, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text(desc, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp), lineHeight = 15.sp)
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Accent.copy(0.5f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun AuthGateSlide(
    isVisible: Boolean = true,
    onGoogleSignIn: () -> Unit, 
    onSignUpEmail: () -> Unit, 
    onLogin: () -> Unit,
    isPolicyAccepted: Boolean,
    onPolicyChange: (Boolean) -> Unit,
    onPrivacyPolicyClick: () -> Unit
) {
    val context = LocalContext.current
    var showError by remember { mutableStateOf<String?>(null) }
    
    // Floating Animation
    val infiniteTransition = rememberInfiniteTransition(label = "hero_float")
    val dy by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dy"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Main Content Card (Centered)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp) // Wider card
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E293B).copy(alpha = 0.90f),
                            Color(0xFF0F172A).copy(alpha = 0.95f)
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(White.copy(alpha = 0.2f), White.copy(alpha = 0.05f))
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp) // Space for the overlapping image
            ) {
                PremiumSlideIn(visible = isVisible, duration = 500) {
                    Text(
                        text = "Welcome to Stoppy",
                        style = MaterialTheme.typography.headlineMedium,
                        color = White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                
                PremiumSlideIn(visible = isVisible, duration = 500, delay = 50) {
                    Text(
                        text = "Your journey to digital freedom starts here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                    )
                }

                // Buttons
                PremiumSlideIn(visible = isVisible, duration = 500, delay = 100) {
                    GradientButton(
                        text = "Continue with Google",
                        onClick = { 
                            if (isPolicyAccepted) {
                                onGoogleSignIn() 
                            } else {
                                showError = "Please accept Privacy Policy first"
                            }
                        },
                        icon = null, // Or Google icon if available
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                PremiumSlideIn(visible = isVisible, duration = 500, delay = 150) {
                    OutlinedButton(
                        onClick = { 
                            if (isPolicyAccepted) {
                                onSignUpEmail() 
                            } else {
                                showError = "Please accept Privacy Policy"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, White.copy(alpha = 0.2f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = White)
                    ) {
                        Text("Sign Up with Email", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                PremiumSlideIn(visible = isVisible, duration = 500, delay = 200) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isPolicyAccepted,
                            onCheckedChange = onPolicyChange,
                            colors = CheckboxDefaults.colors(checkedColor = Accent, uncheckedColor = TextSecondary, checkmarkColor = White)
                        )
                        Text(
                            text = "I agree to Privacy Policy",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { onPrivacyPolicyClick() }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                PremiumSlideIn(visible = isVisible, duration = 500, delay = 250) {
                    TextButton(onClick = onLogin) {
                        Text("Already have an account? Login", color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Floating Hero Image (Overlapping Top-Right)
        Box(
            modifier = Modifier
                .align(Alignment.Center) // Start from center
                .offset(x = 80.dp, y = (-220).dp + dy.dp) // Push to top-right + Animation
        ) {
             androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.auth_hero),
                contentDescription = "Guardian",
                modifier = Modifier.size(200.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }

        if (showError != null) {
            com.lifeforge.app.ui.components.AuthErrorDialog(
                message = showError!!,
                onDismiss = { showError = null }
            )
        }
    }
}

