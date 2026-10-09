package com.qvacell.app.widget

import android.app.Activity
import android.os.Bundle
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.service.DialService

class DialTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val codeId = intent.getStringExtra(EXTRA_CODE_ID)
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
        if (codeId != null) {
            val repository = CatalogRepository(this)
            val code = repository.findCodeById(codeId)
            if (code != null) {
                val dialCode = code.resolvedCode()
                val simSlot = if (widgetId >= 0) {
                    val slot = WidgetPrefs.getSimSlot(this, widgetId)
                    if (slot >= 0) slot else null
                } else null
                if (DialService.hasCallPermission(this)) {
                    DialService.dialDirect(this, dialCode, simSlot)
                } else {
                    DialService.dial(this, dialCode, simSlot)
                }
            }
        }
        finish()
    }

    companion object {
        const val EXTRA_CODE_ID = "code_id"
        const val EXTRA_WIDGET_ID = "widget_id"
    }
}
