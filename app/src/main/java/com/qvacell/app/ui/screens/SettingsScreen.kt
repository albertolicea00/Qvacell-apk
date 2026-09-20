package com.qvacell.app.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qvacell.app.BuildConfig
import kotlinx.coroutines.launch

sealed class SettingsDestination {
    data object Reminders : SettingsDestination()
    data object SmsServices : SettingsDestination()
    data object WifiRooms : SettingsDestination()
    data object DirectorySearch : SettingsDestination()
    data object TransferPin : SettingsDestination()
    data object Help : SettingsDestination()
}

@Composable
fun SettingsScreen(onNavigate: (SettingsDestination) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var versionTapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var debugDbSearchVisible by remember { mutableStateOf(false) }

    fun onVersionTap() {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > 3000) versionTapCount = 0
        lastTapTime = now
        versionTapCount++
        if (versionTapCount >= 5) {
            debugDbSearchVisible = true
            versionTapCount = 0
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Ajustes") }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item { SectionHeader("Preferencias") }
            item {
                ListItem(
                    headlineContent = { Text("Tema") },
                    supportingContent = { Text("Sistema, claro u oscuro") }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Pestaña predeterminada") },
                    supportingContent = { Text("Inicio") }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Color de acento") }
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader("Utilidades") }
            item {
                ListItem(
                    headlineContent = { Text("Recordatorios") },
                    modifier = Modifier.clickable { onNavigate(SettingsDestination.Reminders) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Servicios por SMS") },
                    modifier = Modifier.clickable { onNavigate(SettingsDestination.SmsServices) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Salas de Navegación WiFi") },
                    modifier = Modifier.clickable { onNavigate(SettingsDestination.WifiRooms) }
                )
            }
            if (debugDbSearchVisible) {
                item {
                    ListItem(
                        headlineContent = { Text("Búsqueda en Base de Datos") },
                        supportingContent = { Text("Función de depuración") },
                        modifier = Modifier.clickable { onNavigate(SettingsDestination.DirectorySearch) }
                    )
                }
            }
            item { HorizontalDivider() }

            item { SectionHeader("Cuenta") }
            item {
                ListItem(
                    headlineContent = { Text("Clave de Transferencia") },
                    modifier = Modifier.clickable { onNavigate(SettingsDestination.TransferPin) }
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader("Acerca de") }
            item {
                ListItem(
                    headlineContent = { Text("Identificador de Llamadas") },
                    supportingContent = { Text("Solicitar rol de selección de llamadas") },
                    modifier = Modifier.clickable { requestCallScreeningRole(context) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Ayuda") },
                    modifier = Modifier.clickable { onNavigate(SettingsDestination.Help) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Código fuente en GitHub") }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Versión") },
                    supportingContent = { Text(BuildConfig.VERSION_NAME) },
                    modifier = Modifier.clickable { onVersionTap() }
                )
            }
        }
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

private fun requestCallScreeningRole(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
