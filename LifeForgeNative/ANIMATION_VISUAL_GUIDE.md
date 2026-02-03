# Animation Visual Guide - What Each Animation Does

## Basic Animations

### 1. PremiumSlideIn (Existing)
```
Before:  [Hidden]
         ↓ (300ms)
After:   [Visible - Slid from bottom with fade]

Use: Screen entrance, section headers, card reveals
```

### 2. PremiumScaleIn (Existing)
```
Before:  [Hidden, small]
         ↓ (250ms)
After:   [Visible, full size]

Use: Modal opens, important elements, emphasis
```

### 3. BounceIn (New)
```
Before:  [Hidden]
         ↓ (Spring animation)
After:   [Visible - bounces in with spring effect]
         ↓ (settles)
         [Final position]

Use: Coin rewards, achievements, important notifications
```

### 4. PulseAnimation (New)
```
Before:  [Normal size]
         ↓ (1000ms, repeats)
After:   [Slightly larger]
         ↓ (1000ms)
         [Back to normal]
         ↓ (repeats infinitely)

Use: Active workouts, ongoing challenges, live indicators
```

### 5. ShimmerAnimation (New)
```
Before:  [Opaque placeholder]
         ↓ (1500ms, repeats)
After:   [Fades to semi-transparent]
         ↓ (1500ms)
         [Back to opaque]
         ↓ (repeats infinitely)

Use: Loading states, skeleton screens, data fetching
```

### 6. RotationAnimation (New)
```
Before:  [Icon at 0°]
         ↓ (2000ms, repeats)
After:   [Icon rotates 360°]
         ↓ (2000ms)
         [Back to 0°]
         ↓ (repeats infinitely)

Use: Loading spinners, processing indicators, sync status
```

### 7. FlipAnimation (New)
```
Before:  [Front side visible]
         ↓ (400ms)
After:   [Rotates on Y-axis]
         ↓ (200ms)
         [Back side visible]

Use: Card flips, reveal animations, toggle displays
```

### 8. ShakeAnimation (New)
```
Before:  [Normal position]
         ↓ (350ms)
After:   [Shakes left-right-left-right]
         ↓ (350ms)
         [Back to normal]

Use: Error messages, invalid input, warnings
```

## Advanced Animations

### 9. AnimatedCoinCounter
```
Display: 100 coins
         ↓ (earn 50 coins)
         [Scales down, fades]
         ↓ (150ms)
         [Updates to 150]
         ↓ (150ms)
         [Scales back up, fades in]

Use: Coin displays, score updates, currency changes
```

### 10. AnimatedPressButton
```
Normal:  [Button at 1.0x scale]
         ↓ (user presses)
After:   [Button scales to 0.95x]
         ↓ (spring animation)
         [Button returns to 1.0x]

Use: All interactive buttons, action triggers
```

### 11. StaggeredListAnimation
```
Item 1:  [Hidden] → [Visible] (0ms delay)
Item 2:  [Hidden] → [Visible] (50ms delay)
Item 3:  [Hidden] → [Visible] (100ms delay)
Item 4:  [Hidden] → [Visible] (150ms delay)

Result: Items appear one-by-one with smooth cascade

Use: Lists, grids, card collections, quest displays
```

### 12. AnimatedProgressBar
```
Progress: 0%
          ↓ (800ms)
          [Bar fills smoothly]
          ↓ (800ms)
          75%

Use: Loading progress, workout completion, level progress
```

### 13. FloatingActionButtonAnimated
```
Normal:  [FAB at 1.0x scale]
         ↓ (continuous)
After:   [FAB pulses between 1.0x and 1.08x]
         ↓ (2000ms cycle)
         [Repeats infinitely]

When pressed: [Scales to 0.9x] → [Returns to pulse]

Use: Primary actions, forge button, main CTA
```

### 14. ExpandableCard
```
Collapsed: [Header visible, content hidden]
           ↓ (user taps)
           [Content slides down]
           ↓ (400ms)
Expanded:  [Header + content visible]

Tap again:
           [Content slides up]
           ↓ (400ms)
Collapsed: [Header only]

Use: Settings sections, collapsible details, accordions
```

### 15. SwipeToDismissAnimation
```
Normal:  [Card visible at x=0]
         ↓ (user swipes right)
After:   [Card slides right]
         ↓ (400ms)
         [Card fades out]
         ↓ (400ms)
Removed: [Card gone]

Use: Dismissible notifications, swipe-to-delete, card removal
```

### 16. CounterAnimation
```
Display: 0
         ↓ (1000ms)
         [Counts up smoothly]
         ↓ (1000ms)
         1000

Use: Stats display, achievement counts, total calculations
```

## Animation Timing Comparison

```
Fast (150ms):     ▁▁▁▁▁▁▁▁▁▁ (micro-interactions)
Normal (300ms):   ▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁ (standard)
Slow (600ms):     ▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁ (transitions)
Infinite:         ▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁... (loading)
```

## Screen-by-Screen Visual Examples

### Dashboard Screen
```
┌─────────────────────────────────┐
│ Settings                        │  ← PremiumSlideIn
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ 💰 [1000] ← AnimatedCoinCounter
│ │ Focus Impact                │  ← StaggeredListAnimation
│ │ ████████░░ ← AnimatedProgressBar
│ └─────────────────────────────┘ │
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ Quest 1                     │  ← StaggeredListAnimation
│ │ ████████░░                  │  ← AnimatedProgressBar
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ Quest 2                     │  ← StaggeredListAnimation
│ │ ██████░░░░                  │  ← AnimatedProgressBar
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

### Forge Screen
```
┌─────────────────────────────────┐
│ Forge                           │  ← PremiumSlideIn
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ 💪 Pushups                  │  ← AnimatedPressButton
│ │ [Press to start]            │
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ 🏃 Squats                   │  ← AnimatedPressButton
│ │ [Press to start]            │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ✨ +100 Coins! ✨           │  ← BounceIn (on reward)
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 🔄 Workout in Progress...   │  ← PulseAnimation
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

### Guardian Screen
```
┌─────────────────────────────────┐
│ Guardian                        │  ← PremiumSlideIn
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ 🔒 Instagram                │  ← StaggeredListAnimation
│ │ [Locked] 🔴 ← PulseAnimation
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ 🔒 TikTok                   │  ← StaggeredListAnimation
│ │ [Locked] 🔴 ← PulseAnimation
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ 🔓 Chrome                   │  ← StaggeredListAnimation
│ │ [Unlocked]                  │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

### Settings Screen
```
┌─────────────────────────────────┐
│ Settings                        │  ← PremiumSlideIn
├─────────────────────────────────┤
│ ▼ Profile                       │  ← ExpandableCard
│ ┌─────────────────────────────┐ │
│ │ Name: John Doe              │
│ │ Email: john@example.com     │
│ └─────────────────────────────┘ │
│ ▼ Preferences                   │  ← ExpandableCard
│ ┌─────────────────────────────┐ │
│ │ ☑ Notifications             │
│ │ ☑ Haptic Feedback           │
│ │ ☑ Sound Effects             │
│ └─────────────────────────────┘ │
│ ▼ Appearance                    │  ← ExpandableCard
│ ┌─────────────────────────────┐ │
│ │ Theme: Dark Mode            │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

## Animation Combinations

### Entrance Sequence
```
1. Screen loads
   ↓ (0ms)
2. Header slides in
   ↓ (100ms)
3. Cards stagger in
   ↓ (50ms each)
4. Buttons appear
   ↓ (300ms)
5. FAB pulses
   ↓ (continuous)

Result: Smooth, professional entrance
```

### Interaction Sequence
```
1. User taps button
   ↓ (0ms)
2. Button scales down
   ↓ (100ms)
3. Button scales back up
   ↓ (100ms)
4. Action completes
   ↓ (0ms)
5. Coin counter updates
   ↓ (300ms)
6. Coin bounces in
   ↓ (300ms)

Result: Satisfying, responsive interaction
```

### Loading Sequence
```
1. User initiates action
   ↓ (0ms)
2. Shimmer appears
   ↓ (continuous)
3. Loading spinner rotates
   ↓ (continuous)
4. Data loads
   ↓ (variable)
5. Shimmer fades
   ↓ (300ms)
6. Content slides in
   ↓ (300ms)

Result: Clear loading state with smooth transition
```

## Performance Impact

```
Animation Type          | GPU Load | CPU Load | Battery Impact
─────────────────────────────────────────────────────────────
PremiumSlideIn         | Low      | Low      | Minimal
PremiumScaleIn         | Low      | Low      | Minimal
BounceIn               | Low      | Low      | Minimal
PulseAnimation         | Low      | Low      | Minimal
ShimmerAnimation       | Low      | Low      | Minimal
RotationAnimation      | Low      | Low      | Minimal
FlipAnimation          | Medium   | Low      | Minimal
ShakeAnimation         | Low      | Low      | Minimal
AnimatedCoinCounter    | Low      | Low      | Minimal
AnimatedPressButton    | Low      | Low      | Minimal
StaggeredListAnimation | Medium   | Low      | Low
AnimatedProgressBar    | Low      | Low      | Minimal
FloatingActionButton   | Low      | Low      | Minimal
ExpandableCard         | Medium   | Low      | Low
SwipeToDismiss         | Low      | Low      | Minimal
CounterAnimation       | Low      | Medium   | Minimal
```

All animations are GPU-accelerated and optimized for performance.

## Accessibility Considerations

```
Animation Type          | Respects Reduced Motion | Duration | Clarity
─────────────────────────────────────────────────────────────────────
All animations          | ✓ (can be disabled)     | 150-600ms | ✓ Clear
Infinite animations     | ✓ (can be disabled)     | Continuous| ✓ Clear
Micro-interactions      | ✓ (can be disabled)     | 150-200ms | ✓ Clear
```

All animations include clear visual feedback and respect system settings.
