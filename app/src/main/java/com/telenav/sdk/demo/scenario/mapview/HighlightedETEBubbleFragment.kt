/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.mapview

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.MapView
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.MapViewReadyListener
import com.telenav.map.api.controllers.AnnotationsController
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.controllers.CameraController
import com.telenav.map.api.controllers.RoutesController
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.map.api.touch.listeners.POITouchListener
import com.telenav.map.engine.GLMapAnnotation
import com.telenav.map.engine.GLMapRouteAnnotation
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.examples.R
import kotlinx.android.synthetic.main.highlighted_e_t_e_bubble_fragment.*
import kotlinx.android.synthetic.main.layout_action_bar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * This fragment shows how to operate route annotation in MapView and highlight route bubbles
 *
 * @author Mykola Ivantsov - (p)
 */
class HighlightedETEBubbleFragment : Fragment() {

    companion object {
        fun newInstance() = HighlightedETEBubbleFragment()
        private const val TAG = "HighlightedETEBubbleFragment"
        private const val DEFAULT_VALUE_NOT_SELECTED = 0F
    }

    private val viewModel by viewModels<HighlightedETEBubbleViewModel>()
    private val mainCoroutineScope = CoroutineScope(Dispatchers.Main)
    private val poiTouchListener = object : POITouchListener {
        override fun pressEvent(
            touchType: TouchType,
            position: TouchPosition,
            poiDescription: String?
        ) {
            printDebugLog(
                "HighlightedETEBubbleFragment touchType = $touchType," +
                    " position = $position, poiDescription = $poiDescription"
            )
            runInMain { showToast("poiDescription = $poiDescription, Position: latitude = ${position.geoLocation?.latitude}, longitude = ${position.geoLocation?.longitude}") }
        }
    }
    private val mapViewReadyListener = MapViewReadyListener<MapView> {
        runInMain {
            mapView.cameraController()?.position =
                Camera.Position.Builder().setLocation(viewModel.startLocation).build()
            mapView.vehicleController()?.setLocation(viewModel.startLocation)
        }
    }

    private val mapViewConfig: MapViewInitConfig by lazy {
        MapViewInitConfig(
            context = requireContext(),
            defaultLocation = viewModel.startLocation,
            readyListener = mapViewReadyListener
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.highlighted_e_t_e_bubble_fragment, container, false)
    }

    private fun showToast(msg: String) {
        Toast.makeText(
            requireContext(),
            msg,
            Toast.LENGTH_SHORT
        ).show()
    }

    private var defaultSmartBubbleType = DEFAULT_VALUE_NOT_SELECTED
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val makeRequestOnClickListener = { _: View ->
            //remember current smart-bubble type
            defaultSmartBubbleType = viewModel.getSelectedBubbleType()
            //before we can create a route smart-bubble annotation we have to request the route
            requestDirection(
                getRoutesController(),
                getCameraController(),
                getAnnotationsController(),
                viewModel.startLocation,
                viewModel.stopLocation
            ) { result ->
                runInMain {
                    showToast("result: $result")
                }
            }
        }

        val onRouteTouchListener =
            { _: TouchType, _: TouchPosition, routeID: String ->
                highlightRoute(routeID, getRoutesController())
                //here we can manage updating of route smart-bubble annotations
                //iterate our local annotations that we have created before
                viewModel.getRouteAnnotationMap().forEach { routeAnnotationMap ->
                    printDebugLog("map key -> ${routeAnnotationMap.key}, routeId -> $routeID")
                    when (routeAnnotationMap.value) {
                        is GLMapAnnotation -> {
                            val routeAnnotation = routeAnnotationMap.value as GLMapRouteAnnotation
                            //now we can compare the route id that was clicked  and what we have
                            // in our local storage in the viewModel
                            if (routeAnnotationMap.key != routeID) {
                                //if it is not selected set the smart-bubble type that we took before
                                // we made the route request
                                routeAnnotation.update(defaultSmartBubbleType)
                            } else {
                                //otherwise set the smart-bubble type that we selected
                                routeAnnotation.update(viewModel.getSelectedBubbleType())
                            }
                        }
                    }

                }
            }
        val adapterStyleIdItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val item: String = parent?.getItemAtPosition(position) as String ?: ""
                printDebugLog("item: $item, position = $position")
                viewModel.styleIdSelected = item
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //do nothing
            }
        }
        val adapterStyleId: ArrayAdapter<String> =
            ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                viewModel.styleIds
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        val adapterEvBubbleType: ArrayAdapter<String> =
            ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                viewModel.evBubbleType
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        val adapterEvBubbleTypeItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val item: String = parent?.getItemAtPosition(position) as String ?: ""
                    printDebugLog("item: $item, position = $position")
                    viewModel.evBubbleTypeSelected = position.toFloat()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    //do nothing
                }

            }
        val adapterSmartBubbleType: ArrayAdapter<String> =
            ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                viewModel.smartBubbleType
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        val adapterSmartBubbleTypeItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val item: String = parent?.getItemAtPosition(position) as String ?: ""
                    printDebugLog("item: $item, position = $position")
                    viewModel.smartBubbleTypeSelected = position.toFloat()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    //do nothing
                }

            }

        tv_title.text = getString(R.string.title_activity_map_view_highlight_ete_bubble)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }
        // the initialize function must be called after SDK is initialized
        mapView.initialize(mapViewConfig)
        btn_make_request.setOnClickListener(makeRequestOnClickListener)
        mapView.setOnRouteTouchListener(onRouteTouchListener)
        mapView.setOnAnnotationTouchListener { touchType, position, touchedAnnotations ->
            printDebugLog("HighlightedETEBubbleFragment touchType = $touchType," +
                " position = $position," +
                " touchedAnnotations size = ${touchedAnnotations.size}," +
                " touchedAnnotations = $touchedAnnotations")
            touchedAnnotations.forEach { touchAnnotation ->
                for ((key, value) in viewModel.getRouteAnnotationMap()) {
                    printDebugLog("map key -> $key, value = $value")
                    when ((value as GLMapRouteAnnotation).annotationId) {
                        (touchAnnotation.annotation as GLMapAnnotation).annotationId -> {
                            onRouteTouchListener(touchType,position, key)
                        }
                    }
                }
            }
        }

        mapView.setOnPOITouchListener(poiTouchListener)

        spinner_style.apply {
            adapter = adapterStyleId
            onItemSelectedListener = adapterStyleIdItemSelectedListener
        }

        spinner_ev_bubble.apply {
            adapter = adapterEvBubbleType
            onItemSelectedListener = adapterEvBubbleTypeItemSelectedListener
        }

        spinner_smart_buble.apply {
            adapter = adapterSmartBubbleType
            onItemSelectedListener = adapterSmartBubbleTypeItemSelectedListener
        }
    }

    private fun printDebugLog(msg: String) {
        TaLog.d(TAG, msg)
    }

    private fun runInMain(run: () -> Unit): Job {
        return mainCoroutineScope.launch {
            run()
        }
    }

    private fun highlightRoute(mapRouteId: String, routesController: RoutesController) {
        routesController.highlight(mapRouteId)
        printDebugLog("highlightRoute -> routeId = $mapRouteId")
    }


    private fun requestDirection(
        routesController: RoutesController,
        cameraController: CameraController,
        annotationsController: AnnotationsController,
        begin: Location,
        end: Location,
        result: (Boolean) -> Unit
    ) {
        viewModel.requestDirection(
            routesController,
            cameraController,
            annotationsController,
            begin,
            end,
            result
        )
    }

    private fun getCameraController(): CameraController {
        return mapView.cameraController()
    }

    private fun getAnnotationsController(): AnnotationsController {
        return mapView.annotationsController()
    }

    private fun getRoutesController(): RoutesController {
        return mapView.routesController()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}