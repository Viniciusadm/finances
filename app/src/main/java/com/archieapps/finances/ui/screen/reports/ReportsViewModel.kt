package com.archieapps.finances.ui.screen.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.data.dto.AccountReportRowDto
import com.archieapps.finances.data.dto.CardReportRowDto
import com.archieapps.finances.data.dto.CategoryReportRowDto
import com.archieapps.finances.data.dto.InstitutionReportRowDto
import com.archieapps.finances.data.dto.MethodReportRowDto
import com.archieapps.finances.data.dto.MonthReportMiscDto
import com.archieapps.finances.data.dto.MonthReportRowDto
import com.archieapps.finances.data.dto.ReportWindowDto
import com.archieapps.finances.data.dto.TagReportRowDto
import com.archieapps.finances.data.repo.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

enum class ReportRange(val label: String) {
    Month("Mês"),
    Quarter("3 meses"),
    Year("12 meses")
}

enum class ReportSource(val value: String, val label: String) {
    All("all", "Tudo"),
    Account("account", "Conta"),
    Card("card", "Cartão")
}

data class ReportsState(
    val range: ReportRange = ReportRange.Month,
    val kind: String = "expense",
    val source: ReportSource = ReportSource.All,
    val byCategory: List<CategoryReportRowDto> = emptyList(),
    val categoryWindow: ReportWindowDto? = null,
    val byTag: List<TagReportRowDto> = emptyList(),
    val byMethod: List<MethodReportRowDto> = emptyList(),
    val byInstitution: List<InstitutionReportRowDto> = emptyList(),
    val byAccount: List<AccountReportRowDto> = emptyList(),
    val byCard: List<CardReportRowDto> = emptyList(),
    val byMonth: List<MonthReportRowDto> = emptyList(),
    val monthMisc: MonthReportMiscDto? = null,
    val loading: Boolean = true,
    val error: String? = null
) {
    val topCategoryCents: Long get() = byCategory.maxOfOrNull { it.totalCents } ?: 0

    val topTagCents: Long get() = byTag.maxOfOrNull { it.totalCents } ?: 0

    val topMethodCents: Long get() = byMethod.maxOfOrNull { it.totalCents } ?: 0

    val topInstitutionCents: Long get() = byInstitution.maxOfOrNull { it.totalCents } ?: 0

    val topAccountCents: Long get() = byAccount.maxOfOrNull { it.totalCents } ?: 0

    val topCardCents: Long get() = byCard.maxOfOrNull { it.totalCents } ?: 0

    val maxMonthCents: Long
        get() = byMonth.maxOfOrNull { maxOf(it.expenseCents, it.incomeCents) } ?: 0
}

class ReportsViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            val current = _state.value
            val (from, to) = window(current.range)

            val source = current.source.value

            val category = repository.reportByCategory(from, to, current.kind, source)
            val tag = repository.reportByTag(from, to, current.kind, source)
            val month = repository.reportByMonth(12, source)
            val method = repository.reportByMethod(from, to, current.kind)
            val institution = repository.reportByInstitution(from, to, current.kind)
            val account = repository.reportByAccount(from, to, current.kind)
            val card = if (current.kind == "expense") repository.reportByCard(from, to) else null

            val failure = listOfNotNull(category, tag, month, method, institution, account, card)
                .filterIsInstance<ApiResult.Failure>()
                .firstOrNull()

            _state.update { state ->
                state.copy(
                    byCategory = (category as? ApiResult.Ok)?.value?.first ?: emptyList(),
                    categoryWindow = (category as? ApiResult.Ok)?.value?.second,
                    byTag = (tag as? ApiResult.Ok)?.value?.first ?: emptyList(),
                    byMethod = (method as? ApiResult.Ok)?.value?.first ?: emptyList(),
                    byInstitution = (institution as? ApiResult.Ok)?.value?.first ?: emptyList(),
                    byAccount = (account as? ApiResult.Ok)?.value?.first ?: emptyList(),
                    byCard = (card as? ApiResult.Ok)?.value?.first ?: emptyList(),
                    byMonth = (month as? ApiResult.Ok)?.value?.first ?: emptyList(),
                    monthMisc = (month as? ApiResult.Ok)?.value?.second,
                    loading = false,
                    error = failure?.message
                )
            }
        }
    }

    fun setRange(range: ReportRange) {
        _state.update { it.copy(range = range) }
        refresh()
    }

    fun setKind(kind: String) {
        _state.update { it.copy(kind = kind) }
        refresh()
    }

    fun setSource(source: ReportSource) {
        _state.update { it.copy(source = source) }
        refresh()
    }

    private fun window(range: ReportRange): Pair<String, String> {
        val now = YearMonth.now()

        val start = when (range) {
            ReportRange.Month -> now
            ReportRange.Quarter -> now.minusMonths(2)
            ReportRange.Year -> now.minusMonths(11)
        }

        return start.atDay(1).toString() to now.atEndOfMonth().toString()
    }
}
