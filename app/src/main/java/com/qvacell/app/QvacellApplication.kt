package com.qvacell.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.qvacell.app.service.AppTelemetry
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.ui.theme.syncLaunchTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class QvacellApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        // Also migrates existing installs whose theme was only stored in DataStore.
        applicationScope.launch {
            SettingsDataStore(this@QvacellApplication).themeMode.distinctUntilChanged().collect {
                syncLaunchTheme(this@QvacellApplication, it)
            }
        }
        AppTelemetry.initialize(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel("reminders", "Recordatorios", NotificationManager.IMPORTANCE_HIGH)
        )
    }
}
