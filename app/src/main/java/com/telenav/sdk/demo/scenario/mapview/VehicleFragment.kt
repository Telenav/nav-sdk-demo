/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.mapview

import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.Annotation
import com.telenav.map.api.controllers.AnnotationsController
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.factories.AnnotationFactory
import com.telenav.sdk.demo.R
import kotlinx.android.synthetic.main.fragment_map_view_vehicle.*
import kotlinx.android.synthetic.main.layout_action_bar.*


/**
 * This fragment shows how to set Vehicle in MapView
 * @author shashank reddy on 2021/27/11
 */
class VehicleFragment : Fragment() {

    companion object {
        const val TAG = "VehicleFragment"
    }

    lateinit var factory: AnnotationFactory
    lateinit var annotation: Annotation
    lateinit var annotationsController: AnnotationsController
    private val location = Location(MapViewCameraFragment.TAG).apply {
        this.latitude = 40.7478055
        this.longitude = -73.9850480
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_view_vehicle, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_title.text = getString(R.string.title_activity_map_view_vehicle)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }
        mapViewInit(savedInstanceState)
        setOnClickListener()
        setLocationAnnotations()
        moveCameraToLocation(location)
    }

    private fun mapViewInit(savedInstanceState: Bundle?) {
        mapView.initialize(savedInstanceState, null)
        annotationsController = mapView.annotationsController()
        factory = annotationsController.factory()
    }

    private fun setOnClickListener() {
        btn_set_vehicle_icon.setOnClickListener { setMarker() }
        btn_set_vehicle_icon_bitmap.setOnClickListener { setMarkerBitmap() }
        btn_reset_vehicle_icon.setOnClickListener { removeMarker() }
        btn_locate_map_center.setOnClickListener { moveCameraToLocation(location) }
    }

    /**
     * This function shows how to remove marker
     */
    private fun removeMarker() {
        annotationsController.clear()
        annotation =
            factory.create(requireContext(), R.drawable.map_pin_green_icon_unfocused, location)
        annotationsController.add(listOf(annotation))
    }

    /**
     * This function shows how to set marker annotations
     */
    private fun setMarker() {
        annotationsController.clear()
        annotation = factory.create(requireContext(), R.drawable.map_pin_red_icon_unfocused, location)
        annotationsController.add(listOf(annotation))
    }

    /**
     * This function shows how to set bitmap annotations
     */
    private fun setMarkerBitmap() {
        annotationsController.clear()
        val bitmap = BitmapFactory.decodeResource(context?.resources, R.drawable.car_steering_wheel)
        val userGraphic = Annotation.UserGraphic(bitmap)
        annotation = factory.create(requireContext(), userGraphic, location)
        annotationsController.add(listOf(annotation))
    }

    /**
     * This function shows how to set default annotations
     */
    private fun setLocationAnnotations() {
        annotationsController.clear()
        annotation = factory.create(requireContext(), R.drawable.map_pin_green_icon_unfocused, location)
        annotationsController.add(listOf(annotation))
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
    }
}