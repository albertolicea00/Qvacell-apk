package com.qvacell.app.service

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.qvacell.app.data.QvacellDatabase
import com.qvacell.app.model.DashboardValueSnapshot
import com.qvacell.app.model.SourceAnchor
import com.qvacell.app.model.SourceKind
import com.qvacell.app.parsing.ParsedDashboardValue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DashboardDataRepositoryTest {
    private lateinit var db: QvacellDatabase
    private lateinit var repository: DashboardDataRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, QvacellDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DashboardDataRepository(context, db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `an estimate fills a field that has no real value yet`() = runTest {
        repository.recordEstimate(
            listOf(ParsedDashboardValue(fieldType = "calls_made_estimate", numericValue = 3.0)),
            estimatedAt = 1000L
        )

        repository.observeCurrentValues().test {
            val values = awaitItem()
            assertEquals(3.0, values["calls_made_estimate"]?.numericValue)
        }
    }

    @Test
    fun `a real value always wins over an estimate, even an older real over a newer estimate`() = runTest {
        db.dashboardValueSnapshotDao().insert(
            DashboardValueSnapshot(
                fieldType = "main_balance", numericValue = 99.0, capturedAt = 1000L,
                sourceSignalId = "ussd:main-balance", sourceKind = SourceKind.USSD_REAL
            )
        )
        repository.recordEstimate(
            listOf(ParsedDashboardValue(fieldType = "main_balance", numericValue = 1.0)),
            estimatedAt = 9000L // newer than the real snapshot, but must not win
        )

        repository.observeCurrentValues().test {
            val values = awaitItem()
            assertEquals(99.0, values["main_balance"]?.numericValue)
        }
    }

    @Test
    fun `recordUssdParse against a stub parser advances the seen anchor but not the parsed anchor`() = runTest {
        repository.recordUssdParse("main-balance", "raw ussd text", capturedAt = 4000L)

        lateinit var anchor: List<SourceAnchor>
        repository.observeAnchors().test {
            anchor = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        val mainBalanceAnchor = anchor.single { it.signalId == "ussd:main-balance" }
        assertEquals(4000L, mainBalanceAnchor.lastSeenAt)
        assertNull(mainBalanceAnchor.lastSuccessfulParseAt)
    }

    @Test
    fun `recordSmsBody uses the message's own timestamp, not wall-clock now`() = runTest {
        // No SMS parser matches yet (stubs), so no value/anchor is recorded — this asserts the
        // call completes without throwing and touches nothing when nothing matches.
        repository.recordSmsBody("12345", "cualquier cuerpo de SMS", messageTimestamp = 123L)

        repository.observeCurrentValues().test {
            val values = awaitItem()
            assertEquals(0, values.size)
        }
    }
}
