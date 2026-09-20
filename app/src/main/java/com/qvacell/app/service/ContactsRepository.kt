package com.qvacell.app.service

import android.content.Context
import android.provider.ContactsContract
import com.qvacell.app.data.QvacellDatabase
import com.qvacell.app.model.CubanPhoneNumber
import com.qvacell.app.model.WrappedCaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactPhoneNumber(val raw: String, val normalized: String?)

data class DeviceContact(
    val id: String,
    val name: String,
    val numbers: List<ContactPhoneNumber>
) {
    val cubanNumbers: List<String> get() = numbers.mapNotNull { it.normalized }
}

class ContactsRepository(private val context: Context) {

    suspend fun loadContacts(): List<DeviceContact> = withContext(Dispatchers.IO) {
        val contacts = LinkedHashMap<String, MutableList<ContactPhoneNumber>>()
        val names = HashMap<String, String>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx) ?: continue
                val name = cursor.getString(nameIdx) ?: "Sin nombre"
                val number = cursor.getString(numberIdx) ?: continue

                names[id] = name
                val normalized = CubanPhoneNumber.normalize(number)
                contacts.getOrPut(id) { mutableListOf() }.add(ContactPhoneNumber(number, normalized))
            }
        }

        contacts.map { (id, numbers) ->
            DeviceContact(id = id, name = names[id] ?: "Sin nombre", numbers = numbers)
        }.filter { it.cubanNumbers.isNotEmpty() }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Rebuilds the wrapped_callers table from the given contacts so the CallScreeningService
     * can resolve *99 collect-call caller IDs. Call this after every [loadContacts] fetch,
     * mirroring the iOS app's ContactsService, which rebuilds its CallerIDStore list the same way.
     */
    suspend fun syncWrappedCallers(context: Context, contacts: List<DeviceContact>) = withContext(Dispatchers.IO) {
        val entries = contacts.flatMap { contact ->
            contact.cubanNumbers.mapNotNull { number ->
                WrappedCaller.wrappedNumber(number)?.let { WrappedCaller(it, contact.name) }
            }
        }
        val dao = QvacellDatabase.get(context).wrappedCallerDao()
        dao.clear()
        if (entries.isNotEmpty()) dao.upsertAll(entries)
    }
}
