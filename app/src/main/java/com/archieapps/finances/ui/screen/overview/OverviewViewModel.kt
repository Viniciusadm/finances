package com.archieapps.finances.ui.screen.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.EntryDto
import com.archieapps.finances.data.dto.OverviewDto
import com.archieapps.finances.data.repo.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OverviewState(
    val data: OverviewDto? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val notice: String? = null,
    val settlingId: Int? = null
) {
    val expenseDelta: Long
        get() {
            val current = data?.monthTotals?.expenseCents ?: 0
            val previous = data?.previousTotals?.expenseCents ?: 0
            return current - previous
        }

    val topCategoryCents: Long
        get() = data?.byCategory?.maxOfOrNull { it.totalCents } ?: 0
}

class OverviewViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(OverviewState())
    val state: StateFlow<OverviewState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            when (val overview = repository.overview()) {
                is ApiResult.Ok -> _state.update { it.copy(data = overview.value, loading = false) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = overview.message) }
            }
        }
    }

    fun settle(entry: EntryDto) {
        if (_state.value.settlingId != null) return

        viewModelScope.launch {
            _state.update { it.copy(settlingId = entry.id, error = null, notice = null) }

            when (val result = repository.payEntry(entry.id)) {
                is ApiResult.Ok -> {
                    val verb = if (entry.kind == "income") "Recebido" else "Pago"
                    _state.update {
                        it.copy(settlingId = null, notice = "$verb: ${entry.description} · R$ ${Money.full(entry.amountCents)}")
                    }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(settlingId = null, error = result.message) }
            }
        }
    }
}
