package com.archieapps.finances.ui.screen.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.EntryDto
import com.archieapps.finances.data.dto.PersonBalanceDto
import com.archieapps.finances.data.repo.FinanceRepository
import com.archieapps.finances.data.repo.jsonBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put

data class PersonDetail(
    val person: PersonBalanceDto,
    val entries: List<EntryDto> = emptyList(),
    val loading: Boolean = true,
    val settleReais: String = "",
    val settleCentavos: String = "",
    val settleAccountId: Int? = null
) {
    val settleCents: Long
        get() = if (settleReais.isBlank() && settleCentavos.isBlank()) {
            kotlin.math.abs(person.balanceCents)
        } else {
            Money.fromTyped(settleReais, settleCentavos)
        }

    val canSettle: Boolean get() = settleCents > 0 && person.balanceCents != 0L
}

data class PeopleState(
    val balances: List<PersonBalanceDto> = emptyList(),
    val accounts: List<AccountDto> = emptyList(),
    val includeSettled: Boolean = false,
    val loading: Boolean = true,
    val working: Boolean = false,
    val detail: PersonDetail? = null,
    val error: String? = null,
    val notice: String? = null
) {
    val owedToYou: Long get() = balances.filter { it.balanceCents > 0 }.sumOf { it.balanceCents }

    val youOwe: Long get() = balances.filter { it.balanceCents < 0 }.sumOf { -it.balanceCents }
}

class PeopleViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(PeopleState())
    val state: StateFlow<PeopleState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            val balances = repository.personBalances(_state.value.includeSettled)
            val accounts = repository.accounts()

            val failure = listOf(balances, accounts)
                .filterIsInstance<ApiResult.Failure>()
                .firstOrNull()

            _state.update { current ->
                current.copy(
                    balances = (balances as? ApiResult.Ok)?.value ?: current.balances,
                    accounts = (accounts as? ApiResult.Ok)?.value ?: current.accounts,
                    loading = false,
                    error = failure?.message
                )
            }
        }
    }

    fun toggleSettled() {
        _state.update { it.copy(includeSettled = !it.includeSettled) }
        refresh()
    }

    fun openDetail(person: PersonBalanceDto) {
        _state.update { current ->
            current.copy(
                notice = null,
                detail = PersonDetail(
                    person = person,
                    settleAccountId = current.accounts.firstOrNull()?.id
                )
            )
        }

        viewModelScope.launch {
            when (val result = repository.personEntries(person.id)) {
                is ApiResult.Ok -> _state.update { current ->
                    current.copy(detail = current.detail?.copy(entries = result.value, loading = false))
                }
                is ApiResult.Failure -> _state.update { current ->
                    current.copy(
                        detail = current.detail?.copy(loading = false),
                        error = result.message
                    )
                }
            }
        }
    }

    fun dismissDetail() = _state.update { it.copy(detail = null) }

    fun editDetail(update: (PersonDetail) -> PersonDetail) = _state.update { current ->
        current.copy(detail = current.detail?.let(update))
    }

    fun settle() {
        val detail = _state.value.detail ?: return
        if (!detail.canSettle || _state.value.working) return

        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }

            val body = jsonBody {
                put("amount", detail.settleCents)
                detail.settleAccountId?.let { put("accountId", it) }
            }

            when (val result = repository.settlePerson(detail.person.id, body)) {
                is ApiResult.Ok -> {
                    _state.update {
                        it.copy(
                            working = false,
                            detail = null,
                            notice = "Acerto registrado com ${detail.person.name}."
                        )
                    }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(working = false, error = result.message) }
            }
        }
    }
}
