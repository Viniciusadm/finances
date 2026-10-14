package com.archieapps.finances.ui.screen.entries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.data.dto.EntryDto
import com.archieapps.finances.data.repo.EntryFilter
import com.archieapps.finances.data.repo.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class EntriesState(
    val month: YearMonth = YearMonth.now(),
    val allTime: Boolean = false,
    val kind: String? = null,
    val onlyUnpaid: Boolean = false,
    val search: String = "",
    val entries: List<EntryDto> = emptyList(),
    val page: Int = 1,
    val lastPage: Int = 1,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null
) {
    val expenseCents: Long
        get() = entries.filter { it.kind == "expense" && it.paid }.sumOf { it.amountCents }

    val incomeCents: Long
        get() = entries.filter { it.kind == "income" && it.paid }.sumOf { it.amountCents }

    val hasMore: Boolean get() = page < lastPage

    val grouped: List<Pair<String, List<EntryDto>>>
        get() = entries.groupBy { it.date }.toList()
}

class EntriesViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(EntriesState())
    val state: StateFlow<EntriesState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            load(page = 1, append = false)
        }
    }

    fun loadMore() {
        val current = _state.value
        if (!current.hasMore || current.loadingMore || current.loading) return

        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            load(page = current.page + 1, append = true)
        }
    }

    private suspend fun load(page: Int, append: Boolean) {
        val current = _state.value

        val filter = EntryFilter(
            from = if (current.allTime) null else current.month.atDay(1).toString(),
            to = if (current.allTime) null else current.month.atEndOfMonth().toString(),
            kind = current.kind,
            paid = if (current.onlyUnpaid) false else null,
            search = current.search.takeIf { it.isNotBlank() },
            perPage = 40
        )

        when (val result = repository.entries(filter, page)) {
            is ApiResult.Ok -> _state.update { state ->
                state.copy(
                    entries = if (append) state.entries + result.value.entries else result.value.entries,
                    page = result.value.pagination?.current ?: page,
                    lastPage = result.value.pagination?.last ?: page,
                    loading = false,
                    loadingMore = false
                )
            }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, loadingMore = false, error = result.message)
            }
        }
    }

    fun previousMonth() {
        _state.update { it.copy(month = it.month.minusMonths(1), allTime = false) }
        refresh()
    }

    fun nextMonth() {
        _state.update { it.copy(month = it.month.plusMonths(1), allTime = false) }
        refresh()
    }

    fun toggleAllTime() {
        _state.update { it.copy(allTime = !it.allTime) }
        refresh()
    }

    fun setKind(kind: String?) {
        _state.update { it.copy(kind = kind) }
        refresh()
    }

    fun toggleUnpaid() {
        _state.update { it.copy(onlyUnpaid = !it.onlyUnpaid) }
        refresh()
    }

    fun onSearch(value: String) {
        _state.update { it.copy(search = value) }
        refresh()
    }

    fun togglePaid(entry: EntryDto) {
        viewModelScope.launch {
            val result = if (entry.paid) repository.unpayEntry(entry.id) else repository.payEntry(entry.id)

            when (result) {
                is ApiResult.Ok -> _state.update { state ->
                    state.copy(entries = state.entries.map { if (it.id == entry.id) result.value else it })
                }
                is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            }
        }
    }

    fun dayTotal(entries: List<EntryDto>): Long =
        entries.filter { it.paid }.sumOf {
            when (it.kind) {
                "income" -> it.amountCents
                "expense" -> -it.amountCents
                else -> 0L
            }
        }

    companion object {
        fun today(): LocalDate = LocalDate.now()
    }
}
