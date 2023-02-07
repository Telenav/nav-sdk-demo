/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.mapview

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.touch.GestureType
import com.telenav.map.api.touch.TouchType
import com.telenav.map.api.touch.listeners.TouchListener
import com.telenav.map.api.touch.listeners.ViewTouchListener
import com.telenav.sdk.examples.R
import kotlinx.android.synthetic.main.fragment_map_view_gesture.*
import kotlinx.android.synthetic.main.layout_action_bar.*
import kotlinx.android.synthetic.main.layout_content_map_with_text.*
import kotlinx.android.synthetic.main.layout_operation_gesture.*
import kotlin.math.PI
import kotlin.math.atan2

/**
 * This fragment shows gesture operations of map view
 * @author zhai.xiang on 2021/1/19
 */
class MapViewGestureFragment : Fragment() {
    private val location = Location("MapView").apply {
        this.latitude = 37.398762
        this.longitude = -121.977216
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_view_gesture, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_title.text = getString(R.string.title_activity_map_view_gesture)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }
        btn_show_menu.setOnClickListener {
            drawer_layout.open()
        }
        mapViewInit(savedInstanceState)
        operationInit()
        reactToMapGesture()
    }

    private fun mapViewInit(savedInstanceState: Bundle?) {
        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener = {
                it.vehicleController().setLocation(location)
                it.cameraController().position =
                    Camera.Position.Builder().setLocation(location).build()
                resetGestureState()
            }
        )
        mapView.initialize(mapViewConfig)
    }

    private fun operationInit() {
        sc_zoom_state.setOnCheckedChangeListener(gestureEnableChangeListener)
        sc_rotate_state.setOnCheckedChangeListener(gestureEnableChangeListener)
        sc_tilt_state.setOnCheckedChangeListener(gestureEnableChangeListener)
        sc_pan_state.setOnCheckedChangeListener(gestureEnableChangeListener)
        sc_self_define.setOnCheckedChangeListener { _, isChecked ->
            sc_zoom_state.isEnabled = !isChecked
            sc_rotate_state.isEnabled = !isChecked
            sc_tilt_state.isEnabled = !isChecked
            sc_pan_state.isEnabled = !isChecked

            setAndroidTouchListener(isChecked)

        }
        resetGestureTitleText()
    }

    /**
     * This method shows how to get map information by touch listener.
     */
    private fun reactToMapGesture() {
        mapView.setOnTouchListener(TouchListener { type, touchPosition ->
            when (type) {
                TouchType.LongClick -> {
                    activity?.runOnUiThread {
                        Toast.makeText(
                            requireActivity(),
                            "Long Click", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                TouchType.Click -> {
                    activity?.runOnUiThread {
                        Toast.makeText(
                            requireActivity(),
                            "Click [${touchPosition.geoLocation?.latitude}, ${touchPosition.geoLocation?.longitude}]",
                            Toast.LENGTH_SHORT
                        ).show()
                        location.bearing = (location.bearing + 90) % 360
                    }
                }

                else -> {}
            }
        })

    }

    /**
     * This function shows how to set view touch listener.
     * Notice: This listener will make map's default gesture unavailable.
     */
    private fun setAndroidTouchListener(on: Boolean) {
        if (on) {
            mapView.setOnViewTouchListener(viewTouchListener)
            tv_state.visibility = View.VISIBLE
            tv_state.text = "Use Two fingers to change the head angle of the vehicle"
        } else {
            tv_state.visibility = View.GONE
            mapView.setOnViewTouchListener(null)
        }
    }

    private val gestureEnableChangeListener = CompoundButton.OnCheckedChangeListener { _, _ ->
        resetGestureState()
        resetGestureTitleText()
    }

    private val viewTouchListener = ViewTouchListener {
        rotateGestureDetector.onTouch(it)
        true
    }

    private fun isZoomGestureEnable() = sc_zoom_state.isChecked

    private fun isTiltGestureEnable() = sc_tilt_state.isChecked

    private fun isRotateGestureEnable() = sc_rotate_state.isChecked

    private fun isPanGestureEnable() = sc_pan_state.isChecked

    private val rotateGestureDetector = RotateGestureDetector {
        if (it >= 0) {
            location.bearing = (location.bearing + it) % 360
        } else {
            location.bearing = (location.bearing + it + 360) % 360
        }
        mapView.vehicleController().setLocation(location)
    }

    /**
     * This method shows how to enable or disable the gesture.
     * (If the gesture is not added to the active gesture list, it will be disabled)
     */
    private fun resetGestureState() {
        val activeGestures = mutableSetOf<GestureType>()
        if (isZoomGestureEnable()) {
            activeGestures.add(GestureType.Zoom)
        }
        if (isRotateGestureEnable()) {
            activeGestures.add(GestureType.Rotate)
        }
        if (isTiltGestureEnable()) {
            activeGestures.add(GestureType.Tilt)
        }
        if (isPanGestureEnable()) {
            activeGestures.add(GestureType.Pan)
        }
        mapView.setActiveGestures(activeGestures)
    }

    private fun resetGestureTitleText() {
        tv_title_zoom_state.text = if (isZoomGestureEnable()) {
            "Zoom gesture enable"
        } else {
            "Zoom gesture disable"
        }
        tv_title_rotate_state.text = if (isRotateGestureEnable()) {
            "Rotate gesture enable"
        } else {
            "Rotate gesture disable"
        }
        tv_title_tilt_state.text = if (isTiltGestureEnable()) {
            "Tilt gesture enable"
        } else {
            "Tilt gesture disable"
        }
        tv_title_pan_state.text = if (isPanGestureEnable()) {
            "Pan gesture enable"
        } else {
            "Pan gesture disable"
        }
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
     * The example of self gesture.
     * It shows how to output a rotation degree of gesture.
     */
    private class RotateGestureDetector(val angleBy: (Float) -> Unit) {

        companion object {
            const val INVALID_POINTER_ID = -1
        }

        private var lastAngle: Float = 0f
        private var firstPointerId = INVALID_POINTER_ID
        private var secondPointerId = INVALID_POINTER_ID

        fun onTouch(event: MotionEvent) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    firstPointerId = event.getPointerId(event.actionIndex)

                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (firstPointerId == INVALID_POINTER_ID) {
                        firstPointerId = event.getPointerId(event.actionIndex)
                    } else if (secondPointerId == INVALID_POINTER_ID) {
                        secondPointerId = event.getPointerId(event.actionIndex)
                    }
                    if (isValid()) {
                        lastAngle = getSlope(event)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isValid()) {
                        val newAngle = getSlope(event)
                        angleBy(newAngle - lastAngle)
                        lastAngle = newAngle
                    }
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    val pointer = event.getPointerId(event.actionIndex)
                    if (firstPointerId == pointer) {
                        firstPointerId = INVALID_POINTER_ID
                    } else if (secondPointerId == pointer) {
                        secondPointerId = INVALID_POINTER_ID
                    }
                }
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_UP -> {
                    firstPointerId = INVALID_POINTER_ID
                    secondPointerId = INVALID_POINTER_ID
                }
            }
        }

        /**
         * If there are two fingers touch the screen.
         */
        private fun isValid(): Boolean = firstPointerId != INVALID_POINTER_ID &&
            secondPointerId != INVALID_POINTER_ID

        /**
         * get slope of two fingers.
         */
        private fun getSlope(x1: Float, y1: Float, x2: Float, y2: Float): Float =
            atan2((x2 - x1), (y1 - y2)) * 180 / PI.toFloat()

        /**
         * get slope of two fingers.
         */
        private fun getSlope(event: MotionEvent): Float {
            val index1 = event.findPointerIndex(firstPointerId)
            val index2 = event.findPointerIndex(secondPointerId)
            return getSlope(
                event.getX(index1),
                event.getY(index1),
                event.getX(index2),
                event.getY(index2)
            )
        }
    }
}