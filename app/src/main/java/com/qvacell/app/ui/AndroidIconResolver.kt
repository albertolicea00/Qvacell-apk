package com.qvacell.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Resolves the `icon` string stored in codes.json (e.g. "Filled.Home",
 * "AutoMirrored.Filled.CallMade") to the matching `androidx.compose.material.icons` vector,
 * via reflection on the generated `<Name>Kt.get<Name>(receiver)` accessor. codes.json — not
 * this app — is the single place that picks which icon goes with which code; this is a
 * mechanical lookup, not a per-icon mapping table.
 */
fun resolveAndroidIcon(icon: String?): ImageVector {
    if (icon == null) return Icons.Filled.Circle
    return try {
        val segments = icon.split(".")
        val name = segments.last()
        val isAutoMirrored = segments.contains("AutoMirrored")
        val packageName = if (isAutoMirrored) {
            "androidx.compose.material.icons.automirrored.filled"
        } else {
            "androidx.compose.material.icons.filled"
        }
        val receiver: Any = if (isAutoMirrored) Icons.AutoMirrored.Filled else Icons.Filled
        val accessorClass = Class.forName("$packageName.${name}Kt")
        val method = accessorClass.getMethod("get$name", receiver.javaClass)
        method.invoke(null, receiver) as ImageVector
    } catch (e: ReflectiveOperationException) {
        Icons.Filled.Circle
    }
}
