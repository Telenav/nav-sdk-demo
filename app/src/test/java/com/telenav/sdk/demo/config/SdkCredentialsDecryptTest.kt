package com.telenav.sdk.demo.config

import org.junit.Assert.assertEquals
import org.junit.Test

class SdkCredentialsDecryptTest {

    @Test
    fun decryptOrPlain_returnsPlaintextAsIs() {
        assertEquals("plain-api-key", SdkCredentials.decryptOrPlain("plain-api-key"))
    }
}
