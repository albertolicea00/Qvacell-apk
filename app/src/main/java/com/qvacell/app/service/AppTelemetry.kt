package com.qvacell.app.service

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.qvacell.app.BuildConfig

/** Firebase is inactive until app/google-services.json initializes a default Firebase app. */
object AppTelemetry {
    fun initialize(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        val distribution = if (BuildConfig.DASHBOARD_CAPTURE_ENABLED) "unlocked" else "store"
        FirebaseAnalytics.getInstance(context).setUserProperty("distribution", distribution)
        FirebaseCrashlytics.getInstance().setCustomKey("distribution", distribution)
    }

    fun screen(context: Context, name: String) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        val parameters = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, name)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
        }
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, parameters)
    }
}
