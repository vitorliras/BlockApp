package com.vitor.blockapp

import com.vitor.blockapp.data.LockMethod

/** Como a tela de bloqueio pede o desbloqueio para um app específico. */
sealed interface LockMode {
    /** Digital, rosto ou senha/PIN/padrão do próprio celular. */
    data object Phone : LockMode

    /** PIN ou padrão criado no BlockApp. [hash] é o hash do segredo esperado. */
    data class Secret(val kind: LockMethod, val hash: String) : LockMode
}
