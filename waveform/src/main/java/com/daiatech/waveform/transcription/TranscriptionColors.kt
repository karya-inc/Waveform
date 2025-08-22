package com.daiatech.waveform.transcription

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class TranscriptionColors(
    val openCloseTagColors: List<TagColor>,
    val standaloneTagsColors: List<TagColor>,
    val contentColor: Color,
    val containerColor: Color,
    val errorOutlineColor: Color,
)

data class TagColor(
    val containerColor: Color,
    val contentColor: Color,
    val outlineColor: Color
)

val openCloseTagColors = listOf(
    TagColor(
        containerColor = Color(0xFF996E41),
        contentColor = Color.White,
        outlineColor = Color(0xFF996E41)
    ),
    TagColor(
        containerColor = Color(0xFF985183),
        contentColor = Color.White,
        outlineColor = Color(0xFF985183)
    ),
    TagColor(
        containerColor = Color(0xFF655289),
        contentColor = Color.White,
        outlineColor = Color(0xFF655289)
    ),
    TagColor(
        containerColor = Color(0xFF985551),
        contentColor = Color.White,
        outlineColor = Color(0xFF985551)
    ),
    TagColor(
        containerColor = Color(0xFF519857),
        contentColor = Color.White,
        outlineColor = Color(0xFF519857)
    ),
    TagColor(
        containerColor = Color(0xFF517998),
        contentColor = Color.White,
        outlineColor = Color(0xFF517998)
    )
)


val standaloneTagColors = listOf(
    TagColor(
        containerColor = Color.Transparent,
        contentColor = Color.Black,
        outlineColor = Color(0xFF996E41)
    ),
    TagColor(
        containerColor = Color.Transparent,
        contentColor = Color.Black,
        outlineColor = Color(0xFF985183)
    ),
    TagColor(
        containerColor = Color.Transparent,
        contentColor = Color.Black,
        outlineColor = Color(0xFF655289)
    ),
    TagColor(
        containerColor = Color.Transparent,
        contentColor = Color.Black,
        outlineColor = Color(0xFF985551)
    ),
    TagColor(
        containerColor = Color.Transparent,
        contentColor = Color.Black,
        outlineColor = Color(0xFF519857)
    ),
    TagColor(
        containerColor = Color.Transparent,
        contentColor = Color.Black,
        outlineColor = Color(0xFF517998)
    )
)


val transcriptionColors = TranscriptionColors(
    contentColor = Color.Black,
    containerColor = Color.Transparent,
    errorOutlineColor = Color.Red,
    openCloseTagColors = openCloseTagColors,
    standaloneTagsColors = openCloseTagColors
)


