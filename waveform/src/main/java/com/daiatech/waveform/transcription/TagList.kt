package com.daiatech.waveform.transcription



import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TagList(
    tags: List<String>,
    onClick: (String) -> Unit,
    colors: List<TagColor>
) {

    fun stripBrackets(word: String): String {
        return word.replace(Regex("^<|>$"), "")
    }

    Box(
        modifier = Modifier
            .heightIn(max = 200.dp)
            .verticalScroll(rememberScrollState())
            .padding(2.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tags.forEachIndexed { index, word ->
                val randomColor = colors[index % colors.size]
                val strippedWord = stripBrackets(word)
                SuggestionChip(
                    onClick = {
                        onClick(word)
                    },
                    label = { Text(text = strippedWord, color = randomColor.contentColor) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = randomColor.containerColor),
                    border = BorderStroke(1.dp, color = randomColor.outlineColor)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TagListPreview() {
    TagList(
        tags = listOf("Kotlin", "Compose", "Android", "Preview", "UI", "Jetpack"),
        onClick = { tag -> println("Clicked on: $tag") },
        colors = openCloseTagColors
    )
}

@Preview(showBackground = true)
@Composable
fun TagListPreview1() {
    TagList(
        tags = listOf("Kotlin", "Compose", "Android", "Preview", "UI", "Jetpack"),
        onClick = { tag -> println("Clicked on: $tag") },
        colors = standaloneTagColors
    )
}
