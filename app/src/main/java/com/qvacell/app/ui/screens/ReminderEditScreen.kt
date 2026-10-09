package com.qvacell.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.qvacell.app.model.Reminder
import com.qvacell.app.model.ReminderRecurrence
import com.qvacell.app.model.ReminderTemplate
import com.qvacell.app.service.ReminderRepository
import com.qvacell.app.service.ReminderScheduler
import com.qvacell.app.ui.components.BackNavigationIcon
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditScreen(
    templateKey: String? = null,
    reminderId: String? = null,
    onDone: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val repository = remember { ReminderRepository(context) }
    val scheduler = remember { ReminderScheduler(context) }
    val scope = rememberCoroutineScope()

    val template = remember { ReminderTemplate.forKey(templateKey) }
    val isEditing = reminderId != null

    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var recurrence by remember { mutableStateOf(template.defaultRecurrence) }
    var customDays by remember { mutableIntStateOf(30) }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 3600_000L) }
    var loaded by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(reminderId) {
        if (reminderId != null) {
            repository.getById(reminderId)?.let { r ->
                title = r.title
                message = r.message
                phoneNumber = r.phoneNumber
                recurrence = r.recurrence
                customDays = r.customIntervalDays
                dateMillis = r.date
            }
        } else {
            title = template.title
            message = template.message
        }
        loaded = true
    }

    if (!loaded) return

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 12.dp),
                title = { Text(if (isEditing) "Editar Recordatorio" else template.title) },
                navigationIcon = { if (onBack != null) BackNavigationIcon(onBack) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recordatorio section
            Text(
                "RECORDATORIO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Mensaje") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Phone number section (only for transfer template)
            if (template.needsPhoneNumber) {
                Text(
                    "NÚMERO DE TELÉFONO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Ej: 51234567") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Text(
                            "Se usará como destino al ejecutar la transferencia.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Cuándo section
            Text(
                "CUÁNDO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dateFormatter.format(Date(dateMillis)),
                            onValueChange = {},
                            label = { Text("Fecha") },
                            readOnly = true,
                            modifier = Modifier
                                .weight(1f),
                            singleLine = true,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also {
                                LaunchedEffect(it) {
                                    it.interactions.collect { interaction ->
                                        if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                            showDatePicker = true
                                        }
                                    }
                                }
                            }
                        )
                        OutlinedTextField(
                            value = timeFormatter.format(Date(dateMillis)),
                            onValueChange = {},
                            label = { Text("Hora") },
                            readOnly = true,
                            modifier = Modifier
                                .weight(1f),
                            singleLine = true,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also {
                                LaunchedEffect(it) {
                                    it.interactions.collect { interaction ->
                                        if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                            showTimePicker = true
                                        }
                                    }
                                }
                            }
                        )
                    }

                    Text(
                        "Repetir",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ReminderRecurrence.entries.forEach { option ->
                            FilterChip(
                                selected = recurrence == option,
                                onClick = { recurrence = option },
                                label = { Text(option.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    if (recurrence == ReminderRecurrence.CUSTOM) {
                        OutlinedTextField(
                            value = customDays.toString(),
                            onValueChange = { customDays = it.toIntOrNull()?.coerceIn(2, 365) ?: 30 },
                            label = { Text("Cada cuántos días") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val reminder = Reminder(
                        id = reminderId ?: UUID.randomUUID().toString(),
                        title = title.ifBlank { template.title },
                        message = message,
                        iconName = template.iconName,
                        ussdCodeId = template.ussdCodeId,
                        phoneNumber = phoneNumber,
                        date = dateMillis,
                        recurrence = recurrence,
                        customIntervalDays = customDays,
                        isEnabled = true,
                        templateKey = if (template.key == "custom") null else template.key
                    )
                    scope.launch {
                        repository.save(reminder)
                        scheduler.schedule(reminder)
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank()
            ) {
                Text("Guardar")
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate ->
                        val old = Calendar.getInstance().apply { timeInMillis = dateMillis }
                        val new = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            set(Calendar.HOUR_OF_DAY, old.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, old.get(Calendar.MINUTE))
                        }
                        dateMillis = new.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE)
        )

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Seleccionar hora") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val updated = Calendar.getInstance().apply {
                        timeInMillis = dateMillis
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                    }
                    dateMillis = updated.timeInMillis
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
            }
        )
    }
}
