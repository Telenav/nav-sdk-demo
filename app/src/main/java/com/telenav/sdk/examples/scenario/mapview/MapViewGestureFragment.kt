/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.mapview

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
import com.telenav.map.api.MapView
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.MapViewReadyListener
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.touch.GestureType
import com.telenav.map.api.touch.TouchType
import com.telenav.map.api.touch.listeners.TouchListener
import com.telenav.map.api.touch.listeners.ViewTouchListener
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.FragmentMapViewGestureBinding
import kotlin.math.PI
import kotlin.math.atan2

/**
 * This fragment shows gesture operations of map view
 * @author zhai.xiang on 2021/1/19
 */
class MapViewGestureFragment : Fragment() {
    private var _binding: FragmentMapViewGestureBinding? = null
    private val binding get() = _binding!!

    private val location = Location("MapView").apply {
        this.latitude = 37.398762
        this.longitude = -121.977216
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapViewGestureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.actionBar.tvTitle.text = getString(R.string.title_activity_map_view_gesture)
        binding.actionBar.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.includeContent.btnShowMenu.setOnClickListener {
            binding.drawerLayout.open()
        }
        mapViewInit(savedInstanceState)
        operationInit()
        reactToMapGesture()
    }

    private val mapViewReadyListener = MapViewReadyListener<MapView> {
        binding.includeContent.mapView.getVehicleController()?.setLocation(location)
        binding.includeContent.mapView.getCameraController()?.position =
            Camera.Position.Builder().setLocation(location).build()
        resetGestureState()
    }

    private fun mapViewInit(savedInstanceState: Bundle?) {
        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener = mapViewReadyListener
        )
        binding.includeContent.mapView.initialize(mapViewConfig)
    }

    private fun operationInit() {
        binding.includeOperation.scZoomState.setOnCheckedChangeListener(gestureEnableChangeListener)
        binding.includeOperation.scRotateState.setOnCheckedChangeListener(gestureEnableChangeListener)
        binding.includeOperation.scTiltState.setOnCheckedChangeListener(gestureEnableChangeListener)
        binding.includeOperation.scPanState.setOnCheckedChangeListener(gestureEnableChangeListener)
        binding.includeOperation.scSelfDefine.setOnCheckedChangeListener { _, isChecked ->
            binding.includeOperation.scZoomState.isEnabled = !isChecked
            binding.includeOperation.scRotateState.isEnabled = !isChecked
            binding.includeOperation.scTiltState.isEnabled = !isChecked
            binding.includeOperation.scPanState.isEnabled = !isChecked

            setAndroidTouchListener(isChecked)

        }
        resetGestureTitleText()
    }

    /**
     * This method shows how to get map information by touch listener.
     */
    private fun reactToMapGesture() {
        binding.includeContent.mapView.setOnTouchListener(TouchListener { type, touchPosition ->
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
            binding.includeContent.mapView.setOnViewTouchListener(viewTouchListener)
            binding.includeContent.tvState.visibility = View.VISIBLE
            binding.includeContent.tvState.text = "Use Two fingers to change the head angle of the vehicle"
        } else {
            binding.includeContent.tvState.visibility = View.GONE
            binding.includeContent.mapView.setOnViewTouchListener(null)
        }
    }

    private val gestureEnableChangeListener = CompoundButton.OnCheckedChangeListener { _, _ ->
        resetGestureState()
        resetGestureTitleText()
    }

    private val viewTouchListener = ViewTouchListener { motionEvent ->
        motionEvent?.let {
            rotateGestureDetector.onTouch(it)
        }
        true
    }

    private fun isZoomGestureEnable() = binding.includeOperation.scZoomState.isChecked

    private fun isTiltGestureEnable() = binding.includeOperation.scTiltState.isChecked

    private fun isRotateGestureEnable() = binding.includeOperation.scRotateState.isChecked

    private fun isPanGestureEnable() = binding.includeOperation.scPanState.isChecked

    private val rotateGestureDetector = RotateGestureDetector {
        if (it >= 0) {
            location.bearing = (location.bearing + it) % 360
        } else {
            location.bearing = (location.bearing + it + 360) % 360
        }
        binding.includeContent.mapView.vehicleController().setLocation(location)
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
        binding.includeContent.mapView.setActiveGestures(activeGestures)
    }

    private fun resetGestureTitleText() {
        binding.includeOperation.tvTitleZoomState.text = if (isZoomGestureEnable()) {
            "Zoom gesture enable"
        } else {
            "Zoom gesture disable"
        }
        binding.includeOperation.tvTitleRotateState.text = if (isRotateGestureEnable()) {
            "Rotate gesture enable"
        } else {
            "Rotate gesture disable"
        }
        binding.includeOperation.tvTitleTiltState.text = if (isTiltGestureEnable()) {
            "Tilt gesture enable"
        } else {
            "Tilt gesture disable"
        }
        binding.includeOperation.tvTitlePanState.text = if (isPanGestureEnable()) {
            "Pan gesture enable"
        } else {
            "Pan gesture disable"
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