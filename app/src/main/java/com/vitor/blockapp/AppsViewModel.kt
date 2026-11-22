package com.vitor.blockapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vitor.blockapp.data.BlockedStore
import com.vitor.blockapp.data.InstalledApp
import com.vitor.blockapp.data.LockMethod
import com.vitor.blockapp.data.SettingsStore
import com.vitor.blockapp.data.loadLaunchableApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [tag] aparece ao lado de "Bloqueado" quando o app tem configuração própria de senha. */
data class AppItem(val app: InstalledApp, val blocked: Boolean, val tag: String? = null)

enum class AppFilter(val label: String) {
    ALL("Todos"),
    BLOCKED("Bloqueados"),
    FREE("Liberados"),
}

data class AppsUiState(
    val loading: Boolean = true,
    val items: List<AppItem> = emptyList(),
    val blockedCount: Int = 0,
    val totalCount: Int = 0,
    val query: String = "",
    val filter: AppFilter = AppFilter.ALL,
)

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = BlockedStore(application)
    private val settingsStore = SettingsStore(application)
    private val apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val loading = MutableStateFlow(true)
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(AppFilter.ALL)

    private val base = combine(apps, store.blocked, settingsStore.settings) { all, blocked, settings ->
        Triple(all, blocked, settings)
    }

    val state: StateFlow<AppsUiState> = combine(base, query, filter, loading) { (all, blocked, settings), q, f, isLoading ->
        val items = all
            .filter { it.label.contains(q.trim(), ignoreCase = true) }
            .map {
                AppItem(
                    app = it,
                    blocked = it.packageName in blocked,
                    tag = when (settings.perApp[it.packageName]?.method) {
                        null -> null
                        LockMethod.PHONE -> "senha do celular"
                        LockMethod.PIN -> "PIN próprio"
                        LockMethod.PATTERN -> "padrão próprio"
                    },
                )
            }
            .filter {
                when (f) {
                    AppFilter.ALL -> true
                    AppFilter.BLOCKED -> it.blocked
                    AppFilter.FREE -> !it.blocked
                }
            }
            // Bloqueados primeiro, depois ordem alfabética.
            .sortedWith(compareByDescending<AppItem> { it.blocked }.thenBy { it.app.label.lowercase() })
        AppsUiState(
            loading = isLoading,
            items = items,
            blockedCount = all.count { it.packageName in blocked },
            totalCount = all.size,
            query = q,
            filter = f,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())

    init {
        viewModelScope.launch(Dispatchers.Default) {
            apps.value = loadLaunchableApps(application)
            loading.value = false
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: AppFilter) {
        filter.value = value
    }

    fun setBlocked(packageName: String, blocked: Boolean) {
        viewModelScope.launch { store.setBlocked(packageName, blocked) }
    }
}
