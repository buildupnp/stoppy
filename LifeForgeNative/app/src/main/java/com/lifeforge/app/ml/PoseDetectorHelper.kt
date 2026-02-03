package com.lifeforge.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class PoseDetectorHelper(
    val context: Context,
    val runningMode: RunningMode = RunningMode.LIVE_STREAM,
    val minPoseDetectionConfidence: Float = 0.5f,
    val minPoseTrackingConfidence: Float = 0.5f,
    val minPosePresenceConfidence: Float = 0.5f,
    var poseLandmarkerHelperListener: LandmarkerListener? = null
) {

    private var poseLandmarker: PoseLandmarker? = null

    init {
        setupPoseLandmarker()
    }

    fun setupPoseLandmarker() {
        val baseOptionsBuilder = BaseOptions.builder()
        baseOptionsBuilder.setDelegate(Delegate.CPU)
        // Switching to the Full (Medium) model for a balance of speed and accuracy
        baseOptionsBuilder.setModelAssetPath("pose_landmarker_full.task")

        try {
            val optionsBuilder = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setRunningMode(runningMode)
                .setMinPoseDetectionConfidence(minPoseDetectionConfidence)
                .setMinTrackingConfidence(minPoseTrackingConfidence)
                .setMinPosePresenceConfidence(minPosePresenceConfidence)

            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder.setResultListener(this::returnLivestreamResult)
                optionsBuilder.setErrorListener(this::returnLivestreamError)
            }

            poseLandmarker = PoseLandmarker.createFromOptions(context, optionsBuilder.build())
        } catch (e: Throwable) {
            poseLandmarker = null
            poseLandmarkerHelperListener?.onError("Pose Landmarker failed to initialize: ${e.message}")
        }
    }

    fun detectLiveStream(bitmap: Bitmap, isFrontCamera: Boolean) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            throw IllegalArgumentException("Attempting to call detectLiveStream while not in LIVE_STREAM mode.")
        }
        val frameTime = SystemClock.uptimeMillis()
        val mpImage = BitmapImageBuilder(bitmap).build()
        detectAsync(mpImage, frameTime)
    }

    private fun detectAsync(mpImage: MPImage, frameTime: Long) {
        poseLandmarker?.detectAsync(mpImage, frameTime)
    }

    private fun returnLivestreamResult(
        result: PoseLandmarkerResult,
        input: MPImage
    ) {
        poseLandmarkerHelperListener?.onResults(result, input.height, input.width)
    }

    private fun returnLivestreamError(error: Throwable) {
        poseLandmarkerHelperListener?.onError(error.message ?: "An unknown error has occurred")
    }

    interface LandmarkerListener {
        fun onError(error: String)
        fun onResults(result: PoseLandmarkerResult, imageHeight: Int, imageWidth: Int)
    }
}
