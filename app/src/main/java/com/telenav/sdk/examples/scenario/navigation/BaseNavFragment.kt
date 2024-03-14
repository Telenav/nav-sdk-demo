/*
 *  Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.navigation

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.viewbinding.ViewBinding
import com.telenav.map.api.Annotation
import com.telenav.map.api.MapView
import com.telenav.map.api.Margins
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.examples.base.BaseMapFragment
import com.telenav.sdk.drivesession.NavigationSession
import com.telenav.sdk.drivesession.listener.NavigationEventListener
import com.telenav.sdk.drivesession.model.BetterRouteProposal
import com.telenav.sdk.drivesession.model.BetterRouteUpdateProgress
import com.telenav.sdk.drivesession.model.DepartureWaypointInfo
import com.telenav.sdk.drivesession.model.JunctionViewInfo
import com.telenav.sdk.drivesession.model.ManeuverInfo
import com.telenav.sdk.drivesession.model.NavigationEvent
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.ContentBasicNavigationBinding
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.ContentLevel
import com.telenav.sdk.map.direction.model.DirectionErrorCode
import com.telenav.sdk.map.direction.model.GeoLocation
import com.telenav.sdk.map.direction.model.RequestMode
import com.telenav.sdk.map.direction.model.Route
import com.telenav.sdk.map.direction.model.RouteRequest
import com.telenav.sdk.map.direction.model.Waypoint
import com.telenav.sdk.map.model.AlongRouteTraffic
import com.telenav.sdk.navigation.model.ChargingStationUnreachableEvent
import com.telenav.sdk.navigation.model.TimedRestrictionEdge
import com.telenav.sdk.uikit.turn.TnTurnListItem
import kotlin.math.max
import kotlin.math.min

/**
 * a basic [Fragment] for navigation
 * @author tang.hui on 2021/1/14
 */
abstract class BaseNavFragment<T : ViewBinding> : BaseMapFragment(), NavigationEventListener {
    private var _binding: T? = null
    protected val binding get() = _binding!!

    var routeIds = mutableListOf<String>()
    var routes = mutableListOf<Route>()
    var navigationSession: NavigationSession? = null

    var highlightedRouteId: String? = null

    val navigationOn = MutableLiveData(false)
    var navigating = false
    var distanceToStop = 0.0

    init {
        driveSession.eventHub?.let {
            it.addNavigationEventListener(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = getViewBinding(inflater, container)
        return binding.root
    }

    protected abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): T
    protected abstract fun getBaseBinding(): ContentBasicNavigationBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
        getBaseBinding().mapView.setOnTouchListener { touchType: TouchType, data: TouchPosition ->
            if (touchType == TouchType.LongClick) {
                getBaseBinding().mapView.routesController().clear()
                onLongClick(data.geoLocation)
            }
        }

        getBaseBinding().mapView.setOnRouteTouchListener { touchType, data, routeID ->
            highlightedRouteId = routeID
            getBaseBinding().mapView.routesController().highlight(routeID)
        }

        getBaseBinding().subViewButton.visibility = View.INVISIBLE
    }

    override fun findMapView(): MapView {
        return getBaseBinding().mapView
    }

    open fun onLongClick(location: Location?) {
        location?.let {
            activity?.runOnUiThread {
                // Set annotation at location
                val context = context
                if (context != null) {
                    val factory = getBaseBinding().mapView.annotationsController().factory()
                    val annotation =
                        factory.create(context, R.drawable.map_pin_green_icon_unfocused, location)
                    annotation.displayText = Annotation.TextDisplayInfo.Centered("Destination")
                    getBaseBinding().mapView.annotationsController().clear()
                    getBaseBinding().mapView.annotationsController()
                        .add(arrayListOf(annotation))
                }
            }

            vehicleLocation?.let {
                requestDirection(it, location)
            }
        }
    }

    override fun onDestroyView() {
        driveSession.eventHub.removeNavigationEventListener(this)
        super.onDestroyView()
        _binding = null
    }

    open fun requestDirection(
        begin: Location,
        end: Location,
        wayPointList: MutableList<Waypoint>? = null
    ) {
        Log.d(
            "MapLogsForTestData",
            "MapLogsForTestData >>>> requestDirection begin: $begin + end $end"
        )

        val request: RouteRequest = RouteRequest.Builder(
            GeoLocation(begin),
            GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
            .routeCount(2)
            .stopPoints(wayPointList)
            .build()
        val task = DirectionClient.Factory.hybridClient()
            .createRoutingTask(request, RequestMode.HYBRID)
        task.runAsync { response ->
            Log.d(
                TAG,
                "MapLogsForTestData >>>> requestDirection task status: ${response.response.status}"
            )
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                routes = response.response.result
                routeIds =
                    getBaseBinding().mapView.routesController().add(routes).toMutableList()
                getBaseBinding().mapView.routesController().highlight(routeIds[0])
                val region = getBaseBinding().mapView.routesController().region(routeIds)
                getBaseBinding().mapView.cameraController()
                    .showRegion(region, Margins.Percentages(0.20, 0.20))
                highlightedRouteId = routeIds[0]
                activity?.runOnUiThread {
                    getBaseBinding().navButton.isEnabled = true
                    getBaseBinding().navButton.setText(R.string.start_navigation)
                }

            } else {
                activity?.runOnUiThread {
                    getBaseBinding().navButton.isEnabled = false
                }
            }
            task.dispose()
        }
    }

    private fun setZoomLevel(level: Float) {
        val newLevel = min(max(1f, level), 17f)
        getBaseBinding().mapView.cameraController().position =
            Camera.Position.Builder().setZoomLevel(newLevel).build()
    }

    private fun setupButtons() {
        getBaseBinding().ivCameraFix.setOnClickListener {
            val newPosition = Camera.Position.Builder().setLocation(vehicleLocation).build()
            getBaseBinding().mapView.cameraController().position = newPosition
            getBaseBinding().ivCameraFix.setImageResource(R.drawable.ic_gps_fixed_24)
            if (navigating)
                getBaseBinding().mapView.cameraController().enableFollowVehicleMode(
                    Camera.FollowVehicleMode.HeadingUp, false
                )
        }

        getBaseBinding().ivZoomIn.setOnClickListener {
            getBaseBinding().mapView.cameraController().position.zoomLevel?.let { currentLevel ->
                setZoomLevel(currentLevel - 1)
            }

        }

        getBaseBinding().ivZoomOut.setOnClickListener {
            getBaseBinding().mapView.cameraController().position.zoomLevel?.let { currentLevel ->
                setZoomLevel(currentLevel + 1)
            }
        }


        getBaseBinding().navButton.setOnClickListener {
            navigating = !navigating
            onNavButtonClick(navigating)
            if (navigating) {
                driveSession.stopNavigation()
                var pickedRouteIndex = 0 // default route first
                if (!highlightedRouteId.isNullOrEmpty()) {
                    routes.forEachIndexed { index, route ->
                        if (route.id == highlightedRouteId) {
                            pickedRouteIndex = index
                        }
                    }
                }
                val pickedRoute = routes[pickedRouteIndex]
                navigationSession =
                    driveSession.startNavigation(pickedRoute, true, getDemonstrateSpeed())
                navigationOn.postValue(true)
                routeIds.forEachIndexed { index, element ->
                    if (index != pickedRouteIndex) {
                        getBaseBinding().mapView.routesController().remove(element)
                    }
                }
                getBaseBinding().mapView.routesController()
                    .updateRouteProgress(pickedRoute.id)
                // enableFollowVehicleMode
                getBaseBinding().mapView.cameraController()
                    .enableFollowVehicleMode(Camera.FollowVehicleMode.HeadingUp, false)

                getBaseBinding().navButton.setText(R.string.stop_navigation)
            } else {
                driveSession.stopNavigation()
                navigationOn.postValue(false)
                getBaseBinding().mapView.annotationsController().clear()
                getBaseBinding().mapView.routesController().clear()
                getBaseBinding().mapView.cameraController().disableFollowVehicle()
                activity?.runOnUiThread {
                    getBaseBinding().navButton.isEnabled = false
                }
                getBaseBinding().navButton.setText(R.string.start_navigation)
                navigating = false
            }
        }

    }

    open fun onNavButtonClick(navigating: Boolean) {
    }

    open fun getDemonstrateSpeed(): Double {
        return 40.0
    }

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        navEvent.travelEstToDestination?.let {
            distanceToStop = it.distanceToStop
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
            activity?.runOnUiThread {
                Toast.makeText(
                    activity,
                    "Arrive at the destination in $distanceToStop meters",
                    Toast.LENGTH_LONG
                ).show()
            }
            getBaseBinding().mapView.annotationsController().clear()
            getBaseBinding().mapView.routesController().clear()
            getBaseBinding().mapView.cameraController().disableFollowVehicle()
            navigationOn.postValue(false)
            activity?.runOnUiThread {
                getBaseBinding().navButton.isEnabled = false
            }
            navigating = false
        }
    }

    override fun onNavigationRouteUpdating(progress: BetterRouteUpdateProgress) {
    }

    override fun onBetterRouteDetected(proposal: BetterRouteProposal) {
    }

    override fun onChargingStationUnreachableEventUpdated(unreachableEvent: ChargingStationUnreachableEvent) {
    }

    protected fun onTurnListItemClicked(tnTurnListItem: TnTurnListItem) {
        getBaseBinding().mapView.cameraController().position =
            Camera.Position.Builder().setZoomLevel(2f).build()
        getBaseBinding().mapView.cameraController().disableFollowVehicle()

        getBaseBinding().mapView.routesController()
            .showTurnArrow(
                highlightedRouteId,
                tnTurnListItem.stepInfo!!.legIndex,
                tnTurnListItem.stepInfo!!.stepIndex
            )

        getBaseBinding().mapView.routesController().moveToTurnArrow(
            highlightedRouteId,
            tnTurnListItem.stepInfo!!.legIndex,
            tnTurnListItem.stepInfo!!.stepIndex
        )
    }

    override fun onTurnByTurnListUpdated(maneuverInfoList: List<ManeuverInfo>) {
    }

    override fun onDepartWaypoint(departureWaypointInfo: DepartureWaypointInfo) {
    }

    override fun onTimedRestrictionEventUpdated(timedRestrictionEdges: List<TimedRestrictionEdge>) {
    }
}