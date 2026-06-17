package com.telenav.sdk.demo.config

import android.util.Log
import com.telenav.sdk.demo.security.AESUtil
import com.telenav.sdk.examples.BuildConfig

object SdkCredentials {

    private const val TAG = "SdkCredentials"

    /** App label and SDK identity: project key, userId, deviceGuid. */
    val appName: String get() = BuildConfig.APP_NAME

    const val APP_VERSION = "1.0"

    val apiKey: String by lazy { decryptOrPlain(BuildConfig.API_KEY) }

    val apiSecret: String by lazy { decryptOrPlain(BuildConfig.API_SECRET) }

    val cloudEndpoint: String by lazy {
        BuildConfig.CloudEndPoint.trimEnd('/')
    }

    val region: String by lazy { BuildConfig.Region }

    fun decryptOrPlain(value: String): String {
        if (!value.startsWith("enc:")) return value
        val payload = value.removePrefix("enc:")
        return try {
            AESUtil.decryptBase64KeyAndCiphertext(BuildConfig.API_KEY_AES_KEY, payload)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt credential: ${e.message}", e)
            throw IllegalStateException("Credential decrypt failed", e)
        }
    }
}
