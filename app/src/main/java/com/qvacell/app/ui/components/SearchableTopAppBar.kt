package com.qvacell.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction

/**
 * TopAppBar that shows a title with a search icon action by default, and swaps
 * the title for an inline search field (with a close action) when tapped —
 * the standard Android search-in-app-bar pattern instead of an always-visible field.
 */
@Composable
fun SearchableTopAppBar(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    searching: Boolean,
    onSearchingChange: (Boolean) -> Unit,
    placeholder: String = "Buscar",
    showSearchAction: Boolean = true
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(searching) {
        if (searching) focusRequester.requestFocus()
    }

    TopAppBar(
        title = {
            if (searching) {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { Text(placeholder) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            } else {
                Text(title)
            }
        },
        actions = {
            if (showSearchAction || searching) {
                IconButton(onClick = {
                    if (searching) {
                        onQueryChange("")
                        onSearchingChange(false)
                    } else {
                        onSearchingChange(true)
                    }
                }) {
                    Icon(
                        imageVector = if (searching) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = if (searching) "Cerrar búsqueda" else "Buscar",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    )
}
