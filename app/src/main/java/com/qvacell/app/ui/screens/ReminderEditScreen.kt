package com.qvacell.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qvacell.app.model.Reminder
import com.qvacell.app.model.ReminderRecurrence
import com.qvacell.app.service.ReminderRepository
import com.qvacell.app.service.ReminderScheduler
import com.qvacell.app.ui.components.BackNavigationIcon
import kotlinx.coroutines.launch
import java.util.UUID

private data class ReminderTemplate(val key: String, val label: String, val ussdCodeId: String?)

private val templates = listOf(
    ReminderTemplate("purchase-package", "Comprar Paquete", null),
    ReminderTemplate("transfer-direct", "Hacer Transferencia", "transfer-direct"),
    ReminderTemplate("recharge-card", "Recargar Saldo", "recharge-card"),
    ReminderTemplate("custom", "Personalizado", null)
)

@Composable
fun ReminderEditScreen(onDone: () -> Unit, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val repository = remember { ReminderRepository(context) }
    val scheduler = remember { ReminderScheduler(context) }
    val scope = rememberCoroutineScope()

    var selectedTemplate by remember { mutableStateOf(templates.first()) }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var recurrence by remember { mutableStateOf(ReminderRecurrence.NONE) }
    var customDays by remember { mutableStateOf("1") }
    var daysFromNow by remember { mutableStateOf("1") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Recordatorio") },
                navigationIcon = { if (onBack != null) BackNavigationIcon(onBack) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Plantilla")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(templates) { template ->
                    FilterChip(
                        selected = selectedTemplate == template,
                        onClick = { selectedTemplate = template },
                        label = { Text(template.label) }
                    )
                }
            }
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Mensaje") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = { Text("Número") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = daysFromNow,
                onValueChange = { daysFromNow = it },
                label = { Text("Días a partir de hoy") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Repetición")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderRecurrence.entries.forEach { option ->
                    FilterChip(
                        selected = recurrence == option,
                        onClick = { recurrence = option },
                        label = { Text(option.name) }
                    )
                }
            }
            if (recurrence == ReminderRecurrence.CUSTOM) {
                OutlinedTextField(
                    value = customDays,
                    onValueChange = { customDays = it },
                    label = { Text("Cada cuántos días") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = {
                    val days = daysFromNow.toLongOrNull() ?: 1L
                    val reminder = Reminder(
                        id = UUID.randomUUID().toString(),
                        title = title.ifBlank { selectedTemplate.label },
                        message = message,
                        iconName = "bell.fill",
                        ussdCodeId = selectedTemplate.ussdCodeId,
                        phoneNumber = phoneNumber,
                        date = System.currentTimeMillis() + days * 24 * 60 * 60 * 1000,
                        recurrence = recurrence,
                        customIntervalDays = customDays.toIntOrNull() ?: 1,
                        isEnabled = true,
                        templateKey = selectedTemplate.key
                    )
                    scope.launch {
                        repository.save(reminder)
                        scheduler.schedule(reminder)
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar")
            }
        }
    }
}
