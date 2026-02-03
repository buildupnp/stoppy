# Security Setup Guide for LifeForge

## ✅ Credentials Status

### Supabase Configuration
- **URL:** `https://wnmvqipfifyakepiddti.supabase.co`
- **API Key:** Configured (anon key)
- **Expiry:** 2082 (valid for ~56 years)
- **Status:** ✅ Valid JWT format

### Google Sign-In
- **Client ID:** Configured in `build.gradle.kts`
- **Status:** ✅ Configured

---

## 🔒 Security Checklist

### 1. ✅ Sensitive Files Protected

The following files are now protected by `.gitignore`:

- ✅ `local.properties` - Contains SDK path and Supabase credentials
- ✅ `keystore.properties` - Contains signing key information
- ✅ `*.jks` - Keystore files
- ✅ `*.keystore` - Alternative keystore format
- ✅ `*.log` - Log files that may contain sensitive data
- ✅ `*.hprof` - Memory dumps

### 2. ⚠️ Keystore Configuration

**Current Status:** Keystore properties are empty

```properties
storeFile=my-release-key.jks
storePassword=
keyAlias=
keyPassword=
```

**Action Required:** Before releasing to production:

1. Generate a release keystore:
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

2. Update `keystore.properties`:
```properties
storeFile=my-release-key.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=my-key-alias
keyPassword=YOUR_KEY_PASSWORD
```

3. **IMPORTANT:** Never commit `keystore.properties` or `.jks` files to version control!

### 3. ✅ ProGuard Rules Enhanced

ProGuard rules have been updated to:
- Keep all critical classes (Supabase, Room, Hilt, Services)
- Remove debug logging in release builds
- Protect accessibility and background services
- Handle Kotlin serialization properly

### 4. ✅ Code Quality Improvements

Fixed the following issues:
- ✅ Replaced null assertion operator (`!!`) with safe calls
- ✅ Added logging to empty catch blocks
- ✅ Enhanced error handling in settings navigation

---

## 🔐 Environment Variables (Recommended for CI/CD)

For production builds, consider using environment variables instead of `local.properties`:

### Option 1: GitHub Actions / CI/CD

```yaml
# .github/workflows/build.yml
env:
  SUPABASE_URL: ${{ secrets.SUPABASE_URL }}
  SUPABASE_KEY: ${{ secrets.SUPABASE_KEY }}
  KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
```

### Option 2: Local Development

Create a `.env` file (add to `.gitignore`):
```bash
SUPABASE_URL=https://wnmvqipfifyakepiddti.supabase.co
SUPABASE_KEY=your_key_here
```

Then modify `build.gradle.kts` to read from environment:
```kotlin
buildConfigField("String", "SUPABASE_URL", "\"${System.getenv("SUPABASE_URL") ?: ""}\"")
buildConfigField("String", "SUPABASE_KEY", "\"${System.getenv("SUPABASE_KEY") ?: ""}\"")
```

---

## 🛡️ Supabase Security Best Practices

### Row Level Security (RLS)

Ensure your Supabase tables have RLS enabled:

```sql
-- Enable RLS on all tables
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE coin_balances ENABLE ROW LEVEL SECURITY;
ALTER TABLE activities ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_locks ENABLE ROW LEVEL SECURITY;

-- Example policy: Users can only access their own data
CREATE POLICY "Users can view own profile"
ON profiles FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can update own profile"
ON profiles FOR UPDATE
USING (auth.uid() = user_id);
```

### API Key Types

Your current key is an **anon key** (public key). This is correct for client-side apps.

- ✅ **Anon Key:** Safe to use in mobile apps (current setup)
- ❌ **Service Role Key:** NEVER use in mobile apps (server-side only)

---

## 📱 MIUI Security Considerations

### Xiaomi Permission Helper

Your app includes MIUI-specific permission handling:
- ✅ Autostart permission
- ✅ Background popup permission
- ✅ Battery optimization whitelist

### Testing on Redmi Devices

Before release, test on:
- Redmi Note 10/11/12
- Redmi 9/10/11
- POCO X3/X4/X5
- Different MIUI versions (12, 13, 14)

---

## 🚀 Pre-Release Checklist

### Before Building Release APK:

- [ ] Generate release keystore
- [ ] Update `keystore.properties` with real credentials
- [ ] Verify `.gitignore` includes all sensitive files
- [ ] Test ProGuard build: `./gradlew assembleRelease`
- [ ] Verify Supabase RLS policies are enabled
- [ ] Test on multiple Redmi devices
- [ ] Review ProGuard mapping file for debugging
- [ ] Set up crash reporting (Firebase Crashlytics recommended)
- [ ] Enable Google Play App Signing (recommended)

### Security Audit:

- [ ] No hardcoded passwords or secrets in code
- [ ] All API calls use HTTPS
- [ ] User data encrypted at rest (Supabase handles this)
- [ ] Proper authentication flow
- [ ] Session management secure
- [ ] No sensitive data in logs (release build)

---

## 📞 Support

If you encounter security issues:
1. Do NOT commit sensitive files
2. Rotate compromised keys immediately
3. Update Supabase credentials if exposed
4. Review Git history for leaked secrets

---

## 🔄 Credential Rotation

If credentials are compromised:

### Supabase:
1. Go to Supabase Dashboard
2. Settings > API
3. Reset anon key
4. Update `local.properties`
5. Rebuild app

### Keystore:
1. Generate new keystore
2. Update app signing in Google Play Console
3. Use Play App Signing for easier management

---

**Last Updated:** January 27, 2026  
**Status:** ✅ Security measures implemented
