package com.qvacell.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.qvacell.app.ui.components.BackNavigationIcon

@Composable
fun DashboardHelpScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 12.dp),
                title = { Text("Ayuda del Dashboard") },
                navigationIcon = { BackNavigationIcon(onBack) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            DashboardHelpSection("¿Qué es el Dashboard Dinámico?") {
                DashboardHelpInfoRow(
                    title = "Dashboard Dinámico vs Marcación Rápida",
                    text = "El Dashboard Dinámico consulta tu saldo, datos y bonos automáticamente " +
                        "y muestra los resultados en cards actualizables. La Marcación Rápida es el " +
                        "modo clásico: una lista de accesos directos a los códigos USSD sin consulta " +
                        "automática."
                )
            }

            DashboardHelpSection("Cómo funciona") {
                DashboardHelpInfoRow(
                    title = "Consulta de datos",
                    text = "TODO: Explicar cómo se consultan los datos del dashboard dinámico."
                )
                DashboardHelpInfoRow(
                    title = "Actualización",
                    text = "TODO: Explicar cómo se actualizan las cards (pull-to-refresh, botón de navbar)."
                )
                DashboardHelpInfoRow(
                    title = "Interacción con las cards",
                    text = "TODO: Explicar las diferentes acciones de consulta USSD configurables."
                )
            }

            DashboardHelpSection("Configuración") {
                DashboardHelpLinkRow(
                    label = "Abrir Configuración del Dashboard",
                    icon = Icons.Filled.Settings,
                    onClick = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun DashboardHelpSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun DashboardHelpInfoRow(title: String, text: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun DashboardHelpLinkRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
            Text(label, color = MaterialTheme.colorScheme.primary)
        }
        Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider()
}
