package com.qvacell.app.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.qvacell.app.data.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object DialService {
    fun hasCallPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    fun dial(context: Context, code: String, simSlot: Int? = null) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(code)))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        attachSimAccount(context, intent, simSlot)
        context.startActivity(intent)
    }

    fun dialDirect(context: Context, code: String, simSlot: Int? = null) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(code)))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        attachSimAccount(context, intent, simSlot)
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

    private fun attachSimAccount(context: Context, intent: Intent, explicitSlot: Int?) {
        val handle = resolvePhoneAccountHandle(context, explicitSlot) ?: return
        intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", handle)
    }

    private fun resolvePhoneAccountHandle(context: Context, explicitSlot: Int?): PhoneAccountHandle? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) return null

        val slot = explicitSlot ?: runBlocking {
            SettingsDataStore(context).selectedSimSlot.first()
        }
        if (slot < 0) return null

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE)
            as? TelecomManager ?: return null
        val accounts = try {
            telecomManager.callCapablePhoneAccounts
        } catch (_: SecurityException) {
            return null
        }
        if (accounts.size < 2) return null
        return accounts.getOrNull(slot)
    }
}
