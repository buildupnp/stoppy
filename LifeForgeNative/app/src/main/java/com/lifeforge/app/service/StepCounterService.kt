package com.lifeforge.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.app.PendingIntent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * High-accuracy step counter service using the device's step counter sensor.
 * Features persistence across restarts, validation, and faster updates for ~100% accuracy.
 */
@AndroidEntryPoint
class StepCounterService : Service(), SensorEventListener {
    
    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "lifeforge_steps"
        private const val PREFS_NAME = "step_counter_prefs"
        private const val KEY_LAST_SENSOR_VALUE = "last_sensor_value"
        private const val KEY_LAST_SERVICE_STEPS = "last_service_steps"
        private const val KEY_SENSOR_BASELINE = "sensor_baseline"
        private const val KEY_LAST_UPDATE_TIME = "last_update_time"
        private const val KEY_VEHICLE_OFFSET = "vehicle_offset"
        
        private val _stepCount = MutableStateFlow(0)
        val stepCount: StateFlow<Int> = _stepCount
        
        private var initialSteps: Int = -1

        fun resetSteps() {
            initialSteps = -1
            _stepCount.value = 0
        }
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private lateinit var prefs: SharedPreferences
    
    @javax.inject.Inject
    lateinit var activityRepository: com.lifeforge.app.data.repository.ActivityRepository
    
    @javax.inject.Inject
    lateinit var authRepository: com.lifeforge.app.data.repository.AuthRepository
    
    private var lastLoggedSteps = 0
    private val STEP_BATCH_SIZE = 50 // Log every 50 steps
    
    // Vehicle filtering
    private var isInVehicle = false
    private var vehicleOffset = 0
    
    // Validation thresholds
    private val MAX_STEPS_PER_SECOND = 5 // Maximum realistic steps per second (~18 km/h running)
    private var lastSensorValue: Int = -1
    private var lastUpdateTime: Long = 0
    
    override fun onCreate() {
        super.onCreate()
        
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        
        // Restore previous state
        lastSensorValue = prefs.getInt(KEY_LAST_SENSOR_VALUE, -1)
        lastLoggedSteps = prefs.getInt(KEY_LAST_SERVICE_STEPS, 0)
        initialSteps = prefs.getInt(KEY_SENSOR_BASELINE, -1)
        vehicleOffset = prefs.getInt(KEY_VEHICLE_OFFSET, 0)
        
        requestActivityTransitions()
        
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        
        stepSensor?.let {
            // Use SENSOR_DELAY_UI for faster, more accurate updates (still battery efficient)
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        
        // Save state for persistence
        prefs.edit()
            .putInt(KEY_LAST_SENSOR_VALUE, lastSensorValue)
            .putInt(KEY_LAST_SERVICE_STEPS, lastLoggedSteps)
            .putInt(KEY_SENSOR_BASELINE, initialSteps)
            .putInt(KEY_VEHICLE_OFFSET, vehicleOffset)
            .putLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())
            .apply()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.hasExtra(ActivityTransitionReceiver.EXTRA_IS_IN_VEHICLE)) {
                isInVehicle = it.getBooleanExtra(ActivityTransitionReceiver.EXTRA_IS_IN_VEHICLE, false)
                if (com.lifeforge.app.BuildConfig.DEBUG) {
                    Log.d("StepCounter", "Activity update: isInVehicle = $isInVehicle")
                }
            }
        }
        return START_STICKY
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val totalSteps = it.values[0].toInt()
                val currentTime = System.currentTimeMillis()
                
                // Handle sensor reset (device reboot, sensor reset)
                if (lastSensorValue != -1 && totalSteps < lastSensorValue) {
                    // Sensor was reset, update baseline
                    initialSteps = totalSteps
                    lastLoggedSteps = 0
                    lastSensorValue = totalSteps
                    prefs.edit()
                        .putInt(KEY_SENSOR_BASELINE, initialSteps)
                        .putInt(KEY_LAST_SENSOR_VALUE, lastSensorValue)
                        .putInt(KEY_LAST_SERVICE_STEPS, 0)
                        .apply()
                    _stepCount.value = 0
                    return
                }
                
                // First reading - set as baseline
                if (initialSteps == -1) {
                    initialSteps = totalSteps
                    lastLoggedSteps = 0
                    lastSensorValue = totalSteps
                    lastUpdateTime = currentTime
                    prefs.edit()
                        .putInt(KEY_SENSOR_BASELINE, initialSteps)
                        .putInt(KEY_LAST_SENSOR_VALUE, lastSensorValue)
                        .apply()
                    _stepCount.value = 0
                    return
                }
                
                // Calculate new steps since last reading
                val newSteps = totalSteps - lastSensorValue
                
                // Validate step count (filter out unrealistic jumps)
                if (newSteps > 0) {
                    val timeDiff = (currentTime - lastUpdateTime) / 1000.0 // seconds
                    if (timeDiff > 0) {
                        val stepsPerSecond = newSteps / timeDiff
                        
                        // Only accept if within realistic limits
                        if (stepsPerSecond <= MAX_STEPS_PER_SECOND) {
                            // If in vehicle, add these steps to the offset instead of counting them
                            if (isInVehicle) {
                                vehicleOffset += newSteps
                                prefs.edit().putInt(KEY_VEHICLE_OFFSET, vehicleOffset).apply()
                                if (com.lifeforge.app.BuildConfig.DEBUG) {
                                    Log.d("StepCounter", "Filtering vehicle steps: $newSteps (Total offset: $vehicleOffset)")
                                }
                            }

                            // Calculate total steps since service started, excluding vehicle steps
                            val currentSteps = (totalSteps - initialSteps) - vehicleOffset
                            
                            // Update state
                            lastSensorValue = totalSteps
                            lastUpdateTime = currentTime
                            _stepCount.value = currentSteps
                            
                            // Log steps in batches
                            if (currentSteps - lastLoggedSteps >= STEP_BATCH_SIZE) {
                                val stepsToLog = currentSteps - lastLoggedSteps
                                logSteps(stepsToLog)
                                lastLoggedSteps = currentSteps
                                
                                // Save state after logging
                                prefs.edit()
                                    .putInt(KEY_LAST_SENSOR_VALUE, lastSensorValue)
                                    .putInt(KEY_LAST_SERVICE_STEPS, lastLoggedSteps)
                                    .putLong(KEY_LAST_UPDATE_TIME, lastUpdateTime)
                                    .apply()
                            }
                        } else {
                            // Unrealistic step count detected, log for debugging
                            if (com.lifeforge.app.BuildConfig.DEBUG) {
                                android.util.Log.w("StepCounter", "Filtered unrealistic step count: $newSteps steps in ${timeDiff}s (${stepsPerSecond} steps/s)")
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun logSteps(steps: Int) {
        serviceScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId != null) {
                    val result = activityRepository.logSteps(userId, steps)
                    if (com.lifeforge.app.BuildConfig.DEBUG) {
                        if (result.isSuccess) {
                            android.util.Log.d("StepCounter", "Logged $steps steps, earned ${result.getOrNull()} coins")
                        } else {
                            android.util.Log.e("StepCounter", "Failed to log steps: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                if (com.lifeforge.app.BuildConfig.DEBUG) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(com.lifeforge.app.R.string.step_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(com.lifeforge.app.R.string.step_channel_desc)
                setShowBadge(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(com.lifeforge.app.R.string.step_tracking_title))
            .setContentText(getString(com.lifeforge.app.R.string.step_tracking_desc))
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun requestActivityTransitions() {
        try {
            val transitions = mutableListOf<ActivityTransition>()
            
            // Transitions for In-Vehicle and On-Bicycle (Bike)
            val activities = intArrayOf(DetectedActivity.IN_VEHICLE, DetectedActivity.ON_BICYCLE)
            for (activityType in activities) {
                transitions.add(ActivityTransition.Builder()
                    .setActivityType(activityType)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build())
                transitions.add(ActivityTransition.Builder()
                    .setActivityType(activityType)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build())
            }

            val request = ActivityTransitionRequest(transitions)
            val intent = Intent(this, ActivityTransitionReceiver::class.java).apply {
                action = ActivityTransitionReceiver.ACTION_ACTIVITY_TRANSITION
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                this, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, pendingIntent)
                .addOnSuccessListener {
                    if (com.lifeforge.app.BuildConfig.DEBUG) {
                        Log.d("StepCounter", "Activity transition updates requested successfully")
                    }
                }
                .addOnFailureListener { e ->
                    if (com.lifeforge.app.BuildConfig.DEBUG) {
                        Log.e("StepCounter", "Failed to request activity transition updates", e)
                    }
                }
        } catch (e: Exception) {
            Log.e("StepCounter", "Error setting up Activity Recognition", e)
        }
    }
}
