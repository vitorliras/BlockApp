package com.vitor.blockapp.data

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Transforma PIN/padrão em um "hash" (impressão digital irreversível) com sal aleatório.
 * A senha em si nunca é guardada no celular, só o hash.
 */
object SecretHasher {
    private const val ITERATIONS = 20_000

    fun hash(secret: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return "${encode(salt)}:${encode(derive(secret, salt))}"
    }

    fun verify(secret: String, stored: String): Boolean {
        val parts = stored.split(':')
        if (parts.size != 2) return false
        val salt = Base64.decode(parts[0], Base64.NO_WRAP)
        val expected = Base64.decode(parts[1], Base64.NO_WRAP)
        return MessageDigest.isEqual(derive(secret, salt), expected)
    }

    private fun derive(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
}
