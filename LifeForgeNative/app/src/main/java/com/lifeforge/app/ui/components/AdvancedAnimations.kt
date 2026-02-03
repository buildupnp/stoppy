package com.lifeforge.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Animated coin counter that flips numbers when value changes.
 * Perfect for displaying earned coins with smooth transitions.
 */
@Composable
fun AnimatedCoinCounter(
    value: Int,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    fontSize: Int = 24
) {
    var displayValue by remember { mutableStateOf(value) }
    var isFlipping by remember { mutableStateOf(false) }
    
    LaunchedEffect(value) {
        if (value != displayValue) {
            isFlipping = true
            delay(150)
            displayValue = value
            delay(150)
            isFlipping = false
        }
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isFlipping) 0.8f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "coin_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isFlipping) 0.5f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "coin_alpha"
    )
    
    Text(
        text = displayValue.toString(),
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = modifier.graphicsLayer {
            this.scaleX = scale
            this.scaleY = scale
            this.alpha = alpha
        }
    )
}

/**
 * Button with press animation - scales down on press with haptic feedback.
 * Provides tactile feedback for user interactions.
 */
@Composable
fun AnimatedPressButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = Color.White
) {
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "button_scale"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

/**
 * Staggered list animation - items appear one by one with delay.
 * Great for lists, grids, and card collections.
 */
@Composable
fun StaggeredListAnimation(
    itemCount: Int,
    modifier: Modifier = Modifier,
    staggerDelay: Int = 50,
    content: @Composable (index: Int, isVisible: Boolean) -> Unit
) {
    var visibleItems by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        for (i in 0 until itemCount) {
            delay(staggerDelay.toLong())
            visibleItems = i + 1
        }
    }
    
    Column(modifier = modifier) {
        repeat(itemCount) { index ->
            PremiumSlideIn(
                visible = index < visibleItems,
                delay = 0,
                duration = 300
            ) {
                content(index, index < visibleItems)
            }
        }
    }
}

/**
 * Progress bar with animated fill.
 * Smoothly animates progress changes.
 */
@Composable
fun AnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Gray.copy(alpha = 0.2f),
    progressColor: Color = Color.Green,
    height: Int = 8
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Box(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape(height.dp / 2))
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .background(progressColor)
        )
    }
}

/**
 * Floating action button with entrance animation and continuous pulse.
 * Perfect for primary actions.
 */
@Composable
fun FloatingActionButtonAnimated(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "fab_scale"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_pulse_scale"
    )
    
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(backgroundColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .graphicsLayer {
                this.scaleX = scale * pulseScale
                this.scaleY = scale * pulseScale
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Expandable card with smooth height animation.
 * Perfect for collapsible sections.
 */
@Composable
fun ExpandableCard(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val animatedHeight by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "expand_height"
    )
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "expand_alpha"
    )
    
    Column(modifier = modifier) {
        header()
        
        if (animatedHeight > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.alpha = animatedAlpha
                        this.scaleY = animatedHeight
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                    }
            ) {
                content()
            }
        }
    }
}

/**
 * Swipe-to-dismiss animation with slide and fade out.
 */
@Composable
fun SwipeToDismissAnimation(
    isDismissed: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val offsetX by animateFloatAsState(
        targetValue = if (isDismissed) 500f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "swipe_offset"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isDismissed) 0f else 1f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "swipe_alpha"
    )
    
    Box(
        modifier = modifier.graphicsLayer {
            this.translationX = offsetX
            this.alpha = alpha
        }
    ) {
        content()
    }
}

/**
 * Number counter animation - counts from 0 to target value.
 * Great for stats and achievements.
 */
@Composable
fun CounterAnimation(
    targetValue: Int,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    fontSize: Int = 32,
    duration: Int = 1000
) {
    var displayValue by remember { mutableStateOf(0) }
    
    LaunchedEffect(targetValue) {
        val startTime = System.currentTimeMillis()
        while (displayValue < targetValue) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            displayValue = (targetValue * progress).toInt()
            delay(16) // ~60fps
        }
        displayValue = targetValue
    }
    
    Text(
        text = displayValue.toString(),
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = modifier
    )
}
