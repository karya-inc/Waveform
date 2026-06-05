# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
# Run demo app (Android)
./gradlew :androidApp:assembleDebug

# Lint / format
./gradlew ktlintCheck       # Check formatting
./gradlew ktlintFormat      # Auto-fix formatting

# Publish library to MavenCentral (requires macOS for iOS framework builds)
./gradlew :waveform:publishToMavenCentral
```

There are no unit tests in this project currently.

## Module Structure

Three Gradle modules:

- **`:waveform`** — The publishable KMP library (`io.github.karya-inc:waveform`). All reusable UI components live here. Targets: Android (minSdk 21), JVM, wasmJs, iOS (x64/Arm64/SimulatorArm64).
- **`:composeApp`** — Multiplatform demo app (Android + iOS + JVM). Shows all library components in action with Jetpack Navigation.
- **`:androidApp`** — Thin Android wrapper that hosts `:composeApp` via `MainActivity`.

## Architecture

**No DI framework** — dependencies are provided via `CompositionLocal` (e.g., `LocalAudioManager`, `LocalAudioPlayerDimensions`). State holders are constructed in composables using `remember`.

**Library component pattern**: each major feature exposes:
1. A `State` class (e.g., `SegmentPickerState`, `AudioPlayerState`) that holds all mutable state and business logic.
2. A `remember*State(...)` composable factory that creates and remembers the state.
3. A top-level `@Composable` UI function that takes the state as a parameter.

**Waveform rendering**: done entirely on `Canvas` composable. `WaveformLayout` data class holds pre-computed positions and dimensions used by canvas draw calls.

**Amplitude processing**: heavy work runs on `Dispatchers.Default`. Amplitudes are processed in chunks of 5000 spikes and cached in `drawableAmplitudesStore` keyed by zoom level.

**Audio playback**: ExoPlayer (Media3) is used in `androidMain` only. Progress updates use a `Handler` at ~10ms intervals. The multiplatform interface is exposed via `LocalAudioManager` CompositionLocal.

**Amplitude extraction**: done by the bundled `Amplituda` AAR at `waveform/libs/amplituda.aar` — Android-only.

## Key Source Locations

All library source lives under `waveform/src/`:

```
commonMain/kotlin/com/daiatech/waveform/
├── graphs/         Bar and line graph composables
├── models/         Segment, AmplitudeType, WaveformColors, WaveformAlignment
├── player/         AudioPlayerState + CenterPinnedAmplitudeBarGraph
├── segmentation/   AudioSegmentationState + AudioSegmentationUi
├── segmentPicker/  SegmentPickerState + AudioSegmentPicker
├── common/         Shared utilities (amplitude math, layout helpers)
└── Utils.kt, Constants.kt

androidMain/kotlin/com/daiatech/waveform/
├── AudioPlayer.kt          ExoPlayer integration
├── marker/                 Audio marker rendering
├── segmentation/           Android-specific segmentation UI
├── segmentPicker/          Android-specific picker UI
└── transcription/          Transcription editor
```

Demo screens are in `composeApp/src/androidMain/kotlin/`.

## Toolchain & Key Versions

- AGP: 9.0.0 | Kotlin: 2.3.0 | Compose Multiplatform: 1.10.3
- compileSdk/targetSdk: 36 | minSdk (library): 21 | Java: 21
- All version aliases are in `gradle/libs.versions.toml`
- ktlint is applied to all subprojects via root `build.gradle.kts`

## Publishing

Library coordinates: `io.github.karya-inc:waveform:0.1.0`. Version is set in `waveform/build.gradle.kts`. CI publishes on tag push matching `v*`.
