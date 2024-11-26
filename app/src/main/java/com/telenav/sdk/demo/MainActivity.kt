/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.util.Range
import androidx.appcompat.app.AppCompatActivity
import com.telenav.map.api.Annotation
import com.telenav.map.api.Annotation.Layer.RouteWayPoint
import com.telenav.map.api.Margins
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.touch.GestureType
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.sdk.examples.R
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.drivesession.DriveSession
import com.telenav.sdk.drivesession.NavigationSession
import com.telenav.sdk.drivesession.listener.NavigationEventListener
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.drivesession.model.*
import com.telenav.sdk.drivesession.model.drg.BetterRouteContext
import com.telenav.sdk.drivesession.model.drg.RouteUpdateContext
import com.telenav.sdk.map.SDK
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import com.telenav.sdk.map.model.AlongRouteTraffic
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

/**
 * @author tang.hui on 2021/9/24
 */
class MainActivity : AppCompatActivity(), NavigationEventListener, PositionEventListener {
    private val driveSession: DriveSession = DriveSession.Factory.createInstance()
    private var locationProvider = SimulationLocationProvider(this)
    private var navigationSession: NavigationSession? = null
    private var mapViewInitialized = false
    private var isNavigation = false    //  flag whether in active navigation state
    private var activeRouteId: String? = null
    private val LOG_TAG = "Nav SDK Demo"

    private var vehicleLocation: Location = Location("Demo").apply {
        /*
        //  Telenav-US HQ:
        this.latitude = 37.3982607
        this.longitude = -121.9782241
         */

        this.latitude = 37.4194955
        this.longitude = -122.13814
    }

    init {
        driveSession.injectLocationProvider(locationProvider)
        driveSession.eventHub?.let {
            it.addNavigationEventListener(this)
            it.addPositionEventListener(this)
        }
        locationProvider.setLocation(vehicleLocation)
        locationProvider.start()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, MainActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //  inject customized location provider:
        locationProvider.start()
        driveSession.injectLocationProvider(locationProvider)

        map_view.initialize(savedInstanceState) {
            mapViewInitialized = true
            map_view.vehicleController().setIcon(R.drawable.cvp)

            map_view.featuresController().traffic().setEnabled()
            map_view.featuresController().landmarks().setEnabled()
            map_view.featuresController().buildings().setEnabled()
            map_view.featuresController().terrain().setDisabled()   //  disable terrain
            map_view.featuresController().globe().setEnabled()
            map_view.featuresController().compass().setDisabled()   //  disable compass
            map_view.featuresController().scaleBar().setDisabled()  //  disable scale bar

            //  set zoom level range(1 to 16):
            map_view.cameraController().zoomLevelRange = Range(1.0f, 16.0f)

            // recenter to vehicle position
            map_view.cameraController().position =
                Camera.Position.Builder().setLocation(locationProvider.lastKnownLocation).build()
        }

        map_view?.setOnTouchListener { touchType: TouchType, data: TouchPosition ->
            when (touchType) {
                TouchType.Down, TouchType.Up, TouchType.Click, TouchType.Move, TouchType.Cancel -> {
                    Log.v(
                        "TOUCH_TAG", "Touch type ${touchType}, " +
                                "geoLocation latitude: ${data.geoLocation?.latitude} longitude: ${data.geoLocation?.longitude}"
                    )
                }

                TouchType.LongClick -> {
                    //  during active navigation, disable long-press destination pick
                    if (!isNavigation) {
                        data.geoLocation?.let {
                            runOnUiThread {
                                // Set annotation at location
                                val factory = map_view.annotationsController().factory()
                                val annotation = factory.create(
                                    this,
                                    R.drawable.map_pin_green_icon_unfocused,
                                    data.geoLocation!!
                                )
                                //  annotation.displayText = Annotation.TextDisplayInfo.Centered("Destination")

                                //  Waypoint annotation layer:
                                annotation.layer = Annotation.Layer(RouteWayPoint)

                                //  disable culling for this annotation(always visible):
                                annotation.style = Annotation.Style.ScreenAnnotationFlagNoCulling

                                map_view.annotationsController().clear()
                                map_view.annotationsController().add(arrayListOf(annotation))
                            }
                        }

                        //  TODO("avoid route request while exists one is on the air")
                        val destinationLocation = Location("Demo")
                        destinationLocation.set(data.geoLocation)
                        requestDirection(vehicleLocation, destinationLocation)
                    }
                }
            }
        }

        navButton.setOnClickListener {
            isNavigation = !isNavigation
            if (isNavigation) {
                navigationSession?.stopNavigation()
                navigationSession = driveSession.startNavigation(pickedRoute!!, true, 45.0)

                activeRouteId = pickedRoute!!.id
                activeRouteId?.let {
                    map_view.routesController().updateRouteProgress(it)
                }

                map_view.cameraController()
                    .enableFollowVehicleMode(Camera.FollowVehicleMode.HeadingUp, true)

                //  disable pan during following vehicle mode(just remind since we turned on auto-zoom, so sometimes even user
                //  changed zoom level with gesture but will still back to the calculated zoom automatically):
                val activeGestures = setOf(GestureType.Zoom, GestureType.Tilt)
                map_view.setActiveGestures(activeGestures)

                navButton.setText(R.string.stop_navigation)
            } else {
                //  reset default map gestures:
                val activeGestures = setOf(GestureType.Zoom, GestureType.Pan, GestureType.Rotate, GestureType.Tilt)
                map_view.setActiveGestures(activeGestures)
                handleNavigationSessionEnd(true)
            }
        }
    }

    private fun handleNavigationSessionEnd(forceStop: Boolean) {
        if (forceStop) {
            Log.i(LOG_TAG, "navigation stopped by user")
        } else {
            Log.i(LOG_TAG, "navigation stopped. reach destination")
        }

        navigationSession?.stopNavigation()
        map_view.annotationsController().clear()
        map_view.routesController().clear()

        //  disable following vehicle mode, allow user pan & zoom map:
        map_view.cameraController().disableFollowVehicle()

        //  back to vehicle location and reset to default zoom level(3):
        map_view.cameraController().position =
            Camera.Position.Builder().setLocation(locationProvider.lastKnownLocation).setZoomLevel(3F).build()

        runOnUiThread {
            navButton.isEnabled = false
            navButton.setText(R.string.start_navigation)
        }

        isNavigation = false
        activeRouteId = null
        navigationSession = null
    }

    var pickedRoute: Route? = null

    private fun requestDirection(
        begin: Location,
        end: Location,
        wayPointList: MutableList<Location>? = null
    ) {
        Log.d(LOG_TAG, "requestDirection begin: $begin + end $end")
        val wayPoints: ArrayList<GeoLocation> = arrayListOf()
        wayPointList?.forEach {
            wayPoints.add(GeoLocation(LatLon(it.latitude, it.longitude)))
        }
        val request: RouteRequest = RouteRequest.Builder(
            GeoLocation(begin),
            GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
            .routeCount(1)
            .stopPoints(wayPoints)
            .startTime(Calendar.getInstance().timeInMillis / 1000)
            .build()
        val task = DirectionClient.Factory.hybridClient()
            .createRoutingTask(request, RequestMode.CLOUD_ONLY)
        task.runAsync { response ->
            Log.d(LOG_TAG, "requestDirection task status: ${response.response.status}")
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                map_view.routesController().clear()

                val routes = response.response.result
                val routeIds = map_view.routesController().add(routes)
                map_view.routesController().highlight(routeIds[0])
                val region = map_view.routesController().region(routeIds)
                map_view.cameraController().showRegion(region, Margins.Percentages(0.20, 0.20))
                pickedRoute = routes[0]
                runOnUiThread {
                    navButton.isEnabled = true
                    navButton.setText(R.string.start_navigation)
                }
            } else {
                Log.e(LOG_TAG, "requestDirection task failed! status: ${response.response.status}")

                runOnUiThread {
                    navButton.isEnabled = false
                }
            }

            task.dispose()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        map_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        map_view.onPause()
    }

    override fun onDestroy() {
        driveSession.eventHub?.removePositionEventListener(this)
        driveSession.eventHub?.removeNavigationEventListener(this)
        driveSession.dispose()
        locationProvider.stop()
        SDK.getInstance().dispose()

        Log.i(LOG_TAG, "Telenav SDK disposed")
        super.onDestroy()
    }

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
    }

    override fun onJunctionViewUpdated(junctionViewInfo: JunctionViewInfo) {
    }

    override fun onAlongRouteTrafficUpdated(alongRouteTraffic: AlongRouteTraffic) {
    }

    override fun onLaneGuidanceUpdated(p0: LaneGuidanceEvent) {
    }

    override fun onNavigationStopReached(stopIndex: Int, stopLocation: Int) {
        if (stopIndex == -1) {// -1 means reach destination
            handleNavigationSessionEnd(false)
        }
    }

    override fun onNavigationRouteUpdated(p0: Route, p1: BetterRouteContext?) {
    }

    override fun onNavigationRouteUpdated(route: Route, routeUpdateContext: RouteUpdateContext) {
        activeRouteId?.let {
            if (route.id != it) {
                Log.i(LOG_TAG, "updated route. new route id: " + route.id + "reason: " + routeUpdateContext.reason)

                map_view.routesController().remove(it)
                map_view.routesController().refresh(route)
                map_view.routesController().updateRouteProgress(route.id)
            }
        }

        activeRouteId = route.id
    }

    override fun onBetterRouteInfoUpdated(betterRouteInfo: BetterRouteInfo) {
        TODO("Not yet implemented")
    }

    override fun onBetterRouteDetected(status: NavigationEventListener.BetterRouteDetectionStatus,
                                       betterRouteCandidate: BetterRouteCandidate?) {
        //  TODO("one can decide accept or not according value of status")
        //  ignore the recommended route
        betterRouteCandidate?.accept(false)
    }

    override fun onLocationUpdated(vehicleLocation: Location) {
        if (mapViewInitialized) {
            map_view.vehicleController().setLocation(vehicleLocation)
        }
    }

    override fun onStreetUpdated(curStreetInfo: StreetInfo, drivingOffRoad: Boolean) {
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
    }

    override fun onMMFeedbackUpdated(feedback: MMFeedbackInfo) {
    }
}