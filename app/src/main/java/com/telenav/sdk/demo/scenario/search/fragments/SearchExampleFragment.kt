/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */
package com.telenav.sdk.demo.scenario.search.fragments

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.core.util.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.MapView
import com.telenav.map.api.MapViewReadyListener
import com.telenav.map.api.controllers.AnnotationsController
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.controllers.SearchController
import com.telenav.map.api.controllers.VehicleController
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.examples.R
import com.telenav.sdk.demo.scenario.search.fragments.searchComponents.poiFactory.PoiExampleFactoryUsingStyles
import com.telenav.sdk.demo.scenario.search.fragments.searchComponents.searchEngine.SearchEngineExample
import com.telenav.sdk.demo.scenario.search.fragments.searchComponents.searchEngine.SearchLoading
import kotlinx.android.synthetic.main.layout_action_bar.*
import kotlinx.android.synthetic.main.search_example_fragment.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * @author Mykola Ivantsov - (p)
 */
class SearchExampleFragment : Fragment() {

    companion object {
        fun newInstance() = SearchExampleFragment()
        private const val TAG = "POI_SEARCH"
    }

    private val viewModel: SearchExampleViewModel by viewModels()
    private var searchEngine: SearchEngineExample? = null
    private val getVehicleLocationObserver = Observer<Location> { newLocation ->
        setLocation(getVehicleController(), newLocation)
        searchEngine?.vehicleLocation = newLocation

    }
    private val ivBackOnClickListener = View.OnClickListener {
        findNavController().navigateUp()
    }
    private val mapViewReadyListener = MapViewReadyListener<MapView> {
        runInMain {
            mapView.cameraController()?.position =
                Camera.Position.Builder().setLocation(viewModel.startLocation).build()
            mapView.vehicleController()?.setLocation(viewModel.startLocation)
        }
    }
    private val searchItemList: Array<String> by lazy {
        resources.getStringArray(R.array.search_item_list)
    }
    private val items = listOf(
        SearchEngineExample.SPECIALTY_FOOD, SearchEngineExample.PARKING_LOT,
        SearchEngineExample.E10
    )
    private val onItemClickListenerImpl =
        AdapterView.OnItemClickListener { parent, view, position, id ->
            val chosen = (parent as (ListView)).checkedItemPositions
            viewModel.put(id.toInt(), items[position])
            chosen.forEach { key, value ->
                if (!value) viewModel.remove(key)
            }
            viewModel.injectPoiAnnotationFactory(
                getSearchController(),
                PoiExampleFactoryUsingStyles(getAnnotationFactory(getAnnotationsController()))
            )
            searchEngine?.let { nonNullSearchEngine ->
                viewModel.injectPoiAnnotationFactory(getSearchController(), nonNullSearchEngine)
            }
            val listOfItem = viewModel.getAllSearchItemList()
            displayPOI(
                getSearchController(),
                //Should specify categories that you want to find
                listOfItem
            )
            printDebugLog("chosenSearchItemList = $listOfItem, chosen = $chosen")
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.search_example_fragment, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_title.text = getString(R.string.title_search_example)
        // the initialize function must be called after SDK is initialized
        mapView.initialize(savedInstanceState, mapViewReadyListener)

        searchEngine = SearchEngineExample(requireContext().applicationContext)
        searchEngine?.vehicleLocation = viewModel.startLocation
        val adapter: ArrayAdapter<String> =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_multiple_choice,
                searchItemList
            )
        item_list_view.apply {
            this.adapter = adapter
            choiceMode = ListView.CHOICE_MODE_MULTIPLE
            onItemClickListener = onItemClickListenerImpl
        }
        iv_back.setOnClickListener(ivBackOnClickListener)

        viewModel.getVehicleLocationLiveData()
            .observe(viewLifecycleOwner, getVehicleLocationObserver)
        lifecycleScope.launchWhenResumed {
            SearchEngineExample.uiState.collect {
                when (it) {
                    is SearchLoading.NotInit -> {
                        //do nothing
                    }
                    is SearchLoading.Loading -> {
                        progress_search.visibility = View.VISIBLE
                        printDebugLog("SearchLoading.Loading")
                    }
                    is SearchLoading.Success -> {
                        progress_search.visibility = View.GONE
                        printDebugLog("SearchLoading.Success")
                    }
                    is SearchLoading.Fail -> {
                        progress_search.visibility = View.GONE
                        showToast(it.msg)
                        printDebugLog("SearchLoading.Fail")
                    }
                }
            }
        }

    }

    private fun displayPOI(searchController: SearchController, displayContent: List<String>) {
        searchController.displayPOI(displayContent)
    }

    private fun getSearchController() = mapView.searchController()
    private fun getVehicleController() = mapView.vehicleController()
    private fun getAnnotationsController() = mapView.annotationsController()
    private fun getAnnotationFactory(annotationsController: AnnotationsController) =
        annotationsController.factory()

    private fun setLocation(vehicleController: VehicleController, location: Location) {
        vehicleController.setLocation(location)
    }

    private fun showToast(msg: String) {
        Toast.makeText(
            requireContext(),
            msg,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun runInMain(run: () -> Unit): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            run()
        }
    }

    private fun printDebugLog(msg: String) {
        TaLog.d(TAG, "msg: $msg | Thread.name: ${Thread.currentThread().name}")
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