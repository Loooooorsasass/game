package com.example.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Hằng số cấu hình điều khiển di chuyển trên lưới ô (Grid-based movement)
 */
const val HOLD_THRESHOLD_MS = 150L   // Khoảng chờ trước khi kích hoạt di chuyển liên tục khi giữ ngón tay
const val MOVE_SPEED_MS = 180L       // Thời gian lặp lại mỗi bước khi giữ và thời gian trượt Lerp/Tween

/**
 * Chế độ điều khiển di chuyển
 */
enum class GridControlMode(val displayName: String, val description: String) {
    AUTO("⚡ Tự Động", "Vuốt = 1 ô, Giữ = Đi liên tục"),
    STEP_BY_STEP("👣 Từng Bước", "Chỉ đi 1 ô mỗi lần vuốt")
}

/**
 * Bộ xử lý thao tác cử chỉ trên lưới (GridGestureInputHandler)
 * Hỗ trợ vuốt nhanh (Swipe), vuốt & giữ (Hold & Drag), nhả tay (Release),
 * kiểm tra va chạm tường (Collision detection), di chuyển chuẩn từng ô (Grid snapping),
 * và tích hợp Android HapticFeedbackConstants mang lại cảm giác xúc giác chân thực.
 */
class GridGestureInputHandler(
    private val coroutineScope: CoroutineScope,
    private val onValidateAndMove: (dx: Int, dy: Int) -> Boolean,
    initialControlMode: GridControlMode = GridControlMode.AUTO,
    var targetView: View? = null,
    var onWallHitHaptic: (() -> Unit)? = null,
    var onMoveHaptic: (() -> Unit)? = null
) {
    // Chế độ điều khiển hiện tại ('AUTO' hoặc 'STEP_BY_STEP')
    var controlMode: GridControlMode = initialControlMode
        private set

    // Ngưỡng phát hiện vuốt (độ lệch tối thiểu bằng pixel)
    private val swipeThresholdPx = 28f

    // Trạng thái theo dõi vị trí con trỏ / ngón tay
    private var startPosition = Offset.Zero
    private var accumulatedDragX = 0f
    private var accumulatedDragY = 0f
    private var isHolding = false
    private var hasMovedInitialStep = false
    private var lastDx = 0
    private var lastDy = 0

    // Coroutine Job điều khiển việc di chuyển liên tục khi vuốt và giữ
    private var continuousMoveJob: Job? = null

    /**
     * Chuyển đổi giữa 2 chế độ điều khiển: AUTO và STEP_BY_STEP
     */
    fun setControlMode(mode: GridControlMode) {
        controlMode = mode
        if (mode == GridControlMode.STEP_BY_STEP) {
            cancelContinuousMovement()
        }
    }

    /**
     * Đảo chế độ điều khiển (dùng cho nút Toggle trên UI)
     */
    fun toggleControlMode(): GridControlMode {
        val newMode = if (controlMode == GridControlMode.AUTO) {
            GridControlMode.STEP_BY_STEP
        } else {
            GridControlMode.AUTO
        }
        setControlMode(newMode)
        return newMode
    }

    /**
     * Xử lý sự kiện bắt đầu chạm/nhấn (touchstart / mousedown)
     * Thiết lập tọa độ xuất phát và reset các bộ tích lũy khoảng cách vuốt.
     */
    fun handleInputStart(position: Offset) {
        cancelContinuousMovement()
        startPosition = position
        accumulatedDragX = 0f
        accumulatedDragY = 0f
        isHolding = true
        hasMovedInitialStep = false
        lastDx = 0
        lastDy = 0
    }

    /**
     * Xử lý sự kiện di chuyển ngón tay / kéo chuột (touchmove / mousemove)
     * Xác định hướng vuốt chính (Ngang hoặc Dọc), thực hiện bước đầu tiên,
     * kích hoạt phản hồi xúc giác HapticFeedbackConstants,
     * và kích hoạt timer di chuyển liên tục nếu ở chế độ AUTO.
     */
    fun handleInputMove(delta: Offset): Boolean {
        if (!isHolding) return false

        accumulatedDragX += delta.x
        accumulatedDragY += delta.y

        val absX = abs(accumulatedDragX)
        val absY = abs(accumulatedDragY)

        // Kiểm tra xem khoảng cách kéo đã vượt qua ngưỡng vuốt chưa
        if (absX > swipeThresholdPx || absY > swipeThresholdPx) {
            val dx: Int
            val dy: Int

            if (absX > absY) {
                dx = if (accumulatedDragX > 0) 1 else -1
                dy = 0
            } else {
                // Trong hệ tọa độ màn hình Android: kéo lên (y âm) là đi Bắc (dy = 1), kéo xuống là Nam (dy = -1)
                dx = 0
                dy = if (accumulatedDragY < 0) 1 else -1
            }

            // Reset bộ tích lũy sau khi xác định hướng
            accumulatedDragX = 0f
            accumulatedDragY = 0f

            if (!hasMovedInitialStep || (dx != lastDx || dy != lastDy)) {
                lastDx = dx
                lastDy = dy
                hasMovedInitialStep = true

                // Thực hiện bước đi đầu tiên ngay lập tức
                val moveSuccess = updatePlayerPosition(dx, dy)

                // Nếu bước đầu tiên đụng tường hoặc bị chặn, dừng ngay lập tức và rung va chạm
                if (!moveSuccess) {
                    cancelContinuousMovement()
                    triggerWallHitHaptic()
                    return false
                } else {
                    triggerMoveHaptic()
                }

                // Nếu chế độ là AUTO, khởi động luồng vuốt & giữ (Hold & Drag)
                if (controlMode == GridControlMode.AUTO) {
                    startContinuousMovement(dx, dy)
                }
            }
            return true
        }

        return false
    }

    /**
     * Xử lý sự kiện nhả ngón tay / buông chuột (touchend / mouseup / touchcancel)
     * Hủy toàn bộ timer/job, dừng di chuyển lập tức tại vị trí ô hiện tại.
     */
    fun handleInputEnd() {
        isHolding = false
        cancelContinuousMovement()
        accumulatedDragX = 0f
        accumulatedDragY = 0f
        hasMovedInitialStep = false
        lastDx = 0
        lastDy = 0
    }

    /**
     * Cập nhật vị trí nhân vật trên lưới theo hướng (dx, dy)
     * Kiểm tra va chạm với tường (isTileWalkable/canMove) trước khi xác nhận di chuyển.
     * Trả về true nếu di chuyển hợp lệ, false nếu bị chặn bởi tường hoặc hết màn.
     */
    fun updatePlayerPosition(dx: Int, dy: Int): Boolean {
        if (dx == 0 && dy == 0) return false
        return onValidateAndMove(dx, dy)
    }

    /**
     * Khởi động coroutine di chuyển liên tục khi người chơi vuốt và giữ ngón tay
     * Chờ HOLD_THRESHOLD_MS (150ms), sau đó lặp lại mỗi MOVE_SPEED_MS (180ms)
     * cho đến khi nhả tay hoặc gặp tường cản.
     */
    private fun startContinuousMovement(dx: Int, dy: Int) {
        cancelContinuousMovement()
        continuousMoveJob = coroutineScope.launch {
            // Chờ khoảng ngưỡng giữ ban đầu
            delay(HOLD_THRESHOLD_MS)

            // Vòng lặp di chuyển liên tục
            while (isActive && isHolding) {
                val canContinue = updatePlayerPosition(dx, dy)
                if (!canContinue) {
                    // Gặp vật cản hoặc đụng tường: Kích hoạt xúc giác va tường và dừng di chuyển liên tục
                    triggerWallHitHaptic()
                    break
                } else {
                    triggerMoveHaptic()
                }
                delay(MOVE_SPEED_MS)
            }
        }
    }

    /**
     * Kích hoạt xúc giác khi di chuyển hợp lệ bằng HapticFeedbackConstants
     */
    fun triggerMoveHaptic() {
        targetView?.let { view ->
            val constant = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> HapticFeedbackConstants.CLOCK_TICK
                else -> HapticFeedbackConstants.KEYBOARD_TAP
            }
            view.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
        }
        onMoveHaptic?.invoke()
    }

    /**
     * Kích hoạt xúc giác khi va phải tường bằng HapticFeedbackConstants
     */
    fun triggerWallHitHaptic() {
        targetView?.let { view ->
            val constant = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> HapticFeedbackConstants.REJECT
                else -> HapticFeedbackConstants.LONG_PRESS
            }
            view.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
        }
        onWallHitHaptic?.invoke()
    }

    /**
     * Hủy bỏ quá trình di chuyển liên tục
     */
    fun cancelContinuousMovement() {
        continuousMoveJob?.cancel()
        continuousMoveJob = null
    }
}

/**
 * Modifier mở rộng cho Jetpack Compose: Lắng nghe thao tác cử chỉ Grid Gesture
 * Tương thích cả cảm ứng trên điện thoại (touch) lẫn chuột/bàn phím trên máy tính (pointer/mouse).
 */
fun Modifier.gridGestureInput(
    handler: GridGestureInputHandler
): Modifier = this.pointerInput(handler) {
    awaitEachGesture {
        // Chờ sự kiện chạm/nhấn đầu tiên (touchstart / mousedown)
        val down = awaitFirstDown(requireUnconsumed = false)
        handler.handleInputStart(down.position)

        var pointer = down.id
        var isDown = true

        while (isDown) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointer } ?: event.changes.firstOrNull()

            if (change == null || !change.pressed) {
                // Sự kiện nhả tay (touchend / mouseup)
                isDown = false
                handler.handleInputEnd()
            } else {
                // Sự kiện di chuyển (touchmove / mousemove)
                val delta = change.positionChange()
                if (delta != Offset.Zero) {
                    change.consume()
                    handler.handleInputMove(delta)
                }
            }
        }
    }
}

/**
 * Modifier mở rộng xử lý phím mũi tên trên bàn phím máy tính (Arrow keys)
 * Phục vụ trải nghiệm người chơi trên Desktop / Web / Laptop.
 */
fun Modifier.gridKeyboardInput(
    onMove: (dx: Int, dy: Int) -> Boolean
): Modifier = this.onKeyEvent { keyEvent: KeyEvent ->
    if (keyEvent.type == KeyEventType.KeyDown) {
        when (keyEvent.key) {
            Key.DirectionUp -> {
                onMove(0, 1)
                true
            }
            Key.DirectionDown -> {
                onMove(0, -1)
                true
            }
            Key.DirectionLeft -> {
                onMove(-1, 0)
                true
            }
            Key.DirectionRight -> {
                onMove(1, 0)
                true
            }
            else -> false
        }
    } else {
        false
    }
}
