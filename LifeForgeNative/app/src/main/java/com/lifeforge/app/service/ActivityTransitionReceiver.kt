package com.lifeforge.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

/**
 * Receiver for Activity Transition updates.
 * Signals StepCounterService when user enters or exits a vehicle.
 */
class ActivityTransitionReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_ACTIVITY_TRANSITION = "com.lifeforge.app.ACTION_ACTIVITY_TRANSITION"
        const val EXTRA_IS_IN_VEHICLE = "extra_is_in_vehicle"
        
        // Static state to allow Service to query current state easily if needed
        @Volatile
        var isInVehicle: Boolean = false
            private set
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_ACTIVITY_TRANSITION) {
            if (ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(intent) ?: return
                for (event in result.transitionEvents) {
                    val activityType = event.activityType
                    val transitionType = event.transitionType
                    
                    val isVehicle = activityType == DetectedActivity.IN_VEHICLE || 
                                   activityType == DetectedActivity.ON_BICYCLE
                    
                    if (isVehicle) {
                        if (transitionType == com.google.android.gms.location.ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                            isInVehicle = true
                            notifyService(context, true)
                            if (com.lifeforge.app.BuildConfig.DEBUG) {
                                Log.d("ActivityTransition", "Entered Vehicle/Bike")
                            }
                        } else {
                            isInVehicle = false
                            notifyService(context, false)
                            if (com.lifeforge.app.BuildConfig.DEBUG) {
                                Log.d("ActivityTransition", "Exited Vehicle/Bike")
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun notifyService(context: Context, inVehicle: Boolean) {
        val intent = Intent(context, StepCounterService::class.java).apply {
            putExtra(EXTRA_IS_IN_VEHICLE, inVehicle)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
