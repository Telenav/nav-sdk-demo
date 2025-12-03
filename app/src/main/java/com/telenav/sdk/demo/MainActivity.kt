/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Range
import android.util.TypedValue
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.telenav.map.api.Annotation
import com.telenav.map.api.AutoZoomLevel
import com.telenav.map.api.MapView
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.MapViewReadyListener
import com.telenav.map.api.Margins
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.diagnosis.listener.MapViewStatusListener
import com.telenav.map.api.touch.GestureType
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.map.api.touch.listeners.RouteTouchListener
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

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
//    private var pickedRoute: Route? = null
    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    private val jobList = CopyOnWriteArrayList<Job>()
    // ideally the routeid used for rendering could be different from its intrinsic id;
    // so the id returned from the addRoutes from the mapview's routecontroller, could be different;
    // anyway it's crucial to get the route object from its routeid, when user select the route, so it could be used
    // for navigation purpose;
    private var routes:  MutableList<Route> = ArrayList()
    // routeid: list<weather event>
    private var routeWeather = mutableMapOf<String, List<WeatherEvent>>()

    // record last known location and feed it into the first drawn frame;
    private var lastKnownLocation: Location? = null

    // Location to show if gps hasn't gain signal yet
    private var defaultLocation: Location = Location("Demo").apply {
        // 37.38910, -121.97184 TN HQ
        latitude = 37.38910
        longitude = -121.97184
    }

    private var highlightedRoute: String? = null

    private fun runInMain(run: suspend CoroutineScope.() -> Unit): Job {
        val job = mainScope.launch {
            run()
        }
        jobList.add(job)
        return job
    }

    private fun printInfoLog(msg: String) {
        Log.i(LOG_TAG, "$msg | Thread name: ${Thread.currentThread().name}")
    }

    private fun showToast(msg: String) {
        runInMain {
            Toast.makeText(
                this@MainActivity,
                msg,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val routeTouchListener =
        RouteTouchListener { touchType: TouchType, data: TouchPosition, routeID: String ->
            val geoLocation = data.geoLocation
            printInfoLog(
                "Getting click type ${touchType}, " +
                        "data $data route id $routeID " +
                        "geoLocation ${geoLocation?.latitude}, ${geoLocation?.longitude}"
            )
            showToast(
                "Getting click type ${touchType}, " +
                        "data $data route id $routeID " +
                        "geoLocation ${geoLocation?.latitude}, ${geoLocation?.longitude}"
            )

            if (activeRouteId == routeID) return@RouteTouchListener
            activeRouteId = routeID
            map_view.routesController().highlight(activeRouteId!!)
            // TODO: show all the weather events on map when tap on the route
            showWeatherEventsOnSelectRoute(routeID)
        }

    // TODO: make it real location instead of fake one.
    init {
        driveSession.alertManager.enableLaneGuidanceDetection(true)
        driveSession.audioGuidanceManager.setVerbosityLevel(VerbosityLevel.VERBOSE)
        // NOTE: bypass the gps simulation, use physical location instead;
        driveSession.injectLocationProvider(null)
        driveSession.eventHub.let {
            it.addNavigationEventListener(this)
            it.addPositionEventListener(this)
        }

        //  inject customized location provider:
//        locationProvider.setLocation(vehicleLocation)
//        locationProvider.onStart()
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

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // TODO: read from file for last known location;
        val readyListener = object : MapViewReadyListener<MapView> {
            override fun onReady(view: MapView?) {
                view?.setFPS(60)

                map_view.setOnRouteTouchListener(routeTouchListener)

                //  set zoom level range(1 to 16):
                map_view.getCameraController()?.zoomLevelRange = Range(1.0f, 16.0f)
                // recenter to vehicle position
                map_view.getCameraController()?.position =
                    Camera.Position.Builder().setLocation(defaultLocation).build()
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
            defaultLocation = this.defaultLocation,
            readyListener = readyListener,
            createCvp = true,
            autoZoomLevel = AutoZoomLevel.FAR
        )

        map_view.initialize(mapViewConfig)

        val mapViewStatusListener = object : MapViewStatusListener {
            override fun onDrawFirstFrame() {
                Toast.makeText(this@MainActivity, "first frame has drawn", Toast.LENGTH_SHORT).show()
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

                        val startLocation = lastKnownLocation ?: run {
                            Toast.makeText(
                                this@MainActivity,
                                "Move to open area, use default location instead.",
                                Toast.LENGTH_SHORT
                            ).show()
                            defaultLocation
                        }
                        requestDirection(startLocation, destinationLocation)
                    }
                }
            }
        }

        navButton.setOnClickListener {
            isNavigation = !isNavigation
            if (isNavigation) {
                driveSession.stopNavigation()
                routes.forEach {
                    if (it.id == activeRouteId) {
                        navigationSession = driveSession.startNavigation(it, true, 45.0)
                    } else {
                        map_view.routesController().remove(it.id)
                    }
                }

//                activeRouteId = pickedRoute!!.id
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
        val startLocation = lastKnownLocation ?: run {
            Toast.makeText(
                this@MainActivity,
                "Move to open area, use default location instead.",
                Toast.LENGTH_SHORT
            ).show()
            defaultLocation
        }

        map_view.getCameraController()?.position =
            Camera.Position.Builder().setLocation(startLocation).setZoomLevel(3F).build()

        runOnUiThread {
            navButton.isEnabled = false
            navButton.setText(R.string.start_navigation)
        }

        isNavigation = false
        activeRouteId = null
        navigationSession = null
    }

    enum class WeatherType {
        UNKNOWN,
        AQUAPLANE,
        LOW_VISIBILITY,
        ICY;
        companion object {
            fun fromString(value: String?): WeatherType {
                return when (value?.uppercase()) {
                    "AQUAPLANE" -> AQUAPLANE
                    "LOW_VISIBILITY" -> LOW_VISIBILITY
                    "ICY" -> ICY
                    else -> UNKNOWN // default fallback
                }
            }
        }
    }

    enum class Severity {
        MINOR,
        MEDIUM,
        MAJOR,
        CRITICAL;
        companion object {
            fun fromString(value: String?): Severity {
                return when (value?.uppercase()) {
                    "MINOR" -> MINOR
                    "MEDIUM" -> MEDIUM
                    "MAJOR" -> MAJOR
                    "CRITICAL" -> CRITICAL
                    else -> MINOR // default fallback
                }
            }
        }
    }

    data class WeatherEvent(
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
        var type: WeatherType = WeatherType.UNKNOWN,
        var severity: Severity = Severity.MINOR
    )

    @RequiresApi(Build.VERSION_CODES.N)
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
        // default seasonal restriction default should be true.
        val routePref = RoutePreferences.Builder().enableRouteSafety(true).avoidSeasonalRestrictions(true).build()

        val request: RouteRequest = RouteRequest.Builder(
            GeoLocation(begin),
            GeoLocation(LatLon(end.latitude, end.longitude)),
            routePref
        ).contentLevel(ContentLevel.FULL)
            .routeCount(3)
            .stopPoints(wayPoints)
            .build()

        val task = DirectionClient.Factory.hybridClient().createRoutingTask(request, RequestMode.CLOUD_ONLY)
        task.runAsync { response ->
            Log.d(LOG_TAG, "requestDirection task status: ${response.response.status}")
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                map_view.getRoutesController()?.clear()
//                val routes = response.response.result
                routes = response.response.result
                val routeIds = map_view.getRoutesController()?.add(routes)
                if (routeIds?.isNotEmpty() == true) {
                    // NOTE: select route -> routeid: weather events;
                    routeWeather.clear()
                    routes.forEach(){ route ->
                        var events = mutableListOf<WeatherEvent>()
                        route.routeMetaInfo?.diagnostics?.forEach() {
                            if (it.topic == "WEATHER_EVENT") {
                                var event = WeatherEvent()
                                it.payload?.forEach { key, value ->
                                    // NOTE: key has multiple values
                                    // Log.d("RouteMeta: ", "Key: $key")
                                    value.forEach() { result ->
                                        Log.d("RouteMeta: ", "value entry: $result")
                                        if (key == "severity") {
                                            event.severity = Severity.fromString(result)
                                        }
                                        if (key == "longitude") {
                                            event.longitude = result.toDouble()
                                        }

                                        if (key == "latitude") {
                                            event.latitude = result.toDouble()
                                        }

                                        if (key == "type") {
                                            event.type = WeatherType.fromString(result)
                                        }
                                    }
                                }

                                events.add(event)
                            }
                        }

                        if (events.size > 0) {
                            routeWeather[route.id] = events
                        }
                    }

                    map_view.getRoutesController()?.highlight(routeIds[0])
                    val region = map_view.getRoutesController()?.region(routeIds)
                    map_view.getCameraController()?.showRegion(region, Margins.Percentages(0.20, 0.20))
                    // route default selection id.
                    activeRouteId = routes[0].id
                    // only show weather conditions on highlighted route
                    // select route id to show weather events;
                    showWeatherEventsOnSelectRoute(activeRouteId)

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

    private fun showWeatherEventsOnSelectRoute(routeid: String?) {
        if (routeWeather.isNotEmpty() && routeWeather[routeid]?.isEmpty() == false) {
            var events = routeWeather[routeid]
            var annotations = mutableListOf<com.telenav.map.api.Annotation>()
            events?.forEach() {
                val location = Location("weather").apply {
                    this.latitude = it?.latitude
                    this.longitude = it?.longitude
                }

                val drawable = getResourceBasedOnWeatherType(it.type)
                if (drawable != -1) {
                    val sizeInPx = dpToPx(50f, this)  // e.g., 50dp target size
                    val bitmap = BitmapFactory.decodeResource(resources, drawable)
                    val originalWidth = bitmap.width
                    val originalHeight = bitmap.height
                    val aspectRatio = originalHeight.toFloat() / originalWidth.toFloat()
                    val targetHeight = (sizeInPx * aspectRatio).toInt()
                    val scaled = Bitmap.createScaledBitmap(
                        bitmap,
                        sizeInPx,
                        targetHeight,
                        true
                    )
                    val anno = map_view.getAnnotationsController()?.factory()
                        ?.create(this, Annotation.UserGraphic(scaled, false), location)
                    // always display
                    anno?.style = Annotation.Style.ScreenAnnotationPopup

                    Log.d(
                        "Weather",
                        "type: ${it.type}, loc: ${location.latitude}, ${location.longitude}"
                    )

                    if (anno != null) {
                        annotations.add(anno)
                    }
                }
            }

            Log.d("Weather", "annotation size: ${annotations.size}")
            map_view.getAnnotationsController()?.add(annotations)
        }
    }

    private fun getResourceBasedOnWeatherType(type: WeatherType): Int {
        return when (type) {
            WeatherType.AQUAPLANE -> R.drawable.flooding
            WeatherType.ICY -> R.drawable.snow
            WeatherType.LOW_VISIBILITY -> R.drawable.frog
            // case we shouldn't be drawing anything
            else -> -1
        }
    }

    private fun dpToPx(dp: Float, context: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics
        ).toInt()
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
                activeRouteId?.let { map_view.getRoutesController()?.remove(it) }
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
        Log.d("onLocationUpdated", "lat: ${vehicleLocation.latitude}, lon: ${vehicleLocation.longitude}")
        if (mapViewInitialized) {
            map_view.getVehicleController()?.setLocation(vehicleLocation)

            // first drawn frame
            if (lastKnownLocation == null) {
                lastKnownLocation = vehicleLocation
                map_view.getCameraController()?.position =
                    Camera.Position.Builder().setLocation(vehicleLocation).build()
            }
        }
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
    }

}