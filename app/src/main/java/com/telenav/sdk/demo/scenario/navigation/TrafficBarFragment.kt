package com.telenav.sdk.demo.scenario.navigation

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.telenav.map.api.MapView
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.demo.provider.DemoLocationProvider
import com.telenav.sdk.drivesession.DriveSession
import com.telenav.sdk.drivesession.NavigationSession
import com.telenav.sdk.drivesession.listener.NavigationEventListener
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.drivesession.model.*
import com.telenav.sdk.drivesession.model.drg.RouteUpdateContext
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import kotlinx.android.synthetic.main.content_basic_navigation.*
import kotlinx.android.synthetic.main.fragment_traffic_bar.*
import kotlinx.android.synthetic.main.fragment_traffic_bar.mapView
import kotlinx.android.synthetic.main.fragment_traffic_bar.navButton
import kotlinx.android.synthetic.main.layout_action_bar.*

/**
 * @author zhai.xiang on 2021/2/25
 */
class TrafficBarFragment : Fragment(), PositionEventListener, NavigationEventListener {
    private val driveSession: DriveSession = DriveSession.Factory.createInstance()
    private var route: Route? = null
    private var navigationSession: NavigationSession? = null
    private var currentVehicleLocation: Location? = null
    private lateinit var locationProvider : DemoLocationProvider
    private var navigationOn = false

    init {
        driveSession.eventHub?.addPositionEventListener(this)
        driveSession.eventHub?.addNavigationEventListener(this)
    }

    override fun onDestroyView() {
        driveSession.eventHub?.removePositionEventListener(this)
        driveSession.dispose()
        locationProvider.stop()
        super.onDestroyView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_traffic_bar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationProvider = DemoLocationProvider.Factory.createProvider(requireContext(),DemoLocationProvider.ProviderType.SIMULATION)
        locationProvider.start()
        driveSession.injectLocationProvider(locationProvider)
        mapViewInit(savedInstanceState)
    }

    /**
     * the initialize function must be called after SDK is initialized
     */
    private fun mapViewInit(savedInstanceState: Bundle?) {
        mapView.initialize(savedInstanceState) {
            mapView.featuresController().traffic().setEnabled()
        }

        mapView.setOnTouchListener { touchType: TouchType, data: TouchPosition ->
            if (navigationOn){
                return@setOnTouchListener
            }
            if (touchType == TouchType.LongClick) {
                mapView.routesController().clear()
                activity?.runOnUiThread {
                    val context = context
                    if (context != null) {
                        val factory = mapView.annotationsController().factory()
                        val annotation = factory.create(context, R.drawable.map_pin_green_icon_unfocused, data.geoLocation!!)
                        mapView.annotationsController().clear()
                        mapView.annotationsController().add(arrayListOf(annotation))
                    }
                }

                currentVehicleLocation?.let {
                    requestDirection(it, data.geoLocation!!) { result ->
                        activity?.runOnUiThread {
                            navButton.isEnabled = result
                        }
                    }
                }

            }
        }

        navButton.setOnClickListener {
            navigationOn = !navigationOn
            if (navigationOn) {
                startNavigation(mapView, driveSession)
                navButton.setText(R.string.stop_navigation)
            } else {
                stopNavigation(mapView)
                navButton.setText(R.string.start_navigation)
            }
        }

        ivFix.setOnClickListener {
            currentVehicleLocation?.let{
                mapView.cameraController().position = Camera.Position.Builder().setLocation(it).build()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    private fun requestDirection(begin: Location, end: Location, result: (Boolean) -> Unit) {
        val request: RouteRequest = RouteRequest.Builder(
                GeoLocation(begin),
                GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
                .routeCount(1)
                .build()
        val task = DirectionClient.Factory.hybridClient().createRoutingTask(request, RequestMode.CLOUD_ONLY)
        task.runAsync { response ->
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                route = response.response.result[0]!!
                activity?.runOnUiThread {
                    showRegion()
                    result(true)
                }

            } else {
                activity?.runOnUiThread {
                    result(false)
                }
            }
            task.dispose()
        }
    }

    private fun showRegion(){
        val routeIds = mapView.routesController().add(listOf(route))
        val region = mapView.routesController().region(routeIds)
        mapView.cameraController().showRegion(region)
    }

    private fun startNavigation(map: MapView, driveSession : DriveSession){
        navigationSession?.stopNavigation()
        route?.let{
            val routeIds = map.routesController().add(listOf(route))
            map.routesController().highlight(routeIds[0])
            navigationSession = driveSession.startNavigation(it, true, 40.0)
//            trafficBar.setTotalLength(it.distance.toFloat())
            trafficBar.initTrafficInfo(it)
            map.routesController().updateRouteProgress(routeIds[0])
        }
    }

    private fun stopNavigation(map: MapView){
        navigationSession?.stopNavigation()
        map.routesController().clear()
    }

    override fun onLocationUpdated(vehicleLocation: Location) {
        mapView.vehicleController().setLocation(vehicleLocation)
        currentVehicleLocation = vehicleLocation
    }

    override fun onStreetUpdated(curStreetInfo: StreetInfo, drivingOffRoad: Boolean) {
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
    }

    override fun onMMFeedbackUpdated(feedback: MMFeedbackInfo) {
    }

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        trafficBar.updateTraveledDistance(navEvent.traveledDistance.toFloat())
    }

    override fun onJunctionViewUpdated(junctionViewInfo: JunctionViewInfo) {
    }

    override fun onAlongRouteTrafficUpdated(alongRouteTraffic: AlongRouteTraffic) {
        trafficBar.updateTrafficInfo(alongRouteTraffic)
    }

    override fun onNavigationStopReached(stopIndex: Int, stopLocation: Int) {
    }

    override fun onNavigationRouteUpdated(route: Route, info: RouteUpdateContext?) {
    }

    override fun onBetterRouteDetected(status: NavigationEventListener.BetterRouteDetectionStatus, betterRouteCandidate: BetterRouteCandidate?) {
    }

}