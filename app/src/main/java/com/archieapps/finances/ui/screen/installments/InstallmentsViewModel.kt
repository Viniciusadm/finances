package com.archieapps.finances.ui.screen.installments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.CardDto
import com.archieapps.finances.data.dto.CategoryDto
import com.archieapps.finances.data.dto.InstallmentPlanDto
import com.archieapps.finances.data.repo.FinanceRepository
import com.archieapps.finances.data.repo.jsonBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put
import java.time.LocalDate

data class PlanForm(
    val description: String = "",
    val reais: String = "",
    val centavos: String = "",
    val amountIsInstallment: Boolean = true,
    val installments: String = "12",
    val firstNumber: String = "1",
    val firstDueDate: LocalDate = LocalDate.now(),
    val backfill: Boolean = false,
    val useCard: Boolean = true,
    val cardId: Int? = null,
    val accountId: Int? = null,
    val categoryId: Int? = null
) {
    val amountCents: Long get() = Money.fromTyped(reais, centavos)

    val installmentCount: Int? get() = installments.toIntOrNull()?.takeIf { it in 1..255 }

    val firstNumberValue: Int? get() = firstNumber.toIntOrNull()?.takeIf { it >= 1 }

    val valid: Boolean
        get() = amountCents > 0 &&
            installmentCount != null &&
            firstNumberValue != null &&
            firstNumberValue!! <= installmentCount!! &&
            (if (useCard) cardId != null else accountId != null)

    val projectedTotal: Long
        get() = if (amountIsInstallment) amountCents * (installmentCount ?: 0) else amountCents
}

data class InstallmentsState(
    val plans: List<InstallmentPlanDto> = emptyList(),
    val onlyOpen: Boolean = true,
    val accounts: List<AccountDto> = emptyList(),
    val cards: List<CardDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val form: PlanForm? = null,
    val detail: InstallmentPlanDto? = null,
    val error: String? = null
)

class InstallmentsViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(InstallmentsState())
    val state: StateFlow<InstallmentsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            val plans = repository.installmentPlans(_state.value.onlyOpen)
            val accounts = repository.accounts()
            val cards = repository.cards()
            val categories = repository.categories()

            val failure = listOf(plans, accounts, cards, categories)
                .filterIsInstance<ApiResult.Failure>()
                .firstOrNull()

            _state.update { current ->
                current.copy(
                    plans = (plans as? ApiResult.Ok)?.value ?: current.plans,
                    accounts = (accounts as? ApiResult.Ok)?.value ?: current.accounts,
                    cards = (cards as? ApiResult.Ok)?.value ?: current.cards,
                    categories = (categories as? ApiResult.Ok)?.value ?: current.categories,
                    loading = false,
                    error = failure?.message
                )
            }
        }
    }

    fun toggleOpen() {
        _state.update { it.copy(onlyOpen = !it.onlyOpen) }
        refresh()
    }

    fun startCreate() = _state.update { current ->
        current.copy(
            error = null,
            form = PlanForm(
                cardId = current.cards.firstOrNull()?.id,
                accountId = current.accounts.firstOrNull()?.id,
                useCard = current.cards.isNotEmpty()
            )
        )
    }

    fun dismissForm() = _state.update { it.copy(form = null) }

    fun editForm(update: (PlanForm) -> PlanForm) = _state.update { current ->
        current.copy(form = current.form?.let(update))
    }

    fun openDetail(plan: InstallmentPlanDto) {
        viewModelScope.launch {
            when (val result = repository.installmentPlan(plan.id)) {
                is ApiResult.Ok -> _state.update { it.copy(detail = result.value) }
                is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            }
        }
    }

    fun dismissDetail() = _state.update { it.copy(detail = null) }

    fun save() {
        val form = _state.value.form ?: return
        if (!form.valid || _state.value.saving) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            val body = jsonBody {
                put("description", form.description.trim().ifEmpty { "Parcelamento" })
                put("amount", form.amountCents)
                put("amountIs", if (form.amountIsInstallment) "installment" else "total")
                put("installments", form.installmentCount ?: 1)
                put("firstInstallmentNumber", form.firstNumberValue ?: 1)
                put("firstDueDate", form.firstDueDate.toString())
                put("backfill", form.backfill)
                form.categoryId?.let { put("categoryId", it) }

                if (form.useCard) {
                    form.cardId?.let { put("cardId", it) }
                } else {
                    form.accountId?.let { put("accountId", it) }
                }
            }

            when (val result = repository.createInstallmentPlan(body)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(saving = false, form = null) }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }

    fun delete(plan: InstallmentPlanDto) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            when (val result = repository.deleteInstallmentPlan(plan.id)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(saving = false, detail = null) }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }
}
