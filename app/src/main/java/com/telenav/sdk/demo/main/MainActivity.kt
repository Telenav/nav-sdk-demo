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
//import com.telenav.sdk.common.broker.Constants
import com.telenav.sdk.core.ApplicationInfo
import com.telenav.sdk.core.Locale
import com.telenav.sdk.core.SDKOptions
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.demo.util.RegionCachedHelper
import com.telenav.sdk.demo.util.VehicleProfileHelper
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.api.error.EntityException
import com.telenav.sdk.examples.BuildConfig
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

    // TODO change options here
    private val regionInitList = listOf(
        InitSDKDataModel(Region.NA, "", SDK_KEY_NA, SDK_SECRET_NA, URL_NA),
        InitSDKDataModel(Region.EU, "", SDK_KEY_EU, SDK_SECRET_EU, URL_EU),
        InitSDKDataModel(Region.NA, "$SDK_DATA_DIR_BASE/NA", SDK_KEY_NA, SDK_SECRET_NA, URL_NA),
        InitSDKDataModel(Region.EU, "$SDK_DATA_DIR_BASE/EU", SDK_KEY_EU, SDK_SECRET_EU, URL_EU),
        InitSDKDataModel(Region.EU, "$SDK_DATA_DIR_BASE/EU", "", "", ""),
        InitSDKDataModel(Region.SEA, "$SDK_DATA_DIR_BASE/SEA", ON_BOARD_KEY, ON_BOARD_SECRET, ""),
        InitSDKDataModel(Region.ANZ, "$SDK_DATA_DIR_BASE/ANZ", ON_BOARD_KEY, ON_BOARD_SECRET, ""),
        InitSDKDataModel(Region.CN, "", SDK_KEY_NA, SDK_SECRET_NA, URL_CN_DEMO,false,"DEMO"),
        InitSDKDataModel(Region.NA, "", SDK_KEY_OSM, SDK_SECRET_OSM, URL_NA_OSM, false,"OSM"),
        InitSDKDataModel(Region.EU, "", SDK_KEY_OSM, SDK_SECRET_OSM, URL_EU_OSM, false,"OSM"),
        InitSDKDataModel(Region.EU, "$SDK_DATA_DIR_BASE/EU",
            SDK_KEY_EU, SDK_SECRET_EU, URL_EU, true, "4WD"),
        InitSDKDataModel(Region.EU, "", SDK_KEY_EU, SDK_SECRET_EU, URL_EU,true, "4WD"),
    )

    companion object {
        // The KEY and SECRET for test in NA region.
        const val SDK_KEY_NA = "e48ee2f9-5c2c-41e9-b0d7-167d8ad47870"
        const val SDK_SECRET_NA = "ce7e333a-e168-4fbb-bb5a-ac5a0fb28eac"
        // The KEY and SECRET are used to test NIO project in EU region.
        const val SDK_KEY_EU = "f98eadad-cca7-4a55-b2b5-d6dc930e8bc1"
        const val SDK_SECRET_EU = "1daf6254-1f88-4b8d-8539-308a60e2d181"
        // The KEY and SECRET are used to test OSM project
        const val SDK_KEY_OSM = "fe53800f-2fd1-41a0-81ef-610fb3177e4e"
        const val SDK_SECRET_OSM = "771da85e-a1ef-482b-9abf-c60090d07413"

        // The value for embedded setting, can be any value but empty string.
        const val ON_BOARD_KEY = "0"
        const val ON_BOARD_SECRET = "0"

        // The URL for NA region test
        const val URL_NA = "https://apinastg.telenav.com"
        // The URL is used to test NIO project in EU region
        const val URL_EU = "https://nioeustg.telenav.com"

        // public access server, could also be visited from intranet
        const val URL_CN_DEMO = "http://shs-navdemo-cn-routing-01.telenav.cn:10080"

        // The URL for OSM NA region test
        const val URL_NA_OSM = "http://apinastg.telenav.com"
        // The URL for OSM EU region test
        const val URL_EU_OSM = "https://apieustg.telenav.com"

        private const val SDK_DATA_DIR_BASE = "sdcard/map"

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
        val model = RegionCachedHelper.getSDKDataModel(applicationContext) ?: regionInitList[0]
        val region = model.region
        // TODO set your local embedded map data path
        val sdkDataDir = model.mapDataPath
        val url = model.url
        val key = model.key
        val secret = model.secret
        // TODO set your local writable path
        val sdkCacheDataDir = getCachedDataDir()
        // TODO set your local writable ota path
        val otaDataDir = "sdcard/test/"
        val is4WD = model.is4WD
        val vehicleProfile : VehicleProfile? = if (is4WD) {
            VehicleProfileHelper.createVehicleProfile(Category.k4WD)
        } else {
            null
        }

        val optionsBuilder = SDKOptions.builder()
            .setApiKey(key)
            .setApiSecret(secret)
            .setSdkCacheDataDir(sdkCacheDataDir)
            .setCloudEndPoint(url)
            .setLocale(getLocale(region))    //  if not specified, SDK will assume region EU
            .setUserId("AndroidSampleTest")
        if (!TextUtils.isEmpty(sdkDataDir)) {
            if (File(sdkDataDir).exists()) {
                optionsBuilder.setSdkDataDir(sdkDataDir)
            }else{
                Snackbar.make(layoutContent, "sdkDataDir does not exist",Snackbar.LENGTH_LONG).show()
            }
        }
        Log.i("MainActivity",filesDir.absolutePath)
        val success = initSDK(optionsBuilder.build(), region, vehicleProfile)
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

    /**
     * This method is evoked by FirstFragment
     */
    fun getRegionInitList() = regionInitList

    private suspend fun initSDK(options: SDKOptions, region: Region, vehicleProfile: VehicleProfile? = null) : Boolean {
        val success: Boolean
        withContext(Dispatchers.IO) {
            // Set the fixed port of broker server for example app.
            // To reduce port conflicts, please don't use port in range [20000, 20099].
//            Os.setenv(Constants.KEY_TASDK_BROKER_SERVER_PORT, "20210", true);

            val peLogDir = File(getExternalFilesDir(null),"peLog");
            peLogDir.mkdir()
            val navSDKOptions = NavSDKOptions.builder(options, vehicleProfile)
                //The refresh frequency of the incident is related to the trafficrefreshtime in the configuration,
                // so improve the traffic refresh rate and facilitate the avoidIncident demo demonstration
                .setTrafficRefreshTime(20)
                .setTrafficExpireTime(20)
                .enableTraffic(true)
                .enablePositionEngineLog(true)
                .setTrafficFetchRange(3600)
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

    private fun getUrl(region: Region, tag: String?): String {
        return if (region == Region.CN) {
            if (tag != null && tag.isNotEmpty() && tag == "DEMO") {
                URL_CN_DEMO
            } else {
                ""
            }
        } else if (region == Region.EU) {
            URL_EU
        } else {
            URL_NA
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

    private fun getKey(region: Region): String = if (region == Region.EU) SDK_KEY_EU else SDK_KEY_NA

    private fun getSecret(region: Region): String = if (region == Region.EU) SDK_SECRET_EU else SDK_SECRET_NA

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
        supportActionBar?.title = BuildConfig.APPLICATION_ID
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

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        SDK.getInstance().trimMemory(level)
    }
}