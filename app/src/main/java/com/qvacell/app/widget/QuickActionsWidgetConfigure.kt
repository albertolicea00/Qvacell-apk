package com.qvacell.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.model.UssdCode
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

        // Flat list of (categoryName, code) with category headers interleaved as null codes
        data class ListItem(val categoryHeader: String? = null, val code: UssdCode? = null)

        val listItems = buildList {
            catalog.categories.forEach { cat ->
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
                var selectedCodeId by remember { mutableStateOf(initialCodeId) }

                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Seleccionar acción") })
                    }
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        listItems.forEachIndexed { index, item ->
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
                                                WidgetPrefs.saveCodeId(
                                                    this@QuickActionsWidgetConfigure,
                                                    widgetId,
                                                    code.id
                                                )
                                                val manager = AppWidgetManager.getInstance(
                                                    this@QuickActionsWidgetConfigure
                                                )
                                                QuickActionsWidget.updateWidget(
                                                    this@QuickActionsWidgetConfigure,
                                                    manager,
                                                    widgetId
                                                )
                                                setResult(
                                                    RESULT_OK,
                                                    Intent().putExtra(
                                                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                                                        widgetId
                                                    )
                                                )
                                                finish()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = code.id == selectedCodeId,
                                            onClick = null
                                        )
                                        Text(
                                            text = code.title.value,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp)
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
