package com.tapread.nfc.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Centralized haptic feedback for the whole app.
 */
object HapticUtil {

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= 31) {
            val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            mgr.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /** Light tap — for button presses, list item taps */
    fun tick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /** Medium pulse — for card scan start */
    fun pulse(context: Context) {
        val v = getVibrator(context)
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(80)
        }
    }

    /** Success pattern — for card read complete (da-da-DONE!) */
    fun success(context: Context) {
        val v = getVibrator(context)
        if (Build.VERSION.SDK_INT >= 26) {
            val pattern = longArrayOf(0, 50, 80, 50, 80, 120)
            val amplitudes = intArrayOf(0, 80, 0, 80, 0, 200)
            v.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(longArrayOf(0, 50, 80, 50, 80, 120), -1)
        }
    }

    /** Error buzz — for failed reads */
    fun error(context: Context) {
        val v = getVibrator(context)
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(200, 255))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(200)
        }
    }

    /** POS terminal beep pattern — for the easter egg */
    fun posTerminal(context: Context) {
        val v = getVibrator(context)
        if (Build.VERSION.SDK_INT >= 26) {
            // Mimics a POS terminal processing: tick-tick-tick-tick-BEEP!
            val pattern = longArrayOf(0, 40, 60, 40, 60, 40, 60, 40, 60, 40, 200, 300)
            val amps = intArrayOf(0, 60, 0, 80, 0, 100, 0, 120, 0, 140, 0, 255)
            v.vibrate(VibrationEffect.createWaveform(pattern, amps, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(longArrayOf(0, 40, 60, 40, 60, 40, 60, 40, 200, 300), -1)
        }
    }

    /** Heartbeat — for dramatic effect */
    fun heartbeat(context: Context) {
        val v = getVibrator(context)
        if (Build.VERSION.SDK_INT >= 26) {
            val pattern = longArrayOf(0, 100, 100, 100, 400, 100, 100, 100)
            val amps = intArrayOf(0, 200, 0, 120, 0, 200, 0, 120)
            v.vibrate(VibrationEffect.createWaveform(pattern, amps, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(longArrayOf(0, 100, 100, 100, 400, 100, 100, 100), -1)
        }
    }
}
