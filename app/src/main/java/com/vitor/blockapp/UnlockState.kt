package com.vitor.blockapp

import android.os.SystemClock

/**
 * Lembra qual app foi desbloqueado e por quanto tempo ele continua liberado.
 *
 * Regras:
 * - Enquanto você está usando o app desbloqueado, ele fica liberado.
 * - Se você sair dele (início, outro app), há uma carência curta para voltar sem senha.
 * - Passada a carência, ou ao apagar a tela, pede a senha de novo.
 */
object UnlockState {
    private const val GRACE_MS = 15_000L

    private var unlockedPackage: String? = null
    private var leftAt: Long = 0L // 0 = ainda está dentro do app

    @Synchronized
    fun unlock(packageName: String) {
        unlockedPackage = packageName
        leftAt = 0L
    }

    /** O app [packageName] veio para a frente: ainda vale o desbloqueio? */
    @Synchronized
    fun isUnlocked(packageName: String): Boolean {
        if (unlockedPackage != packageName) return false
        if (leftAt == 0L) return true
        if (SystemClock.elapsedRealtime() - leftAt <= GRACE_MS) {
            leftAt = 0L
            return true
        }
        unlockedPackage = null
        return false
    }

    /** Outro app (não bloqueado) veio para a frente: começa a contar a carência. */
    @Synchronized
    fun onLeftApp() {
        if (unlockedPackage != null && leftAt == 0L) {
            leftAt = SystemClock.elapsedRealtime()
        }
    }

    @Synchronized
    fun reset() {
        unlockedPackage = null
        leftAt = 0L
    }
}
