package com.qvacell.app.ui.screens

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qvacell.app.model.CubanPhoneNumber
import com.qvacell.app.model.Reminder
import com.qvacell.app.model.ReminderRecurrence
import com.qvacell.app.model.ReminderTemplate
import com.qvacell.app.service.ReminderRepository
import com.qvacell.app.service.ReminderScheduler
import com.qvacell.app.ui.components.DialogActionRow
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
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val pickContactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (idx >= 0) {
                    CubanPhoneNumber.normalize(cursor.getString(idx))?.let { phoneNumber = it }
                }
            }
        }
    }

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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onBack?.invoke() ?: onDone() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = when {
                        isEditing -> "Editar Recordatorio"
                        template.title.isNotBlank() -> template.title
                        else -> "Nuevo Recordatorio"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 4.dp)
                )

            // Recordatorio section
            Column {
                SectionHeader("Recordatorio")
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column {
                        EditorInputField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = "Título",
                            singleLine = true
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        EditorInputField(
                            value = message,
                            onValueChange = { message = it },
                            placeholder = "Mensaje",
                            singleLine = false,
                            minHeight = 72.dp
                        )
                    }
                }
            }

            // Phone number section (only for transfer template)
            if (template.needsPhoneNumber) {
                Column {
                    SectionHeader("Número de Teléfono")
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = phoneNumber,
                                    onValueChange = { phoneNumber = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 16.dp),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    decorationBox = { inner ->
                                        if (phoneNumber.isEmpty()) {
                                            Text(
                                                "Número (+53 ...)",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                        inner()
                                    }
                                )
                                IconButton(
                                    onClick = {
                                        pickContactLauncher.launch(
                                            Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                        )
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
                    Text(
                        "Se usará como destino al ejecutar la transferencia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }

            // Cuándo section
            Column {
                SectionHeader("Cuándo")
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                PickerRowItem(
                                    label = "Fecha",
                                    value = dateFormatter.format(Date(dateMillis)),
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                            VerticalDivider(
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                PickerRowItem(
                                    label = "Hora",
                                    value = timeFormatter.format(Date(dateMillis)),
                                    onClick = { showTimePicker = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }

                        var recurrenceExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { recurrenceExpanded = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Repetir",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        recurrence.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = recurrenceExpanded,
                                onDismissRequest = { recurrenceExpanded = false }
                            ) {
                                ReminderRecurrence.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            recurrence = option
                                            recurrenceExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (recurrence == ReminderRecurrence.CUSTOM) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Cada cuántos días",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                BasicTextField(
                                    value = customDays.toString(),
                                    onValueChange = { customDays = it.toIntOrNull()?.coerceIn(2, 365) ?: 30 },
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(52.dp)
                                        .wrapContentHeight(Alignment.CenterVertically),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }
        }

            if (isEditing) {
                DialogActionRow(
                    cancelText = "Eliminar",
                    confirmText = "Guardar",
                    onCancel = { showDeleteConfirm = true },
                    onConfirm = {
                        val reminder = Reminder(
                            id = reminderId,
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
                    confirmEnabled = title.isNotBlank(),
                    cancelColor = MaterialTheme.colorScheme.error
                )
            } else {
                DialogActionRow(
                    cancelText = "Cancelar",
                    confirmText = "Guardar",
                    onCancel = { onBack?.invoke() ?: onDone() },
                    onConfirm = {
                        val reminder = Reminder(
                            id = UUID.randomUUID().toString(),
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
                    confirmEnabled = title.isNotBlank()
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm && reminderId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar recordatorio?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        scheduler.cancel(reminderId)
                        repository.getById(reminderId)?.let { repository.delete(it) }
                        showDeleteConfirm = false
                        onDone()
                    }
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
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

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun EditorInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minHeight: Dp = 52.dp
) {
    val accentColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .padding(horizontal = 16.dp, vertical = if (singleLine) 0.dp else 12.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (singleLine) Modifier.height(minHeight).wrapContentHeight(Alignment.CenterVertically) else Modifier),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(accentColor),
            singleLine = singleLine,
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                inner()
            }
        )
    }
}

@Composable
private fun PickerRowItem(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

