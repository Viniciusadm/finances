package com.archieapps.finances.ui.screen.statement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.EntryDto
import com.archieapps.finances.data.dto.StatementDto
import com.archieapps.finances.data.repo.FinanceRepository
import com.archieapps.finances.data.repo.jsonBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put
import java.time.YearMonth

data class StatementState(
    val cardId: Int,
    val statement: StatementDto? = null,
    val accounts: List<AccountDto> = emptyList(),
    val payAccountId: Int? = null,
    val loading: Boolean = true,
    val working: Boolean = false,
    val error: String? = null
) {
    val byCategory: List<Pair<String, Long>>
        get() = statement?.entries.orEmpty()
            .groupBy { it.categoryName ?: "Sem categoria" }
            .map { (name, entries) -> name to entries.sumOf { it.amountCents } }
            .sortedByDescending { it.second }

    val canPay: Boolean
        get() = statement != null && !statement.paid && statement.totalCents > 0
}

class StatementViewModel(
    private val repository: FinanceRepository,
    cardId: Int
) : ViewModel() {

    private val _state = MutableStateFlow(StatementState(cardId = cardId))
    val state: StateFlow<StatementState> = _state.asStateFlow()

    private var reference: String? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            val cardId = _state.value.cardId
            val target = reference

            val result = if (target == null) {
                repository.currentStatement(cardId)
            } else {
                repository.statement(cardId, target)
            }

            val accounts = repository.accounts()

            when (result) {
                is ApiResult.Ok -> {
                    reference = result.value.reference

                    _state.update { current ->
                        current.copy(
                            statement = result.value,
                            accounts = (accounts as? ApiResult.Ok)?.value ?: current.accounts,
                            payAccountId = current.payAccountId
                                ?: (accounts as? ApiResult.Ok)?.value?.firstOrNull()?.id,
                            loading = false
                        )
                    }
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun previousMonth() = step(-1)

    fun nextMonth() = step(1)

    private fun step(months: Long) {
        val current = reference ?: return
        val parsed = runCatching { YearMonth.parse(current) }.getOrNull() ?: return

        reference = parsed.plusMonths(months).toString()
        load()
    }

    fun onPayAccount(id: Int) = _state.update { it.copy(payAccountId = id) }

    fun pay() {
        val statement = _state.value.statement ?: return
        if (!_state.value.canPay || _state.value.working) return

        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }

            val body = jsonBody {
                _state.value.payAccountId?.let { put("accountId", it) }
            }

            when (val result = repository.payStatement(statement.id, body)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(working = false) }
                    load()
                }
                is ApiResult.Failure -> _state.update { it.copy(working = false, error = result.message) }
            }
        }
    }

    fun unpay() {
        val statement = _state.value.statement ?: return

        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }

            when (val result = repository.unpayStatement(statement.id)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(working = false) }
                    load()
                }
                is ApiResult.Failure -> _state.update { it.copy(working = false, error = result.message) }
            }
        }
    }
}
