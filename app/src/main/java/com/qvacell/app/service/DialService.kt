package com.qvacell.app.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat

/**
 * Builds tel: URIs and dials. `dial()` always uses ACTION_DIAL (opens the system dialer, user
 * taps to confirm — no CALL_PHONE permission needed, matching iOS's tel:// UX); used everywhere
 * except Compras' own purchase confirmation flow (`CodeActionHandler`), which calls
 * `dialDirect()` — ACTION_CALL, places the call immediately with no dialer step — since that flow
 * already shows its own in-app confirmation sheet first.
 */
object DialService {
    fun hasCallPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    fun dial(context: Context, code: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(code)))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** ACTION_CALL — caller must have already confirmed CALL_PHONE is granted (see [hasCallPermission]). */
    fun dialDirect(context: Context, code: String) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(code)))
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
