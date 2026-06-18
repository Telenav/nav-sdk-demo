package com.telenav.sdk.demo.search

import android.content.Context
import com.telenav.sdk.core.SDKOptions
import com.telenav.searchservice.api.SearchServiceInitOptions
import com.telenav.searchservice.api.SearchSettings
import java.io.File

object SearchServiceConfig {

    /**
     * Map SDK and SearchService use different data roots: Map streams without onboard
     * data under [sdkDataDir]; SearchService needs its own [sdkDataDir] for Entity layer.
     */
    fun buildInitOptions(context: Context, sdkOptions: SDKOptions): SearchServiceInitOptions {
        val sdkDataDir = File(context.applicationContext.filesDir, "tn_sdk_data").apply { mkdirs() }
        val searchSdkOptions = sdkOptionsForSearch(sdkOptions, sdkDataDir.absolutePath)

        val searchSettings = SearchSettings(
            timeout = 5_000,
            googleSearchEnabled = true,
            userIsExpired = false,
            rgcIsOnboard = false,
            onlyOnBoardSearch = false
        )

        return SearchServiceInitOptions(
            sdkOptions = searchSdkOptions,
            searchSettings = searchSettings
        )
    }

    private fun sdkOptionsForSearch(base: SDKOptions, sdkDataDir: String): SDKOptions {
        val builder = SDKOptions.builder()
            .setApiKey(base.apiKey)
            .setApiSecret(base.apiSecret)
            .setCloudEndPoint(base.cloudEndPoint)
            .setRegion(base.region)
            .setUserId(base.userId)
            .setDeviceGuid(base.deviceGuid)
            .setSdkDataDir(sdkDataDir)
            .setSdkCacheDataDir(base.sdkCacheDataDir)

        base.locale?.let { builder.setLocale(it) }
        base.applicationInfo?.let { builder.setApplicationInfo(it) }
        base.customContext?.let { builder.setCustomContext(it) }

        val location = base.currentLocation
        if (location != null) {
            builder.setCurrentLocation(location.latitude, location.longitude)
        }

        return builder.build()
    }

    fun defaultLocationForRegion(region: String): Pair<Double, Double> =
        SearchRegionDefaults.defaultLocation(region)
}
