# 🚀 Fix Google Sign-In NOW (5 Minutes)

## The Problem
Your Google Sign-In button doesn't work because you're missing one line in your configuration file.

## The Fix
Add one line to `LifeForgeNative/local.properties`

---

## 🎯 Step 1: Get Your Web Client ID

### Option A: You Already Have It
If you already set up Google Cloud Console for this app:

1. Go to https://console.cloud.google.com/
2. Click **APIs & Services** → **Credentials**
3. Look for **"Web client"** (NOT "Android client")
4. Copy the Client ID (looks like: `123456789-abc123.apps.googleusercontent.com`)

### Option B: You Need to Create It
If you haven't set up Google Cloud yet:

1. Go to https://console.cloud.google.com/
2. Create a new project: **LifeForge**
3. Go to **APIs & Services** → **Credentials**
4. Click **+ CREATE CREDENTIALS** → **OAuth client ID**
5. Choose **Web application**
6. Name: `LifeForge Web Client`
7. Add Authorized redirect URI:
   ```
   https://wnmvqipfifyakepiddti.supabase.co/auth/v1/callback
   ```
8. Click **CREATE**
9. Copy the **Client ID**

---

## 🎯 Step 2: Add It to local.properties

1. Open this file: `LifeForgeNative/local.properties`

2. Add this line at the end:
   ```properties
   GOOGLE_SERVER_CLIENT_ID=YOUR_CLIENT_ID_HERE.apps.googleusercontent.com
   ```

3. Replace `YOUR_CLIENT_ID_HERE` with the actual Client ID you copied

**Example:**
```properties
sdk.dir=D\:\\android studio

# Supabase Configuration
SUPABASE_URL=https://wnmvqipfifyakepiddti.supabase.co
SUPABASE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndubXZxaXBmaWZ5YWtlcGlkZHRpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjY3Mzg5NTYsImV4cCI6MjA4MjMxNDk1Nn0.Cj3EdnW3J6HJbPL38GXzInl73K81igqYx-7qbF6l9IU

# Google Sign-In Configuration
GOOGLE_SERVER_CLIENT_ID=123456789-abc123def456.apps.googleusercontent.com
```

4. Save the file

---

## 🎯 Step 3: Rebuild the App

Open PowerShell/Command Prompt and run:

```bash
cd LifeForgeNative
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

Or in Android Studio:
- **Build** → **Clean Project**
- **Build** → **Rebuild Project**

---

## 🎯 Step 4: Test It

1. Run the app on your device
2. Go through onboarding
3. Click **"Continue with Google"**
4. Select your Google account
5. ✅ Should work now!

---

## ⚠️ Still Not Working?

### Did you also create an Android OAuth Client?

You need TWO OAuth clients in Google Cloud Console:

1. **Web client** (for the Client ID above) ✅
2. **Android client** (for your app)

**To create Android client:**

1. Go to https://console.cloud.google.com/
2. **APIs & Services** → **Credentials**
3. **+ CREATE CREDENTIALS** → **OAuth client ID**
4. Choose **Android**
5. Package name: `com.stoppy.app`
6. Get SHA-1 fingerprint:
   ```bash
   cd LifeForgeNative
   .\gradlew.bat signingReport
   ```
   Copy the SHA1 value from the output
7. Paste SHA-1 in the form
8. Click **CREATE**

### Did you configure Supabase?

1. Go to https://app.supabase.com/
2. Select your project
3. **Authentication** → **Providers** → **Google**
4. Enable it
5. Add your Web Client ID and Secret
6. ✅ **Enable "Skip nonce checks"** (Important!)
7. Click **Save**

---

## 📋 Quick Checklist

Before testing:
- [ ] Web Client ID added to `local.properties`
- [ ] App rebuilt (clean + assembleDebug)
- [ ] Android OAuth Client created with SHA-1
- [ ] Supabase Google provider enabled
- [ ] "Skip nonce checks" enabled in Supabase
- [ ] Google Play Services on device
- [ ] Google account added to device

---

## 🎉 That's It!

Once you add the Client ID and rebuild, Google Sign-In will work perfectly.

**Need more help?**
- See `GOOGLE_SIGNIN_QUICK_FIX.md` for detailed steps
- See `GOOGLE_SIGNIN_TROUBLESHOOTING.md` for debugging
- See `GOOGLE_SIGNIN_STATUS.md` for technical details

---

## 🤔 Why Does This Fix It?

Your app needs to tell Google "I'm allowed to authenticate users." The Web Client ID is like a password that proves your app is legitimate. Without it, Google rejects the authentication request.

The default hardcoded ID in your code (`505119804832-gp3mjp4a0feb3pd84ti0g4llvos1d6ru.apps.googleusercontent.com`) is from a test project and doesn't work for your app.

By adding YOUR Web Client ID to `local.properties`, the app uses your credentials instead, and Google accepts the authentication. ✅
