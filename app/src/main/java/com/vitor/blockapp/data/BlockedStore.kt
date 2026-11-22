package com.vitor.blockapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Guarda no celular quais apps estão bloqueados (lista de nomes de pacote). */
class BlockedStore(private val context: Context) {
    private val key = stringSetPreferencesKey("blocked_packages")

    val blocked: Flow<Set<String>> = context.dataStore.data.map { it[key] ?: emptySet() }

    suspend fun setBlocked(packageName: String, blocked: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            prefs[key] = if (blocked) current + packageName else current - packageName
        }
    }
}
