package com.archieapps.finances.ui.screen.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.data.dto.PriceHistoryDto
import com.archieapps.finances.data.dto.ProductCategoryDto
import com.archieapps.finances.data.dto.ProductDto
import com.archieapps.finances.data.repo.FinanceRepository
import com.archieapps.finances.data.repo.jsonBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put

data class ProductForm(
    val id: Int? = null,
    val name: String = "",
    val brand: String = "",
    val unit: String = "",
    val categoryId: Int? = null,
    val barcode: String = "",
    val tags: String = "",
    val archived: Boolean = false
) {
    val valid: Boolean get() = name.isNotBlank()
}

data class ProductDetail(
    val product: ProductDto,
    val history: List<PriceHistoryDto> = emptyList()
)

data class ProductsState(
    val products: List<ProductDto> = emptyList(),
    val categories: List<ProductCategoryDto> = emptyList(),
    val search: String = "",
    val categoryFilter: Int? = null,
    val page: Int = 1,
    val lastPage: Int = 1,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val form: ProductForm? = null,
    val detail: ProductDetail? = null,
    val error: String? = null
) {
    val hasMore: Boolean get() = page < lastPage
}

class ProductsViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProductsState())
    val state: StateFlow<ProductsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            load(page = 1, append = false)

            when (val categories = repository.productCategories()) {
                is ApiResult.Ok -> _state.update { it.copy(categories = categories.value) }
                is ApiResult.Failure -> Unit
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (!current.hasMore || current.loading) return

        viewModelScope.launch { load(page = current.page + 1, append = true) }
    }

    private suspend fun load(page: Int, append: Boolean) {
        val current = _state.value

        when (val result = repository.products(current.search, current.categoryFilter, page)) {
            is ApiResult.Ok -> _state.update { state ->
                state.copy(
                    products = if (append) state.products + result.value.products else result.value.products,
                    page = result.value.pagination?.current ?: page,
                    lastPage = result.value.pagination?.last ?: page,
                    loading = false
                )
            }
            is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
        }
    }

    fun onSearch(value: String) {
        _state.update { it.copy(search = value) }
        refresh()
    }

    fun setCategory(id: Int?) {
        _state.update { it.copy(categoryFilter = if (it.categoryFilter == id) null else id) }
        refresh()
    }

    fun startCreate() = _state.update { it.copy(form = ProductForm(), error = null) }

    fun startEdit(product: ProductDto) = _state.update {
        it.copy(
            error = null,
            detail = null,
            form = ProductForm(
                id = product.id,
                name = product.name,
                brand = product.brand.orEmpty(),
                unit = product.unit.orEmpty(),
                categoryId = product.categoryId,
                barcode = product.barcode.orEmpty(),
                tags = product.tags?.joinToString(", ").orEmpty(),
                archived = product.archived
            )
        )
    }

    fun dismissForm() = _state.update { it.copy(form = null) }

    fun editForm(update: (ProductForm) -> ProductForm) = _state.update { current ->
        current.copy(form = current.form?.let(update))
    }

    fun openDetail(product: ProductDto) {
        viewModelScope.launch {
            _state.update { it.copy(detail = ProductDetail(product)) }

            when (val history = repository.priceHistory(product.id)) {
                is ApiResult.Ok -> _state.update { current ->
                    current.copy(detail = current.detail?.copy(history = history.value))
                }
                is ApiResult.Failure -> _state.update { it.copy(error = history.message) }
            }
        }
    }

    fun dismissDetail() = _state.update { it.copy(detail = null) }

    fun save() {
        val form = _state.value.form ?: return
        if (!form.valid || _state.value.saving) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            val tags = form.tags.split(',').map(String::trim).filter(String::isNotEmpty)

            val body = jsonBody {
                put("name", form.name.trim())
                form.brand.trim().takeIf { it.isNotEmpty() }?.let { put("brand", it) }
                form.unit.trim().takeIf { it.isNotEmpty() }?.let { put("unit", it) }
                form.barcode.trim().takeIf { it.isNotEmpty() }?.let { put("barcode", it) }
                form.categoryId?.let { put("categoryId", it) }

                if (tags.isNotEmpty()) {
                    put("tags", JsonArray(tags.map { JsonPrimitive(it) }))
                }
            }

            val result = if (form.id == null) {
                repository.createProduct(body)
            } else {
                repository.updateProduct(form.id, body)
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

            when (val result = repository.archiveProduct(id, archived)) {
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

            when (val result = repository.deleteProduct(id)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(saving = false, form = null) }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }
}
