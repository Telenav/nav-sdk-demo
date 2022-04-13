package com.telenav.sdk.demo.scenario.mapview

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.ClusterMapView
import com.telenav.map.api.Margins
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.controllers.CameraController
import com.telenav.map.api.controllers.RoutesController
import com.telenav.map.api.controllers.VehicleController
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.map.views.TnClusterMapView
import com.telenav.sdk.common.logging.TaLog
import com.telenav.map.api.Annotation
import com.telenav.sdk.drivesession.listener.NavigationEventListener
import com.telenav.sdk.drivesession.model.*
import com.telenav.sdk.drivesession.model.drg.BetterRouteContext
import com.telenav.sdk.drivesession.model.drg.RouteUpdateContext
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.direction.model.Route
import kotlinx.android.synthetic.main.content_basic_navigation.*
import kotlinx.android.synthetic.main.fragment_map_view_multi_map.*
import kotlinx.android.synthetic.main.layout_action_bar.*
import kotlinx.android.synthetic.main.layout_content_multi_map.*
import kotlinx.android.synthetic.main.layout_operation_multi_view.*
import kotlin.math.max
import kotlin.math.min

class MultiMapViewFragment : Fragment(), NavigationEventListener {
    private val TAG = "MultiMapViewFragment"
    private lateinit var viewModel: MapViewNavViewModel
    private var clusterMapView: ClusterMapView? = null
    private var mainCameraController: CameraController? = null
    private var mainVehicleController: VehicleController? = null
    private var mainRoutesController: RoutesController? = null
    private var clusterCameraController: CameraController? = null
    private var clusterRoutesController: RoutesController? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map_view_multi_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application))
            .get(MapViewNavViewModel::class.java)
        viewModel.driveSession.eventHub.addNavigationEventListener(this)
        viewModel.currentVehicleLocation.observe(viewLifecycleOwner) {
            moveCVPToLocation(it)
        }

        viewModel.route.observe(viewLifecycleOwner) { route ->
            updateRoute(route)
        }

        viewModel.currentRoute.observe(viewLifecycleOwner) { route ->
            clusterRoutesController?.let {
                it.clear()
                if (route != null) {
                    val routeIds = it.add(listOf(route))
                    it.highlight(routeIds[0])
                    it.updateRouteProgress(routeIds[0])
                }
            }
        }

        tv_title.text = getString(R.string.title_activity_map_view_multi_map)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }

        btn_show_menu.setOnClickListener {
            drawer_layout.open()
        }

        initMaintMapView(savedInstanceState)
        setOnClickListener()
        setMapUpdateListener()
    }

    private fun initMaintMapView(savedInstanceState: Bundle?) {
        map_view.initialize(savedInstanceState) {
            it.featuresController().traffic().setEnabled()
            it.featuresController().compass().setEnabled()
            it.featuresController().buildings().setEnabled()
            it.featuresController().landmarks().setEnabled()
            it.featuresController().scaleBar().setEnabled()
            it.vehicleController().setIcon(R.drawable.cvp)
            mainCameraController = it.cameraController()
            mainVehicleController = it.vehicleController()
            mainRoutesController = it.routesController()
        }

        map_view.setOnTouchListener { touchType: TouchType, position: TouchPosition ->
            if (viewModel.isNavigationOn()) {
                return@setOnTouchListener
            }

            if (touchType == TouchType.LongClick) {
                map_view.routesController().clear()
                activity?.runOnUiThread {
                    val context = context
                    if (context != null) {
                        val factory = map_view.annotationsController().factory()
                        val annotation = factory.create(context, R.drawable.map_pin_green_icon_unfocused, position.geoLocation!!)
                        annotation.style = Annotation.Style.ScreenAnnotationFlagNoCulling
                        map_view.annotationsController().clear()
                        map_view.annotationsController().add(arrayListOf(annotation))
                    }
                }

                viewModel.currentVehicleLocation.value?.let {
                    viewModel.requestDirection(it, position.geoLocation!!) { result ->
                        activity?.runOnUiThread {
                            navButton.isEnabled = result
                        }
                    }
                }
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
            map_view.defaultDpi * 1.2f
        ) {
            it.vehicleController().setIcon(R.drawable.cvp)
            activity?.runOnUiThread {
                clusterCameraController = it.cameraController()
                clusterRoutesController = it.routesController()
                it.featuresController().scaleBar().setEnabled()
                it.layoutController().setVerticalOffset(-0.5)
                it.cameraController()
                    .enableFollowVehicleMode(Camera.FollowVehicleMode.HeadingUp, false)
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
        iv_camera_fix.setOnClickListener {
            val newPosition = Camera.Position.Builder().setLocation(viewModel.currentVehicleLocation.value!!).build()
            map_view.cameraController().position = newPosition
            iv_camera_fix.setImageResource(R.drawable.ic_gps_fixed_24)
        }

        iv_zoom_in.setOnClickListener {
            val currentLevel = map_view.cameraController().position!!.zoomLevel
            setZoomLevel(currentLevel - 1)
        }

        iv_zoom_out.setOnClickListener {
            val currentLevel = map_view.cameraController().position!!.zoomLevel
            setZoomLevel(currentLevel + 1)
        }

        navButton.setOnClickListener {
            navButton.isEnabled = false
            subViewButton.isEnabled = true
            viewModel.startNavigation(map_view)
        }

        subViewButton.setOnClickListener {
            navButton.isEnabled = true
            subViewButton.isEnabled = false
            viewModel.stopNavigation(map_view)
            updateRoute(null)
        }

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
            clusterCameraController?.disableFollowVehicle()
            clusterCameraController = null
            clusterRoutesController = null
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

    }

    private fun setZoomLevel(level: Float) {
        val newLevel = min(max(1f, level), 17f)
        map_view.cameraController().position = Camera.Position.Builder().setZoomLevel(newLevel).build()
    }

    private fun updateRoute(route: Route?) {
        mainRoutesController?.clear()
        route?.let {
            val ids = map_view.routesController().add(listOf(it))
            mainCameraController?.showRegion(mainRoutesController!!.region(ids!!),
                Margins.Percentages(0.2, 0.2))
        }
    }

    override fun onResume() {
        super.onResume()
        map_view.onResume()
        clusterMapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        map_view.onPause()
        clusterMapView?.onPause()
    }

    override fun onDestroy() {
        clusterMapView?.onDestroy()
        clusterMapView = null
        super.onDestroy()
    }

    private fun moveCVPToLocation(location: Location) {
        mainVehicleController?.setLocation(location)
    }

    private fun setZoomLevelBy(value: Float) {
        val zoom = clusterCameraController?.position?.zoomLevel ?: 0f
        clusterCameraController?.position =
            Camera.Position.Builder().setZoomLevel(zoom + value).build()
    }

    private fun setMapUpdateListener() {
        map_view.addMapViewListener {
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
                tv_tip?.text = text
            }
        }
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
            map_view.cameraController().disableFollowVehicle()
        }
    }

    override fun onNavigationRouteUpdated(route: Route, betterRouteContext: BetterRouteContext?) {
        viewModel.route.postValue(route)
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