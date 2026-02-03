package com.lifeforge.app.ui.screens.forge

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.media.AudioManager
import android.media.ToneGenerator
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.lifeforge.app.ml.OverlayView
import com.lifeforge.app.ml.PoseDetectorHelper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import com.lifeforge.app.R

@Composable
fun AIWorkoutScreen(
    workoutType: String = "pushups", // "pushups" or "squats"
    onClose: () -> Unit,
    onFinish: (Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60) }
    
    // States
    var currentAppState by remember { mutableStateOf(AppState.IDLE) }
    var currentWorkoutState by remember { mutableStateOf(WorkoutState.UP) }
    var repCount by remember { mutableFloatStateOf(0f) }
    val animatedRepScale by animateFloatAsState(
        targetValue = if (repCount % 1f == 0.5f || repCount % 1f == 0f) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = { /* Reset if needed */ },
        label = "RepBounce"
    )
    // We want the scale to pop only on the moment of change, so we'll use a side effect to trigger it
    var lastRepCountForAnim by remember { mutableFloatStateOf(0f) }
    var scaleTrigger by remember { mutableStateOf(1f) }
    val finalRepScale by animateFloatAsState(
        targetValue = scaleTrigger,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "FinalRepScale"
    )

    LaunchedEffect(repCount) {
        if (repCount != lastRepCountForAnim) {
            scaleTrigger = 1.3f
            // Play sound on full rep
            if (repCount % 1f == 0f && repCount > 0) {
                try {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                } catch (e: Exception) {
                    Log.e("AIWorkout", "Error playing tone", e)
                }
            }
            kotlinx.coroutines.delay(100)
            scaleTrigger = 1f
            lastRepCountForAnim = repCount
        }
    }

    var peakRefY by remember { mutableFloatStateOf(0f) }
    var baseRefY by remember { mutableFloatStateOf(0f) }
    var isCalibrated by remember { mutableStateOf(false) }
    var isDetecting by remember { mutableStateOf(false) }
    var countdownText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Ready") }
    var hasCameraPermission by remember { mutableStateOf(false) }

    // Throttling state
    var lastProcessTime by remember { mutableLongStateOf(0L) }
    val throttleMs = 55L // ~18 FPS (1000/18 approx 55)

    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    var overlayRef by remember { mutableStateOf<OverlayView?>(null) }
    var poseDetectorHelper by remember { mutableStateOf<PoseDetectorHelper?>(null) }

    // Screen Lock
    val activity = remember {
        var c = context
        while (c !is Activity && c is ContextWrapper) c = (c as ContextWrapper).baseContext
        c as? Activity
    }

    LaunchedEffect(currentAppState) {
        if (currentAppState != AppState.IDLE) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            mainHandler.removeCallbacksAndMessages(null)
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            toneGenerator.release()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    fun resetCounting() {
        isDetecting = false
        currentAppState = AppState.IDLE
        currentWorkoutState = WorkoutState.UP
        repCount = 0f
        peakRefY = 0f
        baseRefY = 0f
        isCalibrated = false
        countdownText = ""
        statusText = "Ready"
        overlayRef?.clear()
        overlayRef?.setProgress(0f)
        mainHandler.removeCallbacksAndMessages(null)
    }

    fun startCountdown() {
        currentAppState = AppState.COUNTDOWN
        var countdownValue = 3
        isDetecting = true
        
        mainHandler.post(object : Runnable {
            override fun run() {
                if (countdownValue > 0) {
                    countdownText = countdownValue.toString()
                    countdownValue--
                    mainHandler.postDelayed(this, 1000)
                } else {
                    countdownText = "GO!"
                    currentAppState = AppState.CALIBRATING
                    mainHandler.postDelayed({ countdownText = "" }, 1000)
                }
            }
        })
    }

    val poseListener = remember {
        object : PoseDetectorHelper.LandmarkerListener {
            override fun onError(error: String) {
                mainHandler.post {
                    resetCounting()
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            }

            override fun onResults(result: PoseLandmarkerResult, imageHeight: Int, imageWidth: Int) {
                if (!isDetecting) return
                mainHandler.post { overlayRef?.setResults(result, imageHeight, imageWidth) }

                if (result.landmarks().isEmpty()) return
                val landmark = result.landmarks()[0]
                if (landmark.size < 33) return 

                // Identification Logic
                val moveJointY: Float
                val refJointY: Float
                val moveVis: Float
                val refVis: Float

                if (workoutType == "pushups") {
                    // Pushup: Shoulder vs Wrist
                    moveJointY = (landmark[11].y() + landmark[12].y()) / 2f
                    refJointY = (landmark[15].y() + landmark[16].y()) / 2f
                    moveVis = (landmark[11].visibility().orElse(0f) + landmark[12].visibility().orElse(0f)) / 2f
                    refVis = (landmark[15].visibility().orElse(0f) + landmark[16].visibility().orElse(0f)) / 2f
                } else {
                    // Squat: Hip vs Knee (Modified for better perspective from low angles)
                    moveJointY = (landmark[23].y() + landmark[24].y()) / 2f
                    refJointY = (landmark[25].y() + landmark[26].y()) / 2f
                    moveVis = (landmark[23].visibility().orElse(0f) + landmark[24].visibility().orElse(0f)) / 2f
                    refVis = (landmark[25].visibility().orElse(0f) + landmark[26].visibility().orElse(0f)) / 2f
                }

                // Initial Position Check
                if (currentAppState == AppState.SETUP_GUIDE) {
                    if (moveVis > 0.7f && refVis > 0.7f) {
                        mainHandler.post { startCountdown() }
                    }
                    return
                }

                if (currentAppState == AppState.CALIBRATING) {
                    peakRefY = moveJointY
                    baseRefY = refJointY
                    if (baseRefY - peakRefY > 0.1f) {
                        isCalibrated = true
                        currentAppState = AppState.COUNTING
                    }
                    return
                }

                if (currentAppState == AppState.COUNTING) {
                    val totalDistance = (baseRefY - peakRefY)
                    if (totalDistance <= 0.05f) return

                    val currentDescentRatio = (moveJointY - peakRefY) / totalDistance
                    val downThreshold = 0.70f 
                    val upThreshold = 0.15f   
                    val barProgress = (currentDescentRatio / downThreshold).coerceIn(0f, 1f)

                    mainHandler.post {
                        statusText = if (currentWorkoutState == WorkoutState.UP) "MOVE DOWN" else "MOVE UP"
                        overlayRef?.setProgress(barProgress)
                    }

                    if (currentWorkoutState == WorkoutState.UP) {
                        if (currentDescentRatio >= downThreshold) {
                            currentWorkoutState = WorkoutState.DOWN
                            repCount += 0.5f
                        }
                    } else if (currentWorkoutState == WorkoutState.DOWN) {
                        if (currentDescentRatio <= upThreshold) {
                            currentWorkoutState = WorkoutState.UP
                            repCount += 0.5f
                        }
                    }
                }
            }
        }
    }

    if (!hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Camera Permission Required", color = Color.White)
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Camera + Overlay
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val frameLayout = android.widget.FrameLayout(ctx)
                    val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                    val overlayView = OverlayView(ctx, null)
                    overlayRef = overlayView
                    frameLayout.addView(previewView)
                    frameLayout.addView(overlayView)

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                    .build().also { it.setAnalyzer(cameraExecutor) { imageProxy ->
                                        val currentTime = System.currentTimeMillis()
                                        if (isDetecting && currentTime - lastProcessTime >= throttleMs) {
                                            lastProcessTime = currentTime
                                            val bitmap = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
                                            bitmap.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
                                            val matrix = android.graphics.Matrix().apply {
                                                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                                                postScale(-1f, 1f, imageProxy.width * 0.5f, imageProxy.height * 0.5f)
                                            }
                                            val outBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                            if (poseDetectorHelper == null) poseDetectorHelper = PoseDetectorHelper(context = ctx, poseLandmarkerHelperListener = poseListener)
                                            poseDetectorHelper?.detectLiveStream(bitmap = outBitmap, isFrontCamera = true)
                                        }
                                        imageProxy.close()
                                    }}
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis)
                        } catch (e: Exception) { Log.e("AIWorkout", "Binding failed", e) }
                    }, ContextCompat.getMainExecutor(ctx))
                    frameLayout
                }
            )

            // --- Header UI ---
            if (currentAppState == AppState.COUNTING || currentAppState == AppState.CALIBRATING) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(statusText.uppercase(), color = Color(0xFF00E5FF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .graphicsLayer(scaleX = finalRepScale, scaleY = finalRepScale)
                            .size(110.dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            .border(2.dp, Brush.linearGradient(listOf(Color(0xFF00E5FF), Color.Blue)), CircleShape)
                            .shadow(12.dp, CircleShape, spotColor = Color(0xFF00E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(repCount.toInt().toString(), color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black)
                            Text("REPS", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- STEP 1: Phone Setup ---
            if (currentAppState == AppState.SETUP_PHONE) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Canvas(modifier = Modifier.size(180.dp)) {
                            val w = size.width; val h = size.height; val neon = Color(0xFF00E5FF)
                            drawLine(Color.Gray, androidx.compose.ui.geometry.Offset(0f, h * 0.8f), androidx.compose.ui.geometry.Offset(w, h * 0.8f), strokeWidth = 4f)
                            drawRect(neon, topLeft = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.25f), size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.55f), style = Stroke(width = 6f))
                            drawCircle(neon, center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.74f), radius = 8f, style = Stroke(width = 2f))
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("POSITION PHONE", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("Place it against the wall facing you", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                        Spacer(modifier = Modifier.height(48.dp))
                        NeonButton("CONTINUE") { currentAppState = AppState.SETUP_GUIDE; isDetecting = true }
                    }
                }
            }

            // --- STEP 2: Body Guide ---
            if (currentAppState == AppState.SETUP_GUIDE) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        if (workoutType == "pushups") {
                            // Use provided fullplank image for pushup guide
                            Image(
                                painter = painterResource(id = R.drawable.fullplank),
                                contentDescription = "Plank Guide",
                                modifier = Modifier.size(300.dp, 220.dp),
                                alpha = 0.6f
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("STAY IN FRONT", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            Text("ALIGN YOUR BODY WITH THE GUIDE", color = Color(0xFF00E5FF), fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        } else {
                            // Premium Squat Standing Silhouette
                            Canvas(modifier = Modifier.size(200.dp, 260.dp)) {
                                val w = size.width; val h = size.height; val color = Color(0xFF00E5FF).copy(alpha = 0.4f)
                                drawCircle(color, center = androidx.compose.ui.geometry.Offset(w*0.5f, h*0.12f), radius = 20f) // Head
                                drawRect(color, topLeft = androidx.compose.ui.geometry.Offset(w*0.35f, h*0.25f), size = androidx.compose.ui.geometry.Size(w*0.3f, h*0.3f)) // Body
                                drawLine(color, androidx.compose.ui.geometry.Offset(w*0.4f, h*0.55f), androidx.compose.ui.geometry.Offset(w*0.38f, h*0.95f), strokeWidth = 18f) // Leg L
                                drawLine(color, androidx.compose.ui.geometry.Offset(w*0.6f, h*0.55f), androidx.compose.ui.geometry.Offset(w*0.62f, h*0.95f), strokeWidth = 18f) // Leg R
                                drawLine(color, androidx.compose.ui.geometry.Offset(w*0.35f, h*0.25f), androidx.compose.ui.geometry.Offset(w*0.25f, h*0.5f), strokeWidth = 12f) // Arm L
                                drawLine(color, androidx.compose.ui.geometry.Offset(w*0.65f, h*0.25f), androidx.compose.ui.geometry.Offset(w*0.75f, h*0.5f), strokeWidth = 12f) // Arm R
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            Text("STAND UP STRAIGHT", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            Text("SHOW YOUR FULL BODY TO START", color = Color(0xFF00E5FF), fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        }
                    }
                }
            }

            // Countdown
            if (countdownText.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(countdownText, color = Color.Red, fontSize = 150.sp, fontWeight = FontWeight.Black)
                }
            }

            // --- Controls ---
            if (currentAppState == AppState.IDLE || currentAppState == AppState.COUNTING) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 50.dp), contentAlignment = Alignment.BottomCenter) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (currentAppState == AppState.IDLE) {
                            NeonButton("START WORKOUT") { currentAppState = AppState.SETUP_PHONE }
                        } else {
                            // Reordered: Finish is Primary, Reset is Secondary
                            NeonButton("FINISH WORKOUT") { onFinish(repCount.toInt()) }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("RESET SESSION", color = Color.White.copy(0.6f), fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.clickable { resetCounting() }.padding(12.dp))
                        }
                    }
                }
            }

            // --- Circular Close ---
            Box(modifier = Modifier.padding(16.dp).statusBarsPadding()) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(48.dp).background(Color.Black.copy(0.4f), CircleShape).border(1.dp, Color.White.copy(0.15f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun NeonButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(260.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF0088FF))))
            .shadow(20.dp, RoundedCornerShape(30.dp), spotColor = Color(0xFF00E5FF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
    }
}

enum class AppState { IDLE, SETUP_PHONE, SETUP_GUIDE, COUNTDOWN, CALIBRATING, COUNTING }
enum class WorkoutState { UP, DOWN }
