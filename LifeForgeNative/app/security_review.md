# Security Review: LifeForgeNative (Android)

**Date**: 2026-01-19
**Application Type**: Android / Kotlin / Jetpack Compose / Supabase / App Blocker
**Review Status**: Completed

---

## Executive Summary

The application shows a solid foundation with modern Android libraries (Compose, Hilt, Coroutines) and secure backend integration (Supabase Auth). However, there are **critical privacy and security issues** related to logging user activity and component exposure that must be addressed before production. The "App Blocking" core feature relies on high-privilege permissions (`ACCESSIBILITY`, `USAGE_STATS`), demanding the highest standard of privacy protection.

## 1. Critical Vulnerabilities (Must Fix)

### 🚨 V1. Privacy Leak in Application Logs
**Severity**: **CRITICAL**
**Location**: `AppDetectorService.kt` (Lines 107, 274, 279)
**Risk**: The application logs the package name of *every* app the user opens to the system log (`Logcat`). Any app with `READ_LOGS` permission (on older Android versions) or a malicious actor with physical/ADB access can reconstruct the user's entire digital life (banking apps, dating apps, etc.). this violates Google Play's Privacy Policy requirements for Accessibility Services.

**Code Example:**
```kotlin
// AppDetectorService.kt
Log.d(TAG, "Foreground package changed: $packageName") // LEAK
```

**Fix:** Remove these logs or wrap them in a debug check that is FALSE in production.
```kotlin
if (BuildConfig.DEBUG) {
    Log.d(TAG, "Foreground package changed: $packageName")
}
```

### 🚨 V2. Activity Spoofing via Unprotected Broadcast Receiver
**Severity**: **HIGH**
**Location**: `ActivityTransitionReceiver.kt`
**Risk**: The receiver is exported (`android:exported="true"`) in `AndroidManifest.xml` but has no permission checks. Any malicious app on the device can send a spoofed broadcast to trigger "In Vehicle" state or crash the app.

**Attack Vector:**
```bash
adb shell am broadcast -a com.lifeforge.app.ACTION_ACTIVITY_TRANSITION --ei extra_is_in_vehicle 1
```

**Fix:**
If this receiver is only triggered by Google Play Services, it does *not* need to be exported if you use `PendingIntent.getBroadcast` with the correct flags, or you should verify the sender.
**Recommended:** Set `android:exported="false"` in Manifest if possible, or verify `ActivityTransitionResult.hasResult(intent)` strictly and consider checking the sender package.

---

## 2. High-Priority Issues (Should Fix Soon)

### ⚠️ H1. Infinite "Emergency Unlock" Loop (Business Logic)
**Severity**: **HIGH**
**Location**: `AppLockRepository.kt` (lines 309-325) & `LockOverlayActivity.kt`
**Issue**: The "Emergency Unlock" feature grants 15 minutes of usage and resets the user's streak. However, there is **no cooldown** enforcement. A user (or script) can simply click "Emergency Unlock" every 15 minutes and never pay coins, rendering the gamification useless.

**Fix:**
Implement a server-side or secure local cooldown (e.g., allow only once per 24 hours).
```kotlin
// AppLockRepository.kt
suspend fun emergencyUnlock(): Result<Unit> {
   val lastReset = authRepository.getLastStreakResetTimestamp()
   if (System.currentTimeMillis() - lastReset < 24 * 60 * 60 * 1000) {
       return Result.failure(Exception("Emergency unlock is on cooldown."))
   }
   // ... proceed
}
```

### ⚠️ H2. Hardcoded Credentials & Fallbacks
**Severity**: **MEDIUM**
**Location**: `build.gradle.kts` (Lines 43-44)
**Issue**: The Supabase URL and ANON KEY are hardcoded as fallbacks.
```kotlin
buildConfigField("String", "SUPABASE_KEY", "\"eyJhv...\"")
```
While Anon keys are technically public, hardcoding them in the build file makes it easy to accidentally commit them or use the wrong environment in production.
**Fix**: Ensure `local.properties` is used strictly and fail the build if keys are missing, rather than falling back to a hardcoded string. Remove the hardcoded fallback string.

---

## 3. Medium-Priority & Architecture Review

### 🔒 M1. Database Transaction Atomicity
**Location**: `AppLockRepository.kt` -> `unlockApp`
**Issue**: The app deducts coins (`coinRepository.spendCoins`) *before* saving the unlock state (`appLockDao.insertUnlock`). If the app crashes or the database write fails between these two steps, the user loses coins but gets no unlock.
**Recommendation**: wrap the operation in a Room `@Transaction` or ensure the coin deduction is reversible.

### 🔒 M2. Foreground Service Privacy
**Location**: `AppMonitorService`
**Issue**: Android 14 requires strict justification for `FOREGROUND_SERVICE_SPECIAL_USE`. Ensure your Google Play Console declaration matches the code's behavior exactly. The service monitors usage; ensure it stops when not needed to save battery and reduce privacy scope.

### 🔒 M3. Overlay Bypass
**Location**: `LockOverlayActivity.kt`
**Issue**: Android "Overlay" blockers are inherently bypassable by:
1.  Opening "Recent Apps" and killing the overlay.
2.  Rapidly switching between apps.
3.  Force-stopping the app.
**Recommendation**: Acknowledge this limitation. For `LockOverlayActivity`, consider listening to `TYPE_WINDOW_STATE_CHANGED` more aggressively or using a specialized "Home" intent catcher if you want to be more aggressive (but risking Play Store rejection). The current implementation is compliant but "soft".

---

## 4. Best Practices Checklist

- [ ] **R8/ProGuard**: Confirmed `isMinifyEnabled = true`. Ensure rules in `proguard-rules.pro` preserve your Data Classes for Supabase JSON serialization.
- [ ] **Network Security**: Supabase uses HTTPS. Ensure no cleartext traffic is allowed (`android:usesCleartextTraffic="false"`).
- [ ] **Exported Components**:
    - `MainActivity`: Exported (Correct).
    - `BootReceiver`: Exported (Correct).
    - `ActivityTransitionReceiver`: **Exported (Fix this!)**.
    - All others: Not exported (Good).
- [ ] **Dependencies**: `play-services-auth` (Legacy) is present alongside `androidx.credentials`. Plan to remove the legacy lib to reduce app size and attack surface.

## 5. Summary of Recommended Actions

1.  **IMMEDIATELY**: Delete all `Log.d` calls in `AppDetectorService.kt` that log package names.
2.  **IMMEDIATELY**: Set `android:exported="false"` for `ActivityTransitionReceiver` in `AndroidManifest.xml` (unless explicitly required by a specific implementation detail, in which case add permission protection).
3.  **SOON**: Add a 24h cooldown to `emergencyUnlock` in `AppLockRepository`.
4.  **SOON**: Remove hardcoded Supabase keys from `build.gradle.kts`.

This codebase is generally clean and follows good architecture (MVVM/Clean), but the privacy handling in the Accessibility Service is the primary barrier to a safe production launch.
