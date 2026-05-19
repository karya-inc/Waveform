package com.daiatech.waveform.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sizing and spacing tokens for [WaveformAudioPlayerUi].
 *
 * Supply a custom instance via [ProvidePlayerDimensions] to make the player more compact or to
 * match your design system. All children — [PlayerWaveform], [PlayerToolbar], and the underlying
 * [AudioPlayerState] layout — read from [LocalAudioPlayerDimensions], so a single
 * [ProvidePlayerDimensions] call at the top of your player hierarchy is enough.
 *
 * @property graphHeight Height of the waveform bar graph. Affects the overall canvas height.
 * @property verticalItemSpacing Spacing used between rows in the waveform canvas (above/below the
 *   graph, around timestamp markers). Reducing this shrinks the total canvas height.
 * @property markerFontSize Font size of the timestamp labels drawn on the waveform.
 * @property toolbarPadding Outer padding applied to the [PlayerToolbar] row on all sides.
 * @property toolbarItemSpacing Horizontal gap between the speed column, play button, and zoom
 *   column in the toolbar.
 * @property toolbarLabelSpacing Vertical gap between a label ("Speed" / "Zoom") and its button,
 *   and between the play button and the elapsed-time text beneath it.
 * @property playButtonSize Diameter of the circular play/pause button.
 * @property iconSize Size of the play/pause icon drawn inside the button.
 * @property controlButtonHeight Height of the speed and zoom buttons in the toolbar.
 */
data class AudioPlayerDimensions(
    val graphHeight: Dp = 48.dp,
    val verticalItemSpacing: Dp = 8.dp,
    val markerFontSize: TextUnit = 12.sp,
    val toolbarPadding: Dp = 16.dp,
    val toolbarItemSpacing: Dp = 16.dp,
    val toolbarLabelSpacing: Dp = 8.dp,
    val playButtonSize: Dp = 48.dp,
    val iconSize: Dp = 24.dp,
    val controlButtonHeight: Dp = 48.dp,
)

/** Composition local that provides [AudioPlayerDimensions] to the player subtree. */
val LocalAudioPlayerDimensions = compositionLocalOf { AudioPlayerDimensions() }

/**
 * Provides [dimensions] to all player composables in [content] via [LocalAudioPlayerDimensions].
 *
 * [rememberAudioPlayerState] must be called inside this block so the waveform layout is computed
 * with the same spacing and font size that the UI will use for drawing.
 *
 * ```kotlin
 * ProvidePlayerDimensions(compactPlayerDimensions()) {
 *     val state = rememberAudioPlayerState(amplitudes, durationMs)
 *     WaveformAudioPlayerUi(state = state, ...)
 * }
 * ```
 */
@Composable
fun ProvidePlayerDimensions(
    dimensions: AudioPlayerDimensions,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAudioPlayerDimensions provides dimensions) {
        content()
    }
}

/**
 * A preset of [AudioPlayerDimensions] optimised for compact layouts.
 *
 * Compared to the defaults: graph height is halved, spacing and padding are halved, buttons are
 * smaller (36 dp), and the marker font is reduced to 9 sp.
 */
fun compactPlayerDimensions() = AudioPlayerDimensions(
    graphHeight = 18.dp,
    verticalItemSpacing = 2.dp,
    markerFontSize = 8.sp,
    toolbarPadding = 4.dp,
    toolbarItemSpacing = 12.dp,
    toolbarLabelSpacing = 2.dp,
    playButtonSize = 24.dp,
    iconSize = 18.dp,
    controlButtonHeight = 32.dp,
)