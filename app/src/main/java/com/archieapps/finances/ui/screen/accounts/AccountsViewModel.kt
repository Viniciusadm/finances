package com.archieapps.finances.ui.screen.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.repo.FinanceRepository
import com.archieapps.finances.data.repo.jsonBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put

data class AccountForm(
    val id: Int? = null,
    val name: String = "",
    val kind: String = "checking",
    val reais: String = "",
    val centavos: String = "",
    val color: String? = "primary",
    val archived: Boolean = false
) {
    val valid: Boolean get() = name.isNotBlank()
}

data class AccountsState(
    val accounts: List<AccountDto> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val reordering: Boolean = false,
    val includeArchived: Boolean = false,
    val form: AccountForm? = null,
    val error: String? = null
)

class AccountsViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(AccountsState())
    val state: StateFlow<AccountsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            when (val result = repository.accounts(_state.value.includeArchived)) {
                is ApiResult.Ok -> _state.update { it.copy(accounts = result.value, loading = false) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun toggleArchived() {
        _state.update { it.copy(includeArchived = !it.includeArchived) }
        refresh()
    }

    fun toggleReordering() {
        val entering = !_state.value.reordering
        _state.update { it.copy(reordering = entering, includeArchived = if (entering) true else it.includeArchived) }
        if (entering) refresh()
    }

    fun startCreate() = _state.update { it.copy(form = AccountForm(), error = null) }

    fun startEdit(account: AccountDto) = _state.update {
        it.copy(
            error = null,
            form = AccountForm(
                id = account.id,
                name = account.name,
                kind = account.kind,
                reais = (account.initialBalanceCents / 100).toString(),
                centavos = (account.initialBalanceCents % 100).toString().padStart(2, '0'),
                color = account.colorToken ?: account.color,
                archived = account.archived
            )
        )
    }

    fun dismissForm() = _state.update { it.copy(form = null) }

    fun editForm(update: (AccountForm) -> AccountForm) = _state.update { current ->
        current.copy(form = current.form?.let(update))
    }

    fun save() {
        val form = _state.value.form ?: return
        if (!form.valid || _state.value.saving) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            val body = jsonBody {
                put("name", form.name.trim())
                put("kind", form.kind)
                put("initialBalance", Money.fromTyped(form.reais, form.centavos))
                form.color?.let { put("color", it) }
            }

            val result = if (form.id == null) {
                repository.createAccount(body)
            } else {
                repository.updateAccount(form.id, body)
            }

            when (result) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(saving = false, form = null) }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }

    fun setArchived(archived: Boolean) {
        val form = _state.value.form ?: return
        val id = form.id ?: return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            when (val result = repository.archiveAccount(id, archived)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(saving = false, form = null) }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }

    fun delete() {
        val id = _state.value.form?.id ?: return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            when (val result = repository.deleteAccount(id)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(saving = false, form = null) }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }

    fun move(from: Int, to: Int) {
        val current = _state.value.accounts
        if (to < 0 || to >= current.size) return

        val reordered = current.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        _state.update { it.copy(accounts = reordered) }

        viewModelScope.launch {
            when (val result = repository.reorderAccounts(reordered.map { it.id })) {
                is ApiResult.Ok -> Unit
                is ApiResult.Failure -> {
                    _state.update { it.copy(error = result.message) }
                    refresh()
                }
            }
        }
    }
}
