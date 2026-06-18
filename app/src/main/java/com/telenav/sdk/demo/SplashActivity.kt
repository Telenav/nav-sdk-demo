/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.common.model.DayNightMode
import com.telenav.sdk.common.model.NavLogLevelType
import com.telenav.sdk.core.ApplicationInfo
import com.telenav.sdk.core.Locale
import com.telenav.sdk.core.SDKOptions
import com.telenav.sdk.demo.config.SdkCredentials
import com.telenav.sdk.demo.search.SearchServiceHolder
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.api.error.EntityException
import com.telenav.sdk.examples.BuildConfig
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.MapContentManager
import com.telenav.sdk.map.SDK
import com.telenav.sdk.map.model.NavSDKOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author tang.hui on 2021/9/24
 */
class SplashActivity : AppCompatActivity() {

    private val permissionRequestCode = 12335

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (checkUserPermission()) {
            initNavSDKAsync {
                MainActivity.start(this)
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    private fun checkUserPermission(): Boolean {
        val permissionsRequired = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET
        )
        permissionsRequired.forEach { permission ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_DENIED
            ) {
                ActivityCompat.requestPermissions(this, permissionsRequired, permissionRequestCode)
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED)) {
                Toast.makeText(this, "We need all Permission to proceed", Toast.LENGTH_SHORT)
                    .show()
                this.finishAffinity()
            } else {
                initNavSDKAsync {
                    MainActivity.start(this)
                    finish()
                }
            }
        }
    }

    private fun initNavSDKAsync(ready: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val success = initNavSDK()
            if (success) {
                ready()
            } else {
                Toast.makeText(applicationContext, "initialized failed!!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private suspend fun initNavSDK(): Boolean {
        TaLog.enableLogs(true)// enable SDK log
        TaLog.setLogLevel(NavLogLevelType.INFO) //  set INFO log level by default

        //  TODO("logging Nav SDK log to local file")
        //  TaLog.enableWriteLogsToFile(true)
        //  TaLog.setLogPath("/sdcard/Download/telenav_sdk_demo.log")

        val locationProvider = SimulationLocationProvider(BuildConfig.Region)
        val loc = locationProvider.getLastKnownLocation()
        val sdkCacheDataDir = "$cacheDir/nav-cached/"
        val sdkOptions = SDKOptions.builder()
            .setApiKey(SdkCredentials.apiKey)
            .setApiSecret(SdkCredentials.apiSecret)
            .setSdkCacheDataDir(sdkCacheDataDir)
            .setCloudEndPoint(SdkCredentials.cloudEndpoint)
            .setLocale(Locale.EN_US)
            .setCurrentLocation(loc.latitude, loc.longitude)
            .setUserId(SdkCredentials.appName)
            .setDeviceGuid(SdkCredentials.appName)
            .setApplicationInfo(
                ApplicationInfo.builder(SdkCredentials.appName, SdkCredentials.APP_VERSION).build()
            )
            .setRegion(SdkCredentials.region)
            .build()
        return initSDK(sdkOptions)
    }

    private suspend fun initSDK(options: SDKOptions): Boolean {
        val success: Boolean
        withContext(Dispatchers.IO) {
            val navSDKOptions = NavSDKOptions.builder(options)
                .setTrafficRefreshTime(20)
                .setTrafficExpireTime(20)
//                .setTrafficFetchRange(3600)
                .setMapStreamingSpaceLimit(1024 * 1024 * 1024)
                .build()
            success = SDK.getInstance().initialize(this@SplashActivity, navSDKOptions) == 0
            if (success) {
                //  by default: using DAY color theme:
                SDK.getInstance().updateDayNightMode(DayNightMode.DAY)
                MapContentManager.getInstance().enableTraffic(true)
            }
        }

        if (success) {
            // SearchService init can block on network/WebView; do not hold Splash.
            CoroutineScope(Dispatchers.Main).launch {
                initSearchService(options)
            }
        }

        return success
    }

    private suspend fun initSearchService(options: SDKOptions) {
        withContext(Dispatchers.IO) {
            val searchOk = SearchServiceHolder.initialize(this@SplashActivity, options)
            if (!searchOk) {
                TaLog.w("SplashActivity", "SearchService init failed; search demo may not work")
            }
        }
    }

    /**
     * Standalone EntityService init example — do not call together with [initSearchService]:
     * SearchService initializes EntityService internally via [EntitySearchBackend].
     */
    @Suppress("unused")
    private suspend fun initEntityService(options: SDKOptions) {
        withContext(Dispatchers.IO) {
            try {
                EntityService.initialize(options)
            } catch (e: IllegalArgumentException) {
                TaLog.e(
                    "TAG",
                    "SDK entity service init error, check your API key/secret, cloud endpoint and lib dependencies",
                    e
                )
            } catch (e: EntityException) {
                TaLog.e(
                    "TAG",
                    "SDK entity service init error, embedded data path: " + e.localizedMessage,
                    e
                )
            }
        }
    }

}