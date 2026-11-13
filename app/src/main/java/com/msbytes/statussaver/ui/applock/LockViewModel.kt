package com.msbytes.statussaver.ui.applock

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msbytes.statussaver.domain.repository.SettingsRepository
import com.msbytes.statussaver.security.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LockUiState(
    val pin: String = "",
    val wrong: Boolean = false,
    /** Elapsed-realtime until which input is blocked after too many attempts. */
    val lockedUntil: Long = 0,
)

@HiltViewModel
class LockViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appLockManager: AppLockManager,
) : ViewModel() {

    private val _state = MutableStateFlow(LockUiState())
    val state: StateFlow<LockUiState> = _state.asStateFlow()

    val biometricEnabled: StateFlow<Boolean> = settingsRepository.settings
        .map { it.biometricEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var failedAttempts = 0

    fun onDigit(digit: Char) {
        val s = _state.value
        if (SystemClock.elapsedRealtime() < s.lockedUntil || s.pin.length >= PIN_LENGTH) return
        val pin = s.pin + digit
        _state.update { it.copy(pin = pin, wrong = false) }
        if (pin.length == PIN_LENGTH) verify(pin)
    }

    fun onBackspace() = _state.update { it.copy(pin = it.pin.dropLast(1), wrong = false) }

    fun onBiometricSuccess() = appLockManager.unlock()

    private fun verify(pin: String) {
        viewModelScope.launch {
            if (settingsRepository.verifyPin(pin)) {
                failedAttempts = 0
                _state.value = LockUiState()
                appLockManager.unlock()
            } else {
                failedAttempts++
                val lockout = if (failedAttempts >= MAX_ATTEMPTS) {
                    failedAttempts = 0
                    SystemClock.elapsedRealtime() + LOCKOUT_MS
                } else {
                    0L
                }
                _state.update { it.copy(pin = "", wrong = true, lockedUntil = lockout) }
            }
        }
    }

    companion object {
        const val PIN_LENGTH = 4
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_MS = 30_000L
    }
}
