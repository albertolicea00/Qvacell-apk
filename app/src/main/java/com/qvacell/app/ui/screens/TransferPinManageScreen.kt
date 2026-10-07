package com.qvacell.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.service.DialService
import com.qvacell.app.service.TransferPinStore
import com.qvacell.app.ui.components.BackNavigationIcon

/**
 * Options › Cuenta › Gestionar PIN de Transferencia — "Cambiar Clave" dials ETECSA's PIN-change
 * code and updates the saved PIN to match; "Guardar Clave" just persists a PIN locally so
 * Transferir can prefill it — ported from iOS's TransferPinSettingsView.
 */
@Composable
fun TransferPinManageScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val pinStore = remember { TransferPinStore(context) }

    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }

    var savedPin by remember { mutableStateOf(pinStore.load() ?: "") }
    var isSavedPinPersisted by remember { mutableStateOf(pinStore.load() != null) }

    val showsSamePinError = currentPin.isNotEmpty() && newPin.isNotEmpty() && newPin == currentPin
    val isChangeDisabled = currentPin.isBlank() || newPin.isBlank() || newPin == currentPin

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 12.dp),
                title = { Text("PIN de Transferencia") },
                navigationIcon = { if (onBack != null) BackNavigationIcon(onBack) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(vertical = 8.dp)) {
            SectionHeader("Cambiar Clave")
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column {
                    PinFieldRow(
                        value = currentPin,
                        onValueChange = { currentPin = it },
                        placeholder = "Clave actual"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    PinFieldRow(
                        value = newPin,
                        onValueChange = { newPin = it },
                        placeholder = "Clave nueva"
                    )
                    if (showsSamePinError) {
                        Text(
                            "La clave nueva es igual a la actual.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SubmitRow(
                        label = "Cambiar Clave",
                        enabled = !isChangeDisabled,
                        onClick = {
                            val code = repository.findCodeById("transfer-pin-change")
                            val resolved = code?.code
                                ?.replace("{current}", currentPin)
                                ?.replace("{new}", newPin)
                            if (resolved != null) {
                                DialService.dialDirect(context, resolved)
                                pinStore.save(newPin)
                                savedPin = newPin
                                isSavedPinPersisted = true
                                currentPin = ""
                                newPin = ""
                            }
                        }
                    )
                }
            }

            SectionHeader("Guardar Clave")
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column {
                    PinFieldRow(
                        value = savedPin,
                        onValueChange = { savedPin = it },
                        placeholder = "Clave"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SubmitRow(
                        label = "Guardar Clave",
                        enabled = savedPin.isNotBlank(),
                        onClick = {
                            pinStore.save(savedPin)
                            isSavedPinPersisted = true
                        }
                    )
                    if (isSavedPinPersisted) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pinStore.delete()
                                    savedPin = ""
                                    isSavedPinPersisted = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                "Olvidar Clave Guardada",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            Text(
                "Se guarda cifrada en este dispositivo (nunca sale de él) y se rellena sola al " +
                    "transferir, tanto en Home como dentro de un contacto.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp)
            )
        }
    }
}

@Composable
private fun PinFieldRow(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val accentColor = MaterialTheme.colorScheme.primary
    var visible by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(accentColor),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                inner()
            }
        )
        IconButton(onClick = { visible = !visible }) {
            Icon(
                if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (visible) "Ocultar" else "Mostrar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun SubmitRow(label: String, enabled: Boolean, onClick: () -> Unit) {
    val tint = if (enabled) MaterialTheme.colorScheme.primary
               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = tint)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
