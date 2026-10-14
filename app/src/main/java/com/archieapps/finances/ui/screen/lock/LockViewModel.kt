package com.archieapps.finances.ui.screen.lock

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.finances.core.session.BiometricAvailability
import com.archieapps.finances.core.session.CodeGate
import com.archieapps.finances.core.session.CodePad
import com.archieapps.finances.core.session.CodeSubmitResult
import com.archieapps.finances.core.session.UnlockResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LockState(
    val checking: Boolean = true,
    val askingCode: Boolean = false,
    val keypad: List<String> = CodePad.shuffleKeypad(),
    val chosen: List<String?> = List(CodePad.SLOTS) { null },
    val sending: Boolean = false,
    val hardStopped: Boolean = false,
    val remaining: Int? = null,
    val error: String? = null,
    val unlocked: Boolean = false,
    val biometricsMissing: Boolean = false
) {
    val typed: Int get() = chosen.count { it != null }

    val complete: Boolean get() = CodePad.assemble(chosen) != null
}

class LockViewModel(private val gate: CodeGate) : ViewModel() {

    private val _state = MutableStateFlow(LockState())
    val state: StateFlow<LockState> = _state.asStateFlow()

    fun start(activity: FragmentActivity) {
        if (_state.value.unlocked || _state.value.sending) return

        viewModelScope.launch {
            _state.update { it.copy(checking = true, error = null) }

            if (gate.unlocked) {
                _state.update { it.copy(checking = false, unlocked = true) }

                return@launch
            }

            val availability = gate.availability()
            val missing = availability == BiometricAvailability.NoneEnrolled ||
                availability == BiometricAvailability.Unavailable

            when (val result = gate.unlock(activity)) {
                UnlockResult.Unlocked -> _state.update {
                    it.copy(checking = false, unlocked = true)
                }

                UnlockResult.NeedsCode -> _state.update {
                    it.copy(
                        checking = false,
                        askingCode = true,
                        biometricsMissing = missing,
                        keypad = CodePad.shuffleKeypad(),
                        chosen = List(CodePad.SLOTS) { null }
                    )
                }

                UnlockResult.Canceled -> _state.update {
                    it.copy(checking = false, error = "Confirme a digital para abrir.")
                }

                is UnlockResult.Blocked -> _state.update {
                    it.copy(checking = false, askingCode = true, error = result.message)
                }
            }
        }
    }

    fun pick(option: String) = _state.update { current ->
        val slot = current.chosen.indexOfFirst { it == null }

        if (slot == -1) {
            current
        } else {
            current.copy(
                chosen = current.chosen.toMutableList().also { it[slot] = option },
                error = null
            )
        }
    }

    fun clear() = _state.update {
        it.copy(chosen = List(CodePad.SLOTS) { null }, error = null)
    }

    fun submit(activity: FragmentActivity) {
        val current = _state.value
        val code = CodePad.assemble(current.chosen) ?: return

        if (current.sending) return

        viewModelScope.launch {
            _state.update { it.copy(sending = true, error = null) }

            when (val result = gate.submitCode(activity, code)) {
                CodeSubmitResult.Stored -> _state.update {
                    it.copy(sending = false, unlocked = true, askingCode = false)
                }

                is CodeSubmitResult.Rejected -> _state.update {
                    it.copy(
                        sending = false,
                        error = result.message,
                        remaining = result.remaining,
                        keypad = CodePad.shuffleKeypad(),
                        chosen = List(CodePad.SLOTS) { null }
                    )
                }

                CodeSubmitResult.HardStopped -> _state.update {
                    it.copy(sending = false, hardStopped = true, error = "Muitas tentativas. Tente amanhã.")
                }

                is CodeSubmitResult.Failed -> _state.update {
                    it.copy(
                        sending = false,
                        error = result.message,
                        keypad = CodePad.shuffleKeypad(),
                        chosen = List(CodePad.SLOTS) { null }
                    )
                }
            }
        }
    }

    fun retryBiometrics(activity: FragmentActivity) {
        _state.update { it.copy(error = null, askingCode = false) }
        start(activity)
    }
}
