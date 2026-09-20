package com.qvacell.app.service

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Builds tel: URIs and opens the system dialer. Uses ACTION_DIAL (not ACTION_CALL)
 * so no CALL_PHONE permission is needed and the user must confirm in the system
 * dialer — matching iOS's tel:// UX where the OS always asks for confirmation
 * before dialing.
 */
object DialService {
    fun dial(context: Context, code: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(code)))
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
