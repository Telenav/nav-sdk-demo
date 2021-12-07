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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.controllers.ShapesController
import com.telenav.map.geo.Attributes
import com.telenav.map.geo.Shape
import com.telenav.map.geo.newImpl.ClientTexture
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.demo.provider.DemoLocationProvider
import com.telenav.sdk.examples.R
import kotlinx.android.synthetic.main.layout_action_bar.*
import kotlinx.android.synthetic.main.map_view_shape_fragment.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
/**
 * This fragment shows how to set Textured on MapView using user attributes or
 * resources which we have in the texture directory.
 *
 * @author Mykola Ivantsov - (p) on 2021/11/10
 */
class MapViewShapeFragment : Fragment() {


    lateinit var locationProvider: DemoLocationProvider
    private lateinit var viewModel: MapViewShapeViewModel
    private val routeTraceShapeStyleAttributes = Attributes.Builder()
        .setShapeStyle(ROUTE_TRACE_SHAPE_STYLE)
        .setColor(0xFF00FF00.toInt())
        .setLineWidth(100.0f)
        .build()
    private val customPolygonShapeStyleAttributes = Attributes.Builder()
        .setShapeStyle(CUSTOM_POLYGON_SHAPE_STYLE)
        .setColor(0xFF00FF00.toInt())
        .setLineWidth(100.0f)
        .build()
    private val resourceNameWorldQuadShapeStyleAttributes: Attributes = Attributes.Builder()
        .setShapeStyle(WORLD_QUAD_SHAPE_STYLE)
        .setTextureResourceName(TEXTURE_RESOURCE_NAME)
        .setTextureResourceWidth(64)
        .setTextureResourceHeight(64)
        .setLineWidth(100.0f)
        .build()

    //bytes, needs to be exactly that.. raw bytes r,g,b,a,r,g,b,a,r,g,b,a etc where every one
    // of those is a byte in a regular byte array
    private val byteArray =
        "255,0,0,255,255,0,0,255,255,0,0,255,255,0,0,255,255,0,0,255,255,0,0,255"

    private val worldQuadShapeStyleAttributes = Attributes.Builder()
        .setShapeStyle(WORLD_QUAD_SHAPE_STYLE)
        .setClientTexture(ClientTexture(byteArray, 6, 1))
        .setLineWidth(100.0f)
        .build()

    // Draw a pseudo-home area rectangle
    private val cords = ArrayList<LatLon>().apply {
        add(LatLon(30.3935, -86.4958))
        add(LatLon(30.3935, -86.5958))
        add(LatLon(30.4935, -86.5958))
        add(LatLon(30.4935, -86.4958))
        add(this.first())
    }

    private val location = Location("").apply {
        latitude = 30.3935
        longitude = -86.4958
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.map_view_shape_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MapViewShapeViewModel::class.java)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationProvider = DemoLocationProvider.Factory.createProvider(
            requireContext(),
            DemoLocationProvider.ProviderType.SIMULATION
        )
        locationProvider.start()

        tv_title.text = getString(R.string.title_activity_map_view_shapes)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }
        mapViewInit(savedInstanceState)
        addShapePolylineBtn.setOnClickListener {
            addShapePolyline(cords, routeTraceShapeStyleAttributes)
        }
        addPoligonShapeBtn.setOnClickListener {
            addPolygonShape(cords, customPolygonShapeStyleAttributes)
        }
        addTexturedQuadResourceNameBtn.setOnClickListener {
            addTexturedQuadResourceName(cords, resourceNameWorldQuadShapeStyleAttributes)
        }
        addTexturedQuadClientTextureBtn.setOnClickListener {
            addTexturedQuadClientTexture(cords, worldQuadShapeStyleAttributes)
        }
        removeAllShapesBtn.setOnClickListener {
            removeAllShapes()
        }
    }

    private fun removeAllShapes() {
        viewModel.getAll().forEach { shapeId ->
            mapView.shapesController().remove(shapeId)
            viewModel.remove(shapeId)
        }
    }

    private fun addTexturedQuadClientTexture(cords: List<LatLon>, attributes: Attributes) {
        addShape(Shape.Type.TexturedQuad, attributes, cords)
    }

    private fun addTexturedQuadResourceName(cords: List<LatLon>, attributes: Attributes) {
        addShape(Shape.Type.TexturedQuad, attributes, cords)
    }

    private fun addPolygonShape(cords: List<LatLon>, attributes: Attributes) {
        addShape(Shape.Type.Polygon, attributes, cords)
    }

    private fun addShapePolyline(cords: List<LatLon>, attributes: Attributes) {
        addShape(Shape.Type.Polyline, attributes, cords)
    }

    private fun addShape(type: Shape.Type, attributes: Attributes, cords: List<LatLon>) {
        val shape = Shape(type, attributes, cords)

        val collectionBuilder = Shape.Collection.Builder()
        collectionBuilder.addShape(shape)
        addModel(collectionBuilder)
    }

    private fun addModel(collectionBuilder: Shape.Collection.Builder): ShapesController.Id? {
        // Don't have to keep track of shapeId since entire view will be closed
        val shapeId: ShapesController.Id? =
            mapView.shapesController().add(collectionBuilder.build())
        shapeId?.let {
            viewModel.add(it)
        }
        return shapeId
    }

    /**
     * the initialize function must be called after SDK is initialized
     */
    private fun mapViewInit(savedInstanceState: Bundle?) {
        mapView.initialize(savedInstanceState) {
            CoroutineScope(Dispatchers.Main).launch {
                mapView.cameraController()?.position =
                    Camera.Position.Builder().setLocation(location).build()
                mapView.vehicleController()?.setLocation(location)
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        locationProvider.stop()
    }

    companion object {
        fun newInstance() = MapViewShapeFragment()
        private const val WORLD_QUAD_SHAPE_STYLE = "world-quad"
        private const val TEXTURE_RESOURCE_NAME = "cvp.png"
        private const val CUSTOM_POLYGON_SHAPE_STYLE = "custom-polygon"
        private const val ROUTE_TRACE_SHAPE_STYLE = "route.trace"
    }

}