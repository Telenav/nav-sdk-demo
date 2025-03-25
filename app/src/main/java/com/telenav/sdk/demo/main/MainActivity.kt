/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.common.model.*
import com.telenav.sdk.core.ApplicationInfo
import com.telenav.sdk.core.Locale
import com.telenav.sdk.core.SDKOptions
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.demo.util.RegionCachedHelper
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.api.error.EntityException
import com.telenav.sdk.examples.BuildConfig.*
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.SDK
import com.telenav.sdk.map.model.NavSDKOptions
import com.telenav.sdk.ota.api.OtaService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.io.File

open class MainActivity : AppCompatActivity() {
    private val LOG_TAG = MainActivity::class.java.name
    private val permissionRequestCode = 12335

    private var initialized = false
    private val region = Region.EU
    var is4WD = false
    var isStreamingEnabled = true
    var isTrafficEnabled = true
    var isCloudRoutingEnabled = true

    companion object {
        const val API_KEY = SDK_KEY
        const val API_SECRET = SDK_SECRET
        const val API_URL = SDK_ENDPOINT
    }


    private fun checkUserPermission() {
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

    fun initNavSDKAsync(ready : ()->Unit) {
        // simulating initializing SDK on worker thread
        if (initialized){
            ready()
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            showProgress("initializing...")
            val success = initNavSDK()
            if (success) {
                initialized = true
                ready()
            } else{
                Toast.makeText(applicationContext, "initialized failed!!", Toast.LENGTH_SHORT).show()
            }
            hideProgress()
        }
    }

    fun disposeSDK(){
        CoroutineScope(Dispatchers.Main).launch {
            showProgress("Disposing...")
            withContext(Dispatchers.IO) {
                SDK.getInstance().dispose()
                EntityService.shutdown()
                DataCollectorService.shutdown()
                OtaService.shutdown()
                if (!TextUtils.isEmpty(getCachedDataDir())) {
                    File(getCachedDataDir()).deleteRecursively()
                }
                if (!TextUtils.isEmpty("${getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}")) {
                    File("${getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}").deleteRecursively()
                }
            }
            initialized = false
            hideProgress()
        }
    }

    private suspend fun initNavSDK() : Boolean {
        // enable tasdk log
        // TaLog.enableWriteLogsToFile(true)
        // TaLog.setLogPath("/sdcard/tasdk.log")
        TaLog.setLogLevel(NavLogLevelType.INFO)
        // TODO set your local embedded map data path
        val sdkDataDir = ""
        // TODO set your local writable path
        val sdkCacheDataDir = getCachedDataDir()
        // TODO set your local writable ota path
        val otaDataDir = "sdcard/test/"
        RegionCachedHelper.saveSDKDataModel(this,InitSDKDataModel(region, "", API_KEY, API_SECRET, API_URL, "stage"))
        val optionsBuilder = SDKOptions.builder()
            .setApiKey(API_KEY)
            .setApiSecret(API_SECRET)
            .setSdkCacheDataDir(sdkCacheDataDir)
            .setCloudEndPoint(API_URL)
            .setLocale(getLocale(region))    //  if not specified, SDK will assume region EU
            .setUserId("AndroidSampleTest")
        if (!TextUtils.isEmpty(sdkDataDir)) {
            if (File(sdkDataDir).exists()) {
                optionsBuilder.setSdkDataDir(sdkDataDir)
            }else{
                Snackbar.make(layoutContent, "sdkDataDir does not exist",Snackbar.LENGTH_LONG).show()
            }
        }

        val success = initSDK(optionsBuilder.build(), region)
        initEntityService(optionsBuilder.build())
        optionsBuilder
            .setDeviceGuid("AndroidDeviceGuid")
            .setApplicationInfo(ApplicationInfo.builder("demo","1").build())
            .setSdkDataDir(otaDataDir)
            .build()
        initDataCollectorService(optionsBuilder.build())
        initOtaService(optionsBuilder.build())
        return success
    }

    /**
     * This method is evoked by FirstFragment
     */
    fun getCachedDataDir(): String {
        return "$cacheDir/nav-cached/"
    }


    private suspend fun initSDK(options: SDKOptions, region: Region, vehicleProfile: VehicleProfile? = null) : Boolean {
        var success: Boolean = false
        withContext(Dispatchers.IO) {
            // Set the fixed port of broker server for example app.
            // To reduce port conflicts, please don't use port in range [20000, 20099].
            val peLogDir = File(getExternalFilesDir(null),"peLog");
            val navSDKOptions = NavSDKOptions.builder(options, vehicleProfile)
                //The refresh frequency of the incident is related to the trafficrefreshtime in the configuration,
                // so improve the traffic refresh rate and facilitate the avoidIncident demo demonstration
                .setTrafficRefreshTime(120)
                .setTrafficExpireTime(120)
                .setTrafficCacheExtendTime(0)
                .enableSelfPropellingUponWeakGPS(true)
                .enableTraffic(true)
                .enablePositionEngineLog(true)
                .setTrafficFetchRange(3600)
                .enableDownloadMapData(isStreamingEnabled)
                .enableCloudRouting(isCloudRoutingEnabled)
                .enableTraffic(isTrafficEnabled)
                .setPositionEngineLogStorePath(peLogDir.absolutePath)
                .setMapStreamingSpaceLimit(1024*1024*1024)
                .setRegion(region)
                .build()
            success = SDK.getInstance().initialize(this@MainActivity, navSDKOptions) == 0
            Log.e("MainActivity", filesDir.absolutePath)
        }
        return success
    }

    private suspend fun initEntityService(options: SDKOptions) {
        withContext(Dispatchers.IO) {
            try {
                EntityService.initialize(options)
            } catch (e: IllegalArgumentException) {
                print("SDK entity service init error, check your API key/secret, cloud endpoint and lib dependencies")
            } catch (e: EntityException) {
                print("SDK entity service init error, embedded data path: " + e.localizedMessage)
            }
        }
    }

    private suspend fun initOtaService(options: SDKOptions) {
        withContext(Dispatchers.IO) {
            try {
                OtaService.initialize(this@MainActivity, options)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun initDataCollectorService(options: SDKOptions) {
        withContext(Dispatchers.IO) {
            try {
                DataCollectorService.initialize(this@MainActivity, options)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getLocale(region: Region): Locale = when (region) {
        Region.CN -> Locale.SIMPLIFIED_CHINESE
        Region.EU -> Locale.NORWEGIAN
        Region.TW -> Locale.TRADITIONAL_CHINESE
        Region.KR -> Locale.KOREAN
        Region.NA -> Locale.EN_US
        Region.SEA -> Locale.INDONESIAN
        Region.MEA -> Locale.ARABIC
        Region.ANZ -> Locale.ENGLISH
        else -> Locale.GERMAN
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkUserPermission()

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = APPLICATION_ID
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        Log.i(LOG_TAG, "Begin onDestroy ...")
        SDK.getInstance().dispose()
        EntityService.shutdown()
        DataCollectorService.shutdown()
        OtaService.shutdown()
        Log.i(LOG_TAG, "Telenav SDK disposed")
        super.onDestroy()
    }

}