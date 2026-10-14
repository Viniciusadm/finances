package com.archieapps.finances.ui.screen.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.data.dto.CategoryDto
import com.archieapps.finances.data.repo.FinanceRepository
import com.archieapps.finances.data.repo.jsonBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put

data class CategoryForm(
    val id: Int? = null,
    val name: String = "",
    val kind: String = "expense",
    val parentId: Int? = null,
    val color: String? = "primary",
    val active: Boolean = true,
    val isDefault: Boolean = false
) {
    val valid: Boolean get() = name.isNotBlank()
}

data class CategoriesState(
    val categories: List<CategoryDto> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val includeArchived: Boolean = false,
    val kindFilter: String? = null,
    val form: CategoryForm? = null,
    val error: String? = null
) {
    val visible: List<CategoryDto>
        get() = categories.filter { kindFilter == null || it.kind == kindFilter || it.kind == "both" }

    val roots: List<CategoryDto>
        get() = visible.filter { it.parentId == null }

    fun childrenOf(id: Int): List<CategoryDto> = visible.filter { it.parentId == id }

    val parentOptions: List<CategoryDto>
        get() = categories.filter { it.parentId == null }
}

class CategoriesViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(CategoriesState())
    val state: StateFlow<CategoriesState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            when (val result = repository.categories(_state.value.includeArchived)) {
                is ApiResult.Ok -> _state.update { it.copy(categories = result.value, loading = false) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun toggleArchived() {
        _state.update { it.copy(includeArchived = !it.includeArchived) }
        refresh()
    }

    fun setKindFilter(kind: String?) = _state.update { it.copy(kindFilter = kind) }

    fun startCreate() = _state.update { it.copy(form = CategoryForm(), error = null) }

    fun startEdit(category: CategoryDto) = _state.update {
        it.copy(
            error = null,
            form = CategoryForm(
                id = category.id,
                name = category.name,
                kind = category.kind,
                parentId = category.parentId,
                color = category.colorToken ?: category.color,
                active = category.active,
                isDefault = category.isDefault
            )
        )
    }

    fun dismissForm() = _state.update { it.copy(form = null) }

    fun editForm(update: (CategoryForm) -> CategoryForm) = _state.update { current ->
        current.copy(form = current.form?.let(update))
    }

    fun save() {
        val form = _state.value.form ?: return
        if (!form.valid || _state.value.saving) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            val body = jsonBody {
                put("name", form.name.trim())
                put("kind", form.kind)
                form.color?.let { put("color", it) }
                if (form.parentId != null) {
                    put("parentId", form.parentId)
                }
            }

            val result = if (form.id == null) {
                repository.createCategory(body)
            } else {
                repository.updateCategory(form.id, body)
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

    fun setActive(active: Boolean) {
        val id = _state.value.form?.id ?: return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            when (val result = repository.archiveCategory(id, active)) {
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

            when (val result = repository.deleteCategory(id, null)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(saving = false, form = null) }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }
}
