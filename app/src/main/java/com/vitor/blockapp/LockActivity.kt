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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.vitor.blockapp.ui.LockScreen
import com.vitor.blockapp.ui.theme.BlockAppTheme

/**
 * Tela que cobre o app bloqueado. Pede a digital, o rosto ou a senha/PIN/padrão do celular.
 * (BiometricPrompt exige FragmentActivity.)
 */
class LockActivity : FragmentActivity() {

    private var blockedPackage by mutableStateOf("")
    private var authenticating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        blockedPackage = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()

        onBackPressedDispatcher.addCallback(this) { goHome() }

        setContent {
            BlockAppTheme {
                LockScreen(
                    packageName = blockedPackage,
                    onUnlock = ::authenticate,
                    onGoHome = ::goHome,
                )
            }
        }

        // Já abre o pedido de digital/senha sozinho.
        window.decorView.post { authenticate() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        blockedPackage = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        window.decorView.post { authenticate() }
    }

    override fun onStop() {
        super.onStop()
        // Se o usuário saiu desta tela (botão início, recentes), ela não deve ficar pendurada.
        if (!authenticating && !isChangingConfigurations) finishAndRemoveTask()
    }

    private fun authenticate() {
        if (authenticating || blockedPackage.isEmpty()) return
        authenticating = true

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticating = false
                    UnlockState.unlock(blockedPackage)
                    finishAndRemoveTask()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Cancelou ou falhou: continua na tela de bloqueio.
                    authenticating = false
                }
            },
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear ${appLabel()}")
            .setSubtitle("Use a digital ou a senha do celular")
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(info)
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
