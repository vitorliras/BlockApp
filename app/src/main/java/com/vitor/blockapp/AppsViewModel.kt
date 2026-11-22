package com.vitor.blockapp

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vitor.blockapp.data.BlockedStore
import com.vitor.blockapp.data.InstalledApp
import com.vitor.blockapp.data.LockMethod
import com.vitor.blockapp.data.SettingsStore
import com.vitor.blockapp.data.homePackages
import com.vitor.blockapp.data.loadLaunchableApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [tagRes] é o texto (traduzido) que aparece ao lado de "Bloqueado" quando o app tem senha própria. */
data class AppItem(val app: InstalledApp, val blocked: Boolean, @StringRes val tagRes: Int? = null)

enum class AppFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    BLOCKED(R.string.filter_blocked),
    FREE(R.string.filter_free),
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
    private val homePackages: Set<String> = homePackages(application)

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
                    tagRes = when (settings.perApp[it.packageName]?.method) {
                        null -> null
                        LockMethod.PHONE -> R.string.tag_phone
                        LockMethod.PIN -> R.string.tag_pin
                        LockMethod.PATTERN -> R.string.tag_pattern
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

    /**
     * Apps que mudariam de estado num "bloquear todos" ([blocked] = true) ou "liberar todos".
     * Vale para os apps que estão aparecendo na lista (respeita busca e filtro).
     * A tela inicial do celular nunca entra, para evitar ciclo de bloqueio.
     */
    fun bulkTargets(blocked: Boolean): List<String> = state.value.items
        .filter { it.blocked != blocked && it.app.packageName !in homePackages }
        .map { it.app.packageName }

    fun setBlockedMany(packageNames: List<String>, blocked: Boolean) {
        viewModelScope.launch { store.setBlockedMany(packageNames, blocked) }
    }

    fun onFilterChange(value: AppFilter) {
        filter.value = value
    }

    fun setBlocked(packageName: String, blocked: Boolean) {
        viewModelScope.launch { store.setBlocked(packageName, blocked) }
    }
}
