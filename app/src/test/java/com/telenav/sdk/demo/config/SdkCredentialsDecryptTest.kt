package com.telenav.sdk.demo.config

import org.junit.Assert.assertEquals
import org.junit.Test

class SdkCredentialsDecryptTest {

    @Test
    fun decryptOrPlain_returnsPlaintextAsIs() {
        assertEquals("plain-api-key", SdkCredentials.decryptOrPlain("plain-api-key"))
        assertEquals(
            "12ea25ed-a6c9-40a4-943a-884fc0aa4c7f",
            SdkCredentials.decryptOrPlain("12ea25ed-a6c9-40a4-943a-884fc0aa4c7f")
        )
    }
}
