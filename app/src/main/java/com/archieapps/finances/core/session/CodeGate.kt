package com.archieapps.finances.core.session

import androidx.fragment.app.FragmentActivity
import com.archieapps.finances.data.repo.CodeCheck
import com.archieapps.finances.data.repo.FinanceRepository

sealed interface UnlockResult {
    data object Unlocked : UnlockResult
    data object NeedsCode : UnlockResult
    data object Canceled : UnlockResult
    data class Blocked(val message: String) : UnlockResult
}

sealed interface CodeSubmitResult {
    data object Stored : CodeSubmitResult
    data class Rejected(val message: String, val remaining: Int) : CodeSubmitResult
    data class Failed(val message: String) : CodeSubmitResult
    data object HardStopped : CodeSubmitResult
}

class CodeGate(
    private val vault: CodeVault,
    private val repository: FinanceRepository,
    private val biometrics: BiometricGate
) {
    val budget = vault.budget

    fun currentCode(): String? = vault.currentCode()

    val unlocked: Boolean get() = vault.unlocked

    suspend fun hasStoredCodeForToday(): Boolean = vault.storedValidForToday()

    fun availability(): BiometricAvailability = biometrics.availability()

    suspend fun unlock(activity: FragmentActivity): UnlockResult {
        val availability = biometrics.availability()

        if (availability == BiometricAvailability.NoneEnrolled ||
            availability == BiometricAvailability.Unavailable
        ) {
            vault.markMemoryOnly()

            return if (vault.unlocked) UnlockResult.Unlocked else UnlockResult.NeedsCode
        }

        if (!vault.storedValidForToday()) {
            return UnlockResult.NeedsCode
        }

        val cipher = vault.decryptCipher() ?: return UnlockResult.NeedsCode

        return when (val outcome = biometrics.authenticate(
            activity = activity,
            title = "Abrir Finanças",
            subtitle = "Confirme para usar o código de hoje",
            cipher = cipher
        )) {
            is AuthOutcome.Succeeded -> {
                val opened = outcome.cipher?.let { vault.openWith(it) } ?: false

                if (opened) UnlockResult.Unlocked else UnlockResult.NeedsCode
            }

            AuthOutcome.Canceled -> UnlockResult.Canceled

            is AuthOutcome.Lockout -> {
                vault.markMemoryOnly()

                UnlockResult.Blocked(
                    if (outcome.permanent) {
                        "Biometria bloqueada. Use o PIN do aparelho."
                    } else {
                        "Biometria bloqueada por 30 segundos."
                    }
                )
            }

            is AuthOutcome.Failed -> UnlockResult.Blocked(outcome.message)
        }
    }

    suspend fun submitCode(activity: FragmentActivity, code: String): CodeSubmitResult {
        val trimmed = code.trim()

        if (!DayCode.isWellFormed(trimmed)) {
            return CodeSubmitResult.Failed("O código tem ${DayCode.LENGTH} dígitos.")
        }

        if (vault.budget.value.hardStopped) {
            return CodeSubmitResult.HardStopped
        }

        return when (val result = repository.validateCode(trimmed)) {
            CodeCheck.Accepted -> {
                vault.onValidated()
                persist(activity, trimmed)

                CodeSubmitResult.Stored
            }

            is CodeCheck.Rejected -> {
                vault.onRejected(result.attemptsLeft)

                CodeSubmitResult.Rejected(result.message, vault.budget.value.remainingShown)
            }

            CodeCheck.Blocked -> {
                vault.setServerBlocked(StrikeBudget.LOCKOUT)

                CodeSubmitResult.Failed("Acesso bloqueado no servidor.")
            }

            is CodeCheck.Failed -> CodeSubmitResult.Failed(result.message)
        }
    }

    suspend fun forget() = vault.forget()

    fun lockSession() = vault.forgetMemo()

    private suspend fun persist(activity: FragmentActivity, code: String) {
        val availability = biometrics.availability()

        if (availability == BiometricAvailability.NoneEnrolled ||
            availability == BiometricAvailability.Unavailable
        ) {
            vault.keepInMemory(code)

            return
        }

        val cipher = vault.encryptCipher()

        if (cipher == null) {
            vault.keepInMemory(code)

            return
        }

        val outcome = biometrics.authenticate(
            activity = activity,
            title = "Guardar o código",
            subtitle = "Assim amanhã abre com a digital",
            cipher = cipher
        )

        val confirmed = (outcome as? AuthOutcome.Succeeded)?.cipher

        if (confirmed != null) {
            vault.save(code, confirmed)
        } else {
            vault.keepInMemory(code)
        }
    }
}

class CodeUnavailableException : IllegalStateException("Código do dia não liberado.")
