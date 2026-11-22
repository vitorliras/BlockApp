package com.vitor.blockapp

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.vitor.blockapp.data.LockMethod
import com.vitor.blockapp.data.SecretHasher
import com.vitor.blockapp.data.SettingsStore
import com.vitor.blockapp.ui.LockScreen
import com.vitor.blockapp.ui.theme.BlockAppTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Tela que cobre o app bloqueado. Conforme a configuração do app, pede a senha do celular
 * (digital, rosto, PIN, padrão) ou o PIN/padrão do BlockApp.
 * (BiometricPrompt exige FragmentActivity.)
 */
class LockActivity : FragmentActivity() {

    private var blockedPackage by mutableStateOf("")
    private var mode by mutableStateOf<LockMode?>(null)
    private var wrong by mutableStateOf(false)
    private var lockedSeconds by mutableIntStateOf(0)

    private var authenticating = false
    private var countdownJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        blockedPackage = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()

        onBackPressedDispatcher.addCallback(this) { goHome() }

        setContent {
            BlockAppTheme {
                LockScreen(
                    packageName = blockedPackage,
                    mode = mode,
                    wrong = wrong,
                    lockedSeconds = lockedSeconds,
                    onUnlockPhone = ::authenticateWithPhone,
                    onSecret = ::onSecretEntered,
                    onInputStarted = { wrong = false },
                    onGoHome = ::goHome,
                )
            }
        }

        loadMode()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        blockedPackage = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        loadMode()
    }

    override fun onStop() {
        super.onStop()
        // Se o usuário saiu desta tela (botão início, recentes), ela não deve ficar pendurada.
        if (!authenticating && !isChangingConfigurations) finishAndRemoveTask()
    }

    /** Descobre como este app deve ser desbloqueado (padrão geral ou configuração própria). */
    private fun loadMode() {
        val pkg = blockedPackage
        if (pkg.isEmpty()) return
        lifecycleScope.launch {
            val settings = SettingsStore(applicationContext).settings.first()
            val method = settings.methodFor(pkg)
            val secret = settings.secretFor(pkg, method)
            wrong = false
            mode = if (method == LockMethod.PHONE || secret == null) {
                LockMode.Phone
            } else {
                LockMode.Secret(method, secret)
            }
            startCountdownIfLocked()
            if (mode is LockMode.Phone) authenticateWithPhone()
        }
    }

    private fun onSecretEntered(secret: String) {
        val current = mode as? LockMode.Secret ?: return
        if (AttemptLimiter.remainingMs() > 0) return

        if (SecretHasher.verify(secret, current.hash)) {
            AttemptLimiter.registerSuccess()
            unlockAndClose()
        } else {
            AttemptLimiter.registerFail()
            wrong = true
            startCountdownIfLocked()
        }
    }

    private fun startCountdownIfLocked() {
        if (AttemptLimiter.remainingMs() <= 0) {
            lockedSeconds = 0
            return
        }
        countdownJob?.cancel()
        countdownJob = lifecycleScope.launch {
            while (true) {
                val remaining = AttemptLimiter.remainingMs()
                if (remaining <= 0) break
                lockedSeconds = ((remaining + 999) / 1000).toInt()
                delay(250)
            }
            lockedSeconds = 0
            wrong = false
        }
    }

    private fun authenticateWithPhone() {
        if (authenticating || blockedPackage.isEmpty()) return
        authenticating = true

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticating = false
                    unlockAndClose()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Cancelou ou falhou: continua na tela de bloqueio.
                    authenticating = false
                }
            },
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.bio_unlock_title, appLabel()))
            .setSubtitle(getString(R.string.bio_subtitle))
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(info)
    }

    private fun unlockAndClose() {
        UnlockState.unlock(blockedPackage)
        finishAndRemoveTask()
    }

    private fun appLabel(): String = try {
        packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(blockedPackage, 0)
        ).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        blockedPackage
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finishAndRemoveTask()
    }

    companion object {
        const val EXTRA_PACKAGE = "blocked_package"
    }
}
