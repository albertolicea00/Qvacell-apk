package com.qvacell.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Ayuda") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Qvacell", style = MaterialTheme.typography.titleLarge)
            Text(
                "Qvacell es una utilidad no oficial para acceder rápidamente a los códigos USSD, " +
                    "líneas de ayuda y servicios de ETECSA (Cubacel). No requiere conexión a internet " +
                    "ni cuenta con acceso a tus datos personales.",
                modifier = Modifier.padding(top = 8.dp)
            )
            Text("Cómo usarla", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Text(
                "Toca cualquier código para marcarlo o enviarlo por SMS. La app abrirá el marcador o " +
                    "la app de mensajes del sistema — tú confirmas antes de que se envíe nada.",
                modifier = Modifier.padding(top = 8.dp)
            )
            Text("Código fuente", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Text("github.com/qvacell/qvacell-apk", modifier = Modifier.padding(top = 8.dp))
        }
    }
}
