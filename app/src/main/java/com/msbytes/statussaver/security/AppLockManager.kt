package com.msbytes.statussaver.security

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Locks the app on cold start and after it was in the background for
 * more than [GRACE_PERIOD_MS] (short trips to the share sheet don't re-lock).
 */
@Singleton
class AppLockManager @Inject constructor() : DefaultLifecycleObserver {

    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var enabled = false
    private var initialized = false
    private var backgroundedAt = 0L

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /** Called whenever settings change. The first call decides the cold-start lock. */
    fun onSettingsLoaded(lockEnabled: Boolean) {
        enabled = lockEnabled
        if (!initialized) {
            initialized = true
            _locked.value = lockEnabled
        }
        if (!lockEnabled) _locked.value = false
    }

    fun unlock() {
        _locked.value = false
    }

    override fun onStop(owner: LifecycleOwner) {
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart(owner: LifecycleOwner) {
        if (enabled && backgroundedAt > 0 && SystemClock.elapsedRealtime() - backgroundedAt > GRACE_PERIOD_MS) {
            _locked.value = true
        }
    }

    private companion object {
        const val GRACE_PERIOD_MS = 30_000L
    }
}
