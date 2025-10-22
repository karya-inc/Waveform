package com.daiatech.waveform.segmentation.component

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

/**
 * A drop down menu
 *
 * @param modifier
 * @param options
 * @param selectedItemIdx index of the selected item
 * @param onItemSelected callback when an item is selected
 */
@Composable
fun SpeedButton(
    modifier: Modifier = Modifier,
    availableSpeeds: List<Float>,
    selectedSpeedIdx: Int,
    onSpeedUpdate: (index: Int) -> Unit,
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
            text = if (selectedSpeedIdx in availableSpeeds.indices) {
                "${availableSpeeds[selectedSpeedIdx]}x"
            } else {
                "Speed"
            },
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
        availableSpeeds.forEachIndexed { index, option ->
            DropdownMenuItem(
                text = { Text("${option}x") },
                onClick = {
                    onSpeedUpdate(index)
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
            availableSpeeds = listOf(1f, 2f, 3f),
            selectedSpeedIdx = 0,
            modifier = Modifier.width(128.dp),
            onSpeedUpdate = { }
        )
    }
}