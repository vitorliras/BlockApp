package com.vitor.blockapp

import android.os.SystemClock

/** Trava por 30 segundos depois de 5 senhas erradas seguidas (vale para PIN e padrão). */
object AttemptLimiter {
    private const val MAX_FAILS = 5
    private const val LOCK_MS = 30_000L

    private var fails = 0
    private var lockedUntil = 0L

    @Synchronized
    fun remainingMs(): Long = maxOf(0L, lockedUntil - SystemClock.elapsedRealtime())

    @Synchronized
    fun registerFail() {
        fails++
        if (fails >= MAX_FAILS) {
            fails = 0
            lockedUntil = SystemClock.elapsedRealtime() + LOCK_MS
        }
    }

    @Synchronized
    fun registerSuccess() {
        fails = 0
        lockedUntil = 0L
    }
}
