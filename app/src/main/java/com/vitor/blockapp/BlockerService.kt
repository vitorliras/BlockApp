package com.vitor.blockapp

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.vitor.blockapp.data.BlockedStore
import com.vitor.blockapp.data.SettingsStore
import com.vitor.blockapp.data.homePackages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Fica rodando em segundo plano. Quando a janela em primeiro plano muda para um app
 * que está na lista de bloqueados (e não foi desbloqueado), abre a tela de bloqueio por cima.
 *
 * Também protege o próprio BlockApp: nas telas de Configurações e do desinstalador, se aparecer
 * o BlockApp junto com ações perigosas (desinstalar, desativar, forçar parada, administrador,
 * acessibilidade), pede a senha antes.
 */
class BlockerService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())

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
        scope.launch {
            SettingsStore(applicationContext).settings.collect {
                UnlockState.graceMs = it.graceSeconds * 1000L
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg in ignoredPackages) return

        // Telas do sistema que podem desativar ou desinstalar o BlockApp.
        if (pkg in GUARDED_SYSTEM_PACKAGES) {
            handler.removeCallbacksAndMessages(null)
            if (UnlockState.isUnlocked(pkg)) return

            // Sinal 1, independente de idioma: o nome técnico da tela de administradores do
            // dispositivo é o mesmo em qualquer língua (ex.: "...DeviceAdminSettingsActivity").
            val screenName = event.className?.toString().orEmpty()
            Log.d(TAG, "guard($pkg): tela=$screenName")
            if (pkg == SETTINGS_PACKAGE && screenName.contains(ADMIN_SCREEN_MARKER, ignoreCase = true)) {
                openLock(pkg)
                return
            }

            // Sinal 2: o nome "BlockApp" aparece na tela. O conteúdo demora um instante para
            // carregar, então confere duas vezes.
            for (delay in CHECK_DELAYS_MS) {
                handler.postDelayed({ checkGuardedScreen(pkg) }, delay)
            }
            // Se este app também estiver na lista de bloqueados, segue para o bloqueio normal.
            if (pkg !in blocked) return
        } else {
            handler.removeCallbacksAndMessages(null)
        }

        // A tela inicial nunca é bloqueada (evita ciclo com o botão "voltar para o início").
        if (pkg !in blocked || pkg in homePackages(applicationContext)) {
            UnlockState.onLeftApp()
            return
        }
        if (UnlockState.isUnlocked(pkg)) return

        openLock(pkg)
    }

    /**
     * Procura o nome "BlockApp" no texto da janela atual (o nome do app não é traduzido, então
     * isto funciona em qualquer idioma). Pede a senha no desinstalador e em páginas curtas de
     * detalhe de Configurações; a lista longa de apps não entra, para não pedir senha à toa.
     */
    private fun checkGuardedScreen(pkg: String) {
        val root = rootInActiveWindow
        if (root == null) {
            Log.d(TAG, "guard($pkg): rootInActiveWindow nulo")
            return
        }
        if (root.packageName?.toString() != pkg) {
            Log.d(TAG, "guard($pkg): janela ativa é de ${root.packageName}")
            return
        }
        if (UnlockState.isUnlocked(pkg)) return

        val texts = StringBuilder()
        collectText(root, texts, budget = intArrayOf(MAX_NODES))
        val screen = texts.toString().lowercase()

        val mentionsBlockApp = screen.contains("blockapp")
        val isInstaller = pkg != SETTINGS_PACKAGE
        val shouldLock = mentionsBlockApp && (isInstaller || screen.length < DETAIL_PAGE_MAX_LETTERS)

        Log.d(TAG, "guard($pkg): ${screen.length} letras, blockapp=$mentionsBlockApp, trava=$shouldLock")
        if (shouldLock) openLock(pkg)
    }

    private fun collectText(node: AccessibilityNodeInfo?, out: StringBuilder, budget: IntArray) {
        if (node == null || budget[0] <= 0) return
        budget[0]--
        node.text?.let { out.append(it).append(' ') }
        node.contentDescription?.let { out.append(it).append(' ') }
        for (i in 0 until node.childCount) collectText(node.getChild(i), out, budget)
    }

    private fun openLock(pkg: String) {
        startActivity(
            Intent(this, LockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(LockActivity.EXTRA_PACKAGE, pkg)
        )
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: IllegalArgumentException) {
        }
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "BlockAppGuard"
        const val SETTINGS_PACKAGE = "com.android.settings"
        const val DETAIL_PAGE_MAX_LETTERS = 800

        // Parte do nome técnico (não traduzido) das telas de administradores do dispositivo.
        const val ADMIN_SCREEN_MARKER = "DeviceAdmin"

        val GUARDED_SYSTEM_PACKAGES = setOf(
            SETTINGS_PACKAGE,
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
        )
        val CHECK_DELAYS_MS = longArrayOf(250L, 900L)
        const val MAX_NODES = 600
    }
}
