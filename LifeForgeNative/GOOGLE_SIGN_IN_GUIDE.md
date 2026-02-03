# Google Sign-In Setup Guide for Stoppy

This guide will help you configure Google Sign-In. You need to perform steps in three places: **Google Cloud Console**, **Supabase**, and **Android Studio**.

---

### Step 1: Google Cloud Console Setup
1.  Go to the [Google Cloud Console](https://console.cloud.google.com/).
2.  Create a **New Project** named "Stoppy".
3.  Go to **APIs & Services > OAuth consent screen**.
    - Choose **External**.
    - Fill in App Name ("Stoppy"), User support email, and Developer contact info.
    - Click "Save and Continue" through the scopes and test users sections.
4.  Go to **Credentials > Create Credentials > OAuth client ID**.
    - Select **Application type: Android**.
    - **Name**: Stoppy Android Client.
    - **Package Name**: `com.lifeforge.app` (This must match your `build.gradle.kts`).
    - **SHA-1 certificate fingerprint**: See Step 2 below to get this.
5.  Click **Create**. You'll see a Client ID, but you don't need this yet.

---

### Step 2: Get your SHA-1 Fingerprint
#### Option A: Gradle Tab (If tasks are visible)
1.  Open the **Gradle** tab on the right side.
2.  Go to **Stoppy > app > Tasks > android > signingReport**.
3.  Double-click **signingReport**.

#### Option B: Terminal (Fastest Fallback)
If you can't see the tasks in the Gradle tab:
1.  In Android Studio, open the **Terminal** tab at the bottom.
2.  Type or paste this command: `.\gradlew signingReport`
3.  Press Enter.

*Note: If you get a **JAVA_HOME** error, see the Troubleshooting section at the bottom.*

#### Option C: Manual Keytool (If Gradle fails)
Copy and paste this into your **PowerShell** terminal:
`keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android`

#### Finding the SHA-1:
- In the output (Run window or Terminal), look for the `SHA1` under `variant: debug`. 
- It looks like: `45:AD:BE:EF:...`
- Copy this SHA-1 and paste it into the Android Client ID creation in Step 1.

---

### Step 3: Get the Web Client ID (Crucial for Code)
1.  In Google Cloud Console, go to **Credentials**.
2.  You will likely see an **"OAuth 2.0 Client IDs"** section.
3.  Notice there is an entry titled **"Web client (auto-created by Google Service)"** or create a new one choosing **Application type: Web application**.
4.  **Copy the "Client ID"** for the **Web application**.
    - It ends with `.apps.googleusercontent.com`.
    - **This is your "Server Client ID"**.

---

### Step 4: Supabase Configuration
1.  Go to your [Supabase Dashboard](https://app.supabase.com/).
2.  Navigate to **Authentication > Providers > Google**.
3.  Enable the Google provider.
4.  **Client ID**: Paste your **Web Client ID**.
5.  **Client Secret**: Paste your **Web Client Secret**.
6.  **Skip nonce checks**: ✅ **Enable this** (Check the box). This makes the Android login much smoother.
7.  **Allow users without an email**: ❌ **Disable this** (Default). Google always provides an email.
8.  **Callback URL**: You can **ignore this** for the Android app! The app handles the login internally. 
    *(Optional: If you ever make a website version of Stoppy, you would copy this URL and paste it into "Authorized redirect URIs" for the Web Client in Google Cloud Console).*
9.  Click **Save**.

---

### Step 5: Update the App Code
1.  In Android Studio, open `app/src/main/java/com/lifeforge/app/util/GoogleAuthHelper.kt`.
2.  Find the line: `.setServerClientId("YOUR_WEB_CLIENT_ID_HERE")`
3.  Replace `YOUR_WEB_CLIENT_ID_HERE` with your **Web Client ID** from Step 3.

---

### Step 6: google-services.json (Optional but Recommended)
1.  In Google Cloud Console, go to **APIs & Services > Credentials**.
2.  If you use Firebase with this project later, you can download the `google-services.json` from Firebase. 
3.  For pure Supabase + Credential Manager, simply having the **Web Client ID** in `GoogleAuthHelper.kt` and the **SHA-1** registered in Google Cloud is usually enough for local testing!

---

### Verification
Once steps 1-5 are done:
1.  Run the app.
2.  Go to the final onboarding slide.
3.  Click **AUTHENTICATE WITH GOOGLE**.
4.  The system should show your Google accounts. Select one, and it will log you into the Dashboard!
