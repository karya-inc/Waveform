package com.daiatech.waveform.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.daiatech.waveform.app.ui.theme.WaveFormTheme
import com.daiatech.waveform.segmetation2.AudioSegmentPicker
import com.daiatech.waveform.segmetation2.rememberAudioSegmentPickerState
import linc.com.amplituda.Amplituda
import linc.com.amplituda.AmplitudaResult
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WaveFormTheme {
                App()
            }
        }
    }
}

@Composable
fun App() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val activity = LocalActivity.current
        val lifecycleOwner = LocalLifecycleOwner.current
        var audioFilePath by remember { mutableStateOf<String?>(null) }
        val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent())
        { uri ->
            if (uri != null && activity != null) {
                val identifier = System.currentTimeMillis().toString().substring(0, 6)
                val file = File(activity.filesDir, "sample_audio_$identifier.mp3")
                activity.contentResolver.openInputStream(uri)?.use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                audioFilePath = file.path
            }
        }
        LaunchedEffect(Unit) {
            audioFilePath = activity?.filesDir?.listFiles()
                ?.firstOrNull { it.name.split(".").last() == ".mp3" }?.path
        }
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    activity?.filesDir?.listFiles()?.forEach { it.delete() }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        var amplitudes by remember { mutableStateOf<List<Int>?>(null) }
        var duration by remember { mutableStateOf<Long?>(null) }
        LaunchedEffect(audioFilePath) {
            if (audioFilePath != null && activity != null) {
                try {
                    Amplituda(activity).processAudio(audioFilePath)
                        .get(
                            { result ->
                                duration =
                                    result.getAudioDuration(AmplitudaResult.DurationUnit.MILLIS)
                                amplitudes = result.amplitudesAsList()
                                    .map { it.times(4000).toInt() }
                            },
                            { error ->
                                error.printStackTrace()
                            }
                        )
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        activity,
                        "Unsupported hardware detected. Amplitudes won't be calculated!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            audioFilePath?.let { path ->
                amplitudes?.let { amplitudes ->
                    duration?.let { duration ->
                        val pickerState = rememberAudioSegmentPickerState(
                            audioFilePath = path,
                            amplitudes = amplitudes,
                            durationMs = duration,
                            segment = Pair(0, duration / 4),
                            window = Pair(0, duration / 8)
                        )
                        AudioSegmentPicker(
                            state = pickerState,
                            mainPlayerProgress = 0,
                            segmentPlaybackProgress = 0,
                        )
                    }
                }
            }
            Button(onClick = { audioPicker.launch("audio/*") }) {
                Text("Pick Audio")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WaveFormTheme {
        App()
    }
}