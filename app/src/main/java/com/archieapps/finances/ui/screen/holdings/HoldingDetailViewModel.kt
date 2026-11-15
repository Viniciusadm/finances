package com.archieapps.finances.ui.screen.holdings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.HoldingDto
import com.archieapps.finances.data.dto.HoldingPreviewDto
import com.archieapps.finances.data.repo.FinanceRepository
import com.archieapps.finances.data.repo.jsonBody
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put

enum class HoldingSheet { Buy, Sell, Price }

data class HoldingForm(
    val sheet: HoldingSheet,
    val quantity: String = "",
    val price: String = "",
    val otherAccountId: Int? = null,
    val date: java.time.LocalDate = java.time.LocalDate.now(),
    val preview: HoldingPreviewDto? = null,
    val previewError: String? = null
) {
    val valid: Boolean
        get() = when (sheet) {
            HoldingSheet.Price -> isPositive(price)
            HoldingSheet.Buy, HoldingSheet.Sell -> isPositive(quantity) && isPositive(price) && otherAccountId != null
        }
}

data class HoldingDetailState(
    val holding: HoldingDto? = null,
    val accounts: List<AccountDto> = emptyList(),
    val loading: Boolean = true,
    val working: Boolean = false,
    val form: HoldingForm? = null,
    val error: String? = null,
    val notice: String? = null,
    val deleted: Boolean = false
)

class HoldingDetailViewModel(
    private val repository: FinanceRepository,
    private val holdingId: Int
) : ViewModel() {

    private val _state = MutableStateFlow(HoldingDetailState())
    val state: StateFlow<HoldingDetailState> = _state.asStateFlow()

    private var previewJob: Job? = null

    init {
        refresh()
        loadAccounts()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            when (val result = repository.holding(holdingId)) {
                is ApiResult.Ok -> _state.update { it.copy(holding = result.value, loading = false) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            when (val result = repository.accounts()) {
                is ApiResult.Ok -> _state.update { it.copy(accounts = result.value.filterNot { account -> account.archived }) }
                is ApiResult.Failure -> Unit
            }
        }
    }

    /** O preço já vem preenchido com o preço atual: é só ajustar o que mudou. */
    fun open(sheet: HoldingSheet) {
        val holding = _state.value.holding ?: return
        val firstAccount = _state.value.accounts.firstOrNull { it.id != holding.accountId && !it.separated }?.id

        _state.update {
            it.copy(
                form = HoldingForm(
                    sheet = sheet,
                    price = formatPrice(holding.price),
                    otherAccountId = if (sheet == HoldingSheet.Price) null else firstAccount
                ),
                error = null,
                notice = null
            )
        }
        schedulePreview()
    }

    fun dismiss() {
        previewJob?.cancel()
        _state.update { it.copy(form = null) }
    }

    fun edit(update: (HoldingForm) -> HoldingForm) {
        _state.update { current -> current.copy(form = current.form?.let(update)) }
        schedulePreview()
    }

    private fun body(form: HoldingForm, preview: Boolean) = jsonBody {
        put("price", form.price)
        put("date", form.date.toString())
        if (form.sheet != HoldingSheet.Price) put("quantity", form.quantity)
        if (form.sheet == HoldingSheet.Buy) form.otherAccountId?.let { put("fromAccountId", it) }
        if (form.sheet == HoldingSheet.Sell) form.otherAccountId?.let { put("toAccountId", it) }
        if (preview) put("preview", true)
    }

    private fun schedulePreview() {
        previewJob?.cancel()
        val form = _state.value.form ?: return

        val ready = isPositive(form.price) && (form.sheet == HoldingSheet.Price || isPositive(form.quantity))

        if (!ready) {
            _state.update { current -> current.copy(form = current.form?.copy(preview = null, previewError = null)) }
            return
        }

        previewJob = viewModelScope.launch {
            delay(300)

            val result = when (form.sheet) {
                HoldingSheet.Buy -> repository.previewBuyHolding(holdingId, body(form, true))
                HoldingSheet.Sell -> repository.previewSellHolding(holdingId, body(form, true))
                HoldingSheet.Price -> repository.previewPriceHolding(holdingId, body(form, true))
            }

            _state.update { current ->
                current.copy(
                    form = current.form?.copy(
                        preview = (result as? ApiResult.Ok)?.value,
                        previewError = (result as? ApiResult.Failure)?.message
                    )
                )
            }
        }
    }

    fun submit(onChanged: () -> Unit) {
        val form = _state.value.form ?: return
        if (!form.valid || _state.value.working) return

        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }

            val result = when (form.sheet) {
                HoldingSheet.Buy -> repository.buyHolding(holdingId, body(form, false))
                HoldingSheet.Sell -> repository.sellHolding(holdingId, body(form, false))
                HoldingSheet.Price -> repository.priceHolding(holdingId, body(form, false))
            }

            when (result) {
                is ApiResult.Ok -> {
                    _state.update {
                        it.copy(
                            working = false,
                            form = null,
                            holding = result.value,
                            notice = when (form.sheet) {
                                HoldingSheet.Buy -> "Compra registrada. O preço atual agora é R$ ${formatPrice(form.price)}."
                                HoldingSheet.Sell -> "Venda registrada. O preço atual agora é R$ ${formatPrice(form.price)}."
                                HoldingSheet.Price -> "Preço atualizado."
                            }
                        )
                    }
                    onChanged()
                }
                is ApiResult.Failure -> _state.update { it.copy(working = false, error = result.message) }
            }
        }
    }

    fun delete(onChanged: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }

            when (val result = repository.deleteHolding(holdingId)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(working = false, deleted = true) }
                    onChanged()
                }
                is ApiResult.Failure -> _state.update { it.copy(working = false, error = result.message) }
            }
        }
    }
}
