package com.qvacell.app.service

import android.content.Context
import com.qvacell.app.data.QvacellDatabase
import com.qvacell.app.model.Reminder
import kotlinx.coroutines.flow.Flow

class ReminderRepository(context: Context) {
    private val dao = QvacellDatabase.get(context).reminderDao()

    fun observeAll(): Flow<List<Reminder>> = dao.observeAll()

    suspend fun getEnabled(): List<Reminder> = dao.getEnabled()

    suspend fun getById(id: String): Reminder? = dao.getById(id)

    suspend fun save(reminder: Reminder) = dao.upsert(reminder)

    suspend fun delete(reminder: Reminder) = dao.delete(reminder)

    suspend fun setEnabled(reminder: Reminder, enabled: Boolean) =
        dao.update(reminder.copy(isEnabled = enabled))
}
