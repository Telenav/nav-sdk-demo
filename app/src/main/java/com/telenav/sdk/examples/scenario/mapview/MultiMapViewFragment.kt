package com.telenav.sdk.examples.scenario.mapview

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.Annotation
import com.telenav.map.api.ClusterMapView
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.controllers.AnnotationsController
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.models.ClusterMapViewParams
import com.telenav.map.views.TnClusterMapView
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.examples.util.DefaultNavigationFragment
import com.telenav.sdk.examples.util.StyleConstants
import com.telenav.sdk.drivesession.DriveSession
import com.telenav.sdk.drivesession.NavigationSession
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.drivesession.model.*
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.FragmentMapViewMultiMapBinding
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import com.telenav.sdk.map.model.AlongRouteTraffic

class MultiMapViewFragment : DefaultNavigationFragment(), PositionEventListener {
    private var _binding: FragmentMapViewMultiMapBinding? = null
    private val binding get() = _binding!!

    private val TAG = "MultiMapViewFragment"
    private val driveSession: DriveSession = DriveSession.Factory.createInstance()
    private var navigationSession: NavigationSession? = null
    private var clusterMapView: ClusterMapView? = null
    private var clusterMapView2: ClusterMapView? = null

    /**
     * location of San Francisco
     */
    private val locationA = Location("MOCK").apply {
        this.latitude = 37.767937
        this.longitude = -122.429250
    }

    /**
     * location of Los Angeles
     */
    private val locationB = Location("MOCK").apply {
        this.latitude = 34.0772327
        this.longitude = -118.2584544
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapViewMultiMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.actionBar.tvTitle.text = getString(R.string.title_activity_map_view_multi_map)
        binding.actionBar.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        initMaintMapView()
        initClusterMapView()
        initClusterMapView2()
        setOnClickListener()
        setMapUpdateListener()
    }

    private fun initMaintMapView() {
        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            dpi = binding.mainMapView.defaultDpi,
            defaultLocation = locationA,
            readyListener = {
                it.getVehicleController()!!.setIcon(R.drawable.cvp)
                moveCVPToLocation(locationA)
            },
            createCvp = false,
            defaultZoomLevel = 17F
        )
        binding.mainMapView.initialize(mapViewConfig)
    }

    private fun initClusterMapView() {
        clusterMapView = TnClusterMapView().apply {
            val clusterMapViewParams = ClusterMapViewParams(
                context = requireContext(),
                surface = binding.clusterMapView.holder.surface,
                width = requireContext().resources.getDimensionPixelSize(R.dimen.dimens_300dp),
                height = requireContext().resources.getDimensionPixelSize(R.dimen.rectangle_map_view_height),
                density = 80f,
                defaultLocation = LatLon(lon = locationA.longitude, lat = locationA.latitude),
                createCVP = false,
                zoomLevel = 17F,
                readyListener = {
                    it?.let {
                        it.themeController()?.loadStyleSheet(StyleConstants.CLUSTER_STYLE_PATH)
                        it.featuresController().traffic().setEnabled()
                        it.featuresController().compass().setEnabled()
                        it.cameraController()
                            ?.enableFollowVehicleMode(Camera.FollowVehicleMode.NorthUp, false)
                        it.cameraController()?.renderMode = Camera.RenderMode.M3D

                        it.vehicleController()?.setLocation(locationA)
                        it.vehicleController()?.setIcon(R.drawable.cvp)
                        it.cameraController()?.position =
                            Camera.Position.Builder().setLocation(locationA).setZoomLevel(5F)
                                .build()
                    }
                }
            )
            initialize(clusterMapViewParams)
        }
        binding.clusterMapView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                clusterMapView!!.onSurfaceCreated()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                clusterMapView!!.onSurfaceChanged(width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                clusterMapView!!.onSurfaceDestroyed()
            }

        })
    }

    private fun initClusterMapView2() {
        clusterMapView2 = TnClusterMapView().apply {
            val clusterMapViewParams = ClusterMapViewParams(
                context = requireContext(),
                surface = binding.clusterMapView2.holder.surface,
                width = requireContext().resources.getDimensionPixelSize(R.dimen.rectangle_map_view_width),
                height = requireContext().resources.getDimensionPixelSize(R.dimen.dimens_300dp),
                density = 80f,
                defaultLocation = LatLon(lon = locationA.longitude, lat = locationA.latitude),
                createCVP = false,
                zoomLevel = 17F,
                readyListener = {
                    it?.let {
                        //stress test load style sheet
                        repeat(50) {
                            clusterMapView2?.themeController()
                                ?.loadStyleSheet(StyleConstants.LOCAL_ABSOLUTE_WARM_STYLE_PATH)
                        }
                        it.featuresController().traffic().setEnabled()
                        it.featuresController().compass().setEnabled()
                        it.cameraController()
                            ?.enableFollowVehicleMode(Camera.FollowVehicleMode.NorthUp, false)
                        it.cameraController()?.renderMode = Camera.RenderMode.M3D

                        it.vehicleController()?.setLocation(locationA)
                        it.vehicleController()?.setIcon(R.drawable.cvp)
                        it.cameraController()?.position =
                            Camera.Position.Builder().setLocation(locationA).setZoomLevel(5F)
                                .build()
                    }
                }
            )
            initialize(clusterMapViewParams)
        }
        binding.clusterMapView2.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                clusterMapView2!!.onSurfaceCreated()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                clusterMapView2!!.onSurfaceChanged(width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                clusterMapView2!!.onSurfaceDestroyed()
            }

        })
    }

    private fun setOnClickListener() {
        binding.btnVehicleLocation.text = String.format(
            "Move to San Francisco\n(%.5f,%.5f)",
            locationA.latitude,
            locationA.longitude
        )
        binding.btnVehicleLocation.setOnClickListener {
            moveCVPToLocation(locationA)
        }

        binding.btnStartNavigation.text = String.format(
            "Navigate to Los Angeles\n(%.5f,%.5f)",
            locationB.latitude,
            locationB.longitude
        )
        binding.btnStartNavigation.setOnClickListener { navigateToLocation(locationA, locationB) }
        binding.btnStopNavigation.setOnClickListener { stopNavigation() }

        binding.btnZoomIn.setOnClickListener { setZoomLevelBy(-1f) }
        binding.btnZoomOut.setOnClickListener { setZoomLevelBy(1f) }

        binding.fpsSlider.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, value: Int, fromUser: Boolean) {
                binding.fpsText.text = "FPS(" + value + ")"
                clusterMapView?.setFPS(value)
            }

            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {}
        })
        clusterMapView?.setFPS(binding.fpsSlider.progress)

        binding.btnPause.setOnClickListener { clusterMapView?.onPause() }
        binding.btnResume.setOnClickListener { clusterMapView?.onResume() }
        binding.btnDestroy.setOnClickListener {
            clusterMapView?.onDestroy()
            clusterMapView = null
        }
        binding.btnCreate.setOnClickListener {
            if (clusterMapView == null) {
                initClusterMapView()
                clusterMapView!!.onSurfaceCreated()
                clusterMapView!!.onSurfaceChanged(
                    requireContext().resources.getDimensionPixelSize(R.dimen.rectangle_map_view_width),
                    requireContext().resources.getDimensionPixelSize(R.dimen.dimens_300dp)
                )
            }
        }
        binding.btnAddMainDestination.setOnClickListener {
            setDestinationAnnotation(
                binding.mainMapView.annotationsController(),
                binding.mainMapView.annotationsController().factory()
                    .create(requireContext(), R.drawable.map_pin_green_icon_unfocused, locationB)
            )
        }
        binding.btnAddClusterDestination.setOnClickListener {
            clusterMapView?.let { cluster ->
                setDestinationAnnotation(
                    cluster.annotationsController()!!,
                    cluster.annotationsController()!!.factory().create(
                        requireContext(),
                        R.drawable.map_pin_orange_icon_unfocused,
                        locationB
                    )
                )
            }
        }
        binding.btnClearAllDestination.setOnClickListener {
            binding.mainMapView.annotationsController().clear()
            clusterMapView?.annotationsController()?.clear()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mainMapView.onResume()
        clusterMapView?.onResume()
        clusterMapView2?.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mainMapView.onPause()
        clusterMapView?.onPause()
        clusterMapView2?.onPause()
    }

    override fun onDestroy() {
        clusterMapView?.onDestroy()
        clusterMapView = null
        clusterMapView2?.onDestroy()
        clusterMapView2 = null
        super.onDestroy()
    }

    private fun setDestinationAnnotation(
        annotationsController: AnnotationsController,
        destination: Annotation
    ) {
        destination.style = Annotation.Style.ScreenAnnotationPopup
        annotationsController.add(listOf(destination))
    }

    private fun moveCVPToLocation(location: Location) {
        binding.mainMapView.getCameraController()?.position =
            Camera.Position.Builder().setLocation(location).setZoomLevel(5F).build()
        binding.mainMapView.getVehicleController()?.setLocation(location)
    }

    private fun navigateToLocation(begin: Location, end: Location) {
        driveSession?.stopNavigation()
        val request: RouteRequest = RouteRequest.Builder(
            GeoLocation(begin),
            GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
            .routeCount(2)
            .build()
        val task = DirectionClient.Factory.hybridClient()
            .createRoutingTask(request, RequestMode.CLOUD_ONLY)
        task.runAsync { response ->
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                val routes = response.response.result
                val routeIds = binding.mainMapView.routesController().add(routes)
                binding.mainMapView.routesController().highlight(routeIds[0])
                navigationSession = driveSession.startNavigation(routes[0], true, 40.0)
                driveSession.eventHub?.let {
                    it.addNavigationEventListener(this)
                    it.addPositionEventListener(this)
                }
                binding.mainMapView.routesController().updateRouteProgress(routeIds[0])
                binding.mainMapView.cameraController()
                    .enableFollowVehicleMode(Camera.FollowVehicleMode.HeadingUp, true)

                clusterMapView?.routesController()?.add(routes)
                clusterMapView?.routesController()?.highlight(routeIds[0])
                clusterMapView?.routesController()?.updateRouteProgress(routeIds[0])
                clusterMapView?.cameraController()
                    ?.enableFollowVehicleMode(Camera.FollowVehicleMode.HeadingUp, true)

            }
            task.dispose()
        }
    }

    private fun stopNavigation() {
        driveSession?.stopNavigation()
        driveSession.eventHub?.let {
            it.removeNavigationEventListener(this)
            it.removePositionEventListener(this)
        }
        binding.mainMapView.run {
            annotationsController().clear()
            routesController().clear()
            cameraController().disableFollowVehicle()
        }
    }

    private fun setZoomLevelBy(value: Float) {
        val zoom = clusterMapView?.cameraController()?.position?.zoomLevel ?: 0f
        clusterMapView?.cameraController()?.position =
            Camera.Position.Builder().setZoomLevel(zoom + value).build()
        clusterMapView2?.cameraController()?.position =
            Camera.Position.Builder().setZoomLevel(zoom + value).build()
    }

    private fun setMapUpdateListener() {
        binding.mainMapView.addMapViewListener {
            it.cameraLocation
            val text = String.format(
                "Main Map\ncamera position: [%.4f , %.4f]\nvehicle position: [%.4f , %.4f]\nzoom level: %.1f\nrange horizontal: %.3f",
                it.cameraLocation.latitude,
                it.cameraLocation.longitude,
                it.carLocation.latitude,
                it.carLocation.longitude,
                it.zoomLevel,
                it.rangeHorizontal
            )
            activity?.runOnUiThread {
                _binding?.tvPrint?.text = text
            }
        }
        clusterMapView?.addMapViewListener {
            it.cameraLocation
            val text = String.format(
                "Cluster Map Cluster Style\ncamera position: [%.4f , %.4f]\nvehicle position: [%.4f , %.4f]\nzoom level: %.1f\nrange horizontal: %.3f",
                it.cameraLocation.latitude,
                it.cameraLocation.longitude,
                it.carLocation.latitude,
                it.carLocation.longitude,
                it.zoomLevel,
                it.rangeHorizontal
            )
            activity?.runOnUiThread {
                _binding?.tvPrint2?.text = text
            }
        }

        clusterMapView2?.addMapViewListener {
            it.cameraLocation
            val text = String.format(
                "Cluster Map2 HUD WARM Style\ncamera position: [%.4f , %.4f]\nvehicle position: [%.4f , %.4f]\nzoom level: %.1f\nrange horizontal: %.3f",
                it.cameraLocation.latitude,
                it.cameraLocation.longitude,
                it.carLocation.latitude,
                it.carLocation.longitude,
                it.zoomLevel,
                it.rangeHorizontal
            )
            activity?.runOnUiThread {
                _binding?.tvPrint3?.text = text
            }
        }
    }

    override fun onLocationUpdated(vehicleLocation: Location, positionInfo: PositionInfo) {
        binding.mainMapView.vehicleController().setLocation(vehicleLocation)
        clusterMapView?.vehicleController()?.setLocation(vehicleLocation)
        clusterMapView2?.vehicleController()?.setLocation(vehicleLocation)

    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
        TaLog.d(TAG, "onCandidateRoadDetected")
    }

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        TaLog.d(TAG, "onNavigationEventUpdated")
    }

    override fun onJunctionViewUpdated(junctionViewInfo: JunctionViewInfo) {
        TaLog.d(TAG, "onJunctionViewUpdated")
    }

    override fun onAlongRouteTrafficUpdated(alongRouteTraffic: AlongRouteTraffic) {
        TaLog.d(TAG, "onAlongRouteTrafficUpdated")
    }

    override fun onNavigationStopReached(stopIndex: Int, stopLocation: Int) {
        TaLog.d(TAG, "onNavigationStopReached: stopIndex=$stopIndex, stopLocation:$stopLocation")
        if (stopIndex == -1) {  //-1 means reach destination
            binding.mainMapView.annotationsController().clear()
            binding.mainMapView.routesController().clear()
            binding.mainMapView.cameraController().disableFollowVehicle()
        }
    }

    override fun onBetterRouteDetected(proposal: BetterRouteProposal) {
    }

}