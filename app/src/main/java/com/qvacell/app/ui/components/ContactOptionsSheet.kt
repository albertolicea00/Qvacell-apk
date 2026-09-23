package com.qvacell.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.GroupRemove
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qvacell.app.service.DeviceContact
import com.qvacell.app.service.DialService
import com.qvacell.app.service.TransferPinStore

/**
 * Bottom sheet shown when a contact row is tapped — call the contact (collect via `*99` or
 * hidden caller ID via `#31#`), transfer balance to it, or add/remove it from the Plan de
 * Amigos. Mirrors iOS's `ContactCallOptionsSheet`.
 */
@Composable
fun ContactOptionsSheet(contact: DeviceContact, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val number = contact.cubanNumbers.firstOrNull()
    var showTransferDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(14.dp).size(28.dp)
                )
            }
            Text(contact.name, style = MaterialTheme.typography.titleMedium)
            if (number != null) {
                Text(number, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (number != null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        DialService.dial(context, "*99$number")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Llamar con *99", modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(
                    onClick = {
                        DialService.dial(context, "#31#$number")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Llamar Anónimo", modifier = Modifier.padding(start = 8.dp))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                TextButton(
                    onClick = { showTransferDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Hacer Transferencia", modifier = Modifier.padding(start = 8.dp))
                }
                TextButton(
                    onClick = {
                        DialService.dial(context, "*133*4*2*1*$number#")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Agregar a mi Plan de Amigos", modifier = Modifier.padding(start = 8.dp))
                }
                TextButton(
                    onClick = {
                        DialService.dial(context, "*133*4*2*2*$number#")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.GroupRemove, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Eliminar de mi Plan de Amigos", modifier = Modifier.padding(start = 8.dp))
                }
            }
        } else {
            Text(
                "Este contacto no tiene un número en formato cubano.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    if (showTransferDialog && number != null) {
        TransferToNumberDialog(
            number = number,
            onDismiss = { showTransferDialog = false },
            onDone = {
                showTransferDialog = false
                onDismiss()
            }
        )
    }
}

/** The Clave/Monto transfer form, prefilled with a fixed number — reused by [ContactOptionsSheet]. */
@Composable
private fun TransferToNumberDialog(number: String, onDismiss: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val pinStore = remember { TransferPinStore(context) }
    var pin by remember { mutableStateOf(pinStore.load() ?: "") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transferir a $number") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("Clave") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    pinStore.save(pin)
                    DialService.dial(context, "*234*1*$number*$pin*$amount#")
                    onDone()
                },
                enabled = pin.isNotBlank() && amount.isNotBlank()
            ) { Text("Transferir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
