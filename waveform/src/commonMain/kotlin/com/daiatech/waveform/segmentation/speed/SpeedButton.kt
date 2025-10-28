package com.daiatech.waveform.segmentation.speed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SpeedButton(
    modifier: Modifier = Modifier,
    availableSpeeds: List<PlaybackSpeed>,
    selectedSpeed: PlaybackSpeed,
    onSpeedUpdate: (speed: PlaybackSpeed) -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor, CircleShape)
            .clickable { expanded = true }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${selectedSpeed.float}x",
            color = contentColor
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Dropdown arrow",
            tint = contentColor
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        availableSpeeds.forEach { option ->
            DropdownMenuItem(
                text = { Text("${option.float}x") },
                onClick = {
                    onSpeedUpdate(option)
                    expanded = false
                }
            )
        }
    }

}

@Composable
fun SpeedButtonPreview() {
    Surface {
        SpeedButton(
            availableSpeeds = PlaybackSpeed.entries,
            selectedSpeed = PlaybackSpeed.X1_00,
            modifier = Modifier.width(128.dp),
            onSpeedUpdate = { }
        )
    }
}