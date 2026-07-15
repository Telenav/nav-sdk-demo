/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Looper
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.util.Range
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.telenav.map.views.TnMapView
import com.telenav.map.api.Annotation
import com.telenav.map.api.AutoZoomLevel
import com.telenav.map.api.MapView
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.MapViewReadyListener
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.diagnosis.listener.MapViewStatusListener
import com.telenav.map.api.touch.GestureType
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.demo.search.CategoryQuickButtonsController
import com.telenav.sdk.demo.search.MapRouteCameraHelper
import com.telenav.sdk.demo.search.SearchOverlayController
import com.telenav.sdk.demo.search.SearchServiceHolder
import com.telenav.sdk.demo.search.SearchViewModel
import com.telenav.sdk.drivesession.NavigationSession
import com.telenav.sdk.drivesession.listener.NavigationEventListener
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.drivesession.model.BetterRouteProposal
import com.telenav.sdk.drivesession.model.BetterRouteUpdateProgress
import com.telenav.sdk.drivesession.model.DepartureWaypointInfo
import com.telenav.sdk.drivesession.model.JunctionViewInfo
import com.telenav.sdk.drivesession.model.ManeuverInfo
import com.telenav.sdk.drivesession.model.NavigationEvent
import com.telenav.sdk.drivesession.model.PositionInfo
import com.telenav.sdk.drivesession.model.RoadCalibrator
import com.telenav.sdk.examples.BuildConfig
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.SearchResultItemDao
import com.telenav.sdk.guidance.audio.model.VerbosityLevel
import com.telenav.sdk.map.SDK
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import com.telenav.sdk.map.model.AlongRouteTraffic
import com.telenav.sdk.navigation.NavigationService
import com.telenav.sdk.navigation.model.ChargingStationUnreachableEvent
import com.telenav.sdk.navigation.model.TimedRestrictionEdge
import java.util.*

/**
 * @author tang.hui on 2024/11/26
 */
class MainActivity : AppCompatActivity(), NavigationEventListener, PositionEventListener {
    private val LOG_TAG = "Nav SDK Demo"
    private val driveSession: NavigationService = NavigationService.Factory.createInstance()
    private var locationProvider = SimulationLocationProvider(BuildConfig.Region)
    private var navigationSession: NavigationSession? = null
    private var mapViewInitialized = false
    private var isNavigation = false    //  flag whether in active navigation state
    private var activeRouteId: String? = null
    private var pickedRoute: Route? = null
    private var pendingStartNavigation = false
    private var lastNavigationStartRequest = 0L
    private var lastNavigationEndRequest = 0L
    private var routeRequestGeneration = 0

    private var vehicleLocation: Location = Location("Demo").apply {
        latitude = locationProvider.getLastKnownLocation().latitude
        longitude =locationProvider.getLastKnownLocation().longitude
    }

    private val searchViewModel: SearchViewModel by viewModels()

    private lateinit var mapView: TnMapView
    private lateinit var navButton: Button
    private lateinit var rectSearchButton: Button
    private lateinit var searchOverlay: View
    private var lastDisplayedRouteIds: List<String>? = null

    private val searchOverlayLayoutListener =
        View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            refitRouteCameraIfNeeded()
        }

    init {
        driveSession.alertManager.enableLaneGuidanceDetection(true)
        driveSession.audioGuidanceManager.setVerbosityLevel(VerbosityLevel.VERBOSE)
        SDK.getInstance().injectLocationProvider(locationProvider)
    }

    companion object {
        private val DEFAULT_MAP_GESTURES = setOf(
            GestureType.Zoom,
            GestureType.Pan,
            GestureType.Rotate,
            GestureType.Tilt,
        )

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
        featuresController?.compass()?.setEnabled()
        featuresController?.scaleBar()?.setEnabled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mapView = findViewById(R.id.map_view)
        navButton = findViewById(R.id.navButton)
        rectSearchButton = findViewById(R.id.rectSearchButton)
        searchOverlay = findViewById(R.id.searchOverlay)

        val lastLoc = locationProvider.getLastKnownLocation()
        searchViewModel.setSearchCenter(lastLoc.latitude, lastLoc.longitude)
        SearchServiceHolder.updateLocation(lastLoc.latitude, lastLoc.longitude)
        val refreshSearchCenter: () -> Unit = {
            searchViewModel.setSearchCenter(vehicleLocation.latitude, vehicleLocation.longitude)
            SearchServiceHolder.updateLocation(vehicleLocation.latitude, vehicleLocation.longitude)
        }
        SearchOverlayController(findViewById(R.id.searchOverlay), searchViewModel, this).apply {
            this.refreshSearchCenter = refreshSearchCenter
            bind()
        }
        CategoryQuickButtonsController(findViewById(R.id.categoryQuickButtons), searchViewModel, this)
            .apply {
                this.refreshSearchCenter = refreshSearchCenter
                bind()
            }

        searchViewModel.showSuggestionPanel.observe(this) { showSuggestions ->
            updateNavButtonPosition(showSuggestions == true)
            updateMapOverlayButtons()
        }

        val readyListener = object : MapViewReadyListener<MapView> {
            override fun onReady(view: MapView?) {
                view?.setFPS(60)
                //  set zoom level range(1 to 16):
                mapView.getCameraController()?.zoomLevelRange = Range(1.0f, 16.0f)
                // recenter to vehicle position
                mapView.getCameraController()?.position =
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
            dpi = mapView.defaultDpi,
            readyListener = readyListener,
            createCvp = true,
            autoZoomLevel = AutoZoomLevel.FAR
        )
        mapView.initialize(mapViewConfig)

        val mapViewStatusListener = object : MapViewStatusListener {
            override fun onDrawFirstFrame() {
                Toast.makeText(this@MainActivity, "first frame has drawn", Toast.LENGTH_SHORT).show()
                locationProvider.setLocation(locationProvider.getLastKnownLocation())
                configureMapView(mapView)
            }

            override fun onMapSurfaceChanged() {
            }

        }
        mapView.mapDiagnosis().addMapViewListener(mapViewStatusListener)

        mapView?.setOnTouchListener { touchType: TouchType, data: TouchPosition ->
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
                                val factory = mapView.getAnnotationsController()?.factory()
                                val annotation = factory?.create(
                                    this,
                                    R.drawable.map_pin_green_icon_unfocused,
                                    data.geoLocation!!
                                )
                                  annotation?.displayText = Annotation.TextDisplayInfo.Centered("Destination")

                                //  disable culling for this annotation(always visible):
                                annotation?.style = Annotation.Style.ScreenAnnotationFlagNoCulling

                                mapView.getAnnotationsController()?.clear()
                                mapView.getAnnotationsController()?.add(arrayListOf(annotation))
                            }
                        }

                        //  TODO("avoid route request while exists one is on the air")
                        val destinationLocation = Location("Demo")
                        destinationLocation.set(data.geoLocation!!)
                        requestDirection(vehicleLocation, destinationLocation)
                    }
                }

                else -> Unit
            }
        }

        navButton.setOnClickListener {
            if (isNavigation) {
                endNavigationSession(userInitiated = true)
            } else {
                pendingStartNavigation = false
                startNavigationSession()
            }
        }

        rectSearchButton.setOnClickListener {
            searchViewModel.setSearchCenter(vehicleLocation.latitude, vehicleLocation.longitude)
            SearchServiceHolder.updateLocation(vehicleLocation.latitude, vehicleLocation.longitude)
            val padding = resources.getDimensionPixelSize(R.dimen.dimens_10dp)
            val bounds = MapRouteCameraHelper.computeVisibleGeoBounds(
                mapView,
                searchOverlay,
                navButton,
                rectSearchButton,
                padding
            )
            if (bounds == null) {
                Toast.makeText(this, R.string.rect_search_map_bounds_unavailable, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            searchViewModel.onRectSearch(bounds.bottomLeft, bounds.topRight)
        }

        searchViewModel.selectedLocation.observe(this) { item ->
            item?.let { showDestinationAndRequestRoute(it) }
        }

        searchViewModel.routePreview.observe(this) { item ->
            showDestinationAndRequestRoute(item)
        }

        searchViewModel.navigationStartRequest.observe(this) { trigger ->
            if (trigger == 0L || trigger == lastNavigationStartRequest) return@observe
            lastNavigationStartRequest = trigger
            pendingStartNavigation = true
            tryStartNavigation()
        }

        searchViewModel.navigationEndRequest.observe(this) { trigger ->
            if (trigger == 0L || trigger == lastNavigationEndRequest) return@observe
            lastNavigationEndRequest = trigger
            if (isNavigation) {
                endNavigationSession(userInitiated = true)
            }
        }

        searchViewModel.showDetailPanel.observe(this) { showDetail ->
            updateMapOverlayButtons()
            if (showDetail == true) {
                searchOverlay.addOnLayoutChangeListener(searchOverlayLayoutListener)
                searchOverlay.post { refitRouteCameraIfNeeded() }
            } else {
                searchOverlay.removeOnLayoutChangeListener(searchOverlayLayoutListener)
            }
        }

        searchViewModel.clearRoutePreview.observe(this) { trigger ->
            if (trigger == 0L || isNavigation) return@observe
            cancelRoutePreview()
        }

        driveSession.eventHub.let {
            it.addNavigationEventListener(this)
            it.addPositionEventListener(this)
        }
        locationProvider.onStart()
    }

    private fun updateMapOverlayButtons() {
        val inResultsMode = searchViewModel.showSuggestionPanel.value != true
        val detailOpen = searchViewModel.showDetailPanel.value == true
        rectSearchButton.visibility =
            if (inResultsMode && !detailOpen && !isNavigation) View.VISIBLE else View.GONE
        val showNavButton = when {
            detailOpen -> false
            inResultsMode && !isNavigation -> false
            else -> true
        }
        navButton.visibility = if (showNavButton) View.VISIBLE else View.GONE
    }

    private fun cancelRoutePreview() {
        routeRequestGeneration++
        pendingStartNavigation = false
        pickedRoute = null
        activeRouteId = null
        lastDisplayedRouteIds = null
        mapView.getRoutesController()?.clear()
        mapView.getAnnotationsController()?.clear()
        navButton.isEnabled = false
        navButton.setText(R.string.start_navigation)
        updateMapOverlayButtons()
    }

    private fun showDestinationAndRequestRoute(item: SearchResultItemDao) {
        if (isNavigation) {
            endNavigationSession(userInitiated = true)
        }
        mapView.getAnnotationsController()?.clear()
        val factory = mapView.getAnnotationsController()?.factory()
        val destAnnotation = factory!!.create(
            this,
            R.drawable.map_pin_green_icon_unfocused,
            item.displayLocation
        )
        destAnnotation.displayText =
            Annotation.TextDisplayInfo.Centered(item.displayText)
                .apply {
                    this.textColor = Color.BLACK
                }
        destAnnotation.style = Annotation.Style.ScreenAnnotationPopup
        mapView.getAnnotationsController()?.add(arrayListOf(destAnnotation))
        val destination = item.navLocation ?: item.displayLocation
        val generation = ++routeRequestGeneration
        requestDirection(vehicleLocation, destination, generation = generation)
    }

    private fun tryStartNavigation() {
        if (pickedRoute == null) return
        pendingStartNavigation = false
        startNavigationSession()
    }

    private fun startNavigationSession() {
        val route = pickedRoute ?: return
        isNavigation = true
        driveSession.stopNavigation()
        navigationSession = driveSession.startNavigation(route, true, 45.0)

        activeRouteId = route.id
        activeRouteId?.let {
            mapView.getRoutesController()?.updateRouteProgress(it)
        }

        mapView.getCameraController()?.enableFollowVehicleMode(Camera.FollowVehicleMode.HeadingUp, true)

        mapView.setActiveGestures(DEFAULT_MAP_GESTURES)

        navButton.isEnabled = true
        navButton.setText(R.string.stop_navigation)
        updateMapOverlayButtons()
        if (searchViewModel.showDetailPanel.value == true) {
            searchViewModel.setDetailNavigationActive(true)
        }
    }

    private fun endNavigationSession(userInitiated: Boolean) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread { endNavigationSession(userInitiated) }
            return
        }
        if (!isNavigation) {
            searchViewModel.setDetailNavigationActive(false)
            return
        }
        val detailOpen = searchViewModel.showDetailPanel.value == true
        handleNavigationSessionEnd(userInitiated)
        if (detailOpen) {
            if (userInitiated) {
                searchViewModel.setDetailNavigationActive(false)
                restoreRoutePreviewIfDetailOpen()
            } else {
                cancelRoutePreview()
                searchViewModel.onNavigationDestinationReached()
            }
        } else {
            searchViewModel.setDetailNavigationActive(false)
        }
    }

    private fun restoreRoutePreviewIfDetailOpen() {
        if (searchViewModel.showDetailPanel.value != true) return
        searchViewModel.detailItem.value?.navigationItem?.let { item ->
            showDestinationAndRequestRoute(item)
        }
    }

    private fun handleNavigationSessionEnd(forceStop: Boolean) {
        if (forceStop) {
            Log.i(LOG_TAG, "navigation stopped by user")
        } else {
            Log.i(LOG_TAG, "navigation stopped. reach destination")
        }

        driveSession.stopNavigation()
        mapView.getAnnotationsController()?.clear()
        mapView.getRoutesController()?.clear()

        //  disable following vehicle mode, allow user pan & zoom map:
        mapView.getCameraController()?.disableFollowVehicle()

        restoreMapGestures()

        //  back to vehicle location and reset to default zoom level(3):
        mapView.getCameraController()?.position =
            Camera.Position.Builder().setLocation(locationProvider.getLastKnownLocation()).setZoomLevel(3F).build()

        runOnUiThread {
            navButton.isEnabled = false
            navButton.setText(R.string.start_navigation)
        }

        isNavigation = false
        activeRouteId = null
        navigationSession = null
        updateMapOverlayButtons()
    }

    private fun restoreMapGestures() {
        mapView.setActiveGestures(DEFAULT_MAP_GESTURES)
    }


    private fun requestDirection(
        begin: Location,
        end: Location,
        wayPointList: MutableList<Location>? = null,
        generation: Int = routeRequestGeneration
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
            if (generation != routeRequestGeneration) {
                task.dispose()
                return@runAsync
            }
            Log.d(LOG_TAG, "requestDirection task status: ${response.response.status}")
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                mapView.getRoutesController()?.clear()

                val routes = response.response.result
                val routeIds = mapView.getRoutesController()?.add(routes)
                if (routeIds?.isNotEmpty() == true) {
                    mapView.getRoutesController()?.highlight(routeIds[0])
                    pickedRoute = routes[0]
                    activeRouteId = pickedRoute!!.id
                    lastDisplayedRouteIds = routeIds
                    runOnUiThread {
                        if (generation != routeRequestGeneration) return@runOnUiThread
                        searchOverlay.post {
                            if (generation != routeRequestGeneration) return@post
                            refitRouteCameraIfNeeded()
                        }
                        applyRouteReadyUi()
                        if (pendingStartNavigation) {
                            tryStartNavigation()
                        }
                    }
                }
            } else {
                Log.e(LOG_TAG, "requestDirection task failed! status: ${response.response.status}")

                runOnUiThread {
                    if (generation != routeRequestGeneration) return@runOnUiThread
                    navButton.isEnabled = false
                    pendingStartNavigation = false
                    updateMapOverlayButtons()
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
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        driveSession.eventHub.removePositionEventListener(this)
        driveSession.eventHub.removeNavigationEventListener(this)
        SDK.getInstance().injectLocationProvider(null)
        driveSession.dispose()
        locationProvider.onStop()
        SearchServiceHolder.release()
        SDK.getInstance().dispose()
        Log.i(LOG_TAG, "Telenav SDK disposed")
        super.onDestroy()
    }

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
    }

    override fun onNavigationRouteUpdating(progress: BetterRouteUpdateProgress) {
        if (progress.newRoute != null && progress.status == BetterRouteUpdateProgress.Status.SUCCEEDED) {
            if (progress.newRoute?.id != activeRouteId) {
                pickedRoute?.id?.let { mapView.getRoutesController()?.remove(it) }
                mapView.getRoutesController()?.refresh(progress.newRoute!!)
                mapView.getRoutesController()?.updateRouteProgress(progress.newRoute!!.id)
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
            endNavigationSession(userInitiated = false)
        }
    }

    override fun onTimedRestrictionEventUpdated(timedRestrictionEdges: List<TimedRestrictionEdge>) {
    }

    override fun onTurnByTurnListUpdated(maneuverInfoList: List<ManeuverInfo>) {
    }

    override fun onLocationUpdated(vehicleLocation: Location, positionInfo: PositionInfo) {
        this.vehicleLocation.set(vehicleLocation)
        runOnUiThread {
            if (isDestroyed) return@runOnUiThread
            searchViewModel.setSearchCenter(vehicleLocation.latitude, vehicleLocation.longitude)
            SearchServiceHolder.updateLocation(vehicleLocation.latitude, vehicleLocation.longitude)
            if (mapViewInitialized) {
                mapView.getVehicleController()?.setLocation(vehicleLocation)
            }
        }
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
    }

    private fun refitRouteCameraIfNeeded() {
        val routeIds = lastDisplayedRouteIds ?: return
        if (searchViewModel.showDetailPanel.value != true && !isNavigation) {
            return
        }
        val padding = resources.getDimensionPixelSize(R.dimen.dimens_10dp)
        val visibleRect = MapRouteCameraHelper.computeVisibleRect(
            mapView,
            searchOverlay,
            navButton,
            padding,
            rectSearchButton,
        )
        MapRouteCameraHelper.showRoutesInVisibleRegion(mapView, routeIds, visibleRect)
    }

    private fun applyRouteReadyUi() {
        navButton.isEnabled = true
        navButton.setText(
            if (isNavigation) R.string.stop_navigation else R.string.start_navigation
        )
        updateMapOverlayButtons()
    }

    private fun updateNavButtonPosition(centered: Boolean) {
        val params = navButton.layoutParams as ConstraintLayout.LayoutParams
        if (centered) {
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            params.horizontalBias = 0.5f
            params.marginEnd = 0
        } else {
            params.startToStart = ConstraintLayout.LayoutParams.UNSET
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            params.horizontalBias = 1f
            params.marginEnd = resources.getDimensionPixelSize(R.dimen.dimens_10dp)
        }
        navButton.layoutParams = params
    }

}