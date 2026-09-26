package com.qvacell.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.qvacell.app.BuildConfig
import com.qvacell.app.ui.components.BackNavigationIcon

private const val REPO_URL = "https://github.com/albertolicea00/Qvacell-apk"
private const val DEVELOPER_URL = "https://github.com/albertolicea00"

@Composable
fun HelpScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayuda") },
                navigationIcon = { if (onBack != null) BackNavigationIcon(onBack) }
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
            HelpSection("Qué es Qvacell") {
                HelpInfoRow(
                    title = "¿Qué hace la app?",
                    text = "Qvacell da acceso rápido a los códigos USSD de servicio de ETECSA " +
                        "(Cubacel): saldo, compras, transferencias y otras utilidades, todo desde " +
                        "una app sin conexión y sin dependencias."
                )
                HelpLinkRow(
                    label = "Código fuente en GitHub",
                    icon = Icons.Filled.Code,
                    onClick = { uriHandler.openUri(REPO_URL) }
                )
            }

            HelpSection("Idioma") {
                HelpInfoRow(
                    title = "¿Cómo cambio el idioma?",
                    text = "Qvacell no tiene un selector de idioma propio — sigue el idioma que " +
                        "elijas para la app en Ajustes de Android. Si tu teléfono está en inglés, " +
                        "la app se muestra en inglés; si está en español (u otro idioma sin " +
                        "traducir), se muestra en español."
                )
                HelpLinkRow(
                    label = "Cambiar Idioma en Ajustes de Android",
                    icon = Icons.Filled.Language,
                    onClick = { openLanguageSettings(context) }
                )
            }

            HelpSection("Cómo Funciona el USSD") {
                HelpInfoRow(
                    title = "¿Qué es el USSD?",
                    text = "El USSD es un protocolo telefónico que te permite interactuar con tu " +
                        "operadora marcando códigos especiales como *222#. Necesita señal celular, " +
                        "no datos ni Wi-Fi. Toca cualquier código de la lista y el marcador del " +
                        "sistema se abre listo para enviarlo — el propio Android te pide confirmar " +
                        "antes de que la llamada se realice."
                )
                HelpInfoRow(
                    title = "Códigos que piden un dato",
                    text = "Algunos códigos, como recargar con tarjeta, necesitan un número " +
                        "adicional (p. ej. *662*{tarjeta}#). Al tocarlos, primero se pide ese dato " +
                        "y luego se marca el código completo."
                )
                HelpLinkRow(
                    label = "Descargar Todos los Códigos",
                    icon = Icons.Filled.Download,
                    onClick = { uriHandler.openUri("$REPO_URL/blob/main/app/src/main/assets/codes.json") }
                )
            }

            HelpSection("Compras: Acción sin Confirmación") {
                HelpInfoRow(
                    title = "¿Qué hace?",
                    text = "En la pestaña Compras hay un interruptor \"Acción sin Confirmación\". " +
                        "Actívalo y los códigos que lo soportan marcan directo el paso de " +
                        "confirmación de ETECSA, ahorrándote un paso — solo actívalo si ya confías " +
                        "en lo que vas a comprar."
                )
            }

            HelpSection("Recordatorios") {
                HelpInfoRow(
                    title = "¿Qué hace?",
                    text = "En Ajustes › Utilidades › Recordatorios puedes crear notificaciones " +
                        "locales (sin servidor, sin internet) para que te avisen cuando toca " +
                        "comprar un paquete, recargar saldo o hacer una transferencia. Hay una " +
                        "sección por plantilla, y puedes agregar tantos recordatorios de cada una " +
                        "como necesites — uno por cada línea que manejes."
                )
                HelpInfoRow(
                    title = "Ejecutar desde el recordatorio",
                    text = "Comprar Paquete te lleva directo a la pestaña Compras (no tiene un " +
                        "solo código fijo, es todo un catálogo). Recargar Saldo te pide el número " +
                        "de la tarjeta justo antes de marcar (nunca se guarda). Transferencia " +
                        "recuerda el número de destino y te pide el monto, con tu Clave de " +
                        "Transferencia ya rellenada si la tienes guardada."
                )
                HelpInfoRow(
                    title = "Recurrencia y notificación",
                    text = "Elige avisarte una sola vez, todos los días, cada semana, cada mes o " +
                        "cada ciertos días. Desde la notificación misma puedes \"Marcar como " +
                        "hecho\" o \"Posponer 1 día\" sin abrir la app; tocarla abre el detalle del " +
                        "recordatorio. También puedes crear un recordatorio totalmente " +
                        "personalizado, sin plantilla. Todos empiezan sin ningún recordatorio " +
                        "activo — los creas tú, a tu medida."
                )
            }

            HelpSection("Plan Amigo") {
                HelpInfoRow(
                    title = "Gestionar Plan Amigo",
                    text = "En Ajustes › Cuenta › Gestionar Plan Amigo puedes activarlo, " +
                        "desactivarlo, agregar o eliminar un amigo (con su número o eligiéndolo " +
                        "de Contactos), y consultar su estado. Activar el Plan Amigos tiene un " +
                        "costo de $25.00."
                )
            }

            HelpSection("PIN de Transferencia") {
                HelpInfoRow(
                    title = "Cambiar y guardar tu PIN",
                    text = "En Ajustes › Cuenta › Gestionar PIN de Transferencia puedes cambiar " +
                        "el PIN que usas para transferir saldo, o guardarlo en este dispositivo " +
                        "para que se rellene solo cada vez que transfieras (desde Home o desde un " +
                        "contacto). Se guarda cifrado en este teléfono y nunca sale de él."
                )
            }

            HelpSection("Servicios por SMS") {
                HelpInfoRow(
                    title = "¿Qué es esto?",
                    text = "Son servicios de ETECSA que se usan enviando un SMS, no marcando un " +
                        "código — tarifas, DHL y vuelos, deportes, noticias, frases y horóscopos. " +
                        "Algunos son suscripciones (se marcan con la etiqueta \"Suscripción\") y " +
                        "pueden tener un costo recurrente. Necesitas un dispositivo que pueda " +
                        "enviar SMS."
                )
            }

            HelpSection("Salas y Zonas WiFi") {
                HelpInfoRow(
                    title = "¿Qué muestra?",
                    text = "Para cada provincia cubana, lista las salas de navegación pagas de " +
                        "ETECSA (con su cantidad de puestos) y las zonas de WiFi público gratis, " +
                        "agrupadas por municipio. Es información pública de ETECSA, incluida en " +
                        "la app — no necesita conexión para verse."
                )
                HelpLinkRow(
                    label = "Descargar JSON de Salas y Zonas WiFi",
                    icon = Icons.Filled.Download,
                    onClick = {
                        uriHandler.openUri("$REPO_URL/blob/main/app/src/main/assets/wifi_navigation_rooms.json")
                    }
                )
            }

            HelpSection("Buscar en Directorio") {
                HelpInfoRow(
                    title = "¿Qué es?",
                    text = "En Ajustes › Utilidades › Buscar en Directorio puedes buscar números " +
                        "y contactos comerciales en el directorio telefónico de ETECSA (función " +
                        "actualmente en desarrollo)."
                )
            }

            HelpSection("Buscar en BBDD") {
                HelpInfoRow(
                    title = "¿De dónde salen los datos?",
                    text = "La app no incluye ninguna base de datos ni la descarga " +
                        "automáticamente — tienes que traer tú mismo el archivo SQLite (.db) de " +
                        "base de datos para poder buscar."
                )
                HelpInfoRow(
                    title = "Búsqueda solo por número",
                    text = "Por privacidad, la búsqueda inversa en la base de datos se realiza " +
                        "únicamente a partir de números de teléfono, no por nombre."
                )
            }

            HelpSection("Desarrollador") {
                HelpLinkRow(
                    label = "@albertolicea00",
                    icon = Icons.Filled.Code,
                    onClick = { uriHandler.openUri(DEVELOPER_URL) }
                )
            }

            HelpSection("Versión") {
                Text(
                    BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun HelpSection(title: String, content: @Composable () -> Unit) {
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
private fun HelpInfoRow(title: String, text: String) {
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
private fun HelpLinkRow(label: String, icon: ImageVector, onClick: () -> Unit) {
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

private fun openLanguageSettings(context: android.content.Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
    } else {
        Intent(Settings.ACTION_LOCALE_SETTINGS)
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
