package com.telenav.sdk.demo.scenario.mapview

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.telenav.map.api.MapView
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.demo.provider.DemoLocationProvider
import com.telenav.sdk.drivesession.DriveSession
import com.telenav.sdk.drivesession.NavigationSession
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.drivesession.model.MMFeedbackInfo
import com.telenav.sdk.drivesession.model.RoadCalibrator
import com.telenav.sdk.drivesession.model.StreetInfo
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*

/**
 * This view model help to operate navigation.
 * @author zhai.xiang on 2021/1/28
 */
class MapViewNavViewModel(app: Application) : AndroidViewModel(app), PositionEventListener {
    val startLocation : Location
    val stopLocation : Location

    val driveSession: DriveSession = DriveSession.Factory.createInstance()
    private var navigationSession: NavigationSession? = null
    var currentVehicleLocation : MutableLiveData<Location>
    var currentRoute = MutableLiveData<Route?>()
    var route = MutableLiveData<Route?>()
    private val locationProvider = DemoLocationProvider.Factory.createProvider(app,DemoLocationProvider.ProviderType.SIMULATION)
    private var navigationOn = false

    init {
        driveSession.eventHub?.addPositionEventListener(this)
        driveSession.injectLocationProvider(locationProvider)
        startLocation = locationProvider.lastKnownLocation
        stopLocation = Location("").apply {
            this.longitude = startLocation.longitude + 0.1
            this.latitude = startLocation.latitude + 0.1
        }
        currentVehicleLocation = MutableLiveData<Location>(startLocation)
        locationProvider.start()
    }

    override fun onCleared() {
        driveSession.eventHub?.removePositionEventListener(this)
        driveSession.dispose()
        locationProvider.stop()
        super.onCleared()
    }

    override fun onLocationUpdated(vehicleLocation: Location) {
        currentVehicleLocation.postValue(vehicleLocation)
    }

    override fun onStreetUpdated(curStreetInfo: StreetInfo, drivingOffRoad: Boolean) {
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
    }

    override fun onMMFeedbackUpdated(feedback: MMFeedbackInfo) {
    }

    fun requestDirection(begin: Location = startLocation, end: Location = stopLocation, result : ((Boolean)->Unit)? = null ) {
        val request: RouteRequest = RouteRequest.Builder(
            GeoLocation(begin),
            GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
            .routeCount(1)
            .build()
        val task = DirectionClient.Factory.hybridClient().createRoutingTask(request)
        task.runAsync { response ->
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                route.postValue(response.response.result[0]!!)
                result?.invoke(true)
            } else {
                route.postValue(null)
                result?.invoke(false)
            }
            task.dispose()
        }
    }

    fun startNavigation(map: MapView) {
        navigationSession?.stopNavigation()
        if (route.value != null) {
            currentRoute.postValue(route.value)
            val routeIds = map.routesController().add(listOf(route.value))
            map.routesController().highlight(routeIds[0])
            navigationSession = driveSession.startNavigation(route.value!!, true, 20.0)
            navigationOn = true
            map.routesController().updateRouteProgress(routeIds[0])
        }
    }

    fun stopNavigation(map: MapView) {
        navigationOn = false
        navigationSession?.stopNavigation()
        currentRoute.postValue(null)
        map.routesController().clear()
    }

    fun isNavigationOn() : Boolean{
        return navigationOn
    }
}