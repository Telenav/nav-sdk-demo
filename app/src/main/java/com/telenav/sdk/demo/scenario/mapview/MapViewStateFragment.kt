/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.mapview

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.controllers.Camera
import com.telenav.map.views.TnMapView
import com.telenav.sdk.demo.main.SecondViewModel
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.FragmentMapViewStateBinding
import kotlinx.android.synthetic.main.fragment_map_view_camera.*
import kotlinx.android.synthetic.main.layout_action_bar.*
import java.util.*


/**
 * This fragment shows how to set camera position in MapView
 * @author zhai.xiang on 2021/1/11
 */
class MapViewStateFragment : Fragment() {

    companion object {
        const val TAG = ""

    }

    private lateinit var viewModel: SecondViewModel
    lateinit var binding: FragmentMapViewStateBinding
    lateinit var mapView: TnMapView

    /**
     * location of New York City
     */
    private val locationA = Location(TAG).apply {
        this.latitude = 40.7478055
        this.longitude = -73.9850480
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = SecondViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_map_view_state, container, false)
        mapView = binding.mapView
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_title.text = getString(R.string.title_activity_map_view_status)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }
        mapViewInit(savedInstanceState)
        setCameraUpdateListener()


    }

    private fun setCameraUpdateListener() {
        mapView.addMapViewListener {
            it.cameraLocation
            val text = String.format(Locale.getDefault(),
                "camera position: [%.4f , %.4f]\nzoom level: %.1f]",
                it.cameraLocation.latitude,
                it.cameraLocation.longitude,
                it.zoomLevel
            )

            activity?.runOnUiThread {
                tv_state?.text = text
            }
        }
    }

    private fun mapViewInit(savedInstanceState: Bundle?) {

        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener = {
                moveCameraToLocation(locationA)
                setLocationAnnotations()
                mapView.mapDiagnosis().addMapViewListener {
                    mapView.mapDiagnosis().mapViewStatus?.let { //viewModel.setUpVehicleDetails(it)
                        binding.tvTitlePositionSet.post(Runnable {
                            val text = String.format(Locale.getDefault(),
                                "Camera latitude : %.4f\nCamera longitude : %.4f\n" +
                                    "Camera heading : %.2f\nCar latitude : %.4f\nCar longitude : %.4f\nZoom level : %s\nIs animating : " +
                                    "%s\nInteraction mode :%s\nRender mode val : %s\nIsAutoZoomAnimationRunning : %s",
                                it.cameraLatitude,
                                it.cameraLongitude,
                                it.cameraHeading,
                                it.carLatitude,
                                it.carLongitude,
                                it.zoomLevel,
                                it.isAnimating,
                                it.interactionMode,
                                it.renderModeval,
                                it.isAutoZoomAnimationRunning
                            )
                            binding.tvTitlePositionSet.text = text
                        })
                    }
                    Log.i(TAG, "onMapFirstFrameDraw ")
                }
            }
        )
        mapView.initialize(mapViewConfig)
    }

    private fun setLocationAnnotations() {
        val factory = mapView.annotationsController().factory()
        val annotation = factory.create(requireContext(),R.drawable.map_pin_green_icon_unfocused,locationA)

        mapView.annotationsController().add(listOf(annotation))
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    /**
     * This function shows how to set location of camera
     */
    private fun moveCameraToLocation(location: Location) {
        mapView.cameraController().position =
            Camera.Position.Builder().setLocation(location).build()
        val factory = mapView.annotationsController().factory()
        val annotation =
            factory.create(requireContext(), R.drawable.map_pin_green_icon_unfocused, locationA)

        mapView.annotationsController().add(listOf(annotation))
    }


}