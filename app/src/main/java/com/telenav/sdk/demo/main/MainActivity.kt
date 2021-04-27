/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.core.ApplicationInfo
import com.telenav.sdk.core.Locale
import com.telenav.sdk.core.SDKOptions
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.demo.BuildConfig
import com.telenav.sdk.demo.R
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.api.error.EntityException
import com.telenav.sdk.map.SDK
import com.telenav.sdk.ota.api.OtaService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private val LOG_TAG = MainActivity::class.java.name
    private val permissionRequestCode = 12335
    private val permissionsRequired = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.INTERNET
    )
    private var initialized = false

    companion object {
        const val SDK_KEY = BuildConfig.SDK_KEY
        const val SDK_SECRET = BuildConfig.SDK_SECRET

        const val URL_NA = "https://apinastg.telenav.com"

    }

    private fun checkUserPermission() {
        permissionsRequired.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_DENIED
            ) {
                ActivityCompat.requestPermissions(this, permissionsRequired, permissionRequestCode)
            }
        }
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
            }
        }
    }

    fun initNavSDKAsync(ready: () -> Unit) {
        // simulating initializing SDK on worker thread
        if (initialized) {
            ready()
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            showProgress("initializing...")
            val success = initNavSDK()
            if (success) {
                initialized = true
                ready()
            } else {
                Toast.makeText(applicationContext, "initialized failed!!", Toast.LENGTH_SHORT)
                    .show()
            }
            hideProgress()
        }
    }

    private fun showProgress(text: String) {
        tvProgress.text = text
        layoutProgress.visibility = View.VISIBLE
        layoutContent.visibility = View.GONE
    }

    private fun hideProgress() {
        layoutProgress.visibility = View.GONE
        layoutContent.visibility = View.VISIBLE
    }

    private suspend fun initNavSDK(): Boolean {
        // enable tasdk log
//        TaLog.enableWriteLogsToFile(true)
//        TaLog.setLogPath("/sdcard/tasdk.log")
        // TODO set your local writable path
        val sdkCacheDataDir = getCachedDataDir()
        // TODO set your local writable ota path
        val otaDataDir = "sdcard/test/"

        val optionsBuilder = SDKOptions.builder()
            .setApiKey(SDK_KEY)
            .setApiSecret(SDK_SECRET)
            .setUserId("AndroidSampleTest")
            .setSdkCacheDataDir(sdkCacheDataDir)
            .setCloudEndPoint(URL_NA)
            .setLocale(Locale.EN_US)
        val success = initSDK(optionsBuilder.build())
        initEntityService(optionsBuilder.build())
        optionsBuilder
            .setDeviceGuid("AndroidDeviceGuid")
            .setApplicationInfo(ApplicationInfo.builder(packageName, "1").build())
            .setSdkDataDir(otaDataDir)
            .build()
        initDataCollectorService(optionsBuilder.build())
        initOtaService(optionsBuilder.build())
        return success
    }

    fun getCachedDataDir(): String {
        return "$cacheDir/nav-cached/"
    }


    private suspend fun initSDK(options: SDKOptions): Boolean {
        val success: Boolean
        withContext(Dispatchers.IO) {
            success = SDK.getInstance().initialize(this@MainActivity, options) == 0
        }
        return success
    }

    private suspend fun initEntityService(options: SDKOptions) {
        withContext(Dispatchers.IO) {
            try {
                EntityService.initialize(options)
            } catch (e: EntityException) {
                print("SDK entity service init error, check your API key/secret, cloud endpoint and lib dependencies")
            } catch (e: IllegalArgumentException) {
                print("SDK entity service init error, embedded data path: " + e.localizedMessage)
            }
        }
    }

    private suspend fun initOtaService(options: SDKOptions) {
        withContext(Dispatchers.Main) {
            try {
                OtaService.initialize(this@MainActivity, options)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun initDataCollectorService(options: SDKOptions) {
        withContext(Dispatchers.IO) {
            try {
                DataCollectorService.initialize(this@MainActivity, options)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkUserPermission()

        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        Log.i(LOG_TAG, "Begin onDestroy ...")
        SDK.getInstance().dispose()
        DataCollectorService.shutdown()
        OtaService.shutdown()
        Log.i(LOG_TAG, "Telenav SDK disposed")
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        SDK.getInstance().trimMemory(level)
    }
}