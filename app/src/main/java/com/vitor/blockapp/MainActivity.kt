package com.vitor.blockapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitor.blockapp.data.LockMethod
import com.vitor.blockapp.ui.AppConfigScreen
import com.vitor.blockapp.ui.AppListScreen
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockAppTheme {
                val settingsViewModel: SettingsViewModel = viewModel()
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
                        title = if (screen.kind == LockMethod.PIN) "PIN do BlockApp" else "Padrão do BlockApp",
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
        }
    }
}
