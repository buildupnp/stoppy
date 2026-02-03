package com.lifeforge.app.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    fun playCoinSound() {
        try {
            // TONE_PROP_ACK is usually a pleasant 'success' ping
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun playSuccessSound() {
        try {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
        } catch (e: Exception) {
            android.util.Log.w("SoundManager", "Failed to play success sound: ${e.message}")
        }
    }
}
