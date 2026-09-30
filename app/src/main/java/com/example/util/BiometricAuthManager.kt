package com.example.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricStatus {
    AVAILABLE,
    NO_HARDWARE,
    HW_UNAVAILABLE,
    NONE_ENROLLED,
    UNSUPPORTED
}

/**
 * BiometricPrompt utility to secure saved WordPress credentials, application passwords,
 * and administrative REST tokens across the application.
 */
object BiometricAuthManager {

    /**
     * Check if the device supports Biometric authentication or Device Passcode / PIN.
     */
    fun checkBiometricAvailability(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HW_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            else -> BiometricStatus.UNSUPPORTED
        }
    }

    /**
     * Launch the system BiometricPrompt modal to verify user identity before
     * unmasking or revealing saved WordPress Application Passwords / Tokens.
     */
    fun authenticateToAccessCredentials(
        activity: FragmentActivity,
        title: String = "Authenticate to Access Credentials",
        subtitle: String = "Verify fingerprint, face, or device PIN to reveal WordPress passwords",
        description: String = "Securing saved WordPress Application Passwords and REST tokens.",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val status = checkBiometricAvailability(activity)
        if (status == BiometricStatus.NO_HARDWARE) {
            // Hardware doesn't support biometrics, allow fallback or direct access after warning
            onError("Biometric hardware not present on this device.")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError("Authentication cancelled.")
                } else {
                    onError("Biometric error ($errorCode): $errString")
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Biometric verification failed. Please try again.")
            }
        }

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)

        // On Android 10+ (API 29+), DEVICE_CREDENTIAL can be used with BIOMETRIC_STRONG.
        // If device credentials are allowed, negative button MUST NOT be set.
        try {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        } catch (e: Exception) {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG)
            promptInfoBuilder.setNegativeButtonText("Cancel")
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        try {
            biometricPrompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            onError("Could not launch BiometricPrompt: ${e.message}")
        }
    }
}
