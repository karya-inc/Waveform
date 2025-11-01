package com.daiatech.waveform.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.daiatech.waveform.app.screens.AmplitudeGraphsScreen
import com.daiatech.waveform.app.screens.AudioSegmentPickerScreen
import com.daiatech.waveform.app.screens.AudioSegmentationScreen
import com.daiatech.waveform.app.screens.AudioSegmentationScreen2
import com.daiatech.waveform.app.screens.HomeScreen
import com.daiatech.waveform.models.Segment
import kotlinx.serialization.Serializable

@Serializable
data class SegmentationScreen(val audioFilePath: String)

@Serializable
data class SegmentationScreen2(val audioFilePath: String)

@Serializable
data class SegmentPickerScreen(val audioFilePath: String)

@Serializable
data class GraphVisualizationScreen(val audioFilePath: String)

@Serializable
object HomeScreen

@Composable
fun AndroidApp() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val navController = rememberNavController()
        val segments = remember { mutableListOf<Segment>() }
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
                    navigateToSegmentation2 = { path ->
                        navController.navigate(SegmentationScreen2(path))
                    },
                    navigateToSegmentPicker = { path ->
                        navController.navigate(SegmentPickerScreen(path))
                    },
                    navigateToAmplitudeGraph = { path ->
                        navController.navigate(GraphVisualizationScreen(path))

                    }
                )
            }

            composable<SegmentationScreen2> { backStackEntry ->
                val route = backStackEntry.toRoute<SegmentationScreen2>()
                AudioSegmentationScreen2(
                    audioFilePath = route.audioFilePath,
                    segments = segments,
                    onSubmit = { segment ->
                        segments.add(segment)
                        navController.navigate(SegmentationScreen2(route.audioFilePath)) {
                            popUpTo(route) { inclusive = true }
                        }
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

            composable<GraphVisualizationScreen> { backStackEntry ->
                val route = backStackEntry.toRoute<SegmentPickerScreen>()
                AmplitudeGraphsScreen(audioFilePath = route.audioFilePath)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidApp()
}