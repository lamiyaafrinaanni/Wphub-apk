package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SiteEntity
import com.example.data.remote.LiveVerificationStep
import com.example.data.remote.MultiStepConnectionResult
import com.example.data.remote.VerificationStepState
import com.example.data.repository.WPHubRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel dedicated to managing the multi-step onboarding connection flow:
 * 1. URL Validation (Host reachability, DNS & SSL/TLS)
 * 2. REST API Discovery (/wp-json/ endpoints, namespaces & permalink routing)
 * 3. Authentication Handshake (Application Passwords, user capabilities & scopes)
 *
 * Emits real-time state updates using the [ProgressState] sealed hierarchy.
 */
class ConnectionProgressViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: WPHubRepository = WPHubRepository(AppDatabase.getDatabase(application))
) : AndroidViewModel(application) {

    companion object {
        fun provideFactory(
            application: Application,
            repository: WPHubRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = repository ?: WPHubRepository(AppDatabase.getDatabase(application))
                return ConnectionProgressViewModel(application, repo) as T
            }
        }
    }

    private val _progressState = MutableStateFlow<ProgressState>(ProgressState.Idle())
    val progressState: StateFlow<ProgressState> = _progressState.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    private val _siteUrl = MutableStateFlow("https://")
    val siteUrl: StateFlow<String> = _siteUrl.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _rawLogs = MutableStateFlow<List<String>>(emptyList())
    val rawLogs: StateFlow<List<String>> = _rawLogs.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun updateSiteUrl(url: String) {
        _siteUrl.value = url
    }

    fun updateUsername(user: String) {
        _username.value = user
    }

    fun updatePassword(pass: String) {
        _password.value = pass
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun resetState() {
        _progressState.value = ProgressState.Idle()
        _isVerifying.value = false
        _rawLogs.value = emptyList()
    }

    /**
     * Executes the live 3-stage onboarding verification pipeline with real-time UI updates:
     * 1. URL & TLS/SSL Validation
     * 2. /wp-json/ REST API Discovery
     * 3. Basic Auth Application Password Handshake
     *
     * On success, optionally saves the site into Room and initiates full synchronization.
     */
    fun startVerificationAndConnection(
        siteUrl: String = _siteUrl.value,
        username: String = _username.value,
        appPasswordOrToken: String = _password.value,
        autoSaveOnSuccess: Boolean = true,
        onSuccess: (SiteEntity) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val cleanUrl = siteUrl.trim().let {
                if (!it.startsWith("http://") && !it.startsWith("https://")) "https://$it" else it
            }

            if (siteUrl.isBlank() || siteUrl.trim() == "https://" || siteUrl.trim() == "http://") {
                val error = "Please enter your WordPress site URL"
                _userMessage.value = error
                onError(error)
                return@launch
            }
            if (username.isBlank()) {
                val error = "Please enter your WordPress username"
                _userMessage.value = error
                onError(error)
                return@launch
            }
            if (appPasswordOrToken.isBlank()) {
                val error = "Please enter your WordPress Application Password"
                _userMessage.value = error
                onError(error)
                return@launch
            }

            _isVerifying.value = true
            val initialSteps = ProgressState.defaultSteps()
            val stepsMap = initialSteps.associateBy { it.stage }.toMutableMap()
            val logAccumulator = mutableListOf<String>()

            logAccumulator.add("[INIT] Initiating multi-step onboarding connection for $cleanUrl")
            logAccumulator.add("[AUTH] Authenticating user: $username")
            _rawLogs.value = logAccumulator.toList()

            // Emit initial Stage 1: URL Validation (In Progress)
            stepsMap[ConnectionStage.URL_VALIDATION] = StepProgress(
                stage = ConnectionStage.URL_VALIDATION,
                status = StageStatus.IN_PROGRESS,
                statusText = "Validating Host & TLS...",
                detailMessage = "Testing DNS resolution and SSL/TLS certificate reachability."
            )
            _progressState.value = ProgressState.UrlValidation(
                status = StageStatus.IN_PROGRESS,
                message = "Validating host URL syntax, DNS resolution, and SSL certificates...",
                steps = stepsMap.values.sortedBy { it.stage.stepIndex }
            )

            // Execute live pipeline via Repository with step updates
            val result = withContext(Dispatchers.IO) {
                repository.performLiveConnectionChecks(
                    siteUrl = cleanUrl,
                    username = username,
                    tokenOrPass = appPasswordOrToken,
                    onStepUpdate = { liveStep ->
                        val stage = ConnectionStage.fromIndex(liveStep.stepIndex)
                        val stepProgress = StepProgress.fromLiveStep(liveStep)
                        stepsMap[stage] = stepProgress

                        val currentSortedSteps = stepsMap.values.sortedBy { it.stage.stepIndex }

                        liveStep.subLogs.forEach { logItem ->
                            if (!logAccumulator.contains(logItem)) {
                                logAccumulator.add("[${stage.shortLabel}] $logItem")
                            }
                        }
                        _rawLogs.value = logAccumulator.toList()

                        // Update ProgressState sealed class based on active stage & status
                        when (stage) {
                            ConnectionStage.URL_VALIDATION -> {
                                when (stepProgress.status) {
                                    StageStatus.IN_PROGRESS, StageStatus.PENDING -> {
                                        _progressState.value = ProgressState.UrlValidation(
                                            status = StageStatus.IN_PROGRESS,
                                            message = "Validating host URL, DNS resolution, and TLS...",
                                            steps = currentSortedSteps,
                                            responseTimeMs = stepProgress.responseTimeMs
                                        )
                                    }
                                    StageStatus.SUCCESS -> {
                                        _progressState.value = ProgressState.UrlValidation(
                                            status = StageStatus.SUCCESS,
                                            message = "Host is reachable (${stepProgress.responseTimeMs ?: 0}ms)",
                                            steps = currentSortedSteps,
                                            responseTimeMs = stepProgress.responseTimeMs
                                        )
                                    }
                                    StageStatus.FAILURE -> {
                                        _progressState.value = ProgressState.Error(
                                            failedStage = ConnectionStage.URL_VALIDATION,
                                            errorMessage = stepProgress.detailMessage ?: "Host is unreachable",
                                            diagnosticAdvice = stepProgress.diagnosticAdvice,
                                            steps = currentSortedSteps
                                        )
                                    }
                                }
                            }
                            ConnectionStage.REST_DISCOVERY -> {
                                when (stepProgress.status) {
                                    StageStatus.IN_PROGRESS, StageStatus.PENDING -> {
                                        _progressState.value = ProgressState.RestDiscovery(
                                            status = StageStatus.IN_PROGRESS,
                                            message = "Discovering /wp-json/ endpoints and namespaces...",
                                            steps = currentSortedSteps,
                                            responseTimeMs = stepProgress.responseTimeMs
                                        )
                                    }
                                    StageStatus.SUCCESS -> {
                                        _progressState.value = ProgressState.RestDiscovery(
                                            status = StageStatus.SUCCESS,
                                            message = "WordPress REST API active & verified",
                                            siteName = "",
                                            steps = currentSortedSteps,
                                            responseTimeMs = stepProgress.responseTimeMs
                                        )
                                    }
                                    StageStatus.FAILURE -> {
                                        _progressState.value = ProgressState.Error(
                                            failedStage = ConnectionStage.REST_DISCOVERY,
                                            errorMessage = stepProgress.detailMessage ?: "REST API discovery failed",
                                            diagnosticAdvice = stepProgress.diagnosticAdvice,
                                            steps = currentSortedSteps
                                        )
                                    }
                                }
                            }
                            ConnectionStage.HANDSHAKE -> {
                                when (stepProgress.status) {
                                    StageStatus.IN_PROGRESS, StageStatus.PENDING -> {
                                        _progressState.value = ProgressState.Handshake(
                                            status = StageStatus.IN_PROGRESS,
                                            message = "Validating Application Password & scopes...",
                                            steps = currentSortedSteps,
                                            responseTimeMs = stepProgress.responseTimeMs
                                        )
                                    }
                                    StageStatus.SUCCESS -> {
                                        _progressState.value = ProgressState.Handshake(
                                            status = StageStatus.SUCCESS,
                                            message = "Credentials validated successfully",
                                            steps = currentSortedSteps,
                                            responseTimeMs = stepProgress.responseTimeMs
                                        )
                                    }
                                    StageStatus.FAILURE -> {
                                        _progressState.value = ProgressState.Error(
                                            failedStage = ConnectionStage.HANDSHAKE,
                                            errorMessage = stepProgress.detailMessage ?: "Authentication handshake failed",
                                            diagnosticAdvice = stepProgress.diagnosticAdvice,
                                            steps = currentSortedSteps
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }

            _isVerifying.value = false
            val finalSteps = stepsMap.values.sortedBy { it.stage.stepIndex }

            if (result.isSuccess) {
                logAccumulator.add("[SUCCESS] All 3 verification checks passed successfully!")
                _rawLogs.value = logAccumulator.toList()

                if (autoSaveOnSuccess) {
                    try {
                        logAccumulator.add("[SYNC] Saving site entity and synchronizing initial data...")
                        _rawLogs.value = logAccumulator.toList()

                        val site = withContext(Dispatchers.IO) {
                            repository.loginToWordPressSite(
                                siteId = null,
                                siteUrl = result.siteUrl,
                                siteName = result.siteName,
                                usernameOrEmail = username,
                                passwordOrToken = appPasswordOrToken,
                                role = result.userRole,
                                displayName = result.userDisplayName
                            )
                        }

                        val successState = ProgressState.Success(
                            site = site,
                            steps = finalSteps,
                            message = "Connected to ${result.siteName} as ${result.userDisplayName} (${result.userRole})"
                        )
                        _progressState.value = successState
                        _userMessage.value = "Connected to ${site.name} successfully!"
                        onSuccess(site)
                    } catch (e: Exception) {
                        val errorState = ProgressState.Error(
                            failedStage = ConnectionStage.HANDSHAKE,
                            errorMessage = "Saved connection but synchronization failed: ${e.message}",
                            diagnosticAdvice = "The credentials are valid. Check server timeout or REST rate limiting.",
                            steps = finalSteps
                        )
                        _progressState.value = errorState
                        _userMessage.value = errorState.errorMessage
                        onError(errorState.errorMessage)
                    }
                } else {
                    val placeholderSite = SiteEntity(
                        id = "site_tested_${System.currentTimeMillis()}",
                        name = result.siteName.ifBlank { "WordPress Site" },
                        url = result.siteUrl,
                        username = username,
                        userDisplayName = result.userDisplayName,
                        userRole = result.userRole,
                        isAuthenticated = true,
                        isCurrent = true
                    )
                    _progressState.value = ProgressState.Success(
                        site = placeholderSite,
                        steps = finalSteps,
                        message = "All 3 checks passed! Ready to connect."
                    )
                    _userMessage.value = "All 3 verification checks passed!"
                    onSuccess(placeholderSite)
                }
            } else {
                val failedStage = when {
                    finalSteps.getOrNull(0)?.status == StageStatus.FAILURE -> ConnectionStage.URL_VALIDATION
                    finalSteps.getOrNull(1)?.status == StageStatus.FAILURE -> ConnectionStage.REST_DISCOVERY
                    else -> ConnectionStage.HANDSHAKE
                }

                val errorState = ProgressState.Error(
                    failedStage = failedStage,
                    errorMessage = result.errorMessage ?: "Connection verification failed.",
                    diagnosticAdvice = result.diagnosticAdvice,
                    steps = finalSteps
                )
                _progressState.value = errorState
                _userMessage.value = errorState.errorMessage
                logAccumulator.add("[ERROR] Stage ${failedStage.shortLabel} failed: ${errorState.errorMessage}")
                _rawLogs.value = logAccumulator.toList()
                onError(errorState.errorMessage)
            }
        }
    }

    /**
     * Runs connection checks without saving into the persistent Room database.
     */
    fun testConnectionOnly(
        siteUrl: String = _siteUrl.value,
        username: String = _username.value,
        appPasswordOrToken: String = _password.value,
        onSuccess: (MultiStepConnectionResult) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        startVerificationAndConnection(
            siteUrl = siteUrl,
            username = username,
            appPasswordOrToken = appPasswordOrToken,
            autoSaveOnSuccess = false,
            onSuccess = { site ->
                onSuccess(
                    MultiStepConnectionResult(
                        isSuccess = true,
                        siteName = site.name,
                        siteUrl = site.url,
                        userDisplayName = site.userDisplayName,
                        userRole = site.userRole
                    )
                )
            },
            onError = onError
        )
    }}
