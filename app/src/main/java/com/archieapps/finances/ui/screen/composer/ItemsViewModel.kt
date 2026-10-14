package com.archieapps.finances.ui.screen.composer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.core.Money
import com.archieapps.finances.data.dto.ProductDto
import com.archieapps.finances.data.repo.FinanceRepository
import com.archieapps.finances.data.repo.jsonBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class DraftItem(
    val productId: Int? = null,
    val productName: String? = null,
    val name: String,
    val quantity: Double = 1.0,
    val unitPriceCents: Long = 0
) {
    val totalCents: Long get() = Math.round(unitPriceCents * quantity)
}

data class ItemsState(
    val entryAmount: Long = 0,
    val items: List<DraftItem> = emptyList(),
    val draftName: String = "",
    val draftProductId: Int? = null,
    val draftQuantity: String = "1",
    val draftReais: String = "",
    val draftCentavos: String = "",
    val suggestions: List<ProductDto> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null
) {
    val itemsTotal: Long get() = items.sumOf { it.totalCents }

    val difference: Long get() = itemsTotal - entryAmount
}

class ItemsViewModel(
    private val repository: FinanceRepository,
    private val entryId: Int
) : ViewModel() {

    private val _state = MutableStateFlow(ItemsState())
    val state: StateFlow<ItemsState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            when (val result = repository.entry(entryId)) {
                is ApiResult.Ok -> _state.update { current ->
                    current.copy(
                        entryAmount = result.value.amountCents,
                        items = result.value.items.orEmpty().map { item ->
                            DraftItem(
                                productId = item.productId,
                                productName = item.productName,
                                name = item.name,
                                quantity = item.quantity,
                                unitPriceCents = item.unitPriceCents
                            )
                        },
                        loading = false
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun onDraftName(value: String) {
        _state.update { it.copy(draftName = value, draftProductId = null, error = null) }

        if (value.length < 2) {
            _state.update { it.copy(suggestions = emptyList()) }
            return
        }

        viewModelScope.launch {
            when (val result = repository.searchProducts(value, 5)) {
                is ApiResult.Ok -> _state.update { it.copy(suggestions = result.value) }
                is ApiResult.Failure -> Unit
            }
        }
    }

    fun pickProduct(product: ProductDto) = _state.update { current ->
        val price = product.lastPaidCents

        current.copy(
            draftProductId = product.id,
            draftName = product.name,
            suggestions = emptyList(),
            draftReais = price?.let { (it / 100).toString() } ?: current.draftReais,
            draftCentavos = price?.let { (it % 100).toString().padStart(2, '0') } ?: current.draftCentavos
        )
    }

    fun onDraftQuantity(value: String) = _state.update {
        it.copy(draftQuantity = value.filter { char -> char.isDigit() || char == '.' || char == ',' }.take(8))
    }

    fun onDraftReais(value: String) = _state.update { it.copy(draftReais = value) }

    fun onDraftCentavos(value: String) = _state.update { it.copy(draftCentavos = value) }

    fun addDraft() {
        val current = _state.value
        val name = current.draftName.trim()
        val quantity = current.draftQuantity.replace(',', '.').toDoubleOrNull() ?: 1.0
        val price = Money.fromTyped(current.draftReais, current.draftCentavos)

        if (name.isEmpty()) {
            _state.update { it.copy(error = "Informe o produto ou um nome para o item.") }
            return
        }

        if (price <= 0) {
            _state.update { it.copy(error = "Informe o preço do item.") }
            return
        }

        if (quantity <= 0) {
            _state.update { it.copy(error = "A quantidade precisa ser maior que zero.") }
            return
        }

        _state.update { state ->
            state.copy(
                items = state.items + DraftItem(
                    productId = state.draftProductId,
                    productName = if (state.draftProductId != null) name else null,
                    name = name,
                    quantity = quantity,
                    unitPriceCents = price
                ),
                draftName = "",
                draftProductId = null,
                draftQuantity = "1",
                draftReais = "",
                draftCentavos = "",
                suggestions = emptyList(),
                error = null
            )
        }
    }

    fun removeAt(index: Int) = _state.update { state ->
        state.copy(items = state.items.filterIndexed { position, _ -> position != index })
    }

    fun save(onSaved: () -> Unit) {
        if (_state.value.saving) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            val body = buildJsonObject {
                put("items", buildJsonArray {
                    _state.value.items.forEach { item ->
                        add(buildJsonObject {
                            item.productId?.let { put("productId", it) }
                            put("name", item.name)
                            put("quantity", item.quantity)
                            put("unitPrice", item.unitPriceCents)
                        })
                    }
                })
            }

            when (val result = repository.replaceEntryItems(entryId, body)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(saving = false) }
                    onSaved()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }
}
