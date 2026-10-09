package com.qvacell.app.ui.components

import android.content.Intent
import android.provider.ContactsContract
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.qvacell.app.model.CubanPhoneNumber
import com.qvacell.app.service.DialService
import com.qvacell.app.service.TransferPinStore

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
    var pinVisible by remember { mutableStateOf(false) }
    var showNonCubanError by remember { mutableStateOf(false) }

    val pickContactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (idx >= 0) {
                    val raw = cursor.getString(idx)
                    val normalized = CubanPhoneNumber.normalize(raw)
                    if (normalized != null) number = normalized else showNonCubanError = true
                }
            }
        }
    }

    val transferEnabled = number.isNotBlank() && pin.isNotBlank() && amount.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                "Transferir Saldo",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Number field (hidden when contact is pre-filled)
            if (fixedNumber == null) {
                Text(
                    "Destinatario",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = number,
                            onValueChange = { number = it },
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (number.isEmpty()) Text("Número (+53 ...)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                inner()
                            }
                        )
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                pickContactLauncher.launch(intent)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "Elegir de contactos",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }

            // PIN + Amount card
            Text(
                "Clave y Monto",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // PIN field with eye toggle
                        Row(
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = pin,
                                onValueChange = { pin = it },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (pin.isEmpty()) Text("Clave", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    inner()
                                }
                            )
                            IconButton(onClick = { pinVisible = !pinVisible }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    if (pinVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Box(Modifier.width(1.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        // Amount field
                        BasicTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (amount.isEmpty()) Text("Monto", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                inner()
                            }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    val tint = if (transferEnabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = transferEnabled) {
                                pinStore.save(pin)
                                DialService.dial(context, "*234*1*$number*$pin*$amount#")
                                onDone()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Transferir", style = MaterialTheme.typography.bodyMedium, color = tint)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    if (showNonCubanError) {
        AlertDialog(
            onDismissRequest = { showNonCubanError = false },
            title = { Text("Número no válido") },
            text = { Text("El número seleccionado no es un número cubano (+53). Solo se puede transferir saldo a números cubanos.") },
            confirmButton = {
                TextButton(onClick = { showNonCubanError = false }) { Text("Aceptar") }
            }
        )
    }
}
