package com.archieapps.finances.ui.screen.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.data.dto.EstablishmentDto
import com.archieapps.finances.data.repo.FinanceRepository
import com.archieapps.finances.data.repo.jsonBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put

data class PlaceForm(
    val id: Int? = null,
    val name: String = "",
    val newUnit: String = "",
    val mergeInto: Int? = null
) {
    val valid: Boolean get() = name.isNotBlank()
}

data class PlacesState(
    val establishments: List<EstablishmentDto> = emptyList(),
    val search: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val form: PlaceForm? = null,
    val error: String? = null
)

class PlacesViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(PlacesState())
    val state: StateFlow<PlacesState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            when (val result = repository.establishments(_state.value.search)) {
                is ApiResult.Ok -> _state.update { it.copy(establishments = result.value, loading = false) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun onSearch(value: String) {
        _state.update { it.copy(search = value) }
        refresh()
    }

    fun startCreate() = _state.update { it.copy(form = PlaceForm(), error = null) }

    fun startEdit(establishment: EstablishmentDto) = _state.update {
        it.copy(error = null, form = PlaceForm(id = establishment.id, name = establishment.name))
    }

    fun dismissForm() = _state.update { it.copy(form = null) }

    fun editForm(update: (PlaceForm) -> PlaceForm) = _state.update { current ->
        current.copy(form = current.form?.let(update))
    }

    fun save() {
        val form = _state.value.form ?: return
        if (!form.valid || _state.value.saving) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            val body = jsonBody {
                put("name", form.name.trim())
                if (form.id == null && form.newUnit.isNotBlank()) put("unit", form.newUnit.trim())
            }

            val result = if (form.id == null) {
                repository.createEstablishment(body)
            } else {
                repository.updateEstablishment(form.id, body)
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

    fun addUnit() {
        val form = _state.value.form ?: return
        val id = form.id ?: return
        if (form.newUnit.isBlank() || _state.value.saving) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            when (val result = repository.createPlace(id, form.newUnit.trim())) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(saving = false, form = it.form?.copy(newUnit = "")) }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }

    fun removeUnit(placeId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            when (val result = repository.deletePlace(placeId)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(saving = false) }
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

            when (val result = repository.mergeEstablishment(id, into)) {
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

            when (val result = repository.deleteEstablishment(id)) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(saving = false, form = null) }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }
}
