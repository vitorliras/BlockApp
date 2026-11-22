package com.vitor.blockapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vitor.blockapp.data.LockMethod
import com.vitor.blockapp.data.LockSettings
import com.vitor.blockapp.data.SecretHasher
import com.vitor.blockapp.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SettingsStore(application)

    val settings: StateFlow<LockSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, LockSettings())

    fun setDefaultMethod(method: LockMethod) {
        viewModelScope.launch { store.setDefaultMethod(method) }
    }

    /** Salva o PIN/padrão geral e já passa a usá-lo como método padrão. */
    fun saveGlobalSecret(kind: LockMethod, secret: String) {
        viewModelScope.launch(Dispatchers.Default) {
            store.setGlobalSecret(kind, SecretHasher.hash(secret))
            store.setDefaultMethod(kind)
        }
    }

    /** [method] nulo = usar o padrão geral. */
    fun setAppMethod(packageName: String, method: LockMethod?) {
        viewModelScope.launch { store.updateApp(packageName) { it.copy(method = method) } }
    }

    /** Salva o PIN/padrão próprio do app e já passa a usá-lo. */
    fun saveAppSecret(packageName: String, kind: LockMethod, secret: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val hash = SecretHasher.hash(secret)
            store.updateApp(packageName) {
                when (kind) {
                    LockMethod.PIN -> it.copy(method = kind, pin = hash)
                    LockMethod.PATTERN -> it.copy(method = kind, pattern = hash)
                    LockMethod.PHONE -> it.copy(method = kind)
                }
            }
        }
    }
}
