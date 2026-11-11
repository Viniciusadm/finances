package com.archieapps.finances.ui.screen.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.BillDto
import com.archieapps.finances.data.dto.BillsMiscDto
import com.archieapps.finances.data.repo.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BillFilter(val status: String, val label: String) {
    Open("open", "Em aberto"),
    Overdue("overdue", "Vencidas"),
    Done("done", "Realizadas"),
    All("all", "Todas")
}

data class SettleForm(
    val bill: BillDto,
    val accountId: Int?,
    val reais: String = "",
    val centavos: String = ""
) {
    val receiving: Boolean get() = bill.kind == "income"

    val amountCents: Long
        get() = if (reais.isBlank() && centavos.isBlank()) bill.amountCents else Money.fromTyped(reais, centavos)

    val valid: Boolean get() = accountId != null && amountCents > 0
}

data class BillsState(
    val filter: BillFilter = BillFilter.Open,
    val bills: List<BillDto> = emptyList(),
    val totals: BillsMiscDto? = null,
    val accounts: List<AccountDto> = emptyList(),
    val loading: Boolean = true,
    val working: Boolean = false,
    val settle: SettleForm? = null,
    val error: String? = null,
    val notice: String? = null,
    val changes: Int = 0
)

class BillsViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(BillsState())
    val state: StateFlow<BillsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            val bills = repository.bills(_state.value.filter.status)
            val accounts = repository.accounts()

            _state.update { current ->
                current.copy(
                    bills = (bills as? ApiResult.Ok)?.value?.first ?: current.bills,
                    totals = (bills as? ApiResult.Ok)?.value?.second ?: current.totals,
                    accounts = (accounts as? ApiResult.Ok)?.value ?: current.accounts,
                    loading = false,
                    error = listOf(bills, accounts).filterIsInstance<ApiResult.Failure>().firstOrNull()?.message
                )
            }
        }
    }

    fun select(filter: BillFilter) {
        if (filter == _state.value.filter) return
        _state.update { it.copy(filter = filter, bills = emptyList()) }
        refresh()
    }

    fun openSettle(bill: BillDto) = _state.update { current ->
        current.copy(
            notice = null,
            error = null,
            settle = SettleForm(bill = bill, accountId = bill.accountId ?: current.accounts.firstOrNull()?.id)
        )
    }

    fun dismissSettle() = _state.update { it.copy(settle = null) }

    fun editSettle(update: (SettleForm) -> SettleForm) = _state.update { current ->
        current.copy(settle = current.settle?.let(update))
    }

    fun quickSettle(bill: BillDto) {
        if (bill.accountId == null) {
            openSettle(bill)
            return
        }

        settle(SettleForm(bill = bill, accountId = bill.accountId))
    }

    fun confirmSettle() {
        val form = _state.value.settle ?: return
        if (!form.valid) return
        settle(form)
    }

    private fun settle(form: SettleForm) {
        if (_state.value.working) return

        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }

            val bill = form.bill
            val amount = form.amountCents.takeIf { it != bill.amountCents }
            val entryId = bill.entryId
            val recurrenceId = bill.recurrenceId
            val occurrenceDate = bill.occurrenceDate

            val result = when {
                entryId != null -> repository.payEntry(entryId, form.accountId, amount)
                recurrenceId != null && occurrenceDate != null ->
                    repository.payOccurrence(recurrenceId, occurrenceDate, form.accountId, amount)
                else -> ApiResult.Failure("Não sei como confirmar esta conta.")
            }

            when (result) {
                is ApiResult.Ok -> {
                    val verb = if (form.receiving) "Recebido" else "Pago"
                    _state.update {
                        it.copy(
                            working = false,
                            settle = null,
                            notice = "$verb: ${bill.description} · R$ ${Money.full(form.amountCents)}",
                            changes = it.changes + 1
                        )
                    }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(working = false, error = result.message) }
            }
        }
    }

    fun skip() {
        val form = _state.value.settle ?: return
        val recurrenceId = form.bill.recurrenceId ?: return
        val date = form.bill.occurrenceDate ?: form.bill.date

        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }

            when (val result = repository.skipOccurrence(recurrenceId, date)) {
                is ApiResult.Ok -> {
                    _state.update {
                        it.copy(
                            working = false,
                            settle = null,
                            notice = "${form.bill.description} pulada neste período.",
                            changes = it.changes + 1
                        )
                    }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(working = false, error = result.message) }
            }
        }
    }
}
