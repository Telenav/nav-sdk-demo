package com.telenav.sdk.demo.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** AES-GCM — same scheme as search-service-demo. */
object AESUtil {
    private const val AES = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12
    private val VALID_KEY_SIZES = setOf(16, 24, 32)

    private fun resolveKeyBytes(base64OrRawKey: String): ByteArray {
        if (VALID_KEY_SIZES.contains(base64OrRawKey.length)) {
            return base64OrRawKey.toByteArray(Charsets.UTF_8)
        }
        try {
            val decoded = Base64.getDecoder().decode(base64OrRawKey)
            if (VALID_KEY_SIZES.contains(decoded.size)) return decoded
        } catch (_: IllegalArgumentException) {
        }
        return base64OrRawKey.toByteArray(Charsets.UTF_8)
    }

    fun decryptBase64KeyAndCiphertext(base64OrRawKey: String?, enc: String): String {
        if (base64OrRawKey.isNullOrEmpty()) return enc
        val keyBytes = resolveKeyBytes(base64OrRawKey)
        val allBytes = Base64.getDecoder().decode(enc)
        require(allBytes.size > IV_BYTES) { "ciphertext too short" }
        val iv = allBytes.copyOfRange(0, IV_BYTES)
        val cipherBytes = allBytes.copyOfRange(IV_BYTES, allBytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, AES), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    }
}
