package com.archieapps.finances.ui.screen.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.CardDto
import com.archieapps.finances.data.repo.FinanceRepository
import com.archieapps.finances.data.repo.jsonBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put

data class CardForm(
    val id: Int? = null,
    val name: String = "",
    val brand: String = "",
    val lastDigits: String = "",
    val accountId: Int? = null,
    val limitReais: String = "",
    val limitCentavos: String = "",
    val closingDay: String = "",
    val dueDay: String = "",
    val color: String? = "primary",
    val archived: Boolean = false
) {
    val valid: Boolean
        get() = name.isNotBlank() && dayValue(closingDay) != null && dayValue(dueDay) != null

    companion object {
        fun dayValue(raw: String): Int? = raw.toIntOrNull()?.takeIf { it in 1..31 }
    }
}

data class CardsState(
    val cards: List<CardDto> = emptyList(),
    val accounts: List<AccountDto> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val includeArchived: Boolean = false,
    val form: CardForm? = null,
    val error: String? = null
)

class CardsViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(CardsState())
    val state: StateFlow<CardsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            val cards = repository.cards(_state.value.includeArchived)
            val accounts = repository.accounts()

            val failure = listOf(cards, accounts).filterIsInstance<ApiResult.Failure>().firstOrNull()

            _state.update { current ->
                current.copy(
                    cards = (cards as? ApiResult.Ok)?.value ?: current.cards,
                    accounts = (accounts as? ApiResult.Ok)?.value ?: current.accounts,
                    loading = false,
                    error = failure?.message
                )
            }
        }
    }

    fun toggleArchived() {
        _state.update { it.copy(includeArchived = !it.includeArchived) }
        refresh()
    }

    fun startCreate() = _state.update { it.copy(form = CardForm(), error = null) }

    fun startEdit(card: CardDto) = _state.update {
        it.copy(
            error = null,
            form = CardForm(
                id = card.id,
                name = card.name,
                brand = card.brand.orEmpty(),
                lastDigits = card.lastDigits.orEmpty(),
                accountId = card.accountId,
                limitReais = card.limitCents?.let { (it / 100).toString() }.orEmpty(),
                limitCentavos = card.limitCents?.let { (it % 100).toString().padStart(2, '0') }.orEmpty(),
                closingDay = card.closingDay.toString(),
                dueDay = card.dueDay.toString(),
                color = card.colorToken ?: card.color,
                archived = card.archived
            )
        )
    }

    fun dismissForm() = _state.update { it.copy(form = null) }

    fun editForm(update: (CardForm) -> CardForm) = _state.update { current ->
        current.copy(form = current.form?.let(update))
    }

    fun save() {
        val form = _state.value.form ?: return
        if (!form.valid || _state.value.saving) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            val body = jsonBody {
                put("name", form.name.trim())
                put("closingDay", CardForm.dayValue(form.closingDay) ?: 1)
                put("dueDay", CardForm.dayValue(form.dueDay) ?: 1)
                form.brand.trim().takeIf { it.isNotEmpty() }?.let { put("brand", it) }
                form.lastDigits.trim().takeIf { it.length == 4 }?.let { put("lastDigits", it) }
                form.accountId?.let { put("accountId", it) }
                form.color?.let { put("color", it) }
                if (form.limitReais.isNotBlank() || form.limitCentavos.isNotBlank()) {
                    put("limit", Money.fromTyped(form.limitReais, form.limitCentavos))
                }
            }

            val result = if (form.id == null) {
                repository.createCard(body)
            } else {
                repository.updateCard(form.id, body)
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
        val id = _state.value.form?.id ?: return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            when (val result = repository.archiveCard(id, archived)) {
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

            when (val result = repository.deleteCard(id)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(saving = false, form = null) }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }
}
