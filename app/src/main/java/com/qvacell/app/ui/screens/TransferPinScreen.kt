package com.qvacell.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qvacell.app.service.TransferPinStore

@Composable
fun TransferPinScreen() {
    val context = LocalContext.current
    val store = remember { TransferPinStore(context) }
    var pin by remember { mutableStateOf(store.load() ?: "") }
    var saved by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Clave de Transferencia") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it; saved = null },
                label = { Text("Clave") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = { store.save(pin); saved = "Guardada" }, modifier = Modifier.fillMaxWidth()) {
                Text("Guardar")
            }
            OutlinedButton(
                onClick = { store.delete(); pin = ""; saved = "Eliminada" },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Eliminar") }
            saved?.let { Text(it) }
        }
    }
}
