package com.example.ui

import com.example.data.local.SiteEntity
import com.example.data.remote.LiveVerificationStep
import com.example.data.remote.VerificationStepState

/**
 * Onboarding stages for establishing and verifying a remote WordPress connection.
 */
enum class ConnectionStage(
    val stepIndex: Int,
    val title: String,
    val shortLabel: String,
    val description: String
) {
    URL_VALIDATION(
        stepIndex = 1,
        title = "URL Validation",
        shortLabel = "1. URL",
        description = "Validates host URL syntax, DNS resolution, and SSL/TLS certificate reachability."
    ),
    REST_DISCOVERY(
        stepIndex = 2,
        title = "REST API Discovery",
        shortLabel = "2. REST API",
        description = "Discovers /wp-json/ route, registered namespaces, permalinks, and WooCommerce API."
    ),
    HANDSHAKE(
        stepIndex = 3,
        title = "Authentication Handshake",
        shortLabel = "3. Handshake",
        description = "Validates Application Password credentials, user identity, roles, and CRUD scopes."
    );

    companion object {
        fun fromIndex(index: Int): ConnectionStage = when (index) {
            1 -> URL_VALIDATION
            2 -> REST_DISCOVERY
            3 -> HANDSHAKE
            else -> URL_VALIDATION
        }
    }
}

/**
 * Real-time execution status for each onboarding stage.
 */
enum class StageStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAILURE
}

/**
 * Snapshot model of an individual step in the multi-step progress pipeline.
 */
data class StepProgress(
    val stage: ConnectionStage,
    val status: StageStatus = StageStatus.PENDING,
    val statusText: String? = null,
    val responseTimeMs: Long? = null,
    val detailMessage: String? = null,
    val diagnosticAdvice: String? = null,
    val logs: List<String> = emptyList()
) {
    val isSuccess: Boolean get() = status == StageStatus.SUCCESS
    val isFailure: Boolean get() = status == StageStatus.FAILURE
    val isInProgress: Boolean get() = status == StageStatus.IN_PROGRESS
    val isPending: Boolean get() = status == StageStatus.PENDING

    companion object {
        fun fromLiveStep(live: LiveVerificationStep): StepProgress {
            val stage = ConnectionStage.fromIndex(live.stepIndex)
            val status = when (live.state) {
                VerificationStepState.IDLE -> StageStatus.PENDING
                VerificationStepState.IN_PROGRESS -> StageStatus.IN_PROGRESS
                VerificationStepState.SUCCESS -> StageStatus.SUCCESS
                VerificationStepState.FAILURE -> StageStatus.FAILURE
            }
            return StepProgress(
                stage = stage,
                status = status,
                statusText = live.statusText,
                responseTimeMs = live.responseTimeMs,
                detailMessage = live.detailMessage,
                diagnosticAdvice = live.diagnosticAdvice,
                logs = live.subLogs
            )
        }
    }
}

/**
 * Sealed class hierarchy modeling real-time UI states for the onboarding connection pipeline.
 */
sealed class ProgressState {
    abstract val steps: List<StepProgress>
    abstract val progressFraction: Float

    /**
     * Initial idle state before validation is triggered.
     */
    data class Idle(
        override val steps: List<StepProgress> = defaultSteps()
    ) : ProgressState() {
        override val progressFraction: Float = 0f
    }

    /**
     * Stage 1: URL & host reachability check is running or just finished.
     */
    data class UrlValidation(
        val status: StageStatus = StageStatus.IN_PROGRESS,
        val message: String = "Validating host URL syntax, DNS resolution, and SSL certificates...",
        override val steps: List<StepProgress>,
        val responseTimeMs: Long? = null
    ) : ProgressState() {
        override val progressFraction: Float
            get() = when (status) {
                StageStatus.SUCCESS -> 0.33f
                StageStatus.FAILURE -> 0.15f
                else -> 0.18f
            }
    }

    /**
     * Stage 2: REST API discovery is querying endpoints and namespaces.
     */
    data class RestDiscovery(
        val status: StageStatus = StageStatus.IN_PROGRESS,
        val message: String = "Discovering WordPress REST API endpoints and namespaces...",
        val siteName: String = "",
        val hasWooCommerce: Boolean = false,
        override val steps: List<StepProgress>,
        val responseTimeMs: Long? = null
    ) : ProgressState() {
        override val progressFraction: Float
            get() = when (status) {
                StageStatus.SUCCESS -> 0.66f
                StageStatus.FAILURE -> 0.45f
                else -> 0.50f
            }
    }

    /**
     * Stage 3: Authentication handshake validates application credentials and user roles.
     */
    data class Handshake(
        val status: StageStatus = StageStatus.IN_PROGRESS,
        val message: String = "Validating Application Password credentials and user capabilities...",
        val userDisplayName: String = "",
        val userRole: String = "",
        override val steps: List<StepProgress>,
        val responseTimeMs: Long? = null
    ) : ProgressState() {
        override val progressFraction: Float
            get() = when (status) {
                StageStatus.SUCCESS -> 1.0f
                StageStatus.FAILURE -> 0.78f
                else -> 0.82f
            }
    }

    /**
     * Pipeline completed successfully: all 3 checks succeeded and site entity is connected.
     */
    data class Success(
        val site: SiteEntity,
        override val steps: List<StepProgress>,
        val message: String = "All 3 verification checks passed! Site synchronized."
    ) : ProgressState() {
        override val progressFraction: Float = 1.0f
    }

    /**
     * Connection verification encountered a blocking issue at one of the stages.
     */
    data class Error(
        val failedStage: ConnectionStage,
        val errorMessage: String,
        val diagnosticAdvice: String? = null,
        override val steps: List<StepProgress>
    ) : ProgressState() {
        override val progressFraction: Float
            get() {
                val completed = steps.count { it.status == StageStatus.SUCCESS }
                return (completed.toFloat() / 3f).coerceIn(0.1f, 0.9f)
            }
    }

    // Convenience properties for UI rendering
    val isLoading: Boolean
        get() = (this is UrlValidation && status == StageStatus.IN_PROGRESS) ||
                (this is RestDiscovery && status == StageStatus.IN_PROGRESS) ||
                (this is Handshake && status == StageStatus.IN_PROGRESS)

    val isComplete: Boolean
        get() = this is Success

    val isFailed: Boolean
        get() = this is Error

    val currentStage: ConnectionStage?
        get() = when (this) {
            is Idle -> null
            is UrlValidation -> ConnectionStage.URL_VALIDATION
            is RestDiscovery -> ConnectionStage.REST_DISCOVERY
            is Handshake -> ConnectionStage.HANDSHAKE
            is Success -> null
            is Error -> failedStage
        }

    val statusSummary: String
        get() = when (this) {
            is Idle -> "Enter WordPress site URL and credentials to begin live verification."
            is UrlValidation -> message
            is RestDiscovery -> message
            is Handshake -> message
            is Success -> message
            is Error -> errorMessage
        }

    companion object {
        fun defaultSteps(): List<StepProgress> = listOf(
            StepProgress(
                stage = ConnectionStage.URL_VALIDATION,
                status = StageStatus.PENDING,
                detailMessage = "URL format, DNS resolution, and SSL/TLS certificate reachability."
            ),
            StepProgress(
                stage = ConnectionStage.REST_DISCOVERY,
                status = StageStatus.PENDING,
                detailMessage = "Discover /wp-json/ endpoints, registered namespaces, and WooCommerce API."
            ),
            StepProgress(
                stage = ConnectionStage.HANDSHAKE,
                status = StageStatus.PENDING,
                detailMessage = "Validate Application Password credentials, roles, and REST permissions."
            )
        )
    }
}
