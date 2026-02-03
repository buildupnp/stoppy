# 🛡️ Stoppy (Native Android)

Welcome to the native implementation of Stoppy! This guide will help you run and test the app.

## 🚀 How to Run the App

### Option 1: Using Android Studio (Recommended)

1.  **Open the Project:**
    *   Open Android Studio.
    *   Click **File > Open**.
    *   Navigate to `d:\stoppy\LifeForgeNative` and click **OK**.
    *   *Wait for the project to "sync" (you'll see progress bars at the bottom right). This might take a few minutes the first time.*

2.  **Select a Device:**
    *   In the top toolbar, look for the device dropdown (usually says "No Devices" or "Pixel...").
    *   **If you have an Android Phone:** Connect it via USB. Enable "USB Debugging" in your phone's Developer Options. It should appear in the dropdown.
    *   **If you don't have a phone:**
        *   Click the dropdown > **Device Manager**.
        *   Click **Create Device**.
        *   Choose "Pixel 7" (or similar) > Next.
        *   Download a system image (like "Release > Tiramisu" or "UpsideDownCake") > Next > Finish.
        *   Select this new emulator in the dropdown.

3.  **Run the App:**
    *   Click the green **▶️ Play Button** in the top toolbar.
    *   The app will build and launch on your device/emulator.

### Option 2: Using Command Line (Terminal)

1.  Open your terminal/command prompt.
2.  Navigate to the project folder:
    ```bash
    cd d:\stoppy\LifeForgeNative
    ```
3.  Run the build command:
    ```bash
    .\gradlew.bat installDebug
    ```
    *This builds the app and installs it on a connected device/emulator.*

---

## 🧪 How to Test Features

### 1. Test App Blocking (The Core Feature)
*   **Enable Permissions:** When the app first opens (or go to Settings tab):
    *   Click "Accessibility Service" -> Find "Stoppy" -> Turn **ON**.
    *   Click "Usage Access" -> Find "Stoppy" -> Turn **ON**.
    *   Click "Overlay Permission" -> Find "Stoppy" -> Turn **ON**.
*   **Block an App:**
    *   Go to the **Guardian** tab (Shield icon).
    *   You should see apps like Instagram or TikTok listed. Ensure the toggle is **ON** (Blocked).
*   **Trigger the Lock:**
    *   Press your phone's Home button.
    *   Open the blocked app (e.g., Instagram).
    *   **Result:** The Stoppy Lock Screen should immediately cover the app! 🔒

### 2. Test Unlocking
*   On the Lock Screen, tap **"Unlock for 30m"**.
*   If you have enough coins, the overlay will disappear, and you can use the app for 30 minutes.

### 3. Test Earning Coins
*   Go to the **Forge** tab (Hammer icon).
*   **Push-ups:** Click "Log Push-ups", enter "10", and submit. You should hear a buzz/vibration and see coins added.
*   **Steps:** Walk around with your phone. Click "Sync Steps" to convert steps to coins.

### 4. Test Settings
*   Go to **Settings** tab.
*   Toggle "Notifications" or "Sound Effects" to see the UI update.

---

## 🆘 Troubleshooting

*   **"Build Failed"**: Check the "Build" tab at the bottom of Android Studio for errors.
*   **"Device not found"**: Make sure your USB cable is connected or the Emulator is running.
*   **App crashes on start**: Check the "Logcat" tab at the bottom to see the error message.
