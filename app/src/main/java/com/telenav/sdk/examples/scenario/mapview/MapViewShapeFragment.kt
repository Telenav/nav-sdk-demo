/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.mapview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.MapView
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.MapViewReadyListener
import com.telenav.map.api.Margins
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.controllers.CameraController
import com.telenav.map.api.controllers.ShapesController
import com.telenav.map.api.models.RegionForModelInstance
import com.telenav.map.geo.Attributes
import com.telenav.map.geo.Shape
import com.telenav.map.geo.newImpl.ClientTexture
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.examples.provider.DemoLocationProvider
import com.telenav.sdk.examples.util.KmlParser
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.MapViewShapeFragmentBinding
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
 * @author Mykola Ivantsov - (p) on 2021/11/10
 */
class MapViewShapeFragment : Fragment() {
    private var _binding: MapViewShapeFragmentBinding? = null
    private val binding get() = _binding!!

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
    private val customDynamicPolygon: Attributes = Attributes.Builder()
        .setShapeStyle(RR_DYNAMIC_POLYGON_SHAPE_STYLE)
        .setColor("rr-color", Color.RED)
        .setColor("rr-outline-color", Color.GREEN)
        .build()
    private val realReachPolygon: Attributes = Attributes.Builder()
        .setShapeStyle(REAL_REACH_POLY_SHAPE_STYLE)
        .setLineWidth(10.0f)
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

    private val createCameraPosition: (Float) -> Camera.Position = { bear ->
        Camera.Position.Builder().setBearing(bear).build()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MapViewShapeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

        binding.include.tvTitle.text = getString(R.string.title_activity_map_view_shapes)
        binding.include.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
        mapViewInit()
        binding.addShapePolylineBtn.setOnClickListener {
            addShapePolyline(cords, routeTraceShapeStyleAttributes)
        }
        binding.addPoligonShapeBtn.setOnClickListener {
            addPolygonShape(cords, customPolygonShapeStyleAttributes)
        }
        binding.addTexturedQuadResourceNameBtn.setOnClickListener {
            addTexturedQuadResourceName(cords, resourceNameWorldQuadShapeStyleAttributes)
        }
        binding.addComplexRRPolygonShape.setOnClickListener {
            addComplexRealReachShape(realReachPolygon)
        }
        binding.addOuterRRPolygonShape.setOnClickListener {
            addOuterRealReachShape(realReachPolygon)
        }
        binding.addTexturedQuadClientTextureBtn.setOnClickListener {
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
        binding.addDynamicPolygonShapeWithAttrs.setOnClickListener {
            addDynamicPolygonShape(cords, customDynamicPolygon)
        }
        binding.removeAllShapesBtn.setOnClickListener {
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
            binding.mapView.getShapesController()?.remove(shapeId)
            viewModel.remove(shapeId)
        }
        // reset map overlay as well
        changeMapOverlay(Color.TRANSPARENT, 0.0f)
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

    private fun addDynamicPolygonShape(cords: List<LatLon>, attributes: Attributes) {
        val id = addShape(Shape.Type.Polygon, attributes, cords)
        val rect = Rect(0, 0, binding.mapView.height, binding.mapView.width)
        val cameraController = binding.mapView.getCameraController()

        cameraController?.renderMode = Camera.RenderMode.M2D
        setBearing(cameraController, createCameraPosition)

        cameraController?.showRegionForModelInstance(RegionForModelInstance(id!!, rect, true))
    }

    private fun addComplexRealReachShape(attributes: Attributes) {
        val coordinates: List<Location> = KmlParser.parse(requireContext(), R.raw.route_na)
        val latLons = mutableListOf<LatLon>().apply {
            for (location in coordinates) {
                add(LatLon(location.latitude, location.longitude))
            }
        }

        addShape(Shape.Type.Polygon, attributes, latLons)
        showRegion(latLons)
    }

    private fun addOuterRealReachShape(attributes: Attributes) {
        changeMapOverlay(Color.RED, 0.5f)
        addComplexRealReachShape(attributes)
    }

    private fun changeMapOverlay(@ColorInt color: Int, opacity: Float) {
        binding.mapView.getShapesController()?.setViewValue("overlay-color", color)
        binding.mapView.getShapesController()?.setViewValue("overlay-opacity", opacity)
    }

    private fun addShape(
        type: Shape.Type,
        attributes: Attributes,
        cords: List<LatLon>
    ): ShapesController.Id? {
        val shape = Shape(type, attributes, cords)

        val collectionBuilder = Shape.Collection.Builder()
        collectionBuilder.addShape(shape)
        return addModel(collectionBuilder)
    }

    private fun addModel(collectionBuilder: Shape.Collection.Builder): ShapesController.Id? {
        // Don't have to keep track of shapeId since entire view will be closed
        val shapeId: ShapesController.Id? =
            binding.mapView.getShapesController()?.add(collectionBuilder.build())
        shapeId?.let {
            viewModel.add(it)
        }
        return shapeId
    }

    private val mapViewReadyListener = MapViewReadyListener<MapView> {
        CoroutineScope(Dispatchers.Main).launch {
            binding.mapView.getCameraController()?.position =
                Camera.Position.Builder().setLocation(location).build()
            binding.mapView.getVehicleController()?.setLocation(location)
        }
    }

    /** Show the region for the shape */
    private fun showRegion(coordinates: List<LatLon>) {
        val region = Camera.Region().apply {
            northLatitude = getMostNorthLatitude(coordinates)
            southLatitude = getMostSouthLatitude(coordinates)
            eastLongitude = getMostEastLongitude(coordinates)
            westLongitude = getMostWestLongitude(coordinates)
        }
        binding.mapView.getCameraController()
            ?.showRegion(region, Margins.Pixels(MAP_MARGIN_PIXELS, MAP_MARGIN_PIXELS))
    }

    /**
     * the initialize function must be called after SDK is initialized
     */
    private fun mapViewInit() {
        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener = mapViewReadyListener
        )
        binding.mapView.initialize(mapViewConfig)
    }

    private fun setBearing(
        cameraController: CameraController?,
        createCameraPosition: (Float) -> Camera.Position
    ) {
        createCameraPosition(CVPPositionOnRoadViewModel.BEAR).let { cameraPosition ->
            cameraController?.position = cameraPosition
        }
    }

    private fun getMostNorthLatitude(cords: List<LatLon>): Double {
        var latitude = LAT_SOUTH_POLE // Starting with South Pole
        cords.forEach {
            latitude = max(it.lat, latitude)
        }
        return latitude
    }

    private fun getMostSouthLatitude(cords: List<LatLon>): Double {
        var latitude = LAT_NORTH_POLE // Starting with North Pole
        cords.forEach {
            latitude = min(it.lat, latitude)
        }
        return latitude
    }

    private fun getMostWestLongitude(cords: List<LatLon>): Double {
        var longitude = LON_EAST_PACIFIC // Starting from East on mid-Pacific Ocean Meridian
        cords.forEach {
            longitude = min(it.lon, longitude)
        }
        return longitude
    }

    private fun getMostEastLongitude(cords: List<LatLon>): Double {
        var longitude = LON_WEST_PACIFIC // Starting from West on mid-Pacific Ocean Meridian
        cords.forEach {
            longitude = max(it.lon, longitude)
        }
        return longitude
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    companion object {
        fun newInstance() = MapViewShapeFragment()
        private const val WORLD_QUAD_SHAPE_STYLE = "world-quad"
        private const val TEXTURE_RESOURCE_NAME = "cvp.png"
        private const val CUSTOM_POLYGON_SHAPE_STYLE = "custom-polygon"
        private const val RR_DYNAMIC_POLYGON_SHAPE_STYLE = "real-reach-dynamic-polygon"
        private const val REAL_REACH_POLY_SHAPE_STYLE = "real-reach-poly"
        private const val ROUTE_TRACE_SHAPE_STYLE = "route.trace"
        private const val CLIENT_PNG_IMAGE_NAME = "polygon_cvp.png"

        // constants for centring complex polygon
        private const val LAT_SOUTH_POLE = -90.0
        private const val LAT_NORTH_POLE = 90.0
        private const val LON_EAST_PACIFIC = 180.0
        private const val LON_WEST_PACIFIC = -180.0

        private const val MAP_MARGIN_PIXELS = 200.0
    }

}