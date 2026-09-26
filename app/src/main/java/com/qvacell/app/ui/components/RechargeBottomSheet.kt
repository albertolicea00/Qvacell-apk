package com.qvacell.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.qvacell.app.service.DialService

/** "Recargar con Tarjeta" bottom sheet — replaces the old full-screen `RechargeFlowScreen`. */
@Composable
fun RechargeBottomSheet(onDismiss: () -> Unit, onDone: () -> Unit = onDismiss) {
    val context = LocalContext.current
    var cardNumber by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Recargar con Tarjeta", style = MaterialTheme.typography.titleLarge)
            RoundedTextField(value = cardNumber, onValueChange = { cardNumber = it }, label = "Número de tarjeta")
        }
        DialogActionRow(
            cancelText = "Cancelar",
            confirmText = "Recargar",
            onCancel = onDismiss,
            onConfirm = {
                if (cardNumber.isNotBlank()) {
                    DialService.dial(context, "*662*$cardNumber#")
                    onDone()
                }
            },
            confirmEnabled = cardNumber.isNotBlank()
        )
    }
}
