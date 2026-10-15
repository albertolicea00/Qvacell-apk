package com.qvacell.app.ui.components

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.R
import com.qvacell.app.service.DialService

/** Calls directly after CALL_PHONE is granted, without opening the dialer. */
@Composable
fun rememberDirectDialAction(onDialed: () -> Unit = {}): (String) -> Unit {
    val context = LocalContext.current
    var pendingCode by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingCode?.let { code ->
            if (granted) {
                DialService.dialDirect(context, code)
                onDialed()
            } else {
                Toast.makeText(context, context.getString(R.string.dial_permission_required), Toast.LENGTH_SHORT).show()
            }
        }
        pendingCode = null
    }
    return { code ->
        if (DialService.hasCallPermission(context)) {
            DialService.dialDirect(context, code)
            onDialed()
        } else {
            pendingCode = code
            permissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }
}

/** Applies the Contacts preference to *99 and hidden-number calls. */
@Composable
fun rememberContactCallAction(onDialed: () -> Unit = {}): (String) -> Unit {
    val context = LocalContext.current
    val settings = remember { SettingsDataStore(context) }
    val callMode by settings.contactCallMode.collectAsStateWithLifecycle(initialValue = "dialer")
    val dialDirect = rememberDirectDialAction(onDialed)
    return { code ->
        if (callMode == "direct") {
            dialDirect(code)
        } else {
            DialService.dial(context, code)
            onDialed()
        }
    }
}
