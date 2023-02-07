/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.search

import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.telenav.map.api.Annotation
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.Margins
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.controllers.VehicleController
import com.telenav.map.api.touch.TouchType
import com.telenav.sdk.common.model.DayNightMode
import com.telenav.sdk.core.Callback
import com.telenav.sdk.demo.util.BitmapUtils
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.search.EntitySearchResponse
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.SDK
import kotlinx.android.synthetic.main.fragment_search_show_poi.*
import kotlinx.android.synthetic.main.layout_action_bar.*
import kotlinx.android.synthetic.main.layout_content_map_nav.btnStartNav
import kotlinx.android.synthetic.main.layout_content_map_nav.btnStopNav
import kotlinx.android.synthetic.main.layout_content_map_nav.btn_show_menu
import kotlinx.android.synthetic.main.layout_content_map_nav.ivFix
import kotlinx.android.synthetic.main.layout_content_map_nav.mapView
import kotlinx.android.synthetic.main.layout_operation_show_poi.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 * This fragment shows how to show poi on map
 */
class SearchShowPoiFragment : Fragment() {

    companion object{
        /**
         * INDEX, NAV_LONGITUDE, NAV_LATITUDE is key of bundle
         */
        const val INDEX = "index"
        const val NAV_LONGITUDE = "longitude"
        const val NAV_LATITUDE = "latitude"
    }

    private val viewModel : SearchNavViewModel by viewModels()
    private var destinationAnnotation : Annotation? = null

    private val searchList = listOf(
        CheckBoxData("School",  R.drawable.baseline_school_24),
        CheckBoxData("Hotel", R.drawable.baseline_apartment_24),
        CheckBoxData("Hospital", R.drawable.baseline_local_hospital_24),
    )

    private lateinit var adapter: CheckBoxAdapter

    private val searchResult = HashMap<String, List<SearchResultModel>>()
    private val annotationList = ArrayList<Annotation>()
    private var vehicleController: VehicleController? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search_show_poi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_title.text = getString(R.string.title_activity_search_show_POI)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }
        btn_show_menu.setOnClickListener {
            drawer_layout.open()
        }
        mapViewInit(savedInstanceState)
        initObserver()
        initOperation()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    private fun mapViewInit(savedInstanceState: Bundle?) {
        SDK.getInstance().updateDayNightMode(DayNightMode.DAY)
        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener = {
                vehicleController = mapView.vehicleController()
                searchList()
            }
        )
        mapView.initialize(mapViewConfig)

        mapView.setOnAnnotationTouchListener { touchType, position, touchedAnnotations ->
            if (viewModel.isNavigationOn()){
                return@setOnAnnotationTouchListener
            }
            if (touchType == TouchType.Click || touchType == TouchType.LongClick){
                mapView.routesController().clear()
                val bundle = touchedAnnotations[0].annotation.extraInfo
                if (bundle == null || bundle.getInt(INDEX, -1) == -1){
                    return@setOnAnnotationTouchListener
                }
                val location = Location("search").apply {
                    this.latitude = bundle.getDouble(NAV_LATITUDE)
                    this.longitude = bundle.getDouble(NAV_LONGITUDE)
                }

                viewModel.currentVehicleLocation.value?.let {
                    viewModel.requestDirection(it, location)
                }
            }
        }
    }

    private fun initOperation() {
        btnStartNav.setOnClickListener {
            viewModel.startNavigation(mapView)
            btnStartNav.isEnabled = false
            btnStopNav.isEnabled = true
        }

        btnStopNav.setOnClickListener {
            viewModel.stopNavigation(mapView)
            destinationAnnotation?.let {
                mapView.annotationsController().remove(mutableListOf(it))
            }

            btnStartNav.isEnabled = false
            btnStopNav.isEnabled = false
        }

        ivFix.setOnClickListener {
            mapView.cameraController().position = Camera.Position.Builder().setLocation(viewModel.currentVehicleLocation.value).build()
        }

        adapter = CheckBoxAdapter(recyclerView, searchList){ index, checked ->
            if (checked){
                showPoiByIndex(index)
            }else{
                hidePoiByIndex(index)
            }
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun initObserver() {
        viewModel.currentVehicleLocation.observe(viewLifecycleOwner){
            vehicleController?.setLocation(it)
        }

        viewModel.route.observe(viewLifecycleOwner){
            mapView.routesController().clear()// clear old route
            if (it != null) {
                val routeIds = mapView.routesController().add(listOf(it))
                mapView.routesController().highlight(routeIds[0]!!)
                val region = mapView.routesController().region(routeIds)
                mapView.cameraController().showRegion(region, Margins.Percentages(0.20, 0.20))
            }

            btnStartNav.isEnabled = it != null
            btnStopNav.isEnabled = false
        }
    }

    private fun searchList(){
        searchResult.clear()
        for(index in searchList.indices){
            search(index)
        }
    }

    /**
     * Show POI
     */
    private fun showPoiByIndex(index: Int){
        val list = searchResult[searchList[index].text] ?: emptyList()
        for (item in list){
            val location = item.geoLocation
            val bitmap = BitmapUtils.getBitmapFromVectorDrawable(requireContext(), searchList[index].icon) ?:
            BitmapFactory.decodeResource(resources,R.drawable.map_pin_green_icon_unfocused)
            val annotation = mapView.annotationsController().factory().create(requireContext(), Annotation.UserGraphic(bitmap), location)
            annotation.extraInfo = Bundle().apply {
                this.putInt(INDEX, index)
                this.putDouble(NAV_LONGITUDE, item.navLocation.longitude)
                this.putDouble(NAV_LATITUDE, item.navLocation.latitude)
            }
            annotation.style = Annotation.Style.ScreenAnnotationPopup
            annotationList.add(annotation)
        }
        mapView.annotationsController().add(annotationList)
    }

    /**
     * Hide POI
     */
    private fun hidePoiByIndex(index: Int){
        val removeList = annotationList.filter {
            index == it.extraInfo?.getInt(INDEX, -1)
        }
        mapView.annotationsController().remove(removeList)
        annotationList.removeAll(removeList)
    }

    /**
     * Do search
     */
    private fun search(index: Int){
        CoroutineScope(Dispatchers.Default).launch {
            val currentLocation = viewModel.initLocation
            val entityClient = EntityService.getClient()
            val text = searchList[index].text
            entityClient.searchRequest()
                .setQuery(text)
                .setLocation(currentLocation.latitude, currentLocation.longitude)
                .asyncCall(object : Callback<EntitySearchResponse> {

                    override fun onSuccess(response: EntitySearchResponse) {
                        onSearchSuccess(index, response)
                    }

                    override fun onFailure(t: Throwable) {
                        t.printStackTrace()
                        onSearchFail(index)
                    }
                })
        }
    }

    private fun onSearchSuccess(index: Int, response: EntitySearchResponse){
        CoroutineScope(Dispatchers.Main).launch {
            activity?.let {
                searchResult[searchList[index].text] = response.results.mapNotNull {
                    SearchResultModel.wrapperFromEntity(it)
                }
                searchList[index].enable = true
                adapter.notifyItemChanged(index)
            }
        }
    }

    private fun onSearchFail(index: Int){
        CoroutineScope(Dispatchers.Main).launch {
            activity?.let {
                searchResult.remove(searchList[index].text)
                searchList[index].enable = false
                adapter.notifyItemChanged(index)
            }
        }
    }

    private class CheckBoxAdapter(val recyclerView: RecyclerView,
                                  val checkBoxDataList: List<CheckBoxData>,val checkedChanged: (Int, Boolean) -> Unit) : RecyclerView.Adapter<CheckBoxViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckBoxViewHolder =
            CheckBoxViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_search_check_box_item,
                parent, false)).apply {
                this.cb.setOnCheckedChangeListener(onCheckedChanged)
            }

        override fun onBindViewHolder(holder: CheckBoxViewHolder, position: Int) {
            holder.cb.text = checkBoxDataList[position].text
            holder.cb.isEnabled = checkBoxDataList[position].enable
        }

        override fun getItemCount(): Int = checkBoxDataList.size

        val onCheckedChanged = CompoundButton.OnCheckedChangeListener{ buttonView, isChecked ->
            val pos = recyclerView.getChildAdapterPosition(buttonView)
            if (pos >= 0 && pos < checkBoxDataList.size){
                checkedChanged(pos, isChecked)
            }
        }

    }

    private class CheckBoxViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val cb: CheckBox = root.findViewById(R.id.cb)
    }

    private data class CheckBoxData(val text: String, val icon: Int, var enable: Boolean = false)

}