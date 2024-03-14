/*
 *  Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.navigation.avoid

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.telenav.map.api.Annotation
import com.telenav.map.api.Margins
import com.telenav.sdk.examples.scenario.navigation.BaseNavFragment
import com.telenav.sdk.examples.util.AndroidThreadUtils
import com.telenav.sdk.drivesession.model.BetterRouteUpdateProgress
import com.telenav.sdk.drivesession.model.NavigationEvent
import com.telenav.sdk.drivesession.model.RerouteRequest
import com.telenav.sdk.drivesession.model.StepInfo
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.ContentBasicNavigationBinding
import com.telenav.sdk.examples.databinding.FragmentAvoidStepBinding
import com.telenav.sdk.map.direction.model.DirectionErrorCode
import com.telenav.sdk.map.direction.model.GeoLocation
import com.telenav.sdk.map.direction.model.Waypoint
import com.telenav.sdk.uikit.turn.TnTurnListItem
import com.telenav.sdk.uikit.turn.TnTurnListRecyclerViewAdapter

/**
 * A simple [Fragment] for avoid step
 * @author wu.changzhong on 2021/9/13
 */

class AvoidStepFragment : BaseNavFragment<FragmentAvoidStepBinding>(), TnTurnListRecyclerViewAdapter.OnItemClickListener {

    private lateinit var viewModel: AvoidStepViewModel
    private var destinationLocation: Location? = null
    private var wayPointList = mutableListOf<Waypoint>()
    private var wayPointAnnotationList = mutableListOf<Annotation>()

    private val tnTurnListAdapter: TnTurnListRecyclerViewAdapter = TnTurnListRecyclerViewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = AvoidStepViewModel(tnTurnListAdapter)
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAvoidStepBinding {
        val binding: FragmentAvoidStepBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_avoid_step, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding
    }

    override fun getBaseBinding(): ContentBasicNavigationBinding {
        return binding.contentBasicNavigation
    }

    override fun onNavigationRouteUpdating(progress: BetterRouteUpdateProgress) {
        if (progress.status == BetterRouteUpdateProgress.Status.SUCCEEDED) {
            findMapView().routesController().clear()
            routeIds =
                ArrayList(findMapView().routesController().add(mutableListOf(progress.newRoute)))
            findMapView().routesController().highlight(routeIds[0])
            findMapView().routesController().updateRouteProgress(routeIds[0])

            highlightedRouteId = routeIds[0]

            viewModel.updateTurnListItem()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.turnDirectionRecyclerView.adapter = tnTurnListAdapter
        navigationOn.observe(owner = viewLifecycleOwner) {
            if (!it) {
                destinationLocation = null
                wayPointList.clear()
                wayPointAnnotationList.clear()
            }
            viewModel.showNavigationDetails.postValue(it)
            viewModel.handleNavTurnList(navigationSession)
        }
        tnTurnListAdapter.setOnItemClickListener(this)
    }

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        viewModel.onNavigationEventUpdated(navEvent)
    }

    override fun onItemClick(view: View, tnTurnListItem: TnTurnListItem) {
        val stepInfo = tnTurnListItem.stepInfo
        if (stepInfo != null) {
            showAvoidDialog(view, stepInfo)
        }
    }

    override fun onLongClick(location: Location?) {
        if (location == null) {
            return
        }
        val annotation: Annotation?
        if (destinationLocation == null) {
            destinationLocation = location
            val factory = findMapView().annotationsController().factory()
            annotation =
                factory.create(requireContext(), R.drawable.map_pin_green_icon_unfocused, location)
            annotation.displayText = Annotation.TextDisplayInfo.Centered("Destination")
            findMapView().annotationsController().add(arrayListOf(annotation))
            vehicleLocation?.let { requestDirection(it, location) }
        } else {
            val factory = findMapView().annotationsController().factory()
            annotation = factory.create(requireContext(), R.drawable.add_location, location)
            annotation.displayText =
                Annotation.TextDisplayInfo.Centered("wayPoint" + (wayPointList.size + 1))
            findMapView().annotationsController().add(arrayListOf(annotation))
            activity?.runOnUiThread {
                AlertDialog.Builder(TurnByTurnListFragment@ this.requireContext())
                    .setCancelable(false)
                    .setIcon(R.drawable.add_location)
                    .setTitle("New Annotation")
                    .setPositiveButton("add wayPoint") { dialog, whichButton ->
                        dialog.dismiss()
                        wayPointList.add(wayPointList.size, Waypoint(GeoLocation(location)))
                        wayPointAnnotationList.add(annotation)
                        if (vehicleLocation != null) {
                            requestDirection(vehicleLocation!!, destinationLocation!!, wayPointList)
                        }
                    }
                    .setNegativeButton("new destination") { dialog, _ ->
                        dialog.dismiss()
                        destinationLocation = null
                        wayPointList.clear()
                        wayPointAnnotationList.clear()
                        findMapView().annotationsController().clear()
                        onLongClick(location)
                    }
                    .setNeutralButton("cancel") { dialog, _ ->
                        dialog.dismiss()
                        findMapView().annotationsController().remove(arrayListOf(annotation))
                    }
                    .show()
            }
        }

    }

    private fun showAvoidDialog(view: View, stepInfo: StepInfo) {
        val builder = android.app.AlertDialog.Builder(requireActivity())
        builder.setTitle("avoidStep")
        builder.setPositiveButton("Avoid") { dialog, _ ->
            val request = RerouteRequest.Builder().setDestination(
                GeoLocation(50.094149, 8.690891)
            ).build()
            val task = navigationSession?.createRerouteTask(request)
            task?.runAsync{
                AndroidThreadUtils.runOnUiThread {
                    if (it.getResponse().getStatus() == DirectionErrorCode.OK) {
                        val routeInfo = it.getResponse()
                        val route = routeInfo.getRoute()!!
                        val routesController = mapView.getRoutesController()!!
                        routeIds = routesController.add(mutableListOf(route))!!.toMutableList()
                        routesController.highlight(routeIds[0])
                        val region = routesController.region(routeIds)
                        mapView.cameraController()
                            .showRegion(region, Margins.Percentages(0.20, 0.20))
                        highlightedRouteId = routeIds[0]
                        navigationSession?.acceptRerouteResult(routeInfo)
                        Toast.makeText(activity, "success", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(activity, "Reroute error!!", Toast.LENGTH_LONG).show()
                    }

                    dialog.dismiss()
                }
            }
        }
        builder.setNegativeButton("cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

}