package com.telenav.sdk.demo.scenario.mapview

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.Annotation
import com.telenav.map.api.ClusterMapView
import com.telenav.map.api.controllers.AnnotationsController
import com.telenav.map.api.controllers.Camera
import com.telenav.map.views.TnClusterMapView
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.drivesession.DriveSession
import com.telenav.sdk.drivesession.NavigationSession
import com.telenav.sdk.drivesession.listener.NavigationEventListener
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.drivesession.model.*
import com.telenav.sdk.drivesession.model.drg.BetterRouteContext
import com.telenav.sdk.drivesession.model.drg.RouteUpdateContext
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import kotlinx.android.synthetic.main.fragment_map_view_multi_map.*
import kotlinx.android.synthetic.main.layout_action_bar.*
import java.util.*

class MultiMapViewFragment : Fragment(), PositionEventListener, NavigationEventListener {
    private val TAG = "MultiMapViewFragment"
    private val driveSession: DriveSession = DriveSession.Factory.createInstance()
    private var navigationSession: NavigationSession? = null
    private var clusterMapView: ClusterMapView? = null

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
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map_view_multi_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_title.text = getString(R.string.title_activity_map_view_multi_map)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }

        initMaintMapView(savedInstanceState)
        initClusterMapView()
        setOnClickListener()
        setMapUpdateListener()
    }

    private fun initMaintMapView(savedInstanceState: Bundle?) {
        main_map_view.initialize(savedInstanceState) {
            it.featuresController().traffic().setEnabled()
            it.featuresController().compass().setEnabled()
            it.vehicleController().setIcon(R.drawable.cvp)
            activity?.runOnUiThread {
                moveCVPToLocation(locationA)
            }
        }
    }

    private fun initClusterMapView() {
        clusterMapView = TnClusterMapView()
        clusterMapView!!.initialize(
            requireContext(),
            cluster_map_view.holder.surface,
            requireContext().resources.getDimensionPixelSize(R.dimen.rectangle_map_view_width),
            requireContext().resources.getDimensionPixelSize(R.dimen.rectangle_map_view_height),
            80f
        ) {
            it.featuresController().traffic().setEnabled()
            it.featuresController().compass().setEnabled()
            it.vehicleController().setIcon(R.drawable.cvp)
            activity?.runOnUiThread {
                it.cameraController()?.position =
                    Camera.Position.Builder().setLocation(locationA).build()
                it.vehicleController()?.setLocation(locationA)
                it.cameraController()
                    .enableFollowVehicleMode(Camera.FollowVehicleMode.NorthUp, false)
                it.cameraController().position = Camera.Position.Builder().setZoomLevel(0f).build()
                it.cameraController().renderMode = Camera.RenderMode.M3D
            }
        }
        cluster_map_view.holder.addCallback(object : SurfaceHolder.Callback {
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

    private fun setOnClickListener() {
        btn_vehicle_location.text = String.format(
            "Move to San Francisco\n(%.5f,%.5f)",
            locationA.latitude,
            locationA.longitude
        )
        btn_vehicle_location.setOnClickListener {
            moveCVPToLocation(locationA)
        }

        btn_start_navigation.text = String.format(
            "Navigate to Los Angeles\n(%.5f,%.5f)",
            locationB.latitude,
            locationB.longitude
        )
        btn_start_navigation.setOnClickListener { navigateToLocation(locationA, locationB) }
        btn_stop_navigation.setOnClickListener { stopNavigation() }

        btn_zoom_in.setOnClickListener { setZoomLevelBy(-1f) }
        btn_zoom_out.setOnClickListener { setZoomLevelBy(1f) }

        fps_slider.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, value: Int, fromUser: Boolean) {
                fps_text.setText("FPS(" + value + ")")
                clusterMapView?.setFPS(value)
            }

            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {}
        })
        clusterMapView?.setFPS(fps_slider.progress)

        btn_pause.setOnClickListener { clusterMapView?.onPause() }
        btn_resume.setOnClickListener { clusterMapView?.onResume() }
        btn_destroy.setOnClickListener {
            clusterMapView?.onDestroy()
            clusterMapView = null
        }
        btn_create.setOnClickListener {
            if (clusterMapView == null) {
                initClusterMapView()
                clusterMapView!!.onSurfaceCreated()
                clusterMapView!!.onSurfaceChanged(
                    requireContext().resources.getDimensionPixelSize(R.dimen.rectangle_map_view_width),
                    requireContext().resources.getDimensionPixelSize(R.dimen.rectangle_map_view_height)
                )
            }
        }
        btn_add_main_destination.setOnClickListener {
            setDestinationAnnotation(main_map_view.annotationsController(), main_map_view.annotationsController().factory().create(requireContext(), R.drawable.map_pin_green_icon_unfocused, locationB))
        }
        btn_add_cluster_destination.setOnClickListener {
            clusterMapView?.let { cluster ->
                setDestinationAnnotation(cluster.annotationsController(), cluster.annotationsController().factory().create(requireContext(), R.drawable.map_pin_orange_icon_unfocused, locationB))
            }
        }
        btn_clear_all_destination.setOnClickListener {
            main_map_view.annotationsController().clear()
            clusterMapView?.annotationsController()?.clear()
        }
    }

    override fun onResume() {
        super.onResume()
        main_map_view.onResume()
        clusterMapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        main_map_view.onPause()
        clusterMapView?.onPause()
    }

    override fun onDestroy() {
        clusterMapView?.onDestroy()
        clusterMapView = null
        super.onDestroy()
    }

    private fun setDestinationAnnotation(annotationsController: AnnotationsController, destination: Annotation) {
        destination.style = Annotation.Style.ScreenAnnotationPopup
        annotationsController.add(listOf(destination))
    }

    private fun moveCVPToLocation(location: Location) {
        main_map_view.cameraController()?.position =
            Camera.Position.Builder().setLocation(location).build()
        main_map_view.vehicleController()?.setLocation(location)
    }

    private fun navigateToLocation(begin: Location, end: Location) {
        navigationSession?.stopNavigation()
        val request: RouteRequest = RouteRequest.Builder(
            GeoLocation(begin),
            GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
            .routeCount(2)
            .startTime(Calendar.getInstance().timeInMillis / 1000)
            .build()
        val task = DirectionClient.Factory.hybridClient()
            .createRoutingTask(request, RequestMode.CLOUD_ONLY)
        task.runAsync { response ->
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                val routes = response.response.result
                val routeIds = main_map_view.routesController().add(routes)
                main_map_view.routesController().highlight(routeIds[0])
                navigationSession = driveSession.startNavigation(routes[0], true, 40.0)
                driveSession.eventHub?.let {
                    it.addNavigationEventListener(this)
                    it.addPositionEventListener(this)
                }
                main_map_view.routesController().updateRouteProgress(routeIds[0])
                main_map_view.cameraController()
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
        navigationSession?.stopNavigation()
        driveSession.eventHub?.let {
            it.removeNavigationEventListener(this)
            it.removePositionEventListener(this)
        }
        main_map_view.run {
            annotationsController().clear()
            routesController().clear()
            cameraController().disableFollowVehicle()
        }
    }

    private fun setZoomLevelBy(value: Float) {
        val zoom = clusterMapView?.cameraController()?.position?.zoomLevel ?: 0f
        clusterMapView?.cameraController()?.position =
            Camera.Position.Builder().setZoomLevel(zoom + value).build()
    }

    private fun setMapUpdateListener() {
        main_map_view.addMapViewListener {
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
                tv_print?.text = text
            }
        }
        clusterMapView?.addMapViewListener {
            it.cameraLocation
            val text = String.format(
                "Cluster Map\ncamera position: [%.4f , %.4f]\nvehicle position: [%.4f , %.4f]\nzoom level: %.1f\nrange horizontal: %.3f",
                it.cameraLocation.latitude,
                it.cameraLocation.longitude,
                it.carLocation.latitude,
                it.carLocation.longitude,
                it.zoomLevel,
                it.rangeHorizontal
            )
            activity?.runOnUiThread {
                tv_print2?.text = text
            }
        }
    }

    override fun onLocationUpdated(vehicleLocation: Location) {
        main_map_view.vehicleController().setLocation(vehicleLocation)
    }

    override fun onStreetUpdated(curStreetInfo: StreetInfo, drivingOffRoad: Boolean) {
        TaLog.d(TAG, "onStreetUpdated")
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
        TaLog.d(TAG, "onCandidateRoadDetected")
    }

    override fun onMMFeedbackUpdated(feedback: MMFeedbackInfo) {
        TaLog.d(TAG, "onMMFeedbackUpdated")
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

    override fun onLaneGuidanceUpdated(laneGuidanceEvent: LaneGuidanceEvent) {
        TaLog.d(TAG, "onLaneGuidanceUpdated")
    }

    override fun onNavigationStopReached(stopIndex: Int, stopLocation: Int) {
        TaLog.d(TAG, "onNavigationStopReached: stopIndex=$stopIndex, stopLocation:$stopLocation")
        if (stopIndex == -1) {  //-1 means reach destination
            main_map_view.annotationsController().clear()
            main_map_view.routesController().clear()
            main_map_view.cameraController().disableFollowVehicle()
        }
    }

    override fun onNavigationRouteUpdated(route: Route, betterRouteContext: BetterRouteContext?) {
        route.dispose()
    }

    override fun onNavigationRouteUpdated(route: Route, routeUpdateContext: RouteUpdateContext?) {
    }

    override fun onBetterRouteInfoUpdated(betterRouteInfo: BetterRouteInfo) {
        betterRouteInfo.betterRouteCandidate?.accept(false)
    }

    override fun onBetterRouteDetected(
        status: NavigationEventListener.BetterRouteDetectionStatus,
        betterRouteCandidate: BetterRouteCandidate?
    ) {
        betterRouteCandidate?.accept(false)
    }
}