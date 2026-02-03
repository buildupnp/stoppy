# Animation Quick Reference Card

## Available Animations

### Basic Animations (Animations.kt)
| Animation | Use Case | Duration | Import |
|-----------|----------|----------|--------|
| `PremiumSlideIn` | Entrance from bottom | 300ms | ✓ Existing |
| `PremiumScaleIn` | Scale entrance | 250ms | ✓ Existing |
| `BounceIn` | Bouncy entrance | Spring | ✓ New |
| `PulseAnimation` | Active state indicator | 1000ms | ✓ New |
| `ShimmerAnimation` | Loading state | 1500ms | ✓ New |
| `RotationAnimation` | Loading spinner | 2000ms | ✓ New |
| `FlipAnimation` | Card flip | 400ms | ✓ New |
| `ShakeAnimation` | Error feedback | 350ms | ✓ New |

### Advanced Animations (AdvancedAnimations.kt)
| Animation | Use Case | Duration | Import |
|-----------|----------|----------|--------|
| `AnimatedCoinCounter` | Coin display | 300ms | ✓ New |
| `AnimatedPressButton` | Button feedback | Spring | ✓ New |
| `StaggeredListAnimation` | List entrance | 300ms + stagger | ✓ New |
| `AnimatedProgressBar` | Progress display | 800ms | ✓ New |
| `FloatingActionButtonAnimated` | FAB with pulse | Spring + 2000ms | ✓ New |
| `ExpandableCard` | Collapsible section | 400ms | ✓ New |
| `SwipeToDismissAnimation` | Swipe dismiss | 400ms | ✓ New |
| `CounterAnimation` | Number counter | 1000ms | ✓ New |

## Import Statements

```kotlin
// Basic animations (already in your project)
import com.lifeforge.app.ui.components.PremiumSlideIn
import com.lifeforge.app.ui.components.PremiumScaleIn

// New basic animations
import com.lifeforge.app.ui.components.BounceIn
import com.lifeforge.app.ui.components.PulseAnimation
import com.lifeforge.app.ui.components.ShimmerAnimation
import com.lifeforge.app.ui.components.RotationAnimation
import com.lifeforge.app.ui.components.FlipAnimation
import com.lifeforge.app.ui.components.ShakeAnimation

// Advanced animations
import com.lifeforge.app.ui.components.AnimatedCoinCounter
import com.lifeforge.app.ui.components.AnimatedPressButton
import com.lifeforge.app.ui.components.StaggeredListAnimation
import com.lifeforge.app.ui.components.AnimatedProgressBar
import com.lifeforge.app.ui.components.FloatingActionButtonAnimated
import com.lifeforge.app.ui.components.ExpandableCard
import com.lifeforge.app.ui.components.SwipeToDismissAnimation
import com.lifeforge.app.ui.components.CounterAnimation
```

## Common Patterns

### Pattern 1: Animated List
```kotlin
StaggeredListAnimation(
    itemCount = items.size,
    staggerDelay = 50
) { index, isVisible ->
    GlassCard {
        Text(items[index].title)
    }
}
```

### Pattern 2: Loading State
```kotlin
if (isLoading) {
    ShimmerAnimation {
        GlassCard { /* skeleton */ }
    }
} else {
    PremiumSlideIn {
        // content
    }
}
```

### Pattern 3: Coin Reward
```kotlin
BounceIn(visible = showReward) {
    Row {
        Icon(Icons.Default.Coin)
        AnimatedCoinCounter(value = earnedCoins)
    }
}
```

### Pattern 4: Button with Feedback
```kotlin
AnimatedPressButton(
    text = "Earn Coins",
    onClick = { 
        viewModel.earnCoins()
        // haptic feedback
    }
)
```

### Pattern 5: Progress Update
```kotlin
AnimatedProgressBar(
    progress = currentProgress / maxProgress,
    progressColor = Success
)
```

## Animation Timing Guide

| Type | Duration | Easing |
|------|----------|--------|
| Micro-interaction | 150-200ms | FastOutSlowInEasing |
| Entrance | 300-400ms | FastOutSlowInEasing |
| Exit | 200-300ms | FastOutSlowInEasing |
| Loading | Infinite | LinearEasing |
| Transition | 400-600ms | FastOutSlowInEasing |
| Spring | Auto | Spring(0.55f) |

## Easing Functions

```kotlin
// Smooth, natural (default)
FastOutSlowInEasing

// Constant speed (loading)
LinearEasing

// Smooth acceleration
EaseInOutQuad

// Quick start, linear end
FastOutLinearInEasing

// Bouncy, natural
Spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
```

## Performance Checklist

- [ ] Using `graphicsLayer` for all animations
- [ ] Not animating layout properties
- [ ] Animations run at 60fps
- [ ] No memory leaks from infinite transitions
- [ ] Respects system animation settings
- [ ] Works on low-end devices
- [ ] Battery impact is minimal

## Common Issues & Solutions

### Issue: Animation stutters
**Solution**: Use `graphicsLayer` instead of layout changes

### Issue: Animation too slow
**Solution**: Reduce duration (300ms is usually good)

### Issue: Animation doesn't trigger
**Solution**: Check `visible` parameter or `LaunchedEffect` dependency

### Issue: Memory leak with infinite animation
**Solution**: Use `rememberInfiniteTransition` (already done in code)

### Issue: Animation blocks UI
**Solution**: Animations run on GPU, shouldn't block. Check for heavy composables inside.

## Screen-by-Screen Recommendations

### Dashboard
```
✓ Coin counter: AnimatedCoinCounter
✓ Quests: StaggeredListAnimation
✓ Progress: AnimatedProgressBar
✓ Stats: CounterAnimation
```

### Forge
```
✓ Buttons: AnimatedPressButton
✓ Reward: BounceIn
✓ Active: PulseAnimation
✓ Options: StaggeredListAnimation
```

### Guardian
```
✓ App list: StaggeredListAnimation
✓ Locked: PulseAnimation
✓ Lock/Unlock: BounceIn
```

### Settings
```
✓ Sections: ExpandableCard
✓ Items: PremiumSlideIn (existing)
✓ Toggles: Already smooth
```

### Challenges
```
✓ Cards: StaggeredListAnimation
✓ Progress: AnimatedProgressBar
✓ Completion: BounceIn
```

## Testing Commands

```bash
# Enable animation duration scale in Developer Options
adb shell settings put global animator_duration_scale 1.0

# Disable animations for testing
adb shell settings put global animator_duration_scale 0.0

# Slow down animations for debugging
adb shell settings put global animator_duration_scale 10.0
```

## Next Steps

1. Copy new animation files to your project
2. Start with Phase 1 animations (coin counter, buttons)
3. Test on real devices
4. Gather feedback
5. Iterate and add more animations
6. Profile and optimize

## Support

For issues or questions:
1. Check Android Profiler for performance
2. Review animation code comments
3. Test with reduced motion enabled
4. Check device compatibility
