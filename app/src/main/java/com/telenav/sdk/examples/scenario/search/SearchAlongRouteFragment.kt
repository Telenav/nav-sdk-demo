/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.search

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.*
import com.telenav.map.api.Annotation
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.controllers.VehicleController
import com.telenav.map.api.touch.TouchType
import com.telenav.sdk.common.model.DayNightMode
import com.telenav.sdk.core.Callback
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.base.GeoPoint
import com.telenav.sdk.entity.model.search.CorridorGeoFilter
import com.telenav.sdk.entity.model.search.EntitySearchResponse
import com.telenav.sdk.entity.model.search.SearchFilters
import com.telenav.sdk.examples.util.BitmapUtils
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.FragmentSearchAlongRouteBinding
import com.telenav.sdk.map.SDK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * This fragment shows how to search along route
 */
class SearchAlongRouteFragment : Fragment() {

    companion object {
        /**
         * INDEX, NAV_LONGITUDE, NAV_LATITUDE is key of bundle
         */
        const val NAV_LONGITUDE = "longitude"
        const val NAV_LATITUDE = "latitude"
        const val SEARCH_TEXT = "gas"

        /**
         * Maximum search distance
         */
        const val MAX_LENGTH = 50000
    }

    private val viewModel: SearchNavViewModel by viewModels()
    private var destinationAnnotation: Annotation? = null
    private var vehicleController: VehicleController? = null

    private val poiRes = R.drawable.baseline_local_gas_station_24
    private lateinit var poiBitmap: Bitmap

    private val searchResult = ArrayList<SearchResultModel>()
    private val annotationList = ArrayList<Annotation>()

    private var _binding: FragmentSearchAlongRouteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchAlongRouteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.actionBar.tvTitle.text = getString(R.string.title_activity_search_along_route)
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
                searchResult.clear()

                val geoList = ArrayList<GeoPoint>()
                var length = 0f
                route.routeLegList?.flatMap {
                    it.routeStepList ?: emptyList()
                }?.flatMap {
                    it.routeEdgeList ?: emptyList()
                }?.flatMap {
                    length += it.length
                    if (length <= MAX_LENGTH) {
                        return@flatMap it.getEdgeShapePoints() ?: emptyList()
                    } else {
                        return@flatMap emptyList()
                    }
                }?.forEach {
                    geoList.add(GeoPoint(it.lat, it.lon))
                }
                search(geoList)
            }

            binding.btnStartNav.isEnabled = route != null
            binding.btnStopNav.isEnabled = false
        }
    }

    /**
     * Show POI
     */
    private fun showPoi() {
        val list = searchResult
        for (item in list) {
            val location = item.geoLocation
            val annotation = binding.mapView.annotationsController().factory()
                .create(requireContext(), Annotation.UserGraphic(poiBitmap), location)
            annotation.extraInfo = Bundle().apply {
                this.putDouble(NAV_LONGITUDE, item.navLocation.longitude)
                this.putDouble(NAV_LATITUDE, item.navLocation.latitude)
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

    /**
     * Do search
     */
    private fun search(geoList: List<GeoPoint>, text: String = SEARCH_TEXT) {
        CoroutineScope(Dispatchers.Default).launch {
            val currentLocation = viewModel.initLocation
            val entityClient = EntityService.getClient()

            val filter = SearchFilters.builder().setGeoFilter(
                CorridorGeoFilter.builder().setRoute(geoList).setRouteWidth(1000.0).build()
            ).build()
            entityClient.searchRequest()
                .setQuery(text)
                .setLocation(currentLocation.latitude, currentLocation.longitude)
                .setFilters(filter)
                .asyncCall(object : Callback<EntitySearchResponse> {

                    override fun onSuccess(response: EntitySearchResponse) {
                        onSearchSuccess(response)
                    }

                    override fun onFailure(t: Throwable) {
                        t.printStackTrace()
                        onSearchFail()
                    }
                })
        }
    }

    private fun onSearchSuccess(response: EntitySearchResponse) {
        CoroutineScope(Dispatchers.Main).launch {
            activity?.let {
                searchResult.addAll(response.results?.mapNotNull {
                    SearchResultModel.wrapperFromEntity(it)
                } ?: emptyList())
                showPoi()
            }
        }
    }

    private fun onSearchFail() {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(requireActivity(), "Search failed!", Toast.LENGTH_SHORT).show()
        }
    }


}