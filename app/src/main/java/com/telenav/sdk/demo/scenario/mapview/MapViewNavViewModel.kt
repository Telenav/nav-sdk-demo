/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.mapview

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.telenav.map.api.MapView
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.drivesession.DriveSession
import com.telenav.sdk.drivesession.NavigationSession
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.drivesession.model.StreetInfo
import com.telenav.sdk.demo.util.DemoLocationProvider
import com.telenav.sdk.drivesession.model.RoadCalibrator
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*

/**
 * This view model help to operate navigation.
 * @author zhai.xiang on 2021/1/28
 */
class MapViewNavViewModel(app: Application) : AndroidViewModel(app), PositionEventListener {
    val startLocation = Location("MapView").apply {
        this.latitude = 37.398762
        this.longitude = -121.977216
        this.bearing = 45.0f
    }

    val stopLocation = Location("MapView").apply {
        this.latitude = 37.40835858
        this.longitude = -121.967860455
    }

    private val driveSession: DriveSession = DriveSession.Factory.createInstance()
    private var navigationSession: NavigationSession? = null
    var currentVehicleLocation = MutableLiveData<Location>(startLocation)
    var route = MutableLiveData<Route?>()
    private val locationProvider = DemoLocationProvider()

    init {
        driveSession.eventHub?.addPositionEventListener(this)
        locationProvider.setLocation(startLocation)
        driveSession.injectLocationProvider(locationProvider)
    }

    override fun onCleared() {
        driveSession.eventHub?.removePositionEventListener(this)
        driveSession.dispose()
        super.onCleared()
    }

    override fun onLocationUpdated(vehicleLocation: Location) {
        currentVehicleLocation.postValue(vehicleLocation)
    }

    override fun onStreetUpdated(curStreetInfo: StreetInfo, drivingOffRoad: Boolean) {
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
    }

    fun requestDirection(begin: Location = startLocation, end: Location = stopLocation) {
        val request: RouteRequest = RouteRequest.Builder(
                GeoLocation(LatLon(begin.latitude, begin.longitude)),
                GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
                .routeCount(1)
                .build()
        val task = DirectionClient.Factory.hybridClient().createRoutingTask(request, RequestMode.CLOUD_ONLY)
        task.runAsync { response ->
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                route.postValue(response.response.result[0]!!)
            } else {
                route.postValue(null)
            }
            task.dispose()
        }
    }

    fun startNavigation(map: MapView) {
        navigationSession?.stopNavigation()
        if (route.value != null) {
            val routeIds = map.routesController().add(listOf(route.value))
            map.routesController().highlight(routeIds[0])
            navigationSession = driveSession.startNavigation(route.value!!, true, 40.0)
            map.routesController().updateRouteProgress(routeIds[0])
        }
    }

    fun stopNavigation(map: MapView) {
        navigationSession?.stopNavigation()
        map.routesController().clear()
    }
}