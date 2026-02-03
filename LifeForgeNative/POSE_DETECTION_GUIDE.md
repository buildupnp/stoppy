# Pose Detection & Skeleton Implementation Guide

This document outlines the architecture and mathematical logic used in the GetFit app to detect body joints and render an accurate, stable skeleton figure in real-time.

---

## 1. High-Level Architecture
The system follows a 5-step pipeline:
1. **Frame Capture**: Input from CameraX.
2. **ML Inference**: Google ML Kit (BlazePose) identifies 33 landmarks.
3. **Signal Processing**: EMA Smoothing to remove jitter.
4. **Geometric Normalization**: Ensuring scale and position invariance.
5. **Rendering**: Drawing the skeleton on a `GraphicOverlay`.

---

## 2. Detection Engine: ML Kit BlazePose
The core detection is handled by `com.google.mlkit:pose-detection`. Unlike older models, this provides **3D coordinates (X, Y, Z)**.

### Key Logic in `Camera.kt`:
```kotlin
val options = PoseDetectorOptions.Builder()
    .setDetectorMode(PoseDetectorOptions.STREAM_MODE) // Optimized for constant video feed
    .build()
val poseDetector = PoseDetection.getClient(options)
```
- **Stream Mode**: Essential for video. It uses "tracking" to predict where joints will be in the next frame based on their velocity, drastically increasing speed.

---

## 3. Achieving "Perfect" Accuracy: The Math

### A. Translation Normalization (Position Invariance)
To make your skeleton logic work regardless of where the person stands in the camera frame, you must translate all points to a "local" coordinate system.
- **Logic**: Find the midpoint between the `LEFT_HIP` and `RIGHT_HIP`. Subtract this midpoint from every other joint's (X, Y, Z) coordinates.
- **Result**: The person's hips effectively become `(0,0,0)`.

### B. Scale Normalization (Distance Invariance)
To make the detection work for both short and tall people, or people standing far away:
- **Logic**: Calculate the "Torso Size" (distance between the shoulder-midpoint and hip-midpoint).
- **Transformation**: Divide every landmark's distance from the center by this Torso Size multiplier.
- **Reference**: See [`PoseEmbedding.java`](file:///app/src/main/java/com/example/android/getfit/classification/PoseEmbedding.java#L42) for the specific multiplier logic.

---

## 4. Visualizing the Skeleton (`PoseGraphic.kt`)
To draw the lines between joints, you maintain a predefined list of "Pairs."

### Landmark Pairs:
To draw the torso and limbs, connect the following:
- **Torso**: `LEFT_SHOULDER` to `RIGHT_SHOULDER`, `LEFT_HIP` to `RIGHT_HIP`.
- **Arms**: `SHOULDER` -> `ELBOW` -> `WRIST`.
- **Legs**: `HIP` -> `KNEE` -> `ANKLE`.

### Drawing Logic:
```kotlin
fun drawLine(canvas: Canvas, start: PoseLandmark, end: PoseLandmark, paint: Paint) {
    canvas.drawLine(
        translateX(start.position.x), 
        translateY(start.position.y),
        translateX(end.position.x), 
        translateY(end.position.y), 
        paint
    )
}
```
*Note: `translateX/Y` are helper functions that map the internal ML Kit coordinate space (e.g., 0-1280) to the actual screen pixels.*

---

## 5. Stability & Smoothing (`EMASmoothing.java`)
Raw ML data can "flicker." To prevent your skeleton from shaking, implement an **Exponential Moving Average (EMA)**.

- **Formula**: `NewValue = (CurrentFrameValue * Alpha) + (PreviousValue * (1 - Alpha))`
- **Optimal Alpha**: The app uses a small alpha (e.g., 0.2). This makes the skeleton feel "heavy" and smooth rather than jittery.

---

## 6. Depth Analysis (The Z-Axis)
The Z-coordinate represents distance relative to the hips. In this app, shoulders are color-coded based on Z:
- **Red**: Joint is moving towards the camera.
- **Blue**: Joint is moving away from the camera.
- **Reference**: Check [`PoseGraphic.kt:L196`](file:///app/src/main/java/com/example/android/getfit/PoseGraphic.kt#L196) for the `maybeUpdatePaintColor` logic.

---

## 7. Implementation Checklist
If you are building your own skeleton tracker, do these in order:
1. [ ] Set up **CameraX** to deliver `ImageProxy` frames.
2. [ ] Pass frames to **ML Kit Pose Detector**.
3. [ ] Create a **GraphicOverlay** (a custom View that sits on top of the camera).
4. [ ] Implement **Translation Normalization** (Hips as 0,0).
5. [ ] Connect joints using a **Line List** (Shoulder-Elbow, Elbow-Wrist).
6. [ ] Apply **EMA Smoothing** to the raw coordinates before drawing.

---

## References in this Codebase:
- **Detection Setup**: [Camera.kt](file:///app/src/main/java/com/example/android/getfit/Camera.kt)
- **Drawing Code**: [PoseGraphic.kt](file:///app/src/main/java/com/example/android/getfit/PoseGraphic.kt)
- **Math & Scaling**: [PoseEmbedding.java](file:///app/src/main/java/com/example/android/getfit/classification/PoseEmbedding.java)
- **Stability**: [EMASmoothing.java](file:///app/src/main/java/com/example/android/getfit/classification/EMASmoothing.java)
