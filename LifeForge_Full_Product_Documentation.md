# LifeCurrency -- Full Product Documentation (Android MVP)

## 1. Product Name (Final Choice)

Recommended Name: LifeForge\
\
Why LifeForge:\
- Implies discipline, effort, and transformation\
- Neutral, professional, and brandable\
- No negative wording like "addiction"\
- Works globally\
\
Alternate Names:\
- EarnLife\
- MotionPay\
- VitalLock\
- DisciplineOS

## 2. Product Vision

LifeForge converts physical effort into digital freedom.\
Users earn time and access to distracting apps by completing real-world
physical activity.\
\
Core principle:\
Movement → Currency → Access

## 3. MVP Feature Scope (Strict, Non-Complex)

\- Android-only\
- Step-based earning (primary)\
- Camera-based AI push-up detection (automatic)\
- App blocking via overlay\
- No social features\
- No cloud ML

## 4. System Architecture Overview

Frontend: React Native (Expo → Bare Workflow)\
Backend: Supabase (Auth + PostgreSQL)\
AI Processing: On-device (MediaPipe Pose)\
Sensors: Android Step Counter

## 5. App Blocking Logic (Guardian)

Uses Android UsageStatsManager to detect foreground apps.\
Polling interval: 3--5 seconds.\
\
If active app is blocked and TimeBalance \<= 0:\
- Display full-screen overlay using SYSTEM_ALERT_WINDOW\
- Disable interaction until coins are earned or spent

## 6. Exercise & Earning System (Forge)

A. Step Counter:\
- Sensor.TYPE_STEP_COUNTER\
- Coins = steps / 100\
- Near-zero battery usage\
\
B. Push-up Detection:\
- MediaPipe Pose Landmarker\
- Track LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST\
- Angle-based rep detection\
- Works fully offline

## 7. Push-up Detection Logic

Angle Calculation:\
θ = angle between shoulder--elbow--wrist vectors\
\
State Logic:\
- Up: θ \> 160°\
- Down: θ \< 90°\
- Valid Rep: Up → Down → Up

## 8. Battery, Data & Storage Optimization

\- No background camera usage\
- Camera sessions \< 60 seconds\
- Pose processing at 15 FPS\
- No video/image storage\
- Supabase sync only on balance change

## 9. Database Schema (Supabase)

profiles:\
- id (uuid)\
- coin_balance (int)\
- last_step_count (bigint)\
\
blocked_apps:\
- id (uuid)\
- user_id (uuid)\
- package_name (text)\
\
history:\
- id (uuid)\
- amount (int)\
- type (text)\
- created_at (timestamp)

## 10. Required Android Permissions

\- PACKAGE_USAGE_STATS\
- SYSTEM_ALERT_WINDOW\
- ACTIVITY_RECOGNITION\
- CAMERA\
- FOREGROUND_SERVICE

## 11. Development Roadmap

Phase 1: Auth, UI, Supabase\
Phase 2: Step counter & coins\
Phase 3: App blocking overlay\
Phase 4: MediaPipe push-up detection\
Phase 5: Optimization & Play Store compliance

## 12. Future Enhancements (Post-MVP)

\- iOS support\
- More exercises (squats, planks)\
- Streaks & goals\
- Smart daily limits
