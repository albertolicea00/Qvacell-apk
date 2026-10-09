package com.qvacell.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Color
import com.qvacell.app.ui.screens.getWidgetShape
import com.qvacell.app.widget.WidgetSettings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.telephony.SubscriptionInfo
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.model.UssdCode
import com.qvacell.app.service.SimUtils
import com.qvacell.app.ui.resolveAndroidIcon
import com.qvacell.app.ui.theme.QvacellTheme

class QuickActionsWidgetConfigure : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(RESULT_CANCELED)

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val catalog = CatalogRepository(this).loadCatalog()

        data class ListItem(val categoryHeader: String? = null, val code: UssdCode? = null)

        val listItems = buildList {
            catalog.categories
                .filter { it.id != "sms" }
                .forEach { cat ->
                    val codes = cat.groups.flatMap { it.codes }
                    if (codes.isNotEmpty()) {
                        add(ListItem(categoryHeader = cat.name.value))
                        codes.forEach { add(ListItem(code = it)) }
                    }
                }
        }

        val initialCodeId = WidgetPrefs.getCodeId(this, widgetId)

        setContent {
            QvacellTheme {
                val widgetBgColor = remember { Color(WidgetSettings.getBackgroundColor(this@QuickActionsWidgetConfigure)) }
                val widgetIconColor = remember { Color(WidgetSettings.getIconColor(this@QuickActionsWidgetConfigure)) }
                val widgetIconShapeName = remember { WidgetSettings.getIconShape(this@QuickActionsWidgetConfigure) }
                val widgetShape = remember(widgetIconShapeName) { getWidgetShape(widgetIconShapeName) }

                var selectedCodeId by remember { mutableStateOf(initialCodeId) }
                var searchActive by remember { mutableStateOf(false) }
                var query by remember { mutableStateOf("") }
                val focusRequester = remember { FocusRequester() }

                val activeSims = remember { SimUtils.getActiveSubscriptions(this@QuickActionsWidgetConfigure) }
                val initialSimSlot = remember { WidgetPrefs.getSimSlot(this@QuickActionsWidgetConfigure, widgetId) }
                var showSimSheet by remember { mutableStateOf(false) }
                var pendingCodeId by remember { mutableStateOf<String?>(null) }

                fun finishWithCode(codeId: String) {
                    WidgetPrefs.saveCodeId(this@QuickActionsWidgetConfigure, widgetId, codeId)
                    val manager = AppWidgetManager.getInstance(this@QuickActionsWidgetConfigure)
                    QuickActionsWidget.updateWidget(this@QuickActionsWidgetConfigure, manager, widgetId)
                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                    finish()
                }

                fun onCodeSelected(codeId: String) {
                    if (activeSims.size >= 2) {
                        pendingCodeId = codeId
                        WidgetPrefs.saveCodeId(this@QuickActionsWidgetConfigure, widgetId, codeId)
                        showSimSheet = true
                    } else {
                        finishWithCode(codeId)
                    }
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
                    }.let { filtered ->
                        // drop orphan category headers (no codes after them)
                        val result = mutableListOf<ListItem>()
                        filtered.forEachIndexed { i, item ->
                            if (item.categoryHeader != null) {
                                val hasCodesAfter = filtered.drop(i + 1).any { it.code != null }
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
                        if (searchActive) {
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
                            androidx.compose.runtime.LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                        } else {
                            TopAppBar(
                                title = { Text("Seleccionar acción") },
                                actions = {
                                    IconButton(onClick = { searchActive = true }) {
                                        Icon(Icons.Filled.Search, contentDescription = "Buscar")
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        if (!searchActive) {
                            item {
                                Text(
                                    text = "Elige la operación que se ejecutará al tocar el widget.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }

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
                                item(key = code.id) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCodeId = code.id
                                                onCodeSelected(code.id)
                                             }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Widget-styled shape container
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(widgetShape)
                                                .background(widgetBgColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = resolveAndroidIcon(code.icon),
                                                contentDescription = null,
                                                tint = widgetIconColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        // Title + description + dial code
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
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (showSimSheet) {
                    ModalBottomSheet(onDismissRequest = {
                        showSimSheet = false
                        val codeId = pendingCodeId ?: return@ModalBottomSheet
                        finishWithCode(codeId)
                    }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "SIM para este widget",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        WidgetPrefs.saveSimSlot(this@QuickActionsWidgetConfigure, widgetId, -1)
                                        showSimSheet = false
                                        finishWithCode(pendingCodeId ?: return@clickable)
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = initialSimSlot < 0, onClick = null)
                                Icon(
                                    Icons.Filled.SimCard,
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 8.dp).size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("Predeterminada (ajuste global)", modifier = Modifier.padding(start = 12.dp))
                            }
                            activeSims.forEachIndexed { _, sim ->
                                val bitmap = remember(sim.subscriptionId) {
                                    try { sim.createIconBitmap(this@QuickActionsWidgetConfigure) } catch (_: Exception) { null }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            WidgetPrefs.saveSimSlot(this@QuickActionsWidgetConfigure, widgetId, sim.simSlotIndex)
                                            showSimSheet = false
                                            finishWithCode(pendingCodeId ?: return@clickable)
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = initialSimSlot == sim.simSlotIndex, onClick = null)
                                    Icon(
                                        Icons.Filled.SimCard,
                                        contentDescription = null,
                                        modifier = Modifier.padding(start = 8.dp).size(28.dp),
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
