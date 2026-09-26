package com.qvacell.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.service.DialService
import com.qvacell.app.service.TransferPinStore
import com.qvacell.app.ui.components.BackNavigationIcon
import com.qvacell.app.ui.components.RoundedTextField

/**
 * Ajustes › Cuenta › Gestionar PIN de Transferencia — "Cambiar Clave" dials ETECSA's PIN-change
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundedTextField(
                        value = currentPin,
                        onValueChange = { currentPin = it },
                        label = "Clave actual",
                        keyboardType = KeyboardType.NumberPassword,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    RoundedTextField(
                        value = newPin,
                        onValueChange = { newPin = it },
                        label = "Clave nueva",
                        keyboardType = KeyboardType.NumberPassword,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (showsSamePinError) {
                        Text(
                            "La clave nueva es igual a la actual.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    SubmitRow(
                        label = "Cambiar Clave",
                        enabled = !isChangeDisabled,
                        onClick = {
                            val code = repository.findCodeById("transfer-pin-change")
                            val resolved = code?.code
                                ?.replace("{current}", currentPin)
                                ?.replace("{new}", newPin)
                            if (resolved != null) {
                                DialService.dial(context, resolved)
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundedTextField(
                        value = savedPin,
                        onValueChange = { savedPin = it },
                        label = "Clave",
                        keyboardType = KeyboardType.NumberPassword,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    SubmitRow(
                        label = "Guardar Clave",
                        enabled = savedPin.isNotBlank(),
                        onClick = {
                            pinStore.save(savedPin)
                            isSavedPinPersisted = true
                        }
                    )
                    if (isSavedPinPersisted) {
                        TextButton(
                            onClick = {
                                pinStore.delete()
                                savedPin = ""
                                isSavedPinPersisted = false
                            }
                        ) { Text("Olvidar Clave Guardada", color = MaterialTheme.colorScheme.error) }
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
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SubmitRow(label: String, enabled: Boolean, onClick: () -> Unit) {
    val tint = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = tint)
        Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = tint, modifier = Modifier.padding(start = 4.dp))
    }
}
