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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qvacell.app.service.DialService

/** Recharges with a scratch-card number by dialing *662*{input}# directly; never persisted. */
@Composable
fun RechargeFlowScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var cardNumber by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Recargar con Tarjeta") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { cardNumber = it },
                label = { Text("Número de tarjeta") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (cardNumber.isNotBlank()) {
                        DialService.dial(context, "*662*$cardNumber#")
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Recargar")
            }
        }
    }
}
