package com.qvacell.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/** Resolves the icon names used by codes.json without reflection, so R8 keeps only these icons. */
fun resolveAndroidIcon(icon: String?): ImageVector = when (icon) {
    "AutoMirrored.Filled.CallMade" -> Icons.AutoMirrored.Filled.CallMade
    "AutoMirrored.Filled.EventNote" -> Icons.AutoMirrored.Filled.EventNote
    "Filled.AccountBalance" -> Icons.Filled.AccountBalance
    "Filled.AccountBalanceWallet" -> Icons.Filled.AccountBalanceWallet
    "Filled.Apartment" -> Icons.Filled.Apartment
    "Filled.Article" -> Icons.Filled.Article
    "Filled.AttachMoney" -> Icons.Filled.AttachMoney
    "Filled.Bolt" -> Icons.Filled.Bolt
    "Filled.Call" -> Icons.Filled.Call
    "Filled.CardGiftcard" -> Icons.Filled.CardGiftcard
    "Filled.CheckCircle" -> Icons.Filled.CheckCircle
    "Filled.CreditCard" -> Icons.Filled.CreditCard
    "Filled.DataUsage" -> Icons.Filled.DataUsage
    "Filled.ErrorOutline" -> Icons.Filled.ErrorOutline
    "Filled.Flight" -> Icons.Filled.Flight
    "Filled.Gavel" -> Icons.Filled.Gavel
    "Filled.Group" -> Icons.Filled.Group
    "Filled.Headset" -> Icons.Filled.Headset
    "Filled.Home" -> Icons.Filled.Home
    "Filled.Info" -> Icons.Filled.Info
    "Filled.Inventory2" -> Icons.Filled.Inventory2
    "Filled.LocalFireDepartment" -> Icons.Filled.LocalFireDepartment
    "Filled.LocalHospital" -> Icons.Filled.LocalHospital
    "Filled.LockReset" -> Icons.Filled.LockReset
    "Filled.Mail" -> Icons.Filled.Mail
    "Filled.MedicalServices" -> Icons.Filled.MedicalServices
    "Filled.MenuBook" -> Icons.Filled.MenuBook
    "Filled.NetworkCell" -> Icons.Filled.NetworkCell
    "Filled.Payments" -> Icons.Filled.Payments
    "Filled.People" -> Icons.Filled.People
    "Filled.PersonAdd" -> Icons.Filled.PersonAdd
    "Filled.PersonRemove" -> Icons.Filled.PersonRemove
    "Filled.Phone" -> Icons.Filled.Phone
    "Filled.PhoneDisabled" -> Icons.Filled.PhoneDisabled
    "Filled.PhoneInTalk" -> Icons.Filled.PhoneInTalk
    "Filled.Photo" -> Icons.Filled.Photo
    "Filled.Receipt" -> Icons.Filled.Receipt
    "Filled.Sailing" -> Icons.Filled.Sailing
    "Filled.Security" -> Icons.Filled.Security
    "Filled.Shield" -> Icons.Filled.Shield
    "Filled.ShoppingCart" -> Icons.Filled.ShoppingCart
    "Filled.Sms" -> Icons.Filled.Sms
    "Filled.SportsSoccer" -> Icons.Filled.SportsSoccer
    "Filled.Warning" -> Icons.Filled.Warning
    "Filled.Watch" -> Icons.Filled.Watch
    "Filled.WaterDrop" -> Icons.Filled.WaterDrop
    "Filled.Wifi" -> Icons.Filled.Wifi
    else -> Icons.Filled.Circle
}
