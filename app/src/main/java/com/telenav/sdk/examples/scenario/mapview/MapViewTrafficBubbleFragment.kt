/*
 * Copyright © 2018 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.mapview

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.*
import com.telenav.map.api.Annotation
import com.telenav.map.api.controllers.*
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.sdk.common.model.DayNightMode
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.examples.util.DefaultNavigationFragment
import com.telenav.sdk.drivesession.model.*
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.FragmentMapViewTrafficBubbleBinding
import com.telenav.sdk.map.SDK
import com.telenav.sdk.map.content.model.TrafficLevel
import com.telenav.sdk.map.direction.model.Route
import com.telenav.sdk.map.direction.model.RouteEdge
import com.telenav.sdk.map.model.AlongRouteTraffic
import kotlin.Int
import kotlin.apply
import kotlin.let

/**
 * This page is to show how to display traffic bubble.
 * The traffic bubble will appear when navigation start. Each traffic flow can create one bubble to show
 * the current traffic status. And the traffic bubble will disappear when the vehicle pass the bubble
 */
class MapViewTrafficBubbleFragment : DefaultNavigationFragment() {
    private var _binding: FragmentMapViewTrafficBubbleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewNavViewModel by viewModels()
    private var cameraController: CameraController? = null
    private var vehicleController: VehicleController? = null
    private var annotationController: AnnotationsController? = null
    private var routesController: RoutesController? = null
    private var alongRouteTraffic: AlongRouteTraffic? = null
    private val bubbleList = ArrayList<Annotation>()

    companion object {
        const val LEG_INDEX = "legIndex"
        const val STEP_INDEX = "stepIndex"
        const val EDGE_INDEX = "edgeIndex"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapViewTrafficBubbleBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val mapViewReadyListener = MapViewReadyListener<MapView> {
        activity?.runOnUiThread {
            cameraController = binding.mapView.cameraController()
            annotationController = binding.mapView.annotationsController()
            vehicleController = binding.mapView.vehicleController()
            routesController = binding.mapView.routesController()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.actionBar.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        SDK.getInstance().updateDayNightMode(DayNightMode.DAY)
        binding.actionBar.tvTitle.setText(R.string.title_activity_map_view_traffic_bubble)
        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener = mapViewReadyListener
        )
        binding.mapView.initialize(mapViewConfig)

        binding.trafficBar.setOrientation(false)
        viewModel.driveSession.eventHub.addNavigationEventListener(this)
        initView()
        initObs()
    }

    override fun onDestroyView() {
        viewModel.driveSession.eventHub.removeNavigationEventListener(this)
        super.onDestroyView()
        _binding = null
    }

    private fun initView() {
        binding.btnStartNav.setOnClickListener {
            viewModel.startNavigation(binding.mapView)
            binding.btnStartNav.isEnabled = false
            binding.btnStopNav.isEnabled = true
        }

        binding.btnStopNav.setOnClickListener {
            viewModel.stopNavigation(binding.mapView)
            binding.btnStartNav.isEnabled = false
            binding.btnStopNav.isEnabled = false
            annotationController?.clear()
            updateCurrentPosition(0, 0, 0)
        }

        binding.ivFix.setOnClickListener {
            cameraController?.position =
                Camera.Position.Builder().setLocation(viewModel.currentVehicleLocation.value!!)
                    .build()
        }

        binding.mapView.setOnTouchListener { touchType: TouchType, position: TouchPosition ->
            if (viewModel.isNavigationOn()) {
                return@setOnTouchListener
            }

            if (touchType == TouchType.LongClick) {
                binding.mapView.routesController().clear()
                activity?.runOnUiThread {
                    val context = context
                    if (context != null) {
                        val factory = annotationController!!.factory()
                        val annotation = factory.create(
                            context,
                            R.drawable.map_pin_green_icon_unfocused,
                            position.geoLocation!!
                        )
                        annotation.style = Annotation.Style.ScreenAnnotationFlagNoCulling
                        annotationController?.clear()
                        annotationController?.add(arrayListOf(annotation))
                    }
                }

                viewModel.currentVehicleLocation.value?.let {
                    viewModel.requestDirection(it, position.geoLocation!!) { result ->
                        activity?.runOnUiThread {
                            binding.btnStartNav.isEnabled = result
                        }
                    }
                }
            }
        }
    }

    private fun initObs() {
        viewModel.currentVehicleLocation.observe(viewLifecycleOwner) {
            vehicleController?.setLocation(it)
            // update current state and remove the traffic bubble if the edge has been passed
            it.extras?.let { bundle ->
                val edgeIndex = bundle.getInt("edgeIndex", 0)
                val legIndex = bundle.getInt("legIndex", 0)
                val stepIndex = bundle.getInt("stepIndex", 0)
                updateCurrentPosition(legIndex, stepIndex, edgeIndex)
            }
        }

        viewModel.route.observe(viewLifecycleOwner) { route ->
            routesController?.clear()
            route?.let {
                val ids = routesController!!.add(listOf(it))
                cameraController?.showRegion(
                    routesController!!.region(ids),
                    Margins.Percentages(0.2, 0.2)
                )
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

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        binding.trafficBar.updateTraveledDistance(navEvent.traveledDistance.toFloat())
    }

    override fun onJunctionViewUpdated(junctionViewInfo: JunctionViewInfo) {
    }

    override fun onAlongRouteTrafficUpdated(alongRouteTraffic: AlongRouteTraffic) {
        this.alongRouteTraffic = alongRouteTraffic
        refreshBubble(generatePoiModel(alongRouteTraffic))
        binding.trafficBar.updateTrafficInfo(alongRouteTraffic)
    }

    override fun onNavigationStopReached(stopIndex: Int, stopLocation: Int) {
        if (stopIndex == -1) {
            annotationController?.remove(bubbleList)
            bubbleList.clear()
        }
    }

    override fun onNavigationRouteUpdating(progress: BetterRouteUpdateProgress) {
        progress.newRoute?.let {
            viewModel.route.postValue(it)
            updateCurrentPosition(0, 0, 0)
        }
    }

    private fun generatePoiModel(alongRouteTraffic: AlongRouteTraffic): List<PoiModel> {
        val modelList = ArrayList<PoiModel>()
        val edges = generalEdgeList(viewModel.route.value)
        alongRouteTraffic.alongRouteTrafficFlow?.forEach { flow ->
            val startIndex = edges.indexOfFirst {
                it.legIndex == flow.startLegIndex && it.stepIndex == flow.startStepIndex && it.edgeIndex == flow.startEdgeIndex
            }

            val lastIndex = edges.indexOfLast {
                it.legIndex == flow.endLegIndex && it.stepIndex == flow.endStepIndex && it.edgeIndex == flow.endEdgeIndex
            }

            val middleEdge = edges[(startIndex + lastIndex) / 2]
            val locations = (middleEdge.edge.getEdgeShapePoints() ?: emptyList())
            val poi = PoiModel(
                middleEdge.legIndex, middleEdge.stepIndex, middleEdge.edgeIndex,
                locations[locations.size / 2], flow.congestionLevel
            )
            modelList.add(poi)
        }

        return modelList
    }

    private fun refreshBubble(models: List<PoiModel>) {
        annotationController?.remove(bubbleList)
        bubbleList.clear()
        models.forEach {
            if (it.congestionLevel != -1 && it.latLon != null) {
                val location = Location("demo").apply {
                    this.latitude = it.latLon.lat
                    this.longitude = it.latLon.lon
                }

                val poi = annotationController!!.factory()
                    .create(Annotation.ExplicitStyle.HeavyCongestionBubble, location)
                poi.extraInfo = Bundle().apply {
                    this.putInt(LEG_INDEX, it.legIndex)
                    this.putInt(STEP_INDEX, it.stepIndex)
                    this.putInt(EDGE_INDEX, it.edgeIndex)
                }

                when (it.congestionLevel) {
                    TrafficLevel.CLOSED -> {
                        poi.displayText = Annotation.TextDisplayInfo.Centered("CLOSED")
                    }

                    TrafficLevel.CONGESTED -> {
                        poi.displayText = Annotation.TextDisplayInfo.Centered("CONGESTED")
                    }

                    TrafficLevel.SLOW_SPEED -> {
                        poi.displayText = Annotation.TextDisplayInfo.Centered("SLOW SPEED")
                    }

                    TrafficLevel.QUEUING -> {
                        poi.displayText = Annotation.TextDisplayInfo.Centered("QUEUING")
                    }

                    TrafficLevel.FREE_FLOW -> {
                        return@forEach
                    }

                    TrafficLevel.HEAVY -> {
                        poi.displayText = Annotation.TextDisplayInfo.Centered("HEAVY")
                    }

                    else -> {
                        poi.displayText = Annotation.TextDisplayInfo.Centered("UNKNOWN")
                    }
                }

                poi.displayText?.textSize = 10.0f
                bubbleList.add(poi)
            }
        }

        annotationController?.add(bubbleList)
    }

    /**
     * update the current position and remove the passed annotations
     */
    private fun updateCurrentPosition(legIndex: Int, stepIndex: Int, edgeIndex: Int) {
        if (alongRouteTraffic == null) {
            return
        }

        val removeList = bubbleList.filter { annotation ->
            val leg = annotation.extraInfo?.getInt(LEG_INDEX) ?: -1
            val step = annotation.extraInfo?.getInt(STEP_INDEX) ?: -1
            val edge = annotation.extraInfo?.getInt(EDGE_INDEX) ?: -1
            if (legIndex > leg) {
                return@filter true
            } else if (legIndex < leg) {
                return@filter false
            }

            if (stepIndex > step) {
                return@filter true
            } else if (stepIndex < step) {
                return@filter false
            }

            if (edgeIndex > edge) {
                return@filter true
            } else if (edgeIndex < edge) {
                return@filter false
            }

            return@filter false
        }

        annotationController?.remove(removeList)
        bubbleList.removeAll(removeList)
    }

    private fun generalEdgeList(route: Route?): List<EdgeModel> {
        val result = ArrayList<EdgeModel>()
        route?.routeLegList?.forEachIndexed { leg, routeLeg ->
            routeLeg.routeStepList?.forEachIndexed { step, routeStep ->
                routeStep.routeEdgeList?.forEachIndexed { edge, routeEdge ->
                    result.add(EdgeModel(leg, step, edge, routeEdge))
                }
            }
        }

        return result
    }

    private data class EdgeModel(
        val legIndex: Int,
        val stepIndex: Int,
        val edgeIndex: Int,
        val edge: RouteEdge
    )

    private data class PoiModel constructor(
        val legIndex: Int,
        val stepIndex: Int,
        val edgeIndex: Int,
        val latLon: LatLon?,
        var congestionLevel: Int = -1
    )
}