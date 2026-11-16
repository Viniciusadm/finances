package com.archieapps.finances.ui.screen.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.data.dto.CardDto
import com.archieapps.finances.data.dto.EntryDto
import com.archieapps.finances.data.repo.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EntryDetailState(
    val entry: EntryDto? = null,
    val card: CardDto? = null,
    val loading: Boolean = true,
    val working: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
)

class EntryDetailViewModel(
    private val repository: FinanceRepository,
    private val entryId: Int
) : ViewModel() {

    private val _state = MutableStateFlow(EntryDetailState())
    val state: StateFlow<EntryDetailState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            when (val result = repository.entry(entryId)) {
                is ApiResult.Ok -> {
                    val entry = result.value
                    val card = entry.cardId?.let { id ->
                        (repository.cards(includeArchived = true) as? ApiResult.Ok)?.value?.firstOrNull { it.id == id }
                    }

                    _state.update { it.copy(entry = entry, card = card, loading = false) }
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun togglePaid(onChanged: () -> Unit) {
        val entry = _state.value.entry ?: return
        if (_state.value.working) return

        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }

            val result = if (entry.paid) repository.unpayEntry(entry.id) else repository.payEntry(entry.id)

            when (result) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(working = false) }
                    refresh()
                    onChanged()
                }
                is ApiResult.Failure -> _state.update { it.copy(working = false, error = result.message) }
            }
        }
    }

    fun delete(scope: DeleteScope, onChanged: () -> Unit) {
        val entry = _state.value.entry ?: return

        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }

            val result = when (scope) {
                DeleteScope.Entry -> repository.deleteEntry(entry.id)
                DeleteScope.Plan -> entry.installmentPlanId?.let { repository.deleteInstallmentPlan(it) } ?: repository.deleteEntry(entry.id)
                DeleteScope.Recurrence -> entry.recurrenceId?.let { repository.deleteRecurrence(it, keepEntries = false) } ?: repository.deleteEntry(entry.id)
            }

            when (result) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(working = false, deleted = true) }
                    onChanged()
                }
                is ApiResult.Failure -> _state.update { it.copy(working = false, error = result.message) }
            }
        }
    }
}

enum class DeleteScope { Entry, Plan, Recurrence }
