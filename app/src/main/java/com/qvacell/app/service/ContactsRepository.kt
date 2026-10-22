package com.qvacell.app.service

import android.content.Context
import android.provider.ContactsContract
import com.qvacell.app.R
import com.qvacell.app.model.CubanPhoneNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactPhoneNumber(val raw: String, val normalized: String?, val label: String = "")

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
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
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
            val typeIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx) ?: continue
                val name = cursor.getString(nameIdx) ?: context.getString(R.string.contacts_unnamed)
                val number = cursor.getString(numberIdx) ?: continue
                val typeInt = cursor.getInt(typeIdx)
                val customLabel = cursor.getString(labelIdx)

                names[id] = name
                val normalized = CubanPhoneNumber.normalize(number)
                val label = phoneTypeLabel(context, typeInt, customLabel)
                contacts.getOrPut(id) { mutableListOf() }.add(ContactPhoneNumber(number, normalized, label))
            }
        }

        contacts.map { (id, numbers) ->
            DeviceContact(id = id, name = names[id] ?: context.getString(R.string.contacts_unnamed), numbers = numbers)
        }.filter { it.cubanNumbers.isNotEmpty() }
            .sortedBy { it.name.lowercase() }
    }

    fun flattenToEntries(contacts: List<DeviceContact>): List<ContactEntry> =
        contacts.flatMap { contact ->
            val cuban = contact.numbers.filter { it.normalized != null }
            if (cuban.isEmpty()) return@flatMap emptyList()
            cuban.map { phone ->
                ContactEntry(
                    contact = contact,
                    number = phone.normalized!!,
                    label = if (cuban.size > 1) phone.label else null
                )
            }
        }
}

data class ContactEntry(
    val contact: DeviceContact,
    val number: String,
    val label: String?
)

private fun phoneTypeLabel(context: Context, type: Int, customLabel: String?): String = when (type) {
    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> context.getString(R.string.contacts_type_mobile)
    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> context.getString(R.string.contacts_type_home)
    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> context.getString(R.string.contacts_type_work)
    ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> context.getString(R.string.contacts_type_main)
    ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> context.getString(R.string.contacts_type_other)
    ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> customLabel ?: context.getString(R.string.contacts_type_other)
    ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> context.getString(R.string.contacts_type_fax_home)
    ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> context.getString(R.string.contacts_type_fax_work)
    ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> context.getString(R.string.contacts_type_pager)
    else -> context.getString(R.string.contacts_type_phone)
}
