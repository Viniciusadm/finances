package com.archieapps.finances.ui.screen.wealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.data.repo.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WealthState(
    val breakdown: WealthBreakdown? = null,
    val loading: Boolean = true,
    val error: String? = null
)

class WealthViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(WealthState())
    val state: StateFlow<WealthState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            when (val result = repository.overview()) {
                is ApiResult.Ok -> _state.update { it.copy(breakdown = WealthBreakdown.from(result.value), loading = false) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
