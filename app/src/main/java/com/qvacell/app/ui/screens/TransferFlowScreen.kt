package com.qvacell.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qvacell.app.model.CubanPhoneNumber
import com.qvacell.app.service.DialService
import com.qvacell.app.service.TransferPinStore

@Composable
fun TransferFlowScreen(
    prefilledNumber: String? = null,
    onPickContact: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val pinStore = remember { TransferPinStore(context) }

    var number by remember { mutableStateOf(prefilledNumber ?: "") }
    var pin by remember { mutableStateOf(pinStore.load() ?: "") }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Transferir Saldo") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = number,
                onValueChange = { number = it },
                label = { Text("Número destino") },
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = onPickContact) { Text("Elegir de contactos") }
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text("Clave de transferencia") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Monto") },
                modifier = Modifier.fillMaxWidth()
            )
            error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    val normalized = CubanPhoneNumber.normalize(number)
                    if (normalized == null) {
                        error = "Número inválido"
                        return@Button
                    }
                    if (pin.isBlank() || amount.isBlank()) {
                        error = "Completa todos los campos"
                        return@Button
                    }
                    pinStore.save(pin)
                    val code = "*234*1*$normalized*$pin*$amount#"
                    DialService.dial(context, code)
                    onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Transferir")
            }
        }
    }
}
