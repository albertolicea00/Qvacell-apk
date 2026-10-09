package com.qvacell.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.service.DialService

@Composable
fun RechargeBottomSheet(onDismiss: () -> Unit, onDone: () -> Unit = onDismiss) {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val advanceBalanceCodes = remember {
        listOf("advance-balance-25", "advance-balance-50").mapNotNull { repository.findCodeById(it) }
    }
    var cardNumber by remember { mutableStateOf("") }

    val rechargeEnabled = cardNumber.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Recargar Saldo",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            // Recargar por Llamada — card sola
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DialService.dialDirect(context, "*88#")
                            onDone()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Recargar por Llamada",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Formulario de Recargar con Tarjeta
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = cardNumber,
                            onValueChange = { cardNumber = it },
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (cardNumber.isEmpty()) {
                                    Text(
                                        "Número de tarjeta",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                inner()
                            }
                        )
                        IconButton(onClick = { /* TODO: camera scan */ }, modifier = Modifier.size(40.dp)) {
                            Icon(
                                Icons.Filled.CameraAlt,
                                contentDescription = "Escanear tarjeta",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    val rechargeTint = if (rechargeEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = rechargeEnabled) {
                                DialService.dialDirect(context, "*662*$cardNumber#")
                                onDone()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recargar con Tarjeta", style = MaterialTheme.typography.bodyMedium, color = rechargeTint)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = rechargeTint, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Opciones de Adelanta Saldo debajo de ambos
            if (advanceBalanceCodes.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "ADELANTA SALDO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        advanceBalanceCodes.forEach { code ->
                            OutlinedButton(
                                onClick = {
                                    DialService.dialDirect(context, code.resolvedCode())
                                    onDone()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(code.price ?: code.title.value)
                            }
                        }
                    }
                }
            }
        }
    }
}
