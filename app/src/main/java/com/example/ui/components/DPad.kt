package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun DPad(
    onMove: (dx: Int, dy: Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val buttonSize = 52.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // UP
        DPadButton(
            icon = Icons.Default.KeyboardArrowUp,
            contentDescription = "Move Up",
            onClick = { onMove(0, 1) },
            enabled = enabled,
            modifier = Modifier.size(buttonSize).testTag("dpad_up")
        )

        // LEFT, CENTER, RIGHT
        Row(
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPadButton(
                icon = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Move Left",
                onClick = { onMove(-1, 0) },
                enabled = enabled,
                modifier = Modifier.size(buttonSize).testTag("dpad_left")
            )

            DPadButton(
                icon = Icons.Default.KeyboardArrowRight,
                contentDescription = "Move Right",
                onClick = { onMove(1, 0) },
                enabled = enabled,
                modifier = Modifier.size(buttonSize).testTag("dpad_right")
            )
        }

        // DOWN
        DPadButton(
            icon = Icons.Default.KeyboardArrowDown,
            contentDescription = "Move Down",
            onClick = { onMove(0, -1) },
            enabled = enabled,
            modifier = Modifier.size(buttonSize).testTag("dpad_down")
        )
    }
}

@Composable
private fun DPadButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.size(32.dp)
        )
    }
}
