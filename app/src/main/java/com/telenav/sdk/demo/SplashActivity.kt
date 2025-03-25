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
import com.telenav.sdk.common.model.Region
import com.telenav.sdk.core.ApplicationInfo
import com.telenav.sdk.core.Locale
import com.telenav.sdk.core.SDKOptions
import com.telenav.sdk.examples.BuildConfig
import com.telenav.sdk.examples.R
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
    val SDK_KEY = BuildConfig.API_KEY
    val SDK_SECRET = BuildConfig.API_SECRET

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

        val sdkCacheDataDir = "$cacheDir/nav-cached/"
        val sdkOptions = SDKOptions.builder()
            .setApiKey(SDK_KEY)
            .setApiSecret(SDK_SECRET)
            .setSdkCacheDataDir(sdkCacheDataDir)
            .setCloudEndPoint("https://apieustg.telenav.com")
            .setLocale(Locale.EN_US)    //  if not specified, SDK will assume region EU
            .setUserId("AndroidDemoTest")
            .setDeviceGuid("AndroidDeviceGuid")
            .setApplicationInfo(ApplicationInfo.builder("demo", "1").build())
            .build()
        return initSDK(sdkOptions)
    }

    private suspend fun initSDK(options: SDKOptions): Boolean {
        val success: Boolean
        withContext(Dispatchers.IO) {
            val navSDKOptions = NavSDKOptions.builder(options)
                .setTrafficRefreshTime(20)
                .setTrafficExpireTime(20)
                .enableTraffic(true)
                .setTrafficFetchRange(3600)
                .setMapStreamingSpaceLimit(1024 * 1024 * 1024)
                .setRegion(Region.EU)
                .build()
            success = SDK.getInstance().initialize(this@SplashActivity, navSDKOptions) == 0
            if (success) {
                //  by default: using DAY color theme:
                SDK.getInstance().updateDayNightMode(DayNightMode.DAY)
            }
        }

        return success
    }
}