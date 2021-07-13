/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.mapview

import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.controllers.Camera
import com.telenav.sdk.examples.R
import kotlinx.android.synthetic.main.fragment_map_view_camera.*
import kotlinx.android.synthetic.main.fragment_map_view_camera.mapView
import kotlinx.android.synthetic.main.layout_action_bar.*


/**
 * This fragment shows how to set camera position in MapView
 * @author zhai.xiang on 2021/1/11
 */
class MapViewCameraFragment : Fragment() {

    companion object {
        const val TAG = ""

        /**
         * maximum value of offset
         */
        const val MAX_OFFSET = 1.0

        /**
         * minimum value of offset
         */
        const val MIN_OFFSET = -1.0
    }

    /**
     * location of New York City
     */
    private val locationA = Location(TAG).apply {
        this.latitude = 40.7478055
        this.longitude = -73.9850480
    }

    /**
     * location of Los Angeles
     */
    private val locationB = Location(TAG).apply {
        this.latitude = 34.0772327
        this.longitude = -118.2584544
    }

    private var currentVerticalOffset = 0.0
    private var currentHorizontalOffset = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map_view_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_title.text = getString(R.string.title_activity_map_view_camera)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }
        mapViewInit(savedInstanceState)
        setOnClickListener()
        setCameraUpdateListener()
    }

    private fun mapViewInit(savedInstanceState: Bundle?) {
        mapView.initialize(savedInstanceState){
            setLocationAnnotations()
            moveCameraToLocation(locationA)
            activity?.runOnUiThread {
                mapView.vehicleController().setLocation(locationA)
            }
        }
    }

    private fun setOnClickListener() {
        btn_position_A.text = String.format("Go to New York\n(%.5f,%.5f)",locationA.latitude,locationA.longitude)
        btn_position_A.setOnClickListener { moveCameraToLocation(locationA) }
        btn_position_B.text = String.format("Go to Los Angeles\n(%.5f,%.5f)",locationB.latitude,locationB.longitude)
        btn_position_B.setOnClickListener { moveCameraToLocation(locationB) }

        btn_offset_up.setOnClickListener { setVerticalOffset(-0.1) }
        btn_offset_down.setOnClickListener { setVerticalOffset(0.1) }
        btn_offset_left.setOnClickListener { setHorizontalOffset(-0.1) }
        btn_offset_right.setOnClickListener { setHorizontalOffset(0.1) }

        btn_tilt_add.setOnClickListener { setTiltBy(10f) }
        btn_tilt_minus.setOnClickListener { setTiltBy(-10f) }

        btn_zoom_add.setOnClickListener { setZoomLevelBy(1f) }
        btn_zoom_minus.setOnClickListener { setZoomLevelBy(-1f) }
    }

    private fun setLocationAnnotations() {
        val factory = mapView.annotationsController().factory()
        val annotationA = factory.create(requireContext(),R.drawable.map_pin_green_icon_unfocused,locationA)
        val annotationB = factory.create(requireContext(),R.drawable.map_pin_green_icon_unfocused,locationB)
        mapView.annotationsController().add(listOf(annotationA,annotationB))
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
        mapView.cameraController().position = Camera.Position.Builder().setLocation(location).build()
    }

    /**
     * This function shows how to set vertical offset of map view.
     */
    private fun setVerticalOffset(value: Double) {
        currentVerticalOffset += value
        currentVerticalOffset = currentVerticalOffset.coerceAtMost(MAX_OFFSET).coerceAtLeast(MIN_OFFSET)
        mapView.layoutController().setVerticalOffset(currentVerticalOffset)
    }

    /**
     * This function shows how to set horizontal offset of map view.
     */
    private fun setHorizontalOffset(value: Double) {
        currentHorizontalOffset += value
        currentHorizontalOffset = currentHorizontalOffset.coerceAtMost(MAX_OFFSET).coerceAtLeast(MIN_OFFSET)
        mapView.layoutController().setHorizontalOffset(currentHorizontalOffset)
    }

    /**
     * This function shows how to set tilt of camera.
     * The tilt is range from 0 to 60
     */
    private fun setTiltBy(value: Float) {
        var currentTilt = mapView.cameraController().position?.tilt ?: 0f
        // TODO must delete this code when the bug is fixed!!
        if (currentTilt < 0f) {
            currentTilt += 90f
        }
        mapView.cameraController().position = Camera.Position.Builder().setTilt(currentTilt + value).build()
    }

    /**
     * This function shows how to set zoom level of camera.
     * The maximum value of zoom level is 16 and minimum value of zoom level is 0
     */
    private fun setZoomLevelBy(value: Float) {
        val zoom = mapView.cameraController().position?.zoomLevel ?: 0f
        mapView.cameraController().position = Camera.Position.Builder().setZoomLevel(zoom + value).build()
    }

    /**
     * This function shows how to add a listener to update camera state
     */
    private fun setCameraUpdateListener() {
        mapView.addMapViewListener {
            it.cameraLocation
            val text = String.format("camera position: [%.4f , %.4f]\nzoom level: %.1f\nrange horizontal: %.3f\noffset [%.1f, %.1f]",
                    it.cameraLocation.latitude,
                    it.cameraLocation.longitude,
                    it.zoomLevel,
                    it.rangeHorizontal,
                    currentVerticalOffset,
                    currentHorizontalOffset)
            activity?.runOnUiThread{
                tv_state?.text = text
            }
        }
    }
}