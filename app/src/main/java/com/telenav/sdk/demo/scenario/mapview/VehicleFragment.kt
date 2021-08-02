/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
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
import com.telenav.map.api.controllers.Camera
import com.telenav.sdk.examples.R
import kotlinx.android.synthetic.main.fragment_map_view_camera.mapView
import kotlinx.android.synthetic.main.fragment_map_view_vehicle.*
import kotlinx.android.synthetic.main.layout_action_bar.*


/**
 * This fragment shows how to set Vehicle in MapView
 * @author wu,changzhong on 2021/30/07
 */
class VehicleFragment : Fragment() {

    companion object {
        const val TAG = "VehicleFragment"
    }

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

    }

    private fun mapViewInit(savedInstanceState: Bundle?) {
        mapView.initialize(savedInstanceState) {
            moveCameraToLocation(location)
            mapView.vehicleController().setLocation(location)
        }
    }

    private fun setOnClickListener() {
        btn_set_vehicle_icon.setOnClickListener { setVehicleIconByDrawable() }
        btn_set_vehicle_icon_bitmap.setOnClickListener { setVehicleIconByBitmap() }
        btn_locate_map_center.setOnClickListener { moveCameraToLocation(location) }
    }

    /**
     * This function shows how to set vehicle icon by drawable
     */
    private fun setVehicleIconByDrawable() {
        mapView.vehicleController().setIcon(R.drawable.map_pin_red_icon_unfocused)
    }

    /**
     * This function shows how to set vehicle icon by bitmap
     */
    private fun setVehicleIconByBitmap() {
        val bitmap = BitmapFactory.decodeResource(context?.resources, R.drawable.car_steering_wheel)
        mapView.vehicleController().setIcon(bitmap)
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