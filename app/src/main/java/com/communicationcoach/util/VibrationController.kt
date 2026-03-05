package com.communicationcoach.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class VibrationController(private val context: Context) {
    companion object {
        private const val TAG = "VibrationController"
        
        // Vibration pattern: 3 quick buzzes for harsh tone
        val HARSH_TONE_PATTERN = longArrayOf(0, 200, 100, 200, 100, 200)
        
        // Vibration pattern: 2 buzzes for talking fast
        val TALKING_FAST_PATTERN = longArrayOf(0, 300, 150, 300)
        
        // Vibration pattern: 1 long buzz for monologuing
        val MONOLOGUING_PATTERN = longArrayOf(0, 500)
    }
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    fun vibrateForBehavior(behaviorType: BehaviorType) {
        val pattern = when (behaviorType) {
            BehaviorType.HARSH_TONE -> HARSH_TONE_PATTERN
            BehaviorType.TALKING_FAST -> TALKING_FAST_PATTERN
            BehaviorType.MONOLOGUING -> MONOLOGUING_PATTERN
        }
        
        vibrate(pattern)
        Log.d(TAG, "Vibrated for behavior: $behaviorType")
    }
    
    private fun vibrate(pattern: LongArray) {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(pattern, -1)
                v.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, -1)
            }
        } ?: Log.w(TAG, "Vibrator not available")
    }
    
    fun cancelVibration() {
        vibrator?.cancel()
    }
}

enum class BehaviorType {
    HARSH_TONE,
    TALKING_FAST,
    MONOLOGUING
}
