package com.qvacell.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.qvacell.app.service.AppTelemetry

class QvacellApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppTelemetry.initialize(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel("reminders", "Recordatorios", NotificationManager.IMPORTANCE_HIGH)
        )
        manager.createNotificationChannel(
            NotificationChannel("caller_id", "Identificador de Llamadas", NotificationManager.IMPORTANCE_HIGH)
        )
    }
}
