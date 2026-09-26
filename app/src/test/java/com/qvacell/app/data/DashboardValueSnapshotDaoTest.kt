package com.qvacell.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.qvacell.app.model.DashboardValueSnapshot
import com.qvacell.app.model.SourceKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DashboardValueSnapshotDaoTest {
    private lateinit var db: QvacellDatabase
    private lateinit var dao: DashboardValueSnapshotDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), QvacellDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dashboardValueSnapshotDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `latest-per-field groups by MAX capturedAt not MAX id under out-of-order backfill`() = runTest {
        // Real-time capture (day 25) inserted first — lower id.
        dao.insert(
            DashboardValueSnapshot(
                fieldType = "main_balance", numericValue = 100.0, capturedAt = 25_000L,
                sourceSignalId = "ussd:main-balance", sourceKind = SourceKind.USSD_REAL
            )
        )
        // Backfilled SMS from an older day (10) arrives later — higher id, older timestamp.
        dao.insert(
            DashboardValueSnapshot(
                fieldType = "main_balance", numericValue = 50.0, capturedAt = 10_000L,
                sourceSignalId = "sms:etecsa-balance-alert", sourceKind = SourceKind.SMS_REAL
            )
        )

        val latest = dao.observeAllLatestPerField().first()
        assertEquals(100.0, latest.single { it.fieldType == "main_balance" }.numericValue)
    }

    @Test
    fun `history returns all rows in capturedAt order regardless of insertion order`() = runTest {
        dao.insert(
            DashboardValueSnapshot(
                fieldType = "voice_minutes_remaining", numericValue = 5.0, capturedAt = 3000L,
                sourceSignalId = "ussd:voice-balance", sourceKind = SourceKind.USSD_REAL
            )
        )
        dao.insert(
            DashboardValueSnapshot(
                fieldType = "voice_minutes_remaining", numericValue = 1.0, capturedAt = 1000L,
                sourceSignalId = "sms:etecsa-balance-alert", sourceKind = SourceKind.SMS_REAL
            )
        )
        dao.insert(
            DashboardValueSnapshot(
                fieldType = "voice_minutes_remaining", numericValue = 3.0, capturedAt = 2000L,
                sourceSignalId = "ussd:voice-balance", sourceKind = SourceKind.USSD_REAL
            )
        )

        val history = dao.observeHistory("voice_minutes_remaining").first()
        assertEquals(listOf(1000L, 2000L, 3000L), history.map { it.capturedAt })
    }

    @Test
    fun `different field types never mix in latest-per-field`() = runTest {
        dao.insert(
            DashboardValueSnapshot(
                fieldType = "main_balance", numericValue = 100.0, capturedAt = 1000L,
                sourceSignalId = "ussd:main-balance", sourceKind = SourceKind.USSD_REAL
            )
        )
        dao.insert(
            DashboardValueSnapshot(
                fieldType = "data_plan_gb", numericValue = 6.0, capturedAt = 500L,
                sourceSignalId = "ussd:data-plan", sourceKind = SourceKind.USSD_REAL
            )
        )

        val latest = dao.observeAllLatestPerField().first()
        assertEquals(2, latest.size)
    }
}
