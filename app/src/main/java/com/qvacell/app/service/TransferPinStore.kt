package com.qvacell.app.service

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Stores the user's ETECSA transfer PIN using EncryptedSharedPreferences. */
class TransferPinStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "qvacell_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(pin: String) {
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    fun load(): String? = prefs.getString(KEY_PIN, null)

    fun delete() {
        prefs.edit().remove(KEY_PIN).apply()
    }

    companion object {
        private const val KEY_PIN = "transfer_pin"
    }
}
