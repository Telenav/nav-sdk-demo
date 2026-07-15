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
import com.telenav.sdk.demo.utils.StoragePermissionHelper
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

    private val storagePermissionRequestCode = 12335
    private val locationPermissionRequestCode = 12336

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        ensurePermissionsAndStart()
    }

    override fun onResume() {
        super.onResume()
        if (!sdkInitStarted) {
            ensurePermissionsAndStart()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    private var sdkInitStarted = false

    private fun ensurePermissionsAndStart() {
        if (sdkInitStarted) {
            return
        }
        if (!StoragePermissionHelper.hasStoragePermission(this)) {
            StoragePermissionHelper.requestStoragePermission(this, storagePermissionRequestCode)
            return
        }
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionRequestCode
            )
            return
        }
        sdkInitStarted = true
        initNavSDKAsync {
            MainActivity.start(this)
            finish()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            storagePermissionRequestCode -> {
                if (StoragePermissionHelper.hasStoragePermission(this)) {
                    ensurePermissionsAndStart()
                } else {
                    Toast.makeText(this, R.string.storage_permission_required, Toast.LENGTH_SHORT)
                        .show()
                    finishAffinity()
                }
            }
            locationPermissionRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ensurePermissionsAndStart()
                } else {
                    Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_SHORT)
                        .show()
                    finishAffinity()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == storagePermissionRequestCode) {
            if (StoragePermissionHelper.hasStoragePermission(this)) {
                ensurePermissionsAndStart()
            } else {
                Toast.makeText(this, R.string.storage_permission_required, Toast.LENGTH_SHORT)
                    .show()
                finishAffinity()
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

        val sdkCacheDataDir = "$cacheDir/nav-cached/"
        val sdkOptions = SDKOptions.builder()
            .setApiKey(SdkCredentials.apiKey)
            .setApiSecret(SdkCredentials.apiSecret)
            .setSdkCacheDataDir(sdkCacheDataDir)
            .setCloudEndPoint(SdkCredentials.cloudEndpoint)
            .setLocale(Locale.EN_US)
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
            initEntityService(options)
            initSearchService()
        }

        return success
    }

    private suspend fun initSearchService() {
        withContext(Dispatchers.IO) {
            val locationProvider = SimulationLocationProvider(BuildConfig.Region)
            val loc = locationProvider.getLastKnownLocation()
            val searchOk = SearchServiceHolder.initialize(
                this@SplashActivity,
                loc.latitude,
                loc.longitude
            )
            if (!searchOk) {
                TaLog.w("SplashActivity", "SearchService init failed; search demo may not work")
            }
        }
    }

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