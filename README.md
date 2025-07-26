# 🎵 Waveform

A modern, Jetpack Compose-based Android library to visualize audio waveforms, playback progress, and interactively select or segment audio clips with ease.


## ✨ Features

- 📊 **Graph Visualizations**: Render line and bar graphs for audio amplitude data.
- 🎧 **Playback Visualizer**: Display a scrollable, center-pinned waveform synced to audio playback.
- ✂️ **Audio Segmentation Tool**: Select multiple audio segments using an intuitive timeline interface.
- 🔍 **Audio Segment Picker**: Zoom into waveform regions and fine-tune a single segment.

## 📦 Installation

```kotlin
dependencies {
    implementation("io.github.karya-inc:waveform:<latest-version>")
}
```


## 🚀 Quick Start

### Amplitude Bar Graph

```kotlin
@Composable
fun BarGraphExample() {
    AmplitudeBarGraph(
        amplitudes = listOf(100, 200, 300, 500, 100, 20),
        onProgressChange = { newProgress -> /* handle scrub */ }
    )
}
```

<img width="452" height="73" alt="image" src="https://github.com/user-attachments/assets/98ed44d1-8b85-4c98-b576-2520d4eeaeca" />


### Center-Pinned Waveform for Playback

```kotlin
@Composable
fun PlaybackVisualizer() {
    CenterPinnedAmplitudeBarGraph(
        amplitudes = listOf(100, 200, 300, 500, 100, 20),
        durationMs = 2000,
        progressMs = 1000
    )
}
```
<img width="452" height="132" alt="image" src="https://github.com/user-attachments/assets/ff048242-6226-4c17-a2ea-3030a6a388ca" />


## ✂️ Audio Segmentation

### Segment Selector with Multiple Segments

```kotlin
val segmentationState = rememberAudioSegmentationState(
    audioFilePath = audioPath,
    amplitudes = amplitudeList,
    durationMs = totalDuration,
    enableAdjustment = true
)

AudioSegmentationUi(state = segmentationState)
```
<img width="422" height="399" alt="image" src="https://github.com/user-attachments/assets/782fa21f-b249-4734-b91a-25f815c560be" />



- ✅ Drag to select segments
- 🛠️ Enable adjustments with min/max segment durations
- 📄 Output format: `List<Pair<startMs, endMs>>`

## 🎚 Audio Segment Picker

Zoom into a waveform and select a precise segment interactively.

```kotlin
val pickerState = rememberAudioSegmentPickerState(
    audioFilePath = audioPath,
    amplitudes = amplitudeList,
    durationMs = totalDuration,
    segment = Pair(0, totalDuration / 4),
    window = Pair(0, totalDuration / 8)
)

AudioSegmentPicker(
    state = pickerState,
    mainPlayerProgress = pickerState.activeSegment.first + currentProgress,
    segmentPlaybackProgress = pickerState.activeSegment.first + currentProgress,
    isPlaying = isPlaying,
    toggleSegmentPlayback = { 
        val segment = pickerState.activeSegment
        // Control playback with your own ExoPlayer instance
    }
)
```
<img width="429" height="243" alt="image" src="https://github.com/user-attachments/assets/86a7d732-6b20-4cad-8696-5499a49faa6b" />


## 📈 Graph Variants

### Bar and Line Graphs

```kotlin
Graph(
    amplitudes = listOf(200f, 30f, 45f, 5f, 16f, 20f),
    type = GraphType.Bar,
    maxAmplitude = 300f
)

Graph(
    amplitudes = listOf(200f, 30f, 45f, 5f, 16f, 20f),
    type = GraphType.Line,
    maxAmplitude = 300f
)
```
<img width="409" height="477" alt="image" src="https://github.com/user-attachments/assets/24de3ff4-b3e8-4740-9bad-ce075853fa12" />



## 🛠 Customization

You can customize:

- Spike width, spacing, and corner radius
- Waveform alignment (`Top`, `Center`, `Bottom`)
- Drawing style (`Fill` or `Stroke`)
- Amplitude aggregation (`Max`, `Average`)
- Zoom levels and segment constraints

## 🧱 Built With

- Jetpack Compose
- ExoPlayer (integration friendly)
- Kotlin Coroutines

## 📄 License

MIT License. See [LICENSE](LICENSE) for details.

## 🤝 Contributing

Contributions, bug reports, and feature suggestions are welcome! Feel free to open an issue or pull request.

## 🙏 Acknowledgements

This library is inspired by https://github.com/lincollincol/compose-audiowaveform by @lincollincol
