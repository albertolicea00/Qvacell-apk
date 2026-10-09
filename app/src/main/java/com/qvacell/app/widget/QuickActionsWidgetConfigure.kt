package com.qvacell.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.data.ThemeMode
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.model.UssdCode
import com.qvacell.app.service.SimUtils
import com.qvacell.app.ui.resolveAndroidIcon
import com.qvacell.app.ui.screens.ColorPickerSheet
import com.qvacell.app.ui.screens.getWidgetShape
import com.qvacell.app.ui.theme.QvacellTheme

class QuickActionsWidgetConfigure : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(RESULT_CANCELED)

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val settings = SettingsDataStore(applicationContext)
        val catalog = CatalogRepository(this).loadCatalog()

        data class ListItemData(val categoryHeader: String? = null, val code: UssdCode? = null)

        val listItems = buildList {
            catalog.categories
                .filter { it.id != "sms" }
                .forEach { cat ->
                    val codes = cat.groups.flatMap { it.codes }
                    if (codes.isNotEmpty()) {
                        add(ListItemData(categoryHeader = cat.name.value))
                        codes.forEach { add(ListItemData(code = it)) }
                    }
                }
        }

        val initialCodeId = WidgetPrefs.getCodeId(this, widgetId)
        val initialSimSlot = WidgetPrefs.getSimSlot(this, widgetId)
        val initialShape = WidgetPrefs.getIconShape(this, widgetId)
        val initialBgColor = WidgetPrefs.getBackgroundColor(this, widgetId)
        val initialIconColor = WidgetPrefs.getIconColor(this, widgetId)

        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val accentColor by settings.accentColor.collectAsStateWithLifecycle(initialValue = SettingsDataStore.DEFAULT_ACCENT_COLOR)

            QvacellTheme(themeMode = themeMode, accentColorHex = accentColor) {
                val navBarColor = MaterialTheme.colorScheme.surface
                SideEffect {
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = navBarColor.toArgb()
                }

                val activeSims = remember { SimUtils.getActiveSubscriptions(this@QuickActionsWidgetConfigure) }

                var selectedCodeId by remember { mutableStateOf(initialCodeId) }
                var selectedSimSlot by remember { mutableIntStateOf(initialSimSlot) }
                var selectedShape by remember { mutableStateOf(initialShape) }
                var selectedBgColor by remember { mutableStateOf(Color(initialBgColor)) }
                var selectedIconColor by remember { mutableStateOf(Color(initialIconColor)) }

                var selectedTab by remember { mutableIntStateOf(0) }
                var searchActive by remember { mutableStateOf(false) }
                var query by remember { mutableStateOf("") }
                val focusRequester = remember { FocusRequester() }

                var showShapeSheet by remember { mutableStateOf(false) }
                var showBgColorSheet by remember { mutableStateOf(false) }
                var showIconColorSheet by remember { mutableStateOf(false) }
                var showSimSheet by remember { mutableStateOf(false) }

                val currentCode = remember(selectedCodeId) {
                    catalog.categories.flatMap { it.groups }.flatMap { it.codes }
                        .firstOrNull { it.id == selectedCodeId }
                }

                fun saveAndFinish() {
                    WidgetPrefs.saveCodeId(this@QuickActionsWidgetConfigure, widgetId, selectedCodeId)
                    WidgetPrefs.saveSimSlot(this@QuickActionsWidgetConfigure, widgetId, selectedSimSlot)
                    WidgetPrefs.saveIconShape(this@QuickActionsWidgetConfigure, widgetId, selectedShape)
                    WidgetPrefs.saveBackgroundColor(this@QuickActionsWidgetConfigure, widgetId, selectedBgColor.toArgb())
                    WidgetPrefs.saveIconColor(this@QuickActionsWidgetConfigure, widgetId, selectedIconColor.toArgb())

                    val manager = AppWidgetManager.getInstance(this@QuickActionsWidgetConfigure)
                    QuickActionsWidget.updateWidget(this@QuickActionsWidgetConfigure, manager, widgetId)
                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                    finish()
                }

                val filtered = if (query.isBlank()) {
                    listItems
                } else {
                    listItems.filter { item ->
                        item.categoryHeader != null ||
                            (item.code != null && (
                                item.code.title.value.contains(query, ignoreCase = true) ||
                                item.code.details.value.contains(query, ignoreCase = true) ||
                                item.code.code.contains(query, ignoreCase = true)
                            ))
                    }.let { filteredList ->
                        val result = mutableListOf<ListItemData>()
                        filteredList.forEachIndexed { i, item ->
                            if (item.categoryHeader != null) {
                                val hasCodesAfter = filteredList.drop(i + 1).any { it.code != null }
                                if (hasCodesAfter) result.add(item)
                            } else {
                                result.add(item)
                            }
                        }
                        result
                    }
                }

                Scaffold(
                    topBar = {
                        Column {
                            if (searchActive && selectedTab == 0) {
                                TopAppBar(
                                    title = {
                                        BasicTextField(
                                            value = query,
                                            onValueChange = { query = it },
                                            singleLine = true,
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            textStyle = TextStyle(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 16.sp
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(focusRequester),
                                            decorationBox = { inner ->
                                                if (query.isEmpty()) {
                                                    Text(
                                                        "Buscar acción...",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 16.sp
                                                    )
                                                }
                                                inner()
                                            }
                                        )
                                    },
                                    actions = {
                                        IconButton(onClick = {
                                            searchActive = false
                                            query = ""
                                        }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Cerrar búsqueda")
                                        }
                                    }
                                )
                                LaunchedEffect(Unit) {
                                    focusRequester.requestFocus()
                                }
                            } else {
                                TopAppBar(
                                    title = { Text("Configurar widget") },
                                    actions = {
                                        if (selectedTab == 0) {
                                            IconButton(onClick = { searchActive = true }) {
                                                Icon(Icons.Filled.Search, contentDescription = "Buscar")
                                            }
                                        }
                                        TextButton(onClick = { saveAndFinish() }) {
                                            Text("Listo", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }

                            PrimaryTabRow(selectedTabIndex = selectedTab) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    text = { Text("Acción") },
                                    icon = { Icon(Icons.Filled.TouchApp, contentDescription = null) }
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1; searchActive = false },
                                    text = { Text("Estilo") },
                                    icon = { Icon(Icons.Filled.Palette, contentDescription = null) }
                                )
                                if (activeSims.size >= 2) {
                                    Tab(
                                        selected = selectedTab == 2,
                                        onClick = { selectedTab = 2; searchActive = false },
                                        text = { Text("SIM") },
                                        icon = { Icon(Icons.Filled.SimCard, contentDescription = null) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        0 -> {
                            // Pestaña Acción
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                val hasResults = filtered.any { it.code != null }
                                if (!hasResults && query.isNotBlank()) {
                                    item(key = "empty") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 48.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Sin resultados para \"$query\"",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                filtered.forEachIndexed { index, item ->
                                    if (item.categoryHeader != null) {
                                        item(key = "header_$index") {
                                            Text(
                                                text = item.categoryHeader,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(
                                                    start = 16.dp, top = 16.dp, bottom = 4.dp
                                                )
                                            )
                                        }
                                    } else if (item.code != null) {
                                        val code = item.code
                                        val isSelected = code.id == selectedCodeId
                                        item(key = code.id) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedCodeId = code.id
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(getWidgetShape(selectedShape))
                                                        .background(selectedBgColor),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = resolveAndroidIcon(code.icon),
                                                        contentDescription = null,
                                                        tint = selectedIconColor,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(14.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = code.title.value,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        text = code.details.value,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = code.code,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                    )
                                                }

                                                RadioButton(selected = isSelected, onClick = null)
                                            }
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // Pestaña Estilo
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    // Vista previa del widget
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                "Vista previa",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(bottom = 16.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(getWidgetShape(selectedShape))
                                                    .background(selectedBgColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = resolveAndroidIcon(currentCode?.icon ?: "dollarsign.circle"),
                                                    contentDescription = null,
                                                    tint = selectedIconColor,
                                                    modifier = Modifier.size(46.dp)
                                                )
                                            }
                                            Text(
                                                currentCode?.title?.value ?: "Acción",
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(top = 12.dp)
                                            )
                                        }
                                    }
                                }

                                item {
                                    Text(
                                        "Personalización del widget",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                    ) {
                                        Column {
                                            val shapeLabel = when (selectedShape) {
                                                "rounded_square"  -> "Redondeado"
                                                "squircle"        -> "Squircle (Pixel)"
                                                "circle"          -> "Círculo"
                                                "square"          -> "Cuadrado"
                                                "hexagon"         -> "Hexágono"
                                                "rounded_hexagon" -> "Hexágono redondeado"
                                                "diamond"         -> "Diamante"
                                                "rounded_diamond" -> "Diamante redondeado"
                                                "badge"           -> "Sello"
                                                else -> "Redondeado"
                                            }
                                            ListItem(
                                                headlineContent = { Text("Forma") },
                                                supportingContent = { Text(shapeLabel, style = MaterialTheme.typography.labelSmall) },
                                                modifier = Modifier.clickable { showShapeSheet = true },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                            )
                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                            ListItem(
                                                headlineContent = { Text("Color de fondo") },
                                                supportingContent = { Text("Fondo de este widget", style = MaterialTheme.typography.labelSmall) },
                                                trailingContent = {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(selectedBgColor)
                                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                                    )
                                                },
                                                modifier = Modifier.clickable { showBgColorSheet = true },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                            )
                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                            ListItem(
                                                headlineContent = { Text("Color del icono") },
                                                supportingContent = { Text("Color del símbolo", style = MaterialTheme.typography.labelSmall) },
                                                trailingContent = {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(selectedIconColor)
                                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                                    )
                                                },
                                                modifier = Modifier.clickable { showIconColorSheet = true },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Pestaña SIM
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    Text(
                                        "Línea para ejecutar esta acción",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedSimSlot = -1 }
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(selected = selectedSimSlot < 0, onClick = null)
                                                Icon(
                                                    Icons.Filled.SimCard,
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(start = 12.dp).size(28.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                                    Text("Predeterminada", style = MaterialTheme.typography.bodyLarge)
                                                    Text(
                                                        "Usa el ajuste global configurado en la app",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            activeSims.forEachIndexed { _, sim ->
                                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { selectedSimSlot = sim.simSlotIndex }
                                                        .padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(selected = selectedSimSlot == sim.simSlotIndex, onClick = null)
                                                    Icon(
                                                        Icons.Filled.SimCard,
                                                        contentDescription = null,
                                                        modifier = Modifier.padding(start = 12.dp).size(28.dp),
                                                        tint = SimUtils.simColor(sim.simSlotIndex)
                                                    )
                                                    Column(modifier = Modifier.padding(start = 12.dp)) {
                                                        Text(SimUtils.simLabel(sim), style = MaterialTheme.typography.bodyLarge)
                                                        val carrier = sim.carrierName?.toString()?.takeIf { it.isNotBlank() }
                                                        val number = SimUtils.getPhoneNumber(this@QuickActionsWidgetConfigure, sim)
                                                        val subtitle = listOfNotNull(carrier, number).joinToString(" · ")
                                                        if (subtitle.isNotBlank() && subtitle != SimUtils.simLabel(sim)) {
                                                            Text(
                                                                subtitle,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Sheets de selección de estilo
                if (showShapeSheet) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(onDismissRequest = { showShapeSheet = false }, sheetState = sheetState) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                            Text("Forma de este widget", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                            listOf(
                                "rounded_square"  to "Redondeado",
                                "squircle"        to "Squircle (Pixel)",
                                "circle"          to "Círculo",
                                "square"          to "Cuadrado",
                                "hexagon"         to "Hexágono",
                                "rounded_hexagon" to "Hexágono redondeado",
                                "diamond"         to "Diamante",
                                "rounded_diamond" to "Diamante redondeado",
                                "badge"           to "Sello",
                            ).forEach { (value, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedShape = value
                                            showShapeSheet = false
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedShape == value, onClick = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                if (showBgColorSheet) {
                    ColorPickerSheet(
                        title = "Color de fondo del widget",
                        initialColor = selectedBgColor,
                        showAlpha = true,
                        onColorSelected = { color -> selectedBgColor = color },
                        onDismiss = { showBgColorSheet = false }
                    )
                }

                if (showIconColorSheet) {
                    ColorPickerSheet(
                        title = "Color del icono del widget",
                        initialColor = selectedIconColor,
                        showAlpha = false,
                        onColorSelected = { color -> selectedIconColor = color },
                        onDismiss = { showIconColorSheet = false }
                    )
                }
            }
        }
    }
}
