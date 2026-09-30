package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.SiteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WPMobile Hub", appName)
    }

    @Test
    fun `insert and retrieve site entity`() = runBlocking {
        val site = SiteEntity(
            id = "test_site",
            name = "Test WP Store",
            url = "https://test.local",
            iconEmoji = "🛒",
            isCurrent = true,
            totalSales = 12500.0,
            visitorsToday = 3400
        )
        db.siteDao().insertSite(site)

        val retrieved = db.siteDao().getCurrentSite().first()
        assertNotNull(retrieved)
        assertEquals("Test WP Store", retrieved?.name)
        assertEquals(12500.0, retrieved?.totalSales ?: 0.0, 0.001)
    }
}

