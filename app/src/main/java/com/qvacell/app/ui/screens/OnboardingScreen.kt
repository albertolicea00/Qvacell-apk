package com.qvacell.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.telephony.SubscriptionInfo
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
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Work
import com.qvacell.app.ui.navigation.SquareGrid2x2
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.BuildConfig
import com.qvacell.app.R
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.data.SimAppearance
import com.qvacell.app.service.DialService
import com.qvacell.app.service.OnboardingDataPrefetch
import com.qvacell.app.service.SimUtils
import com.qvacell.app.ui.components.SimBadge
import kotlinx.coroutines.launch

private sealed class OnboardingPage {
    object Welcome : OnboardingPage()
    object UssdPermission : OnboardingPage()
    object SimSetup : OnboardingPage()
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
    var phoneStateGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
    }
    var activeSims by remember { mutableStateOf(SimUtils.getActiveSubscriptions(context)) }
    val selectedSimSlot by settings.selectedSimSlot.collectAsStateWithLifecycle(initialValue = -1)
    var tourSelectedSlot by remember { mutableIntStateOf(-1) }
    LaunchedEffect(selectedSimSlot, activeSims) {
        tourSelectedSlot = SimUtils.resolveSelectedSlot(context, selectedSimSlot)
    }
    val simAppearances by settings.simAppearances.collectAsStateWithLifecycle(initialValue = emptyMap())
    var tourLineTypes by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }
    var dashboardPrefetchStarted by remember { mutableStateOf(false) }

    val pages = remember(phoneStateGranted, activeSims) {
        buildList {
            add(OnboardingPage.Welcome)
            add(OnboardingPage.UssdPermission)
            if (!phoneStateGranted || activeSims.isNotEmpty()) add(OnboardingPage.SimSetup)
            add(OnboardingPage.FeatureContacts)
            add(OnboardingPage.FeatureHome)
            add(OnboardingPage.FeaturePurchases)
            add(OnboardingPage.FeatureOptions)
        }
    }

    val ussdPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> ussdGranted = granted }
    val phoneStateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        phoneStateGranted = granted
        activeSims = SimUtils.getActiveSubscriptions(context)
    }

    LaunchedEffect(currentPageIndex, ussdGranted, pages) {
        val setupIndex = pages.indexOf(OnboardingPage.SimSetup).takeIf { it >= 0 }
            ?: pages.indexOf(OnboardingPage.UssdPermission)
        if (BuildConfig.DASHBOARD_CAPTURE_ENABLED && ussdGranted &&
            currentPageIndex > setupIndex && !dashboardPrefetchStarted
        ) {
            dashboardPrefetchStarted = true
            OnboardingDataPrefetch.startBackgroundFetch(context.applicationContext)
        }
    }

    fun next() {
        if (currentPageIndex < pages.lastIndex) {
            if (pages[currentPageIndex] == OnboardingPage.SimSetup) {
                scope.launch {
                    val slot = SimUtils.resolveSelectedSlot(context, tourSelectedSlot)
                    if (phoneStateGranted && activeSims.isNotEmpty()) {
                        for (sim in activeSims) {
                            val existing = simAppearances[sim.subscriptionId.toString()]
                            val isCorporate = tourLineTypes[sim.subscriptionId] ?: existing?.isCorporate
                                ?: return@launch
                            settings.setSimAppearance(
                                sim.subscriptionId,
                                (existing ?: SimAppearance()).copy(
                                    isCorporate = isCorporate,
                                    icon = if (isCorporate) "work" else "home"
                                )
                            )
                        }
                    }
                    if (slot >= 0) settings.setSelectedSimSlot(slot)
                    currentPageIndex++
                }
            } else {
                currentPageIndex++
            }
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
                    OnboardingPage.SimSetup -> if (!phoneStateGranted) {
                        PageLayout(
                            icon = Icons.Filled.SimCard,
                            title = stringResource(R.string.onboarding_sim_title),
                            subtitle = stringResource(R.string.onboarding_sim_subtitle),
                            content = { Text(stringResource(R.string.onboarding_sim_permission)) },
                            buttonLabel = stringResource(R.string.onboarding_sim_detect),
                            onButton = { phoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE) },
                            skipLabel = stringResource(R.string.onboarding_skip_for_now),
                            onSkip = ::next
                        )
                    } else {
                        SimSetupPage(
                            sims = activeSims,
                            selectedSlot = tourSelectedSlot,
                            appearances = simAppearances,
                            lineTypes = tourLineTypes,
                            onSelect = { slot -> tourSelectedSlot = slot },
                            onSelectType = { sim, isCorporate ->
                                tourLineTypes = tourLineTypes + (sim.subscriptionId to isCorporate)
                            },
                            onNext = ::next
                        )
                    }
                    OnboardingPage.FeatureContacts -> FeaturePage(
                        icon = Icons.Filled.People,
                        title = stringResource(R.string.onboarding_contacts_title),
                        subtitle = stringResource(R.string.onboarding_contacts_subtitle),
                        bullets = listOf(
                            stringResource(R.string.onboarding_contacts_bullet1),
                            stringResource(R.string.onboarding_contacts_bullet2),
                            stringResource(R.string.onboarding_contacts_bullet3)
                        ),
                        onNext = ::next
                    )
                    OnboardingPage.FeatureHome -> FeaturePage(
                        icon = Icons.Filled.Home,
                        title = stringResource(R.string.onboarding_home_title),
                        subtitle = stringResource(R.string.onboarding_home_subtitle),
                        bullets = buildList {
                            add(stringResource(R.string.onboarding_home_bullet1))
                            if (BuildConfig.DASHBOARD_CAPTURE_ENABLED) {
                                add(stringResource(R.string.onboarding_home_bullet2))
                            }
                            add(stringResource(R.string.onboarding_home_bullet3))
                        },
                        onNext = ::next
                    )
                    OnboardingPage.FeaturePurchases -> FeaturePage(
                        icon = Icons.Filled.ShoppingCart,
                        title = stringResource(R.string.onboarding_purchases_title),
                        subtitle = stringResource(R.string.onboarding_purchases_subtitle),
                        bullets = listOf(
                            stringResource(R.string.onboarding_purchases_bullet1),
                            stringResource(R.string.onboarding_purchases_bullet2),
                            stringResource(R.string.onboarding_purchases_bullet3)
                        ),
                        onNext = ::next
                    )
                    OnboardingPage.FeatureOptions -> FeaturePage(
                        icon = SquareGrid2x2,
                        title = stringResource(R.string.onboarding_options_title),
                        subtitle = stringResource(R.string.onboarding_options_subtitle),
                        bullets = listOf(
                            stringResource(R.string.onboarding_options_bullet1),
                            stringResource(R.string.onboarding_options_bullet2),
                            stringResource(R.string.onboarding_options_bullet3)
                        ),
                        buttonLabel = stringResource(R.string.onboarding_ready_button),
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
                    Text(stringResource(R.string.onboarding_skip_tour), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    buttonLabel: String? = null,
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
                Text(buttonLabel ?: stringResource(R.string.onboarding_continue), modifier = Modifier.padding(vertical = 4.dp))
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
                stringResource(R.string.onboarding_ussd_granted),
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
                stringResource(R.string.onboarding_author),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            stringResource(R.string.onboarding_welcome_subtitle),
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
            Text(stringResource(R.string.onboarding_start), modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun UssdPermissionPage(granted: Boolean, onRequest: () -> Unit, onNext: () -> Unit) {
    PageLayout(
        icon = Icons.Filled.Phone,
        title = stringResource(R.string.onboarding_ussd_title),
        subtitle = stringResource(R.string.onboarding_ussd_subtitle),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                BulletItem(Icons.Filled.ShoppingCart, stringResource(R.string.onboarding_ussd_bullet1))
                BulletItem(Icons.Filled.AccountBalance, stringResource(R.string.onboarding_ussd_bullet2))
                BulletItem(Icons.Filled.Sms, stringResource(R.string.onboarding_ussd_bullet3))
                Spacer(Modifier.height(4.dp))
                if (granted) {
                    GrantedBadge()
                } else {
                    Text(
                        stringResource(R.string.onboarding_ussd_rationale),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        buttonLabel = if (granted) stringResource(R.string.onboarding_continue) else stringResource(R.string.onboarding_grant_permission),
        onButton = if (granted) onNext else onRequest,
        skipLabel = if (!granted) stringResource(R.string.onboarding_skip_for_now) else null,
        onSkip = if (!granted) onNext else null
    )
}

@Composable
private fun SimSetupPage(
    sims: List<SubscriptionInfo>,
    selectedSlot: Int,
    appearances: Map<String, SimAppearance>,
    lineTypes: Map<Int, Boolean>,
    onSelect: (Int) -> Unit,
    onSelectType: (SubscriptionInfo, Boolean) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val effectiveSlot = SimUtils.resolveSelectedSlot(context, selectedSlot)
    val canContinue = effectiveSlot >= 0 && sims.all {
        it.subscriptionId in lineTypes || it.subscriptionId.toString() in appearances
    }
    PageLayout(
        icon = Icons.Filled.SimCard,
        title = stringResource(R.string.onboarding_sim_title),
        subtitle = stringResource(R.string.onboarding_sim_subtitle),
        content = {
            sims.forEach { sim ->
                val appearance = appearances[sim.subscriptionId.toString()]
                val isCorporate = lineTypes[sim.subscriptionId] ?: appearance?.isCorporate
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            SimBadge(
                                sim.simSlotIndex,
                                if (isCorporate == null) appearance else (appearance ?: SimAppearance()).copy(
                                    icon = if (isCorporate) "work" else "home"
                                )
                            )
                            Text(
                                SimUtils.simLabel(sim, appearance),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f).padding(start = 12.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = isCorporate == false,
                                onClick = { onSelectType(sim, false) },
                                label = { Text(stringResource(R.string.sim_standard_line)) },
                                leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = isCorporate == true,
                                onClick = { onSelectType(sim, true) },
                                label = { Text(stringResource(R.string.onboarding_sim_corporate)) },
                                leadingIcon = { Icon(Icons.Filled.Work, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = effectiveSlot == sim.simSlotIndex,
                                onClick = { onSelect(sim.simSlotIndex) }
                            )
                            Text(stringResource(R.string.onboarding_sim_primary))
                        }
                    }
                }
            }
        },
        onButton = onNext,
        buttonEnabled = canContinue
    )
}

@Composable
private fun FeaturePage(
    icon: ImageVector,
    title: String,
    subtitle: String,
    bullets: List<String>,
    buttonLabel: String? = null,
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
