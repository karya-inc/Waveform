package com.daiatech.waveform.app.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
fun HomeScreen(
    navigateToSegmentation: (String) -> Unit,
    navigateToSegmentPicker: (String) -> Unit
) {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<String>>(listOf()) }
    var selectedFileIdx by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        context.filesDir?.listFiles()?.map { it.path }
            ?.filter { it.split(".").last() == "mp3" }
            ?.let { files = it }
    }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val identifier = System.currentTimeMillis().toString().substring(0, 6)
            val file = File(context.filesDir, "sample_audio_$identifier.mp3")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            // recompose
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { audioPicker.launch("audio/*") }) {
            Text("Pick Audio")
        }

        AnimatedVisibility(selectedFileIdx != null) {
            selectedFileIdx?.let { selected ->
                Column {
                    Button(onClick = { navigateToSegmentation(files[selected]) }) {
                        Text("Audio Segmentation")
                    }

                    Button(onClick = { navigateToSegmentPicker(files[selected]) }) {
                        Text("Audio Segment Picker")
                    }
                }
            }
        }

        files.forEachIndexed { idx, filePath ->
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedFileIdx = idx },
                headlineContent = {
                    val displayName = filePath.split("/").last()
                    Text(displayName)
                },
                leadingContent = {
                    RadioButton(
                        selected = (idx == selectedFileIdx),
                        onClick = null
                    )
                }
            )
        }
    }
}