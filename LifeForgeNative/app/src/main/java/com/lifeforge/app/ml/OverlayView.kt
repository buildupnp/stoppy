package com.lifeforge.app.ml

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.max

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint()
    private var linePaintRed = Paint()
    private var linePaintWhite = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f
    
    private var topThreshold: Float? = null
    private var bottomThreshold: Float? = null
    private var depthProgress: Float = 0f // 0.0 to 1.0
    private var smoothedProgress: Float = 0f
    private val emaAlpha = 0.2f // Smoothing factor for buttery movement

    private var thresholdPaint = Paint()
    private var barOutlinePaint = Paint()
    private var barFillPaint = Paint()

    init {
        initPaints()
    }

    private fun initPaints() {
        thresholdPaint.color = Color.TRANSPARENT // Invisible as requested
        thresholdPaint.strokeWidth = 4f
        thresholdPaint.style = Paint.Style.STROKE
        thresholdPaint.alpha = 0

        barOutlinePaint.color = Color.WHITE
        barOutlinePaint.strokeWidth = 4f
        barOutlinePaint.style = Paint.Style.STROKE

        barFillPaint.color = Color.parseColor("#00E5FF") // Blue (matching app theme)
        barFillPaint.style = Paint.Style.FILL
        linePaintRed.color = Color.parseColor("#00E5FF") // Blue Skeleton line
        linePaintRed.strokeWidth = 8f
        linePaintRed.style = Paint.Style.STROKE
        // Neon Glow for lines
        linePaintRed.setShadowLayer(15f, 0f, 0f, Color.parseColor("#00E5FF"))

        linePaintWhite.color = Color.WHITE
        linePaintWhite.strokeWidth = 22f 
        linePaintWhite.style = Paint.Style.STROKE

        pointPaint.color = Color.RED // Red joints
        pointPaint.strokeWidth = 25f
        pointPaint.style = Paint.Style.FILL
        // Neon Glow for joints
        pointPaint.setShadowLayer(20f, 0f, 0f, Color.RED)
        pointPaint.textSize = 50f
    }

    fun clear() {
        results = null
        topThreshold = null
        bottomThreshold = null
        smoothedProgress = 0f
        invalidate()
    }

    fun setThresholds(top: Float?, bottom: Float?) {
        this.topThreshold = top
        this.bottomThreshold = bottom
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val scaleFactor = width.toFloat() / imageWidth
        val scaleFactorH = height.toFloat() / imageHeight
        val offsetY = 0f

        drawProgressBar(canvas)

        results?.let { poseLandmarkerResult ->
            for (landmark in poseLandmarkerResult.landmarks()) {
                if (landmark.size > 32) {
                    // Actual Logic Connections (Shoulder -> Elbow -> Wrist)
                    drawConnection(canvas, landmark[11], landmark[13])
                    drawConnection(canvas, landmark[13], landmark[15])
                    drawConnection(canvas, landmark[12], landmark[14])
                    drawConnection(canvas, landmark[14], landmark[16])

                    // Decorative Connections (Shoulders, Hips, Torso, Legs, Head, Ankles)
                    drawConnection(canvas, landmark[11], landmark[12]) // Shoulder link
                    drawConnection(canvas, landmark[23], landmark[24]) // Hip link
                    drawConnection(canvas, landmark[11], landmark[23]) // Left Torso
                    drawConnection(canvas, landmark[12], landmark[24]) // Right Torso
                    drawConnection(canvas, landmark[23], landmark[25]) // Left Knee
                    drawConnection(canvas, landmark[24], landmark[26]) // Right Knee
                    drawConnection(canvas, landmark[25], landmark[27]) // Left Ankle
                    drawConnection(canvas, landmark[26], landmark[28]) // Right Ankle

                    // Draw head (simplified connection to shoulders)
                    drawConnection(canvas, landmark[11], landmark[0]) 
                    drawConnection(canvas, landmark[12], landmark[0])

                    // Draw joints (Red circles)
                    listOf(0, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28).forEach { idx ->
                        drawJoint(canvas, landmark[idx])
                    }
                }
            }
        }
    }

    private fun drawConnection(canvas: Canvas, start: com.google.mediapipe.tasks.components.containers.NormalizedLandmark, end: com.google.mediapipe.tasks.components.containers.NormalizedLandmark) {
        val startX = start.x() * imageWidth * scaleFactor + offsetX
        val startY = start.y() * imageHeight * scaleFactor + offsetY
        val endX = end.x() * imageWidth * scaleFactor + offsetX
        val endY = end.y() * imageHeight * scaleFactor + offsetY

        // Draw white background line (like an outline)
        canvas.drawLine(startX, startY, endX, endY, linePaintWhite)
        // Draw red foreground line
        canvas.drawLine(startX, startY, endX, endY, linePaintRed)
    }

    private fun drawJoint(canvas: Canvas, landmark: com.google.mediapipe.tasks.components.containers.NormalizedLandmark) {
        val px = landmark.x() * imageWidth * scaleFactor + offsetX
        val py = landmark.y() * imageHeight * scaleFactor + offsetY
        canvas.drawCircle(px, py, 15f, pointPaint)
    }

    private fun drawProgressBar(canvas: Canvas) {
        val barWidth = 40f
        val barHeight = height * 0.5f
        val left = width - 80f
        val top = (height - barHeight) / 2f
        val right = left + barWidth
        val bottom = top + barHeight

        // Draw outline
        canvas.drawRect(left, top, right, bottom, barOutlinePaint)

        // Draw fill based on progress
        val fillTop = bottom - (barHeight * depthProgress.coerceIn(0f, 1f))
        canvas.drawRect(left, fillTop, right, bottom, barFillPaint)

        // Draw label
        canvas.drawText("DEPTH", left - 40f, top - 20f, pointPaint.apply { textSize = 30f; color = Color.WHITE })
    }

    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int
    ) {
        this.results = poseLandmarkerResults
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
        offsetX = (width - imageWidth * scaleFactor) / 2f
        offsetY = (height - imageHeight * scaleFactor) / 2f
        invalidate()
    }

    fun setProgress(progress: Float) {
        this.depthProgress = progress
        invalidate()
    }
}
