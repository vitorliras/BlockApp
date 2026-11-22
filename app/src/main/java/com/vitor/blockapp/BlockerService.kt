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
            // O conteúdo da tela demora um instante para carregar: confere duas vezes.
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

    /** Lê o texto da janela atual e, se mostrar o BlockApp com uma ação perigosa, pede a senha. */
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
        val dangerous = DANGEROUS_KEYWORDS.any { screen.contains(it) }

        // 1) Tela de administradores do dispositivo: protegida sempre, mesmo que o texto dela
        //    não traga o nome do BlockApp (acontece em alguns caminhos, como o link da notificação).
        val adminPage = pkg == SETTINGS_PACKAGE && ADMIN_PAGE_PHRASES.any { screen.contains(it) }
        // 2) Tela que cita o BlockApp com ação perigosa, ou página curta de detalhe dele
        //    (a lista longa de apps não entra, para não pedir senha à toa).
        val blockAppPage = mentionsBlockApp && (dangerous || screen.length < DETAIL_PAGE_MAX_LETTERS)

        Log.d(
            TAG,
            "guard($pkg): ${screen.length} letras, blockapp=$mentionsBlockApp, perigosa=$dangerous, " +
                "admin=$adminPage, trava=${adminPage || blockAppPage}",
        )
        if (adminPage || blockAppPage) openLock(pkg)
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

        // Frases que identificam a tela de apps administradores (pt e en, minúsculas).
        val ADMIN_PAGE_PHRASES = listOf(
            "apps administradores", "aplicativos administradores", "administradores do dispositivo",
            "administrador do dispositivo", "app administrador", "device admin", "device administrator",
        )
        val GUARDED_SYSTEM_PACKAGES = setOf(
            "com.android.settings",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
        )
        val CHECK_DELAYS_MS = longArrayOf(250L, 900L)
        const val MAX_NODES = 600

        // Em português e inglês, em minúsculas.
        val DANGEROUS_KEYWORDS = listOf(
            "desinstal", "forçar parada", "forçar a parada", "desativar", "desative",
            "administrador", "acessibilidade", "limpar dados", "armazenamento",
            "uninstall", "force stop", "deactivate", "device admin", "accessibility", "clear data",
        )
    }
}
