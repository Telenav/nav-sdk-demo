/*
 *  Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.navigation.avoid

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
import com.telenav.sdk.demo.scenario.navigation.BaseNavFragment
import com.telenav.sdk.demo.util.AndroidThreadUtils
import com.telenav.sdk.drivesession.model.NavigationEvent
import com.telenav.sdk.drivesession.model.StepInfo
import com.telenav.sdk.drivesession.model.avoid.AvoidRouteStatus
import com.telenav.sdk.drivesession.model.avoid.FailReason
import com.telenav.sdk.drivesession.model.drg.RouteUpdateContext
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.FragmentAvoidStepBinding
import com.telenav.sdk.map.direction.model.Route
import com.telenav.sdk.uikit.turn.TnTurnListItem
import com.telenav.sdk.uikit.turn.TnTurnListRecyclerViewAdapter
import kotlinx.android.synthetic.main.content_basic_navigation.*
import kotlinx.android.synthetic.main.fragment_nav_turnbyturn_list.*

/**
 * A simple [Fragment] for avoid step
 * @author wu.changzhong on 2021/9/13
 */

class AvoidStepFragment : BaseNavFragment(), TnTurnListRecyclerViewAdapter.OnItemClickListener {

    private lateinit var viewModel: AvoidStepViewModel
    private var destinationLocation: Location? = null
    private var wayPointList = mutableListOf<Location>()
    private var wayPointAnnotationList = mutableListOf<Annotation>()

    private val tnTurnListAdapter: TnTurnListRecyclerViewAdapter = TnTurnListRecyclerViewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = AvoidStepViewModel(tnTurnListAdapter)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentAvoidStepBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_avoid_step, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onNavigationRouteUpdated(route: Route, info: RouteUpdateContext?) {
        super.onNavigationRouteUpdated(route, info)
        viewModel.updateTurnListItem()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        turn_direction_recycler_view.adapter = tnTurnListAdapter
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
            val factory = map_view.annotationsController().factory()
            annotation = factory.create(requireContext(), R.drawable.map_pin_green_icon_unfocused, location)
            annotation?.displayText = Annotation.TextDisplayInfo.Centered("Destination")
            map_view.annotationsController().add(arrayListOf(annotation))
            vehicleLocation?.let { requestDirection(it, location) }
        } else {
            val factory = map_view.annotationsController().factory()
            annotation = factory.create(requireContext(), R.drawable.add_location, location)
            annotation?.displayText = Annotation.TextDisplayInfo.Centered("wayPoint" + (wayPointList.size + 1))
            map_view.annotationsController().add(arrayListOf(annotation))
            activity?.runOnUiThread {
                AlertDialog.Builder(TurnByTurnListFragment@ this.requireContext())
                    .setCancelable(false)
                    .setIcon(R.drawable.add_location)
                    .setTitle("New Annotation")
                    .setPositiveButton("add wayPoint") { dialog, whichButton ->
                        dialog.dismiss()
                        wayPointList.add(wayPointList.size, location)
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
                        map_view.annotationsController().clear()
                        onLongClick(location)
                    }
                    .setNeutralButton("cancel") { dialog, _ ->
                        dialog.dismiss()
                        map_view.annotationsController().remove(arrayListOf(annotation))
                    }
                    .show()
            }
        }

    }

    private fun showAvoidDialog(view: View, stepInfo: StepInfo) {
        val builder = android.app.AlertDialog.Builder(requireActivity())
        builder.setTitle("avoidStep")
        builder.setPositiveButton("Avoid") { dialog, _ ->
            navigationSession?.avoidStep(mutableListOf(stepInfo)
            ) {

                val avoidStatus = it.avoidStatus
                val avoidFailReason = it.failReason

                AndroidThreadUtils.runOnUiThread {

                    if (avoidStatus == AvoidRouteStatus.SUCCESS) {
                        val route = it.route!!
                        routeIds = map_view.routesController().add(mutableListOf(route))
                        map_view.routesController().highlight(routeIds[0])
                        val region = map_view.routesController().region(routeIds)
                        map_view.cameraController().showRegion(region, Margins.Percentages(0.20, 0.20))
                        highlightedRouteId = routeIds[0]
                        navigationSession?.updateRoute(route)
                        Toast.makeText(activity, "success", Toast.LENGTH_LONG).show()
                    } else {
                        when (avoidFailReason) {
                            FailReason.RouteRequestFail -> {
                                Toast.makeText(activity, "RouteRequestFail", Toast.LENGTH_LONG).show()
                            }
                            FailReason.CannotAvoid -> {
                                Toast.makeText(activity, "CannotAvoid", Toast.LENGTH_LONG).show()
                            }
                            FailReason.AvoidRouteInProgress -> {
                                Toast.makeText(activity, "AvoidRouteInProgress", Toast.LENGTH_LONG).show()
                            }
                            FailReason.WrongNavStatus -> {
                                Toast.makeText(activity, "WrongNavStatus", Toast.LENGTH_LONG).show()
                            }
                        }
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