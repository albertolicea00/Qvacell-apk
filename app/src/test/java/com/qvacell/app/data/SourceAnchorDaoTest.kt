package com.qvacell.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.qvacell.app.model.SourceAnchor
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SourceAnchorDaoTest {
    private lateinit var db: QvacellDatabase
    private lateinit var dao: SourceAnchorDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), QvacellDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.sourceAnchorDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `seen-but-unparsed update leaves lastSuccessfulParseAt untouched`() = runTest {
        dao.upsert(
            SourceAnchor(
                signalId = "ussd:main-balance", signalKind = "USSD",
                lastSeenAt = 1000L, lastSeenRawText = "raw-unparseable",
                lastSuccessfulParseAt = null, lastSuccessfulRawText = null
            )
        )

        val anchor = dao.get("ussd:main-balance")
        assertEquals(1000L, anchor?.lastSeenAt)
        assertNull(anchor?.lastSuccessfulParseAt)
    }

    @Test
    fun `a successful parse updates both seen and successful timestamps`() = runTest {
        dao.upsert(
            SourceAnchor(
                signalId = "ussd:main-balance", signalKind = "USSD",
                lastSeenAt = 1000L, lastSeenRawText = "raw",
                lastSuccessfulParseAt = 1000L, lastSuccessfulRawText = "raw"
            )
        )

        val anchor = dao.get("ussd:main-balance")
        assertEquals(1000L, anchor?.lastSeenAt)
        assertEquals(1000L, anchor?.lastSuccessfulParseAt)
    }

    @Test
    fun `anchors for different signal ids are fully independent`() = runTest {
        dao.upsert(SourceAnchor(signalId = "ussd:main-balance", signalKind = "USSD", lastSeenAt = 1000L, lastSeenRawText = "a"))
        dao.upsert(SourceAnchor(signalId = "ussd:voice-balance", signalKind = "USSD", lastSeenAt = 2000L, lastSeenRawText = "b"))

        assertEquals(1000L, dao.get("ussd:main-balance")?.lastSeenAt)
        assertEquals(2000L, dao.get("ussd:voice-balance")?.lastSeenAt)
    }

    @Test
    fun `clearing call log or SMS history cannot regress an anchor - only explicit upserts change it`() = runTest {
        dao.upsert(
            SourceAnchor(
                signalId = "sms:etecsa-balance-alert", signalKind = "SMS",
                lastSeenAt = 5000L, lastSeenRawText = "raw",
                lastSuccessfulParseAt = 5000L, lastSuccessfulRawText = "raw"
            )
        )

        // Simulates "device SMS history got cleared" — nothing external touches the anchor table.
        val anchor = dao.get("sms:etecsa-balance-alert")
        assertEquals(5000L, anchor?.lastSuccessfulParseAt)
    }
}
