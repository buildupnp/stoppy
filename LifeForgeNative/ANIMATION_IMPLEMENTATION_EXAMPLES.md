# Animation Implementation Examples

## Quick Start - Copy & Paste Examples

### 1. Animated Coin Counter
Replace your coin display with animated counter:

```kotlin
// Before
Text(text = coins.toString(), fontSize = 24.sp)

// After
AnimatedCoinCounter(
    value = coins,
    fontSize = 24,
    textColor = Color.White
)
```

### 2. Button with Press Feedback
Replace regular buttons with animated press buttons:

```kotlin
// Before
Button(onClick = { /* action */ }) {
    Text("Earn Coins")
}

// After
AnimatedPressButton(
    text = "Earn Coins",
    onClick = { /* action */ },
    backgroundColor = Accent,
    textColor = White
)
```

### 3. Staggered List Animation
Animate list items appearing one by one:

```kotlin
StaggeredListAnimation(
    itemCount = questList.size,
    staggerDelay = 50
) { index, isVisible ->
    GlassCard {
        Text(questList[index].title)
    }
}
```

### 4. Animated Progress Bar
Replace static progress bars:

```kotlin
// Before
LinearProgressIndicator(progress = 0.75f)

// After
AnimatedProgressBar(
    progress = 0.75f,
    progressColor = Success,
    height = 8
)
```

### 5. Expandable Settings Section
Collapsible settings sections:

```kotlin
var isExpanded by remember { mutableStateOf(false) }

ExpandableCard(
    isExpanded = isExpanded,
    header = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(16.dp)
        ) {
            Text("Advanced Settings")
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }
    },
    content = {
        Column(modifier = Modifier.padding(16.dp)) {
            // Settings content here
        }
    }
)
```

### 6. Floating Action Button with Pulse
Primary action button with continuous pulse:

```kotlin
FloatingActionButtonAnimated(
    onClick = { /* navigate to forge */ },
    backgroundColor = Accent
) {
    Icon(Icons.Default.Bolt, contentDescription = "Forge")
}
```

### 7. Loading Shimmer Effect
Show loading state with shimmer:

```kotlin
ShimmerAnimation {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(Color.Gray.copy(alpha = 0.3f))
            )
        }
    }
}
```

### 8. Bounce Animation for Important Elements
Highlight important elements:

```kotlin
BounceIn(visible = showCoinReward) {
    GlassCard {
        Text("+100 Coins!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}
```

### 9. Pulse Animation for Active States
Show active/ongoing states:

```kotlin
PulseAnimation {
    GlassCard {
        Text("Workout in Progress...")
    }
}
```

### 10. Counter Animation for Stats
Animate numbers counting up:

```kotlin
CounterAnimation(
    targetValue = totalCoins,
    fontSize = 32,
    duration = 1000
)
```

## Where to Apply These

### Dashboard Screen
- Coin counter: `AnimatedCoinCounter`
- Stats: `CounterAnimation`
- Quest list: `StaggeredListAnimation`
- Progress bars: `AnimatedProgressBar`

### Forge Screen
- Workout buttons: `AnimatedPressButton`
- Reward display: `BounceIn`
- Active workout: `PulseAnimation`

### Settings Screen
- Expandable sections: `ExpandableCard`
- Toggle animations: Already using `PremiumSlideIn`

### Guardian Screen
- App list: `StaggeredListAnimation`
- Lock status: `PulseAnimation`

### Challenges Screen
- Challenge cards: `StaggeredListAnimation`
- Progress: `AnimatedProgressBar`

## Performance Tips

1. **Use `graphicsLayer`** - All animations use GPU-accelerated graphics layer
2. **Avoid layout animations** - Use `offset` instead of changing layout
3. **Profile regularly** - Use Android Profiler to check frame rates
4. **Limit simultaneous animations** - Don't animate everything at once
5. **Use appropriate durations**:
   - Micro-interactions: 150-300ms
   - Entrance animations: 300-500ms
   - Loading states: Continuous (infinite)
   - Transitions: 400-600ms

## Easing Functions Reference

- `FastOutSlowInEasing` - Natural, smooth (default for most)
- `LinearEasing` - Constant speed (loading spinners)
- `EaseInOutQuad` - Smooth acceleration/deceleration
- `FastOutLinearInEasing` - Quick start, linear end
- `Spring` - Bouncy, natural feel

## Testing Animations

1. Enable "Animation Duration Scale" in Developer Options (1x for normal)
2. Use Android Profiler to check GPU/CPU usage
3. Test on low-end devices to ensure smooth performance
4. Check battery impact with continuous animations

## Next Steps

1. Start with coin counter and button animations (high impact, low effort)
2. Add staggered list animations to all list screens
3. Implement progress bar animations
4. Add expandable sections to settings
5. Polish with micro-interactions and entrance animations
