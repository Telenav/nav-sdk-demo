/*
 *  Copyright © 2022 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */
package com.telenav.sdk.examples.scenario.findchargestation

import android.location.Location
import android.os.Bundle
import android.view.*
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import com.telenav.map.api.Annotation
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.sdk.common.model.EnergyLevel
import com.telenav.sdk.common.model.ResponseErrorCode
import com.telenav.sdk.examples.scenario.navigation.BaseNavFragment
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.ContentBasicNavigationBinding
import com.telenav.sdk.examples.databinding.FragmentFindChargeStationBinding
import com.telenav.sdk.examples.util.BitmapUtils
import com.telenav.sdk.map.SDK
import com.telenav.sdk.map.chargestation.ChargeStationContent
import com.telenav.sdk.map.chargestation.ChargingStationFindingRequest
import com.telenav.sdk.map.chargestation.model.ChargeStationInfo
import com.telenav.sdk.map.chargestation.model.ChargingStrategy

/**
 * This fragment is used to find charge station
 * @author wu.changzhong on 2022/7/30
 */
class FindChargeStationFragment : BaseNavFragment<FragmentFindChargeStationBinding>(), RadioGroup.OnCheckedChangeListener,
    SeekBar.OnSeekBarChangeListener {

    private var chargeStrategy: Int = ChargingStrategy.fasterCharging
    private var maximumNumber: Int = 20
    private var chargeStationList = mutableListOf<ChargeStationInfo>()

    private var annotations: MutableList<Annotation> = java.util.ArrayList()
    private var chargeStationContent: ChargeStationContent? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentFindChargeStationBinding {
        val binding: FragmentFindChargeStationBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_find_charge_station,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        chargeStationContent = SDK.getInstance().chargeStationContent

        return binding
    }

    override fun getBaseBinding(): ContentBasicNavigationBinding {
        return binding.includeContent
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        binding.includeContent.tvTip.text = "Please Long press to move the vehicle position"
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        setupDrawerButtons()
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
            binding.includeFindChargeStation.rbFasterCharging.id -> chargeStrategy = ChargingStrategy.fasterCharging
            binding.includeFindChargeStation.rbLessDetour.id -> chargeStrategy = ChargingStrategy.lessDriving
        }
    }

    private fun setupDrawerButtons() {
        binding.includeFindChargeStation.chargeStrategy.setOnCheckedChangeListener(this)
        binding.includeFindChargeStation.CurrentEnergyLevel.setOnSeekBarChangeListener(this)
        binding.includeFindChargeStation.maximumNumber.setOnSeekBarChangeListener(this)
        findMapView().setOnTouchListener { touchType: TouchType, data: TouchPosition ->
            if (touchType == TouchType.LongClick) {
                data.geoLocation?.apply {
                    locationProvider.setLocation(this)
                    findChargeStation(this)
                }
            }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        when (seekBar) {

            binding.includeFindChargeStation.maximumNumber -> {
                binding.includeFindChargeStation.maximumNumberChargingStations.text = "MaximumNumberChargeStations (${progress})"
                maximumNumber = progress
            }

            binding.includeFindChargeStation.CurrentEnergyLevel -> {
                binding.includeFindChargeStation.setCurrentEnergyLevel.text = "setCurrentEnergyLevel (${progress})"
                SDK.getInstance().vehicleInfoProvider.setEnergyLevel(
                    EnergyLevel(
                        0f,
                        progress.toFloat()
                    )
                )
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {

    }

    private fun findChargeStation(location: Location) {

        findMapView().getAnnotationsController()?.remove(annotations)
        annotations.clear()
        val evConnectTypes = ArrayList<Int>()
        binding.layoutProgress.visibility = View.VISIBLE
        val request = ChargingStationFindingRequest.Builder(location)
            .setLimit(maximumNumber)
            .setPreferredChargerBrandId(arrayListOf("99100314,99100315"))
            .setChargerStrategyPreference(ChargingStrategy.fasterCharging)
            .setEvConnectTypes(evConnectTypes)
            .build()

        val findingTask = chargeStationContent?.createChargingStationFindingTask(request)

        findingTask?.runAsync { response ->
            activity?.runOnUiThread {
                binding.layoutProgress.visibility = View.GONE
                val response = response.getResponse()
                if (response?.status == ResponseErrorCode.OK && response.result.isNotEmpty()) {
                    chargeStationList = response.result as MutableList<ChargeStationInfo>
                    addEvChargingStationAnnotations(chargeStationList)
                    findMapView().getAnnotationsController()?.add(annotations)
                    val region = findMapView().getAnnotationsController()?.region(annotations)
                    findMapView().cameraController().showRegion(region)
                }
            }
        }
    }

    private fun addEvChargingStationAnnotations(chargeStationList: List<ChargeStationInfo>) {
        chargeStationList.forEach { it ->
            val location = it.getLocation()
            location?.apply {
                val annotation = createAnnotation(this)
                annotations.add(annotation)
            }
        }
    }

    private fun createAnnotation(location: Location): Annotation {
        val factory = findMapView().annotationsController().factory()
        val evChargeGraphic = createUserGraphic(R.drawable.ev_charge_station)
        val annotation = factory.create(requireContext(), evChargeGraphic, location)
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

}