package com.archieapps.finances.ui.screen.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.ApiResult
import com.archieapps.finances.data.net.SessionHolder
import com.archieapps.finances.data.net.SessionStore
import com.archieapps.finances.data.repo.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectState(
    val baseUrl: String = SessionStore.DEFAULT_BASE_URL,
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null
) {
    val canSubmit: Boolean
        get() = baseUrl.isNotBlank() && email.isNotBlank() && password.isNotBlank()
}

class ConnectViewModel(
    private val repository: FinanceRepository,
    private val sessionStore: SessionStore,
    private val sessionHolder: SessionHolder
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectState())
    val state: StateFlow<ConnectState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionStore.session.first()
            _state.update { it.copy(baseUrl = session.baseUrl) }
        }
    }

    fun onBaseUrl(value: String) = _state.update { it.copy(baseUrl = value, error = null) }

    fun onEmail(value: String) = _state.update { it.copy(email = value, error = null) }

    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit || current.loading) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val baseUrl = current.baseUrl.trim()
            sessionHolder.baseUrl = baseUrl
            sessionStore.saveBaseUrl(baseUrl)

            when (val result = repository.login(current.email.trim(), current.password)) {
                is ApiResult.Ok -> {
                    sessionHolder.token = result.value
                    sessionStore.save(result.value, baseUrl)
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
