package com.telenav.sdk.demo.config

import com.telenav.sdk.demo.security.AESUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class SdkCredentialsDecryptTest {

    @Test
    fun decrypt_nanjingFordCredentials() {
        val key = "TNGoogleUiKitAar"
        val apiKeyEnc =
            "WUaFh1mTlqVJvg4N+C8smF27e1RXVUdDzqJ/ij+8EVyqvLZvetMbfM/sOCvOMwE38Z1sV8NXT5CrPa+frMDBTA=="
        val apiSecretEnc =
            "S2RYTtUEkdGqalgS4quaG17xZvrvKDojpA+8g+zSfdGbd2jq9l5Ho2C1If6AuvoMcpADOHZulujH8YBgVDh6zQ=="

        assertEquals(
            "7d31644c-d878-421c-b632-cf1497047616",
            AESUtil.decryptBase64KeyAndCiphertext(key, apiKeyEnc)
        )
        assertEquals(
            "a1276daf-371e-4574-8c68-fd38e580153e",
            AESUtil.decryptBase64KeyAndCiphertext(key, apiSecretEnc)
        )
    }
}
