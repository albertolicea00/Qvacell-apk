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
                Toast.makeText(context, "Concede el permiso de llamadas para marcar directamente", Toast.LENGTH_SHORT).show()
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
