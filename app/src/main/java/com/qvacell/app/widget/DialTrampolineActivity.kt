package com.qvacell.app.widget

import android.app.Activity
import android.os.Bundle
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.service.DialService

class DialTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val codeId = intent.getStringExtra(EXTRA_CODE_ID)
        if (codeId != null) {
            val repository = CatalogRepository(this)
            val code = repository.findCodeById(codeId)
            if (code != null) {
                val dialCode = code.resolvedCode()
                if (DialService.hasCallPermission(this)) {
                    DialService.dialDirect(this, dialCode)
                } else {
                    DialService.dial(this, dialCode)
                }
            }
        }
        finish()
    }

    companion object {
        const val EXTRA_CODE_ID = "code_id"
    }
}
