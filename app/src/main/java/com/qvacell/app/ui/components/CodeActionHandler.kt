package com.qvacell.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qvacell.app.model.UssdActionType
import com.qvacell.app.model.UssdCode
import com.qvacell.app.service.DialService

/**
 * Drives the "tap a CodeRow" flow exactly as specified: input prompt when needed,
 * variant/options picker for SMS codes, then either an SMS intent or a dial intent.
 */
@Composable
fun rememberCodeActionHandler(): (UssdCode) -> Unit {
    val context = LocalContext.current
    var activeCode by remember { mutableStateOf<UssdCode?>(null) }
    var inputText by remember { mutableStateOf("") }

    activeCode?.let { code ->
        val hasOptionsOrVariants = !code.options.isNullOrEmpty() || !code.variants.isNullOrEmpty()

        if (code.requiresInput) {
            AlertDialog(
                onDismissRequest = { activeCode = null; inputText = "" },
                title = { Text(code.title.value) },
                text = {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text(code.inputPlaceholder ?: "Valor") },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        performAction(context, code, inputText.ifBlank { null })
                        activeCode = null
                        inputText = ""
                    }) { Text("Aceptar") }
                },
                dismissButton = {
                    TextButton(onClick = { activeCode = null; inputText = "" }) { Text("Cancelar") }
                }
            )
        } else if (hasOptionsOrVariants) {
            val labels = code.variants?.map { it.label.value } ?: code.options.orEmpty()
            AlertDialog(
                onDismissRequest = { activeCode = null },
                title = { Text(code.title.value) },
                text = {
                    Column {
                        labels.forEach { label ->
                            TextButton(onClick = {
                                val body = code.variants?.firstOrNull { it.label.value == label }?.smsBody
                                    ?: code.resolvedSmsBody(label)
                                if (code.type == UssdActionType.SMS) {
                                    DialService.sendSms(context, code.code, body)
                                } else {
                                    DialService.dial(context, code.resolvedCode(label))
                                }
                                activeCode = null
                            }) { Text(label) }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { activeCode = null }) { Text("Cancelar") }
                }
            )
        } else {
            performAction(context, code, null)
            activeCode = null
        }
    }

    return { code -> activeCode = code }
}

private fun performAction(context: android.content.Context, code: UssdCode, input: String?) {
    when (code.type) {
        UssdActionType.SMS -> DialService.sendSms(context, code.code, code.resolvedSmsBody(input))
        UssdActionType.USSD, UssdActionType.CALL -> DialService.dial(context, code.resolvedCode(input))
    }
}
