/*
 *  Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.navigation

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.telenav.sdk.drivesession.model.PositionInfo
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.ContentBasicNavigationBinding
import com.telenav.sdk.examples.databinding.FragmentNavWhereamiBinding

/**
 * A simple [Fragment] for whereami and location provider
 * @author tang.hui on 2021/1/22
 */
class WhereamiFragment : BaseNavFragment<FragmentNavWhereamiBinding>() {

    private val viewModel: WhereamiViewModel by viewModels()
    var isSettingLocation: Boolean = false

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNavWhereamiBinding {
        val binding: FragmentNavWhereamiBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_nav_whereami, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding
    }

    override fun getBaseBinding(): ContentBasicNavigationBinding {
        return binding.contentBasicNavigation
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigationOn.observe(viewLifecycleOwner) {
            if (!it) routes.clear()
        }
        binding.setVehicleLocation.setOnClickListener {
            driveSession.stopNavigation()
            findMapView().annotationsController().clear()
            findMapView().routesController().clear()
            findMapView().cameraController().disableFollowVehicle()
            getBaseBinding().navButton.setText(R.string.stop_navigation)
            getBaseBinding().navButton.isEnabled = false
            navigating = false
            isSettingLocation = true
        }
    }

    override fun onLongClick(location: Location?) {
        if (isSettingLocation) {
            /**
             * change the location of vehicle
             */
            location?.let { locationProvider.setLocation(it) }
            isSettingLocation = false
        } else {
            super.onLongClick(location)
        }
    }

    override fun onLocationUpdated(vehicleLocation: Location, positionInfo: PositionInfo) {
        viewModel.onLocationUpdated(vehicleLocation)
        positionInfo.currentRoad?.roadName?.let {
            viewModel.currentStreetName.postValue("current street:$it")
        }

        positionInfo.regionalInfo?.admin?.let {
            viewModel.countryLiveData.postValue("current country: ${it.country}")

            var cityName = it.city
            if (cityName.isNullOrEmpty()) {
                cityName = it.subCity
            }

            if (!cityName.isNullOrEmpty()) {
                viewModel.countyLiveData.postValue("current county: $cityName")
            }
        }

        if (positionInfo.currentRoad == null) {
            viewModel.roadStateLiveData.postValue("off-road state:off-road")
        } else {
            viewModel.roadStateLiveData.postValue("off-road state:along road")
        }

        val previousDistanceToVehicle = positionInfo.behindIntersection?.run { distance }
        val nextDistanceToVehicle = positionInfo.aheadIntersection?.run { distance }
        val previousContent = positionInfo.behindIntersection?.crossRoads?.getOrNull(0)?.orthography?.content
        val nextContent = positionInfo.aheadIntersection?.crossRoads?.getOrNull(0)?.orthography?.content
        val closetStreetName = positionInfo.nearByRoad?.roadName?.orthography?.content
        val closetStreetDistance = positionInfo.nearByRoad?.distance
        previousDistanceToVehicle.let {
            viewModel.previousStreetDistanceToVehicle.postValue(
                "previousStreetDistance:${
                    getDesDistance(
                        it
                    )
                }"
            )
        }
        nextDistanceToVehicle.let {
            viewModel.nextStreetDistanceToVehicle.postValue(
                "nextStreetDistance:${
                    getDesDistance(
                        it
                    )
                }"
            )
        }
        previousContent.let { viewModel.previousStreetName.postValue("previousStreet:${it ?: ""}") }
        nextContent.let { viewModel.nextStreetName.postValue("nextStreet:${it ?: ""}") }
        closetStreetName.let { viewModel.closetStreetName.postValue("closetStreet:${it ?: ""}") }
        closetStreetDistance.let {
            viewModel.closetStreetDistance.postValue(
                "closetStreetDistance:${
                    getDesDistance(
                        it
                    )
                }"
            )
        }
    }

    private fun getDesDistance(meters: Long?): String? {
        return if (meters != null) {
            "$meters meters"
        } else {
            ""
        }
    }

}