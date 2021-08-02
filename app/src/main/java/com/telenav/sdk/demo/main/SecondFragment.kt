/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.main

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.telenav.map.api.*
import com.telenav.map.api.Annotation
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.controllers.SearchController
import com.telenav.map.api.controllers.ShapesController
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.map.elements.MapElement
import com.telenav.map.elements.TrafficInfo
import com.telenav.map.geo.Attributes
import com.telenav.map.geo.Shape
import com.telenav.sdk.common.internal.SdkUserSettingInternal
import com.telenav.sdk.common.internal.SdkUserSettingInternal.ColorTheme
import com.telenav.sdk.common.model.DayNightMode
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.common.model.UnitType
import com.telenav.sdk.demo.SearchResultItemDao
import com.telenav.sdk.demo.SharedSearchLocationViewModel
import com.telenav.sdk.demo.provider.DemoLocationProvider
import com.telenav.sdk.demo.seekbar.ProgressSeekBar
import com.telenav.sdk.demo.util.BitmapUtils
import com.telenav.sdk.demo.util.NavPoiFactory
import com.telenav.sdk.demo.util.NavSearchEngine
import com.telenav.sdk.demo.util.NavSearchEngine.Companion.SEARCH_FOOD
import com.telenav.sdk.examples.*
import com.telenav.sdk.examples.main.CandidateRoadDialogFragment
import com.telenav.sdk.examples.main.SetVehicleHeadingDialogFragment
import com.telenav.sdk.examples.main.SetVehicleHeadingViewModel
import com.telenav.sdk.map.SDK
import com.telenav.sdk.map.direction.model.RequestMode
import com.telenav.sdk.map.direction.model.Route
import com.telenav.sdk.ui.turn.TnTurnListRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_second.*
import kotlinx.android.synthetic.main.layout_operation_second.*
import kotlinx.android.synthetic.main.layout_operation_tune_mode.*
import kotlinx.android.synthetic.main.layout_second_content.*
import kotlinx.android.synthetic.main.layout_second_content.navButton
import kotlinx.android.synthetic.main.layout_second_content.subViewButton
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {
    private val TAG = "SecondFragment"

    private lateinit var routeIds: List<String>
    private var updateRouteProgress = true
    private val turnListAdapter: TnTurnListRecyclerViewAdapter =
            TnTurnListRecyclerViewAdapter()

    private lateinit var viewModel: SecondViewModel
    private lateinit var candidateRoadViewModel: CandidateRoadViewModel
    private lateinit var setVehicleHeadingViewModel: SetVehicleHeadingViewModel
    private var candidateRoadDialogFragment: CandidateRoadDialogFragment? = null
    private lateinit var navViewModel: NavSessionViewModel
    private val searchLocationViewModel: SharedSearchLocationViewModel by activityViewModels()

    private var highlightedRoute: String? = null
    private var destinationLocation: Location? = null
    private var wayPointList = mutableListOf<Location>()

    private var locations: List<Location> = ArrayList()
    private var startFreeDrive = false
    private var job: Job? = null
    private var destroy = false

    private var subView: MapSubView? = null
    private val routeAnnotations = ArrayList<Annotation>()
    private var searchController: SearchController? = null
    private var searchEngine: NavSearchEngine? = null
    private var trafficInfoAnnotation: Annotation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        SDK.getInstance().updateUnit(UnitType.IMPERIAL)
        viewModel = SecondViewModel()
        navViewModel = NavSessionViewModel(turnListAdapter, this)

//        TaLog.enableLogs(false)
//        LoggingHelperJni.setLevelJni(NavLogLevelType.VERBOSE)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            findNavController().navigateUp()
            return true
        }
        if (item.itemId == R.id.action_settings) {
            drawer_layout.open()
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        job = CoroutineScope(Dispatchers.IO).launch {
            loadgps()
        }
    }

    private suspend fun loadgps() {
        val gpsfile = getResources().openRawResource(R.raw.gps2)
        val reader = BufferedReader(
                InputStreamReader(gpsfile))
        var str: String? = null
        while (true) {
            str = reader.readLine()
            if (str != null) {
                val scanner = Scanner(str).useDelimiter(",");
                var str = scanner.next()
                var lat = str.toDouble()
                str = scanner.next()
                val lon = str.toDouble()
                str = scanner.next()
                val speed = str.toFloat()
                str = scanner.next()
                val bearing = str.toFloat()
                var location = Location("test")
                location.latitude = lat
                location.longitude = lon
                location.speed = speed
                location.bearing = bearing

                locations += location
            } else {
                break
            }
        }
        gpsfile.close()
        var index = 0;
        while (!destroy) {
            if (startFreeDrive) {
                if (index >= locations.size) {
                    index = 0
                }
                navViewModel.setCurrentLocation(locations[index])
                index++
                delay(250)

            } else {
                delay(1000)
            }
        }
        Log.i(TAG, "destroy the launch")
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val binding: com.telenav.sdk.examples.databinding.FragmentSecondBinding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_second, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.navViewModel = navViewModel
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDrawerOperation()

        subViewButton.visibility = View.VISIBLE

        candidateRoadViewModel =
                ViewModelProvider(requireActivity(), ViewModelProvider.NewInstanceFactory()).get(
                        CandidateRoadViewModel::class.java
                )
        setVehicleHeadingViewModel = ViewModelProvider(requireActivity(), ViewModelProvider.NewInstanceFactory()).get(
                SetVehicleHeadingViewModel::class.java
        )

        map_view.initialize(savedInstanceState, {
            // @TODO: For some reason without this it defaults to 3d render mode, but previously it defaulted to 2D mode
            resetMapCamera()
            configureMapView()
            // Setup observation of viewModel's liveData
            activity?.runOnUiThread {
                setupObservers()
            }
        })

        // Setup on click listeners for buttons
        setupButtons()
        // Receive map press events
        setOnTouchFunction()
        // Receive Annotation press events
        setOnTouchAnnotationFunctions()
        // Receive route press events
        setOnTouchRouteFunction()
        // Receive map element press events
        setOnTouchMapElementsFunction()

        map_view.addMapViewListener { mapViewData ->
            viewModel.zoomLevelLiveData.postValue(
                    String.format(
                            "ZoomLevel: %.3f \nRangeHorizontal: %.3f\n LatLon:(%.3f, %.3f)",
                            mapViewData.zoomLevel,
                            mapViewData.rangeHorizontal,
                            mapViewData.cameraLocation.latitude,
                            mapViewData.cameraLocation.longitude
                    )
            )
        }

        turn_direction_recycler_view.setTurnListAdapter(turnListAdapter)

        trafficBar.setOrientation(true)
        searchEditText.requestFocus()
        searchEditText.setOnClickListener({
            navViewModel.stopNavigation()
            // TODO: use user selected route & clear route arrow.

            activity?.runOnUiThread {
                junction_view.hideImage()
                routeIds = emptyList()
                viewModel.routes.value = emptyList()
                map_view.annotationsController().clear()
                map_view.routesController().clear()
                navButton.isEnabled = false
                navViewModel.navigationOn.value = false
            }
            navViewModel.disableCameraFollow()
            val bundle = Bundle()
            bundle.putDouble("lat", navViewModel.getCurrentLocation().latitude)
            bundle.putDouble("lng", navViewModel.getCurrentLocation().longitude)
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment, bundle)

        })
    }

    override fun onPause() {
        super.onPause()
        map_view.onPause()
    }

    override fun onResume() {
        super.onResume()
        map_view.onResume()
    }

    override fun onDestroy() {
        destroy = true
        navViewModel.stopTTS()
        navViewModel.shutdown()
        super.onDestroy()
    }

    private fun setupButtons() {

        navButton.isEnabled = false
        navButton.setOnClickListener {
            viewModel.routes.value?.let {

                if (it.isEmpty()) {
                    return@let
                }

                val navigating = navViewModel.navigationOn.value as Boolean
                if (navigating) {
                    stopNavigating()
                    navButton.setText(R.string.start_navigation)
                } else {
                    startNavigating(it)
                    navButton.setText(R.string.stop_navigation)
                }
            }
        }

        // @TODO: Subview demo, move to a "scenario"?
        var subViewTick = 0
        subViewButton.setOnClickListener {

            val current = subViewTick++ % 3
            when (current) {
                // Create the subView
                0 -> {
                    val dim = 600
                    subView = map_view.createSubView(0.0, 0.0, dim, dim, {
                        // Configure subView
                    })
                }
                // Resize or move subView
                1 -> {
                    val dim = 300
                    subView?.resizeView(dim.toDouble(), dim.toDouble(), dim, dim)
                }
                // Close/delete subView
                2 -> {
                    subView?.deleteView()
                    subView = null
                }
            }

        }

        cameraFollowModeButton.setOnClickListener {
            navViewModel.changeFollowVehicleMode()
        }

        cameraRenderModeButton.setOnClickListener {
            if (map_view.cameraController().renderMode == Camera.RenderMode.M3D) {
                map_view.cameraController().renderMode = Camera.RenderMode.M2D
                cameraRenderModeButton.setText("2D")
            } else { // renderMode == Camera.RenderMode.M2D
                map_view.cameraController().renderMode = Camera.RenderMode.M3D
                cameraRenderModeButton.setText("3D")
            }
        }

        cameraResetButton.setOnClickListener {
            resetMapCamera()
        }

        iv_camera_fix.setOnClickListener {
            val newPosition = Camera.Position.Builder()
                    .setLocation(navViewModel.vehicleLocation.value!!)
                    .build()
            map_view.cameraController().position = newPosition
            iv_camera_fix.setImageResource(R.drawable.ic_gps_fixed_24)
        }

        iv_show_roads.setOnClickListener {
            navViewModel.showAllCandidateRoads()
        }

        tv_request_mode.setOnClickListener {
            val newMode = viewModel.switchRequestMode()
            val imageRes = if (newMode == RequestMode.EMBEDDED_ONLY) {
                R.drawable.ic_cloud_off_24
            } else {
                R.drawable.ic_cloud_24
            }

            tv_request_mode.setImageResource(imageRes)
        }
        val ss: ProgressSeekBar = seekBar
        ss.onProgressChangedListener = object : ProgressSeekBar.OnProgressChangedListener {
            override fun onProgressChanged(
                    progressSeekBar: ProgressSeekBar?,
                    progress: Int,
                    progressFloat: Float,
                    fromUser: Boolean
            ) {
                navViewModel.updateNavigationSpeed(progress.toDouble())
            }

            override fun getProgressOnActionUp(
                    progressSeekBar: ProgressSeekBar?,
                    progress: Int,
                    progressFloat: Float
            ) {
                Log.i(TAG, "getProgressOnActionUp")
            }

            override fun getProgressOnFinally(
                    progressSeekBar: ProgressSeekBar?,
                    progress: Int,
                    progressFloat: Float,
                    fromUser: Boolean
            ) {
                Log.i(TAG, "getProgressOnFinally")
            }

        }
    }

    private fun setupObservers() {

        navViewModel.vehicleLocation.observe(owner = viewLifecycleOwner) {
            map_view.vehicleController().setLocation(it)
            searchEngine?.vehicleLocation = it
        }

        viewModel.routes.observe(owner = viewLifecycleOwner) {
            map_view.routesController().clear()// clear old route
            if (it != null && it.isNotEmpty()) {
                routeIds = map_view.routesController().add(it)
                highlightedRoute = routeIds[0]
                map_view.routesController().highlight(highlightedRoute!!)
                showRouteAnnotation(routeIds[0])
                val region = map_view.routesController().region(routeIds)
                map_view.cameraController().showRegion(region, Margins.Percentages(0.20, 0.20))
                navButton.isEnabled = true
            }
        }

        viewModel.isochronePoints.observe(owner = viewLifecycleOwner) {
            if (testShapeId != null) {
                map_view.shapesController().remove(testShapeId!!)
                testShapeId = null
            }
            if (it != null && it.isNotEmpty()) {
                testShapeApi(map_view, it)
            }
        }

        navViewModel.showNavigationDetails.observe(owner = viewLifecycleOwner) {
            if (!it) {//reach destination
                destinationLocation = null
                wayPointList.clear()
                routeIds = emptyList()
                viewModel.routes.value = emptyList()
                clearRouteAnnotation()
                map_view.annotationsController().clear()
                map_view.routesController().clear()
                map_view.cameraController().disableFollowVehicle()
                activity?.runOnUiThread {
                    navButton.isEnabled = false
                    navButton.setText(R.string.start_navigation)
                }
            }
        }

        navViewModel.junctionBitmap.observe(owner = viewLifecycleOwner) {
            if (it == null) {
                junction_view.hideImage()
            } else {
                junction_view.showImage(it)
            }
        }

        navViewModel.currentFollowVehicleMode.observe(owner = viewLifecycleOwner) {
            if (it == null) {
                map_view.cameraController().disableFollowVehicle()
            } else {
                map_view.cameraController().enableFollowVehicleMode(it, sw_use_auto_zoom.isChecked)
                cameraFollowModeButton.visibility = View.VISIBLE
                cameraFollowModeButton.text = it.name
            }
        }

        navViewModel.candidateRoadsLiveData.observe(owner = viewLifecycleOwner) {
            if (it != null) {
                if (it.isEmpty()) {
                    Toast.makeText(activity, "No candidates", Toast.LENGTH_SHORT).show()
                    hideRoadsDialog()
                } else {
                    showRoadsDialog()
                    candidateRoadViewModel.candidateRoads.postValue(it)
                }
            }
        }

        navViewModel.toast.observe(owner = viewLifecycleOwner) {
            Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
        }

        candidateRoadViewModel.selectedRoad.observe(owner = viewLifecycleOwner) {
            navViewModel.selectCandidateRoad(it)
        }

        navViewModel.betterRouteLiveData.observe(owner = viewLifecycleOwner) {
            Log.i(TAG, "betterRoute unique id: " + it!!.id)
            if (it.id != highlightedRoute) {
                highlightedRoute?.let { it1 -> map_view.routesController().remove(it1) }
            }
            map_view.routesController().refresh(it)
            if (it.id != highlightedRoute) {
                highlightedRoute = it.id
                highlightedRoute?.let { it2 ->
                    map_view.routesController().updateRouteProgress(it2)
                }
            }
        }

        searchLocationViewModel.mutableSelectedLocation.observe(viewLifecycleOwner) {
            setDestinationAndRequestRoute(it)
            val location = Location("")
//            searchLocationViewModel.mutableSelectedLocation.postValue(SearchResultItemDao(Location(""),"",0.0))

        }

        setVehicleHeadingViewModel.headingAngle.observe(owner = viewLifecycleOwner) {
            val location = navViewModel.vehicleLocation.value.apply {
                this?.bearing = it
                this?.time = Calendar.getInstance().timeInMillis
            }
            if (location != null) {
                navViewModel.setCurrentLocation(location)
            }
        }

        navViewModel.alongRouteTrafficLiveData.observe(owner = viewLifecycleOwner) {
            trafficBar.updateTrafficInfo(it)
        }

        navViewModel.traveledDistance.observe(owner = viewLifecycleOwner) {
            trafficBar.updateTraveledDistance(it.toFloat())
        }
    }

    private fun setOnTouchFunction() {
        map_view.setOnTouchListener { touchType: TouchType, data: TouchPosition ->
            when (touchType) {

                TouchType.Down, TouchType.Up, TouchType.Click, TouchType.Move, TouchType.Cancel -> {
                    Log.i(
                            "TOUCH_TAG", "Touch type ${touchType}, " +
                            "geoLocation latitude: ${data.geoLocation?.latitude} longitude: ${data.geoLocation?.longitude}"
                    )
                }
                TouchType.LongClick -> {
                    val location = data.geoLocation
                    if (viewModel.isSettingLocation) {
                        setVehicleLocation(location)
                    } else if (!navViewModel.navigationOn.value!!) {
                        val searchResultItemDao = location?.let { SearchResultItemDao(it, "destination", 0.0) }
                        searchResultItemDao?.let { setDestinationAndRequestRoute(it) }
                    }
                }
            }
        }

        map_view.setOnViewTouchListener { event: MotionEvent ->
            Log.i("TOUCH_TAG", "event action ${event.action} ${event.x} ${event.y}")
            false
        }
    }

    private fun setOnTouchAnnotationFunctions() {
        map_view.setOnAnnotationTouchListener() { touchType, data, touchedAnnotations ->
            if (touchType == TouchType.Click) {
                if (touchedAnnotations[0].annotation == trafficInfoAnnotation) {
                    hideTrafficIncidentInfo()
                    return@setOnAnnotationTouchListener
                }

                val vehicleLocation = navViewModel.vehicleLocation.value
                if (touchedAnnotations[0].annotation.displayText?.text?.contains("wayPoint") == true) {
                    activity?.runOnUiThread {
                        AlertDialog.Builder(this.requireContext())
                                .setIcon(R.drawable.add_location)
                                .setTitle("Delete WayPoint")
                                .setPositiveButton("ok") { dialog, whichButton ->
                                    dialog.dismiss()
                                    val iterator = wayPointList.iterator()
                                    if (iterator.hasNext()) {
                                        val location = iterator.next()
                                        if (location.latitude == touchedAnnotations[0].annotation.location.latitude
                                                || location.longitude == touchedAnnotations[0].annotation.location.longitude) {
                                            iterator.remove()
                                        }
                                    }
                                    map_view.annotationsController().remove(arrayListOf(touchedAnnotations[0].annotation))
                                    if (vehicleLocation != null) {
                                        viewModel.requestDirection(vehicleLocation, destinationLocation!!, wayPointList)
                                    }
                                }
                                .setNegativeButton("cancel") { dialog, whichButton ->
                                    dialog.dismiss()
                                }
                                .show()
                    }
                }
            }
        }
    }

    private fun setOnTouchRouteFunction() {
        // Simple tests for all touch types
        map_view.setOnRouteTouchListener { touchType, data, routeID ->
            val geoLocation = data.geoLocation
            Log.i(
                    "ROUTE_TOUCH_TAG", "Getting click type ${touchType}, " +
                    "data ${data} route id ${routeID} " +
                    "geoLocation ${geoLocation?.latitude}, ${geoLocation?.longitude}"
            )
            highlightedRoute = routeID
            map_view.routesController().highlight(highlightedRoute!!)
            showRouteAnnotation(routeID)
        }
    }

    private fun showRouteAnnotation(routeId: String) {
        clearRouteAnnotation()
        val factory = map_view.annotationsController().factory()
        val annotation = factory.createRouteAnnotation(routeId)
        annotation.displayText = Annotation.TextDisplayInfo.Centered("Selected Route")
        routeAnnotations.add(annotation)
        map_view.annotationsController().add(routeAnnotations)
    }

    private fun clearRouteAnnotation() {
        map_view.annotationsController().remove(routeAnnotations)
        routeAnnotations.clear()
    }

    private fun setOnTouchMapElementsFunction() {
        map_view.setOnMapElementTouchListener { touchType, position, elements ->
            Log.i("MAP_ELEMENT_TOUCH_TAG", "Getting touch ${touchType} for list of elements of length ${elements.size} " +
                    "location:[${position.geoLocation?.latitude},${position.geoLocation?.longitude}]")
            for (element in elements) {
                when (element.type) {
                    MapElement.Type.TrafficIncident -> {
                        Log.i("MAP_ELEMENT_TOUCH_TAG", "Traffic Incident")
                        if (element is MapElement.TrafficIncident) {
                            position.geoLocation?.let {
                                showTrafficIncidentInfo(it, element.info)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showTrafficIncidentInfo(location: Location, trafficInfo: TrafficInfo) {
        hideTrafficIncidentInfo()

        val showText = "IncidentType: ${trafficInfo.incidentType}\n" +
                "message: ${trafficInfo.message}\n" +
                "severity: ${trafficInfo.severity}\n" +
                "sourceType: ${trafficInfo.sourceType}\n" +
                "crossStreet: ${trafficInfo.crossStreet}\n" +
                "firstCrossStreet: ${trafficInfo.firstCrossStreet}\n" +
                "secondCrossStreet: ${trafficInfo.secondCrossStreet}"
        val layout: View = LayoutInflater.from(requireActivity()).inflate(R.layout.layout_traffic_incident_info,
                null, false)
        layout.findViewById<TextView>(R.id.tv).text = showText
        val bitmap = BitmapUtils.createBitmapFromView(layout)
        val factory = map_view.annotationsController().factory()
        trafficInfoAnnotation = factory.create(requireContext(), Annotation.UserGraphic(bitmap), location).apply {
            this.style = Annotation.Style.ScreenAnnotationFlagNoCulling
            this.iconY = -0.5
        }

        map_view.annotationsController().add(listOf(trafficInfoAnnotation))
    }

    private fun hideTrafficIncidentInfo() {
        trafficInfoAnnotation?.let {
            map_view.annotationsController().remove(listOf(it))
            trafficInfoAnnotation = null
        }
    }


    private fun setZoomLevel(level: Float) {
        val newLevel =
                min(max(SecondViewModel.MIN_ZOOM_LEVEL, level), SecondViewModel.MAX_ZOOM_LEVEL)
        map_view.cameraController().position =
                Camera.Position.Builder().setZoomLevel(newLevel).build()
    }

    private fun showRoadsDialog() {
        candidateRoadDialogFragment?.dismiss()
        candidateRoadDialogFragment = CandidateRoadDialogFragment()
        candidateRoadDialogFragment?.show(childFragmentManager, "CandidateRoad")
    }

    private fun hideRoadsDialog() {
        candidateRoadDialogFragment?.dismiss()
    }


    private fun initDrawerOperation() {
        initStyleChangeItem()
        initDayNightItem()
        initIsochroneItem()
        initSetLocationItem()
        initTextScaleItem()
        initStartFreeDriveItem()
        initSetVehicleHeadingItem()
        initSetFPSDataItem()
        initSetPrefetchDataItem()
        initZoomSetItem()
        initLocationProviderItem()
        initShowPOIOnMap()
        initShowVersionItem()
        initLocaleItem()
        initSwitchAutoZoomDelegate()
    }

    private fun initStyleChangeItem() {
        var loadTssStyleButtonClicks = 0
        layout_style.setOnClickListener {
            if (loadTssStyleButtonClicks % 2 == 0) {
                Log.i(TAG, "load stylesheet newstyle2")
                val tssbytes = resources.openRawResource(R.raw.newstyle2).readBytes()
                Log.i(TAG, "new style newstyle2 $loadTssStyleButtonClicks")
                map_view.themeController().loadStyleSheet(tssbytes)
            } else {
                val tssbytes = resources.openRawResource(R.raw.newstyle).readBytes()
                Log.i(TAG, "new style newstyle $loadTssStyleButtonClicks")
                map_view.themeController().loadStyleSheet(tssbytes)
            }
            loadTssStyleButtonClicks++
        }
    }

    private fun initDayNightItem() {
        val settings = SdkUserSettingInternal.getInstance()
        val title = when (settings.colorTheme) {
            ColorTheme.NIGHT -> {
                "-> Day Mode"
            }
            ColorTheme.DAY -> {
                "-> Auto Mode"
            }
            else -> {
                "-> Night mode"
            }
        }
        tv_day_night.text = title
        layout_day_night.setOnClickListener {
            when {
                ColorTheme.NIGHT == settings.colorTheme -> {
                    SDK.getInstance().updateDayNightMode(DayNightMode.DAY)
                    tv_day_night.text = "-> Auto Mode"
                    Log.i(TAG, "Changed dayNightMode: Day")
                }
                ColorTheme.DAY == settings.colorTheme -> {
                    SDK.getInstance().updateDayNightMode(DayNightMode.AUTO)
                    tv_day_night.text = "-> Night Mode"
                    Log.i(TAG, "Changed dayNightMode: Auto")
                }
                else -> {
                    SDK.getInstance().updateDayNightMode(DayNightMode.NIGHT)
                    tv_day_night.text = "-> Day Mode"
                    Log.i(TAG, "Changed dayNightMode: Night")
                }
            }
        }
    }

    private fun initIsochroneItem() {
        layout_isochrone.setOnClickListener {
            viewModel.showIsochrone = !viewModel.showIsochrone
            if (!viewModel.showIsochrone && testShapeId != null) {
                map_view.shapesController().remove(testShapeId!!)
                testShapeId = null
            }
            tv_isochrone.text = if (viewModel.showIsochrone) "-> Show Isochrone" else "-> Hide Isochrone"
        }
    }

    private fun initSetLocationItem() {
        layout_change_location.setOnClickListener {
            if (!navViewModel.navigationOn.value!!) {
                viewModel.isSettingLocation = true
                Snackbar.make(root, getString(R.string.set_location_message), Snackbar.LENGTH_LONG)
                        .show()
            } else {
                navViewModel.toast.postValue(getString(R.string.not_available))
            }
        }
    }

    private fun initTextScaleItem() {
        tv_change_font_size.text = "-> Big Text"
        var clickCount = 0;
        val list = listOf(
                Pair(4.0f, "-> Small Text"),
                Pair(0.05f, "-> Normal Text"),
                Pair(1.0f, "-> Big Text")
        )
        layout_change_font_size.setOnClickListener {
            val index = clickCount % 3
            map_view.themeController().setTextScale(list[index].first)
            tv_change_font_size.text = list[index].second
            clickCount++
        }
    }


    private fun initStartFreeDriveItem() {
        tv_free_drive.text = "Start free drive"
        layout_free_drive.setOnClickListener {
            if (!startFreeDrive) {
                tv_free_drive.text = "Stop free drive"
                map_view.cameraController().enableFollowVehicleMode(Camera.FollowVehicleMode.HeadingUp, true)
            } else {
                tv_free_drive.text = "Start free drive"
                map_view.cameraController().disableFollowVehicle()
            }
            startFreeDrive = !startFreeDrive
        }
    }

    private fun initSetVehicleHeadingItem() {
        layout_vehicle_heading.setOnClickListener {
            val fragment = SetVehicleHeadingDialogFragment()
            fragment.show(childFragmentManager, "Heading")
        }
    }

    private fun initSetFPSDataItem() {
        slider_fps.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, value: Int, fromUser: Boolean) {
                setFPSLabel.setText("Set FPS (" + value + ")")
                map_view.setFPS(value)
            }

            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {}
        })
    }

    private fun initSetPrefetchDataItem() {
        sc_prefetch.setOnCheckedChangeListener { _, isChecked ->
            navViewModel.enablePrefetchData(isChecked)
        }
    }

    private fun initZoomSetItem() {
        btn_zoom_up.setOnClickListener {
            val zoomLevel = map_view.cameraController().position.zoomLevel
            setZoomLevel(zoomLevel + 1)
        }
        btn_zoom_down.setOnClickListener {
            val zoomLevel = map_view.cameraController().position.zoomLevel
            setZoomLevel(zoomLevel - 1)
        }
    }

    private fun initLocationProviderItem() {
        rg_location.setOnCheckedChangeListener { _, checkedId ->
            val type = when (checkedId) {
                R.id.rb_default -> DemoLocationProvider.ProviderType.SIMULATION
                R.id.rb_gps -> DemoLocationProvider.ProviderType.REAL_GPS
                R.id.rb_file -> DemoLocationProvider.ProviderType.FILE
                else -> DemoLocationProvider.ProviderType.SIMULATION
            }
            navViewModel.setLocationProvider(type)
        }
    }

    private fun initShowPOIOnMap() {
        sw_show_poi.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                searchController?.displayPOI(listOf(SEARCH_FOOD))
            } else {
                searchController?.displayPOI(emptyList())
            }
        }
    }


    private fun initSwitchAutoZoomDelegate() {
        sw_az_delegate.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                map_view.routesController().setAutoZoomDataDelegate(NIOAutoZoomDelegate())
            } else {
                map_view.routesController().setAutoZoomDataDelegate(null)
            }
        }
    }

    private fun initShowVersionItem() {
        layout_version.setOnClickListener {
            val version = SDK.getInstance().version
            Toast.makeText(activity, "sdk: ${version.sdkVersion}\n" +
                    "active-data: ${version.activeMapDataVersion}\n" +
                    "base-data: ${version.baseMapDataVersion}\n" +
                    "streaming-version: ${version.streamingMapDataVersion}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initLocaleItem() {
        val localeList = listOf(
                com.telenav.sdk.core.Locale.ENGLISH,
                com.telenav.sdk.core.Locale.GERMAN,
                com.telenav.sdk.core.Locale.FRENCH,
                com.telenav.sdk.core.Locale.DUTCH,
                com.telenav.sdk.core.Locale.SWEDISH,
                com.telenav.sdk.core.Locale.ITALIAN,
                com.telenav.sdk.core.Locale.DANISH,
                com.telenav.sdk.core.Locale.NORWEGIAN,
                com.telenav.sdk.core.Locale.PORTUGUESE,
                com.telenav.sdk.core.Locale.FINNISH,
                com.telenav.sdk.core.Locale.EN_US,
                com.telenav.sdk.core.Locale.RUSSIAN,
                com.telenav.sdk.core.Locale.SIMPLIFIED_CHINESE,
        )
        layout_locale.setOnClickListener {
            AlertDialog.Builder(requireActivity())
                    .setItems(localeList.map { it.name }.toTypedArray()) { _, index ->
                        SDK.getInstance().updateLocale(localeList[index])
                        drawer_layout.close()
                        Toast.makeText(requireContext(), "change locale to ${localeList[index].name}", Toast.LENGTH_SHORT).show()
                    }.setTitle("Select Locale")
                    .create().show()
        }
    }

    private fun setDestinationAndRequestRoute(searchResultItemDao: SearchResultItemDao) {
        if (searchResultItemDao != null) {
            val vehicleLocation = navViewModel.vehicleLocation.value

            val annotation: Annotation?
            if (destinationLocation == null) {
                destinationLocation = searchResultItemDao.location
                val factory = map_view.annotationsController().factory()
                annotation = factory.create(requireContext(), R.drawable.map_pin_green_icon_unfocused, searchResultItemDao?.location)
                annotation?.displayText = Annotation.TextDisplayInfo.Centered(if (TextUtils.isEmpty(searchResultItemDao.displayText)) "Destination" else searchResultItemDao.displayText).apply {
                    this.textColor = Color.RED
                }
                annotation?.style = Annotation.Style.ScreenAnnotationPopup
                map_view.annotationsController().add(arrayListOf(annotation))
                if (vehicleLocation != null) {
                    searchResultItemDao.location?.let {
                        viewModel.requestDirection(vehicleLocation,
                                it
                        )
                    }
                }
            } else {
                val factory = map_view.annotationsController().factory()
                annotation = factory.create(requireContext(), R.drawable.add_location, searchResultItemDao.location)
                annotation?.displayText = Annotation.TextDisplayInfo.Centered("wayPoint" + (wayPointList.size + 1))
                annotation?.style = Annotation.Style.ScreenAnnotationPopup
                map_view.annotationsController().add(arrayListOf(annotation))
                activity?.runOnUiThread {
                    AlertDialog.Builder(SecondFragment@ this.requireContext())
                            .setCancelable(false)
                            .setIcon(R.drawable.add_location)
                            .setTitle("New Annotation")
                            .setPositiveButton("add wayPoint") { dialog, whichButton ->
                                dialog.dismiss()
                                if (vehicleLocation != null) {
                                    searchResultItemDao.location?.let {
                                        wayPointList.add(wayPointList.size,
                                                it
                                        )
                                    }
                                    viewModel.requestDirection(vehicleLocation, destinationLocation!!, wayPointList)
                                }
                            }
                            .setNegativeButton("new destination") { dialog, whichButton ->
                                dialog.dismiss()
                                destinationLocation = null
                                wayPointList.clear()
                                map_view.annotationsController().clear()
                                setDestinationAndRequestRoute(searchResultItemDao)
                            }
                            .setNeutralButton("cancel") { dialog, whichButton ->
                                dialog.dismiss()
                                map_view.annotationsController().remove(arrayListOf(annotation))
                            }
                            .show()
                }
            }

        }
    }

    private fun setVehicleLocation(location: Location?) {
        if (location != null) {
            navViewModel.setCurrentLocation(location)
            viewModel.isSettingLocation = false
        }
    }

    private fun resetMapCamera() {
        navViewModel.disableCameraFollow()
    }

    private fun configureMapView() {
        // Enable all of the MapView features
        map_view.featuresController().traffic().setEnabled()
        map_view.featuresController().landmarks().setEnabled()
        map_view.featuresController().buildings().setEnabled()
        map_view.featuresController().terrain().setEnabled()
        map_view.featuresController().globe().setEnabled()
        map_view.featuresController().compass().setEnabled()
        map_view.featuresController().scaleBar().setEnabled()

        map_view.layoutController().setVerticalOffset(-0.5)

        SDK.getInstance().updateDayNightMode(DayNightMode.DAY)

        // recenter to vehicle position
        map_view.cameraController().position =
                Camera.Position.Builder().setLocation(navViewModel.getCurrentLocation()).build()

        map_view.vehicleController().setIcon(R.drawable.cvp)
        searchEngine = NavSearchEngine(requireContext())
        searchController = map_view.searchController()
        searchController?.injectPoiAnnotationFactory(NavPoiFactory(requireContext(), map_view.annotationsController().factory()))
        searchController?.injectSearchEngine(searchEngine!!)
    }

    private fun startNavigating(routes: List<Route>) {
        var pickedRouteIdx = 0
        if (highlightedRoute != null) {
            routes.forEachIndexed { index, route ->
                if (route.id == highlightedRoute) {
                    pickedRouteIdx = index
                }
            }
        }

        val pickedRoute = routes[pickedRouteIdx]
        if (navViewModel.startNavigation(pickedRoute)) {
            // Remove any other routes from view
            routeIds.forEachIndexed { index, element ->
                if (index != pickedRouteIdx) {
                    map_view.routesController().remove(element)
                }
            }
        }

        if (updateRouteProgress) {
            map_view.routesController().updateRouteProgress(pickedRoute.id)
        }

        setZoomLevel(SecondViewModel.DEFAULT_ZOOM_LEVEL)

        val status = map_view.mapDiagnosis().mapViewStatus
        if (status != null) {
            Log.i(TAG, "VieStatus cameraLatitude: " + status.cameraLatitude + " cameraLongitude: " + status.cameraLongitude)
        }
        // set camera follow vehicle state on
        navViewModel.enableCameraFollow()

        trafficBar.visibility = View.VISIBLE
        trafficBar.initTrafficInfo(pickedRoute)
    }

    private fun stopNavigating() {
        navViewModel.stopNavigation()
        // TODO: use user selected route & clear route arrow.

        junction_view.hideImage()
        routeIds = emptyList()
        viewModel.routes.value = emptyList()
        destinationLocation = null
        wayPointList.clear()
        map_view.annotationsController().clear()
        map_view.routesController().clear()
        navViewModel.disableCameraFollow()

        trafficBar.visibility = View.GONE
    }

    // Test functions: @TODO: Move these to their own file
    private var testShapeId: ShapesController.Id? = null
    private fun testShapeApi(mapView: MapView, points: List<LatLon>) {

        if (testShapeId == null) {
            val coords = ArrayList<LatLon>()

            coords.add(LatLon(30.0, -85.0));
            coords.add(LatLon(40.0, -85.0));
            coords.add(LatLon(40.0, -105.0));
            coords.add(LatLon(30.0, -105.0));
            coords.add(coords.first());

            val attributes = Attributes.Builder()
                    .setShapeStyle("route.CLOSED_EDGE")
                    .setColor(0xFF00FF00.toInt())
                    .setLineWidth(100.0f)
                    .build();

            val shape = Shape(Shape.Type.Polyline, attributes, points)

            val collectionBuilder = Shape.Collection.Builder()
            collectionBuilder.addShape(shape)

            testShapeId = mapView.shapesController().add(collectionBuilder.build())
            if (testShapeId != null)
                mapView.shapesController().setAlphaValue(testShapeId!!, 0.5f)
        } else {
            mapView.shapesController().remove(testShapeId!!)
            testShapeId = null
        }
    }

    var snapCount = 0;
    private fun testSnapshots() {
        val current = snapCount++ % 3
        if (current == 0 && subView != null) {
            subView?.let { it1 -> testSnapshot(it1, "SubMapView Snapshot") }
        } else if (current == 1) {
            testOffscreenSnapshot()
        } else { // current == 2
            testSnapshot(map_view, "MapView Snapshot")
        }
    }

    private fun testSnapshot(mapView: MapView, message: String) {
        var activity = activity
        mapView.generateSnapshot {
            activity?.runOnUiThread {
                displayBitmap(it, message)
            }
        }
    }

    private fun testOffscreenSnapshot() {
        map_view.generateOffscreenSnapshot(1000, 1000, {
            // Configure snapshot, set region, add annotations, load styles, etc...
            // Destin, FL

            //val location = Location("")
            //location.latitude = 30.3935
            //location.longitude = -86.4958
            //it.cameraController().position = Camera.Position.Builder().setLocation(location).setZoomLevel(7f).build()

            // Show the region of a pseudo-home area rectangle
            val region = Camera.Region()
            region.extend(30.3935, -86.4958)
            region.extend(30.3935, -86.5958)
            region.extend(30.4935, -86.5958)
            region.extend(30.4935, -86.4958)

            it.cameraController().showRegion(region, Margins.Percentages(0.20, 0.20))

            // Draw a pseudo-home area rectangle
            val coords = ArrayList<LatLon>()
            coords.add(LatLon(30.3935, -86.4958));
            coords.add(LatLon(30.3935, -86.5958));
            coords.add(LatLon(30.4935, -86.5958));
            coords.add(LatLon(30.4935, -86.4958));
            coords.add(coords.first());

            val attributes = Attributes.Builder()
                    .setShapeStyle("route.trace")
                    .setColor(0xFF00FF00.toInt())
                    .setLineWidth(100.0f)
                    .build();

            val shape = Shape(Shape.Type.Polyline, attributes, coords)

            val collectionBuilder = Shape.Collection.Builder()
            collectionBuilder.addShape(shape)

            // Don't have to keep track of shapeId since entire view will be closed
            val shapeId = it.shapesController().add(collectionBuilder.build())
        }, {
            activity?.runOnUiThread {
                displayBitmap(it, "Offscreen Snapshot")
            }
        })
    }

    private fun displayBitmap(bitmap: Bitmap, message: String) {
        val context = this.context
        if (context == null)
            return

        val builder = Dialog(context)
        builder.setTitle(message)
        //builder.requestWindowFeature(Window.FEATURE_NO_TITLE)
        builder.getWindow()?.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT))
        builder.setOnDismissListener {
            //nothing;
        }

        val imageView = ImageView(context)
        imageView.setImageBitmap(bitmap)
        builder.addContentView(imageView, RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT))
        builder.show()
    }

    var largeAnnotationList: List<Annotation> = ArrayList()
    private fun testLargeNumberOfAnnotations() {
        if (largeAnnotationList.isEmpty()) {
            largeAnnotationList = createLargeAmountOfAnnotations()
            map_view.annotationsController().add(largeAnnotationList)
        } else {
            map_view.annotationsController().remove(largeAnnotationList)
            largeAnnotationList = ArrayList()
        }
    }

    private fun createLargeAmountOfAnnotations(): List<Annotation> {
        val annotations: MutableList<Annotation> = ArrayList()

        val camPos = map_view.cameraController().position

        val max = 2000
        var count = 0

        var x = 0.0
        var y = 0.0
        var angle = 0.0

        val a = 0.005
        val b = 0.005

        annotations.add(createAnnotation(camPos.location))

        // Fill the screen with a spiral of annotations
        while (count < max) {

            angle = 0.1 * count;
            x = (a + b * angle) * cos(angle);
            y = (a + b * angle) * sin(angle);

            val currLocation = camPos.location
            currLocation.latitude += y
            currLocation.longitude += x

            annotations.add(createAnnotation(currLocation))

            count++
        }

        return annotations
    }

    private fun createAnnotation(location: Location): Annotation {
        val factory = map_view.annotationsController().factory()
        val annotation = factory.create(requireContext(), R.drawable.map_pin_green_icon_unfocused, location)
        annotation?.displayText = Annotation.TextDisplayInfo.Centered("test")
        annotation?.style = Annotation.Style.ScreenAnnotationPopup
        return annotation
    }

    var stage = 0
    var annotationLayerTestList: List<Annotation> = ArrayList()
}