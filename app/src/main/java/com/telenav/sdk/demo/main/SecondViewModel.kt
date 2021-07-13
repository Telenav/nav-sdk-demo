/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.main

import android.location.Location
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.telenav.map.api.Annotation
import com.telenav.map.api.controllers.Camera
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.entity.model.search.EntitySearchResponse
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import java.util.*

class SecondViewModel() : ViewModel() {
    companion object{
        const val TAG = "ViewModel"

        /**
         * default value of camera zoom level.
         */
        const val DEFAULT_ZOOM_LEVEL = 5f

        /**
         * max number of zoom level
         */
        const val MAX_ZOOM_LEVEL = 17f

        /**
         * min number of zoom level
         */
        const val MIN_ZOOM_LEVEL = 1f

        /**
         * default mode if no follow vehicle mode selected before
         */
        val DEFAULT_FOLLOW_VEHICLE_MODE = Camera.FollowVehicleMode.Enhanced
    }

    val searchResponse = MutableLiveData<EntitySearchResponse?>()
    val searchAnnotations = MutableLiveData<List<Annotation>?>()
    val zoomLevelLiveData = MutableLiveData<String>()

    val routes = MutableLiveData<List<Route>?>()
    val isochronePoints = MutableLiveData<List<LatLon>?>()

    var isSettingLocation : Boolean = false

    var showIsochrone: Boolean = false

    var requestMode : RequestMode = RequestMode.CLOUD_ONLY

    private fun requestIsochrone(origin: GeoLocation, distance: Int) {
        val request = IsochroneRequest.Builder(
            origin
        ).setMaxDistance(distance)
            .build()

        val task = DirectionClient.Factory.hybridClient().createIsochroneTask(request, requestMode)
        task.runAsync { response ->
            Log.d("MapLogsForTestData", "MapLogsForTestData >>>> request isochrone task: $response")
            if (response.response.status == DirectionErrorCode.OK) {
                isochronePoints.postValue(response.response.result?.let { if (it.geometry.isNotEmpty()) it.geometry else null })
            } else {
                isochronePoints.postValue(null)
            }
            task.dispose()
        }
    }

    fun requestDirection(begin: Location, end: Location, wayPointList: MutableList<Location>? = null) {
        Log.d("MapLogsForTestData" , "MapLogsForTestData >>>> requestDirection begin: $begin + end $end  wayPoint $wayPointList")
        val wayPoints:ArrayList<GeoLocation> = arrayListOf()
        wayPointList?.forEach {
            wayPoints.add(GeoLocation(LatLon(it.latitude, it.longitude)))
        }
        val request: RouteRequest = RouteRequest.Builder(
            GeoLocation(begin),
            GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
            .routeCount(2)
            .startTime(Calendar.getInstance().timeInMillis/1000)
            .stopPoints(wayPoints)
            .build()
        val task = DirectionClient.Factory.hybridClient().createRoutingTask(request, requestMode)
        task.runAsync { response ->
            Log.d("MapLogsForTestData" , "MapLogsForTestData >>>> requestDirection task: $response")
            if (response.response.status == DirectionErrorCode.OK) {
                routes.postValue(response.response.result?.let {
                    if (it.size > 0) {
                        if (showIsochrone) {
                            requestIsochrone(GeoLocation(LatLon(begin.latitude, begin.longitude)), it.first().distance)
                        }
                        it
                    } else null
                })
            } else {
                routes.postValue(null)
            }
            task.dispose()
        }
    }

    /**
     * switch request mode and return the changed RequestMode
     */
    fun switchRequestMode() : RequestMode {
        requestMode = if (requestMode == RequestMode.EMBEDDED_ONLY){
            RequestMode.CLOUD_ONLY
        }else{
            RequestMode.EMBEDDED_ONLY
        }
        return requestMode
    }
}