package com.vitor.blockapp

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.vitor.blockapp.data.BlockedStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Fica rodando em segundo plano. Quando a janela em primeiro plano muda para um app
 * que está na lista de bloqueados (e não foi desbloqueado), abre a tela de bloqueio por cima.
 */
class BlockerService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile
    private var blocked: Set<String> = emptySet()

    // Teclados e a barra do sistema não contam como "saí do app".
    private var ignoredPackages: Set<String> = setOf("com.android.systemui")

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            UnlockState.reset()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        ignoredPackages = imm.enabledInputMethodList.map { it.packageName }.toSet() +
            "com.android.systemui"

        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        scope.launch {
            BlockedStore(applicationContext).blocked.collect { blocked = it }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg in ignoredPackages) return

        if (pkg !in blocked) {
            UnlockState.onLeftApp()
            return
        }
        if (UnlockState.isUnlocked(pkg)) return

        startActivity(
            Intent(this, LockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(LockActivity.EXTRA_PACKAGE, pkg)
        )
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: IllegalArgumentException) {
        }
        scope.cancel()
        super.onDestroy()
    }
}
