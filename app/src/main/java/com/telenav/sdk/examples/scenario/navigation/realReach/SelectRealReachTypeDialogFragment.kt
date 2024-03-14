package com.telenav.sdk.examples.scenario.navigation.realReach

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
/*
 *  Copyright © 2022 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.telenav.sdk.common.model.EnergyLevel
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.SDK

/**
 * This fragment is used to select request mode
 * @author wu.changzhong on 2022/3/23
 */
class SelectRealReachTypeDialogFragment : DialogFragment() ,RadioGroup.OnCheckedChangeListener,
    SeekBar.OnSeekBarChangeListener{

    private val viewModel:RealReachViewModel by activityViewModels()

    private lateinit var EV: RadioButton
    private lateinit var NOT_EV: RadioButton
    private lateinit var currentEnergyLevel: SeekBar
    private lateinit var llCurrentEnergyLevel: LinearLayout
    private lateinit var setCurrentEnergyLevel: TextView
    private var realReachType: RealReachType = RealReachType.EV

    @SuppressLint("MissingInflatedId")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_real_reach_type,null)
        var rgSource = view.findViewById<RadioGroup>(R.id.rg_source)
        EV = view.findViewById(R.id.rb_ev)
        NOT_EV = view.findViewById(R.id.rb_not_ev)
        currentEnergyLevel = view.findViewById(R.id.currentEnergyLevel)
        llCurrentEnergyLevel = view.findViewById(R.id.ll_currentEnergyLevel)
        rgSource.setOnCheckedChangeListener(this)
        currentEnergyLevel.setOnSeekBarChangeListener(this)
        setCurrentEnergyLevel = view.findViewById(R.id.setCurrentEnergyLevel)
        setCurrentEnergyLevel.text= "setCurrentEnergyLevel (${SDK.getInstance().vehicleInfoProvider.getEnergyLevel()?.evBatteryPercent})"
        currentEnergyLevel?.progress= SDK.getInstance().vehicleInfoProvider.getEnergyLevel()?.evBatteryPercent?.toInt()!!
        val builder = AlertDialog.Builder(activity,R.style.DialogStyle)
                .setTitle("select real reach type mode")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Submit"){_,_ ->
                    viewModel.realReachType.value = realReachType
                }.setView(view)
        setRealReachType()
        return builder.create()
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        when (checkedId) {
            R.id.rb_ev -> {
                realReachType = RealReachType.EV
                llCurrentEnergyLevel.visibility = View.VISIBLE
            }

            R.id.rb_not_ev -> {
                realReachType = RealReachType.NOT_EV
                llCurrentEnergyLevel.visibility = View.GONE
            }
        }
    }

    private fun setRealReachType(){
        when (viewModel.realReachType.value) {
            RealReachType.EV -> {
                EV?.isChecked = true
                NOT_EV?.isChecked = false
                llCurrentEnergyLevel.visibility = View.VISIBLE
            }

            RealReachType.NOT_EV -> {
                EV?.isChecked = false
                NOT_EV?.isChecked = true
                llCurrentEnergyLevel.visibility = View.GONE
            }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        setCurrentEnergyLevel.text = "setCurrentEnergyLevel (${progress})"
        SDK.getInstance().vehicleInfoProvider.setEnergyLevel(EnergyLevel(0f, progress.toFloat()))
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {

    }
}