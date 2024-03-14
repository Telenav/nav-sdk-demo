/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.mapview

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.MapView
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.MapViewReadyListener
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.diagnosis.RenderMode
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.examples.provider.DemoLocationProvider
import com.telenav.sdk.examples.provider.SimulationLocationProvider
import com.telenav.sdk.drivesession.DriveSession
import com.telenav.sdk.drivesession.NavigationSession
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.drivesession.model.PositionInfo
import com.telenav.sdk.drivesession.model.RoadCalibrator
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.FragmentMapViewTuneModeBinding
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import java.util.*

/**
 * This fragment shows how to change the map mode
 * @author zhai.xiang on 2021/1/18
 */
class MapViewTuneModeFragment : Fragment(), PositionEventListener {

    private val startLocation = Location("MapView").apply {
        this.latitude = 37.398762
        this.longitude = -121.977216
        this.bearing = 45.0f
    }

    private val stopLocation = Location("MapView").apply {
        this.latitude = 37.40835858
        this.longitude = -121.967860455
    }

    private val driveSession: DriveSession = DriveSession.Factory.createInstance()
    private var route: Route? = null
    private var navigationSession: NavigationSession? = null
    private var currentVerticalOffset = 0.0
    private var currentVehicleLocation: Location? = null
    private lateinit var locationProvider: DemoLocationProvider

    init {
        driveSession.eventHub?.addPositionEventListener(this)
    }

    private var _binding: FragmentMapViewTuneModeBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        driveSession.eventHub?.removePositionEventListener(this)
        driveSession.dispose()
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapViewTuneModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationProvider = DemoLocationProvider.Factory.createProvider(
            requireContext(),
            DemoLocationProvider.ProviderType.SIMULATION
        )
        driveSession.injectLocationProvider(locationProvider)
        (locationProvider as SimulationLocationProvider).setLocation(startLocation)
        binding.actionBar.tvTitle.text = getString(R.string.title_activity_map_view_tune_mode)
        binding.actionBar.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.includeContent.btnShowMenu.setOnClickListener {
            binding.drawerLayoutMapViewTune.open()
        }
        mapViewInit(savedInstanceState)
        setupDrawerOperations()
        requestDirection(startLocation, stopLocation) {
            binding.includeOperation.navButton.isEnabled = it
        }
    }

    override fun onLocationUpdated(vehicleLocation: Location, positionInfo: PositionInfo) {
        binding.includeContent.mapView.getVehicleController()?.setLocation(vehicleLocation)
        currentVehicleLocation = vehicleLocation
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
    }

    private val mapViewReadyListener = MapViewReadyListener<MapView> {
        activity?.runOnUiThread {
            binding.includeContent.mapView.cameraController().position =
                Camera.Position.Builder().setLocation(startLocation).build()
        }
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
        binding.includeContent.mapView.initialize(mapViewConfig)

        setCameraUpdateListener()
    }

    private fun setupDrawerOperations() {
        binding.includeOperation.scFollowVehicle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.includeOperation.rgFollowModel.visibility = View.VISIBLE
                enableFollowVehicle()
                binding.includeOperation.tvTitleEnableFollowVehicle.text = "Follow Vehicle On"
            } else {
                binding.includeOperation.rgFollowModel.visibility = View.GONE
                binding.includeOperation.tvTitleEnableFollowVehicle.text = "Follow Vehicle Off"
                disableFollowVehicle()
            }
        }

        binding.includeOperation.rgFollowModel.setOnCheckedChangeListener { _, _ ->
            enableFollowVehicle()
        }

        binding.includeOperation.btnMoveToVehiclePosition.setOnClickListener {
            moveToVehiclePosition()
        }

        var navigating = false
        binding.includeOperation.navButton.setOnClickListener {
            navigating = !navigating
            if (navigating) {
                startNavigation(binding.includeContent.mapView, driveSession)
                binding.includeOperation.navButton.setText(R.string.stop_navigation)
            } else {
                stopNavigation(binding.includeContent.mapView)
                binding.includeOperation.navButton.setText(R.string.start_navigation)
            }
        }

        binding.includeOperation.btnOffsetTop.setOnClickListener {
            setVerticalOffset(-0.1)
        }

        binding.includeOperation.btnOffsetDown.setOnClickListener {
            setVerticalOffset(0.1)
        }

        binding.includeOperation.btnShowFollowMode.setOnClickListener {
            showFollowMode()
        }

        binding.includeOperation.btnShowRegion.setOnClickListener {
            showRegion()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.includeContent.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.includeContent.mapView.onPause()
    }

    /**
     * This function shows how to enable follow vehicle with mode
     */
    private fun enableFollowVehicle() {
        val model = getSelectedFollowMode()
        val useAutoZoom = getUseAutoZoom()
        binding.includeContent.mapView.cameraController().enableFollowVehicleMode(model, useAutoZoom)
    }

    /**
     * This function shows how to disable follow vehicle.
     */
    private fun disableFollowVehicle() {
        binding.includeContent.mapView.cameraController().disableFollowVehicle()
    }

    private fun getSelectedFollowMode(): Camera.FollowVehicleMode =
        when (binding.includeOperation.rgFollowModel.checkedRadioButtonId) {
            R.id.rb_heading_up_3D -> Camera.FollowVehicleMode.HeadingUp
            R.id.rb_north_up -> Camera.FollowVehicleMode.NorthUp
            else -> Camera.FollowVehicleMode.Static
        }

    private fun getUseAutoZoom(): Boolean {
        return binding.includeOperation.scUseAutoZoom.isChecked
    }

    /**
     *T his function shows how to move camera to the vehicle.
     */
    private fun moveToVehiclePosition() {
        currentVehicleLocation?.let {
            binding.includeContent.mapView.cameraController().position = Camera.Position.Builder().setLocation(it).build()
        }
    }

    /**
     * This function shows how to set vertical offset of map view.
     */
    private fun setVerticalOffset(value: Double) {
        currentVerticalOffset += value
        currentVerticalOffset = currentVerticalOffset.coerceAtMost(1.0).coerceAtLeast(-1.0)
        binding.includeContent.mapView.layoutController().setVerticalOffset(currentVerticalOffset)
    }

    /**
     * This function shows how to get follow mode.
     */
    private fun showFollowMode() {
        activity?.runOnUiThread {
            val status = binding.includeContent.mapView.mapDiagnosis().mapViewStatus
            val text = when (status?.renderModeval) {
                RenderMode.RenderingMode_Invalid -> "Invalid"
                RenderMode.RenderingMode_3D -> "3D"
                RenderMode.RenderingMode_3DNorthUp -> "3D North Up"
                RenderMode.RenderingMode_3DHeadingUp -> "3D Heading Up"
                RenderMode.RenderingMode_2D -> "2D"
                RenderMode.RenderingMode_2DNorthUp -> "2D North Up"
                RenderMode.RenderingMode_2DHeadingUp -> "2D Heading Up"
                else -> ""
            }
            Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * This function shows how to add a listener to update camera state
     */
    private fun setCameraUpdateListener() {
        binding.includeContent.mapView.addMapViewListener {
            it.cameraLocation
            val text = String.format(
                Locale.getDefault(),
                "camera position: [%.4f , %.4f]\nzoom level: %.1f\nrange horizontal: %.3f\nvertical offset %.1f",
                it.cameraLocation.latitude,
                it.cameraLocation.longitude,
                it.zoomLevel,
                it.rangeHorizontal,
                currentVerticalOffset,
            )
            activity?.runOnUiThread {
                _binding?.includeContent?.tvState?.text = text
            }
        }
    }

    /**
     * This function shows how to show region of routes.
     */
    private fun showRegion() {
        val routeIds = binding.includeContent.mapView.routesController().add(listOf(route))
        val region = binding.includeContent.mapView.routesController().region(routeIds)
        binding.includeContent.mapView.cameraController().showRegion(region)
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

    private fun startNavigation(map: MapView, driveSession: DriveSession) {
        driveSession?.stopNavigation()
        val routeIds = map.routesController().add(listOf(route))
        map.routesController().highlight(routeIds[0])
        if (route != null) {
            navigationSession = driveSession.startNavigation(route!!, true, 40.0)
        }
    }

    private fun stopNavigation(map: MapView) {
        driveSession?.stopNavigation()
        map.routesController().clear()
    }
}