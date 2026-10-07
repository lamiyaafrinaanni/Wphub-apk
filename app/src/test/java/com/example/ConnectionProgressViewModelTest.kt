package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.WPHubRepository
import com.example.ui.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ConnectionProgressViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: WPHubRepository
    private lateinit var viewModel: ConnectionProgressViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WPHubRepository(db)
        viewModel = ConnectionProgressViewModel(app, repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `initial state is ProgressState Idle with 3 default steps`() {
        val state = viewModel.progressState.value
        assertTrue("Initial state must be Idle", state is ProgressState.Idle)
        assertEquals(3, state.steps.size)
        assertEquals(ConnectionStage.URL_VALIDATION, state.steps[0].stage)
        assertEquals(ConnectionStage.REST_DISCOVERY, state.steps[1].stage)
        assertEquals(ConnectionStage.HANDSHAKE, state.steps[2].stage)
        assertEquals(0f, state.progressFraction, 0.001f)
        assertFalse(state.isLoading)
        assertFalse(state.isComplete)
        assertFalse(state.isFailed)
        assertNull(state.currentStage)
    }

    @Test
    fun `constructor with Application only and provideFactory instantiate successfully`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vmFromApp = ConnectionProgressViewModel(app)
        assertNotNull(vmFromApp)
        val factory = ConnectionProgressViewModel.provideFactory(app)
        val vmFromFactory = factory.create(ConnectionProgressViewModel::class.java)
        assertNotNull(vmFromFactory)
    }

    @Test
    fun `input field updates update StateFlow values`() {
        viewModel.updateSiteUrl("https://mytestblog.com")
        viewModel.updateUsername("admin_tester")
        viewModel.updatePassword("abcd 1234 efgh 5678")

        assertEquals("https://mytestblog.com", viewModel.siteUrl.value)
        assertEquals("admin_tester", viewModel.username.value)
        assertEquals("abcd 1234 efgh 5678", viewModel.password.value)
    }

    @Test
    fun `resetState restores Idle and clears logs`() {
        viewModel.updateSiteUrl("https://site.com")
        viewModel.resetState()

        val state = viewModel.progressState.value
        assertTrue(state is ProgressState.Idle)
        assertFalse(viewModel.isVerifying.value)
        assertTrue(viewModel.rawLogs.value.isEmpty())
    }

    @Test
    fun `blank inputs fail immediately with error callback`() {
        var errorReceived: String? = null
        viewModel.startVerificationAndConnection(
            siteUrl = "",
            username = "",
            appPasswordOrToken = "",
            onError = { errorReceived = it }
        )

        assertNotNull(errorReceived)
        assertTrue(errorReceived?.contains("URL", ignoreCase = true) == true)
    }

    @Test
    fun `progressState progressFraction increases across stages`() {
        val defaultSteps = ProgressState.defaultSteps()

        val idle = ProgressState.Idle(defaultSteps)
        assertEquals(0f, idle.progressFraction, 0.01f)

        val stage1 = ProgressState.UrlValidation(
            status = StageStatus.IN_PROGRESS,
            steps = defaultSteps
        )
        assertTrue(stage1.progressFraction > 0.1f)
        assertTrue(stage1.isLoading)
        assertEquals(ConnectionStage.URL_VALIDATION, stage1.currentStage)

        val stage2 = ProgressState.RestDiscovery(
            status = StageStatus.IN_PROGRESS,
            steps = defaultSteps
        )
        assertTrue(stage2.progressFraction > stage1.progressFraction)
        assertTrue(stage2.isLoading)
        assertEquals(ConnectionStage.REST_DISCOVERY, stage2.currentStage)

        val stage3 = ProgressState.Handshake(
            status = StageStatus.IN_PROGRESS,
            steps = defaultSteps
        )
        assertTrue(stage3.progressFraction > stage2.progressFraction)
        assertTrue(stage3.isLoading)
        assertEquals(ConnectionStage.HANDSHAKE, stage3.currentStage)

        val successSteps = defaultSteps.map { it.copy(status = StageStatus.SUCCESS) }
        val success = ProgressState.Success(
            site = com.example.data.local.SiteEntity(id = "s1", name = "Test", url = "https://t.com"),
            steps = successSteps
        )
        assertEquals(1.0f, success.progressFraction, 0.001f)
        assertTrue(success.isComplete)
        assertFalse(success.isLoading)
    }
}
