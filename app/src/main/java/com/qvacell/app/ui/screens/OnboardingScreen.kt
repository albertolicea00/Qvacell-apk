package com.qvacell.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import com.qvacell.app.ui.navigation.SquareGrid2x2
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qvacell.app.R
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.service.DialService
import kotlinx.coroutines.launch

private sealed class OnboardingPage {
    object Welcome : OnboardingPage()
    object UssdPermission : OnboardingPage()
    object FeatureContacts : OnboardingPage()
    object FeatureHome : OnboardingPage()
    object FeaturePurchases : OnboardingPage()
    object FeatureOptions : OnboardingPage()

    @Deprecated("Renamed to FeatureOptions", ReplaceWith("FeatureOptions"))
    val FeatureSettings = FeatureOptions
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit, onSkipTour: (() -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsDataStore(context) }

    var currentPageIndex by remember { mutableIntStateOf(0) }
    var ussdGranted by remember { mutableStateOf(DialService.hasCallPermission(context)) }

    val pages = remember {
        buildList {
            add(OnboardingPage.Welcome)
            add(OnboardingPage.UssdPermission)
            add(OnboardingPage.FeatureContacts)
            add(OnboardingPage.FeatureHome)
            add(OnboardingPage.FeaturePurchases)
            add(OnboardingPage.FeatureOptions)
        }
    }

    val ussdPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> ussdGranted = granted }

    fun next() {
        if (currentPageIndex < pages.lastIndex) {
            currentPageIndex++
        } else {
            scope.launch {
                settings.setHasCompletedOnboarding(true)
                if (ussdGranted) settings.setUssdCaptureEnabled(true)
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
                    OnboardingPage.UssdPermission -> UssdPermissionPage(
                        granted = ussdGranted,
                        onRequest = { ussdPermissionLauncher.launch(Manifest.permission.CALL_PHONE) },
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
                            "Dashboard Dinámico desde la barra superior",
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
                    OnboardingPage.FeatureOptions -> FeaturePage(
                        icon = SquareGrid2x2,
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

            if (onSkipTour != null) {
                TextButton(
                    onClick = onSkipTour,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 12.dp)
                ) {
                    Text("Saltar tour", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                shape = RoundedCornerShape(50)
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
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(160.dp)
        )

        TextButton(onClick = { uriHandler.openUri("https://github.com/albertolicea00") }) {
            Text(
                "by @albertolicea00",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Bienvenido a Qvacell",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Tu asistente para servicios ETECSA y Cubacel en Cuba",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50)
        ) {
            Text("Comenzar", modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(Modifier.height(16.dp))
    }
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
                        "Sin este permiso abriremos el marcador del sistema. Puedes concederlo más tarde desde Opciones.",
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
