package com.vitor.blockapp

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitor.blockapp.data.LockMethod
import com.vitor.blockapp.ui.AppConfigScreen
import com.vitor.blockapp.ui.AppListScreen
import com.vitor.blockapp.ui.GateScreen
import com.vitor.blockapp.ui.SecurityScreen
import com.vitor.blockapp.ui.SetupScreen
import com.vitor.blockapp.ui.theme.BlockAppTheme

/** Telas do app. A navegação é uma pilha simples: a última da lista é a que aparece. */
sealed interface Screen {
    data object Home : Screen
    data object Security : Screen
    data class AppConfig(val packageName: String) : Screen

    /** Criar PIN/padrão. [packageName] nulo = senha geral; senão, senha própria daquele app. */
    data class Setup(val kind: LockMethod, val packageName: String?) : Screen
}

/**
 * Tela principal. Para abrir o BlockApp é preciso a digital ou a senha do celular, senão qualquer
 * pessoa com o celular em mãos poderia desmarcar apps ou trocar senhas.
 * (BiometricPrompt exige FragmentActivity.)
 */
class MainActivity : FragmentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    private var unlocked by mutableStateOf(false)
    private var authenticating = false
    private var leftAt = 0L // 0 = ainda dentro do app

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockAppTheme {
                if (unlocked) {
                    AppContent(settingsViewModel)
                } else {
                    GateScreen(onUnlock = ::requestAuth)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Voltou depois de mais tempo que a carência? Pede a senha de novo.
        val graceMs = settingsViewModel.settings.value.graceSeconds * 1000L
        if (unlocked && leftAt != 0L && SystemClock.elapsedRealtime() - leftAt > graceMs) {
            unlocked = false
        }
        if (unlocked) leftAt = 0L else requestAuth()
    }

    override fun onStop() {
        super.onStop()
        if (unlocked && !isChangingConfigurations) leftAt = SystemClock.elapsedRealtime()
    }

    private fun requestAuth() {
        if (authenticating || unlocked) return

        // Sem nenhuma senha/biometria cadastrada no celular não há como proteger: deixa entrar.
        val canAuth = BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            unlocked = true
            return
        }

        authenticating = true
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticating = false
                    leftAt = 0L
                    unlocked = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authenticating = false
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.bio_open_title))
                .setSubtitle(getString(R.string.bio_subtitle))
                .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                .build()
        )
    }
}

@androidx.compose.runtime.Composable
private fun AppContent(settingsViewModel: SettingsViewModel) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var stack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Home)) }

    fun push(screen: Screen) {
        stack = stack + screen
    }

    fun pop() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    when (val screen = stack.last()) {
        Screen.Home -> AppListScreen(
            listState = listState,
            onOpenSecurity = { push(Screen.Security) },
            onOpenApp = { push(Screen.AppConfig(it)) },
        )

        Screen.Security -> SecurityScreen(
            settings = settings,
            onBack = ::pop,
            onSelectDefault = settingsViewModel::setDefaultMethod,
            onSetup = { push(Screen.Setup(it, null)) },
            onSelectGrace = settingsViewModel::setGraceSeconds,
        )

        is Screen.AppConfig -> AppConfigScreen(
            packageName = screen.packageName,
            settings = settings,
            onBack = ::pop,
            onSelectMethod = { settingsViewModel.setAppMethod(screen.packageName, it) },
            onSetup = { push(Screen.Setup(it, screen.packageName)) },
        )

        is Screen.Setup -> SetupScreen(
            kind = screen.kind,
            title = stringResource(
                if (screen.kind == LockMethod.PIN) R.string.pin_blockapp else R.string.pattern_blockapp
            ),
            onCancel = ::pop,
            onDone = { secret ->
                if (screen.packageName == null) {
                    settingsViewModel.saveGlobalSecret(screen.kind, secret)
                } else {
                    settingsViewModel.saveAppSecret(screen.packageName, screen.kind, secret)
                }
                pop()
            },
        )
    }
}
