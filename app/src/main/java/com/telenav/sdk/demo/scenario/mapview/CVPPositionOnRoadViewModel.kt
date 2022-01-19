package com.telenav.sdk.examples.scenario.mapview

import android.graphics.Rect
import android.location.Location
import androidx.lifecycle.ViewModel
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.controllers.CameraController
import com.telenav.map.api.controllers.RoutesController
import com.telenav.map.api.models.RegionForRoutesInfo
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.drivesession.DriveSession
import com.telenav.sdk.drivesession.NavigationSession
import com.telenav.sdk.drivesession.listener.NavigationEventListener
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.map.Task
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import java.util.*

class CVPPositionOnRoadViewModel : ViewModel() {

    private var navigationSession: NavigationSession? = null
    lateinit var mapActiveRouteId: String
        private set

    private val rect: Rect by lazy {
        Rect(X, Y, WIDTH, HEIGHT)
    }

    val driveSession: DriveSession = DriveSession.Factory.createInstance()

    val startLocation by lazy {
        createLocation(37.353396, -121.99414)
    }
    val stopLocation by lazy {
        createLocation(37.351183, -121.970336)
    }

    val add: (Int) -> (Int) -> Int = { a -> { b -> a + b } }

    val navigateToLocation: (
        routesController: RoutesController,
        cameraController: CameraController,
        navigationEventListener: NavigationEventListener,
        positionEventListener: PositionEventListener,
        begin: Location,
        end: Location,
        task: (RouteRequest, RequestMode) -> Task<RouteResponse>,
        routeRequest: (Location, Location) -> RouteRequest,
        success: (Boolean) -> Unit,
        onLoad: () -> Unit
    ) -> Boolean = { routesController,
                     cameraController,
                     navigationEventListener,
                     positionEventListener,
                     begin,
                     end,
                     task,
                     routeRequest,
                     success,
                     onLoad ->
        try {
            onLoad()
            with(task(routeRequest(begin, end), RequestMode.CLOUD_ONLY)) {
                navigationSession?.stopNavigation()
                runAsync { response ->
                    if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                        val routes = response.response.result
                        navigationSession = driveSession.startNavigation(
                            routes.first(),
                            DEMONSTRATE_MODE,
                            SPEED
                        )
                        val routeIds = routesController.add(routes)
                        mapActiveRouteId = routeIds.first()
                        routesController.highlight(routeIds.first())

                        driveSession.eventHub?.let { eventHub ->
                            eventHub.addNavigationEventListener(navigationEventListener)
                            eventHub.addPositionEventListener(positionEventListener)
                        }
                        routesController.updateRouteProgress(routeIds.first())
                        cameraController
                            .enableFollowVehicleMode(Camera.FollowVehicleMode.HeadingUp, true)

                    }
                    dispose()
                    success(true)
                }
            }
        } catch (ex: Throwable) {
            printErrorLog(ex)
            false
        }
    }

    private fun printErrorLog(ex: Throwable) {
        TaLog.e(TAG, ex.message, ex)
    }

    fun showActiveRouteInRegion(
        routesController: RoutesController,
        cameraController: CameraController,
        regionForRoutesInfo: RegionForRoutesInfo = RegionForRoutesInfo(
            listOf(mapActiveRouteId),
            rect,
            GRID_ALIGNED,
            SHOW_FULL_ROUTE_OVERVIEW,
            INCLUDE_CVP
        ),
        location: Location? = null
    ) {
        if (mapActiveRouteId.isNotEmpty()) {
            val region = routesController.region(listOf(mapActiveRouteId))

            location?.let {
                region.northLatitude = it.latitude.coerceAtLeast(region.northLatitude);
                region.southLatitude = it.latitude.coerceAtMost(region.southLatitude);
                region.westLongitude = it.longitude.coerceAtMost(region.westLongitude);
                region.eastLongitude = it.longitude.coerceAtLeast(region.eastLongitude);
            }
            region?.let {
                cameraController.showRegionForRoutes(regionForRoutesInfo)
            }
        }
    }

    fun killNavigationSession() {
        navigationSession = null
    }

    companion object {
        const val X = 0
        const val Y = 0
        const val BEAR = 0.0F
        private const val WIDTH = 2141
        private const val HEIGHT = 1180
        private const val SPEED = 100.0
        private const val DEMONSTRATE_MODE = true
        private const val ROUTE_COUNT = 2
        const val GRID_ALIGNED = true
        const val SHOW_FULL_ROUTE_OVERVIEW = false
        const val INCLUDE_CVP = true
        private const val TAG = "CVPPositionOnRoadViewModel"

        fun createRegionForRoutesInfo(
            routes: List<String>,
            rect: Rect,
            gridAligned: Boolean,
            showFullRouteOverview: Boolean,
            includeCVP: Boolean
        ): RegionForRoutesInfo {
            return RegionForRoutesInfo(
                routes,
                rect,
                gridAligned,
                showFullRouteOverview,
                includeCVP
            )
        }

        fun createLocation(latitude: Double, longitude: Double): Location {
            return Location("MOCK").apply {
                this.latitude = latitude
                this.longitude = longitude
            }
        }

        fun createRect(left: Int, top: Int, right: Int, bottom: Int): Rect {
            return Rect(left, top, right, bottom)
        }

        val createRouteRequest: (Location, Location) -> RouteRequest = { begin, end ->
            RouteRequest.Builder(
                GeoLocation(begin),
                GeoLocation(LatLon(end.latitude, end.longitude))
            ).contentLevel(ContentLevel.FULL)
                .routeCount(ROUTE_COUNT)
                .startTime(Calendar.getInstance().timeInMillis / 1000)
                .build()
        }
        val createRoutingTask: (RouteRequest, RequestMode) -> Task<RouteResponse> =
            { routeRequest, requestMode ->
                DirectionClient.Factory
                    .hybridClient()
                    .createRoutingTask(
                        routeRequest,
                        requestMode
                    )
            }
    }
}
