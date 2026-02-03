# LifeForge - Complete App Layout & Structure Documentation

## 📱 App Navigation Structure

### Bottom Navigation Bar (Always Visible)

The app uses a **4-tab bottom navigation** system:

1. **🏠 Home** - Dashboard & Control Center
2. **💪 Forge** - Exercise & Earning Zone
3. **🛡️ Guardian** - Blocked Apps Manager
4. **⚙️ Settings** - Permissions & Preferences

---

## 🏠 HOME TAB (Dashboard)

### Top Section
- **Large Coin Balance Display** (center, most prominent)
  - Current total coins in large font
  - Example: "250 coins"
- **Greeting Text** (small, above balance)
  - "Good Morning, User"
- **Status Badge** (small indicator)
  - "Active" or "Locked"

### Middle Section - Part 1: Stats

**Daily Progress Card:**
- Steps taken today (e.g., "4,250 steps")
- Progress bar showing daily goal
- Small coin icon with coins earned today
- Visual progress indicator (percentage)

**Quick Stats Card:**
- Total push-ups this week
- Active streak days (e.g., "5 day streak 🔥")

### Middle Section - Part 2: Active Apps (COIN SPENDING)

**"Unlocked Apps" Card:**
- Shows currently unlocked apps with live countdown timers
- Example display:
  - Instagram - 23 min left
  - YouTube - 1h 12 min left
- **Tap any app to extend time** (spend more coins)
- Empty state message: "No apps unlocked"
- Option to add more time before expiry

**Quick Unlock Section:**
- Horizontal scrollable row of blocked app icons
- Shows 4-5 app icons at a time
- **Tap any app icon** → Opens quick unlock modal
- Fast access to spend coins without navigating away

### Bottom Section

**Quick Action Buttons:**
- **"Start Exercise"** button (with camera icon)
  - Takes you to Forge tab
  - Prominent, primary action styling
- **"View Blocked Apps"** button (with shield icon)
  - Takes you to Guardian tab
  - Secondary styling

---

## 💪 FORGE TAB (Exercise & Earning)

### Main Content

**Exercise Type Selector:**
- Tab switcher between:
  - **Push-ups** (with camera icon)
  - **Walking** (with steps icon)
- Shows current earning rate
  - Example: "1 coin per 100 steps" or "1 coin per push-up"

**Activity Information:**
- Current balance reminder
- Coins earned in current session
- **"Start Activity"** button (large, prominent, centered)

### During Exercise Screen

**For Push-ups (Camera Active):**
- Full-screen camera view
- **Rep counter** (large, top center)
  - Example: "12 reps"
- **Coins earned this session** (small, top right)
  - Example: "+12 coins"
- **Stop button** (bottom center, red)
- Minimal overlays to avoid distraction

**For Walking (Steps):**
- Live step counter
- Coins being earned in real-time
- Pause/Stop button

### After Exercise Screen

**Summary Card:**
- Reps/Steps completed
- Total coins earned this session
- Time spent exercising
- Motivational message
- **"Done"** button to return to home

---

## 🛡️ GUARDIAN TAB (Blocked Apps Manager)

### Top Section

**Summary Card:**
- Number of apps currently blocked
  - Example: "5 apps blocked"
- Time saved today (optional metric)
- Quick stats about usage

**Action Button:**
- **"Add Apps to Block"** button
  - Opens app selection interface

### App List (Main Content)

**Scrollable List of Installed Apps:**

Each app entry shows:
- **App icon** (full color if unlocked, grayscale if locked)
- **App name**
- **Current status:**
  - 🔒 Locked
  - 🔓 Unlocked (with countdown timer if active)
  - Example: "Instagram 🔓 23 min left"
- **Toggle switch** (Block/Unblock)
- Usage time today (optional)

**Interactive Behaviors:**
- **Tap any BLOCKED app** → Opens unlock options modal
- Modal shows:
  - App name & icon
  - Coin spending options:
    - 15 minutes (10 coins)
    - 30 minutes (18 coins)
    - 1 hour (30 coins)
  - "Unlock Now" confirmation button
- **Tap any UNLOCKED app** → Option to extend time
- **Toggle switch** → Add/remove from blocked list

**Search Bar:**
- Located at top of list
- Filter apps by name
- Quick access to specific apps

### Bottom Info Section

**Information Card:**
- Brief explanation of how blocking works
- Reminder: "You need coins to access blocked apps"
- Link to permissions if not granted

---

## ⚙️ SETTINGS TAB

### Account Section
- Profile information display
  - Email address
  - Username (if applicable)
- **Sign Out** button (destructive styling)

### Permissions Section

**Required Permissions List:**

Each permission shows:
- Permission name
- Status indicator (✅ Granted / ❌ Not Granted)
- "Grant" button if missing

Permissions:
1. **Usage Access** (PACKAGE_USAGE_STATS)
   - Required for app blocking
2. **Overlay Permission** (SYSTEM_ALERT_WINDOW)
   - Required for lock screen
3. **Camera Access**
   - Required for push-up detection
4. **Activity Recognition**
   - Required for step counting
5. **Foreground Service**
   - Required for background monitoring

### Preferences Section

**Adjustable Settings:**
- **Daily step goal** (slider or number input)
  - Default: 10,000 steps
- **Notification settings**
  - Toggle for daily reminders
  - Toggle for achievement notifications
- **Haptic feedback** toggle
  - Vibration on rep completion
- **Sound effects** toggle

### Data & Privacy Section

**Data Management:**
- **Clear history** button
  - Clears exercise history
  - Keeps coin balance
- **Export data** button
  - Download personal data (JSON/CSV)
- **Reset all data** (destructive action)
  - Confirmation required

**Legal & Info:**
- Privacy policy link
- Terms of service link
- App version number
- About LifeForge

---

## 🔒 SPECIAL SCREENS (Not in Bottom Nav)

### Lock Overlay Screen (PRIMARY COIN EXCHANGE)

**Trigger:** User tries to open blocked app with 0 coins or expired timer

**Full-screen overlay with:**

**Top Section:**
- Blocked app icon (large)
- App name
- Message: "This app is locked"

**Center Section:**
- **Current coin balance** (large, prominent display)
  - Example: "You have 45 coins"

**Coin Exchange Options (Buttons):**
- 💰 **"Unlock for 15 min"** - 10 coins
- 💰 **"Unlock for 30 min"** - 18 coins
- 💰 **"Unlock for 1 hour"** - 30 coins

Each button shows:
- Duration clearly
- Coin cost
- Disabled if insufficient coins

**Bottom Section:**
- **"Earn More Coins"** button (prominent)
  - Takes user to Forge tab
- **"Emergency Unlock"** link (small text)
  - Opens confirmation dialog
  - Bypasses system (for emergencies only)
  - Logs usage for user awareness

**Design:**
- Dark blur background
- Cannot be dismissed by tapping outside
- Back button shows warning before dismissing

---

## 💰 COIN EXCHANGE SYSTEM - Summary

### Three Ways to Spend Coins:

#### 1. **Home Tab** (Proactive Planning)
- View unlocked apps with timers
- Tap quick unlock icons
- Extend time before it expires
- Best for: Planning ahead

#### 2. **Lock Overlay** (Reactive/Impulse Control)
- Appears when opening blocked app
- Forces awareness of cost
- Quick unlock options
- Best for: Moment of need

#### 3. **Guardian Tab** (Management View)
- Tap blocked apps in list
- Pre-unlock for later
- Manage all apps in one place
- Best for: Bulk management

### Pricing Structure (Example):
- **15 minutes** = 10 coins (100 steps or 10 push-ups)
- **30 minutes** = 18 coins (180 steps or 18 push-ups)
- **60 minutes** = 30 coins (300 steps or 30 push-ups)

*Note: Longer durations have slightly better value to encourage planning*

---

## 🎨 UI/UX Principles Applied

### Visual Hierarchy:
1. Coin balance always prominent
2. Primary actions use accent color (orange)
3. Locked apps use muted colors
4. Active timers use bright indicators

### Interaction Patterns:
- **Tap app icon** = Quick action (unlock/extend)
- **Toggle switch** = On/off for blocking
- **Large buttons** = Primary actions
- **Small links** = Secondary/destructive actions

### Feedback:
- Haptic vibration on rep completion
- Smooth animations for coin earning
- Clear countdown timers
- Toast notifications for unlocks/locks

---

## 🔄 User Flows

### Flow 1: Earning Coins
1. Home → Tap "Start Exercise"
2. Forge → Select Push-ups
3. Grant camera permission (if needed)
4. Complete push-ups (camera detects)
5. See coin counter increase
6. Tap "Done"
7. Return to Home with new balance

### Flow 2: Unlocking App (Proactive)
1. Home → See blocked apps quick unlock row
2. Tap Instagram icon
3. Modal shows unlock options
4. Select "30 min (18 coins)"
5. Confirm unlock
6. Instagram unlocked for 30 min
7. Timer visible on Home & Guardian tabs

### Flow 3: Unlocking App (Reactive)
1. User opens Instagram from phone launcher
2. Lock Overlay appears immediately
3. See coin balance and options
4. Insufficient coins → Tap "Earn More"
5. Goes to Forge tab
6. Completes exercise, earns coins
7. Returns to Instagram → Try again
8. Now has coins → Unlock successful

### Flow 4: Managing Blocked Apps
1. Guardian tab → Tap "Add Apps to Block"
2. Browse installed apps list
3. Toggle Instagram to "Block"
4. Instagram now appears in blocked list
5. Can tap to unlock or extend time

---

## 📊 Screen Priority & Development Order

### Phase 1: Core Functionality
1. ✅ Authentication screens
2. ✅ Home tab (basic coin display)
3. ✅ Settings tab (permissions)

### Phase 2: Earning System
4. ✅ Forge tab (step counter)
5. ✅ Coin earning logic
6. ✅ Forge tab (camera push-ups)

### Phase 3: Blocking System
7. ✅ Guardian tab (app list)
8. ✅ Lock overlay screen
9. ✅ Usage monitoring service

### Phase 4: Coin Exchange
10. ✅ Lock overlay unlock buttons
11. ✅ Guardian tab unlock modal
12. ✅ Home tab quick unlock section

### Phase 5: Polish
13. ✅ Timers and countdowns
14. ✅ Animations and feedback
15. ✅ Edge cases and error handling

---

## 🎯 Key Design Decisions

### Why Home Tab Has Coin Spending:
- ✅ Central control center concept
- ✅ Immediate visibility of unlocked apps
- ✅ Easy to extend time before expiry
- ✅ Reduces navigation between tabs
- ✅ Encourages proactive coin management

### Why Keep Lock Overlay:
- ✅ Impulse control checkpoint
- ✅ Makes user aware of cost
- ✅ Cannot be bypassed easily
- ✅ Forces intentional decision

### Why Guardian Has Unlock Too:
- ✅ Management view for power users
- ✅ See all apps in one place
- ✅ Batch operations possible
- ✅ Complete control interface

---

## 📱 Bottom Navigation Labels

| Icon | Tab Name | Purpose |
|------|----------|---------|
| 🏠 Home | Home | Dashboard & quick actions |
| 💪 Dumbbell | Forge | Earn coins through exercise |
| 🛡️ Shield | Guardian | Manage blocked apps |
| ⚙️ Gear | Settings | Permissions & preferences |

---

## ✨ Empty States

### Home Tab:
- No unlocked apps: "No apps unlocked yet"
- No exercise today: "Start your first exercise!"
- New user: Onboarding tooltip

### Forge Tab:
- First time: Brief tutorial on how to earn
- No camera permission: Prompt to grant

### Guardian Tab:
- No blocked apps: "Add apps to block"
- All apps unlocked: Encouraging message

---

This structure provides a clear, intuitive, and efficient user experience while maintaining the core principle: **Movement → Currency → Access**