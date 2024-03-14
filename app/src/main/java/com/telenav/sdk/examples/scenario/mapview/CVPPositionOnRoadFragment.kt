/*
 * Copyright © 2024 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */
package com.telenav.sdk.examples.scenario.mapview

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.Annotation
import com.telenav.map.api.MapView
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.MapViewReadyListener
import com.telenav.map.api.controllers.*
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.map.api.touch.TouchedAnnotation
import com.telenav.map.api.touch.listeners.TouchListener
import com.telenav.map.internal.AnnotationLayerIndex
import com.telenav.map.views.TnMapView
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.examples.scenario.mapview.CVPPositionOnRoadViewModel.Companion.createRect
import com.telenav.sdk.examples.scenario.mapview.CVPPositionOnRoadViewModel.Companion.createRegionForRoutesInfo
import com.telenav.sdk.examples.scenario.mapview.CVPPositionOnRoadViewModel.Companion.createRouteRequest
import com.telenav.sdk.examples.scenario.mapview.CVPPositionOnRoadViewModel.Companion.createRoutingTask
import com.telenav.sdk.examples.util.DefaultNavigationFragment
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.drivesession.model.*
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.CVPPositionOnRoadFragmentBinding
import com.telenav.sdk.map.model.AlongRouteTraffic
import kotlinx.coroutines.*


/**
 * This fragment shows how you can get and show the route from CVP to the destination
 * point on the map
 *
 * @author Mykola Ivantsov - (p)
 */
class CVPPositionOnRoadFragment : DefaultNavigationFragment(), PositionEventListener {
    private var _binding: CVPPositionOnRoadFragmentBinding? = null
    private val binding get() = _binding!!
    companion object {
        fun newInstance() = CVPPositionOnRoadFragment()
        private const val TAG = "CVPPositionOnRoadFragment"
        private const val REACH_DESTINATION = -1 //-1 means reach destination
    }

    private val annotations = ArrayList<Annotation>()

    private val viewModel: CVPPositionOnRoadViewModel by viewModels()
    private val jobList = mutableListOf<Job>()
    private val backListener = View.OnClickListener {
        findNavController().navigateUp()
    }
    private val success: (Boolean) -> Unit = {
        runInMain {
            setVisible(binding.btnRemainingWay, View.VISIBLE)
        }
    }
    private val onLoad: () -> Unit = {
        runInMain {
            setVisible(binding.btnRemainingWay, View.GONE)
        }
    }
    private val navigateListener = View.OnClickListener { _: View ->
        viewModel.navigateToLocation(
            getRoutesController(),
            getCameraController(),
            this,
            this,
            viewModel.startLocation,
            viewModel.stopLocation,
            createRoutingTask,
            createRouteRequest,
            success,
            onLoad
        )

    }
    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    private val remainingWayListener = View.OnClickListener {
        disableFollowVehicle(getCameraController())
        setRenderMode(getCameraController(), Camera.RenderMode.M2D)
        setBearing(binding.mapView, CVPPositionOnRoadViewModel.BEAR, createCameraPosition)
        showActiveRouteInRegion(getRoutesController(), getCameraController(),annotations)
    }

    private val getAnnotationsController: () -> AnnotationsController = {
        binding.mapView.annotationsController()
    }

    private val getCameraController: () -> CameraController = {
        binding.mapView.cameraController()
    }

    private val getRoutesController: () -> RoutesController = {
        binding.mapView.routesController()
    }

    private val getVehicleController: () -> VehicleController = {
        printDebugLog("CALL getVehicleController()")
        binding.mapView.vehicleController()
    }

    private val createCameraPosition: (Float) -> Camera.Position = { bear ->
        Camera.Position.Builder().setBearing(bear).build()
    }
    private val gridAlignedListener = { _: CompoundButton, isChecked: Boolean ->
        viewModel.gridAligned = isChecked
    }
    private val showFullRouteOverviewListener = { _: CompoundButton, isChecked: Boolean ->
        viewModel.showFullRouteOverview = isChecked
    }
    private val includeCVPListener = { _: CompoundButton, isChecked: Boolean ->
        viewModel.includeCVP = isChecked
    }
    private val nearestLegModeListener = { _: CompoundButton, isChecked: Boolean ->
        viewModel.nearestLegMode = isChecked
    }

    private val mapListener: MapViewReadyListener<MapView> = MapViewReadyListener<MapView> {
        binding.mapView.apply {
            featuresController().traffic().setEnabled()
            featuresController().compass().setEnabled()
            vehicleController().setIcon(R.drawable.cvp)
        }

        runInMain {
            moveCVPToLocation(viewModel.startLocation, getCameraController, getVehicleController)
        }
    }


    /**
     * add annotation.
     */
    private fun addAnnotation(position: TouchPosition) {
        val annotation = createAnnotationWithResource(position)
        annotation.style = Annotation.Style.ScreenAnnotationPopup
        annotation.type = Annotation.Type.Screen2D
        binding.mapView.getAnnotationsController()?.add(listOf(annotation))
        annotations.add(annotation)
    }

    private fun createAnnotationWithResource(position: TouchPosition): Annotation {
        val factory = binding.mapView.annotationsController().factory()
        return factory.create(
            requireContext(),
            R.drawable.map_pin_green_icon_unfocused,
            position.geoLocation!!
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CVPPositionOnRoadFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.include.tvTitle.text = getString(R.string.title_activity_map_view_cvp_position_on_road)

        setOnCheckedChangeListener(binding.switchGridAligned, gridAlignedListener)
        setOnCheckedChangeListener(binding.switchShowFullRouteOverview, showFullRouteOverviewListener)
        setOnCheckedChangeListener(binding.switchIncludeCvp, includeCVPListener)
        setOnCheckedChangeListener(binding.switchNearestLegMode, nearestLegModeListener)
        mapViewInit(savedInstanceState, mapListener)

        setOnClickListener(binding.include.ivBack, backListener)
        setOnClickListener(binding.btnNavigate, navigateListener)
        setOnClickListener(binding.btnRemainingWay, remainingWayListener)
        binding.mapView.setOnAnnotationTouchListener(onAnnotationTouchListener)

        binding.mapView.setOnTouchListener(TouchListener { touchType, position ->
            if (touchType == TouchType.LongClick) {
                addAnnotation(position)
            }
        })
    }

    private fun setBearing(
        mapView: TnMapView,
        bear: Float,
        createCameraPosition: (Float) -> Camera.Position
    ) {
        createCameraPosition(bear).let { cameraPosition ->
            mapView.cameraController().position = cameraPosition
        }
    }

    private fun setOnClickListener(view: View, onClickListener: View.OnClickListener) {
        view.setOnClickListener(onClickListener)
    }

    private fun setOnCheckedChangeListener(
        view: SwitchCompat,
        listener: CompoundButton.OnCheckedChangeListener
    ) {
        view.setOnCheckedChangeListener(listener)
    }

    private fun setVisible(view: View, visibility: Int) {
        view.visibility = visibility
    }

    private fun setRenderMode(cameraController: CameraController, renderMode: Camera.RenderMode) {
        cameraController.renderMode = renderMode
    }

    // the initialize function must be called after SDK is initialized
    private fun mapViewInit(
        savedInstanceState: Bundle?,
        mapViewReadyListener: MapViewReadyListener<MapView>,
        tnMapView: TnMapView = this.binding.mapView
    ) {
        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener =  mapViewReadyListener
        )
        tnMapView.initialize(mapViewConfig)
    }

    private fun moveCVPToLocation(
        location: Location,
        cameraController: () -> CameraController,
        vehicleController: () -> VehicleController
    ) {
        cameraController().position = Camera.Position.Builder().setLocation(location).build()
        vehicleController().setLocation(location)
    }

    private val onAnnotationTouchListener: (TouchType, TouchPosition, List<TouchedAnnotation>) -> Unit =
        { touchType, position, touchedAnnotations ->
            printDebugLog("touchType: $touchType, data: $position, touchedAnnotations: $touchedAnnotations")
            touchedAnnotations.forEach { touchedAnnotation ->
                if (touchedAnnotation.annotation.layer.rawValue == AnnotationLayerIndex.VEHICLE_LAYER) {
                    val job = runInMain {
                        showToast("clicked CVP!")
                    }
                    jobList.add(job)
                    printDebugLog("annotation_layer_rawValue = ${touchedAnnotation.annotation.layer.rawValue}")
                }
            }
        }

    private fun showToast(msg: String) {
        Toast.makeText(
            requireContext(),
            msg,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun runInMain(run: () -> Unit): Job {

        val job = mainScope.launch {
            run()
        }
        jobList.add(job)
        return job
    }

    private fun disableFollowVehicle(cameraController: CameraController) {
        cameraController.disableFollowVehicle()
    }

    private fun showActiveRouteInRegion(
        routesController: RoutesController,
        cameraController: CameraController,
        annotations: List<Annotation>?
    ) {
        val regionForRoutesInfo = createRegionForRoutesInfo(
            listOf(viewModel.mapActiveRouteId),
            createRect(
                CVPPositionOnRoadViewModel.X,
                CVPPositionOnRoadViewModel.Y,
                binding.mapRect.width,
                binding.mapRect.height
            ),
            annotations,
            viewModel.gridAligned,
            viewModel.showFullRouteOverview,
            viewModel.includeCVP,
            viewModel.nearestLegMode
        )
        viewModel.showActiveRouteInRegion(
            routesController,
            cameraController,
            annotations,
            regionForRoutesInfo
        )
    }

    private fun printDebugLog(msg: String) {
        TaLog.d(TAG, "$msg | Thread name: ${Thread.currentThread().name}")
    }

    override fun onLocationUpdated(vehicleLocation: Location, positionInfo: PositionInfo) {
        printDebugLog("CALL onLocationUpdated()")
        getVehicleController().setLocation(vehicleLocation)
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
        printDebugLog("onCandidateRoadDetected")
    }

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        printDebugLog("onNavigationEventUpdated")
    }

    override fun onJunctionViewUpdated(junctionViewInfo: JunctionViewInfo) {
        printDebugLog("onJunctionViewUpdated")
    }

    override fun onAlongRouteTrafficUpdated(alongRouteTraffic: AlongRouteTraffic) {
        printDebugLog("onAlongRouteTrafficUpdated")
    }

    override fun onNavigationStopReached(stopIndex: Int, stopLocation: Int) {
        printDebugLog("onNavigationStopReached: stopIndex=$stopIndex, stopLocation:$stopLocation")
        if (stopIndex == REACH_DESTINATION) {
            getAnnotationsController().clear()
            getRoutesController().clear()
            getCameraController().disableFollowVehicle()
            setVisible(binding.btnRemainingWay, View.GONE)
            viewModel.killNavigationSession()
        }
    }

    override fun onBetterRouteDetected(proposal: BetterRouteProposal) {
    }

    override fun onResume() {
        viewModel.addPositionEventListener(this)
        viewModel.addNavigationEventListener(this)
        binding.mapView.onResume()
        super.onResume()
    }

    override fun onPause() {
        CoroutineScope(Dispatchers.Unconfined).launch {
            jobList.forEach {
                it.cancelAndJoin()
            }
        }
        viewModel.removePositionEventListener(this)
        viewModel.removeNavigationEventListener(this)
        binding.mapView.onPause()
        super.onPause()
    }
}