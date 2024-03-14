/*
 *  Copyright © 2022 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */
package com.telenav.sdk.examples.scenario.navigation.evtripplan

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.CompoundButton
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.observe
import com.telenav.map.api.Annotation
import com.telenav.map.api.Margins
import com.telenav.map.api.controllers.RouteEdgeIndex
import com.telenav.map.api.controllers.RouteRenderOptions
import com.telenav.sdk.common.model.EnergyLevel
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.examples.scenario.navigation.BaseNavFragment
import com.telenav.sdk.examples.scenario.navigation.TurnbyturnViewModel
import com.telenav.sdk.drivesession.model.NavigationEvent
import com.telenav.sdk.evdirection.EvTripPlanRequest
import com.telenav.sdk.evdirection.model.ChargingPlanPreference
import com.telenav.sdk.evdirection.model.PlanningStrategy
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.ContentBasicNavigationBinding
import com.telenav.sdk.examples.databinding.FragmentEvTripPlanBinding
import com.telenav.sdk.examples.util.BitmapUtils
import com.telenav.sdk.map.SDK
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import com.telenav.sdk.uikit.turn.TnTurnListRecyclerViewAdapter

/**
 * This fragment is used to show EvTripPlan
 * @author wu.changzhong on 2022/3/23
 */
class EvTripPlanFragment : BaseNavFragment<FragmentEvTripPlanBinding>(), RadioGroup.OnCheckedChangeListener,
    CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener{

    var planningStrategy: Int = PlanningStrategy.HigherPowerChargerStrategy
    var chargingPlanPreferenceBuilder: ChargingPlanPreference.Builder? = null
    private var preferredStart = 20
    private var preferredStop = 80
    private var preferredArrival = 20
    private var enableChargingPlanning = true
    private var preferToChargeUponArrival = false
    private var evTripPlanCostTime : Float? = null
    private var routeDistance : Float? = null
    private var estimatedReachableLength: Float? =null

    var annotations: MutableList<Annotation> = java.util.ArrayList()

    private lateinit var viewModel: TurnbyturnViewModel

    private val tnTurnListAdapter: TnTurnListRecyclerViewAdapter = TnTurnListRecyclerViewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = TurnbyturnViewModel(tnTurnListAdapter)
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentEvTripPlanBinding {
        val binding: FragmentEvTripPlanBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_ev_trip_plan,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        tnTurnListAdapter.setOnItemClickListener { _, tnTurnListItem ->
            onTurnListItemClicked(tnTurnListItem)
        }
        return binding
    }

    override fun getBaseBinding(): ContentBasicNavigationBinding {
        return binding.includeContent
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        binding.includeContent.tvTip.visibility = View.INVISIBLE
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        setupDrawerButtons()
        chargingPlanPreferenceBuilder = ChargingPlanPreference.Builder()
        binding.turnDirectionRecyclerView.adapter = tnTurnListAdapter
        navigationOn.observe(owner = viewLifecycleOwner) {
            viewModel.showNavigationDetails.postValue(it)
            viewModel.handleNavTurnList(navigationSession)
        }
    }

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        viewModel.onNavigationEventUpdated(navEvent)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.action_settings) {
            binding.drawerLayout.open()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        when (checkedId) {
            binding.includeTripPlan.rbHigherPowery.id -> planningStrategy = PlanningStrategy.HigherPowerChargerStrategy
            binding.includeTripPlan.rbLessDetour.id -> planningStrategy = PlanningStrategy.LessDetourStrategy
            binding.includeTripPlan.rbAlterNativeRoute.id -> planningStrategy = PlanningStrategy.AlternativeRouteStrategy
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (buttonView == binding.includeTripPlan.scEnableCharging) {
            enableChargingPlanning = isChecked
        } else if (buttonView == binding.includeTripPlan.scPreferToChargeUponArrival) {
            preferToChargeUponArrival = isChecked
        }
    }

    private fun setupDrawerButtons() {
        binding.includeTripPlan.scEnableCharging.setOnCheckedChangeListener(this)
        binding.includeTripPlan.scPreferToChargeUponArrival.setOnCheckedChangeListener(this)
        binding.includeTripPlan.planningStrategy.setOnCheckedChangeListener(this)
        binding.includeTripPlan.arrivalBatteryLevel.setOnSeekBarChangeListener(this)
        binding.includeTripPlan.StartChargingBatteryLeve.setOnSeekBarChangeListener(this)
        binding.includeTripPlan.StartChargingBatteryLeve.setOnSeekBarChangeListener(this)
        binding.includeTripPlan.CurrentEnergyLevel.setOnSeekBarChangeListener(this)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        when (seekBar) {
            binding.includeTripPlan.arrivalBatteryLevel -> {
                binding.includeTripPlan.preferredArrivalBatteryLevel.text= "preferredArrivalBatteryLevel (${progress})"
                preferredArrival = progress
            }
            binding.includeTripPlan.StartChargingBatteryLeve -> {
                binding.includeTripPlan.preferredStartChargingBatteryLevel.text = "preferredStartChargingBatteryLevel (${progress})"
                preferredStart = progress
            }
            binding.includeTripPlan.StopChargingBatteryLevel -> {
                binding.includeTripPlan.preferredStopChargingBatteryLevel.text = "preferredStopChargingBatteryLevel (${progress})"
                preferredStop = progress
            }
            binding.includeTripPlan.CurrentEnergyLevel -> {
                binding.includeTripPlan.setCurrentEnergyLevel.text = "setCurrentEnergyLevel (${progress})"
                SDK.getInstance().vehicleInfoProvider.setEnergyLevel(EnergyLevel(0f, progress.toFloat()))
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {

    }

    @SuppressLint("WrongConstant")
    override fun requestDirection(
        begin: Location,
        end: Location,
        wayPointList: MutableList<Waypoint>?
    ) {
        Log.d(
            "MapLogsForTestData",
            "MapLogsForTestData >>>> requestDirection begin: $begin + end $end"
        )

        binding.layoutProgress.visibility= View.VISIBLE
        val stopEvWaypoints = ArrayList<Waypoint>()

//        val evWaypoint = EvWaypoint(GeoLocation(latLon, Address("", "", "", "1333")),false)
//        stopEvWaypoints.add(evWaypoint)
        val chargingStationBlocklist = ArrayList<String>()

        val chargingPlanPreference = chargingPlanPreferenceBuilder?.apply {
            setPreferredArrivalBatteryLevel(preferredArrival.toLong())
            setPreferredStartChargingBatteryLevel(preferredStart.toLong())
            setPreferredStopChargingBatteryLevel(preferredStop.toLong())
            setEnableChargingPlanning(enableChargingPlanning)
            setPreferToChargeUponArrival(preferToChargeUponArrival)
                .setChargingStationBlocklist(chargingStationBlocklist)
        }?.build()

        val evTripPlanRequest = EvTripPlanRequest.Builder(
            GeoLocation(begin),
            GeoLocation(LatLon(end.latitude, end.longitude))
        )
            .setHeading(20)
            .setRouteCount(3)
            .setSpeedInMps(20)
            .setWaypoints(stopEvWaypoints)
            .setChargingPlanPreference(chargingPlanPreference)
            .build()
        val startTimeMillis = System.currentTimeMillis()
        val evTripPlanTask = DirectionClient.Factory.hybridClient().createEvTripPlanTask(evTripPlanRequest)
        evTripPlanTask.runAsync { response ->
            Log.d(
                TAG,
                "MapLogsForTestData >>>> requestDirection task status: ${response?.response?.status}"
            )
            val stopTimeMillis = System.currentTimeMillis()
            evTripPlanCostTime = (stopTimeMillis -startTimeMillis).toFloat()
            activity?.runOnUiThread {
                binding.time?.text = "${evTripPlanCostTime!! /1000}"
            }
            findMapView().annotationsController().remove(annotations)
            annotations.clear()

            activity?.runOnUiThread{
                binding.layoutProgress.visibility= View.GONE
            }

            var status = response?.response?.status

            if (status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                routes = response.response.result
                val route = routes[0]
                routeDistance = route.distance.toFloat()
                estimatedReachableLength = route.estimatedReachableLength
                var renderOptions: MutableList<RouteRenderOptions> = java.util.ArrayList()
                renderOptions.add(RouteRenderOptions(false, RouteEdgeIndex(0,0,0)))
                routeIds = findMapView().routesController().add(routes, renderOptions).toMutableList()
                findMapView().routesController().highlight(routeIds[0])
                val region = findMapView().routesController().region(routeIds)
                val travelPoints = route.travelPoints
                Log.d("EV", "travelPoints: $travelPoints")
                addEvChargingStationAnnotations(travelPoints)
                findMapView().annotationsController().add(annotations)
                findMapView().cameraController()
                    .showRegion(region, Margins.Percentages(0.20, 0.20))
                activity?.runOnUiThread {
                    binding.ReachableDistance?.text = "$estimatedReachableLength"
                    binding.distance.text = "$routeDistance"
                    binding.includeContent.navButton.isEnabled = true
                    binding.includeContent.navButton.setText(R.string.start_navigation)
                }

            } else {
                activity?.runOnUiThread {
                    binding.includeContent.navButton.isEnabled = false
                }
            }
            evTripPlanTask.dispose()
        }
    }

    private fun addEvChargingStationAnnotations(travelPointList: List<TravelPoint>) {

        travelPointList.forEach { it ->
            val chargingPlan = it.chargingPlan
            var chargingDuration = 0f

            if (chargingPlan != null) {
                chargingDuration = chargingPlan.chargingInstruction?.chargingDuration!!
            }
            val geoLocation = it.location
            val point = geoLocation.displayPoint
            val location = Location("")
            location.latitude = point?.lat!!
            location.longitude = point.lon

            if (chargingDuration > 0) {
                location?.apply {
                    val annotation = createAnnotation(this)
                    annotations.add(annotation)
                }
            }
        }
    }

    private fun createAnnotation(location: Location): Annotation {
        val factory = findMapView().annotationsController().factory()
        val evChargeGraphic = createUserGraphic(R.drawable.ev_charge_station)
        val annotation = factory.create(requireContext(),evChargeGraphic, location)
        annotation?.displayText = Annotation.TextDisplayInfo.Centered("")
        annotation?.style = Annotation.Style.ScreenAnnotationPopup
        return annotation
    }

    private fun createUserGraphic(drawableId: Int): Annotation.UserGraphic {
        return Annotation.UserGraphic(
            activity?.let {
                BitmapUtils.getBitmapFromVectorDrawable(
                    it,
                    drawableId
                )
            }!!
        )
    }

    override fun getDemonstrateSpeed(): Double {
        return 100.0
    }
}