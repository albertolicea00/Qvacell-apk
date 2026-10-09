package com.qvacell.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReminderRecurrence(val label: String) {
    NONE("Una vez"),
    DAILY("Cada día"),
    WEEKLY("Cada semana"),
    MONTHLY("Cada mes"),
    CUSTOM("Personalizado");
}

data class ReminderTemplate(
    val key: String,
    val title: String,
    val message: String,
    val iconName: String,
    val ussdCodeId: String?,
    val needsPhoneNumber: Boolean,
    val defaultRecurrence: ReminderRecurrence
) {
    companion object {
        val quickTemplates = listOf(
            ReminderTemplate("purchase-package", "Comprar Paquete", "Recuerda comprar tu paquete de datos, voz o SMS.", "shippingbox.fill", null, false, ReminderRecurrence.MONTHLY),
            ReminderTemplate("transfer-direct", "Hacer Transferencia", "Recuerda hacer tu transferencia de saldo.", "arrow.left.arrow.right", "transfer-direct", true, ReminderRecurrence.NONE),
            ReminderTemplate("recharge-card", "Recargar Saldo", "Recuerda recargar tu saldo con una tarjeta.", "creditcard.fill", "recharge-card", false, ReminderRecurrence.NONE),
        )
        val custom = ReminderTemplate("custom", "Personalizado", "", "bell.fill", null, false, ReminderRecurrence.NONE)

        fun forKey(key: String?): ReminderTemplate =
            quickTemplates.find { it.key == key } ?: custom
    }
}

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val iconName: String,
    val ussdCodeId: String?,
    val phoneNumber: String,
    val date: Long,
    val recurrence: ReminderRecurrence,
    val customIntervalDays: Int,
    val isEnabled: Boolean,
    val templateKey: String?
)
