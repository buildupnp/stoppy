# 🔧 Google Sign-In Troubleshooting Guide

**Issue:** Google Sign-In not working  
**Date:** January 27, 2026

---

## 🔍 Common Issues & Solutions

### Issue 1: Missing GOOGLE_SERVER_CLIENT_ID in local.properties

**Symptom:** Google Sign-In fails silently or shows "Sign-in cancelled"

**Current Status:**
- ✅ Default Client ID is hardcoded in `build.gradle.kts`
- ⚠️ `GOOGLE_SERVER_CLIENT_ID` not in `local.properties`

**Solution:**

Add this line to `LifeForgeNative/local.properties`:
```properties
GOOGLE_SERVER_CLIENT_ID=YOUR_WEB_CLIENT_ID_HERE
```

Replace `YOUR_WEB_CLIENT_ID_HERE` with your actual Web Client ID from Google Cloud Console.

---

### Issue 2: SHA-1 Fingerprint Not Registered

**Symptom:** "Sign-in failed" or "Developer error"

**How to Get SHA-1:**

**Option A - Gradle (Recommended):**
```bash
cd LifeForgeNative
.\gradlew.bat signingReport
```

Look for output like:
```
Variant: debug
Config: debug
Store: C:\Users\YourName\.android\debug.keystore
Alias: AndroidDebugKey
MD5: XX:XX:XX:...
SHA1: 45:AD:BE:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF
SHA-256: XX:XX:XX:...
```

Copy the SHA1 value.

**Option B - Keytool:**
```powershell
keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**Then:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Navigate to **Credentials**
3. Find your **Android OAuth Client**
4. Add the SHA-1 fingerprint
5. Click **Save**

---

### Issue 3: Wrong Package Name

**Symptom:** "Sign-in failed" or "Invalid package name"

**Check:**
Your package name in Google Cloud Console must match your app:

**In Google Cloud Console:**
- Package Name: `com.stoppy.app`

**In your app (`build.gradle.kts`):**
```kotlin
applicationId = "com.stoppy.app"
```

✅ These match! This is correct.

---

### Issue 4: Supabase Google Provider Not Configured

**Symptom:** Sign-in succeeds but user not created in Supabase

**Solution:**

1. Go to [Supabase Dashboard](https://app.supabase.com/)
2. Select your project
3. Navigate to **Authentication > Providers > Google**
4. Enable Google provider
5. Add your **Web Client ID** and **Web Client Secret**
6. ✅ **Enable "Skip nonce checks"** (Important for Android!)
7. Click **Save**

---

### Issue 5: Missing Google Play Services

**Symptom:** "Google Play Services not available"

**Solution:**

Ensure Google Play Services is installed on your device:
- Physical device: Update Google Play Services from Play Store
- Emulator: Use an emulator with Google Play (not Google APIs)

---

## 📋 Complete Setup Checklist

### Google Cloud Console:
- [ ] Project created
- [ ] OAuth consent screen configured
- [ ] Android OAuth Client created
- [ ] Package name: `com.stoppy.app`
- [ ] SHA-1 fingerprint added
- [ ] Web OAuth Client exists (for Server Client ID)

### Supabase:
- [ ] Google provider enabled
- [ ] Web Client ID added
- [ ] Web Client Secret added
- [ ] "Skip nonce checks" enabled
- [ ] Configuration saved

### App Configuration:
- [ ] `GOOGLE_SERVER_CLIENT_ID` in `local.properties`
- [ ] App rebuilt after adding Client ID
- [ ] Package name matches: `com.stoppy.app`

### Device/Emulator:
- [ ] Google Play Services installed
- [ ] Google account added to device
- [ ] Internet connection available

---

## 🔧 Quick Fix Steps

### Step 1: Add Client ID to local.properties

Edit `LifeForgeNative/local.properties`:
```properties
sdk.dir=D\:\\android studio

# Supabase Configuration
SUPABASE_URL=https://wnmvqipfifyakepiddti.supabase.co
SUPABASE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# Google Sign-In Configuration
GOOGLE_SERVER_CLIENT_ID=YOUR_WEB_CLIENT_ID_HERE.apps.googleusercontent.com
```

### Step 2: Get Your Web Client ID

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Navigate to **Credentials**
3. Look for **"Web client (auto-created by Google Service)"**
4. Copy the Client ID (ends with `.apps.googleusercontent.com`)
5. Paste it in `local.properties`

### Step 3: Rebuild the App

```bash
cd LifeForgeNative
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

Or in Android Studio:
- Build > Clean Project
- Build > Rebuild Project

### Step 4: Test

1. Run the app
2. Go to onboarding
3. Click "Authenticate with Google"
4. Select your Google account
5. Should redirect to Dashboard

---

## 🐛 Debugging

### Check Logcat for Errors

In Android Studio, open **Logcat** and filter by:
- `GoogleAuthHelper`
- `AuthViewModel`
- `Supabase`

Common error messages:
- `"Developer error"` → SHA-1 not registered
- `"Sign-in cancelled"` → User cancelled or configuration issue
- `"Invalid client ID"` → Wrong Client ID or not configured
- `"Network error"` → Internet connection or Supabase issue

### Test with Hardcoded Client ID

Temporarily test by hardcoding in `GoogleAuthHelper.kt`:

```kotlin
.setServerClientId("YOUR_ACTUAL_WEB_CLIENT_ID.apps.googleusercontent.com")
```

If this works, the issue is with `local.properties` or BuildConfig.

---

## 📞 Still Not Working?

### Check These:

1. **Is the default Client ID correct?**
   - Current default: `505119804832-gp3mjp4a0feb3pd84ti0g4llvos1d6ru.apps.googleusercontent.com`
   - This might be from a different project

2. **Do you have your own Google Cloud project?**
   - If yes, use your own Web Client ID
   - If no, create one following `GOOGLE_SIGN_IN_GUIDE.md`

3. **Is Supabase configured?**
   - Check Authentication > Providers > Google
   - Ensure "Skip nonce checks" is enabled

4. **Is the app using the correct Client ID?**
   - Check BuildConfig after rebuild
   - Add logging to verify:
   ```kotlin
   android.util.Log.d("GoogleAuth", "Client ID: ${BuildConfig.GOOGLE_SERVER_CLIENT_ID}")
   ```

---

## ✅ Expected Behavior

When working correctly:

1. User clicks "Authenticate with Google"
2. Google account picker appears
3. User selects account
4. Brief loading indicator
5. User redirected to Dashboard
6. User profile created in Supabase

---

## 🎯 Most Likely Issue

Based on your setup, the most likely issue is:

**Missing or incorrect GOOGLE_SERVER_CLIENT_ID**

**Quick Fix:**
1. Get your Web Client ID from Google Cloud Console
2. Add to `local.properties`:
   ```
   GOOGLE_SERVER_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
   ```
3. Rebuild the app
4. Test again

---

## 🔍 Current Status Analysis

**What's Configured:**
- ✅ Supabase URL and Key are present
- ✅ Default Client ID exists in code: `505119804832-gp3mjp4a0feb3pd84ti0g4llvos1d6ru.apps.googleusercontent.com`
- ❌ `GOOGLE_SERVER_CLIENT_ID` NOT in `local.properties`

**What This Means:**
The app is using a hardcoded default Client ID which is likely from a test/demo project. This Client ID:
- May not be configured for your package name (`com.stoppy.app`)
- May not have your SHA-1 fingerprint registered
- May not be connected to your Supabase project

**Error Flow:**
1. User clicks "Continue with Google" in onboarding
2. `GoogleAuthHelper.getGoogleIdToken()` is called
3. Credential Manager tries to authenticate with the default Client ID
4. Authentication fails (returns `null`)
5. Error message shown: "Google Sign-In cancelled or failed"

---

## 📝 Step-by-Step Fix

### Step 1: Create/Configure Google Cloud Project

If you don't have a Google Cloud project yet:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable **Google+ API** (if not already enabled)

### Step 2: Create OAuth Credentials

**A. Create Web OAuth Client (for Server Client ID):**

1. Go to **APIs & Services > Credentials**
2. Click **+ CREATE CREDENTIALS > OAuth client ID**
3. Application type: **Web application**
4. Name: `LifeForge Web Client`
5. Authorized redirect URIs: Add your Supabase callback URL:
   ```
   https://wnmvqipfifyakepiddti.supabase.co/auth/v1/callback
   ```
6. Click **CREATE**
7. **Copy the Client ID** (ends with `.apps.googleusercontent.com`)
8. **Copy the Client Secret**

**B. Create Android OAuth Client:**

1. Click **+ CREATE CREDENTIALS > OAuth client ID** again
2. Application type: **Android**
3. Name: `LifeForge Android`
4. Package name: `com.stoppy.app`
5. SHA-1 certificate fingerprint: Get it by running:
   ```bash
   cd LifeForgeNative
   .\gradlew.bat signingReport
   ```
   Copy the SHA1 value from the output
6. Click **CREATE**

### Step 3: Configure Supabase

1. Go to [Supabase Dashboard](https://app.supabase.com/)
2. Select your project
3. Navigate to **Authentication > Providers**
4. Find **Google** and click to configure
5. Enable the provider
6. Add your **Web Client ID** (from Step 2A)
7. Add your **Web Client Secret** (from Step 2A)
8. ✅ **IMPORTANT:** Enable **"Skip nonce checks"** (required for Android!)
9. Click **Save**

### Step 4: Update local.properties

Edit `LifeForgeNative/local.properties` and add:

```properties
sdk.dir=D\:\\android studio

# Supabase Configuration
SUPABASE_URL=https://wnmvqipfifyakepiddti.supabase.co
SUPABASE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndubXZxaXBmaWZ5YWtlcGlkZHRpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjY3Mzg5NTYsImV4cCI6MjA4MjMxNDk1Nn0.Cj3EdnW3J6HJbPL38GXzInl73K81igqYx-7qbF6l9IU

# Google Sign-In Configuration
GOOGLE_SERVER_CLIENT_ID=YOUR_WEB_CLIENT_ID_HERE.apps.googleusercontent.com
```

Replace `YOUR_WEB_CLIENT_ID_HERE` with the actual Web Client ID from Step 2A.

### Step 5: Rebuild the App

```bash
cd LifeForgeNative
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

Or in Android Studio:
- **Build > Clean Project**
- **Build > Rebuild Project**

### Step 6: Test

1. Install the rebuilt app on your device
2. Open the app
3. Go through onboarding
4. Click **"Continue with Google"**
5. Select your Google account
6. Should redirect to Dashboard

---

## 🐛 If Still Not Working

### Check Logcat

In Android Studio, open **Logcat** and filter by:
- `GoogleAuthHelper`
- `AuthRepository`
- `AppNavigation`

Look for error messages like:
- `"Google Sign-In cancelled or failed"` → Token is null
- `"Error initializing Google Sign-In"` → Exception in GoogleAuthHelper
- `"Google SignIn error"` → Supabase authentication failed

### Add Debug Logging

Temporarily add logging to `GoogleAuthHelper.kt`:

```kotlin
suspend fun getGoogleIdToken(activityContext: Context): String? {
    android.util.Log.d("GoogleAuth", "Client ID: ${BuildConfig.GOOGLE_SERVER_CLIENT_ID}")
    
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
        .setAutoSelectEnabled(true)
        .build()

    // ... rest of code
    
    return try {
        android.util.Log.d("GoogleAuth", "Requesting credentials...")
        val result = credentialManager.getCredential(
            context = activityContext,
            request = request
        )
        android.util.Log.d("GoogleAuth", "Credentials received!")
        
        val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(result.credential.data)
        android.util.Log.d("GoogleAuth", "Token extracted successfully")
        googleIdTokenCredential.idToken
    } catch (e: Exception) {
        android.util.Log.e("GoogleAuth", "Error: ${e.message}", e)
        e.printStackTrace()
        null
    }
}
```

This will help identify exactly where the failure occurs.

---

**Need Help?**
- Check `GOOGLE_SIGN_IN_GUIDE.md` for complete setup
- Review Logcat for specific error messages
- Verify all checklist items above
- Ensure Google Play Services is installed on your device
