package com.example.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * HapticManager: Quản lý phản hồi xúc giác (Haptic Feedback) cho trò chơi mê cung.
 * Tích hợp đầy đủ Android HapticFeedbackConstants để mang lại xúc giác chân thực,
 * chuẩn quy chuẩn hệ thống Android khi người chơi vuốt di chuyển (move) hoặc va chạm tường (wall hit).
 * Đồng thời có fallback mượt mà qua Vibrator / VibrationEffect.
 */
class HapticManager(private val context: Context) {
    var isHapticEnabled: Boolean = true

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Thực hiện phản hồi xúc giác khi người chơi di chuyển 1 ô (Move step).
     * Ưu tiên dùng HapticFeedbackConstants.KEYBOARD_TAP / CLOCK_TICK / VIRTUAL_KEY
     * kết hợp với Vibrator rung siêu nhẹ (10ms) để cảm nhận chân thực tức thì.
     */
    fun performMoveFeedback(targetView: View? = null) {
        if (!isHapticEnabled) return

        var performed = false
        if (targetView != null) {
            val constant = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> HapticFeedbackConstants.CLOCK_TICK
                else -> HapticFeedbackConstants.KEYBOARD_TAP
            }
            performed = targetView.performHapticFeedback(
                constant,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }

        if (!performed) {
            vibrateStep()
        }
    }

    /**
     * Thực hiện phản hồi xúc giác khi người chơi va phải tường mê cung (Wall bump / collision).
     * Ưu tiên dùng HapticFeedbackConstants.REJECT / LONG_PRESS
     * để tạo cảm giác rung giật, cảnh báo va chạm vật lý rõ nét.
     */
    fun performWallHitFeedback(targetView: View? = null) {
        if (!isHapticEnabled) return

        var performed = false
        if (targetView != null) {
            val constant = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> HapticFeedbackConstants.REJECT
                else -> HapticFeedbackConstants.LONG_PRESS
            }
            performed = targetView.performHapticFeedback(
                constant,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }

        if (!performed) {
            vibrateBump()
        }
    }

    /**
     * Thực hiện phản hồi xúc giác khi hoàn thành màn chơi (Level complete).
     * Dùng HapticFeedbackConstants.CONFIRM trên Android R+ hoặc nhịp điệu ăn mừng.
     */
    fun performWinFeedback(targetView: View? = null) {
        if (!isHapticEnabled) return

        if (targetView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            targetView.performHapticFeedback(
                HapticFeedbackConstants.CONFIRM,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
        vibrateWin()
    }

    fun vibrateStep() {
        if (!isHapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(12)
            }
        } catch (_: Exception) {}
    }

    fun vibrateBump() {
        if (!isHapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        } catch (_: Exception) {}
    }

    fun vibrateWin() {
        if (!isHapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 70, 60, 110, 60, 180)
                val amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 70, 60, 110, 60, 180), -1)
            }
        } catch (_: Exception) {}
    }
}
