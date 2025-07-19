package com.daiatech.waveform.app.utils

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import linc.com.amplituda.Amplituda
import linc.com.amplituda.AmplitudaResult
import linc.com.amplituda.Cache
import linc.com.amplituda.Compress


interface AudioManager {
    suspend fun getAmplitudes(
        path: String,
        startMs: Long? = null,
        endMs: Long? = null,
        refresh: Boolean = false,
        scaleFactor: Int = 4000
    ): List<Int>

    suspend fun getDuration(path: String, startMs: Long? = null, endMs: Long? = null): Long
}


class AudioManagerImpl(private val context: Context) : AudioManager {
    private var amplituda: Amplituda? = null

    init {
        try {
            amplituda = Amplituda(context)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unsupported hardware detected. Amplitudes won't be calculated!",
                Toast.LENGTH_SHORT
            ).show()
            // cannot initialize Amplituda, see https://github.com/lincollincol/Amplituda/issues/58
        }
    }

    override suspend fun getAmplitudes(
        path: String,
        startMs: Long?,
        endMs: Long?,
        refresh: Boolean,
        scaleFactor: Int
    ) = withContext(Dispatchers.Default) {
        val cachePolicy: Int = if (refresh) Cache.REFRESH else Cache.REUSE
        var amplitudes = listOf<Int>()
        amplituda?.processAudio(
            path,
            Compress.withParams(Compress.AVERAGE, 40),
            Cache.withParams(cachePolicy)
        )?.get(
            { result ->
                val dur = result.getAudioDuration(AmplitudaResult.DurationUnit.MILLIS)
                val ampls = result.amplitudesAsList().map { it.times(scaleFactor).toInt() }
                val start = startMs ?: 0
                val end = endMs?.let { minOf(dur, it) } ?: dur
                amplitudes = if (dur != 0L) {
                    val startIndex = (ampls.size.toFloat().div(dur)).times(start).toInt()
                    val endIndex = (ampls.size.toFloat().div(dur)).times(end).toInt()
                    ampls.subList(startIndex, endIndex)
                } else {
                    ampls
                }
            },
            { error ->
                error.printStackTrace()
            }
        )
        return@withContext amplitudes
    }

    override suspend fun getDuration(path: String, startMs: Long?, endMs: Long?): Long =
        withContext(Dispatchers.Default) {
            if (startMs != null && endMs != null) {
                return@withContext endMs - startMs
            }

            return@withContext try { // get duration using MediaPlayer
                val mediaPLayer =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        MediaPlayer(context)
                    } else {
                        MediaPlayer()
                    }
                mediaPLayer.setDataSource(path)
                mediaPLayer.prepare()
                val duration = mediaPLayer.duration.toLong()
                mediaPLayer.release()
                duration.coerceAtLeast(0)
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
}

val LocalAudioManager = staticCompositionLocalOf<AudioManager> {
    error("No AudioManager provided")
}
