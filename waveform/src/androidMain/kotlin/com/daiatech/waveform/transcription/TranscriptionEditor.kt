package com.daiatech.waveform.transcription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TagInfo(
    val startIndex: Int,
    val endIndex: Int,
    val content: String,
    val displayText: String
)

@Composable
fun TranscriptionEditor(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    tags: List<Pair<Int, String>>
) {
    // Convert tags to structured format
    val tagInfos = remember(tags, value) {
        processTagsToStructure(tags, value)
    }

    // Create display text with placeholders for chips
    val (displayText, textFieldValue) = remember(value, tagInfos) {
        createDisplayText(value, tagInfos)
    }

    var textFieldState by remember(textFieldValue) {
        mutableStateOf(textFieldValue)
    }

    // Custom text selection colors
    val customTextSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colorScheme.primary,
        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = textFieldState,
            onValueChange = { newValue ->
                // Handle text changes while preserving tags
                val adjustedValue = handleTextChange(newValue, textFieldState, tagInfos, value)
                textFieldState = newValue
                onValueChange(adjustedValue)
            },
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp)
                    ) {
                        if (textFieldState.text.isEmpty()) {
                            Text(
                                text = "Start typing...",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }

                        // Custom text display with chips
                        DisplayTextWithChips(
                            displayText = displayText,
                            tagInfos = tagInfos,
                            textFieldValue = textFieldState
                        )

                        // Invisible text field for cursor and selection
                        Box(modifier = Modifier.fillMaxWidth().alpha(0F)) {
                            innerTextField()
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun DisplayTextWithChips(
    displayText: AnnotatedString,
    tagInfos: List<TagInfo>,
    textFieldValue: TextFieldValue
) {
    // This is a simplified approach - in a full implementation,
    // you'd need to create a custom layout that properly positions
    // chips within the text flow
    Text(
        text = displayText,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun TagChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = when {
            text.startsWith("<") -> MaterialTheme.colorScheme.primaryContainer
            text.startsWith("[") -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.tertiaryContainer
        }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = when {
                text.startsWith("<") -> MaterialTheme.colorScheme.onPrimaryContainer
                text.startsWith("[") -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onTertiaryContainer
            }
        )
    }
}

private fun processTagsToStructure(tags: List<Pair<Int, String>>, text: String): List<TagInfo> {
    return tags.mapIndexed { index, (position, content) ->
        TagInfo(
            startIndex = position,
            endIndex = position,
            content = content,
            displayText = content
        )
    }.sortedBy { it.startIndex }
}

private fun createDisplayText(
    originalText: String,
    tagInfos: List<TagInfo>
): Pair<AnnotatedString, TextFieldValue> {
    val annotatedString = buildAnnotatedString {
        var currentIndex = 0

        tagInfos.forEach { tag ->
            // Add text before tag
            if (tag.startIndex > currentIndex) {
                append(originalText.substring(currentIndex, tag.startIndex))
            }

            // Add tag as styled text (placeholder for chip)
            pushStyle(
                SpanStyle(
                    background = when {
                        tag.content.startsWith("<") -> Color(0xFF6366F1).copy(alpha = 0.2f)
                        tag.content.startsWith("[") -> Color(0xFF10B981).copy(alpha = 0.2f)
                        else -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                    },
                    fontWeight = FontWeight.Medium
                )
            )
            append(" ${tag.displayText} ")
            pop()

            currentIndex = tag.startIndex
        }

        // Add remaining text
        if (currentIndex < originalText.length) {
            append(originalText.substring(currentIndex))
        }
    }

    return annotatedString to TextFieldValue(
        text = annotatedString.text,
        selection = TextRange(annotatedString.text.length)
    )
}

private fun handleTextChange(
    newValue: TextFieldValue,
    oldValue: TextFieldValue,
    tagInfos: List<TagInfo>,
    originalText: String
): String {
    // This is a simplified implementation
    // In a full implementation, you'd need to:
    // 1. Detect which part of the text changed
    // 2. Adjust tag positions accordingly
    // 3. Prevent editing within tag regions
    // 4. Return the updated text with tags intact

    // For now, return a basic implementation
    return newValue.text.filter { it != '\uFEFF' } // Remove any placeholder characters
}

@Preview(showBackground = true)
@Composable
private fun TranscriptionEditorPreview() {
    MaterialTheme {
        Surface {
            var text by remember { mutableStateOf("My name is Divyansh. This is a tag") }
            val tags: List<Pair<Int, String>> by remember {
                mutableStateOf(
                    listOf(
                        10 to "<NOUN>",
                        18 to "</NOUN>",
                        30 to "[PUN]"
                    )
                )
            }

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Transcription Editor",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TranscriptionEditor(
                    value = text,
                    onValueChange = {
                        text = it
                        // readjust tags indices
                    },
                    tags = tags
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Debug info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Debug Info:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Original: $text",
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Tags: $tags",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// Additional composable for standalone chip preview
@Preview
@Composable
private fun TagChipPreview() {
    MaterialTheme {
        Surface {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TagChip(text = "<NOUN>")
                TagChip(text = "</NOUN>")
                TagChip(text = "[PUN]")
                TagChip(text = "baby talking")
            }
        }
    }
}