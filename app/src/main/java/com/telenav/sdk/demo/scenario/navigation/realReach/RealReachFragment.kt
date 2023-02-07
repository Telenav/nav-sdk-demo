/*
 *  Copyright © 2022 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */
package com.telenav.sdk.demo.scenario.navigation.realReach

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import com.telenav.map.api.MapView
import com.telenav.map.api.Margins
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.controllers.ShapesController
import com.telenav.map.geo.Attributes
import com.telenav.map.geo.Shape
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.demo.scenario.navigation.BaseNavFragment
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.RealReachFragmentBinding
import com.telenav.sdk.map.direction.model.GeoLocation
import kotlinx.android.synthetic.main.content_basic_navigation.*
import kotlinx.android.synthetic.main.real_reach_fragment.*
import kotlin.math.max
import kotlin.math.min

/**
 * This fragment is used to show real reach
 * @author wu.changzhong on 2022/3/23
 */
class RealReachFragment : BaseNavFragment() {

    companion object {
        private const val LAT_SOUTH_POLE = -90.0
        private const val LAT_NORTH_POLE = 90.0
        private const val LON_EAST_PACIFIC = 180.0
        private const val LON_WEST_PACIFIC = -180.0
        private const val MAP_MARGIN_PIXELS = 200.0
    }

    private val viewModel: RealReachViewModel by activityViewModels()
    private var testShapeId: ShapesController.Id? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: RealReachFragmentBinding =
            DataBindingUtil.inflate(inflater, R.layout.real_reach_fragment, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navButton.visibility = View.GONE
        tv_tip.text = "Long Press to choose an origin point"
        btInputDistance.setOnClickListener {
            val fragment = SetDistanceDialogFragment()
            fragment.show(childFragmentManager, "distance")
        }
        selectRequestMode.setOnClickListener {
            val fragment = SelectRequestModeDialogFragment()
            fragment.show(childFragmentManager, "requestMode")
        }
        requestRealReach.setOnClickListener {
            layoutProgress.visibility= View.VISIBLE
            viewModel.requestIsochrone()
        }
        viewModel.isochronePoints.observe(owner = viewLifecycleOwner) {
            layoutProgress.visibility= View.GONE
            if (testShapeId != null) {
                map_view.shapesController().remove(testShapeId!!)
                testShapeId = null
            }
            if (it != null && it.isNotEmpty()) {
                drawRealReach(map_view, it)
            }
        }
    }

    override fun onLongClick(location: Location?) {
        setVehicleNewLocation(location)
    }

    private fun setVehicleNewLocation(location: Location?) {
        if (location != null) {
            locationProvider.setLocation(location)
        }
    }

    override fun onLocationUpdated(vehicleLocation: Location) {
        super.onLocationUpdated(vehicleLocation)
        viewModel.origin.postValue(GeoLocation(vehicleLocation))
    }

    @SuppressLint("SuspiciousIndentation")
    private fun drawRealReach(mapView: MapView, points: List<LatLon>) {

        if (testShapeId == null) {

            val attributes = Attributes.Builder()
                .setShapeStyle("route.CLOSED_EDGE")
                .setColor(0xFF00FF00.toInt())
                .setLineWidth(100.0f)
                .build()

            val shape = Shape(Shape.Type.Polyline, attributes, points)

            val collectionBuilder = Shape.Collection.Builder()
            collectionBuilder.addShape(shape)

            testShapeId = mapView.shapesController().add(collectionBuilder.build())
            if (testShapeId != null)
                mapView.shapesController().setAlphaValue(testShapeId!!, 0.5f)
                showRegion(points)
        } else {
            mapView.shapesController().remove(testShapeId!!)
            testShapeId = null
        }
    }

    /** Show the region for the shape with unknown amount of lat-lon points */
    private fun showRegion(cords: List<LatLon>) {
        val region = Camera.Region().apply {
            northLatitude = getMostNorthLatitude(cords)
            southLatitude = getMostSouthLatitude(cords)
            eastLongitude = getMostEastLongitude(cords)
            westLongitude = getMostWestLongitude(cords)
        }
        map_view.cameraController().showRegion(region, Margins.Pixels(
            MAP_MARGIN_PIXELS, MAP_MARGIN_PIXELS
        ))
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
}