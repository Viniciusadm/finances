package com.archieapps.finances.core.session

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.crypto.Cipher
import kotlin.coroutines.resume

enum class BiometricAvailability { Ready, DeviceCredentialOnly, NoneEnrolled, Unavailable }

sealed interface AuthOutcome {
    data class Succeeded(val cipher: Cipher?) : AuthOutcome
    data object Canceled : AuthOutcome
    data class Lockout(val permanent: Boolean) : AuthOutcome
    data class Failed(val message: String) : AuthOutcome
}

class BiometricGate(private val context: Context) {
    private val manager = BiometricManager.from(context)

    fun availability(): BiometricAvailability {
        val strong = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

        return when (strong) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Ready
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                if (hasDeviceCredential()) {
                    BiometricAvailability.DeviceCredentialOnly
                } else {
                    BiometricAvailability.NoneEnrolled
                }
            }
            else -> if (hasDeviceCredential()) {
                BiometricAvailability.DeviceCredentialOnly
            } else {
                BiometricAvailability.Unavailable
            }
        }
    }

    fun hasDeviceCredential(): Boolean =
        manager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) ==
            BiometricManager.BIOMETRIC_SUCCESS

    val cryptoWithCredentialSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        cipher: Cipher? = null,
        allowDeviceCredential: Boolean = true,
    ): AuthOutcome = suspendCancellableCoroutine { continuation ->
        val useCredential = allowDeviceCredential &&
            hasDeviceCredential() &&
            (cipher == null || cryptoWithCredentialSupported)

        val authenticators = if (useCredential) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .setConfirmationRequired(false)
            .apply {
                if (!useCredential) {
                    setNegativeButtonText("Cancelar")
                }
            }
            .build()

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) {
                        continuation.resume(AuthOutcome.Succeeded(result.cryptoObject?.cipher))
                    }
                }

                override fun onAuthenticationError(code: Int, message: CharSequence) {
                    if (!continuation.isActive) {
                        return
                    }

                    val outcome = when (code) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED,
                        -> AuthOutcome.Canceled

                        BiometricPrompt.ERROR_LOCKOUT -> AuthOutcome.Lockout(false)
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> AuthOutcome.Lockout(true)
                        else -> AuthOutcome.Failed(message.toString())
                    }

                    continuation.resume(outcome)
                }
            },
        )

        continuation.invokeOnCancellation { prompt.cancelAuthentication() }

        if (cipher == null) {
            prompt.authenticate(info)
        } else {
            prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
        }
    }
}
