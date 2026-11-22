package com.vitor.blockapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/** Guarda no celular o método de desbloqueio (geral e por app) e os hashes das senhas. */
class SettingsStore(private val context: Context) {
    private val defaultMethodKey = stringPreferencesKey("default_method")
    private val globalPinKey = stringPreferencesKey("global_pin")
    private val globalPatternKey = stringPreferencesKey("global_pattern")
    private val graceKey = intPreferencesKey("grace_seconds")

    private fun appKey(packageName: String) = stringPreferencesKey("cfg_$packageName")

    val settings: Flow<LockSettings> = context.dataStore.data.map { prefs ->
        val perApp = prefs.asMap().entries
            .filter { it.key.name.startsWith(APP_PREFIX) && it.value is String }
            .associate { it.key.name.removePrefix(APP_PREFIX) to parse(it.value as String) }

        LockSettings(
            defaultMethod = prefs[defaultMethodKey]?.toMethod() ?: LockMethod.PHONE,
            globalPin = prefs[globalPinKey],
            globalPattern = prefs[globalPatternKey],
            perApp = perApp,
            graceSeconds = prefs[graceKey] ?: 15,
        )
    }

    suspend fun setGraceSeconds(seconds: Int) {
        context.dataStore.edit { it[graceKey] = seconds }
    }

    suspend fun setDefaultMethod(method: LockMethod) {
        context.dataStore.edit { it[defaultMethodKey] = method.name }
    }

    /** [hash] já deve vir transformado por [SecretHasher.hash]. */
    suspend fun setGlobalSecret(kind: LockMethod, hash: String) {
        context.dataStore.edit {
            when (kind) {
                LockMethod.PIN -> it[globalPinKey] = hash
                LockMethod.PATTERN -> it[globalPatternKey] = hash
                LockMethod.PHONE -> Unit
            }
        }
    }

    suspend fun updateApp(packageName: String, change: (AppLockConfig) -> AppLockConfig) {
        context.dataStore.edit { prefs ->
            val current = prefs[appKey(packageName)]?.let(::parse) ?: AppLockConfig()
            prefs[appKey(packageName)] = serialize(change(current))
        }
    }

    private fun String.toMethod(): LockMethod? = runCatching { LockMethod.valueOf(this) }.getOrNull()

    private fun parse(json: String): AppLockConfig {
        val o = JSONObject(json)
        return AppLockConfig(
            method = o.optString("method", "").takeIf { it.isNotEmpty() }?.toMethod(),
            pin = o.optString("pin", "").takeIf { it.isNotEmpty() },
            pattern = o.optString("pattern", "").takeIf { it.isNotEmpty() },
        )
    }

    private fun serialize(cfg: AppLockConfig): String = JSONObject().apply {
        cfg.method?.let { put("method", it.name) }
        cfg.pin?.let { put("pin", it) }
        cfg.pattern?.let { put("pattern", it) }
    }.toString()

    private companion object {
        const val APP_PREFIX = "cfg_"
    }
}
