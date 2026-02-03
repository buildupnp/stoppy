# LifeForge Animation Enhancement Guide

## Current State Analysis
Your app has basic animations:
- `PremiumSlideIn` - Fade + slide from bottom
- `PremiumScaleIn` - Fade + scale animation
- Used primarily in Settings and Forge screens

## Recommended Enhancements

### 1. **Shared Element Transitions**
- Animate coins when earned/spent
- Smooth transitions between screens
- Card expansion animations

### 2. **Micro-interactions**
- Button press feedback (scale + haptic)
- Coin counter animations (number flip)
- Progress bar animations
- Swipe gesture feedback

### 3. **Gesture-Based Animations**
- Drag-to-unlock animations
- Swipe card animations
- Pull-to-refresh animations

### 4. **Loading & State Animations**
- Skeleton loading screens
- Shimmer effects
- Pulse animations for active states
- Rotation animations for loading

### 5. **Entrance Animations**
- Staggered list animations
- Cascade animations for cards
- Bounce animations for important elements

### 6. **Exit Animations**
- Fade out on navigation
- Slide out on dismiss
- Scale down on delete

## Performance Considerations
- Use `graphicsLayer` for GPU-accelerated animations
- Avoid animating layout properties (use `offset` instead)
- Use `rememberInfiniteTransition` for continuous animations
- Profile with Android Profiler to ensure 60fps

## Recommended Implementation Priority
1. **High Impact**: Coin animations, button feedback, list stagger
2. **Medium Impact**: Loading states, gesture feedback
3. **Polish**: Entrance/exit transitions, micro-interactions
