package com.qvacell.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DialerSip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qvacell.app.R
import com.qvacell.app.BuildConfig
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.service.DashboardCapture
import com.qvacell.app.service.DialService
import com.qvacell.app.service.OnboardingDataPrefetch
import kotlinx.coroutines.launch

private const val GITHUB_RELEASES_URL = "https://github.com/albertolicea00/qvacell-apk/releases"

private sealed class OnboardingPage {
    object Welcome : OnboardingPage()
    object DashboardChoice : OnboardingPage()
    object HowDynamicWorks : OnboardingPage()
    object DynamicUnavailable : OnboardingPage()
    object UssdPermission : OnboardingPage()
    object SmsPermission : OnboardingPage()
    object CallLogPermission : OnboardingPage()
    object FeatureContacts : OnboardingPage()
    object FeatureHome : OnboardingPage()
    object FeaturePurchases : OnboardingPage()
    object FeatureSettings : OnboardingPage()
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsDataStore(context) }

    var dashboardChoice by remember { mutableStateOf<String?>(null) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var ussdGranted by remember { mutableStateOf(DialService.hasCallPermission(context)) }
    var smsGranted by remember { mutableStateOf(false) }
    var callLogGranted by remember { mutableStateOf(false) }

    val pages = remember(dashboardChoice) {
        buildList {
            add(OnboardingPage.Welcome)
            add(OnboardingPage.DashboardChoice)
            if (dashboardChoice == "dynamic") {
                if (BuildConfig.DASHBOARD_CAPTURE_ENABLED) {
                    add(OnboardingPage.HowDynamicWorks)
                } else {
                    add(OnboardingPage.DynamicUnavailable)
                }
            }
            add(OnboardingPage.UssdPermission)
            if (dashboardChoice == "dynamic" && BuildConfig.DASHBOARD_CAPTURE_ENABLED) {
                add(OnboardingPage.SmsPermission)
                add(OnboardingPage.CallLogPermission)
            }
            add(OnboardingPage.FeatureContacts)
            add(OnboardingPage.FeatureHome)
            add(OnboardingPage.FeaturePurchases)
            add(OnboardingPage.FeatureSettings)
        }
    }

    // Fire background prefetch as soon as the user picks dynamic mode.
    LaunchedEffect(dashboardChoice) {
        if (dashboardChoice == "dynamic") {
            OnboardingDataPrefetch.startBackgroundFetch(context)
        }
    }

    val ussdPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> ussdGranted = granted }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> smsGranted = results.values.all { it } }

    val callLogPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> callLogGranted = granted }

    fun next() {
        if (currentPageIndex < pages.lastIndex) {
            currentPageIndex++
        } else {
            scope.launch {
                settings.setHasCompletedOnboarding(true)
                if (dashboardChoice != null) settings.setDashboardMode(dashboardChoice!!)
                if (ussdGranted) settings.setUssdCaptureEnabled(true)
                if (smsGranted) {
                    settings.setSmsCaptureEnabled(true)
                    DashboardCapture.scheduleEstimationIfEnabled(context, true)
                }
                onComplete()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            ProgressDots(
                total = pages.size,
                current = currentPageIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )

            AnimatedContent(
                targetState = pages.getOrNull(currentPageIndex),
                transitionSpec = {
                    (slideInHorizontally { it / 2 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 2 } + fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "onboarding_page"
            ) { page ->
                when (page) {
                    OnboardingPage.Welcome -> WelcomePage(
                        onNext = {
                            ussdPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                            next()
                        }
                    )
                    OnboardingPage.DashboardChoice -> DashboardChoicePage(
                        selectedChoice = dashboardChoice,
                        onChoiceSelected = { dashboardChoice = it },
                        onNext = ::next
                    )
                    OnboardingPage.HowDynamicWorks -> HowDynamicWorksPage(onNext = ::next)
                    OnboardingPage.DynamicUnavailable -> DynamicUnavailablePage(
                        onSwitchToManual = {
                            dashboardChoice = "manual"
                            // pages rebuilds → currentPageIndex now points to UssdPermission
                        },
                        onOpenGitHub = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_RELEASES_URL))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    )
                    OnboardingPage.UssdPermission -> UssdPermissionPage(
                        granted = ussdGranted,
                        onRequest = { ussdPermissionLauncher.launch(Manifest.permission.CALL_PHONE) },
                        onNext = ::next
                    )
                    OnboardingPage.SmsPermission -> SmsPermissionPage(
                        granted = smsGranted,
                        onRequest = {
                            smsPermissionLauncher.launch(
                                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                            )
                        },
                        onNext = ::next
                    )
                    OnboardingPage.CallLogPermission -> CallLogPermissionPage(
                        granted = callLogGranted,
                        onRequest = { callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG) },
                        onNext = ::next
                    )
                    OnboardingPage.FeatureContacts -> FeaturePage(
                        icon = Icons.Filled.People,
                        title = "Contactos",
                        subtitle = "Gestiona y llama directamente desde la app",
                        bullets = listOf(
                            "Marcación directa a cualquier contacto",
                            "Compara planes entre contactos",
                            "Búsqueda rápida en tu agenda"
                        ),
                        onNext = ::next
                    )
                    OnboardingPage.FeatureHome -> FeaturePage(
                        icon = Icons.Filled.Home,
                        title = "Inicio",
                        subtitle = "Acceso rápido a los servicios más usados",
                        bullets = listOf(
                            "Recargas y paquetes en un toque",
                            "Dashboard con tu saldo y consumo",
                            "Consultas USSD sin salir de la app"
                        ),
                        onNext = ::next
                    )
                    OnboardingPage.FeaturePurchases -> FeaturePage(
                        icon = Icons.Filled.ShoppingCart,
                        title = "Compras",
                        subtitle = "Todos los paquetes y servicios ETECSA",
                        bullets = listOf(
                            "Paquetes de datos, Nauta, voz y combinados",
                            "Activación de servicios especiales",
                            "Confirmación antes de marcar"
                        ),
                        onNext = ::next
                    )
                    OnboardingPage.FeatureSettings -> FeaturePage(
                        icon = Icons.Filled.Settings,
                        title = "Personalización",
                        subtitle = "Adapta la app a tu estilo y necesidades",
                        bullets = listOf(
                            "Tema claro, oscuro o automático",
                            "Color de acento personalizado",
                            "Recordatorios automáticos de recarga"
                        ),
                        buttonLabel = "¡Listo, vamos!",
                        onNext = ::next
                    )
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun ProgressDots(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val isActive = index == current
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .then(if (isActive) Modifier.width(24.dp) else Modifier.size(8.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
            )
            if (index < total - 1) Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun PageLayout(
    icon: ImageVector? = null,
    painter: Painter? = null,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit = {},
    buttonLabel: String = "Continuar",
    onButton: (() -> Unit)?,
    buttonEnabled: Boolean = true,
    skipLabel: String? = null,
    onSkip: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.5f))

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(72.dp)
                )
            } else if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        content()

        Spacer(Modifier.weight(1f))

        if (onButton != null) {
            Button(
                onClick = onButton,
                enabled = buttonEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(buttonLabel, modifier = Modifier.padding(vertical = 4.dp))
            }
        }

        if (skipLabel != null && onSkip != null) {
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text(skipLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InfoCard(icon: ImageVector, text: String, containerColor: androidx.compose.ui.graphics.Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp).padding(top = 1.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GrantedBadge() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(
                "Permiso concedido",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun BulletItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

// ─── Individual pages ───────────────────────────────────────────────────────

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    PageLayout(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        title = "Bienvenido a Qvacell",
        subtitle = "Tu asistente para servicios ETECSA y Cubacel en Cuba",
        buttonLabel = "Comenzar",
        onButton = onNext
    )
}

@Composable
private fun DashboardChoicePage(
    selectedChoice: String?,
    onChoiceSelected: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.5f))

        Text(
            "¿Cómo quieres ver tu información?",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Puedes cambiarlo más adelante desde Ajustes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        DashboardChoiceCard(
            selected = selectedChoice == "dynamic",
            icon = Icons.Filled.Analytics,
            title = "Dashboard Dinámico",
            description = "Saldo, datos y llamadas actualizados en tiempo real. Requiere algunos permisos opcionales.",
            onClick = { onChoiceSelected("dynamic") }
        )

        Spacer(Modifier.height(16.dp))

        DashboardChoiceCard(
            selected = selectedChoice == "manual",
            icon = Icons.Filled.DialerSip,
            title = "Marcación Rápida",
            description = "Solo los botones esenciales para marcar códigos USSD. Sin permisos adicionales.",
            onClick = { onChoiceSelected("manual") }
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onNext,
            enabled = selectedChoice != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Continuar", modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DashboardChoiceCard(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                         else MaterialTheme.colorScheme.surfaceContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun HowDynamicWorksPage(onNext: () -> Unit) {
    PageLayout(
        icon = Icons.Filled.Analytics,
        title = "Tu dashboard en tiempo real",
        subtitle = "El modo dinámico mantiene tu información siempre al día",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                BulletItem(Icons.Filled.AccountBalance, "Saldo ETECSA siempre actualizado")
                BulletItem(Icons.Filled.DataUsage, "Consumo de datos y minutos en tiempo real")
                BulletItem(Icons.Filled.History, "Historial de recargas y compras")
                Spacer(Modifier.height(4.dp))
                InfoCard(
                    icon = Icons.Filled.Info,
                    text = "Más permisos aceptes → más datos en tiempo real. Tú decides cuáles conceder — todos son opcionales.",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        },
        buttonLabel = "Entendido",
        onButton = onNext
    )
}

@Composable
private fun UssdPermissionPage(granted: Boolean, onRequest: () -> Unit, onNext: () -> Unit) {
    PageLayout(
        icon = Icons.Filled.Phone,
        title = "Marcación USSD",
        subtitle = "Permite a Qvacell marcar códigos como *222# directamente sin abrir el marcador",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                BulletItem(Icons.Filled.ShoppingCart, "Compras y recargas sin salir de la app")
                BulletItem(Icons.Filled.AccountBalance, "Consultas de saldo en tiempo real")
                BulletItem(Icons.Filled.Sms, "Activación de servicios ETECSA")
                Spacer(Modifier.height(4.dp))
                if (granted) {
                    GrantedBadge()
                } else {
                    Text(
                        "Sin este permiso abriremos el marcador del sistema. Puedes concederlo más tarde desde Ajustes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        buttonLabel = if (granted) "Continuar" else "Conceder Permiso",
        onButton = if (granted) onNext else onRequest,
        skipLabel = if (!granted) "Saltar por ahora" else null,
        onSkip = if (!granted) onNext else null
    )
}

@Composable
private fun SmsPermissionPage(granted: Boolean, onRequest: () -> Unit, onNext: () -> Unit) {
    PageLayout(
        icon = Icons.Filled.Sms,
        title = "Lectura de SMS",
        subtitle = "Detecta recargas y saldo leyendo los SMS de ETECSA y Cubacel automáticamente",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                BulletItem(Icons.Filled.AccountBalance, "Detecta recargas al instante")
                BulletItem(Icons.Filled.Notifications, "Saldo actualizado al recibir el SMS")
                Spacer(Modifier.height(4.dp))
                InfoCard(
                    icon = Icons.Filled.Security,
                    text = "Solo se leen mensajes de ETECSA y Cubacel. Tus conversaciones personales nunca se acceden ni se envían a ningún servidor.",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
                if (granted) {
                    Spacer(Modifier.height(4.dp))
                    GrantedBadge()
                }
            }
        },
        buttonLabel = if (granted) "Continuar" else "Conceder Permiso",
        onButton = if (granted) onNext else onRequest,
        skipLabel = if (!granted) "Saltar por ahora" else null,
        onSkip = if (!granted) onNext else null
    )
}

@Composable
private fun CallLogPermissionPage(granted: Boolean, onRequest: () -> Unit, onNext: () -> Unit) {
    PageLayout(
        icon = Icons.Filled.Phone,
        title = "Registro de Llamadas",
        subtitle = "Calcula los minutos usados y mantiene tu saldo de voz siempre al día",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                BulletItem(Icons.Filled.Timer, "Cuenta las llamadas realizadas")
                BulletItem(Icons.Filled.DataUsage, "Calcula el consumo de minutos")
                BulletItem(Icons.Filled.Sync, "Actualiza el dashboard en segundo plano")
                if (granted) {
                    Spacer(Modifier.height(4.dp))
                    GrantedBadge()
                }
            }
        },
        buttonLabel = if (granted) "Continuar" else "Conceder Permiso",
        onButton = if (granted) onNext else onRequest,
        skipLabel = if (!granted) "Saltar por ahora" else null,
        onSkip = if (!granted) onNext else null
    )
}

@Composable
private fun FeaturePage(
    icon: ImageVector,
    title: String,
    subtitle: String,
    bullets: List<String>,
    buttonLabel: String = "Continuar",
    onNext: () -> Unit
) {
    PageLayout(
        icon = icon,
        title = title,
        subtitle = subtitle,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                bullets.forEach { BulletItem(Icons.Filled.CheckCircle, it) }
            }
        },
        buttonLabel = buttonLabel,
        onButton = onNext
    )
}

@Composable
private fun DynamicUnavailablePage(
    onSwitchToManual: () -> Unit,
    onOpenGitHub: () -> Unit
) {
    PageLayout(
        icon = Icons.Filled.OpenInBrowser,
        title = "Función no disponible",
        subtitle = "El Dashboard Dinámico no está incluido en la versión de Play Store",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                InfoCard(
                    icon = Icons.Filled.Info,
                    text = "Esta versión de Qvacell está limitada por las políticas de Google Play. La captura automática de USSD, SMS y registro de llamadas solo está disponible en la versión completa desde GitHub.",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
                Button(
                    onClick = onOpenGitHub,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ver releases en GitHub", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        },
        buttonLabel = "Continuar con Marcación Rápida",
        onButton = onSwitchToManual
    )
}
