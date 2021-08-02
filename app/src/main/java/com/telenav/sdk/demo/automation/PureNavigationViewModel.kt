/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.automation

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.common.model.Region
import com.telenav.sdk.demo.provider.DemoLocationProvider
import com.telenav.sdk.demo.util.RegionCachedHelper
import com.telenav.sdk.demo.util.SingleLiveEvent
import com.telenav.sdk.drivesession.DriveSession
import com.telenav.sdk.drivesession.NavigationSession
import com.telenav.sdk.drivesession.listener.*
import com.telenav.sdk.drivesession.model.*
import com.telenav.sdk.drivesession.model.adas.AdasMessage
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author zhai.xiang on 2021/7/17
 */
class PureNavigationViewModel(application: Application) : AndroidViewModel(application), NavigationEventListener,
        PositionEventListener, AlertEventListener, ADASEventListener,
        AudioInstructionEventListener{
    val navigationEventLiveData = MutableLiveData<NavigationEvent>()
    val currentStreetName = MutableLiveData<String>()
    val startLocationText = MutableLiveData<String>()
    val alertNumber = MutableLiveData<Int>(0)
    val adasNumber = MutableLiveData<Int>(0)
    val drgNumber = MutableLiveData<Int>(0)
    val stopLocationText = MutableLiveData<String>()
    val speedLimitLiveData = MutableLiveData<String>()
    val vehicleLocation = MutableLiveData<Location>()
    val junctionBitmap = MutableLiveData<Bitmap?>()
    val navigationOn = MutableLiveData<Boolean>(false)
    var toast = SingleLiveEvent<String>()
    val startLocation : Location
    val stopLocation : Location
    val betterRouteLiveData = MutableLiveData<Route>()
    val alongRouteTrafficLiveData = MutableLiveData<AlongRouteTraffic>()
    private var navigationSession: NavigationSession? = null
    private var locationsFromFile = ArrayList<Location>()
    private var locationProvider : DemoLocationProvider
    private var driveSession: DriveSession? = DriveSession.Factory.createInstance()
    private var route: Route? = null
    private var runJob : Deferred<Any?>? = null
    private var readJob : Deferred<Any?>? = null
    private var locationIndex = 0

    init {
        driveSession?.enableAlert(true)
        driveSession?.enableADAS(true)
        locationProvider = DemoLocationProvider.Factory.createProvider(application, DemoLocationProvider.ProviderType.SIMULATION)
        driveSession?.injectLocationProvider(locationProvider)
        driveSession?.eventHub?.let {
            it.addNavigationEventListener(this)
            it.addPositionEventListener(this)
            it.addAlertEventListener(this)
            it.addADASEventListener(this)
            it.addAudioInstructionEventListener(this)
        }

        locationProvider.start()
        startReadingFile()
        startLocation = locationProvider.lastKnownLocation
        stopLocation = Location("").apply {
            this.longitude = startLocation.longitude + 0.1
            this.latitude = startLocation.latitude + 0.1
        }

        startLocationText.postValue(String.format("[%.6f,%.6f]", startLocation.latitude, startLocation.longitude))
        stopLocationText.postValue(String.format("[%.6f,%.6f]", stopLocation.latitude, stopLocation.longitude))
    }


    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        navigationEventLiveData.postValue(navEvent)
    }

    override fun onJunctionViewUpdated(junctionViewInfo: JunctionViewInfo) {
        if (junctionViewInfo.isAvailable()) {
            val junctionImageData = junctionViewInfo.getImageData()
            if (junctionImageData != null) {
                val junctionImage =
                        BitmapFactory.decodeByteArray(junctionImageData, 0, junctionImageData.size)
                junctionBitmap.postValue(junctionImage)
            }
        } else {
            junctionBitmap.postValue(null)
        }
    }

    override fun onAlongRouteTrafficUpdated(alongRouteTraffic: AlongRouteTraffic) {
        val logString = String.format("along route traffic updated. route distance: %d; distance collected: %d; flows: %d",
                alongRouteTraffic.totalRouteDistance,
                alongRouteTraffic.alongRouteTrafficCollectDistance,
                alongRouteTraffic.alongRouteTrafficFlow?.size)
        Log.i("Navigation", logString)
        alongRouteTrafficLiveData.postValue(alongRouteTraffic)
    }

    override fun onNavigationStopReached(stopIndex: Int, stopLocation: Int) {
        if (stopIndex == -1) {// -1 means reach destination
            stopNavigation()
        }
    }

    override fun onNavigationRouteUpdated(route: Route, reason: NavigationEventListener.RouteUpdateReason?) {
        Log.i("navigation", "current route updated. unique id: " + route.getID())
        betterRouteLiveData.postValue(route)
        navigationSession?.updateRoute(route)
        drgNumber.value?.let {
            drgNumber.postValue(it + 1)
        }
    }

    override fun onBetterRouteDetected(betterRouteCandidate: BetterRouteCandidate) {
        betterRouteCandidate.accept(true)
    }

    override fun onLocationUpdated(vehicleLocationIn: Location) {
        vehicleLocationIn.let {
            vehicleLocation.postValue(it)
        }
    }

    override fun onStreetUpdated(curStreetInfo: StreetInfo, drivingOffRoad: Boolean) {
        curStreetInfo.streetName?.let {
            currentStreetName.postValue(it)
            Log.i("navigation", "current street: $it")
        }
        curStreetInfo.speedLimit?.let {

            var speedLimit = "invalid"
            var speed = 33.0;
            if (it.speedLimit <= -1 ){
                speed = 1.38; //5km/h
            }else if(it.speedLimit > -1 && it.speedLimit < 0x7FFFFFFF) {
                if (it.speedLimitUnit == SpeedLimitInfo.SpeedLimitUnit.KILOMETERS_PER_HOUR) {
                    speed =  it.speedLimit / 3.6 - 1.0
                    speedLimit = it.speedLimit.toString() + "km/h"

                } else {
                    speed =  1.609344 * it.speedLimit / 3.6
                    speedLimit = it.speedLimit.toString() + "mph"
                }
            }else if (it.speedLimit >= 0x7FFFFFFF){
                speed = 41.67; // 150 km/h
                speedLimit = "unlimited"

            }
            speedLimitLiveData.postValue(speedLimit)
            navigationSession?.setDemonstrateSpeed(speed.toDouble())
        }
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
    }

    override fun onMMFeedbackUpdated(feedback: MMFeedbackInfo) {
    }

    override fun onAlertEventUpdated(alertEvent: AlertEvent) {
        alertNumber.value?.let {
            alertNumber.postValue(it + 1)
        }
    }

    override fun onADASEventUpdated(adasMessageList: MutableList<AdasMessage>) {
        adasNumber.value?.let {
            adasNumber.postValue(it + 1)
        }
    }

    override fun onAudioInstructionUpdated(audioInstruction: AudioInstruction) {
    }

    fun startNavigation(demonstrateMode : Boolean, speed : Double = 40.0): Boolean {
        Log.d("MapLogsForTestData", "MapLogsForTestData >>>> startNavigation $route")
        drgNumber.postValue(0)
        stopNavigation()
        route?.let {
            navigationOn.postValue(true)
            navigationSession = driveSession?.startNavigation(it, demonstrateMode, speed)
        }

        return true
    }

    fun stopNavigation() {
        navigationSession?.stopNavigation()
        navigationOn.postValue(false)
        startRunningTraceData()
    }

    fun requestDirection(requestMode: RequestMode,
                         avoidOptions : RoutePreferences = RoutePreferences.Builder().build(),
                         routeStyle: Int = RouteStyle.FASTEST,
                         contentLevel: Int = ContentLevel.FULL, complete: (Int) -> Unit) {
        val request: RouteRequest = RouteRequest.Builder(
                GeoLocation(startLocation),
                GeoLocation(LatLon(stopLocation.latitude, stopLocation.longitude))
        ).contentLevel(ContentLevel.FULL)
                .routeCount(1)
                .avoidOption(avoidOptions)
                .contentLevel(contentLevel)
                .routeStyle(routeStyle)
                .startTime(Calendar.getInstance().timeInMillis/1000)
                .build()

        val task = DirectionClient.Factory.hybridClient().createRoutingTask(request, requestMode)
        task.runAsync { response ->
            Log.d("MapLogsForTestData" , "MapLogsForTestData >>>> requestDirection task: $response")
            complete(response.response.status)
            response.response.result?.let {
                route = if (it.isNotEmpty()){
                    it[0]
                }else{
                    null
                }
            }

            task.dispose()
        }
    }



    override fun onCleared() {
        stopReadingFile()
        stopRunningTraceData()
        super.onCleared()
    }

    private fun startReadingFile() {
        CoroutineScope(Dispatchers.Main).launch {
            if (readJob != null && !readJob!!.isActive) {
                readJob?.cancelAndJoin()
            }
            readJob = getLoationsFromRawFile()
            readJob?.await()
        }
    }

    private fun stopReadingFile() {
        CoroutineScope(Dispatchers.Main).launch {
            if (readJob != null && !readJob!!.isActive) {
                readJob?.cancelAndJoin()
            }
        }
    }

    fun startRunningTraceData(){
        CoroutineScope(Dispatchers.Main).launch {
            if (runJob != null && !runJob!!.isActive) {
                runJob?.cancelAndJoin()
            }
            runJob = runFile()
            runJob?.await()
        }
    }

    private fun stopRunningTraceData(){
        CoroutineScope(Dispatchers.Main).launch {
            if (runJob != null && !runJob!!.isActive) {
                runJob?.cancelAndJoin()
            }
        }
    }

    private fun CoroutineScope.runFile() = async(Dispatchers.Default){
        while (navigationOn.value!!) {
            if (locationIndex >= locationsFromFile.size) {
                locationIndex = 0
            }

            locationProvider.setLocation(locationsFromFile[locationIndex])
            locationIndex++
            delay(250)
        }
    }

    private fun CoroutineScope.getLoationsFromRawFile() = async(Dispatchers.Default){
        val gpsfile = when(RegionCachedHelper.getRegion(getApplication())){
            Region.EU -> getApplication<Application>().resources.openRawResource(R.raw.eu_trace)
            Region.NA -> getApplication<Application>().resources.openRawResource(R.raw.gps2)
            else -> getApplication<Application>().resources.openRawResource(R.raw.gps2)
        }

        val reader = BufferedReader(InputStreamReader(gpsfile))
        var str: String? = null
        locationsFromFile.clear()
        try {
            while (true) {
                str = reader.readLine()
                if (str != null) {
                    val scanner = Scanner(str).useDelimiter(",");
                    var str = scanner.next()
                    var lat = str.toDouble()
                    str = scanner.next()
                    val lon = str.toDouble()
                    str = scanner.next()
                    val speed = str.toFloat()
                    str = scanner.next()
                    val bearing = str.toFloat()
                    var location = Location("test")
                    location.latitude = lat
                    location.longitude = lon
                    location.speed = speed
                    location.bearing = bearing
                    locationsFromFile.add(location)
                } else {
                    break
                }
            }

            gpsfile.close()
        }catch (e : Exception){
            e.printStackTrace()
        }
    }

    fun enableAlert(enable : Boolean){
        driveSession?.enableAlert(enable)
        alertNumber.postValue(0)
    }

    fun enableAdas(enable : Boolean){
        driveSession?.enableADAS(enable)
        adasNumber.postValue(0)
    }
}