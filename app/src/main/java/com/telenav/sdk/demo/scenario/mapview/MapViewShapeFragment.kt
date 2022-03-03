/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.mapview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.Margins
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.controllers.ShapesController
import com.telenav.map.geo.Attributes
import com.telenav.map.geo.Shape
import com.telenav.sdk.demo.provider.DemoLocationProvider
import com.telenav.map.geo.newImpl.ClientTexture
import com.telenav.sdk.demo.util.KmlParser
import com.telenav.sdk.examples.R
import kotlinx.android.synthetic.main.layout_action_bar.*
import kotlinx.android.synthetic.main.map_view_shape_fragment.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/**
 * This fragment shows how to set Textured on MapView using user attributes or
 * resources which we have in the texture directory.
 *
 * Real reach shapes.
 * To see the test real reach shape it is mandatory to add a proper layer into the newstyle.tss.
 * Polygons that can be used in this test have this configuration:
 *
 * layer<custom-polygon> rrtest[custom-polygon-type = "rrtest"]
{
color: $color;
outline-width : $outline-width;
outline-color : $outline-color;
};
 *
 * @author Mykola Ivantsov - (p) on 2021/11/10
 * @author Dmytro Lavrikov - (p) on 2022/02/01
 */
class MapViewShapeFragment : Fragment() {


    lateinit var locationProvider: DemoLocationProvider
    private lateinit var viewModel: MapViewShapeViewModel
    private val routeTraceShapeStyleAttributes = Attributes.Builder()
        .setShapeStyle(ROUTE_TRACE_SHAPE_STYLE)
        .setColor(Color.GREEN)
        .setLineWidth(100.0f)
        .build()
    private val customPolygonShapeStyleAttributes = Attributes.Builder()
        .setShapeStyle(CUSTOM_POLYGON_SHAPE_STYLE)
        .setColor(Color.GREEN)
        .setLineWidth(100.0f)
        .build()
    private val resourceNameWorldQuadShapeStyleAttributes: Attributes = Attributes.Builder()
        .setShapeStyle(WORLD_QUAD_SHAPE_STYLE)
        .setTextureResourceName(TEXTURE_RESOURCE_NAME)
        .setTextureResourceWidth(64)
        .setTextureResourceHeight(64)
        .setLineWidth(100.0f)
        .build()

    /** Polygon that represents a 'rrtest' example of custom-polygon in the TSS */
    private val realReachTestPolygon: Attributes = Attributes.Builder()
        .setShapeStyle(REAL_REACH_TEST_SHAPE_STYLE)
        .setLineWidth(10.0f)
        .setColor(Color.RED)
        .setFloat("outline-width", 10.0f)
        .setColor("outline-color", Color.BLUE)
        .build()
    private val realReachPolygon: Attributes = Attributes.Builder()
        .setShapeStyle(REAL_REACH_POLY_SHAPE_STYLE)
        .setColor(Color.RED)
        .setLineWidth(10.0f)
        .setColor("outline-color", Color.BLUE)
        .build()


    // Draw a pseudo-home area rectangle
    private val cords = ArrayList<Location>().apply {
        add(Location("").apply {
            latitude = 30.3935
            longitude = -86.4958
        })
        add(Location("").apply {
            latitude = 30.3935
            longitude = -86.5958
        })
        add(Location("").apply {
            latitude = 30.4935
            longitude = -86.5958
        })
        add(Location("").apply {
            latitude = 30.4935
            longitude = -86.4958
        })
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
        addSquareRRPolygonShape.setOnClickListener {
            addSquareRealReachShape(realReachPolygon)
        }
        addComplexRRPolygonShape.setOnClickListener {
            addComplexRealReachShape(realReachPolygon)
        }
        addTestRRPolygonShape.setOnClickListener {
            addTestRealReachShape(realReachTestPolygon)
        }
        addTexturedQuadClientTextureBtn.setOnClickListener {
            //bytes, needs to be exactly that.. raw bytes r,g,b,a,r,g,b,a,r,g,b,a etc where every one
            // of those is a byte in a regular byte array
            try {
                requireContext().assets.open(CLIENT_PNG_IMAGE_NAME).use { inputStream ->
                    val bufferedInputStream = BufferedInputStream(inputStream)
                    val bmp: Bitmap = BitmapFactory.decodeStream(bufferedInputStream)
                    if (bmp.config == Bitmap.Config.ARGB_8888) {
                        val worldQuadShapeStyleAttributes = Attributes.Builder()
                            .setShapeStyle(WORLD_QUAD_SHAPE_STYLE)
                            .setClientTexture(ClientTexture(bmp, bmp.width, bmp.height))
                            .build()
                        addTexturedQuadClientTexture(cords, worldQuadShapeStyleAttributes)
                    } else {
                        showToast("Bitmap must be in ARGB_8888 format!")
                    }
                }
            } catch (ex: IOException) {
                showToast("File not found in assets ...")
            }


        }
        removeAllShapesBtn.setOnClickListener {
            removeAllShapes()
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(
            requireContext(),
            msg,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun removeAllShapes() {
        viewModel.getAll().forEach { shapeId ->
            mapView.shapesController().remove(shapeId)
            viewModel.remove(shapeId)
        }
    }

    private fun addTexturedQuadClientTexture(cords: List<Location>, attributes: Attributes) {
        addShape(Shape.Type.TexturedQuad, attributes, cords)
    }

    private fun addTexturedQuadResourceName(cords: List<Location>, attributes: Attributes) {
        addShape(Shape.Type.TexturedQuad, attributes, cords)
    }

    private fun addComplexRealReachShape(attributes: Attributes) {
        val coordinates: List<Location> = KmlParser.parse(requireContext(), R.raw.route_na)

        addShape(Shape.Type.Polygon, attributes, coordinates)
        showRegion(coordinates)
    }

    private fun addTestRealReachShape(attributes: Attributes) {
        addShape(Shape.Type.Polygon, attributes, cords)
        showRegion(cords)
    }

    private fun addSquareRealReachShape(attributes: Attributes) {
        addShape(Shape.Type.Polygon, attributes, cords)
        showRegion(cords)
    }

    private fun addPolygonShape(cords: List<Location>, attributes: Attributes) {
        addShape(Shape.Type.Polygon, attributes, cords)
    }

    private fun addShapePolyline(cords: List<Location>, attributes: Attributes) {
        addShape(Shape.Type.Polyline, attributes, cords)
    }

    private fun addShape(type: Shape.Type, attributes: Attributes, cords: List<Location>) {
       // val shape = Shape(type, cords, attributes)

        val collectionBuilder = Shape.Collection.Builder()
        //collectionBuilder.addShape(shape)
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

    /** Show the region for the shape with unknown amount of lat-lon points */
    private fun showRegion(cords: List<Location>) {
        val region = Camera.Region().apply {
            northLatitude = getMostNorthLatitude(cords)
            southLatitude = getMostSouthLatitude(cords)
            eastLongitude = getMostEastLongitude(cords)
            westLongitude = getMostWestLongitude(cords)
        }
        mapView.cameraController().showRegion(region, Margins.Pixels(MAP_MARGIN_PIXELS, MAP_MARGIN_PIXELS))
    }

    private fun getMostNorthLatitude(cords: List<Location>): Double {
        var latitude = LAT_SOUTH_POLE // Starting with South Pole
        cords.forEach {
            latitude = max(it.latitude, latitude)
        }
        return latitude
    }

    private fun getMostSouthLatitude(cords: List<Location>): Double {
        var latitude = LAT_NORTH_POLE // Starting with North Pole
        cords.forEach {
            latitude = min(it.latitude, latitude)
        }
        return latitude
    }

    private fun getMostWestLongitude(cords: List<Location>): Double {
        var longitude = LON_EAST_PACIFIC // Starting from East on mid-Pacific Ocean Meridian
        cords.forEach {
            longitude = min(it.longitude, longitude)
        }
        return longitude
    }

    private fun getMostEastLongitude(cords: List<Location>): Double {
        var longitude = LON_WEST_PACIFIC // Starting from West on mid-Pacific Ocean Meridian
        cords.forEach {
            longitude = max(it.longitude, longitude)
        }
        return longitude
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
        private const val REAL_REACH_POLY_SHAPE_STYLE = "real-reach-poly"
        private const val REAL_REACH_TEST_SHAPE_STYLE = "rrtest"
        private const val ROUTE_TRACE_SHAPE_STYLE = "route.trace"
        private const val CLIENT_PNG_IMAGE_NAME = "4-5-5.png"

        private const val LAT_SOUTH_POLE = -90.0
        private const val LAT_NORTH_POLE = 90.0
        private const val LON_EAST_PACIFIC = 180.0
        private const val LON_WEST_PACIFIC = -180.0

        private const val MAP_MARGIN_PIXELS = 200.0
    }

}