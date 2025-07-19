package com.daiatech.waveform.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.daiatech.waveform.app.screens.AudioSegmentPickerScreen
import com.daiatech.waveform.app.screens.AudioSegmentationScreen
import com.daiatech.waveform.app.screens.HomeScreen
import com.daiatech.waveform.app.ui.theme.WaveFormTheme
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class SegmentationScreen(val audioFilePath: String)

@Serializable
data class SegmentPickerScreen(val audioFilePath: String)

@Serializable
object HomeScreen

@Composable
fun App() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val navController = rememberNavController()
        val context = LocalContext.current
        NavHost(
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            startDestination = HomeScreen
        ) {

            composable<HomeScreen> {
                HomeScreen(
                    navigateToSegmentation = { path ->
                        navController.navigate(SegmentationScreen(path))
                    },
                    navigateToSegmentPicker = { path ->
                        navController.navigate(SegmentPickerScreen(path))
                    }
                )
            }

            composable<SegmentationScreen> { backStackEntry ->
                val route = backStackEntry.toRoute<SegmentationScreen>()
                AudioSegmentationScreen(audioFilePath = route.audioFilePath)
            }

            composable<SegmentPickerScreen> { backStackEntry ->
                val route = backStackEntry.toRoute<SegmentPickerScreen>()
                AudioSegmentPickerScreen(audioFilePath = route.audioFilePath)
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