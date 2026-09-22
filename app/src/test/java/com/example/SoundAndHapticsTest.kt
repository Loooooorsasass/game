package com.example

import com.example.ui.components.GridControlMode
import com.example.ui.components.GridGestureInputHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SoundAndHapticsTest {

    @Test
    fun testControlModeToggle() {
        val testScope = TestScope()
        var moved = false
        val handler = GridGestureInputHandler(
            coroutineScope = testScope,
            onValidateAndMove = { _, _ ->
                moved = true
                true
            },
            initialControlMode = GridControlMode.AUTO
        )

        assertEquals(GridControlMode.AUTO, handler.controlMode)
        val toggled = handler.toggleControlMode()
        assertEquals(GridControlMode.STEP_BY_STEP, toggled)
        assertEquals(GridControlMode.STEP_BY_STEP, handler.controlMode)

        val toggledBack = handler.toggleControlMode()
        assertEquals(GridControlMode.AUTO, toggledBack)
    }

    @Test
    fun testHapticCallbacksTriggeredOnMoveAndWallHit() = runTest {
        var moveHapticTriggered = false
        var wallHitHapticTriggered = false

        var canWalk = true
        val handler = GridGestureInputHandler(
            coroutineScope = this,
            onValidateAndMove = { _, _ -> canWalk },
            initialControlMode = GridControlMode.AUTO,
            onMoveHaptic = { moveHapticTriggered = true },
            onWallHitHaptic = { wallHitHapticTriggered = true }
        )

        // Valid move
        canWalk = true
        handler.handleInputStart(androidx.compose.ui.geometry.Offset(100f, 100f))
        val moved = handler.handleInputMove(androidx.compose.ui.geometry.Offset(50f, 0f)) // right swipe > 28px
        assertTrue(moved)
        assertTrue(moveHapticTriggered)
        assertFalse(wallHitHapticTriggered)

        // Reset and test wall hit
        moveHapticTriggered = false
        wallHitHapticTriggered = false
        canWalk = false

        handler.handleInputStart(androidx.compose.ui.geometry.Offset(100f, 100f))
        val wallHit = handler.handleInputMove(androidx.compose.ui.geometry.Offset(-50f, 0f)) // left swipe into wall
        assertFalse(wallHit)
        assertFalse(moveHapticTriggered)
        assertTrue(wallHitHapticTriggered)
    }
}
