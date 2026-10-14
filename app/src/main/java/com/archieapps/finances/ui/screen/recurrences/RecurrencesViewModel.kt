package com.archieapps.finances.ui.screen.recurrences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.CardDto
import com.archieapps.finances.data.dto.CategoryDto
import com.archieapps.finances.data.dto.RecurrenceDto
import com.archieapps.finances.data.dto.UpcomingOccurrenceDto
import com.archieapps.finances.data.repo.FinanceRepository
import com.archieapps.finances.data.repo.jsonBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put
import java.time.LocalDate

enum class Cadence(val label: String) {
    Monthly("Mensal"),
    Weekly("Semanal"),
    Yearly("Anual")
}

data class RecurrenceForm(
    val id: Int? = null,
    val description: String = "",
    val reais: String = "",
    val centavos: String = "",
    val kind: String = "expense",
    val cadence: Cadence = Cadence.Monthly,
    val startsOn: LocalDate = LocalDate.now(),
    val endsOn: LocalDate? = null,
    val useCard: Boolean = true,
    val cardId: Int? = null,
    val accountId: Int? = null,
    val categoryId: Int? = null,
    val active: Boolean = true
) {
    val amountCents: Long get() = Money.fromTyped(reais, centavos)

    val valid: Boolean
        get() = description.isNotBlank() &&
            amountCents > 0 &&
            (if (useCard) cardId != null else accountId != null)

    fun rule(): String = when (cadence) {
        Cadence.Monthly -> "FREQ=MONTHLY;BYMONTHDAY=${startsOn.dayOfMonth}"
        Cadence.Weekly -> "FREQ=WEEKLY;BYDAY=${weekdayCode(startsOn)}"
        Cadence.Yearly -> "FREQ=YEARLY;BYMONTH=${startsOn.monthValue};BYMONTHDAY=${startsOn.dayOfMonth}"
    }

    private fun weekdayCode(date: LocalDate): String = when (date.dayOfWeek.value) {
        1 -> "MO"
        2 -> "TU"
        3 -> "WE"
        4 -> "TH"
        5 -> "FR"
        6 -> "SA"
        else -> "SU"
    }
}

data class RecurrencesState(
    val recurrences: List<RecurrenceDto> = emptyList(),
    val upcoming: List<UpcomingOccurrenceDto> = emptyList(),
    val onlyActive: Boolean = true,
    val accounts: List<AccountDto> = emptyList(),
    val cards: List<CardDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val form: RecurrenceForm? = null,
    val error: String? = null,
    val notice: String? = null
)

class RecurrencesViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(RecurrencesState())
    val state: StateFlow<RecurrencesState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            val recurrences = repository.recurrences(_state.value.onlyActive)
            val upcoming = repository.upcomingOccurrences(60)
            val accounts = repository.accounts()
            val cards = repository.cards()
            val categories = repository.categories()

            val failure = listOf(recurrences, upcoming, accounts, cards, categories)
                .filterIsInstance<ApiResult.Failure>()
                .firstOrNull()

            _state.update { current ->
                current.copy(
                    recurrences = (recurrences as? ApiResult.Ok)?.value ?: current.recurrences,
                    upcoming = (upcoming as? ApiResult.Ok)?.value ?: current.upcoming,
                    accounts = (accounts as? ApiResult.Ok)?.value ?: current.accounts,
                    cards = (cards as? ApiResult.Ok)?.value ?: current.cards,
                    categories = (categories as? ApiResult.Ok)?.value ?: current.categories,
                    loading = false,
                    error = failure?.message
                )
            }
        }
    }

    fun toggleActive() {
        _state.update { it.copy(onlyActive = !it.onlyActive) }
        refresh()
    }

    fun startCreate() = _state.update { current ->
        current.copy(
            error = null,
            notice = null,
            form = RecurrenceForm(
                cardId = current.cards.firstOrNull()?.id,
                accountId = current.accounts.firstOrNull()?.id,
                useCard = current.cards.isNotEmpty()
            )
        )
    }

    fun startEdit(recurrence: RecurrenceDto) = _state.update {
        it.copy(
            error = null,
            notice = null,
            form = RecurrenceForm(
                id = recurrence.id,
                description = recurrence.description,
                reais = (recurrence.amountCents / 100).toString(),
                centavos = (recurrence.amountCents % 100).toString().padStart(2, '0'),
                kind = recurrence.kind,
                cadence = cadenceOf(recurrence.recurrenceRule),
                startsOn = runCatching { LocalDate.parse(recurrence.startsOn) }.getOrDefault(LocalDate.now()),
                endsOn = recurrence.endsOn?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() },
                useCard = recurrence.cardId != null,
                cardId = recurrence.cardId ?: it.cards.firstOrNull()?.id,
                accountId = recurrence.accountId ?: it.accounts.firstOrNull()?.id,
                categoryId = recurrence.categoryId,
                active = recurrence.active
            )
        )
    }

    fun dismissForm() = _state.update { it.copy(form = null) }

    fun editForm(update: (RecurrenceForm) -> RecurrenceForm) = _state.update { current ->
        current.copy(form = current.form?.let(update))
    }

    fun save() {
        val form = _state.value.form ?: return
        if (!form.valid || _state.value.saving) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            val body = jsonBody {
                put("description", form.description.trim())
                put("amount", form.amountCents)
                put("kind", form.kind)
                put("recurrenceRule", form.rule())
                put("startsOn", form.startsOn.toString())
                form.endsOn?.let { put("endsOn", it.toString()) }
                form.categoryId?.let { put("categoryId", it) }

                if (form.useCard) {
                    form.cardId?.let { put("cardId", it) }
                } else {
                    form.accountId?.let { put("accountId", it) }
                }
            }

            val result = if (form.id == null) {
                repository.createRecurrence(body)
            } else {
                repository.updateRecurrence(form.id, body)
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

    fun setActive(recurrence: RecurrenceDto, active: Boolean) {
        viewModelScope.launch {
            when (val result = repository.pauseRecurrence(recurrence.id, active)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(form = null) }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            }
        }
    }

    fun materialize(recurrence: RecurrenceDto) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null, notice = null) }

            when (val result = repository.materializeRecurrence(recurrence.id)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(saving = false, form = null, notice = "Lançamentos em dia.") }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }

    fun delete(recurrence: RecurrenceDto, keepEntries: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            when (val result = repository.deleteRecurrence(recurrence.id, keepEntries)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(saving = false, form = null) }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }

    private fun cadenceOf(rule: String): Cadence = when {
        rule.contains("FREQ=WEEKLY") -> Cadence.Weekly
        rule.contains("FREQ=YEARLY") -> Cadence.Yearly
        else -> Cadence.Monthly
    }
}
