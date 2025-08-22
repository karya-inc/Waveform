package com.daiatech.waveform.transcription

import androidx.compose.runtime.Composable


import android.view.ViewTreeObserver
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.daiatech.waveform.R

const val SHORTCUT_CHAR_1 = "{}"

/**
 * A Composable function that renders a user interface for inputting and editing audio transcriptions with validation and tagging capabilities.
 *
 * This composable provides a text field with various features including input validation based on a regular expression, dynamic error messages, tagging options, and customizable buttons.
 *
 * @param modifier Optional [Modifier] to be applied to the column container.
 * @param value The current text value of the input field.
 * @param errorMessage An optional error message to be displayed when the input is invalid.
 * @param openCloseTags A list of tags that support an open/close format (e.g., <tag></tag>).
 * @param standAloneTags A list of tags that stand alone (e.g., [tag]).
 * @param onValueChange A lambda function to be called when the text value changes. It provides the new text value.
 * @param onDone A lambda function to be called when the input is submitted (e.g., when the Done button is pressed).
 * @param negativeResponse The text to display on the negative action button.
 * @param onNegative A lambda function to be called when the negative action button is pressed.
 * @param negativeEnabled A boolean indicating whether the negative action button should be enabled.
 * @param label A composable function for rendering a label for the input field. It will be displayed when the input field is empty.
 */
@Composable
fun TranscriptionTextEditor(
    modifier: Modifier = Modifier,
    value: String,
    errorMessage: String?,
    openCloseTags: List<String>,
    standAloneTags: List<String>,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    negativeResponse: String,
    onNegative: () -> Unit,
    negativeEnabled: Boolean,
    label: @Composable () -> Unit = {},
    hideTagsButton: Boolean,
    hideCurlyTagButton: Boolean,
    specialStandaloneTags: Set<String> = setOf("PAUSE", "UNKNOWN_SEGMENT", "SIL"),
    colors: TranscriptionColors
) {
    var isColumnVisible by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    val borderColor =
        if (errorMessage?.isNotBlank() == true) colors.errorOutlineColor else Color.Transparent

    val selectedTagOptions = remember { mutableStateOf(TagType.STANDALONE) }

    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }

    var isKeyboardVisible by remember { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val insets = ViewCompat.getRootWindowInsets(view)
            isKeyboardVisible = insets?.isVisible(WindowInsetsCompat.Type.ime()) ?: false

            if (isKeyboardVisible) {
                isColumnVisible = false
            }
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(listener)

        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onValueChange(newValue.text)
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardActions = KeyboardActions { onDone() },
            decorationBox = { innerTextField ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.containerColor)
                        .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                ) {
                    Row(
                        Modifier
                            .background(Color.White, smallRoundedCorner)
                            .border(2.dp, borderColor, smallRoundedCorner),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            Modifier
                                .weight(1f)
                                .padding(8.dp)
                        ) {
                            Box {
                                if (textFieldValue.text.isBlank()) {
                                    label()
                                }

                                innerTextField()
                            }
                        }

                        if (!hideCurlyTagButton) {
                            IconButton(onClick = {
                                val currentText = textFieldValue.text
                                val cursorPosition = textFieldValue.selection.start
                                val newText = currentText.substring(
                                    0,
                                    cursorPosition
                                ) + SHORTCUT_CHAR_1 + currentText.substring(cursorPosition)
                                textFieldValue = textFieldValue.copy(
                                    text = newText,
                                    selection = TextRange(cursorPosition + 1)
                                )
                            }) {
                                Text(text = SHORTCUT_CHAR_1, color = Color(0xFF6A6B6B))
                            }
                        }

                        if (!hideTagsButton) {
                            IconButton(onClick = {
                                isColumnVisible = !isColumnVisible

                                if (isColumnVisible) {
                                    keyboardController?.hide()
                                } else {
                                    keyboardController?.show()
                                }
                            }) {
                                Icon(
                                    painter = if (isColumnVisible) {
                                        painterResource(id = R.drawable.ic_tag_filled)
                                    } else {
                                        painterResource(
                                            id = R.drawable.ic_tag
                                        )
                                    },
                                    contentDescription = "Tags",
                                    tint = Color(0xFF6A6B6B)
                                )
                            }
                        }
                    }

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = colors.contentColor,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        )

        if (isColumnVisible) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(colors.containerColor)
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isStandaloneSelected = selectedTagOptions.value == TagType.STANDALONE
                val isOpenCloseSelected = selectedTagOptions.value == TagType.OPEN_CLOSE

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        onClick = {
                            selectedTagOptions.value = TagType.STANDALONE
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isStandaloneSelected) Color(0xFF015964) else Color.Transparent // Change the color as needed
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.standalone_tag),
                            color = colors.contentColor
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(28.dp))

                    Button(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        onClick = {
                            selectedTagOptions.value = TagType.OPEN_CLOSE
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isOpenCloseSelected) Color(0xFF015964) else Color.Transparent // Change the color as needed
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.open_close_tag),
                            color = colors.contentColor
                        )
                    }
                }

                val tagList = when (selectedTagOptions.value) {
                    TagType.STANDALONE -> standAloneTags
                    TagType.OPEN_CLOSE -> openCloseTags
                }


                TagList(
                    tagList,
                    onClick = { selectedTag ->
                        val currentText = textFieldValue.text
                        val cursorPosition = textFieldValue.selection.start

                        val newText: String
                        val newCursorPosition: Int

                        when (selectedTagOptions.value) {
                            TagType.STANDALONE -> {
                                val cleanTag = selectedTag.trim()
                                // Check if the tag is one of the exceptions
                                val formattedTag = if (cleanTag in specialStandaloneTags) {
                                    "<$cleanTag>"
                                } else {
                                    "[$cleanTag]"
                                }

                                // Insert the clean tag wrapped in []
                                newText = currentText.substring(0, cursorPosition) +
                                        formattedTag +
                                        currentText.substring(cursorPosition)

                                newCursorPosition =
                                    cursorPosition + cleanTag.length + 2 // +2 for []
                            }

                            TagType.OPEN_CLOSE -> {
                                newText = currentText.substring(0, cursorPosition) +
                                        "<$selectedTag></$selectedTag>" +
                                        currentText.substring(cursorPosition)

                                newCursorPosition =
                                    cursorPosition + selectedTag.length + 2 // +2 for <>
                            }
                        }

                        textFieldValue = textFieldValue.copy(
                            text = newText,
                            selection = TextRange(newCursorPosition)
                        )

                        onValueChange(textFieldValue.text)
                    },
                    colors = when (selectedTagOptions.value) {
                        TagType.STANDALONE -> colors.standaloneTagsColors
                        TagType.OPEN_CLOSE -> colors.openCloseTagColors
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun TextInputPrev() {
    TranscriptionTextEditor(
        value = "",
        label = {
            Text(text = "Hint")
        },
        errorMessage = null,
        onValueChange = {},
        onDone = { },
        negativeResponse = "Back",
        onNegative = { },
        negativeEnabled = true,
        standAloneTags = listOf(),
        openCloseTags = listOf(),
        hideTagsButton = false,
        hideCurlyTagButton = false,
        colors = transcriptionColors
    )
}

@Preview
@Composable
private fun TextInputPrev2() {
    TranscriptionTextEditor(
        value = "Value",
        label = {
            Text(text = "Hint", color = Color(0xFF494949))
        },
        standAloneTags = listOf("hello", "horn"),
        openCloseTags = listOf(
            "hello",
            "horn",
            "Meow",
            "hello",
            "horn",
            "Meow",
            "hello",
            "horn",
            "Meow"
        ),
        errorMessage = null,
        onValueChange = {},
        onDone = { },
        negativeResponse = "back",
        onNegative = { },
        negativeEnabled = true,
        hideTagsButton = false,
        hideCurlyTagButton = false,
        colors = transcriptionColors
    )
}
