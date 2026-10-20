package com.qvacell.app.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContactsRepositoryTest {
    private val repository = ContactsRepository(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun flattenToEntriesCreatesOneRowPerValidCubanNumberAndKeepsLabelsOnlyForMultipleNumbers() {
        val oneNumber = DeviceContact(
            id = "1", name = "Ana",
            numbers = listOf(ContactPhoneNumber("51234567", "51234567", "Móvil"))
        )
        val multipleNumbers = DeviceContact(
            id = "2", name = "Luis",
            numbers = listOf(
                ContactPhoneNumber("61234567", "61234567", "Casa"),
                ContactPhoneNumber("+53 5123 4567", "51234567", "Trabajo"),
                ContactPhoneNumber("12345678", null, "Otro")
            )
        )

        val entries = repository.flattenToEntries(listOf(oneNumber, multipleNumbers))

        assertEquals(listOf("51234567", "61234567", "51234567"), entries.map { it.number })
        assertEquals(listOf(null, "Casa", "Trabajo"), entries.map { it.label })
        assertEquals(listOf("Ana", "Luis", "Luis"), entries.map { it.contact.name })
    }

    @Test
    fun flattenToEntriesDropsContactsWithoutValidCubanNumbers() {
        val contact = DeviceContact(
            id = "3", name = "No mobile",
            numbers = listOf(ContactPhoneNumber("12345678", null, "Casa"))
        )

        assertEquals(emptyList<ContactEntry>(), repository.flattenToEntries(listOf(contact)))
    }
}
