package com.lifeforge.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/**
 * A smooth entrance animation that triggers on visibility change.
 * 
 * @param visible When this becomes true, the animation starts. Use pagerState.currentPage == thisPage.
 * @param delay Milliseconds before animation starts after becoming visible.
 * @param duration Duration of the animation in ms. Default 300ms (Snappy).
 * @param offsetY Starting Y offset in pixels.
 */
@Composable
fun PremiumSlideIn(
    modifier: Modifier = Modifier,
    visible: Boolean = true, // Default true for backwards compatibility
    delay: Int = 0,
    duration: Int = 300, // Default to snappy for main app
    offsetY: Float = 30f, // Reduced offset for snappy feel
    content: @Composable () -> Unit
) {
    // Track if animation should run
    var shouldAnimate by remember { mutableStateOf(false) }
    
    // Reset and trigger animation when visible changes to true
    LaunchedEffect(visible) {
        if (visible) {
            shouldAnimate = false // Reset first
            if (delay > 0) {
                delay(delay.toLong())
            }
            shouldAnimate = true
        } else {
            shouldAnimate = false // Reset when not visible
        }
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (shouldAnimate) 1f else 0f,
        animationSpec = tween(
            durationMillis = duration, 
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )
    
    val translationY by animateFloatAsState(
        targetValue = if (shouldAnimate) 0f else offsetY,
        animationSpec = tween(
            durationMillis = duration + 50, 
            easing = FastOutSlowInEasing
        ),
        label = "translationY"
    )
    
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
        }
    ) {
        content()
    }
}

/**
 * A smooth scale-in animation that triggers on visibility change.
 */
@Composable
fun PremiumScaleIn(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    delay: Int = 0,
    duration: Int = 250, // Default to snappy
    content: @Composable () -> Unit
) {
    var shouldAnimate by remember { mutableStateOf(false) }
    
    LaunchedEffect(visible) {
        if (visible) {
            shouldAnimate = false
            if (delay > 0) {
                delay(delay.toLong())
            }
            shouldAnimate = true
        } else {
            shouldAnimate = false
        }
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (shouldAnimate) 1f else 0f,
        animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (shouldAnimate) 1f else 0.95f,
        animationSpec = tween(durationMillis = duration + 50, easing = FastOutSlowInEasing),
        label = "scale"
    )
    
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.scaleX = scale
            this.scaleY = scale
        }
    ) {
        content()
    }
}

/**
 * Bounce animation - scales up then settles with a spring effect.
 * Perfect for buttons, coins, and important elements.
 */
@Composable
fun BounceIn(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    delay: Int = 0,
    content: @Composable () -> Unit
) {
    var shouldAnimate by remember { mutableStateOf(false) }
    
    LaunchedEffect(visible) {
        if (visible) {
            shouldAnimate = false
            if (delay > 0) delay(delay.toLong())
            shouldAnimate = true
        } else {
            shouldAnimate = false
        }
    }
    
    val scale by animateFloatAsState(
        targetValue = if (shouldAnimate) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounce_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (shouldAnimate) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "bounce_alpha"
    )
    
    Box(
        modifier = modifier.graphicsLayer {
            this.scaleX = scale
            this.scaleY = scale
            this.alpha = alpha
        }
    ) {
        content()
    }
}

/**
 * Pulse animation - continuous subtle scale animation for active states.
 */
@Composable
fun PulseAnimation(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Box(
        modifier = modifier.graphicsLayer {
            this.scaleX = scale
            this.scaleY = scale
        }
    ) {
        content()
    }
}

/**
 * Shimmer loading animation - creates a shimmer effect across content.
 */
@Composable
fun ShimmerAnimation(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
        }
    ) {
        content()
    }
}

/**
 * Rotation animation - continuous rotation for loading indicators.
 */
@Composable
fun RotationAnimation(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )
    
    Box(
        modifier = modifier.graphicsLayer {
            this.rotationZ = rotation
        }
    ) {
        content()
    }
}

/**
 * Flip animation - rotates on Y axis for card flip effects.
 */
@Composable
fun FlipAnimation(
    modifier: Modifier = Modifier,
    isFlipped: Boolean = false,
    duration: Int = 400,
    content: @Composable (isFlipped: Boolean) -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(duration, easing = FastOutSlowInEasing),
        label = "flip_rotation"
    )
    
    Box(
        modifier = modifier.graphicsLayer {
            this.rotationY = rotation
            cameraDistance = 12f * density
        }
    ) {
        content(isFlipped)
    }
}

/**
 * Shake animation - horizontal vibration effect for errors or alerts.
 */
@Composable
fun ShakeAnimation(
    modifier: Modifier = Modifier,
    trigger: Boolean = false,
    content: @Composable () -> Unit
) {
    var shouldShake by remember { mutableStateOf(false) }
    
    LaunchedEffect(trigger) {
        if (trigger) {
            shouldShake = true
            delay(500)
            shouldShake = false
        }
    }
    
    val offsetX by animateFloatAsState(
        targetValue = if (shouldShake) 0f else 0f,
        animationSpec = if (shouldShake) {
            keyframes {
                0f at 0
                -10f at 50
                10f at 100
                -10f at 150
                10f at 200
                -5f at 250
                5f at 300
                0f at 350
            }
        } else {
            tween(0)
        },
        label = "shake_offset"
    )
    
    Box(
        modifier = modifier.graphicsLayer {
            this.translationX = offsetX
        }
    ) {
        content()
    }
}
