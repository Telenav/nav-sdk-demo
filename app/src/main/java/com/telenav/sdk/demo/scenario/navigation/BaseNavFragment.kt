/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.navigation

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.telenav.map.api.Annotation
import com.telenav.map.api.Margins
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.demo.R
import com.telenav.sdk.drivesession.DriveSession
import com.telenav.sdk.drivesession.NavigationSession
import com.telenav.sdk.drivesession.listener.NavigationEventListener
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.drivesession.model.*
import com.telenav.sdk.demo.util.DemoLocationProvider
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import kotlinx.android.synthetic.main.content_basic_navigation.*
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * a basic [Fragment] for navigation
 * @author tang.hui on 2021/1/14
 */
abstract class BaseNavFragment : Fragment(), PositionEventListener, NavigationEventListener {

    val TAG = this.javaClass.simpleName
    var routeIds = mutableListOf<String>()
    var routes = mutableListOf<Route>()
    val driveSession: DriveSession = DriveSession.Factory.createInstance()
    var navigationSession: NavigationSession? = null
    var vehicleLocation: Location? = null
    val locationProvider: DemoLocationProvider = DemoLocationProvider()

    var highlightedRouteId: String? = null

    val navigationOn = MutableLiveData(false)

    init {
        driveSession.injectLocationProvider(locationProvider)
        driveSession.eventHub?.let {
            it.addNavigationEventListener(this)
            it.addPositionEventListener(this)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        map_view.initialize(savedInstanceState, null)
        // Enable all of the MapView features
        map_view.featuresController().traffic().setEnabled()
        map_view.featuresController().landmarks().setEnabled()
        map_view.featuresController().buildings().setEnabled()
        map_view.featuresController().terrain().setEnabled()
        map_view.featuresController().globe().setDisabled()
        map_view.featuresController().compass().setEnabled()
        map_view.featuresController().scaleBar().setEnabled()
        map_view.featuresController().adiLine().setEnabled()
        map_view.layoutController().setVerticalOffset(-0.5)

        // recenter to vehicle position
        map_view.cameraController().position = Camera.Position.Builder().setLocation(locationProvider.lastKnownLocation).build()

        setupButtons()

        map_view.setOnTouchListener { touchType: TouchType, data: TouchPosition ->
            if (touchType == TouchType.LongClick) {
                map_view.routesController().clear()
                onLongClick(data.geoLocation)
            }
        }
        map_view.setOnRouteTouchListener { touchType, data, routeID ->
            highlightedRouteId = routeID
            map_view.routesController().highlight(routeID)
        }
    }

    open fun onLongClick(location: Location?) {
        location?.let {
            activity?.runOnUiThread {
                // Set annotation at location
                val context = context
                if (context != null) {
                    val factory = map_view.annotationsController().factory()
                    val annotation = factory.create(context, R.drawable.map_pin_green_icon_unfocused, location)
                    annotation.displayText = Annotation.TextDisplayInfo.Centered("Destination")
                    map_view.annotationsController().clear()
                    map_view.annotationsController().add(arrayListOf(annotation))
                }
            }

            vehicleLocation?.let {
                requestDirection(it, location)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        map_view.onPause()
    }

    override fun onDestroyView() {
        driveSession.eventHub?.removePositionEventListener(this)
        driveSession.eventHub?.removeNavigationEventListener(this)
        driveSession.dispose()
        super.onDestroyView()
    }

    open fun requestDirection(begin: Location, end: Location, wayPointList: MutableList<Location>? = null) {
        Log.d("MapLogsForTestData", "MapLogsForTestData >>>> requestDirection begin: $begin + end $end")
        val wayPoints: ArrayList<GeoLocation> = arrayListOf()
        wayPointList?.forEach {
            wayPoints.add(GeoLocation(LatLon(it.latitude, it.longitude)))
        }
        val request: RouteRequest = RouteRequest.Builder(
                GeoLocation(LatLon(begin.latitude, begin.longitude)),
                GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
                .routeCount(2)
                .stopPoints(wayPoints)
                .startTime(Calendar.getInstance().timeInMillis / 1000)
                .build()
        val task = DirectionClient.Factory.hybridClient().createRoutingTask(request, RequestMode.CLOUD_ONLY)
        task.runAsync { response ->
            Log.d(TAG, "MapLogsForTestData >>>> requestDirection task status: ${response.response.status}")
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                routes = response.response.result
                routeIds = map_view.routesController().add(routes)
                map_view.routesController().highlight(routeIds[0])
                val region = map_view.routesController().region(routeIds)
                map_view.cameraController().showRegion(region, Margins.Percentages(0.20, 0.20))
                highlightedRouteId = routeIds[0]
                activity?.runOnUiThread {
                    startNavButton.isEnabled = true
                }

            } else {
                activity?.runOnUiThread {
                    startNavButton.isEnabled = false
                }
            }
            task.dispose()
        }
    }

    private fun setZoomLevel(level: Float) {
        val newLevel = min(max(1f, level), 17f)
        map_view.cameraController().position = Camera.Position.Builder().setZoomLevel(newLevel).build()
    }

    private fun setupButtons() {
        iv_camera_fix.setOnClickListener {
            val newPosition = Camera.Position.Builder().setLocation(vehicleLocation).build()
            map_view.cameraController().position = newPosition
            map_view.cameraController().resumeAutoZoom()
            iv_camera_fix.setImageResource(R.drawable.ic_gps_fixed_24)
        }

        iv_zoom_in.setOnClickListener {
            val currentLevel = map_view.cameraController().position!!.zoomLevel
            setZoomLevel(currentLevel - 1)
        }

        iv_zoom_out.setOnClickListener {
            val currentLevel = map_view.cameraController().position!!.zoomLevel
            setZoomLevel(currentLevel + 1)
        }

        startNavButton.setOnClickListener {
            navigationSession?.stopNavigation()
            startNavButton.isEnabled = false
            stopNavButton.isEnabled = true

            var pickedRouteIndex = 0 // default route first
            if (!highlightedRouteId.isNullOrEmpty()) {
                routes.forEachIndexed { index, route ->
                    if (route.id == highlightedRouteId) {
                        pickedRouteIndex = index
                    }
                }
            }
            val pickedRoute = routes[pickedRouteIndex]
            navigationSession = driveSession.startNavigation(pickedRoute, true, getDemonstrateSpeed())
            navigationOn.postValue(true)
            routeIds.forEachIndexed { index, element ->
                if (index != pickedRouteIndex) {
                    map_view.routesController().remove(element)
                }
            }
            map_view.routesController().updateRouteProgress(pickedRoute.id)
            // enableFollowVehicleMode
            map_view.cameraController().enableFollowVehicleMode(Camera.FollowVehicleMode.HeadingUp)

        }

        stopNavButton.setOnClickListener {
            navigationSession?.stopNavigation()
            navigationOn.postValue(false)
            map_view.annotationsController().clear()
            map_view.routesController().clear()
            map_view.cameraController().disableFollowVehicle()
            activity?.runOnUiThread {
                startNavButton.isEnabled = false
                stopNavButton.isEnabled = false
            }
        }

    }

    open fun getDemonstrateSpeed(): Double {
        return 40.0
    }

    override fun onLocationUpdated(vehicleLocation: Location) {
        this.vehicleLocation = vehicleLocation
        map_view.vehicleController().setLocation(vehicleLocation)
    }

    override fun onStreetUpdated(curStreetInfo: StreetInfo, drivingOffRoad: Boolean) {
        Log.i(TAG, "current street: $curStreetInfo")
    }

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        navEvent.travelEstToDestination?.let {
            println(it.distanceToStop)
        }
    }

    override fun onJunctionViewUpdated(junctionViewInfo: JunctionViewInfo) {
        Log.d(TAG, "onJunctionViewUpdated")
    }

    override fun onAlongRouteTrafficUpdated(alongRouteTraffic: AlongRouteTraffic) {
        Log.d(TAG, "onAlongRouteTrafficUpdated")
    }

    override fun onNavigationStopReached(stopIndex: Int, stopLocation: Int) {
        Log.d(TAG, "onNavigationStopReached: stopIndex=$stopIndex, stopLocation:$stopLocation")
        if (stopIndex == -1) {//-1 means reach destination
            map_view.annotationsController().clear()
            map_view.routesController().clear()
            map_view.cameraController().disableFollowVehicle()
            navigationOn.postValue(false)
            activity?.runOnUiThread {
                startNavButton.isEnabled = false
                stopNavButton.isEnabled = false
            }
        }
    }

    override fun onNavigationRouteUpdated(route: Route, reason: NavigationEventListener.RouteUpdateReason?) {
        route.dispose()
    }

    override fun onBetterRouteDetected(betterRouteCandidate: BetterRouteCandidate) {
        betterRouteCandidate.accept(false)
    }

}