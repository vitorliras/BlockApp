package com.vitor.blockapp.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** Único arquivo de dados do app (lista de bloqueados + configurações de senha). */
internal val Context.dataStore by preferencesDataStore(name = "blockapp")
