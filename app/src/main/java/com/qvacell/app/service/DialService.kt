package com.qvacell.app.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat

/**
 * Builds tel: URIs and dials. Defaults to ACTION_DIAL (opens the system dialer, user taps to
 * confirm — no CALL_PHONE permission needed, matching iOS's tel:// UX). "Marcar Directamente" in
 * Ajustes › Identificador de Llamadas opts into ACTION_CALL instead (places the call immediately,
 * no confirmation step) — only takes effect once CALL_PHONE is actually granted; otherwise this
 * silently falls back to ACTION_DIAL rather than crashing on the missing permission.
 */
object DialService {
    private const val PREFS_NAME = "qvacell_dial_prefs"
    private const val KEY_DIRECT_DIAL = "direct_dial_enabled"

    fun isDirectDialEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_DIRECT_DIAL, false)

    fun setDirectDialEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DIRECT_DIAL, enabled)
            .apply()
    }

    private fun hasCallPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    fun dial(context: Context, code: String) {
        val action = if (isDirectDialEnabled(context) && hasCallPermission(context)) {
            Intent.ACTION_CALL
        } else {
            Intent.ACTION_DIAL
        }
        val intent = Intent(action, Uri.parse("tel:" + Uri.encode(code)))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun sendSms(context: Context, number: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number)))
        intent.putExtra("sms_body", body)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openMapsSearch(context: Context, query: String) {
        val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
