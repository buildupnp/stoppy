# MediaPipe BlazePose Setup Instructions

## Model File Required

MediaPipe BlazePose requires a model file to be placed in the `app/src/main/assets/` folder.

### Download the Model

1. Download the MediaPipe Pose Landmarker model from:
   - **Lite version (recommended for speed):** https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task
   - **Full version (better accuracy):** https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/1/pose_landmarker_full.task

2. Create the assets folder if it doesn't exist:
   ```
   app/src/main/assets/
   ```

3. Place the downloaded file as:
   ```
   app/src/main/assets/pose_landmarker_lite.task
   ```

   Or for full version:
   ```
   app/src/main/assets/pose_landmarker_full.task
   ```

4. Update the model path in `AIWorkoutScreen.kt` if using full version:
   ```kotlin
   .setModelAssetPath("pose_landmarker_full.task")
   ```

## Model Comparison

| Model | Size | Speed | Accuracy | Use Case |
|-------|------|-------|----------|----------|
| **Lite** | ~2MB | Fast | 95-98% | Recommended for real-time |
| **Full** | ~12MB | Slower | 98-99% | Maximum accuracy |

## Verification

After adding the model file, rebuild the app. The MediaPipe PoseLandmarker will initialize automatically.

If you see errors in logcat about missing model file, verify:
1. File is in `app/src/main/assets/` (not `res/`)
2. File name matches exactly (case-sensitive)
3. File was not corrupted during download

## Benefits Over ML Kit

✅ **33 keypoints** vs 17 (better body tracking)  
✅ **Higher accuracy** (~95-98% vs ~85-90%)  
✅ **Better occlusion handling**  
✅ **More stable tracking**  
✅ **Still completely free**

