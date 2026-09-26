package com.qvacell.app.ui.components

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.qvacell.app.model.CubanPhoneNumber
import com.qvacell.app.service.DialService
import com.qvacell.app.service.TransferPinStore

/**
 * "Transferir Saldo" bottom sheet — used by Home's Transferir button and by
 * `ContactOptionsSheet`'s "Hacer Transferencia", with the number pre-filled and locked in the
 * contact case. Replaces the old full-screen `TransferFlowScreen`.
 */
@Composable
fun TransferBottomSheet(
    fixedNumber: String? = null,
    onDismiss: () -> Unit,
    onDone: () -> Unit = onDismiss
) {
    val context = LocalContext.current
    val pinStore = remember { TransferPinStore(context) }
    var number by remember { mutableStateOf(fixedNumber ?: "") }
    var pin by remember { mutableStateOf(pinStore.load() ?: "") }
    var amount by remember { mutableStateOf("") }

    val pickContactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex >= 0) {
                    CubanPhoneNumber.normalize(cursor.getString(numberIndex))?.let { number = it }
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Transferir Saldo", style = MaterialTheme.typography.titleLarge)
            RoundedTextField(
                value = number,
                onValueChange = { number = it },
                label = "Número (+53 ...)",
                keyboardType = KeyboardType.Phone,
                enabled = fixedNumber == null,
                trailingIcon = if (fixedNumber == null) {
                    {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                            pickContactLauncher.launch(intent)
                        }) {
                            Icon(Icons.Filled.Person, contentDescription = "Elegir de contactos")
                        }
                    }
                } else {
                    null
                }
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = "Clave",
                    keyboardType = KeyboardType.NumberPassword,
                    modifier = Modifier.weight(1f)
                )
                RoundedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Monto",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        DialogActionRow(
            cancelText = "Cancelar",
            confirmText = "Transferir",
            onCancel = onDismiss,
            onConfirm = {
                pinStore.save(pin)
                DialService.dial(context, "*234*1*$number*$pin*$amount#")
                onDone()
            },
            confirmEnabled = number.isNotBlank() && pin.isNotBlank() && amount.isNotBlank()
        )
    }
}
