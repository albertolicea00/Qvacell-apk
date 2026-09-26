package com.qvacell.app.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Vertical A-Z rail, like native Android Contacts' fast-scroll index — tap or drag along it to
 * jump straight to a letter's section instead of scrolling through the whole list.
 */
@Composable
fun AlphabetIndexBar(
    letters: List<String>,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (letters.isEmpty()) return

    fun letterAt(y: Float, height: Float): String {
        val index = (y / height * letters.size).toInt().coerceIn(0, letters.size - 1)
        return letters[index]
    }

    Column(
        modifier = modifier
            .width(24.dp)
            .padding(vertical = 4.dp)
            .pointerInput(letters) {
                detectTapGestures { offset -> onLetterSelected(letterAt(offset.y, size.height.toFloat())) }
            }
            .pointerInput(letters) {
                detectDragGestures { change, _ ->
                    onLetterSelected(letterAt(change.position.y, size.height.toFloat()))
                }
            }
    ) {
        letters.forEach { letter ->
            Text(
                letter,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
