package com.archieapps.finances.ui.screen.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.data.dto.TagDto
import com.archieapps.finances.data.repo.FinanceRepository
import com.archieapps.finances.data.repo.jsonBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put

data class TagForm(
    val id: Int? = null,
    val name: String = "",
    val color: String? = "primary",
    val mergeInto: Int? = null
) {
    val valid: Boolean get() = name.isNotBlank()
}

data class TagsState(
    val tags: List<TagDto> = emptyList(),
    val search: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val form: TagForm? = null,
    val error: String? = null
)

class TagsViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(TagsState())
    val state: StateFlow<TagsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            when (val result = repository.tags(_state.value.search)) {
                is ApiResult.Ok -> _state.update { it.copy(tags = result.value, loading = false) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun onSearch(value: String) {
        _state.update { it.copy(search = value) }
        refresh()
    }

    fun startCreate() = _state.update { it.copy(form = TagForm(), error = null) }

    fun startEdit(tag: TagDto) = _state.update {
        it.copy(
            error = null,
            form = TagForm(id = tag.id, name = tag.name, color = tag.colorToken ?: tag.color)
        )
    }

    fun dismissForm() = _state.update { it.copy(form = null) }

    fun editForm(update: (TagForm) -> TagForm) = _state.update { current ->
        current.copy(form = current.form?.let(update))
    }

    fun save() {
        val form = _state.value.form ?: return
        if (!form.valid || _state.value.saving) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            val body = jsonBody {
                put("name", form.name.trim())
                form.color?.let { put("color", it) }
            }

            val result = if (form.id == null) {
                repository.createTag(body)
            } else {
                repository.updateTag(form.id, body)
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

    fun merge() {
        val form = _state.value.form ?: return
        val id = form.id ?: return
        val into = form.mergeInto ?: return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            when (val result = repository.mergeTag(id, into)) {
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

            when (val result = repository.deleteTag(id)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(saving = false, form = null) }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }
}
