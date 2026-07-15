package com.telenav.sdk.demo.search

import android.content.Context
import com.telenav.sdk.core.ApplicationInfo
import com.telenav.sdk.core.SDKOptions
import com.telenav.sdk.demo.config.SdkCredentials
import com.telenav.searchservice.api.SearchServiceInitOptions
import com.telenav.searchservice.api.SearchSettings
import java.io.File

object SearchServiceConfig {

    fun buildInitOptions(
        context: Context,
        latitude: Double,
        longitude: Double
    ): SearchServiceInitOptions {
        val appContext = context.applicationContext
        val sdkDataDir = File(appContext.filesDir, "tn_sdk_data").apply { mkdirs() }
        val sdkCacheDir = File(appContext.cacheDir, "tn_sdk_cache").apply { mkdirs() }

        val sdkOptions = SDKOptions.builder()
            .setApiKey(SdkCredentials.apiKey)
            .setApiSecret(SdkCredentials.apiSecret)
            .setCloudEndPoint(SdkCredentials.cloudEndpoint)
            .setRegion(SdkCredentials.region)
            .setCurrentLocation(latitude, longitude)
            .setUserId(SdkCredentials.appName)
            .setDeviceGuid(SdkCredentials.appName)
            .setApplicationInfo(
                ApplicationInfo.builder(SdkCredentials.appName, SdkCredentials.APP_VERSION).build()
            )
            .setSdkDataDir(sdkDataDir.absolutePath)
            .setSdkCacheDataDir(sdkCacheDir.absolutePath)
            .build()

        val searchSettings = SearchSettings(
            timeout = 5_000,
            googleSearchEnabled = true,
            userIsExpired = false,
            rgcIsOnboard = false,
            onlyOnBoardSearch = false
        )

        return SearchServiceInitOptions(
            sdkOptions = sdkOptions,
            searchSettings = searchSettings
        )
    }

    fun defaultLocationForRegion(region: String): Pair<Double, Double> =
        SearchRegionDefaults.defaultLocation(region)
}
