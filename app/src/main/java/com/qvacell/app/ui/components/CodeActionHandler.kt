package com.qvacell.app.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qvacell.app.model.UssdActionType
import com.qvacell.app.model.UssdCode
import com.qvacell.app.service.DialService

/**
 * Drives the "tap a CodeRow" flow: input prompt when needed, variant/options picker for SMS
 * codes, then either an SMS intent or a dial intent. Compras codes ([isPurchaseCategory]) always
 * go through an in-app confirmation sheet and dial directly (ACTION_CALL) on acceptance — Ayuda
 * and everything else always opens the system dialer (ACTION_DIAL) instead.
 */
@Composable
fun rememberCodeActionHandler(
    useNoConfirmCode: Boolean = false,
    isPurchaseCategory: Boolean = false,
    onOpenCodeOptions: ((UssdCode) -> Unit)? = null
): (UssdCode) -> Unit {
    val context = LocalContext.current
    var activeCode by remember { mutableStateOf<UssdCode?>(null) }
    var inputText by remember { mutableStateOf("") }
    var pendingConfirmCode by remember { mutableStateOf<UssdCode?>(null) }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val code = pendingConfirmCode ?: return@rememberLauncherForActivityResult
        // Denied: fall back to the normal dialer rather than silently doing nothing.
        if (granted) DialService.dialDirect(context, resolvedDialCode(code, useNoConfirmCode))
        else DialService.dial(context, resolvedDialCode(code, useNoConfirmCode))
        pendingConfirmCode = null
    }

    var pendingDirectCallCode by remember { mutableStateOf<UssdCode?>(null) }
    val directCallPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val code = pendingDirectCallCode ?: return@rememberLauncherForActivityResult
        if (granted) DialService.dialDirect(context, resolvedDialCode(code, false))
        else DialService.dial(context, resolvedDialCode(code, false))
        pendingDirectCallCode = null
    }

    activeCode?.let { code ->
        val hasOptionsOrVariants = !code.options.isNullOrEmpty() || !code.variants.isNullOrEmpty()

        if (code.requiresInput) {
            val submitInput = {
                performAction(context, code, inputText.ifBlank { null }, useNoConfirmCode = false)
                activeCode = null
                inputText = ""
            }
            ModalBottomSheet(
                onDismissRequest = { activeCode = null; inputText = "" },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        code.title.value,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        code.details.value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    modifier = Modifier.weight(1f),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    decorationBox = { inner ->
                                        if (inputText.isEmpty()) Text(
                                            code.inputPlaceholder ?: "Valor",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                        inner()
                                    }
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            val tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = inputText.isNotBlank(), onClick = submitInput)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Enviar", style = MaterialTheme.typography.bodyMedium, color = tint)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        } else if (hasOptionsOrVariants) {
            // Only reached when the caller didn't supply onOpenCodeOptions — the returned lambda
            // below routes straight there instead of ever setting activeCode in that case.
            val labels = code.variants?.map { it.label.value } ?: code.options.orEmpty()
            ModalBottomSheet(onDismissRequest = { activeCode = null }) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        code.title.value,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    labels.forEach { label ->
                        TextButton(
                            onClick = {
                                dialCodeOption(context, code, label)
                                activeCode = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.ui.graphics.RectangleShape
                        ) { Text(label, modifier = Modifier.fillMaxWidth()) }
                    }
                }
            }
        } else if (isPurchaseCategory && code.type != UssdActionType.SMS) {
            if (useNoConfirmCode) {
                // Compras always confirms in-app before dialing directly — there's no dialer step to
                // catch a mis-tap otherwise.
                pendingConfirmCode = code
                activeCode = null
            } else {
                if (DialService.hasCallPermission(context)) {
                    DialService.dialDirect(context, resolvedDialCode(code, false))
                } else {
                    pendingDirectCallCode = code
                    directCallPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                }
                activeCode = null
            }
        } else {
            performAction(context, code, null, useNoConfirmCode = useNoConfirmCode)
            activeCode = null
        }
    }

    pendingConfirmCode?.let { code ->
        val dialCode = resolvedDialCode(code, useNoConfirmCode)
        ModalBottomSheet(onDismissRequest = { pendingConfirmCode = null }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Confirmar compra",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Se marcará directamente, sin confirmación de respuesta de ETECSA.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (code.price != null) {
                    Text(
                        code.price,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    code.title.value,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            }
            DialogActionRow(
                cancelText = "Cancelar",
                confirmText = "Aceptar",
                onCancel = { pendingConfirmCode = null },
                onConfirm = {
                    if (DialService.hasCallPermission(context)) {
                        DialService.dialDirect(context, dialCode)
                        pendingConfirmCode = null
                    } else {
                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                }
            )
        }
    }

    return { code ->
        val hasOptionsOrVariants = !code.options.isNullOrEmpty() || !code.variants.isNullOrEmpty()
        if (!code.requiresInput && hasOptionsOrVariants && onOpenCodeOptions != null) {
            onOpenCodeOptions(code)
        } else {
            activeCode = code
        }
    }
}

/** The SMS/dial action for one option or variant label — shared with `CodeOptionsSheet`. */
fun dialCodeOption(context: android.content.Context, code: UssdCode, label: String) {
    val body = code.variants?.firstOrNull { it.label.value == label }?.smsBody
        ?: code.resolvedSmsBody(label)
    if (code.type == UssdActionType.SMS) {
        DialService.sendSms(context, code.code, body)
    } else {
        DialService.dial(context, code.resolvedCode(label))
    }
}

/** "Acción sin Confirmación": auto-selects ETECSA's own confirmation step in one dial instead of
 *  stopping there — only for codes that opted in via `noConfirmCode`. */
private fun resolvedDialCode(code: UssdCode, useNoConfirmCode: Boolean): String =
    code.noConfirmCode?.takeIf { useNoConfirmCode } ?: code.resolvedCode(null)

private fun performAction(context: android.content.Context, code: UssdCode, input: String?, useNoConfirmCode: Boolean) {
    when (code.type) {
        UssdActionType.SMS -> DialService.sendSms(context, code.code, code.resolvedSmsBody(input))
        UssdActionType.USSD, UssdActionType.CALL -> {
            val dialCode = code.noConfirmCode?.takeIf { useNoConfirmCode } ?: code.resolvedCode(input)
            DialService.dial(context, dialCode)
        }
    }
}
