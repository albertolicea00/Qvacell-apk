package com.qvacell.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Manual mapping from the SF Symbol names used in codes.json to a comparable
 * Material icon. Falls back to a generic circle icon when no mapping exists.
 */
fun sfSymbolToMaterialIcon(name: String?): ImageVector = when (name) {
    "house.fill" -> Icons.Filled.Home
    "cart" -> Icons.Filled.ShoppingCart
    "flipphone" -> Icons.Filled.Phone
    "envelope.badge" -> Icons.Filled.Sms
    "phone.fill" -> Icons.Filled.Call
    "creditcard" -> Icons.Filled.CreditCard
    "wifi" -> Icons.Filled.Wifi
    "antenna.radiowaves.left.and.right" -> Icons.Filled.NetworkCell
    "checkmark.circle.fill" -> Icons.Filled.CheckCircle
    "photo.on.rectangle.angled" -> Icons.Filled.Photo
    "recordingtape" -> Icons.Filled.Watch
    "headphones" -> Icons.Filled.Headset
    "pills.fill" -> Icons.Filled.MedicalServices
    "cross.case.fill" -> Icons.Filled.LocalHospital
    "flame.fill", "flame" -> Icons.Filled.LocalFireDepartment
    "shield.fill" -> Icons.Filled.Security
    "sailboat.fill" -> Icons.Filled.Sailing
    "bolt.fill" -> Icons.Filled.Bolt
    "drop.fill" -> Icons.Filled.WaterDrop
    "phone.arrow.up.right" -> Icons.Filled.CallMade
    "info.circle.fill" -> Icons.Filled.Info
    "phone.down.fill" -> Icons.Filled.PhoneDisabled
    "building.2.fill" -> Icons.Filled.Apartment
    "clock.fill" -> Icons.Filled.EventNote
    "person.2.wave.2.fill" -> Icons.Filled.People
    "exclamationmark.bubble.fill" -> Icons.Filled.ErrorOutline
    "building.columns.fill" -> Icons.Filled.Gavel
    "envelope.fill" -> Icons.Filled.Mail
    else -> Icons.Filled.Circle
}
