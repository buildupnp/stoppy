# 🚀 Google Sign-In Quick Fix Guide

**Problem:** Google Sign-In button not working in LifeForge app

**Root Cause:** Missing `GOOGLE_SERVER_CLIENT_ID` in `local.properties`

---

## ⚡ Quick Fix (5 Minutes)

### Option 1: Use Existing Google Cloud Project

If you already have a Google Cloud project with OAuth configured:

1. **Get your Web Client ID:**
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Navigate to **APIs & Services > Credentials**
   - Find your **Web client** (not Android client)
   - Copy the Client ID (ends with `.apps.googleusercontent.com`)

2. **Add to local.properties:**
   ```bash
   # Open this file: LifeForgeNative/local.properties
   # Add this line at the end:
   GOOGLE_SERVER_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
   ```

3. **Rebuild:**
   ```bash
   cd LifeForgeNative
   .\gradlew.bat clean
   .\gradlew.bat assembleDebug
   ```

4. **Test:** Run the app and try Google Sign-In

---

### Option 2: Create New Google Cloud Project (15 Minutes)

If you don't have a Google Cloud project yet:

#### Step 1: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click **Select a project** > **NEW PROJECT**
3. Name: `LifeForge`
4. Click **CREATE**

#### Step 2: Configure OAuth Consent Screen

1. Go to **APIs & Services > OAuth consent screen**
2. User Type: **External**
3. Click **CREATE**
4. Fill in:
   - App name: `LifeForge`
   - User support email: Your email
   - Developer contact: Your email
5. Click **SAVE AND CONTINUE**
6. Skip Scopes (click **SAVE AND CONTINUE**)
7. Add test users (your email)
8. Click **SAVE AND CONTINUE**

#### Step 3: Create Web OAuth Client

1. Go to **APIs & Services > Credentials**
2. Click **+ CREATE CREDENTIALS > OAuth client ID**
3. Application type: **Web application**
4. Name: `LifeForge Web Client`
5. Authorized redirect URIs:
   ```
   https://wnmvqipfifyakepiddti.supabase.co/auth/v1/callback
   ```
6. Click **CREATE**
7. **COPY the Client ID** (you'll need this!)
8. **COPY the Client Secret** (you'll need this too!)

#### Step 4: Create Android OAuth Client

1. Click **+ CREATE CREDENTIALS > OAuth client ID** again
2. Application type: **Android**
3. Name: `LifeForge Android`
4. Package name: `com.stoppy.app`
5. Get SHA-1 fingerprint:
   ```bash
   cd LifeForgeNative
   .\gradlew.bat signingReport
   ```
   Look for output like:
   ```
   SHA1: 45:AD:BE:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF
   ```
   Copy this SHA1 value
6. Paste SHA-1 in the form
7. Click **CREATE**

#### Step 5: Configure Supabase

1. Go to [Supabase Dashboard](https://app.supabase.com/)
2. Select your project
3. Navigate to **Authentication > Providers**
4. Find **Google** and click to expand
5. Toggle **Enable Sign in with Google**
6. Paste your **Web Client ID** (from Step 3)
7. Paste your **Web Client Secret** (from Step 3)
8. ✅ **IMPORTANT:** Check **"Skip nonce checks"** (required for Android!)
9. Click **Save**

#### Step 6: Update local.properties

Edit `LifeForgeNative/local.properties`:

```properties
sdk.dir=D\:\\android studio

# Supabase Configuration
SUPABASE_URL=https://wnmvqipfifyakepiddti.supabase.co
SUPABASE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndubXZxaXBmaWZ5YWtlcGlkZHRpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjY3Mzg5NTYsImV4cCI6MjA4MjMxNDk1Nn0.Cj3EdnW3J6HJbPL38GXzInl73K81igqYx-7qbF6l9IU

# Google Sign-In Configuration
GOOGLE_SERVER_CLIENT_ID=YOUR_WEB_CLIENT_ID_HERE.apps.googleusercontent.com
```

Replace `YOUR_WEB_CLIENT_ID_HERE` with the actual Web Client ID from Step 3.

#### Step 7: Rebuild & Test

```bash
cd LifeForgeNative
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

Then run the app and test Google Sign-In!

---

## ✅ Expected Behavior

When working correctly:

1. User opens app → Onboarding screens
2. User clicks **"Continue with Google"**
3. Google account picker appears
4. User selects account
5. Brief loading indicator
6. ✅ User redirected to Dashboard
7. ✅ User profile created in Supabase

---

## 🐛 Troubleshooting

### Error: "Google Sign-In cancelled or failed"

**Cause:** Token is null (authentication failed)

**Check:**
- Is `GOOGLE_SERVER_CLIENT_ID` in `local.properties`?
- Did you rebuild the app after adding it?
- Is the Client ID correct (ends with `.apps.googleusercontent.com`)?

### Error: "Developer error"

**Cause:** SHA-1 fingerprint not registered

**Fix:**
1. Get SHA-1: `.\gradlew.bat signingReport`
2. Add to Android OAuth Client in Google Cloud Console

### Error: "Invalid client ID"

**Cause:** Wrong Client ID or not configured

**Fix:**
- Verify you're using the **Web Client ID** (not Android Client ID)
- Check it's correctly pasted in `local.properties`
- Ensure no extra spaces or quotes

### Sign-In Works But User Not Created

**Cause:** Supabase Google provider not configured

**Fix:**
1. Go to Supabase Dashboard
2. Authentication > Providers > Google
3. Enable provider
4. Add Web Client ID and Secret
5. ✅ Enable "Skip nonce checks"
6. Save

---

## 📱 Testing Checklist

Before testing:
- [ ] `GOOGLE_SERVER_CLIENT_ID` added to `local.properties`
- [ ] App rebuilt (clean + assembleDebug)
- [ ] Google Play Services installed on device
- [ ] Google account added to device
- [ ] Internet connection available

During testing:
- [ ] Click "Continue with Google"
- [ ] Google account picker appears
- [ ] Select account
- [ ] No error messages
- [ ] Redirected to Dashboard

After testing:
- [ ] Check Supabase Dashboard > Authentication > Users
- [ ] Verify user was created
- [ ] Check user profile has correct name/email

---

## 📞 Still Not Working?

1. **Check Logcat** in Android Studio:
   - Filter by: `GoogleAuth`, `AuthRepository`, `AppNavigation`
   - Look for error messages

2. **Add Debug Logging:**
   - See `GOOGLE_SIGNIN_TROUBLESHOOTING.md` for debug code

3. **Verify Configuration:**
   - Google Cloud Console: Web + Android OAuth clients exist
   - Supabase: Google provider enabled with "Skip nonce checks"
   - local.properties: `GOOGLE_SERVER_CLIENT_ID` present

4. **Test with Different Account:**
   - Try a different Google account
   - Ensure account is added to test users in OAuth consent screen

---

## 🎯 Summary

**The Fix:**
1. Add `GOOGLE_SERVER_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com` to `local.properties`
2. Rebuild the app
3. Test Google Sign-In

**Why This Fixes It:**
- The app needs a valid Web Client ID to authenticate with Google
- This Client ID must be registered in Google Cloud Console
- It must be connected to your Supabase project
- Without it, authentication fails silently

**Time Required:**
- If you have credentials: 5 minutes
- If you need to create everything: 15 minutes

---

**For detailed information, see:**
- `GOOGLE_SIGN_IN_GUIDE.md` - Complete setup guide
- `GOOGLE_SIGNIN_TROUBLESHOOTING.md` - Detailed troubleshooting
