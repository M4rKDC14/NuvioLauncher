package com.nuvio.tv.launcherhome

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Nuvio Launcher: Quick settings → Sleep timer. Each press moves to the next time (15, 30, 60, 90, 120 min, Off).
 * When the time is up, the launcher's button service puts the TV to sleep (standby). Switching the TV off first
 * cancels it (HomeKeyService listens for the screen going off).
 */
object SleepTimer {
    val CHOICES_MINUTES = listOf(0, 15, 30, 60, 90, 120)

    data class State(val minutes: Int, val endsAtWallClock: Long)

    private val _state = MutableStateFlow<State?>(null)
    /** Null when off; otherwise the chosen time and when (System.currentTimeMillis) the TV will sleep. */
    val state: StateFlow<State?> = _state.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val fire = Runnable {
        _state.value = null
        HomeKeyService.sleepNow()
    }

    /** Moves to the next choice: Off → 15 → 30 → 60 → 90 → 120 → Off. */
    fun next() {
        val current = _state.value?.minutes ?: 0
        val i = CHOICES_MINUTES.indexOf(current).coerceAtLeast(0)
        set(CHOICES_MINUTES[(i + 1) % CHOICES_MINUTES.size])
    }

    fun set(minutes: Int) {
        handler.removeCallbacks(fire)
        if (minutes <= 0) {
            _state.value = null
            return
        }
        val delayMs = minutes * 60_000L
        handler.postAtTime(fire, SystemClock.uptimeMillis() + delayMs)
        _state.value = State(minutes, System.currentTimeMillis() + delayMs)
    }

    fun cancel() = set(0)
}
