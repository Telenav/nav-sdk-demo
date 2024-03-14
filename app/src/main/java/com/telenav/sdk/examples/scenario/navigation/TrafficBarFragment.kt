package com.telenav.sdk.examples.scenario.navigation

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.telenav.map.api.MapView
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.MapViewReadyListener
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.examples.provider.DemoLocationProvider
import com.telenav.sdk.examples.util.DefaultNavigationFragment
import com.telenav.sdk.drivesession.DriveSession
import com.telenav.sdk.drivesession.NavigationSession
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.drivesession.model.BetterRouteProposal
import com.telenav.sdk.drivesession.model.JunctionViewInfo
import com.telenav.sdk.drivesession.model.NavigationEvent
import com.telenav.sdk.drivesession.model.PositionInfo
import com.telenav.sdk.drivesession.model.RoadCalibrator
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.FragmentTrafficBarBinding
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.ContentLevel
import com.telenav.sdk.map.direction.model.DirectionErrorCode
import com.telenav.sdk.map.direction.model.GeoLocation
import com.telenav.sdk.map.direction.model.RequestMode
import com.telenav.sdk.map.direction.model.Route
import com.telenav.sdk.map.direction.model.RouteRequest
import com.telenav.sdk.map.model.AlongRouteTraffic

/**
 * @author zhai.xiang on 2021/2/25
 */
class TrafficBarFragment : DefaultNavigationFragment(), PositionEventListener {
    private var _binding: FragmentTrafficBarBinding? = null
    private val binding get() = _binding!!

    private val driveSession: DriveSession = DriveSession.Factory.createInstance()
    private var route: Route? = null
    private var navigationSession: NavigationSession? = null
    private var currentVehicleLocation: Location? = null
    private lateinit var locationProvider: DemoLocationProvider
    private var navigationOn = false

    init {
        driveSession.eventHub?.addPositionEventListener(this)
        driveSession.eventHub?.addNavigationEventListener(this)
    }

    override fun onDestroyView() {
        driveSession.eventHub?.removePositionEventListener(this)
        driveSession.eventHub?.removeNavigationEventListener(this)
        driveSession.dispose()
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrafficBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationProvider = DemoLocationProvider.Factory.createProvider(
            requireContext(),
            DemoLocationProvider.ProviderType.SIMULATION
        )
        driveSession.injectLocationProvider(locationProvider)
        mapViewInit(savedInstanceState)
    }

    private val mapViewReadyListener = MapViewReadyListener<MapView> {
        binding.mapView.getFeaturesController()?.traffic()?.setEnabled()
    }

    /**
     * the initialize function must be called after SDK is initialized
     */
    private fun mapViewInit(savedInstanceState: Bundle?) {
        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener = mapViewReadyListener
        )
        binding.mapView.initialize(mapViewConfig)

        binding.mapView.setOnTouchListener { touchType: TouchType, data: TouchPosition ->
            if (navigationOn) {
                return@setOnTouchListener
            }
            if (touchType == TouchType.LongClick) {
                binding.mapView.routesController().clear()
                activity?.runOnUiThread {
                    val context = context
                    if (context != null) {
                        val factory = binding.mapView.annotationsController().factory()
                        val annotation = factory.create(
                            context,
                            R.drawable.map_pin_green_icon_unfocused,
                            data.geoLocation!!
                        )
                        binding.mapView.annotationsController().clear()
                        binding.mapView.annotationsController().add(arrayListOf(annotation))
                    }
                }

                currentVehicleLocation?.let {
                    requestDirection(it, data.geoLocation!!) { result ->
                        activity?.runOnUiThread {
                            binding.navButton.isEnabled = result
                        }
                    }
                }

            }
        }

        binding.navButton.setOnClickListener {
            navigationOn = !navigationOn
            if (navigationOn) {
                startNavigation(binding.mapView, driveSession)
                binding.navButton.setText(R.string.stop_navigation)
            } else {
                stopNavigation(binding.mapView)
                binding.navButton.setText(R.string.start_navigation)
            }
        }

        binding.ivFix.setOnClickListener {
            currentVehicleLocation?.let {
                binding.mapView.cameraController().position =
                    Camera.Position.Builder().setLocation(it).build()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    private fun requestDirection(begin: Location, end: Location, result: (Boolean) -> Unit) {
        val request: RouteRequest = RouteRequest.Builder(
            GeoLocation(begin),
            GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
            .routeCount(1)
            .build()
        val task = DirectionClient.Factory.hybridClient()
            .createRoutingTask(request, RequestMode.CLOUD_ONLY)
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

    private fun showRegion() {
        val routeIds = binding.mapView.routesController().add(listOf(route))
        val region = binding.mapView.routesController().region(routeIds)
        binding.mapView.cameraController().showRegion(region)
    }

    private fun startNavigation(map: MapView, driveSession: DriveSession) {
        driveSession?.stopNavigation()
        route?.let {
            val routeIds = map.routesController().add(listOf(route))
            map.routesController().highlight(routeIds[0])
            navigationSession = driveSession.startNavigation(it, true, 40.0)
//            trafficBar.setTotalLength(it.distance.toFloat())
            binding.trafficBar.initTrafficInfo(it)
            map.routesController().updateRouteProgress(routeIds[0])
        }
    }

    private fun stopNavigation(map: MapView) {
        driveSession?.stopNavigation()
        map.routesController().clear()
    }

    override fun onLocationUpdated(vehicleLocation: Location, positionInfo: PositionInfo) {
        _binding?.mapView?.getVehicleController()?.setLocation(vehicleLocation)
        currentVehicleLocation = vehicleLocation
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
    }

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        binding.trafficBar.updateTraveledDistance(navEvent.traveledDistance.toFloat())
    }

    override fun onJunctionViewUpdated(junctionViewInfo: JunctionViewInfo) {
    }

    override fun onAlongRouteTrafficUpdated(alongRouteTraffic: AlongRouteTraffic) {
        binding.trafficBar.updateTrafficInfo(alongRouteTraffic)
    }

    override fun onNavigationStopReached(stopIndex: Int, stopLocation: Int) {
    }

    override fun onBetterRouteDetected(proposal: BetterRouteProposal) {
    }


}