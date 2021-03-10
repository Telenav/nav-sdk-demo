/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.navigation

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.telenav.sdk.demo.R
import com.telenav.sdk.demo.databinding.FragmentNavWhereamiBinding
import com.telenav.sdk.drivesession.model.StreetInfo
import kotlinx.android.synthetic.main.content_basic_navigation.*
import kotlinx.android.synthetic.main.fragment_nav_whereami.*

/**
 * A simple [Fragment] for whereami and location provider
 * @author tang.hui on 2021/1/22
 */
class WhereamiFragment : BaseNavFragment() {

    private val viewModel: WhereamiViewModel by viewModels()
    var isSettingLocation: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: FragmentNavWhereamiBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_nav_whereami, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigationOn.observe(viewLifecycleOwner) {
            viewModel.showNavigationDetails.postValue(it)
        }
        setVehicleLocation.setOnClickListener {
            stopNavButton.performClick()
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

    override fun onStreetUpdated(curStreetInfo: StreetInfo, drivingOffRoad: Boolean) {
        super.onStreetUpdated(curStreetInfo, drivingOffRoad)
        curStreetInfo.streetName?.let {
            viewModel.currentStreetName.postValue("current street:$it")
        }

        (curStreetInfo.adminInfo)?.let {
            viewModel.countryLiveData.postValue("current country: ${it.countryCode}")

            var cityName = it.city
            if (cityName.isNullOrEmpty()) {
                cityName = it.subCity
            }

            if (!cityName.isNullOrEmpty()) {
                viewModel.countyLiveData.postValue("current county: ${cityName}")
            }
        }

        if (drivingOffRoad) {
            viewModel.roadStateLiveData.postValue("off-road state:off-road")
        } else {
            viewModel.roadStateLiveData.postValue("off-road state:along road")
        }
    }

    override fun onLocationUpdated(vehicleLocation: Location) {
        super.onLocationUpdated(vehicleLocation)
        viewModel.onLocationUpdated(vehicleLocation)
    }


}