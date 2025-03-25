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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.telenav.map.api.Annotation
import com.telenav.map.api.AutoZoomLevel
import com.telenav.map.api.MapView
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.MapViewReadyListener
import com.telenav.map.api.Margins
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.controllers.RouteRenderOptions
import com.telenav.map.api.controllers.VehicleController
import com.telenav.map.api.diagnosis.listener.MapViewStatusListener
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
import com.telenav.sdk.guidance.audio.model.VerbosityLevel
import com.telenav.sdk.map.SDK
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import com.telenav.sdk.map.model.AlongRouteTraffic
import com.telenav.sdk.navigation.model.ChargingStationUnreachableEvent
import com.telenav.sdk.navigation.model.TimedRestrictionEdge
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

/**
 * @author tang.hui on 2024/11/26
 */
class MainActivity : AppCompatActivity(), NavigationEventListener, PositionEventListener {
    private val LOG_TAG = "Nav SDK Demo"
    private val driveSession: DriveSession = DriveSession.Factory.createInstance()
    private var locationProvider = SimulationLocationProvider(this)
    private var navigationSession: NavigationSession? = null
    private var mapViewInitialized = false
    private var isNavigation = false    //  flag whether in active navigation state
    private var activeRouteId: String? = null
    private var pickedRoute: Route? = null

    private var vehicleLocation: Location = Location("Demo").apply {
        //  city center of "Frankfurt, Germany":
        latitude = 50.10215257
        longitude = 8.681829184
    }

    init {
        driveSession.alertManager.enableLaneGuidanceDetection(true)
        driveSession.audioGuidanceManager.setVerbosityLevel(VerbosityLevel.VERBOSE)
        driveSession.injectLocationProvider(locationProvider)
        driveSession.eventHub.let {
            it.addNavigationEventListener(this)
            it.addPositionEventListener(this)
        }
        //  inject customized location provider:
        locationProvider.setLocation(vehicleLocation)
        locationProvider.onStart()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, MainActivity::class.java))
        }
    }

    private fun configureMapView(mapView: MapView) {
        // Enable all of the MapView features
        val featuresController = mapView.getFeaturesController()
        featuresController?.traffic()?.setEnabled()
        featuresController?.freeFlowTraffic()?.setEnabled()
        featuresController?.landmarks()?.setEnabled()
        featuresController?.buildings()?.setEnabled()
        featuresController?.flatTerrain()?.setDisabled()
        featuresController?.globe()?.setEnabled()
        featuresController?.terrain()?.setEnabled()
        featuresController?.compass()?.setEnabled()
        featuresController?.scaleBar()?.setEnabled()
        featuresController?.roadBubbles()?.setEnabled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val readyListener = object : MapViewReadyListener<MapView> {
            override fun onReady(view: MapView?) {
                view?.setFPS(60)
                //  set zoom level range(1 to 16):
                map_view.getCameraController()?.zoomLevelRange = Range(1.0f, 16.0f)
                // recenter to vehicle position
                map_view.getCameraController()?.position =
                    Camera.Position.Builder().setLocation(vehicleLocation).build()
                mapViewInitialized = true
            }

            // by default it will use the default interface method which returns eFeatureCategory_Vital
            // it is not necessary to be implemented
            override fun getReadyFeaturesMask(): Int {
                return MapView.eFeatureCategory_Vital
            }
        }

        val mapViewConfig = MapViewInitConfig(
            context = this.applicationContext,
            lifecycleOwner = this,
            dpi = map_view.defaultDpi,
            defaultLocation = Location("").apply {
                this.latitude = 50.10215257
                this.longitude = 8.681829184
            },
            readyListener = readyListener,
            createCvp = true,
            autoZoomLevel = AutoZoomLevel.FAR
        )
        map_view.initialize(mapViewConfig)

        val mapViewStatusListener = object : MapViewStatusListener {
            override fun onDrawFirstFrame() {
                Toast.makeText(this@MainActivity, "first frame has drawn", Toast.LENGTH_SHORT).show()
                locationProvider.setLocation(locationProvider.getLastKnownLocation())
                configureMapView(map_view)
            }

            override fun onMapSurfaceChanged() {
            }

        }
        map_view.mapDiagnosis().addMapViewListener(mapViewStatusListener)

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
                                val factory = map_view.getAnnotationsController()?.factory()
                                val annotation = factory?.create(
                                    this,
                                    R.drawable.map_pin_green_icon_unfocused,
                                    data.geoLocation!!
                                )
                                  annotation?.displayText = Annotation.TextDisplayInfo.Centered("Destination")

                                //  disable culling for this annotation(always visible):
                                annotation?.style = Annotation.Style.ScreenAnnotationFlagNoCulling

                                map_view.getAnnotationsController()?.clear()
                                map_view.getAnnotationsController()?.add(arrayListOf(annotation))
                            }
                        }

                        //  TODO("avoid route request while exists one is on the air")
                        val destinationLocation = Location("Demo")
                        destinationLocation.set(data.geoLocation!!)
                        requestDirection(vehicleLocation, destinationLocation)
                    }
                }
            }
        }

        navButton.setOnClickListener {
            isNavigation = !isNavigation
            if (isNavigation) {
                driveSession.stopNavigation()
                navigationSession = driveSession.startNavigation(pickedRoute!!, true, 45.0)

                activeRouteId = pickedRoute!!.id
                activeRouteId?.let {
                    map_view.getRoutesController()?.updateRouteProgress(it)
                }

                map_view.getCameraController()?.enableFollowVehicleMode(Camera.FollowVehicleMode.HeadingUp, true)

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

        driveSession.stopNavigation()
        map_view.getAnnotationsController()?.clear()
        map_view.getRoutesController()?.clear()

        //  disable following vehicle mode, allow user pan & zoom map:
        map_view.getCameraController()?.disableFollowVehicle()

        //  back to vehicle location and reset to default zoom level(3):
        map_view.getCameraController()?.position =
            Camera.Position.Builder().setLocation(locationProvider.getLastKnownLocation()).setZoomLevel(3F).build()

        runOnUiThread {
            navButton.isEnabled = false
            navButton.setText(R.string.start_navigation)
        }

        isNavigation = false
        activeRouteId = null
        navigationSession = null
    }


    private fun requestDirection(
        begin: Location,
        end: Location,
        wayPointList: MutableList<Location>? = null
    ) {
        Log.d(LOG_TAG, "requestDirection begin: $begin + end $end")
        val wayPoints: ArrayList<Waypoint> = arrayListOf()
        wayPointList?.forEach {
            wayPoints.add(Waypoint(GeoLocation(LatLon(it.latitude, it.longitude))))
        }
        val request: RouteRequest = RouteRequest.Builder(
            GeoLocation(begin),
            GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
            .routeCount(1)
            .stopPoints(wayPoints)
            .build()
        val task = DirectionClient.Factory.hybridClient()
            .createRoutingTask(request, RequestMode.CLOUD_ONLY)
        task.runAsync { response ->
            Log.d(LOG_TAG, "requestDirection task status: ${response.response.status}")
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                map_view.getRoutesController()?.clear()

                val routes = response.response.result
                val routeIds = map_view.getRoutesController()?.add(routes)
                if (routeIds?.isNotEmpty() == true) {
                    map_view.getRoutesController()?.highlight(routeIds[0])
                    val region = map_view.getRoutesController()?.region(routeIds)
                    map_view.getCameraController()?.showRegion(region, Margins.Percentages(0.20, 0.20))
                    pickedRoute = routes[0]
                    activeRouteId = pickedRoute!!.id
                    runOnUiThread {
                        navButton.isEnabled = true
                        navButton.setText(R.string.start_navigation)
                    }
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
        driveSession.eventHub.removePositionEventListener(this)
        driveSession.eventHub.removeNavigationEventListener(this)
        driveSession.injectLocationProvider(null)
        driveSession.dispose()
        locationProvider.onStop()
        SDK.getInstance().dispose()
        Log.i(LOG_TAG, "Telenav SDK disposed")
        super.onDestroy()
    }

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
    }

    override fun onNavigationRouteUpdating(progress: BetterRouteUpdateProgress) {
        if (progress.newRoute != null && progress.status == BetterRouteUpdateProgress.Status.SUCCEEDED) {
            if (progress.newRoute?.id != activeRouteId) {
                pickedRoute?.id?.let { map_view.getRoutesController()?.remove(it) }
                map_view.getRoutesController()?.refresh(progress.newRoute!!)
                map_view.getRoutesController()?.updateRouteProgress(progress.newRoute!!.id)
            }
            activeRouteId = progress.newRoute?.id
        }
    }

    override fun onJunctionViewUpdated(junctionViewInfo: JunctionViewInfo) {
    }

    override fun onAlongRouteTrafficUpdated(alongRouteTraffic: AlongRouteTraffic) {
    }

    override fun onBetterRouteDetected(proposal: BetterRouteProposal) {
        Log.i(LOG_TAG, "onBetterRouteDetected: ${proposal.reason.name}")
        if (proposal.status == BetterRouteProposal.Status.NEW_ROUTE_DETECTED) {
            navigationSession?.acceptRouteProposal(proposal)
        }
    }

    override fun onChargingStationUnreachableEventUpdated(unreachableEvent: ChargingStationUnreachableEvent) {
    }

    override fun onDepartWaypoint(departureWaypointInfo: DepartureWaypointInfo) {
    }

    override fun onNavigationStopReached(stopIndex: Int, stopLocation: Int) {
        if (stopIndex == -1) {// -1 means reach destination
            handleNavigationSessionEnd(false)
        }
    }

    override fun onTimedRestrictionEventUpdated(timedRestrictionEdges: List<TimedRestrictionEdge>) {
    }

    override fun onTurnByTurnListUpdated(maneuverInfoList: List<ManeuverInfo>) {
    }

    override fun onLocationUpdated(vehicleLocation: Location, positionInfo: PositionInfo) {
        if (mapViewInitialized) {
            map_view.getVehicleController()?.setLocation(vehicleLocation)
        }
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
    }


}