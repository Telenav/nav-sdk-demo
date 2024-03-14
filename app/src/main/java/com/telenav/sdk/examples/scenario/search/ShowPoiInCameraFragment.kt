/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.search

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.*
import com.telenav.map.api.Annotation
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.controllers.VehicleController
import com.telenav.map.api.touch.TouchType
import com.telenav.sdk.common.model.DayNightMode
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.search.BBox
import com.telenav.sdk.entity.model.search.BBoxGeoFilter
import com.telenav.sdk.entity.model.search.EntitySearchResponse
import com.telenav.sdk.entity.model.search.SearchFilters
import com.telenav.sdk.examples.util.BitmapUtils
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.FragmentShowPoiInCameraBinding
import com.telenav.sdk.map.SDK
import kotlinx.coroutines.*

/**
 * This fragment shows how to show poi in the camera
 */
class ShowPoiInCameraFragment : Fragment() {

    companion object {
        /**
         * INDEX, NAV_LONGITUDE, NAV_LATITUDE is key of bundle
         */
        const val NAV_LONGITUDE = "longitude"
        const val NAV_LATITUDE = "latitude"
        const val SEARCH_TEXT = "gas"
        const val ID = "id"

        /**
         * If move the camera larger than this distance, the POI should updated. Unit is meter
         */
        const val CAMERA_MIN_DISTANCE = 1000.0

        const val SEARCH_DELAY_MILLISECOND = 500L

        /**
         * The scope of search
         */
        const val CAMERA_LONGITUDE_SCOPE = 0.025
        const val CAMERA_LATITUDE_SCOPE = 0.025
    }

    private val viewModel: SearchNavViewModel by viewModels()
    private var destinationAnnotation: Annotation? = null
    private var vehicleController: VehicleController? = null

    private val poiRes = R.drawable.baseline_local_gas_station_24
    private lateinit var poiBitmap: Bitmap

    private var lastSearchedLocation: Location? = null
    private var lastLocation: Location = Location("demo")

    private val annotationList = ArrayList<Annotation>()
    private var searchJob: Deferred<EntitySearchResponse?>? = null
    private var lastMovingTime = 0L

    private var _binding: FragmentShowPoiInCameraBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShowPoiInCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.actionBar.tvTitle.text = getString(R.string.title_activity_show_poi_in_camera)
        binding.actionBar.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
        mapViewInit(savedInstanceState)
        initObserver()
        initOperation()
        poiBitmap = BitmapUtils.getBitmapFromVectorDrawable(requireContext(), poiRes)
            ?: BitmapFactory.decodeResource(resources, R.drawable.map_pin_green_icon_unfocused)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    private val mapViewReadyListener = MapViewReadyListener<MapView> {
        vehicleController = binding.mapView.getVehicleController()
    }

    private fun mapViewInit(savedInstanceState: Bundle?) {
        SDK.getInstance().updateDayNightMode(DayNightMode.DAY)

        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener = mapViewReadyListener
        )
        binding.mapView.initialize(mapViewConfig)

        binding.mapView.setOnTouchListener { touchType: TouchType, position ->
            if (viewModel.isNavigationOn()) {
                return@setOnTouchListener
            }
            if (touchType == TouchType.LongClick) {
                binding.mapView.routesController().clear()
                val location = position.geoLocation
                location?.let {
                    activity?.runOnUiThread {
                        // Set annotation at location
                        val context = context
                        if (context != null) {
                            val factory = binding.mapView.annotationsController().factory()
                            val annotation = factory.create(
                                context,
                                R.drawable.map_pin_green_icon_unfocused,
                                location
                            )
                            annotation.displayText =
                                Annotation.TextDisplayInfo.Centered("Destination")
                            binding.mapView.annotationsController().clear()
                            binding.mapView.annotationsController().add(arrayListOf(annotation))
                        }
                    }

                    viewModel.currentVehicleLocation.value?.let {
                        viewModel.requestDirection(it, location)
                    }
                }
            }
        }

        binding.mapView.addMapViewListener { mapViewData ->
            mapViewData.cameraLocation.let {
                if (lastSearchedLocation == null) {
                    lastSearchedLocation = it
                    startSearch(it)
                } else {
                    if (!it.longitude.equals(lastLocation.longitude) || !it.latitude.equals(
                            lastLocation.latitude
                        )
                    ) {
                        lastMovingTime = SystemClock.elapsedRealtime()
                    }
                    if (it.distanceTo(lastSearchedLocation!!) > CAMERA_MIN_DISTANCE) {
                        if (!binding.layoutProgress.isVisible) {
                            binding.layoutProgress.post {
                                binding.layoutProgress?.visibility = View.VISIBLE
                            }
                        }
                        if (SystemClock.elapsedRealtime() - lastMovingTime > SEARCH_DELAY_MILLISECOND) {
                            startSearch(it)
                            lastSearchedLocation = it
                        }
                    }
                }
                lastLocation = it
            }
        }
    }

    private fun initOperation() {
        binding.btnStartNav.setOnClickListener {
            viewModel.startNavigation(binding.mapView)
            binding.btnStartNav.isEnabled = false
            binding.btnStopNav.isEnabled = true
        }

        binding.btnStopNav.setOnClickListener {
            viewModel.stopNavigation(binding.mapView)
            destinationAnnotation?.let {
                binding.mapView.annotationsController().remove(mutableListOf(it))
            }
            hidePoi()
            binding.btnStartNav.isEnabled = false
            binding.btnStopNav.isEnabled = false
        }

        binding.ivFix.setOnClickListener {
            binding.mapView.cameraController().position =
                Camera.Position.Builder().setLocation(viewModel.currentVehicleLocation.value)
                    .build()
        }
    }

    private fun initObserver() {
        viewModel.currentVehicleLocation.observe(viewLifecycleOwner) {
            vehicleController?.setLocation(it)
        }

        viewModel.route.observe(viewLifecycleOwner) { route ->
            binding.mapView.routesController().clear()// clear old route
            if (route != null) {
                val routeIds = binding.mapView.routesController().add(listOf(route))
                binding.mapView.routesController().highlight(routeIds[0])
                val region = binding.mapView.routesController().region(routeIds)
                binding.mapView.cameraController().showRegion(region, Margins.Percentages(0.20, 0.20))
            }

            binding.btnStartNav.isEnabled = route != null
            binding.btnStopNav.isEnabled = false
        }
    }

    /**
     * Show POI
     */
    private fun showPoi(searchResult: List<SearchResultModel>) {
        for (item in searchResult) {
            val location = item.geoLocation
            val annotation = binding.mapView.annotationsController().factory()
                .create(requireContext(), Annotation.UserGraphic(poiBitmap), location)
            annotation.extraInfo = Bundle().apply {
                this.putDouble(NAV_LONGITUDE, item.navLocation.longitude)
                this.putDouble(NAV_LATITUDE, item.navLocation.latitude)
                this.putString(ID, item.id)
            }
            annotation.displayText = Annotation.TextDisplayInfo.Centered(item.name)
            annotation.style = Annotation.Style.ScreenAnnotationPopup
            annotationList.add(annotation)
        }
        binding.mapView.annotationsController().add(annotationList)
    }

    /**
     * Hide POI
     */
    private fun hidePoi() {
        binding.mapView.annotationsController().remove(annotationList)
        annotationList.clear()
    }

    private fun startSearch(center: Location) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.layoutProgress.visibility = View.VISIBLE
            if (searchJob != null && !searchJob!!.isActive) {
                searchJob?.cancelAndJoin()
            }
            searchJob = search(center)
            searchJob?.await()?.let {
                onSearchSuccess(it)
            }
            binding.layoutProgress.visibility = View.GONE
        }
    }

    private fun CoroutineScope.search(center: Location, text: String = SEARCH_TEXT) =
        async(Dispatchers.Default) {
            val bottomLeftLocation =
                getLocation(center, -CAMERA_LATITUDE_SCOPE, -CAMERA_LONGITUDE_SCOPE)
            val topRightLocation =
                getLocation(center, CAMERA_LATITUDE_SCOPE, CAMERA_LONGITUDE_SCOPE)
            val bBox = BBox
                .builder()
                .setBottomLeft(bottomLeftLocation.latitude, bottomLeftLocation.longitude)
                .setTopRight(topRightLocation.latitude, topRightLocation.longitude)
                .build()
            val geoFilter = BBoxGeoFilter
                .builder(bBox)
                .build()
            val geoSearchFilters = SearchFilters
                .builder()
                .setGeoFilter(geoFilter)
                .build()
            val entityClient = EntityService.getClient()
            try {
                return@async entityClient.searchRequest()
                    .setQuery(text)
                    .setFilters(geoSearchFilters)
                    .setLocation(center.latitude, center.longitude)
                    .execute()
            } catch (e: Throwable) {
                e.printStackTrace()
                return@async null
            }
        }

    private fun onSearchSuccess(response: EntitySearchResponse) {
        activity?.let {
            val searchResult = ArrayList<SearchResultModel>()
            searchResult.addAll(response.results?.mapNotNull {
                SearchResultModel.wrapperFromEntity(it)
            } ?: emptyList())
            hidePoi()
            showPoi(searchResult)
        }
    }

    private fun getLocation(location: Location, offsetLat: Double, offsetLon: Double): Location {
        val lat = (location.latitude + offsetLat).coerceAtLeast(-90.0).coerceAtMost(90.0)
        var lon = location.longitude + offsetLon
        if (lon < -180) {
            lon += 360
        }
        if (lon > 180) {
            lon -= 360
        }
        return Location(location.provider).apply {
            this.latitude = lat
            this.longitude = lon
        }
    }


}