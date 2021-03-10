/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.navigation

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.telenav.sdk.common.model.DayNightMode
import com.telenav.sdk.demo.R
import com.telenav.sdk.demo.databinding.FragmentNavAlertInfoBinding
import com.telenav.sdk.drivesession.listener.AlertEventListener
import com.telenav.sdk.drivesession.model.AlertEvent
import com.telenav.sdk.drivesession.model.alert.ExitInfo
import com.telenav.sdk.drivesession.model.alert.ExitType
import com.telenav.sdk.map.SDK

/**
 * A simple [Fragment] for alert info
 * @author tang.hui on 2021/1/27
 */
class AlertInfoFragment : BaseNavFragment(), AlertEventListener {

    private val viewModel: AlertInfoViewModel by viewModels()
    private var destinationLocation: Location? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: FragmentNavAlertInfoBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_nav_alert_info, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        driveSession.enableAlert(true)
        driveSession.eventHub.addAlertEventListener(this)
        SDK.getInstance().updateDayNightMode(DayNightMode.DAY)//high way can be seen clearly on DayMode
        navigationOn.observe(viewLifecycleOwner) {
            viewModel.showNavigationDetails.postValue(it)
            if (!it) {
                destinationLocation = null
            }
        }
        viewModel.resetLiveData.observe(viewLifecycleOwner) {
            if (it) {
                destinationLocation = null
            }
        }
    }

    override fun onDestroyView() {
        driveSession.eventHub.removeAlertEventListener(this)
        super.onDestroyView()
    }

    override fun getDemonstrateSpeed(): Double {
        return 140.0 // set speed faster
    }

    override fun onLongClick(location: Location?) {
        if (destinationLocation == null) {
            destinationLocation = location
            /**
             * set the location of vehicle at high way
             */
            location?.let { locationProvider.setLocation(it) }
        } else {
            super.onLongClick(location)
        }
    }

    override fun onAlertEventUpdated(alertEvent: AlertEvent) {
        if (viewModel.resetLiveData.value == true) {
            return
        }
        Log.d(TAG, "onAlertEventUpdated aheadHighwayInfoItems:${alertEvent.aheadHighwayInfoItems}")
        alertEvent.aheadHighwayInfoItems?.let { it ->
            activity?.runOnUiThread {
                viewModel.dataList.clear()
                viewModel.dataList.add("highWay exits")
                viewModel.serviceAreaList.clear()
                viewModel.serviceAreaList.add("service areas")
                Log.v("alert event", "HW EXITs: + ${it.size}, detail:")

                for ((index, item) in it.withIndex()) {
                    var roadNumber = String()

                    item.highwayName?.routeNumbers?.let {
                        roadNumber = item.highwayName?.routeNumbers!![0].orthography?.content.toString();
                    }

                    val exitLabel = getExitLabel(item.exitInfo)

                    val locationStr = "location: [%.6f, %.6f]; distance: %d".format(
                            item.location?.lat,
                            item.location?.lon,
                            item.distanceToVehicle
                    )
                    val detailHighwayExit = "${index + 1}; RN: ${roadNumber}; EXIT label: ${exitLabel}, " + locationStr;
                    Log.v("HW Exit detail", detailHighwayExit)

                    val sb = StringBuilder()
                    sb.append("roadNumber: $roadNumber").append("\n")
                    sb.append("EXIT label: $exitLabel").append("\n")
                    sb.append("ExitType: ${getExitType(item.exitInfo)}").append("\n")
                    sb.append(locationStr)
                    viewModel.dataList.add(sb.toString())

                    item.exitInfo?.let {
                        if (it.type == ExitType.TO_SERVICE_AREA) {
                            val sb = StringBuilder()
                            sb.append("roadNumber: $roadNumber").append("\n")
                            sb.append("EXIT label: $exitLabel").append("\n")
                            sb.append("ExitType: ${getExitType(item.exitInfo)}").append("\n")
                            sb.append(locationStr)
                            viewModel.serviceAreaList.add(sb.toString())
                        }
                    }

                }
                viewModel.adapter.notifyDataSetChanged()
                viewModel.serviceAreaAdapter.notifyDataSetChanged()
            }

        }

        alertEvent.aheadAlertItems?.let { it ->
            activity?.runOnUiThread {
                viewModel.cameraList.clear()
                viewModel.cameraList.add("camera info")

                val sb = StringBuilder()
                it.forEach { item ->
                    if (sb.isNotEmpty()) {
                        sb.append("\n")
                    }
                    Log.d(TAG, "onAlertEventUpdated cameraInfo:${item.cameraInfo}")
                    sb.append("fixtureStatus: ${item.cameraInfo?.fixtureStatus}").append("\n")
                    sb.append("speedLimit: ${item.cameraInfo?.speedLimit}")
                    viewModel.cameraList.add(sb.toString())
                }

                viewModel.cameraAdapter.notifyDataSetChanged()
            }
        }

    }

    private fun getExitLabel(exitInfo: ExitInfo?): String {
        exitInfo?.let { info ->
            info.exitNumber?.let {
                return it
            }
        }
        return ""
    }

    private fun getExitType(exitInfo: ExitInfo?): String {
        exitInfo?.let {
            return when (it.type) {
                ExitType.TO_HIGHWAY -> "TO_HIGHWAY"
                ExitType.TO_LOCAL -> "TO_LOCAL"
                ExitType.TO_SERVICE_AREA -> "TO_SERVICE_AREA"
                ExitType.TO_LOCAL_AND_HIGHWAY -> "TO_LOCAL_AND_HIGHWAY"
                else -> "INVALID"
            }

        }
        return ""
    }


}