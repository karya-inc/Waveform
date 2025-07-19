package com.daiatech.waveform.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.daiatech.waveform.app.ui.theme.WaveFormTheme
import com.daiatech.waveform.app.utils.AudioManagerImpl
import com.daiatech.waveform.app.utils.LocalAudioManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val audioManager = AudioManagerImpl(this.applicationContext)
        setContent {
            WaveFormTheme {
                CompositionLocalProvider(
                    LocalAudioManager provides audioManager,
                ) { App() }
            }
        }
    }
}
