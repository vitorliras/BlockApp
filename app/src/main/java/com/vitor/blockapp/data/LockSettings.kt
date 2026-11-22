package com.vitor.blockapp.data

enum class LockMethod { PHONE, PIN, PATTERN }

/** Configuração própria de um app. [method] nulo = "usar o padrão geral". */
data class AppLockConfig(
    val method: LockMethod? = null,
    val pin: String? = null,
    val pattern: String? = null,
)

/** Todas as configurações de desbloqueio. Os segredos aqui são sempre hashes. */
data class LockSettings(
    val defaultMethod: LockMethod = LockMethod.PHONE,
    val globalPin: String? = null,
    val globalPattern: String? = null,
    val perApp: Map<String, AppLockConfig> = emptyMap(),
    /** Quanto tempo um app desbloqueado continua liberado depois que você sai dele. */
    val graceSeconds: Int = 15,
) {
    /** Hash do segredo do método [method] para o app (próprio ou geral). */
    fun secretFor(packageName: String, method: LockMethod): String? {
        val cfg = perApp[packageName]
        val usesOwn = cfg?.method != null
        return when (method) {
            LockMethod.PHONE -> null
            LockMethod.PIN -> if (usesOwn) cfg?.pin else globalPin
            LockMethod.PATTERN -> if (usesOwn) cfg?.pattern else globalPattern
        }
    }

    /** Método que de fato vale para o app. Se a senha escolhida não existe, cai na do celular. */
    fun methodFor(packageName: String): LockMethod {
        val method = perApp[packageName]?.method ?: defaultMethod
        return if (method != LockMethod.PHONE && secretFor(packageName, method) == null) {
            LockMethod.PHONE
        } else {
            method
        }
    }
}
