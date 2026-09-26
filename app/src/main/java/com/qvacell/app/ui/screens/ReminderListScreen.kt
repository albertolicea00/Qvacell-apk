package com.qvacell.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.model.Reminder
import com.qvacell.app.service.ReminderRepository
import com.qvacell.app.service.ReminderScheduler
import com.qvacell.app.ui.components.BackNavigationIcon
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReminderListScreen(onAdd: () -> Unit, onEdit: (Reminder) -> Unit, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val repository = remember { ReminderRepository(context) }
    val scheduler = remember { ReminderScheduler(context) }
    val scope = rememberCoroutineScope()
    val reminders by repository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recordatorios") },
                navigationIcon = { if (onBack != null) BackNavigationIcon(onBack) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Agregar") }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (reminders.isEmpty()) {
                Text(
                    "No tienes recordatorios.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn {
                    items(reminders, key = { it.id }) { reminder ->
                        ListItem(
                            headlineContent = { Text(reminder.title) },
                            supportingContent = { Text(formatter.format(Date(reminder.date))) },
                            trailingContent = {
                                Switch(
                                    checked = reminder.isEnabled,
                                    onCheckedChange = { enabled ->
                                        scope.launch {
                                            repository.setEnabled(reminder, enabled)
                                            if (enabled) scheduler.schedule(reminder.copy(isEnabled = true))
                                            else scheduler.cancel(reminder.id)
                                        }
                                    }
                                )
                            modifier = Modifier
                                .clickable { onEdit(reminder) }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
