package com.archieapps.finances.ui.screen.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.AccountHistoryDto
import com.archieapps.finances.data.dto.ReconcilePreviewDto
import com.archieapps.finances.data.repo.FinanceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AccountSheet { Reconcile, Yield }

data class AccountAmountForm(
    val sheet: AccountSheet,
    val reais: String = "",
    val centavos: String = "",
    val preview: ReconcilePreviewDto? = null,
    val confirmLoss: Boolean = false
) {
    val cents: Long get() = Money.fromTyped(reais, centavos)

    val typed: Boolean get() = reais.isNotBlank() || centavos.isNotBlank()

    val needsLossConfirmation: Boolean
        get() = sheet == AccountSheet.Reconcile && (preview?.differenceCents ?: 0L) < 0L

    val valid: Boolean
        get() = when (sheet) {
            AccountSheet.Yield -> cents > 0
            AccountSheet.Reconcile -> typed && (!needsLossConfirmation || confirmLoss)
        }
}

data class AccountDetailState(
    val history: AccountHistoryDto? = null,
    val loading: Boolean = true,
    val working: Boolean = false,
    val form: AccountAmountForm? = null,
    val error: String? = null,
    val notice: String? = null
)

class AccountDetailViewModel(
    private val repository: FinanceRepository,
    private val accountId: Int
) : ViewModel() {

    private val _state = MutableStateFlow(AccountDetailState())
    val state: StateFlow<AccountDetailState> = _state.asStateFlow()

    private var previewJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            when (val result = repository.accountHistory(accountId)) {
                is ApiResult.Ok -> _state.update { it.copy(history = result.value, loading = false) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun open(sheet: AccountSheet) = _state.update { it.copy(form = AccountAmountForm(sheet), error = null, notice = null) }

    fun dismiss() {
        previewJob?.cancel()
        _state.update { it.copy(form = null) }
    }

    fun edit(update: (AccountAmountForm) -> AccountAmountForm) {
        _state.update { current -> current.copy(form = current.form?.let(update)) }

        val form = _state.value.form ?: return

        if (form.sheet == AccountSheet.Reconcile) {
            schedulePreview(form)
        }
    }

    private fun schedulePreview(form: AccountAmountForm) {
        previewJob?.cancel()

        if (!form.typed) {
            _state.update { current -> current.copy(form = current.form?.copy(preview = null)) }
            return
        }

        previewJob = viewModelScope.launch {
            delay(300)

            when (val result = repository.previewReconcile(accountId, form.cents)) {
                is ApiResult.Ok -> _state.update { current ->
                    current.copy(form = current.form?.copy(preview = result.value))
                }
                is ApiResult.Failure -> Unit
            }
        }
    }

    fun submit() {
        val form = _state.value.form ?: return
        if (!form.valid || _state.value.working) return

        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }

            val outcome: ApiResult<String> = when (form.sheet) {
                AccountSheet.Yield -> when (val result = repository.registerYield(accountId, form.cents)) {
                    is ApiResult.Ok -> ApiResult.Ok("Rendimento de R$ ${Money.full(form.cents)} registrado.")
                    is ApiResult.Failure -> result
                }
                AccountSheet.Reconcile -> when (val result = repository.reconcileAccount(accountId, form.cents, form.confirmLoss)) {
                    is ApiResult.Ok -> ApiResult.Ok(reconcileNotice(result.value.differenceCents))
                    is ApiResult.Failure -> result
                }
            }

            when (outcome) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(working = false, form = null, notice = outcome.value) }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(working = false, error = outcome.message) }
            }
        }
    }

    private fun reconcileNotice(differenceCents: Long): String = when {
        differenceCents > 0 -> "Rendeu R$ ${Money.full(differenceCents)} desde a última vez."
        differenceCents < 0 -> "Ajuste de R$ ${Money.full(-differenceCents)} registrado."
        else -> "O saldo já estava certo."
    }
}
